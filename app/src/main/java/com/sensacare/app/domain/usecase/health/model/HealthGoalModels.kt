package com.sensacare.app.domain.usecase.health.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import com.sensacare.app.domain.usecase.health.TimeRange

/**
 * Comprehensive data models for the Health Goal management system
 * 
 * This file contains all the data models used by the Health Goal management use case,
 * including goal types, progress tracking, suggestions, statistics, and support classes.
 */

/**
 * Main data class representing a health goal
 */
data class HealthGoal(
    val id: Long,
    val userId: Long,
    val type: GoalType,
    val title: String,
    val description: String,
    val targetValue: Double,
    val unit: String,
    val frequency: GoalFrequency,
    val duration: GoalDuration,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val reminderTime: String? = null,
    val reminderDays: Set<DayOfWeek> = emptySet(),
    val progress: Double = 0.0,
    val status: GoalStatus = GoalStatus.ACTIVE,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val lastAchievedDate: LocalDate? = null,
    val streakDays: Int = 0,
    val lastUpdated: LocalDateTime = LocalDateTime.now(),
    val allowMultiple: Boolean = false,
    val isPublic: Boolean = false,
    val additionalData: Map<String, Any> = emptyMap()
) {
    /**
     * Check if the goal is currently active
     */
    fun isActive(): Boolean {
        return status == GoalStatus.ACTIVE && (endDate == null || endDate.isAfter(LocalDate.now()))
    }
    
    /**
     * Check if the goal has been achieved today
     */
    fun isAchievedToday(): Boolean {
        return status == GoalStatus.ACHIEVED && lastAchievedDate == LocalDate.now()
    }
    
    /**
     * Check if the goal is overdue
     */
    fun isOverdue(): Boolean {
        return status == GoalStatus.ACTIVE && endDate != null && endDate.isBefore(LocalDate.now())
    }
    
    /**
     * Check if the goal has a reminder for today
     */
    fun hasReminderForToday(): Boolean {
        return reminderTime != null && reminderDays.contains(LocalDate.now().dayOfWeek)
    }
    
    /**
     * Get the remaining days until the goal end date
     */
    fun getRemainingDays(): Int? {
        return endDate?.let {
            val today = LocalDate.now()
            if (it.isAfter(today)) {
                java.time.temporal.ChronoUnit.DAYS.between(today, it).toInt()
            } else {
                0
            }
        }
    }
    
    /**
     * Get a formatted string representation of the target
     */
    fun getFormattedTarget(): String {
        return when (type) {
            GoalType.SLEEP_DURATION -> "${(targetValue / 60).toInt()} hours"
            else -> "${targetValue.toInt()} $unit"
        }
    }
}

/**
 * Enum representing the status of a goal
 */
enum class GoalStatus {
    ACTIVE,      // Goal is currently active
    ACHIEVED,    // Goal has been achieved but is still active (for recurring goals)
    COMPLETED,   // Goal has been completed and is no longer active
    ABANDONED    // Goal was abandoned before completion
}

/**
 * Enum representing the frequency of a goal
 */
enum class GoalFrequency {
    DAILY,      // Goal resets daily
    WEEKLY,     // Goal resets weekly
    MONTHLY,    // Goal resets monthly
    ONE_TIME    // Goal does not reset
}

/**
 * Enum representing the duration of a goal
 */
enum class GoalDuration {
    DAILY,      // Goal lasts for a day
    WEEKLY,     // Goal lasts for a week
    MONTHLY,    // Goal lasts for a month
    QUARTERLY,  // Goal lasts for three months
    YEARLY,     // Goal lasts for a year
    ONGOING     // Goal has no end date
}

/**
 * Enum representing the type of a goal
 */
enum class GoalType {
    STEPS,           // Daily step count
    SLEEP_DURATION,  // Sleep duration
    ACTIVITY_MINUTES, // Activity minutes
    HEART_RATE_ZONE, // Time in heart rate zone
    WEIGHT,          // Weight goal
    BLOOD_PRESSURE,  // Blood pressure goal
    WATER_INTAKE,    // Water intake goal
    CUSTOM           // Custom goal
}

