package com.sensacare.app.data.dao

import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery
import com.sensacare.app.data.entity.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Base DAO interface with common CRUD operations for all entities
 */
interface BaseDao<T : BaseEntity> {
    /**
     * Insert a single entity
     * @param entity Entity to insert
     * @return The row ID of the inserted entity
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: T): Long

    /**
     * Insert multiple entities
     * @param entities List of entities to insert
     * @return List of row IDs for the inserted entities
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<T>): List<Long>

    /**
     * Update a single entity
     * @param entity Entity to update
     * @return Number of rows updated
     */
    @Update
    suspend fun update(entity: T): Int

    /**
     * Update multiple entities
     * @param entities List of entities to update
     * @return Number of rows updated
     */
    @Update
    suspend fun updateAll(entities: List<T>): Int

    /**
     * Delete a single entity
     * @param entity Entity to delete
     * @return Number of rows deleted
     */
    @Delete
    suspend fun delete(entity: T): Int

    /**
     * Delete multiple entities
     * @param entities List of entities to delete
     * @return Number of rows deleted
     */
    @Delete
    suspend fun deleteAll(entities: List<T>): Int

    /**
     * Get entity by ID
     * @param id Entity ID
     * @return Entity with the specified ID, or null if not found
     */
    @Query("SELECT * FROM #{tableName} WHERE id = :id")
    suspend fun getById(id: Long): T?

    /**
     * Get entity by ID as Flow
     * @param id Entity ID
     * @return Flow emitting the entity with the specified ID, or null if not found
     */
    @Query("SELECT * FROM #{tableName} WHERE id = :id")
    fun getByIdAsFlow(id: Long): Flow<T?>

    /**
     * Get all entities
     * @return List of all entities
     */
    @Query("SELECT * FROM #{tableName}")
    suspend fun getAll(): List<T>

    /**
     * Get all entities as Flow
     * @return Flow emitting a list of all entities
     */
    @Query("SELECT * FROM #{tableName}")
    fun getAllAsFlow(): Flow<List<T>>

    /**
     * Get all entities with pending sync
     * @return List of entities with pending sync
     */
    @Query("SELECT * FROM #{tableName} WHERE syncStatus = 'PENDING'")
    suspend fun getAllPendingSync(): List<T>

    /**
     * Update sync status for an entity
     * @param id Entity ID
     * @param syncStatus New sync status
     * @return Number of rows updated
     */
    @Query("UPDATE #{tableName} SET syncStatus = :syncStatus, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, syncStatus: SyncStatus, timestamp: Long = System.currentTimeMillis()): Int

    /**
     * Update sync status for multiple entities
     * @param ids List of entity IDs
     * @param syncStatus New sync status
     * @return Number of rows updated
     */
    @Query("UPDATE #{tableName} SET syncStatus = :syncStatus, updatedAt = :timestamp WHERE id IN (:ids)")
    suspend fun updateSyncStatusBatch(ids: List<Long>, syncStatus: SyncStatus, timestamp: Long = System.currentTimeMillis()): Int

    /**
     * Count all entities
     * @return Number of entities
     */
    @Query("SELECT COUNT(*) FROM #{tableName}")
    suspend fun count(): Int

    /**
     * Execute a raw query
     * @param query Raw SQL query
     * @return List of entities matching the query
     */
    @RawQuery
    suspend fun executeRawQuery(query: SupportSQLiteQuery): List<T>
}

/**
 * Device DAO for accessing device entities
 */
@Dao
interface DeviceDao : BaseDao<DeviceEntity> {
    /**
     * Get device by MAC address
     * @param macAddress Device MAC address
     * @return Device with the specified MAC address, or null if not found
     */
    @Query("SELECT * FROM devices WHERE macAddress = :macAddress")
    suspend fun getByMacAddress(macAddress: String): DeviceEntity?

    /**
     * Get device by MAC address as Flow
     * @param macAddress Device MAC address
     * @return Flow emitting the device with the specified MAC address, or null if not found
     */
    @Query("SELECT * FROM devices WHERE macAddress = :macAddress")
    fun getByMacAddressAsFlow(macAddress: String): Flow<DeviceEntity?>

    /**
     * Get active device
     * @return Active device, or null if none
     */
    @Query("SELECT * FROM devices WHERE isActive = 1 ORDER BY lastConnected DESC LIMIT 1")
    suspend fun getActiveDevice(): DeviceEntity?

    /**
     * Get active device as Flow
     * @return Flow emitting the active device, or null if none
     */
    @Query("SELECT * FROM devices WHERE isActive = 1 ORDER BY lastConnected DESC LIMIT 1")
    fun getActiveDeviceAsFlow(): Flow<DeviceEntity?>

