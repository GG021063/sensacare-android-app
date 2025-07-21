package com.sensacare.app.data.local.dao

import androidx.room.*
import com.sensacare.app.data.local.entity.GoalProgressEntity
import com.sensacare.app.data.local.entity.HealthGoalEntity
import com.sensacare.app.data.local.entity.MetricType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * HealthGoalDao - Data Access Object for health goals management
 *
 * This DAO provides comprehensive access to health goals and progress data, including:
 * - Goal CRUD operations
 * - Goal progress tracking and updates
 * - Active/inactive goal management
 * - Goal achievement history
 * - Goal type filtering and sorting
 * - Goal deadline and reminder management
 * - Progress statistics and analytics
 *
 * The HealthGoalDao enables users to set, track, and analyze progress towards
 * their health and fitness goals across various metrics.
 */
@Dao
interface HealthGoalDao {

    /**
     * Basic CRUD Operations for Goals
     */

    /**
     * Insert a single health goal
     * @param goal The health goal entity to insert
     * @return The row ID of the inserted item
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: HealthGoalEntity): Long

    /**
     * Insert multiple health goals in a single transaction
     * @param goals List of health goal entities to insert
     * @return List of row IDs for the inserted items
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(goals: List<HealthGoalEntity>): List<Long>

    /**
     * Update a health goal
     * @param goal The health goal entity to update
     * @return Number of rows updated
     */
    @Update
    suspend fun update(goal: HealthGoalEntity): Int

    /**
     * Delete a health goal
     * @param goal The health goal entity to delete
     * @return Number of rows deleted
     */
    @Delete
    suspend fun delete(goal: HealthGoalEntity): Int

    /**
     * Delete a health goal by ID
     * @param goalId The ID of the health goal to delete
     * @return Number of rows deleted
     */
    @Query("DELETE FROM health_goals WHERE id = :goalId")
    suspend fun deleteById(goalId: String): Int

