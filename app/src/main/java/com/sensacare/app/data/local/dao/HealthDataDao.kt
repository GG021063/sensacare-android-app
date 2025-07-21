package com.sensacare.app.data.local.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.sensacare.app.data.local.entity.HealthDataEntity
import com.sensacare.app.data.local.entity.MetricType
import com.sensacare.app.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * HealthDataDao - Data Access Object for the core health data entity
 *
 * This DAO provides comprehensive access to the health_data table, including:
 * - Basic CRUD operations (create, read, update, delete)
 * - Complex queries for analytics and reporting
 * - Time-series data analysis and aggregation
 * - Sync status management for offline-first architecture
 * - Batch operations for efficient synchronization
 * - Reactive queries with Flow for real-time UI updates
 * - Paging support for large datasets
 *
 * The HealthDataDao serves as the foundation for all health metric data access,
 * with more specialized DAOs (HeartRateDao, BloodPressureDao, etc.) building on top
 * of this core functionality for metric-specific operations.
 */
@Dao
interface HealthDataDao {

    /**
     * Basic CRUD Operations
     */

    /**
     * Insert a single health data record
     * @param healthData The health data entity to insert
     * @return The row ID of the inserted item
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(healthData: HealthDataEntity): Long

    /**
     * Insert multiple health data records in a single transaction
     * @param healthDataList List of health data entities to insert
     * @return List of row IDs for the inserted items
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(healthDataList: List<HealthDataEntity>): List<Long>

    /**
     * Update a health data record
     * @param healthData The health data entity to update
     * @return Number of rows updated
     */
    @Update
    suspend fun update(healthData: HealthDataEntity): Int

    /**
     * Update multiple health data records in a single transaction
     * @param healthDataList List of health data entities to update
     * @return Number of rows updated
     */
    @Update
    suspend fun updateAll(healthDataList: List<HealthDataEntity>): Int

    /**
     * Upsert (insert or update) a health data record
     * @param healthData The health data entity to upsert
     */
    @Transaction
    suspend fun upsert(healthData: HealthDataEntity) {
        val id = insert(healthData)
        if (id == -1L) {
            update(healthData)
        }
    }

    /**
     * Upsert multiple health data records in a single transaction
     * @param healthDataList List of health data entities to upsert
     */
    @Transaction
    suspend fun upsertAll(healthDataList: List<HealthDataEntity>) {
        for (healthData in healthDataList) {
            upsert(healthData)
        }
    }

    /**
     * Delete a health data record
     * @param healthData The health data entity to delete
     * @return Number of rows deleted
     */
    @Delete
    suspend fun delete(healthData: HealthDataEntity): Int

    /**
     * Delete health data by ID
     * @param id The ID of the health data to delete
     * @return Number of rows deleted
     */
    @Query("DELETE FROM health_data WHERE id = :id")
    suspend fun deleteById(id: String): Int