    /**
     * Update device battery level
     * @param id Device ID
     * @param batteryLevel New battery level
     * @return Number of rows updated
     */
    @Query("UPDATE devices SET batteryLevel = :batteryLevel, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateBatteryLevel(id: Long, batteryLevel: Int, timestamp: Long = System.currentTimeMillis()): Int

    /**
     * Update device last connected timestamp
     * @param id Device ID
     * @param lastConnected New last connected timestamp
     * @return Number of rows updated
     */
    @Query("UPDATE devices SET lastConnected = :lastConnected, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateLastConnected(id: Long, lastConnected: Long, timestamp: Long = System.currentTimeMillis()): Int

    /**
     * Set device as active and others as inactive
     * @param id Device ID to set as active
     * @return Number of rows updated
     */
    @Transaction
    suspend fun setAsActiveDevice(id: Long): Int {
        // First set all devices as inactive
        val deactivated = setAllDevicesInactive()
        // Then set the specified device as active
        val activated = setDeviceActive(id)
        return deactivated + activated
    }

    /**
     * Set all devices as inactive
     * @return Number of rows updated
     */
    @Query("UPDATE devices SET isActive = 0, updatedAt = :timestamp")
    suspend fun setAllDevicesInactive(timestamp: Long = System.currentTimeMillis()): Int

    /**
     * Set device as active
     * @param id Device ID
     * @return Number of rows updated
     */
    @Query("UPDATE devices SET isActive = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun setDeviceActive(id: Long, timestamp: Long = System.currentTimeMillis()): Int

    /**
     * Get all devices sorted by last connected timestamp
     * @return List of devices sorted by last connected timestamp (descending)
     */
    @Query("SELECT * FROM devices ORDER BY lastConnected DESC")
    suspend fun getAllSortedByLastConnected(): List<DeviceEntity>

    /**
     * Get all devices sorted by last connected timestamp as Flow
     * @return Flow emitting a list of devices sorted by last connected timestamp (descending)
     */
    @Query("SELECT * FROM devices ORDER BY lastConnected DESC")
    fun getAllSortedByLastConnectedAsFlow(): Flow<List<DeviceEntity>>

    /**
     * Get devices with low battery
     * @param threshold Battery level threshold (default: 20%)
     * @return List of devices with battery level below the threshold
     */
    @Query("SELECT * FROM devices WHERE batteryLevel < :threshold AND batteryLevel IS NOT NULL")
    suspend fun getDevicesWithLowBattery(threshold: Int = 20): List<DeviceEntity>

    /**
     * Get devices with low battery as Flow
     * @param threshold Battery level threshold (default: 20%)
     * @return Flow emitting a list of devices with battery level below the threshold
     */
    @Query("SELECT * FROM devices WHERE batteryLevel < :threshold AND batteryLevel IS NOT NULL")
    fun getDevicesWithLowBatteryAsFlow(threshold: Int = 20): Flow<List<DeviceEntity>>
}

/**
 * User DAO for accessing user entities
 */
@Dao
interface UserDao : BaseDao<UserEntity> {
    /**
     * Get user by email
     * @param email User email
     * @return User with the specified email, or null if not found
     */
    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun getByEmail(email: String): UserEntity?

    /**
     * Get user by email as Flow
     * @param email User email
     * @return Flow emitting the user with the specified email, or null if not found
     */
    @Query("SELECT * FROM users WHERE email = :email")
    fun getByEmailAsFlow(email: String): Flow<UserEntity?>

    /**
     * Get current user (first user in the database)
     * @return Current user, or null if none
     */
    @Query("SELECT * FROM users ORDER BY id ASC LIMIT 1")
    suspend fun getCurrentUser(): UserEntity?

    /**
     * Get current user as Flow
     * @return Flow emitting the current user, or null if none
     */
    @Query("SELECT * FROM users ORDER BY id ASC LIMIT 1")
    fun getCurrentUserAsFlow(): Flow<UserEntity?>

    /**
     * Update user profile
     * @param id User ID
     * @param name User name
     * @param heightCm User height in cm
     * @param weightKg User weight in kg
     * @param stepLengthCm User step length in cm
     * @param targetSteps User target steps
     * @return Number of rows updated
     */
    @Query("UPDATE users SET name = :name, heightCm = :heightCm, weightKg = :weightKg, stepLengthCm = :stepLengthCm, targetSteps = :targetSteps, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateProfile(
        id: Long,
        name: String,
        heightCm: Int?,
        weightKg: Float?,
        stepLengthCm: Int?,
        targetSteps: Int,
        timestamp: Long = System.currentTimeMillis()
    ): Int

    /**
     * Update user preferred device
     * @param id User ID
     * @param preferredDeviceId Preferred device ID
     * @return Number of rows updated
     */
    @Query("UPDATE users SET preferredDeviceId = :preferredDeviceId, updatedAt = :timestamp WHERE id = :id")
    suspend fun updatePreferredDevice(id: Long, preferredDeviceId: Long?, timestamp: Long = System.currentTimeMillis()): Int

    /**
     * Update user profile image URI
     * @param id User ID
     * @param profileImageUri Profile image URI
     * @return Number of rows updated
     */
    @Query("UPDATE users SET profileImageUri = :profileImageUri, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateProfileImage(id: Long, profileImageUri: String?, timestamp: Long = System.currentTimeMillis()): Int
}

/**
 * Heart Rate DAO for accessing heart rate entities
 */
@Dao
interface HeartRateDao : BaseDao<HeartRateEntity> {
    /**
     * Get heart rate measurements for a specific date
     * @param deviceId Device ID
     * @param date Date in YYYY-MM-DD format
     * @return List of heart rate measurements for the specified date
     */
    @Query("SELECT * FROM heart_rates WHERE deviceId = :deviceId AND date = :date ORDER BY timestamp ASC")
    suspend fun getByDate(deviceId: Long, date: String): List<HeartRateEntity>

    /**
     * Get heart rate measurements for a specific date as Flow
     * @param deviceId Device ID
     * @param date Date in YYYY-MM-DD format
     * @return Flow emitting a list of heart rate measurements for the specified date
     */
    @Query("SELECT * FROM heart_rates WHERE deviceId = :deviceId AND date = :date ORDER BY timestamp ASC")
    fun getByDateAsFlow(deviceId: Long, date: String): Flow<List<HeartRateEntity>>

    /**
     * Get heart rate measurements for a date range
     * @param deviceId Device ID
     * @param startDate Start date in YYYY-MM-DD format
     * @param endDate End date in YYYY-MM-DD format
     * @return List of heart rate measurements for the specified date range
     */
    @Query("SELECT * FROM heart_rates WHERE deviceId = :deviceId AND date BETWEEN :startDate AND :endDate ORDER BY timestamp ASC")
    suspend fun getByDateRange(deviceId: Long, startDate: String, endDate: String): List<HeartRateEntity>

    /**
     * Get heart rate measurements for a date range as Flow
     * @param deviceId Device ID
     * @param startDate Start date in YYYY-MM-DD format
     * @param endDate End date in YYYY-MM-DD format
     * @return Flow emitting a list of heart rate measurements for the specified date range
     */
    @Query("SELECT * FROM heart_rates WHERE deviceId = :deviceId AND date BETWEEN :startDate AND :endDate ORDER BY timestamp ASC")
    fun getByDateRangeAsFlow(deviceId: Long, startDate: String, endDate: String): Flow<List<HeartRateEntity>>

    /**
     * Get heart rate measurements for a timestamp range
     * @param deviceId Device ID
     * @param startTimestamp Start timestamp
     * @param endTimestamp End timestamp
     * @return List of heart rate measurements for the specified timestamp range
     */
    @Query("SELECT * FROM heart_rates WHERE deviceId = :deviceId AND timestamp BETWEEN :startTimestamp AND :endTimestamp ORDER BY timestamp ASC")
    suspend fun getByTimestampRange(deviceId: Long, startTimestamp: Long, endTimestamp: Long): List<HeartRateEntity>

    /**
     * Get average heart rate for a specific date
     * @param deviceId Device ID
     * @param date Date in YYYY-MM-DD format
     * @return Average heart rate for the specified date, or null if no data
     */
    @Query("SELECT AVG(heartRate) FROM heart_rates WHERE deviceId = :deviceId AND date = :date")
    suspend fun getAverageByDate(deviceId: Long, date: String): Double?

    /**
     * Get average heart rate for a date range
     * @param deviceId Device ID
     * @param startDate Start date in YYYY-MM-DD format
     * @param endDate End date in YYYY-MM-DD format
     * @return Average heart rate for the specified date range, or null if no data
     */
    @Query("SELECT AVG(heartRate) FROM heart_rates WHERE deviceId = :deviceId AND date BETWEEN :startDate AND :endDate")
    suspend fun getAverageByDateRange(deviceId: Long, startDate: String, endDate: String): Double?

    /**
     * Get minimum heart rate for a specific date
     * @param deviceId Device ID
     * @param date Date in YYYY-MM-DD format
     * @return Minimum heart rate for the specified date, or null if no data
     */
    @Query("SELECT MIN(heartRate) FROM heart_rates WHERE deviceId = :deviceId AND date = :date")
    suspend fun getMinimumByDate(deviceId: Long, date: String): Int?

    /**
     * Get maximum heart rate for a specific date
     * @param deviceId Device ID
     * @param date Date in YYYY-MM-DD format
     * @return Maximum heart rate for the specified date, or null if no data
     */
    @Query("SELECT MAX(heartRate) FROM heart_rates WHERE deviceId = :deviceId AND date = :date")
    suspend fun getMaximumByDate(deviceId: Long, date: String): Int?

    /**
     * Get resting heart rate for a specific date (average of lowest 10% of readings)
     * @param deviceId Device ID
     * @param date Date in YYYY-MM-DD format
     * @return Resting heart rate for the specified date, or null if no data
     */
    @Query("""
        SELECT AVG(heartRate) FROM (
            SELECT heartRate FROM heart_rates 
            WHERE deviceId = :deviceId AND date = :date 
            ORDER BY heartRate ASC 
            LIMIT (SELECT COUNT(*) * 0.1 FROM heart_rates WHERE deviceId = :deviceId AND date = :date)
        )
    """)
    suspend fun getRestingHeartRateByDate(deviceId: Long, date: String): Double?

    /**
     * Get daily heart rate statistics for a date range
     * @param deviceId Device ID
     * @param startDate Start date in YYYY-MM-DD format
     * @param endDate End date in YYYY-MM-DD format
     * @return List of daily heart rate statistics for the specified date range
     */
    @Query("""
        SELECT 
            date, 
            MIN(heartRate) as minHeartRate, 
            MAX(heartRate) as maxHeartRate, 
            AVG(heartRate) as avgHeartRate,
            COUNT(*) as measurementCount
        FROM heart_rates 
        WHERE deviceId = :deviceId AND date BETWEEN :startDate AND :endDate 
        GROUP BY date 
        ORDER BY date ASC
    """)
    suspend fun getDailyStatsByDateRange(deviceId: Long, startDate: String, endDate: String): List<HeartRateStats>

    /**
     * Get daily heart rate statistics for a date range as Flow
     * @param deviceId Device ID
     * @param startDate Start date in YYYY-MM-DD format
     * @param endDate End date in YYYY-MM-DD format
     * @return Flow emitting a list of daily heart rate statistics for the specified date range
     */
    @Query("""
        SELECT 
            date, 
            MIN(heartRate) as minHeartRate, 
            MAX(heartRate) as maxHeartRate, 
            AVG(heartRate) as avgHeartRate,
            COUNT(*) as measurementCount
        FROM heart_rates 
        WHERE deviceId = :deviceId AND date BETWEEN :startDate AND :endDate 
        GROUP BY date 
        ORDER BY date ASC
    """)
    fun getDailyStatsByDateRangeAsFlow(deviceId: Long, startDate: String, endDate: String): Flow<List<HeartRateStats>>

    /**
     * Delete heart rate measurements for a specific date
     * @param deviceId Device ID
     * @param date Date in YYYY-MM-DD format
     * @return Number of rows deleted
     */
    @Query("DELETE FROM heart_rates WHERE deviceId = :deviceId AND date = :date")
    suspend fun deleteByDate(deviceId: Long, date: String): Int

    /**
     * Delete heart rate measurements for a date range
     * @param deviceId Device ID
     * @param startDate Start date in YYYY-MM-DD format
     * @param endDate End date in YYYY-MM-DD format
     * @return Number of rows deleted
     */
    @Query("DELETE FROM heart_rates WHERE deviceId = :deviceId AND date BETWEEN :startDate AND :endDate")
    suspend fun deleteByDateRange(deviceId: Long, startDate: String, endDate: String): Int

    /**
     * Get the latest heart rate measurement
     * @param deviceId Device ID
     * @return Latest heart rate measurement, or null if none
     */
    @Query("SELECT * FROM heart_rates WHERE deviceId = :deviceId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(deviceId: Long): HeartRateEntity?

    /**
     * Get the latest heart rate measurement as Flow
     * @param deviceId Device ID
     * @return Flow emitting the latest heart rate measurement, or null if none
     */
    @Query("SELECT * FROM heart_rates WHERE deviceId = :deviceId ORDER BY timestamp DESC LIMIT 1")
    fun getLatestAsFlow(deviceId: Long): Flow<HeartRateEntity?>

    /**
     * Get heart rate zones distribution for a specific date
     * @param deviceId Device ID
     * @param date Date in YYYY-MM-DD format
     * @param restingHeartRate Resting heart rate (default: 60)
     * @param maxHeartRate Maximum heart rate (default: 220 - 30)
     * @return Heart rate zones distribution for the specified date
     */
    @Transaction
    suspend fun getHeartRateZonesByDate(
        deviceId: Long, 
        date: String, 
        restingHeartRate: Int = 60, 
        maxHeartRate: Int = 190
    ): HeartRateZones {
        val measurements = getByDate(deviceId, date)
        
        // Calculate heart rate reserve (HRR)
        val hrr = maxHeartRate - restingHeartRate
        
        // Define zones based on HRR percentages
        val zone1Count = measurements.count { it.heartRate < restingHeartRate + 0.5 * hrr }
        val zone2Count = measurements.count { it.heartRate >= restingHeartRate + 0.5 * hrr && it.heartRate < restingHeartRate + 0.6 * hrr }
        val zone3Count = measurements.count { it.heartRate >= restingHeartRate + 0.6 * hrr && it.heartRate < restingHeartRate + 0.7 * hrr }
        val zone4Count = measurements.count { it.heartRate >= restingHeartRate + 0.7 * hrr && it.heartRate < restingHeartRate + 0.8 * hrr }
        val zone5Count = measurements.count { it.heartRate >= restingHeartRate + 0.8 * hrr }
        
        return HeartRateZones(
            date = date,
            zone1Count = zone1Count,
            zone2Count = zone2Count,
            zone3Count = zone3Count,
            zone4Count = zone4Count,
            zone5Count = zone5Count,
            totalCount = measurements.size
        )
    }
}

/**
 * Blood Oxygen DAO for accessing blood oxygen entities
 */
@Dao
interface BloodOxygenDao : BaseDao<BloodOxygenEntity> {
    /**
     * Get blood oxygen measurements for a specific date
     * @param deviceId Device ID
     * @param date Date in YYYY-MM-DD format
     * @return List of blood oxygen measurements for the specified date
     */
    @Query("SELECT * FROM blood_oxygen WHERE deviceId = :deviceId AND date = :date ORDER BY timestamp ASC")
    suspend fun getByDate(deviceId: Long, date: String): List<BloodOxygenEntity>

    /**
     * Get blood oxygen measurements for a specific date as Flow
     * @param deviceId Device ID
     * @param date Date in YYYY-MM-DD format
     * @return Flow emitting a list of blood oxygen measurements for the specified date
     */
    @Query("SELECT * FROM blood_oxygen WHERE deviceId = :deviceId AND date = :date ORDER BY timestamp ASC")
    fun getByDateAsFlow(deviceId: Long, date: String): Flow<List<BloodOxygenEntity>>

    /**
     * Get blood oxygen measurements for a date range
     * @param deviceId Device ID
     * @param startDate Start date in YYYY-MM-DD format
     * @param endDate End date in YYYY-MM-DD format
     * @return List of blood oxygen measurements for the specified date range
     */
    @Query("SELECT * FROM blood_oxygen WHERE deviceId = :deviceId AND date BETWEEN :startDate AND :endDate ORDER BY timestamp ASC")
    suspend fun getByDateRange(deviceId: Long, startDate: String, endDate: String): List<BloodOxygenEntity>

    /**
     * Get blood oxygen measurements for a date range as Flow
     * @param deviceId Device ID
     * @param startDate Start date in YYYY-MM-DD format
     * @param endDate End date in YYYY-MM-DD format
     * @return Flow emitting a list of blood oxygen measurements for the specified date range
     */
    @Query("SELECT * FROM blood_oxygen WHERE deviceId = :deviceId AND date BETWEEN :startDate AND :endDate ORDER BY timestamp ASC")
    fun getByDateRangeAsFlow(deviceId: Long, startDate: String, endDate: String): Flow<List<BloodOxygenEntity>>

    /**
     * Get average blood oxygen for a specific date
     * @param deviceId Device ID
     * @param date Date in YYYY-MM-DD format
     * @return Average blood oxygen for the specified date, or null if no data
     */
    @Query("SELECT AVG(bloodOxygen) FROM blood_oxygen WHERE deviceId = :deviceId AND date = :date")
    suspend fun getAverageByDate(deviceId: Long, date: String): Double?

    /**
     * Get minimum blood oxygen for a specific date
     * @param deviceId Device ID
     * @param date Date in YYYY-MM-DD format
     * @return Minimum blood oxygen for the specified date, or null if no data
     */
    @Query("SELECT MIN(bloodOxygen) FROM blood_oxygen WHERE deviceId = :deviceId AND date = :date")
    suspend fun getMinimumByDate(deviceId: Long, date: String): Int?

    /**
     * Get maximum blood oxygen for a specific date
     * @param deviceId Device ID
     * @param date Date in YYYY-MM-DD format
     * @return Maximum blood oxygen for the specified date, or null if no data
     */
    @Query("SELECT MAX(bloodOxygen) FROM blood_oxygen WHERE deviceId = :deviceId AND date = :date")
    suspend fun getMaximumByDate(deviceId: Long, date: String): Int?

    /**
     * Get daily blood oxygen statistics for a date range
     * @param deviceId Device ID
     * @param startDate Start date in YYYY-MM-DD format
     * @param endDate End date in YYYY-MM-DD format
     * @return List of daily blood oxygen statistics for the specified date range
     */
    @Query("""
        SELECT 
            date, 
            MIN(bloodOxygen) as minBloodOxygen, 
            MAX(bloodOxygen) as maxBloodOxygen, 
            AVG(bloodOxygen) as avgBloodOxygen,
            COUNT(*) as measurementCount
        FROM blood_oxygen 
        WHERE deviceId = :deviceId AND date BETWEEN :startDate AND :endDate 
        GROUP BY date 
        ORDER BY date ASC
    """)
    suspend fun getDailyStatsByDateRange(deviceId: Long, startDate: String, endDate: String): List<BloodOxygenStats>

    /**
     * Get daily blood oxygen statistics for a date range as Flow
     * @param deviceId Device ID
     * @param startDate Start date in YYYY-MM-DD format
     * @param endDate End date in YYYY-MM-DD format
     * @return Flow emitting a list of daily blood oxygen statistics for the specified date range
     */
    @Query("""
        SELECT 
            date, 
            MIN(bloodOxygen) as minBloodOxygen, 
            MAX(bloodOxygen) as maxBloodOxygen, 
            AVG(bloodOxygen) as avgBloodOxygen,
            COUNT(*) as measurementCount
        FROM blood_oxygen 
        WHERE deviceId = :deviceId AND date BETWEEN :startDate AND :endDate 
        GROUP BY date 
        ORDER BY date ASC
    """)
    fun getDailyStatsByDateRangeAsFlow(deviceId: Long, startDate: String, endDate: String): Flow<List<BloodOxygenStats>>

    /**
     * Get the latest blood oxygen measurement
     * @param deviceId Device ID
     * @return Latest blood oxygen measurement, or null if none
     */
    @Query("SELECT * FROM blood_oxygen WHERE deviceId = :deviceId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(deviceId: Long): BloodOxygenEntity?

    /**
     * Get the latest blood oxygen measurement as Flow
     * @param deviceId Device ID
     * @return Flow emitting the latest blood oxygen measurement, or null if none
     */
    @Query("SELECT * FROM blood_oxygen WHERE deviceId = :deviceId ORDER BY timestamp DESC LIMIT 1")
    fun getLatestAsFlow(deviceId: Long): Flow<BloodOxygenEntity?>

    /**
     * Count low blood oxygen events for a specific date
     * @param deviceId Device ID
     * @param date Date in YYYY-MM-DD format
     * @param threshold Blood oxygen threshold (default: 95%)
     * @return Number of blood oxygen measurements below the threshold
     */
    @Query("SELECT COUNT(*) FROM blood_oxygen WHERE deviceId = :deviceId AND date = :date AND bloodOxygen < :threshold")
    suspend fun countLowOxygenEvents(deviceId: Long, date: String, threshold: Int = 95): Int
}

/**
 * Step DAO for accessing step entities
 */
@Dao
interface StepDao : BaseDao<StepEntity> {
    /**
     * Get step data for a specific date
     * @param deviceId Device ID
     * @param date Date in YYYY-MM-DD format
     * @return Step data for the specified date, or null if none
     */
    @Query("SELECT * FROM steps WHERE deviceId = :deviceId AND date = :date")
    suspend fun getByDate(deviceId: Long, date: String): StepEntity?

    /**
     * Get step data for a specific date as Flow
     * @param deviceId Device ID
     * @param date Date in YYYY-MM-DD format
     * @return Flow emitting the step data for the specified date, or null if none
     */
    @Query("SELECT * FROM steps WHERE deviceId = :deviceId AND date = :date")
    fun getByDateAsFlow(deviceId: Long, date: String): Flow<StepEntity?>

    /**
     * Get step data for a date range
     * @param deviceId Device ID
     * @param startDate Start date in YYYY-MM-DD format
     * @param endDate End date in YYYY-MM-DD format
     * @return List of step data for the specified date range
     */
    @Query("SELECT * FROM steps WHERE deviceId = :deviceId AND date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    suspend fun getByDateRange(deviceId: Long, startDate: String, endDate: String): List<StepEntity>

    /**
     * Get step data for a date range as Flow
     * @param deviceId Device ID
     * @param startDate Start date in YYYY-MM-DD format
     * @param endDate End date in YYYY-MM-DD format
     * @return Flow emitting a list of step data for the specified date range
     */
    @Query("SELECT * FROM steps WHERE deviceId = :deviceId AND date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getByDateRangeAsFlow(deviceId: Long, startDate: String, endDate: String): Flow<List<StepEntity>>

    /**
     * Get total steps for a date range
     * @param deviceId Device ID
     * @param startDate Start date in YYYY-MM-DD format
     * @param endDate End date in YYYY-MM-DD format
     * @return Total steps for the specified date range
     */
    @Query("SELECT SUM(steps) FROM steps WHERE deviceId = :deviceId AND date BETWEEN :startDate AND :endDate")
    suspend fun getTotalStepsByDateRange(deviceId: Long, startDate: String, endDate: String): Int?

    /**
     * Get total distance for a date range
     * @param deviceId Device ID
     * @param startDate Start date in YYYY-MM-DD format
     * @param endDate End date in YYYY-MM-DD format
     * @return Total distance for the specified date range
     */
    @Query("SELECT SUM(distance) FROM steps WHERE deviceId = :deviceId AND date BETWEEN :startDate AND :endDate")
    suspend fun getTotalDistanceByDateRange(deviceId: Long, startDate: String, endDate: String): Float?

    /**
     * Get total calories for a date range
     * @param deviceId Device ID
     * @param startDate Start date in YYYY-MM-DD format
     * @param endDate End date in YYYY-MM-DD format
     * @return Total calories for the specified date range
     */
    @Query("SELECT SUM(calories) FROM steps WHERE deviceId = :deviceId AND date BETWEEN :startDate AND :endDate")
    suspend fun getTotalCaloriesByDateRange(deviceId: Long, startDate: String, endDate: String): Float?

    /**
     * Get average steps for a date range
     * @param deviceId Device ID
     * @param startDate Start date in YYYY-MM-DD format
     * @param endDate End date in YYYY-MM-DD format
     * @return Average steps for the specified date range
     */
    @Query("SELECT AVG(steps) FROM steps WHERE deviceId = :deviceId AND date BETWEEN :startDate AND :endDate")
    suspend fun getAverageStepsByDateRange(deviceId: Long, startDate: String, endDate: String): Double?

    /**
     * Get days with goal achieved for a date range
     * @param deviceId Device ID
     * @param startDate Start date in YYYY-MM-DD format
     * @param endDate End date in YYYY-MM-DD format
     * @return Number of days with goal achieved for the specified date range
     */
    @Query("SELECT COUNT(*) FROM steps WHERE deviceId = :deviceId AND date BETWEEN :startDate AND :endDate AND goalAchieved = 1")
    suspend fun getGoalAchievedDaysByDateRange(deviceId: Long, startDate: String, endDate: String): Int

    /**
     * Update step data for a specific date
     * @param deviceId Device ID
     * @param date Date in YYYY-MM-DD format
     * @param steps Steps count
     * @param distance Distance in meters
     * @param calories Calories burned
     * @param goalSteps Goal steps
     * @return Number of rows updated
     */
    @Query("""
        UPDATE steps SET 
            steps = :steps, 
            distance = :distance, 
            calories = :calories, 
            goalSteps = :goalSteps, 
            goalAchieved = (steps >= goalSteps),
            updatedAt = :timestamp,
            syncStatus = :syncStatus
        WHERE deviceId = :deviceId AND date = :date
    """)
    suspend fun updateStepData(
        deviceId: Long,
        date: String,
        steps: Int,
        distance: Float,
        calories: Float,
        goalSteps: Int,
        syncStatus: SyncStatus = SyncStatus.PENDING,
        timestamp: Long = System.currentTimeMillis()
    ): Int

    /**
     * Insert or update step data for a specific date
     * @param deviceId Device ID
     * @param date Date in YYYY-MM-DD format
     * @param steps Steps count
     * @param distance Distance in meters
     * @param calories Calories burned
     * @param goalSteps Goal steps
     * @return ID of the inserted or updated entity
     */
    @Transaction
    suspend fun upsertStepData(
        deviceId: Long,
        date: String,
        steps: Int,
        distance: Float,
        calories: Float,
        goalSteps: Int
    ): Long {
        val existing = getByDate(deviceId, date)
        return if (existing != null) {
            updateStepData(deviceId, date, steps, distance, calories, goalSteps)
            existing.id
        } else {
            insert(
                StepEntity(
                    deviceId = deviceId,
                    timestamp = System.currentTimeMillis(),
                    date = date,
                    steps = steps,
                    distance = distance,
                    calories = calories,
                    goalSteps = goalSteps,
                    goalAchieved = steps >= goalSteps
                )
            )
        }
    }

    /**
     * Get step streak (consecutive days with goal achieved)
     * @param deviceId Device ID
     * @param endDate End date in YYYY-MM-DD format (default: today)
     * @return Current streak of consecutive days with goal achieved
     */
    @Transaction
    suspend fun getCurrentStreak(deviceId: Long, endDate: String = LocalDate.now().toString()): Int {
        var streak = 0
        var currentDate = LocalDate.parse(endDate)
        
        while (true) {
            val stepData = getByDate(deviceId, currentDate.toString())
            if (stepData == null || !stepData.goalAchieved) {
                break
            }
            
            streak++
            currentDate = currentDate.minusDays(1)
        }
        
        return streak
    }

    /**
     * Get best step streak (consecutive days with goal achieved)
     * @param deviceId Device ID
     * @return Best streak of consecutive days with goal achieved
     */
    @Transaction
    suspend fun getBestStreak(deviceId: Long): Int {
        val allStepData = getAll().filter { it.deviceId == deviceId }.sortedBy { it.date }
        if (allStepData.isEmpty()) {
            return 0
        }
        
        var bestStreak = 0
        var currentStreak = 0
        var previousDate: LocalDate? = null
        
        for (stepData in allStepData) {
            val currentDate = LocalDate.parse(stepData.date)
            
            if (stepData.goalAchieved) {
                if (previousDate == null || currentDate.minusDays(1) == previousDate) {
                    currentStreak++
                } else {
                    currentStreak = 1
                }
                
                bestStreak = maxOf(bestStreak, currentStreak)
            } else {
                currentStreak = 0
            }
            
            previousDate = currentDate
        }
        
        return bestStreak
    }
}

/**
 * Hourly Step DAO for accessing hourly step entities
 */
@Dao
interface HourlyStepDao : BaseDao<HourlyStepEntity> {
    /**
     * Get hourly step data for a specific date
     * @param deviceId Device ID
     * @param date Date in YYYY-MM-DD format
     * @return List of hourly step data for the specified date
     */
    @Query("SELECT * FROM hourly_steps WHERE deviceId = :deviceId AND date = :date ORDER BY hour ASC")
    suspend fun getByDate(deviceId: Long, date: String): List<HourlyStepEntity>

    /**
     * Get hourly step data for a specific date as Flow
     * @param deviceId Device ID
     * @param date Date in YYYY-MM-DD format
     * @return Flow emitting a list of hourly step data for the specified date
     */
    @Query("SELECT * FROM hourly_steps WHERE deviceId = :deviceId AND date = :date ORDER BY hour ASC")
    fun getByDateAsFlow(deviceId: Long, date: String): Flow<List<HourlyStepEntity>>

    /**
     * Get hourly step data for a specific date and hour
     * @param deviceId Device ID
     * @param date Date in YYYY-MM-DD format
     * @param hour Hour (0-23)
     * @return Hourly step data for the specified date and hour, or null if none
     */
    @Query("SELECT * FROM hourly_steps WHERE deviceId = :deviceId AND date = :date AND hour = :hour")
    suspend fun getByDateAndHour(deviceId: Long, date: String, hour: Int): HourlyStepEntity?

    /**
     * Insert or update hourly step data
     * @param deviceId Device ID
     * @param date Date in YYYY-MM-DD format
     * @param hour Hour (0-23)
     * @param steps Steps count
     * @param distance Distance in meters
     * @param calories Calories burned
     * @return ID of the inserted or updated entity
     */
    @Transaction
    suspend fun upsertHourlyStepData(
        deviceId: Long,
        date: String,
        hour: Int,
        steps: Int,
        distance: Float,
        calories: Float
    ): Long {
        val existing = getByDateAndHour(deviceId, date, hour)
        return if (existing != null) {
            updateHourlyStepData(deviceId, date, hour, steps, distance, calories)
            existing.id
        } else {
            insert(
                HourlyStepEntity(
                    deviceId = deviceId,
                    timestamp = System.currentTimeMillis(),
                    date = date,
                    hour = hour,
                    steps = steps,
                    distance = distance,
                    calories = calories
                )
            )
        }
    }

    /**
     * Update hourly step data
     * @param deviceId Device ID
     * @param date Date in YYYY-MM-DD format
     * @param hour Hour (0-23)
     * @param steps Steps count
     * @param distance Distance in meters
     * @param calories Calories burned
     * @return Number of rows updated
     */
    @Query("""
        UPDATE hourly_steps SET 
            steps = :steps, 
            distance = :distance, 
            calories = :calories, 
            updatedAt = :timestamp,
            syncStatus = :syncStatus
        WHERE deviceId = :deviceId AND date = :date AND hour = :hour
    """)
    suspend fun updateHourlyStepData(
        deviceId: Long,
        date: String,
        hour: Int,
        steps: Int,
        distance: Float,
        calories: Float,
        syncStatus: SyncStatus = SyncStatus.PENDING,
        timestamp: Long = System.currentTimeMillis()
    ): Int

    /**
     * Get hourly step data for a date range
     * @param deviceId Device ID
     * @param startDate Start date in YYYY-MM-DD format
     * @param endDate End date in YYYY-MM-DD format
     * @return List of hourly step data for the specified date range
     */
    @Query("SELECT * FROM hourly_steps WHERE deviceId = :deviceId AND date BETWEEN :startDate AND :endDate ORDER BY date ASC, hour ASC")
    suspend fun getByDateRange(deviceId: Long, startDate: String, endDate: String): List<HourlyStepEntity>

    /**
     * Get hourly step data for a date range as Flow
     * @param deviceId Device ID
     * @param startDate Start date in YYYY-MM-DD format
     * @param endDate End date in YYYY-MM-DD format
     * @return Flow emitting a list of hourly step data for the specified date range
     */
    @Query("SELECT * FROM hourly_steps WHERE deviceId = :deviceId AND date BETWEEN :startDate AND :endDate ORDER BY date ASC, hour ASC")
    fun getByDateRangeAsFlow(deviceId: Long, startDate: String, endDate: String): Flow<List<HourlyStepEntity>>

    /**
     * Get hourly step data for a specific hour range
     * @param deviceId Device ID
     * @param date Date in YYYY-MM-DD format
     * @param startHour Start hour (0-23)
     * @param endHour End hour (0-23)
     * @return List of hourly step data for the specified hour range
     */
    @Query("SELECT * FROM hourly_steps WHERE deviceId = :deviceId AND date = :date AND hour BETWEEN :startHour AND :endHour ORDER BY hour ASC")
    suspend fun getByHourRange(deviceId: Long, date: String, startHour: Int, endHour: Int): List<HourlyStepEntity>

    /**
     * Get active hours for a specific date (hours with steps > 0)
     * @param deviceId Device ID
     * @param date Date in YYYY-MM-DD format
     * @return Number of active hours for the specified date
     */
    @Query("SELECT COUNT(*) FROM hourly_steps WHERE deviceId = :deviceId AND date = :date AND steps > 0")
    suspend fun getActiveHoursByDate(deviceId: Long, date: String): Int

    /**
     * Get most active hour for a specific date
     * @param deviceId Device ID
     * @param date Date in YYYY-MM-DD format
     * @return Most active hour for the specified date, or null if no data
     */
    @Query("SELECT hour FROM hourly_steps WHERE deviceId = :deviceId AND date = :date ORDER BY steps DESC LIMIT 1")
    suspend fun getMostActiveHourByDate(deviceId: Long, date: String): Int?

    /**
     * Get activity pattern (average steps per hour) for a date range
     * @param deviceId Device ID
     * @param startDate Start date in YYYY-MM-DD format
     * @param endDate End date in YYYY-MM-DD format
     * @return List of average steps per hour for the specified date range
     */
    @Query("""
        SELECT 
            hour, 
            AVG(steps) as avgSteps
        FROM hourly_steps 
        WHERE deviceId = :deviceId AND date BETWEEN :startDate AND :endDate 
        GROUP BY hour 
        ORDER BY hour ASC
    """)
    suspend fun getActivityPatternByDateRange(deviceId: Long, startDate: String, endDate: String): List<HourlyStepAverage>

    /**
     * Get activity pattern (average steps per hour) for a date range as Flow
     * @param deviceId Device ID
     * @param startDate Start date in YYYY-MM-DD format
     * @param endDate End date in YYYY-MM-DD format
     * @return Flow emitting a list of average steps per hour for the specified date range
     */
    @Query("""
        SELECT 
            hour, 
            AVG(steps) as avgSteps
        FROM hourly_steps 
        WHERE deviceId = :deviceId AND date BETWEEN :startDate AND :endDate 
        GROUP BY hour 
        ORDER BY hour ASC
    """)
    fun getActivityPatternByDateRangeAsFlow(deviceId: Long, startDate: String, endDate: String): Flow<List<HourlyStepAverage>>
}

/**
 * Sleep DAO for accessing sleep entities
 */
@Dao
interface SleepDao : BaseDao<SleepEntity> {
    /**
     * Get sleep data for a specific date
     * @param deviceId Device ID
     * @param date Date in YYYY-MM-DD format
     * @return Sleep data for the specified date, or null if none
     */
    @Query("SELECT * FROM sleep WHERE deviceId = :deviceId AND date = :date")
    suspend fun getByDate(deviceId: Long, date: String): SleepEntity?

    /**
     * Get sleep data for a specific date as Flow
     * @param deviceId Device ID
     * @param date Date in YYYY-MM-DD format
     * @return Flow emitting the sleep data for the specified date, or null if none
     */
    @Query("SELECT * FROM sleep WHERE deviceId = :deviceId AND date = :date")
    fun getByDateAsFlow(deviceId: Long, date: String): Flow<SleepEntity?>

    /**
     * Get sleep data for a date range
     * @param deviceId Device ID
     * @param startDate Start date in YYYY-MM-DD format
     * @param endDate End date in YYYY-MM-DD format
     * @return List of sleep data for the specified date range
     */
    @Query("SELECT * FROM sleep WHERE deviceId = :deviceId AND date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    suspend fun getByDateRange(deviceId: Long, startDate: String, endDate: String): List<SleepEntity>

    /**
     * Get sleep data for a date range as Flow
     * @param deviceId Device ID
     * @param startDate Start date in YYYY-MM-DD format
     * @param endDate End date in YYYY-MM-DD format
     * @return Flow emitting a list of sleep data for the specified date range
     */
    @Query("SELECT * FROM sleep WHERE deviceId = :deviceId AND date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getByDateRangeAsFlow(deviceId: Long, startDate: String, endDate: String): Flow<List<SleepEntity>>

    /**
     * Get average sleep duration for a date range
     * @param deviceId Device ID
     * @param startDate Start date in YYYY-MM-DD format
     * @param endDate End date in YYYY-MM-DD format
     * @return Average sleep duration in minutes for the specified date range
     */
    @Query("SELECT AVG(totalSleepMinutes) FROM sleep WHERE deviceId = :deviceId AND date BETWEEN :startDate AND :endDate")
    suspend fun getAverageSleepDurationByDateRange(deviceId: Long, startDate: String, endDate: String): Double?

    /**
     * Get average deep sleep duration for a date range
     * @param deviceId Device ID
     * @param startDate Start date in YYYY-MM-DD format
     * @param endDate End date in YYYY-MM-DD format
     * @return Average deep sleep duration in minutes for the specified date range
     */
    @Query("SELECT AVG(deepSleepMinutes) FROM sleep WHERE deviceId = :deviceId AND date BETWEEN :startDate AND :endDate")
    suspend fun getAverageDeepSleepDurationByDateRange(deviceId: Long, startDate: String, endDate: String): Double?

    /**
     * Get average sleep quality for a date range
     * @param deviceId Device ID
     * @param startDate Start date in YYYY-MM-DD format
     * @param endDate End date in YYYY-MM-DD format
     * @return Average sleep quality for the specified date range
     */
    @Query("SELECT AVG(sleepQuality) FROM sleep WHERE deviceId = :deviceId AND date BETWEEN :startDate AND :endDate")
    suspend fun getAverageSleepQualityByDateRange(deviceId: Long, startDate: String, endDate: String): Double?

    /**
     * Get sleep statistics for a date range
     * @param deviceId Device ID
     * @param startDate Start date in YYYY-MM-DD format
     * @param endDate End date in YYYY-MM-DD format
     * @return Sleep statistics for the specified date range
     */
    @Query("""
        SELECT 
            AVG(totalSleepMinutes) as avgTotalSleepMinutes,
            AVG(deepSleepMinutes) as avgDeepSleepMinutes,
            AVG(lightSleepMinutes) as avgLightSleepMinutes,
            AVG(remSleepMinutes) as avgRemSleepMinutes,
            AVG(awakeSleepMinutes) as avgAwakeSleepMinutes,
            AVG(sleepQuality) as avgSleepQuality,
            COUNT(*) as recordCount
        FROM sleep 
        WHERE deviceId = :deviceId AND date BETWEEN :startDate AND :endDate
    """)
    suspend fun getSleepStatsByDateRange(deviceId: Long, startDate: String, endDate: String): SleepStats?

    /**
     * Get sleep statistics for a date range as Flow
     * @param deviceId Device ID
     * @param startDate Start date in YYYY-MM-DD format
     * @param endDate End date in YYYY-MM-DD format
     * @return Flow emitting sleep statistics for the specified date range
     */
    @Query("""
        SELECT 
            AVG(totalSleepMinutes) as avgTotalSleepMinutes,
            AVG(deepSleepMinutes) as avgDeepSleepMinutes,
            AVG(lightSleepMinutes) as avgLightSleepMinutes,
            AVG(remSleepMinutes) as avgRemSleepMinutes,
            AVG(awakeSleepMinutes) as avgAwakeSleepMinutes,
            AVG(sleepQuality) as avgSleepQuality,
            COUNT(*) as recordCount
        FROM sleep 
        WHERE deviceId = :deviceId AND date BETWEEN :startDate AND :endDate
    """)
    fun getSleepStatsByDateRangeAsFlow(deviceId: Long, startDate: String, endDate: String): Flow<SleepStats?>

    /**
     * Get sleep efficiency for a specific date (deep sleep percentage)
     * @param deviceId Device ID
     * @param date Date in YYYY-MM-DD format
     * @return Sleep efficiency for the specified date, or null if no data
     */
    @Transaction
    suspend fun getSleepEfficiencyByDate(deviceId: Long, date: String): Double? {
        val sleepData = getByDate(deviceId, date) ?: return null
        return if (sleepData.totalSleepMinutes > 0) {
            sleepData.deepSleepMinutes.toDouble() / sleepData.totalSleepMinutes.toDouble() * 100.0
        } else {
            null
        }
    }

    /**
     * Get sleep debt for a date range (difference between actual and target sleep)
     * @param deviceId Device ID
     * @param startDate Start date in YYYY-MM-DD format
     * @param endDate End date in YYYY-MM-DD format
     * @param targetSleepMinutes Target sleep duration in minutes (default: 480 minutes = 8 hours)
     * @return Sleep debt in minutes for the specified date range
     */
    @Transaction
    suspend fun getSleepDebtByDateRange(
        deviceId: Long,
        startDate: String,
        endDate: String,
        targetSleepMinutes: Int = 480
    ): Int? {
        val sleepData = getByDateRange(deviceId, startDate, endDate)
        if (sleepData.isEmpty()) {
            return null
        }
        
        val totalActualSleep = sleepData.sumOf { it.totalSleepMinutes }
        val totalTargetSleep = targetSleepMinutes * sleepData.size
        
        return totalTargetSleep - totalActualSleep
    }
}

/**
 * Sleep Detail DAO for accessing sleep detail entities
 */
@Dao
interface SleepDetailDao : BaseDao<SleepDetailEntity> {
    /**
     * Get sleep details for a specific sleep record
     * @param sleepId Sleep record ID
     * @return List of sleep details for the specified sleep record
     */
    @Query("SELECT * FROM sleep_details WHERE sleepId = :sleepId ORDER BY timestamp ASC")
    suspend fun getBySleepId(sleepId: Long): List<SleepDetailEntity>

    /**
     * Get sleep details for a specific sleep record as Flow
     * @param sleepId Sleep record ID
     * @return Flow emitting a list of sleep details for the specified sleep record
     */
    @Query("SELECT * FROM sleep_details WHERE sleepId = :sleepId ORDER BY timestamp ASC")
    fun getBySleepIdAsFlow(sleepId: Long): Flow<List<SleepDetailEntity>>

    /**
     * Delete sleep details for a specific sleep record
     * @param sleepId Sleep record ID
     * @return Number of rows deleted
     */
    @Query("DELETE FROM sleep_details WHERE sleepId = :sleepId")
    suspend fun deleteBySleepId(sleepId: Long): Int

    /**
     * Get sleep stage distribution for a specific sleep record
     * @param sleepId Sleep record ID
     * @return Sleep stage distribution for the specified sleep record
     */
    @Query("""
        SELECT 
            sleepType, 
            COUNT(*) as count,
            SUM(durationMinutes) as totalMinutes
        FROM sleep_details 
        WHERE sleepId = :sleepId 
        GROUP BY sleepType
    """)
    suspend fun getSleepStageDistribution(sleepId: Long): List<SleepStageDistribution>

    /**
     * Get sleep stage distribution for a specific sleep record as Flow
     * @param sleepId Sleep record ID
     * @return Flow emitting sleep stage distribution for the specified sleep record
     */
    @Query("""
        SELECT 
            sleepType, 
            COUNT(*) as count,
            SUM(durationMinutes) as totalMinutes
        FROM sleep_details 
        WHERE sleepId = :sleepId 
        GROUP BY sleepType
    """)
    fun getSleepStageDistributionAsFlow(sleepId: Long): Flow<List<SleepStageDistribution>>
}

/**
 * Blood Pressure DAO for accessing blood pressure entities
 */
@Dao
interface BloodPressureDao : BaseDao<BloodPressureEntity> {
    /**
     * Get blood pressure measurements for a specific date
     * @param deviceId Device ID
     * @param date Date in YYYY-MM-DD format
     * @return List of blood pressure measurements for the specified date
     */
    @Query("SELECT * FROM blood_pressure WHERE deviceId = :deviceId AND date = :date ORDER BY timestamp ASC")
    suspend fun getByDate(deviceId: Long, date: String): List<BloodPressureEntity>

    /**
     * Get blood pressure measurements for a specific date as Flow
     * @param deviceId Device ID
     * @param date Date in YYYY-MM-DD format
     * @return Flow emitting a list of blood pressure measurements for the specified date
     */
    @Query("SELECT * FROM blood_pressure WHERE deviceId = :deviceId AND date = :date ORDER BY timestamp ASC")
    fun getByDateAsFlow(deviceId: Long, date: String): Flow<List<BloodPressureEntity>>

    /**
     * Get blood pressure measurements for a date range
     * @param deviceId Device ID
     * @param startDate Start date in YYYY-MM-DD format
     * @param endDate End date in YYYY-MM-DD format
     * @return List of blood pressure measurements for the specified date range
     */
    @Query("SELECT * FROM blood_pressure WHERE deviceId = :deviceId AND date BETWEEN :startDate AND :endDate ORDER BY timestamp ASC")
    suspend fun getByDateRange(deviceId: Long, startDate: String, endDate: String): List<BloodPressureEntity>

    /**
     * Get blood pressure measurements for a date range as Flow
     * @param deviceId Device ID
     * @param startDate Start date in YYYY-MM-DD format
     * @param endDate End date in YYYY-MM-DD format
     * @return Flow emitting a list of blood pressure measurements for the specified date range
     */
    @Query("SELECT * FROM blood_pressure WHERE deviceId = :deviceId AND date BETWEEN :startDate AND :endDate ORDER BY timestamp ASC")
    fun getByDateRangeAsFlow(deviceId: Long, startDate: String, endDate: String): Flow<List<BloodPressureEntity>>

    /**
     * Get average blood pressure for a specific date
     * @param deviceId Device ID
     * @param date Date in YYYY-MM-DD format
     * @return Average blood pressure for the specified date, or null if no data
     */
    @Query("""
        SELECT 
            AVG(systolic) as avgSystolic, 
            AVG(diastolic) as avgDiastolic
        FROM blood_pressure 
        WHERE deviceId = :deviceId AND date = :date
    """)
    suspend fun getAverageByDate(deviceId: Long, date: String): BloodPressureAverage?

    /**
     * Get average blood pressure for a date range
     * @param deviceId Device ID
     * @param startDate Start date in YYYY-MM-DD format
     * @param endDate End date in YYYY-MM-DD format
     * @return Average blood pressure for the specified date range, or null if no data
     */
    @Query("""
        SELECT 
            AVG(systolic) as avgSystolic, 
            AVG(diastolic) as avgDiastolic
        FROM blood_pressure 
        WHERE deviceId = :deviceId AND date BETWEEN :startDate AND :endDate
    """)
    suspend fun getAverageByDateRange(deviceId: Long, startDate: String, endDate: String): BloodPressureAverage?

    /**
     * Get the latest blood pressure measurement
     * @param deviceId Device ID
     * @return Latest blood pressure measurement, or null if none
     */
    @Query("SELECT * FROM blood_pressure WHERE deviceId = :deviceId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(deviceId: Long): BloodPressureEntity?

    /**
     * Get the latest blood pressure measurement as Flow
     * @param deviceId Device ID
     * @return Flow emitting the latest blood pressure measurement, or null if none
     */
    @Query("SELECT * FROM blood_pressure WHERE deviceId = :deviceId ORDER BY timestamp DESC LIMIT 1")
    fun getLatestAsFlow(deviceId: Long): Flow<BloodPressureEntity?>

    /**
     * Count high blood pressure events for a specific date
     * @param deviceId Device ID
     * @param date Date in YYYY-MM-DD format
     * @param systolicThreshold Systolic threshold (default: 140 mmHg)
     * @param diastolicThreshold Diastolic threshold (default: 90 mmHg)
     * @return Number of blood pressure measurements above the thresholds
     */
    @Query("SELECT COUNT(*) FROM blood_pressure WHERE deviceId = :deviceId AND date = :date AND (systolic >= :systolicThreshold OR diastolic >= :diastolicThreshold)")
    suspend fun countHighBloodPressureEvents(
        deviceId: Long,
        date: String,
        systolicThreshold: Int = 140,
        diastolicThreshold: Int = 90
    ): Int

    /**
     * Get blood pressure statistics for a date range
     * @param deviceId Device ID
     * @param startDate Start date in YYYY-MM-DD format
     * @param endDate End date in YYYY-MM-DD format
     * @return Blood pressure statistics for the specified date range
     */
    @Query("""
        SELECT 
            date, 
            AVG(systolic) as avgSystolic, 
            AVG(diastolic) as avgDiastolic,
            MIN(systolic) as minSystolic,
            MAX(systolic) as maxSystolic,
            MIN(diastolic) as minDiastolic,
            MAX(diastolic) as maxDiastolic,
            COUNT(*) as measurementCount
        FROM blood_pressure 
        WHERE deviceId = :deviceId AND date BETWEEN :startDate AND :endDate 
        GROUP BY date 
        ORDER BY date ASC
    """)
    suspend fun getStatsByDateRange(deviceId: Long, startDate: String, endDate: String): List<BloodPressureStats>

    /**
     * Get blood pressure statistics for a date range as Flow
     * @param deviceId Device ID
     * @param startDate Start date in YYYY-MM-DD format
     * @param endDate End date in YYYY-MM-DD format
     * @return Flow emitting blood pressure statistics for the specified date range
     */
    @Query("""
        SELECT 
            date, 
            AVG(systolic) as avgSystolic, 
            AVG(diastolic) as avgDiastolic,
            MIN(systolic) as minSystolic,
            MAX(systolic) as maxSystolic,
            MIN(diastolic) as minDiastolic,
            MAX(diastolic) as maxDiastolic,
            COUNT(*) as measurementCount
        FROM blood_pressure 
        WHERE deviceId = :deviceId AND date BETWEEN :startDate AND :endDate 
        GROUP BY date 
        ORDER BY date ASC
    """)
    fun getStatsByDateRangeAsFlow(deviceId: Long, startDate: String, endDate: String): Flow<List<BloodPressureStats>>
}

/**
 * Temperature DAO for accessing temperature entities
 */
@Dao
interface TemperatureDao : BaseDao<TemperatureEntity> {
    /**
     * Get temperature measurements for a specific date
     * @param deviceId Device ID
     * @param date Date in YYYY-MM-DD format
     * @return List of temperature measurements for the specified date
     */
    @Query("SELECT * FROM temperature WHERE deviceId = :deviceId AND date = :date ORDER BY timestamp ASC")
    suspend fun getByDate(deviceId: Long, date: String): List<TemperatureEntity>

    /**
     * Get temperature measurements for a specific date as Flow
     * @param deviceId Device ID
     * @param date Date in YYYY-MM-DD format
     * @return Flow emitting a list of temperature measurements for the specified date
     */
    @Query("SELECT * FROM temperature WHERE deviceId = :deviceId AND date = :date ORDER BY timestamp ASC")
    fun getByDateAsFlow(deviceId: Long, date: String): Flow<List<TemperatureEntity>>

    /**
     * Get temperature measurements for a date range
     * @param deviceId Device ID
     * @param startDate Start date in YYYY-MM-DD format
     * @param endDate End date in YYYY-MM-DD format
     * @return List of temperature measurements for the specified date range
     */
    @Query("SELECT * FROM temperature WHERE deviceId = :deviceId AND date BETWEEN :startDate AND :endDate ORDER BY timestamp ASC")
    suspend fun getByDateRange(deviceId: Long, startDate: String, endDate: String): List<TemperatureEntity>

    /**
     * Get temperature measurements for a date range as Flow
     * @param deviceId Device ID
     * @param startDate Start date in YYYY-MM-DD format
     * @param endDate End date in YYYY-MM-DD format
     * @return Flow emitting a list of temperature measurements for the specified date range
     */
    @Query("SELECT * FROM temperature WHERE deviceId = :deviceId AND date BETWEEN :startDate AND :endDate ORDER BY timestamp ASC")
    fun getByDateRangeAsFlow(deviceId: Long, startDate: String, endDate: String): Flow<List<TemperatureEntity>>

    /**
     * Get average temperature for a specific date
     * @param deviceId Device ID
     * @param date Date in YYYY-MM-DD format
     * @return Average temperature for the specified date, or null if no data
     */
    @Query("SELECT AVG(temperatureCelsius) FROM temperature WHERE deviceId = :deviceId AND date = :date")
    suspend fun getAverageByDate(deviceId: Long, date: String): Float?

    /**
     * Get average temperature for a date range
     * @param deviceId Device ID
     * @param startDate Start date in YYYY-MM-DD format
     * @param endDate End date in YYYY-MM-DD format
     * @return Average temperature for the specified date range, or null if no data
     */
    @Query("SELECT AVG(temperatureCelsius) FROM temperature WHERE deviceId = :deviceId AND date BETWEEN :startDate AND :endDate")
    suspend fun getAverageByDateRange(deviceId: Long, startDate: String, endDate: String): Float?

    /**
     * Get the latest temperature measurement
     * @param deviceId Device ID
     * @return Latest temperature measurement, or null if none
     */
    @Query("SELECT * FROM temperature WHERE deviceId = :deviceId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(deviceId: Long): TemperatureEntity?

    /**
     * Get the latest temperature measurement as Flow
     * @param deviceId Device ID
     * @return Flow emitting the latest temperature measurement, or null if none
     */
    @Query("SELECT * FROM temperature WHERE deviceId = :deviceId ORDER BY timestamp DESC LIMIT 1")
    fun getLatestAsFlow(deviceId: Long): Flow<TemperatureEntity?>

    /**
     * Count fever events for a specific date
     * @param deviceId Device ID
     * @param date Date in YYYY-MM-DD format
     * @param threshold Fever threshold in Celsius (default: 37.5°C)
     * @return Number of temperature measurements above the threshold
     */
    @Query("SELECT COUNT(*) FROM temperature WHERE deviceId = :deviceId AND date = :date AND temperatureCelsius >= :threshold")
    suspend fun countFeverEvents(deviceId: Long, date: String, threshold: Float = 37.5f): Int

    /**
     * Get temperature statistics for a date range
     * @param deviceId Device ID
     * @param startDate Start date in YYYY-MM-DD format
     * @param endDate End date in YYYY-MM-DD format
     * @return Temperature statistics for the specified date range
     */
    @Query("""
        SELECT 
            date, 
            MIN(temperatureCelsius) as minTemperature, 
            MAX(temperatureCelsius) as maxTemperature, 
            AVG(temperatureCelsius) as avgTemperature,
            COUNT(*) as measurementCount
        FROM temperature 
        WHERE deviceId = :deviceId AND date BETWEEN :startDate AND :endDate 
        GROUP BY date 
        ORDER BY date ASC
    """)
    suspend fun getStatsByDateRange(deviceId: Long, startDate: String, endDate: String): List<TemperatureStats>

    /**
     * Get temperature statistics for a date range as Flow
     * @param deviceId Device ID
     * @param startDate Start date in YYYY-MM-DD format
     * @param endDate End date in YYYY-MM-DD format
     * @return Flow emitting temperature statistics for the specified date range
     */
    @Query("""
        SELECT 
            date, 
            MIN(temperatureCelsius) as minTemperature, 
            MAX(temperatureCelsius) as maxTemperature, 
            AVG(temperatureCelsius) as avgTemperature,
            COUNT(*) as measurementCount
        FROM temperature 
        WHERE deviceId = :deviceId AND date BETWEEN :startDate AND :endDate 
        GROUP BY date 
        ORDER BY date ASC
    """)
    fun getStatsByDateRangeAsFlow(deviceId: Long, startDate: String, endDate: String): Flow<List<TemperatureStats>>
}

/**
 * Activity DAO for accessing activity entities
 */
@Dao
interface ActivityDao : BaseDao<ActivityEntity> {
    /**
     * Get activities for a specific date
     * @param deviceId Device ID
     * @param date Date in YYYY-MM-DD format
     * @return List of activities for the specified date
     */
    @Query("SELECT * FROM activities WHERE deviceId = :deviceId AND date = :date ORDER BY startTime ASC")
    suspend fun getByDate(deviceId: Long, date: String): List<ActivityEntity>

    /**
     * Get activities for a specific date as Flow
     * @param deviceId Device ID
     * @param date Date in YYYY-MM-DD format
     * @return Flow emitting a list of activities for the specified date
     */
    @Query("SELECT * FROM activities WHERE deviceId = :deviceId AND date = :date ORDER BY startTime ASC")
    fun getByDateAsFlow(deviceId: Long, date: String): Flow<List<ActivityEntity>>

    /**
     * Get activities for a date range
     * @param deviceId Device ID
     * @param startDate Start date in YYYY-MM-DD format
     * @param endDate End date in YYYY-MM-DD format
     * @return List of activities for the specified date range
     */
    @Query("SELECT * FROM activities WHERE deviceId = :deviceId AND date BETWEEN :startDate AND :endDate ORDER BY date ASC, startTime ASC")
    suspend fun getByDateRange(deviceId: Long, startDate: String, endDate: String): List<ActivityEntity>

    /**
     * Get activities for a date range as Flow
     * @param deviceId Device ID
     * @param startDate Start date in YYYY-MM-DD format
     * @param endDate End date in YYYY-MM-DD format
     * @return Flow emitting a list of activities for the specified date range
     */
    @Query("SELECT * FROM activities WHERE deviceId = :deviceId AND date BETWEEN :startDate AND :endDate ORDER BY date ASC, startTime ASC")
    fun getByDateRangeAsFlow(deviceId: Long, startDate: String, endDate: String): Flow<List<ActivityEntity>>

    /**
     * Get activities by type for a date range
     * @param deviceId Device ID
     * @param activityType Activity type
     * @param startDate Start date in YYYY-MM-DD format
     * @param endDate End date in YYYY-MM-DD format
     * @return List of activities of the specified type for the specified date range
     */
    @Query("SELECT * FROM activities WHERE deviceId = :deviceId AND activityType = :activityType AND date BETWEEN :startDate AND :endDate ORDER BY date ASC, startTime ASC")
    suspend fun getByTypeAndDateRange(deviceId: Long, activityType: String, startDate: String, endDate: String): List<ActivityEntity>

    /**
     * Get activities by type for a date range as Flow
     * @param deviceId Device ID
     * @param activityType Activity type
     * @param startDate Start date in YYYY-MM-DD format
     * @param endDate End date in YYYY-MM-DD format
     * @return Flow emitting a list of activities of the specified type for the specified date range
     */
    @Query("SELECT * FROM activities WHERE deviceId = :deviceId AND activityType = :activityType AND date BETWEEN :startDate AND :endDate ORDER BY date ASC, startTime ASC")
    fun getByTypeAndDateRangeAsFlow(deviceId: Long, activityType: String, startDate: String, endDate: String): Flow<List<ActivityEntity>>

    /**
     * Get total activity duration for a date range
     * @param deviceId Device ID
     * @param startDate Start date in YYYY-MM-DD format
     * @param endDate End date in YYYY-MM-DD format
     * @return Total activity duration in seconds for the specified date range
     */
    @Query("SELECT SUM(durationSeconds) FROM activities WHERE deviceId = :deviceId AND date BETWEEN :startDate AND :endDate")
    suspend fun getTotalDurationByDateRange(deviceId: Long, startDate: String, endDate: String): Int?

    /**
     * Get total activity duration by type for a date range
     * @param deviceId Device ID
     * @param activityType Activity type
     * @param startDate Start date in YYYY-MM-DD format
     * @param endDate End date in YYYY-MM-DD format
     * @return Total activity duration in seconds for the specified type and date range
     */
    @Query("SELECT SUM(durationSeconds) FROM activities WHERE deviceId = :deviceId AND activityType = :activityType AND date BETWEEN :startDate AND :endDate")
    suspend fun getTotalDurationByTypeAndDateRange(deviceId: Long, activityType: String, startDate: String, endDate: String): Int?

    /**
     * Get activity statistics by type for a date range
     * @param deviceId Device ID
     * @param startDate Start date in YYYY-MM-DD format
     * @param endDate End date in YYYY-MM-DD format
     * @return Activity statistics by type for the specified date range
     */
    @Query("""
        SELECT 
            activityType, 
            COUNT(*) as activityCount, 
            SUM(durationSeconds) as totalDurationSeconds,
            AVG(durationSeconds) as avgDurationSeconds,
            SUM(steps) as totalSteps,
            SUM(distance) as totalDistance,
            SUM(calories) as totalCalories,
            AVG(avgHeartRate) as avgHeartRate,
            MAX(maxHeartRate) as maxHeartRate
        FROM activities 
        WHERE deviceId = :deviceId AND date BETWEEN :startDate AND :endDate 
        GROUP BY activityType
    """)
    suspend fun getStatsByTypeAndDateRange(deviceId: Long, startDate: String, endDate: String): List<ActivityTypeStats>

    /**
     * Get activity statistics by type for a date range as Flow
     * @param deviceId Device ID
     * @param startDate Start date in YYYY-MM-DD format
     * @param endDate End date in YYYY-MM-DD format
     * @return Flow emitting activity statistics by type for the specified date range
     */
    @Query("""
        SELECT 
            activityType, 
            COUNT(*) as activityCount, 
            SUM(durationSeconds) as totalDurationSeconds,
            AVG(durationSeconds) as avgDurationSeconds,
            SUM(steps) as totalSteps,
            SUM(distance) as totalDistance,
            SUM(calories) as totalCalories,
            AVG(avgHeartRate) as avgHeartRate,
            MAX(maxHeartRate) as maxHeartRate
        FROM activities 
        WHERE deviceId = :deviceId AND date BETWEEN :startDate AND :endDate 
        GROUP BY activityType
    """)
    fun getStatsByTypeAndDateRangeAsFlow(deviceId: Long, startDate: String, endDate: String): Flow<List<ActivityTypeStats>>

    /**
     * Get daily activity statistics for a date range
     * @param deviceId Device ID
     * @param startDate Start date in YYYY-MM-DD format
     * @param endDate End date in YYYY-MM-DD format
     * @return Daily activity statistics for the specified date range
     */
    @Query("""
        SELECT 
            date, 
            COUNT(*) as activityCount, 
            SUM(durationSeconds) as totalDurationSeconds,
            SUM(steps) as totalSteps,
            SUM(distance) as totalDistance,
            SUM(calories) as totalCalories
        FROM activities 
        WHERE deviceId = :deviceId AND date BETWEEN :startDate AND :endDate 
        GROUP BY date 
        ORDER BY date ASC
    """)
    suspend fun getDailyStatsByDateRange(deviceId: Long, startDate: String, endDate: String): List<DailyActivityStats>

    /**
     * Get daily activity statistics for a date range as Flow
     * @param deviceId Device ID
     * @param startDate Start date in YYYY-MM-DD format
     * @param endDate End date in YYYY-MM-DD format
     * @return Flow emitting daily activity statistics for the specified date range
     */
    @Query("""
        SELECT 
            date, 
            COUNT(*) as activityCount, 
            SUM(durationSeconds) as totalDurationSeconds,
            SUM(steps) as totalSteps,
            SUM(distance) as totalDistance,
            SUM(calories) as totalCalories
        FROM activities 
        WHERE deviceI