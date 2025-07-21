package com.sensacare.app.domain.usecase.health.model

import com.sensacare.app.domain.model.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.LinkedHashMap

/**
 * Comprehensive data models for the Health Insights system
 * 
 * This file contains all the data models used by the Health Insights use case,
 * including insight types, health scoring, enums, and support classes.
 */

/**
 * Main container for all health insights generated for a user
 */
data class HealthInsights(
    val userId: Long,
    val generatedAt: LocalDateTime,
    val timeRange: TimeRange,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime,
    val insights: List<HealthInsight>,
    val healthScore: HealthScore,
    val disclaimer: String
) {
    /**
     * Get insights filtered by category
     */
    fun getInsightsByCategory(category: InsightCategory): List<HealthInsight> {
        return insights.filter { it.category == category }
    }
    
    /**
     * Get insights filtered by severity
     */
    fun getInsightsBySeverity(severity: InsightSeverity): List<HealthInsight> {
        return insights.filter { it.severity == severity }
    }
    
    /**
     * Get insights filtered by metric type
     */
    fun getInsightsByMetricType(metricType: MetricType): List<HealthInsight> {
        return insights.filter { 
            when (it) {
                is CorrelationInsight -> it.primaryMetricType == metricType || it.secondaryMetricType == metricType
                else -> it.metricType == metricType
            }
        }
    }
    
    /**
     * Get the most critical insights (HIGH and CRITICAL severity)
     */
    fun getCriticalInsights(): List<HealthInsight> {
        return insights.filter { 
            it.severity == InsightSeverity.HIGH || it.severity == InsightSeverity.CRITICAL 
        }
    }
    
    /**
     * Get the total number of insights
     */
    fun getTotalInsightCount(): Int = insights.size
    
    /**
     * Check if insights were generated within the given time frame
     */
    fun isRecent(withinHours: Int): Boolean {
        val hoursAgo = LocalDateTime.now().minusHours(withinHours.toLong())
        return generatedAt.isAfter(hoursAgo)
    }
}

/**
 * Base interface for all health insights
 */
interface HealthInsight {
    val id: String
    val title: String
    val description: String
    val severity: InsightSeverity
    val category: InsightCategory
    val metricType: MetricType
    val relatedInsightIds: List<String>
}

/**
 * Insight that identifies trends in health data
 */
data class TrendInsight(
    override val id: String,
    override val title: String,
    override val description: String,
    override val severity: InsightSeverity,
    override val category: InsightCategory,
    override val metricType: MetricType,
    val trendDirection: TrendDirection,
    val percentageChange: Double,
    override val relatedInsightIds: List<String>
) : HealthInsight

/**
 * Insight that identifies anomalies in health data
 */
data class AnomalyInsight(
    override val id: String,
    override val title: String,
    override val description: String,
    override val severity: InsightSeverity,
    override val category: InsightCategory,
    override val metricType: MetricType,
    val anomalyType: AnomalyType,
    val detectedAt: LocalDateTime,
    val affectedValue: Double,
    val expectedRange: Pair<Double, Double>,
    override val relatedInsightIds: List<String>
) : HealthInsight

/**
 * Insight that identifies correlations between different health metrics
 */
data class CorrelationInsight(
    override val id: String,
    override val title: String,
    override val description: String,
    override val severity: InsightSeverity,
    override val category: InsightCategory,
    val primaryMetricType: MetricType,
    val secondaryMetricType: MetricType,
    val correlationType: CorrelationType,
    val correlationStrength: Double,
    override val relatedInsightIds: List<String>
) : HealthInsight {
    override val metricType: MetricType = MetricType.MULTIPLE
}

/**
 * Insight that assesses health risks based on data patterns
 */
data class RiskAssessmentInsight(
    override val id: String,
    override val title: String,
    override val description: String,
    override val severity: InsightSeverity,
    override val category: InsightCategory,
    override val metricType: MetricType,
    val riskLevel: RiskLevel,
    val riskFactors: List<String>,
    val recommendedActions: List<String>,
    override val relatedInsightIds: List<String>
) : HealthInsight

