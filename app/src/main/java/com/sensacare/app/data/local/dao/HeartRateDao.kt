package com.sensacare.app.data.local.dao

import androidx.room.*
import com.sensacare.app.data.local.entity.HeartRateEntity
import com.sensacare.app.data.local.entity.MetricType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * HeartRateDao - Specialized Data Access Object for heart rate metrics
 *
 * This DAO extends the core functionality of HealthDataDao with heart rate specific
 * queries and analysis capabilities, including:
 * - Rest heart rate analysis
 * - Heart rate zones calculation
 * - Abnormal heart rate detection
 * - Heart rate variability metrics
 * - Exercise heart rate tracking
 * - Recovery heart rate analysis
 * - Age-adjusted heart rate thresholds
 * - Heart rate trends over time
 *
 * The HeartRateDao provides comprehensive access to heart rate data for both
 * real-time monitoring and long-term trend analysis.
 */
@Dao
interface HeartRateDao {

    /**
     * Basic CRUD Operations
     */

    /**
     * Insert a single heart rate record
     * @param heartRate The heart rate entity to insert
     * @return The row ID of the inserted item
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(heartRate: HeartRateEntity): Long

    /**
     * Insert multiple heart rate records in a single transaction
     * @param heartRates List of heart rate entities to insert
     * @return List of row IDs for the inserted items
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(heartRates: List<HeartRateEntity>): List<Long>

    /**
     * Update a heart rate record
     * @param heartRate The heart rate entity to update
     * @return Number of rows updated
     */
    @Update
    suspend fun update(heartRate: HeartRateEntity): Int

    /**
     * Delete a heart rate record
     * @param heartRate The heart rate entity to delete
     * @return Number of rows deleted
     */
    @Delete
    suspend fun delete(heartRate: HeartRateEntity): Int

    /**
     * Basic Queries
     */

    /**
     * Get heart rate by ID
     * @param id The ID of the heart rate to retrieve
     * @return The heart rate entity or null if not found
     */
    @Query("SELECT * FROM heart_rate WHERE id = :id")
    suspend fun getById(id: String): HeartRateEntity?

    /**
     * Get heart rate by ID as Flow for reactive updates
     * @param id The ID of the heart rate to retrieve
     * @return Flow emitting the heart rate entity
     */
    @Query("SELECT * FROM heart_rate WHERE id = :id")
    fun getByIdAsFlow(id: String): Flow<HeartRateEntity?>

