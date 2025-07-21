package com.sensacare.app.data.dao

import androidx.room.*
import com.sensacare.app.data.entity.DeviceEntity
import com.sensacare.app.data.entity.DeviceWithMeasurements
import com.sensacare.app.data.entity.SyncStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * DeviceDao - Data Access Object for Device operations
 *
 * This DAO provides comprehensive access to device data with:
 * - CRUD operations
 * - Status tracking (pairing, connection)
 * - Feature management
 * - Sync status operations
 * - Search functionality
 * - Complex queries with joins
 * - Flow-based reactive data
 * - Coroutine support
 */
@Dao
interface DeviceDao {

    /**
     * ====================================
     * Basic CRUD Operations
     * ====================================
     */

    /**
     * Insert a new device and return its ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(device: DeviceEntity): Long

    /**
     * Insert multiple devices
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(devices: List<DeviceEntity>): List<Long>

    /**
     * Update an existing device
     */
    @Update
    suspend fun update(device: DeviceEntity): Int

    /**
     * Update multiple devices
     */
    @Update
    suspend fun updateAll(devices: List<DeviceEntity>): Int

    /**
     * Delete a device
     */
    @Delete
    suspend fun delete(device: DeviceEntity): Int

    /**
     * Delete a device by ID
     */
    @Query("DELETE FROM devices WHERE id = :deviceId")
    suspend fun deleteById(deviceId: Long): Int

    /**
     * Delete all devices
     */
    @Query("DELETE FROM devices")
    suspend fun deleteAll(): Int

    /**
     * ====================================
     * Basic Query Operations
     * ====================================
     */

    /**
     * Get a device by ID
     */
    @Query("SELECT * FROM devices WHERE id = :deviceId")
    suspend fun getDeviceById(deviceId: Long): DeviceEntity?

    /**
     * Get a device by ID as Flow
     */
    @Query("SELECT * FROM devices WHERE id = :deviceId")
    fun getDeviceByIdFlow(deviceId: Long): Flow<DeviceEntity?>

    /**
     * Get a device by MAC address
     */
    @Query("SELECT * FROM devices WHERE macAddress = :macAddress")
    suspend fun getDeviceByMacAddress(macAddress: String): DeviceEntity?

    /**
     * Get all devices
     */
    @Query("SELECT * FROM devices ORDER BY name ASC")
    suspend fun getAllDevices(): List<DeviceEntity>

    /**
     * Get all devices as Flow
     */
    @Query("SELECT * FROM devices ORDER BY name ASC")
    fun getAllDevicesFlow(): Flow<List<DeviceEntity>>

    /**
     * Count all devices
     */
    @Query("SELECT COUNT(*) FROM devices")
    suspend fun getDeviceCount(): Int

    /**
     * ====================================
     * Active Device Operations
     * ====================================
     */

    /**
     * Get all active devices
     */
    @Query("SELECT * FROM devices WHERE isActive = 1 ORDER BY name ASC")
    suspend fun getActiveDevices(): List<DeviceEntity>

    /**
     * Get all active devices as Flow
     */
    @Query("SELECT * FROM devices WHERE isActive = 1 ORDER BY name ASC")
    fun getActiveDevicesFlow(): Flow<List<DeviceEntity>>

    /**
     * Set a device as active or inactive
     */
    @Query("UPDATE devices SET isActive = :isActive, updatedAt = :timestamp WHERE id = :deviceId")
    suspend fun setDeviceActive(deviceId: Long, isActive: Boolean, timestamp: Long = System.currentTimeMillis()): Int

    /**
     * Get the most recently connected device
     */
    @Query("SELECT * FROM devices WHERE lastConnected IS NOT NULL ORDER BY lastConnected DESC LIMIT 1")
    suspend fun getMostRecentlyConnectedDevice(): DeviceEntity?

    /**
     * ====================================
     * Device Pairing Status Operations
     * ====================================
     */

    /**
     * Get all paired devices
     */
    @Query("SELECT * FROM devices WHERE isPaired = 1 ORDER BY name ASC")
    suspend fun getPairedDevices(): List<DeviceEntity>

    /**
     * Get all paired devices as Flow
     */
    @Query("SELECT * FROM devices WHERE isPaired = 1 ORDER BY name ASC")
    fun getPairedDevicesFlow(): Flow<List<DeviceEntity>>