/**
 * Insight that provides personalized health recommendations
 */
data class RecommendationInsight(
    override val id: String,
    override val title: String,
    override val description: String,
    override val severity: InsightSeverity,
    override val category: InsightCategory,
    override val metricType: MetricType,
    val recommendationActions: List<String>,
    val expectedBenefits: List<String>,
    override val relatedInsightIds: List<String>
) : HealthInsight

/**
 * Insight that tracks progress toward health goals
 */
data class GoalInsight(
    override val id: String,
    override val title: String,
    override val description: String,
    override val severity: InsightSeverity,
    override val category: InsightCategory,
    override val metricType: MetricType,
    val goalType: GoalType,
    val goalTarget: Double,
    val currentValue: Double,
    val progressPercentage: Double,
    val isAchieved: Boolean,
    val streakDays: Int,
    override val relatedInsightIds: List<String>
) : HealthInsight

/**
 * Health score model with overall score and component breakdown
 */
data class HealthScore(
    val score: Int, // 0-100
    val grade: HealthGrade,
    val components: List<HealthScoreComponent>,
    val description: String
) {
    /**
     * Get the component with the highest score
     */
    fun getHighestComponent(): HealthScoreComponent? {
        return components.maxByOrNull { it.score }
    }
    
    /**
     * Get the component with the lowest score
     */
    fun getLowestComponent(): HealthScoreComponent? {
        return components.minByOrNull { it.score }
    }
    
    /**
     * Get components that need improvement (score < 60)
     */
    fun getComponentsNeedingImprovement(): List<HealthScoreComponent> {
        return components.filter { it.score < 60 }
    }
    
    /**
     * Check if the score is in the healthy range
     */
    fun isHealthy(): Boolean {
        return score >= 70
    }
}

/**
 * Component of the health score for a specific metric
 */
data class HealthScoreComponent(
    val metricType: MetricType,
    val score: Int, // 0-100
    val weight: Double, // 0.0-1.0
    val description: String
) {
    /**
     * Get the grade for this component
     */
    val grade: HealthGrade
        get() = getHealthGradeFromScore(score)
}

/**
 * Sealed class for health insights operation results
 */
sealed class HealthInsightsResult {
    /**
     * Loading state
     */
    data object Loading : HealthInsightsResult()
    
    /**
     * Success state with insights
     */
    data class Success(
        val insights: HealthInsights,
        val fromCache: Boolean
    ) : HealthInsightsResult()
    
    /**
     * Error state
     */
    data class Error(
        val error: InsightError
    ) : HealthInsightsResult()
}

/**
 * Sealed class for insight errors
 */
sealed class InsightError {
    abstract val message: String
    
    /**
     * Error when processing insights
     */
    data class ProcessingError(override val message: String) : InsightError()
    
    /**
     * Error when insufficient data is available
     */
    data class InsufficientDataError(
        override val message: String,
        val metricType: MetricType? = null
    ) : InsightError()
    
    /**
     * Error when data validation fails
     */
    data class ValidationError(override val message: String) : InsightError()
    
    /**
     * Error when user preferences are missing
     */
    data class MissingPreferencesError(override val message: String) : InsightError()
    
    /**
     * Unknown error
     */
    data class UnknownError(
        override val message: String,
        val cause: Throwable? = null
    ) : InsightError()
}

/**
 * Container for all health data needed for analysis
 */