    /**
     * Delete all goals for a specific user
     * @param userId The user ID
     * @return Number of rows deleted
     */
    @Query("DELETE FROM health_goals WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String): Int

    /**
     * Delete all completed goals for a specific user
     * @param userId The user ID
     * @return Number of rows deleted
     */
    @Query("DELETE FROM health_goals WHERE userId = :userId AND isCompleted = 1")
    suspend fun deleteAllCompletedGoals(userId: String): Int

    /**
     * Basic Queries for Goals
     */

    /**
     * Get health goal by ID
     * @param goalId The ID of the health goal to retrieve
     * @return The health goal entity or null if not found
     */
    @Query("SELECT * FROM health_goals WHERE id = :goalId")
    suspend fun getById(goalId: String): HealthGoalEntity?

    /**
     * Get health goal by ID as Flow for reactive updates
     * @param goalId The ID of the health goal to retrieve
     * @return Flow emitting the health goal entity
     */
    @Query("SELECT * FROM health_goals WHERE id = :goalId")
    fun getByIdAsFlow(goalId: String): Flow<HealthGoalEntity?>

    /**
     * Get all health goals for a specific user
     * @param userId The user ID
     * @return List of health goal entities
     */
    @Query("SELECT * FROM health_goals WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getAllForUser(userId: String): List<HealthGoalEntity>

    /**
     * Get all health goals for a specific user as Flow for reactive updates
     * @param userId The user ID
     * @return Flow emitting list of health goal entities
     */
    @Query("SELECT * FROM health_goals WHERE userId = :userId ORDER BY createdAt DESC")
    fun getAllForUserAsFlow(userId: String): Flow<List<HealthGoalEntity>>

    /**
     * Get health goals by metric type
     * @param userId The user ID
     * @param metricType The metric type to filter by
     * @return List of health goal entities for the specified metric type
     */
    @Query("SELECT * FROM health_goals WHERE userId = :userId AND metricType = :metricType ORDER BY createdAt DESC")
    suspend fun getByMetricType(userId: String, metricType: String): List<HealthGoalEntity>

    /**
     * Get health goals by metric type as Flow for reactive updates
     * @param userId The user ID
     * @param metricType The metric type to filter by
     * @return Flow emitting list of health goal entities for the specified metric type
     */
    @Query("SELECT * FROM health_goals WHERE userId = :userId AND metricType = :metricType ORDER BY createdAt DESC")
    fun getByMetricTypeAsFlow(userId: String, metricType: String): Flow<List<HealthGoalEntity>>

    /**
     * Goal Progress Tracking and Updates
     */

    /**
     * Insert a goal progress record
     * @param progress The goal progress entity to insert
     * @return The row ID of the inserted item
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: GoalProgressEntity): Long

    /**
     * Insert multiple goal progress records in a single transaction
     * @param progressList List of goal progress entities to insert
     * @return List of row IDs for the inserted items
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllProgress(progressList: List<GoalProgressEntity>): List<Long>

    /**
     * Update a goal progress record
     * @param progress The goal progress entity to update
     * @return Number of rows updated
     */
    @Update
    suspend fun updateProgress(progress: GoalProgressEntity): Int

    /**
     * Delete a goal progress record
     * @param progress The goal progress entity to delete
     * @return Number of rows deleted
     */
    @Delete
    suspend fun deleteProgress(progress: GoalProgressEntity): Int

    /**
     * Delete all progress records for a specific goal
     * @param goalId The goal ID
     * @return Number of rows deleted
     */
    @Query("DELETE FROM goal_progress WHERE goalId = :goalId")
    suspend fun deleteAllProgressForGoal(goalId: String): Int

    /**
     * Get goal progress by ID
     * @param progressId The ID of the goal progress to retrieve
     * @return The goal progress entity or null if not found
     */
    @Query("SELECT * FROM goal_progress WHERE id = :progressId")
    suspend fun getProgressById(progressId: String): GoalProgressEntity?

    /**
     * Get all progress records for a specific goal
     * @param goalId The goal ID
     * @return List of goal progress entities
     */
    @Query("SELECT * FROM goal_progress WHERE goalId = :goalId ORDER BY timestamp DESC")
    suspend fun getProgressForGoal(goalId: String): List<GoalProgressEntity>

    /**
     * Get all progress records for a specific goal as Flow for reactive updates
     * @param goalId The goal ID
     * @return Flow emitting list of goal progress entities
     */
    @Query("SELECT * FROM goal_progress WHERE goalId = :goalId ORDER BY timestamp DESC")
    fun getProgressForGoalAsFlow(goalId: String): Flow<List<GoalProgressEntity>>

    /**
     * Get latest progress record for a specific goal
     * @param goalId The goal ID
     * @return The latest goal progress entity or null if not found
     */
    @Query("SELECT * FROM goal_progress WHERE goalId = :goalId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestProgressForGoal(goalId: String): GoalProgressEntity?

    /**
     * Get latest progress record for a specific goal as Flow for reactive updates
     * @param goalId The goal ID
     * @return Flow emitting the latest goal progress entity
     */
    @Query("SELECT * FROM goal_progress WHERE goalId = :goalId ORDER BY timestamp DESC LIMIT 1")
    fun getLatestProgressForGoalAsFlow(goalId: String): Flow<GoalProgressEntity?>

    /**
     * Update goal progress
     * @param goalId The goal ID
     * @param currentValue The current progress value
     * @param timestamp The timestamp of the update
     * @return Number of rows updated
     */
    @Transaction
    suspend fun updateGoalProgress(
        goalId: String,
        currentValue: Double,
        timestamp: LocalDateTime = LocalDateTime.now()
    ): Long {
        val goal = getById(goalId) ?: return -1
        
        // Calculate progress percentage
        val progressPercentage = when {
            goal.targetValue == goal.startValue -> 100.0 // Avoid division by zero
            goal.isIncremental -> ((currentValue - goal.startValue) / (goal.targetValue - goal.startValue) * 100)
                .coerceIn(0.0, 100.0)
            else -> ((goal.startValue - currentValue) / (goal.startValue - goal.targetValue) * 100)
                .coerceIn(0.0, 100.0)
        }
        
        // Check if goal is completed
        val isCompleted = if (goal.isIncremental) {
            currentValue >= goal.targetValue
        } else {
            currentValue <= goal.targetValue
        }
        
        // Update goal status if completed
        if (isCompleted && !goal.isCompleted) {
            update(goal.copy(
                isCompleted = true,
                completedAt = timestamp,
                modifiedAt = timestamp
            ))
        }
        
        // Create progress entity
        val progress = GoalProgressEntity(
            id = 0, // Auto-generated
            goalId = goalId,
            timestamp = timestamp,
            currentValue = currentValue,
            progressPercentage = progressPercentage,
            isCompleted = isCompleted,
            notes = null,
            createdAt = timestamp,
            modifiedAt = timestamp
        )
        
        return insertProgress(progress)
    }

    /**
     * Active/Inactive Goal Management
     */

    /**
     * Get all active goals for a specific user
     * @param userId The user ID
     * @return List of active health goal entities
     */
    @Query("""
        SELECT * FROM health_goals 
        WHERE userId = :userId 
        AND isActive = 1 
        AND (isCompleted = 0 OR isRecurring = 1)
        ORDER BY 
            CASE 
                WHEN deadline IS NOT NULL THEN 0 
                ELSE 1 
            END,
            deadline ASC,
            createdAt DESC
    """)
    suspend fun getActiveGoals(userId: String): List<HealthGoalEntity>

    /**
     * Get all active goals for a specific user as Flow for reactive updates
     * @param userId The user ID
     * @return Flow emitting list of active health goal entities
     */
    @Query("""
        SELECT * FROM health_goals 
        WHERE userId = :userId 
        AND isActive = 1 
        AND (isCompleted = 0 OR isRecurring = 1)
        ORDER BY 
            CASE 
                WHEN deadline IS NOT NULL THEN 0 
                ELSE 1 
            END,
            deadline ASC,
            createdAt DESC
    """)
    fun getActiveGoalsAsFlow(userId: String): Flow<List<HealthGoalEntity>>

    /**
     * Get all inactive goals for a specific user
     * @param userId The user ID
     * @return List of inactive health goal entities
     */
    @Query("SELECT * FROM health_goals WHERE userId = :userId AND isActive = 0 ORDER BY createdAt DESC")
    suspend fun getInactiveGoals(userId: String): List<HealthGoalEntity>

    /**
     * Get all completed goals for a specific user
     * @param userId The user ID
     * @return List of completed health goal entities
     */
    @Query("SELECT * FROM health_goals WHERE userId = :userId AND isCompleted = 1 ORDER BY completedAt DESC")
    suspend fun getCompletedGoals(userId: String): List<HealthGoalEntity>

    /**
     * Get all incomplete goals for a specific user
     * @param userId The user ID
     * @return List of incomplete health goal entities
     */
    @Query("SELECT * FROM health_goals WHERE userId = :userId AND isCompleted = 0 ORDER BY createdAt DESC")
    suspend fun getIncompleteGoals(userId: String): List<HealthGoalEntity>

    /**
     * Activate a goal
     * @param goalId The goal ID
     * @param timestamp The timestamp of the update
     * @return Number of rows updated
     */
    @Query("""
        UPDATE health_goals 
        SET 
            isActive = 1, 
            modifiedAt = :timestamp
        WHERE id = :goalId
    """)
    suspend fun activateGoal(
        goalId: String,
        timestamp: LocalDateTime = LocalDateTime.now()
    ): Int

    /**
     * Deactivate a goal
     * @param goalId The goal ID
     * @param timestamp The timestamp of the update
     * @return Number of rows updated
     */
    @Query("""
        UPDATE health_goals 
        SET 
            isActive = 0, 
            modifiedAt = :timestamp
        WHERE id = :goalId
    """)
    suspend fun deactivateGoal(
        goalId: String,
        timestamp: LocalDateTime = LocalDateTime.now()
    ): Int

    /**
     * Reset a recurring goal after completion
     * @param goalId The goal ID
     * @param timestamp The timestamp of the update
     * @return Number of rows updated
     */
    @Query("""
        UPDATE health_goals 
        SET 
            isCompleted = 0, 
            completedAt = NULL,
            startDate = :timestamp,
            deadline = CASE 
                WHEN durationDays IS NOT NULL THEN datetime(:timestamp, '+' || durationDays || ' days')
                ELSE NULL
            END,
            modifiedAt = :timestamp
        WHERE id = :goalId AND isRecurring = 1
    """)
    suspend fun resetRecurringGoal(
        goalId: String,
        timestamp: LocalDateTime = LocalDateTime.now()
    ): Int

    /**
     * Goal Achievement History
     */

    /**
     * Get goal achievement history for a specific user
     * @param userId The user ID
     * @param limit Maximum number of records to return
     * @return List of completed goal entities
     */
    @Query("""
        SELECT * FROM health_goals 
        WHERE userId = :userId AND isCompleted = 1
        ORDER BY completedAt DESC
        LIMIT :limit
    """)
    suspend fun getGoalAchievementHistory(userId: String, limit: Int = 50): List<HealthGoalEntity>

    /**
     * Get goal achievement history for a specific metric type
     * @param userId The user ID
     * @param metricType The metric type to filter by
     * @param limit Maximum number of records to return
     * @return List of completed goal entities for the specified metric type
     */
    @Query("""
        SELECT * FROM health_goals 
        WHERE userId = :userId AND metricType = :metricType AND isCompleted = 1
        ORDER BY completedAt DESC
        LIMIT :limit
    """)
    suspend fun getGoalAchievementHistoryByMetricType(
        userId: String,
        metricType: String,
        limit: Int = 50
    ): List<HealthGoalEntity>

    /**
     * Get goal achievement count by metric type
     * @param userId The user ID
     * @return List of metric types and their achievement counts
     */
    @Query("""
        SELECT 
            metricType,
            COUNT(*) as achievementCount
        FROM health_goals
        WHERE userId = :userId AND isCompleted = 1
        GROUP BY metricType
        ORDER BY achievementCount DESC
    """)
    suspend fun getGoalAchievementCountByMetricType(userId: String): List<MetricTypeAchievementCount>

    /**
     * Get goal achievement streak for a specific user
     * @param userId The user ID
     * @return Current streak information
     */
    @Query("""
        WITH completed_goals AS (
            SELECT 
                date(completedAt) as date,
                COUNT(*) as goalCount
            FROM health_goals
            WHERE userId = :userId AND isCompleted = 1
            GROUP BY date(completedAt)
            ORDER BY date(completedAt) DESC
        ),
        streak_data AS (
            SELECT 
                date,
                (
                    SELECT COUNT(*)
                    FROM completed_goals cg2
                    WHERE julianday(cg1.date) - julianday(cg2.date) = 
                        julianday(cg1.date) - julianday(cg1.date) - (ROW_NUMBER() OVER (ORDER BY cg2.date DESC) - 1)
                ) as streak_length
            FROM completed_goals cg1
            WHERE date = (SELECT MAX(date) FROM completed_goals)
        )
        SELECT 
            (SELECT date FROM streak_data) as lastAchievementDate,
            (SELECT streak_length FROM streak_data) as currentStreak,
            (
                SELECT COUNT(*)
                FROM (
                    SELECT date FROM completed_goals GROUP BY date
                )
            ) as totalAchievementDays,
            (
                SELECT COUNT(*)
                FROM health_goals
                WHERE userId = :userId AND isCompleted = 1
            ) as totalGoalsAchieved
    """)
    suspend fun getGoalAchievementStreak(userId: String): GoalAchievementStreak?

    /**
     * Get monthly goal achievement counts
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return Monthly goal achievement counts
     */
    @Query("""
        SELECT 
            strftime('%Y-%m', completedAt) as yearMonth,
            COUNT(*) as achievementCount
        FROM health_goals
        WHERE userId = :userId 
        AND isCompleted = 1
        AND date(completedAt) BETWEEN date(:startDate) AND date(:endDate)
        GROUP BY strftime('%Y-%m', completedAt)
        ORDER BY yearMonth
    """)
    suspend fun getMonthlyGoalAchievementCounts(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<MonthlyAchievementCount>

    /**
     * Goal Type Filtering and Sorting
     */

    /**
     * Get goals by category
     * @param userId The user ID
     * @param category The category to filter by
     * @return List of health goal entities for the specified category
     */
    @Query("SELECT * FROM health_goals WHERE userId = :userId AND category = :category ORDER BY createdAt DESC")
    suspend fun getGoalsByCategory(userId: String, category: String): List<HealthGoalEntity>

    /**
     * Get goals by priority
     * @param userId The user ID
     * @param priority The priority to filter by
     * @return List of health goal entities for the specified priority
     */
    @Query("SELECT * FROM health_goals WHERE userId = :userId AND priority = :priority ORDER BY createdAt DESC")
    suspend fun getGoalsByPriority(userId: String, priority: String): List<HealthGoalEntity>

    /**
     * Get goals sorted by deadline
     * @param userId The user ID
     * @param activeOnly Whether to include only active goals
     * @return List of health goal entities sorted by deadline
     */
    @Query("""
        SELECT * FROM health_goals 
        WHERE userId = :userId 
        AND deadline IS NOT NULL
        AND (:activeOnly = 0 OR (isActive = 1 AND (isCompleted = 0 OR isRecurring = 1)))
        ORDER BY deadline ASC
    """)
    suspend fun getGoalsSortedByDeadline(userId: String, activeOnly: Boolean = true): List<HealthGoalEntity>

    /**
     * Get goals sorted by progress
     * @param userId The user ID
     * @param activeOnly Whether to include only active goals
     * @return List of health goal entities with their latest progress
     */
    @Query("""
        SELECT g.*, p.progressPercentage as latestProgress
        FROM health_goals g
        LEFT JOIN (
            SELECT 
                goalId, 
                progressPercentage,
                ROW_NUMBER() OVER (PARTITION BY goalId ORDER BY timestamp DESC) as rn
            FROM goal_progress
        ) p ON g.id = p.goalId AND p.rn = 1
        WHERE g.userId = :userId
        AND (:activeOnly = 0 OR (g.isActive = 1 AND (g.isCompleted = 0 OR g.isRecurring = 1)))
        ORDER BY p.progressPercentage DESC
    """)
    suspend fun getGoalsSortedByProgress(userId: String, activeOnly: Boolean = true): List<GoalWithProgress>

    /**
     * Get goals sorted by creation date
     * @param userId The user ID
     * @param activeOnly Whether to include only active goals
     * @return List of health goal entities sorted by creation date
     */
    @Query("""
        SELECT * FROM health_goals 
        WHERE userId = :userId 
        AND (:activeOnly = 0 OR (isActive = 1 AND (isCompleted = 0 OR isRecurring = 1)))
        ORDER BY createdAt DESC
    """)
    suspend fun getGoalsSortedByCreationDate(userId: String, activeOnly: Boolean = true): List<HealthGoalEntity>

    /**
     * Get goals with specific tags
     * @param userId The user ID
     * @param tagQuery The tag query string
     * @return List of health goal entities with matching tags
     */
    @Query("""
        SELECT * FROM health_goals
        WHERE userId = :userId
        AND tags LIKE '%' || :tagQuery || '%'
        ORDER BY createdAt DESC
    """)
    suspend fun getGoalsByTags(userId: String, tagQuery: String): List<HealthGoalEntity>

    /**
     * Goal Deadline and Reminder Management
     */

    /**
     * Get goals approaching deadline
     * @param userId The user ID
     * @param daysThreshold Days threshold for approaching deadline
     * @return List of health goal entities approaching deadline
     */
    @Query("""
        SELECT * FROM health_goals 
        WHERE userId = :userId 
        AND isActive = 1 
        AND isCompleted = 0
        AND deadline IS NOT NULL
        AND deadline <= datetime('now', '+' || :daysThreshold || ' days')
        AND deadline > datetime('now')
        ORDER BY deadline ASC
    """)
    suspend fun getGoalsApproachingDeadline(userId: String, daysThreshold: Int = 7): List<HealthGoalEntity>

    /**
     * Get goals approaching deadline as Flow for reactive updates
     * @param userId The user ID
     * @param daysThreshold Days threshold for approaching deadline
     * @return Flow emitting list of health goal entities approaching deadline
     */
    @Query("""
        SELECT * FROM health_goals 
        WHERE userId = :userId 
        AND isActive = 1 
        AND isCompleted = 0
        AND deadline IS NOT NULL
        AND deadline <= datetime('now', '+' || :daysThreshold || ' days')
        AND deadline > datetime('now')
        ORDER BY deadline ASC
    """)
    fun getGoalsApproachingDeadlineAsFlow(userId: String, daysThreshold: Int = 7): Flow<List<HealthGoalEntity>>

    /**
     * Get overdue goals
     * @param userId The user ID
     * @return List of overdue health goal entities
     */
    @Query("""
        SELECT * FROM health_goals 
        WHERE userId = :userId 
        AND isActive = 1 
        AND isCompleted = 0
        AND deadline IS NOT NULL
        AND deadline < datetime('now')
        ORDER BY deadline ASC
    """)
    suspend fun getOverdueGoals(userId: String): List<HealthGoalEntity>

    /**
     * Get overdue goals as Flow for reactive updates
     * @param userId The user ID
     * @return Flow emitting list of overdue health goal entities
     */
    @Query("""
        SELECT * FROM health_goals 
        WHERE userId = :userId 
        AND isActive = 1 
        AND isCompleted = 0
        AND deadline IS NOT NULL
        AND deadline < datetime('now')
        ORDER BY deadline ASC
    """)
    fun getOverdueGoalsAsFlow(userId: String): Flow<List<HealthGoalEntity>>

    /**
     * Get goals with reminders due
     * @param userId The user ID
     * @param currentTime Current time
     * @param reminderWindowMinutes Time window in minutes to check for due reminders
     * @return List of health goal entities with reminders due
     */
    @Query("""
        SELECT * FROM health_goals 
        WHERE userId = :userId 
        AND isActive = 1 
        AND isCompleted = 0
        AND reminderTime IS NOT NULL
        AND reminderTime <= datetime(:currentTime, '+' || :reminderWindowMinutes || ' minutes')
        AND reminderTime > datetime(:currentTime, '-' || :reminderWindowMinutes || ' minutes')
        ORDER BY reminderTime ASC
    """)
    suspend fun getGoalsWithRemindersDue(
        userId: String,
        currentTime: LocalDateTime = LocalDateTime.now(),
        reminderWindowMinutes: Int = 15
    ): List<HealthGoalEntity>

    /**
     * Update goal deadline
     * @param goalId The goal ID
     * @param newDeadline The new deadline
     * @param updateTime The time of the update
     * @return Number of rows updated
     */
    @Query("""
        UPDATE health_goals 
        SET 
            deadline = :newDeadline, 
            modifiedAt = :updateTime
        WHERE id = :goalId
    """)
    suspend fun updateGoalDeadline(
        goalId: String,
        newDeadline: LocalDateTime?,
        updateTime: LocalDateTime = LocalDateTime.now()
    ): Int

    /**
     * Update goal reminder time
     * @param goalId The goal ID
     * @param newReminderTime The new reminder time
     * @param updateTime The time of the update
     * @return Number of rows updated
     */
    @Query("""
        UPDATE health_goals 
        SET 
            reminderTime = :newReminderTime, 
            modifiedAt = :updateTime
        WHERE id = :goalId
    """)
    suspend fun updateGoalReminderTime(
        goalId: String,
        newReminderTime: LocalDateTime?,
        updateTime: LocalDateTime = LocalDateTime.now()
    ): Int

    /**
     * Progress Statistics and Analytics
     */

    /**
     * Get goal completion rate
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return Goal completion rate metrics
     */
    @Query("""
        WITH goal_stats AS (
            SELECT 
                COUNT(*) as totalGoals,
                SUM(CASE WHEN isCompleted = 1 THEN 1 ELSE 0 END) as completedGoals,
                SUM(CASE WHEN deadline IS NOT NULL AND deadline < datetime('now') AND isCompleted = 0 THEN 1 ELSE 0 END) as overdueGoals
            FROM health_goals
            WHERE userId = :userId
            AND (
                (startDate IS NULL) OR
                (date(startDate) BETWEEN date(:startDate) AND date(:endDate)) OR
                (date(deadline) BETWEEN date(:startDate) AND date(:endDate)) OR
                (date(completedAt) BETWEEN date(:startDate) AND date(:endDate))
            )
        )
        SELECT 
            totalGoals,
            completedGoals,
            overdueGoals,
            CASE 
                WHEN totalGoals > 0 THEN (completedGoals * 100.0 / totalGoals)
                ELSE 0
            END as completionRate,
            CASE 
                WHEN totalGoals > 0 THEN (overdueGoals * 100.0 / totalGoals)
                ELSE 0
            END as overdueRate
        FROM goal_stats
    """)
    suspend fun getGoalCompletionRate(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): GoalCompletionRate

    /**
     * Get goal completion rate by metric type
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return Goal completion rate by metric type
     */
    @Query("""
        SELECT 
            metricType,
            COUNT(*) as totalGoals,
            SUM(CASE WHEN isCompleted = 1 THEN 1 ELSE 0 END) as completedGoals,
            CASE 
                WHEN COUNT(*) > 0 THEN (SUM(CASE WHEN isCompleted = 1 THEN 1 ELSE 0 END) * 100.0 / COUNT(*))
                ELSE 0
            END as completionRate
        FROM health_goals
        WHERE userId = :userId
        AND (
            (startDate IS NULL) OR
            (date(startDate) BETWEEN date(:startDate) AND date(:endDate)) OR
            (date(deadline) BETWEEN date(:startDate) AND date(:endDate)) OR
            (date(completedAt) BETWEEN date(:startDate) AND date(:endDate))
        )
        GROUP BY metricType
        ORDER BY completionRate DESC
    """)
    suspend fun getGoalCompletionRateByMetricType(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<MetricTypeCompletionRate>

    /**
     * Get goal completion time statistics
     * @param userId The user ID
     * @param metricType Optional metric type to filter by
     * @return Goal completion time statistics
     */
    @Query("""
        WITH completion_times AS (
            SELECT 
                (julianday(completedAt) - julianday(startDate)) as daysToComplete
            FROM health_goals
            WHERE userId = :userId
            AND isCompleted = 1
            AND startDate IS NOT NULL
            AND completedAt IS NOT NULL
            AND (:metricType IS NULL OR metricType = :metricType)
        )
        SELECT 
            AVG(daysToComplete) as avgDaysToComplete,
            MIN(daysToComplete) as minDaysToComplete,
            MAX(daysToComplete) as maxDaysToComplete,
            COUNT(*) as completedGoalsCount
        FROM completion_times
    """)
    suspend fun getGoalCompletionTimeStats(
        userId: String,
        metricType: String? = null
    ): GoalCompletionTimeStats?

    /**
     * Get goal progress trend
     * @param goalId The goal ID
     * @param limit Maximum number of records to return
     * @return List of goal progress records showing trend over time
     */
    @Query("""
        SELECT * FROM goal_progress
        WHERE goalId = :goalId
        ORDER BY timestamp ASC
        LIMIT :limit
    """)
    suspend fun getGoalProgressTrend(goalId: String, limit: Int = 100): List<GoalProgressEntity>

    /**
     * Get goal progress trend as Flow for reactive updates
     * @param goalId The goal ID
     * @param limit Maximum number of records to return
     * @return Flow emitting list of goal progress records
     */
    @Query("""
        SELECT * FROM goal_progress
        WHERE goalId = :goalId
        ORDER BY timestamp ASC
        LIMIT :limit
    """)
    fun getGoalProgressTrendAsFlow(goalId: String, limit: Int = 100): Flow<List<GoalProgressEntity>>

    /**
     * Get goal progress velocity
     * @param goalId The goal ID
     * @return Goal progress velocity metrics
     */
    @Query("""
        WITH progress_points AS (
            SELECT 
                timestamp,
                progressPercentage,
                LAG(timestamp) OVER (ORDER BY timestamp) as prev_timestamp,
                LAG(progressPercentage) OVER (ORDER BY timestamp) as prev_percentage
            FROM goal_progress
            WHERE goalId = :goalId
            ORDER BY timestamp
        ),
        velocity_points AS (
            SELECT 
                (julianday(timestamp) - julianday(prev_timestamp)) as days_elapsed,
                (progressPercentage - prev_percentage) as percentage_change,
                CASE 
                    WHEN (julianday(timestamp) - julianday(prev_timestamp)) > 0
                    THEN (progressPercentage - prev_percentage) / (julianday(timestamp) - julianday(prev_timestamp))
                    ELSE 0
                END as daily_velocity
            FROM progress_points
            WHERE prev_timestamp IS NOT NULL
        )
        SELECT 
            AVG(daily_velocity) as avgDailyProgressPercentage,
            MAX(daily_velocity) as maxDailyProgressPercentage,
            (
                SELECT progressPercentage
                FROM goal_progress
                WHERE goalId = :goalId
                ORDER BY timestamp DESC
                LIMIT 1
            ) as currentProgressPercentage,
            (
                SELECT 
                    CASE 
                        WHEN deadline IS NOT NULL AND AVG(daily_velocity) > 0
                        THEN (100 - currentProgressPercentage) / AVG(daily_velocity)
                        ELSE NULL
                    END
                FROM velocity_points, health_goals
                WHERE health_goals.id = :goalId
            ) as estimatedDaysToCompletion
        FROM velocity_points
    """)
    suspend fun getGoalProgressVelocity(goalId: String): GoalProgressVelocity?

    /**
     * Get goal achievement rate over time
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @param intervalType Interval type (daily, weekly, monthly)
     * @return Goal achievement rate over time
     */
    @Query("""
        WITH time_periods AS (
            SELECT 
                CASE 
                    WHEN :intervalType = 'daily' THEN date(completedAt)
                    WHEN :intervalType = 'weekly' THEN strftime('%Y-%W', completedAt)
                    WHEN :intervalType = 'monthly' THEN strftime('%Y-%m', completedAt)
                    ELSE date(completedAt)
                END as period,
                COUNT(*) as completedCount
            FROM health_goals
            WHERE userId = :userId
            AND isCompleted = 1
            AND date(completedAt) BETWEEN date(:startDate) AND date(:endDate)
            GROUP BY period
        ),
        created_periods AS (
            SELECT 
                CASE 
                    WHEN :intervalType = 'daily' THEN date(createdAt)
                    WHEN :intervalType = 'weekly' THEN strftime('%Y-%W', createdAt)
                    WHEN :intervalType = 'monthly' THEN strftime('%Y-%m', createdAt)
                    ELSE date(createdAt)
                END as period,
                COUNT(*) as createdCount
            FROM health_goals
            WHERE userId = :userId
            AND date(createdAt) BETWEEN date(:startDate) AND date(:endDate)
            GROUP BY period
        )
        SELECT 
            cp.period,
            COALESCE(tp.completedCount, 0) as completedCount,
            cp.createdCount,
            CASE 
                WHEN cp.createdCount > 0 THEN (COALESCE(tp.completedCount, 0) * 100.0 / cp.createdCount)
                ELSE 0
            END as achievementRate
        FROM created_periods cp
        LEFT JOIN time_periods tp ON cp.period = tp.period
        ORDER BY cp.period
    """)
    suspend fun getGoalAchievementRateOverTime(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate,
        intervalType: String = "weekly"
    ): List<TimePeriodicAchievementRate>

    /**
     * Get goal consistency score
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return Goal consistency metrics
     */
    @Query("""
        WITH goal_stats AS (
            SELECT 
                COUNT(*) as totalGoals,
                SUM(CASE WHEN isCompleted = 1 THEN 1 ELSE 0 END) as completedGoals,
                (
                    SELECT COUNT(DISTINCT date(completedAt))
                    FROM health_goals
                    WHERE userId = :userId
                    AND isCompleted = 1
                    AND date(completedAt) BETWEEN date(:startDate) AND date(:endDate)
                ) as daysWithCompletions,
                (julianday(:endDate) - julianday(:startDate) + 1) as totalDays
            FROM health_goals
            WHERE userId = :userId
            AND (
                (startDate IS NULL) OR
                (date(startDate) BETWEEN date(:startDate) AND date(:endDate)) OR
                (date(deadline) BETWEEN date(:startDate) AND date(:endDate)) OR
                (date(completedAt) BETWEEN date(:startDate) AND date(:endDate))
            )
        ),
        streak_data AS (
            SELECT 
                (
                    SELECT MAX(streak_length)
                    FROM (
                        SELECT 
                            COUNT(*) as streak_length
                        FROM (
                            SELECT 
                                date(completedAt) as completion_date,
                                date(completedAt, '-' || ROW_NUMBER() OVER (ORDER BY completedAt) || ' days') as group_date
                            FROM health_goals
                            WHERE userId = :userId
                            AND isCompleted = 1
                            AND date(completedAt) BETWEEN date(:startDate) AND date(:endDate)
                            GROUP BY date(completedAt)
                        )
                        GROUP BY group_date
                    )
                ) as longestStreak
            FROM health_goals
            LIMIT 1
        )
        SELECT 
            (SELECT totalGoals FROM goal_stats) as totalGoals,
            (SELECT completedGoals FROM goal_stats) as completedGoals,
            (SELECT daysWithCompletions FROM goal_stats) as daysWithCompletions,
            (SELECT totalDays FROM goal_stats) as totalDays,
            (SELECT longestStreak FROM streak_data) as longestStreak,
            (SELECT daysWithCompletions * 100.0 / totalDays FROM goal_stats) as consistencyPercentage,
            (
                (SELECT completedGoals * 0.4 FROM goal_stats) + 
                (SELECT daysWithCompletions * 100.0 / totalDays * 0.4 FROM goal_stats) + 
                (SELECT longestStreak * 100.0 / totalDays * 0.2 FROM goal_stats)
            ) as consistencyScore
        FROM goal_stats
    """)
    suspend fun getGoalConsistencyScore(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): GoalConsistencyScore

    /**
     * Get goals needing attention
     * @param userId The user ID
     * @return List of goals needing attention
     */
    @Query("""
        WITH goal_progress AS (
            SELECT 
                g.id,
                g.title,
                g.metricType,
                g.deadline,
                g.isCompleted,
                (
                    SELECT progressPercentage
                    FROM goal_progress gp
                    WHERE gp.goalId = g.id
                    ORDER BY timestamp DESC
                    LIMIT 1
                ) as latestProgress,
                CASE 
                    WHEN g.deadline IS NOT NULL THEN (julianday(g.deadline) - julianday('now')) 
                    ELSE NULL
                END as daysToDeadline
            FROM health_goals g
            WHERE g.userId = :userId
            AND g.isActive = 1
            AND g.isCompleted = 0
        )
        SELECT 
            id,
            title,
            metricType,
            deadline,
            latestProgress,
            daysToDeadline,
            CASE
                WHEN daysToDeadline < 0 THEN 'OVERDUE'
                WHEN daysToDeadline < 3 AND (latestProgress IS NULL OR latestProgress < 80) THEN 'URGENT'
                WHEN daysToDeadline < 7 AND (latestProgress IS NULL OR latestProgress < 50) THEN 'AT_RISK'
                WHEN latestProgress IS NULL THEN 'NO_PROGRESS'
                WHEN latestProgress < 20 THEN 'JUST_STARTED'
                WHEN daysToDeadline > 30 AND latestProgress < 10 THEN 'SLOW_PROGRESS'
                ELSE 'ON_TRACK'
            END as attentionStatus
        FROM goal_progress
        WHERE 
            attentionStatus IN ('OVERDUE', 'URGENT', 'AT_RISK', 'NO_PROGRESS', 'SLOW_PROGRESS')
        ORDER BY 
            CASE attentionStatus
                WHEN 'OVERDUE' THEN 1
                WHEN 'URGENT' THEN 2
                WHEN 'AT_RISK' THEN 3
                WHEN 'NO_PROGRESS' THEN 4
                WHEN 'SLOW_PROGRESS' THEN 5
                ELSE 6
            END,
            daysToDeadline ASC
    """)
    suspend fun getGoalsNeedingAttention(userId: String): List<GoalAttentionStatus>

    /**
     * Get goal completion time distribution
     * @param userId The user ID
     * @param metricType Optional metric type to filter by
     * @return Distribution of goal completion times
     */
    @Query("""
        WITH completion_times AS (
            SELECT 
                CASE
                    WHEN (julianday(completedAt) - julianday(startDate)) <= 7 THEN 'Within 1 week'
                    WHEN (julianday(completedAt) - julianday(startDate)) <= 14 THEN '1-2 weeks'
                    WHEN (julianday(completedAt) - julianday(startDate)) <= 30 THEN '2-4 weeks'
                    WHEN (julianday(completedAt) - julianday(startDate)) <= 90 THEN '1-3 months'
                    ELSE 'Over 3 months'
                END as timeFrame,
                COUNT(*) as goalCount
            FROM health_goals
            WHERE userId = :userId
            AND isCompleted = 1
            AND startDate IS NOT NULL
            AND completedAt IS NOT NULL
            AND (:metricType IS NULL OR metricType = :metricType)
            GROUP BY timeFrame
        )
        SELECT 
            timeFrame,
            goalCount,
            (
                SELECT SUM(goalCount) FROM completion_times
            ) as totalGoals,
            (goalCount * 100.0 / (SELECT SUM(goalCount) FROM completion_times)) as percentage
        FROM completion_times
        ORDER BY 
            CASE timeFrame
                WHEN 'Within 1 week' THEN 1
                WHEN '1-2 weeks' THEN 2
                WHEN '2-4 weeks' THEN 3
                WHEN '1-3 months' THEN 4
                WHEN 'Over 3 months' THEN 5
            END
    """)
    suspend fun getGoalCompletionTimeDistribution(
        userId: String,
        metricType: String? = null
    ): List<CompletionTimeDistribution>

    /**
     * Data classes for query results
     */

    /**
     * Goal with progress information
     */
    data class GoalWithProgress(
        @Embedded val goal: HealthGoalEntity,
        val latestProgress: Double?
    )

    /**
     * Metric type achievement count
     */
    data class MetricTypeAchievementCount(
        val metricType: String,
        val achievementCount: Int
    )

    /**
     * Goal achievement streak
     */
    data class GoalAchievementStreak(
        val lastAchievementDate: LocalDate?,
        val currentStreak: Int,
        val totalAchievementDays: Int,
        val totalGoalsAchieved: Int
    )

    /**
     * Monthly achievement count
     */
    data class MonthlyAchievementCount(
        val yearMonth: String,
        val achievementCount: Int
    )

    /**
     * Goal completion rate
     */
    data class GoalCompletionRate(
        val totalGoals: Int,
        val completedGoals: Int,
        val overdueGoals: Int,
        val completionRate: Double,
        val overdueRate: Double
    )

    /**
     * Metric type completion rate
     */
    data class MetricTypeCompletionRate(
        val metricType: String,
        val totalGoals: Int,
        val completedGoals: Int,
        val completionRate: Double
    )

    /**
     * Goal completion time statistics
     */
    data class GoalCompletionTimeStats(
        val avgDaysToComplete: Double,
        val minDaysToComplete: Double,
        val maxDaysToComplete: Double,
        val completedGoalsCount: Int
    )

    /**
     * Goal progress velocity
     */
    data class GoalProgressVelocity(
        val avgDailyProgressPercentage: Double,
        val maxDailyProgressPercentage: Double,
        val currentProgressPercentage: Double,
        val estimatedDaysToCompletion: Double?
    )

    /**
     * Time periodic achievement rate
     */
    data class TimePeriodicAchievementRate(
        val period: String,
        val completedCount: Int,
        val createdCount: Int,
        val achievementRate: Double
    )

    /**
     * Goal consistency score
     */
    data class GoalConsistencyScore(
        val totalGoals: Int,
        val completedGoals: Int,
        val daysWithCompletions: Int,
        val totalDays: Double,
        val longestStreak: Int,
        val consistencyPercentage: Double,
        val consistencyScore: Double
    )

    /**
     * Goal attention status
     */
    data class GoalAttentionStatus(
        val id: String,
        val title: String,
        val metricType: String,
        val deadline: LocalDateTime?,
        val latestProgress: Double?,
        val daysToDeadline: Double?,
        val attentionStatus: String
    )

    /**
     * Completion time distribution
     */
    data class CompletionTimeDistribution(
        val timeFrame: String,
        val goalCount: Int,
        val totalGoals: Int,
        val percentage: Double
    )
}