    /**
     * Set a device as paired or unpaired
     */
    @Query("UPDATE devices SET isPaired = :isPaired, pairingTime = :pairingTime, updatedAt = :timestamp WHERE id = :deviceId")
    suspend fun setDevicePaired(deviceId: Long, isPaired: Boolean, pairingTime: Long? = if (isPaired) System.currentTimeMillis() else null, timestamp: Long = System.currentTimeMillis()): Int

    /**
     * Get all unpaired devices
     */
    @Query("SELECT * FROM devices WHERE isPaired = 0 OR isPaired IS NULL ORDER BY name ASC")
    suspend fun getUnpairedDevices(): List<DeviceEntity>

    /**
     * ====================================
     * Connection Status Operations
     * ====================================
     */

    /**
     * Update device connection status and time
     */
    @Query("UPDATE devices SET lastConnected = :timestamp, batteryLevel = :batteryLevel, updatedAt = :timestamp WHERE id = :deviceId")
    suspend fun updateDeviceConnection(deviceId: Long, batteryLevel: Int? = null, timestamp: Long = System.currentTimeMillis()): Int

    /**
     * Get devices connected within the last 24 hours
     */
    @Query("SELECT * FROM devices WHERE lastConnected >= :timestamp ORDER BY lastConnected DESC")
    suspend fun getRecentlyConnectedDevices(timestamp: Long = System.currentTimeMillis() - (24 * 60 * 60 * 1000)): List<DeviceEntity>