data class HealthDataSet(
    val heartRate: List<HeartRateMeasurement> = emptyList(),
    val bloodOxygen: List<BloodOxygenMeasurement> = emptyList(),
    val bloodPressure: List<BloodPressureMeasurement> = emptyList(),
    val sleep: List<SleepRecord> = emptyList(),
    val steps: List<StepRecord> = emptyList(),
    val activities: List<ActivityRecord> = emptyList(),
    val temperature: List<TemperatureMeasurement> = emptyList()
) {
    /**
     * Check if the dataset has sufficient data for analysis
     */
    fun hasSufficientData(): Boolean {
        // At least one type of data with at least 3 measurements
        return heartRate.size >= 3 || bloodOxygen.size >= 3 || 
               bloodPressure.size >= 3 || sleep.size >= 3 || 
               steps.size >= 3 || activities.size >= 3 || 
               temperature.size >= 3
    }
    
    /**
     * Get the total number of data points
     */
    fun getTotalDataPoints(): Int {
        return heartRate.size + bloodOxygen.size + bloodPressure.size + 
               sleep.size + steps.size + activities.size + temperature.size
    }
    
    /**
     * Check if a specific metric type has data
     */
    fun hasDataForMetric(metricType: MetricType): Boolean {
        return when (metricType) {
            MetricType.HEART_RATE -> heartRate.isNotEmpty()
            MetricType.BLOOD_OXYGEN -> bloodOxygen.isNotEmpty()
            MetricType.BLOOD_PRESSURE -> bloodPressure.isNotEmpty()
            MetricType.SLEEP -> sleep.isNotEmpty()
            MetricType.STEPS -> steps.isNotEmpty()
            MetricType.ACTIVITY -> activities.isNotEmpty()
            MetricType.TEMPERATURE -> temperature.isNotEmpty()
            MetricType.MULTIPLE -> true // Always consider multiple metrics as having data
        }
    }
}

/**
 * Cache key for insights
 */
data class CacheKey(
    val userId: Long,
    val timeRange: TimeRange,
    val categories: Set<InsightCategory>
)

/**
 * Cache entry for insights
 */
data class CacheEntry(
    val insights: HealthInsights,
    val timestamp: LocalDateTime = LocalDateTime.now()
) {
    /**
     * Check if the cache entry is expired
     */
    fun isExpired(expiryMinutes: Long = 30): Boolean {
        val expiryTime = timestamp.plusMinutes(expiryMinutes)
        return LocalDateTime.now().isAfter(expiryTime)
    }
}

/**
 * Result of correlation analysis
 */
data class CorrelationResult(
    val type: CorrelationType,
    val strength: Double // 0.0-1.0
)

/**
 * Simple LRU cache implementation
 */
class LruCache<K, V>(private val maxSize: Int) {
    private val map = LinkedHashMap<K, V>(maxSize, 0.75f, true)
    
    /**
     * Put a value in the cache
     */
    fun put(key: K, value: V) {
        if (map.size >= maxSize) {
            val eldestEntry = map.entries.iterator().next()
            map.remove(eldestEntry.key)
        }
        map[key] = value
    }
    
    /**
     * Get a value from the cache
     */
    fun get(key: K): V? {
        return map[key]
    }
    
    /**
     * Remove a value from the cache
     */
    fun remove(key: K) {
        map.remove(key)
    }
    
    /**
     * Clear the cache
     */
    fun clear() {
        map.clear()
    }
    
    /**
     * Get the size of the cache
     */
    fun size(): Int {
        return map.size
    }
}

/**
 * Enum for insight categories
 */
enum class InsightCategory {
    TRENDS,
    ANOMALIES,
    CORRELATIONS,
    RISK_ASSESSMENT,
    RECOMMENDATIONS,
    GOALS;
    
    companion object {
        /**
         * Get all categories
         */
        fun values(): Set<InsightCategory> {
            return setOf(
                TRENDS,
                ANOMALIES,
                CORRELATIONS,
                RISK_ASSESSMENT,
                RECOMMENDATIONS,
                GOALS
            )
        }
    }
}

/**
 * Enum for insight severity levels
 */
enum class InsightSeverity {
    LOW,
    MODERATE,
    HIGH,
    CRITICAL;
    
    /**
     * Get the color associated with this severity
     */
    val color: String
        get() = when (this) {
            LOW -> "#4CAF50" // Green
            MODERATE -> "#FFC107" // Amber
            HIGH -> "#FF9800" // Orange
            CRITICAL -> "#F44336" // Red
        }
}

/**
 * Enum for trend directions
 */
enum class TrendDirection {
    INCREASING,
    DECREASING,
    STABLE;
    