    /**
     * Delete multiple health data records by their IDs
     * @param ids List of IDs to delete
     * @return Number of rows deleted
     */
    @Query("DELETE FROM health_data WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>): Int

    /**
     * Delete all health data for a specific user
     * @param userId The user ID
     * @return Number of rows deleted
     */
    @Query("DELETE FROM health_data WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String): Int

    /**
     * Delete all health data for a specific metric type
     * @param userId The user ID
     * @param metricType The metric type to delete
     * @return Number of rows deleted
     */
    @Query("DELETE FROM health_data WHERE userId = :userId AND metricType = :metricType")
    suspend fun deleteAllByMetricType(userId: String, metricType: String): Int

    /**
     * Delete all health data older than a specified date
     * @param userId The user ID
     * @param olderThan The cutoff date
     * @return Number of rows deleted
     */
    @Query("DELETE FROM health_data WHERE userId = :userId AND timestamp < :olderThan")
    suspend fun deleteOlderThan(userId: String, olderThan: LocalDateTime): Int

    /**
     * Basic Queries
     */

    /**
     * Get health data by ID
     * @param id The ID of the health data to retrieve
     * @return The health data entity or null if not found
     */
    @Query("SELECT * FROM health_data WHERE id = :id")
    suspend fun getById(id: String): HealthDataEntity?

    /**
     * Get health data by ID as Flow for reactive updates
     * @param id The ID of the health data to retrieve
     * @return Flow emitting the health data entity
     */
    @Query("SELECT * FROM health_data WHERE id = :id")
    fun getByIdAsFlow(id: String): Flow<HealthDataEntity?>

    /**
     * Get all health data for a specific user
     * @param userId The user ID
     * @return List of health data entities
     */
    @Query("SELECT * FROM health_data WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getAllForUser(userId: String): List<HealthDataEntity>

    /**
     * Get all health data for a specific user as Flow for reactive updates
     * @param userId The user ID
     * @return Flow emitting list of health data entities
     */
    @Query("SELECT * FROM health_data WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllForUserAsFlow(userId: String): Flow<List<HealthDataEntity>>

    /**
     * Get all health data for a specific metric type
     * @param userId The user ID
     * @param metricType The metric type to retrieve
     * @return List of health data entities
     */
    @Query("SELECT * FROM health_data WHERE userId = :userId AND metricType = :metricType ORDER BY timestamp DESC")
    suspend fun getAllByMetricType(userId: String, metricType: String): List<HealthDataEntity>

    /**
     * Get all health data for a specific metric type as Flow for reactive updates
     * @param userId The user ID
     * @param metricType The metric type to retrieve
     * @return Flow emitting list of health data entities
     */
    @Query("SELECT * FROM health_data WHERE userId = :userId AND metricType = :metricType ORDER BY timestamp DESC")
    fun getAllByMetricTypeAsFlow(userId: String, metricType: String): Flow<List<HealthDataEntity>>

    /**
     * Get health data for a specific time range
     * @param userId The user ID
     * @param startTime Start of the time range
     * @param endTime End of the time range
     * @return List of health data entities
     */
    @Query("SELECT * FROM health_data WHERE userId = :userId AND timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    suspend fun getByTimeRange(
        userId: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): List<HealthDataEntity>

    /**
     * Get health data for a specific time range as Flow for reactive updates
     * @param userId The user ID
     * @param startTime Start of the time range
     * @param endTime End of the time range
     * @return Flow emitting list of health data entities
     */
    @Query("SELECT * FROM health_data WHERE userId = :userId AND timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getByTimeRangeAsFlow(
        userId: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): Flow<List<HealthDataEntity>>

    /**
     * Get health data for a specific metric type and time range
     * @param userId The user ID
     * @param metricType The metric type to retrieve
     * @param startTime Start of the time range
     * @param endTime End of the time range
     * @return List of health data entities
     */
    @Query("SELECT * FROM health_data WHERE userId = :userId AND metricType = :metricType AND timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    suspend fun getByMetricTypeAndTimeRange(
        userId: String,
        metricType: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): List<HealthDataEntity>

    /**
     * Get health data for a specific metric type and time range as Flow for reactive updates
     * @param userId The user ID
     * @param metricType The metric type to retrieve
     * @param startTime Start of the time range
     * @param endTime End of the time range
     * @return Flow emitting list of health data entities
     */
    @Query("SELECT * FROM health_data WHERE userId = :userId AND metricType = :metricType AND timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getByMetricTypeAndTimeRangeAsFlow(
        userId: String,
        metricType: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): Flow<List<HealthDataEntity>>

    /**
     * Get health data from a specific device
     * @param userId The user ID
     * @param deviceId The device ID
     * @return List of health data entities
     */
    @Query("SELECT * FROM health_data WHERE userId = :userId AND deviceId = :deviceId ORDER BY timestamp DESC")
    suspend fun getByDevice(userId: String, deviceId: String): List<HealthDataEntity>

    /**
     * Get health data from a specific device as Flow for reactive updates
     * @param userId The user ID
     * @param deviceId The device ID
     * @return Flow emitting list of health data entities
     */
    @Query("SELECT * FROM health_data WHERE userId = :userId AND deviceId = :deviceId ORDER BY timestamp DESC")
    fun getByDeviceAsFlow(userId: String, deviceId: String): Flow<List<HealthDataEntity>>

    /**
     * Get the latest health data record for a specific metric type
     * @param userId The user ID
     * @param metricType The metric type to retrieve
     * @return The latest health data entity or null if not found
     */
    @Query("SELECT * FROM health_data WHERE userId = :userId AND metricType = :metricType ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestByMetricType(userId: String, metricType: String): HealthDataEntity?

    /**
     * Get the latest health data record for a specific metric type as Flow for reactive updates
     * @param userId The user ID
     * @param metricType The metric type to retrieve
     * @return Flow emitting the latest health data entity
     */
    @Query("SELECT * FROM health_data WHERE userId = :userId AND metricType = :metricType ORDER BY timestamp DESC LIMIT 1")
    fun getLatestByMetricTypeAsFlow(userId: String, metricType: String): Flow<HealthDataEntity?>

    /**
     * Pagination Support
     */

    /**
     * Get paged health data for a specific user
     * @param userId The user ID
     * @return PagingSource for health data entities
     */
    @Query("SELECT * FROM health_data WHERE userId = :userId ORDER BY timestamp DESC")
    fun getPagedHealthData(userId: String): PagingSource<Int, HealthDataEntity>

    /**
     * Get paged health data for a specific metric type
     * @param userId The user ID
     * @param metricType The metric type to retrieve
     * @return PagingSource for health data entities
     */
    @Query("SELECT * FROM health_data WHERE userId = :userId AND metricType = :metricType ORDER BY timestamp DESC")
    fun getPagedHealthDataByMetricType(userId: String, metricType: String): PagingSource<Int, HealthDataEntity>

    /**
     * Get paged health data for a specific time range
     * @param userId The user ID
     * @param startTime Start of the time range
     * @param endTime End of the time range
     * @return PagingSource for health data entities
     */
    @Query("SELECT * FROM health_data WHERE userId = :userId AND timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getPagedHealthDataByTimeRange(
        userId: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): PagingSource<Int, HealthDataEntity>

    /**
     * Aggregation Queries
     */

    /**
     * Get the count of health data records for a specific metric type
     * @param userId The user ID
     * @param metricType The metric type to count
     * @return Count of health data records
     */
    @Query("SELECT COUNT(*) FROM health_data WHERE userId = :userId AND metricType = :metricType")
    suspend fun getCountByMetricType(userId: String, metricType: String): Int

    /**
     * Get the count of health data records for a specific time range
     * @param userId The user ID
     * @param startTime Start of the time range
     * @param endTime End of the time range
     * @return Count of health data records
     */
    @Query("SELECT COUNT(*) FROM health_data WHERE userId = :userId AND timestamp BETWEEN :startTime AND :endTime")
    suspend fun getCountByTimeRange(
        userId: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): Int

    /**
     * Get the count of health data records for a specific metric type and time range
     * @param userId The user ID
     * @param metricType The metric type to count
     * @param startTime Start of the time range
     * @param endTime End of the time range
     * @return Count of health data records
     */
    @Query("SELECT COUNT(*) FROM health_data WHERE userId = :userId AND metricType = :metricType AND timestamp BETWEEN :startTime AND :endTime")
    suspend fun getCountByMetricTypeAndTimeRange(
        userId: String,
        metricType: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): Int

    /**
     * Get the average value for a specific metric type
     * @param userId The user ID
     * @param metricType The metric type to average
     * @return Average value or null if no data
     */
    @Query("SELECT AVG(value) FROM health_data WHERE userId = :userId AND metricType = :metricType")
    suspend fun getAverageByMetricType(userId: String, metricType: String): Double?

    /**
     * Get the average value for a specific metric type and time range
     * @param userId The user ID
     * @param metricType The metric type to average
     * @param startTime Start of the time range
     * @param endTime End of the time range
     * @return Average value or null if no data
     */
    @Query("SELECT AVG(value) FROM health_data WHERE userId = :userId AND metricType = :metricType AND timestamp BETWEEN :startTime AND :endTime")
    suspend fun getAverageByMetricTypeAndTimeRange(
        userId: String,
        metricType: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): Double?

    /**
     * Get the minimum value for a specific metric type
     * @param userId The user ID
     * @param metricType The metric type to get minimum for
     * @return Minimum value or null if no data
     */
    @Query("SELECT MIN(value) FROM health_data WHERE userId = :userId AND metricType = :metricType")
    suspend fun getMinByMetricType(userId: String, metricType: String): Double?

    /**
     * Get the minimum value for a specific metric type and time range
     * @param userId The user ID
     * @param metricType The metric type to get minimum for
     * @param startTime Start of the time range
     * @param endTime End of the time range
     * @return Minimum value or null if no data
     */
    @Query("SELECT MIN(value) FROM health_data WHERE userId = :userId AND metricType = :metricType AND timestamp BETWEEN :startTime AND :endTime")
    suspend fun getMinByMetricTypeAndTimeRange(
        userId: String,
        metricType: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): Double?

    /**
     * Get the maximum value for a specific metric type
     * @param userId The user ID
     * @param metricType The metric type to get maximum for
     * @return Maximum value or null if no data
     */
    @Query("SELECT MAX(value) FROM health_data WHERE userId = :userId AND metricType = :metricType")
    suspend fun getMaxByMetricType(userId: String, metricType: String): Double?

    /**
     * Get the maximum value for a specific metric type and time range
     * @param userId The user ID
     * @param metricType The metric type to get maximum for
     * @param startTime Start of the time range
     * @param endTime End of the time range
     * @return Maximum value or null if no data
     */
    @Query("SELECT MAX(value) FROM health_data WHERE userId = :userId AND metricType = :metricType AND timestamp BETWEEN :startTime AND :endTime")
    suspend fun getMaxByMetricTypeAndTimeRange(
        userId: String,
        metricType: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): Double?

    /**
     * Get the sum of values for a specific metric type
     * @param userId The user ID
     * @param metricType The metric type to sum
     * @return Sum of values or null if no data
     */
    @Query("SELECT SUM(value) FROM health_data WHERE userId = :userId AND metricType = :metricType")
    suspend fun getSumByMetricType(userId: String, metricType: String): Double?

    /**
     * Get the sum of values for a specific metric type and time range
     * @param userId The user ID
     * @param metricType The metric type to sum
     * @param startTime Start of the time range
     * @param endTime End of the time range
     * @return Sum of values or null if no data
     */
    @Query("SELECT SUM(value) FROM health_data WHERE userId = :userId AND metricType = :metricType AND timestamp BETWEEN :startTime AND :endTime")
    suspend fun getSumByMetricTypeAndTimeRange(
        userId: String,
        metricType: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): Double?

    /**
     * Get daily aggregated data for a specific metric type
     * @param userId The user ID
     * @param metricType The metric type to aggregate
     * @param startDate Start date
     * @param endDate End date
     * @return List of daily aggregated values (date, avg, min, max, count)
     */
    @Query("""
        SELECT 
            date(timestamp) as date, 
            AVG(value) as avgValue, 
            MIN(value) as minValue, 
            MAX(value) as maxValue, 
            COUNT(*) as count
        FROM health_data 
        WHERE userId = :userId 
        AND metricType = :metricType 
        AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
        GROUP BY date(timestamp)
        ORDER BY date(timestamp)
    """)
    suspend fun getDailyAggregates(
        userId: String,
        metricType: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<DailyAggregate>

    /**
     * Get daily aggregated data for a specific metric type as Flow for reactive updates
     * @param userId The user ID
     * @param metricType The metric type to aggregate
     * @param startDate Start date
     * @param endDate End date
     * @return Flow emitting list of daily aggregated values
     */
    @Query("""
        SELECT 
            date(timestamp) as date, 
            AVG(value) as avgValue, 
            MIN(value) as minValue, 
            MAX(value) as maxValue, 
            COUNT(*) as count
        FROM health_data 
        WHERE userId = :userId 
        AND metricType = :metricType 
        AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
        GROUP BY date(timestamp)
        ORDER BY date(timestamp)
    """)
    fun getDailyAggregatesAsFlow(
        userId: String,
        metricType: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<DailyAggregate>>

    /**
     * Get hourly aggregated data for a specific metric type and date
     * @param userId The user ID
     * @param metricType The metric type to aggregate
     * @param date The date to aggregate for
     * @return List of hourly aggregated values (hour, avg, min, max, count)
     */
    @Query("""
        SELECT 
            strftime('%H', timestamp) as hour, 
            AVG(value) as avgValue, 
            MIN(value) as minValue, 
            MAX(value) as maxValue, 
            COUNT(*) as count
        FROM health_data 
        WHERE userId = :userId 
        AND metricType = :metricType 
        AND date(timestamp) = date(:date)
        GROUP BY strftime('%H', timestamp)
        ORDER BY hour
    """)
    suspend fun getHourlyAggregates(
        userId: String,
        metricType: String,
        date: LocalDate
    ): List<HourlyAggregate>

    /**
     * Get hourly aggregated data for a specific metric type and date as Flow for reactive updates
     * @param userId The user ID
     * @param metricType The metric type to aggregate
     * @param date The date to aggregate for
     * @return Flow emitting list of hourly aggregated values
     */
    @Query("""
        SELECT 
            strftime('%H', timestamp) as hour, 
            AVG(value) as avgValue, 
            MIN(value) as minValue, 
            MAX(value) as maxValue, 
            COUNT(*) as count
        FROM health_data 
        WHERE userId = :userId 
        AND metricType = :metricType 
        AND date(timestamp) = date(:date)
        GROUP BY strftime('%H', timestamp)
        ORDER BY hour
    """)
    fun getHourlyAggregatesAsFlow(
        userId: String,
        metricType: String,
        date: LocalDate
    ): Flow<List<HourlyAggregate>>

    /**
     * Get weekly aggregated data for a specific metric type
     * @param userId The user ID
     * @param metricType The metric type to aggregate
     * @param startDate Start date
     * @param endDate End date
     * @return List of weekly aggregated values (week, avg, min, max, count)
     */
    @Query("""
        SELECT 
            strftime('%Y-%W', timestamp) as week, 
            AVG(value) as avgValue, 
            MIN(value) as minValue, 
            MAX(value) as maxValue, 
            COUNT(*) as count
        FROM health_data 
        WHERE userId = :userId 
        AND metricType = :metricType 
        AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
        GROUP BY strftime('%Y-%W', timestamp)
        ORDER BY week
    """)
    suspend fun getWeeklyAggregates(
        userId: String,
        metricType: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<WeeklyAggregate>

    /**
     * Get monthly aggregated data for a specific metric type
     * @param userId The user ID
     * @param metricType The metric type to aggregate
     * @param startDate Start date
     * @param endDate End date
     * @return List of monthly aggregated values (month, avg, min, max, count)
     */
    @Query("""
        SELECT 
            strftime('%Y-%m', timestamp) as month, 
            AVG(value) as avgValue, 
            MIN(value) as minValue, 
            MAX(value) as maxValue, 
            COUNT(*) as count
        FROM health_data 
        WHERE userId = :userId 
        AND metricType = :metricType 
        AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
        GROUP BY strftime('%Y-%m', timestamp)
        ORDER BY month
    """)
    suspend fun getMonthlyAggregates(
        userId: String,
        metricType: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<MonthlyAggregate>

    /**
     * Trend Analysis Queries
     */

    /**
     * Get trend data for a specific metric type
     * @param userId The user ID
     * @param metricType The metric type to analyze
     * @param days Number of days to analyze
     * @return List of daily average values for trend analysis
     */
    @Query("""
        SELECT 
            date(timestamp) as date, 
            AVG(value) as value
        FROM health_data 
        WHERE userId = :userId 
        AND metricType = :metricType 
        AND timestamp >= datetime('now', '-' || :days || ' days')
        GROUP BY date(timestamp)
        ORDER BY date(timestamp)
    """)
    suspend fun getTrendData(
        userId: String,
        metricType: String,
        days: Int
    ): List<TrendPoint>

    /**
     * Get trend data for a specific metric type as Flow for reactive updates
     * @param userId The user ID
     * @param metricType The metric type to analyze
     * @param days Number of days to analyze
     * @return Flow emitting list of daily average values
     */
    @Query("""
        SELECT 
            date(timestamp) as date, 
            AVG(value) as value
        FROM health_data 
        WHERE userId = :userId 
        AND metricType = :metricType 
        AND timestamp >= datetime('now', '-' || :days || ' days')
        GROUP BY date(timestamp)
        ORDER BY date(timestamp)
    """)
    fun getTrendDataAsFlow(
        userId: String,
        metricType: String,
        days: Int
    ): Flow<List<TrendPoint>>

    /**
     * Calculate the rate of change for a specific metric type
     * @param userId The user ID
     * @param metricType The metric type to analyze
     * @param days Number of days to analyze
     * @return Rate of change per day (positive = increasing, negative = decreasing)
     */
    @Query("""
        WITH daily_avgs AS (
            SELECT 
                date(timestamp) as date, 
                AVG(value) as avg_value
            FROM health_data 
            WHERE userId = :userId 
            AND metricType = :metricType 
            AND timestamp >= datetime('now', '-' || :days || ' days')
            GROUP BY date(timestamp)
            ORDER BY date(timestamp)
        )
        SELECT 
            CASE 
                WHEN COUNT(*) <= 1 THEN 0
                ELSE (last_value(avg_value) OVER (ORDER BY date) - first_value(avg_value) OVER (ORDER BY date)) / (COUNT(*) - 1)
            END as rate_of_change
        FROM daily_avgs
    """)
    suspend fun getRateOfChange(
        userId: String,
        metricType: String,
        days: Int
    ): Double

    /**
     * Sync-Related Queries
     */

    /**
     * Get all health data records that need to be synced
     * @param userId The user ID
     * @return List of health data entities that need syncing
     */
    @Query("SELECT * FROM health_data WHERE userId = :userId AND syncStatus = :syncStatus")
    suspend fun getUnsynced(
        userId: String,
        syncStatus: String = SyncStatus.LOCAL.name
    ): List<HealthDataEntity>

    /**
     * Get count of unsynced health data records
     * @param userId The user ID
     * @return Count of unsynced records
     */
    @Query("SELECT COUNT(*) FROM health_data WHERE userId = :userId AND syncStatus = :syncStatus")
    suspend fun getUnsyncedCount(
        userId: String,
        syncStatus: String = SyncStatus.LOCAL.name
    ): Int

    /**
     * Update sync status for a health data record
     * @param id The ID of the health data record
     * @param syncStatus The new sync status
     * @param remoteId The remote ID from the server
     * @param errorMessage Error message if sync failed
     * @return Number of rows updated
     */
    @Query("""
        UPDATE health_data 
        SET 
            syncStatus = :syncStatus, 
            remoteId = :remoteId, 
            lastSyncAttempt = :lastSyncAttempt,
            syncErrorMessage = :errorMessage,
            modifiedAt = :modifiedAt
        WHERE id = :id
    """)
    suspend fun updateSyncStatus(
        id: String,
        syncStatus: String,
        remoteId: String? = null,
        lastSyncAttempt: LocalDateTime = LocalDateTime.now(),
        errorMessage: String? = null,
        modifiedAt: LocalDateTime = LocalDateTime.now()
    ): Int

    /**
     * Update sync status for multiple health data records
     * @param ids List of IDs to update
     * @param syncStatus The new sync status
     * @return Number of rows updated
     */
    @Query("""
        UPDATE health_data 
        SET 
            syncStatus = :syncStatus, 
            lastSyncAttempt = :lastSyncAttempt,
            modifiedAt = :modifiedAt
        WHERE id IN (:ids)
    """)
    suspend fun updateSyncStatusBatch(
        ids: List<String>,
        syncStatus: String,
        lastSyncAttempt: LocalDateTime = LocalDateTime.now(),
        modifiedAt: LocalDateTime = LocalDateTime.now()
    ): Int

    /**
     * Mark all health data as needing sync
     * @param userId The user ID
     * @return Number of rows updated
     */
    @Query("""
        UPDATE health_data 
        SET 
            syncStatus = :syncStatus, 
            modifiedAt = :modifiedAt
        WHERE userId = :userId
    """)
    suspend fun markAllForSync(
        userId: String,
        syncStatus: String = SyncStatus.LOCAL.name,
        modifiedAt: LocalDateTime = LocalDateTime.now()
    ): Int

    /**
     * Dashboard and Analytics Queries
     */

    /**
     * Get the latest value for each metric type
     * @param userId The user ID
     * @return List of latest values for each metric type
     */
    @Query("""
        SELECT * FROM health_data h1
        WHERE userId = :userId
        AND timestamp = (
            SELECT MAX(timestamp) FROM health_data h2
            WHERE h2.userId = h1.userId
            AND h2.metricType = h1.metricType
        )
    """)
    suspend fun getLatestForAllMetrics(userId: String): List<HealthDataEntity>

    /**
     * Get the latest value for each metric type as Flow for reactive updates
     * @param userId The user ID
     * @return Flow emitting list of latest values for each metric type
     */
    @Query("""
        SELECT * FROM health_data h1
        WHERE userId = :userId
        AND timestamp = (
            SELECT MAX(timestamp) FROM health_data h2
            WHERE h2.userId = h1.userId
            AND h2.metricType = h1.metricType
        )
    """)
    fun getLatestForAllMetricsAsFlow(userId: String): Flow<List<HealthDataEntity>>

    /**
     * Get daily summary for dashboard
     * @param userId The user ID
     * @param date The date to summarize
     * @return List of daily summary items for each metric type
     */
    @Query("""
        SELECT 
            metricType,
            AVG(value) as avgValue,
            MIN(value) as minValue,
            MAX(value) as maxValue,
            COUNT(*) as count
        FROM health_data
        WHERE userId = :userId
        AND date(timestamp) = date(:date)
        GROUP BY metricType
    """)
    suspend fun getDailySummary(
        userId: String,
        date: LocalDate = LocalDate.now()
    ): List<MetricSummary>

    /**
     * Get daily summary for dashboard as Flow for reactive updates
     * @param userId The user ID
     * @param date The date to summarize
     * @return Flow emitting list of daily summary items
     */
    @Query("""
        SELECT 
            metricType,
            AVG(value) as avgValue,
            MIN(value) as minValue,
            MAX(value) as maxValue,
            COUNT(*) as count
        FROM health_data
        WHERE userId = :userId
        AND date(timestamp) = date(:date)
        GROUP BY metricType
    """)
    fun getDailySummaryAsFlow(
        userId: String,
        date: LocalDate = LocalDate.now()
    ): Flow<List<MetricSummary>>

    /**
     * Get metric summary for a specific time range
     * @param userId The user ID
     * @param startTime Start of the time range
     * @param endTime End of the time range
     * @return List of summary items for each metric type
     */
    @Query("""
        SELECT 
            metricType,
            AVG(value) as avgValue,
            MIN(value) as minValue,
            MAX(value) as maxValue,
            COUNT(*) as count
        FROM health_data
        WHERE userId = :userId
        AND timestamp BETWEEN :startTime AND :endTime
        GROUP BY metricType
    """)
    suspend fun getMetricSummary(
        userId: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): List<MetricSummary>

    /**
     * Get health data count by device
     * @param userId The user ID
     * @return List of device IDs and their data count
     */
    @Query("""
        SELECT 
            deviceId,
            COUNT(*) as count
        FROM health_data
        WHERE userId = :userId
        GROUP BY deviceId
    """)
    suspend fun getDataCountByDevice(userId: String): List<DeviceDataCount>

    /**
     * Get health data by tags
     * @param userId The user ID
     * @param tags List of tags to filter by
     * @return List of health data entities with matching tags
     */
    @Query("""
        SELECT * FROM health_data
        WHERE userId = :userId
        AND tags LIKE '%' || :tagQuery || '%'
        ORDER BY timestamp DESC
    """)
    suspend fun getByTags(userId: String, tagQuery: String): List<HealthDataEntity>

    /**
     * Check if any data exists for a specific metric type
     * @param userId The user ID
     * @param metricType The metric type to check
     * @return True if data exists, false otherwise
     */
    @Query("SELECT EXISTS(SELECT 1 FROM health_data WHERE userId = :userId AND metricType = :metricType)")
    suspend fun hasDataForMetricType(userId: String, metricType: String): Boolean

    /**
     * Get count of records by metric type
     * @param userId The user ID
     * @return List of metric types and their record counts
     */
    @Query("""
        SELECT 
            metricType,
            COUNT(*) as count
        FROM health_data
        WHERE userId = :userId
        GROUP BY metricType
    """)
    suspend fun getCountByMetricTypes(userId: String): List<MetricTypeCount>

    /**
     * Get the first and last timestamp for each metric type
     * @param userId The user ID
     * @return List of metric types with their first and last timestamps
     */
    @Query("""
        SELECT 
            metricType,
            MIN(timestamp) as firstTimestamp,
            MAX(timestamp) as lastTimestamp
        FROM health_data
        WHERE userId = :userId
        GROUP BY metricType
    """)
    suspend fun getMetricDateRanges(userId: String): List<MetricDateRange>

    /**
     * Get data density by day of week
     * @param userId The user ID
     * @param metricType The metric type to analyze
     * @return List of day of week and record counts
     */
    @Query("""
        SELECT 
            strftime('%w', timestamp) as dayOfWeek,
            COUNT(*) as count
        FROM health_data
        WHERE userId = :userId
        AND metricType = :metricType
        GROUP BY dayOfWeek
        ORDER BY dayOfWeek
    """)
    suspend fun getDataDensityByDayOfWeek(
        userId: String,
        metricType: String
    ): List<DayOfWeekCount>

    /**
     * Get data density by hour of day
     * @param userId The user ID
     * @param metricType The metric type to analyze
     * @return List of hour of day and record counts
     */
    @Query("""
        SELECT 
            strftime('%H', timestamp) as hourOfDay,
            COUNT(*) as count
        FROM health_data
        WHERE userId = :userId
        AND metricType = :metricType
        GROUP BY hourOfDay
        ORDER BY hourOfDay
    """)
    suspend fun getDataDensityByHourOfDay(
        userId: String,
        metricType: String
    ): List<HourOfDayCount>

    /**
     * Data classes for query results
     */

    /**
     * Daily aggregate result
     */
    data class DailyAggregate(
        val date: LocalDate,
        val avgValue: Double,
        val minValue: Double,
        val maxValue: Double,
        val count: Int
    )

    /**
     * Hourly aggregate result
     */
    data class HourlyAggregate(
        val hour: String,
        val avgValue: Double,
        val minValue: Double,
        val maxValue: Double,
        val count: Int
    )

    /**
     * Weekly aggregate result
     */
    data class WeeklyAggregate(
        val week: String,
        val avgValue: Double,
        val minValue: Double,
        val maxValue: Double,
        val count: Int
    )

    /**
     * Monthly aggregate result
     */
    data class MonthlyAggregate(
        val month: String,
        val avgValue: Double,
        val minValue: Double,
        val maxValue: Double,
        val count: Int
    )

    /**
     * Trend point for trend analysis
     */
    data class TrendPoint(
        val date: LocalDate,
        val value: Double
    )

    /**
     * Metric summary for dashboard
     */
    data class MetricSummary(
        val metricType: String,
        val avgValue: Double,
        val minValue: Double,
        val maxValue: Double,
        val count: Int
    )

    /**
     * Device data count
     */
    data class DeviceDataCount(
        val deviceId: String,
        val count: Int
    )

    /**
     * Metric type count
     */
    data class MetricTypeCount(
        val metricType: String,
        val count: Int
    )

    /**
     * Metric date range
     */
    data class MetricDateRange(
        val metricType: String,
        val firstTimestamp: LocalDateTime,
        val lastTimestamp: LocalDateTime
    )

    /**
     * Day of week count
     */
    data class DayOfWeekCount(
        val dayOfWeek: String,
        val count: Int
    )

    /**
     * Hour of day count
     */
    data class HourOfDayCount(
        val hourOfDay: String,
        val count: Int
    )
}
