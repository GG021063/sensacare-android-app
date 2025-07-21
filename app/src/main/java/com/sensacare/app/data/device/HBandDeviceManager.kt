package com.sensacare.app.data.device

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.sensacare.app.data.device.mapper.HBandDataMapper
import com.sensacare.app.data.repository.DeviceCapabilityRepositoryImpl
import com.sensacare.app.domain.model.*
import com.sensacare.app.domain.repository.*
import com.sensacare.app.domain.util.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import timber.log.Timber
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Interface for HBand device manager
 */
interface HBandDeviceManager {
    // Connection management
    suspend fun startDeviceDiscovery(): Result<Unit>
    suspend fun stopDeviceDiscovery(): Result<Unit>
    suspend fun connectToDevice(deviceId: String): Result<Unit>
    suspend fun disconnectFromDevice(deviceId: String): Result<Unit>
    suspend fun disconnectAllDevices(): Result<Unit>
    suspend fun getConnectedDevices(): Result<List<DeviceCapability>>
    suspend fun getPairedDevices(): Result<List<DeviceCapability>>
    
    // Device capabilities
    suspend fun getDeviceCapabilities(deviceId: String): Result<DeviceCapability>
    suspend fun refreshDeviceCapabilities(deviceId: String): Result<DeviceCapability>
    
    // Data collection
    suspend fun startDataCollection(deviceId: String, metrics: Set<SupportedMetric>): Result<Unit>
    suspend fun stopDataCollection(deviceId: String, metrics: Set<SupportedMetric>): Result<Unit>
    suspend fun stopAllDataCollection(): Result<Unit>
    
    // One-time measurements
    suspend fun measureHeartRate(deviceId: String): Result<HeartRate>
    suspend fun measureBloodPressure(deviceId: String): Result<BloodPressure>
    suspend fun measureBloodOxygen(deviceId: String): Result<BloodOxygen>
    suspend fun measureBodyTemperature(deviceId: String): Result<BodyTemperature>
    suspend fun measureStressLevel(deviceId: String): Result<StressLevel>
    suspend fun recordEcg(deviceId: String, durationSeconds: Int): Result<Ecg>
    suspend fun measureBloodGlucose(deviceId: String): Result<BloodGlucose>
    
    // Data synchronization
    suspend fun synchronizeData(deviceId: String): Result<SyncResult>
    
    // Device management
    suspend fun getBatteryLevel(deviceId: String): Result<Int>
    suspend fun getFirmwareVersion(deviceId: String): Result<String>
    suspend fun updateFirmware(deviceId: String, firmwareUrl: String): Result<Unit>
    
    // Event flows
    val deviceEvents: SharedFlow<DeviceEvent>
    val dataEvents: SharedFlow<DataEvent>
}

/**
 * Device events
 */
sealed class DeviceEvent {
    data class DeviceDiscovered(val device: BluetoothDevice, val rssi: Int) : DeviceEvent()
    data class DeviceConnected(val deviceId: String, val deviceCapability: DeviceCapability) : DeviceEvent()
    data class DeviceDisconnected(val deviceId: String, val reason: DisconnectReason) : DeviceEvent()
    data class ConnectionFailed(val deviceId: String, val reason: String) : DeviceEvent()
    data class BatteryLevelChanged(val deviceId: String, val batteryLevel: Int) : DeviceEvent()
    data class SyncProgress(val deviceId: String, val progress: Int, val total: Int) : DeviceEvent()
    data class SyncCompleted(val deviceId: String, val result: SyncResult) : DeviceEvent()
    data class CapabilitiesUpdated(val deviceId: String, val deviceCapability: DeviceCapability) : DeviceEvent()
}

/**
 * Data events
 */
sealed class DataEvent {
    data class HeartRateReceived(val deviceId: String, val heartRate: HeartRate) : DataEvent()
    data class BloodPressureReceived(val deviceId: String, val bloodPressure: BloodPressure) : DataEvent()
    data class BloodOxygenReceived(val deviceId: String, val bloodOxygen: BloodOxygen) : DataEvent()
    data class BodyTemperatureReceived(val deviceId: String, val bodyTemperature: BodyTemperature) : DataEvent()
    data class StressLevelReceived(val deviceId: String, val stressLevel: StressLevel) : DataEvent()
    data class EcgReceived(val deviceId: String, val ecg: Ecg) : DataEvent()
    data class BloodGlucoseReceived(val deviceId: String, val bloodGlucose: BloodGlucose) : DataEvent()
    data class ActivityReceived(val deviceId: String, val activity: Activity) : DataEvent()
    data class SleepReceived(val deviceId: String, val sleep: Sleep) : DataEvent()
    data class DataError(val deviceId: String, val metric: SupportedMetric, val error: String) : DataEvent()
}

/**
 * Disconnect reasons
 */
enum class DisconnectReason {
    USER_REQUESTED,
    DEVICE_REQUESTED,
    CONNECTION_LOST,
    TIMEOUT,
    ERROR,
    UNKNOWN
}

/**
 * Sync result
 */
data class SyncResult(
    val deviceId: String,
    val syncStartTime: LocalDateTime,
    val syncEndTime: LocalDateTime,
    val syncDurationMillis: Long,
    val heartRateCount: Int = 0,
    val bloodPressureCount: Int = 0,
    val bloodOxygenCount: Int = 0,
    val temperatureCount: Int = 0,
    val stressLevelCount: Int = 0,
    val ecgCount: Int = 0,
    val bloodGlucoseCount: Int = 0,
    val activityCount: Int = 0,
    val sleepCount: Int = 0,
    val errors: List<String> = emptyList()
) {
    val totalRecords: Int
        get() = heartRateCount + bloodPressureCount + bloodOxygenCount + temperatureCount + 
                stressLevelCount + ecgCount + bloodGlucoseCount + activityCount + sleepCount
    
    val hasErrors: Boolean
        get() = errors.isNotEmpty()
}

/**
 * HBand device manager implementation
 */