    /**
     * Human-readable description of the trend
     */
    val description: String
        get() = when (this) {
            INCREASING -> "Increasing"
            DECREASING -> "Decreasing"
            STABLE -> "Stable"
        }
}

/**
 * Enum for correlation types
 */
enum class CorrelationType {
    POSITIVE,
    INVERSE,
    NONE;
    
    /**
     * Human-readable description of the correlation
     */
    val description: String
        get() = when (this) {
            POSITIVE -> "Positive"
            INVERSE -> "Inverse"
            NONE -> "No"
        }
}

/**
 * Enum for anomaly types
 */
enum class AnomalyType {
    OUTLIER,
    PATTERN,
    ABOVE_THRESHOLD,
    BELOW_THRESHOLD,
    SUDDEN_CHANGE;
    
    /**
     * Human-readable description of the anomaly
     */
    val description: String
        get() = when (this) {
            OUTLIER -> "Unusual Value"
            PATTERN -> "Unusual Pattern"
            ABOVE_THRESHOLD -> "Above Normal Range"
            BELOW_THRESHOLD -> "Below Normal Range"
            SUDDEN_CHANGE -> "Sudden Change"
        }
}

/**
 * Enum for risk levels
 */
enum class RiskLevel {
    LOW,
    MODERATE,
    ELEVATED,
    HIGH;
    
    /**
     * Human-readable description of the risk level
     */
    val description: String
        get() = when (this) {
            LOW -> "Low Risk"
            MODERATE -> "Moderate Risk"
            ELEVATED -> "Elevated Risk"
            HIGH -> "High Risk"
        }
    
    /**
     * Get the associated severity
     */
    val severity: InsightSeverity
        get() = when (this) {
            LOW -> InsightSeverity.LOW
            MODERATE -> InsightSeverity.MODERATE
            ELEVATED -> InsightSeverity.HIGH
            HIGH -> InsightSeverity.CRITICAL
        }
}

/**
 * Enum for goal types
 */
enum class GoalType {
    STEPS,
    SLEEP_DURATION,
    ACTIVITY_MINUTES,
    HEART_RATE_ZONE,
    WEIGHT,
    BLOOD_PRESSURE,
    CUSTOM;
    
    /**
     * Human-readable description of the goal type
     */
    val description: String
        get() = when (this) {
            STEPS -> "Daily Steps"
            SLEEP_DURATION -> "Sleep Duration"
            ACTIVITY_MINUTES -> "Active Minutes"
            HEART_RATE_ZONE -> "Heart Rate Zone"
            WEIGHT -> "Weight"
            BLOOD_PRESSURE -> "Blood Pressure"
            CUSTOM -> "Custom Goal"
        }
}

/**
 * Enum for metric types
 */
enum class MetricType {
    HEART_RATE,
    BLOOD_OXYGEN,
    BLOOD_PRESSURE,
    SLEEP,
    STEPS,
    ACTIVITY,
    TEMPERATURE,
    MULTIPLE;
    
    /**
     * Human-readable description of the metric type
     */
    val description: String
        get() = when (this) {
            HEART_RATE -> "Heart Rate"
            BLOOD_OXYGEN -> "Blood Oxygen"
            BLOOD_PRESSURE -> "Blood Pressure"
            SLEEP -> "Sleep"
            STEPS -> "Steps"
            ACTIVITY -> "Activity"
            TEMPERATURE -> "Body Temperature"
            MULTIPLE -> "Multiple Metrics"
        }
    
    /**
     * Get the unit of measurement for this metric
     */
    val unit: String
        get() = when (this) {
            HEART_RATE -> "bpm"
            BLOOD_OXYGEN -> "%"
            BLOOD_PRESSURE -> "mmHg"
            SLEEP -> "hours"
            STEPS -> "steps"
            ACTIVITY -> "minutes"
            TEMPERATURE -> "°C"
            MULTIPLE -> ""
        }
}

/**
 * Enum for health grades
 */
