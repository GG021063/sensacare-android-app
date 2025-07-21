package com.sensacare.app.presentation.device

import android.bluetooth.BluetoothAdapter
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sensacare.app.domain.model.*
import com.sensacare.app.domain.repository.*
import com.sensacare.app.domain.usecase.device.ConnectDeviceUseCase
import com.sensacare.app.domain.usecase.device.DuplicateHandling
import com.sensacare.app.domain.usecase.device.SyncDeviceDataUseCase
import com.sensacare.app.domain.usecase.device.SyncParams
import com.sensacare.app.domain.usecase.device.SyncProgress
import com.sensacare.app.domain.usecase.device.SyncType
import com.sensacare.app.util.PermissionChecker
import com.sensacare.app.util.SingleLiveEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * DeviceManagementViewModel - Manages device discovery, connection, and synchronization
 *
 * This ViewModel implements MVVM architecture with reactive UI state management for
 * all device-related operations in the SensaCare app:
 * 
 * Key features:
 * - Device discovery and scanning with Bluetooth management
 * - Device connection, pairing, and status monitoring
 * - Comprehensive sync progress tracking with detailed states
 * - Multi-device support with concurrent operations
 * - Battery level and feature monitoring
 * - Error handling with user-friendly messages
 * - Background operations with proper lifecycle management
 * - Permission handling for Bluetooth access
 * - Efficient coroutine scope management
 */
