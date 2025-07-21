package com.sensacare.app.data.local.dao

import androidx.room.*
import com.sensacare.app.data.local.entity.DeviceEntity
import com.sensacare.app.data.local.entity.DeviceSettingEntity
import com.sensacare.app.data.local.entity.DeviceSyncHistoryEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * DeviceDao - Data Access Object for device management
 *
 * This DAO provides comprehensive access to device-related data, including:
 * - Device CRUD operations
 * - Connection status tracking
 * - Battery level monitoring
 * - Sync status and sync history
 * - Device settings management
 * - Pairing and unpairing operations
 * - Device health monitoring
 * - Last seen/activity tracking
 *
 * The DeviceDao enables the app to maintain reliable connections with health monitoring
 * devices and ensure data synchronization is properly tracked and managed.
 */
@Dao
interface DeviceDao {

    /**
     * Basic CRUD Operations
     */

    /**
     * Insert a single device
     * @param device The device entity to insert
     * @return The row ID of the inserted item
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(device: DeviceEntity): Long

    /**
     * Insert multiple devices in a single transaction
     * @param devices List of device entities to insert
     * @return List of row IDs for the inserted items
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(devices: List<DeviceEntity>): List<Long>

    /**
     * Update a device
     * @param device The device entity to update
     * @return Number of rows updated
     */
    @Update
    suspend fun update(device: DeviceEntity): Int

    /**
     * Delete a device
     * @param device The device entity to delete
     * @return Number of rows deleted
     */
    @Delete
    suspend fun delete(device: DeviceEntity): Int

    /**
     * Delete a device by ID
     * @param deviceId The ID of the device to delete
     * @return Number of rows deleted
     */
    @Query("DELETE FROM devices WHERE id = :deviceId")
    suspend fun deleteById(deviceId: String): Int

    /**
     * Device Settings Operations
     */

    /**
     * Insert a device setting
     * @param deviceSetting The device setting entity to insert
     * @return The row ID of the inserted item
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(deviceSetting: DeviceSettingEntity): Long

    /**
     * Update a device setting
     * @param deviceSetting The device setting entity to update
     * @return Number of rows updated
     */
    @Update
    suspend fun updateSetting(deviceSetting: DeviceSettingEntity): Int

    /**
     * Delete a device setting
     * @param deviceSetting The device setting entity to delete
     * @return Number of rows deleted
     */
    @Delete
    suspend fun deleteSetting(deviceSetting: DeviceSettingEntity): Int

    /**
     * Delete all settings for a device
     * @param deviceId The device ID
     * @return Number of rows deleted
     */
    @Query("DELETE FROM device_settings WHERE deviceId = :deviceId")
    suspend fun deleteAllSettingsForDevice(deviceId: String): Int

    /**
     * Device Sync History Operations
     */

    /**
     * Insert a sync history record
     * @param syncHistory The sync history entity to insert
     * @return The row ID of the inserted item
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncHistory(syncHistory: DeviceSyncHistoryEntity): Long

    /**
     * Insert multiple sync history records in a single transaction
     * @param syncHistoryList List of sync history entities to insert
     * @return List of row IDs for the inserted items
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSyncHistory(syncHistoryList: List<DeviceSyncHistoryEntity>): List<Long>

    /**
     * Delete sync history older than a specified date
     * @param deviceId The device ID
     * @param olderThan The cutoff date
     * @return Number of rows deleted
     */
    @Query("DELETE FROM device_sync_history WHERE deviceId = :deviceId AND syncTime < :olderThan")
    suspend fun deleteSyncHistoryOlderThan(deviceId: String, olderThan: LocalDateTime): Int

    /**
     * Basic Queries
     */

    /**
     * Get device by ID
     * @param deviceId The ID of the device to retrieve
     * @return The device entity or null if not found
     */
    @Query("SELECT * FROM devices WHERE id = :deviceId")
    suspend fun getById(deviceId: String): DeviceEntity?

    /**
     * Get device by ID as Flow for reactive updates
     * @param deviceId The ID of the device to retrieve
     * @return Flow emitting the device entity
     */
    @Query("SELECT * FROM devices WHERE id = :deviceId")
    fun getByIdAsFlow(deviceId: String): Flow<DeviceEntity?>

    /**
     * Get device by MAC address
     * @param macAddress The MAC address of the device to retrieve
     * @return The device entity or null if not found
     */
    @Query("SELECT * FROM devices WHERE macAddress = :macAddress")
    suspend fun getByMacAddress(macAddress: String): DeviceEntity?

    /**
     * Get all devices for a specific user
     * @param userId The user ID
     * @return List of device entities
     */
    @Query("SELECT * FROM devices WHERE userId = :userId ORDER BY name")
    suspend fun getAllForUser(userId: String): List<DeviceEntity>

    /**
     * Get all devices for a specific user as Flow for reactive updates
     * @param userId The user ID
     * @return Flow emitting list of device entities
     */
    @Query("SELECT * FROM devices WHERE userId = :userId ORDER BY name")
    fun getAllForUserAsFlow(userId: String): Flow<List<DeviceEntity>>

    /**
     * Get all connected devices for a specific user
     * @param userId The user ID
     * @return List of connected device entities
     */
    @Query("SELECT * FROM devices WHERE userId = :userId AND isConnected = 1 ORDER BY name")
    suspend fun getConnectedDevices(userId: String): List<DeviceEntity>