enum class HealthGrade {
    A_PLUS,
    A,
    B_PLUS,
    B,
    C_PLUS,
    C,
    D,
    F;
    
    /**
     * Human-readable description of the grade
     */
    val description: String
        get() = when (this) {
            A_PLUS -> "Excellent"
            A -> "Very Good"
            B_PLUS -> "Good"
            B -> "Above Average"
            C_PLUS -> "Average"
            C -> "Below Average"
            D -> "Poor"
            F -> "Concerning"
        }
    
    /**
     * Letter representation of the grade
     */
    val letter: String
        get() = when (this) {
            A_PLUS -> "A+"
            A -> "A"
            B_PLUS -> "B+"
            B -> "B"
            C_PLUS -> "C+"
            C -> "C"
            D -> "D"
            F -> "F"
        }
}

/**
 * Sealed class for time ranges
 */
sealed class TimeRange {
    /**
     * Human-readable description of the time range
     */
    abstract val description: String
    
    /**
     * Day time range (last 24 hours)
     */
    data object DAY : TimeRange() {
        override val description: String = "Day"
    }
    
    /**
     * Week time range (last 7 days)
     */
    data object WEEK : TimeRange() {
        override val description: String = "Week"
    }
    
    /**
     * Month time range (last 30 days)
     */
    data object MONTH : TimeRange() {
        override val description: String = "Month"
    }
    
    /**
     * Quarter time range (last 90 days)
     */
    data object QUARTER : TimeRange() {
        override val description: String = "Quarter"
    }
    
    /**
     * Year time range (last 365 days)
     */
    data object YEAR : TimeRange() {
        override val description: String = "Year"
    }
    
    /**
     * Custom time range
     */
    data class CUSTOM(
        val startDate: LocalDateTime,
        val endDate: LocalDateTime = LocalDateTime.now(),
        override val description: String = "Custom Range"
    ) : TimeRange()
}

/**
 * Extension function to square a double
 */
fun Double.pow(exponent: Int): Double {
    return kotlin.math.pow(this, exponent.toDouble())
}

/**
 * Get the health grade from a score
 */
fun getHealthGradeFromScore(score: Int): HealthGrade {
    return when {
        score >= 95 -> HealthGrade.A_PLUS
        score >= 85 -> HealthGrade.A
        score >= 80 -> HealthGrade.B_PLUS
        score >= 75 -> HealthGrade.B
        score >= 70 -> HealthGrade.C_PLUS
        score >= 60 -> HealthGrade.C
        score >= 50 -> HealthGrade.D
        else -> HealthGrade.F
    }
}

/**
 * Get a description for a health score
 */
fun getHealthScoreDescription(
    score: Int,
    grade: HealthGrade,
    components: List<HealthScoreComponent>
): String {
    val baseDescription = "Your overall health score is $score (${grade.letter} - ${grade.description})."
    
    if (components.isEmpty()) {
        return baseDescription
    }
    
    val highestComponent = components.maxByOrNull { it.score }
    val lowestComponent = components.minByOrNull { it.score }
    
    val strengthsAndWeaknesses = if (highestComponent != null && lowestComponent != null) {
        "Your strongest area is ${highestComponent.metricType.description} " +
        "(${highestComponent.score}/100), and your area with most room for improvement " +
        "is ${lowestComponent.metricType.description} (${lowestComponent.score}/100)."
    } else {
        ""
    }
    
    return "$baseDescription $strengthsAndWeaknesses"
}

/**
 * Get a description for a heart rate score
 */
fun getHeartRateScoreDescription(score: Int): String {
    return when {
        score >= 90 -> "excellent, indicating optimal cardiovascular health"
        score >= 80 -> "very good, within the healthy range"
        score >= 70 -> "good, but could be improved with regular exercise"
        score >= 60 -> "acceptable, but consider lifestyle modifications"
        else -> "higher than optimal, which may indicate cardiovascular stress"
    }
}

/**
 * Get a description for a blood pressure score
 */
