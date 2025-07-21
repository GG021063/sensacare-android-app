package com.sensacare.app.data.local.dao

import androidx.room.*
import com.sensacare.app.data.local.entity.SleepEntity
import com.sensacare.app.data.local.entity.SleepStageEntity
import kotlinx.coroutines.flow.Flow
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/**
 * SleepDao - Specialized Data Access Object for sleep metrics
 *
 * This DAO extends the core functionality with sleep-specific
 * queries and analysis capabilities, including:
 * - Sleep stage analysis (light, deep, REM, awake)
 * - Sleep efficiency calculations
 * - Sleep pattern detection
 * - Sleep debt tracking
 * - Circadian rhythm analysis
 * - Sleep quality scoring
 * - Bedtime and wake time patterns
 * - Sleep consistency metrics
 *
 * The SleepDao provides comprehensive access to sleep data for both
 * real-time monitoring and long-term trend analysis.
 */
@Dao
interface SleepDao {

    /**
     * Basic CRUD Operations
     */

    /**
     * Insert a single sleep record
     * @param sleep The sleep entity to insert
     * @return The row ID of the inserted item
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sleep: SleepEntity): Long

    /**
     * Insert multiple sleep records in a single transaction
     * @param sleepList List of sleep entities to insert
     * @return List of row IDs for the inserted items
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sleepList: List<SleepEntity>): List<Long>

    /**
     * Insert a single sleep stage record
     * @param sleepStage The sleep stage entity to insert
     * @return The row ID of the inserted item
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSleepStage(sleepStage: SleepStageEntity): Long

    /**
     * Insert multiple sleep stage records in a single transaction
     * @param sleepStageList List of sleep stage entities to insert
     * @return List of row IDs for the inserted items
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSleepStages(sleepStageList: List<SleepStageEntity>): List<Long>

    /**
     * Update a sleep record
     * @param sleep The sleep entity to update
     * @return Number of rows updated
     */
    @Update
    suspend fun update(sleep: SleepEntity): Int

    /**
     * Update a sleep stage record
     * @param sleepStage The sleep stage entity to update
     * @return Number of rows updated
     */
    @Update
    suspend fun updateSleepStage(sleepStage: SleepStageEntity): Int

    /**
     * Delete a sleep record
     * @param sleep The sleep entity to delete
     * @return Number of rows deleted
     */
    @Delete
    suspend fun delete(sleep: SleepEntity): Int

    /**
     * Delete a sleep stage record
     * @param sleepStage The sleep stage entity to delete
     * @return Number of rows deleted
     */
    @Delete
    suspend fun deleteSleepStage(sleepStage: SleepStageEntity): Int

    /**
     * Delete all sleep stages for a specific sleep session
     * @param sleepId The sleep session ID
     * @return Number of rows deleted
     */
    @Query("DELETE FROM sleep_stages WHERE sleepId = :sleepId")
    suspend fun deleteSleepStagesBySleepId(sleepId: String): Int

    /**
     * Basic Queries
     */

    /**
     * Get sleep record by ID
     * @param id The ID of the sleep record to retrieve
     * @return The sleep entity or null if not found
     */
    @Query("SELECT * FROM sleep WHERE id = :id")
    suspend fun getById(id: String): SleepEntity?

    /**
     * Get sleep record by ID as Flow for reactive updates
     * @param id The ID of the sleep record to retrieve
     * @return Flow emitting the sleep entity
     */
    @Query("SELECT * FROM sleep WHERE id = :id")
    fun getByIdAsFlow(id: String): Flow<SleepEntity?>

    /**
     * Get sleep stages for a specific sleep session
     * @param sleepId The sleep session ID
     * @return List of sleep stage entities
     */
    @Query("SELECT * FROM sleep_stages WHERE sleepId = :sleepId ORDER BY startTime")
    suspend fun getSleepStagesBySleepId(sleepId: String): List<SleepStageEntity>

    /**
     * Get sleep stages for a specific sleep session as Flow for reactive updates
     * @param sleepId The sleep session ID
     * @return Flow emitting list of sleep stage entities
     */
    @Query("SELECT * FROM sleep_stages WHERE sleepId = :sleepId ORDER BY startTime")
    fun getSleepStagesBySleepIdAsFlow(sleepId: String): Flow<List<SleepStageEntity>>

    /**
     * Get all sleep records for a specific user
     * @param userId The user ID
     * @return List of sleep entities
     */
    @Query("SELECT * FROM sleep WHERE userId = :userId ORDER BY startTime DESC")
    suspend fun getAllForUser(userId: String): List<SleepEntity>

    /**
     * Get all sleep records for a specific user as Flow for reactive updates
     * @param userId The user ID
     * @return Flow emitting list of sleep entities
     */
    @Query("SELECT * FROM sleep WHERE userId = :userId ORDER BY startTime DESC")
    fun getAllForUserAsFlow(userId: String): Flow<List<SleepEntity>>

    /**
     * Get sleep records for a specific time range
     * @param userId The user ID
     * @param startTime Start of the time range
     * @param endTime End of the time range
     * @return List of sleep entities
     */
    @Query("""
        SELECT * FROM sleep 
        WHERE userId = :userId 
        AND ((startTime BETWEEN :startTime AND :endTime) 
            OR (endTime BETWEEN :startTime AND :endTime)
            OR (startTime <= :startTime AND endTime >= :endTime))
        ORDER BY startTime DESC
    """)
    suspend fun getByTimeRange(
        userId: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): List<SleepEntity>

    /**
     * Get sleep records for a specific time range as Flow for reactive updates
     * @param userId The user ID
     * @param startTime Start of the time range
     * @param endTime End of the time range
     * @return Flow emitting list of sleep entities
     */
    @Query("""
        SELECT * FROM sleep 
        WHERE userId = :userId 
        AND ((startTime BETWEEN :startTime AND :endTime) 
            OR (endTime BETWEEN :startTime AND :endTime)
            OR (startTime <= :startTime AND endTime >= :endTime))
        ORDER BY startTime DESC
    """)
    fun getByTimeRangeAsFlow(
        userId: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): Flow<List<SleepEntity>>

    /**
     * Get the latest sleep record
     * @param userId The user ID
     * @return The latest sleep entity or null if not found
     */
    @Query("SELECT * FROM sleep WHERE userId = :userId ORDER BY endTime DESC LIMIT 1")
    suspend fun getLatest(userId: String): SleepEntity?

    /**
     * Get the latest sleep record as Flow for reactive updates
     * @param userId The user ID
     * @return Flow emitting the latest sleep entity
     */
    @Query("SELECT * FROM sleep WHERE userId = :userId ORDER BY endTime DESC LIMIT 1")
    fun getLatestAsFlow(userId: String): Flow<SleepEntity?>

    /**
     * Get sleep records for a specific date
     * @param userId The user ID
     * @param date The date to retrieve sleep records for
     * @return List of sleep entities
     */
    @Query("""
        SELECT * FROM sleep 
        WHERE userId = :userId 
        AND (date(startTime) = date(:date) OR date(endTime) = date(:date))
        ORDER BY startTime DESC
    """)
    suspend fun getByDate(userId: String, date: LocalDate): List<SleepEntity>

    /**
     * Get sleep records for a specific date as Flow for reactive updates
     * @param userId The user ID
     * @param date The date to retrieve sleep records for
     * @return Flow emitting list of sleep entities
     */
    @Query("""
        SELECT * FROM sleep 
        WHERE userId = :userId 
        AND (date(startTime) = date(:date) OR date(endTime) = date(:date))
        ORDER BY startTime DESC
    """)
    fun getByDateAsFlow(userId: String, date: LocalDate): Flow<List<SleepEntity>>

    /**
     * Sleep Stage Analysis
     */

