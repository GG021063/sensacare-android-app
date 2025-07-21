package com.sensacare.app.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Heart Rate Variability Analysis
 *
 * Contains the results of heart rate variability analysis, including
 * time-domain metrics (SDNN, RMSSD, pNN50) and daily averages.
 */
data class HeartRateVariabilityAnalysis(
    val userId: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val meanNN: Double, // Mean of NN intervals (ms)
    val sdnn: Double, // Standard deviation of NN intervals (ms)
    val rmssd: Double, // Root mean square of successive differences (ms)
    val pnn50: Double, // Percentage of successive NN intervals that differ by more than 50 ms
    val sampleCount: Int, // Number of samples used in analysis
    val dailyAverages: Map<LocalDate, Double>, // Daily average HRV values
    val analysisTimestamp: LocalDateTime // When the analysis was performed
) {
    val hrvScore: Int
        get() = calculateHrvScore()
    
    private fun calculateHrvScore(): Int {
        // Calculate HRV score based on RMSSD and SDNN
        // Higher values generally indicate better autonomic balance
        val rmssdScore = when {
            rmssd < 10 -> 1
            rmssd < 20 -> 2
            rmssd < 30 -> 3
            rmssd < 40 -> 4
            else -> 5
        }
        
        val sdnnScore = when {
            sdnn < 20 -> 1
            sdnn < 40 -> 2
            sdnn < 60 -> 3
            sdnn < 80 -> 4
            else -> 5
        }
        
        // Weighted average (RMSSD is slightly more important for short-term recordings)
        return ((rmssdScore * 0.6) + (sdnnScore * 0.4)).toInt()
    }
}

/**
 * Heart Rate Trend
 *
 * Contains various heart rate trends over time, including resting heart rate,
 * daily averages, recovery patterns, and circadian rhythm.
 */
data class HeartRateTrend(
    val userId: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val restingHeartRateTrend: Map<LocalDate, Int>, // Daily resting heart rates
    val dailyAverageHeartRateTrend: Map<LocalDate, Int>, // Daily average heart rates
    val recoveryTrend: Map<LocalDate, Double>, // Heart rate recovery rates (bpm/minute)
    val circadianRhythm: Map<Int, Int>, // Hour of day to average heart rate
    val analysisTimestamp: LocalDateTime // When the analysis was performed
) {
    val isRestingHeartRateDecreasing: Boolean
        get() = calculateRestingTrend() < 0
    
    val recoveryImproving: Boolean
        get() = calculateRecoveryTrend() > 0
    
    private fun calculateRestingTrend(): Double {
        if (restingHeartRateTrend.size < 2) return 0.0
        
        // Simple linear regression slope
        val xValues = restingHeartRateTrend.keys.mapIndexed { index, _ -> index.toDouble() }
        val yValues = restingHeartRateTrend.values.map { it.toDouble() }
        
        return calculateSlope(xValues, yValues)
    }
    
    private fun calculateRecoveryTrend(): Double {
        if (recoveryTrend.size < 2) return 0.0
        
        // Simple linear regression slope
        val xValues = recoveryTrend.keys.mapIndexed { index, _ -> index.toDouble() }
        val yValues = recoveryTrend.values.toList()
        
        return calculateSlope(xValues, yValues)
    }
    
    private fun calculateSlope(xValues: List<Double>, yValues: List<Double>): Double {
        val n = xValues.size
        val sumX = xValues.sum()
        val sumY = yValues.sum()
        val sumXY = xValues.zip(yValues).sumOf { it.first * it.second }
        val sumXX = xValues.sumOf { it * it }
        
        return (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX)
    }
}

/**
 * Heart Rate Abnormality Type
 *
 * Enumeration of different types of heart rate abnormalities that can be detected.
 */
enum class HeartRateAbnormalityType {
    TACHYCARDIA, // Abnormally high heart rate at rest (>100 bpm)
    BRADYCARDIA, // Abnormally low heart rate at rest (<50 bpm)
    EXERCISE_TACHYCARDIA, // Excessively high heart rate during exercise
    IRREGULAR_RHYTHM, // Irregular heart rhythm
    SUDDEN_CHANGE, // Sudden significant change in heart rate
    SUSTAINED_ELEVATED, // Sustained elevated heart rate
    POOR_RECOVERY, // Poor heart rate recovery after exercise
    NOCTURNAL_TACHYCARDIA, // High heart rate during sleep
    CHRONOTROPIC_INCOMPETENCE, // Inadequate heart rate response to exercise
    HEART_BLOCK // Potential heart block pattern
}