fun getBloodPressureScoreDescription(score: Int): String {
    return when {
        score >= 90 -> "optimal, indicating excellent cardiovascular health"
        score >= 80 -> "normal, within the healthy range"
        score >= 70 -> "high-normal, consider monitoring more frequently"
        score >= 60 -> "elevated, consider lifestyle modifications"
        else -> "high, which may indicate hypertension"
    }
}

/**
 * Get a description for a blood oxygen score
 */
fun getBloodOxygenScoreDescription(score: Int): String {
    return when {
        score >= 90 -> "excellent, indicating optimal oxygen saturation"
        score >= 80 -> "very good, within the normal range"
        score >= 70 -> "acceptable, but could be improved"
        score >= 60 -> "lower than optimal, consider monitoring"
        else -> "below normal range, which may require attention"
    }
}

/**
 * Get a description for sleep quality
 */
fun getSleepQualityDescription(qualityValue: Double): String {
    return when {
        qualityValue >= 0.9 -> "excellent sleep quality"
        qualityValue >= 0.7 -> "good sleep quality"
        qualityValue >= 0.5 -> "fair sleep quality"
        qualityValue >= 0.3 -> "poor sleep quality"
        else -> "very poor sleep quality"
    }
}

/**
 * Get a description for a heart rate anomaly
 */
fun getHeartRateAnomalyDescription(anomalies: List<HeartRateMeasurement>): String {
    val highHeartRates = anomalies.filter { it.value > 100 }
    val lowHeartRates = anomalies.filter { it.value < 50 }
    
    return when {
        highHeartRates.isNotEmpty() && lowHeartRates.isNotEmpty() ->
            "Your heart rate showed significant variability, with both unusually high and low readings. " +
            "This could be related to physical activity, stress, medication, or other factors."
        
        highHeartRates.isNotEmpty() ->
            "Your heart rate was unusually elevated. This could be due to physical activity, " +
            "stress, caffeine, medication, or other factors."
        
        lowHeartRates.isNotEmpty() ->
            "Your heart rate was unusually low. This could be due to rest, sleep, " +
            "medication, or in some cases, a sign of excellent cardiovascular fitness."
        
        else ->
            "Your heart rate showed unusual patterns. Consider tracking any factors that " +
            "might have influenced these readings."
    }
}

/**
 * Get a description for a blood oxygen anomaly
 */
fun getBloodOxygenAnomalyDescription(anomalies: List<BloodOxygenMeasurement>): String {
    val lowestReading = anomalies.minByOrNull { it.percentage }?.percentage ?: 0
    
    return when {
        lowestReading < 90 ->
            "Blood oxygen levels below 90% may indicate significant oxygen deprivation " +
            "and could require medical attention. If you're experiencing symptoms like " +
            "shortness of breath or dizziness, consider consulting a healthcare provider."
        
        lowestReading < 94 ->
            "Blood oxygen levels below 94% are below the normal range. This could be " +
            "temporary or related to measurement error, but if persistent or accompanied " +
            "by symptoms, consider consulting a healthcare provider."
        
        else ->
            "Your blood oxygen levels were slightly below the optimal range. This could be " +
            "due to various factors including measurement position, time of day, or activity level."
    }
}

/**
 * Get a description for a blood pressure anomaly
 */
fun getBloodPressureAnomalyDescription(anomalies: List<BloodPressureMeasurement>): String {
    val highestSystolic = anomalies.maxByOrNull { it.systolic }?.systolic ?: 0
    val highestDiastolic = anomalies.maxByOrNull { it.diastolic }?.diastolic ?: 0
    
    return when {
        highestSystolic >= 180 || highestDiastolic >= 120 ->
            "These readings are in the range of a hypertensive crisis, which requires " +
            "immediate medical attention. If you haven't already, please consult a " +
            "healthcare provider as soon as possible."
        
        highestSystolic >= 160 || highestDiastolic >= 100 ->
            "These readings are in the range of stage 2 hypertension. It's recommended " +
            "to consult with a healthcare provider to discuss management strategies."
        
        highestSystolic >= 140 || highestDiastolic >= 90 ->
            "These readings are in the range of stage 1 hypertension. Consider lifestyle " +
            "modifications and consult with a healthcare provider for guidance."
        
        highestSystolic >= 130 || highestDiastolic >= 85 ->
            "These readings are in the elevated range. Consider lifestyle modifications " +
            "such as reducing sodium intake, increasing physical activity, and managing stress."
        
        else ->
            "Your blood pressure readings were slightly elevated. This could be due to " +
            "various factors including time of day, recent activity, or stress."
    }
}