@HiltViewModel
class DeviceManagementViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val connectDeviceUseCase: ConnectDeviceUseCase,
    private val syncDeviceUseCase: SyncDeviceDataUseCase,
    private val permissionChecker: PermissionChecker,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow<DeviceManagementUiState>(DeviceManagementUiState.Loading)
    val uiState: StateFlow<DeviceManagementUiState> = _uiState.asStateFlow()
    
    // Device scanning state
    private val _scanningState = MutableStateFlow<ScanningState>(ScanningState.Idle)
    val scanningState: StateFlow<ScanningState> = _scanningState.asStateFlow()
    
    // Connection state
    private val _connectionState = MutableStateFlow<Map<Long, DeviceConnectionState>>(emptyMap())
    val connectionState: StateFlow<Map<Long, DeviceConnectionState>> = _connectionState.asStateFlow()
    
    // Sync state
    private val _syncState = MutableStateFlow<Map<Long, SyncState>>(emptyMap())
    val syncState: StateFlow<Map<Long, SyncState>> = _syncState.asStateFlow()
    
    // Device details
    private val _deviceDetails = MutableStateFlow<Map<Long, DeviceDetails>>(emptyMap())
    val deviceDetails: StateFlow<Map<Long, DeviceDetails>> = _deviceDetails.asStateFlow()
    
    // Error events
    private val _errorEvents = MutableSharedFlow<DeviceErrorEvent>()
    val errorEvents: SharedFlow<DeviceErrorEvent> = _errorEvents.asSharedFlow()
    
    // Permission events
    private val _permissionEvents = MutableSharedFlow<PermissionEvent>()
    val permissionEvents: SharedFlow<PermissionEvent> = _permissionEvents.asSharedFlow()
    
    // Single event for navigation
    val navigationEvent = SingleLiveEvent<NavigationEvent>()
    
    // Jobs for cancellable operations
    private var scanningJob: Job? = null
    private val connectionJobs = mutableMapOf<Long, Job>()
    private val syncJobs = mutableMapOf<Long, Job>()
    private var monitoringJob: Job? = null
    
    // Initialize
    init {
        Timber.d("DeviceManagementViewModel initialized")
        loadSavedDevices()
        startDeviceMonitoring()
    }
    
    /**
     * Load saved devices from repository
     */
    private fun loadSavedDevices() {
        viewModelScope.launch {
            _uiState.value = DeviceManagementUiState.Loading
            
            try {
                deviceRepository.getAllDevices().collect { devices ->
                    if (devices.isEmpty()) {
                        _uiState.value = DeviceManagementUiState.Empty
                    } else {
                        _uiState.value = DeviceManagementUiState.DeviceList(devices)
                        
                        // Initialize connection states
                        val connectionStates = devices.associate { device ->
                            device.id to if (device.isConnected) 
                                DeviceConnectionState.CONNECTED 
                            else 
                                DeviceConnectionState.DISCONNECTED
                        }
                        _connectionState.value = connectionStates
                        
                        // Initialize device details
                        val details = devices.associate { device ->
                            device.id to DeviceDetails(
                                batteryLevel = device.batteryLevel ?: 0,
                                firmwareVersion = device.firmwareVersion ?: "Unknown",
                                lastSyncTime = device.lastSyncTime,
                                features = device.features
                            )
                        }
                        _deviceDetails.value = details
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading saved devices")
                _uiState.value = DeviceManagementUiState.Error("Failed to load devices: ${e.message}")
                _errorEvents.emit(DeviceErrorEvent.LoadingError("Failed to load devices", e))
            }
        }
    }
    
    /**
     * Start scanning for devices
     */
    fun startDeviceScanning() {
        viewModelScope.launch {
            // Check Bluetooth permissions
            if (!permissionChecker.hasBluetoothPermissions()) {
                _permissionEvents.emit(PermissionEvent.BluetoothPermissionsNeeded)
                return@launch
            }
            
            // Check if Bluetooth is enabled
            if (!permissionChecker.isBluetoothEnabled()) {
                _permissionEvents.emit(PermissionEvent.BluetoothDisabled)
                return@launch
            }
            
            // Cancel any existing scanning
            scanningJob?.cancel()
            
            // Update state
            _scanningState.value = ScanningState.Scanning(emptyList())
            
            scanningJob = viewModelScope.launch {
                try {
                    // Start scanning with timeout
                    val scanTimeoutMs = 30000L // 30 seconds
                    val scanTimeout = System.currentTimeMillis() + scanTimeoutMs
                    
                    deviceRepository.startDeviceDiscovery().collect { discoveryResult ->
                        when (discoveryResult) {
                            is DeviceDiscoveryResult.DeviceFound -> {
                                val currentDevices = (_scanningState.value as? ScanningState.Scanning)?.discoveredDevices ?: emptyList()
                                val updatedDevices = currentDevices.toMutableList()
                                
                                // Check if device already in list
                                val existingIndex = updatedDevices.indexOfFirst { it.macAddress == discoveryResult.device.macAddress }
                                if (existingIndex >= 0) {
                                    // Update existing device
                                    updatedDevices[existingIndex] = discoveryResult.device
                                } else {
                                    // Add new device
                                    updatedDevices.add(discoveryResult.device)
                                }
                                
                                _scanningState.value = ScanningState.Scanning(updatedDevices)
                            }
                            is DeviceDiscoveryResult.ScanningCompleted -> {
                                _scanningState.value = ScanningState.Completed(
                                    discoveryResult.devices,
                                    discoveryResult.devices.isEmpty()
                                )
                                break
                            }
                            is DeviceDiscoveryResult.Error -> {
                                _scanningState.value = ScanningState.Error(discoveryResult.error.message)
                                _errorEvents.emit(DeviceErrorEvent.ScanningError(discoveryResult.error.message))
                                break
                            }
                        }
                        
                        // Check for timeout
                        if (System.currentTimeMillis() > scanTimeout) {
                            val currentDevices = (_scanningState.value as? ScanningState.Scanning)?.discoveredDevices ?: emptyList()
                            _scanningState.value = ScanningState.Completed(currentDevices, currentDevices.isEmpty())
                            break
                        }
                    }
                } catch (e: CancellationException) {
                    // Scanning was cancelled, update state
                    val currentDevices = (_scanningState.value as? ScanningState.Scanning)?.discoveredDevices ?: emptyList()
                    _scanningState.value = ScanningState.Completed(currentDevices, false)
                    throw e // Rethrow to respect cancellation
                } catch (e: Exception) {
                    Timber.e(e, "Error during device scanning")
                    _scanningState.value = ScanningState.Error("Scanning failed: ${e.message}")
                    _errorEvents.emit(DeviceErrorEvent.ScanningError("Scanning failed: ${e.message}"))
                }
            }
        }
    }
    
    /**
     * Stop scanning for devices
     */
    fun stopDeviceScanning() {
        scanningJob?.cancel()
        scanningJob = null
        
        val currentDevices = (_scanningState.value as? ScanningState.Scanning)?.discoveredDevices ?: emptyList()
        _scanningState.value = ScanningState.Completed(currentDevices, false)
        
        viewModelScope.launch {
            deviceRepository.stopDeviceDiscovery()
        }
    }
    
    /**
     * Pair and save a discovered device
     */
    fun pairDevice(device: Device) {
        viewModelScope.launch {
            try {
                // Update connection state
                updateConnectionState(device.id, DeviceConnectionState.PAIRING)
                
                // Attempt to pair
                val result = deviceRepository.pairDevice(device)
                
                when (result) {
                    is DeviceOperationResult.Success -> {
                        // Device paired successfully
                        val pairedDevice = result.data
                        
                        // Save device
                        val savedResult = deviceRepository.saveDevice(pairedDevice)
                        
                        when (savedResult) {
                            is DeviceOperationResult.Success -> {
                                // Update UI state to include the new device
                                loadSavedDevices()
                                
                                // Update connection state
                                updateConnectionState(pairedDevice.id, DeviceConnectionState.PAIRED)
                                
                                // Show success message
                                _errorEvents.emit(DeviceErrorEvent.Success("Device paired successfully"))
                            }
                            is DeviceOperationResult.Error -> {
                                updateConnectionState(device.id, DeviceConnectionState.PAIRING_FAILED)
                                _errorEvents.emit(DeviceErrorEvent.PairingError(
                                    "Failed to save paired device: ${savedResult.error.message}"
                                ))
                            }
                            else -> { /* Ignore loading state */ }
                        }
                    }
                    is DeviceOperationResult.Error -> {
                        updateConnectionState(device.id, DeviceConnectionState.PAIRING_FAILED)
                        _errorEvents.emit(DeviceErrorEvent.PairingError(
                            "Failed to pair device: ${result.error.message}"
                        ))
                    }
                    else -> { /* Ignore loading state */ }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error pairing device")
                updateConnectionState(device.id, DeviceConnectionState.PAIRING_FAILED)
                _errorEvents.emit(DeviceErrorEvent.PairingError(
                    "Failed to pair device: ${e.message}"
                ))
            }
        }
    }
    
    /**
     * Connect to a device
     */
    fun connectToDevice(deviceId: Long) {
        // Cancel any existing connection job for this device
        connectionJobs[deviceId]?.cancel()
        
        connectionJobs[deviceId] = viewModelScope.launch {
            try {
                // Update connection state
                updateConnectionState(deviceId, DeviceConnectionState.CONNECTING)
                
                // Use the ConnectDeviceUseCase
                connectDeviceUseCase(
                    deviceId = deviceId,
                    autoRetry = true,
                    autoPair = true
                ).collect { result ->
                    when (result) {
                        is DeviceOperationResult.Loading -> {
                            // Connection in progress
                            updateConnectionState(deviceId, DeviceConnectionState.CONNECTING)
                        }
                        is DeviceOperationResult.Success -> {
                            // Connection successful
                            updateConnectionState(deviceId, DeviceConnectionState.CONNECTED)
                            
                            // Update device details
                            updateDeviceDetails(result.data)
                            
                            // Show success message
                            _errorEvents.emit(DeviceErrorEvent.Success("Connected to ${result.data.name}"))
                            
                            // Update UI state
                            updateUiStateAfterConnection(result.data)
                        }
                        is DeviceOperationResult.Error -> {
                            // Connection failed
                            updateConnectionState(deviceId, DeviceConnectionState.CONNECTION_FAILED)
                            
                            // Show error message
                            _errorEvents.emit(DeviceErrorEvent.ConnectionError(
                                "Failed to connect: ${result.error.message}",
                                result.error
                            ))
                        }
                    }
                }
            } catch (e: CancellationException) {
                // Connection was cancelled
                updateConnectionState(deviceId, DeviceConnectionState.DISCONNECTED)
                throw e // Rethrow to respect cancellation
            } catch (e: Exception) {
                Timber.e(e, "Error connecting to device")
                updateConnectionState(deviceId, DeviceConnectionState.CONNECTION_FAILED)
                _errorEvents.emit(DeviceErrorEvent.ConnectionError(
                    "Failed to connect: ${e.message}",
                    DeviceError.UnknownError(e.message ?: "Unknown error", e)
                ))
            }
        }
    }
    
    /**
     * Disconnect from a device
     */
    fun disconnectFromDevice(deviceId: Long) {
        // Cancel any existing connection job for this device
        connectionJobs[deviceId]?.cancel()
        connectionJobs.remove(deviceId)
        
        // Cancel any existing sync job for this device
        syncJobs[deviceId]?.cancel()
        syncJobs.remove(deviceId)
        
        viewModelScope.launch {
            try {
                // Update connection state
                updateConnectionState(deviceId, DeviceConnectionState.DISCONNECTING)
                
                // Disconnect from device
                val result = deviceRepository.disconnectFromDevice(deviceId)
                
                when (result) {
                    is DeviceOperationResult.Success -> {
                        // Disconnection successful
                        updateConnectionState(deviceId, DeviceConnectionState.DISCONNECTED)
                        
                        // Show success message
                        _errorEvents.emit(DeviceErrorEvent.Success("Disconnected from device"))
                        
                        // Update UI state
                        updateUiStateAfterDisconnection(result.data)
                    }
                    is DeviceOperationResult.Error -> {
                        // Disconnection failed
                        _errorEvents.emit(DeviceErrorEvent.DisconnectionError(
                            "Failed to disconnect: ${result.error.message}"
                        ))
                        
                        // Force disconnect state anyway
                        updateConnectionState(deviceId, DeviceConnectionState.DISCONNECTED)
                    }
                    else -> { /* Ignore loading state */ }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error disconnecting from device")
                _errorEvents.emit(DeviceErrorEvent.DisconnectionError(
                    "Failed to disconnect: ${e.message}"
                ))
                
                // Force disconnect state anyway
                updateConnectionState(deviceId, DeviceConnectionState.DISCONNECTED)
            }
        }
    }
    
    /**
     * Sync data from a device
     */
    fun syncDeviceData(
        deviceId: Long,
        syncType: SyncType = SyncType.INCREMENTAL,
        dataTypes: List<DeviceDataType> = DeviceDataType.values().filter { it != DeviceDataType.ALL },
        handleDuplicates: DuplicateHandling = DuplicateHandling.SKIP
    ) {
        // Cancel any existing sync job for this device
        syncJobs[deviceId]?.cancel()
        
        // Check if device is connected
        if (_connectionState.value[deviceId] != DeviceConnectionState.CONNECTED) {
            viewModelScope.launch {
                _errorEvents.emit(DeviceErrorEvent.SyncError(
                    "Cannot sync: Device is not connected",
                    deviceId
                ))
                
                // Update sync state
                updateSyncState(deviceId, SyncState.Error("Device not connected"))
            }
            return
        }
        
        // Create sync parameters
        val syncParams = SyncParams(
            deviceId = deviceId,
            syncType = syncType,
            dataTypes = dataTypes,
            handleDuplicates = handleDuplicates,
            disconnectAfterSync = false, // Don't disconnect automatically
            forceSync = syncType == SyncType.FULL // Force sync for full sync
        )
        
        // Start sync job
        syncJobs[deviceId] = viewModelScope.launch {
            try {
                // Update sync state
                updateSyncState(deviceId, SyncState.Syncing(0, emptyMap()))
                
                // Use the SyncDeviceDataUseCase
                syncDeviceUseCase(syncParams).collect { progress ->
                    when (progress) {
                        is SyncProgress.Started -> {
                            updateSyncState(deviceId, SyncState.Started(progress.syncType, progress.dataTypes))
                        }
                        is SyncProgress.Connecting -> {
                            updateSyncState(deviceId, SyncState.Connecting)
                        }
                        is SyncProgress.Connected -> {
                            updateSyncState(deviceId, SyncState.Connected)
                        }
                        is SyncProgress.DataTypeSyncStarted -> {
                            val currentState = _syncState.value[deviceId] as? SyncState.Syncing
                            val dataTypeProgress = currentState?.dataTypeProgress?.toMutableMap() ?: mutableMapOf()
                            dataTypeProgress[progress.dataType] = 0
                            updateSyncState(deviceId, SyncState.Syncing(currentState?.overallProgress ?: 0, dataTypeProgress))
                        }
                        is SyncProgress.DataTypeSyncProgress -> {
                            val currentState = _syncState.value[deviceId] as? SyncState.Syncing
                            val dataTypeProgress = currentState?.dataTypeProgress?.toMutableMap() ?: mutableMapOf()
                            dataTypeProgress[progress.dataType] = progress.progress
                            updateSyncState(deviceId, SyncState.Syncing(currentState?.overallProgress ?: 0, dataTypeProgress))
                        }
                        is SyncProgress.DataTypeSyncCompleted -> {
                            val currentState = _syncState.value[deviceId] as? SyncState.Syncing
                            val dataTypeProgress = currentState?.dataTypeProgress?.toMutableMap() ?: mutableMapOf()
                            dataTypeProgress[progress.dataType] = 100
                            updateSyncState(deviceId, SyncState.Syncing(currentState?.overallProgress ?: 0, dataTypeProgress))
                        }
                        is SyncProgress.DataTypeSyncFailed -> {
                            val currentState = _syncState.value[deviceId] as? SyncState.Syncing
                            val dataTypeProgress = currentState?.dataTypeProgress?.toMutableMap() ?: mutableMapOf()
                            dataTypeProgress[progress.dataType] = -1 // -1 indicates failure
                            updateSyncState(deviceId, SyncState.Syncing(currentState?.overallProgress ?: 0, dataTypeProgress))
                        }
                        is SyncProgress.DataTypeSyncRetrying -> {
                            val currentState = _syncState.value[deviceId] as? SyncState.Syncing
                            val dataTypeProgress = currentState?.dataTypeProgress?.toMutableMap() ?: mutableMapOf()
                            // Keep the current progress but update the state message
                            updateSyncState(deviceId, SyncState.Syncing(
                                currentState?.overallProgress ?: 0,
                                dataTypeProgress,
                                "Retrying ${progress.dataType.name.lowercase()} (${progress.attempt}/${progress.maxAttempts})"
                            ))
                        }
                        is SyncProgress.DataTypeSkipped -> {
                            val currentState = _syncState.value[deviceId] as? SyncState.Syncing
                            val dataTypeProgress = currentState?.dataTypeProgress?.toMutableMap() ?: mutableMapOf()
                            dataTypeProgress[progress.dataType] = -2 // -2 indicates skipped
                            updateSyncState(deviceId, SyncState.Syncing(currentState?.overallProgress ?: 0, dataTypeProgress))
                        }
                        is SyncProgress.OverallProgress -> {
                            val currentState = _syncState.value[deviceId] as? SyncState.Syncing
                            updateSyncState(deviceId, SyncState.Syncing(
                                progress.progress,
                                currentState?.dataTypeProgress ?: emptyMap()
                            ))
                        }
                        is SyncProgress.Disconnecting -> {
                            updateSyncState(deviceId, SyncState.Disconnecting)
                        }
                        is SyncProgress.Disconnected -> {
                            updateSyncState(deviceId, SyncState.Disconnected)
                        }
                        is SyncProgress.Completed -> {
                            // Sync completed successfully
                            updateSyncState(deviceId, SyncState.Completed(progress.summary))
                            
                            // Update device last sync time
                            updateDeviceLastSyncTime(deviceId, LocalDateTime.now())
                            
                            // Show success message
                            _errorEvents.emit(DeviceErrorEvent.Success(
                                "Sync completed: ${progress.summary.totalItemsSynced} items synced"
                            ))
                        }
                        is SyncProgress.Error -> {
                            // Sync failed
                            updateSyncState(deviceId, SyncState.Error(progress.error.message))
                            
                            // Show error message
                            _errorEvents.emit(DeviceErrorEvent.SyncError(
                                "Sync failed: ${progress.error.message}",
                                deviceId
                            ))
                        }
                    }
                }
            } catch (e: CancellationException) {
                // Sync was cancelled
                updateSyncState(deviceId, SyncState.Cancelled)
                throw e // Rethrow to respect cancellation
            } catch (e: Exception) {
                Timber.e(e, "Error syncing device data")
                updateSyncState(deviceId, SyncState.Error("Sync failed: ${e.message}"))
                _errorEvents.emit(DeviceErrorEvent.SyncError(
                    "Sync failed: ${e.message}",
                    deviceId
                ))
            }
        }
    }
    
    /**
     * Cancel a sync operation
     */
    fun cancelSync(deviceId: Long) {
        syncJobs[deviceId]?.cancel()
        syncJobs.remove(deviceId)
        
        updateSyncState(deviceId, SyncState.Cancelled)
    }
    
    /**
     * Forget a device (unpair and delete)
     */
    fun forgetDevice(deviceId: Long) {
        // Cancel any existing jobs for this device
        connectionJobs[deviceId]?.cancel()
        connectionJobs.remove(deviceId)
        
        syncJobs[deviceId]?.cancel()
        syncJobs.remove(deviceId)
        
        viewModelScope.launch {
            try {
                // Disconnect if connected
                if (_connectionState.value[deviceId] == DeviceConnectionState.CONNECTED) {
                    deviceRepository.disconnectFromDevice(deviceId)
                }
                
                // Unpair device
                deviceRepository.unpairDevice(deviceId)
                
                // Delete device
                val result = deviceRepository.deleteDevice(deviceId)
                
                when (result) {
                    is DeviceOperationResult.Success -> {
                        // Remove from connection state
                        val updatedConnectionState = _connectionState.value.toMutableMap()
                        updatedConnectionState.remove(deviceId)
                        _connectionState.value = updatedConnectionState
                        
                        // Remove from sync state
                        val updatedSyncState = _syncState.value.toMutableMap()
                        updatedSyncState.remove(deviceId)
                        _syncState.value = updatedSyncState
                        
                        // Remove from device details
                        val updatedDeviceDetails = _deviceDetails.value.toMutableMap()
                        updatedDeviceDetails.remove(deviceId)
                        _deviceDetails.value = updatedDeviceDetails
                        
                        // Reload device list
                        loadSavedDevices()
                        
                        // Show success message
                        _errorEvents.emit(DeviceErrorEvent.Success("Device removed successfully"))
                    }
                    is DeviceOperationResult.Error -> {
                        _errorEvents.emit(DeviceErrorEvent.DeviceOperationError(
                            "Failed to remove device: ${result.error.message}"
                        ))
                    }
                    else -> { /* Ignore loading state */ }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error forgetting device")
                _errorEvents.emit(DeviceErrorEvent.DeviceOperationError(
                    "Failed to remove device: ${e.message}"
                ))
            }
        }
    }
    
    /**
     * Update device settings
     */
    fun updateDeviceSettings(deviceId: Long, settings: DeviceSettings) {
        viewModelScope.launch {
            try {
                // Update settings
                val result = deviceRepository.updateDeviceSettings(deviceId, settings)
                
                when (result) {
                    is DeviceOperationResult.Success -> {
                        // Show success message
                        _errorEvents.emit(DeviceErrorEvent.Success("Device settings updated"))
                    }
                    is DeviceOperationResult.Error -> {
                        _errorEvents.emit(DeviceErrorEvent.DeviceOperationError(
                            "Failed to update settings: ${result.error.message}"
                        ))
                    }
                    else -> { /* Ignore loading state */ }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error updating device settings")
                _errorEvents.emit(DeviceErrorEvent.DeviceOperationError(
                    "Failed to update settings: ${e.message}"
                ))
            }
        }
    }
    
    /**
     * Refresh device battery level
     */
    fun refreshBatteryLevel(deviceId: Long) {
        viewModelScope.launch {
            try {
                // Check if device is connected
                if (_connectionState.value[deviceId] != DeviceConnectionState.CONNECTED) {
                    _errorEvents.emit(DeviceErrorEvent.DeviceOperationError(
                        "Cannot refresh battery: Device not connected"
                    ))
                    return@launch
                }
                
                // Get battery level
                val result = deviceRepository.getDeviceBatteryLevel(deviceId)
                
                when (result) {
                    is DeviceOperationResult.Success -> {
                        // Update device details
                        val currentDetails = _deviceDetails.value[deviceId] ?: DeviceDetails()
                        val updatedDetails = currentDetails.copy(batteryLevel = result.data)
                        
                        val updatedDeviceDetails = _deviceDetails.value.toMutableMap()
                        updatedDeviceDetails[deviceId] = updatedDetails
                        _deviceDetails.value = updatedDeviceDetails
                    }
                    is DeviceOperationResult.Error -> {
                        _errorEvents.emit(DeviceErrorEvent.DeviceOperationError(
                            "Failed to get battery level: ${result.error.message}"
                        ))
                    }
                    else -> { /* Ignore loading state */ }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error refreshing battery level")
                _errorEvents.emit(DeviceErrorEvent.DeviceOperationError(
                    "Failed to get battery level: ${e.message}"
                ))
            }
        }
    }
    
    /**
     * Check if Bluetooth permissions are granted
     */
    fun checkBluetoothPermissions(): Boolean {
        return permissionChecker.hasBluetoothPermissions()
    }
    
    /**
     * Check if Bluetooth is enabled
     */
    fun isBluetoothEnabled(): Boolean {
        return permissionChecker.isBluetoothEnabled()
    }
    
    /**
     * Request to enable Bluetooth
     */
    fun requestEnableBluetooth() {
        viewModelScope.launch {
            _permissionEvents.emit(PermissionEvent.BluetoothEnableRequest)
        }
    }
    
    /**
     * Request Bluetooth permissions
     */
    fun requestBluetoothPermissions() {
        viewModelScope.launch {
            _permissionEvents.emit(PermissionEvent.BluetoothPermissionsNeeded)
        }
    }
    
    /**
     * Navigate to device details screen
     */
    fun navigateToDeviceDetails(deviceId: Long) {
        navigationEvent.value = NavigationEvent.ToDeviceDetails(deviceId)
    }
    
    /**
     * Navigate to sync history screen
     */
    fun navigateToSyncHistory(deviceId: Long) {
        navigationEvent.value = NavigationEvent.ToSyncHistory(deviceId)
    }
    
    /**
     * Start monitoring devices in the background
     */
    private fun startDeviceMonitoring() {
        // Cancel any existing monitoring job
        monitoringJob?.cancel()
        
        monitoringJob = viewModelScope.launch {
            try {
                // Monitor connection states
                deviceRepository.getAllDeviceConnectionStates().collect { states ->
                    _connectionState.value = states
                    
                    // Update UI state if needed
                    updateUiStateForConnectionChanges(states)
                }
            } catch (e: CancellationException) {
                throw e // Rethrow to respect cancellation
            } catch (e: Exception) {
                Timber.e(e, "Error monitoring devices")
            }
        }
    }
    
    /**
     * Update connection state for a device
     */
    private fun updateConnectionState(deviceId: Long, state: DeviceConnectionState) {
        val updatedConnectionState = _connectionState.value.toMutableMap()
        updatedConnectionState[deviceId] = state
        _connectionState.value = updatedConnectionState
    }
    
    /**
     * Update sync state for a device
     */
    private fun updateSyncState(deviceId: Long, state: SyncState) {
        val updatedSyncState = _syncState.value.toMutableMap()
        updatedSyncState[deviceId] = state
        _syncState.value = updatedSyncState
    }
    
    /**
     * Update device details
     */
    private fun updateDeviceDetails(device: Device) {
        val currentDetails = _deviceDetails.value[device.id] ?: DeviceDetails()
        
        val updatedDetails = currentDetails.copy(
            batteryLevel = device.batteryLevel ?: currentDetails.batteryLevel,
            firmwareVersion = device.firmwareVersion ?: currentDetails.firmwareVersion,
            lastSyncTime = device.lastSyncTime ?: currentDetails.lastSyncTime,
            features = device.features
        )
        
        val updatedDeviceDetails = _deviceDetails.value.toMutableMap()
        updatedDeviceDetails[device.id] = updatedDetails
        _deviceDetails.value = updatedDeviceDetails
    }
    
    /**
     * Update device last sync time
     */
    private fun updateDeviceLastSyncTime(deviceId: Long, timestamp: LocalDateTime) {
        viewModelScope.launch {
            try {
                // Get current device
                val device = when (val uiState = _uiState.value) {
                    is DeviceManagementUiState.DeviceList -> {
                        uiState.devices.find { it.id == deviceId }
                    }
                    else -> null
                } ?: return@launch
                
                // Update device with new sync time
                val updatedDevice = device.copy(lastSyncTime = timestamp)
                deviceRepository.updateDevice(updatedDevice)
                
                // Update device details
                val currentDetails = _deviceDetails.value[deviceId] ?: DeviceDetails()
                val updatedDetails = currentDetails.copy(lastSyncTime = timestamp)
                
                val updatedDeviceDetails = _deviceDetails.value.toMutableMap()
                updatedDeviceDetails[deviceId] = updatedDetails
                _deviceDetails.value = updatedDeviceDetails
            } catch (e: Exception) {
                Timber.e(e, "Error updating device sync time")
            }
        }
    }
    
    /**
     * Update UI state after connection
     */
    private fun updateUiStateAfterConnection(device: Device) {
        when (val currentState = _uiState.value) {
            is DeviceManagementUiState.DeviceList -> {
                // Update the device in the list
                val updatedDevices = currentState.devices.map {
                    if (it.id == device.id) device else it
                }
                _uiState.value = DeviceManagementUiState.DeviceList(updatedDevices)
            }
            is DeviceManagementUiState.DeviceDetail -> {
                if (currentState.device.id == device.id) {
                    // Update the device details
                    _uiState.value = DeviceManagementUiState.DeviceDetail(device)
                }
            }
            else -> {
                // No need to update other states
            }
        }
    }
    
    /**
     * Update UI state after disconnection
     */
    private fun updateUiStateAfterDisconnection(device: Device) {
        when (val currentState = _uiState.value) {
            is DeviceManagementUiState.DeviceList -> {
                // Update the device in the list
                val updatedDevices = currentState.devices.map {
                    if (it.id == device.id) device else it
                }
                _uiState.value = DeviceManagementUiState.DeviceList(updatedDevices)
            }
            is DeviceManagementUiState.DeviceDetail -> {
                if (currentState.device.id == device.id) {
                    // Update the device details
                    _uiState.value = DeviceManagementUiState.DeviceDetail(device)
                }
            }
            else -> {
                // No need to update other states
            }
        }
    }
    
    /**
     * Update UI state for connection changes
     */
    private fun updateUiStateForConnectionChanges(connectionStates: Map<Long, DeviceConnectionState>) {
        when (val currentState = _uiState.value) {
            is DeviceManagementUiState.DeviceList -> {
                // No need to update the list, connection states are shown separately
            }
            is DeviceManagementUiState.DeviceDetail -> {
                // If we're showing device details, update the connection state
                val deviceId = currentState.device.id
                val connectionState = connectionStates[deviceId]
                
                if (connectionState == DeviceConnectionState.DISCONNECTED && 
                    currentState.device.isConnected) {
                    // Device was disconnected, update the device
                    viewModelScope.launch {
                        try {
                            val device = deviceRepository.getDevice(deviceId).first()
                            _uiState.value = DeviceManagementUiState.DeviceDetail(device)
                        } catch (e: Exception) {
                            Timber.e(e, "Error updating device details after disconnection")
                        }
                    }
                }
            }
            else -> {
                // No need to update other states
            }
        }
    }
    
    /**
     * Show device details
     */
    fun showDeviceDetails(deviceId: Long) {
        viewModelScope.launch {
            try {
                val device = deviceRepository.getDevice(deviceId).first()
                _uiState.value = DeviceManagementUiState.DeviceDetail(device)
            } catch (e: Exception) {
                Timber.e(e, "Error loading device details")
                _errorEvents.emit(DeviceErrorEvent.LoadingError("Failed to load device details", e))
            }
        }
    }
    
    /**
     * Back to device list
     */
    fun backToDeviceList() {
        viewModelScope.launch {
            loadSavedDevices()
        }
    }
    
    /**
     * Retry after error
     */
    fun retry() {
        viewModelScope.launch {
            loadSavedDevices()
        }
    }
    
    /**
     * Clean up resources when ViewModel is cleared
     */
    override fun onCleared() {
        super.onCleared()
        
        // Cancel all jobs
        scanningJob?.cancel()
        connectionJobs.values.forEach { it.cancel() }
        syncJobs.values.forEach { it.cancel() }
        monitoringJob?.cancel()
        
        // Clear job maps
        connectionJobs.clear()
        syncJobs.clear()
    }
}

/**
 * Sealed class representing the UI state for device management
 */
sealed class DeviceManagementUiState {
    /**
     * Loading state
     */
    data object Loading : DeviceManagementUiState()
    
    /**
     * Empty state (no devices)
     */
    data object Empty : DeviceManagementUiState()
    
    /**
     * Device list state
     */
    data class DeviceList(val devices: List<Device>) : DeviceManagementUiState()
    
    /**
     * Device detail state
     */
    data class DeviceDetail(val device: Device) : DeviceManagementUiState()
    
    /**
     * Error state
     */
    data class Error(val message: String) : DeviceManagementUiState()
}

/**
 * Sealed class representing the scanning state
 */
sealed class ScanningState {
    /**
     * Idle state (not scanning)
     */
    data object Idle : ScanningState()
    
    /**
     * Scanning state
     */
    data class Scanning(
        val discoveredDevices: List<Device>,
        val message: String? = null
    ) : ScanningState()
    
    /**
     * Completed state
     */
    data class Completed(
        val discoveredDevices: List<Device>,
        val noDevicesFound: Boolean
    ) : ScanningState()
    
    /**
     * Error state
     */
    data class Error(val message: String) : ScanningState()
}

/**
 * Sealed class representing the sync state
 */
sealed class SyncState {
    /**
     * Idle state (not syncing)
     */
    data object Idle : SyncState()
    
    /**
     * Started state
     */
    data class Started(
        val syncType: SyncType,
        val dataTypes: List<DeviceDataType>
    ) : SyncState()
    
    /**
     * Connecting state
     */
    data object Connecting : SyncState()
    
    /**
     * Connected state
     */
    data object Connected : SyncState()
    
    /**
     * Syncing state
     */
    data class Syncing(
        val overallProgress: Int,
        val dataTypeProgress: Map<DeviceDataType, Int>,
        val message: String? = null
    ) : SyncState() {
        /**
         * Get the overall progress as a percentage
         */
        fun getOverallProgressPercentage(): Int = overallProgress
        
        /**
         * Get the number of completed data types
         */
        fun getCompletedDataTypes(): Int = dataTypeProgress.count { it.value == 100 }
        
        /**
         * Get the total number of data types
         */
        fun getTotalDataTypes(): Int = dataTypeProgress.size
        
        /**
         * Get the number of failed data types
         */
        fun getFailedDataTypes(): Int = dataTypeProgress.count { it.value == -1 }
        
        /**
         * Get the number of skipped data types
         */
        fun getSkippedDataTypes(): Int = dataTypeProgress.count { it.value == -2 }
    }
    
    /**
     * Disconnecting state
     */
    data object Disconnecting : SyncState()
    
    /**
     * Disconnected state
     */
    data object Disconnected : SyncState()
    
    /**
     * Completed state
     */
    data class Completed(val summary: SyncSummary) : SyncState()
    
    /**
     * Error state
     */
    data class Error(val message: String) : SyncState()
    
    /**
     * Cancelled state
     */
    data object Cancelled : SyncState()
}

/**
 * Data class for device details
 */
data class DeviceDetails(
    val batteryLevel: Int = 0,
    val firmwareVersion: String = "Unknown",
    val lastSyncTime: LocalDateTime? = null,
    val features: Set<DeviceFeature> = emptySet()
) {
    /**
     * Format battery level as a string
     */
    fun getBatteryLevelFormatted(): String = "$batteryLevel%"
    
    /**
     * Get battery level as a color
     */
    fun getBatteryLevelColor(): String {
        return when {
            batteryLevel >= 60 -> "#4CAF50" // Green
            batteryLevel >= 30 -> "#FFC107" // Yellow
            else -> "#F44336" // Red
        }
    }
    
    /**
     * Format last sync time as a string
     */
    fun getLastSyncTimeFormatted(): String {
        return lastSyncTime?.let {
            // Format relative time (e.g., "2 hours ago")
            val now = LocalDateTime.now()
            val minutes = java.time.temporal.ChronoUnit.MINUTES.between(it, now)
            
            when {
                minutes < 1 -> "Just now"
                minutes < 60 -> "$minutes minute${if (minutes > 1) "s" else ""} ago"
                minutes < 24 * 60 -> "${minutes / 60} hour${if (minutes / 60 > 1) "s" else ""} ago"
                minutes < 48 * 60 -> "Yesterday"
                else -> "${minutes / (24 * 60)} days ago"
            }
        } ?: "Never"
    }
    
    /**
     * Check if a feature is supported
     */
    fun supportsFeature(feature: DeviceFeature): Boolean {
        return features.contains(feature)
    }
    
    /**
     * Get supported data types
     */
    fun getSupportedDataTypes(): List<DeviceDataType> {
        val supportedTypes = mutableListOf<DeviceDataType>()
        
        if (features.contains(DeviceFeature.HEART_RATE)) {
            supportedTypes.add(DeviceDataType.HEART_RATE)
        }
        
        if (features.contains(DeviceFeature.BLOOD_OXYGEN)) {
            supportedTypes.add(DeviceDataType.BLOOD_OXYGEN)
        }
        
        if (features.contains(DeviceFeature.BLOOD_PRESSURE)) {
            supportedTypes.add(DeviceDataType.BLOOD_PRESSURE)
        }
        
        if (features.contains(DeviceFeature.STEPS)) {
            supportedTypes.add(DeviceDataType.STEPS)
        }
        
        if (features.contains(DeviceFeature.SLEEP)) {
            supportedTypes.add(DeviceDataType.SLEEP)
        }
        
        if (features.contains(DeviceFeature.ACTIVITY_TRACKING)) {
            supportedTypes.add(DeviceDataType.ACTIVITY)
        }
        
        if (features.contains(DeviceFeature.TEMPERATURE)) {
            supportedTypes.add(DeviceDataType.TEMPERATURE)
        }
        
        return supportedTypes
    }
}

/**
 * Sealed class for device error events
 */
sealed class DeviceErrorEvent {
    /**
     * Success event
     */
    data class Success(val message: String) : DeviceErrorEvent()
    
    /**
     * Loading error
     */
    data class LoadingError(
        val message: String,
        val cause: Throwable? = null
    ) : DeviceErrorEvent()
    
    /**
     * Scanning error
     */
    data class ScanningError(val message: String) : DeviceErrorEvent()
    
    /**
     * Pairing error
     */
    data class PairingError(val message: String) : DeviceErrorEvent()
    
    /**
     * Connection error
     */
    data class ConnectionError(
        val message: String,
        val error: DeviceError? = null
    ) : DeviceErrorEvent()
    
    /**
     * Disconnection error
     */
    data class DisconnectionError(val message: String) : DeviceErrorEvent()
    
    /**
     * Sync error
     */
    data class SyncError(
        val message: String,
        val deviceId: Long
    ) : DeviceErrorEvent()
    
    /**
     * Device operation error
     */
    data class DeviceOperationError(val message: String) : DeviceErrorEvent()
}

/**
 * Sealed class for permission events
 */
sealed class PermissionEvent {
    /**
     * Bluetooth permissions needed
     */
    data object BluetoothPermissionsNeeded : PermissionEvent()
    
    /**
     * Bluetooth disabled
     */
    data object BluetoothDisabled : PermissionEvent()
    
    /**
     * Request to enable Bluetooth
     */
    data object BluetoothEnableRequest : PermissionEvent()
}

/**
 * Sealed class for navigation events
 */
sealed class NavigationEvent {
    /**
     * Navigate to device details screen
     */
    data class ToDeviceDetails(val deviceId: Long) : NavigationEvent()
    
    /**
     * Navigate to sync history screen
     */
    data class ToSyncHistory(val deviceId: Long) : NavigationEvent()
}