@Singleton
class HBandDeviceManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val heartRateRepository: HeartRateRepository,
    private val bloodPressureRepository: BloodPressureRepository,
    private val bloodOxygenRepository: BloodOxygenRepository,
    private val bodyTemperatureRepository: BodyTemperatureRepository,
    private val stressLevelRepository: StressLevelRepository,
    private val ecgRepository: EcgRepository,
    private val bloodGlucoseRepository: BloodGlucoseRepository,
    private val activityRepository: ActivityRepository,
    private val sleepRepository: SleepRepository,
    private val deviceCapabilityRepository: DeviceCapabilityRepository
) : HBandDeviceManager {
    
    private val tag = "HBandDeviceManager"
    
    // Coroutine scope for device operations
    private val deviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // Bluetooth adapter
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    
    // Event flows
    private val _deviceEvents = MutableSharedFlow<DeviceEvent>(extraBufferCapacity = 10)
    override val deviceEvents: SharedFlow<DeviceEvent> = _deviceEvents
    
    private val _dataEvents = MutableSharedFlow<DataEvent>(extraBufferCapacity = 50)
    override val dataEvents: SharedFlow<DataEvent> = _dataEvents
    
    // Connected devices
    private val connectedDevices = ConcurrentHashMap<String, HBandDevice>()
    
    // Data collection jobs
    private val dataCollectionJobs = ConcurrentHashMap<String, MutableMap<SupportedMetric, Job>>()
    
    // Data buffers for batch processing
    private val dataBuffers = ConcurrentHashMap<String, MutableMap<SupportedMetric, MutableList<Any>>>()
    
    // Reconnection handlers
    private val reconnectionHandlers = ConcurrentHashMap<String, Handler>()
    private val reconnectionAttempts = ConcurrentHashMap<String, Int>()
    private val maxReconnectionAttempts = 5
    
    // HBand SDK wrapper (mock implementation for now)
    private val hbandSdk = HBandSdkWrapper(context)
    
    // Data mapper
    private val dataMapper = HBandDataMapper()
    
    // Device discovery
    private var isDiscovering = false
    private var discoveryJob: Job? = null
    
    // Initialize
    init {
        Timber.d("Initializing HBandDeviceManager")
        
        // Register for Bluetooth events
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        }
        
        // Start monitoring for device disconnections
        deviceScope.launch {
            while (isActive) {
                checkDeviceConnections()
                delay(5000) // Check every 5 seconds
            }
        }
    }
    
    /**
     * Start device discovery
     */
    override suspend fun startDeviceDiscovery(): Result<Unit> = withContext(Dispatchers.IO) {
        if (bluetoothAdapter == null) {
            Timber.e("Bluetooth not supported")
            return@withContext Result.Error(Exception("Bluetooth not supported"))
        }
        
        if (!bluetoothAdapter.isEnabled) {
            Timber.e("Bluetooth not enabled")
            return@withContext Result.Error(Exception("Bluetooth not enabled"))
        }
        
        if (isDiscovering) {
            Timber.d("Discovery already in progress")
            return@withContext Result.Success(Unit)
        }
        
        try {
            isDiscovering = true
            
            // Cancel any previous discovery
            if (bluetoothAdapter.isDiscovering) {
                bluetoothAdapter.cancelDiscovery()
            }
            
            // Start discovery through HBand SDK
            hbandSdk.startDiscovery { device, rssi ->
                deviceScope.launch {
                    _deviceEvents.emit(DeviceEvent.DeviceDiscovered(device, rssi))
                }
            }
            
            // Set a timeout for discovery
            discoveryJob = deviceScope.launch {
                delay(30000) // 30 seconds timeout
                stopDeviceDiscovery()
            }
            
            Timber.d("Device discovery started")
            Result.Success(Unit)
        } catch (e: Exception) {
            isDiscovering = false
            Timber.e(e, "Failed to start device discovery")
            Result.Error(e)
        }
    }
    
    /**
     * Stop device discovery
     */
    override suspend fun stopDeviceDiscovery(): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isDiscovering) {
            return@withContext Result.Success(Unit)
        }
        
        try {
            isDiscovering = false
            discoveryJob?.cancel()
            discoveryJob = null
            
            // Stop discovery through HBand SDK
            hbandSdk.stopDiscovery()
            
            Timber.d("Device discovery stopped")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to stop device discovery")
            Result.Error(e)
        }
    }
    
    /**
     * Connect to device
     */
    override suspend fun connectToDevice(deviceId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (connectedDevices.containsKey(deviceId)) {
            Timber.d("Device already connected: $deviceId")
            return@withContext Result.Success(Unit)
        }
        
        try {
            // Stop discovery if it's running
            if (isDiscovering) {
                stopDeviceDiscovery()
            }
            
            // Connect through HBand SDK
            val device = hbandSdk.connectToDevice(deviceId)
            if (device == null) {
                Timber.e("Failed to connect to device: $deviceId")
                return@withContext Result.Error(Exception("Failed to connect to device"))
            }
            
            // Get device capabilities
            val capabilities = detectDeviceCapabilities(device)
            
            // Save device capabilities
            deviceCapabilityRepository.saveDeviceCapability(capabilities)
            
            // Create HBand device wrapper
            val hbandDevice = HBandDevice(
                device = device,
                capabilities = capabilities,
                connectionTime = LocalDateTime.now()
            )
            
            // Add to connected devices
            connectedDevices[deviceId] = hbandDevice
            
            // Reset reconnection attempts
            reconnectionAttempts[deviceId] = 0
            
            // Initialize data buffers
            dataBuffers[deviceId] = mutableMapOf()
            
            // Emit connected event
            _deviceEvents.emit(DeviceEvent.DeviceConnected(deviceId, capabilities))
            
            Timber.d("Connected to device: $deviceId")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to connect to device: $deviceId")
            _deviceEvents.emit(DeviceEvent.ConnectionFailed(deviceId, e.message ?: "Unknown error"))
            Result.Error(e)
        }
    }
    
    /**
     * Disconnect from device
     */
    override suspend fun disconnectFromDevice(deviceId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val device = connectedDevices[deviceId]
        if (device == null) {
            Timber.d("Device not connected: $deviceId")
            return@withContext Result.Success(Unit)
        }
        
        try {
            // Stop all data collection for this device
            stopDataCollection(deviceId, device.capabilities.supportedMetrics)
            
            // Cancel any reconnection attempts
            reconnectionHandlers[deviceId]?.removeCallbacksAndMessages(null)
            reconnectionHandlers.remove(deviceId)
            reconnectionAttempts.remove(deviceId)
            
            // Disconnect through HBand SDK
            hbandSdk.disconnectDevice(deviceId)
            
            // Remove from connected devices
            connectedDevices.remove(deviceId)
            
            // Clean up data buffers
            dataBuffers.remove(deviceId)
            
            // Emit disconnected event
            _deviceEvents.emit(DeviceEvent.DeviceDisconnected(deviceId, DisconnectReason.USER_REQUESTED))
            
            Timber.d("Disconnected from device: $deviceId")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to disconnect from device: $deviceId")
            Result.Error(e)
        }
    }
    
    /**
     * Disconnect all devices
     */
    override suspend fun disconnectAllDevices(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val deviceIds = connectedDevices.keys.toList()
            
            for (deviceId in deviceIds) {
                disconnectFromDevice(deviceId)
            }
            
            Timber.d("Disconnected from all devices")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to disconnect from all devices")
            Result.Error(e)
        }
    }
    
    /**
     * Get connected devices
     */
    override suspend fun getConnectedDevices(): Result<List<DeviceCapability>> = withContext(Dispatchers.IO) {
        try {
            val devices = connectedDevices.values.map { it.capabilities }
            Result.Success(devices)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get connected devices")
            Result.Error(e)
        }
    }
    
    /**
     * Get paired devices
     */
    override suspend fun getPairedDevices(): Result<List<DeviceCapability>> = withContext(Dispatchers.IO) {
        if (bluetoothAdapter == null) {
            Timber.e("Bluetooth not supported")
            return@withContext Result.Error(Exception("Bluetooth not supported"))
        }
        
        try {
            val pairedDevices = hbandSdk.getPairedDevices()
            val deviceCapabilities = mutableListOf<DeviceCapability>()
            
            for (device in pairedDevices) {
                // Check if we already have capabilities for this device
                val savedCapabilities = deviceCapabilityRepository.getDeviceCapability(device.address)
                
                if (savedCapabilities is Result.Success) {
                    deviceCapabilities.add(savedCapabilities.data)
                } else {
                    // Create basic capabilities
                    val capabilities = DeviceCapability(
                        deviceId = device.address,
                        deviceName = device.name ?: "Unknown",
                        deviceModel = "Unknown",
                        supportedMetrics = emptySet(),
                        batteryLevel = null,
                        firmwareVersion = null
                    )
                    deviceCapabilities.add(capabilities)
                }
            }
            
            Result.Success(deviceCapabilities)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get paired devices")
            Result.Error(e)
        }
    }
    
    /**
     * Get device capabilities
     */
    override suspend fun getDeviceCapabilities(deviceId: String): Result<DeviceCapability> = withContext(Dispatchers.IO) {
        // Check if device is connected
        val connectedDevice = connectedDevices[deviceId]
        if (connectedDevice != null) {
            return@withContext Result.Success(connectedDevice.capabilities)
        }
        
        // Try to get from repository
        return@withContext deviceCapabilityRepository.getDeviceCapability(deviceId)
    }
    
    /**
     * Refresh device capabilities
     */
    override suspend fun refreshDeviceCapabilities(deviceId: String): Result<DeviceCapability> = withContext(Dispatchers.IO) {
        val device = connectedDevices[deviceId]
        if (device == null) {
            Timber.e("Device not connected: $deviceId")
            return@withContext Result.Error(Exception("Device not connected"))
        }
        
        try {
            // Detect device capabilities
            val capabilities = detectDeviceCapabilities(device.device)
            
            // Update device capabilities
            device.capabilities = capabilities
            
            // Save device capabilities
            deviceCapabilityRepository.saveDeviceCapability(capabilities)
            
            // Emit capabilities updated event
            _deviceEvents.emit(DeviceEvent.CapabilitiesUpdated(deviceId, capabilities))
            
            Timber.d("Refreshed device capabilities: $deviceId")
            Result.Success(capabilities)
        } catch (e: Exception) {
            Timber.e(e, "Failed to refresh device capabilities: $deviceId")
            Result.Error(e)
        }
    }
    
    /**
     * Start data collection
     */
    override suspend fun startDataCollection(deviceId: String, metrics: Set<SupportedMetric>): Result<Unit> = withContext(Dispatchers.IO) {
        val device = connectedDevices[deviceId]
        if (device == null) {
            Timber.e("Device not connected: $deviceId")
            return@withContext Result.Error(Exception("Device not connected"))
        }
        
        try {
            // Check if device supports all requested metrics
            val supportedMetrics = device.capabilities.supportedMetrics
            val unsupportedMetrics = metrics - supportedMetrics
            
            if (unsupportedMetrics.isNotEmpty()) {
                Timber.e("Device does not support metrics: $unsupportedMetrics")
                return@withContext Result.Error(Exception("Device does not support metrics: $unsupportedMetrics"))
            }
            
            // Initialize data collection jobs map if needed
            if (!dataCollectionJobs.containsKey(deviceId)) {
                dataCollectionJobs[deviceId] = mutableMapOf()
            }
            
            // Initialize data buffers if needed
            if (!dataBuffers.containsKey(deviceId)) {
                dataBuffers[deviceId] = mutableMapOf()
            }
            
            // Start data collection for each metric
            for (metric in metrics) {
                // Skip if already collecting
                if (dataCollectionJobs[deviceId]?.containsKey(metric) == true) {
                    continue
                }
                
                // Create data buffer for this metric
                if (!dataBuffers[deviceId]!!.containsKey(metric)) {
                    dataBuffers[deviceId]!![metric] = mutableListOf()
                }
                
                // Start data collection job
                val job = deviceScope.launch {
                    collectMetricData(deviceId, metric)
                }
                
                // Store job
                dataCollectionJobs[deviceId]!![metric] = job
            }
            
            Timber.d("Started data collection for device: $deviceId, metrics: $metrics")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to start data collection for device: $deviceId")
            Result.Error(e)
        }
    }
    
    /**
     * Stop data collection
     */
    override suspend fun stopDataCollection(deviceId: String, metrics: Set<SupportedMetric>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val jobs = dataCollectionJobs[deviceId]
            if (jobs == null) {
                Timber.d("No data collection jobs for device: $deviceId")
                return@withContext Result.Success(Unit)
            }
            
            // Stop data collection for each metric
            for (metric in metrics) {
                val job = jobs[metric]
                if (job != null) {
                    job.cancel()
                    jobs.remove(metric)
                }
            }
            
            // Remove jobs map if empty
            if (jobs.isEmpty()) {
                dataCollectionJobs.remove(deviceId)
            }
            
            Timber.d("Stopped data collection for device: $deviceId, metrics: $metrics")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to stop data collection for device: $deviceId")
            Result.Error(e)
        }
    }
    
    /**
     * Stop all data collection
     */
    override suspend fun stopAllDataCollection(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Cancel all data collection jobs
            for ((deviceId, jobs) in dataCollectionJobs) {
                for ((_, job) in jobs) {
                    job.cancel()
                }
                jobs.clear()
            }
            
            dataCollectionJobs.clear()
            
            Timber.d("Stopped all data collection")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to stop all data collection")
            Result.Error(e)
        }
    }
    
    /**
     * Measure heart rate
     */
    override suspend fun measureHeartRate(deviceId: String): Result<HeartRate> = withContext(Dispatchers.IO) {
        val device = connectedDevices[deviceId]
        if (device == null) {
            Timber.e("Device not connected: $deviceId")
            return@withContext Result.Error(Exception("Device not connected"))
        }
        
        if (!device.capabilities.supportsMetric(SupportedMetric.HEART_RATE)) {
            Timber.e("Device does not support heart rate measurement")
            return@withContext Result.Error(Exception("Device does not support heart rate measurement"))
        }
        
        try {
            // Measure heart rate through HBand SDK
            val heartRateData = hbandSdk.measureHeartRate(deviceId)
            if (heartRateData == null) {
                Timber.e("Failed to measure heart rate")
                return@withContext Result.Error(Exception("Failed to measure heart rate"))
            }
            
            // Map to domain model
            val heartRate = dataMapper.mapToHeartRate(
                heartRateData = heartRateData,
                userId = "current_user", // Replace with actual user ID
                deviceId = deviceId,
                deviceType = device.capabilities.deviceModel
            )
            
            // Save to repository
            heartRateRepository.saveHeartRate(heartRate)
            
            // Emit data event
            _dataEvents.emit(DataEvent.HeartRateReceived(deviceId, heartRate))
            
            Timber.d("Measured heart rate: ${heartRate.value} bpm")
            Result.Success(heartRate)
        } catch (e: Exception) {
            Timber.e(e, "Failed to measure heart rate")
            _dataEvents.emit(DataEvent.DataError(deviceId, SupportedMetric.HEART_RATE, e.message ?: "Unknown error"))
            Result.Error(e)
        }
    }
    
    /**
     * Measure blood pressure
     */
    override suspend fun measureBloodPressure(deviceId: String): Result<BloodPressure> = withContext(Dispatchers.IO) {
        val device = connectedDevices[deviceId]
        if (device == null) {
            Timber.e("Device not connected: $deviceId")
            return@withContext Result.Error(Exception("Device not connected"))
        }
        
        if (!device.capabilities.supportsMetric(SupportedMetric.BLOOD_PRESSURE)) {
            Timber.e("Device does not support blood pressure measurement")
            return@withContext Result.Error(Exception("Device does not support blood pressure measurement"))
        }
        
        try {
            // Measure blood pressure through HBand SDK
            val bloodPressureData = hbandSdk.measureBloodPressure(deviceId)
            if (bloodPressureData == null) {
                Timber.e("Failed to measure blood pressure")
                return@withContext Result.Error(Exception("Failed to measure blood pressure"))
            }
            
            // Map to domain model
            val bloodPressure = dataMapper.mapToBloodPressure(
                bloodPressureData = bloodPressureData,
                userId = "current_user", // Replace with actual user ID
                deviceId = deviceId,
                deviceType = device.capabilities.deviceModel
            )
            
            // Save to repository
            bloodPressureRepository.saveBloodPressure(bloodPressure)
            
            // Emit data event
            _dataEvents.emit(DataEvent.BloodPressureReceived(deviceId, bloodPressure))
            
            Timber.d("Measured blood pressure: ${bloodPressure.systolic}/${bloodPressure.diastolic} mmHg")
            Result.Success(bloodPressure)
        } catch (e: Exception) {
            Timber.e(e, "Failed to measure blood pressure")
            _dataEvents.emit(DataEvent.DataError(deviceId, SupportedMetric.BLOOD_PRESSURE, e.message ?: "Unknown error"))
            Result.Error(e)
        }
    }
    
    /**
     * Measure blood oxygen
     */
    override suspend fun measureBloodOxygen(deviceId: String): Result<BloodOxygen> = withContext(Dispatchers.IO) {
        val device = connectedDevices[deviceId]
        if (device == null) {
            Timber.e("Device not connected: $deviceId")
            return@withContext Result.Error(Exception("Device not connected"))
        }
        
        if (!device.capabilities.supportsMetric(SupportedMetric.BLOOD_OXYGEN)) {
            Timber.e("Device does not support blood oxygen measurement")
            return@withContext Result.Error(Exception("Device does not support blood oxygen measurement"))
        }
        
        try {
            // Measure blood oxygen through HBand SDK
            val bloodOxygenData = hbandSdk.measureBloodOxygen(deviceId)
            if (bloodOxygenData == null) {
                Timber.e("Failed to measure blood oxygen")
                return@withContext Result.Error(Exception("Failed to measure blood oxygen"))
            }
            
            // Map to domain model
            val bloodOxygen = dataMapper.mapToBloodOxygen(
                bloodOxygenData = bloodOxygenData,
                userId = "current_user", // Replace with actual user ID
                deviceId = deviceId,
                deviceType = device.capabilities.deviceModel
            )
            
            // Save to repository
            bloodOxygenRepository.saveBloodOxygen(bloodOxygen)
            
            // Emit data event
            _dataEvents.emit(DataEvent.BloodOxygenReceived(deviceId, bloodOxygen))
            
            Timber.d("Measured blood oxygen: ${bloodOxygen.value}%")
            Result.Success(bloodOxygen)
        } catch (e: Exception) {
            Timber.e(e, "Failed to measure blood oxygen")
            _dataEvents.emit(DataEvent.DataError(deviceId, SupportedMetric.BLOOD_OXYGEN, e.message ?: "Unknown error"))
            Result.Error(e)
        }
    }
    
    /**
     * Measure body temperature
     */
    override suspend fun measureBodyTemperature(deviceId: String): Result<BodyTemperature> = withContext(Dispatchers.IO) {
        val device = connectedDevices[deviceId]
        if (device == null) {
            Timber.e("Device not connected: $deviceId")
            return@withContext Result.Error(Exception("Device not connected"))
        }
        
        if (!device.capabilities.supportsMetric(SupportedMetric.BODY_TEMPERATURE)) {
            Timber.e("Device does not support body temperature measurement")
            return@withContext Result.Error(Exception("Device does not support body temperature measurement"))
        }
        
        try {
            // Measure body temperature through HBand SDK
            val temperatureData = hbandSdk.measureBodyTemperature(deviceId)
            if (temperatureData == null) {
                Timber.e("Failed to measure body temperature")
                return@withContext Result.Error(Exception("Failed to measure body temperature"))
            }
            
            // Map to domain model
            val bodyTemperature = dataMapper.mapToBodyTemperature(
                temperatureData = temperatureData,
                userId = "current_user", // Replace with actual user ID
                deviceId = deviceId,
                deviceType = device.capabilities.deviceModel
            )
            
            // Save to repository
            bodyTemperatureRepository.saveBodyTemperature(bodyTemperature)
            
            // Emit data event
            _dataEvents.emit(DataEvent.BodyTemperatureReceived(deviceId, bodyTemperature))
            
            Timber.d("Measured body temperature: ${bodyTemperature.value}°C")
            Result.Success(bodyTemperature)
        } catch (e: Exception) {
            Timber.e(e, "Failed to measure body temperature")
            _dataEvents.emit(DataEvent.DataError(deviceId, SupportedMetric.BODY_TEMPERATURE, e.message ?: "Unknown error"))
            Result.Error(e)
        }
    }
    
    /**
     * Measure stress level
     */
    override suspend fun measureStressLevel(deviceId: String): Result<StressLevel> = withContext(Dispatchers.IO) {
        val device = connectedDevices[deviceId]
        if (device == null) {
            Timber.e("Device not connected: $deviceId")
            return@withContext Result.Error(Exception("Device not connected"))
        }
        
        if (!device.capabilities.supportsMetric(SupportedMetric.STRESS_LEVEL)) {
            Timber.e("Device does not support stress level measurement")
            return@withContext Result.Error(Exception("Device does not support stress level measurement"))
        }
        
        try {
            // Measure stress level through HBand SDK
            val stressData = hbandSdk.measureStressLevel(deviceId)
            if (stressData == null) {
                Timber.e("Failed to measure stress level")
                return@withContext Result.Error(Exception("Failed to measure stress level"))
            }
            
            // Map to domain model
            val stressLevel = dataMapper.mapToStressLevel(
                stressData = stressData,
                userId = "current_user", // Replace with actual user ID
                deviceId = deviceId,
                deviceType = device.capabilities.deviceModel
            )
            
            // Save to repository
            stressLevelRepository.saveStressLevel(stressLevel)
            
            // Emit data event
            _dataEvents.emit(DataEvent.StressLevelReceived(deviceId, stressLevel))
            
            Timber.d("Measured stress level: ${stressLevel.value}")
            Result.Success(stressLevel)
        } catch (e: Exception) {
            Timber.e(e, "Failed to measure stress level")
            _dataEvents.emit(DataEvent.DataError(deviceId, SupportedMetric.STRESS_LEVEL, e.message ?: "Unknown error"))
            Result.Error(e)
        }
    }
    
    /**
     * Record ECG
     */
    override suspend fun recordEcg(deviceId: String, durationSeconds: Int): Result<Ecg> = withContext(Dispatchers.IO) {
        val device = connectedDevices[deviceId]
        if (device == null) {
            Timber.e("Device not connected: $deviceId")
            return@withContext Result.Error(Exception("Device not connected"))
        }
        
        if (!device.capabilities.supportsMetric(SupportedMetric.ECG)) {
            Timber.e("Device does not support ECG recording")
            return@withContext Result.Error(Exception("Device does not support ECG recording"))
        }
        
        try {
            // Record ECG through HBand SDK
            val ecgData = hbandSdk.recordEcg(deviceId, durationSeconds)
            if (ecgData == null) {
                Timber.e("Failed to record ECG")
                return@withContext Result.Error(Exception("Failed to record ECG"))
            }
            
            // Map to domain model
            val ecg = dataMapper.mapToEcg(
                ecgData = ecgData,
                userId = "current_user", // Replace with actual user ID
                deviceId = deviceId,
                deviceType = device.capabilities.deviceModel,
                durationSeconds = durationSeconds
            )
            
            // Save to repository
            ecgRepository.saveEcg(ecg)
            
            // Emit data event
            _dataEvents.emit(DataEvent.EcgReceived(deviceId, ecg))
            
            Timber.d("Recorded ECG: ${ecg.id}, duration: $durationSeconds seconds")
            Result.Success(ecg)
        } catch (e: Exception) {
            Timber.e(e, "Failed to record ECG")
            _dataEvents.emit(DataEvent.DataError(deviceId, SupportedMetric.ECG, e.message ?: "Unknown error"))
            Result.Error(e)
        }
    }
    
    /**
     * Measure blood glucose
     */
    override suspend fun measureBloodGlucose(deviceId: String): Result<BloodGlucose> = withContext(Dispatchers.IO) {
        val device = connectedDevices[deviceId]
        if (device == null) {
            Timber.e("Device not connected: $deviceId")
            return@withContext Result.Error(Exception("Device not connected"))
        }
        
        if (!device.capabilities.supportsMetric(SupportedMetric.BLOOD_GLUCOSE)) {
            Timber.e("Device does not support blood glucose measurement")
            return@withContext Result.Error(Exception("Device does not support blood glucose measurement"))
        }
        
        try {
            // Measure blood glucose through HBand SDK
            val glucoseData = hbandSdk.measureBloodGlucose(deviceId)
            if (glucoseData == null) {
                Timber.e("Failed to measure blood glucose")
                return@withContext Result.Error(Exception("Failed to measure blood glucose"))
            }
            
            // Map to domain model
            val bloodGlucose = dataMapper.mapToBloodGlucose(
                glucoseData = glucoseData,
                userId = "current_user", // Replace with actual user ID
                deviceId = deviceId,
                deviceType = device.capabilities.deviceModel
            )
            
            // Save to repository
            bloodGlucoseRepository.saveBloodGlucose(bloodGlucose)
            
            // Emit data event
            _dataEvents.emit(DataEvent.BloodGlucoseReceived(deviceId, bloodGlucose))
            
            Timber.d("Measured blood glucose: ${bloodGlucose.value} mg/dL")
            Result.Success(bloodGlucose)
        } catch (e: Exception) {
            Timber.e(e, "Failed to measure blood glucose")
            _dataEvents.emit(DataEvent.DataError(deviceId, SupportedMetric.BLOOD_GLUCOSE, e.message ?: "Unknown error"))
            Result.Error(e)
        }
    }
    
    /**
     * Synchronize data
     */
    override suspend fun synchronizeData(deviceId: String): Result<SyncResult> = withContext(Dispatchers.IO) {
        val device = connectedDevices[deviceId]
        if (device == null) {
            Timber.e("Device not connected: $deviceId")
            return@withContext Result.Error(Exception("Device not connected"))
        }
        
        try {
            val syncStartTime = LocalDateTime.now()
            var heartRateCount = 0
            var bloodPressureCount = 0
            var bloodOxygenCount = 0
            var temperatureCount = 0
            var stressLevelCount = 0
            var ecgCount = 0
            var bloodGlucoseCount = 0
            var activityCount = 0
            var sleepCount = 0
            val errors = mutableListOf<String>()
            
            // Emit sync started event
            _deviceEvents.emit(DeviceEvent.SyncProgress(deviceId, 0, 100))
            
            // Sync heart rate data if supported
            if (device.capabilities.supportsMetric(SupportedMetric.HEART_RATE)) {
                try {
                    val heartRateData = hbandSdk.syncHeartRateData(deviceId)
                    if (heartRateData.isNotEmpty()) {
                        val heartRates = heartRateData.map { 
                            dataMapper.mapToHeartRate(
                                heartRateData = it,
                                userId = "current_user", // Replace with actual user ID
                                deviceId = deviceId,
                                deviceType = device.capabilities.deviceModel
                            )
                        }
                        
                        // Save to repository
                        heartRateRepository.saveMultipleHeartRates(heartRates)
                        
                        heartRateCount = heartRates.size
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to sync heart rate data")
                    errors.add("Heart rate sync failed: ${e.message}")
                }
            }
            
            // Update progress
            _deviceEvents.emit(DeviceEvent.SyncProgress(deviceId, 10, 100))
            
            // Sync blood pressure data if supported
            if (device.capabilities.supportsMetric(SupportedMetric.BLOOD_PRESSURE)) {
                try {
                    val bloodPressureData = hbandSdk.syncBloodPressureData(deviceId)
                    if (bloodPressureData.isNotEmpty()) {
                        val bloodPressures = bloodPressureData.map { 
                            dataMapper.mapToBloodPressure(
                                bloodPressureData = it,
                                userId = "current_user", // Replace with actual user ID
                                deviceId = deviceId,
                                deviceType = device.capabilities.deviceModel
                            )
                        }
                        
                        // Save to repository
                        bloodPressureRepository.saveMultipleBloodPressures(bloodPressures)
                        
                        bloodPressureCount = bloodPressures.size
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to sync blood pressure data")
                    errors.add("Blood pressure sync failed: ${e.message}")
                }
            }
            
            // Update progress
            _deviceEvents.emit(DeviceEvent.SyncProgress(deviceId, 20, 100))
            
            // Sync blood oxygen data if supported
            if (device.capabilities.supportsMetric(SupportedMetric.BLOOD_OXYGEN)) {
                try {
                    val bloodOxygenData = hbandSdk.syncBloodOxygenData(deviceId)
                    if (bloodOxygenData.isNotEmpty()) {
                        val bloodOxygens = bloodOxygenData.map { 
                            dataMapper.mapToBloodOxygen(
                                bloodOxygenData = it,
                                userId = "current_user", // Replace with actual user ID
                                deviceId = deviceId,
                                deviceType = device.capabilities.deviceModel
                            )
                        }
                        
                        // Save to repository
                        bloodOxygenRepository.saveMultipleBloodOxygen(bloodOxygens)
                        
                        bloodOxygenCount = bloodOxygens.size
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to sync blood oxygen data")
                    errors.add("Blood oxygen sync failed: ${e.message}")
                }
            }
            
            // Update progress
            _deviceEvents.emit(DeviceEvent.SyncProgress(deviceId, 30, 100))
            
            // Sync temperature data if supported
            if (device.capabilities.supportsMetric(SupportedMetric.BODY_TEMPERATURE)) {
                try {
                    val temperatureData = hbandSdk.syncTemperatureData(deviceId)
                    if (temperatureData.isNotEmpty()) {
                        val temperatures = temperatureData.map { 
                            dataMapper.mapToBodyTemperature(
                                temperatureData = it,
                                userId = "current_user", // Replace with actual user ID
                                deviceId = deviceId,
                                deviceType = device.capabilities.deviceModel
                            )
                        }
                        
                        // Save to repository
                        bodyTemperatureRepository.saveMultipleBodyTemperature(temperatures)
                        
                        temperatureCount = temperatures.size
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to sync temperature data")
                    errors.add("Temperature sync failed: ${e.message}")
                }
            }
            
            // Update progress
            _deviceEvents.emit(DeviceEvent.SyncProgress(deviceId, 40, 100))
            
            // Sync stress level data if supported
            if (device.capabilities.supportsMetric(SupportedMetric.STRESS_LEVEL)) {
                try {
                    val stressData = hbandSdk.syncStressLevelData(deviceId)
                    if (stressData.isNotEmpty()) {
                        val stressLevels = stressData.map { 
                            dataMapper.mapToStressLevel(
                                stressData = it,
                                userId = "current_user", // Replace with actual user ID
                                deviceId = deviceId,
                                deviceType = device.capabilities.deviceModel
                            )
                        }
                        
                        // Save to repository
                        stressLevelRepository.saveMultipleStressLevel(stressLevels)
                        
                        stressLevelCount = stressLevels.size
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to sync stress level data")
                    errors.add("Stress level sync failed: ${e.message}")
                }
            }
            
            // Update progress
            _deviceEvents.emit(DeviceEvent.SyncProgress(deviceId, 50, 100))
            
            // Sync ECG data if supported
            if (device.capabilities.supportsMetric(SupportedMetric.ECG)) {
                try {
                    val ecgData = hbandSdk.syncEcgData(deviceId)
                    if (ecgData.isNotEmpty()) {
                        for (data in ecgData) {
                            val ecg = dataMapper.mapToEcg(
                                ecgData = data,
                                userId = "current_user", // Replace with actual user ID
                                deviceId = deviceId,
                                deviceType = device.capabilities.deviceModel,
                                durationSeconds = data.durationSeconds
                            )
                            
                            // Save to repository
                            ecgRepository.saveEcg(ecg)
                            
                            ecgCount++
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to sync ECG data")
                    errors.add("ECG sync failed: ${e.message}")
                }
            }
            
            // Update progress
            _deviceEvents.emit(DeviceEvent.SyncProgress(deviceId, 60, 100))
            
            // Sync blood glucose data if supported
            if (device.capabilities.supportsMetric(SupportedMetric.BLOOD_GLUCOSE)) {
                try {
                    val glucoseData = hbandSdk.syncBloodGlucoseData(deviceId)
                    if (glucoseData.isNotEmpty()) {
                        val bloodGlucoses = glucoseData.map { 
                            dataMapper.mapToBloodGlucose(
                                glucoseData = it,
                                userId = "current_user", // Replace with actual user ID
                                deviceId = deviceId,
                                deviceType = device.capabilities.deviceModel
                            )
                        }
                        
                        // Save to repository
                        bloodGlucoseRepository.saveMultipleBloodGlucose(bloodGlucoses)
                        
                        bloodGlucoseCount = bloodGlucoses.size
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to sync blood glucose data")
                    errors.add("Blood glucose sync failed: ${e.message}")
                }
            }
            
            // Update progress
            _deviceEvents.emit(DeviceEvent.SyncProgress(deviceId, 70, 100))
            
            // Sync activity data if supported
            if (device.capabilities.supportsMetric(SupportedMetric.ACTIVITY)) {
                try {
                    val activityData = hbandSdk.syncActivityData(deviceId)
                    if (activityData.isNotEmpty()) {
                        val activities = activityData.map { 
                            dataMapper.mapToActivity(
                                activityData = it,
                                userId = "current_user", // Replace with actual user ID
                                deviceId = deviceId,
                                deviceType = device.capabilities.deviceModel
                            )
                        }
                        
                        // Save to repository
                        activityRepository.saveMultipleActivities(activities)
                        
                        activityCount = activities.size
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to sync activity data")
                    errors.add("Activity sync failed: ${e.message}")
                }
            }
            
            // Update progress
            _deviceEvents.emit(DeviceEvent.SyncProgress(deviceId, 80, 100))
            
            // Sync sleep data if supported
            if (device.capabilities.supportsMetric(SupportedMetric.SLEEP)) {
                try {
                    val sleepData = hbandSdk.syncSleepData(deviceId)
                    if (sleepData.isNotEmpty()) {
                        val sleeps = sleepData.map { 
                            dataMapper.mapToSleep(
                                sleepData = it,
                                userId = "current_user", // Replace with actual user ID
                                deviceId = deviceId,
                                deviceType = device.capabilities.deviceModel
                            )
                        }
                        
                        // Save to repository
                        sleepRepository.saveMultipleSleeps(sleeps)
                        
                        sleepCount = sleeps.size
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to sync sleep data")
                    errors.add("Sleep sync failed: ${e.message}")
                }
            }
            
            // Update progress
            _deviceEvents.emit(DeviceEvent.SyncProgress(deviceId, 90, 100))
            
            // Clear device data if necessary
            if (hbandSdk.shouldClearDeviceDataAfterSync(deviceId)) {
                try {
                    hbandSdk.clearDeviceData(deviceId)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to clear device data")
                    errors.add("Clear device data failed: ${e.message}")
                }
            }
            
            // Update progress
            _deviceEvents.emit(DeviceEvent.SyncProgress(deviceId, 100, 100))
            
            // Create sync result
            val syncEndTime = LocalDateTime.now()
            val syncDurationMillis = java.time.Duration.between(syncStartTime, syncEndTime).toMillis()
            
            val syncResult = SyncResult(
                deviceId = deviceId,
                syncStartTime = syncStartTime,
                syncEndTime = syncEndTime,
                syncDurationMillis = syncDurationMillis,
                heartRateCount = heartRateCount,
                bloodPressureCount = bloodPressureCount,
                bloodOxygenCount = bloodOxygenCount,
                temperatureCount = temperatureCount,
                stressLevelCount = stressLevelCount,
                ecgCount = ecgCount,
                bloodGlucoseCount = bloodGlucoseCount,
                activityCount = activityCount,
                sleepCount = sleepCount,
                errors = errors
            )
            
            // Emit sync completed event
            _deviceEvents.emit(DeviceEvent.SyncCompleted(deviceId, syncResult))
            
            Timber.d("Synchronized data from device: $deviceId, total records: ${syncResult.totalRecords}")
            Result.Success(syncResult)
        } catch (e: Exception) {
            Timber.e(e, "Failed to synchronize data from device: $deviceId")
            Result.Error(e)
        }
    }
    
    /**
     * Get battery level
     */
    override suspend fun getBatteryLevel(deviceId: String): Result<Int> = withContext(Dispatchers.IO) {
        val device = connectedDevices[deviceId]
        if (device == null) {
            Timber.e("Device not connected: $deviceId")
            return@withContext Result.Error(Exception("Device not connected"))
        }
        
        try {
            // Get battery level through HBand SDK
            val batteryLevel = hbandSdk.getBatteryLevel(deviceId)
            if (batteryLevel == null) {
                Timber.e("Failed to get battery level")
                return@withContext Result.Error(Exception("Failed to get battery level"))
            }
            
            // Update device capabilities
            device.capabilities = device.capabilities.copy(batteryLevel = batteryLevel)
            
            // Save device capabilities
            deviceCapabilityRepository.updateDeviceBatteryLevel(deviceId, batteryLevel)
            
            // Emit battery level changed event
            _deviceEvents.emit(DeviceEvent.BatteryLevelChanged(deviceId, batteryLevel))
            
            Timber.d("Got battery level: $batteryLevel%")
            Result.Success(batteryLevel)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get battery level")
            Result.Error(e)
        }
    }
    
    /**
     * Get firmware version
     */
    override suspend fun getFirmwareVersion(deviceId: String): Result<String> = withContext(Dispatchers.IO) {
        val device = connectedDevices[deviceId]
        if (device == null) {
            Timber.e("Device not connected: $deviceId")
            return@withContext Result.Error(Exception("Device not connected"))
        }
        
        try {
            // Get firmware version through HBand SDK
            val firmwareVersion = hbandSdk.getFirmwareVersion(deviceId)
            if (firmwareVersion == null) {
                Timber.e("Failed to get firmware version")
                return@withContext Result.Error(Exception("Failed to get firmware version"))
            }
            
            // Update device capabilities
            device.capabilities = device.capabilities.copy(firmwareVersion = firmwareVersion)
            
            // Save device capabilities
            deviceCapabilityRepository.updateDeviceFirmware(deviceId, firmwareVersion)
            
            Timber.d("Got firmware version: $firmwareVersion")
            Result.Success(firmwareVersion)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get firmware version")
            Result.Error(e)
        }
    }
    
    /**
     * Update firmware
     */
    override suspend fun updateFirmware(deviceId: String, firmwareUrl: String): Result<Unit> = withContext(Dispatchers.IO) {
        val device = connectedDevices[deviceId]
        if (device == null) {
            Timber.e("Device not connected: $deviceId")
            return@withContext Result.Error(Exception("Device not connected"))
        }
        
        try {
            // Update firmware through HBand SDK
            val success = hbandSdk.updateFirmware(deviceId, firmwareUrl)
            if (!success) {
                Timber.e("Failed to update firmware")
                return@withContext Result.Error(Exception("Failed to update firmware"))
            }
            
            // Get new firmware version
            val firmwareVersion = hbandSdk.getFirmwareVersion(deviceId)
            if (firmwareVersion != null) {
                // Update device capabilities
                device.capabilities = device.capabilities.copy(firmwareVersion = firmwareVersion)
                
                // Save device capabilities
                deviceCapabilityRepository.updateDeviceFirmware(deviceId, firmwareVersion)
            }
            
            Timber.d("Updated firmware")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update firmware")
            Result.Error(e)
        }
    }
    
    /**
     * Collect metric data
     */
    private suspend fun collectMetricData(deviceId: String, metric: SupportedMetric) {
        Timber.d("Starting data collection for metric: $metric")
        
        try {
            while (isActive) {
                try {
                    when (metric) {
                        SupportedMetric.HEART_RATE -> {
                            val result = measureHeartRate(deviceId)
                            if (result is Result.Success) {
                                // Data is already saved and event emitted in measureHeartRate
                            }
                        }
                        SupportedMetric.BLOOD_PRESSURE -> {
                            val result = measureBloodPressure(deviceId)
                            if (result is Result.Success) {
                                // Data is already saved and event emitted in measureBloodPressure
                            }
                        }
                        SupportedMetric.BLOOD_OXYGEN -> {
                            val result = measureBloodOxygen(deviceId)
                            if (result is Result.Success) {
                                // Data is already saved and event emitted in measureBloodOxygen
                            }
                        }
                        SupportedMetric.BODY_TEMPERATURE -> {
                            val result = measureBodyTemperature(deviceId)
                            if (result is Result.Success) {
                                // Data is already saved and event emitted in measureBodyTemperature
                            }
                        }
                        SupportedMetric.STRESS_LEVEL -> {
                            val result = measureStressLevel(deviceId)
                            if (result is Result.Success) {
                                // Data is already saved and event emitted in measureStressLevel
                            }
                        }
                        SupportedMetric.ECG -> {
                            // ECG is not collected continuously
                        }
                        SupportedMetric.BLOOD_GLUCOSE -> {
                            // Blood glucose is not collected continuously
                        }
                        SupportedMetric.ACTIVITY -> {
                            // Activity data is collected by the device and synced later
                        }
                        SupportedMetric.SLEEP -> {
                            // Sleep data is collected by the device and synced later
                        }
                        else -> {
                            Timber.d("Unsupported metric for continuous collection: $metric")
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error collecting data for metric: $metric")
                    _dataEvents.emit(DataEvent.DataError(deviceId, metric, e.message ?: "Unknown error"))
                }
                
                // Get collection interval for this metric
                val interval = getCollectionInterval(deviceId, metric)
                delay(interval)
            }
        } catch (e: CancellationException) {
            Timber.d("Data collection cancelled for metric: $metric")
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Data collection failed for metric: $metric")
        }
    }
    
    /**
     * Get collection interval for metric
     */
    private fun getCollectionInterval(deviceId: String, metric: SupportedMetric): Long {
        val device = connectedDevices[deviceId] ?: return DEFAULT_COLLECTION_INTERVAL
        
        // Get sampling rate from device capabilities
        val samplingRates = device.capabilities.samplingRates
        val samplingRate = samplingRates[metric]
        
        if (samplingRate != null && samplingRate > 0) {
            // Convert sampling rate (Hz) to interval (ms)
            return (1000 / samplingRate).toLong()
        }
        
        // Default intervals for different metrics
        return when (metric) {
            SupportedMetric.HEART_RATE -> 60_000L // 1 minute
            SupportedMetric.BLOOD_PRESSURE -> 300_000L // 5 minutes
            SupportedMetric.BLOOD_OXYGEN -> 300_000L // 5 minutes
            SupportedMetric.BODY_TEMPERATURE -> 300_000L // 5 minutes
            SupportedMetric.STRESS_LEVEL -> 300_000L // 5 minutes
            else -> DEFAULT_COLLECTION_INTERVAL
        }
    }
    
    /**
     * Check device connections
     */
    private suspend fun checkDeviceConnections() {
        for ((deviceId, device) in connectedDevices) {
            try {
                // Check if device is still connected
                val isConnected = hbandSdk.isDeviceConnected(deviceId)
                if (!isConnected) {
                    Timber.d("Device disconnected: $deviceId")
                    
                    // Stop data collection
                    stopDataCollection(deviceId, device.capabilities.supportedMetrics)
                    
                    // Remove from connected devices
                    connectedDevices.remove(deviceId)
                    
                    // Emit disconnected event
                    _deviceEvents.emit(DeviceEvent.DeviceDisconnected(deviceId, DisconnectReason.CONNECTION_LOST))
                    
                    // Start reconnection if needed
                    startReconnection(deviceId)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error checking device connection: $deviceId")
            }
        }
    }
    
    /**
     * Start reconnection
     */
    private fun startReconnection(deviceId: String) {
        // Check if we should attempt reconnection
        val attempts = reconnectionAttempts[deviceId] ?: 0
        if (attempts >= maxReconnectionAttempts) {
            Timber.d("Max reconnection attempts reached for device: $deviceId")
            return
        }
        
        // Create handler if needed
        if (!reconnectionHandlers.containsKey(deviceId)) {
            reconnectionHandlers[deviceId] = Handler(Looper.getMainLooper())
        }
        
        // Calculate delay based on attempt count (exponential backoff)
        val delay = calculateReconnectionDelay(attempts)
        
        // Schedule reconnection
        reconnectionHandlers[deviceId]?.postDelayed({
            deviceScope.launch {
                Timber.d("Attempting to reconnect to device: $deviceId (attempt ${attempts + 1})")
                
                try {
                    // Attempt to reconnect
                    val result = connectToDevice(deviceId)
                    
                    if (result is Result.Success) {
                        // Reset reconnection attempts
                        reconnectionAttempts[deviceId] = 0
                        Timber.d("Reconnected to device: $deviceId")
                    } else {
                        // Increment reconnection attempts
                        reconnectionAttempts[deviceId] = attempts + 1
                        
                        // Start next reconnection attempt
                        startReconnection(deviceId)
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error reconnecting to device: $deviceId")
                    
                    // Increment reconnection attempts
                    reconnectionAttempts[deviceId] = attempts + 1
                    
                    // Start next reconnection attempt
                    startReconnection(deviceId)
                }
            }
        }, delay)
    }
    
    /**
     * Calculate reconnection delay
     */
    private fun calculateReconnectionDelay(attempts: Int): Long {
        // Exponential backoff with jitter
        val baseDelay = 1000L // 1 second
        val maxDelay = 60000L // 1 minute
        
        val exponentialDelay = baseDelay * (1 shl attempts.coerceAtMost(10))
        val jitter = (Math.random() * 0.3 * exponentialDelay).toLong()
        
        return (exponentialDelay + jitter).coerceAtMost(maxDelay)
    }
    
    /**
     * Detect device capabilities
     */
    private suspend fun detectDeviceCapabilities(device: BluetoothDevice): DeviceCapability {
        Timber.d("Detecting capabilities for device: ${device.address}")
        
        // Check if we already have capabilities for this device
        val savedCapabilities = deviceCapabilityRepository.getDeviceCapability(device.address)
        if (savedCapabilities is Result.Success) {
            Timber.d("Using saved capabilities for device: ${device.address}")
            
            // Update battery level and firmware version
            val batteryLevel = hbandSdk.getBatteryLevel(device.address)
            val firmwareVersion = hbandSdk.getFirmwareVersion(device.address)
            
            return savedCapabilities.data.copy(
                batteryLevel = batteryLevel,
                firmwareVersion = firmwareVersion
            )
        }
        
        // Get device info
        val deviceInfo = hbandSdk.getDeviceInfo(device.address)
        
        // Detect supported metrics
        val supportedMetrics = mutableSetOf<SupportedMetric>()
        
        // Check for ET492 device specifically
        if (deviceInfo.model == "ET492") {
            // ET492 supports all metrics
            supportedMetrics.addAll(SupportedMetric.values())
        } else {
            // For other devices, check each metric individually
            if (hbandSdk.supportsHeartRate(device.address)) {
                supportedMetrics.add(SupportedMetric.HEART_RATE)
            }
            
            if (hbandSdk.supportsBloodPressure(device.address)) {
                supportedMetrics.add(SupportedMetric.BLOOD_PRESSURE)
            }
            
            if (hbandSdk.supportsBloodOxygen(device.address)) {
                supportedMetrics.add(SupportedMetric.BLOOD_OXYGEN)
            }
            
            if (hbandSdk.supportsTemperature(device.address)) {
                supportedMetrics.add(SupportedMetric.BODY_TEMPERATURE)
            }
            
            if (hbandSdk.supportsStressLevel(device.address)) {
                supportedMetrics.add(SupportedMetric.STRESS_LEVEL)
            }
            
            if (hbandSdk.supportsEcg(device.address)) {
                supportedMetrics.add(SupportedMetric.ECG)
            }
            
            if (hbandSdk.supportsBloodGlucose(device.address)) {
                supportedMetrics.add(SupportedMetric.BLOOD_GLUCOSE)
            }
            
            if (hbandSdk.supportsActivity(device.address)) {
                supportedMetrics.add(SupportedMetric.ACTIVITY)
            }
            
            if (hbandSdk.supportsSleep(device.address)) {
                supportedMetrics.add(SupportedMetric.SLEEP)
            }
        }
        
        // Get sampling rates
        val samplingRates = mutableMapOf<SupportedMetric, Int>()
        for (metric in supportedMetrics) {
            val rate = hbandSdk.getSamplingRate(device.address, metric)
            if (rate > 0) {
                samplingRates[metric] = rate
            }
        }
        
        // Get accuracy ratings
        val accuracyRatings = mutableMapOf<SupportedMetric, Float>()
        for (metric in supportedMetrics) {
            val accuracy = hbandSdk.getAccuracyRating(device.address, metric)
            if (accuracy > 0) {
                accuracyRatings[metric] = accuracy
            }
        }
        
        // Get battery level and firmware version
        val batteryLevel = hbandSdk.getBatteryLevel(device.address)
        val firmwareVersion = hbandSdk.getFirmwareVersion(device.address)
        
        // Create device capability
        return DeviceCapability(
            deviceId = device.address,
            deviceName = device.name ?: "Unknown",
            deviceModel = deviceInfo.model,
            supportedMetrics = supportedMetrics,
            samplingRates = samplingRates,
            accuracyRatings = accuracyRatings,
            batteryLevel = batteryLevel,
            firmwareVersion = firmwareVersion
        )
    }
    
    /**
     * HBand device wrapper
     */
    private data class HBandDevice(
        val device: BluetoothDevice,
        var capabilities: DeviceCapability,
        val connectionTime: LocalDateTime
    )
    
    companion object {
        private const val DEFAULT_COLLECTION_INTERVAL = 60_000L // 1 minute
    }
}

/**
 * HBand SDK Wrapper
 *
 * This is a mock implementation of the HBand SDK for demonstration purposes.
 * In a real application, this would be replaced with the actual SDK implementation.
 */
class HBandSdkWrapper(private val context: Context) {
    
    private val tag = "HBandSdkWrapper"
    
    // Mock connected devices
    private val connectedDevices = mutableMapOf<String, BluetoothDevice>()
    
    // Mock device info
    private val deviceInfo = mutableMapOf<String, DeviceInfo>()
    
    // Mock battery levels
    private val batteryLevels = mutableMapOf<String, Int>()
    
    // Mock firmware versions
    private val firmwareVersions = mutableMapOf<String, String>()
    
    // Mock discovery callback
    private var discoveryCallback: ((BluetoothDevice, Int) -> Unit)? = null
    
    /**
     * Start discovery
     */
    fun startDiscovery(callback: (BluetoothDevice, Int) -> Unit) {
        Log.d(tag, "Starting discovery")
        discoveryCallback = callback
        
        // In a real implementation, this would start Bluetooth discovery
        // For now, we'll just simulate finding the ET492 device
        
        // Get Bluetooth adapter
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        
        // Get paired devices
        val pairedDevices = bluetoothAdapter?.bondedDevices ?: emptySet()
        
        // Simulate finding devices
        for (device in pairedDevices) {
            // Simulate RSSI
            val rssi = (-60..(-40)).random()
            
            // Call discovery callback
            discoveryCallback?.invoke(device, rssi)
        }
    }
    
    /**
     * Stop discovery
     */
    fun stopDiscovery() {
        Log.d(tag, "Stopping discovery")
        discoveryCallback = null
        
        // In a real implementation, this would stop Bluetooth discovery
    }
    
    /**
     * Connect to device
     */
    fun connectToDevice(deviceId: String): BluetoothDevice? {
        Log.d(tag, "Connecting to device: $deviceId")
        
        // Get Bluetooth adapter
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        
        // Get device
        val device = bluetoothAdapter?.getRemoteDevice(deviceId)
        if (device == null) {
            Log.e(tag, "Device not found: $deviceId")
            return null
        }
        
        // Simulate connection
        connectedDevices[deviceId] = device
        
        // Initialize device info
        if (!deviceInfo.containsKey(deviceId)) {
            // Check if device name contains "ET492"
            val isET492 = device.name?.contains("ET492", ignoreCase = true) ?: false
            
            deviceInfo[deviceId] = DeviceInfo(
                model = if (isET492) "ET492" else "HBand-${deviceId.takeLast(4)}",
                manufacturer = "HBand",
                serialNumber = "SN-${deviceId.replace(":", "").takeLast(8)}"
            )
        }
        
        // Initialize battery level
        if (!batteryLevels.containsKey(deviceId)) {
            batteryLevels[deviceId] = (60..95).random()
        }
        
        // Initialize firmware version
        if (!firmwareVersions.containsKey(deviceId)) {
            firmwareVersions[deviceId] = "v${(1..3).random()}.${(0..9).random()}.${(0..9).random()}"
        }
        
        return device
    }
    
    /**
     * Disconnect device
     */
    fun disconnectDevice(deviceId: String) {
        Log.d(tag, "Disconnecting device: $deviceId")
        connectedDevices.remove(deviceId)
    }
    
    /**
     * Is device connected
     */
    fun isDeviceConnected(deviceId: String): Boolean {
        return connectedDevices.containsKey(deviceId)
    }
    
    /**
     * Get paired devices
     */
    fun getPairedDevices(): Set<BluetoothDevice> {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        
        return bluetoothAdapter?.bondedDevices ?: emptySet()
    }
    
    /**
     * Get device info
     */
    fun getDeviceInfo(deviceId: String): DeviceInfo {
        return deviceInfo[deviceId] ?: DeviceInfo(
            model = "Unknown",
            manufacturer = "Unknown",
            serialNumber = "Unknown"
        )
    }
    
    /**
     * Get battery level
     */
    fun getBatteryLevel(deviceId: String): Int? {
        if (!isDeviceConnected(deviceId)) {
            return null
        }
        
        // Simulate battery drain
        val currentLevel = batteryLevels[deviceId] ?: return null
        val newLevel = (currentLevel - (0..1).random()).coerceAtLeast(5)
        batteryLevels[deviceId] = newLevel
        
        return newLevel
    }
    
    /**
     * Get firmware version
     */
    fun getFirmwareVersion(deviceId: String): String? {
        if (!isDeviceConnected(deviceId)) {
            return null
        }
        
        return firmwareVersions[deviceId]
    }
    
    /**
     * Update firmware
     */
    fun updateFirmware(deviceId: String, firmwareUrl: String): Boolean {
        if (!isDeviceConnected(deviceId)) {
            return false
        }
        
        // Simulate firmware update
        firmwareVersions[deviceId] = "v${(1..3).random()}.${(0..9).random()}.${(0..9).random()}"
        
        return true
    }
    
    /**
     * Measure heart rate
     */
    fun measureHeartRate(deviceId: String): HeartRateData? {
        if (!isDeviceConnected(deviceId)) {
            return null
        }
        
        // Simulate heart rate measurement
        return HeartRateData(
            value = (60..100).random(),
            timestamp = System.currentTimeMillis(),
            confidence = (0.8f..1.0f).random().toFloat()
        )
    }
    
    /**
     * Measure blood pressure
     */
    fun measureBloodPressure(deviceId: String): BloodPressureData? {
        if (!isDeviceConnected(deviceId)) {
            return null
        }
        
        // Simulate blood pressure measurement
        val systolic = (110..140).random()
        val diastolic = (70..90).random()
        
        return BloodPressureData(
            systolic = systolic,
            diastolic = diastolic,
            pulse = (60..100).random(),
            timestamp = System.currentTimeMillis()
        )
    }
    
    /**
     * Measure blood oxygen
     */
    fun measureBloodOxygen(deviceId: String): BloodOxygenData? {
        if (!isDeviceConnected(deviceId)) {
            return null
        }
        
        // Simulate blood oxygen measurement
        return BloodOxygenData(
            value = (94..99).random(),
            timestamp = System.currentTimeMillis(),
            confidence = (0.8f..1.0f).random().toFloat(),
            pulseRate = (60..100).random()
        )
    }
    
    /**
     * Measure body temperature
     */
    fun measureBodyTemperature(deviceId: String): TemperatureData? {
        if (!isDeviceConnected(deviceId)) {
            return null
        }
        
        // Simulate temperature measurement
        return TemperatureData(
            value = (36.0f..37.5f).random().toFloat(),
            timestamp = System.currentTimeMillis(),
            site = TemperatureMeasurementSite.values().random()
        )
    }
    
    /**
     * Measure stress level
     */
    fun measureStressLevel(deviceId: String): StressData? {
        if (!isDeviceConnected(deviceId)) {
            return null
        }
        
        // Simulate stress level measurement
        return StressData(
            value = (10..70).random(),
            timestamp = System.currentTimeMillis(),
            hrvValue = (20.0..80.0).random()
        )
    }
    
    /**
     * Record ECG
     */
    fun recordEcg(deviceId: String, durationSeconds: Int): EcgData? {
        if (!isDeviceConnected(deviceId)) {
            return null
        }
        
        // Simulate ECG recording
        val samplingRate = 250 // Hz
        val totalSamples = samplingRate * durationSeconds
        val waveformData = mutableListOf<Float>()
        
        // Generate simulated ECG waveform
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / samplingRate
            val