    /**
     * Get all connected devices for a specific user as Flow for reactive updates
     * @param userId The user ID
     * @return Flow emitting list of connected device entities
     */
    @Query("SELECT * FROM devices WHERE userId = :userId AND isConnected = 1 ORDER BY name")
    fun getConnectedDevicesAsFlow(userId: String): Flow<List<DeviceEntity>>

    /**
     * Get all devices by type
     * @param userId The user ID
     * @param deviceType The device type to filter by
     * @return List of device entities of the specified type
     */
    @Query("SELECT * FROM devices WHERE userId = :userId AND type = :deviceType ORDER BY name")
    suspend fun getDevicesByType(userId: String, deviceType: String): List<DeviceEntity>

    /**
     * Get all devices by type as Flow for reactive updates
     * @param userId The user ID
     * @param deviceType The device type to filter by
     * @return Flow emitting list of device entities of the specified type
     */
    @Query("SELECT * FROM devices WHERE userId = :userId AND type = :deviceType ORDER BY name")
    fun getDevicesByTypeAsFlow(userId: String, deviceType: String): Flow<List<DeviceEntity>>

    /**
     * Get all devices with low battery
     * @param userId The user ID
     * @param threshold Battery level threshold (percentage)
     * @return List of device entities with battery level below threshold
     */
    @Query("SELECT * FROM devices WHERE userId = :userId AND batteryLevel < :threshold ORDER BY batteryLevel")
    suspend fun getDevicesWithLowBattery(userId: String, threshold: Int = 20): List<DeviceEntity>

    /**
     * Get all devices with low battery as Flow for reactive updates
     * @param userId The user ID
     * @param threshold Battery level threshold (percentage)
     * @return Flow emitting list of device entities with battery level below threshold
     */
    @Query("SELECT * FROM devices WHERE userId = :userId AND batteryLevel < :threshold ORDER BY batteryLevel")
    fun getDevicesWithLowBatteryAsFlow(userId: String, threshold: Int = 20): Flow<List<DeviceEntity>>

    /**
     * Get device settings
     * @param deviceId The device ID
     * @return List of device setting entities
     */
    @Query("SELECT * FROM device_settings WHERE deviceId = :deviceId ORDER BY settingKey")
    suspend fun getDeviceSettings(deviceId: String): List<DeviceSettingEntity>

    /**
     * Get device settings as Flow for reactive updates
     * @param deviceId The device ID
     * @return Flow emitting list of device setting entities
     */
    @Query("SELECT * FROM device_settings WHERE deviceId = :deviceId ORDER BY settingKey")
    fun getDeviceSettingsAsFlow(deviceId: String): Flow<List<DeviceSettingEntity>>

    /**
     * Get device setting by key
     * @param deviceId The device ID
     * @param settingKey The setting key
     * @return The device setting entity or null if not found
     */
    @Query("SELECT * FROM device_settings WHERE deviceId = :deviceId AND settingKey = :settingKey")
    suspend fun getDeviceSettingByKey(deviceId: String, settingKey: String): DeviceSettingEntity?

    /**
     * Get device setting by key as Flow for reactive updates
     * @param deviceId The device ID
     * @param settingKey The setting key
     * @return Flow emitting the device setting entity
     */
    @Query("SELECT * FROM device_settings WHERE deviceId = :deviceId AND settingKey = :settingKey")
    fun getDeviceSettingByKeyAsFlow(deviceId: String, settingKey: String): Flow<DeviceSettingEntity?>

    /**
     * Get sync history for a device
     * @param deviceId The device ID
     * @param limit Maximum number of records to return
     * @return List of sync history entities
     */
    @Query("SELECT * FROM device_sync_history WHERE deviceId = :deviceId ORDER BY syncTime DESC LIMIT :limit")
    suspend fun getSyncHistory(deviceId: String, limit: Int = 50): List<DeviceSyncHistoryEntity>

    /**
     * Get sync history for a device as Flow for reactive updates
     * @param deviceId The device ID
     * @param limit Maximum number of records to return
     * @return Flow emitting list of sync history entities
     */
    @Query("SELECT * FROM device_sync_history WHERE deviceId = :deviceId ORDER BY syncTime DESC LIMIT :limit")
    fun getSyncHistoryAsFlow(deviceId: String, limit: Int = 50): Flow<List<DeviceSyncHistoryEntity>>

    /**
     * Connection Status Management
     */

    /**
     * Update device connection status
     * @param deviceId The device ID
     * @param isConnected Whether the device is connected
     * @param lastConnectionTime The time of last connection
     * @return Number of rows updated
     */
    @Query("""
        UPDATE devices 
        SET 
            isConnected = :isConnected, 
            lastConnectionTime = :lastConnectionTime,
            lastSeenTime = :lastConnectionTime,
            modifiedAt = :lastConnectionTime
        WHERE id = :deviceId
    """)
    suspend fun updateConnectionStatus(
        deviceId: String,
        isConnected: Boolean,
        lastConnectionTime: LocalDateTime = LocalDateTime.now()
    ): Int

    /**
     * Update device disconnection
     * @param deviceId The device ID
     * @param disconnectionTime The time of disconnection
     * @return Number of rows updated
     */
    @Query("""
        UPDATE devices 
        SET 
            isConnected = 0, 
            lastDisconnectionTime = :disconnectionTime,
            lastSeenTime = :disconnectionTime,
            modifiedAt = :disconnectionTime
        WHERE id = :deviceId
    """)
    suspend fun updateDisconnection(
        deviceId: String,
        disconnectionTime: LocalDateTime = LocalDateTime.now()
    ): Int

