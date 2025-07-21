package com.sensacare.app.data.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.sensacare.app.data.entity.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * HealthDao.kt - Comprehensive Health Data Access Objects for SensaCare
 *
 * This file contains multiple DAOs for various health measurements:
 * - HeartRateDao: Heart rate measurements
 * - BloodOxygenDao: SpO2 measurements
 * - BloodPressureDao: Blood pressure readings
 * - StepDao: Step and activity data
 * - SleepDao: Sleep tracking
 * - TemperatureDao: Temperature measurements
 * - ActivityDao: Exercise tracking
 *
 * Each DAO provides:
 * - Complex queries with date ranges
 * - Statistical aggregation functions
 * - Flow-based reactive data
 * - Sync status management
 * - Pagination support
 */

/**
 * HeartRateDao - Data Access Object for heart rate measurements
 */
@Dao
interface HeartRateDao {

    /**
     * ====================================
     * Basic CRUD Operations
     * ====================================
     */

    /**
     * Insert a new heart rate measurement
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(heartRate: HeartRateEntity): Long

    /**
     * Insert multiple heart rate measurements
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(heartRates: List<HeartRateEntity>): List<Long>

    /**
     * Update a heart rate measurement
     */
    @Update
    suspend fun update(heartRate: HeartRateEntity): Int

    /**
     * Delete a heart rate measurement
     */
    @Delete
    suspend fun delete(heartRate: HeartRateEntity): Int

    /**
     * Delete a heart rate measurement by ID
     */
    @Query("DELETE FROM heart_rates WHERE id = :heartRateId")
    suspend fun deleteById(heartRateId: Long): Int

    /**
     * Delete all heart rate measurements
     */
    @Query("DELETE FROM heart_rates")
    suspend fun deleteAll(): Int

    /**
     * Delete all heart rate measurements for a device
     */
    @Query("DELETE FROM heart_rates WHERE deviceId = :deviceId")
    suspend fun deleteAllForDevice(deviceId: Long): Int

    /**
     * ====================================
     * Basic Query Operations
     * ====================================
     */

    /**
     * Get a heart rate measurement by ID
     */
    @Query("SELECT * FROM heart_rates WHERE id = :heartRateId")
    suspend fun getHeartRateById(heartRateId: Long): HeartRateEntity?

    /**
     * Get a heart rate measurement by ID as Flow
     */
    @Query("SELECT * FROM heart_rates WHERE id = :heartRateId")
    fun getHeartRateByIdFlow(heartRateId: Long): Flow<HeartRateEntity?>

    /**
     * Get all heart rate measurements
     */
    @Query("SELECT * FROM heart_rates ORDER BY timestamp DESC")
    suspend fun getAllHeartRates(): List<HeartRateEntity>

    /**
     * Get all heart rate measurements as Flow
     */
    @Query("SELECT * FROM heart_rates ORDER BY timestamp DESC")
    fun getAllHeartRatesFlow(): Flow<List<HeartRateEntity>>

    /**
     * Get all heart rate measurements for a device
     */
    @Query("SELECT * FROM heart_rates WHERE deviceId = :deviceId ORDER BY timestamp DESC")
    suspend fun getHeartRatesForDevice(deviceId: Long): List<HeartRateEntity>

    /**
     * Get all heart rate measurements for a device as Flow
     */
    @Query("SELECT * FROM heart_rates WHERE deviceId = :deviceId ORDER BY timestamp DESC")
    fun getHeartRatesForDeviceFlow(deviceId: Long): Flow<List<HeartRateEntity>>

    /**
     * Count all heart rate measurements
     */
    @Query("SELECT COUNT(*) FROM heart_rates")
    suspend fun getHeartRateCount(): Int

    /**
     * Count heart rate measurements for a device
     */
    @Query("SELECT COUNT(*) FROM heart_rates WHERE deviceId = :deviceId")
    suspend fun getHeartRateCountForDevice(deviceId: Long): Int

    /**
     * ====================================
     * Date Range Queries
     * ====================================
     */