/**
 * Enum representing the difficulty of a goal
 */
enum class GoalDifficulty {
    EASY,
    MODERATE,
    CHALLENGING,
    DIFFICULT
}

/**
 * Sealed class representing the result of a goal operation
 */
sealed class GoalOperationResult {
    /**
     * Loading state
     */
    data object Loading : GoalOperationResult()
    
    /**
     * Success state with goal
     */
    data class Success(val goal: HealthGoal) : GoalOperationResult()
    
    /**
     * Error state
     */
    data class Error(val error: GoalError) : GoalOperationResult()
}

/**
 * Sealed class representing goal errors
 */
sealed class GoalError {
    abstract val message: String
    
    /**
     * Error when goal validation fails
     */
    data class ValidationError(
        val reasons: List<String>,
        override val message: String = "Goal validation failed: ${reasons.joinToString(", ")}"
    ) : GoalError()
    
    /**
     * Error when goal not found
     */
    data class GoalNotFoundError(
        override val message: String
    ) : GoalError()
    
    /**
     * Error when duplicate goal exists
     */
    data class DuplicateGoalError(
        override val message: String
    ) : GoalError()
    
    /**
     * Error when repository operation fails
     */
    data class RepositoryError(
        override val message: String,
        val cause: Throwable? = null
    ) : GoalError()
    
    /**
     * Error when progress update fails
     */
    data class ProgressUpdateError(
        override val message: String,
        val cause: Throwable? = null
    ) : GoalError()
    
    /**
     * Error when goal suggestion fails
     */
    data class SuggestionError(
        override val message: String,
        val cause: Throwable? = null
    ) : GoalError()
}

/**
 * Sealed class representing goal validation result
 */
sealed class GoalValidationResult {
    /**
     * Valid goal
     */
    data object Valid : GoalValidationResult()
    
    /**
     * Invalid goal with reasons
     */
    data class Invalid(val reasons: List<String>) : GoalValidationResult()
}

/**
 * Sealed class representing goal progress update result
 */
sealed class GoalProgressUpdateResult {
    /**
     * Loading state
     */
    data object Loading : GoalProgressUpdateResult()
    
    /**
     * Success state with updated goals
     */
    data class Success(val updates: List<GoalProgressUpdate>) : GoalProgressUpdateResult()
    
    /**
     * Error state
     */
    data class Error(val error: GoalError) : GoalProgressUpdateResult()
}

/**
 * Data class representing a goal progress update
 */
data class GoalProgressUpdate(
    val goal: HealthGoal,
    val previousProgress: Double,
    val newProgress: Double,
    val isNewlyAchieved: Boolean
) {
    /**
     * Check if progress has increased
     */
    fun hasProgressIncreased(): Boolean = newProgress > previousProgress
    
    /**
     * Get progress change amount
     */
    fun getProgressChange(): Double = newProgress - previousProgress
    
    /**
     * Get progress change percentage
     */
    fun getProgressChangePercentage(): Double {
        return if (previousProgress > 0) {
            ((newProgress - previousProgress) / previousProgress) * 100
        } else {
            0.0
        }
    }
}

/**
 * Data class representing a goal progress history entry
 */
data class GoalProgressHistory(
    val id: Long,
    val goalId: Long,
    val date: LocalDate,
    val progress: Double,
    val value: Double,
    val note: String? = null
)

/**
 * Sealed class representing goal suggestion result
 */
sealed class GoalSuggestionResult {
    /**
     * Loading state
     */
    data object Loading : GoalSuggestionResult()
    
    /**
     * Success state with suggestions
     */
    data class Success(val suggestions: List<HealthGoalSuggestion>) : GoalSuggestionResult()
    
    /**
     * Error state
     */
    data class Error(val error: GoalError) : GoalSuggestionResult()
}

