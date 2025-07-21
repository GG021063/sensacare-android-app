package com.sensacare.app.data.local.entity

import androidx.room.*
import com.sensacare.app.domain.model.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * HealthGoalEntity - Room database entity for health goals
 *
 * This entity stores comprehensive information about user health goals, including:
 * - Goal type (steps, weight, activity, sleep, etc.)
 * - Target values and current progress
 * - Time-based parameters (start date, deadline, frequency)
 * - Achievement status and streak tracking
 * - Priority and reminders
 * - User notes and customizations
 *
 * The entity enables the app to track progress toward health goals,
 * provide achievement notifications, and adapt recommendations
 * based on user performance.
 */
@Entity(
    tableName = "health_goal",
    indices = [
        Index("userId"),
        Index("goalType"),
        Index("priority"),
        Index("status"),
        Index("startDate"),
        Index("targetDate")
    ]
)
data class HealthGoalEntity(
    /**
     * Primary key - unique identifier for the goal
     */
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    /**
     * User identifier - allows for multi-user support
     */
    @ColumnInfo(name = "userId")
    val userId: String,

    /**
     * Type of health goal (steps, weight, activity, sleep, etc.)
     */
    @ColumnInfo(name = "goalType")
    val goalType: String,

    /**
     * Specific metric being tracked (e.g., "daily_steps", "sleep_duration", "weight_loss")
     */
    @ColumnInfo(name = "metricType")
    val metricType: String,

    /**
     * Title of the goal
     */
    @ColumnInfo(name = "title")
    val title: String,

    /**
     * Detailed description of the goal
     */
    @ColumnInfo(name = "description")
    val description: String? = null,

    /**
     * Target value to achieve (e.g., 10000 steps, 8 hours sleep)
     */
    @ColumnInfo(name = "targetValue")
    val targetValue: Double,

    /**
     * Starting value when the goal was created
     */
    @ColumnInfo(name = "startValue")
    val startValue: Double? = null,

    /**
     * Current progress value
     */
    @ColumnInfo(name = "currentValue")
    val currentValue: Double,

    /**
     * Unit of measurement (steps, kg, hours, etc.)
     */
    @ColumnInfo(name = "unit")
    val unit: String,

    /**
     * Date when the goal was started
     */
    @ColumnInfo(name = "startDate")
    val startDate: LocalDate,

    /**
     * Target date for goal completion
     */
    @ColumnInfo(name = "targetDate")
    val targetDate: LocalDate? = null,

    /**
     * Goal frequency (DAILY, WEEKLY, MONTHLY, ONCE)
     */
    @ColumnInfo(name = "frequency")
    val frequency: String,

    /**
     * Days of week for weekly goals (comma-separated, e.g., "MON,WED,FRI")
     */
    @ColumnInfo(name = "daysOfWeek")
    val daysOfWeek: String? = null,

    /**
     * Priority level (HIGH, MEDIUM, LOW)
     */
    @ColumnInfo(name = "priority")
    val priority: String,

    /**
     * Current goal status (ACTIVE, COMPLETED, FAILED, PAUSED)
     */
    @ColumnInfo(name = "status")
    val status: String,

    /**
     * Achievement percentage (0-100)
     */
    @ColumnInfo(name = "achievementPercentage")
    val achievementPercentage: Int,

    /**
     * Current streak (consecutive days/weeks of meeting the goal)
     */
    @ColumnInfo(name = "currentStreak")
    val currentStreak: Int = 0,

    /**
     * Longest streak achieved for this goal
     */
    @ColumnInfo(name = "longestStreak")
    val longestStreak: Int = 0,

    /**
     * Last date when progress was updated
     */
    @ColumnInfo(name = "lastUpdatedDate")
    val lastUpdatedDate: LocalDate? = null,

    /**
     * Number of times the goal has been achieved
     */
    @ColumnInfo(name = "achievementCount")
    val achievementCount: Int = 0,

    /**
     * Flag indicating if reminders are enabled for this goal
     */
    @ColumnInfo(name = "remindersEnabled")
    val remindersEnabled: Boolean = true,

    /**
     * Reminder time as string (HH:MM)
     */
    @ColumnInfo(name = "reminderTime")
    val reminderTime: String? = null,

    /**
     * Flag indicating if the goal was suggested by the app
     */
    @ColumnInfo(name = "isSystemSuggested")
    val isSystemSuggested: Boolean = false,

    /**
     * Flag indicating if the goal recurs after completion
     */
    @ColumnInfo(name = "isRecurring")
    val isRecurring: Boolean = true,

    /**
     * Associated health metric ID (if applicable)
     */
    @ColumnInfo(name = "associatedMetricId")
    val associatedMetricId: String? = null,

    /**
     * User notes about the goal
     */
    @ColumnInfo(name = "notes")
    val notes: String? = null,

    /**
     * Tags for categorizing goals
     */
    @ColumnInfo(name = "tags")
    val tags: List<String>? = null,

    /**
     * Timestamp when this record was created
     */
    @ColumnInfo(name = "createdAt")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    /**
     * Timestamp when this record was last modified
     */
    @ColumnInfo(name = "modifiedAt")
    val modifiedAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * Convert entity to domain model
     */
    fun toDomainModel(): HealthGoal {
        return HealthGoal(
            id = id,
            userId = userId,
            goalType = GoalType.valueOf(goalType),
            metricType = metricType,
            title = title,
            description = description,
            targetValue = targetValue,
            startValue = startValue,
            currentValue = currentValue,
            unit = unit,
            startDate = startDate,
            targetDate = targetDate,
            frequency = GoalFrequency.valueOf(frequency),
            daysOfWeek = daysOfWeek?.split(",")?.mapNotNull { 
                try { DayOfWeek.valueOf(it) } catch (e: Exception) { null }
            }?.toSet(),
            priority = GoalPriority.valueOf(priority),
            status = GoalStatus.valueOf(status),
            achievementPercentage = achievementPercentage,
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            lastUpdatedDate = lastUpdatedDate,
            achievementCount = achievementCount,
            remindersEnabled = remindersEnabled,
            reminderTime = reminderTime,
            isSystemSuggested = isSystemSuggested,
            isRecurring = isRecurring,
            associatedMetricId = associatedMetricId,
            notes = notes,
            tags = tags ?: emptyList(),
            createdAt = createdAt,
            modifiedAt = modifiedAt
        )
    }

    companion object {
        /**
         * Convert domain model to entity
         */
        fun fromDomainModel(domainModel: HealthGoal): HealthGoalEntity {
            return HealthGoalEntity(
                id = domainModel.id,
                userId = domainModel.userId,
                goalType = domainModel.goalType.name,
                metricType = domainModel.metricType,
                title = domainModel.title,
                description = domainModel.description,
                targetValue = domainModel.targetValue,
                startValue = domainModel.startValue,
                currentValue = domainModel.currentValue,
                unit = domainModel.unit,
                startDate = domainModel.startDate,
                targetDate = domainModel.targetDate,
                frequency = domainModel.frequency.name,
                daysOfWeek = domainModel.daysOfWeek?.joinToString(",") { it.name },
                priority = domainModel.priority.name,
                status = domainModel.status.name,
                achievementPercentage = domainModel.achievementPercentage,
                currentStreak = domainModel.currentStreak,
                longestStreak = domainModel.longestStreak,
                lastUpdatedDate = domainModel.lastUpdatedDate,
                achievementCount = domainModel.achievementCount,
                remindersEnabled = domainModel.remindersEnabled,
                reminderTime = domainModel.reminderTime,
                isSystemSuggested = domainModel.isSystemSuggested,
                isRecurring = domainModel.isRecurring,
                associatedMetricId = domainModel.associatedMetricId,
                notes = domainModel.notes,
                tags = domainModel.tags,
                createdAt = domainModel.createdAt,
                modifiedAt = domainModel.modifiedAt
            )
        }

        /**
         * Calculate achievement percentage based on current and target values
         * @param currentValue Current progress value
         * @param targetValue Target goal value
         * @param startValue Starting value (optional)
         * @param isInverse Whether the goal is to decrease the value (e.g., weight loss)
         * @return Achievement percentage (0-100)
         */
        fun calculateAchievementPercentage(
            currentValue: Double,
            targetValue: Double,
            startValue: Double? = null,
            isInverse: Boolean = false
        ): Int {
            // For goals with no starting value
            if (startValue == null) {
                return if (!isInverse) {
                    // Regular goals (increase): percentage of target achieved
                    min(100, max(0, ((currentValue / targetValue) * 100).roundToInt()))
                } else {
                    // Inverse goals (decrease): percentage of target achieved
                    min(100, max(0, ((1 - (currentValue / targetValue)) * 100).roundToInt()))
                }
            }
            
            // For goals with a starting value (e.g., weight loss from 80kg to 70kg)
            return if (!isInverse) {
                // Regular goals (increase)
                val progress = currentValue - startValue
                val total = targetValue - startValue
                if (total <= 0) return 100 // Already achieved or invalid
                min(100, max(0, ((progress / total) * 100).roundToInt()))
            } else {
                // Inverse goals (decrease)
                val progress = startValue - currentValue
                val total = startValue - targetValue
                if (total <= 0) return 100 // Already achieved or invalid
                min(100, max(0, ((progress / total) * 100).roundToInt()))
            }
        }

        /**
         * Check if a goal is achieved based on current value and target
         * @param currentValue Current progress value
         * @param targetValue Target goal value
         * @param isInverse Whether the goal is to decrease the value
         * @return True if the goal is achieved
         */
        fun isGoalAchieved(
            currentValue: Double,
            targetValue: Double,
            isInverse: Boolean = false
        ): Boolean {
            return if (!isInverse) {
                currentValue >= targetValue
            } else {
                currentValue <= targetValue
            }
        }

        /**
         * Calculate days remaining until target date
         * @param targetDate Target completion date
         * @return Number of days remaining, or null if no target date
         */
        fun calculateDaysRemaining(targetDate: LocalDate?): Int? {
            if (targetDate == null) return null
            val today = LocalDate.now()
            if (targetDate.isBefore(today)) return 0
            return ChronoUnit.DAYS.between(today, targetDate).toInt()
        }

        /**
         * Calculate required daily progress to meet the goal by the target date
         * @param currentValue Current progress value
         * @param targetValue Target goal value
         * @param daysRemaining Days remaining until target date
         * @param isInverse Whether the goal is to decrease the value
         * @return Required daily progress
         */
        fun calculateRequiredDailyProgress(
            currentValue: Double,
            targetValue: Double,
            daysRemaining: Int,
            isInverse: Boolean = false
        ): Double {
            if (daysRemaining <= 0) return 0.0
            
            return if (!isInverse) {
                // For increasing goals
                val remaining = targetValue - currentValue
                if (remaining <= 0) return 0.0 // Already achieved
                remaining / daysRemaining
            } else {
                // For decreasing goals
                val remaining = currentValue - targetValue
                if (remaining <= 0) return 0.0 // Already achieved
                remaining / daysRemaining
            }
        }

        /**
         * Update streak based on goal achievement
         * @param currentStreak Current streak count
         * @param longestStreak Longest streak achieved so far
         * @param isAchieved Whether the goal was achieved today
         * @param lastUpdatedDate Last date when progress was updated
         * @return Pair of (new current streak, new longest streak)
         */
        fun updateStreak(
            currentStreak: Int,
            longestStreak: Int,
            isAchieved: Boolean,
            lastUpdatedDate: LocalDate?
        ): Pair<Int, Int> {
            val today = LocalDate.now()
            
            // If this is the first update or goal wasn't achieved, reset or keep streak
            if (lastUpdatedDate == null) {
                return if (isAchieved) {
                    Pair(1, max(1, longestStreak))
                } else {
                    Pair(0, longestStreak)
                }
            }
            
            // Check if the last update was yesterday (continuing streak)
            val isConsecutive = ChronoUnit.DAYS.between(lastUpdatedDate, today) == 1L
            
            // Update streak based on achievement and consecutive days
            val newCurrentStreak = when {
                !isAchieved -> 0 // Reset streak if goal not achieved
                isConsecutive -> currentStreak + 1 // Increment streak if consecutive
                today == lastUpdatedDate -> currentStreak // Same day, keep streak
                else -> 1 // Non-consecutive, start new streak
            }
            
            // Update longest streak if current streak is longer
            val newLongestStreak = max(longestStreak, newCurrentStreak)
            
            return Pair(newCurrentStreak, newLongestStreak)
        }

        /**
         * Generate a suggested goal based on user metrics and history
         * @param metricType Type of metric to create goal for
         * @param currentAverage Current average value of the metric
         * @param recommendedValue Recommended value for the metric
         * @param userId User identifier
         * @return A suggested goal entity
         */
        fun createSuggestedGoal(
            metricType: String,
            currentAverage: Double,
            recommendedValue: Double,
            userId: String
        ): HealthGoalEntity {
            // Determine goal type and title based on metric
            val (goalType, title, unit, isInverse) = when (metricType) {
                "daily_steps" -> Quadruple(
                    GoalType.ACTIVITY.name,
                    "Increase Daily Steps",
                    "steps",
                    false
                )
                "sleep_duration" -> Quadruple(
                    GoalType.SLEEP.name,
                    "Improve Sleep Duration",
                    "hours",
                    false
                )
                "weight" -> Quadruple(
                    GoalType.WEIGHT.name,
                    "Achieve Healthy Weight",
                    "kg",
                    currentAverage > recommendedValue
                )
                "heart_rate_resting" -> Quadruple(
                    GoalType.HEART_HEALTH.name,
                    "Improve Resting Heart Rate",
                    "bpm",
                    true
                )
                "water_intake" -> Quadruple(
                    GoalType.NUTRITION.name,
                    "Increase Water Intake",
                    "ml",
                    false
                )
                "active_minutes" -> Quadruple(
                    GoalType.ACTIVITY.name,
                    "Increase Active Minutes",
                    "minutes",
                    false
                )
                else -> Quadruple(
                    GoalType.OTHER.name,
                    "Improve Health Metric",
                    "units",
                    false
                )
            }
            
            // Set target value based on current average and recommended value
            val targetValue = recommendedValue
            
            // Create a goal with standard parameters
            return HealthGoalEntity(
                userId = userId,
                goalType = goalType,
                metricType = metricType,
                title = title,
                description = "System-suggested goal based on your health data",
                targetValue = targetValue,
                startValue = currentAverage,
                currentValue = currentAverage,
                unit = unit,
                startDate = LocalDate.now(),
                targetDate = LocalDate.now().plusWeeks(4), // 4-week goal
                frequency = GoalFrequency.DAILY.name,
                priority = GoalPriority.MEDIUM.name,
                status = GoalStatus.ACTIVE.name,
                achievementPercentage = calculateAchievementPercentage(
                    currentAverage, targetValue, currentAverage, isInverse
                ),
                isSystemSuggested = true,
                remindersEnabled = true
            )
        }

        /**
         * Check if a goal needs to be updated to inactive status
         * @param status Current goal status
         * @param targetDate Target completion date
         * @return True if the goal should be marked as inactive
         */
        fun shouldMarkInactive(status: GoalStatus, targetDate: LocalDate?): Boolean {
            if (status != GoalStatus.ACTIVE) return false
            if (targetDate == null) return false
            
            return targetDate.isBefore(LocalDate.now())
        }

        /**
         * Data class to hold four related values
         */
        private data class Quadruple<A, B, C, D>(
            val first: A,
            val second: B,
            val third: C,
            val fourth: D
        )
    }
}

