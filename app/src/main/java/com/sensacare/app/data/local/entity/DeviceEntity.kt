package com.sensacare.app.data.local.entity

import androidx.room.*
import com.sensacare.app.domain.model.Device
import com.sensacare.app.domain.model.DeviceCapability
import com.sensacare.app.domain.model.DeviceConnectionStatus
import com.sensacare.app.domain.model.DeviceSyncStatus
import java.time.LocalDateTime
import java.util.UUID

/**
 * DeviceEntity - Room database entity for HBand device management
 *
 * This entity stores comprehensive information about HBand devices connected to the app, including:
 * - Device identification (ID, name, model, MAC address)
 * - Connection status and history
 * - Battery and charging information
 * - Firmware and hardware details
 * - Synchronization status and history
 * - Device capabilities and supported features
 * - User-specific settings and preferences
 * - Pairing and authentication information
 *
 * The entity serves as the central record for all device management operations,
 * enabling the app to maintain persistent device connections, track sync status,
 * and manage device-specific settings.
 */
@Entity(
    tableName = "device",
    indices = [
        Index("deviceId", unique = true),
        Index("userId"),
        Index("macAddress", unique = true),
        Index("connectionStatus"),
        Index("syncStatus"),
        Index("lastSyncTime"),
        Index("lastConnectionTime")
    ]
)
data class DeviceEntity(
    /**
     * Primary key - unique identifier for the device record
     */
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    /**
     * Device identifier - typically provided by the device itself
     */
    @ColumnInfo(name = "deviceId")
    val deviceId: String,

    /**
     * User identifier - allows for multi-user support
     */
    @ColumnInfo(name = "userId")
    val userId: String,

    /**
     * User-assigned name for the device
     */
    @ColumnInfo(name = "name")
    val name: String,

    /**
     * Device model (e.g., "HBand Pro", "HBand Lite")
     */
    @ColumnInfo(name = "model")
    val model: String,

    /**
     * Device manufacturer
     */
    @ColumnInfo(name = "manufacturer")
    val manufacturer: String,

    /**
     * MAC address for Bluetooth identification
     */
    @ColumnInfo(name = "macAddress")
    val macAddress: String,

    /**
     * Current connection status (CONNECTED, DISCONNECTED, PAIRING, etc.)
     */
    @ColumnInfo(name = "connectionStatus")
    val connectionStatus: String,

    /**
     * Current battery level as percentage (0-100)
     */
    @ColumnInfo(name = "batteryLevel")
    val batteryLevel: Int,

    /**
     * Flag indicating if device is currently charging
     */
    @ColumnInfo(name = "isCharging")
    val isCharging: Boolean = false,

    /**
     * Current firmware version installed on the device
     */
    @ColumnInfo(name = "firmwareVersion")
    val firmwareVersion: String,

    /**
     * Latest available firmware version (if an update is available)
     */
    @ColumnInfo(name = "latestFirmwareVersion")
    val latestFirmwareVersion: String? = null,

    /**
     * Flag indicating if a firmware update is available
     */
    @ColumnInfo(name = "firmwareUpdateAvailable")
    val firmwareUpdateAvailable: Boolean = false,

    /**
     * Hardware version of the device
     */
    @ColumnInfo(name = "hardwareVersion")
    val hardwareVersion: String,

    /**
     * Serial number of the device
     */
    @ColumnInfo(name = "serialNumber")
    val serialNumber: String? = null,

    /**
     * Current synchronization status (SYNCED, SYNCING, PENDING, ERROR)
     */
    @ColumnInfo(name = "syncStatus")
    val syncStatus: String,

    /**
     * Last time the device was successfully synced
     */
    @ColumnInfo(name = "lastSyncTime")
    val lastSyncTime: LocalDateTime? = null,

    /**
     * Last time the device was connected
     */
    @ColumnInfo(name = "lastConnectionTime")
    val lastConnectionTime: LocalDateTime? = null,

    /**
     * Duration of the last sync operation in seconds
     */
    @ColumnInfo(name = "lastSyncDuration")
    val lastSyncDuration: Int? = null,

    /**
     * Error message from the last failed sync attempt
     */
    @ColumnInfo(name = "lastSyncError")
    val lastSyncError: String? = null,

    /**
     * Number of consecutive failed sync attempts
     */
    @ColumnInfo(name = "failedSyncAttempts")
    val failedSyncAttempts: Int = 0,

    /**
     * Device capabilities as a comma-separated list of capability codes
     */
    @ColumnInfo(name = "capabilities")
    val capabilities: String,

    /**
     * Device-specific settings as a JSON string
     */
    @ColumnInfo(name = "settings")
    val settings: String? = null,

    /**
     * Authentication token for secure device communication
     */
    @ColumnInfo(name = "authToken")
    val authToken: String? = null,

    /**
     * Timestamp when the auth token expires
     */
    @ColumnInfo(name = "authTokenExpiry")
    val authTokenExpiry: LocalDateTime? = null,

    /**
     * Flag indicating if the device is the primary device for the user
     */
    @ColumnInfo(name = "isPrimary")
    val isPrimary: Boolean = false,

    /**
     * Flag indicating if the device supports auto-sync
     */
    @ColumnInfo(name = "autoSyncEnabled")
    val autoSyncEnabled: Boolean = true,

    /**
     * Auto-sync frequency in minutes
     */
    @ColumnInfo(name = "autoSyncFrequency")
    val autoSyncFrequency: Int = 60,

    /**
     * Bluetooth signal strength (RSSI) from last connection
     */
    @ColumnInfo(name = "signalStrength")
    val signalStrength: Int? = null,

    /**
     * Transmission power level (0-3)
     */
    @ColumnInfo(name = "txPowerLevel")
    val txPowerLevel: Int? = null,

    /**
     * MTU (Maximum Transmission Unit) size for BLE communication
     */
    @ColumnInfo(name = "mtuSize")
    val mtuSize: Int? = null,

    /**
     * Device color
     */
    @ColumnInfo(name = "color")
    val color: String? = null,

    /**
     * Flag indicating if notifications are enabled for this device
     */
    @ColumnInfo(name = "notificationsEnabled")
    val notificationsEnabled: Boolean = true,

    /**
     * Date when the device was first paired
     */
    @ColumnInfo(name = "pairingDate")
    val pairingDate: LocalDateTime = LocalDateTime.now(),

    /**
     * Timestamp when this record was created locally
     */
    @ColumnInfo(name = "createdAt")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    /**
     * Timestamp when this record was last modified locally
     */
    @ColumnInfo(name = "modifiedAt")
    val modifiedAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * Convert entity to domain model
     */
    fun toDomainModel(): Device {
        return Device(
            id = id,
            deviceId = deviceId,
            userId = userId,
            name = name,
            model = model,
            manufacturer = manufacturer,
            macAddress = macAddress,
            connectionStatus = DeviceConnectionStatus.valueOf(connectionStatus),
            batteryLevel = batteryLevel,
            isCharging = isCharging,
            firmwareVersion = firmwareVersion,
            latestFirmwareVersion = latestFirmwareVersion,
            firmwareUpdateAvailable = firmwareUpdateAvailable,
            hardwareVersion = hardwareVersion,
            serialNumber = serialNumber,
            syncStatus = DeviceSyncStatus.valueOf(syncStatus),
            lastSyncTime = lastSyncTime,
            lastConnectionTime = lastConnectionTime,
            lastSyncDuration = lastSyncDuration,
            lastSyncError = lastSyncError,
            failedSyncAttempts = failedSyncAttempts,
            capabilities = parseCapabilities(capabilities),
            settings = settings,
            authToken = authToken,
            authTokenExpiry = authTokenExpiry,
            isPrimary = isPrimary,
            autoSyncEnabled = autoSyncEnabled,
            autoSyncFrequency = autoSyncFrequency,
            signalStrength = signalStrength,
            txPowerLevel = txPowerLevel,
            mtuSize = mtuSize,
            color = color,
            notificationsEnabled = notificationsEnabled,
            pairingDate = pairingDate,
            createdAt = createdAt,
            modifiedAt = modifiedAt
        )
    }

    companion object {
        /**
         * Convert domain model to entity
         */
        fun fromDomainModel(domainModel: Device): DeviceEntity {
            return DeviceEntity(
                id = domainModel.id,
                deviceId = domainModel.deviceId,
                userId = domainModel.userId,
                name = domainModel.name,
                model = domainModel.model,
                manufacturer = domainModel.manufacturer,
                macAddress = domainModel.macAddress,
                connectionStatus = domainModel.connectionStatus.name,
                batteryLevel = domainModel.batteryLevel,
                isCharging = domainModel.isCharging,
                firmwareVersion = domainModel.firmwareVersion,
                latestFirmwareVersion = domainModel.latestFirmwareVersion,
                firmwareUpdateAvailable = domainModel.firmwareUpdateAvailable,
                hardwareVersion = domainModel.hardwareVersion,
                serialNumber = domainModel.serialNumber,
                syncStatus = domainModel.syncStatus.name,
                lastSyncTime = domainModel.lastSyncTime,
                lastConnectionTime = domainModel.lastConnectionTime,
                lastSyncDuration = domainModel.lastSyncDuration,
                lastSyncError = domainModel.lastSyncError,
                failedSyncAttempts = domainModel.failedSyncAttempts,
                capabilities = formatCapabilities(domainModel.capabilities),
                settings = domainModel.settings,
                authToken = domainModel.authToken,
                authTokenExpiry = domainModel.authTokenExpiry,
                isPrimary = domainModel.isPrimary,
                autoSyncEnabled = domainModel.autoSyncEnabled,
                autoSyncFrequency = domainModel.autoSyncFrequency,
                signalStrength = domainModel.signalStrength,
                txPowerLevel = domainModel.txPowerLevel,
                mtuSize = domainModel.mtuSize,
                color = domainModel.color,
                notificationsEnabled = domainModel.notificationsEnabled,
                pairingDate = domainModel.pairingDate,
                createdAt = domainModel.createdAt,
                modifiedAt = domainModel.modifiedAt
            )
        }

        /**
         * Parse capabilities string into a set of DeviceCapability enum values
         * @param capabilitiesStr Comma-separated list of capability codes
         * @return Set of DeviceCapability enum values
         */
        private fun parseCapabilities(capabilitiesStr: String): Set<DeviceCapability> {
            if (capabilitiesStr.isBlank()) return emptySet()
            
            return capabilitiesStr.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .mapNotNull { code ->
                    try {
                        DeviceCapability.valueOf(code)
                    } catch (e: IllegalArgumentException) {
                        null
                    }
                }
                .toSet()
        }

        /**
         * Format a set of DeviceCapability enum values into a comma-separated string
         * @param capabilities Set of DeviceCapability enum values
         * @return Comma-separated string of capability codes
         */
        private fun formatCapabilities(capabilities: Set<DeviceCapability>): String {
            return capabilities.joinToString(",") { it.name }
        }

        /**
         * Validate MAC address format
         * @param macAddress MAC address to validate
         * @return True if the MAC address is valid
         */
        fun isValidMacAddress(macAddress: String): Boolean {
            // MAC address format: XX:XX:XX:XX:XX:XX where X is a hexadecimal digit
            val macRegex = "^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$".toRegex()
            return macRegex.matches(macAddress)
        }

        /**
         * Format MAC address to standard format (XX:XX:XX:XX:XX:XX)
         * @param macAddress MAC address to format
         * @return Formatted MAC address
         */
        fun formatMacAddress(macAddress: String): String {
            // Remove any non-alphanumeric characters
            val cleanMac = macAddress.replace(Regex("[^0-9A-Fa-f]"), "")
            
            // Check if we have exactly 12 hexadecimal characters
            if (cleanMac.length != 12) {
                throw IllegalArgumentException("Invalid MAC address: $macAddress")
            }
            
            // Format as XX:XX:XX:XX:XX:XX
            return cleanMac.chunked(2).joinToString(":") { it.uppercase() }
        }

        /**
         * Calculate battery status category based on battery level
         * @param batteryLevel Battery level as percentage (0-100)
         * @return Battery status category (CRITICAL, LOW, MEDIUM, HIGH, FULL)
         */
        fun getBatteryStatusCategory(batteryLevel: Int): BatteryStatus {
            return when {
                batteryLevel < 10 -> BatteryStatus.CRITICAL
                batteryLevel < 30 -> BatteryStatus.LOW
                batteryLevel < 60 -> BatteryStatus.MEDIUM
                batteryLevel < 95 -> BatteryStatus.HIGH
                else -> BatteryStatus.FULL
            }
        }

        /**
         * Estimate battery remaining time based on battery level and usage pattern
         * @param batteryLevel Current battery level as percentage (0-100)
         * @param isHighUsage Whether the device is in high usage mode
         * @return Estimated remaining time in hours
         */
        fun estimateBatteryRemainingTime(batteryLevel: Int, isHighUsage: Boolean): Int {
            // Base battery life in hours (full charge)
            val baseBatteryLife = if (isHighUsage) 72 else 168 // 3 days or 7 days
            
            // Calculate remaining time proportionally
            return (baseBatteryLife * batteryLevel / 100)
        }

        /**
         * Check if firmware update is needed based on version comparison
         * @param currentVersion Current firmware version
         * @param latestVersion Latest available firmware version
         * @return True if update is needed
         */
        fun isFirmwareUpdateNeeded(currentVersion: String, latestVersion: String?): Boolean {
            if (latestVersion == null) return false
            
            // Parse version strings (assuming format like "1.2.3")
            val current = parseVersionString(currentVersion)
            val latest = parseVersionString(latestVersion)
            
            // Compare version components
            for (i in 0 until minOf(current.size, latest.size)) {
                if (latest[i] > current[i]) return true
                if (latest[i] < current[i]) return false
            }
            
            // If we get here and latest has more components, it's newer
            return latest.size > current.size
        }

        /**
         * Parse version string into list of integers
         * @param version Version string (e.g., "1.2.3")
         * @return List of version components as integers
         */
        private fun parseVersionString(version: String): List<Int> {
            return version.split(".")
                .mapNotNull { it.toIntOrNull() }
        }

        /**
         * Check if device has a specific capability
         * @param capabilities Set of device capabilities
         * @param capability Capability to check for
         * @return True if the device has the capability
         */
        fun hasCapability(capabilities: Set<DeviceCapability>, capability: DeviceCapability): Boolean {
            return capabilities.contains(capability)
        }

        /**
         * Get recommended sync frequency based on device capabilities and usage
         * @param capabilities Set of device capabilities
         * @param isActiveUser Whether the user is highly active
         * @return Recommended sync frequency in minutes
         */
        fun getRecommendedSyncFrequency(
            capabilities: Set<DeviceCapability>,
            isActiveUser: Boolean
        ): Int {
            // Base sync frequency
            var frequency = 60 // 1 hour
            
            // Adjust based on capabilities
            if (capabilities.contains(DeviceCapability.CONTINUOUS_HEART_RATE)) {
                frequency = 30 // 30 minutes
            }
            
            if (capabilities.contains(DeviceCapability.SLEEP_TRACKING) || 
                capabilities.contains(DeviceCapability.STRESS_MONITORING)) {
                frequency = minOf(frequency, 45) // 45 minutes
            }
            
            // Adjust for active users
            if (isActiveUser) {
                frequency = (frequency * 0.7).toInt() // 30% more frequent
            }
            
            return frequency
        }

        /**
         * Calculate signal strength quality from RSSI value
         * @param rssi RSSI value in dBm
         * @return Signal quality (EXCELLENT, GOOD, FAIR, POOR)
         */
        fun getSignalQuality(rssi: Int): SignalQuality {
            return when {
                rssi > -60 -> SignalQuality.EXCELLENT
                rssi > -70 -> SignalQuality.GOOD
                rssi > -80 -> SignalQuality.FAIR
                else -> SignalQuality.POOR
            }
        }

        /**
         * Calculate estimated connection range based on RSSI and txPower
         * @param rssi RSSI value in dBm
         * @param txPower Transmission power in dBm
         * @return Estimated distance in meters
         */
        fun estimateConnectionRange(rssi: Int, txPower: Int): Float {
            // Simple distance estimation using the log-distance path loss model
            // Distance = 10^((txPower - rssi) / (10 * n))
            // where n is the path loss exponent (typically 2-4)
            val pathLossExponent = 2.5f
            return Math.pow(10.0, (txPower - rssi) / (10.0 * pathLossExponent)).toFloat()
        }

        /**
         * Check if device needs reconnection based on last connection time
         * @param lastConnectionTime Last time the device was connected
         * @param connectionStatus Current connection status
         * @return True if reconnection is recommended
         */
        fun needsReconnection(
            lastConnectionTime: LocalDateTime?,
            connectionStatus: DeviceConnectionStatus
        ): Boolean {
            if (connectionStatus == DeviceConnectionStatus.CONNECTED) return false
            if (lastConnectionTime == null) return true
            
            // Recommend reconnection if last connection was more than 24 hours ago
            val hoursSinceLastConnection = java.time.Duration.between(
                lastConnectionTime,
                LocalDateTime.now()
            ).toHours()
            
            return hoursSinceLastConnection >= 24
        }
    }

    /**
     * Enum for battery status categories
     */
    enum class BatteryStatus {
        CRITICAL,
        LOW,
        MEDIUM,
        HIGH,
        FULL
    }

    /**
     * Enum for signal quality categories
     */
    enum class SignalQuality {
        EXCELLENT,
        GOOD,
        FAIR,
        POOR
    }
}