/**
 * Get a description for a sleep anomaly
 */
fun getSleepAnomalyDescription(anomalies: List<SleepRecord>): String {
    val averageDuration = anomalies.map { it.durationMinutes }.average() / 60.0
    val poorQualitySleep = anomalies.count { it.sleepQuality == SleepQuality.POOR }
    
    return when {
        averageDuration < 5 ->
            "Consistently getting less than 5 hours of sleep can significantly impact cognitive " +
            "function, mood, immune system, and overall health. Consider prioritizing sleep and " +
            "establishing a regular sleep schedule."
        
        averageDuration < 6 ->
            "Getting less than 6 hours of sleep regularly can affect your health and daily " +
            "performance. Try to identify factors that might be limiting your sleep duration " +
            "and consider adjusting your sleep schedule."
        
        poorQualitySleep >= 3 ->
            "You've experienced several nights of poor sleep quality. This could be related " +
            "to factors such as sleep environment, stress, or sleep disorders. Consider " +
            "evaluating your sleep habits and environment."
        
        else ->
            "Your sleep duration has been below your target. Quality sleep is essential for " +
            "overall health and wellbeing. Consider establishing a consistent sleep schedule " +
            "and creating a restful sleep environment."
    }
}

/**
 * Get a description for a heart rate trend
 */
fun getHeartRateTrendDescription(
    trend: TrendDirection,
    data: List<HeartRateMeasurement>
): String {
    val avgHeartRate = data.map { it.value }.average()
    
    return when (trend) {
        TrendDirection.INCREASING -> {
            if (avgHeartRate > 100) {
                "This increasing trend may indicate increased cardiovascular stress. " +
                "Consider factors like stress, caffeine intake, or medication changes that might be affecting your heart rate."
            } else {
                "While your heart rate is trending upward, it remains within a normal range. " +
                "This could be related to changes in activity levels, stress, or other lifestyle factors."
            }
        }
        TrendDirection.DECREASING -> {
            if (avgHeartRate < 50) {
                "Your heart rate is trending downward and is now in a low range. " +
                "For athletes, this can be normal, but if you're experiencing symptoms like dizziness or fatigue, " +
                "consider consulting a healthcare provider."
            } else {
                "Your decreasing heart rate trend could indicate improving cardiovascular fitness " +
                "or changes in factors like stress levels or medication."
            }
        }
        TrendDirection.STABLE -> {
            "Your heart rate has remained stable, which generally indicates consistent " +
            "cardiovascular function and lifestyle patterns."
        }
    }
}

/**
 * Get a description for a blood oxygen trend
 */
fun getBloodOxygenTrendDescription(
    trend: TrendDirection,
    data: List<BloodOxygenMeasurement>
): String {
    val avgOxygen = data.map { it.percentage }.average()
    
    return when (trend) {
        TrendDirection.INCREASING -> {
            "Your blood oxygen levels are showing an improving trend, which is positive for " +
            "overall respiratory and cardiovascular health."
        }
        TrendDirection.DECREASING -> {
            if (avgOxygen < 95) {
                "Your blood oxygen levels are trending downward and are below the optimal range. " +
                "This could be related to respiratory function, altitude changes, or other factors. " +
                "If this trend continues or you experience symptoms, consider consulting a healthcare provider."
            } else {
                "Your blood oxygen levels are trending slightly downward but remain in a normal range. " +
                "This could be due to various factors including measurement conditions or activity patterns."
            }
        }
        TrendDirection.STABLE -> {
            "Your blood oxygen levels have remained stable, which generally indicates " +
            "consistent respiratory function."
        }
    }
}

