package com.sensacare.app.domain.repository

import com.sensacare.app.domain.model.Device
import com.sensacare.app.domain.model.DeviceFeature
import com.sensacare.app.domain.model.DeviceSettings
import com.sensacare.app.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * DeviceRepository - Interface for device management operations
 *
 * This repository handles all device-related operations including:
 * - Device discovery and pairing
 * - Connection management
 * - Feature detection
 * - Data synchronization
 * - Battery monitoring
 * - Device configuration
 */
interface DeviceRepository {

    /**
     * ====================================
     * Device Discovery and Pairing
     * ====================================
     */

    /**
     * Start scanning for nearby devices
     * @param timeoutSeconds Maximum scan duration in seconds
     * @return Flow of discovered devices
     */
    fun startDeviceScan(timeoutSeconds: Int = 30): Flow<DeviceOperationResult<List<Device>>>

    /**
     * Stop the current device scan
     */
    suspend fun stopDeviceScan()

    /**
     * Check if a scan is currently in progress
     * @return Flow emitting true if scanning, false otherwise
     */
    fun isScanning(): Flow<Boolean>

    /**
     * Pair with a device
     * @param device The device to pair with
     * @return Result of the pairing operation
     */
    suspend fun pairDevice(device: Device): DeviceOperationResult<Device>

    /**
     * Unpair a device
     * @param deviceId The ID of the device to unpair
     * @return Result of the unpairing operation
     */
    suspend fun unpairDevice(deviceId: Long): DeviceOperationResult<Boolean>

    /**
     * Get all paired devices
     * @return Flow of all paired devices
     */
    fun getPairedDevices(): Flow<List<Device>>

    /**
     * Check if a device is paired
     * @param deviceId The ID of the device to check
     * @return Flow emitting true if paired, false otherwise
     */
    fun isDevicePaired(deviceId: Long): Flow<Boolean>

    /**
     * ====================================
     * Connection Management
     * ====================================
     */

    /**
     * Connect to a device
     * @param deviceId The ID of the device to connect to
     * @return Result of the connection operation
     */
    suspend fun connectToDevice(deviceId: Long): DeviceOperationResult<Device>

    /**
     * Disconnect from a device
     * @param deviceId The ID of the device to disconnect from
     * @return Result of the disconnection operation
     */
    suspend fun disconnectFromDevice(deviceId: Long): DeviceOperationResult<Boolean>

    /**
     * Get the current connection state for a device
     * @param deviceId The ID of the device to check
     * @return Flow of the connection state
     */
    fun getDeviceConnectionState(deviceId: Long): Flow<DeviceConnectionState>

    /**
     * Get all currently connected devices
     * @return Flow of all connected devices
     */
    fun getConnectedDevices(): Flow<List<Device>>

    /**
     * Check if a device is connected
     * @param deviceId The ID of the device to check
     * @return Flow emitting true if connected, false otherwise
     */
    fun isDeviceConnected(deviceId: Long): Flow<Boolean>

    /**
     * Get the last connected time for a device
     * @param deviceId The ID of the device to check
     * @return Flow of the last connected time, or null if never connected
     */
    fun getLastConnectedTime(deviceId: Long): Flow<LocalDateTime?>

    /**
     * ====================================
     * Device Feature Management
     * ====================================
     */

    /**
     * Get all features supported by a device
     * @param deviceId The ID of the device to check
     * @return Flow of supported features
     */
    fun getDeviceFeatures(deviceId: Long): Flow<Set<DeviceFeature>>

    /**
     * Check if a device supports a specific feature
     * @param deviceId The ID of the device to check
     * @param feature The feature to check for
     * @return Flow emitting true if supported, false otherwise
     */
    fun deviceSupportsFeature(deviceId: Long, feature: DeviceFeature): Flow<Boolean>

    /**
     * Get devices that support a specific feature
     * @param feature The feature to filter by
     * @return Flow of devices supporting the feature
     */
    fun getDevicesWithFeature(feature: DeviceFeature): Flow<List<Device>>

    /**
     * Update the detected features for a device
     * @param deviceId The ID of the device to update
     * @param features The set of features to update
     * @return Result of the update operation
     */
    suspend fun updateDeviceFeatures(deviceId: Long, features: Set<DeviceFeature>): DeviceOperationResult<Device>

    /**
     * ====================================
     * Battery and Status Monitoring
     * ====================================
     */

    /**
     * Get the current battery level for a device
     * @param deviceId The ID of the device to check
     * @return Flow of the battery level (0-100), or null if unknown
     */
    fun getDeviceBatteryLevel(deviceId: Long): Flow<Int?>

    /**
     * Get devices with low battery (below threshold)
     * @param threshold The battery level threshold (default 20%)
     * @return Flow of devices with battery below threshold
     */
    fun getLowBatteryDevices(threshold: Int = 20): Flow<List<Device>>

    /**
     * Update the battery level for a device
     * @param deviceId The ID of the device to update
     * @param batteryLevel The new battery level
     * @return Result of the update operation
     */
    suspend fun updateDeviceBatteryLevel(deviceId: Long, batteryLevel: Int): DeviceOperationResult<Device>

    /**
     * Get the firmware version for a device
     * @param deviceId The ID of the device to check
     * @return Flow of the firmware version
     */
    fun getDeviceFirmwareVersion(deviceId: Long): Flow<String>

    /**
     * Get the hardware version for a device
     * @param deviceId The ID of the device to check
     * @return Flow of the hardware version
     */
    fun getDeviceHardwareVersion(deviceId: Long): Flow<String>

