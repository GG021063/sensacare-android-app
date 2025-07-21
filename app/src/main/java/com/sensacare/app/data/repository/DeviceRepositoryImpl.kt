package com.sensacare.app.data.repository

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import com.sensacare.app.data.dao.DeviceDao
import com.sensacare.app.data.dao.SettingDao
import com.sensacare.app.data.entity.DeviceEntity
import com.sensacare.app.data.entity.SettingEntity
import com.sensacare.app.data.entity.SyncStatus
import com.sensacare.app.data.mapper.toDomain
import com.sensacare.app.data.mapper.toDomainList
import com.sensacare.app.data.mapper.toEntity
import com.sensacare.app.domain.model.Device
import com.sensacare.app.domain.model.DeviceFeature
import com.sensacare.app.domain.model.DeviceSettings
import com.sensacare.app.domain.repository.*
import com.sensacare.veepoo.VeepooManager
import com.sensacare.veepoo.model.VeepooDeviceInfo
import com.veepoo.protocol.VPOperateManager
import com.veepoo.protocol.listener.base.IBleWriteResponse
import com.veepoo.protocol.listener.data.IDeviceFwVersionListener
import com.veepoo.protocol.listener.data.ISocialMsgDataListener
import com.veepoo.protocol.model.datas.FunctionSocailMsgData
import com.veepoo.protocol.model.datas.TimeData
import com.veepoo.protocol.model.enums.EFunctionStatus
import com.veepoo.protocol.model.settings.CustomSetting
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private const val TAG = "DeviceRepositoryImpl"

/**
 * Implementation of DeviceRepository that integrates with VeepooSDK and Room database
 *
 * This repository handles all device-related operations including:
 * - Device discovery and pairing via Bluetooth
 * - Connection management with VeepooSDK
 * - Feature detection and management
 * - Battery monitoring and status tracking
 * - Sync operations with error recovery
 * - Device configuration management
 * - Caching and offline support
 */