/**
 * GoalProgressEntity - Room database entity for tracking daily progress towards goals
 *
 * This entity stores detailed progress records for each health goal, including:
 * - Daily progress values and achievement status
 * - Completion timestamps and durations
 * - Progress notes and user feedback
 * - Achievement streaks and milestones
 *
 * The entity enables the app to track detailed progress history,
 * generate progress charts, and provide insights into goal achievement patterns.
 */
@Entity(
    tableName = "goal_progress",
    foreignKeys = [
        ForeignKey(
            entity = HealthGoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("goalId"),
        Index("userId"),
        Index("date"),
        Index("isAchieved")
    ]
)
data class GoalProgressEntity(
    /**
     * Primary key - unique identifier for the progress record
     */
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    /**
     * Foreign key reference to the associated health goal
     */
    @ColumnInfo(name = "goalId")
    val goalId: String,

    /**
     * User identifier - allows for multi-user support
     */
    @ColumnInfo(name = "userId")
    val userId: String,

    /**
     * Date of the progress record
     */
    @ColumnInfo(name = "date")
    val date: LocalDate,

    /**
     * Progress value for the day
     */
    @ColumnInfo(name = "value")
    val value: Double,

    /**
     * Target value for the day (may differ from overall goal for adaptive goals)
     */
    @ColumnInfo(name = "targetValue")
    val targetValue: Double,

    /**
     * Flag indicating if the daily goal was achieved
     */
    @ColumnInfo(name = "isAchieved")
    val isAchieved: Boolean,

    /**
     * Achievement percentage for the day (0-100)
     */
    @ColumnInfo(name = "achievementPercentage")
    val achievementPercentage: Int,

    /**
     * Time when the progress was recorded
     */
    @ColumnInfo(name = "recordedAt")
    val recordedAt: LocalDateTime,

    /**
     * Time when the goal was completed (if achieved)
     */
    @ColumnInfo(name = "completedAt")
    val completedAt: LocalDateTime? = null,

    /**
     * Duration taken to complete the goal in minutes (if applicable)
     */
    @ColumnInfo(name = "completionDurationMinutes")
    val completionDurationMinutes: Int? = null,

    /**
     * Notes about the progress
     */
    @ColumnInfo(name = "notes")
    val notes: String? = null,

    /**
     * User's difficulty rating (1-5, where 1 is easy and 5 is very difficult)
     */
    @ColumnInfo(name = "difficultyRating")
    val difficultyRating: Int? = null,

    /**
     * User's satisfaction rating (1-5, where 1 is not satisfied and 5 is very satisfied)
     */
    @ColumnInfo(name = "satisfactionRating")
    val satisfactionRating: Int? = null,

    /**
     * Mood associated with the progress (HAPPY, NEUTRAL, SAD, etc.)
     */
    @ColumnInfo(name = "mood")
    val mood: String? = null,

    /**
     * Energy level during goal pursuit (1-5)
     */
    @ColumnInfo(name = "energyLevel")
    val energyLevel: Int? = null,

    /**
     * Streak count as of this progress record
     */
    @ColumnInfo(name = "streakCount")
    val streakCount: Int = 0,

    /**
     * Flag indicating if this progress record hit a milestone
     */
    @ColumnInfo(name = "isMilestone")
    val isMilestone: Boolean = false,

    /**
     * Milestone description (if applicable)
     */
    @ColumnInfo(name = "milestoneDescription")
    val milestoneDescription: String? = null,

    /**
     * Flag indicating if this progress was manually entered
     */
    @ColumnInfo(name = "isManualEntry")
    val isManualEntry: Boolean = false,

    /**
     * Device ID that recorded this progress (if applicable)
     */
    @ColumnInfo(name = "deviceId")
    val deviceId: String? = null,

    /**
     * Timestamp when this record was created
     */
    @ColumnInfo(name = "createdAt")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    /**
     * Timestamp when this record was last modified
     */
    @ColumnInfo(name = "modifiedAt")
    val modifiedAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * Convert entity to domain model
     */
    fun toDomainModel(): GoalProgress {
        return GoalProgress(
            id = id,
            goalId = goalId,
            userId = userId,
            date = date,
            value = value,
            targetValue = targetValue,
            isAchieved = isAchieved,
            achievementPercentage = achievementPercentage,
            recordedAt = recordedAt,
            completedAt = completedAt,
            completionDurationMinutes = completionDurationMinutes,
            notes = notes,
            difficultyRating = difficultyRating,
            satisfactionRating = satisfactionRating,
            mood = mood?.let { Mood.valueOf(it) },
            energyLevel = energyLevel,
            streakCount = streakCount,
            isMilestone = isMilestone,
            milestoneDescription = milestoneDescription,
            isManualEntry = isManualEntry,
            deviceId = deviceId,
            createdAt = createdAt,
            modifiedAt = modifiedAt
        )
    }

    companion object {
        /**
         * Convert domain model to entity
         */
        fun fromDomainModel(domainModel: GoalProgress): GoalProgressEntity {
            return GoalProgressEntity(
                id = domainModel.id,
                goalId = domainModel.goalId,
                userId = domainModel.userId,
                date = domainModel.date,
                value = domainModel.value,
                targetValue = domainModel.targetValue,
                isAchieved = domainModel.isAchieved,
                achievementPercentage = domainModel.achievementPercentage,
                recordedAt = domainModel.recordedAt,
                completedAt = domainModel.completedAt,
                completionDurationMinutes = domainModel.completionDurationMinutes,
                notes = domainModel.notes,
                difficultyRating = domainModel.difficultyRating,
                satisfactionRating = domainModel.satisfactionRating,
                mood = domainModel.mood?.name,
                energyLevel = domainModel.energyLevel,
                streakCount = domainModel.streakCount,
                isMilestone = domainModel.isMilestone,
                milestoneDescription = domainModel.milestoneDescription,
                isManualEntry = domainModel.isManualEntry,
                deviceId = domainModel.deviceId,
                createdAt = domainModel.createdAt,
                modifiedAt = domainModel.modifiedAt
            )
        }

        /**
         * Create a new progress record for a goal
         * @param goal The health goal
         * @param value Progress value for the day
         * @param isManualEntry Whether this is a manual entry
         * @param deviceId Device ID if recorded automatically
         * @return A new progress entity
         */
        fun createProgressRecord(
            goal: HealthGoalEntity,
            value: Double,
            isManualEntry: Boolean = false,
            deviceId: String? = null
        ): GoalProgressEntity {
            val today = LocalDate.now()
            val now = LocalDateTime.now()
            
            // Determine if the goal is achieved
            val isInverse = when (GoalType.valueOf(goal.goalType)) {
                GoalType.WEIGHT, GoalType.BLOOD_PRESSURE, GoalType.STRESS -> true
                else -> false
            }
            
            val isAchieved = HealthGoalEntity.isGoalAchieved(value, goal.targetValue, isInverse)
            
            // Calculate achievement percentage
            val achievementPercentage = HealthGoalEntity.calculateAchievementPercentage(
                value, goal.targetValue, goal.startValue, isInverse
            )
            
            // Check if this is a milestone (25%, 50%, 75%, 100%)
            val isMilestone = achievementPercentage in listOf(25, 50, 75, 100)
            val milestoneDescription = when (achievementPercentage) {
                25 -> "25% of goal achieved"
                50 -> "Halfway to your goal!"
                75 -> "75% of goal achieved"
                100 -> "Goal achieved!"
                else -> null
            }
            
            // Create the progress record
            return GoalProgressEntity(
                goalId = goal.id,
                userId = goal.userId,
                date = today,
                value = value,
                targetValue = goal.targetValue,
                isAchieved = isAchieved,
                achievementPercentage = achievementPercentage,
                recordedAt = now,
                completedAt = if (isAchieved) now else null,
                streakCount = if (isAchieved) goal.currentStreak + 1 else 0,
                isMilestone = isMilestone,
                milestoneDescription = milestoneDescription,
                isManualEntry = isManualEntry,
                deviceId = deviceId
            )
        }

        /**
         * Check if this progress represents a new personal best
         * @param value Current progress value
         * @param previousBest Previous best value
         * @param isInverse Whether lower values are better (e.g., weight loss)
         * @return True if this is a new personal best
         */
        fun isPersonalBest(
            value: Double,
            previousBest: Double?,
            isInverse: Boolean = false
        ): Boolean {
            if (previousBest == null) return true
            
            return if (!isInverse) {
                value > previousBest
            } else {
                value < previousBest
            }
        }

        /**
         * Calculate completion time in minutes
         * @param startTime Time when goal tracking started
         * @param completionTime Time when goal was achieved
         * @return Duration in minutes
         */
        fun calculateCompletionTime(
            startTime: LocalDateTime,
            completionTime: LocalDateTime
        ): Int {
            return ChronoUnit.MINUTES.between(startTime, completionTime).toInt()
        }

        /**
         * Check if a goal is on track based on progress history
         * @param currentProgress Current progress value
         * @param targetValue Target goal value
         * @param daysElapsed Days elapsed since goal start
         * @param totalDays Total days for goal completion
         * @param isInverse Whether the goal is to decrease the value
         * @return True if the goal is on track
         */
        fun isGoalOnTrack(
            currentProgress: Double,
            targetValue: Double,
            startValue: Double?,
            daysElapsed: Int,
            totalDays: Int,
            isInverse: Boolean = false
        ): Boolean {
            // For goals without a start value
            if (startValue == null) {
                val expectedProgress = (targetValue * daysElapsed) / totalDays
                return if (!isInverse) {
                    currentProgress >= expectedProgress
                } else {
                    currentProgress <= expectedProgress
                }
            }
            
            // For goals with a start value (e.g., weight loss)
            val totalChange = if (!isInverse) {
                targetValue - startValue
            } else {
                startValue - targetValue
            }
            
            val expectedChange = (totalChange * daysElapsed) / totalDays
            val actualChange = if (!isInverse) {
                currentProgress - startValue
            } else {
                startValue - currentProgress
            }
            
            return actualChange >= expectedChange
        }

        /**
         * Calculate the trend direction based on recent progress
         * @param recentValues List of recent progress values (oldest first)
         * @param isInverse Whether lower values are better
         * @return Trend direction (UP, DOWN, STABLE)
         */
        fun calculateTrendDirection(
            recentValues: List<Double>,
            isInverse: Boolean = false
        ): TrendDirection {
            if (recentValues.size < 2) return TrendDirection.STABLE
            
            // Calculate average change
            var totalChange = 0.0
            for (i in 1 until recentValues.size) {
                totalChange += recentValues[i] - recentValues[i-1]
            }
            
            val averageChange = totalChange / (recentValues.size - 1)
            
            // Determine trend direction
            return when {
                averageChange.absoluteValue < 0.01 -> TrendDirection.STABLE
                (!isInverse && averageChange > 0) || (isInverse && averageChange < 0) -> TrendDirection.UP
                else -> TrendDirection.DOWN
            }
        }
    }
}

/**
 * HealthAlertEntity - Room database entity for health alerts and notifications
 *
 * This entity stores health alerts generated based on user health data, including:
 * - Alert type and severity classification
 * - Alert message and recommendations
 * - Related health metrics and threshold violations
 * - User acknowledgment and response tracking
 * - Emergency contact notification status
 *
 * The entity enables the app to notify users about potential health issues,
 * track user responses to alerts, and manage emergency notifications.
 */
@Entity(
    tableName = "health_alert",
    indices = [
        Index("userId"),
        Index("alertType"),
        Index("severity"),
        Index("timestamp"),
        Index("isAcknowledged"),
        Index("isEmergency")
    ]
)
data class HealthAlertEntity(
    /**
     * Primary key - unique identifier for the alert
     */
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    /**
     * User identifier - allows for multi-user support
     */
    @ColumnInfo(name = "userId")
    val userId: String,

    /**
     * Type of health alert (heart_rate, blood_pressure, etc.)
     */
    @ColumnInfo(name = "alertType")
    val alertType: String,

    /**
     * Alert severity level (LOW, MEDIUM, HIGH, EMERGENCY)
     */
    @ColumnInfo(name = "severity")
    val severity: String,

    /**
     * Alert title
     */
    @ColumnInfo(name = "title")
    val title: String,

    /**
     * Detailed alert message
     */
    @ColumnInfo(name = "message")
    val message: String,

    /**
     * Recommended actions to take
     */
    @ColumnInfo(name = "recommendation")
    val recommendation: String,

    /**
     * Timestamp when the alert was generated
     */
    @ColumnInfo(name = "timestamp")
    val timestamp: LocalDateTime,

    /**
     * ID of the health data that triggered the alert
     */
    @ColumnInfo(name = "triggeringDataId")
    val triggeringDataId: String? = null,

    /**
     * Value that triggered the alert
     */
    @ColumnInfo(name = "triggeringValue")
    val triggeringValue: Double? = null,

    /**
     * Threshold value that was exceeded
     */
    @ColumnInfo(name = "thresholdValue")
    val thresholdValue: Double? = null,

    /**
     * ID of the alert rule that generated this alert
     */
    @ColumnInfo(name = "alertRuleId")
    val alertRuleId: String? = null,

    /**
     * Flag indicating if this is an emergency alert
     */
    @ColumnInfo(name = "isEmergency")
    val isEmergency: Boolean = false,

    /**
     * Flag indicating if emergency contacts were notified
     */
    @ColumnInfo(name = "emergencyContactsNotified")
    val emergencyContactsNotified: Boolean = false,

    /**
     * Timestamp when emergency contacts were notified
     */
    @ColumnInfo(name = "emergencyNotificationTime")
    val emergencyNotificationTime: LocalDateTime? = null,

    /**
     * Flag indicating if the alert was acknowledged by the user
     */
    @ColumnInfo(name = "isAcknowledged")
    val isAcknowledged: Boolean = false,

    /**
     * Timestamp when the alert was acknowledged
     */
    @ColumnInfo(name = "acknowledgedAt")
    val acknowledgedAt: LocalDateTime? = null,

    /**
     * User's response to the alert
     */
    @ColumnInfo(name = "userResponse")
    val userResponse: String? = null,

    /**
     * Flag indicating if this alert was dismissed
     */
    @ColumnInfo(name = "isDismissed")
    val isDismissed: Boolean = false,

    /**
     * Flag indicating if a notification was sent for this alert
     */
    @ColumnInfo(name = "notificationSent")
    val notificationSent: Boolean = false,

    /**
     * Timestamp when the notification was sent
     */
    @ColumnInfo(name = "notificationSentAt")
    val notificationSentAt: LocalDateTime? = null,

    /**
     * Flag indicating if the notification was opened
     */
    @ColumnInfo(name = "notificationOpened")
    val notificationOpened: Boolean = false,

    /**
     * Timestamp when the notification was opened
     */
    @ColumnInfo(name = "notificationOpenedAt")
    val notificationOpenedAt: LocalDateTime? = null,

    /**
     * Time to acknowledge the alert in minutes
     */
    @ColumnInfo(name = "timeToAcknowledge")
    val timeToAcknowledge: Int? = null,

    /**
     * Timestamp when this record was created
     */
    @ColumnInfo(name = "createdAt")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    /**
     * Timestamp when this record was last modified
     */
    @ColumnInfo(name = "modifiedAt")
    val modifiedAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * Convert entity to domain model
     */
    fun toDomainModel(): HealthAlert {
        return HealthAlert(
            id = id,
            userId = userId,
            alertType = AlertType.valueOf(alertType),
            severity = AlertSeverity.valueOf(severity),
            title = title,
            message = message,
            recommendation = recommendation,
            timestamp = timestamp,
            triggeringDataId = triggeringDataId,
            triggeringValue = triggeringValue,
            thresholdValue = thresholdValue,
            alertRuleId = alertRuleId,
            isEmergency = isEmergency,
            emergencyContactsNotified = emergencyContactsNotified,
            emergencyNotificationTime = emergencyNotificationTime,
            isAcknowledged = isAcknowledged,
            acknowledgedAt = acknowledgedAt,
            userResponse = userResponse,
            isDismissed = isDismissed,
            notificationSent = notificationSent,
            notificationSentAt = notificationSentAt,
            notificationOpened = notificationOpened,
            notificationOpenedAt = notificationOpenedAt,
            timeToAcknowledge = timeToAcknowledge,
            createdAt = createdAt,
            modifiedAt = modifiedAt
        )
    }

    companion object {
        /**
         * Convert domain model to entity
         */
        fun fromDomainModel(domainModel: HealthAlert): HealthAlertEntity {
            return HealthAlertEntity(
                id = domainModel.id,
                userId = domainModel.userId,
                alertType = domainModel.alertType.name,
                severity = domainModel.severity.name,
                title = domainModel.title,
                message = domainModel.message,
                recommendation = domainModel.recommendation,
                timestamp = domainModel.timestamp,
                triggeringDataId = domainModel.triggeringDataId,
                triggeringValue = domainModel.triggeringValue,
                thresholdValue = domainModel.thresholdValue,
                alertRuleId = domainModel.alertRuleId,
                isEmergency = domainModel.isEmergency,
                emergencyContactsNotified = domainModel.emergencyContactsNotified,
                emergencyNotificationTime = domainModel.emergencyNotificationTime,
                isAcknowledged = domainModel.isAcknowledged,
                acknowledgedAt = domainModel.acknowledgedAt,
                userResponse = domainModel.userResponse,
                isDismissed = domainModel.isDismissed,
                notificationSent = domainModel.notificationSent,
                notificationSentAt = domainModel.notificationSentAt,
                notificationOpened = domainModel.notificationOpened,
                notificationOpenedAt = domainModel.notificationOpenedAt,
                timeToAcknowledge = domainModel.timeToAcknowledge,
                createdAt = domainModel.createdAt,
                modifiedAt = domainModel.modifiedAt
            )
        }

        /**
         * Create a health alert based on a threshold violation
         * @param userId User identifier
         * @param alertType Type of alert
         * @param triggeringValue Value that triggered the alert
         * @param thresholdValue Threshold that was exceeded
         * @param triggeringDataId ID of the health data that triggered the alert
         * @param alertRuleId ID of the alert rule
         * @return A new health alert entity
         */
        fun createAlert(
            userId: String,
            alertType: AlertType,
            triggeringValue: Double,
            thresholdValue: Double,
            triggeringDataId: String? = null,
            alertRuleId: String? = null
        ): HealthAlertEntity {
            // Determine severity based on how much the threshold was exceeded
            val severity = determineSeverity(alertType, triggeringValue, thresholdValue)
            
            // Generate title and message based on alert type and severity
            val (title, message, recommendation) = generateAlertContent(
                alertType, severity, triggeringValue, thresholdValue
            )
            
            // Determine if this is an emergency alert
            val isEmergency = severity == AlertSeverity.EMERGENCY
            
            return HealthAlertEntity(
                userId = userId,
                alertType = alertType.name,
                severity = severity.name,
                title = title,
                message = message,
                recommendation = recommendation,
                timestamp = LocalDateTime.now(),
                triggeringDataId = triggeringDataId,
                triggeringValue = triggeringValue,
                thresholdValue = thresholdValue,
                alertRuleId = alertRuleId,
                isEmergency = isEmergency
            )
        }

        /**
         * Determine alert severity based on how much the threshold was exceeded
         * @param alertType Type of alert
         * @param value Measured value
         * @param threshold Threshold value
         * @return Alert severity level
         */
        fun determineSeverity(
            alertType: AlertType,
            value: Double,
            threshold: Double
        ): AlertSeverity {
            // Calculate percentage deviation from threshold
            val deviation = when (alertType) {
                // For these types, higher values are concerning
                AlertType.HEART_RATE_HIGH,
                AlertType.BLOOD_PRESSURE_HIGH,
                AlertType.GLUCOSE_HIGH,
                AlertType.TEMPERATURE_HIGH -> {
                    ((value - threshold) / threshold) * 100
                }
                
                // For these types, lower values are concerning
                AlertType.HEART_RATE_LOW,
                AlertType.BLOOD_PRESSURE_LOW,
                AlertType.GLUCOSE_LOW,
                AlertType.OXYGEN_LOW,
                AlertType.TEMPERATURE_LOW -> {
                    ((threshold - value) / threshold) * 100
                }
                
                // For other types, use a default approach
                else -> abs(((value - threshold) / threshold) * 100)
            }
            
            // Assign severity based on deviation percentage and alert type
            return when {
                // Emergency conditions for specific alert types
                (alertType == AlertType.HEART_RATE_HIGH && value > 180) ||
                (alertType == AlertType.HEART_RATE_LOW && value < 40) ||
                (alertType == AlertType.BLOOD_PRESSURE_HIGH && value > 180) ||
                (alertType == AlertType.OXYGEN_LOW && value < 90) ||
                (alertType == AlertType.TEMPERATURE_HIGH && value > 39.5) ||
                (alertType == AlertType.GLUCOSE_LOW && value < 54) ||
                (alertType == AlertType.GLUCOSE_HIGH && value > 250) ||
                deviation > 50 -> AlertSeverity.EMERGENCY
                
                deviation > 30 -> AlertSeverity.HIGH
                deviation > 15 -> AlertSeverity.MEDIUM
                else -> AlertSeverity.LOW
            }
        }

        /**
         * Generate alert content based on type and severity
         * @param alertType Type of alert
         * @param severity Alert severity
         * @param value Measured value
         * @param threshold Threshold value
         * @return Triple of (title, message, recommendation)
         */
        private fun generateAlertContent(
            alertType: AlertType,
            severity: AlertSeverity,
            value: Double,
            threshold: Double
        ): Triple<String, String, String> {
            // Format the value and threshold based on alert type
            val formattedValue = formatValue(alertType, value)
            val formattedThreshold = formatValue(alertType, threshold)
            
            // Generate alert content based on type and severity
            return when (alertType) {
                AlertType.HEART_RATE_HIGH -> {
                    val title = "Elevated Heart Rate Detected"
                    val message = "Your heart rate of $formattedValue is above the threshold of $formattedThreshold."
                    val recommendation = when (severity) {
                        AlertSeverity.LOW, AlertSeverity.MEDIUM -> 
                            "Consider resting and monitoring your heart rate. Stay hydrated."
                        AlertSeverity.HIGH -> 
                            "Sit down, take slow deep breaths, and rest. Monitor your heart rate."
                        AlertSeverity.EMERGENCY -> 
                            "Seek immediate medical attention if you also feel chest pain, shortness of breath, or dizziness."
                    }
                    Triple(title, message, recommendation)
                }
                
                AlertType.HEART_RATE_LOW -> {
                    val title = "Low Heart Rate Detected"
                    val message = "Your heart rate of $formattedValue is below the threshold of $formattedThreshold."
                    val recommendation = when (severity) {
                        AlertSeverity.LOW, AlertSeverity.MEDIUM -> 
                            "Monitor your heart rate. This may be normal if you're physically fit or resting."
                        AlertSeverity.HIGH -> 
                            "Sit or lie down. If you feel dizzy or lightheaded, have someone stay with you."
                        AlertSeverity.EMERGENCY -> 
                            "Seek immediate medical attention if you feel faint, dizzy, or have difficulty breathing."
                    }
                    Triple(title, message, recommendation)
                }
                
                AlertType.BLOOD_PRESSURE_HIGH -> {
                    val title = "Elevated Blood Pressure Detected"
                    val message = "Your blood pressure of $formattedValue is above the threshold of $formattedThreshold."
                    val recommendation = when (severity) {
                        AlertSeverity.LOW, AlertSeverity.MEDIUM -> 
                            "Rest for 5 minutes and take another reading. Reduce sodium intake and stress."
                        AlertSeverity.HIGH -> 
                            "Rest in a quiet place. If your reading remains high after 30 minutes, contact your doctor."
                        AlertSeverity.EMERGENCY -> 
                            "Seek immediate medical attention, especially if you have headache, vision changes, or chest pain."
                    }
                    Triple(title, message, recommendation)
                }
                
                AlertType.BLOOD_PRESSURE_LOW -> {
                    val title = "Low Blood Pressure Detected"
                    val message = "Your blood pressure of $formattedValue is below the threshold of $formattedThreshold."
                    val recommendation = when (severity) {
                        AlertSeverity.LOW, AlertSeverity.MEDIUM -> 
                            "Stay hydrated and consider increasing salt intake slightly if recommended by your doctor."
                        AlertSeverity.HIGH -> 
                            "Sit or lie down and elevate your legs. Drink water and have a salty snack if possible."
                        AlertSeverity.EMERGENCY -> 
                            "Seek immediate medical attention if you feel faint, dizzy, or have blurred vision."
                    }
                    Triple(title, message, recommendation)
                }
                
                AlertType.OXYGEN_LOW -> {
                    val title = "Low Oxygen Level Detected"
                    val message = "Your oxygen saturation of $formattedValue is below the threshold of $formattedThreshold."
                    val recommendation = when (severity) {
                        AlertSeverity.LOW, AlertSeverity.MEDIUM -> 
                            "Take slow, deep breaths. Sit upright and ensure you're in well-ventilated area."
                        AlertSeverity.HIGH -> 
                            "Stop any activity, sit upright, and focus on slow deep breathing. Monitor your levels."
                        AlertSeverity.EMERGENCY -> 
                            "Seek immediate medical attention. Low oxygen levels can be dangerous."
                    }
                    Triple(title, message, recommendation)
                }
                
                AlertType.GLUCOSE_HIGH -> {
                    val title = "High Blood Glucose Detected"
                    val message = "Your blood glucose of $formattedValue is above the threshold of $formattedThreshold."
                    val recommendation = when (severity) {
                        AlertSeverity.LOW, AlertSeverity.MEDIUM -> 
                            "Drink water and consider light exercise if approved by your doctor. Monitor your levels."
                        AlertSeverity.HIGH -> 
                            "Take insulin if prescribed. Drink water and monitor your levels closely."
                        AlertSeverity.EMERGENCY -> 
                            "Seek immediate medical attention. Very high blood glucose can lead to serious complications."
                    }
                    Triple(title, message, recommendation)
                }
                
                AlertType.GLUCOSE_LOW -> {
                    val title = "Low Blood Glucose Detected"
                    val message = "Your blood glucose of $formattedValue is below the threshold of $formattedThreshold."
                    val recommendation = when (severity) {
                        AlertSeverity.LOW, AlertSeverity.MEDIUM -> 
                            "Consume 15g of fast-acting carbohydrates like juice or glucose tablets."
                        AlertSeverity.HIGH -> 
                            "Consume 15-20g of fast-acting carbohydrates immediately. Have someone stay with you."
                        AlertSeverity.EMERGENCY -> 
                            "Seek immediate medical attention. If conscious, consume sugar. If unconscious, emergency services are needed."
                    }
                    Triple(title, message, recommendation)
                }
                
                AlertType.TEMPERATURE_HIGH -> {
                    val title = "Elevated Body Temperature Detected"
                    val message = "Your body temperature of $formattedValue is above the threshold of $formattedThreshold."
                    val recommendation = when (severity) {
                        AlertSeverity.LOW, AlertSeverity.MEDIUM -> 
                            "Rest, stay hydrated, and consider taking fever-reducing medication if appropriate."
                        AlertSeverity.HIGH -> 
                            "Take fever-reducing medication, use cool compresses, and drink plenty of fluids."
                        AlertSeverity.EMERGENCY -> 
                            "Seek immediate medical attention. High fever can lead to serious complications."
                    }
                    Triple(title, message, recommendation)
                }
                
                AlertType.TEMPERATURE_LOW -> {
                    val title = "Low Body Temperature Detected"
                    val message = "Your body temperature of $formattedValue is below the threshold of $formattedThreshold."
                    val recommendation = when (severity) {
                        AlertSeverity.LOW, AlertSeverity.MEDIUM -> 
                            "Warm up gradually with blankets and warm (not hot) drinks."
                        AlertSeverity.HIGH -> 
                            "Move to a warm environment, use blankets, and drink warm fluids. Have someone stay with you."
                        AlertSeverity.EMERGENCY -> 
                            "Seek immediate medical attention. Low body temperature can be dangerous."
                    }
                    Triple(title, message, recommendation)
                }
                
                AlertType.IRREGULAR_HEARTBEAT -> {
                    val title = "Irregular Heartbeat Detected"
                    val message = "An irregular heart rhythm has been detected in your recent measurements."
                    val recommendation = when (severity) {
                        AlertSeverity.LOW, AlertSeverity.MEDIUM -> 
                            "Monitor your heart rate and rhythm. Consider discussing with your doctor at your next visit."
                        AlertSeverity.HIGH -> 
                            "Rest and avoid stimulants like caffeine. Contact your doctor within 24 hours."
                        AlertSeverity.EMERGENCY -> 
                            "Seek immediate medical attention, especially if you feel chest pain, dizziness, or shortness of breath."
                    }
                    Triple(title, message, recommendation)
                }
                
                AlertType.SLEEP_APNEA -> {
                    val title = "Possible Sleep Apnea Detected"
                    val message = "Your sleep data shows patterns consistent with sleep apnea."
                    val recommendation = "Discuss these findings with your healthcare provider. They may recommend a sleep study for proper diagnosis."
                    Triple(title, message, recommendation)
                }
                
                AlertType.STRESS_HIGH -> {
                    val title = "Elevated Stress Level Detected"
                    val message = "Your stress level of $formattedValue is above your typical baseline."
                    val recommendation = "Consider stress reduction techniques like deep breathing, meditation, or light exercise. Ensure you're getting adequate rest."
                    Triple(title, message, recommendation)
                }
                
                AlertType.ACTIVITY_LOW -> {
                    val title = "Low Activity Level Alert"
                    val message = "Your activity level has been below your goal for several days."
                    val recommendation = "Try to incorporate more movement into your day. Even short walks can be beneficial for your health."
                    Triple(title, message, recommendation)
                }
                
                AlertType.DEHYDRATION -> {
                    val title = "Possible Dehydration Alert"
                    val message = "Your hydration metrics suggest you may be dehydrated."
                    val recommendation = "Increase your fluid intake, especially water. Monitor for symptoms like dry mouth, headache, or dark urine."
                    Triple(title, message, recommendation)
                }
                
                else -> {
                    val title = "Health Alert"
                    val message = "A health metric requires your attention."
                    val recommendation = "Please review your health data and consult with a healthcare professional if needed."
                    Triple(title, message, recommendation)
                }
            }
        }

        /**
         * Format value based on alert type
         * @param alertType Type of alert
         * @param value Value to format
         * @return Formatted value string
         */
        private fun formatValue(alertType: AlertType, value: Double): String {
            return when (alertType) {
                AlertType.HEART_RATE_HIGH, AlertType.HEART_RATE_LOW -> 
                    "${value.roundToInt()} bpm"
                AlertType.BLOOD_PRESSURE_HIGH, AlertType.BLOOD_PRESSURE_LOW -> 
                    "${value.roundToInt()} mmHg"
                AlertType.OXYGEN_LOW -> 
                    "${value.roundToInt()}%"
                AlertType.GLUCOSE_HIGH, AlertType.GLUCOSE_LOW -> 
                    "${value.roundToInt()} mg/dL"
                AlertType.TEMPERATURE_HIGH, AlertType.TEMPERATURE_LOW -> 
                    String.format("%.1f°C", value)
                else -> value.toString()
            }
        }

        /**
         * Calculate time to acknowledge in minutes
         * @param alertTimestamp Time when the alert was generated
         * @param acknowledgedAt Time when the alert was acknowledged
         * @return Time to acknowledge in minutes
         */
        fun calculateTimeToAcknowledge(
            alertTimestamp: LocalDateTime,
            acknowledgedAt: LocalDateTime
        ): Int {
            return ChronoUnit.MINUTES.between(alertTimestamp, acknowledgedAt).toInt()
        }

        /**
         * Check if an alert should be auto-escalated based on time since creation
         * @param alert The health alert to check
         * @param currentTime Current time
         * @return True if the alert should be escalated
         */
        fun shouldEscalateAlert(
            alert: HealthAlertEntity,
            currentTime: LocalDateTime = LocalDateTime.now()
        ): Boolean {
            // Don't escalate acknowledged or already emergency alerts
            if (alert.isAcknowledged || alert.severity == AlertSeverity.EMERGENCY.name) {
                return false
            }
            
            // Calculate time since alert was created
            val minutesSinceCreation = ChronoUnit.MINUTES.between(alert.timestamp, currentTime)
            
            // Escalation thresholds based on current severity
            return when (AlertSeverity.valueOf(alert.severity)) {
                AlertSeverity.HIGH -> minutesSinceCreation >= 30
                AlertSeverity.MEDIUM -> minutesSinceCreation >= 60
                AlertSeverity.LOW -> minutesSinceCreation >= 120
                else -> false
            }
        }

        /**
         * Get the next severity level for escalation
         * @param currentSeverity Current alert severity
         * @return Next severity level, or same if already at maximum
         */
        fun getEscalatedSeverity(currentSeverity: AlertSeverity): AlertSeverity {
            return when (currentSeverity) {
                AlertSeverity.LOW -> AlertSeverity.MEDIUM
                AlertSeverity.MEDIUM -> AlertSeverity.HIGH
                AlertSeverity.HIGH -> AlertSeverity.EMERGENCY
                AlertSeverity.EMERGENCY -> AlertSeverity.EMERGENCY
            }
        }

        /**
         * Absolute value function for Double
         */
        private fun abs(value: Double): Double = if (value < 0) -value else value
    }
}

/**
 * AlertRuleEntity - Room database entity for customizable alert thresholds
 *
 * This entity stores user-defined rules for generating health alerts, including:
 * - Metric type and threshold values
 * - Condition operators (above, below, between)
 * - Time constraints and frequency limits
 * - Alert message customization
 * - Rule priority and activation status
 *
 * The entity enables the app to generate personalized health alerts
 * based on user-specific thresholds and conditions.
 */
@Entity(
    tableName = "alert_rule",
    indices = [
        Index("userId"),
        Index("metricType"),
        Index("isActive")
    ]
)
data class AlertRuleEntity(
    /**
     * Primary key - unique identifier for the rule
     */
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    /**
     * User identifier - allows for multi-user support
     */
    @ColumnInfo(name = "userId")
    val userId: String,

    /**
     * Name of the alert rule
     */
    @ColumnInfo(name = "name")
    val name: String,

    /**
     * Description of the alert rule
     */
    @ColumnInfo(name = "description")
    val description: String? = null,

    /**
     * Type of health metric to monitor
     */
    @ColumnInfo(name = "metricType")
    val metricType: String,

    /**
     * Condition operator (ABOVE, BELOW, BETWEEN, OUTSIDE)
     */
    @ColumnInfo(name = "conditionOperator")
    val conditionOperator: String,

    /**
     * Threshold value
     */
    @ColumnInfo(name = "thresholdValue")
    val thresholdValue: Double,

    /**
     * Secondary threshold value (for BETWEEN and OUTSIDE conditions)
     */
    @ColumnInfo(name = "secondaryThresholdValue")
    val secondaryThresholdValue: Double? = null,

    /**
     * Unit of measurement
     */
    @ColumnInfo(name = "unit")
    val unit: String,

    /**
     * Alert type to generate
     */
    @ColumnInfo(name = "alertType")
    val alertType: String,

    /**
     * Default severity for alerts generated by this rule
     */
    @ColumnInfo(name = "defaultSeverity")
    val defaultSeverity: String,

    /**
     * Custom alert title
     */
    @ColumnInfo(name = "customTitle")
    val customTitle: String? = null,

    /**
     * Custom alert message
     */
    @ColumnInfo(name = "customMessage")
    val customMessage: String? = null,

    /**
     * Custom recommendation
     */
    @ColumnInfo(name = "customRecommendation")
    val customRecommendation: String? = null,

    /**
     * Time window in minutes for condition evaluation
     */
    @ColumnInfo(name = "timeWindowMinutes")
    val timeWindowMinutes: Int? = null,

    /**
     * Number of occurrences required to trigger alert
     */
    @ColumnInfo(name = "occurrencesRequired")
    val occurrencesRequired: Int = 1,

    /**
     * Minimum time between alerts in minutes
     */
    @ColumnInfo(name = "cooldownMinutes")
    val cooldownMinutes: Int = 60,

    /**
     * Flag indicating if the rule is active
     */
    @ColumnInfo(name = "isActive")
    val isActive: Boolean = true,

    /**
     * Flag indicating if this is a system-defined rule
     */
    @ColumnInfo(name = "isSystemRule")
    val isSystemRule: Boolean = false,

    /**
     * Priority level for this rule
     */
    @ColumnInfo(name = "priority")
    val priority: Int = 5,

    /**
     * Time of day restriction start (HH:MM)
     */
    @ColumnInfo(name = "timeRestrictionStart")
    val timeRestrictionStart: String? = null,

    /**
     * Time of day restriction end (HH:MM)
     */
    @ColumnInfo(name = "timeRestrictionEnd")
    val timeRestrictionEnd: String? = null,

    /**
     * Days of week when this rule is active (comma-separated)
     */
    @ColumnInfo(name = "activeDaysOfWeek")
    val activeDaysOfWeek: String? = null,

    /**
     * Last time an alert was generated by this rule
     */
    @ColumnInfo(name = "lastTriggeredAt")
    val lastTriggeredAt: LocalDateTime? = null,

    /**
     * Count of alerts generated by this rule
     */
    @ColumnInfo(name = "triggerCount")
    val triggerCount: Int = 0,

    /**
     * Timestamp when this record was created
     */
    @ColumnInfo(name = "createdAt")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    /**
     * Timestamp when this record was last modified
     */
    @ColumnInfo(name = "modifiedAt")
    val modifiedAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * Convert entity to domain model
     */
    fun toDomainModel(): AlertRule {
        return AlertRule(
            id = id,
            userId = userId,
            name = name,
            description = description,
            metricType = metricType,
            conditionOperator = ConditionOperator.valueOf(conditionOperator),
            thresholdValue = thresholdValue,
            secondaryThresholdValue = secondaryThresholdValue,
            unit = unit,
            alertType = AlertType.valueOf(alertType),
            defaultSeverity = AlertSeverity.valueOf(defaultSeverity),
            customTitle = customTitle,
            customMessage = customMessage,
            customRecommendation = customRecommendation,
            timeWindowMinutes = timeWindowMinutes,
            occurrencesRequired = occurrencesRequired,
            cooldownMinutes = cooldownMinutes,
            isActive = isActive,
            isSystemRule = isSystemRule,
            priority = priority,
            timeRestrictionStart = timeRestrictionStart,
            timeRestrictionEnd = timeRestrictionEnd,
            activeDaysOfWeek = activeDaysOfWeek?.split(",")?.mapNotNull { 
                try { DayOfWeek.valueOf(it) } catch (e: Exception) { null }
            }?.toSet(),
            lastTriggeredAt = lastTriggeredAt,
            triggerCount = triggerCount,
            createdAt = createdAt,
            modifiedAt = modifiedAt
        )
    }

    companion object {
        /**
         * Convert domain model to entity
         */
        fun fromDomainModel(domainModel: AlertRule): AlertRuleEntity {
            return AlertRuleEntity(
                id = domainModel.id,
                userId = domainModel.userId,
                name = domainModel.name,
                description = domainModel.description,
                metricType = domainModel.metricType,
                conditionOperator = domainModel.conditionOperator.name,
                thresholdValue = domainModel.thresholdValue,
                secondaryThresholdValue = domainModel.secondaryThresholdValue,
                unit = domainModel.unit,
                alertType = domainModel.alertType.name,
                defaultSeverity = domainModel.defaultSeverity.name,
                customTitle = domainModel.customTitle,
                customMessage = domainModel.customMessage,
                customRecommendation = domainModel.customRecommendation,
                timeWindowMinutes = domainModel.timeWindowMinutes,
                occurrencesRequired = domainModel.occurrencesRequired,
                cooldownMinutes = domainModel.cooldownMinutes,
                isActive = domainModel.isActive,
                isSystemRule = domainModel.isSystemRule,
                priority = domainModel.priority,
                timeRestrictionStart = domainModel.timeRestrictionStart,
                timeRestrictionEnd = domainModel.timeRestrictionEnd,
                activeDaysOfWeek = domainModel.activeDaysOfWeek?.joinToString(",") { it.name },
                lastTriggeredAt = domainModel.lastTriggeredAt,
                triggerCount = domainModel.triggerCount,
                createdAt = domainModel.createdAt,
                modifiedAt = domainModel.modifiedAt
            )
        }

        /**
         * Create a default set of alert rules for a new user
         * @param userId User identifier
         * @return List of default alert rules
         */
        fun createDefaultRules(userId: String): List<AlertRuleEntity> {
            return listOf(
                // Heart rate high alert
                AlertRuleEntity(
                    userId = userId,
                    name = "High Heart Rate Alert",
                    description = "Alerts when heart rate exceeds normal range",
                    metricType = "heart_rate",
                    conditionOperator = ConditionOperator.ABOVE.name,
                    thresholdValue = 120.0,
                    unit = "bpm",
                    alertType = AlertType.HEART_RATE_HIGH.name,
                    defaultSeverity = AlertSeverity.MEDIUM.name,
                    occurrencesRequired = 3,
                    cooldownMinutes = 60,
                    isSystemRule = true,
                    priority = 7
                ),
                
                // Heart rate low alert
                AlertRuleEntity(
                    userId = userId,
                    name = "Low Heart Rate Alert",
                    description = "Alerts when heart rate falls below normal range",
                    metricType = "heart_rate",
                    conditionOperator = ConditionOperator.BELOW.name,
                    thresholdValue = 50.0,
                    unit = "bpm",
                    alertType = AlertType.HEART_RATE_LOW.name,
                    defaultSeverity = AlertSeverity.MEDIUM.name,
                    occurrencesRequired = 3,
                    cooldownMinutes = 60,
                    isSystemRule = true,
                    priority = 7
                ),
                
                // Blood pressure high alert
                AlertRuleEntity(
                    userId = userId,
                    name = "High Blood Pressure Alert",
                    description = "Alerts when systolic blood pressure is elevated",
                    metricType = "blood_pressure_systolic",
                    conditionOperator = ConditionOperator.ABOVE.name,
                    thresholdValue = 140.0,
                    unit = "mmHg",
                    alertType = AlertType.BLOOD_PRESSURE_HIGH.name,
                    defaultSeverity = AlertSeverity.MEDIUM.name,
                    occurrencesRequired = 2,
                    cooldownMinutes = 120,
                    isSystemRule = true,
                    priority = 8
                ),
                
                // Oxygen level low alert
                AlertRuleEntity(
                    userId = userId,
                    name = "Low Oxygen Level Alert",
                    description = "Alerts when blood oxygen saturation falls below safe levels",
                    metricType = "oxygen_saturation",
                    conditionOperator = ConditionOperator.BELOW.name,
                    thresholdValue = 95.0,
                    unit = "%",
                    alertType = AlertType.OXYGEN_LOW.name,
                    defaultSeverity = AlertSeverity.HIGH.name,
                    occurrencesRequired = 2,
                    cooldownMinutes = 30,
                    isSystemRule = true,
                    priority = 9
                ),
                
                // Irregular heartbeat alert
                AlertRuleEntity(
                    userId = userId,
                    name = "Irregular Heartbeat Alert",
                    description = "Alerts when heart rhythm irregularities are detected",
                    metricType = "heart_rate_variability",
                    conditionOperator = ConditionOperator.ABOVE.name,
                    thresholdValue = 25.0,
                    unit = "ms",
                    alertType = AlertType.IRREGULAR_HEARTBEAT.name,
                    defaultSeverity = AlertSeverity.MEDIUM.name,
                    timeWindowMinutes = 60,
                    occurrencesRequired = 3,
                    cooldownMinutes = 240,
                    isSystemRule = true,
                    priority = 8
                ),
                
                // High temperature alert
                AlertRuleEntity(
                    userId = userId,
                    name = "Fever Alert",
                    description = "Alerts when body temperature indicates fever",
                    metricType = "body_temperature",
                    conditionOperator = ConditionOperator.ABOVE.name,
                    thresholdValue = 38.0,
                    unit = "°C",
                    alertType = AlertType.TEMPERATURE_HIGH.name,
                    defaultSeverity = AlertSeverity.MEDIUM.name,
                    occurrencesRequired = 2,
                    cooldownMinutes = 180,
                    isSystemRule = true,
                    priority = 7
                ),
                
                // Low activity alert
                AlertRuleEntity(
                    userId = userId,
                    name = "Low Activity Alert",
                    description = "Alerts when daily activity is below target for several days",
                    metricType = "daily_steps",
                    conditionOperator = ConditionOperator.BELOW.name,
                    thresholdValue = 3000.0,
                    unit = "steps",
                    alertType = AlertType.ACTIVITY_LOW.name,
                    defaultSeverity = AlertSeverity.LOW.name,
                    timeWindowMinutes = 1440, // 24 hours
                    occurrencesRequired = 3, // 3 consecutive days
                    cooldownMinutes = 1440, // Once per day
                    isSystemRule = true,
                    priority = 4
                )
            )
        }

        /**
         * Check if a value meets the rule condition
         * @param value Value to check
         * @param rule Alert rule
         * @return True if the condition is met
         */
        fun checkCondition(value: Double, rule: AlertRuleEntity): Boolean {
            return when (ConditionOperator.valueOf(rule.conditionOperator)) {
                ConditionOperator.ABOVE -> value > rule.thresholdValue
                ConditionOperator.BELOW -> value < rule.thresholdValue
                ConditionOperator.EQUAL -> value == rule.thresholdValue
                ConditionOperator.NOT_EQUAL -> value != rule.thresholdValue
                ConditionOperator.BETWEEN -> {
                    val secondaryThreshold = rule.secondaryThresholdValue
                        ?: return false
                    value in rule.thresholdValue..secondaryThreshold
                }
                ConditionOperator.OUTSIDE -> {
                    val secondaryThreshold = rule.secondaryThresholdValue
                        ?: return false
                    value < rule.thresholdValue || value > secondaryThreshold
                }
            }
        }

        /**
         * Check if the rule is currently active based on time restrictions
         * @param rule Alert rule to check
         * @param currentTime Current time
         * @return True if the rule is active at the current time
         */
        fun isRuleActiveNow(
            rule: AlertRuleEntity,
            currentTime: LocalDateTime = LocalDateTime.now()
        ): Boolean {
            // Check if rule is enabled
            if (!rule.isActive) return false
            
            // Check day of week restrictions
            val currentDayOfWeek = currentTime.dayOfWeek.name
            if (rule.activeDaysOfWeek != null && !rule.activeDaysOfWeek.contains(currentDayOfWeek)) {
                return false
            }
            
            // Check time of day restrictions
            if (rule.timeRestrictionStart != null && rule.timeRestrictionEnd != null) {
                val currentTimeOfDay = currentTime.toLocalTime()
                val startTime = java.time.LocalTime.parse(rule.timeRestrictionStart)
                val endTime = java.time.LocalTime.parse(rule.timeRestrictionEnd)
                
                // Handle cases where time range crosses midnight
                return if (startTime.isBefore(endTime)) {
                    !currentTimeOfDay.isBefore(startTime) && !currentTimeOfDay.isAfter(endTime)
                } else {
                    !currentTimeOfDay.isBefore(startTime) || !currentTimeOfDay.isAfter(endTime)
                }
            }
            
            // Check cooldown period
            if (rule.lastTriggeredAt != null && rule.cooldownMinutes > 0) {
                val minutesSinceLastTrigger = ChronoUnit.MINUTES.between(
                    rule.lastTriggeredAt, currentTime
                )
                if (minutesSinceLastTrigger < rule.cooldownMinutes) {
                    return false
                }
            }
            
            return true
        }

        /**
         * Update the rule after it has triggered an alert
         * @param rule Alert rule that triggered
         * @param triggerTime Time when the rule triggered
         * @return Updated alert rule
         */
        fun updateAfterTrigger(
            rule: AlertRuleEntity,
            triggerTime: LocalDateTime = LocalDateTime.now()
        ): AlertRuleEntity {
            return rule.copy(
                lastTriggeredAt = triggerTime,
                triggerCount = rule.triggerCount + 1,
                modifiedAt = triggerTime
            )
        }
    }
}

/**
 * EmergencyContactEntity - Room database entity for emergency contacts
 *
 * This entity stores emergency contact information for health alerts, including:
 * - Contact details (name, phone, email, relationship)
 * - Notification preferences and history
 * - Alert type filters and priority
 * - Contact availability schedule
 *
 * The entity enables the app to notify designated contacts
 * during health emergencies or critical alerts.
 */
@Entity(
    tableName = "emergency_contact",
    indices = [
        Index("userId"),
        Index("priority"),
        Index("isActive")
    ]
)
data class EmergencyContactEntity(
    /**
     * Primary key - unique identifier for the contact
     */
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    /**
     * User identifier - allows for multi-user support
     */
    @ColumnInfo(name = "userId")
    val userId: String,

    /**
     * Contact name
     */
    @ColumnInfo(name = "name")
    val name: String,

    /**
     * Relationship to the user
     */
    @ColumnInfo(name = "relationship")
    val relationship: String,

    /**
     * Phone number
     */
    @ColumnInfo(name = "phoneNumber")
    val phoneNumber: String,

    /**
     * Alternative phone number
     */
    @ColumnInfo(name = "alternativePhoneNumber")
    val alternativePhoneNumber: String? = null,

    /**
     * Email address
     */
    @ColumnInfo(name = "email")
    val email: String? = null,

    /**
     * Priority order (lower number = higher priority)
     */
    @ColumnInfo(name = "priority")
    val priority: Int = 1,

    /**
     * Flag indicating if this contact is active
     */
    @ColumnInfo(name = "isActive")
    val isActive: Boolean = true,

    /**
     * Flag indicating if SMS notifications are enabled
     */
    @ColumnInfo(name = "smsEnabled")
    val smsEnabled: Boolean = true,

    /**
     * Flag indicating if email notifications are