    /**
     * ====================================
     * Sync Operations
     * ====================================
     */

    /**
     * Sync all data from a device
     * @param deviceId The ID of the device to sync
     * @return Result of the sync operation
     */
    suspend fun syncDeviceData(deviceId: Long): DeviceOperationResult<SyncResult>

    /**
     * Sync specific data type from a device
     * @param deviceId The ID of the device to sync
     * @param dataType The type of data to sync
     * @return Result of the sync operation
     */
    suspend fun syncDeviceDataType(deviceId: Long, dataType: DeviceDataType): DeviceOperationResult<SyncResult>

    /**
     * Get the sync status for a device
     * @param deviceId The ID of the device to check
     * @return Flow of the sync status
     */
    fun getDeviceSyncStatus(deviceId: Long): Flow<SyncStatus>

    /**
     * Get the last sync time for a device
     * @param deviceId The ID of the device to check
     * @param dataType Optional data type to check specific sync time
     * @return Flow of the last sync time, or null if never synced
     */
    fun getLastSyncTime(deviceId: Long, dataType: DeviceDataType? = null): Flow<LocalDateTime?>

    /**
     * Get devices that need syncing
     * @return Flow of devices that need to be synced
     */
    fun getDevicesNeedingSync(): Flow<List<Device>>

    /**
     * ====================================
     * Device Configuration
     * ====================================
     */

    /**
     * Get the current settings for a device
     * @param deviceId The ID of the device to check
     * @return Flow of the device settings
     */
    fun getDeviceSettings(deviceId: Long): Flow<DeviceSettings>

    /**
     * Update settings for a device
     * @param deviceId The ID of the device to update
     * @param settings The new settings to apply
     * @return Result of the update operation
     */
    suspend fun updateDeviceSettings(deviceId: Long, settings: DeviceSettings): DeviceOperationResult<DeviceSettings>

    /**
     * Set a device as the preferred device
     * @param deviceId The ID of the device to set as preferred
     * @return Result of the operation
     */
    suspend fun setPreferredDevice(deviceId: Long): DeviceOperationResult<Device>

    /**
     * Get the preferred device
     * @return Flow of the preferred device, or null if none set
     */
    fun getPreferredDevice(): Flow<Device?>

    /**
     * ====================================
     * Device Management
     * ====================================
     */

    /**
     * Get a device by ID
     * @param deviceId The ID of the device to get
     * @return Flow of the device, or null if not found
     */
    fun getDevice(deviceId: Long): Flow<Device?>

    /**
     * Get all devices
     * @return Flow of all devices
     */
    fun getAllDevices(): Flow<List<Device>>

    /**
     * Get active devices
     * @return Flow of active devices
     */
    fun getActiveDevices(): Flow<List<Device>>

    /**
     * Set a device as active or inactive
     * @param deviceId The ID of the device to update
     * @param isActive Whether the device should be active
     * @return Result of the update operation
     */
    suspend fun setDeviceActive(deviceId: Long, isActive: Boolean): DeviceOperationResult<Device>

    /**
     * Update device information
     * @param device The updated device information
     * @return Result of the update operation
     */
    suspend fun updateDevice(device: Device): DeviceOperationResult<Device>

    /**
     * Delete a device
     * @param deviceId The ID of the device to delete
     * @return Result of the delete operation
     */
    suspend fun deleteDevice(deviceId: Long): DeviceOperationResult<Boolean>

    /**
     * Search for devices by name or address
     * @param query The search query
     * @return Flow of devices matching the query
     */
    fun searchDevices(query: String): Flow<List<Device>>
}

/**
 * Sealed class representing the result of a device operation
 */
sealed class DeviceOperationResult<out T> {
    data class Success<T>(val data: T) : DeviceOperationResult<T>()
    data class Error(val error: DeviceError) : DeviceOperationResult<Nothing>()
    data object Loading : DeviceOperationResult<Nothing>()
}

/**
 * Sealed class representing device errors
 */
sealed class DeviceError {
    data class ConnectionError(val message: String, val cause: Throwable? = null) : DeviceError()
    data class BluetoothDisabledError(val message: String = "Bluetooth is disabled") : DeviceError()
    data class PermissionError(val message: String, val permission: String) : DeviceError()
    data class PairingError(val message: String, val cause: Throwable? = null) : DeviceError()
    data class SyncError(val message: String, val dataType: DeviceDataType? = null, val cause: Throwable? = null) : DeviceError()
    data class DeviceNotFoundError(val deviceId: Long, val message: String = "Device not found") : DeviceError()
    data class DeviceNotSupportedError(val message: String, val feature: DeviceFeature? = null) : DeviceError()
    data class TimeoutError(val message: String, val operation: String) : DeviceError()
    data class UnknownError(val message: String, val cause: Throwable? = null) : DeviceError()
}

/**
 * Enum representing the connection state of a device
 */
enum class DeviceConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
    CONNECTION_FAILED
}

/**
 * Enum representing the types of data that can be synced from a device
 */
enum class DeviceDataType {
    HEART_RATE,
    BLOOD_OXYGEN,
    BLOOD_PRESSURE,
    STEPS,
    SLEEP,
    ACTIVITY,
    TEMPERATURE,
    ALL
}

/**
 * Data class representing the result of a sync operation
 */
data class SyncResult(
    val deviceId: Long,
    val syncedDataTypes: Set<DeviceDataType>,
    val syncTime: LocalDateTime,
    val itemsSynced: Map<DeviceDataType, Int>,
    val syncStatus: SyncStatus,
    val errorMessage: String? = null
)
