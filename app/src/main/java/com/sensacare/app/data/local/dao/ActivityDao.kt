package com.sensacare.app.data.local.dao

import androidx.room.*
import com.sensacare.app.data.local.entity.ActivityEntity
import com.sensacare.app.data.local.entity.ActivitySessionEntity
import com.sensacare.app.data.local.entity.ActivityType
import kotlinx.coroutines.flow.Flow
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * ActivityDao - Specialized Data Access Object for activity metrics
 *
 * This DAO extends the core functionality with activity-specific
 * queries and analysis capabilities, including:
 * - Step counting and goal tracking
 * - Activity type analysis (walking, running, cycling, etc.)
 * - Calorie burn calculations
 * - Distance tracking
 * - Active minutes vs sedentary time
 * - Exercise session detection
 * - Workout summaries and metrics
 * - Movement pattern analysis
 *
 * The ActivityDao provides comprehensive access to activity data for both
 * real-time monitoring and long-term trend analysis.
 */
@Dao
interface ActivityDao {

    /**
     * Basic CRUD Operations
     */

    /**
     * Insert a single activity record
     * @param activity The activity entity to insert
     * @return The row ID of the inserted item
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(activity: ActivityEntity): Long

    /**
     * Insert multiple activity records in a single transaction
     * @param activityList List of activity entities to insert
     * @return List of row IDs for the inserted items
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(activityList: List<ActivityEntity>): List<Long>

    /**
     * Insert a single activity session record
     * @param activitySession The activity session entity to insert
     * @return The row ID of the inserted item
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(activitySession: ActivitySessionEntity): Long

    /**
     * Insert multiple activity session records in a single transaction
     * @param activitySessionList List of activity session entities to insert
     * @return List of row IDs for the inserted items
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSessions(activitySessionList: List<ActivitySessionEntity>): List<Long>

    /**
     * Update an activity record
     * @param activity The activity entity to update
     * @return Number of rows updated
     */
    @Update
    suspend fun update(activity: ActivityEntity): Int

    /**
     * Update an activity session record
     * @param activitySession The activity session entity to update
     * @return Number of rows updated
     */
    @Update
    suspend fun updateSession(activitySession: ActivitySessionEntity): Int

    /**
     * Delete an activity record
     * @param activity The activity entity to delete
     * @return Number of rows deleted
     */
    @Delete
    suspend fun delete(activity: ActivityEntity): Int

    /**
     * Delete an activity session record
     * @param activitySession The activity session entity to delete
     * @return Number of rows deleted
     */
    @Delete
    suspend fun deleteSession(activitySession: ActivitySessionEntity): Int

    /**
     * Basic Queries
     */

    /**
     * Get activity by ID
     * @param id The ID of the activity to retrieve
     * @return The activity entity or null if not found
     */
    @Query("SELECT * FROM activity WHERE id = :id")
    suspend fun getById(id: String): ActivityEntity?

    /**
     * Get activity by ID as Flow for reactive updates
     * @param id The ID of the activity to retrieve
     * @return Flow emitting the activity entity
     */
    @Query("SELECT * FROM activity WHERE id = :id")
    fun getByIdAsFlow(id: String): Flow<ActivityEntity?>

    /**
     * Get activity session by ID
     * @param id The ID of the activity session to retrieve
     * @return The activity session entity or null if not found
     */
    @Query("SELECT * FROM activity_sessions WHERE id = :id")
    suspend fun getSessionById(id: String): ActivitySessionEntity?

    /**
     * Get activity session by ID as Flow for reactive updates
     * @param id The ID of the activity session to retrieve
     * @return Flow emitting the activity session entity
     */
    @Query("SELECT * FROM activity_sessions WHERE id = :id")
    fun getSessionByIdAsFlow(id: String): Flow<ActivitySessionEntity?>