/**
 * Abnormal Heart Rate Detection
 *
 * Contains the results of abnormal heart rate pattern detection, including
 * the types of abnormalities detected and the associated heart rate measurements.
 */
data class AbnormalHeartRateDetection(
    val userId: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val abnormalitiesDetected: Boolean,
    val abnormalities: Map<HeartRateAbnormalityType, List<HeartRate>>, // Type to associated heart rates
    val analysisTimestamp: LocalDateTime // When the analysis was performed
) {
    val severityScore: Int
        get() = calculateSeverityScore()
    
    val requiresMedicalAttention: Boolean
        get() = severityScore >= 7 || 
                abnormalities.containsKey(HeartRateAbnormalityType.SUSTAINED_ELEVATED) ||
                (abnormalities[HeartRateAbnormalityType.TACHYCARDIA]?.size ?: 0) > 10 ||
                (abnormalities[HeartRateAbnormalityType.BRADYCARDIA]?.size ?: 0) > 10
    
    private fun calculateSeverityScore(): Int {
        if (!abnormalitiesDetected) return 0
        
        // Calculate severity based on types and frequency of abnormalities
        var score = 0
        
        // Add points based on abnormality type
        if (abnormalities.containsKey(HeartRateAbnormalityType.TACHYCARDIA)) score += 3
        if (abnormalities.containsKey(HeartRateAbnormalityType.BRADYCARDIA)) score += 3
        if (abnormalities.containsKey(HeartRateAbnormalityType.IRREGULAR_RHYTHM)) score += 4
        if (abnormalities.containsKey(HeartRateAbnormalityType.SUSTAINED_ELEVATED)) score += 5
        if (abnormalities.containsKey(HeartRateAbnormalityType.HEART_BLOCK)) score += 7
        if (abnormalities.containsKey(HeartRateAbnormalityType.NOCTURNAL_TACHYCARDIA)) score += 4
        if (abnormalities.containsKey(HeartRateAbnormalityType.SUDDEN_CHANGE)) score += 2
        
        // Add points based on frequency (more occurrences = higher severity)
        val totalAbnormalHeartRates = abnormalities.values.sumOf { it.size }
        score += when {
            totalAbnormalHeartRates > 50 -> 5
            totalAbnormalHeartRates > 20 -> 3
            totalAbnormalHeartRates > 10 -> 2
            else -> 1
        }
        
        return score.coerceAtMost(10) // Cap at 10
    }
}

/**
 * Heart Rate Zones
 *
 * Contains heart rate training zones based on age and fitness level,
 * used for exercise intensity guidance and analysis.
 */
data class HeartRateZones(
    val zone1Range: Pair<Double, Double>, // Very light intensity (50-60% of max HR)
    val zone2Range: Pair<Double, Double>, // Light intensity (60-70% of max HR)
    val zone3Range: Pair<Double, Double>, // Moderate intensity (70-80% of max HR)
    val zone4Range: Pair<Double, Double>, // Hard intensity (80-90% of max HR)
    val zone5Range: Pair<Double, Double>  // Maximum intensity (90-100% of max HR)
) {
    fun getZoneForHeartRate(heartRate: Int): Int {
        return when (heartRate.toDouble()) {
            in 0.0..zone1Range.first -> 0 // Below zone 1
            in zone1Range.first..zone1Range.second -> 1
            in zone1Range.second..zone2Range.second -> 2
            in zone2Range.second..zone3Range.second -> 3
            in zone3Range.second..zone4Range.second -> 4
            in zone4Range.second..zone5Range.second -> 5
            else -> 6 // Above zone 5
        }
    }
    
    fun getZoneName(zone: Int): String {
        return when (zone) {
            0 -> "Rest"
            1 -> "Very Light"
            2 -> "Light"
            3 -> "Moderate"
            4 -> "Hard"
            5 -> "Maximum"
            6 -> "Above Maximum"
            else -> "Unknown"
        }
    }
}

/**
 * Heart Rate Stats
 *
 * Contains statistical analysis of heart rate data, including averages,
 * minimums, maximums, and time spent in different heart rate zones.
 */