/**
 * Get a description for a blood pressure trend
 */
fun getBloodPressureTrendDescription(
    systolicTrend: TrendDirection,
    diastolicTrend: TrendDirection,
    data: List<BloodPressureMeasurement>
): String {
    val avgSystolic = data.map { it.systolic }.average()
    val avgDiastolic = data.map { it.diastolic }.average()
    
    return when {
        systolicTrend == TrendDirection.INCREASING && diastolicTrend == TrendDirection.INCREASING -> {
            if (avgSystolic > 130 || avgDiastolic > 85) {
                "Both your systolic and diastolic pressures are trending upward and are in an elevated range. " +
                "Consider lifestyle modifications like reducing sodium intake, increasing physical activity, " +
                "and stress management. Consulting with a healthcare provider may be beneficial."
            } else {
                "Both your systolic and diastolic pressures are trending upward but remain in a normal range. " +
                "Continue monitoring and maintaining healthy lifestyle habits."
            }
        }
        systolicTrend == TrendDirection.DECREASING && diastolicTrend == TrendDirection.DECREASING -> {
            "Both your systolic and diastolic pressures are trending downward, which is generally positive. " +
            "This could be related to lifestyle changes, medication effects, or other factors."
        }
        systolicTrend == TrendDirection.INCREASING -> {
            "Your systolic pressure (the top number) is trending upward. This could be related to " +
            "increased cardiovascular stress, sodium intake, or other factors."
        }
        diastolicTrend == TrendDirection.INCREASING -> {
            "Your diastolic pressure (the bottom number) is trending upward. This could indicate " +
            "changes in vascular resistance and may be worth monitoring."
        }
        else -> {
            "Your blood pressure is showing some variability but remains relatively stable overall. " +
            "Continue with regular monitoring and healthy lifestyle practices."
        }
    }
}

/**
 * Get a description for a sleep trend
 */
fun getSleepTrendDescription(
    trend: TrendDirection,
    data: List<SleepRecord>
): String {
    val avgDuration = data.map { it.durationMinutes }.average() / 60.0
    
    return when (trend) {
        TrendDirection.INCREASING -> {
            "Your sleep duration is trending upward, which is positive for overall health and wellbeing. " +
            "Consistent, quality sleep supports cognitive function, mood regulation, and physical recovery."
        }
        TrendDirection.DECREASING -> {
            if (avgDuration < 7) {
                "Your sleep duration is trending downward and is below the recommended 7-9 hours for adults. " +
                "Insufficient sleep can impact cognitive function, mood, immune system, and overall health. " +
                "Consider prioritizing sleep and establishing a regular sleep schedule."
            } else {
                "Your sleep duration is trending downward but remains within a healthy range. " +
                "Continue to prioritize consistent, quality sleep for optimal health."
            }
        }
        TrendDirection.STABLE -> {
            "Your sleep duration has remained stable, which indicates consistent sleep patterns. " +
            "Regular sleep schedules support overall health and wellbeing."
        }
    }
}

/**
 * Get a description for a steps trend
 */
fun getStepsTrendDescription(
    trend: TrendDirection,
    data: List<StepRecord>
): String {
    val avgSteps = data.map { it.count }.average()
    
    return when (trend) {
        TrendDirection.INCREASING -> {
            "Your daily step count is trending upward, which is positive for cardiovascular health, " +
            "weight management, and overall wellbeing. Consistent physical activity supports long-term health."
        }
        TrendDirection.DECREASING -> {
            if (avgSteps < 5000) {
                "Your daily step count is trending downward and is below the recommended minimum. " +
                "Regular physical activity is essential for health. Consider incorporating more " +
                "walking into your daily routine."
            } else {
                "Your daily step count is trending downward but remains at a moderate level. " +
                "Try to maintain consistent physical activity for optimal health benefits."
            }
        }
        TrendDirection.STABLE -> {
            "Your daily step count has remained stable, indicating consistent activity patterns. " +
            "Regular physical activity supports overall health and wellbeing."
        }
    }
}