    /**
     * Get devices not connected for more than a week
     */
    @Query("SELECT * FROM devices WHERE lastConnected IS NULL OR lastConnected < :timestamp ORDER BY lastConnected ASC")
    suspend fun getInactiveDevices(timestamp: Long = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)): List<DeviceEntity>

    /**
     * Get devices with low battery (less than 20%)
     */
    @Query("SELECT * FROM devices WHERE batteryLevel IS NOT NULL AND batteryLevel < 20 ORDER BY batteryLevel ASC")
    suspend fun getLowBatteryDevices(): List<DeviceEntity>

    /**
     * Get devices with low battery as Flow
     */
    @Query("SELECT * FROM devices WHERE batteryLevel IS NOT NULL AND batteryLevel < 20 ORDER BY batteryLevel ASC")
    fun getLowBatteryDevicesFlow(): Flow<List<DeviceEntity>>

    /**
     * ====================================
     * Device Feature Management
     * ====================================
     */

    /**
     * Get devices that support a specific feature
     */
    @Query("SELECT * FROM devices WHERE features LIKE '%' || :feature || '%' ORDER BY name ASC")
    suspend fun getDevicesWithFeature(feature: String): List<DeviceEntity>

    /**
     * Get devices that support a specific feature as Flow
     */
    @Query("SELECT * FROM devices WHERE features LIKE '%' || :feature || '%' ORDER BY name ASC")
    fun getDevicesWithFeatureFlow(feature: String): Flow<List<DeviceEntity>>

    /**
     * Get devices that support multiple features
     */
    @Query("""
        SELECT * FROM devices 
        WHERE features LIKE '%' || :feature1 || '%' 
        AND features LIKE '%' || :feature2 || '%' 
        ORDER BY name ASC
    """)
    suspend fun getDevicesWithFeatures(feature1: String, feature2: String): List<DeviceEntity>

    /**
     * Update device features
     */
    @Query("UPDATE devices SET features = :features, updatedAt = :timestamp WHERE id = :deviceId")
    suspend fun updateDeviceFeatures(deviceId: Long, features: String, timestamp: Long = System.currentTimeMillis()): Int

    /**
     * ====================================
     * Sync Status Operations
     * ====================================
     */

    /**
     * Get devices by sync status
     */
    @Query("SELECT * FROM devices WHERE syncStatus = :syncStatus ORDER BY updatedAt DESC")
    suspend fun getDevicesBySyncStatus(syncStatus: SyncStatus): List<DeviceEntity>

    /**
     * Get devices by sync status as Flow
     */
    @Query("SELECT * FROM devices WHERE syncStatus = :syncStatus ORDER BY updatedAt DESC")
    fun getDevicesBySyncStatusFlow(syncStatus: SyncStatus): Flow<List<DeviceEntity>>

    /**
     * Update device sync status
     */
    @Query("UPDATE devices SET syncStatus = :syncStatus, updatedAt = :timestamp WHERE id = :deviceId")
    suspend fun updateDeviceSyncStatus(deviceId: Long, syncStatus: SyncStatus, timestamp: Long = System.currentTimeMillis()): Int

    /**
     * Update sync status for multiple devices
     */
    @Query("UPDATE devices SET syncStatus = :syncStatus, updatedAt = :timestamp WHERE id IN (:deviceIds)")
    suspend fun updateDevicesSyncStatus(deviceIds: List<Long>, syncStatus: SyncStatus, timestamp: Long = System.currentTimeMillis()): Int

    /**
     * Get devices that need syncing
     */
    @Query("SELECT * FROM devices WHERE syncStatus IN ('PENDING', 'FAILED') ORDER BY updatedAt ASC")
    suspend fun getDevicesNeedingSync(): List<DeviceEntity>

    /**
     * ====================================
     * Search Functionality
     * ====================================
     */

    /**
     * Search devices by name or address
     */
    @Query("""
        SELECT * FROM devices 
        WHERE name LIKE '%' || :query || '%' 
        OR macAddress LIKE '%' || :query || '%'
        OR deviceType LIKE '%' || :query || '%'
        ORDER BY 
        CASE 
            WHEN name LIKE :query || '%' THEN 0
            WHEN name LIKE '%' || :query || '%' THEN 1
            ELSE 2
        END,
        name ASC
    """)
    suspend fun searchDevices(query: String): List<DeviceEntity>

    /**
     * Search devices by name or address as Flow
     */
    @Query("""
        SELECT * FROM devices 
        WHERE name LIKE '%' || :query || '%' 
        OR macAddress LIKE '%' || :query || '%'
        OR deviceType LIKE '%' || :query || '%'
        ORDER BY 
        CASE 
            WHEN name LIKE :query || '%' THEN 0
            WHEN name LIKE '%' || :query || '%' THEN 1
            ELSE 2
        END,
        name ASC
    """)
    fun searchDevicesFlow(query: String): Flow<List<DeviceEntity>>

    /**
     * ====================================
     * Complex Queries with Joins
     * ====================================
     */

    /**
     * Get devices with their latest heart rate measurement
     */
    @Transaction
    @Query("""
        SELECT d.*, hr.heartRate as lastHeartRate, hr.timestamp as lastHeartRateTimestamp
        FROM devices d
        LEFT JOIN (
            SELECT deviceId, heartRate, timestamp, 
                   ROW_NUMBER() OVER (PARTITION BY deviceId ORDER BY timestamp DESC) as rn
            FROM heart_rates
        ) hr ON d.id = hr.deviceId AND hr.rn = 1
        ORDER BY d.name ASC
    """)
    suspend fun getDevicesWithLatestHeartRate(): List<DeviceWithMeasurements>

    /**
     * Get devices with their latest heart rate measurement as Flow
     */
    @Transaction
    @Query("""
        SELECT d.*, hr.heartRate as lastHeartRate, hr.timestamp as lastHeartRateTimestamp
        FROM devices d
        LEFT JOIN (
            SELECT deviceId, heartRate, timestamp, 
                   ROW_NUMBER() OVER (PARTITION BY deviceId ORDER BY timestamp DESC) as rn
            FROM heart_rates
        ) hr ON d.id = hr.deviceId AND hr.rn = 1
        ORDER BY d.name ASC
    """)
    fun getDevicesWithLatestHeartRateFlow(): Flow<List<DeviceWithMeasurements>>

    /**
     * Get devices with their latest step count
     */
    @Transaction
    @Query("""
        SELECT d.*, s.steps as lastSteps, s.timestamp as lastStepsTimestamp
        FROM devices d
        LEFT JOIN (
            SELECT deviceId, steps, timestamp, 
                   ROW_NUMBER() OVER (PARTITION BY deviceId ORDER BY timestamp DESC) as rn
            FROM steps
        ) s ON d.id = s.deviceId AND s.rn = 1
        ORDER BY d.name ASC
    """)
    suspend fun getDevicesWithLatestSteps(): List<DeviceWithMeasurements>

    /**
     * Get devices with activity summary (step count, heart rate, etc.)
     */
    @Transaction
    @Query("""
        SELECT d.*,
               (SELECT COUNT(*) FROM heart_rates WHERE deviceId = d.id) as heartRateCount,
               (SELECT COUNT(*) FROM steps WHERE deviceId = d.id) as stepCount,
               (SELECT COUNT(*) FROM blood_oxygen WHERE deviceId = d.id) as bloodOxygenCount,
               (SELECT COUNT(*) FROM blood_pressure WHERE deviceId = d.id) as bloodPressureCount,
               (SELECT COUNT(*) FROM sleep WHERE deviceId = d.id) as sleepCount
        FROM devices d
        ORDER BY d.name ASC
    """)
    suspend fun getDevicesWithActivitySummary(): List<DeviceWithMeasurements>

    /**
     * Get devices with activity summary as Flow
     */
    @Transaction
    @Query("""
        SELECT d.*,
               (SELECT COUNT(*) FROM heart_rates WHERE deviceId = d.id) as heartRateCount,
               (SELECT COUNT(*) FROM steps WHERE deviceId = d.id) as stepCount,
               (SELECT COUNT(*) FROM blood_oxygen WHERE deviceId = d.id) as bloodOxygenCount,
               (SELECT COUNT(*) FROM blood_pressure WHERE deviceId = d.id) as bloodPressureCount,
               (SELECT COUNT(*) FROM sleep WHERE deviceId = d.id) as sleepCount
        FROM devices d
        ORDER BY d.name ASC
    """)
    fun getDevicesWithActivitySummaryFlow(): Flow<List<DeviceWithMeasurements>>

    /**
     * Get devices with their latest sync time for each measurement type
     */
    @Transaction
    @Query("""
        SELECT d.*,
               (SELECT MAX(updatedAt) FROM heart_rates WHERE deviceId = d.id) as lastHeartRateSync,
               (SELECT MAX(updatedAt) FROM steps WHERE deviceId = d.id) as lastStepsSync,
               (SELECT MAX(updatedAt) FROM blood_oxygen WHERE deviceId = d.id) as lastBloodOxygenSync,
               (SELECT MAX(updatedAt) FROM blood_pressure WHERE deviceId = d.id) as lastBloodPressureSync,
               (SELECT MAX(updatedAt) FROM sleep WHERE deviceId = d.id) as lastSleepSync
        FROM devices d
        ORDER BY d.name ASC
    """)
    suspend fun getDevicesWithLastSyncTimes(): List<DeviceWithMeasurements>

    /**
     * ====================================
     * Advanced Update Operations
     * ====================================
     */

    /**
     * Update device firmware version
     */
    @Query("UPDATE devices SET firmwareVersion = :firmwareVersion, updatedAt = :timestamp WHERE id = :deviceId")
    suspend fun updateDeviceFirmware(deviceId: Long, firmwareVersion: String, timestamp: Long = System.currentTimeMillis()): Int

    /**
     * Update device hardware version
     */
    @Query("UPDATE devices SET hardwareVersion = :hardwareVersion, updatedAt = :timestamp WHERE id = :deviceId")
    suspend fun updateDeviceHardware(deviceId: Long, hardwareVersion: String, timestamp: Long = System.currentTimeMillis()): Int

    /**
     * Update device name
     */
    @Query("UPDATE devices SET name = :name, updatedAt = :timestamp WHERE id = :deviceId")
    suspend fun updateDeviceName(deviceId: Long, name: String, timestamp: Long = System.currentTimeMillis()): Int

    /**
     * Update device battery level
     */
    @Query("UPDATE devices SET batteryLevel = :batteryLevel, updatedAt = :timestamp WHERE id = :deviceId")
    suspend fun updateDeviceBattery(deviceId: Long, batteryLevel: Int, timestamp: Long = System.currentTimeMillis()): Int

    /**
     * ====================================
     * Bulk Operations
     * ====================================
     */

    /**
     * Upsert a device (insert or update)
     */
    @Transaction
    suspend fun upsertDevice(device: DeviceEntity): Long {
        val existingDevice = getDeviceByMacAddress(device.macAddress)
        return if (existingDevice != null) {
            val updatedDevice = device.copy(id = existingDevice.id)
            update(updatedDevice)
            existingDevice.id
        } else {
            insert(device)
        }
    }

    /**
     * Upsert multiple devices
     */
    @Transaction
    suspend fun upsertDevices(devices: List<DeviceEntity>): List<Long> {
        val results = mutableListOf<Long>()
        devices.forEach { device ->
            results.add(upsertDevice(device))
        }
        return results
    }
}