data class HeartRateStats(
    val avgRestingHeartRate: Int?, // Average resting heart rate
    val minHeartRate: Int, // Minimum heart rate
    val maxHeartRate: Int, // Maximum heart rate
    val avgHeartRate: Int, // Average heart rate
    val stdDevHeartRate: Double, // Standard deviation of heart rate
    val timeInZone1: Int, // Minutes spent in Zone 1
    val timeInZone2: Int, // Minutes spent in Zone 2
    val timeInZone3: Int, // Minutes spent in Zone 3
    val timeInZone4: Int, // Minutes spent in Zone 4
    val timeInZone5: Int, // Minutes spent in Zone 5
    val hrvAvg: Double?, // Average heart rate variability
    val readingsCount: Int // Number of readings
) {
    val cardioFitnessScore: Int
        get() = calculateCardioFitnessScore()
    
    private fun calculateCardioFitnessScore(): Int {
        // Higher score is better
        if (avgRestingHeartRate == null) return 0
        
        // Resting heart rate scoring (lower is better)
        val restingScore = when {
            avgRestingHeartRate < 50 -> 10
            avgRestingHeartRate < 60 -> 8
            avgRestingHeartRate < 70 -> 6
            avgRestingHeartRate < 80 -> 4
            avgRestingHeartRate < 90 -> 2
            else -> 0
        }
        
        // Heart rate variability scoring (higher is better)
        val hrvScore = if (hrvAvg != null) {
            when {
                hrvAvg > 70 -> 10
                hrvAvg > 50 -> 8
                hrvAvg > 30 -> 6
                hrvAvg > 20 -> 4
                hrvAvg > 10 -> 2
                else -> 0
            }
        } else {
            0
        }
        
        // Heart rate range scoring (wider range is better)
        val rangeScore = when (maxHeartRate - minHeartRate) {
            in 100..Int.MAX_VALUE -> 5
            in 80..99 -> 4
            in 60..79 -> 3
            in 40..59 -> 2
            in 20..39 -> 1
            else -> 0
        }
        
        return (restingScore * 0.5 + hrvScore * 0.3 + rangeScore * 0.2).toInt()
    }
}

/**
 * Blood Pressure Classification
 *
 * Enumeration of blood pressure classifications based on AHA guidelines.
 */
enum class BloodPressureClassification {
    HYPOTENSION, // Low blood pressure
    NORMAL, // Normal blood pressure
    ELEVATED, // Elevated blood pressure
    HYPERTENSION_STAGE_1, // Hypertension Stage 1
    HYPERTENSION_STAGE_2, // Hypertension Stage 2
    HYPERTENSIVE_CRISIS // Hypertensive Crisis
}

/**
 * Blood Pressure Stats
 *
 * Contains statistical analysis of blood pressure data, including averages,
 * minimums, maximums, and classification counts.
 */
data class BloodPressureStats(
    val avgSystolic: Int, // Average systolic pressure
    val avgDiastolic: Int, // Average diastolic pressure
    val minSystolic: Int, // Minimum systolic pressure
    val maxSystolic: Int, // Maximum systolic pressure
    val minDiastolic: Int, // Minimum diastolic pressure
    val maxDiastolic: Int, // Maximum diastolic pressure
    val avgPulse: Int?, // Average pulse rate
    val readingsCount: Int, // Number of readings
    val hypertensiveReadingsCount: Int, // Number of hypertensive readings
    val normalReadingsCount: Int, // Number of normal readings
    val hypotensiveReadingsCount: Int, // Number of hypotensive readings
    val morningAvgSystolic: Int?, // Morning average systolic
    val morningAvgDiastolic: Int?, // Morning average diastolic
    val eveningAvgSystolic: Int?, // Evening average systolic
    val eveningAvgDiastolic: Int? // Evening average diastolic
) {
    val overallClassification: BloodPressureClassification
        get() = classifyBloodPressure(avgSystolic, avgDiastolic)
    
    val morningEveningDifference: Pair<Int, Int>?
        get() = if (morningAvgSystolic != null && eveningAvgSystolic != null && 
                   morningAvgDiastolic != null && eveningAvgDiastolic != null) {
            Pair(morningAvgSystolic - eveningAvgSystolic, morningAvgDiastolic - eveningAvgDiastolic)
        } else {
            null
        }
    
    val pulsePressure: Int
        get() = avgSystolic - avgDiastolic
    
    val hasWhiteCoatHypertensionPattern: Boolean
        get() = readingsCount >= 5 && 
                hypertensiveReadingsCount.toDouble() / readingsCount < 0.3 &&
                maxSystolic >= 140
    
    private fun classifyBloodPressure(systolic: Int, diastolic: Int): BloodPressureClassification {
        return when {
            systolic >= 180 || diastolic >= 120 -> BloodPressureClassification.HYPERTENSIVE_CRISIS
            systolic >= 140 || diastolic >= 90 -> BloodPressureClassification.HYPERTENSION_STAGE_2
            systolic >= 130 || diastolic >= 80 -> BloodPressureClassification.HYPERTENSION_STAGE_1
            systolic >= 120 && diastolic < 80 -> BloodPressureClassification.ELEVATED
            systolic in 90..119 && diastolic in 60..79 -> BloodPressureClassification.NORMAL
            else -> BloodPressureClassification.HYPOTENSION
        }
    }
}