    /**
     * Get heart rate measurements for a date range
     */
    @Query("SELECT * FROM heart_rates WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    suspend fun getHeartRatesInRange(startTime: Long, endTime: Long): List<HeartRateEntity>

    /**
     * Get heart rate measurements for a date range as Flow
     */
    @Query("SELECT * FROM heart_rates WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getHeartRatesInRangeFlow(startTime: Long, endTime: Long): Flow<List<HeartRateEntity>>

    /**
     * Get heart rate measurements for a device in a date range
     */
    @Query("SELECT * FROM heart_rates WHERE deviceId = :deviceId AND timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    suspend fun getHeartRatesForDeviceInRange(deviceId: Long, startTime: Long, endTime: Long): List<HeartRateEntity>

    /**
     * Get heart rate measurements for a device in a date range as Flow
     */
    @Query("SELECT * FROM heart_rates WHERE deviceId = :deviceId AND timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getHeartRatesForDeviceInRangeFlow(deviceId: Long, startTime: Long, endTime: Long): Flow<List<HeartRateEntity>>

    /**
     * Get heart rate measurements for a specific date
     */
    @Query("SELECT * FROM heart_rates WHERE date = :date ORDER BY timestamp DESC")
    suspend fun getHeartRatesForDate(date: String): List<HeartRateEntity>

    /**
     * Get heart rate measurements for a specific date as Flow
     */
    @Query("SELECT * FROM heart_rates WHERE date = :date ORDER BY timestamp DESC")
    fun getHeartRatesForDateFlow(date: String): Flow<List<HeartRateEntity>>

    /**
     * Get heart rate measurements for a device on a specific date
     */
    @Query("SELECT * FROM heart_rates WHERE deviceId = :deviceId AND date = :date ORDER BY timestamp DESC")
    suspend fun getHeartRatesForDeviceAndDate(deviceId: Long, date: String): List<HeartRateEntity>

    /**
     * Get heart rate measurements for a device on a specific date as Flow
     */
    @Query("SELECT * FROM heart_rates WHERE deviceId = :deviceId AND date = :date ORDER BY timestamp DESC")
    fun getHeartRatesForDeviceAndDateFlow(deviceId: Long, date: String): Flow<List<HeartRateEntity>>

    /**
     * Get heart rate measurements for the last N days
     */
    @Query("SELECT * FROM heart_rates WHERE timestamp >= :timestamp ORDER BY timestamp DESC")
    suspend fun getHeartRatesForLastNDays(timestamp: Long): List<HeartRateEntity>

    /**
     * ====================================
     * Statistical Queries
     * ====================================
     */

    /**
     * Get the average heart rate for a date range
     */
    @Query("SELECT AVG(heartRate) FROM heart_rates WHERE timestamp BETWEEN :startTime AND :endTime")
    suspend fun getAverageHeartRateInRange(startTime: Long, endTime: Long): Float?

    /**
     * Get the average heart rate for a device in a date range
     */
    @Query("SELECT AVG(heartRate) FROM heart_rates WHERE deviceId = :deviceId AND timestamp BETWEEN :startTime AND :endTime")
    suspend fun getAverageHeartRateForDeviceInRange(deviceId: Long, startTime: Long, endTime: Long): Float?

    /**
     * Get the minimum heart rate for a date range
     */
    @Query("SELECT MIN(heartRate) FROM heart_rates WHERE timestamp BETWEEN :startTime AND :endTime")
    suspend fun getMinHeartRateInRange(startTime: Long, endTime: Long): Int?

    /**
     * Get the maximum heart rate for a date range
     */
    @Query("SELECT MAX(heartRate) FROM heart_rates WHERE timestamp BETWEEN :startTime AND :endTime")
    suspend fun getMaxHeartRateInRange(startTime: Long, endTime: Long): Int?

    /**
     * Get the minimum heart rate for a device in a date range
     */
    @Query("SELECT MIN(heartRate) FROM heart_rates WHERE deviceId = :deviceId AND timestamp BETWEEN :startTime AND :endTime")
    suspend fun getMinHeartRateForDeviceInRange(deviceId: Long, startTime: Long, endTime: Long): Int?

    /**
     * Get the maximum heart rate for a device in a date range
     */
    @Query("SELECT MAX(heartRate) FROM heart_rates WHERE deviceId = :deviceId AND timestamp BETWEEN :startTime AND :endTime")
    suspend fun getMaxHeartRateForDeviceInRange(deviceId: Long, startTime: Long, endTime: Long): Int?

    /**
     * Get heart rate statistics for a date range
     */
    @Query("""
        SELECT 
            AVG(heartRate) as average,
            MIN(heartRate) as minimum,
            MAX(heartRate) as maximum,
            COUNT(*) as count
        FROM heart_rates 
        WHERE timestamp BETWEEN :startTime AND :endTime
    """)
    suspend fun getHeartRateStatsInRange(startTime: Long, endTime: Long): HeartRateStats?

    /**
     * Get heart rate statistics for a device in a date range
     */
    @Query("""
        SELECT 
            AVG(heartRate) as average,
            MIN(heartRate) as minimum,
            MAX(heartRate) as maximum,
            COUNT(*) as count
        FROM heart_rates 
        WHERE deviceId = :deviceId AND timestamp BETWEEN :startTime AND :endTime
    """)
    suspend fun getHeartRateStatsForDeviceInRange(deviceId: Long, startTime: Long, endTime: Long): HeartRateStats?

    /**
     * Get heart rate statistics by date
     */
    @Query("""
        SELECT 
            date,
            AVG(heartRate) as average,
            MIN(heartRate) as minimum,
            MAX(heartRate) as maximum,
            COUNT(*) as count
        FROM heart_rates 
        WHERE timestamp BETWEEN :startTime AND :endTime
        GROUP BY date
        ORDER BY date ASC
    """)
    suspend fun getHeartRateStatsByDate(startTime: Long, endTime: Long): List<HeartRateStatsByDate>

    /**
     * Get heart rate statistics by date as Flow
     */
    @Query("""
        SELECT 
            date,
            AVG(heartRate) as average,
            MIN(heartRate) as minimum,
            MAX(heartRate) as maximum,
            COUNT(*) as count
        FROM heart_rates 
        WHERE timestamp BETWEEN :startTime AND :endTime
        GROUP BY date
        ORDER BY date ASC
    """)
    fun getHeartRateStatsByDateFlow(startTime: Long, endTime: Long): Flow<List<HeartRateStatsByDate>>

    /**
     * Get heart rate statistics by date for a device
     */
    @Query("""
        SELECT 
            date,
            AVG(heartRate) as average,
            MIN(heartRate) as minimum,
            MAX(heartRate) as maximum,
            COUNT(*) as count
        FROM heart_rates 
        WHERE deviceId = :deviceId AND timestamp BETWEEN :startTime AND :endTime
        GROUP BY date
        ORDER BY date ASC
    """)
    suspend fun getHeartRateStatsByDateForDevice(deviceId: Long, startTime: Long, endTime: Long): List<HeartRateStatsByDate>

    /**
     * ====================================
     * Pagination Support
     * ====================================
     */

    /**
     * Get paged heart rate measurements
     */
    @Query("SELECT * FROM heart_rates ORDER BY timestamp DESC")
    fun getPagedHeartRates(): PagingSource<Int, HeartRateEntity>

    /**
     * Get paged heart rate measurements for a device
     */
    @Query("SELECT * FROM heart_rates WHERE deviceId = :deviceId ORDER BY timestamp DESC")
    fun getPagedHeartRatesForDevice(deviceId: Long): PagingSource<Int, HeartRateEntity>

    /**
     * Get paged heart rate measurements for a date range
     */
    @Query("SELECT * FROM heart_rates WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getPagedHeartRatesInRange(startTime: Long, endTime: Long): PagingSource<Int, HeartRateEntity>

    /**
     * Get paged heart rate measurements for a device in a date range
     */
    @Query("SELECT * FROM heart_rates WHERE deviceId = :deviceId AND timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getPagedHeartRatesForDeviceInRange(deviceId: Long, startTime: Long, endTime: Long): PagingSource<Int, HeartRateEntity>

    /**
     * ====================================
     * Sync Status Operations
     * ====================================
     */

    /**
     * Get heart rate measurements by sync status
     */
    @Query("SELECT * FROM heart_rates WHERE syncStatus = :syncStatus ORDER BY timestamp DESC")
    suspend fun getHeartRatesBySyncStatus(syncStatus: SyncStatus): List<HeartRateEntity>

    /**
     * Update heart rate sync status
     */
    @Query("UPDATE heart_rates SET syncStatus = :syncStatus, updatedAt = :timestamp WHERE id = :heartRateId")
    suspend fun updateHeartRateSyncStatus(heartRateId: Long, syncStatus: SyncStatus, timestamp: Long = System.currentTimeMillis()): Int

    /**
     * Update sync status for multiple heart rate measurements
     */
    @Query("UPDATE heart_rates SET syncStatus = :syncStatus, updatedAt = :timestamp WHERE id IN (:heartRateIds)")
    suspend fun updateHeartRatesSyncStatus(heartRateIds: List<Long>, syncStatus: SyncStatus, timestamp: Long = System.currentTimeMillis()): Int

    /**
     * Get heart rate measurements that need syncing
     */
    @Query("SELECT * FROM heart_rates WHERE syncStatus IN ('PENDING', 'FAILED') ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getHeartRatesNeedingSync(limit: Int = 100): List<HeartRateEntity>

    /**
     * Count heart rate measurements that need syncing
     */
    @Query("SELECT COUNT(*) FROM heart_rates WHERE syncStatus IN ('PENDING', 'FAILED')")
    suspend fun getHeartRatesNeedingSyncCount(): Int

    /**
     * ====================================
     * Advanced Queries
     * ====================================
     */

    /**
     * Get the latest heart rate measurement
     */
    @Query("SELECT * FROM heart_rates ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestHeartRate(): HeartRateEntity?

    /**
     * Get the latest heart rate measurement as Flow
     */
    @Query("SELECT * FROM heart_rates ORDER BY timestamp DESC LIMIT 1")
    fun getLatestHeartRateFlow(): Flow<HeartRateEntity?>

    /**
     * Get the latest heart rate measurement for a device
     */
    @Query("SELECT * FROM heart_rates WHERE deviceId = :deviceId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestHeartRateForDevice(deviceId: Long): HeartRateEntity?

    /**
     * Get the latest heart rate measurement for a device as Flow
     */
    @Query("SELECT * FROM heart_rates WHERE deviceId = :deviceId ORDER BY timestamp DESC LIMIT 1")
    fun getLatestHeartRateForDeviceFlow(deviceId: Long): Flow<HeartRateEntity?>

    /**
     * Get heart rate measurements with a specific status
     */
    @Query("SELECT * FROM heart_rates WHERE status = :status ORDER BY timestamp DESC")
    suspend fun getHeartRatesByStatus(status: String): List<HeartRateEntity>

    /**
     * Get heart rate measurements with a specific status for a device
     */
    @Query("SELECT * FROM heart_rates WHERE deviceId = :deviceId AND status = :status ORDER BY timestamp DESC")
    suspend fun getHeartRatesByStatusForDevice(deviceId: Long, status: String): List<HeartRateEntity>

    /**
     * Get heart rate measurements with a specific status in a date range
     */
    @Query("SELECT * FROM heart_rates WHERE status = :status AND timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    suspend fun getHeartRatesByStatusInRange(status: String, startTime: Long, endTime: Long): List<HeartRateEntity>

    /**
     * Get heart rate measurements above a threshold
     */
    @Query("SELECT * FROM heart_rates WHERE heartRate > :threshold ORDER BY timestamp DESC")
    suspend fun getHeartRatesAboveThreshold(threshold: Int): List<HeartRateEntity>

    /**
     * Get heart rate measurements below a threshold
     */
    @Query("SELECT * FROM heart_rates WHERE heartRate < :threshold ORDER BY timestamp DESC")
    suspend fun getHeartRatesBelowThreshold(threshold: Int): List<HeartRateEntity>

    /**
     * Get heart rate measurements between thresholds
     */
    @Query("SELECT * FROM heart_rates WHERE heartRate BETWEEN :minThreshold AND :maxThreshold ORDER BY timestamp DESC")
    suspend fun getHeartRatesBetweenThresholds(minThreshold: Int, maxThreshold: Int): List<HeartRateEntity>

    /**
     * Get heart rate measurements between thresholds for a device
     */
    @Query("SELECT * FROM heart_rates WHERE deviceId = :deviceId AND heartRate BETWEEN :minThreshold AND :maxThreshold ORDER BY timestamp DESC")
    suspend fun getHeartRatesBetweenThresholdsForDevice(deviceId: Long, minThreshold: Int, maxThreshold: Int): List<HeartRateEntity>

    /**
     * Get heart rate measurements between thresholds in a date range
     */
    @Query("SELECT * FROM heart_rates WHERE heartRate BETWEEN :minThreshold AND :maxThreshold AND timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    suspend fun getHeartRatesBetweenThresholdsInRange(minThreshold: Int, maxThreshold: Int, startTime: Long, endTime: Long): List<HeartRateEntity>

    /**
     * Get heart rate measurements between thresholds in a date range as Flow
     */
    @Query("SELECT * FROM heart_rates WHERE heartRate BETWEEN :minThreshold AND :maxThreshold AND timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getHeartRatesBetweenThresholdsInRangeFlow(minThreshold: Int, maxThreshold: Int, startTime: Long, endTime: Long): Flow<List<HeartRateEntity>>

    /**
     * Get manual heart rate measurements
     */
    @Query("SELECT * FROM heart_rates WHERE isManualEntry = 1 ORDER BY timestamp DESC")
    suspend fun getManualHeartRates(): List<HeartRateEntity>

    /**
     * Get automatic heart rate measurements
     */
    @Query("SELECT * FROM heart_rates WHERE isManualEntry = 0 ORDER BY timestamp DESC")
    suspend fun getAutomaticHeartRates(): List<HeartRateEntity>

    /**
     * Get heart rate measurements with notes
     */
    @Query("SELECT * FROM heart_rates WHERE notes IS NOT NULL AND notes != '' ORDER BY timestamp DESC")
    suspend fun getHeartRatesWithNotes(): List<HeartRateEntity>
}

/**
 * BloodOxygenDao - Data Access Object for blood oxygen measurements
 */
@Dao
interface BloodOxygenDao {

    /**
     * ====================================
     * Basic CRUD Operations
     * ====================================
     */

    /**
     * Insert a new blood oxygen measurement
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bloodOxygen: BloodOxygenEntity): Long

    /**
     * Insert multiple blood oxygen measurements
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(bloodOxygens: List<BloodOxygenEntity>): List<Long>

    /**
     * Update a blood oxygen measurement
     */
    @Update
    suspend fun update(bloodOxygen: BloodOxygenEntity): Int

    /**
     * Delete a blood oxygen measurement
     */
    @Delete
    suspend fun delete(bloodOxygen: BloodOxygenEntity): Int

    /**
     * Delete a blood oxygen measurement by ID
     */
    @Query("DELETE FROM blood_oxygen WHERE id = :bloodOxygenId")
    suspend fun deleteById(bloodOxygenId: Long): Int

    /**
     * Delete all blood oxygen measurements
     */
    @Query("DELETE FROM blood_oxygen")
    suspend fun deleteAll(): Int

    /**
     * Delete all blood oxygen measurements for a device
     */
    @Query("DELETE FROM blood_oxygen WHERE deviceId = :deviceId")
    suspend fun deleteAllForDevice(deviceId: Long): Int

    /**
     * ====================================
     * Basic Query Operations
     * ====================================
     */

    /**
     * Get a blood oxygen measurement by ID
     */
    @Query("SELECT * FROM blood_oxygen WHERE id = :bloodOxygenId")
    suspend fun getBloodOxygenById(bloodOxygenId: Long): BloodOxygenEntity?

    /**
     * Get a blood oxygen measurement by ID as Flow
     */
    @Query("SELECT * FROM blood_oxygen WHERE id = :bloodOxygenId")
    fun getBloodOxygenByIdFlow(bloodOxygenId: Long): Flow<BloodOxygenEntity?>

    /**
     * Get all blood oxygen measurements
     */
    @Query("SELECT * FROM blood_oxygen ORDER BY timestamp DESC")
    suspend fun getAllBloodOxygens(): List<BloodOxygenEntity>

    /**
     * Get all blood oxygen measurements as Flow
     */
    @Query("SELECT * FROM blood_oxygen ORDER BY timestamp DESC")
    fun getAllBloodOxygensFlow(): Flow<List<BloodOxygenEntity>>

    /**
     * Get all blood oxygen measurements for a device
     */
    @Query("SELECT * FROM blood_oxygen WHERE deviceId = :deviceId ORDER BY timestamp DESC")
    suspend fun getBloodOxygensForDevice(deviceId: Long): List<BloodOxygenEntity>

    /**
     * Get all blood oxygen measurements for a device as Flow
     */
    @Query("SELECT * FROM blood_oxygen WHERE deviceId = :deviceId ORDER BY timestamp DESC")
    fun getBloodOxygensForDeviceFlow(deviceId: Long): Flow<List<BloodOxygenEntity>>

    /**
     * Count all blood oxygen measurements
     */
    @Query("SELECT COUNT(*) FROM blood_oxygen")
    suspend fun getBloodOxygenCount(): Int

    /**
     * Count blood oxygen measurements for a device
     */
    @Query("SELECT COUNT(*) FROM blood_oxygen WHERE deviceId = :deviceId")
    suspend fun getBloodOxygenCountForDevice(deviceId: Long): Int

    /**
     * ====================================
     * Date Range Queries
     * ====================================
     */

    /**
     * Get blood oxygen measurements for a date range
     */
    @Query("SELECT * FROM blood_oxygen WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    suspend fun getBloodOxygensInRange(startTime: Long, endTime: Long): List<BloodOxygenEntity>

    /**
     * Get blood oxygen measurements for a date range as Flow
     */
    @Query("SELECT * FROM blood_oxygen WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getBloodOxygensInRangeFlow(startTime: Long, endTime: Long): Flow<List<BloodOxygenEntity>>

    /**
     * Get blood oxygen measurements for a device in a date range
     */
    @Query("SELECT * FROM blood_oxygen WHERE deviceId = :deviceId AND timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    suspend fun getBloodOxygensForDeviceInRange(deviceId: Long, startTime: Long, endTime: Long): List<BloodOxygenEntity>

    /**
     * Get blood oxygen measurements for a device in a date range as Flow
     */
    @Query("SELECT * FROM blood_oxygen WHERE deviceId = :deviceId AND timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getBloodOxygensForDeviceInRangeFlow(deviceId: Long, startTime: Long, endTime: Long): Flow<List<BloodOxygenEntity>>

    /**
     * Get blood oxygen measurements for a specific date
     */
    @Query("SELECT * FROM blood_oxygen WHERE date = :date ORDER BY timestamp DESC")
    suspend fun getBloodOxygensForDate(date: String): List<BloodOxygenEntity>

    /**
     * Get blood oxygen measurements for a specific date as Flow
     */
    @Query("SELECT * FROM blood_oxygen WHERE date = :date ORDER BY timestamp DESC")
    fun getBloodOxygensForDateFlow(date: String): Flow<List<BloodOxygenEntity>>

    /**
     * Get blood oxygen measurements for a device on a specific date
     */
    @Query("SELECT * FROM blood_oxygen WHERE deviceId = :deviceId AND date = :date ORDER BY timestamp DESC")
    suspend fun getBloodOxygensForDeviceAndDate(deviceId: Long, date: String): List<BloodOxygenEntity>

    /**
     * ====================================
     * Statistical Queries
     * ====================================
     */

    /**
     * Get the average blood oxygen for a date range
     */
    @Query("SELECT AVG(bloodOxygen) FROM blood_oxygen WHERE timestamp BETWEEN :startTime AND :endTime")
    suspend fun getAverageBloodOxygenInRange(startTime: Long, endTime: Long): Float?

    /**
     * Get the average blood oxygen for a device in a date range
     */
    @Query("SELECT AVG(bloodOxygen) FROM blood_oxygen WHERE deviceId = :deviceId AND timestamp BETWEEN :startTime AND :endTime")
    suspend fun getAverageBloodOxygenForDeviceInRange(deviceId: Long, startTime: Long, endTime: Long): Float?

    /**
     * Get the minimum blood oxygen for a date range
     */
    @Query("SELECT MIN(bloodOxygen) FROM blood_oxygen WHERE timestamp BETWEEN :startTime AND :endTime")
    suspend fun getMinBloodOxygenInRange(startTime: Long, endTime: Long): Int?

    /**
     * Get the maximum blood oxygen for a date range
     */
    @Query("SELECT MAX(bloodOxygen) FROM blood_oxygen WHERE timestamp BETWEEN :startTime AND :endTime")
    suspend fun getMaxBloodOxygenInRange(startTime: Long, endTime: Long): Int?

    /**
     * Get blood oxygen statistics for a date range
     */
    @Query("""
        SELECT 
            AVG(bloodOxygen) as average,
            MIN(bloodOxygen) as minimum,
            MAX(bloodOxygen) as maximum,
            COUNT(*) as count
        FROM blood_oxygen 
        WHERE timestamp BETWEEN :startTime AND :endTime
    """)
    suspend fun getBloodOxygenStatsInRange(startTime: Long, endTime: Long): BloodOxygenStats?

    /**
     * Get blood oxygen statistics by date
     */
    @Query("""
        SELECT 
            date,
            AVG(bloodOxygen) as average,
            MIN(bloodOxygen) as minimum,
            MAX(bloodOxygen) as maximum,
            COUNT(*) as count
        FROM blood_oxygen 
        WHERE timestamp BETWEEN :startTime AND :endTime
        GROUP BY date
        ORDER BY date ASC
    """)
    suspend fun getBloodOxygenStatsByDate(startTime: Long, endTime: Long): List<BloodOxygenStatsByDate>

    /**
     * Get blood oxygen statistics by date as Flow
     */
    @Query("""
        SELECT 
            date,
            AVG(bloodOxygen) as average,
            MIN(bloodOxygen) as minimum,
            MAX(bloodOxygen) as maximum,
            COUNT(*) as count
        FROM blood_oxygen 
        WHERE timestamp BETWEEN :startTime AND :endTime
        GROUP BY date
        ORDER BY date ASC
    """)
    fun getBloodOxygenStatsByDateFlow(startTime: Long, endTime: Long): Flow<List<BloodOxygenStatsByDate>>

    /**
     * ====================================
     * Pagination Support
     * ====================================
     */

    /**
     * Get paged blood oxygen measurements
     */
    @Query("SELECT * FROM blood_oxygen ORDER BY timestamp DESC")
    fun getPagedBloodOxygens(): PagingSource<Int, BloodOxygenEntity>

    /**
     * Get paged blood oxygen measurements for a device
     */
    @Query("SELECT * FROM blood_oxygen WHERE deviceId = :deviceId ORDER BY timestamp DESC")
    fun getPagedBloodOxygensForDevice(deviceId: Long): PagingSource<Int, BloodOxygenEntity>

    /**
     * Get paged blood oxygen measurements for a date range
     */
    @Query("SELECT * FROM blood_oxygen WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getPagedBloodOxygensInRange(startTime: Long, endTime: Long): PagingSource<Int, BloodOxygenEntity>

    /**
     * ====================================
     * Sync Status Operations
     * ====================================
     */

    /**
     * Get blood oxygen measurements by sync status
     */
    @Query("SELECT * FROM blood_oxygen WHERE syncStatus = :syncStatus ORDER BY timestamp DESC")
    suspend fun getBloodOxygensBySyncStatus(syncStatus: SyncStatus): List<BloodOxygenEntity>

    /**
     * Update blood oxygen sync status
     */
    @Query("UPDATE blood_oxygen SET syncStatus = :syncStatus, updatedAt = :timestamp WHERE id = :bloodOxygenId")
    suspend fun updateBloodOxygenSyncStatus(bloodOxygenId: Long, syncStatus: SyncStatus, timestamp: Long = System.currentTimeMillis()): Int

    /**
     * Update sync status for multiple blood oxygen measurements
     */
    @Query("UPDATE blood_oxygen SET syncStatus = :syncStatus, updatedAt = :timestamp WHERE id IN (:bloodOxygenIds)")
    suspend fun updateBloodOxygensSyncStatus(bloodOxygenIds: List<Long>, syncStatus: SyncStatus, timestamp: Long = System.currentTimeMillis()): Int

    /**
     * Get blood oxygen measurements that need syncing
     */
    @Query("SELECT * FROM blood_oxygen WHERE syncStatus IN ('PENDING', 'FAILED') ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getBloodOxygensNeedingSync(limit: Int = 100): List<BloodOxygenEntity>

    /**
     * ====================================
     * Advanced Queries
     * ====================================
     */

    /**
     * Get the latest blood oxygen measurement
     */
    @Query("SELECT * FROM blood_oxygen ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestBloodOxygen(): BloodOxygenEntity?

    /**
     * Get the latest blood oxygen measurement as Flow
     */
    @Query("SELECT * FROM blood_oxygen ORDER BY timestamp DESC LIMIT 1")
    fun getLatestBloodOxygenFlow(): Flow<BloodOxygenEntity?>

    /**
     * Get the latest blood oxygen measurement for a device
     */
    @Query("SELECT * FROM blood_oxygen WHERE deviceId = :deviceId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestBloodOxygenForDevice(deviceId: Long): BloodOxygenEntity?

    /**
     * Get blood oxygen measurements below a threshold
     */
    @Query("SELECT * FROM blood_oxygen WHERE bloodOxygen < :threshold ORDER BY timestamp DESC")
    suspend fun getBloodOxygensBelowThreshold(threshold: Int): List<BloodOxygenEntity>

    /**
     * Get blood oxygen measurements below a threshold for a device
     */
    @Query("SELECT * FROM blood_oxygen WHERE deviceId = :deviceId AND bloodOxygen < :threshold ORDER BY timestamp DESC")
    suspend fun getBloodOxygensBelowThresholdForDevice(deviceId: Long, threshold: Int): List<BloodOxygenEntity>

    /**
     * Get manual blood oxygen measurements
     */
    @Query("SELECT * FROM blood_oxygen WHERE isManualEntry = 1 ORDER BY timestamp DESC")
    suspend fun getManualBloodOxygens(): List<BloodOxygenEntity>

    /**
     * Get automatic blood oxygen measurements
     */
    @Query("SELECT * FROM blood_oxygen WHERE isManualEntry = 0 ORDER BY timestamp DESC")
    suspend fun getAutomaticBloodOxygens(): List<BloodOxygenEntity>
}

/**
 * BloodPressureDao - Data Access Object for blood pressure measurements
 */
@Dao
interface BloodPressureDao {

    /**
     * ====================================
     * Basic CRUD Operations
     * ====================================
     */

    /**
     * Insert a new blood pressure measurement
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bloodPressure: BloodPressureEntity): Long

    /**
     * Insert multiple blood pressure measurements
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(bloodPressures: List<BloodPressureEntity>): List<Long>

    /**
     * Update a blood pressure measurement
     */
    @Update
    suspend fun update(bloodPressure: BloodPressureEntity): Int

    /**
     * Delete a blood pressure measurement
     */
    @Delete
    suspend fun delete(bloodPressure: BloodPressureEntity): Int

    /**
     * Delete a blood pressure measurement by ID
     */
    @Query("DELETE FROM blood_pressure WHERE id = :bloodPressureId")
    suspend fun deleteById(bloodPressureId: Long): Int

    /**
     * Delete all blood pressure measurements
     */
    @Query("DELETE FROM blood_pressure")
    suspend fun deleteAll(): Int

    /**
     * Delete all blood pressure measurements for a device
     */
    @Query("DELETE FROM blood_pressure WHERE deviceId = :deviceId")
    suspend fun deleteAllForDevice(deviceId: Long): Int

    /**
     * ====================================
     * Basic Query Operations
     * ====================================
     */

    /**
     * Get a blood pressure measurement by ID
     */
    @Query("SELECT * FROM blood_pressure WHERE id = :bloodPressureId")
    suspend fun getBloodPressureById(bloodPressureId: Long): BloodPressureEntity?

    /**
     * Get a blood pressure measurement by ID as Flow
     */
    @Query("SELECT * FROM blood_pressure WHERE id = :bloodPressureId")
    fun getBloodPressureByIdFlow(bloodPressureId: Long): Flow<BloodPressureEntity?>

    /**
     * Get all blood pressure measurements
     */
    @Query("SELECT * FROM blood_pressure ORDER BY timestamp DESC")
    suspend fun getAllBloodPressures(): List<BloodPressureEntity>

    /**
     * Get all blood pressure measurements as Flow
     */
    @Query("SELECT * FROM blood_pressure ORDER BY timestamp DESC")
    fun getAllBloodPressuresFlow(): Flow<List<BloodPressureEntity>>

    /**
     * Get all blood pressure measurements for a device
     */
    @Query("SELECT * FROM blood_pressure WHERE deviceId = :deviceId ORDER BY timestamp DESC")
    suspend fun getBloodPressuresForDevice(deviceId: Long): List<BloodPressureEntity>

    /**
     * Get all blood pressure measurements for a device as Flow
     */
    @Query("SELECT * FROM blood_pressure WHERE deviceId = :deviceId ORDER BY timestamp DESC")
    fun getBloodPressuresForDeviceFlow(deviceId: Long): Flow<List<BloodPressureEntity>>

    /**
     * Count all blood pressure measurements
     */
    @Query("SELECT COUNT(*) FROM blood_pressure")
    suspend fun getBloodPressureCount(): Int

    /**
     * ====================================
     * Date Range Queries
     * ====================================
     */

    /**
     * Get blood pressure measurements for a date range
     */
    @Query("SELECT * FROM blood_pressure WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    suspend fun getBloodPressuresInRange(startTime: Long, endTime: Long): List<BloodPressureEntity>

    /**
     * Get blood pressure measurements for a date range as Flow
     */
    @Query("SELECT * FROM blood_pressure WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getBloodPressuresInRangeFlow(startTime: Long, endTime: Long): Flow<List<BloodPressureEntity>>

    /**
     * Get blood pressure measurements for a device in a date range
     */
    @Query("SELECT * FROM blood_pressure WHERE deviceId = :deviceId AND timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    suspend fun getBloodPressuresForDeviceInRange(deviceId: Long, startTime: Long, endTime: Long): List<BloodPressureEntity>

    /**
     * Get blood pressure measurements for a specific date
     */
    @Query("SELECT * FROM blood_pressure WHERE date = :date ORDER BY timestamp DESC")
    suspend fun getBloodPressuresForDate(date: String): List<BloodPressureEntity>

    /**
     * ====================================
     * Statistical Queries
     * ====================================
     */

    /**
     * Get the average systolic for a date range
     */
    @Query("SELECT AVG(systolic) FROM blood_pressure WHERE timestamp BETWEEN :startTime AND :endTime")
    suspend fun getAverageSystolicInRange(startTime: Long, endTime: Long): Float?

    /**
     * Get the average diastolic for a date range
     */
    @Query("SELECT AVG(diastolic) FROM blood_pressure WHERE timestamp BETWEEN :startTime AND :endTime")
    suspend fun getAverageDiastolicInRange(startTime: Long, endTime: Long): Float?

    /**
     * Get blood pressure statistics for a date range
     */
    @Query("""
        SELECT 
            AVG(systolic) as avgSystolic,
            MIN(systolic) as minSystolic,
            MAX(systolic) as maxSystolic,
            AVG(diastolic) as avgDiastolic,
            MIN(diastolic) as minDiastolic,
            MAX(diastolic) as maxDiastolic,
            COUNT(*) as count
        FROM blood_pressure 
        WHERE timestamp BETWEEN :startTime AND :endTime
    """)
    suspend fun getBloodPressureStatsInRange(startTime: Long, endTime: Long): BloodPressureStats?

    /**
     * Get blood pressure statistics by date
     */
    @Query("""
        SELECT 
            date,
            AVG(systolic) as avgSystolic,
            MIN(systolic) as minSystolic,
            MAX(systolic) as maxSystolic,
            AVG(diastolic) as avgDiastolic,
            MIN(diastolic) as minDiastolic,
            MAX(diastolic) as maxDiastolic,
            COUNT(*) as count
        FROM blood_pressure 
        WHERE timestamp BETWEEN :startTime AND :endTime
        GROUP BY date
        ORDER BY date ASC
    """)
    suspend fun getBloodPressureStatsByDate(startTime: Long, endTime: Long): List<BloodPressureStatsByDate>

    /**
     * Get blood pressure statistics by date as Flow
     */
    @Query("""
        SELECT 
            date,
            AVG(systolic) as avgSystolic,
            MIN(systolic) as minSystolic,
            MAX(systolic) as maxSystolic,
            AVG(diastolic) as avgDiastolic,
            MIN(diastolic) as minDiastolic,
            MAX(diastolic) as maxDiastolic,
            COUNT(*) as count
        FROM blood_pressure 
        WHERE timestamp BETWEEN :startTime AND :endTime
        GROUP BY date
        ORDER BY date ASC
    """)
    fun getBloodPressureStatsByDateFlow(startTime: Long, endTime: Long): Flow<List<BloodPressureStatsByDate>>

    /**
     * ====================================
     * Pagination Support
     * ====================================
     */

    /**
     * Get paged blood pressure measurements
     */
    @Query("SELECT * FROM blood_pressure ORDER BY timestamp DESC")
    fun getPagedBloodPressures(): PagingSource<Int, BloodPressureEntity>

    /**
     * Get paged blood pressure measurements for a device
     */
    @Query("SELECT * FROM blood_pressure WHERE deviceId = :deviceId ORDER BY timestamp DESC")
    fun getPagedBloodPressuresForDevice(deviceId: Long): PagingSource<Int, BloodPressureEntity>

    /**
     * Get paged blood pressure measurements for a date range
     */
    @Query("SELECT * FROM blood_pressure WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getPagedBloodPressuresInRange(startTime: Long, endTime: Long): PagingSource<Int, BloodPressureEntity>

    /**
     * ====================================
     * Sync Status Operations
     * ====================================
     */

    /**
     * Get blood pressure measurements by sync status
     */
    @Query("SELECT * FROM blood_pressure WHERE syncStatus = :syncStatus ORDER BY timestamp DESC")
    suspend fun getBloodPressuresBySyncStatus(syncStatus: SyncStatus): List<BloodPressureEntity>

    /**
     * Update blood pressure sync status
     */
    @Query("UPDATE blood_pressure SET syncStatus = :syncStatus, updatedAt = :timestamp WHERE id = :bloodPressureId")
    suspend fun updateBloodPressureSyncStatus(bloodPressureId: Long, syncStatus: SyncStatus, timestamp: Long = System.currentTimeMillis()): Int

    /**
     * Update sync status for multiple blood pressure measurements
     */
    @Query("UPDATE blood_pressure SET syncStatus = :syncStatus, updatedAt = :timestamp WHERE id IN (:bloodPressureIds)")
    suspend fun updateBloodPressuresSyncStatus(bloodPressureIds: List<Long>, syncStatus: SyncStatus, timestamp: Long = System.currentTimeMillis()): Int

    /**
     * Get blood pressure measurements that need syncing
     */
    @Query("SELECT * FROM blood_pressure WHERE syncStatus IN ('PENDING', 'FAILED') ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getBloodPressuresNeedingSync(limit: Int = 100): List<BloodPressureEntity>

    /**
     * ====================================
     * Advanced Queries
     * ====================================
     */

    /**
     * Get the latest blood pressure measurement
     */
    @Query("SELECT * FROM blood_pressure ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestBloodPressure(): BloodPressureEntity?

    /**
     * Get the latest blood pressure measurement as Flow
     */
    @Query("SELECT * FROM blood_pressure ORDER BY timestamp DESC LIMIT 1")
    fun getLatestBloodPressureFlow(): Flow<BloodPressureEntity?>

    /**
     * Get the latest blood pressure measurement for a device
     */
    @Query("SELECT * FROM blood_pressure WHERE deviceId = :deviceId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestBloodPressureForDevice(deviceId: Long): BloodPressureEntity?

    /**
     * Get blood pressure measurements with high systolic
     */
    @Query("SELECT * FROM blood_pressure WHERE systolic >= :threshold ORDER BY timestamp DESC")
    suspend fun getBloodPressuresWithHighSystolic(threshold: Int = 140): List<BloodPressureEntity>

    /**
     * Get blood pressure measurements with high diastolic
     */
    @Query("SELECT * FROM blood_pressure WHERE diastolic >= :threshold ORDER BY timestamp DESC")
    suspend fun getBloodPressuresWithHighDiastolic(threshold: Int = 90): List<BloodPressureEntity>

    /**
     * Get blood pressure measurements with normal range
     */
    @Query("SELECT * FROM blood_pressure WHERE systolic < :systolicThreshold AND diastolic < :diastolicThreshold ORDER BY timestamp DESC")
    suspend fun getBloodPressuresWithNormalRange(systolicThreshold: Int = 140, diastolicThreshold: Int = 90): List<BloodPressureEntity>

    /**
     * Get manual blood pressure measurements
     */
    @Query("SELECT * FROM blood_pressure WHERE isManualEntry = 1 ORDER BY timestamp DESC")
    suspend fun getManualBloodPressures(): List<BloodPressureEntity>
}

/**
 * StepDao - Data Access Object for step and activity data
 */
@Dao
interface StepDao {

    /**
     * ====================================
     * Basic CRUD Operations
     * ====================================
     */

    /**
     * Insert a new step record
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(step: StepEntity): Long

    /**
     * Insert multiple step records
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(steps: List<StepEntity>): List<Long>

    /**
     * Update a step record
     */
    @Update
    suspend fun update(step: StepEntity): Int

    /**
     * Delete a step record
     */
    @Delete
    suspend fun delete(step: StepEntity): Int

    /**
     * Delete a step record by ID
     */
    @Query("DELETE FROM steps WHERE id = :stepId")
    suspend fun deleteById(stepId: Long): Int

    /**
     * Delete all step records
     */
    @Query("DELETE FROM steps")
    suspend fun deleteAll(): Int

    /**
     * Delete all step records for a device
     */
    @Query("DELETE FROM steps WHERE deviceId = :deviceId")
    suspend fun deleteAllForDevice(deviceId: Long): Int

    /**
     * ====================================
     * Basic Query Operations
     * ====================================
     */

    /**
     * Get a step record by ID
     */
    @Query("SELECT * FROM steps WHERE id = :stepId")
    suspend fun getStepById(stepId: Long): StepEntity?

    /**
     * Get a step record by ID as Flow
     */
    @Query("SELECT * FROM steps WHERE id = :stepId")
    fun getStepByIdFlow(stepId: Long): Flow<StepEntity?>

    /**
     * Get all step records
     */
    @Query("SELECT * FROM steps ORDER BY timestamp DESC")
    suspend fun getAllSteps(): List<StepEntity>

    /**
     * Get all step records as Flow
     */
    @Query("SELECT * FROM steps ORDER BY timestamp DESC")
    fun getAllStepsFlow(): Flow<List<StepEntity>>

    /**
     * Get all step records for a device
     */
    @Query("SELECT * FROM steps WHERE deviceId = :deviceId ORDER BY timestamp DESC")
    suspend fun getStepsForDevice(deviceId: Long): List<StepEntity>

    /**
     * Get all step records for a device as Flow
     */
    @Query("SELECT * FROM steps WHERE deviceId = :deviceId ORDER BY timestamp DESC")
    fun getStepsForDeviceFlow(deviceId: Long): Flow<List<StepEntity>>

    /**
     * Count all step records
     */
    @Query("SELECT COUNT(*) FROM steps")
    suspend fun getStepCount(): Int

    /**
     * ====================================
     * Date Range Queries
     * ====================================
     */

    /**
     * Get step records for a date range
     */
    @Query("SELECT * FROM steps WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    suspend fun getStepsInRange(startTime: Long, endTime: Long): List<StepEntity>

    /**
     * Get step records for a date range as Flow
     */
    @Query("SELECT * FROM steps WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getStepsInRangeFlow(startTime: Long, endTime: Long): Flow<List<StepEntity>>

    /**
     * Get step records for a device in a date range
     */
    @Query("SELECT * FROM steps WHERE deviceId = :deviceId AND timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    suspend fun getStepsForDeviceInRange(deviceId: Long, startTime: Long, endTime: Long): List<StepEntity>

    /**
     * Get step records for a specific date
     */
    @Query("SELECT * FROM steps WHERE date = :date ORDER BY timestamp DESC")
    suspend fun getStepsForDate(date: String): List<StepEntity>

    /**
     * Get step records for a specific date as Flow
     */
    @Query("SELECT * FROM steps WHERE date = :date ORDER BY timestamp DESC")
    fun getStepsForDateFlow(date: String): Flow<List<StepEntity>>

    /**
     * Get step record for a device on a specific date
     */
    @Query("SELECT * FROM steps WHERE deviceId = :deviceId AND date = :date")
    suspend fun getStepForDeviceAndDate(deviceId: Long, date: String): StepEntity?

    /**
     * Get step record for a device on a specific date as Flow
     */
    @Query("SELECT * FROM steps WHERE deviceId = :deviceId AND date = :date")
    fun getStepForDeviceAndDateFlow(deviceId: Long, date: String): Flow<StepEntity?>

    /**
     * ====================================
     * Statistical Queries
     * ====================================
     */

    /**
     * Get the total steps for a date range
     */
    @Query("SELECT SUM(steps) FROM steps WHERE timestamp BETWEEN :startTime AND :endTime")
    suspend fun getTotalStepsInRange(startTime: Long, endTime: Long): Int?

    /**
     * Get the total steps for a device in a date range
     */
    @Query("SELECT SUM(steps) FROM steps WHERE deviceId = :deviceId AND timestamp BETWEEN :startTime AND :endTime")
    suspend fun getTotalStepsForDeviceInRange(deviceId: Long, startTime: Long, endTime: Long): Int?

    /**
     * Get the total distance for a date range
     */
    @Query("SELECT SUM(distance) FROM steps WHERE timestamp BETWEEN :startTime AND :endTime")
    suspend fun getTotalDistanceInRange(startTime: Long, endTime: Long): Float?

    /**
     * Get the total calories for a date range
     */
    @Query("SELECT SUM(calories) FROM steps WHERE timestamp BETWEEN :startTime AND :endTime")
    suspend fun getTotalCaloriesInRange(startTime: Long, endTime: Long): Float?

    /**
     * Get the average daily steps for a date range
     */
    @Query("SELECT AVG(steps) FROM steps WHERE timestamp BETWEEN :startTime AND :endTime GROUP BY date")
    suspend fun getAverageDailyStepsInRange(startTime: Long, endTime: Long): Float?

    /**
     * Get step statistics by date
     */
    @Query("""
        SELECT 
            date,
            SUM(steps) as totalSteps,
            SUM(distance) as totalDistance,
            SUM(calories) as totalCalories,
            SUM(activeMinutes) as totalActiveMinutes
        FROM steps 
        WHERE timestamp BETWEEN :startTime AND :endTime
        GROUP BY date
        ORDER BY date ASC
    """)
    suspend fun getStepStatsByDate(startTime: Long, endTime: Long): List<StepStatsByDate>

    /**
     * Get step statistics by date as Flow
     */
    @Query("""
        SELECT 
            date,
            SUM(steps) as totalSteps,
            SUM(distance) as totalDistance,
            SUM(calories) as totalCalories,
            SUM(activeMinutes) as totalActiveMinutes
        FROM steps 
        WHERE timestamp BETWEEN :startTime AND :endTime
        GROUP BY date
        ORDER BY date ASC
    """)
    fun getStepStatsByDateFlow(startTime: Long, endTime: Long): Flow<List<StepStatsByDate>>

    /**
     * Get step statistics by date for a device
     */
    @Query("""
        SELECT 
            date,
            SUM(steps) as totalSteps,
            SUM(distance) as totalDistance,
            SUM(calories) as totalCalories,
            SUM(activeMinutes) as totalActiveMinutes
        FROM steps 
        WHERE deviceId = :deviceId AND timestamp BETWEEN :startTime AND :endTime
        GROUP BY date
        ORDER BY date ASC
    """)
    suspend fun getStepStatsByDateForDevice(deviceId: Long, startTime: Long, endTime: Long): List<StepStatsByDate>

    /**
     * Get step statistics by week
     */
    @Query("""
        SELECT 
            SUBSTR(date, 1, 7) || '-W' || (CAST(STRFTIME('%W', date) AS INTEGER) + 1) as week,
            SUM(steps) as totalSteps,
            SUM(distance) as totalDistance,
            SUM(calories) as totalCalories,
            SUM(activeMinutes) as totalActiveMinutes,
            COUNT(DISTINCT date) as daysCount
        FROM steps 
        WHERE timestamp BETWEEN :startTime AND :endTime
        GROUP BY week
        ORDER BY week ASC
    """)
    suspend fun getStepStatsByWeek(startTime: Long, endTime: Long): List<StepStatsByWeek>

    /**
     * Get step statistics by month
     */
    @Query("""
        SELECT 
            SUBSTR(date, 1, 7) as month,
            SUM(steps) as totalSteps,
            SUM(distance) as totalDistance,
            SUM(calories) as totalCalories,
            SUM(activeMinutes) as totalActiveMinutes,
            COUNT(DISTINCT date) as daysCount
        FROM steps 
        WHERE timestamp BETWEEN :startTime AND :endTime
        GROUP BY month
        ORDER BY month ASC
    """)
    suspend fun getStepStatsByMonth(startTime: Long, endTime: Long): List<StepStatsByMonth>

    /**
     * ====================================
     * Pagination Support
     * ====================================
     */

    /**
     * Get paged step records
     */
    @Query("SELECT * FROM steps ORDER BY timestamp DESC")
    fun getPagedSteps(): PagingSource<Int, StepEntity>

    /**
     * Get paged step records for a device
     */
    @Query("SELECT * FROM steps WHERE deviceId = :deviceId ORDER BY timestamp DESC")
    fun getPagedStepsForDevice(deviceId: Long): PagingSource<Int, StepEntity>

    /**
     * Get paged step records for a date range
     */
    @Query("SELECT * FROM steps WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getPagedStepsInRange(startTime: Long, endTime: Long): PagingSource<Int, StepEntity>

    /**
     * ====================================
     * Sync Status Operations
     * ====================================
     */

    /**
     * Get step records by sync status
     */
    @Query("SELECT * FROM steps WHERE syncStatus = :syncStatus ORDER BY timestamp DESC")
    suspend fun getStepsBySyncStatus(syncStatus: SyncStatus): List<StepEntity>

    /**
     * Update step sync status
     */
    @Query("UPDATE steps SET syncStatus = :syncStatus, updatedAt = :timestamp WHERE id = :stepId")
    suspend fun updateStepSyncStatus(stepId: Long, syncStatus: SyncStatus, timestamp: Long = System.currentTimeMillis()): Int

    /**
     * Update sync status for multiple step records
     */
    @Query("UPDATE steps SET syncStatus = :syncStatus, updatedAt = :timestamp WHERE id IN (:stepIds)")
    suspend fun updateStepsSyncStatus(stepIds: List<Long>, syncStatus: SyncStatus, timestamp: Long = System.currentTimeMillis()): Int

    /**
     * Get step records that need syncing
     */
    @Query("SELECT * FROM steps WHERE syncStatus IN ('PENDING', 'FAILED') ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getStepsNeedingSync(limit: Int = 100): List<StepEntity>

    /**
     * ====================================
     * Hourly Step Operations
     * ====================================
     */

    /**
     * Insert a new hourly step record
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHourlyStep(hourlyStep: HourlyStepEntity): Long

    /**
     * Insert multiple hourly step records
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllHourlySteps(hourlySteps: List<HourlyStepEntity>): List<Long>

    /**
     * Get hourly step records for a specific date
     */
    @Query("SELECT * FROM hourly_steps WHERE date = :date ORDER BY hour ASC")
    suspend fun getHourlyStepsForDate(date: String): List<HourlyStepEntity>

    /**
     * Get hourly step records for a specific date as Flow
     */
    @Query("SELECT * FROM hourly_steps WHERE date = :date ORDER BY hour ASC")
    fun getHourlyStepsForDateFlow(date: String): Flow<List<HourlyStepEntity>>

    /**
     * Get hourly step records for a device on a specific date
     */
    @Query("SELECT * FROM hourly_steps WHERE deviceId = :deviceId AND date = :date ORDER BY hour ASC")
    suspend fun getHourlyStepsForDeviceAndDate(deviceId: Long, date: String): List<HourlyStepEntity>

    /**
     * Get hourly step records for a device on a specific date as Flow
     */
    @Query("SELECT * FROM hourly_steps WHERE deviceId = :deviceId AND date = :date ORDER BY hour ASC")
    fun getHourlyStepsForDeviceAndDateFlow(deviceId: Long, date: String): Flow<List<HourlyStepEntity>>

    /**
     * ====================================
     * Advanced Queries
     * ====================================
     */

    /**
     * Get the latest step record
     */
    @Query("SELECT * FROM steps ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestStep(): StepEntity?

    /**
     * Get the latest step record as Flow
     */
    @Query("SELECT * FROM steps ORDER BY timestamp DESC LIMIT 1")
    fun getLatestStepFlow(): Flow<StepEntity?>

    /**
     * Get the latest step record for a device
     */
    @Query("SELECT * FROM steps WHERE deviceId = :deviceId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestStepForDevice(deviceId: Long): StepEntity?

    /**
     * Get step records where goal was achieved
     */
    @Query("SELECT * FROM steps WHERE goalAchieved = 1 ORDER BY timestamp DESC")
    suspend fun getStepsWithGoalAchieved(): List<StepEntity>

    /**
     * Get step records where goal was achieved for a device
     */
    @Query("SELECT * FROM steps WHERE deviceId = :deviceId AND goalAchieved = 1 ORDER BY timestamp DESC")
    suspend fun getStepsWithGoalAchievedForDevice(deviceId: Long): List<StepEntity>

    /**
     * Get step records with steps above threshold
     */
    @Query("SELECT * FROM steps WHERE steps > :threshold ORDER BY timestamp DESC")
    suspend fun getStepsAboveThreshold(threshold: Int): List<StepEntity>

    /**
     * Get streak of days with goal achieved
     */
    @Query("""
        WITH consecutive_days AS (
            SELECT 
                date,
                goalAchieved,
                date(date, '-' || ROW_NUMBER() OVER (ORDER BY date) || ' day') as grp
            FROM steps
            WHERE deviceId = :deviceId AND goalAchieved = 1
            GROUP BY date
        )
        SELECT 
            MIN(date) as startDate,
            MAX(date) as endDate,
            COUNT(*) as streakDays
        FROM consecutive_days
        GROUP BY grp
        HAVING COUNT(*) >= :minDays
        ORDER BY streakDays DESC, endDate DESC
    """)
    suspend fun getGoalAchievedStreaks(deviceId: Long, minDays: Int = 1): List<StepStreak>

    /**
     * Get current streak of days with goal achieved
     */
    @Query("""
        WITH consecutive_days AS (
            SELECT 
                date,
                goalAchieved,
                date(date, '-' || ROW_NUMBER() OVER (ORDER BY date DESC) || ' day') as grp
            FROM steps
            WHERE deviceId = :deviceId AND goalAchieved = 1 AND date <= :today
            GROUP BY date
        )
        SELECT 
            MIN(date) as startDate,
            MAX(date) as endDate,
            COUNT(*) as streakDays
        FROM consecutive_days
        WHERE endDate = :today
        GROUP BY grp
        ORDER BY streakDays DESC
        LIMIT 1
    """)
    suspend fun getCurrentGoalAchievedStreak(deviceId: Long, today: String): StepStreak?

    /**
     * Get best streak of days with goal achieved
     */
    @Query("""
        WITH consecutive_days AS (
            SELECT 
                date,
                goalAchieved,
                date(date, '-' || ROW_NUMBER() OVER (ORDER BY date) || ' day') as grp
            FROM steps
            WHERE deviceId = :deviceId AND goalAchieved = 1
            GROUP BY date
        )
        SELECT 
            MIN(date) as startDate,
            MAX(date) as endDate,
            COUNT(*) as streakDays
        FROM consecutive_days
        GROUP BY grp
        ORDER BY streakDays DESC
        LIMIT 1
    """)
    suspend fun getBestGoalAchievedStreak(deviceId: Long): StepStreak?

    /**
     * Get manual step records
     */
    @Query("SELECT * FROM steps WHERE isManualEntry = 1 ORDER BY timestamp DESC")
    suspend fun getManualSteps(): List<StepEntity>
}

/**
 * SleepDao - Data Access Object for sleep tracking
 */
@Dao
interface SleepDao {

    /**
     * ====================================
     * Basic CRUD Operations
     * ====================================
     */

    /**
     * Insert a new sleep record
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sleep: SleepEntity): Long

    /**
     * Insert multiple sleep records
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sleeps: List<SleepEntity>): List<Long>

    /**
     * Update a sleep record
     */
    @Update
    suspend fun update(sleep: SleepEntity): Int

    /**
     * Delete a sleep record
     */
    @Delete
    suspend fun delete(sleep: SleepEntity): Int

    /**
     * Delete a sleep record by ID
     */
    @Query("DELETE FROM sleep WHERE id = :sleepId")
    suspend fun deleteById(sleepId: Long): Int

    /**
     * Delete all sleep records
     */
    @Query("DELETE FROM sleep")
    suspend fun deleteAll(): Int

    /**
     * Delete all sleep records for a device
     */
    @Query("DELETE FROM sleep WHERE deviceId = :deviceId")
    suspend fun deleteAllForDevice(deviceId: Long): Int

    /**
     * ====================================
     * Basic Query Operations
     * ====================================
     */

    /**
     * Get a sleep record by ID
     */
    @Query("SELECT * FROM sleep WHERE id = :sleepId")
    suspend fun getSleepById(sleepId: Long): SleepEntity?

    /**
     * Get a sleep record by ID as Flow
     */
    @Query("SELECT * FROM sleep WHERE id = :sleepId")
    fun getSleepByIdFlow(sleepId: Long): Flow<SleepEntity?>

    /**
     * Get all sleep records
     */
    @Query("SELECT * FROM sleep ORDER BY startTime DESC")
    suspend fun getAllSleeps(): List<SleepEntity>

    /**
     * Get all sleep records as Flow
     */
    @Query("SELECT * FROM sleep ORDER BY startTime DESC")
    fun getAllSleepsFlow(): Flow<List<SleepEntity>>

    /**
     * Get all sleep records for a device
     */
    @Query("SELECT * FROM sleep WHERE deviceId = :deviceId ORDER BY startTime DESC")
    suspend fun getSleepsForDevice(deviceId: Long): List<SleepEntity>

    /**
     * Get all sleep records for a device as Flow
     */
    @Query("SELECT * FROM sleep WHERE deviceId = :deviceId ORDER BY startTime DESC")
    fun getSleepsForDeviceFlow(deviceId: Long): Flow<List<SleepEntity>>

    /**
     * Count all sleep records
     */
    @Query("SELECT COUNT(*) FROM sleep")
    suspend fun getSleepCount(): Int

    /**
     * ====================================
     * Date Range Queries
     * ====================================
     */

    /**
     * Get sleep records for a date range
     */
    @Query("SELECT * FROM sleep WHERE startTime BETWEEN :startTime AND :endTime ORDER BY startTime DESC")
    suspend fun getSleepsInRange(startTime: Long, endTime: Long): List<SleepEntity>

    /**
     * Get sleep records for a date range as Flow
     */
    @Query("SELECT * FROM sleep WHERE startTime BETWEEN :startTime AND :endTime ORDER BY startTime DESC")
    fun getSleepsInRangeFlow(startTime: Long, endTime: Long): Flow<List<SleepEntity>>

    /**
     * Get sleep records for a device in a date range
     */
    @Query("SELECT * FROM sleep WHERE deviceId = :deviceId AND startTime BETWEEN :startTime AND :endTime ORDER BY startTime DESC")
    suspend fun getSleepsForDeviceInRange(deviceId: Long, startTime: Long, endTime: Long): List<SleepEntity>

    /**
     * Get sleep records for a specific date
     */
    @Query("SELECT * FROM sleep WHERE date = :date ORDER BY startTime DESC")
    suspend fun getSleepsForDate(date: String): List<SleepEntity>

    /**
     * Get sleep records for a specific date as Flow
     */
    @Query("SELECT * FROM sleep WHERE date = :date ORDER BY startTime DESC")
    fun getSleepsForDateFlow(date: String): Flow<List<SleepEntity>>

    /**
     * Get sleep record for a device on a specific date
     */
    @Query("SELECT * FROM sleep WHERE deviceId = :deviceId AND date = :date")
    suspend fun getSleepForDeviceAndDate(deviceId: Long, date: String): SleepEntity?

    /**
     * Get sleep record for a device on a specific date as Flow
     */
    @Query("SELECT * FROM sleep WHERE deviceId = :deviceId AND date = :date")
    fun getSleepForDeviceAndDateFlow(deviceId: Long, date: String): Flow<SleepEntity?>

    /**
     * ====================================
     * Sleep Detail Operations
     * ====================================
     */

    /**
     * Insert a new sleep detail record
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSleepDetail(sleepDetail: SleepDetailEntity): Long

    /**
     * Insert multiple sleep detail records
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSleepDetails(sleepDetails: List<SleepDetailEntity>): List<Long>

    /**
     * Get sleep detail records for a sleep ID
     */
    @Query("SELECT * FROM sleep_details WHERE sleepId = :sleepId ORDER BY timestamp ASC")
    suspend fun getSleepDetailsForSleep(sleepId: Long): List<SleepDetailEntity>

    /**
     * Get sleep detail records for a sleep ID as Flow
     */
    @Query("SELECT * FROM sleep_details WHERE sleepId = :sleepId ORDER BY timestamp ASC")
    fun getSleepDetailsForSleepFlow(sleepId: Long): Flow<List<SleepDetailEntity>>

    /**
     * Delete sleep detail records for a sleep ID
     */
    @Query("DELETE FROM sleep_details WHERE sleepId = :sleepId")
    suspend fun deleteSleepDetailsForSleep(sleepId: Long): Int

    /**
     * ====================================
     * Statistical Queries
     * ====================================
     */

    /**
     * Get the average sleep duration for a date range
     */
    @Query("SELECT AVG(totalSleepMinutes) FROM sleep WHERE startTime BETWEEN :startTime AND :endTime")
    suspend fun getAverageSleepDurationInRange(startTime: Long, endTime: Long): Float?

    /**
     * Get the average deep sleep duration for a date range
     */
    @Query("SELECT AVG(deepSleepMinutes) FROM sleep WHERE startTime BETWEEN :startTime AND :endTime")
    suspend fun getAverageDeepSleepDurationInRange(startTime: Long, endTime: Long): Float?

    /**
     * Get the average light sleep duration for a date range
     */
    @Query("SELECT AVG(lightSleepMinutes) FROM sleep WHERE startTime BETWEEN :startTime AND :endTime")
    suspend fun getAverageLightSleepDurationInRange(startTime: Long, endTime: Long): Float?

    /**
     * Get the average REM sleep duration for a date range
     */
    @Query("SELECT AVG(remSleepMinutes) FROM sleep WHERE startTime BETWEEN :startTime AND :endTime")
    suspend fun getAverageRemSleepDurationInRange(startTime: Long, endTime: Long): Float?

    /**
     * Get the average awake duration for a date range
     */
    @Query("SELECT AVG(awakeSleepMinutes) FROM sleep WHERE startTime BETWEEN :startTime AND :endTime")
    suspend fun getAverageAwakeDurationInRange(startTime: Long, endTime: Long): Float?

    /**
     * Get the average sleep quality for a date range
     */
    @Query("SELECT AVG(sleepQuality) FROM sleep WHERE startTime BETWEEN :startTime AND :endTime")
    suspend fun getAverageSleepQualityInRange(startTime: Long, endTime: Long): Float?

    /**
     * Get sleep statistics for a date range
     */
    @Query("""
        SELECT 
            AVG(totalSleepMinutes) as avgTotalSleep,
            MIN(totalSleepMinutes) as minTotalSleep,
            MAX(totalSleepMinutes) as maxTotalSleep,
            AVG(deepSleepMinutes) as avgDeepSleep,
            AVG(lightSleepMinutes) as avgLightSleep,
            AVG(remSleepMinutes) as avgRemSleep,
            AVG(awakeSleepMinutes) as avgAwakeSleep,
            AVG(sleepQuality) as avgSleepQuality,
            COUNT(*) as count
        FROM sleep 
        WHERE startTime BETWEEN :startTime AND :endTime
    """)
    suspend fun getSleepStatsInRange(startTime: Long, endTime: Long): SleepStats?

    /**
     * Get sleep statistics by date
     */
    @Query("""
        SELECT 
            date,
            SUM(totalSleepMinutes) as totalSleepMinutes,
            SUM(deepSleepMinutes) as deepSleepMinutes,
            SUM(lightSleepMinutes) as lightSleepMinutes,
            SUM(remSleepMinutes) as remSleepMinutes,
            SUM(awakeSleepMinutes) as awakeSleepMinutes,
            AVG(sleepQuality) as avgSleepQuality
        FROM sleep 
        WHERE startTime BETWEEN :startTime AND :endTime
        GROUP BY date
        ORDER BY date ASC
    """)
    suspend fun getSleepStatsByDate(startTime: Long, endTime: Long): List<SleepStatsByDate>

    /**
     * Get sleep statistics by date as Flow
     */
    @Query("""
        SELECT 
            date,
            SUM(totalSleepMinutes) as totalSleepMinutes,
            SUM(deepSleepMinutes) as deepSleepMinutes,
            SUM(lightSleepMinutes) as lightSleepMinutes,
            SUM(remSleepMinutes) as remSleepMinutes,
            SUM(awakeSleepMinutes) as awakeSleepMinutes,
            AVG(sleepQuality) as avgSleepQuality
        FROM sleep 
        WHERE startTime BETWEEN :startTime AND :endTime
        GROUP BY date
        ORDER BY date ASC
    """)
    fun getSleepStatsByDateFlow(startTime: Long, endTime: Long): Flow<List<SleepStatsByDate>>

    /**
     * Get sleep statistics by week
     */
    @Query("""
        SELECT 
            SUBSTR(date, 1, 7) || '-W' || (CAST(STRFTIME('%W', date) AS INTEGER) + 1) as week,
            AVG(totalSleepMinutes) as avgTotalSleep,
            AVG(deepSleepMinutes) as avgDeepSleep,
            AVG(lightSleepMinutes) as avgLightSleep,
            AVG(remSleepMinutes) as avgRemSleep,
            AVG(awakeSleepMinutes) as avgAwakeSleep,
            AVG(sleepQuality) as avgSleepQuality,
            COUNT(DISTINCT date) as daysCount
        FROM sleep 
        WHERE startTime BETWEEN :startTime AND :endTime
        GROUP BY week
        ORDER BY week ASC
    """)
    suspend fun getSleepStatsByWeek(startTime: Long, endTime: Long): List<SleepStatsByWeek>

    /**
     * ====================================
     * Pagination Support
     * ====================================
     */

    /**
     * Get paged sleep records
     */
    @Query("SELECT * FROM sleep ORDER BY startTime DESC")
    fun getPagedSleeps(): PagingSource<Int, SleepEntity>

    /**
     * Get paged sleep records for a device
     */
    @Query("SELECT * FROM sleep WHERE deviceId = :deviceId ORDER BY startTime DESC")
    fun getPagedSleepsForDevice(deviceId: Long): PagingSource<Int, SleepEntity>

    /**
     * Get paged sleep records for a date range
     */
    @Query("SELECT * FROM sleep WHERE startTime BETWEEN :startTime AND :endTime ORDER BY startTime DESC")
    fun getPagedSleepsInRange(startTime: Long, endTime: Long): PagingSource<Int, SleepEntity>

    /**
     * ====================================
     * Sync Status Operations
     * ====================================
     */

    /**
     * Get sleep records by sync status
     */
    @Query("SELECT * FROM sleep WHERE syncStatus = :syncStatus ORDER BY startTime DESC")
    suspend fun getSleepsBySyncStatus(syncStatus: SyncStatus): List<SleepEntity>

    /**
     * Update sleep sync status
     */
    @Query("UPDATE sleep SET syncStatus = :syncStatus, updatedAt = :timestamp WHERE id = :sleepId")
    suspend fun updateSleepSyncStatus(sleepId: Long, syncStatus: SyncStatus, timestamp: Long = System.currentTimeMillis()): Int

    /**
     * Update sync status for multiple sleep records
     */
    @Query("UPDATE sleep SET syncStatus = :syncStatus, updatedAt = :timestamp WHERE id IN (:sleepIds)")
    suspend fun updateSleepsSyncStatus(sleepIds: List<Long>, syncStatus: SyncStatus, timestamp: Long = System.currentTimeMillis()): Int

    /**
     * Get sleep records that need syncing
     */
    @Query("SELECT * FROM sleep WHERE syncStatus IN ('PENDING', 'FAILED') ORDER BY startTime ASC LIMIT :limit")
    suspend fun getSleepsNeedingSync(limit: Int = 100): List<SleepEntity>

    /**
     * ====================================
     * Advanced Queries
     * ====================================
     */

    /**
     * Get the latest sleep record
     */
    @Query("SELECT * FROM sleep ORDER BY startTime DESC LIMIT 1")
    suspend fun getLatestSleep(): SleepEntity?

    /**
     * Get the latest sleep record as Flow
     */
    @Query("SELECT * FROM sleep ORDER BY startTime DESC LIMIT 1")
    fun getLatestSleepFlow(): Flow<SleepEntity?>

    /**
     * Get the latest sleep record for a device
     */
    @Query("SELECT * FROM sleep WHERE deviceId = :deviceId ORDER BY startTime DESC LIMIT 1")
    suspend fun getLatestSleepForDevice(deviceId: Long): SleepEntity?

    /**
     * Get sleep records with good quality
     */
    @Query("SELECT * FROM sleep WHERE sleepQuality >= :threshold ORDER BY startTime DESC")
    suspend fun getSleepsWithGoodQuality(threshold: Int = 80): List<SleepEntity>

    /**
     * Get sleep records with poor quality
     */
    @Query("SELECT * FROM sleep WHERE sleepQuality < :threshold ORDER BY startTime DESC")
    suspend fun getSleepsWithPoorQuality(threshold: Int = 60): List<SleepEntity>

    /**
     * Get sleep records with long duration
     */
    @Query("SELECT * FROM sleep WHERE totalSleepMinutes >= :thresholdMinutes ORDER BY startTime DESC