    /**
     * Get all activity records for a specific user
     * @param userId The user ID
     * @return List of activity entities
     */
    @Query("SELECT * FROM activity WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getAllForUser(userId: String): List<ActivityEntity>

    /**
     * Get all activity records for a specific user as Flow for reactive updates
     * @param userId The user ID
     * @return Flow emitting list of activity entities
     */
    @Query("SELECT * FROM activity WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllForUserAsFlow(userId: String): Flow<List<ActivityEntity>>

    /**
     * Get all activity sessions for a specific user
     * @param userId The user ID
     * @return List of activity session entities
     */
    @Query("SELECT * FROM activity_sessions WHERE userId = :userId ORDER BY startTime DESC")
    suspend fun getAllSessionsForUser(userId: String): List<ActivitySessionEntity>

    /**
     * Get all activity sessions for a specific user as Flow for reactive updates
     * @param userId The user ID
     * @return Flow emitting list of activity session entities
     */
    @Query("SELECT * FROM activity_sessions WHERE userId = :userId ORDER BY startTime DESC")
    fun getAllSessionsForUserAsFlow(userId: String): Flow<List<ActivitySessionEntity>>

    /**
     * Get activity records for a specific time range
     * @param userId The user ID
     * @param startTime Start of the time range
     * @param endTime End of the time range
     * @return List of activity entities
     */
    @Query("SELECT * FROM activity WHERE userId = :userId AND timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp")
    suspend fun getByTimeRange(
        userId: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): List<ActivityEntity>

    /**
     * Get activity records for a specific time range as Flow for reactive updates
     * @param userId The user ID
     * @param startTime Start of the time range
     * @param endTime End of the time range
     * @return Flow emitting list of activity entities
     */
    @Query("SELECT * FROM activity WHERE userId = :userId AND timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp")
    fun getByTimeRangeAsFlow(
        userId: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): Flow<List<ActivityEntity>>

    /**
     * Get activity sessions for a specific time range
     * @param userId The user ID
     * @param startTime Start of the time range
     * @param endTime End of the time range
     * @return List of activity session entities
     */
    @Query("""
        SELECT * FROM activity_sessions 
        WHERE userId = :userId 
        AND ((startTime BETWEEN :startTime AND :endTime) 
            OR (endTime BETWEEN :startTime AND :endTime)
            OR (startTime <= :startTime AND endTime >= :endTime))
        ORDER BY startTime
    """)
    suspend fun getSessionsByTimeRange(
        userId: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): List<ActivitySessionEntity>

    /**
     * Get activity sessions for a specific time range as Flow for reactive updates
     * @param userId The user ID
     * @param startTime Start of the time range
     * @param endTime End of the time range
     * @return Flow emitting list of activity session entities
     */
    @Query("""
        SELECT * FROM activity_sessions 
        WHERE userId = :userId 
        AND ((startTime BETWEEN :startTime AND :endTime) 
            OR (endTime BETWEEN :startTime AND :endTime)
            OR (startTime <= :startTime AND endTime >= :endTime))
        ORDER BY startTime
    """)
    fun getSessionsByTimeRangeAsFlow(
        userId: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): Flow<List<ActivitySessionEntity>>

    /**
     * Get the latest activity record
     * @param userId The user ID
     * @return The latest activity entity or null if not found
     */
    @Query("SELECT * FROM activity WHERE userId = :userId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(userId: String): ActivityEntity?

    /**
     * Get the latest activity record as Flow for reactive updates
     * @param userId The user ID
     * @return Flow emitting the latest activity entity
     */
    @Query("SELECT * FROM activity WHERE userId = :userId ORDER BY timestamp DESC LIMIT 1")
    fun getLatestAsFlow(userId: String): Flow<ActivityEntity?>

    /**
     * Step Counting and Goal Tracking
     */

    /**
     * Get daily step counts
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return List of daily step counts
     */
    @Query("""
        SELECT 
            date(timestamp) as date,
            SUM(steps) as totalSteps
        FROM activity
        WHERE userId = :userId
        AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
        GROUP BY date(timestamp)
        ORDER BY date(timestamp)
    """)
    suspend fun getDailyStepCounts(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<DailySteps>

    /**
     * Get daily step counts as Flow for reactive updates
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return Flow emitting list of daily step counts
     */
    @Query("""
        SELECT 
            date(timestamp) as date,
            SUM(steps) as totalSteps
        FROM activity
        WHERE userId = :userId
        AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
        GROUP BY date(timestamp)
        ORDER BY date(timestamp)
    """)
    fun getDailyStepCountsAsFlow(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<DailySteps>>

    /**
     * Get today's step count
     * @param userId The user ID
     * @return Today's total step count
     */
    @Query("""
        SELECT COALESCE(SUM(steps), 0) as totalSteps
        FROM activity
        WHERE userId = :userId
        AND date(timestamp) = date('now')
    """)
    suspend fun getTodayStepCount(userId: String): Int

    /**
     * Get today's step count as Flow for reactive updates
     * @param userId The user ID
     * @return Flow emitting today's total step count
     */
    @Query("""
        SELECT COALESCE(SUM(steps), 0) as totalSteps
        FROM activity
        WHERE userId = :userId
        AND date(timestamp) = date('now')
    """)
    fun getTodayStepCountAsFlow(userId: String): Flow<Int>

    /**
     * Get step count for a specific date
     * @param userId The user ID
     * @param date The date to get step count for
     * @return Total step count for the specified date
     */
    @Query("""
        SELECT COALESCE(SUM(steps), 0) as totalSteps
        FROM activity
        WHERE userId = :userId
        AND date(timestamp) = date(:date)
    """)
    suspend fun getStepCountForDate(userId: String, date: LocalDate): Int

    /**
     * Get step count for a specific date as Flow for reactive updates
     * @param userId The user ID
     * @param date The date to get step count for
     * @return Flow emitting total step count for the specified date
     */
    @Query("""
        SELECT COALESCE(SUM(steps), 0) as totalSteps
        FROM activity
        WHERE userId = :userId
        AND date(timestamp) = date(:date)
    """)
    fun getStepCountForDateAsFlow(userId: String, date: LocalDate): Flow<Int>

    /**
     * Get step count progress towards goal
     * @param userId The user ID
     * @param date The date to check progress for
     * @param goalSteps The step count goal
     * @return Step count progress metrics
     */
    @Query("""
        SELECT 
            COALESCE(SUM(steps), 0) as currentSteps,
            :goalSteps as goalSteps,
            (COALESCE(SUM(steps), 0) * 100.0 / :goalSteps) as percentComplete,
            MAX(:goalSteps - COALESCE(SUM(steps), 0), 0) as stepsRemaining
        FROM activity
        WHERE userId = :userId
        AND date(timestamp) = date(:date)
    """)
    suspend fun getStepCountProgress(
        userId: String,
        date: LocalDate = LocalDate.now(),
        goalSteps: Int = 10000
    ): StepGoalProgress

    /**
     * Get step count progress towards goal as Flow for reactive updates
     * @param userId The user ID
     * @param date The date to check progress for
     * @param goalSteps The step count goal
     * @return Flow emitting step count progress metrics
     */
    @Query("""
        SELECT 
            COALESCE(SUM(steps), 0) as currentSteps,
            :goalSteps as goalSteps,
            (COALESCE(SUM(steps), 0) * 100.0 / :goalSteps) as percentComplete,
            MAX(:goalSteps - COALESCE(SUM(steps), 0), 0) as stepsRemaining
        FROM activity
        WHERE userId = :userId
        AND date(timestamp) = date(:date)
    """)
    fun getStepCountProgressAsFlow(
        userId: String,
        date: LocalDate = LocalDate.now(),
        goalSteps: Int = 10000
    ): Flow<StepGoalProgress>

    /**
     * Get step count goal achievement history
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @param goalSteps The step count goal
     * @return List of daily goal achievement status
     */
    @Query("""
        WITH daily_steps AS (
            SELECT 
                date(timestamp) as date,
                SUM(steps) as totalSteps
            FROM activity
            WHERE userId = :userId
            AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
            GROUP BY date(timestamp)
        )
        SELECT 
            date,
            totalSteps,
            :goalSteps as goalSteps,
            (totalSteps * 100.0 / :goalSteps) as percentComplete,
            CASE WHEN totalSteps >= :goalSteps THEN 1 ELSE 0 END as goalAchieved
        FROM daily_steps
        ORDER BY date
    """)
    suspend fun getStepGoalAchievementHistory(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate,
        goalSteps: Int = 10000
    ): List<DailyStepGoalStatus>

    /**
     * Get step count by hour of day
     * @param userId The user ID
     * @param date The date to analyze
     * @return List of hourly step counts
     */
    @Query("""
        SELECT 
            strftime('%H', timestamp) as hour,
            SUM(steps) as steps
        FROM activity
        WHERE userId = :userId
        AND date(timestamp) = date(:date)
        GROUP BY strftime('%H', timestamp)
        ORDER BY hour
    """)
    suspend fun getHourlyStepCount(userId: String, date: LocalDate): List<HourlySteps>

    /**
     * Get average step count by day of week
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return List of average step counts by day of week
     */
    @Query("""
        SELECT 
            strftime('%w', timestamp) as dayOfWeek,
            AVG(daily_total) as avgSteps
        FROM (
            SELECT 
                timestamp,
                SUM(steps) as daily_total
            FROM activity
            WHERE userId = :userId
            AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
            GROUP BY date(timestamp)
        )
        GROUP BY strftime('%w', timestamp)
        ORDER BY dayOfWeek
    """)
    suspend fun getAverageStepsByDayOfWeek(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<DayOfWeekSteps>

    /**
     * Get step count streak information
     * @param userId The user ID
     * @param goalSteps The step count goal
     * @return Current streak information
     */
    @Query("""
        WITH daily_steps AS (
            SELECT 
                date(timestamp) as date,
                SUM(steps) as totalSteps
            FROM activity
            WHERE userId = :userId
            GROUP BY date(timestamp)
            ORDER BY date(timestamp) DESC
        ),
        goal_status AS (
            SELECT 
                date,
                CASE WHEN totalSteps >= :goalSteps THEN 1 ELSE 0 END as goalAchieved
            FROM daily_steps
        ),
        streak_data AS (
            SELECT 
                date,
                goalAchieved,
                (
                    SELECT COUNT(*)
                    FROM goal_status gs2
                    WHERE gs2.goalAchieved = 1
                    AND gs2.date <= gs1.date
                    AND gs2.date > (
                        SELECT MAX(gs3.date)
                        FROM goal_status gs3
                        WHERE gs3.goalAchieved = 0
                        AND gs3.date <= gs1.date
                    )
                ) as current_streak
            FROM goal_status gs1
            WHERE date = (SELECT MAX(date) FROM goal_status)
        )
        SELECT 
            date as lastActiveDate,
            goalAchieved as lastDayAchieved,
            CASE 
                WHEN goalAchieved = 1 THEN current_streak 
                ELSE 0 
            END as currentStreak,
            (
                SELECT MAX(current_streak)
                FROM streak_data
            ) as longestStreak
        FROM streak_data
        LIMIT 1
    """)
    suspend fun getStepCountStreak(
        userId: String,
        goalSteps: Int = 10000
    ): StepStreak?

    /**
     * Activity Type Analysis
     */

    /**
     * Get activity distribution by type
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return Distribution of activity by type
     */
    @Query("""
        SELECT 
            activityType,
            COUNT(*) as sessionCount,
            SUM((julianday(endTime) - julianday(startTime)) * 24 * 60) as totalMinutes,
            AVG((julianday(endTime) - julianday(startTime)) * 24 * 60) as avgDurationMinutes,
            SUM(caloriesBurned) as totalCalories,
            SUM(distanceMeters) as totalDistanceMeters
        FROM activity_sessions
        WHERE userId = :userId
        AND date(startTime) BETWEEN date(:startDate) AND date(:endDate)
        GROUP BY activityType
        ORDER BY totalMinutes DESC
    """)
    suspend fun getActivityDistributionByType(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<ActivityTypeDistribution>

    /**
     * Get activity sessions by type
     * @param userId The user ID
     * @param activityType The activity type to filter by
     * @param startDate Start date
     * @param endDate End date
     * @return List of activity sessions of the specified type
     */
    @Query("""
        SELECT * 
        FROM activity_sessions
        WHERE userId = :userId
        AND activityType = :activityType
        AND date(startTime) BETWEEN date(:startDate) AND date(:endDate)
        ORDER BY startTime DESC
    """)
    suspend fun getActivitySessionsByType(
        userId: String,
        activityType: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<ActivitySessionEntity>

    /**
     * Get activity type trends over time
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return Weekly activity type distribution
     */
    @Query("""
        WITH week_numbers AS (
            SELECT 
                startTime,
                activityType,
                (julianday(endTime) - julianday(startTime)) * 24 * 60 as durationMinutes,
                strftime('%Y-%W', startTime) as yearWeek
            FROM activity_sessions
            WHERE userId = :userId
            AND date(startTime) BETWEEN date(:startDate) AND date(:endDate)
        )
        SELECT 
            yearWeek,
            activityType,
            SUM(durationMinutes) as totalMinutes,
            COUNT(*) as sessionCount
        FROM week_numbers
        GROUP BY yearWeek, activityType
        ORDER BY yearWeek, totalMinutes DESC
    """)
    suspend fun getActivityTypeTrends(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<WeeklyActivityTypeDistribution>

    /**
     * Get most frequent activity types
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @param limit Maximum number of activity types to return
     * @return List of most frequent activity types
     */
    @Query("""
        SELECT 
            activityType,
            COUNT(*) as sessionCount,
            SUM((julianday(endTime) - julianday(startTime)) * 24 * 60) as totalMinutes
        FROM activity_sessions
        WHERE userId = :userId
        AND date(startTime) BETWEEN date(:startDate) AND date(:endDate)
        GROUP BY activityType
        ORDER BY sessionCount DESC
        LIMIT :limit
    """)
    suspend fun getMostFrequentActivityTypes(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate,
        limit: Int = 5
    ): List<FrequentActivityType>

    /**
     * Get activity intensity distribution
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return Distribution of activity by intensity
     */
    @Query("""
        SELECT 
            intensity,
            COUNT(*) as sessionCount,
            SUM((julianday(endTime) - julianday(startTime)) * 24 * 60) as totalMinutes,
            AVG((julianday(endTime) - julianday(startTime)) * 24 * 60) as avgDurationMinutes,
            SUM(caloriesBurned) as totalCalories
        FROM activity_sessions
        WHERE userId = :userId
        AND date(startTime) BETWEEN date(:startDate) AND date(:endDate)
        GROUP BY intensity
        ORDER BY 
            CASE intensity
                WHEN 'LOW' THEN 1
                WHEN 'MODERATE' THEN 2
                WHEN 'HIGH' THEN 3
                ELSE 4
            END
    """)
    suspend fun getActivityIntensityDistribution(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<ActivityIntensityDistribution>

    /**
     * Calorie Burn Calculations
     */

    /**
     * Get daily calorie burn
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return List of daily calorie burn
     */
    @Query("""
        SELECT 
            date(timestamp) as date,
            SUM(caloriesBurned) as totalCalories
        FROM activity
        WHERE userId = :userId
        AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
        GROUP BY date(timestamp)
        ORDER BY date(timestamp)
    """)
    suspend fun getDailyCalorieBurn(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<DailyCalories>

    /**
     * Get daily calorie burn as Flow for reactive updates
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return Flow emitting list of daily calorie burn
     */
    @Query("""
        SELECT 
            date(timestamp) as date,
            SUM(caloriesBurned) as totalCalories
        FROM activity
        WHERE userId = :userId
        AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
        GROUP BY date(timestamp)
        ORDER BY date(timestamp)
    """)
    fun getDailyCalorieBurnAsFlow(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<DailyCalories>>

    /**
     * Get today's calorie burn
     * @param userId The user ID
     * @return Today's total calorie burn
     */
    @Query("""
        SELECT COALESCE(SUM(caloriesBurned), 0) as totalCalories
        FROM activity
        WHERE userId = :userId
        AND date(timestamp) = date('now')
    """)
    suspend fun getTodayCalorieBurn(userId: String): Int

    /**
     * Get today's calorie burn as Flow for reactive updates
     * @param userId The user ID
     * @return Flow emitting today's total calorie burn
     */
    @Query("""
        SELECT COALESCE(SUM(caloriesBurned), 0) as totalCalories
        FROM activity
        WHERE userId = :userId
        AND date(timestamp) = date('now')
    """)
    fun getTodayCalorieBurnAsFlow(userId: String): Flow<Int>

    /**
     * Get calorie burn by activity type
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return Calorie burn distribution by activity type
     */
    @Query("""
        SELECT 
            activityType,
            SUM(caloriesBurned) as totalCalories,
            AVG(caloriesBurned) as avgCaloriesPerSession,
            COUNT(*) as sessionCount
        FROM activity_sessions
        WHERE userId = :userId
        AND date(startTime) BETWEEN date(:startDate) AND date(:endDate)
        GROUP BY activityType
        ORDER BY totalCalories DESC
    """)
    suspend fun getCalorieBurnByActivityType(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<ActivityTypeCalories>

    /**
     * Get calorie burn rate by activity type
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return Calorie burn rate by activity type
     */
    @Query("""
        SELECT 
            activityType,
            SUM(caloriesBurned) as totalCalories,
            SUM((julianday(endTime) - julianday(startTime)) * 24 * 60) as totalMinutes,
            CASE 
                WHEN SUM((julianday(endTime) - julianday(startTime)) * 24 * 60) > 0 
                THEN SUM(caloriesBurned) / SUM((julianday(endTime) - julianday(startTime)) * 24 * 60)
                ELSE 0
            END as caloriesPerMinute
        FROM activity_sessions
        WHERE userId = :userId
        AND date(startTime) BETWEEN date(:startDate) AND date(:endDate)
        GROUP BY activityType
        ORDER BY caloriesPerMinute DESC
    """)
    suspend fun getCalorieBurnRateByActivityType(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<ActivityTypeCalorieRate>

    /**
     * Get calorie burn progress towards goal
     * @param userId The user ID
     * @param date The date to check progress for
     * @param goalCalories The calorie burn goal
     * @return Calorie burn progress metrics
     */
    @Query("""
        SELECT 
            COALESCE(SUM(caloriesBurned), 0) as currentCalories,
            :goalCalories as goalCalories,
            (COALESCE(SUM(caloriesBurned), 0) * 100.0 / :goalCalories) as percentComplete,
            MAX(:goalCalories - COALESCE(SUM(caloriesBurned), 0), 0) as caloriesRemaining
        FROM activity
        WHERE userId = :userId
        AND date(timestamp) = date(:date)
    """)
    suspend fun getCalorieBurnProgress(
        userId: String,
        date: LocalDate = LocalDate.now(),
        goalCalories: Int = 500
    ): CalorieGoalProgress

    /**
     * Get calorie burn progress towards goal as Flow for reactive updates
     * @param userId The user ID
     * @param date The date to check progress for
     * @param goalCalories The calorie burn goal
     * @return Flow emitting calorie burn progress metrics
     */
    @Query("""
        SELECT 
            COALESCE(SUM(caloriesBurned), 0) as currentCalories,
            :goalCalories as goalCalories,
            (COALESCE(SUM(caloriesBurned), 0) * 100.0 / :goalCalories) as percentComplete,
            MAX(:goalCalories - COALESCE(SUM(caloriesBurned), 0), 0) as caloriesRemaining
        FROM activity
        WHERE userId = :userId
        AND date(timestamp) = date(:date)
    """)
    fun getCalorieBurnProgressAsFlow(
        userId: String,
        date: LocalDate = LocalDate.now(),
        goalCalories: Int = 500
    ): Flow<CalorieGoalProgress>

    /**
     * Distance Tracking
     */

    /**
     * Get daily distance
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return List of daily distances
     */
    @Query("""
        SELECT 
            date(timestamp) as date,
            SUM(distanceMeters) as totalDistanceMeters
        FROM activity
        WHERE userId = :userId
        AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
        GROUP BY date(timestamp)
        ORDER BY date(timestamp)
    """)
    suspend fun getDailyDistance(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<DailyDistance>

    /**
     * Get daily distance as Flow for reactive updates
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return Flow emitting list of daily distances
     */
    @Query("""
        SELECT 
            date(timestamp) as date,
            SUM(distanceMeters) as totalDistanceMeters
        FROM activity
        WHERE userId = :userId
        AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
        GROUP BY date(timestamp)
        ORDER BY date(timestamp)
    """)
    fun getDailyDistanceAsFlow(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<DailyDistance>>

    /**
     * Get today's distance
     * @param userId The user ID
     * @return Today's total distance in meters
     */
    @Query("""
        SELECT COALESCE(SUM(distanceMeters), 0) as totalDistanceMeters
        FROM activity
        WHERE userId = :userId
        AND date(timestamp) = date('now')
    """)
    suspend fun getTodayDistance(userId: String): Float

    /**
     * Get today's distance as Flow for reactive updates
     * @param userId The user ID
     * @return Flow emitting today's total distance in meters
     */
    @Query("""
        SELECT COALESCE(SUM(distanceMeters), 0) as totalDistanceMeters
        FROM activity
        WHERE userId = :userId
        AND date(timestamp) = date('now')
    """)
    fun getTodayDistanceAsFlow(userId: String): Flow<Float>

    /**
     * Get distance by activity type
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return Distance distribution by activity type
     */
    @Query("""
        SELECT 
            activityType,
            SUM(distanceMeters) as totalDistanceMeters,
            AVG(distanceMeters) as avgDistancePerSession,
            COUNT(*) as sessionCount
        FROM activity_sessions
        WHERE userId = :userId
        AND date(startTime) BETWEEN date(:startDate) AND date(:endDate)
        GROUP BY activityType
        ORDER BY totalDistanceMeters DESC
    """)
    suspend fun getDistanceByActivityType(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<ActivityTypeDistance>

    /**
     * Get distance progress towards goal
     * @param userId The user ID
     * @param date The date to check progress for
     * @param goalDistanceMeters The distance goal in meters
     * @return Distance progress metrics
     */
    @Query("""
        SELECT 
            COALESCE(SUM(distanceMeters), 0) as currentDistanceMeters,
            :goalDistanceMeters as goalDistanceMeters,
            (COALESCE(SUM(distanceMeters), 0) * 100.0 / :goalDistanceMeters) as percentComplete,
            MAX(:goalDistanceMeters - COALESCE(SUM(distanceMeters), 0), 0) as distanceRemainingMeters
        FROM activity
        WHERE userId = :userId
        AND date(timestamp) = date(:date)
    """)
    suspend fun getDistanceProgress(
        userId: String,
        date: LocalDate = LocalDate.now(),
        goalDistanceMeters: Float = 5000f
    ): DistanceGoalProgress

    /**
     * Get distance progress towards goal as Flow for reactive updates
     * @param userId The user ID
     * @param date The date to check progress for
     * @param goalDistanceMeters The distance goal in meters
     * @return Flow emitting distance progress metrics
     */
    @Query("""
        SELECT 
            COALESCE(SUM(distanceMeters), 0) as currentDistanceMeters,
            :goalDistanceMeters as goalDistanceMeters,
            (COALESCE(SUM(distanceMeters), 0) * 100.0 / :goalDistanceMeters) as percentComplete,
            MAX(:goalDistanceMeters - COALESCE(SUM(distanceMeters), 0), 0) as distanceRemainingMeters
        FROM activity
        WHERE userId = :userId
        AND date(timestamp) = date(:date)
    """)
    fun getDistanceProgressAsFlow(
        userId: String,
        date: LocalDate = LocalDate.now(),
        goalDistanceMeters: Float = 5000f
    ): Flow<DistanceGoalProgress>

    /**
     * Get pace/speed statistics by activity type
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return Pace/speed statistics by activity type
     */
    @Query("""
        SELECT 
            activityType,
            AVG(
                CASE 
                    WHEN distanceMeters > 0 AND (julianday(endTime) - julianday(startTime)) * 24 * 60 * 60 > 0
                    THEN distanceMeters / ((julianday(endTime) - julianday(startTime)) * 24 * 60 * 60)
                    ELSE 0
                END
            ) as avgSpeedMetersPerSecond,
            MAX(
                CASE 
                    WHEN distanceMeters > 0 AND (julianday(endTime) - julianday(startTime)) * 24 * 60 * 60 > 0
                    THEN distanceMeters / ((julianday(endTime) - julianday(startTime)) * 24 * 60 * 60)
                    ELSE 0
                END
            ) as maxSpeedMetersPerSecond,
            AVG(
                CASE 
                    WHEN distanceMeters > 0
                    THEN ((julianday(endTime) - julianday(startTime)) * 24 * 60) / (distanceMeters / 1000)
                    ELSE 0
                END
            ) as avgPaceMinutesPerKm
        FROM activity_sessions
        WHERE userId = :userId
        AND date(startTime) BETWEEN date(:startDate) AND date(:endDate)
        AND distanceMeters > 0
        GROUP BY activityType
        ORDER BY avgSpeedMetersPerSecond DESC
    """)
    suspend fun getPaceStatisticsByActivityType(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<ActivityTypePace>

    /**
     * Active Minutes vs Sedentary Time
     */

    /**
     * Get daily active minutes
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return List of daily active minutes
     */
    @Query("""
        SELECT 
            date(timestamp) as date,
            SUM(activeMinutes) as totalActiveMinutes
        FROM activity
        WHERE userId = :userId
        AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
        GROUP BY date(timestamp)
        ORDER BY date(timestamp)
    """)
    suspend fun getDailyActiveMinutes(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<DailyActiveMinutes>

    /**
     * Get daily active minutes as Flow for reactive updates
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return Flow emitting list of daily active minutes
     */
    @Query("""
        SELECT 
            date(timestamp) as date,
            SUM(activeMinutes) as totalActiveMinutes
        FROM activity
        WHERE userId = :userId
        AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
        GROUP BY date(timestamp)
        ORDER BY date(timestamp)
    """)
    fun getDailyActiveMinutesAsFlow(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<DailyActiveMinutes>>

    /**
     * Get today's active minutes
     * @param userId The user ID
     * @return Today's total active minutes
     */
    @Query("""
        SELECT COALESCE(SUM(activeMinutes), 0) as totalActiveMinutes
        FROM activity
        WHERE userId = :userId
        AND date(timestamp) = date('now')
    """)
    suspend fun getTodayActiveMinutes(userId: String): Int

    /**
     * Get today's active minutes as Flow for reactive updates
     * @param userId The user ID
     * @return Flow emitting today's total active minutes
     */
    @Query("""
        SELECT COALESCE(SUM(activeMinutes), 0) as totalActiveMinutes
        FROM activity
        WHERE userId = :userId
        AND date(timestamp) = date('now')
    """)
    fun getTodayActiveMinutesAsFlow(userId: String): Flow<Int>

    /**
     * Get active vs sedentary time distribution
     * @param userId The user ID
     * @param date The date to analyze
     * @return Active vs sedentary time distribution
     */
    @Query("""
        WITH time_data AS (
            SELECT 
                SUM(activeMinutes) as totalActiveMinutes
            FROM activity
            WHERE userId = :userId
            AND date(timestamp) = date(:date)
        )
        SELECT 
            (SELECT totalActiveMinutes FROM time_data) as activeMinutes,
            (1440 - (SELECT totalActiveMinutes FROM time_data)) as sedentaryMinutes,
            ((SELECT totalActiveMinutes FROM time_data) * 100.0 / 1440) as activePercentage,
            (100.0 - ((SELECT totalActiveMinutes FROM time_data) * 100.0 / 1440)) as sedentaryPercentage
    """)
    suspend fun getActiveVsSedentaryDistribution(
        userId: String,
        date: LocalDate = LocalDate.now()
    ): ActiveSedentaryDistribution

    /**
     * Get active vs sedentary time distribution as Flow for reactive updates
     * @param userId The user ID
     * @param date The date to analyze
     * @return Flow emitting active vs sedentary time distribution
     */
    @Query("""
        WITH time_data AS (
            SELECT 
                SUM(activeMinutes) as totalActiveMinutes
            FROM activity
            WHERE userId = :userId
            AND date(timestamp) = date(:date)
        )
        SELECT 
            (SELECT totalActiveMinutes FROM time_data) as activeMinutes,
            (1440 - (SELECT totalActiveMinutes FROM time_data)) as sedentaryMinutes,
            ((SELECT totalActiveMinutes FROM time_data) * 100.0 / 1440) as activePercentage,
            (100.0 - ((SELECT totalActiveMinutes FROM time_data) * 100.0 / 1440)) as sedentaryPercentage
    """)
    fun getActiveVsSedentaryDistributionAsFlow(
        userId: String,
        date: LocalDate = LocalDate.now()
    ): Flow<ActiveSedentaryDistribution>

    /**
     * Get hourly active minutes
     * @param userId The user ID
     * @param date The date to analyze
     * @return List of hourly active minutes
     */
    @Query("""
        SELECT 
            strftime('%H', timestamp) as hour,
            SUM(activeMinutes) as activeMinutes
        FROM activity
        WHERE userId = :userId
        AND date(timestamp) = date(:date)
        GROUP BY strftime('%H', timestamp)
        ORDER BY hour
    """)
    suspend fun getHourlyActiveMinutes(userId: String, date: LocalDate): List<HourlyActiveMinutes>

    /**
     * Get active minutes progress towards goal
     * @param userId The user ID
     * @param date The date to check progress for
     * @param goalActiveMinutes The active minutes goal
     * @return Active minutes progress metrics
     */
    @Query("""
        SELECT 
            COALESCE(SUM(activeMinutes), 0) as currentActiveMinutes,
            :goalActiveMinutes as goalActiveMinutes,
            (COALESCE(SUM(activeMinutes), 0) * 100.0 / :goalActiveMinutes) as percentComplete,
            MAX(:goalActiveMinutes - COALESCE(SUM(activeMinutes), 0), 0) as activeMinutesRemaining
        FROM activity
        WHERE userId = :userId
        AND date(timestamp) = date(:date)
    """)
    suspend fun getActiveMinutesProgress(
        userId: String,
        date: LocalDate = LocalDate.now(),
        goalActiveMinutes: Int = 30
    ): ActiveMinutesGoalProgress

    /**
     * Get active minutes progress towards goal as Flow for reactive updates
     * @param userId The user ID
     * @param date The date to check progress for
     * @param goalActiveMinutes The active minutes goal
     * @return Flow emitting active minutes progress metrics
     */
    @Query("""
        SELECT 
            COALESCE(SUM(activeMinutes), 0) as currentActiveMinutes,
            :goalActiveMinutes as goalActiveMinutes,
            (COALESCE(SUM(activeMinutes), 0) * 100.0 / :goalActiveMinutes) as percentComplete,
            MAX(:goalActiveMinutes - COALESCE(SUM(activeMinutes), 0), 0) as activeMinutesRemaining
        FROM activity
        WHERE userId = :userId
        AND date(timestamp) = date(:date)
    """)
    fun getActiveMinutesProgressAsFlow(
        userId: String,
        date: LocalDate = LocalDate.now(),
        goalActiveMinutes: Int = 30
    ): Flow<ActiveMinutesGoalProgress>

    /**
     * Get longest sedentary periods
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @param minDurationMinutes Minimum duration to consider as sedentary period
     * @return List of longest sedentary periods
     */
    @Query("""
        WITH activity_times AS (
            SELECT 
                userId,
                timestamp,
                activeMinutes,
                LAG(timestamp) OVER (PARTITION BY userId, date(timestamp) ORDER BY timestamp) as prev_timestamp,
                LAG(activeMinutes) OVER (PARTITION BY userId, date(timestamp) ORDER BY timestamp) as prev_active_minutes
            FROM activity
            WHERE userId = :userId
            AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
        ),
        sedentary_periods AS (
            SELECT 
                date(prev_timestamp) as date,
                prev_timestamp as periodStart,
                timestamp as periodEnd,
                (julianday(timestamp) - julianday(prev_timestamp)) * 24 * 60 as durationMinutes
            FROM activity_times
            WHERE prev_active_minutes = 0
            AND (julianday(timestamp) - julianday(prev_timestamp)) * 24 * 60 >= :minDurationMinutes
        )
        SELECT 
            date,
            periodStart,
            periodEnd,
            durationMinutes
        FROM sedentary_periods
        ORDER BY durationMinutes DESC
        LIMIT 10
    """)
    suspend fun getLongestSedentaryPeriods(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate,
        minDurationMinutes: Int = 60
    ): List<SedentaryPeriod>

    /**
     * Exercise Session Detection
     */

    /**
     * Detect exercise sessions from activity data
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @param minActiveMinutes Minimum active minutes to consider as exercise
     * @param minSteps Minimum steps to consider as exercise
     * @return List of detected exercise sessions
     */
    @Query("""
        WITH continuous_activity AS (
            SELECT 
                userId,
                timestamp,
                activeMinutes,
                steps,
                caloriesBurned,
                distanceMeters,
                LAG(timestamp) OVER (PARTITION BY userId, date(timestamp) ORDER BY timestamp) as prev_timestamp,
                SUM(
                    CASE 
                        WHEN (julianday(timestamp) - julianday(LAG(timestamp) OVER (PARTITION BY userId, date(timestamp) ORDER BY timestamp))) * 24 * 60 > 15
                        OR LAG(timestamp) OVER (PARTITION BY userId, date(timestamp) ORDER BY timestamp) IS NULL
                        THEN 1 
                        ELSE 0 
                    END
                ) OVER (PARTITION BY userId, date(timestamp) ORDER BY timestamp) as session_group
            FROM activity
            WHERE userId = :userId
            AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
            AND activeMinutes > 0
        ),
        session_aggregates AS (
            SELECT 
                userId,
                date(MIN(timestamp)) as date,
                MIN(timestamp) as startTime,
                MAX(timestamp) as endTime,
                SUM(activeMinutes) as totalActiveMinutes,
                SUM(steps) as totalSteps,
                SUM(caloriesBurned) as totalCaloriesBurned,
                SUM(distanceMeters) as totalDistanceMeters,
                session_group
            FROM continuous_activity
            GROUP BY userId, date(timestamp), session_group
        )
        SELECT 
            date,
            startTime,
            endTime,
            (julianday(endTime) - julianday(startTime)) * 24 * 60 as durationMinutes,
            totalActiveMinutes,
            totalSteps,
            totalCaloriesBurned,
            totalDistanceMeters,
            CASE
                WHEN totalSteps / (julianday(endTime) - julianday(startTime)) * 24 * 60 > 100 THEN 'RUNNING'
                WHEN totalSteps / (julianday(endTime) - julianday(startTime)) * 24 * 60 > 60 THEN 'WALKING'
                ELSE 'OTHER'
            END as detectedActivityType
        FROM session_aggregates
        WHERE totalActiveMinutes >= :minActiveMinutes
        AND totalSteps >= :minSteps
        ORDER BY startTime DESC
    """)
    suspend fun detectExerciseSessions(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate,
        minActiveMinutes: Int = 10,
        minSteps: Int = 1000
    ): List<DetectedExerciseSession>

    /**
     * Get exercise frequency
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @param minDurationMinutes Minimum duration to consider as exercise
     * @return Exercise frequency metrics
     */
    @Query("""
        WITH exercise_days AS (
            SELECT 
                date(startTime) as date,
                COUNT(*) as sessionCount,
                SUM((julianday(endTime) - julianday(startTime)) * 24 * 60) as totalMinutes
            FROM activity_sessions
            WHERE userId = :userId
            AND date(startTime) BETWEEN date(:startDate) AND date(:endDate)
            AND (julianday(endTime) - julianday(startTime)) * 24 * 60 >= :minDurationMinutes
            GROUP BY date(startTime)
        ),
        day_count AS (
            SELECT 
                COUNT(DISTINCT date(startTime)) as totalDays
            FROM activity_sessions
            WHERE userId = :userId
            AND date(startTime) BETWEEN date(:startDate) AND date(:endDate)
        ),
        date_range AS (
            SELECT 
                julianday(:endDate) - julianday(:startDate) + 1 as totalPossibleDays
            FROM activity_sessions
            LIMIT 1
        )
        SELECT 
            COUNT(*) as daysWithExercise,
            (SELECT totalPossibleDays FROM date_range) as totalDays,
            (COUNT(*) * 100.0 / (SELECT totalPossibleDays FROM date_range)) as exerciseFrequencyPercent,
            AVG(sessionCount) as avgSessionsPerExerciseDay,
            AVG(totalMinutes) as avgMinutesPerExerciseDay,
            SUM(totalMinutes) as totalExerciseMinutes
        FROM exercise_days
    """)
    suspend fun getExerciseFrequency(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate,
        minDurationMinutes: Int = 10
    ): ExerciseFrequency

    /**
     * Workout Summaries and Metrics
     */

    /**
     * Get workout summary statistics
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return Workout summary statistics
     */
    @Query("""
        SELECT 
            COUNT(*) as totalWorkouts,
            AVG((julianday(endTime) - julianday(startTime)) * 24 * 60) as avgDurationMinutes,
            MAX((julianday(endTime) - julianday(startTime)) * 24 * 60) as maxDurationMinutes,
            AVG(caloriesBurned) as avgCaloriesBurned,
            MAX(caloriesBurned) as maxCaloriesBurned,
            SUM(caloriesBurned) as totalCaloriesBurned,
            AVG(distanceMeters) as avgDistanceMeters,
            MAX(distanceMeters) as maxDistanceMeters,
            SUM(distanceMeters) as totalDistanceMeters
        FROM activity_sessions
        WHERE userId = :userId
        AND date(startTime) BETWEEN date(:startDate) AND date(:endDate)
    """)
    suspend fun getWorkoutSummaryStatistics(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): WorkoutSummaryStatistics

    /**
     * Get workout trends over time
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return Weekly workout trends
     */
    @Query("""
        SELECT 
            strftime('%Y-%W', startTime) as yearWeek,
            COUNT(*) as workoutCount,
            SUM((julianday(endTime) - julianday(startTime)) * 24 * 60) as totalMinutes,
            AVG((julianday(endTime) - julianday(startTime)) * 24 * 60) as avgDurationMinutes,
            SUM(caloriesBurned) as totalCaloriesBurned,
            SUM(distanceMeters) as totalDistanceMeters
        FROM activity_sessions
        WHERE userId = :userId
        AND date(startTime) BETWEEN date(:startDate) AND date(:endDate)
        GROUP BY strftime('%Y-%W', startTime)
        ORDER BY yearWeek
    """)
    suspend fun getWorkoutTrendsOverTime(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<WeeklyWorkoutTrend>

    /**
     * Get workout intensity distribution
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return Distribution of workouts by intensity
     */
    @Query("""
        SELECT 
            intensity,
            COUNT(*) as workoutCount,
            SUM((julianday(endTime) - julianday(startTime)) * 24 * 60) as totalMinutes,
            AVG((julianday(endTime) - julianday(startTime)) * 24 * 60) as avgDurationMinutes,
            SUM(caloriesBurned) as totalCaloriesBurned,
            AVG(caloriesBurned) as avgCaloriesBurned
        FROM activity_sessions
        WHERE userId = :userId
        AND date(startTime) BETWEEN date(:startDate) AND date(:endDate)
        GROUP BY intensity
        ORDER BY 
            CASE intensity
                WHEN 'LOW' THEN 1
                WHEN 'MODERATE' THEN 2
                WHEN 'HIGH' THEN 3
                ELSE 4
            END
    """)
    suspend fun getWorkoutIntensityDistribution(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<WorkoutIntensityDistribution>

    /**
     * Get workout day of week distribution
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return Distribution of workouts by day of week
     */
    @Query("""
        SELECT 
            strftime('%w', startTime) as dayOfWeek,
            COUNT(*) as workoutCount,
            SUM((julianday(endTime) - julianday(startTime)) * 24 * 60) as totalMinutes,
            AVG((julianday(endTime) - julianday(startTime)) * 24 * 60) as avgDurationMinutes,
            SUM(caloriesBurned) as totalCaloriesBurned
        FROM activity_sessions
        WHERE userId = :userId
        AND date(startTime) BETWEEN date(:startDate) AND date(:endDate)
        GROUP BY strftime('%w', startTime)
        ORDER BY dayOfWeek
    """)
    suspend fun getWorkoutDayOfWeekDistribution(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<DayOfWeekWorkoutDistribution>

    /**
     * Get workout time of day distribution
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return Distribution of workouts by time of day
     */
    @Query("""
        SELECT 
            CASE
                WHEN strftime('%H', startTime) BETWEEN '05' AND '08' THEN 'Early Morning'
                WHEN strftime('%H', startTime) BETWEEN '09' AND '11' THEN 'Morning'
                WHEN strftime('%H', startTime) BETWEEN '12' AND '14' THEN 'Midday'
                WHEN strftime('%H', startTime) BETWEEN '15' AND '17' THEN 'Afternoon'
                WHEN strftime('%H', startTime) BETWEEN '18' AND '20' THEN 'Evening'
                ELSE 'Night'
            END as timeOfDay,
            COUNT(*) as workoutCount,
            AVG((julianday(endTime) - julianday(startTime)) * 24 * 60) as avgDurationMinutes,
            AVG(caloriesBurned) as avgCaloriesBurned
        FROM activity_sessions
        WHERE userId = :userId
        AND date(startTime) BETWEEN date(:startDate) AND date(:endDate)
        GROUP BY timeOfDay
        ORDER BY 
            CASE timeOfDay
                WHEN 'Early Morning' THEN 1
                WHEN 'Morning' THEN 2
                WHEN 'Midday' THEN 3
                WHEN 'Afternoon' THEN 4
                WHEN 'Evening' THEN 5
                WHEN 'Night' THEN 6
            END
    """)
    suspend fun getWorkoutTimeOfDayDistribution(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<TimeOfDayWorkoutDistribution>

    /**
     * Get personal bests/records
     * @param userId The user ID
     * @return Personal best workout metrics
     */
    @Query("""
        SELECT 
            (SELECT id FROM activity_sessions WHERE userId = :userId ORDER BY caloriesBurned DESC LIMIT 1) as highestCalorieWorkoutId,
            (SELECT caloriesBurned FROM activity_sessions WHERE userId = :userId ORDER BY caloriesBurned DESC LIMIT 1) as highestCalorieBurn,
            (SELECT id FROM activity_sessions WHERE userId = :userId ORDER BY distanceMeters DESC LIMIT 1) as longestDistanceWorkoutId,
            (SELECT distanceMeters FROM activity_sessions WHERE userId = :userId ORDER BY distanceMeters DESC LIMIT 1) as longestDistance,
            (SELECT id FROM activity_sessions WHERE userId = :userId ORDER BY (julianday(endTime) - julianday(startTime)) * 24 * 60 DESC LIMIT 1) as longestDurationWorkoutId,
            (SELECT (julianday(endTime) - julianday(startTime)) * 24 * 60 FROM activity_sessions WHERE userId = :userId ORDER BY (julianday(endTime) - julianday(startTime)) * 24 * 60 DESC LIMIT 1) as longestDurationMinutes,
            (
                SELECT id FROM activity_sessions 
                WHERE userId = :userId AND distanceMeters > 0 
                ORDER BY (distanceMeters / ((julianday(endTime) - julianday(startTime)) * 24 * 60 * 60)) DESC LIMIT 1
            ) as fastestPaceWorkoutId,
            (
                SELECT (distanceMeters / ((julianday(endTime) - julianday(startTime)) * 24 * 60 * 60)) 
                FROM activity_sessions 
                WHERE userId = :userId AND distanceMeters > 0 
                ORDER BY (distanceMeters / ((julianday(endTime) - julianday(startTime)) * 24 * 60 * 60)) DESC LIMIT 1
            ) as fastestPaceMetersPerSecond
    """)
    suspend fun getPersonalBests(userId: String): PersonalBests

    /**
     * Movement Pattern Analysis
     */

    /**
     * Get movement patterns by hour of day
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return Hourly movement patterns
     */
    @Query("""
        SELECT 
            strftime('%H', timestamp) as hour,
            AVG(steps) as avgSteps,
            AVG(activeMinutes) as avgActiveMinutes,
            AVG(caloriesBurned) as avgCaloriesBurned,
            COUNT(*) as sampleCount
        FROM activity
        WHERE userId = :userId
        AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
        GROUP BY strftime('%H', timestamp)
        ORDER BY hour
    """)
    suspend fun getMovementPatternsByHour(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<HourlyMovementPattern>

    /**
     * Get movement patterns by day of week
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return Daily movement patterns by day of week
     */
    @Query("""
        WITH daily_totals AS (
            SELECT 
                date(timestamp) as date,
                strftime('%w', timestamp) as dayOfWeek,
                SUM(steps) as totalSteps,
                SUM(activeMinutes) as totalActiveMinutes,
                SUM(caloriesBurned) as totalCaloriesBurned,
                SUM(distanceMeters) as totalDistanceMeters
            FROM activity
            WHERE userId = :userId
            AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
            GROUP BY date(timestamp)
        )
        SELECT 
            dayOfWeek,
            AVG(totalSteps) as avgSteps,
            AVG(totalActiveMinutes) as avgActiveMinutes,
            AVG(totalCaloriesBurned) as avgCaloriesBurned,
            AVG(totalDistanceMeters) as avgDistanceMeters,
            COUNT(*) as dayCount
        FROM daily_totals
        GROUP BY dayOfWeek
        ORDER BY dayOfWeek
    """)
    suspend fun getMovementPatternsByDayOfWeek(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<DayOfWeekMovementPattern>

    /**
     * Detect activity pattern changes
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @param thresholdPercent Threshold for significant change in activity
     * @return List of significant activity pattern changes
     */
    @Query("""
        WITH daily_activity AS (
            SELECT 
                date(timestamp) as date,
                SUM(steps) as totalSteps,
                SUM(activeMinutes) as totalActiveMinutes,
                SUM(caloriesBurned) as totalCaloriesBurned
            FROM activity
            WHERE userId = :userId
            AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
            GROUP BY date(timestamp)
        ),
        moving_avg AS (
            SELECT 
                date,
                totalSteps,
                totalActiveMinutes,
                totalCaloriesBurned,
                AVG(totalSteps) OVER (
                    ORDER BY date
                    ROWS BETWEEN 7 PRECEDING AND 1 PRECEDING
                ) as avg_steps_previous_week,
                AVG(totalActiveMinutes) OVER (
                    ORDER BY date
                    ROWS BETWEEN 7 PRECEDING AND 1 PRECEDING
                ) as avg_active_minutes_previous_week,
                AVG(totalCaloriesBurned) OVER (
                    ORDER BY date
                    ROWS BETWEEN 7 PRECEDING AND 1 PRECEDING
                ) as avg_calories_previous_week
            FROM daily_activity
        )
        SELECT 
            date,
            totalSteps,
            avg_steps_previous_week,
            ((totalSteps - avg_steps_previous_week) * 100.0 / avg_steps_previous_week) as steps_percent_change,
            totalActiveMinutes,
            avg_active_minutes_previous_week,
            ((totalActiveMinutes - avg_active_minutes_previous_week) * 100.0 / avg_active_minutes_previous_week) as active_minutes_percent_change,
            totalCaloriesBurned,
            avg_calories_previous_week,
            ((totalCaloriesBurned - avg_calories_previous_week) * 100.0 / avg_calories_previous_week) as calories_percent_change
        FROM moving_avg
        WHERE ABS((totalSteps - avg_steps_previous_week) * 100.0 / avg_steps_previous_week) >= :thresholdPercent
        OR ABS((totalActiveMinutes - avg_active_minutes_previous_week) * 100.0 / avg_active_minutes_previous_week) >= :thresholdPercent
        OR ABS((totalCaloriesBurned - avg_calories_previous_week) * 100.0 / avg_calories_previous_week) >= :thresholdPercent
        AND avg_steps_previous_week IS NOT NULL
        ORDER BY date
    """)
    suspend fun detectActivityPatternChanges(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate,
        thresholdPercent: Double = 30.0
    ): List<ActivityPatternChange>

    /**
     * Detect activity level changes by time of day
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return Activity level changes by time of day
     */
    @Query("""
        WITH hourly_activity AS (
            SELECT 
                strftime('%H', timestamp) as hour,
                AVG(steps) as avgSteps,
                AVG(activeMinutes) as avgActiveMinutes
            FROM activity
            WHERE userId = :userId
            AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
            GROUP BY strftime('%H', timestamp)
        )
        SELECT 
            hour,
            avgSteps,
            avgActiveMinutes,
            LAG(avgSteps) OVER (ORDER BY hour) as prevHourSteps,
            LAG(avgActiveMinutes) OVER (ORDER BY hour) as prevHourActiveMinutes,
            (avgSteps - LAG(avgSteps) OVER (ORDER BY hour)) as stepsDifference,
            (avgActiveMinutes - LAG(avgActiveMinutes) OVER (ORDER BY hour)) as activeMinutesDifference,
            CASE 
                WHEN avgSteps > LAG(avgSteps) OVER (ORDER BY hour) * 2 THEN 'Sharp Increase'
                WHEN avgSteps > LAG(avgSteps) OVER (ORDER BY hour) * 1.5 THEN 'Moderate Increase'
                WHEN avgSteps > LAG(avgSteps) OVER (ORDER BY hour) * 1.2 THEN 'Slight Increase'
                WHEN avgSteps < LAG(avgSteps) OVER (ORDER BY hour) * 0.5 THEN 'Sharp Decrease'
                WHEN avgSteps < LAG(avgSteps) OVER (ORDER BY hour) * 0.7 THEN 'Moderate Decrease'
                WHEN avgSteps < LAG(avgSteps) OVER (ORDER BY hour) * 0.8 THEN 'Slight Decrease'
                ELSE 'Stable'
            END as activityChangePattern
        FROM hourly_activity
        WHERE prevHourSteps IS NOT NULL
        ORDER BY hour
    """)
    suspend fun detectActivityLevelChangesByTimeOfDay(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<HourlyActivityChange>

    /**
     * Get activity consistency score
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return Activity consistency metrics
     */
    @Query("""
        WITH daily_activity AS (
            SELECT 
                date(timestamp) as date,
                SUM(steps) as totalSteps,
                SUM(activeMinutes) as totalActiveMinutes,
                SUM(caloriesBurned) as totalCaloriesBurned
            FROM activity
            WHERE userId = :userId
            AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
            GROUP BY date(timestamp)
        ),
        activity_stats AS (
            SELECT 
                AVG(totalSteps) as avgSteps,
                SQRT(AVG((totalSteps - AVG(totalSteps)) * (totalSteps - AVG(totalSteps)))) / AVG(totalSteps) * 100 as stepsCoeffOfVariation,
                AVG(totalActiveMinutes) as avgActiveMinutes,
                SQRT(AVG((totalActiveMinutes - AVG(totalActiveMinutes)) * (totalActiveMinutes - AVG(totalActiveMinutes)))) / AVG(totalActiveMinutes) * 100 as activeMinutesCoeffOfVariation,
                COUNT(*) as totalDays,
                (
                    SELECT COUNT(*)
                    FROM daily_activity
                    WHERE totalSteps > 1000
                ) as daysWithSignificantActivity
            FROM daily_activity
        )
        SELECT 
            avgSteps,
            stepsCoeffOfVariation,
            avgActiveMinutes,
            activeMinutesCoeffOfVariation,
            (daysWithSignificantActivity * 100.0 / totalDays) as percentDaysActive,
            CASE
                WHEN stepsCoeffOfVariation < 30 AND percentDaysActive > 80 THEN 'High'
                WHEN stepsCoeffOfVariation < 50 AND percentDaysActive > 60 THEN 'Medium'
                ELSE 'Low'
            END as consistencyLevel,
            (100 - MIN(stepsCoeffOfVariation, 100)) * 0.4 + percentDaysActive * 0.6 as consistencyScore
        FROM activity_stats
    """)
    suspend fun getActivityConsistencyScore(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): ActivityConsistency

    /**
     * Data classes for query results
     */

    /**
     * Daily step count
     */
    data class DailySteps(
        val date: LocalDate,
        val totalSteps: Int
    )

    /**
     * Hourly step count
     */
    data class HourlySteps(
        val hour: String,
        val steps: Int
    )

    /**
     * Day of week step count
     */
    data class DayOfWeekSteps(
        val dayOfWeek: String,
        val avgSteps: Double
    )

    /**
     * Step goal progress
     */
    data class StepGoalProgress(
        val currentSteps: Int,
        val goalSteps: Int,
        val percentComplete: Double,
        val stepsRemaining: Int
    )

    /**
     * Daily step goal status
     */
    data class DailyStepGoalStatus(
        val date: LocalDate,
        val totalSteps: Int,
        val goalSteps: Int,
        val percentComplete: Double,
        val goalAchieved: Boolean
    )

    /**
     * Step streak information
     */
    data class StepStreak(
        val lastActiveDate: LocalDate,
        val lastDayAchieved: Boolean,
        val currentStreak: Int,
        val longestStreak: Int
    )

    /**
     * Activity type distribution
     */
    data class ActivityTypeDistribution(
        val activityType: String,
        val sessionCount: Int,
        val totalMinutes: Double,
        val avgDurationMinutes: Double,
        val totalCalories: Int,
        val totalDistanceMeters: Float
    )

    /**
     * Weekly activity type distribution
     */
    data class WeeklyActivityTypeDistribution(
        val yearWeek: String,
        val activityType: String,
        val totalMinutes: Double,
        val sessionCount: Int
    )

    /**
     * Frequent activity type
     */
    data class FrequentActivityType(
        val activityType: String,
        val sessionCount: Int,
        val totalMinutes: Double
    )

    /**
     * Activity intensity distribution
     */
    data class ActivityIntensityDistribution(
        val intensity: String,
        val sessionCount: Int,
        val totalMinutes: Double,
        val avgDurationMinutes: Double,
        val totalCalories: Int
    )

    /**
     * Daily calorie burn
     */
    data class DailyCalories(
        val date: LocalDate,
        val totalCalories: Int
    )

    /**
     * Activity type calorie distribution
     */
    data class ActivityTypeCalories(
        val activityType: String,
        val totalCalories: Int,
        val avgCaloriesPerSession: Double,
        val sessionCount: Int
    )

    /**
     * Activity type calorie rate
     */
    data class ActivityTypeCalorieRate(
        val activityType: String,
        val totalCalories: Int,
        val totalMinutes: Double,
        val caloriesPerMinute: Double
    )

    /**
     * Calorie goal progress
     */
    data class CalorieGoalProgress(
        val currentCalories: Int,
        val goalCalories: Int,
        val percentComplete: Double,
        val caloriesRemaining: Int
    )

    /**
     * Daily distance
     */
    data class DailyDistance(
        val date: LocalDate,
        val totalDistanceMeters: Float
    )

    /**
     * Activity type distance
     */
    data class ActivityTypeDistance(
        val activityType: String,
        val totalDistanceMeters: Float,
        val avgDistancePerSession: Float,
        val sessionCount: Int
    )

    /**
     * Distance goal progress
     */
    data class DistanceGoalProgress(
        val currentDistanceMeters: Float,
        val goalDistanceMeters: Float,
        val percentComplete: Double,
        val distanceRemainingMeters: Float
    )

    /**
     * Activity type pace statistics
     */
    data class ActivityTypePace(
        val activityType: String,
        val avgSpeedMetersPerSecond: Float,
        val maxSpeedMetersPerSecond: Float,
        val avgPaceMinutesPerKm: Double
    )

    /**
     * Daily active minutes
     */
    data class DailyActiveMinutes(
        val date: LocalDate,
        val totalActiveMinutes: Int
    )

    /**
     * Active vs sedentary time distribution
     */
    data class ActiveSedentaryDistribution(
        val activeMinutes: Int,
        val sedentaryMinutes: Int,
        val activePercentage: Double,
        val sedentaryPercentage: Double
    )

    /**
     * Hourly active minutes
     */
    data class HourlyActiveMinutes(
        val hour: String,
        val activeMinutes: Int
    )

    /**
     * Active minutes goal progress
     */
    data class ActiveMinutesGoalProgress(
        val currentActiveMinutes: Int,
        val goalActiveMinutes: Int,
        val percentComplete: Double,
        val activeMinutesRemaining: Int
    )

    /**
     * Sedentary period
     */
    data class SedentaryPeriod(
        val date: LocalDate,
        val periodStart: LocalDateTime,
        val periodEnd: LocalDateTime,
        val durationMinutes: Double
    )

    /**
     * Detected exercise session
     */
    data class DetectedExerciseSession(
        val date: LocalDate,
        val startTime: LocalDateTime,
        val endTime: LocalDateTime,
        val durationMinutes: Double,
        val totalActiveMinutes: Int,
        val totalSteps: Int,
        val totalCaloriesBurned: Int,
        val totalDistanceMeters: Float,
        val detectedActivityType: String
    )

    /**
     * Exercise frequency
     */
    data class ExerciseFrequency(
        val daysWithExercise: Int,
        val totalDays: Int,
        val exerciseFrequencyPercent: Double,
        val avgSessionsPerExerciseDay: Double,
        val avgMinutesPerExerciseDay: Double,
        val totalExerciseMinutes: Double
    )

    /**
     * Workout summary statistics
     */
    data class WorkoutSummaryStatistics(
        val totalWorkouts: Int,
        val avgDurationMinutes: Double,
        val maxDurationMinutes: Double,
        val avgCaloriesBurned: Double,
        val maxCalorie