/**
 * Sleep Stage
 *
 * Enumeration of sleep stages for sleep analysis.
 */
enum class SleepStage {
    AWAKE,
    LIGHT,
    DEEP,
    REM
}

/**
 * Sleep Stats
 *
 * Contains statistical analysis of sleep data, including duration,
 * efficiency, and time spent in different sleep stages.
 */
data class SleepStats(
    val avgDuration: Double, // Average sleep duration in hours
    val avgSleepEfficiency: Double, // Average sleep efficiency (percentage)
    val avgDeepSleepPercentage: Double, // Average percentage of deep sleep
    val avgRemSleepPercentage: Double, // Average percentage of REM sleep
    val avgLightSleepPercentage: Double, // Average percentage of light sleep
    val avgAwakeTime: Double, // Average time spent awake during sleep in minutes
    val avgSleepScore: Int?, // Average sleep score
    val avgSleepOnset: Int?, // Average sleep onset time in minutes
    val avgWakeTime: Int?, // Average wake time in minutes after sleep onset
    val totalSleepNights: Int // Total number of nights analyzed
) {
    val sleepQualityScore: Int
        get() = calculateSleepQualityScore()
    
    val sleepPatternRegularity: Double
        get() = 100.0 - (avgWakeTime ?: 0).toDouble().coerceAtMost(60.0)
    
    private fun calculateSleepQualityScore(): Int {
        // Duration score (optimal is 7-9 hours)
        val durationScore = when {
            avgDuration < 5 -> 0
            avgDuration < 6 -> 1
            avgDuration < 7 -> 2
            avgDuration <= 9 -> 3
            avgDuration <= 10 -> 2
            else -> 1
        }
        
        // Efficiency score
        val efficiencyScore = when {
            avgSleepEfficiency < 65 -> 0
            avgSleepEfficiency < 75 -> 1
            avgSleepEfficiency < 85 -> 2
            avgSleepEfficiency <= 95 -> 3
            else -> 2
        }
        
        // Deep sleep score (optimal is 15-25%)
        val deepSleepScore = when {
            avgDeepSleepPercentage < 10 -> 0
            avgDeepSleepPercentage < 15 -> 1
            avgDeepSleepPercentage <= 25 -> 3
            avgDeepSleepPercentage <= 30 -> 2
            else -> 1
        }
        
        // REM sleep score (optimal is 20-25%)
        val remSleepScore = when {
            avgRemSleepPercentage < 15 -> 0
            avgRemSleepPercentage < 20 -> 1
            avgRemSleepPercentage <= 25 -> 3
            avgRemSleepPercentage <= 30 -> 2
            else -> 1
        }
        
        // Sleep onset score
        val onsetScore = if (avgSleepOnset != null) {
            when {
                avgSleepOnset > 60 -> 0
                avgSleepOnset > 30 -> 1
                avgSleepOnset > 15 -> 2
                else -> 3
            }
        } else {
            1
        }
        
        // Calculate total score (0-15 scale)
        return durationScore + efficiencyScore + deepSleepScore + remSleepScore + onsetScore
    }
}

/**
 * Activity Type
 *
 * Enumeration of different types of physical activities.
 */
enum class ActivityType {
    WALKING,
    RUNNING,
    CYCLING,
    SWIMMING,
    STRENGTH_TRAINING,
    YOGA,
    HIIT,
    ELLIPTICAL,
    ROWING,
    STAIR_CLIMBING,
    HIKING,
    SPORTS,
    OTHER
}

/**
 * Intensity
 *
 * Enumeration of exercise intensity levels.
 */
enum class Intensity {
    SEDENTARY,
    LOW,
    MODERATE,
    HIGH,
    VERY_HIGH
}

/**
 * Activity Stats
 *
 * Contains statistical analysis of activity data, including steps,
 * distance, calories, and active minutes.
 */