/**
 * Data class representing a health goal suggestion
 */
data class HealthGoalSuggestion(
    val type: GoalType,
    val title: String,
    val description: String,
    val suggestedValue: Double,
    val currentAverage: Double,
    val difficulty: GoalDifficulty,
    val healthBenefits: List<String>,
    val suggestedDuration: GoalDuration,
    val additionalData: Map<String, Any>? = null
) {
    /**
     * Get improvement percentage from current average
     */
    fun getImprovementPercentage(): Double {
        return if (currentAverage > 0) {
            ((suggestedValue - currentAverage) / currentAverage) * 100
        } else {
            0.0
        }
    }
    
    /**
     * Get a formatted string representation of the suggested value
     */
    fun getFormattedValue(): String {
        return when (type) {
            GoalType.SLEEP_DURATION -> "${(suggestedValue / 60).toInt()} hours"
            GoalType.STEPS -> "${suggestedValue.toInt()} steps"
            GoalType.ACTIVITY_MINUTES -> "${suggestedValue.toInt()} minutes"
            GoalType.HEART_RATE_ZONE -> "${suggestedValue.toInt()} minutes"
            GoalType.WEIGHT -> "$suggestedValue kg"
            GoalType.WATER_INTAKE -> "${suggestedValue.toInt()} ml"
            else -> suggestedValue.toString()
        }
    }
}

/**
 * Data class representing goal statistics
 */
data class GoalStatistics(
    val userId: Long,
    val timeRange: TimeRange,
    val totalGoals: Int,
    val completedGoals: Int,
    val abandonedGoals: Int,
    val activeGoals: Int,
    val completionRate: Double,
    val completionRateByType: Map<GoalType, Double>,
    val longestStreak: Int,
    val currentStreaks: Map<Long, Int>,
    val goalCountByType: Map<GoalType, Int>,
    val error: String? = null
) {
    /**
     * Check if statistics are valid
     */
    fun isValid(): Boolean = error == null
    
    /**
     * Get most common goal type
     */
    fun getMostCommonGoalType(): GoalType? {
        return goalCountByType.entries.maxByOrNull { it.value }?.key
    }
    
    /**
     * Get most successful goal type
     */
    fun getMostSuccessfulGoalType(): GoalType? {
        return completionRateByType.entries
            .filter { goalCountByType[it.key] ?: 0 >= 3 } // At least 3 goals of this type
            .maxByOrNull { it.value }?.key
    }
    
    /**
     * Get goal with longest current streak
     */
    fun getLongestCurrentStreakGoalId(): Long? {
        return currentStreaks.entries.maxByOrNull { it.value }?.key
    }
    
    /**
     * Get formatted completion rate
     */
    fun getFormattedCompletionRate(): String {
        return String.format("%.1f%%", completionRate)
    }
}

/**
 * Data class representing a goal achievement
 */
data class GoalAchievement(
    val goalId: Long,
    val goalTitle: String,
    val goalType: GoalType,
    val achievementDate: LocalDate,
    val streakDays: Int,
    val message: String,
    val rewardPoints: Int,
    val milestoneAchieved: Boolean,
    val targetValue: Double,
    val unit: String
) {
    /**
     * Check if this is a first-time achievement
     */
    fun isFirstTimeAchievement(): Boolean = streakDays == 1
    
    /**
     * Get achievement level based on streak
     */
    fun getAchievementLevel(): AchievementLevel {
        return when {
            streakDays >= 90 -> AchievementLevel.PLATINUM
            streakDays >= 30 -> AchievementLevel.GOLD
            streakDays >= 7 -> AchievementLevel.SILVER
            else -> AchievementLevel.BRONZE
        }
    }
    
    /**
     * Get formatted target value
     */
    fun getFormattedTarget(): String {
        return when (goalType) {
            GoalType.SLEEP_DURATION -> "${(targetValue / 60).toInt()} hours"
            else -> "${targetValue.toInt()} $unit"
        }
    }
}