    /**
     * Get device connection statistics
     * @param deviceId The device ID
     * @param startDate Start date
     * @param endDate End date
     * @return Device connection statistics
     */
    @Query("""
        WITH connection_events AS (
            SELECT 
                deviceId,
                syncTime,
                syncStatus,
                LAG(syncStatus) OVER (PARTITION BY deviceId ORDER BY syncTime) as prev_status
            FROM device_sync_history
            WHERE deviceId = :deviceId
            AND date(syncTime) BETWEEN date(:startDate) AND date(:endDate)
        ),
        connections AS (
            SELECT COUNT(*) as connectionCount
            FROM connection_events
            WHERE syncStatus = 'CONNECTED' AND prev_status = 'DISCONNECTED'
        ),
        disconnections AS (
            SELECT COUNT(*) as disconnectionCount
            FROM connection_events
            WHERE syncStatus = 'DISCONNECTED' AND prev_status = 'CONNECTED'
        ),
        sync_success AS (
            SELECT COUNT(*) as successCount
            FROM device_sync_history
            WHERE deviceId = :deviceId
            AND syncStatus = 'SUCCESS'
            AND date(syncTime) BETWEEN date(:startDate) AND date(:endDate)
        ),
        sync_failed AS (
            SELECT COUNT(*) as failedCount
            FROM device_sync_history
            WHERE deviceId = :deviceId
            AND syncStatus = 'FAILED'
            AND date(syncTime) BETWEEN date(:startDate) AND date(:endDate)
        ),
        total_sync AS (
            SELECT COUNT(*) as totalSyncAttempts
            FROM device_sync_history
            WHERE deviceId = :deviceId
            AND date(syncTime) BETWEEN date(:startDate) AND date(:endDate)
        )
        SELECT 
            (SELECT connectionCount FROM connections) as connectionCount,
            (SELECT disconnectionCount FROM disconnections) as disconnectionCount,
            (SELECT successCount FROM sync_success) as syncSuccessCount,
            (SELECT failedCount FROM sync_failed) as syncFailedCount,
            (SELECT totalSyncAttempts FROM total_sync) as totalSyncAttempts,
            CASE 
                WHEN (SELECT totalSyncAttempts FROM total_sync) > 0 
                THEN ((SELECT successCount FROM sync_success) * 100.0 / (SELECT totalSyncAttempts FROM total_sync))
                ELSE 0
            END as syncSuccessRate
    """)
    suspend fun getDeviceConnectionStatistics(
        deviceId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): DeviceConnectionStats

    /**
     * Get connection uptime percentage
     * @param deviceId The device ID
     * @param startDate Start date
     * @param endDate End date
     * @return Connection uptime percentage
     */
    @Query("""
        WITH connection_periods AS (
            SELECT 
                syncTime as startTime,
                LEAD(syncTime) OVER (ORDER BY syncTime) as endTime,
                syncStatus
            FROM device_sync_history
            WHERE deviceId = :deviceId
            AND date(syncTime) BETWEEN date(:startDate) AND date(:endDate)
        ),
        connected_time AS (
            SELECT 
                SUM(
                    CASE 
                        WHEN syncStatus = 'CONNECTED' AND endTime IS NOT NULL
                        THEN (julianday(endTime) - julianday(startTime)) * 24 * 60 * 60
                        ELSE 0
                    END
                ) as connectedSeconds
            FROM connection_periods
        ),
        total_time AS (
            SELECT 
                (julianday(:endDate) - julianday(:startDate) + 1) * 24 * 60 * 60 as totalSeconds
            FROM devices
            LIMIT 1
        )
        SELECT 
            (SELECT connectedSeconds FROM connected_time) as connectedSeconds,
            (SELECT totalSeconds FROM total_time) as totalSeconds,
            CASE 
                WHEN (SELECT totalSeconds FROM total_time) > 0 
                THEN ((SELECT connectedSeconds FROM connected_time) * 100.0 / (SELECT totalSeconds FROM total_time))
                ELSE 0
            END as uptimePercentage
    """)
    suspend fun getConnectionUptimePercentage(
        deviceId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): DeviceUptime

    /**
     * Battery Level Monitoring
     */

    /**
     * Update device battery level
     * @param deviceId The device ID
     * @param batteryLevel The new battery level (percentage)
     * @param updateTime The time of the update
     * @return Number of rows updated
     */
    @Query("""
        UPDATE devices 
        SET 
            batteryLevel = :batteryLevel, 
            lastBatteryUpdateTime = :updateTime,
            lastSeenTime = :updateTime,
            modifiedAt = :updateTime
        WHERE id = :deviceId
    """)
    suspend fun updateBatteryLevel(
        deviceId: String,
        batteryLevel: Int,
        updateTime: LocalDateTime = LocalDateTime.now()
    ): Int