data class ActivityStats(
    val totalSteps: Int, // Total steps
    val totalDistance: Double, // Total distance in meters
    val totalCalories: Double, // Total calories burned
    val totalActiveMinutes: Int, // Total active minutes
    val avgDailySteps: Int, // Average daily steps
    val avgDailyActiveMinutes: Int, // Average daily active minutes
    val highIntensityMinutes: Int, // Minutes of high intensity activity
    val moderateIntensityMinutes: Int, // Minutes of moderate intensity activity
    val lowIntensityMinutes: Int, // Minutes of low intensity activity
    val sedentaryMinutes: Int, // Minutes of sedentary activity
    val daysWithActivity: Int // Number of days with activity
) {
    val activePercentage: Double
        get() = if (totalActiveMinutes + sedentaryMinutes > 0) {
            (totalActiveMinutes.toDouble() / (totalActiveMinutes + sedentaryMinutes)) * 100
        } else {
            0.0
        }
    
    val activityScore: Int
        get() = calculateActivityScore()
    
    val meetsWhoGuidelines: Boolean
        get() = (highIntensityMinutes >= 75 || moderateIntensityMinutes >= 150 || 
                (highIntensityMinutes * 2 + moderateIntensityMinutes >= 150))
    
    private fun calculateActivityScore(): Int {
        // Steps score (10,000 steps per day is the target)
        val stepsScore = when {
            avgDailySteps >= 10000 -> 5
            avgDailySteps >= 7500 -> 4
            avgDailySteps >= 5000 -> 3
            avgDailySteps >= 2500 -> 2
            avgDailySteps > 0 -> 1
            else -> 0
        }
        
        // Active minutes score (150 minutes of moderate or 75 minutes of vigorous per week)
        val weeklyModerateEquivalent = moderateIntensityMinutes + (highIntensityMinutes * 2)
        val activeMinutesScore = when {
            weeklyModerateEquivalent >= 300 -> 5
            weeklyModerateEquivalent >= 150 -> 4
            weeklyModerateEquivalent >= 75 -> 3
            weeklyModerateEquivalent >= 30 -> 2
            weeklyModerateEquivalent > 0 -> 1
            else -> 0
        }
        
        // Consistency score
        val consistencyScore = when {
            daysWithActivity >= 6 -> 5
            daysWithActivity >= 5 -> 4
            daysWithActivity >= 4 -> 3
            daysWithActivity >= 3 -> 2
            daysWithActivity > 0 -> 1
            else -> 0
        }
        
        // Calculate total score (0-15 scale)
        return stepsScore + activeMinutesScore + consistencyScore
    }
}

/**
 * Activity Type Stats
 *
 * Contains statistical analysis for a specific type of activity.
 */
data class ActivityTypeStats(
    val activityType: ActivityType,
    val totalSessions: Int,
    val totalDuration: Int, // Total duration in seconds
    val totalDistance: Double?, // Total distance in meters
    val totalCalories: Double?, // Total calories burned
    val avgDuration: Int, // Average duration in seconds
    val avgDistance: Double?, // Average distance in meters
    val avgCalories: Double?, // Average calories burned
    val avgHeartRate: Int?, // Average heart rate
    val maxHeartRate: Int?, // Maximum heart rate
    val avgIntensity: Intensity // Average intensity
)

/**
 * User
 *
 * Contains user information and preferences.
 */
data class User(
    val id: String,
    val email: String,
    val firstName: String?,
    val lastName: String?,
    val age: Int?,
    val gender: String?,
    val height: Double?, // Height in cm
    val weight: Double?, // Weight in kg
    val dateOfBirth: LocalDate?,
    val restingHeartRate: Int?,
    val maxHeartRate: Int?,
    val stepGoal: Int?,
    val sleepGoal: Double?, // Sleep goal in hours
    val preferredUnits: String?, // "metric" or "imperial"
    val createdAt: LocalDateTime,
    val modifiedAt: LocalDateTime
) {
    val bmi: Double?
        get() = if (height != null && weight != null && height > 0) {
            weight / ((height / 100) * (height / 100))
        } else {
            null
        }
    
    val fullName: String
        get() = listOfNotNull(firstName, lastName).joinToString(" ")
    
    val calculatedAge: Int?
        get() = dateOfBirth?.let { 
            LocalDate.now().year - it.year - 
            if (LocalDate.now().dayOfYear < it.dayOfYear) 1 else 0 
        }
    
    val calculatedMaxHeartRate: Int?
        get() = age?.let { 220 - it }
}

/**
 * Aggregation Type
 *
 * Enumeration of different time-based aggregation types for data analysis.
 */
enum class AggregationType {
    DAILY,
    WEEKLY,
    MONTHLY
}

/**
 * Health Data Aggregate
 *
 * Contains aggregated health data for a specific metric type and time period.
 */
data class HealthDataAggregate(
    val date: LocalDate,
    val metricType: MetricType,
    val minValue: Double,
    val maxValue: Double,
    val avgValue: Double,
    val sumValue: Double,
    val count: Int
)