    /**
     * Get all heart rate data for a specific user
     * @param userId The user ID
     * @return List of heart rate entities
     */
    @Query("SELECT * FROM heart_rate WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getAllForUser(userId: String): List<HeartRateEntity>

    /**
     * Get all heart rate data for a specific user as Flow for reactive updates
     * @param userId The user ID
     * @return Flow emitting list of heart rate entities
     */
    @Query("SELECT * FROM heart_rate WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllForUserAsFlow(userId: String): Flow<List<HeartRateEntity>>

    /**
     * Get heart rate data for a specific time range
     * @param userId The user ID
     * @param startTime Start of the time range
     * @param endTime End of the time range
     * @return List of heart rate entities
     */
    @Query("SELECT * FROM heart_rate WHERE userId = :userId AND timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    suspend fun getByTimeRange(
        userId: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): List<HeartRateEntity>

    /**
     * Get heart rate data for a specific time range as Flow for reactive updates
     * @param userId The user ID
     * @param startTime Start of the time range
     * @param endTime End of the time range
     * @return Flow emitting list of heart rate entities
     */
    @Query("SELECT * FROM heart_rate WHERE userId = :userId AND timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getByTimeRangeAsFlow(
        userId: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): Flow<List<HeartRateEntity>>

    /**
     * Get the latest heart rate record
     * @param userId The user ID
     * @return The latest heart rate entity or null if not found
     */
    @Query("SELECT * FROM heart_rate WHERE userId = :userId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(userId: String): HeartRateEntity?

    /**
     * Get the latest heart rate record as Flow for reactive updates
     * @param userId The user ID
     * @return Flow emitting the latest heart rate entity
     */
    @Query("SELECT * FROM heart_rate WHERE userId = :userId ORDER BY timestamp DESC LIMIT 1")
    fun getLatestAsFlow(userId: String): Flow<HeartRateEntity?>

    /**
     * Resting Heart Rate Analysis
     */

    /**
     * Get resting heart rate for a specific day
     * This uses the lowest heart rate during sleep or early morning hours
     * @param userId The user ID
     * @param date The date to analyze
     * @return The resting heart rate entity or null if not found
     */
    @Query("""
        SELECT * FROM heart_rate 
        WHERE userId = :userId 
        AND date(timestamp) = date(:date)
        AND (
            (strftime('%H', timestamp) BETWEEN '00' AND '05') OR
            (tags LIKE '%sleep%' OR tags LIKE '%rest%')
        )
        ORDER BY bpm ASC
        LIMIT 1
    """)
    suspend fun getRestingHeartRate(userId: String, date: LocalDate): HeartRateEntity?

    /**
     * Get resting heart rate for a specific day as Flow for reactive updates
     * @param userId The user ID
     * @param date The date to analyze
     * @return Flow emitting the resting heart rate entity
     */
    @Query("""
        SELECT * FROM heart_rate 
        WHERE userId = :userId 
        AND date(timestamp) = date(:date)
        AND (
            (strftime('%H', timestamp) BETWEEN '00' AND '05') OR
            (tags LIKE '%sleep%' OR tags LIKE '%rest%')
        )
        ORDER BY bpm ASC
        LIMIT 1
    """)
    fun getRestingHeartRateAsFlow(userId: String, date: LocalDate): Flow<HeartRateEntity?>

    /**
     * Get average resting heart rate over a time period
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return Average resting heart rate or null if not found
     */
    @Query("""
        WITH daily_resting AS (
            SELECT 
                date(timestamp) as date,
                MIN(bpm) as resting_hr
            FROM heart_rate
            WHERE userId = :userId
            AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
            AND (
                (strftime('%H', timestamp) BETWEEN '00' AND '05') OR
                (tags LIKE '%sleep%' OR tags LIKE '%rest%')
            )
            GROUP BY date(timestamp)
        )
        SELECT AVG(resting_hr) FROM daily_resting
    """)
    suspend fun getAverageRestingHeartRate(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Double?

    /**
     * Get resting heart rate trend over time
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return List of daily resting heart rates
     */
    @Query("""
        WITH daily_resting AS (
            SELECT 
                date(timestamp) as date,
                MIN(bpm) as resting_hr
            FROM heart_rate
            WHERE userId = :userId
            AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
            AND (
                (strftime('%H', timestamp) BETWEEN '00' AND '05') OR
                (tags LIKE '%sleep%' OR tags LIKE '%rest%')
            )
            GROUP BY date(timestamp)
        )
        SELECT date, resting_hr as bpm
        FROM daily_resting
        ORDER BY date
    """)
    suspend fun getRestingHeartRateTrend(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<DailyHeartRate>

    /**
     * Get resting heart rate trend over time as Flow for reactive updates
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return Flow emitting list of daily resting heart rates
     */
    @Query("""
        WITH daily_resting AS (
            SELECT 
                date(timestamp) as date,
                MIN(bpm) as resting_hr
            FROM heart_rate
            WHERE userId = :userId
            AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
            AND (
                (strftime('%H', timestamp) BETWEEN '00' AND '05') OR
                (tags LIKE '%sleep%' OR tags LIKE '%rest%')
            )
            GROUP BY date(timestamp)
        )
        SELECT date, resting_hr as bpm
        FROM daily_resting
        ORDER BY date
    """)
    fun getRestingHeartRateTrendAsFlow(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<DailyHeartRate>>

    /**
     * Heart Rate Zones Analysis
     */

    /**
     * Get heart rate zone distribution for a specific time range
     * Zones are calculated based on max heart rate (typically 220 - age)
     * Zone 1: 50-60% of max HR (Very Light)
     * Zone 2: 60-70% of max HR (Light)
     * Zone 3: 70-80% of max HR (Moderate)
     * Zone 4: 80-90% of max HR (Hard)
     * Zone 5: 90-100% of max HR (Maximum)
     * 
     * @param userId The user ID
     * @param maxHeartRate The maximum heart rate (typically 220 - age)
     * @param startTime Start of the time range
     * @param endTime End of the time range
     * @return Distribution of time spent in each heart rate zone
     */
    @Query("""
        SELECT
            CASE
                WHEN bpm < (:maxHeartRate * 0.5) THEN 'Below Zone 1'
                WHEN bpm BETWEEN (:maxHeartRate * 0.5) AND (:maxHeartRate * 0.6) THEN 'Zone 1'
                WHEN bpm BETWEEN (:maxHeartRate * 0.6) AND (:maxHeartRate * 0.7) THEN 'Zone 2'
                WHEN bpm BETWEEN (:maxHeartRate * 0.7) AND (:maxHeartRate * 0.8) THEN 'Zone 3'
                WHEN bpm BETWEEN (:maxHeartRate * 0.8) AND (:maxHeartRate * 0.9) THEN 'Zone 4'
                WHEN bpm >= (:maxHeartRate * 0.9) THEN 'Zone 5'
            END as zone,
            COUNT(*) as count,
            MIN(bpm) as minBpm,
            MAX(bpm) as maxBpm,
            AVG(bpm) as avgBpm
        FROM heart_rate
        WHERE userId = :userId
        AND timestamp BETWEEN :startTime AND :endTime
        GROUP BY zone
        ORDER BY minBpm
    """)
    suspend fun getHeartRateZoneDistribution(
        userId: String,
        maxHeartRate: Int,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): List<HeartRateZone>

    /**
     * Get heart rate zone distribution for a specific time range as Flow for reactive updates
     * @param userId The user ID
     * @param maxHeartRate The maximum heart rate (typically 220 - age)
     * @param startTime Start of the time range
     * @param endTime End of the time range
     * @return Flow emitting distribution of time spent in each heart rate zone
     */
    @Query("""
        SELECT
            CASE
                WHEN bpm < (:maxHeartRate * 0.5) THEN 'Below Zone 1'
                WHEN bpm BETWEEN (:maxHeartRate * 0.5) AND (:maxHeartRate * 0.6) THEN 'Zone 1'
                WHEN bpm BETWEEN (:maxHeartRate * 0.6) AND (:maxHeartRate * 0.7) THEN 'Zone 2'
                WHEN bpm BETWEEN (:maxHeartRate * 0.7) AND (:maxHeartRate * 0.8) THEN 'Zone 3'
                WHEN bpm BETWEEN (:maxHeartRate * 0.8) AND (:maxHeartRate * 0.9) THEN 'Zone 4'
                WHEN bpm >= (:maxHeartRate * 0.9) THEN 'Zone 5'
            END as zone,
            COUNT(*) as count,
            MIN(bpm) as minBpm,
            MAX(bpm) as maxBpm,
            AVG(bpm) as avgBpm
        FROM heart_rate
        WHERE userId = :userId
        AND timestamp BETWEEN :startTime AND :endTime
        GROUP BY zone
        ORDER BY minBpm
    """)
    fun getHeartRateZoneDistributionAsFlow(
        userId: String,
        maxHeartRate: Int,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): Flow<List<HeartRateZone>>

    /**
     * Calculate time spent in each heart rate zone during a workout
     * @param userId The user ID
     * @param maxHeartRate The maximum heart rate (typically 220 - age)
     * @param workoutId The workout ID
     * @return Distribution of time spent in each heart rate zone
     */
    @Query("""
        SELECT
            CASE
                WHEN bpm < (:maxHeartRate * 0.5) THEN 'Below Zone 1'
                WHEN bpm BETWEEN (:maxHeartRate * 0.5) AND (:maxHeartRate * 0.6) THEN 'Zone 1'
                WHEN bpm BETWEEN (:maxHeartRate * 0.6) AND (:maxHeartRate * 0.7) THEN 'Zone 2'
                WHEN bpm BETWEEN (:maxHeartRate * 0.7) AND (:maxHeartRate * 0.8) THEN 'Zone 3'
                WHEN bpm BETWEEN (:maxHeartRate * 0.8) AND (:maxHeartRate * 0.9) THEN 'Zone 4'
                WHEN bpm >= (:maxHeartRate * 0.9) THEN 'Zone 5'
            END as zone,
            COUNT(*) as count,
            MIN(bpm) as minBpm,
            MAX(bpm) as maxBpm,
            AVG(bpm) as avgBpm
        FROM heart_rate
        WHERE userId = :userId
        AND workoutId = :workoutId
        GROUP BY zone
        ORDER BY minBpm
    """)
    suspend fun getWorkoutHeartRateZones(
        userId: String,
        maxHeartRate: Int,
        workoutId: String
    ): List<HeartRateZone>

    /**
     * Abnormal Heart Rate Detection
     */

    /**
     * Get abnormally high heart rate events
     * Detects heart rates that are significantly above resting heart rate
     * when not exercising or active
     * 
     * @param userId The user ID
     * @param threshold The BPM threshold above resting to consider abnormal
     * @param startDate Start date
     * @param endDate End date
     * @return List of abnormal heart rate events
     */
    @Query("""
        WITH resting_hr AS (
            SELECT 
                userId,
                AVG(bpm) as avg_resting
            FROM heart_rate
            WHERE userId = :userId
            AND (
                (strftime('%H', timestamp) BETWEEN '00' AND '05') OR
                (tags LIKE '%sleep%' OR tags LIKE '%rest%')
            )
            AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
            GROUP BY userId
        )
        SELECT hr.*
        FROM heart_rate hr
        JOIN resting_hr rhr ON hr.userId = rhr.userId
        WHERE hr.userId = :userId
        AND hr.bpm > (rhr.avg_resting + :threshold)
        AND hr.tags NOT LIKE '%exercise%'
        AND hr.tags NOT LIKE '%workout%'
        AND hr.tags NOT LIKE '%activity%'
        AND date(hr.timestamp) BETWEEN date(:startDate) AND date(:endDate)
        ORDER BY hr.timestamp DESC
    """)
    suspend fun getAbnormallyHighHeartRates(
        userId: String,
        threshold: Int = 30,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<HeartRateEntity>

    /**
     * Get abnormally low heart rate events
     * Detects heart rates that are significantly below normal resting heart rate
     * 
     * @param userId The user ID
     * @param threshold The BPM threshold below resting to consider abnormal
     * @param startDate Start date
     * @param endDate End date
     * @return List of abnormal heart rate events
     */
    @Query("""
        WITH resting_hr AS (
            SELECT 
                userId,
                AVG(bpm) as avg_resting
            FROM heart_rate
            WHERE userId = :userId
            AND (
                (strftime('%H', timestamp) BETWEEN '00' AND '05') OR
                (tags LIKE '%sleep%' OR tags LIKE '%rest%')
            )
            AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
            GROUP BY userId
        )
        SELECT hr.*
        FROM heart_rate hr
        JOIN resting_hr rhr ON hr.userId = rhr.userId
        WHERE hr.userId = :userId
        AND hr.bpm < (rhr.avg_resting - :threshold)
        AND date(hr.timestamp) BETWEEN date(:startDate) AND date(:endDate)
        ORDER BY hr.timestamp DESC
    """)
    suspend fun getAbnormallyLowHeartRates(
        userId: String,
        threshold: Int = 15,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<HeartRateEntity>

    /**
     * Detect potential arrhythmia events based on heart rate variability
     * Identifies periods with unusually high variability in short time spans
     * 
     * @param userId The user ID
     * @param variabilityThreshold The threshold for heart rate change to consider abnormal
     * @param timeWindowMinutes The time window in minutes to check for variability
     * @param startDate Start date
     * @param endDate End date
     * @return List of potential arrhythmia events
     */
    @Query("""
        WITH hr_with_prev AS (
            SELECT 
                hr1.*,
                (
                    SELECT hr2.bpm
                    FROM heart_rate hr2
                    WHERE hr2.userId = hr1.userId
                    AND hr2.timestamp < hr1.timestamp
                    ORDER BY hr2.timestamp DESC
                    LIMIT 1
                ) as prev_bpm
            FROM heart_rate hr1
            WHERE hr1.userId = :userId
            AND date(hr1.timestamp) BETWEEN date(:startDate) AND date(:endDate)
        )
        SELECT *
        FROM hr_with_prev
        WHERE ABS(bpm - prev_bpm) > :variabilityThreshold
        AND (
            julianday(timestamp) - julianday(
                (
                    SELECT hr3.timestamp
                    FROM heart_rate hr3
                    WHERE hr3.userId = hr_with_prev.userId
                    AND hr3.timestamp < hr_with_prev.timestamp
                    ORDER BY hr3.timestamp DESC
                    LIMIT 1
                )
            )
        ) * 24 * 60 < :timeWindowMinutes
        ORDER BY timestamp DESC
    """)
    suspend fun detectPotentialArrhythmia(
        userId: String,
        variabilityThreshold: Int = 20,
        timeWindowMinutes: Int = 5,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<HeartRateEntity>

    /**
     * Heart Rate Variability Metrics
     */

    /**
     * Calculate RMSSD (Root Mean Square of Successive Differences)
     * A common HRV metric that measures the square root of the mean of the squared differences 
     * between successive R-R intervals
     * 
     * @param userId The user ID
     * @param startTime Start of the time range
     * @param endTime End of the time range
     * @return RMSSD value or null if not enough data
     */
    @Query("""
        WITH rr_intervals AS (
            SELECT 
                timestamp,
                (60000.0 / bpm) as rr_ms, -- Convert BPM to RR interval in ms
                (
                    SELECT (60000.0 / hr2.bpm)
                    FROM heart_rate hr2
                    WHERE hr2.userId = hr1.userId
                    AND hr2.timestamp < hr1.timestamp
                    ORDER BY hr2.timestamp DESC
                    LIMIT 1
                ) as prev_rr_ms
            FROM heart_rate hr1
            WHERE hr1.userId = :userId
            AND hr1.timestamp BETWEEN :startTime AND :endTime
        ),
        rr_diffs AS (
            SELECT 
                (rr_ms - prev_rr_ms) as diff
            FROM rr_intervals
            WHERE prev_rr_ms IS NOT NULL
        )
        SELECT 
            SQRT(AVG(diff * diff)) as rmssd
        FROM rr_diffs
    """)
    suspend fun calculateRMSSD(
        userId: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): Double?

    /**
     * Calculate SDNN (Standard Deviation of NN intervals)
     * Represents the standard deviation of the time between heartbeats
     * 
     * @param userId The user ID
     * @param startTime Start of the time range
     * @param endTime End of the time range
     * @return SDNN value or null if not enough data
     */
    @Query("""
        WITH rr_intervals AS (
            SELECT 
                (60000.0 / bpm) as rr_ms -- Convert BPM to RR interval in ms
            FROM heart_rate
            WHERE userId = :userId
            AND timestamp BETWEEN :startTime AND :endTime
        )
        SELECT 
            (
                SELECT 
                    SQRT(
                        SUM((rr_ms - (SELECT AVG(rr_ms) FROM rr_intervals)) * (rr_ms - (SELECT AVG(rr_ms) FROM rr_intervals))) / 
                        (COUNT(*) - 1)
                    )
                FROM rr_intervals
                WHERE COUNT(*) > 1
            ) as sdnn
        FROM rr_intervals
        LIMIT 1
    """)
    suspend fun calculateSDNN(
        userId: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): Double?

    /**
     * Calculate daily HRV metrics
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return List of daily HRV metrics
     */
    @Query("""
        WITH daily_data AS (
            SELECT 
                date(timestamp) as date,
                (60000.0 / bpm) as rr_ms
            FROM heart_rate
            WHERE userId = :userId
            AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
        ),
        daily_stats AS (
            SELECT 
                date,
                AVG(rr_ms) as mean_rr,
                (
                    SELECT 
                        SQRT(
                            SUM((rr_ms - (SELECT AVG(rr_ms) FROM daily_data d2 WHERE d2.date = d1.date)) * 
                                (rr_ms - (SELECT AVG(rr_ms) FROM daily_data d2 WHERE d2.date = d1.date))) / 
                            (COUNT(*) - 1)
                        )
                    FROM daily_data d3
                    WHERE d3.date = d1.date
                    AND COUNT(*) > 1
                ) as sdnn
            FROM daily_data d1
            GROUP BY date
        )
        SELECT 
            date,
            mean_rr,
            sdnn
        FROM daily_stats
        ORDER BY date
    """)
    suspend fun getDailyHRVMetrics(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<DailyHRVMetrics>

    /**
     * Exercise Heart Rate Tracking
     */

    /**
     * Get heart rate data for a specific workout
     * @param userId The user ID
     * @param workoutId The workout ID
     * @return List of heart rate entities during the workout
     */
    @Query("SELECT * FROM heart_rate WHERE userId = :userId AND workoutId = :workoutId ORDER BY timestamp")
    suspend fun getWorkoutHeartRates(userId: String, workoutId: String): List<HeartRateEntity>

    /**
     * Get workout heart rate summary
     * @param userId The user ID
     * @param workoutId The workout ID
     * @return Summary of heart rate during workout
     */
    @Query("""
        SELECT 
            MIN(bpm) as minBpm,
            MAX(bpm) as maxBpm,
            AVG(bpm) as avgBpm,
            (
                SELECT bpm
                FROM heart_rate
                WHERE userId = :userId
                AND workoutId = :workoutId
                ORDER BY bpm DESC
                LIMIT 1 OFFSET (SELECT COUNT(*) * 8 / 10 FROM heart_rate WHERE userId = :userId AND workoutId = :workoutId)
            ) as anaerobicThresholdEstimate
        FROM heart_rate
        WHERE userId = :userId
        AND workoutId = :workoutId
    """)
    suspend fun getWorkoutHeartRateSummary(
        userId: String,
        workoutId: String
    ): WorkoutHeartRateSummary?

    /**
     * Get maximum heart rate achieved during workouts
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return Maximum heart rate achieved or null if no workout data
     */
    @Query("""
        SELECT MAX(bpm)
        FROM heart_rate
        WHERE userId = :userId
        AND (workoutId IS NOT NULL OR tags LIKE '%exercise%' OR tags LIKE '%workout%')
        AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
    """)
    suspend fun getMaximumExerciseHeartRate(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Int?

    /**
     * Recovery Heart Rate Analysis
     */

    /**
     * Calculate heart rate recovery after exercise
     * Measures how quickly heart rate drops after exercise stops
     * 
     * @param userId The user ID
     * @param workoutId The workout ID
     * @param recoveryMinutes Number of minutes after workout to measure recovery
     * @return Heart rate recovery metrics or null if not enough data
     */
    @Query("""
        WITH workout_end AS (
            SELECT 
                MAX(timestamp) as end_time,
                MAX(bpm) as max_workout_bpm
            FROM heart_rate
            WHERE userId = :userId
            AND workoutId = :workoutId
        ),
        recovery_hr AS (
            SELECT 
                hr.bpm,
                (julianday(hr.timestamp) - julianday(we.end_time)) * 24 * 60 as minutes_after
            FROM heart_rate hr, workout_end we
            WHERE hr.userId = :userId
            AND hr.timestamp > we.end_time
            AND (julianday(hr.timestamp) - julianday(we.end_time)) * 24 * 60 <= :recoveryMinutes
            ORDER BY hr.timestamp
        )
        SELECT 
            we.max_workout_bpm as peakBpm,
            (
                SELECT bpm
                FROM recovery_hr
                WHERE minutes_after <= 1
                ORDER BY minutes_after DESC
                LIMIT 1
            ) as bpmAfter1Min,
            (
                SELECT bpm
                FROM recovery_hr
                WHERE minutes_after <= 2
                ORDER BY minutes_after DESC
                LIMIT 1
            ) as bpmAfter2Min,
            (
                SELECT bpm
                FROM recovery_hr
                WHERE minutes_after <= 5
                ORDER BY minutes_after DESC
                LIMIT 1
            ) as bpmAfter5Min,
            we.max_workout_bpm - (
                SELECT bpm
                FROM recovery_hr
                WHERE minutes_after <= 1
                ORDER BY minutes_after DESC
                LIMIT 1
            ) as recovery1Min,
            we.max_workout_bpm - (
                SELECT bpm
                FROM recovery_hr
                WHERE minutes_after <= 2
                ORDER BY minutes_after DESC
                LIMIT 1
            ) as recovery2Min,
            we.max_workout_bpm - (
                SELECT bpm
                FROM recovery_hr
                WHERE minutes_after <= 5
                ORDER BY minutes_after DESC
                LIMIT 1
            ) as recovery5Min
        FROM workout_end we
    """)
    suspend fun getHeartRateRecovery(
        userId: String,
        workoutId: String,
        recoveryMinutes: Int = 5
    ): HeartRateRecovery?

    /**
     * Track heart rate recovery improvement over time
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return List of heart rate recovery metrics for each workout
     */
    @Query("""
        WITH workout_list AS (
            SELECT DISTINCT workoutId
            FROM heart_rate
            WHERE userId = :userId
            AND workoutId IS NOT NULL
            AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
        ),
        workout_data AS (
            SELECT 
                wl.workoutId,
                MAX(hr.timestamp) as end_time,
                MAX(hr.bpm) as max_workout_bpm,
                (
                    SELECT MIN(hr2.bpm)
                    FROM heart_rate hr2
                    WHERE hr2.userId = :userId
                    AND hr2.workoutId = wl.workoutId
                    AND hr2.timestamp > (
                        SELECT MAX(hr3.timestamp)
                        FROM heart_rate hr3
                        WHERE hr3.userId = :userId
                        AND hr3.workoutId = wl.workoutId
                    )
                    AND (julianday(hr2.timestamp) - julianday(MAX(hr.timestamp))) * 24 * 60 <= 1
                ) as bpm_after_1min
            FROM workout_list wl
            JOIN heart_rate hr ON hr.workoutId = wl.workoutId AND hr.userId = :userId
            GROUP BY wl.workoutId
        )
        SELECT 
            workoutId,
            max_workout_bpm as peakBpm,
            bpm_after_1min as bpmAfter1Min,
            (max_workout_bpm - bpm_after_1min) as recovery1Min,
            date(end_time) as workoutDate
        FROM workout_data
        WHERE bpm_after_1min IS NOT NULL
        ORDER BY end_time
    """)
    suspend fun getHeartRateRecoveryTrend(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<HeartRateRecoveryTrend>

    /**
     * Age-adjusted Heart Rate Thresholds
     */

    /**
     * Calculate age-adjusted heart rate zones
     * @param userId The user ID
     * @param age User's age
     * @param restingHeartRate User's resting heart rate
     * @return Heart rate zones adjusted for age and resting heart rate
     */
    @Query("""
        WITH max_hr AS (
            SELECT (220 - :age) as mhr
        ),
        heart_reserve AS (
            SELECT (mhr - :restingHeartRate) as hr_reserve
            FROM max_hr
        )
        SELECT 
            :restingHeartRate as restingHeartRate,
            (SELECT mhr FROM max_hr) as maxHeartRate,
            ROUND(:restingHeartRate + (SELECT hr_reserve FROM heart_reserve) * 0.5) as zone1Min,
            ROUND(:restingHeartRate + (SELECT hr_reserve FROM heart_reserve) * 0.6) as zone1Max,
            ROUND(:restingHeartRate + (SELECT hr_reserve FROM heart_reserve) * 0.6) as zone2Min,
            ROUND(:restingHeartRate + (SELECT hr_reserve FROM heart_reserve) * 0.7) as zone2Max,
            ROUND(:restingHeartRate + (SELECT hr_reserve FROM heart_reserve) * 0.7) as zone3Min,
            ROUND(:restingHeartRate + (SELECT hr_reserve FROM heart_reserve) * 0.8) as zone3Max,
            ROUND(:restingHeartRate + (SELECT hr_reserve FROM heart_reserve) * 0.8) as zone4Min,
            ROUND(:restingHeartRate + (SELECT hr_reserve FROM heart_reserve) * 0.9) as zone4Max,
            ROUND(:restingHeartRate + (SELECT hr_reserve FROM heart_reserve) * 0.9) as zone5Min,
            (SELECT mhr FROM max_hr) as zone5Max
    """)
    suspend fun getAgeAdjustedHeartRateZones(
        userId: String,
        age: Int,
        restingHeartRate: Int
    ): AgeAdjustedHeartRateZones

    /**
     * Check if user exceeds age-adjusted maximum heart rate
     * @param userId The user ID
     * @param age User's age
     * @param startDate Start date
     * @param endDate End date
     * @return List of heart rate records exceeding age-adjusted maximum
     */
    @Query("""
        SELECT *
        FROM heart_rate
        WHERE userId = :userId
        AND bpm > (220 - :age)
        AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
        ORDER BY timestamp DESC
    """)
    suspend fun getHeartRatesExceedingAgeMax(
        userId: String,
        age: Int,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<HeartRateEntity>

    /**
     * Heart Rate Trends Over Time
     */

    /**
     * Get daily heart rate summary
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return List of daily heart rate summaries
     */
    @Query("""
        SELECT 
            date(timestamp) as date,
            MIN(bpm) as minBpm,
            MAX(bpm) as maxBpm,
            AVG(bpm) as avgBpm,
            COUNT(*) as measurementCount
        FROM heart_rate
        WHERE userId = :userId
        AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
        GROUP BY date(timestamp)
        ORDER BY date(timestamp)
    """)
    suspend fun getDailyHeartRateSummary(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<DailyHeartRateSummary>

    /**
     * Get daily heart rate summary as Flow for reactive updates
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return Flow emitting list of daily heart rate summaries
     */
    @Query("""
        SELECT 
            date(timestamp) as date,
            MIN(bpm) as minBpm,
            MAX(bpm) as maxBpm,
            AVG(bpm) as avgBpm,
            COUNT(*) as measurementCount
        FROM heart_rate
        WHERE userId = :userId
        AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
        GROUP BY date(timestamp)
        ORDER BY date(timestamp)
    """)
    fun getDailyHeartRateSummaryAsFlow(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<DailyHeartRateSummary>>

    /**
     * Get heart rate variability over time
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return List of daily heart rate variability metrics
     */
    @Query("""
        SELECT 
            date(timestamp) as date,
            SQRT(
                AVG(
                    (bpm - (
                        SELECT AVG(bpm) 
                        FROM heart_rate hr2 
                        WHERE hr2.userId = hr1.userId 
                        AND date(hr2.timestamp) = date(hr1.timestamp)
                    )) * 
                    (bpm - (
                        SELECT AVG(bpm) 
                        FROM heart_rate hr2 
                        WHERE hr2.userId = hr1.userId 
                        AND date(hr2.timestamp) = date(hr1.timestamp)
                    ))
                )
            ) as stdDev
        FROM heart_rate hr1
        WHERE userId = :userId
        AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
        GROUP BY date(timestamp)
        ORDER BY date(timestamp)
    """)
    suspend fun getHeartRateVariabilityTrend(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<DailyHeartRateVariability>

    /**
     * Get hourly heart rate pattern
     * @param userId The user ID
     * @param date The date to analyze
     * @return List of hourly heart rate averages
     */
    @Query("""
        SELECT 
            strftime('%H', timestamp) as hour,
            AVG(bpm) as avgBpm,
            MIN(bpm) as minBpm,
            MAX(bpm) as maxBpm,
            COUNT(*) as count
        FROM heart_rate
        WHERE userId = :userId
        AND date(timestamp) = date(:date)
        GROUP BY strftime('%H', timestamp)
        ORDER BY hour
    """)
    suspend fun getHourlyHeartRatePattern(
        userId: String,
        date: LocalDate
    ): List<HourlyHeartRate>

    /**
     * Get weekly heart rate pattern
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return List of day-of-week heart rate averages
     */
    @Query("""
        SELECT 
            strftime('%w', timestamp) as dayOfWeek,
            AVG(bpm) as avgBpm,
            MIN(bpm) as minBpm,
            MAX(bpm) as maxBpm,
            COUNT(*) as count
        FROM heart_rate
        WHERE userId = :userId
        AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
        GROUP BY strftime('%w', timestamp)
        ORDER BY dayOfWeek
    """)
    suspend fun getWeeklyHeartRatePattern(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<DayOfWeekHeartRate>

    /**
     * Get heart rate trend by activity type
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return List of activity types with heart rate metrics
     */
    @Query("""
        SELECT 
            CASE
                WHEN tags LIKE '%sleep%' THEN 'Sleep'
                WHEN tags LIKE '%rest%' THEN 'Rest'
                WHEN tags LIKE '%exercise%' OR tags LIKE '%workout%' THEN 'Exercise'
                WHEN tags LIKE '%walking%' THEN 'Walking'
                WHEN tags LIKE '%sitting%' THEN 'Sitting'
                ELSE 'Other'
            END as activityType,
            AVG(bpm) as avgBpm,
            MIN(bpm) as minBpm,
            MAX(bpm) as maxBpm,
            COUNT(*) as count
        FROM heart_rate
        WHERE userId = :userId
        AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
        GROUP BY activityType
        ORDER BY avgBpm
    """)
    suspend fun getHeartRateByActivityType(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<ActivityHeartRate>

    /**
     * Data classes for query results
     */

    /**
     * Daily heart rate result
     */
    data class DailyHeartRate(
        val date: LocalDate,
        val bpm: Int
    )

    /**
     * Heart rate zone distribution
     */
    data class HeartRateZone(
        val zone: String,
        val count: Int,
        val minBpm: Int,
        val maxBpm: Int,
        val avgBpm: Double
    )

    /**
     * Daily HRV metrics
     */
    data class DailyHRVMetrics(
        val date: LocalDate,
        val mean_rr: Double,  // Mean R-R interval in ms
        val sdnn: Double      // Standard deviation of NN intervals
    )

    /**
     * Workout heart rate summary
     */
    data class WorkoutHeartRateSummary(
        val minBpm: Int,
        val maxBpm: Int,
        val avgBpm: Double,
        val anaerobicThresholdEstimate: Int
    )

    /**
     * Heart rate recovery metrics
     */
    data class HeartRateRecovery(
        val peakBpm: Int,
        val bpmAfter1Min: Int?,
        val bpmAfter2Min: Int?,
        val bpmAfter5Min: Int?,
        val recovery1Min: Int?,
        val recovery2Min: Int?,
        val recovery5Min: Int?
    )

    /**
     * Heart rate recovery trend
     */
    data class HeartRateRecoveryTrend(
        val workoutId: String,
        val peakBpm: Int,
        val bpmAfter1Min: Int,
        val recovery1Min: Int,
        val workoutDate: LocalDate
    )

    /**
     * Age-adjusted heart rate zones
     */
    data class AgeAdjustedHeartRateZones(
        val restingHeartRate: Int,
        val maxHeartRate: Int,
        val zone1Min: Int,
        val zone1Max: Int,
        val zone2Min: Int,
        val zone2Max: Int,
        val zone3Min: Int,
        val zone3Max: Int,
        val zone4Min: Int,
        val zone4Max: Int,
        val zone5Min: Int,
        val zone5Max: Int
    )

    /**
     * Daily heart rate summary
     */
    data class DailyHeartRateSummary(
        val date: LocalDate,
        val minBpm: Int,
        val maxBpm: Int,
        val avgBpm: Double,
        val measurementCount: Int
    )

    /**
     * Daily heart rate variability
     */
    data class DailyHeartRateVariability(
        val date: LocalDate,
        val stdDev: Double
    )

    /**
     * Hourly heart rate
     */
    data class HourlyHeartRate(
        val hour: String,
        val avgBpm: Double,
        val minBpm: Int,
        val maxBpm: Int,
        val count: Int
    )

    /**
     * Day of week heart rate
     */
    data class DayOfWeekHeartRate(
        val dayOfWeek: String,
        val avgBpm: Double,
        val minBpm: Int,
        val maxBpm: Int,
        val count: Int
    )

    /**
     * Activity type heart rate
     */
    data class ActivityHeartRate(
        val activityType: String,
        val avgBpm: Double,
        val minBpm: Int,
        val maxBpm: Int,
        val count: Int
    )
}