    /**
     * Get sleep stage distribution for a specific sleep session
     * @param sleepId The sleep session ID
     * @return Distribution of time spent in each sleep stage
     */
    @Query("""
        SELECT 
            stage,
            SUM(julianday(endTime) - julianday(startTime)) * 24 * 60 as durationMinutes,
            COUNT(*) as count
        FROM sleep_stages
        WHERE sleepId = :sleepId
        GROUP BY stage
        ORDER BY stage
    """)
    suspend fun getSleepStageDistribution(sleepId: String): List<SleepStageDistribution>

    /**
     * Get sleep stage distribution for a specific sleep session as Flow for reactive updates
     * @param sleepId The sleep session ID
     * @return Flow emitting distribution of time spent in each sleep stage
     */
    @Query("""
        SELECT 
            stage,
            SUM(julianday(endTime) - julianday(startTime)) * 24 * 60 as durationMinutes,
            COUNT(*) as count
        FROM sleep_stages
        WHERE sleepId = :sleepId
        GROUP BY stage
        ORDER BY stage
    """)
    fun getSleepStageDistributionAsFlow(sleepId: String): Flow<List<SleepStageDistribution>>

    /**
     * Get sleep stage distribution for a date range
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return Distribution of time spent in each sleep stage
     */
    @Query("""
        SELECT 
            ss.stage,
            SUM(julianday(ss.endTime) - julianday(ss.startTime)) * 24 * 60 as durationMinutes,
            COUNT(*) as count,
            AVG(julianday(ss.endTime) - julianday(ss.startTime)) * 24 * 60 as avgDurationMinutes
        FROM sleep_stages ss
        JOIN sleep s ON ss.sleepId = s.id
        WHERE s.userId = :userId
        AND date(s.startTime) BETWEEN date(:startDate) AND date(:endDate)
        GROUP BY ss.stage
        ORDER BY ss.stage
    """)
    suspend fun getSleepStageDistributionByDateRange(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<SleepStageDistribution>

    /**
     * Get daily sleep stage distribution
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return Daily distribution of time spent in each sleep stage
     */
    @Query("""
        SELECT 
            date(s.startTime) as date,
            ss.stage,
            SUM(julianday(ss.endTime) - julianday(ss.startTime)) * 24 * 60 as durationMinutes,
            COUNT(*) as count
        FROM sleep_stages ss
        JOIN sleep s ON ss.sleepId = s.id
        WHERE s.userId = :userId
        AND date(s.startTime) BETWEEN date(:startDate) AND date(:endDate)
        GROUP BY date(s.startTime), ss.stage
        ORDER BY date(s.startTime), ss.stage
    """)
    suspend fun getDailySleepStageDistribution(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<DailySleepStageDistribution>

    /**
     * Get REM sleep percentage over time
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return Daily REM sleep percentage
     */
    @Query("""
        WITH daily_stages AS (
            SELECT 
                date(s.startTime) as date,
                ss.stage,
                SUM(julianday(ss.endTime) - julianday(ss.startTime)) * 24 * 60 as durationMinutes
            FROM sleep_stages ss
            JOIN sleep s ON ss.sleepId = s.id
            WHERE s.userId = :userId
            AND date(s.startTime) BETWEEN date(:startDate) AND date(:endDate)
            GROUP BY date(s.startTime), ss.stage
        ),
        daily_total AS (
            SELECT 
                date,
                SUM(durationMinutes) as totalMinutes
            FROM daily_stages
            GROUP BY date
        ),
        rem_sleep AS (
            SELECT 
                date,
                durationMinutes as remMinutes
            FROM daily_stages
            WHERE stage = 'REM'
        )
        SELECT 
            dt.date,
            COALESCE(rs.remMinutes, 0) as remMinutes,
            dt.totalMinutes,
            (COALESCE(rs.remMinutes, 0) * 100.0 / dt.totalMinutes) as remPercentage
        FROM daily_total dt
        LEFT JOIN rem_sleep rs ON dt.date = rs.date
        ORDER BY dt.date
    """)
    suspend fun getRemSleepPercentageOverTime(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<DailyRemPercentage>

    /**
     * Get deep sleep percentage over time
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return Daily deep sleep percentage
     */
    @Query("""
        WITH daily_stages AS (
            SELECT 
                date(s.startTime) as date,
                ss.stage,
                SUM(julianday(ss.endTime) - julianday(ss.startTime)) * 24 * 60 as durationMinutes
            FROM sleep_stages ss
            JOIN sleep s ON ss.sleepId = s.id
            WHERE s.userId = :userId
            AND date(s.startTime) BETWEEN date(:startDate) AND date(:endDate)
            GROUP BY date(s.startTime), ss.stage
        ),
        daily_total AS (
            SELECT 
                date,
                SUM(durationMinutes) as totalMinutes
            FROM daily_stages
            GROUP BY date
        ),
        deep_sleep AS (
            SELECT 
                date,
                durationMinutes as deepMinutes
            FROM daily_stages
            WHERE stage = 'DEEP'
        )
        SELECT 
            dt.date,
            COALESCE(ds.deepMinutes, 0) as deepMinutes,
            dt.totalMinutes,
            (COALESCE(ds.deepMinutes, 0) * 100.0 / dt.totalMinutes) as deepPercentage
        FROM daily_total dt
        LEFT JOIN deep_sleep ds ON dt.date = ds.date
        ORDER BY dt.date
    """)
    suspend fun getDeepSleepPercentageOverTime(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<DailyDeepPercentage>

    /**
     * Get sleep stage transition patterns
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return Sleep stage transition counts
     */
    @Query("""
        WITH stage_pairs AS (
            SELECT 
                ss1.stage as fromStage,
                ss2.stage as toStage,
                COUNT(*) as transitionCount
            FROM sleep_stages ss1
            JOIN sleep_stages ss2 ON ss1.sleepId = ss2.sleepId
            JOIN sleep s ON ss1.sleepId = s.id
            WHERE s.userId = :userId
            AND date(s.startTime) BETWEEN date(:startDate) AND date(:endDate)
            AND ss2.startTime = ss1.endTime
            GROUP BY ss1.stage, ss2.stage
        )
        SELECT 
            fromStage,
            toStage,
            transitionCount
        FROM stage_pairs
        ORDER BY transitionCount DESC
    """)
    suspend fun getSleepStageTransitions(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<SleepStageTransition>

    /**
     * Sleep Efficiency Calculations
     */

    /**
     * Calculate sleep efficiency for a specific sleep session
     * Sleep efficiency = (Total sleep time / Time in bed) * 100
     * @param sleepId The sleep session ID
     * @return Sleep efficiency metrics
     */
    @Query("""
        WITH sleep_data AS (
            SELECT 
                s.id,
                (julianday(s.endTime) - julianday(s.startTime)) * 24 * 60 as timeInBedMinutes,
                (
                    SELECT SUM(julianday(ss.endTime) - julianday(ss.startTime)) * 24 * 60
                    FROM sleep_stages ss
                    WHERE ss.sleepId = s.id
                    AND ss.stage != 'AWAKE'
                ) as totalSleepMinutes,
                (
                    SELECT SUM(julianday(ss.endTime) - julianday(ss.startTime)) * 24 * 60
                    FROM sleep_stages ss
                    WHERE ss.sleepId = s.id
                    AND ss.stage = 'AWAKE'
                ) as awakeMinutes
            FROM sleep s
            WHERE s.id = :sleepId
        )
        SELECT 
            id as sleepId,
            timeInBedMinutes,
            COALESCE(totalSleepMinutes, 0) as totalSleepMinutes,
            COALESCE(awakeMinutes, 0) as awakeMinutes,
            CASE 
                WHEN timeInBedMinutes > 0 THEN (COALESCE(totalSleepMinutes, 0) * 100.0 / timeInBedMinutes)
                ELSE 0
            END as sleepEfficiencyPercentage
        FROM sleep_data
    """)
    suspend fun calculateSleepEfficiency(sleepId: String): SleepEfficiency?

    /**
     * Calculate sleep efficiency for a specific sleep session as Flow for reactive updates
     * @param sleepId The sleep session ID
     * @return Flow emitting sleep efficiency metrics
     */
    @Query("""
        WITH sleep_data AS (
            SELECT 
                s.id,
                (julianday(s.endTime) - julianday(s.startTime)) * 24 * 60 as timeInBedMinutes,
                (
                    SELECT SUM(julianday(ss.endTime) - julianday(ss.startTime)) * 24 * 60
                    FROM sleep_stages ss
                    WHERE ss.sleepId = s.id
                    AND ss.stage != 'AWAKE'
                ) as totalSleepMinutes,
                (
                    SELECT SUM(julianday(ss.endTime) - julianday(ss.startTime)) * 24 * 60
                    FROM sleep_stages ss
                    WHERE ss.sleepId = s.id
                    AND ss.stage = 'AWAKE'
                ) as awakeMinutes
            FROM sleep s
            WHERE s.id = :sleepId
        )
        SELECT 
            id as sleepId,
            timeInBedMinutes,
            COALESCE(totalSleepMinutes, 0) as totalSleepMinutes,
            COALESCE(awakeMinutes, 0) as awakeMinutes,
            CASE 
                WHEN timeInBedMinutes > 0 THEN (COALESCE(totalSleepMinutes, 0) * 100.0 / timeInBedMinutes)
                ELSE 0
            END as sleepEfficiencyPercentage
        FROM sleep_data
    """)
    fun calculateSleepEfficiencyAsFlow(sleepId: String): Flow<SleepEfficiency?>

    /**
     * Calculate sleep efficiency over time
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return Daily sleep efficiency metrics
     */
    @Query("""
        WITH sleep_data AS (
            SELECT 
                s.id,
                date(s.startTime) as date,
                (julianday(s.endTime) - julianday(s.startTime)) * 24 * 60 as timeInBedMinutes,
                (
                    SELECT SUM(julianday(ss.endTime) - julianday(ss.startTime)) * 24 * 60
                    FROM sleep_stages ss
                    WHERE ss.sleepId = s.id
                    AND ss.stage != 'AWAKE'
                ) as totalSleepMinutes,
                (
                    SELECT SUM(julianday(ss.endTime) - julianday(ss.startTime)) * 24 * 60
                    FROM sleep_stages ss
                    WHERE ss.sleepId = s.id
                    AND ss.stage = 'AWAKE'
                ) as awakeMinutes
            FROM sleep s
            WHERE s.userId = :userId
            AND date(s.startTime) BETWEEN date(:startDate) AND date(:endDate)
        ),
        daily_data AS (
            SELECT 
                date,
                SUM(timeInBedMinutes) as timeInBedMinutes,
                SUM(COALESCE(totalSleepMinutes, 0)) as totalSleepMinutes,
                SUM(COALESCE(awakeMinutes, 0)) as awakeMinutes
            FROM sleep_data
            GROUP BY date
        )
        SELECT 
            date,
            timeInBedMinutes,
            totalSleepMinutes,
            awakeMinutes,
            CASE 
                WHEN timeInBedMinutes > 0 THEN (totalSleepMinutes * 100.0 / timeInBedMinutes)
                ELSE 0
            END as sleepEfficiencyPercentage
        FROM daily_data
        ORDER BY date
    """)
    suspend fun getSleepEfficiencyOverTime(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<DailySleepEfficiency>

    /**
     * Get sleep latency (time to fall asleep) over time
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return Daily sleep latency metrics
     */
    @Query("""
        WITH sleep_data AS (
            SELECT 
                s.id,
                date(s.startTime) as date,
                s.startTime as bedtime,
                (
                    SELECT MIN(ss.startTime)
                    FROM sleep_stages ss
                    WHERE ss.sleepId = s.id
                    AND ss.stage != 'AWAKE'
                ) as firstSleepTime
            FROM sleep s
            WHERE s.userId = :userId
            AND date(s.startTime) BETWEEN date(:startDate) AND date(:endDate)
        )
        SELECT 
            date,
            (julianday(firstSleepTime) - julianday(bedtime)) * 24 * 60 as latencyMinutes
        FROM sleep_data
        WHERE firstSleepTime IS NOT NULL
        ORDER BY date
    """)
    suspend fun getSleepLatencyOverTime(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<DailySleepLatency>

    /**
     * Get wake after sleep onset (WASO) over time
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return Daily WASO metrics
     */
    @Query("""
        WITH sleep_data AS (
            SELECT 
                s.id,
                date(s.startTime) as date,
                (
                    SELECT SUM(julianday(ss.endTime) - julianday(ss.startTime)) * 24 * 60
                    FROM sleep_stages ss
                    WHERE ss.sleepId = s.id
                    AND ss.stage = 'AWAKE'
                    AND ss.startTime > (
                        SELECT MIN(ss2.startTime)
                        FROM sleep_stages ss2
                        WHERE ss2.sleepId = s.id
                        AND ss2.stage != 'AWAKE'
                    )
                ) as wasoMinutes
            FROM sleep s
            WHERE s.userId = :userId
            AND date(s.startTime) BETWEEN date(:startDate) AND date(:endDate)
        ),
        daily_data AS (
            SELECT 
                date,
                SUM(COALESCE(wasoMinutes, 0)) as wasoMinutes
            FROM sleep_data
            GROUP BY date
        )
        SELECT 
            date,
            wasoMinutes
        FROM daily_data
        ORDER BY date
    """)
    suspend fun getWasoOverTime(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<DailyWaso>

    /**
     * Sleep Pattern Detection
     */

    /**
     * Get sleep duration over time
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return Daily sleep duration
     */
    @Query("""
        SELECT 
            date(startTime) as date,
            SUM((julianday(endTime) - julianday(startTime)) * 24) as hoursSlept
        FROM sleep
        WHERE userId = :userId
        AND date(startTime) BETWEEN date(:startDate) AND date(:endDate)
        GROUP BY date(startTime)
        ORDER BY date(startTime)
    """)
    suspend fun getSleepDurationOverTime(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<DailySleepDuration>

    /**
     * Get sleep duration over time as Flow for reactive updates
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return Flow emitting daily sleep duration
     */
    @Query("""
        SELECT 
            date(startTime) as date,
            SUM((julianday(endTime) - julianday(startTime)) * 24) as hoursSlept
        FROM sleep
        WHERE userId = :userId
        AND date(startTime) BETWEEN date(:startDate) AND date(:endDate)
        GROUP BY date(startTime)
        ORDER BY date(startTime)
    """)
    fun getSleepDurationOverTimeAsFlow(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<DailySleepDuration>>

    /**
     * Get sleep duration by day of week
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return Sleep duration by day of week
     */
    @Query("""
        SELECT 
            strftime('%w', startTime) as dayOfWeek,
            AVG((julianday(endTime) - julianday(startTime)) * 24) as avgHoursSlept,
            MIN((julianday(endTime) - julianday(startTime)) * 24) as minHoursSlept,
            MAX((julianday(endTime) - julianday(startTime)) * 24) as maxHoursSlept,
            COUNT(*) as sleepCount
        FROM sleep
        WHERE userId = :userId
        AND date(startTime) BETWEEN date(:startDate) AND date(:endDate)
        GROUP BY strftime('%w', startTime)
        ORDER BY dayOfWeek
    """)
    suspend fun getSleepDurationByDayOfWeek(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<DayOfWeekSleepDuration>

    /**
     * Detect sleep interruptions
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @param minInterruptionMinutes Minimum duration to consider as interruption
     * @return List of sleep interruptions
     */
    @Query("""
        SELECT 
            s.id as sleepId,
            date(s.startTime) as date,
            ss.startTime as interruptionStart,
            ss.endTime as interruptionEnd,
            (julianday(ss.endTime) - julianday(ss.startTime)) * 24 * 60 as durationMinutes
        FROM sleep_stages ss
        JOIN sleep s ON ss.sleepId = s.id
        WHERE s.userId = :userId
        AND date(s.startTime) BETWEEN date(:startDate) AND date(:endDate)
        AND ss.stage = 'AWAKE'
        AND (julianday(ss.endTime) - julianday(ss.startTime)) * 24 * 60 >= :minInterruptionMinutes
        AND ss.startTime > (
            SELECT MIN(ss2.startTime)
            FROM sleep_stages ss2
            WHERE ss2.sleepId = s.id
            AND ss2.stage != 'AWAKE'
        )
        AND ss.endTime < (
            SELECT MAX(ss2.endTime)
            FROM sleep_stages ss2
            WHERE ss2.sleepId = s.id
            AND ss2.stage != 'AWAKE'
        )
        ORDER BY date, interruptionStart
    """)
    suspend fun detectSleepInterruptions(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate,
        minInterruptionMinutes: Double = 5.0
    ): List<SleepInterruption>

    /**
     * Get fragmentation index (number of awakenings per hour of sleep)
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return Daily fragmentation index
     */
    @Query("""
        WITH sleep_data AS (
            SELECT 
                s.id,
                date(s.startTime) as date,
                (julianday(s.endTime) - julianday(s.startTime)) * 24 as hoursInBed,
                (
                    SELECT COUNT(*)
                    FROM sleep_stages ss
                    WHERE ss.sleepId = s.id
                    AND ss.stage = 'AWAKE'
                    AND ss.startTime > (
                        SELECT MIN(ss2.startTime)
                        FROM sleep_stages ss2
                        WHERE ss2.sleepId = s.id
                        AND ss2.stage != 'AWAKE'
                    )
                    AND ss.endTime < (
                        SELECT MAX(ss2.endTime)
                        FROM sleep_stages ss2
                        WHERE ss2.sleepId = s.id
                        AND ss2.stage != 'AWAKE'
                    )
                ) as awakeningCount
            FROM sleep s
            WHERE s.userId = :userId
            AND date(s.startTime) BETWEEN date(:startDate) AND date(:endDate)
        ),
        daily_data AS (
            SELECT 
                date,
                SUM(hoursInBed) as totalHoursInBed,
                SUM(awakeningCount) as totalAwakeningCount
            FROM sleep_data
            GROUP BY date
        )
        SELECT 
            date,
            totalHoursInBed,
            totalAwakeningCount,
            CASE 
                WHEN totalHoursInBed > 0 THEN (totalAwakeningCount * 1.0 / totalHoursInBed)
                ELSE 0
            END as fragmentationIndex
        FROM daily_data
        ORDER BY date
    """)
    suspend fun getSleepFragmentationIndex(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<DailyFragmentationIndex>

    /**
     * Detect sleep pattern changes
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @param thresholdHours Threshold for significant change in sleep duration
     * @return List of significant sleep pattern changes
     */
    @Query("""
        WITH daily_sleep AS (
            SELECT 
                date(startTime) as date,
                SUM((julianday(endTime) - julianday(startTime)) * 24) as hoursSlept
            FROM sleep
            WHERE userId = :userId
            AND date(startTime) BETWEEN date(:startDate) AND date(:endDate)
            GROUP BY date(startTime)
        ),
        moving_avg AS (
            SELECT 
                date,
                hoursSlept,
                AVG(hoursSlept) OVER (
                    ORDER BY date
                    ROWS BETWEEN 7 PRECEDING AND 1 PRECEDING
                ) as avg_previous_week
            FROM daily_sleep
        )
        SELECT 
            date,
            hoursSlept,
            avg_previous_week,
            (hoursSlept - avg_previous_week) as change
        FROM moving_avg
        WHERE ABS(hoursSlept - avg_previous_week) >= :thresholdHours
        AND avg_previous_week IS NOT NULL
        ORDER BY ABS(hoursSlept - avg_previous_week) DESC
    """)
    suspend fun detectSleepPatternChanges(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate,
        thresholdHours: Double = 2.0
    ): List<SleepPatternChange>

    /**
     * Sleep Debt Tracking
     */

    /**
     * Calculate sleep debt based on recommended sleep duration
     * @param userId The user ID
     * @param recommendedHours Recommended hours of sleep per night
     * @param startDate Start date
     * @param endDate End date
     * @return Daily and cumulative sleep debt
     */
    @Query("""
        WITH daily_sleep AS (
            SELECT 
                date(startTime) as date,
                SUM((julianday(endTime) - julianday(startTime)) * 24) as hoursSlept
            FROM sleep
            WHERE userId = :userId
            AND date(startTime) BETWEEN date(:startDate) AND date(:endDate)
            GROUP BY date(startTime)
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
                SELECT 31 UNION SELECT 32 UNION SELECT 33 UNION SELECT 34 UNION SELECT 35 UNION
                SELECT 36 UNION SELECT 37 UNION SELECT 38 UNION SELECT 39 UNION SELECT 40 UNION
                SELECT 41 UNION SELECT 42 UNION SELECT 43 UNION SELECT 44 UNION SELECT 45 UNION
                SELECT 46 UNION SELECT 47 UNION SELECT 48 UNION SELECT 49 UNION SELECT 50 UNION
                SELECT 51 UNION SELECT 52 UNION SELECT 53 UNION SELECT 54 UNION SELECT 55 UNION
                SELECT 56 UNION SELECT 57 UNION SELECT 58 UNION SELECT 59 UNION SELECT 60 UNION
                SELECT 61 UNION SELECT 62 UNION SELECT 63 UNION SELECT 64 UNION SELECT 65 UNION
                SELECT 66 UNION SELECT 67 UNION SELECT 68 UNION SELECT 69 UNION SELECT 70 UNION
                SELECT 71 UNION SELECT 72 UNION SELECT 73 UNION SELECT 74 UNION SELECT 75 UNION
                SELECT 76 UNION SELECT 77 UNION SELECT 78 UNION SELECT 79 UNION SELECT 80 UNION
                SELECT 81 UNION SELECT 82 UNION SELECT 83 UNION SELECT 84 UNION SELECT 85 UNION
                SELECT 86 UNION SELECT 87 UNION SELECT 88 UNION SELECT 89 UNION SELECT 90 UNION
                SELECT 91 UNION SELECT 92 UNION SELECT 93 UNION SELECT 94 UNION SELECT 95 UNION
                SELECT 96 UNION SELECT 97 UNION SELECT 98 UNION SELECT 99 UNION SELECT 100
            )
            WHERE date < date(:endDate, '+1 day')
        ),
        daily_debt AS (
            SELECT 
                ad.date,
                COALESCE(ds.hoursSlept, 0) as hoursSlept,
                :recommendedHours as recommendedHours,
                (:recommendedHours - COALESCE(ds.hoursSlept, 0)) as dailyDebt
            FROM all_dates ad
            LEFT JOIN daily_sleep ds ON ad.date = ds.date
        )
        SELECT 
            date,
            hoursSlept,
            recommendedHours,
            dailyDebt,
            SUM(dailyDebt) OVER (ORDER BY date) as cumulativeDebt
        FROM daily_debt
        ORDER BY date
    """)
    suspend fun calculateSleepDebt(
        userId: String,
        recommendedHours: Double = 8.0,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<SleepDebt>

    /**
     * Get sleep debt summary
     * @param userId The user ID
     * @param recommendedHours Recommended hours of sleep per night
     * @param startDate Start date
     * @param endDate End date
     * @return Sleep debt summary metrics
     */
    @Query("""
        WITH daily_sleep AS (
            SELECT 
                date(startTime) as date,
                SUM((julianday(endTime) - julianday(startTime)) * 24) as hoursSlept
            FROM sleep
            WHERE userId = :userId
            AND date(startTime) BETWEEN date(:startDate) AND date(:endDate)
            GROUP BY date(startTime)
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
                SELECT 31 UNION SELECT 32 UNION SELECT 33 UNION SELECT 34 UNION SELECT 35 UNION
                SELECT 36 UNION SELECT 37 UNION SELECT 38 UNION SELECT 39 UNION SELECT 40 UNION
                SELECT 41 UNION SELECT 42 UNION SELECT 43 UNION SELECT 44 UNION SELECT 45 UNION
                SELECT 46 UNION SELECT 47 UNION SELECT 48 UNION SELECT 49 UNION SELECT 50 UNION
                SELECT 51 UNION SELECT 52 UNION SELECT 53 UNION SELECT 54 UNION SELECT 55 UNION
                SELECT 56 UNION SELECT 57 UNION SELECT 58 UNION SELECT 59 UNION SELECT 60 UNION
                SELECT 61 UNION SELECT 62 UNION SELECT 63 UNION SELECT 64 UNION SELECT 65 UNION
                SELECT 66 UNION SELECT 67 UNION SELECT 68 UNION SELECT 69 UNION SELECT 70 UNION
                SELECT 71 UNION SELECT 72 UNION SELECT 73 UNION SELECT 74 UNION SELECT 75 UNION
                SELECT 76 UNION SELECT 77 UNION SELECT 78 UNION SELECT 79 UNION SELECT 80 UNION
                SELECT 81 UNION SELECT 82 UNION SELECT 83 UNION SELECT 84 UNION SELECT 85 UNION
                SELECT 86 UNION SELECT 87 UNION SELECT 88 UNION SELECT 89 UNION SELECT 90 UNION
                SELECT 91 UNION SELECT 92 UNION SELECT 93 UNION SELECT 94 UNION SELECT 95 UNION
                SELECT 96 UNION SELECT 97 UNION SELECT 98 UNION SELECT 99 UNION SELECT 100
            )
            WHERE date < date(:endDate, '+1 day')
        ),
        daily_debt AS (
            SELECT 
                ad.date,
                COALESCE(ds.hoursSlept, 0) as hoursSlept,
                :recommendedHours as recommendedHours,
                (:recommendedHours - COALESCE(ds.hoursSlept, 0)) as dailyDebt
            FROM all_dates ad
            LEFT JOIN daily_sleep ds ON ad.date = ds.date
        )
        SELECT 
            SUM(dailyDebt) as totalSleepDebt,
            AVG(dailyDebt) as avgDailyDebt,
            MAX(dailyDebt) as maxDailyDebt,
            COUNT(CASE WHEN dailyDebt > 0 THEN 1 END) as daysWithDebt,
            COUNT(*) as totalDays,
            (COUNT(CASE WHEN dailyDebt > 0 THEN 1 END) * 100.0 / COUNT(*)) as percentDaysWithDebt
        FROM daily_debt
    """)
    suspend fun getSleepDebtSummary(
        userId: String,
        recommendedHours: Double = 8.0,
        startDate: LocalDate,
        endDate: LocalDate
    ): SleepDebtSummary

    /**
     * Circadian Rhythm Analysis
     */

    /**
     * Get bedtime and wake time patterns
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return Daily bedtime and wake time
     */
    @Query("""
        SELECT 
            date(startTime) as date,
            time(startTime) as bedtime,
            time(endTime) as wakeTime,
            (julianday(endTime) - julianday(startTime)) * 24 as hoursSlept
        FROM sleep
        WHERE userId = :userId
        AND date(startTime) BETWEEN date(:startDate) AND date(:endDate)
        ORDER BY date
    """)
    suspend fun getBedtimeWakeTimePatterns(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<BedtimeWakeTime>

    /**
     * Get bedtime and wake time consistency
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return Bedtime and wake time consistency metrics
     */
    @Query("""
        WITH sleep_times AS (
            SELECT 
                strftime('%H:%M:%S', startTime) as bedtime,
                strftime('%H:%M:%S', endTime) as wakeTime
            FROM sleep
            WHERE userId = :userId
            AND date(startTime) BETWEEN date(:startDate) AND date(:endDate)
        ),
        bedtime_seconds AS (
            SELECT 
                (CAST(substr(bedtime, 1, 2) as INTEGER) * 3600 + 
                 CAST(substr(bedtime, 4, 2) as INTEGER) * 60 + 
                 CAST(substr(bedtime, 7, 2) as INTEGER)) as seconds_from_midnight
            FROM sleep_times
        ),
        waketime_seconds AS (
            SELECT 
                (CAST(substr(wakeTime, 1, 2) as INTEGER) * 3600 + 
                 CAST(substr(wakeTime, 4, 2) as INTEGER) * 60 + 
                 CAST(substr(wakeTime, 7, 2) as INTEGER)) as seconds_from_midnight
            FROM sleep_times
        )
        SELECT 
            (SELECT AVG(seconds_from_midnight) FROM bedtime_seconds) / 3600.0 as avgBedtimeHour,
            (SELECT SQRT(AVG((seconds_from_midnight - AVG(seconds_from_midnight)) * 
                            (seconds_from_midnight - AVG(seconds_from_midnight)))) 
             FROM bedtime_seconds) / 60.0 as bedtimeVariabilityMinutes,
            (SELECT AVG(seconds_from_midnight) FROM waketime_seconds) / 3600.0 as avgWaketimeHour,
            (SELECT SQRT(AVG((seconds_from_midnight - AVG(seconds_from_midnight)) * 
                            (seconds_from_midnight - AVG(seconds_from_midnight)))) 
             FROM waketime_seconds) / 60.0 as waketimeVariabilityMinutes
    """)
    suspend fun getSleepTimeConsistency(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): SleepTimeConsistency?

    /**
     * Get social jetlag (difference between weekday and weekend sleep patterns)
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return Social jetlag metrics
     */
    @Query("""
        WITH weekday_sleep AS (
            SELECT 
                AVG((julianday(endTime) - julianday(startTime)) * 24) as avgSleepHours,
                AVG(strftime('%H', startTime) * 60 + strftime('%M', startTime)) as avgBedtimeMinutes,
                AVG(strftime('%H', endTime) * 60 + strftime('%M', endTime)) as avgWaketimeMinutes
            FROM sleep
            WHERE userId = :userId
            AND date(startTime) BETWEEN date(:startDate) AND date(:endDate)
            AND strftime('%w', startTime) NOT IN ('0', '6')
        ),
        weekend_sleep AS (
            SELECT 
                AVG((julianday(endTime) - julianday(startTime)) * 24) as avgSleepHours,
                AVG(strftime('%H', startTime) * 60 + strftime('%M', startTime)) as avgBedtimeMinutes,
                AVG(strftime('%H', endTime) * 60 + strftime('%M', endTime)) as avgWaketimeMinutes
            FROM sleep
            WHERE userId = :userId
            AND date(startTime) BETWEEN date(:startDate) AND date(:endDate)
            AND strftime('%w', startTime) IN ('0', '6')
        )
        SELECT 
            (SELECT avgSleepHours FROM weekday_sleep) as weekdayAvgSleepHours,
            (SELECT avgSleepHours FROM weekend_sleep) as weekendAvgSleepHours,
            (SELECT avgBedtimeMinutes FROM weekday_sleep) / 60.0 as weekdayAvgBedtimeHour,
            (SELECT avgBedtimeMinutes FROM weekend_sleep) / 60.0 as weekendAvgBedtimeHour,
            (SELECT avgWaketimeMinutes FROM weekday_sleep) / 60.0 as weekdayAvgWaketimeHour,
            (SELECT avgWaketimeMinutes FROM weekend_sleep) / 60.0 as weekendAvgWaketimeHour,
            ABS((SELECT avgBedtimeMinutes FROM weekend_sleep) - 
                (SELECT avgBedtimeMinutes FROM weekday_sleep)) / 60.0 as bedtimeDifferenceHours,
            ABS((SELECT avgWaketimeMinutes FROM weekend_sleep) - 
                (SELECT avgWaketimeMinutes FROM weekday_sleep)) / 60.0 as waketimeDifferenceHours,
            ((SELECT avgSleepHours FROM weekend_sleep) - 
             (SELECT avgSleepHours FROM weekday_sleep)) as sleepDurationDifferenceHours
    """)
    suspend fun getSocialJetlag(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): SocialJetlag?

    /**
     * Detect irregular sleep patterns
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @param bedtimeThresholdHours Threshold for bedtime irregularity
     * @param durationThresholdHours Threshold for duration irregularity
     * @return List of days with irregular sleep
     */
    @Query("""
        WITH sleep_data AS (
            SELECT 
                date(startTime) as date,
                time(startTime) as bedtime,
                time(endTime) as wakeTime,
                (julianday(endTime) - julianday(startTime)) * 24 as hoursSlept,
                (strftime('%H', startTime) * 60 + strftime('%M', startTime)) as bedtimeMinutes
            FROM sleep
            WHERE userId = :userId
            AND date(startTime) BETWEEN date(:startDate) AND date(:endDate)
        ),
        avg_sleep AS (
            SELECT 
                AVG(bedtimeMinutes) as avgBedtimeMinutes,
                AVG(hoursSlept) as avgHoursSlept
            FROM sleep_data
        )
        SELECT 
            sd.date,
            sd.bedtime,
            sd.wakeTime,
            sd.hoursSlept,
            ABS(sd.bedtimeMinutes - (SELECT avgBedtimeMinutes FROM avg_sleep)) / 60.0 as bedtimeDeviationHours,
            ABS(sd.hoursSlept - (SELECT avgHoursSlept FROM avg_sleep)) as durationDeviationHours
        FROM sleep_data sd
        WHERE 
            ABS(sd.bedtimeMinutes - (SELECT avgBedtimeMinutes FROM avg_sleep)) / 60.0 > :bedtimeThresholdHours
            OR ABS(sd.hoursSlept - (SELECT avgHoursSlept FROM avg_sleep)) > :durationThresholdHours
        ORDER BY 
            CASE 
                WHEN ABS(sd.bedtimeMinutes - (SELECT avgBedtimeMinutes FROM avg_sleep)) / 60.0 > :bedtimeThresholdHours
                     AND ABS(sd.hoursSlept - (SELECT avgHoursSlept FROM avg_sleep)) > :durationThresholdHours
                THEN 1
                ELSE 2
            END,
            ABS(sd.bedtimeMinutes - (SELECT avgBedtimeMinutes FROM avg_sleep)) / 60.0 +
            ABS(sd.hoursSlept - (SELECT avgHoursSlept FROM avg_sleep)) DESC
    """)
    suspend fun detectIrregularSleepPatterns(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate,
        bedtimeThresholdHours: Double = 1.5,
        durationThresholdHours: Double = 2.0
    ): List<IrregularSleepPattern>

    /**
     * Sleep Quality Scoring
     */

    /**
     * Calculate sleep quality score
     * Score is based on:
     * - Sleep duration (optimal is 7-9 hours)
     * - Sleep efficiency (optimal is >85%)
     * - Deep sleep percentage (optimal is >20%)
     * - REM sleep percentage (optimal is >20%)
     * - Sleep latency (optimal is <30 minutes)
     * - Number of awakenings (optimal is <3)
     * 
     * @param sleepId The sleep session ID
     * @return Sleep quality score and components
     */
    @Query("""
        WITH sleep_data AS (
            SELECT 
                s.id,
                (julianday(s.endTime) - julianday(s.startTime)) * 24 as durationHours,
                (
                    SELECT (julianday(MIN(ss.startTime)) - julianday(s.startTime)) * 24 * 60
                    FROM sleep_stages ss
                    WHERE ss.sleepId = s.id
                    AND ss.stage != 'AWAKE'
                ) as latencyMinutes,
                (
                    SELECT COUNT(*)
                    FROM sleep_stages ss
                    WHERE ss.sleepId = s.id
                    AND ss.stage = 'AWAKE'
                    AND ss.startTime > (
                        SELECT MIN(ss2.startTime)
                        FROM sleep_stages ss2
                        WHERE ss2.sleepId = s.id
                        AND ss2.stage != 'AWAKE'
                    )
                ) as awakeningCount,
                (
                    SELECT SUM(julianday(ss.endTime) - julianday(ss.startTime)) * 24 * 60
                    FROM sleep_stages ss
                    WHERE ss.sleepId = s.id
                    AND ss.stage = 'DEEP'
                ) as deepSleepMinutes,
                (
                    SELECT SUM(julianday(ss.endTime) - julianday(ss.startTime)) * 24 * 60
                    FROM sleep_stages ss
                    WHERE ss.sleepId = s.id
                    AND ss.stage = 'REM'
                ) as remSleepMinutes,
                (
                    SELECT SUM(julianday(ss.endTime) - julianday(ss.startTime)) * 24 * 60
                    FROM sleep_stages ss
                    WHERE ss.sleepId = s.id
                    AND ss.stage != 'AWAKE'
                ) as totalSleepMinutes
            FROM sleep s
            WHERE s.id = :sleepId
        ),
        score_components AS (
            SELECT 
                id,
                durationHours,
                CASE
                    WHEN durationHours BETWEEN 7 AND 9 THEN 25
                    WHEN durationHours BETWEEN 6 AND 7 OR durationHours BETWEEN 9 AND 10 THEN 15
                    WHEN durationHours BETWEEN 5 AND 6 OR durationHours BETWEEN 10 AND 11 THEN 10
                    ELSE 0
                END as durationScore,
                
                COALESCE(latencyMinutes, 0) as latencyMinutes,
                CASE
                    WHEN latencyMinutes < 15 THEN 20
                    WHEN latencyMinutes BETWEEN 15 AND 30 THEN 15
                    WHEN latencyMinutes BETWEEN 30 AND 60 THEN 10
                    ELSE 0
                END as latencyScore,
                
                COALESCE(awakeningCount, 0) as awakeningCount,
                CASE
                    WHEN awakeningCount <= 1 THEN 15
                    WHEN awakeningCount = 2 THEN 10
                    WHEN awakeningCount = 3 THEN 5
                    ELSE 0
                END as awakeningScore,
                
                COALESCE(deepSleepMinutes, 0) as deepSleepMinutes,
                COALESCE(totalSleepMinutes, 0) as totalSleepMinutes,
                CASE
                    WHEN totalSleepMinutes > 0 THEN (deepSleepMinutes * 100.0 / totalSleepMinutes)
                    ELSE 0
                END as deepSleepPercentage,
                CASE
                    WHEN deepSleepPercentage >= 20 THEN 20
                    WHEN deepSleepPercentage >= 15 THEN 15
                    WHEN deepSleepPercentage >= 10 THEN 10
                    ELSE 0
                END as deepSleepScore,
                
                COALESCE(remSleepMinutes, 0) as remSleepMinutes,
                CASE
                    WHEN totalSleepMinutes > 0 THEN (remSleepMinutes * 100.0 / totalSleepMinutes)
                    ELSE 0
                END as remSleepPercentage,
                CASE
                    WHEN remSleepPercentage >= 20 THEN 20
                    WHEN remSleepPercentage >= 15 THEN 15
                    WHEN remSleepPercentage >= 10 THEN 10
                    ELSE 0
                END as remSleepScore
            FROM sleep_data
        )
        SELECT 
            id as sleepId,
            durationHours,
            durationScore,
            latencyMinutes,
            latencyScore,
            awakeningCount,
            awakeningScore,
            deepSleepMinutes,
            deepSleepPercentage,
            deepSleepScore,
            remSleepMinutes,
            remSleepPercentage,
            remSleepScore,
            (durationScore + latencyScore + awakeningScore + deepSleepScore + remSleepScore) as totalScore
        FROM score_components
    """)
    suspend fun calculateSleepQualityScore(sleepId: String): SleepQualityScore?

    /**
     * Calculate sleep quality score as Flow for reactive updates
     * @param sleepId The sleep session ID
     * @return Flow emitting sleep quality score and components
     */
    @Query("""
        WITH sleep_data AS (
            SELECT 
                s.id,
                (julianday(s.endTime) - julianday(s.startTime)) * 24 as durationHours,
                (
                    SELECT (julianday(MIN(ss.startTime)) - julianday(s.startTime)) * 24 * 60
                    FROM sleep_stages ss
                    WHERE ss.sleepId = s.id
                    AND ss.stage != 'AWAKE'
                ) as latencyMinutes,
                (
                    SELECT COUNT(*)
                    FROM sleep_stages ss
                    WHERE ss.sleepId = s.id
                    AND ss.stage = 'AWAKE'
                    AND ss.startTime > (
                        SELECT MIN(ss2.startTime)
                        FROM sleep_stages ss2
                        WHERE ss2.sleepId = s.id
                        AND ss2.stage != 'AWAKE'
                    )
                ) as awakeningCount,
                (
                    SELECT SUM(julianday(ss.endTime) - julianday(ss.startTime)) * 24 * 60
                    FROM sleep_stages ss
                    WHERE ss.sleepId = s.id
                    AND ss.stage = 'DEEP'
                ) as deepSleepMinutes,
                (
                    SELECT SUM(julianday(ss.endTime) - julianday(ss.startTime)) * 24 * 60
                    FROM sleep_stages ss
                    WHERE ss.sleepId = s.id
                    AND ss.stage = 'REM'
                ) as remSleepMinutes,
                (
                    SELECT SUM(julianday(ss.endTime) - julianday(ss.startTime)) * 24 * 60
                    FROM sleep_stages ss
                    WHERE ss.sleepId = s.id
                    AND ss.stage != 'AWAKE'
                ) as totalSleepMinutes
            FROM sleep s
            WHERE s.id = :sleepId
        ),
        score_components AS (
            SELECT 
                id,
                durationHours,
                CASE
                    WHEN durationHours BETWEEN 7 AND 9 THEN 25
                    WHEN durationHours BETWEEN 6 AND 7 OR durationHours BETWEEN 9 AND 10 THEN 15
                    WHEN durationHours BETWEEN 5 AND 6 OR durationHours BETWEEN 10 AND 11 THEN 10
                    ELSE 0
                END as durationScore,
                
                COALESCE(latencyMinutes, 0) as latencyMinutes,
                CASE
                    WHEN latencyMinutes < 15 THEN 20
                    WHEN latencyMinutes BETWEEN 15 AND 30 THEN 15
                    WHEN latencyMinutes BETWEEN 30 AND 60 THEN 10
                    ELSE 0
                END as latencyScore,
                
                COALESCE(awakeningCount, 0) as awakeningCount,
                CASE
                    WHEN awakeningCount <= 1 THEN 15
                    WHEN awakeningCount = 2 THEN 10
                    WHEN awakeningCount = 3 THEN 5
                    ELSE 0
                END as awakeningScore,
                
                COALESCE(deepSleepMinutes, 0) as deepSleepMinutes,
                COALESCE(totalSleepMinutes, 0) as totalSleepMinutes,
                CASE
                    WHEN totalSleepMinutes > 0 THEN (deepSleepMinutes * 100.0 / totalSleepMinutes)
                    ELSE 0
                END as deepSleepPercentage,
                CASE
                    WHEN deepSleepPercentage >= 20 THEN 20
                    WHEN deepSleepPercentage >= 15 THEN 15
                    WHEN deepSleepPercentage >= 10 THEN 10
                    ELSE 0
                END as deepSleepScore,
                
                COALESCE(remSleepMinutes, 0) as remSleepMinutes,
                CASE
                    WHEN totalSleepMinutes > 0 THEN (remSleepMinutes * 100.0 / totalSleepMinutes)
                    ELSE 0
                END as remSleepPercentage,
                CASE
                    WHEN remSleepPercentage >= 20 THEN 20
                    WHEN remSleepPercentage >= 15 THEN 15
                    WHEN remSleepPercentage >= 10 THEN 10
                    ELSE 0
                END as remSleepScore
            FROM sleep_data
        )
        SELECT 
            id as sleepId,
            durationHours,
            durationScore,
            latencyMinutes,
            latencyScore,
            awakeningCount,
            awakeningScore,
            deepSleepMinutes,
            deepSleepPercentage,
            deepSleepScore,
            remSleepMinutes,
            remSleepPercentage,
            remSleepScore,
            (durationScore + latencyScore + awakeningScore + deepSleepScore + remSleepScore) as totalScore
        FROM score_components
    """)
    fun calculateSleepQualityScoreAsFlow(sleepId: String): Flow<SleepQualityScore?>

    /**
     * Get sleep quality scores over time
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return Daily sleep quality scores
     */
    @Query("""
        WITH sleep_sessions AS (
            SELECT 
                s.id,
                date(s.startTime) as date,
                (julianday(s.endTime) - julianday(s.startTime)) * 24 as durationHours,
                (
                    SELECT (julianday(MIN(ss.startTime)) - julianday(s.startTime)) * 24 * 60
                    FROM sleep_stages ss
                    WHERE ss.sleepId = s.id
                    AND ss.stage != 'AWAKE'
                ) as latencyMinutes,
                (
                    SELECT COUNT(*)
                    FROM sleep_stages ss
                    WHERE ss.sleepId = s.id
                    AND ss.stage = 'AWAKE'
                    AND ss.startTime > (
                        SELECT MIN(ss2.startTime)
                        FROM sleep_stages ss2
                        WHERE ss2.sleepId = s.id
                        AND ss2.stage != 'AWAKE'
                    )
                ) as awakeningCount,
                (
                    SELECT SUM(julianday(ss.endTime) - julianday(ss.startTime)) * 24 * 60
                    FROM sleep_stages ss
                    WHERE ss.sleepId = s.id
                    AND ss.stage = 'DEEP'
                ) as deepSleepMinutes,
                (
                    SELECT SUM(julianday(ss.endTime) - julianday(ss.startTime)) * 24 * 60
                    FROM sleep_stages ss
                    WHERE ss.sleepId = s.id
                    AND ss.stage = 'REM'
                ) as remSleepMinutes,
                (
                    SELECT SUM(julianday(ss.endTime) - julianday(ss.startTime)) * 24 * 60
                    FROM sleep_stages ss
                    WHERE ss.sleepId = s.id
                    AND ss.stage != 'AWAKE'
                ) as totalSleepMinutes
            FROM sleep s
            WHERE s.userId = :userId
            AND date(s.startTime) BETWEEN date(:startDate) AND date(:endDate)
        ),
        score_components AS (
            SELECT 
                id,
                date,
                durationHours,
                CASE
                    WHEN durationHours BETWEEN 7 AND 9 THEN 25
                    WHEN durationHours BETWEEN 6 AND 7 OR durationHours BETWEEN 9 AND 10 THEN 15
                    WHEN durationHours BETWEEN 5 AND 6 OR durationHours BETWEEN 10 AND 11 THEN 10
                    ELSE 0
                END as durationScore,
                
                COALESCE(latencyMinutes, 0) as latencyMinutes,
                CASE
                    WHEN latencyMinutes < 15 THEN 20
                    WHEN latencyMinutes BETWEEN 15 AND 30 THEN 15
                    WHEN latencyMinutes BETWEEN 30 AND 60 THEN 10
                    ELSE 0
                END as latencyScore,
                
                COALESCE(awakeningCount, 0) as awakeningCount,
                CASE
                    WHEN awakeningCount <= 1 THEN 15
                    WHEN awakeningCount = 2 THEN 10
                    WHEN awakeningCount = 3 THEN 5
                    ELSE 0
                END as awakeningScore,
                
                COALESCE(deepSleepMinutes, 0) as deepSleepMinutes,
                COALESCE(totalSleepMinutes, 0) as totalSleepMinutes,
                CASE
                    WHEN totalSleepMinutes > 0 THEN (deepSleepMinutes * 100.0 / totalSleepMinutes)
                    ELSE 0
                END as deepSleepPercentage,
                CASE
                    WHEN deepSleepPercentage >= 20 THEN 20
                    WHEN deepSleepPercentage >= 15 THEN 15
                    WHEN deepSleepPercentage >= 10 THEN 10
                    ELSE 0
                END as deepSleepScore,
                
                COALESCE(remSleepMinutes, 0) as remSleepMinutes,
                CASE
                    WHEN totalSleepMinutes > 0 THEN (remSleepMinutes * 100.0 / totalSleepMinutes)
                    ELSE 0
                END as remSleepPercentage,
                CASE
                    WHEN remSleepPercentage >= 20 THEN 20
                    WHEN remSleepPercentage >= 15 THEN 15
                    WHEN remSleepPercentage >= 10 THEN 10
                    ELSE 0
                END as remSleepScore
            FROM sleep_sessions
        ),
        daily_scores AS (
            SELECT 
                date,
                AVG(durationScore + latencyScore + awakeningScore + deepSleepScore + remSleepScore) as totalScore,
                AVG(durationScore) as avgDurationScore,
                AVG(latencyScore) as avgLatencyScore,
                AVG(awakeningScore) as avgAwakeningScore,
                AVG(deepSleepScore) as avgDeepSleepScore,
                AVG(remSleepScore) as avgRemSleepScore
            FROM score_components
            GROUP BY date
        )
        SELECT 
            date,
            totalScore,
            avgDurationScore,
            avgLatencyScore,
            avgAwakeningScore,
            avgDeepSleepScore,
            avgRemSleepScore,
            CASE
                WHEN totalScore >= 80 THEN 'Excellent'
                WHEN totalScore >= 60 THEN 'Good'
                WHEN totalScore >= 40 THEN 'Fair'
                ELSE 'Poor'
            END as qualityCategory
        FROM daily_scores
        ORDER BY date
    """)
    suspend fun getSleepQualityScoresOverTime(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<DailySleepQualityScore>

    /**
     * Bedtime and Wake Time Patterns
     */

    /**
     * Get average bedtime and wake time by day of week
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return Bedtime and wake time by day of week
     */
    @Query("""
        SELECT 
            strftime('%w', startTime) as dayOfWeek,
            AVG(strftime('%H', startTime) * 60 + strftime('%M', startTime)) / 60.0 as avgBedtimeHour,
            AVG(strftime('%H', endTime) * 60 + strftime('%M', endTime)) / 60.0 as avgWaketimeHour,
            AVG((julianday(endTime) - julianday(startTime)) * 24) as avgSleepDurationHours,
            COUNT(*) as sleepCount
        FROM sleep
        WHERE userId = :userId
        AND date(startTime) BETWEEN date(:startDate) AND date(:endDate)
        GROUP BY strftime('%w', startTime)
        ORDER BY dayOfWeek
    """)
    suspend fun getBedtimeWaketimeByDayOfWeek(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<DayOfWeekSleepPattern>

    /**
     * Detect bedtime shift patterns
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @param thresholdHours Threshold for significant bedtime shift
     * @return List of significant bedtime shifts
     */
    @Query("""
        WITH daily_bedtimes AS (
            SELECT 
                date(startTime) as date,
                strftime('%H', startTime) * 60 + strftime('%M', startTime) as bedtimeMinutes
            FROM sleep
            WHERE userId = :userId
            AND date(startTime) BETWEEN date(:startDate) AND date(:endDate)
        ),
        bedtime_pairs AS (
            SELECT 
                d1.date as date1,
                d2.date as date2,
                d1.bedtimeMinutes as bedtime1,
                d2.bedtimeMinutes as bedtime2,
                (d2.bedtimeMinutes - d1.bedtimeMinutes) / 60.0 as shift_hours
            FROM daily_bedtimes d1
            JOIN daily_bedtimes d2 ON d2.date = date(d1.date, '+1 day')
        )
        SELECT 
            date1 as date,
            bedtime1 / 60.0 as bedtimeHour1,
            bedtime2 / 60.0 as bedtimeHour2,
            shift_hours
        FROM bedtime_pairs
        WHERE ABS(shift_hours) >= :thresholdHours
        ORDER BY ABS(shift_hours) DESC
    """)
    suspend fun detectBedtimeShifts(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate,
        thresholdHours: Double = 1.5
    ): List<BedtimeShift>

    /**
     * Sleep Consistency Metrics
     */

    /**
     * Calculate sleep consistency score
     * Score is based on:
     * - Bedtime consistency (lower variability = higher score)
     * - Wake time consistency (lower variability = higher score)
     * - Sleep duration consistency (lower variability = higher score)
     * - Weekday/weekend sleep schedule alignment (smaller difference = higher score)
     * 
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return Sleep consistency score and components
     */
    @Query("""
        WITH sleep_times AS (
            SELECT 
                date(startTime) as date,
                strftime('%w', startTime) as dayOfWeek,
                strftime('%H', startTime) * 60 + strftime('%M', startTime) as bedtimeMinutes,
                strftime('%H', endTime) * 60 + strftime('%M', endTime) as waketimeMinutes,
                (julianday(endTime) - julianday(startTime)) * 24 as sleepDurationHours
            FROM sleep
            WHERE userId = :userId
            AND date(startTime) BETWEEN date(:startDate) AND date(:endDate)
        ),
        weekday_sleep AS (
            SELECT 
                AVG(bedtimeMinutes) as avgBedtimeMinutes,
                AVG(waketimeMinutes) as avgWaketimeMinutes,
                AVG(sleepDurationHours) as avgSleepDurationHours
            FROM sleep_times
            WHERE dayOfWeek NOT IN ('0', '6')
        ),
        weekend_sleep AS (
            SELECT 
                AVG(bedtimeMinutes) as avgBedtimeMinutes,
                AVG(waketimeMinutes) as avgWaketimeMinutes,
                AVG(sleepDurationHours) as avgSleepDurationHours
            FROM sleep_times
            WHERE dayOfWeek IN ('0', '6')
        ),
        variability AS (
            SELECT 
                SQRT(AVG((bedtimeMinutes - AVG(bedtimeMinutes)) * (bedtimeMinutes - AVG(bedtimeMinutes)))) as bedtimeStdDevMinutes,
                SQRT(AVG((waketimeMinutes - AVG(waketimeMinutes)) * (waketimeMinutes - AVG(waketimeMinutes)))) as waketimeStdDevMinutes,
                SQRT(AVG((sleepDurationHours - AVG(sleepDurationHours)) * (sleepDurationHours - AVG(sleepDurationHours)))) as durationStdDevHours
            FROM sleep_times
        )
        SELECT 
            (SELECT bedtimeStdDevMinutes FROM variability) / 60.0 as bedtimeVariabilityHours,
            CASE
                WHEN bedtimeVariabilityHours < 0.5 THEN 25
                WHEN bedtimeVariabilityHours < 1.0 THEN 20
                WHEN bedtimeVariabilityHours < 1.5 THEN 15
                WHEN bedtimeVariabilityHours < 2.0 THEN 10
                ELSE 5
            END as bedtimeConsistencyScore,
            
            (SELECT waketimeStdDevMinutes FROM variability) / 60.0 as waketimeVariabilityHours,
            