class DeviceRepositoryImpl(
    private val context: Context,
    private val deviceDao: DeviceDao,
    private val settingDao: SettingDao,
    private val veepooManager: VeepooManager,
    private val bluetoothManager: BluetoothManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : DeviceRepository {

    private val vpOperateManager: VPOperateManager = VPOperateManager.getMangerInstance(context)
    private val deviceConnectionStateMap = ConcurrentHashMap<Long, MutableStateFlow<DeviceConnectionState>>()
    private val deviceScanStateFlow = MutableStateFlow(false)
    private val scanResultsFlow = MutableStateFlow<List<Device>>(emptyList())
    private var scanJob: Job? = null
    private var syncJob: Job? = null
    
    // Cache for device settings to avoid frequent database queries
    private val deviceSettingsCache = ConcurrentHashMap<Long, DeviceSettings>()

    /**
     * ====================================
     * Device Discovery and Pairing
     * ====================================
     */

    override fun startDeviceScan(timeoutSeconds: Int): Flow<DeviceOperationResult<List<Device>>> = flow {
        emit(DeviceOperationResult.Loading)
        
        try {
            if (!isBluetoothEnabled()) {
                emit(DeviceOperationResult.Error(DeviceError.BluetoothDisabledError()))
                return@flow
            }
            
            if (!hasBluetoothPermissions()) {
                emit(DeviceOperationResult.Error(
                    DeviceError.PermissionError(
                        "Bluetooth permissions not granted",
                        "android.permission.BLUETOOTH_SCAN, android.permission.BLUETOOTH_CONNECT"
                    )
                ))
                return@flow
            }
            
            // Clear previous scan results
            scanResultsFlow.value = emptyList()
            
            // Start scanning
            deviceScanStateFlow.value = true
            
            // Cancel any existing scan job
            scanJob?.cancel()
            
            scanJob = CoroutineScope(ioDispatcher).launch {
                try {
                    // Use VeepooSDK to scan for devices
                    val scanResults = suspendCancellableCoroutine<List<VeepooDeviceInfo>> { continuation ->
                        veepooManager.startScan(
                            onDeviceFound = { deviceInfo ->
                                val currentList = scanResultsFlow.value.toMutableList()
                                val device = mapVeepooDeviceInfoToDevice(deviceInfo)
                                if (!currentList.any { it.macAddress == device.macAddress }) {
                                    currentList.add(device)
                                    scanResultsFlow.value = currentList
                                    emit(DeviceOperationResult.Success(currentList))
                                }
                            },
                            onScanFinished = { devices ->
                                if (continuation.isActive) {
                                    continuation.resume(devices)
                                }
                            },
                            onScanFailed = { error ->
                                if (continuation.isActive) {
                                    continuation.resumeWithException(Exception(error))
                                }
                            },
                            timeoutSeconds = timeoutSeconds
                        )
                        
                        continuation.invokeOnCancellation {
                            veepooManager.stopScan()
                        }
                    }
                    
                    // Update scan results one last time
                    val devices = scanResults.map { mapVeepooDeviceInfoToDevice(it) }
                    scanResultsFlow.value = devices
                    emit(DeviceOperationResult.Success(devices))
                    
                } catch (e: Exception) {
                    Timber.e(e, "Error scanning for devices")
                    emit(DeviceOperationResult.Error(
                        DeviceError.UnknownError(
                            "Error scanning for devices: ${e.message}",
                            e
                        )
                    ))
                } finally {
                    deviceScanStateFlow.value = false
                }
            }
            
            // Set a timeout
            delay(TimeUnit.SECONDS.toMillis(timeoutSeconds.toLong()))
            stopDeviceScan()
            
        } catch (e: Exception) {
            Timber.e(e, "Error starting device scan")
            emit(DeviceOperationResult.Error(
                DeviceError.UnknownError(
                    "Error starting device scan: ${e.message}",
                    e
                )
            ))
            deviceScanStateFlow.value = false
        }
    }.flowOn(ioDispatcher)

    override suspend fun stopDeviceScan() {
        try {
            scanJob?.cancel()
            veepooManager.stopScan()
            deviceScanStateFlow.value = false
        } catch (e: Exception) {
            Timber.e(e, "Error stopping device scan: ${e.message}")
        }
    }

    override fun isScanning(): Flow<Boolean> = deviceScanStateFlow.asStateFlow()

    override suspend fun pairDevice(device: Device): DeviceOperationResult<Device> {
        return withContext(ioDispatcher) {
            try {
                if (!isBluetoothEnabled()) {
                    return@withContext DeviceOperationResult.Error(DeviceError.BluetoothDisabledError())
                }
                
                if (!hasBluetoothPermissions()) {
                    return@withContext DeviceOperationResult.Error(
                        DeviceError.PermissionError(
                            "Bluetooth permissions not granted",
                            "android.permission.BLUETOOTH_SCAN, android.permission.BLUETOOTH_CONNECT"
                        )
                    )
                }
                
                // Try to connect to the device first to verify it's available
                val connectionResult = connectToVeepooDevice(device.macAddress)
                if (connectionResult !is DeviceOperationResult.Success) {
                    return@withContext DeviceOperationResult.Error(
                        DeviceError.PairingError(
                            "Failed to connect to device during pairing",
                            (connectionResult as? DeviceOperationResult.Error)?.error as? Exception
                        )
                    )
                }
                
                // Get device info from VeepooSDK
                val deviceInfo = veepooManager.getDeviceInfo()
                    ?: return@withContext DeviceOperationResult.Error(
                        DeviceError.PairingError("Failed to get device info")
                    )
                
                // Create or update device in database
                val deviceEntity = device.toEntity().copy(
                    isPaired = true,
                    pairingTime = System.currentTimeMillis(),
                    lastConnected = System.currentTimeMillis(),
                    isActive = true,
                    firmwareVersion = deviceInfo.firmwareVersion ?: device.firmwareVersion,
                    hardwareVersion = deviceInfo.hardwareVersion ?: device.hardwareVersion,
                    features = getDeviceFeaturesFromInfo(deviceInfo).joinToString(",") { it.name },
                    syncStatus = SyncStatus.NOT_SYNCED
                )
                
                val deviceId = deviceDao.upsertDevice(deviceEntity)
                
                // Create default settings for the device
                createDefaultDeviceSettings(deviceId)
                
                // Get the updated device from the database
                val updatedDevice = deviceDao.getDeviceById(deviceId)?.toDomain()
                    ?: return@withContext DeviceOperationResult.Error(
                        DeviceError.DeviceNotFoundError(deviceId)
                    )
                
                // Disconnect after pairing to save battery
                disconnectFromVeepooDevice()
                
                DeviceOperationResult.Success(updatedDevice)
                
            } catch (e: Exception) {
                Timber.e(e, "Error pairing device: ${e.message}")
                DeviceOperationResult.Error(
                    DeviceError.PairingError(
                        "Error pairing device: ${e.message}",
                        e
                    )
                )
            }
        }
    }

    override suspend fun unpairDevice(deviceId: Long): DeviceOperationResult<Boolean> {
        return withContext(ioDispatcher) {
            try {
                val device = deviceDao.getDeviceById(deviceId)
                    ?: return@withContext DeviceOperationResult.Error(
                        DeviceError.DeviceNotFoundError(deviceId)
                    )
                
                // Disconnect if connected
                if (getDeviceConnectionState(deviceId).first() == DeviceConnectionState.CONNECTED) {
                    disconnectFromDevice(deviceId)
                }
                
                // Update device in database
                val updatedDevice = device.copy(
                    isPaired = false,
                    pairingTime = null,
                    isActive = false,
                    updatedAt = System.currentTimeMillis()
                )
                
                deviceDao.update(updatedDevice)
                
                // Clear device settings cache
                deviceSettingsCache.remove(deviceId)
                
                DeviceOperationResult.Success(true)
                
            } catch (e: Exception) {
                Timber.e(e, "Error unpairing device: ${e.message}")
                DeviceOperationResult.Error(
                    DeviceError.UnknownError(
                        "Error unpairing device: ${e.message}",
                        e
                    )
                )
            }
        }
    }

    override fun getPairedDevices(): Flow<List<Device>> {
        return deviceDao.getPairedDevicesFlow().map { it.toDomainList() }
    }

    override fun isDevicePaired(deviceId: Long): Flow<Boolean> {
        return deviceDao.getDeviceByIdFlow(deviceId).map { it?.isPaired ?: false }
    }

    /**
     * ====================================
     * Connection Management
     * ====================================
     */

    override suspend fun connectToDevice(deviceId: Long): DeviceOperationResult<Device> {
        return withContext(ioDispatcher) {
            try {
                val device = deviceDao.getDeviceById(deviceId)?.toDomain()
                    ?: return@withContext DeviceOperationResult.Error(
                        DeviceError.DeviceNotFoundError(deviceId)
                    )
                
                if (!isBluetoothEnabled()) {
                    return@withContext DeviceOperationResult.Error(DeviceError.BluetoothDisabledError())
                }
                
                if (!hasBluetoothPermissions()) {
                    return@withContext DeviceOperationResult.Error(
                        DeviceError.PermissionError(
                            "Bluetooth permissions not granted",
                            "android.permission.BLUETOOTH_CONNECT"
                        )
                    )
                }
                
                // Get or create connection state flow for this device
                val connectionStateFlow = deviceConnectionStateMap.getOrPut(deviceId) {
                    MutableStateFlow(DeviceConnectionState.DISCONNECTED)
                }
                
                // Update connection state
                connectionStateFlow.value = DeviceConnectionState.CONNECTING
                
                // Connect to the device
                val connectionResult = connectToVeepooDevice(device.macAddress)
                if (connectionResult !is DeviceOperationResult.Success) {
                    connectionStateFlow.value = DeviceConnectionState.CONNECTION_FAILED
                    return@withContext connectionResult
                }
                
                // Update connection state
                connectionStateFlow.value = DeviceConnectionState.CONNECTED
                
                // Update device in database
                val batteryLevel = veepooManager.getBatteryLevel()
                deviceDao.updateDeviceConnection(
                    deviceId = deviceId,
                    batteryLevel = batteryLevel,
                    timestamp = System.currentTimeMillis()
                )
                
                // Get the updated device from the database
                val updatedDevice = deviceDao.getDeviceById(deviceId)?.toDomain()
                    ?: return@withContext DeviceOperationResult.Error(
                        DeviceError.DeviceNotFoundError(deviceId)
                    )
                
                DeviceOperationResult.Success(updatedDevice)
                
            } catch (e: Exception) {
                Timber.e(e, "Error connecting to device: ${e.message}")
                // Update connection state
                deviceConnectionStateMap[deviceId]?.value = DeviceConnectionState.CONNECTION_FAILED
                
                DeviceOperationResult.Error(
                    DeviceError.ConnectionError(
                        "Error connecting to device: ${e.message}",
                        e
                    )
                )
            }
        }
    }

    override suspend fun disconnectFromDevice(deviceId: Long): DeviceOperationResult<Boolean> {
        return withContext(ioDispatcher) {
            try {
                val device = deviceDao.getDeviceById(deviceId)
                    ?: return@withContext DeviceOperationResult.Error(
                        DeviceError.DeviceNotFoundError(deviceId)
                    )
                
                // Get connection state flow for this device
                val connectionStateFlow = deviceConnectionStateMap.getOrPut(deviceId) {
                    MutableStateFlow(DeviceConnectionState.DISCONNECTED)
                }
                
                // If already disconnected, return success
                if (connectionStateFlow.value == DeviceConnectionState.DISCONNECTED) {
                    return@withContext DeviceOperationResult.Success(true)
                }
                
                // Update connection state
                connectionStateFlow.value = DeviceConnectionState.DISCONNECTING
                
                // Disconnect from the device
                disconnectFromVeepooDevice()
                
                // Update connection state
                connectionStateFlow.value = DeviceConnectionState.DISCONNECTED
                
                DeviceOperationResult.Success(true)
                
            } catch (e: Exception) {
                Timber.e(e, "Error disconnecting from device: ${e.message}")
                DeviceOperationResult.Error(
                    DeviceError.ConnectionError(
                        "Error disconnecting from device: ${e.message}",
                        e
                    )
                )
            }
        }
    }

    override fun getDeviceConnectionState(deviceId: Long): Flow<DeviceConnectionState> {
        return deviceConnectionStateMap.getOrPut(deviceId) {
            MutableStateFlow(DeviceConnectionState.DISCONNECTED)
        }.asStateFlow()
    }

    override fun getConnectedDevices(): Flow<List<Device>> {
        return flow {
            val connectedDeviceIds = deviceConnectionStateMap
                .filter { it.value.value == DeviceConnectionState.CONNECTED }
                .map { it.key }
            
            if (connectedDeviceIds.isEmpty()) {
                emit(emptyList())
                return@flow
            }
            
            val devices = deviceDao.getAllDevices()
                .filter { it.id in connectedDeviceIds }
                .toDomainList()
            
            emit(devices)
        }.flowOn(ioDispatcher)
    }

    override fun isDeviceConnected(deviceId: Long): Flow<Boolean> {
        return getDeviceConnectionState(deviceId).map { it == DeviceConnectionState.CONNECTED }
    }

    override fun getLastConnectedTime(deviceId: Long): Flow<LocalDateTime?> {
        return deviceDao.getDeviceByIdFlow(deviceId).map { device ->
            device?.lastConnected?.let { timestamp ->
                LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(timestamp),
                    ZoneId.systemDefault()
                )
            }
        }
    }

    /**
     * ====================================
     * Device Feature Management
     * ====================================
     */

    override fun getDeviceFeatures(deviceId: Long): Flow<Set<DeviceFeature>> {
        return deviceDao.getDeviceByIdFlow(deviceId).map { device ->
            device?.features
                ?.split(",")
                ?.filter { it.isNotBlank() }
                ?.mapNotNull { DeviceFeature.fromString(it) }
                ?.toSet()
                ?: emptySet()
        }
    }

    override fun deviceSupportsFeature(deviceId: Long, feature: DeviceFeature): Flow<Boolean> {
        return getDeviceFeatures(deviceId).map { features -> feature in features }
    }

    override fun getDevicesWithFeature(feature: DeviceFeature): Flow<List<Device>> {
        return deviceDao.getDevicesWithFeatureFlow(feature.name).map { it.toDomainList() }
    }

    override suspend fun updateDeviceFeatures(deviceId: Long, features: Set<DeviceFeature>): DeviceOperationResult<Device> {
        return withContext(ioDispatcher) {
            try {
                val device = deviceDao.getDeviceById(deviceId)
                    ?: return@withContext DeviceOperationResult.Error(
                        DeviceError.DeviceNotFoundError(deviceId)
                    )
                
                val featuresString = features.joinToString(",") { it.name }
                
                deviceDao.updateDeviceFeatures(
                    deviceId = deviceId,
                    features = featuresString
                )
                
                val updatedDevice = deviceDao.getDeviceById(deviceId)?.toDomain()
                    ?: return@withContext DeviceOperationResult.Error(
                        DeviceError.DeviceNotFoundError(deviceId)
                    )
                
                DeviceOperationResult.Success(updatedDevice)
                
            } catch (e: Exception) {
                Timber.e(e, "Error updating device features: ${e.message}")
                DeviceOperationResult.Error(
                    DeviceError.UnknownError(
                        "Error updating device features: ${e.message}",
                        e
                    )
                )
            }
        }
    }

    /**
     * ====================================
     * Battery and Status Monitoring
     * ====================================
     */

    override fun getDeviceBatteryLevel(deviceId: Long): Flow<Int?> {
        return deviceDao.getDeviceByIdFlow(deviceId).map { it?.batteryLevel }
    }

    override fun getLowBatteryDevices(threshold: Int): Flow<List<Device>> {
        return deviceDao.getLowBatteryDevicesFlow().map { it.toDomainList() }
    }

    override suspend fun updateDeviceBatteryLevel(deviceId: Long, batteryLevel: Int): DeviceOperationResult<Device> {
        return withContext(ioDispatcher) {
            try {
                val device = deviceDao.getDeviceById(deviceId)
                    ?: return@withContext DeviceOperationResult.Error(
                        DeviceError.DeviceNotFoundError(deviceId)
                    )
                
                deviceDao.updateDeviceBattery(
                    deviceId = deviceId,
                    batteryLevel = batteryLevel
                )
                
                val updatedDevice = deviceDao.getDeviceById(deviceId)?.toDomain()
                    ?: return@withContext DeviceOperationResult.Error(
                        DeviceError.DeviceNotFoundError(deviceId)
                    )
                
                DeviceOperationResult.Success(updatedDevice)
                
            } catch (e: Exception) {
                Timber.e(e, "Error updating device battery level: ${e.message}")
                DeviceOperationResult.Error(
                    DeviceError.UnknownError(
                        "Error updating device battery level: ${e.message}",
                        e
                    )
                )
            }
        }
    }

    override fun getDeviceFirmwareVersion(deviceId: Long): Flow<String> {
        return deviceDao.getDeviceByIdFlow(deviceId).map { it?.firmwareVersion ?: "Unknown" }
    }

    override fun getDeviceHardwareVersion(deviceId: Long): Flow<String> {
        return deviceDao.getDeviceByIdFlow(deviceId).map { it?.hardwareVersion ?: "Unknown" }
    }

    /**
     * ====================================
     * Sync Operations
     * ====================================
     */

    override suspend fun syncDeviceData(deviceId: Long): DeviceOperationResult<SyncResult> {
        return withContext(ioDispatcher) {
            try {
                val device = deviceDao.getDeviceById(deviceId)?.toDomain()
                    ?: return@withContext DeviceOperationResult.Error(
                        DeviceError.DeviceNotFoundError(deviceId)
                    )
                
                // Check if device is connected
                if (getDeviceConnectionState(deviceId).first() != DeviceConnectionState.CONNECTED) {
                    // Try to connect to the device
                    val connectionResult = connectToDevice(deviceId)
                    if (connectionResult !is DeviceOperationResult.Success) {
                        return@withContext DeviceOperationResult.Error(
                            DeviceError.SyncError(
                                "Device not connected and connection failed",
                                DeviceDataType.ALL
                            )
                        )
                    }
                }
                
                // Update device sync status
                deviceDao.updateDeviceSyncStatus(
                    deviceId = deviceId,
                    syncStatus = SyncStatus.SYNCING
                )
                
                // Start sync for all data types
                val syncResults = mutableMapOf<DeviceDataType, Int>()
                val syncedDataTypes = mutableSetOf<DeviceDataType>()
                var errorMessage: String? = null
                
                // Sync heart rate data
                if (device.features.contains(DeviceFeature.HEART_RATE)) {
                    try {
                        val heartRateCount = veepooManager.syncHeartRateData(deviceId)
                        syncResults[DeviceDataType.HEART_RATE] = heartRateCount
                        syncedDataTypes.add(DeviceDataType.HEART_RATE)
                    } catch (e: Exception) {
                        Timber.e(e, "Error syncing heart rate data: ${e.message}")
                        errorMessage = "Error syncing heart rate data: ${e.message}"
                    }
                }
                
                // Sync blood oxygen data
                if (device.features.contains(DeviceFeature.BLOOD_OXYGEN)) {
                    try {
                        val bloodOxygenCount = veepooManager.syncBloodOxygenData(deviceId)
                        syncResults[DeviceDataType.BLOOD_OXYGEN] = bloodOxygenCount
                        syncedDataTypes.add(DeviceDataType.BLOOD_OXYGEN)
                    } catch (e: Exception) {
                        Timber.e(e, "Error syncing blood oxygen data: ${e.message}")
                        errorMessage = "Error syncing blood oxygen data: ${e.message}"
                    }
                }
                
                // Sync blood pressure data
                if (device.features.contains(DeviceFeature.BLOOD_PRESSURE)) {
                    try {
                        val bloodPressureCount = veepooManager.syncBloodPressureData(deviceId)
                        syncResults[DeviceDataType.BLOOD_PRESSURE] = bloodPressureCount
                        syncedDataTypes.add(DeviceDataType.BLOOD_PRESSURE)
                    } catch (e: Exception) {
                        Timber.e(e, "Error syncing blood pressure data: ${e.message}")
                        errorMessage = "Error syncing blood pressure data: ${e.message}"
                    }
                }
                
                // Sync step data
                if (device.features.contains(DeviceFeature.STEPS)) {
                    try {
                        val stepCount = veepooManager.syncStepData(deviceId)
                        syncResults[DeviceDataType.STEPS] = stepCount
                        syncedDataTypes.add(DeviceDataType.STEPS)
                    } catch (e: Exception) {
                        Timber.e(e, "Error syncing step data: ${e.message}")
                        errorMessage = "Error syncing step data: ${e.message}"
                    }
                }
                
                // Sync sleep data
                if (device.features.contains(DeviceFeature.SLEEP)) {
                    try {
                        val sleepCount = veepooManager.syncSleepData(deviceId)
                        syncResults[DeviceDataType.SLEEP] = sleepCount
                        syncedDataTypes.add(DeviceDataType.SLEEP)
                    } catch (e: Exception) {
                        Timber.e(e, "Error syncing sleep data: ${e.message}")
                        errorMessage = "Error syncing sleep data: ${e.message}"
                    }
                }
                
                // Sync activity data
                if (device.features.contains(DeviceFeature.ACTIVITY_TRACKING)) {
                    try {
                        val activityCount = veepooManager.syncActivityData(deviceId)
                        syncResults[DeviceDataType.ACTIVITY] = activityCount
                        syncedDataTypes.add(DeviceDataType.ACTIVITY)
                    } catch (e: Exception) {
                        Timber.e(e, "Error syncing activity data: ${e.message}")
                        errorMessage = "Error syncing activity data: ${e.message}"
                    }
                }
                
                // Sync temperature data if supported
                if (device.features.contains(DeviceFeature.TEMPERATURE)) {
                    try {
                        val temperatureCount = veepooManager.syncTemperatureData(deviceId)
                        syncResults[DeviceDataType.TEMPERATURE] = temperatureCount
                        syncedDataTypes.add(DeviceDataType.TEMPERATURE)
                    } catch (e: Exception) {
                        Timber.e(e, "Error syncing temperature data: ${e.message}")
                        errorMessage = "Error syncing temperature data: ${e.message}"
                    }
                }
                
                // Update device sync status
                val syncStatus = if (syncedDataTypes.isNotEmpty()) {
                    if (syncedDataTypes.size == DeviceDataType.values().size - 1) { // -1 for ALL
                        SyncStatus.SYNCED
                    } else {
                        SyncStatus.PARTIALLY_SYNCED
                    }
                } else {
                    SyncStatus.FAILED
                }
                
                deviceDao.updateDeviceSyncStatus(
                    deviceId = deviceId,
                    syncStatus = syncStatus
                )
                
                // Create sync result
                val syncResult = SyncResult(
                    deviceId = deviceId,
                    syncedDataTypes = syncedDataTypes,
                    syncTime = LocalDateTime.now(),
                    itemsSynced = syncResults,
                    syncStatus = syncStatus,
                    errorMessage = errorMessage
                )
                
                DeviceOperationResult.Success(syncResult)
                
            } catch (e: Exception) {
                Timber.e(e, "Error syncing device data: ${e.message}")
                
                // Update device sync status
                deviceDao.updateDeviceSyncStatus(
                    deviceId = deviceId,
                    syncStatus = SyncStatus.FAILED
                )
                
                DeviceOperationResult.Error(
                    DeviceError.SyncError(
                        "Error syncing device data: ${e.message}",
                        DeviceDataType.ALL,
                        e
                    )
                )
            }
        }
    }

    override suspend fun syncDeviceDataType(deviceId: Long, dataType: DeviceDataType): DeviceOperationResult<SyncResult> {
        return withContext(ioDispatcher) {
            try {
                val device = deviceDao.getDeviceById(deviceId)?.toDomain()
                    ?: return@withContext DeviceOperationResult.Error(
                        DeviceError.DeviceNotFoundError(deviceId)
                    )
                
                // Check if device is connected
                if (getDeviceConnectionState(deviceId).first() != DeviceConnectionState.CONNECTED) {
                    // Try to connect to the device
                    val connectionResult = connectToDevice(deviceId)
                    if (connectionResult !is DeviceOperationResult.Success) {
                        return@withContext DeviceOperationResult.Error(
                            DeviceError.SyncError(
                                "Device not connected and connection failed",
                                dataType
                            )
                        )
                    }
                }
                
                // Sync specific data type
                val syncResults = mutableMapOf<DeviceDataType, Int>()
                val syncedDataTypes = mutableSetOf<DeviceDataType>()
                var errorMessage: String? = null
                
                when (dataType) {
                    DeviceDataType.HEART_RATE -> {
                        if (device.features.contains(DeviceFeature.HEART_RATE)) {
                            try {
                                val heartRateCount = veepooManager.syncHeartRateData(deviceId)
                                syncResults[DeviceDataType.HEART_RATE] = heartRateCount
                                syncedDataTypes.add(DeviceDataType.HEART_RATE)
                            } catch (e: Exception) {
                                Timber.e(e, "Error syncing heart rate data: ${e.message}")
                                errorMessage = "Error syncing heart rate data: ${e.message}"
                            }
                        } else {
                            errorMessage = "Device does not support heart rate monitoring"
                        }
                    }
                    DeviceDataType.BLOOD_OXYGEN -> {
                        if (device.features.contains(DeviceFeature.BLOOD_OXYGEN)) {
                            try {
                                val bloodOxygenCount = veepooManager.syncBloodOxygenData(deviceId)
                                syncResults[DeviceDataType.BLOOD_OXYGEN] = bloodOxygenCount
                                syncedDataTypes.add(DeviceDataType.BLOOD_OXYGEN)
                            } catch (e: Exception) {
                                Timber.e(e, "Error syncing blood oxygen data: ${e.message}")
                                errorMessage = "Error syncing blood oxygen data: ${e.message}"
                            }
                        } else {
                            errorMessage = "Device does not support blood oxygen monitoring"
                        }
                    }
                    DeviceDataType.BLOOD_PRESSURE -> {
                        if (device.features.contains(DeviceFeature.BLOOD_PRESSURE)) {
                            try {
                                val bloodPressureCount = veepooManager.syncBloodPressureData(deviceId)
                                syncResults[DeviceDataType.BLOOD_PRESSURE] = bloodPressureCount
                                syncedDataTypes.add(DeviceDataType.BLOOD_PRESSURE)
                            } catch (e: Exception) {
                                Timber.e(e, "Error syncing blood pressure data: ${e.message}")
                                errorMessage = "Error syncing blood pressure data: ${e.message}"
                            }
                        } else {
                            errorMessage = "Device does not support blood pressure monitoring"
                        }
                    }
                    DeviceDataType.STEPS -> {
                        if (device.features.contains(DeviceFeature.STEPS)) {
                            try {
                                val stepCount = veepooManager.syncStepData(deviceId)
                                syncResults[DeviceDataType.STEPS] = stepCount
                                syncedDataTypes.add(DeviceDataType.STEPS)
                            } catch (e: Exception) {
                                Timber.e(e, "Error syncing step data: ${e.message}")
                                errorMessage = "Error syncing step data: ${e.message}"
                            }
                        } else {
                            errorMessage = "Device does not support step tracking"
                        }
                    }
                    DeviceDataType.SLEEP -> {
                        if (device.features.contains(DeviceFeature.SLEEP)) {
                            try {
                                val sleepCount = veepooManager.syncSleepData(deviceId)
                                syncResults[DeviceDataType.SLEEP] = sleepCount
                                syncedDataTypes.add(DeviceDataType.SLEEP)
                            } catch (e: Exception) {
                                Timber.e(e, "Error syncing sleep data: ${e.message}")
                                errorMessage = "Error syncing sleep data: ${e.message}"
                            }
                        } else {
                            errorMessage = "Device does not support sleep tracking"
                        }
                    }
                    DeviceDataType.ACTIVITY -> {
                        if (device.features.contains(DeviceFeature.ACTIVITY_TRACKING)) {
                            try {
                                val activityCount = veepooManager.syncActivityData(deviceId)
                                syncResults[DeviceDataType.ACTIVITY] = activityCount
                                syncedDataTypes.add(DeviceDataType.ACTIVITY)
                            } catch (e: Exception) {
                                Timber.e(e, "Error syncing activity data: ${e.message}")
                                errorMessage = "Error syncing activity data: ${e.message}"
                            }
                        } else {
                            errorMessage = "Device does not support activity tracking"
                        }
                    }
                    DeviceDataType.TEMPERATURE -> {
                        if (device.features.contains(DeviceFeature.TEMPERATURE)) {
                            try {
                                val temperatureCount = veepooManager.syncTemperatureData(deviceId)
                                syncResults[DeviceDataType.TEMPERATURE] = temperatureCount
                                syncedDataTypes.add(DeviceDataType.TEMPERATURE)
                            } catch (e: Exception) {
                                Timber.e(e, "Error syncing temperature data: ${e.message}")
                                errorMessage = "Error syncing temperature data: ${e.message}"
                            }
                        } else {
                            errorMessage = "Device does not support temperature monitoring"
                        }
                    }
                    DeviceDataType.ALL -> {
                        // Handle ALL by calling the full sync method
                        return@withContext syncDeviceData(deviceId)
                    }
                }
                
                // Create sync result
                val syncResult = SyncResult(
                    deviceId = deviceId,
                    syncedDataTypes = syncedDataTypes,
                    syncTime = LocalDateTime.now(),
                    itemsSynced = syncResults,
                    syncStatus = if (syncedDataTypes.isNotEmpty()) SyncStatus.SYNCED else SyncStatus.FAILED,
                    errorMessage = errorMessage
                )
                
                DeviceOperationResult.Success(syncResult)
                
            } catch (e: Exception) {
                Timber.e(e, "Error syncing device data type: ${e.message}")
                DeviceOperationResult.Error(
                    DeviceError.SyncError(
                        "Error syncing device data type: ${e.message}",
                        dataType,
                        e
                    )
                )
            }
        }
    }

    override fun getDeviceSyncStatus(deviceId: Long): Flow<SyncStatus> {
        return deviceDao.getDeviceByIdFlow(deviceId).map { device ->
            device?.syncStatus ?: SyncStatus.NOT_SYNCED
        }
    }

    override fun getLastSyncTime(deviceId: Long, dataType: DeviceDataType?): Flow<LocalDateTime?> {
        return flow {
            val device = deviceDao.getDeviceById(deviceId) ?: return@flow emit(null)
            
            val lastSyncTime = when (dataType) {
                DeviceDataType.HEART_RATE -> {
                    // Get last sync time for heart rate data
                    // This would typically come from a health data repository
                    // For simplicity, we'll just use the device's updatedAt time
                    device.updatedAt
                }
                DeviceDataType.BLOOD_OXYGEN -> {
                    // Get last sync time for blood oxygen data
                    device.updatedAt
                }
                DeviceDataType.BLOOD_PRESSURE -> {
                    // Get last sync time for blood pressure data
                    device.updatedAt
                }
                DeviceDataType.STEPS -> {
                    // Get last sync time for step data
                    device.updatedAt
                }
                DeviceDataType.SLEEP -> {
                    // Get last sync time for sleep data
                    device.updatedAt
                }
                DeviceDataType.ACTIVITY -> {
                    // Get last sync time for activity data
                    device.updatedAt
                }
                DeviceDataType.TEMPERATURE -> {
                    // Get last sync time for temperature data
                    device.updatedAt
                }
                DeviceDataType.ALL, null -> {
                    // Get last sync time for all data
                    device.updatedAt
                }
            }
            
            emit(
                lastSyncTime?.let {
                    LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(it),
                        ZoneId.systemDefault()
                    )
                }
            )
        }.flowOn(ioDispatcher)
    }

    override fun getDevicesNeedingSync(): Flow<List<Device>> {
        return deviceDao.getDevicesNeedingSyncFlow().map { it.toDomainList() }
    }

    /**
     * ====================================
     * Device Configuration
     * ====================================
     */

    override fun getDeviceSettings(deviceId: Long): Flow<DeviceSettings> {
        return flow {
            // Check cache first
            val cachedSettings = deviceSettingsCache[deviceId]
            if (cachedSettings != null) {
                emit(cachedSettings)
                return@flow
            }
            
            // Load settings from database
            val settings = settingDao.getSettingsForDevice(deviceId)
            
            // Convert to DeviceSettings
            val deviceSettings = mapSettingsToDeviceSettings(settings)
            
            // Update cache
            deviceSettingsCache[deviceId] = deviceSettings
            
            emit(deviceSettings)
        }.flowOn(ioDispatcher)
    }

    override suspend fun updateDeviceSettings(deviceId: Long, settings: DeviceSettings): DeviceOperationResult<DeviceSettings> {
        return withContext(ioDispatcher) {
            try {
                val device = deviceDao.getDeviceById(deviceId)
                    ?: return@withContext DeviceOperationResult.Error(
                        DeviceError.DeviceNotFoundError(deviceId)
                    )
                
                // Check if device is connected
                if (getDeviceConnectionState(deviceId).first() != DeviceConnectionState.CONNECTED) {
                    // Try to connect to the device
                    val connectionResult = connectToDevice(deviceId)
                    if (connectionResult !is DeviceOperationResult.Success) {
                        return@withContext DeviceOperationResult.Error(
                            DeviceError.ConnectionError(
                                "Device not connected and connection failed"
                            )
                        )
                    }
                }
                
                // Apply settings to the device
                try {
                    applySettingsToDevice(deviceId, settings)
                } catch (e: Exception) {
                    Timber.e(e, "Error applying settings to device: ${e.message}")
                    return@withContext DeviceOperationResult.Error(
                        DeviceError.UnknownError(
                            "Error applying settings to device: ${e.message}",
                            e
                        )
                    )
                }
                
                // Save settings to database
                val settingEntities = mapDeviceSettingsToEntities(deviceId, settings)
                settingDao.upsertSettings(settingEntities)
                
                // Update cache
                deviceSettingsCache[deviceId] = settings
                
                DeviceOperationResult.Success(settings)
                
            } catch (e: Exception) {
                Timber.e(e, "Error updating device settings: ${e.message}")
                DeviceOperationResult.Error(
                    DeviceError.UnknownError(
                        "Error updating device settings: ${e.message}",
                        e
                    )
                )
            }
        }
    }

    override suspend fun setPreferredDevice(deviceId: Long): DeviceOperationResult<Device> {
        return withContext(ioDispatcher) {
            try {
                val device = deviceDao.getDeviceById(deviceId)
                    ?: return@withContext DeviceOperationResult.Error(
                        DeviceError.DeviceNotFoundError(deviceId)
                    )
                
                // Save preferred device ID to settings
                val settingEntity = SettingEntity(
                    id = 0,
                    key = "preferred_device_id",
                    value = deviceId.toString(),
                    category = "app",
                    deviceId = null,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    syncStatus = SyncStatus.NOT_SYNCED
                )
                
                settingDao.upsertSetting(settingEntity)
                
                DeviceOperationResult.Success(device.toDomain())
                
            } catch (e: Exception) {
                Timber.e(e, "Error setting preferred device: ${e.message}")
                DeviceOperationResult.Error(
                    DeviceError.UnknownError(
                        "Error setting preferred device: ${e.message}",
                        e
                    )
                )
            }
        }
    }

    override fun getPreferredDevice(): Flow<Device?> {
        return flow {
            val preferredDeviceIdSetting = settingDao.getSettingByKey("preferred_device_id")
            
            if (preferredDeviceIdSetting == null) {
                emit(null)
                return@flow
            }
            
            val preferredDeviceId = preferredDeviceIdSetting.value.toLongOrNull()
                ?: return@flow emit(null)
            
            val device = deviceDao.getDeviceById(preferredDeviceId)
            emit(device?.toDomain())
        }.flowOn(ioDispatcher)
    }

    /**
     * ====================================
     * Device Management
     * ====================================
     */

    override fun getDevice(deviceId: Long): Flow<Device?> {
        return deviceDao.getDeviceByIdFlow(deviceId).map { it?.toDomain() }
    }

    override fun getAllDevices(): Flow<List<Device>> {
        return deviceDao.getAllDevicesFlow().map { it.toDomainList() }
    }

    override fun getActiveDevices(): Flow<List<Device>> {
        return deviceDao.getActiveDevicesFlow().map { it.toDomainList() }
    }

    override suspend fun setDeviceActive(deviceId: Long, isActive: Boolean): DeviceOperationResult<Device> {
        return withContext(ioDispatcher) {
            try {
                val device = deviceDao.getDeviceById(deviceId)
                    ?: return@withContext DeviceOperationResult.Error(
                        DeviceError.DeviceNotFoundError(deviceId)
                    )
                
                deviceDao.setDeviceActive(
                    deviceId = deviceId,
                    isActive = isActive
                )
                
                val updatedDevice = deviceDao.getDeviceById(deviceId)?.toDomain()
                    ?: return@withContext DeviceOperationResult.Error(
                        DeviceError.DeviceNotFoundError(deviceId)
                    )
                
                DeviceOperationResult.Success(updatedDevice)
                
            } catch (e: Exception) {
                Timber.e(e, "Error setting device active state: ${e.message}")
                DeviceOperationResult.Error(
                    DeviceError.UnknownError(
                        "Error setting device active state: ${e.message}",
                        e
                    )
                )
            }
        }
    }

    override suspend fun updateDevice(device: Device): DeviceOperationResult<Device> {
        return withContext(ioDispatcher) {
            try {
                val existingDevice = deviceDao.getDeviceById(device.id)
                    ?: return@withContext DeviceOperationResult.Error(
                        DeviceError.DeviceNotFoundError(device.id)
                    )
                
                val deviceEntity = device.toEntity()
                deviceDao.update(deviceEntity)
                
                val updatedDevice = deviceDao.getDeviceById(device.id)?.toDomain()
                    ?: return@withContext DeviceOperationResult.Error(
                        DeviceError.DeviceNotFoundError(device.id)
                    )
                
                DeviceOperationResult.Success(updatedDevice)
                
            } catch (e: Exception) {
                Timber.e(e, "Error updating device: ${e.message}")
                DeviceOperationResult.Error(
                    DeviceError.UnknownError(
                        "Error updating device: ${e.message}",
                        e
                    )
                )
            }
        }
    }

    override suspend fun deleteDevice(deviceId: Long): DeviceOperationResult<Boolean> {
        return withContext(ioDispatcher) {
            try {
                val device = deviceDao.getDeviceById(deviceId)
                    ?: return@withContext DeviceOperationResult.Error(
                        DeviceError.DeviceNotFoundError(deviceId)
                    )
                
                // Disconnect if connected
                if (getDeviceConnectionState(deviceId).first() == DeviceConnectionState.CONNECTED) {
                    disconnectFromDevice(deviceId)
                }
                
                // Delete device from database
                deviceDao.deleteById(deviceId)
                
                // Clear device settings cache
                deviceSettingsCache.remove(deviceId)
                
                // Clear connection state
                deviceConnectionStateMap.remove(deviceId)
                
                DeviceOperationResult.Success(true)
                
            } catch (e: Exception) {
                Timber.e(e, "Error deleting device: ${e.message}")
                DeviceOperationResult.Error(
                    DeviceError.UnknownError(
                        "Error deleting device: ${e.message}",
                        e
                    )
                )
            }
        }
    }

    override fun searchDevices(query: String): Flow<List<Device>> {
        return deviceDao.searchDevicesFlow(query).map { it.toDomainList() }
    }

    /**
     * ====================================
     * Private Helper Methods
     * ====================================
     */

    /**
     * Check if Bluetooth is enabled
     */
    private fun isBluetoothEnabled(): Boolean {
        val bluetoothAdapter = bluetoothManager.adapter
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled
    }

    /**
     * Check if the app has the necessary Bluetooth permissions
     */
    private fun hasBluetoothPermissions(): Boolean {
        // In a real app, we would check for BLUETOOTH_SCAN and BLUETOOTH_CONNECT permissions
        // For simplicity, we'll assume permissions are granted
        return true
    }

    /**
     * Connect to a VeepooSDK device
     */
    private suspend fun connectToVeepooDevice(macAddress: String): DeviceOperationResult<Boolean> {
        return suspendCancellableCoroutine { continuation ->
            try {
                vpOperateManager.connectDevice(macAddress, object : IBleWriteResponse {
                    override fun onResponse(code: Int) {
                        if (code == 0) {
                            // Connection successful
                            if (continuation.isActive) {
                                continuation.resume(DeviceOperationResult.Success(true))
                            }
                        } else {
                            // Connection failed
                            if (continuation.isActive) {
                                continuation.resume(
                                    DeviceOperationResult.Error(
                                        DeviceError.ConnectionError(
                                            "Failed to connect to device: code $code"
                                        )
                                    )
                                )
                            }
                        }
                    }
                })
            } catch (e: Exception) {
                if (continuation.isActive) {
                    continuation.resume(
                        DeviceOperationResult.Error(
                            DeviceError.ConnectionError(
                                "Error connecting to device: ${e.message}",
                                e
                            )
                        )
                    )
                }
            }
            
            continuation.invokeOnCancellation {
                try {
                    vpOperateManager.disconnectDevice()
                } catch (e: Exception) {
                    Timber.e(e, "Error disconnecting device on cancellation: ${e.message}")
                }
            }
        }
    }

    /**
     * Disconnect from a VeepooSDK device
     */
    private fun disconnectFromVeepooDevice() {
        try {
            vpOperateManager.disconnectDevice()
        } catch (e: Exception) {
            Timber.e(e, "Error disconnecting from device: ${e.message}")
        }
    }

    /**
     * Map a VeepooDeviceInfo to a Device domain model
     */
    private fun mapVeepooDeviceInfoToDevice(deviceInfo: VeepooDeviceInfo): Device {
        return Device(
            id = 0,
            name = deviceInfo.name ?: "Unknown Device",
            macAddress = deviceInfo.address ?: "",
            deviceType = deviceInfo.type ?: "Unknown",
            firmwareVersion = deviceInfo.firmwareVersion ?: "Unknown",
            hardwareVersion = deviceInfo.hardwareVersion ?: "Unknown",
            lastConnected = null,
            batteryLevel = null,
            isActive = false,
            features = getDeviceFeaturesFromInfo(deviceInfo).toSet(),
            syncStatus = SyncStatus.NOT_SYNCED
        )
    }

    /**
     * Get device features from VeepooDeviceInfo
     */
    private fun getDeviceFeaturesFromInfo(deviceInfo: VeepooDeviceInfo): List<DeviceFeature> {
        val features = mutableListOf<DeviceFeature>()
        
        // Add basic features
        features.add(DeviceFeature.STEPS)
        
        // Add features based on device type
        val deviceType = deviceInfo.type?.lowercase() ?: ""
        
        if (deviceType.contains("hb") || deviceType.contains("band")) {
            features.add(DeviceFeature.HEART_RATE)
            features.add(DeviceFeature.SLEEP)
            features.add(DeviceFeature.ACTIVITY_TRACKING)
            features.add(DeviceFeature.NOTIFICATIONS)
        }
        
        // Add features based on firmware version
        val firmwareVersion = deviceInfo.firmwareVersion ?: ""
        
        if (firmwareVersion.isNotEmpty()) {
            // Newer firmware versions support more features
            if (firmwareVersion.compareTo("1.5", ignoreCase = true) >= 0) {
                features.add(DeviceFeature.BLOOD_OXYGEN)
            }
            
            if (firmwareVersion.compareTo("2.0", ignoreCase = true) >= 0) {
                features.add(DeviceFeature.BLOOD_PRESSURE)
            }
            
            if (firmwareVersion.compareTo("2.5", ignoreCase = true) >= 0) {
                features.add(DeviceFeature.TEMPERATURE)
            }
        }
        
        return features
    }

    /**
     * Create default settings for a device
     */
    private suspend fun createDefaultDeviceSettings(deviceId: Long) {
        val defaultSettings = listOf(
            SettingEntity(
                id = 0,
                key = "step_goal",
                value = "10000",
                deviceId = deviceId,
                category = "goals",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.NOT_SYNCED
            ),
            SettingEntity(
                id = 0,
                key = "heart_rate_interval",
                value = "30", // minutes
                deviceId = deviceId,
                category = "monitoring",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.NOT_SYNCED
            ),
            SettingEntity(
                id = 0,
                key = "notifications_enabled",
                value = "true",
                deviceId = deviceId,
                category = "notifications",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.NOT_SYNCED
            ),
            SettingEntity(
                id = 0,
                key = "wrist_lift_enabled",
                value = "true",
                deviceId = deviceId,
                category = "display",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.NOT_SYNCED
            ),
            SettingEntity(
                id = 0,
                key = "time_format",
                value = "24h",
                deviceId = deviceId,
                category = "display",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.NOT_SYNCED
            ),
            SettingEntity(
                id = 0,
                key = "language",
                value = "en",
                deviceId = deviceId,
                category = "system",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.NOT_SYNCED
            ),
            SettingEntity(
                id = 0,
                key = "auto_sync",
                value = "true",
                deviceId = deviceId,
                category = "sync",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.NOT_SYNCED
            ),
            SettingEntity(
                id = 0,
                key = "sync_interval",
                value = "60", // minutes
                deviceId = deviceId,
                category = "sync",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.NOT_SYNCED
            )
        )
        
        settingDao.insertAll(defaultSettings)
    }

    /**
     * Map settings entities to a DeviceSettings domain model
     */
    private fun mapSettingsToDeviceSettings(settings: List<SettingEntity>): DeviceSettings {
        val settingsMap = settings.associateBy { it.key }
        
        return DeviceSettings(
            stepGoal = settingsMap["step_goal"]?.value?.toIntOrNull() ?: 10000,
            heartRateInterval = settingsMap["heart_rate_interval"]?.value?.toIntOrNull() ?: 30,
            notificationsEnabled = settingsMap["notifications_enabled"]?.value?.toBooleanStrictOrNull() ?: true,
            wristLiftEnabled = settingsMap["wrist_lift_enabled"]?.value?.toBooleanStrictOrNull() ?: true,
            timeFormat = settingsMap["time_format"]?.value ?: "24h",
            language = settingsMap["language"]?.value ?: "en",
            autoSync = settingsMap["auto_sync"]?.value?.toBooleanStrictOrNull() ?: true,
            syncInterval = settingsMap["sync_interval"]?.value?.toIntOrNull() ?: 60,
            additionalSettings = settings
                .filter { it.key !in listOf("step_goal", "heart_rate_interval", "notifications_enabled", "wrist_lift_enabled", "time_format", "language", "auto_sync", "sync_interval") }
                .associate { it.key to it.value }
        )
    }

    /**
     * Map a DeviceSettings domain model to setting entities
     */
    private fun mapDeviceSettingsToEntities(deviceId: Long, settings: DeviceSettings): List<SettingEntity> {
        val now = System.currentTimeMillis()
        
        val entities = mutableListOf(
            SettingEntity(
                id = 0,
                key = "step_goal",
                value = settings.stepGoal.toString(),
                deviceId = deviceId,
                category = "goals",
                createdAt = now,
                updatedAt = now,
                syncStatus = SyncStatus.NOT_SYNCED
            ),
            SettingEntity(
                id = 0,
                key = "heart_rate_interval",
                value = settings.heartRateInterval.toString(),
                deviceId = deviceId,
                category = "monitoring",
                createdAt = now,
                updatedAt = now,
                syncStatus = SyncStatus.NOT_SYNCED
            ),
            SettingEntity(
                id = 0,
                key = "notifications_enabled",
                value = settings.notificationsEnabled.toString(),
                deviceId = deviceId,
                category = "notifications",
                createdAt = now,
                updatedAt = now,
                syncStatus = SyncStatus.NOT_SYNCED
            ),
            SettingEntity(
                id = 0,
                key = "wrist_lift_enabled",
                value = settings.wristLiftEnabled.toString(),
                deviceId = deviceId,
                category = "display",
                createdAt = now,
                updatedAt = now,
                syncStatus = SyncStatus.NOT_SYNCED
            ),
            SettingEntity(
                id = 0,
                key = "time_format",
                value = settings.timeFormat,
                deviceId = deviceId,
                category = "display",
                createdAt = now,
                updatedAt = now,
                syncStatus = SyncStatus.NOT_SYNCED
            ),
            SettingEntity(
                id = 0,
                key = "language",
                value = settings.language,
                deviceId = deviceId,
                category = "system",
                createdAt = now,
                updatedAt = now,
                syncStatus = SyncStatus.NOT_SYNCED
            ),
            SettingEntity(
                id = 0,
                key = "auto_sync",
                value = settings.autoSync.toString(),
                deviceId = deviceId,
                category = "sync",
                createdAt = now,
                updatedAt = now,
                syncStatus = SyncStatus.NOT_SYNCED
            ),
            SettingEntity(
                id = 0,
                key = "sync_interval",
                value = settings.syncInterval.toString(),
                deviceId = deviceId,
                category = "sync",
                createdAt = now,
                updatedAt = now,
                syncStatus = SyncStatus.NOT_SYNCED
            )
        )
        
        // Add additional settings
        settings.additionalSettings.forEach { (key, value) ->
            entities.add(
                SettingEntity(
                    id = 0,
                    key = key,
                    value = value,
                    deviceId = deviceId,
                    category = "custom",
                    createdAt = now,
                    updatedAt = now,
                    syncStatus = SyncStatus.NOT_SYNCED
                )
            )
        }
        
        return entities
    }

    /**
     * Apply settings to a device
     */
    private suspend fun applySettingsToDevice(deviceId: Long, settings: DeviceSettings) {
        // Check if device is connected
        if (getDeviceConnectionState(deviceId).first() != DeviceConnectionState.CONNECTED) {
            throw IllegalStateException("Device is not connected")
        }
        
        // Apply settings to the device using VeepooSDK
        
        // Set time format
        val is24Hour = settings.timeFormat == "24h"
        vpOperateManager.setDeviceTime(TimeData(is24Hour), getDefaultWriteResponse())
        
        // Set notifications
        if (settings.notificationsEnabled) {
            vpOperateManager.settingSocialMsg(
                FunctionSocailMsgData(EFunctionStatus.SUPPORT),
                object : ISocialMsgDataListener {
                    override fun onSocialMsgSupportDataChange(socailMsgData: FunctionSocailMsgData?) {
                        Timber.d("Social message support data changed: $socailMsgData")
                    }
                },
                getDefaultWriteResponse()
            )
        } else {
            vpOperateManager.settingSocialMsg(
                FunctionSocailMsgData(EFunctionStatus.UNSUPPORT),
                object : ISocialMsgDataListener {
                    override fun onSocialMsgSupportDataChange(socailMsgData: FunctionSocailMsgData?) {
                        Timber.d("Social message support data changed: $socailMsgData")
                    }
                },
                getDefaultWriteResponse()
            )
        }
        
        // Set heart rate monitoring interval
        val customSetting = CustomSetting(
            isHaveMetricSystem = true,
            isHave24Hour = is24Hour,
            isHaveAutomaticHeartRate = true,
            isHaveAutomaticSpo2 = true,
            isHaveLiftWristToViewInfo = settings.wristLiftEnabled,
            isHaveHeartRateAlarm = true,
            isHaveDisconnectRemind = true,
            isHaveSpo2Alarm = true,
            automaticHeartRateInterval = settings.heartRateInterval
        )
        
        vpOperateManager.changeCustomSetting(customSetting, getDefaultWriteResponse())
        
        // Set language
        val languageCode = when (settings.language) {
            "en" -> 0
            "zh" -> 1
            "es" -> 2
            "fr" -> 3
            "de" -> 4
            "it" -> 5
            "ja" -> 6
            "ru" -> 7
            else -> 0 // Default to English
        }
        
        vpOperateManager.settingLanguage(languageCode, getDefaultWriteResponse())
    }

    /**
     * Get a default write response for VeepooSDK operations
     */
    private fun getDefaultWriteResponse(): IBleWriteResponse {
        return object : IBleWriteResponse {
            override fun onResponse(code: Int) {
                if (code != 0) {
                    Timber.e("VeepooSDK write operation failed with code: $code")
                }
            }
        }
    }
}