/**
 * Enum representing achievement levels
 */
enum class AchievementLevel {
    BRONZE,
    SILVER,
    GOLD,
    PLATINUM
}

/**
 * Data class representing a goal reminder
 */
data class GoalReminder(
    val goalId: Long,
    val goalTitle: String,
    val goalType: GoalType,
    val reminderTime: String?,
    val message: String,
    val currentProgress: Double,
    val targetValue: Double
) {
    /**
     * Get progress percentage
     */
    fun getProgressPercentage(): Int = currentProgress.toInt()
    
    /**
     * Get remaining value to achieve goal
     */
    fun getRemainingValue(): Double {
        val remaining = targetValue - (currentProgress * targetValue / 100)
        return if (remaining < 0) 0.0 else remaining
    }
    
    /**
     * Get formatted remaining value
     */
    fun getFormattedRemainingValue(): String {
        return when (goalType) {
            GoalType.SLEEP_DURATION -> "${(getRemainingValue() / 60).toInt()} hours"
            else -> "${getRemainingValue().toInt()} ${if (targetValue == 1.0) "" else "s"}"
        }
    }
}

/**
 * Data class representing a goal adjustment suggestion
 */
data class GoalAdjustmentSuggestion(
    val goalId: Long,
    val goalTitle: String,
    val goalType: GoalType,
    val currentTarget: Double,
    val suggestedTarget: Double,
    val adjustmentReason: AdjustmentReason,
    val message: String
) {
    /**
     * Get adjustment percentage
     */
    fun getAdjustmentPercentage(): Double {
        return ((suggestedTarget - currentTarget) / currentTarget) * 100
    }
    
    /**
     * Check if this is an increase
     */
    fun isIncrease(): Boolean = suggestedTarget > currentTarget
    
    /**
     * Get formatted current target
     */
    fun getFormattedCurrentTarget(): String {
        return when (goalType) {
            GoalType.SLEEP_DURATION -> "${(currentTarget / 60).toInt()} hours"
            else -> "${currentTarget.toInt()}"
        }
    }
    
    /**
     * Get formatted suggested target
     */
    fun getFormattedSuggestedTarget(): String {
        return when (goalType) {
            GoalType.SLEEP_DURATION -> "${(suggestedTarget / 60).toInt()} hours"
            else -> "${suggestedTarget.toInt()}"
        }
    }
}

/**
 * Enum representing adjustment reasons
 */
enum class AdjustmentReason {
    TOO_EASY,
    TOO_DIFFICULT,
    PROGRESS_PLATEAU,
    HEALTH_CHANGE,
    SEASONAL_CHANGE,
    USER_REQUESTED
}

/**
 * Data class representing sharing content
 */
data class SharingContent(
    val title: String,
    val message: String,
    val imageType: SharingImageType,
    val goalType: GoalType,
    val achievementDate: LocalDate,
    val streakDays: Int,
    val additionalData: Map<String, Any> = emptyMap()
) {
    /**
     * Get hashtags for sharing
     */
    fun getHashtags(): List<String> {
        val hashtags = mutableListOf("SensaCare", "HealthGoals")
        
        when (goalType) {
            GoalType.STEPS -> hashtags.add("StepGoal")
            GoalType.SLEEP_DURATION -> hashtags.add("SleepGoal")
            GoalType.ACTIVITY_MINUTES -> hashtags.add("ActiveLifestyle")
            GoalType.HEART_RATE_ZONE -> hashtags.add("CardioGoals")
            GoalType.WEIGHT -> hashtags.add("WeightGoal")
            GoalType.BLOOD_PRESSURE -> hashtags.add("HeartHealth")
            GoalType.WATER_INTAKE -> hashtags.add("StayHydrated")
            GoalType.CUSTOM -> hashtags.add("PersonalGoal")
        }
        
        if (streakDays >= 7) hashtags.add("StreakGoals")
        if (streakDays >= 30) hashtags.add("ConsistencyWins")
        
        return hashtags
    }
    
    /**
     * Get formatted message with hashtags
     */
    fun getFormattedMessage(): String {
        return "$message\n\n${getHashtags().joinToString(" ") { "#$it" }}"
    }
}