    /**
     * Get battery level history
     * @param deviceId The device ID
     * @param startDate Start date
     * @param endDate End date
     * @return List of battery level history records
     */
    @Query("""
        SELECT 
            deviceId,
            syncTime as timestamp,
            batteryLevel
        FROM device_sync_history
        WHERE deviceId = :deviceId
        AND batteryLevel IS NOT NULL
        AND date(syncTime) BETWEEN date(:startDate) AND date(:endDate)
        ORDER BY syncTime
    """)
    suspend fun getBatteryLevelHistory(
        deviceId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<BatteryLevelHistory>

    /**
     * Get battery drain rate
     * @param deviceId The device ID
     * @param startDate Start date
     * @param endDate End date
     * @return Battery drain rate (percentage per hour)
     */
    @Query("""
        WITH battery_changes AS (
            SELECT 
                deviceId,
                syncTime,
                batteryLevel,
                LAG(syncTime) OVER (PARTITION BY deviceId ORDER BY syncTime) as prev_time,
                LAG(batteryLevel) OVER (PARTITION BY deviceId ORDER BY syncTime) as prev_level
            FROM device_sync_history
            WHERE deviceId = :deviceId
            AND batteryLevel IS NOT NULL
            AND date(syncTime) BETWEEN date(:startDate) AND date(:endDate)
        ),
        drain_rates AS (
            SELECT 
                (prev_level - batteryLevel) as level_drop,
                (julianday(syncTime) - julianday(prev_time)) * 24 as hours_elapsed,
                CASE 
                    WHEN (julianday(syncTime) - julianday(prev_time)) * 24 > 0
                    THEN (prev_level - batteryLevel) / ((julianday(syncTime) - julianday(prev_time)) * 24)
                    ELSE 0
                END as drain_rate_per_hour
            FROM battery_changes
            WHERE prev_level > batteryLevel
            AND prev_time IS NOT NULL
            AND (julianday(syncTime) - julianday(prev_time)) * 24 < 24 -- Exclude gaps longer than 24 hours
        )
        SELECT 
            AVG(drain_rate_per_hour) as avgDrainRatePerHour,
            MAX(drain_rate_per_hour) as maxDrainRatePerHour,
            MIN(drain_rate_per_hour) as minDrainRatePerHour,
            COUNT(*) as sampleCount
        FROM drain_rates
        WHERE drain_rate_per_hour > 0
    """)
    suspend fun getBatteryDrainRate(
        deviceId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): BatteryDrainRate?

    /**
     * Estimate time until battery depletion
     * @param deviceId The device ID
     * @return Estimated hours until battery depletion
     */
    @Query("""
        WITH current_battery AS (
            SELECT batteryLevel
            FROM devices
            WHERE id = :deviceId
        ),
        recent_drain_rate AS (
            WITH battery_changes AS (
                SELECT 
                    deviceId,
                    syncTime,
                    batteryLevel,
                    LAG(syncTime) OVER (PARTITION BY deviceId ORDER BY syncTime) as prev_time,
                    LAG(batteryLevel) OVER (PARTITION BY deviceId ORDER BY syncTime) as prev_level
                FROM device_sync_history
                WHERE deviceId = :deviceId
                AND batteryLevel IS NOT NULL
                AND syncTime >= datetime('now', '-7 days')
            ),
            drain_rates AS (
                SELECT 
                    (prev_level - batteryLevel) as level_drop,
                    (julianday(syncTime) - julianday(prev_time)) * 24 as hours_elapsed,
                    CASE 
                        WHEN (julianday(syncTime) - julianday(prev_time)) * 24 > 0
                        THEN (prev_level - batteryLevel) / ((julianday(syncTime) - julianday(prev_time)) * 24)
                        ELSE 0
                    END as drain_rate_per_hour
                FROM battery_changes
                WHERE prev_level > batteryLevel
                AND prev_time IS NOT NULL
                AND (julianday(syncTime) - julianday(prev_time)) * 24 < 24 -- Exclude gaps longer than 24 hours
            )
            SELECT AVG(drain_rate_per_hour) as avgDrainRatePerHour
            FROM drain_rates
            WHERE drain_rate_per_hour > 0
        )
        SELECT 
            (SELECT batteryLevel FROM current_battery) as currentBatteryLevel,
            (SELECT avgDrainRatePerHour FROM recent_drain_rate) as drainRatePerHour,
            CASE 
                WHEN (SELECT avgDrainRatePerHour FROM recent_drain_rate) > 0
                THEN (SELECT batteryLevel FROM current_battery) / (SELECT avgDrainRatePerHour FROM recent_drain_rate)
                ELSE -1 -- Indicates insufficient data
            END as estimatedHoursRemaining
    """)
    suspend fun estimateBatteryTimeRemaining(deviceId: String): BatteryEstimate?

    /**
     * Sync Status Management
     */

    /**
     * Update device sync status
     * @param deviceId The device ID
     * @param syncStatus The new sync status
     * @param syncTime The time of the sync
     * @param dataPoints Number of data points synced
     * @param errorMessage Error message if sync failed
     * @return Number of rows updated
     */
    @Query("""
        UPDATE devices 
        SET 
            syncStatus = :syncStatus, 
            lastSyncTime = :syncTime,
            lastSeenTime = :syncTime,
            lastSyncDataPoints = :dataPoints,
            lastSyncError = :errorMessage,
            modifiedAt = :syncTime
        WHERE id = :deviceId
    """)
    suspend fun updateSyncStatus(
        deviceId: String,
        syncStatus: String,
        syncTime: LocalDateTime = LocalDateTime.now(),
        dataPoints: Int = 0,
        errorMessage: String? = null
    ): Int

    /**
     * Record sync history
     * @param deviceId The device ID
     * @param syncStatus The sync status
     * @param syncTime The time of the sync
     * @param dataPoints Number of data points synced
     * @param batteryLevel Battery level at time of sync
     * @param errorMessage Error message if sync failed
     * @return ID of the inserted record
     */
    @Transaction
    suspend fun recordSyncEvent(
        deviceId: String,
        syncStatus: String,
        syncTime: LocalDateTime = LocalDateTime.now(),
        dataPoints: Int = 0,
        batteryLevel: Int? = null,
        errorMessage: String? = null
    ): Long {
        val syncHistory = DeviceSyncHistoryEntity(
            id = 0, // Auto-generated
            deviceId = deviceId,
            syncTime = syncTime,
            syncStatus = syncStatus,
            dataPoints = dataPoints,
            batteryLevel = batteryLevel,
            errorMessage = errorMessage
        )
        
        // Update the device sync status
        updateSyncStatus(deviceId, syncStatus, syncTime, dataPoints, errorMessage)
        
        // Insert the sync history record
        return insertSyncHistory(syncHistory)
    }

    /**
     * Get sync success rate
     * @param deviceId The device ID
     * @param startDate Start date
     * @param endDate End date
     * @return Sync success rate metrics
     */
    @Query("""
        WITH sync_counts AS (
            SELECT 
                COUNT(*) as totalAttempts,
                SUM(CASE WHEN syncStatus = 'SUCCESS' THEN 1 ELSE 0 END) as successCount,
                SUM(CASE WHEN syncStatus = 'FAILED' THEN 1 ELSE 0 END) as failedCount
            FROM device_sync_history
            WHERE deviceId = :deviceId
            AND date(syncTime) BETWEEN date(:startDate) AND date(:endDate)
            AND syncStatus IN ('SUCCESS', 'FAILED')
        )
        SELECT 
            totalAttempts,
            successCount,
            failedCount,
            CASE 
                WHEN totalAttempts > 0 THEN (successCount * 100.0 / totalAttempts)
                ELSE 0
            END as successRate
        FROM sync_counts
    """)
    suspend fun getSyncSuccessRate(
        deviceId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): SyncSuccessRate

    /**
     * Get sync frequency
     * @param deviceId The device ID
     * @param startDate Start date
     * @param endDate End date
     * @return Sync frequency metrics
     */
    @Query("""
        WITH sync_events AS (
            SELECT 
                syncTime,
                LAG(syncTime) OVER (ORDER BY syncTime) as prev_sync_time
            FROM device_sync_history
            WHERE deviceId = :deviceId
            AND date(syncTime) BETWEEN date(:startDate) AND date(:endDate)
            AND syncStatus = 'SUCCESS'
        ),
        sync_intervals AS (
            SELECT 
                (julianday(syncTime) - julianday(prev_sync_time)) * 24 * 60 as minutes_between_syncs
            FROM sync_events
            WHERE prev_sync_time IS NOT NULL
        )
        SELECT 
            COUNT(*) as syncCount,
            (julianday(:endDate) - julianday(:startDate) + 1) as daysCovered,
            (COUNT(*) / (julianday(:endDate) - julianday(:startDate) + 1)) as syncsPerDay,
            AVG(minutes_between_syncs) as avgMinutesBetweenSyncs,
            MIN(minutes_between_syncs) as minMinutesBetweenSyncs,
            MAX(minutes_between_syncs) as maxMinutesBetweenSyncs
        FROM sync_intervals
    """)
    suspend fun getSyncFrequency(
        deviceId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): SyncFrequency?

    /**
     * Get daily sync counts
     * @param deviceId The device ID
     * @param startDate Start date
     * @param endDate End date
     * @return Daily sync count metrics
     */
    @Query("""
        SELECT 
            date(syncTime) as date,
            COUNT(*) as totalSyncs,
            SUM(CASE WHEN syncStatus = 'SUCCESS' THEN 1 ELSE 0 END) as successfulSyncs,
            SUM(CASE WHEN syncStatus = 'FAILED' THEN 1 ELSE 0 END) as failedSyncs,
            SUM(dataPoints) as totalDataPoints
        FROM device_sync_history
        WHERE deviceId = :deviceId
        AND date(syncTime) BETWEEN date(:startDate) AND date(:endDate)
        GROUP BY date(syncTime)
        ORDER BY date(syncTime)
    """)
    suspend fun getDailySyncCounts(
        deviceId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<DailySyncCount>

    /**
     * Get common sync errors
     * @param deviceId The device ID
     * @param startDate Start date
     * @param endDate End date
     * @return List of common sync errors and their frequency
     */
    @Query("""
        SELECT 
            errorMessage,
            COUNT(*) as errorCount
        FROM device_sync_history
        WHERE deviceId = :deviceId
        AND date(syncTime) BETWEEN date(:startDate) AND date(:endDate)
        AND syncStatus = 'FAILED'
        AND errorMessage IS NOT NULL
        GROUP BY errorMessage
        ORDER BY errorCount DESC
    """)
    suspend fun getCommonSyncErrors(
        deviceId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<SyncError>

    /**
     * Device Settings Management
     */

    /**
     * Get device setting value
     * @param deviceId The device ID
     * @param settingKey The setting key
     * @return The setting value or null if not found
     */
    @Query("SELECT settingValue FROM device_settings WHERE deviceId = :deviceId AND settingKey = :settingKey")
    suspend fun getSettingValue(deviceId: String, settingKey: String): String?

    /**
     * Get device setting value as Flow for reactive updates
     * @param deviceId The device ID
     * @param settingKey The setting key
     * @return Flow emitting the setting value
     */
    @Query("SELECT settingValue FROM device_settings WHERE deviceId = :deviceId AND settingKey = :settingKey")
    fun getSettingValueAsFlow(deviceId: String, settingKey: String): Flow<String?>

    /**
     * Set device setting value
     * @param deviceId The device ID
     * @param settingKey The setting key
     * @param settingValue The setting value
     * @param updateTime The time of the update
     * @return Number of rows updated or inserted
     */
    @Transaction
    suspend fun setSettingValue(
        deviceId: String,
        settingKey: String,
        settingValue: String,
        updateTime: LocalDateTime = LocalDateTime.now()
    ): Long {
        val existing = getDeviceSettingByKey(deviceId, settingKey)
        
        return if (existing != null) {
            val updated = existing.copy(
                settingValue = settingValue,
                modifiedAt = updateTime
            )
            updateSetting(updated)
            existing.id.toLong()
        } else {
            val newSetting = DeviceSettingEntity(
                id = 0, // Auto-generated
                deviceId = deviceId,
                settingKey = settingKey,
                settingValue = settingValue,
                createdAt = updateTime,
                modifiedAt = updateTime
            )
            insertSetting(newSetting)
        }
    }

    /**
     * Check if setting exists
     * @param deviceId The device ID
     * @param settingKey The setting key
     * @return True if setting exists, false otherwise
     */
    @Query("SELECT EXISTS(SELECT 1 FROM device_settings WHERE deviceId = :deviceId AND settingKey = :settingKey)")
    suspend fun settingExists(deviceId: String, settingKey: String): Boolean

    /**
     * Get all settings as key-value map
     * @param deviceId The device ID
     * @return Map of setting keys to values
     */
    @Query("SELECT settingKey, settingValue FROM device_settings WHERE deviceId = :deviceId")
    suspend fun getAllSettingsAsMap(deviceId: String): Map<String, String>

    /**
     * Pairing and Unpairing Operations
     */

    /**
     * Pair a device
     * @param deviceId The device ID
     * @param pairingTime The time of pairing
     * @return Number of rows updated
     */
    @Query("""
        UPDATE devices 
        SET 
            isPaired = 1, 
            pairingTime = :pairingTime,
            lastSeenTime = :pairingTime,
            modifiedAt = :pairingTime
        WHERE id = :deviceId
    """)
    suspend fun pairDevice(
        deviceId: String,
        pairingTime: LocalDateTime = LocalDateTime.now()
    ): Int

    /**
     * Unpair a device
     * @param deviceId The device ID
     * @param unpairingTime The time of unpairing
     * @return Number of rows updated
     */
    @Query("""
        UPDATE devices 
        SET 
            isPaired = 0, 
            isConnected = 0,
            unpairingTime = :unpairingTime,
            lastSeenTime = :unpairingTime,
            modifiedAt = :unpairingTime
        WHERE id = :deviceId
    """)
    suspend fun unpairDevice(
        deviceId: String,
        unpairingTime: LocalDateTime = LocalDateTime.now()
    ): Int

    /**
     * Get all paired devices for a specific user
     * @param userId The user ID
     * @return List of paired device entities
     */
    @Query("SELECT * FROM devices WHERE userId = :userId AND isPaired = 1 ORDER BY name")
    suspend fun getPairedDevices(userId: String): List<DeviceEntity>

    /**
     * Get all paired devices for a specific user as Flow for reactive updates
     * @param userId The user ID
     * @return Flow emitting list of paired device entities
     */
    @Query("SELECT * FROM devices WHERE userId = :userId AND isPaired = 1 ORDER BY name")
    fun getPairedDevicesAsFlow(userId: String): Flow<List<DeviceEntity>>

    /**
     * Get all unpaired devices for a specific user
     * @param userId The user ID
     * @return List of unpaired device entities
     */
    @Query("SELECT * FROM devices WHERE userId = :userId AND isPaired = 0 ORDER BY name")
    suspend fun getUnpairedDevices(userId: String): List<DeviceEntity>

    /**
     * Get pairing history
     * @param deviceId The device ID
     * @return List of pairing and unpairing events
     */
    @Query("""
        SELECT 
            'PAIRED' as eventType,
            pairingTime as eventTime
        FROM devices
        WHERE id = :deviceId AND pairingTime IS NOT NULL
        
        UNION ALL
        
        SELECT 
            'UNPAIRED' as eventType,
            unpairingTime as eventTime
        FROM devices
        WHERE id = :deviceId AND unpairingTime IS NOT NULL
        
        ORDER BY eventTime DESC
    """)
    suspend fun getPairingHistory(deviceId: String): List<PairingEvent>

    /**
     * Device Health Monitoring
     */

    /**
     * Update device health status
     * @param deviceId The device ID
     * @param firmwareVersion Firmware version
     * @param signalStrength Signal strength (RSSI)
     * @param errorCount Number of errors
     * @param healthStatus Overall health status
     * @param updateTime The time of the update
     * @return Number of rows updated
     */
    @Query("""
        UPDATE devices 
        SET 
            firmwareVersion = :firmwareVersion, 
            signalStrength = :signalStrength,
            errorCount = :errorCount,
            healthStatus = :healthStatus,
            lastHealthCheckTime = :updateTime,
            lastSeenTime = :updateTime,
            modifiedAt = :updateTime
        WHERE id = :deviceId
    """)
    suspend fun updateDeviceHealth(
        deviceId: String,
        firmwareVersion: String?,
        signalStrength: Int?,
        errorCount: Int?,
        healthStatus: String?,
        updateTime: LocalDateTime = LocalDateTime.now()
    ): Int

    /**
     * Get devices needing firmware update
     * @param userId The user ID
     * @return List of devices needing firmware update
     */
    @Query("""
        SELECT * FROM devices 
        WHERE userId = :userId 
        AND (
            firmwareUpdateAvailable = 1 OR
            (latestFirmwareVersion IS NOT NULL AND firmwareVersion IS NOT NULL AND latestFirmwareVersion != firmwareVersion)
        )
        ORDER BY name
    """)
    suspend fun getDevicesNeedingFirmwareUpdate(userId: String): List<DeviceEntity>

    /**
     * Get devices with health issues
     * @param userId The user ID
     * @return List of devices with health issues
     */
    @Query("""
        SELECT * FROM devices 
        WHERE userId = :userId 
        AND (
            healthStatus = 'POOR' OR
            errorCount > 5 OR
            (signalStrength IS NOT NULL AND signalStrength < -80)
        )
        ORDER BY 
            CASE healthStatus
                WHEN 'POOR' THEN 1
                WHEN 'FAIR' THEN 2
                ELSE 3
            END,
            errorCount DESC
    """)
    suspend fun getDevicesWithHealthIssues(userId: String): List<DeviceEntity>

    /**
     * Get signal strength history
     * @param deviceId The device ID
     * @param startDate Start date
     * @param endDate End date
     * @return List of signal strength history records
     */
    @Query("""
        SELECT 
            deviceId,
            syncTime as timestamp,
            signalStrength
        FROM device_sync_history
        WHERE deviceId = :deviceId
        AND signalStrength IS NOT NULL
        AND date(syncTime) BETWEEN date(:startDate) AND date(:endDate)
        ORDER BY syncTime
    """)
    suspend fun getSignalStrengthHistory(
        deviceId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<SignalStrengthHistory>

    /**
     * Get error count history
     * @param deviceId The device ID
     * @param startDate Start date
     * @param endDate End date
     * @return Daily error counts
     */
    @Query("""
        SELECT 
            date(syncTime) as date,
            COUNT(CASE WHEN syncStatus = 'FAILED' THEN 1 END) as errorCount,
            COUNT(*) as totalEvents
        FROM device_sync_history
        WHERE deviceId = :deviceId
        AND date(syncTime) BETWEEN date(:startDate) AND date(:endDate)
        GROUP BY date(syncTime)
        ORDER BY date(syncTime)
    """)
    suspend fun getErrorCountHistory(
        deviceId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<DailyErrorCount>

    /**
     * Last Seen/Activity Tracking
     */

    /**
     * Update device last seen time
     * @param deviceId The device ID
     * @param lastSeenTime The time the device was last seen
     * @return Number of rows updated
     */
    @Query("""
        UPDATE devices 
        SET 
            lastSeenTime = :lastSeenTime,
            modifiedAt = :lastSeenTime
        WHERE id = :deviceId
    """)
    suspend fun updateLastSeenTime(
        deviceId: String,
        lastSeenTime: LocalDateTime = LocalDateTime.now()
    ): Int

    /**
     * Get recently active devices
     * @param userId The user ID
     * @param hoursThreshold Hours threshold to consider a device recently active
     * @return List of recently active device entities
     */
    @Query("""
        SELECT * FROM devices 
        WHERE userId = :userId 
        AND lastSeenTime >= datetime('now', '-' || :hoursThreshold || ' hours')
        ORDER BY lastSeenTime DESC
    """)
    suspend fun getRecentlyActiveDevices(userId: String, hoursThreshold: Int = 24): List<DeviceEntity>

    /**
     * Get inactive devices
     * @param userId The user ID
     * @param daysThreshold Days threshold to consider a device inactive
     * @return List of inactive device entities
     */
    @Query("""
        SELECT * FROM devices 
        WHERE userId = :userId 
        AND (
            lastSeenTime IS NULL OR
            lastSeenTime < datetime('now', '-' || :daysThreshold || ' days')
        )
        ORDER BY lastSeenTime
    """)
    suspend fun getInactiveDevices(userId: String, daysThreshold: Int = 30): List<DeviceEntity>

    /**
     * Get device activity summary
     * @param deviceId The device ID
     * @param startDate Start date
     * @param endDate End date
     * @return Device activity summary metrics
     */
    @Query("""
        WITH sync_events AS (
            SELECT 
                date(syncTime) as date,
                COUNT(*) as syncCount,
                SUM(dataPoints) as dataPointsCount
            FROM device_sync_history
            WHERE deviceId = :deviceId
            AND date(syncTime) BETWEEN date(:startDate) AND date(:endDate)
            GROUP BY date(syncTime)
        ),
        all_dates AS (
            SELECT date(:startDate) + (n - 1) as date
            FROM (
                SELECT 1 as n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION
                SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION
                SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15 UNION
                SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19 UNION SELECT 20 UNION
                SELECT 21 UNION SELECT 22 UNION SELECT 23 UNION SELECT 24 UNION SELECT 25 UNION
                SELECT 26 UNION SELECT 27 UNION SELECT 28 UNION SELECT 29 UNION SELECT 30 UNION
                SELECT 31
            )
            WHERE date < date(:endDate, '+1 day')
        ),
        daily_activity AS (
            SELECT 
                ad.date,
                COALESCE(se.syncCount, 0) as syncCount,
                COALESCE(se.dataPointsCount, 0) as dataPointsCount
            FROM all_dates ad
            LEFT JOIN sync_events se ON ad.date = se.date
        )
        SELECT 
            COUNT(*) as totalDays,
            SUM(CASE WHEN syncCount > 0 THEN 1 ELSE 0 END) as activeDays,
            (SUM(CASE WHEN syncCount > 0 THEN 1 ELSE 0 END) * 100.0 / COUNT(*)) as activeDaysPercentage,
            SUM(syncCount) as totalSyncEvents,
            SUM(dataPointsCount) as totalDataPoints,
            AVG(CASE WHEN syncCount > 0 THEN syncCount ELSE NULL END) as avgSyncsPerActiveDay,
            MAX(syncCount) as maxSyncsInOneDay
        FROM daily_activity
    """)
    suspend fun getDeviceActivitySummary(
        deviceId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): DeviceActivitySummary

    /**
     * Get device usage patterns by day of week
     * @param deviceId The device ID
     * @param startDate Start date
     * @param endDate End date
     * @return Device usage patterns by day of week
     */
    @Query("""
        SELECT 
            strftime('%w', syncTime) as dayOfWeek,
            COUNT(*) as syncCount,
            SUM(dataPoints) as dataPointsCount,
            COUNT(DISTINCT date(syncTime)) as uniqueDays
        FROM device_sync_history
        WHERE deviceId = :deviceId
        AND date(syncTime) BETWEEN date(:startDate) AND date(:endDate)
        GROUP BY strftime('%w', syncTime)
        ORDER BY dayOfWeek
    """)
    suspend fun getDeviceUsageByDayOfWeek(
        deviceId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<DayOfWeekDeviceUsage>

    /**
     * Get device usage patterns by hour of day
     * @param deviceId The device ID
     * @param startDate Start date
     * @param endDate End date
     * @return Device usage patterns by hour of day
     */
    @Query("""
        SELECT 
            strftime('%H', syncTime) as hourOfDay,
            COUNT(*) as syncCount,
            SUM(dataPoints) as dataPointsCount
        FROM device_sync_history
        WHERE deviceId = :deviceId
        AND date(syncTime) BETWEEN date(:startDate) AND date(:endDate)
        GROUP BY strftime('%H', syncTime)
        ORDER BY hourOfDay
    """)
    suspend fun getDeviceUsageByHourOfDay(
        deviceId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<HourOfDayDeviceUsage>

    /**
     * Data classes for query results
     */

    /**
     * Device connection statistics
     */
    data class DeviceConnectionStats(
        val connectionCount: Int,
        val disconnectionCount: Int,
        val syncSuccessCount: Int,
        val syncFailedCount: Int,
        val totalSyncAttempts: Int,
        val syncSuccessRate: Double
    )

    /**
     * Device uptime
     */
    data class DeviceUptime(
        val connectedSeconds: Long,
        val totalSeconds: Long,
        val uptimePercentage: Double
    )

    /**
     * Battery level history
     */
    data class BatteryLevelHistory(
        val deviceId: String,
        val timestamp: LocalDateTime,
        val batteryLevel: Int
    )

    /**
     * Battery drain rate
     */
    data class BatteryDrainRate(
        val avgDrainRatePerHour: Double,
        val maxDrainRatePerHour: Double,
        val minDrainRatePerHour: Double,
        val sampleCount: Int
    )

    /**
     * Battery estimate
     */
    data class BatteryEstimate(
        val currentBatteryLevel: Int,
        val drainRatePerHour: Double,
        val estimatedHoursRemaining: Double
    )

    /**
     * Sync success rate
     */
    data class SyncSuccessRate(
        val totalAttempts: Int,
        val successCount: Int,
        val failedCount: Int,
        val successRate: Double
    )

    /**
     * Sync frequency
     */
    data class SyncFrequency(
        val syncCount: Int,
        val daysCovered: Double,
        val syncsPerDay: Double,
        val avgMinutesBetweenSyncs: Double,
        val minMinutesBetweenSyncs: Double,
        val maxMinutesBetweenSyncs: Double
    )

    /**
     * Daily sync count
     */
    data class DailySyncCount(
        val date: LocalDate,
        val totalSyncs: Int,
        val successfulSyncs: Int,
        val failedSyncs: Int,
        val totalDataPoints: Int
    )

    /**
     * Sync error
     */
    data class SyncError(
        val errorMessage: String,
        val errorCount: Int
    )

    /**
     * Pairing event
     */
    data class PairingEvent(
        val eventType: String,
        val eventTime: LocalDateTime
    )

    /**
     * Signal strength history
     */
    data class SignalStrengthHistory(
        val deviceId: String,
        val timestamp: LocalDateTime,
        val signalStrength: Int
    )

    /**
     * Daily error count
     */
    data class DailyErrorCount(
        val date: LocalDate,
        val errorCount: Int,
        val totalEvents: Int
    )

    /**
     * Device activity summary
     */
    data class DeviceActivitySummary(
        val totalDays: Int,
        val activeDays: Int,
        val activeDaysPercentage: Double,
        val totalSyncEvents: Int,
        val totalDataPoints: Int,
        val avgSyncsPerActiveDay: Double,
        val maxSyncsInOneDay: Int
    )

    /**
     * Day of week device usage
     */
    data class DayOfWeekDeviceUsage(
        val dayOfWeek: String,
        val syncCount: Int,
        val dataPointsCount: Int,
        val uniqueDays: Int
    )

    /**
     * Hour of day device usage
     */
    data class HourOfDayDeviceUsage(
        val hourOfDay: String,
        val syncCount: Int,
        val dataPointsCount: Int
    )
}