/**
 * Enum representing sharing image types
 */
enum class SharingImageType {
    STEPS,
    SLEEP,
    ACTIVITY,
    HEART_RATE,
    WEIGHT,
    BLOOD_PRESSURE,
    WATER,
    GENERIC
}

/**
 * Calculate goal difficulty based on current and target values
 *
 * @param currentValue Current average value
 * @param targetValue Target value
 * @return Goal difficulty
 */
fun calculateGoalDifficulty(currentValue: Double, targetValue: Double): GoalDifficulty {
    if (currentValue <= 0) return GoalDifficulty.MODERATE
    
    val improvementPercentage = ((targetValue - currentValue) / currentValue) * 100
    
    return when {
        improvementPercentage < 10 -> GoalDifficulty.EASY
        improvementPercentage < 25 -> GoalDifficulty.MODERATE
        improvementPercentage < 50 -> GoalDifficulty.CHALLENGING
        else -> GoalDifficulty.DIFFICULT
    }
}

/**
 * Check if streak is a milestone
 *
 * @param streakDays Number of streak days
 * @return Whether this is a milestone streak
 */
fun isMilestoneStreak(streakDays: Int): Boolean {
    return streakDays == 1 || // First achievement
           streakDays == 7 || // One week
           streakDays == 30 || // One month
           streakDays == 90 || // Three months
           streakDays == 180 || // Six months
           streakDays == 365 || // One year
           streakDays % 100 == 0 // Every 100 days
}

/**
 * Get date range for goal frequency
 *
 * @param frequency Goal frequency
 * @return Pair of start and end dates
 */
fun getDateRangeForFrequency(frequency: GoalFrequency): Pair<LocalDate, LocalDate> {
    val today = LocalDate.now()
    
    return when (frequency) {
        GoalFrequency.DAILY -> Pair(today, today)
        GoalFrequency.WEEKLY -> {
            val dayOfWeek = today.dayOfWeek.value
            val startOfWeek = today.minusDays((dayOfWeek - 1).toLong())
            Pair(startOfWeek, today)
        }
        GoalFrequency.MONTHLY -> {
            val startOfMonth = today.withDayOfMonth(1)
            Pair(startOfMonth, today)
        }
        GoalFrequency.ONE_TIME -> Pair(today, today)
    }
}

/**
 * Calculate reward points for goal achievement
 *
 * @param goal The achieved goal
 * @return Reward points
 */
fun calculateRewardPoints(goal: HealthGoal): Int {
    // Base points by goal type
    val basePoints = when (goal.type) {
        GoalType.STEPS -> 10
        GoalType.SLEEP_DURATION -> 15
        GoalType.ACTIVITY_MINUTES -> 20
        GoalType.HEART_RATE_ZONE -> 25
        GoalType.WEIGHT -> 30
        GoalType.BLOOD_PRESSURE -> 20
        GoalType.WATER_INTAKE -> 10
        GoalType.CUSTOM -> 15
    }
    
    // Streak multiplier
    val streakMultiplier = when {
        goal.streakDays >= 90 -> 3.0 // 3x for 90+ days
        goal.streakDays >= 30 -> 2.0 // 2x for 30+ days
        goal.streakDays >= 7 -> 1.5 // 1.5x for 7+ days
        else -> 1.0
    }
    
    // Difficulty multiplier
    val difficultyMultiplier = when (goal.additionalData["difficulty"]) {
        GoalDifficulty.EASY.name -> 1.0
        GoalDifficulty.MODERATE.name -> 1.2
        GoalDifficulty.CHALLENGING.name -> 1.5
        GoalDifficulty.DIFFICULT.name -> 2.0
        else -> 1.0
    }
    
    return (basePoints * streakMultiplier * difficultyMultiplier).toInt()
}
