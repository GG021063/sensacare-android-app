package com.sensacare.app.data.local.entity

import androidx.room.*
import com.sensacare.app.domain.model.Activity
import com.sensacare.app.domain.model.ActivityIntensity
import com.sensacare.app.domain.model.ActivityType
import com.sensacare.app.domain.model.HeartRateZone
import com.sensacare.app.domain.model.WeatherCondition
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * ActivityEntity - Room database entity for physical activity tracking
 *
 * This entity stores comprehensive physical activity data collected from HBand devices, including:
 * - Step tracking (count, distance, pace, cadence, elevation)
 * - Activity classification (walking, running, cycling, swimming, etc.)
 * - Energy expenditure (calories burned, METs)
 * - Activity duration and intensity metrics
 * - Heart rate data during activity (zones, average, peak)
 * - GPS tracking for outdoor activities (route, speed, altitude)
 * - Recovery metrics and effort scores
 * - Environmental conditions (weather, temperature)
 * - Activity goals and achievements
 *
 * The entity maintains a foreign key relationship with the base HealthDataEntity
 * to enable unified queries across all health metrics while providing
 * activity-specific details.
 */
@Entity(
    tableName = "activity",
    foreignKeys = [
        ForeignKey(
            entity = HealthDataEntity::class,
            parentColumns = ["id"],
            childColumns = ["healthDataId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("healthDataId", unique = true),
        Index("timestamp"),
        Index("userId"),
        Index("activityType"),
        Index("steps"),
        Index("distance"),
        Index("caloriesBurned"),
        Index("startTime"),
        Index("endTime"),
        Index("intensity")
    ]
)
data class ActivityEntity(
    /**
     * Primary key - unique identifier for the activity record
     */
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    /**
     * Foreign key reference to the base health data record
     */
    @ColumnInfo(name = "healthDataId", index = true)
    val healthDataId: String,

    /**
     * Timestamp when the activity data was recorded
     */
    @ColumnInfo(name = "timestamp")
    val timestamp: LocalDateTime,

    /**
     * User identifier - allows for multi-user support
     */
    @ColumnInfo(name = "userId")
    val userId: String,

    /**
     * Device identifier - tracks which device recorded this data
     */
    @ColumnInfo(name = "deviceId")
    val deviceId: String,

    /**
     * Type of activity (walking, running, cycling, etc.)
     */
    @ColumnInfo(name = "activityType")
    val activityType: String,

    /**
     * Start time of the activity
     */
    @ColumnInfo(name = "startTime")
    val startTime: LocalDateTime,

    /**
     * End time of the activity
     */
    @ColumnInfo(name = "endTime")
    val endTime: LocalDateTime,

    /**
     * Duration of the activity in seconds
     */
    @ColumnInfo(name = "durationSeconds")
    val durationSeconds: Int,

    /**
     * Activity intensity level (low, moderate, high, very high)
     */
    @ColumnInfo(name = "intensity")
    val intensity: String,

    /**
     * Number of steps taken during the activity
     */
    @ColumnInfo(name = "steps")
    val steps: Int? = null,

    /**
     * Distance covered in meters
     */
    @ColumnInfo(name = "distance")
    val distance: Float? = null,

    /**
     * Average pace in seconds per kilometer
     */
    @ColumnInfo(name = "pace")
    val pace: Int? = null,

    /**
     * Average speed in meters per second
     */
    @ColumnInfo(name = "speed")
    val speed: Float? = null,

    /**
     * Maximum speed achieved in meters per second
     */
    @ColumnInfo(name = "maxSpeed")
    val maxSpeed: Float? = null,

    /**
     * Average cadence in steps per minute
     */
    @ColumnInfo(name = "cadence")
    val cadence: Int? = null,

    /**
     * Maximum cadence achieved in steps per minute
     */
    @ColumnInfo(name = "maxCadence")
    val maxCadence: Int? = null,

    /**
     * Total elevation gain in meters
     */
    @ColumnInfo(name = "elevationGain")
    val elevationGain: Float? = null,

    /**
     * Total elevation loss in meters
     */
    @ColumnInfo(name = "elevationLoss")
    val elevationLoss: Float? = null,

    /**
     * Maximum elevation reached in meters
     */
    @ColumnInfo(name = "maxElevation")
    val maxElevation: Float? = null,

    /**
     * Minimum elevation reached in meters
     */
    @ColumnInfo(name = "minElevation")
    val minElevation: Float? = null,

    /**
     * Total calories burned during activity
     */
    @ColumnInfo(name = "caloriesBurned")
    val caloriesBurned: Int,

    /**
     * Metabolic Equivalent of Task (MET) value
     */
    @ColumnInfo(name = "metValue")
    val metValue: Float,

    /**
     * Average heart rate during activity in BPM
     */
    @ColumnInfo(name = "avgHeartRate")
    val avgHeartRate: Int? = null,

    /**
     * Maximum heart rate during activity in BPM
     */
    @ColumnInfo(name = "maxHeartRate")
    val maxHeartRate: Int? = null,

    /**
     * Minimum heart rate during activity in BPM
     */
    @ColumnInfo(name = "minHeartRate")
    val minHeartRate: Int? = null,

    /**
     * Time spent in fat burn heart rate zone in seconds
     */
    @ColumnInfo(name = "fatBurnZoneSeconds")
    val fatBurnZoneSeconds: Int? = null,

    /**
     * Time spent in cardio heart rate zone in seconds
     */
    @ColumnInfo(name = "cardioZoneSeconds")
    val cardioZoneSeconds: Int? = null,

    /**
     * Time spent in peak heart rate zone in seconds
     */
    @ColumnInfo(name = "peakZoneSeconds")
    val peakZoneSeconds: Int? = null,

    /**
     * Time spent in custom heart rate zones as JSON string
     * Format: [{"zoneName":"Zone 1", "lowerBound":100, "upperBound":120, "timeSeconds":300}, ...]
     */
    @ColumnInfo(name = "customZonesData")
    val customZonesData: String? = null,

    /**
     * GPS route data as GeoJSON string
     */
    @ColumnInfo(name = "routeData")
    val routeData: String? = null,

    /**
     * GPS data points as JSON array
     * Format: [{"lat":37.7749, "lon":-122.4194, "alt":10, "time":"2023-01-01T12:00:00"}, ...]
     */
    @ColumnInfo(name = "gpsDataPoints")
    val gpsDataPoints: String? = null,

    /**
     * Average stride length in meters
     */
    @ColumnInfo(name = "strideLength")
    val strideLength: Float? = null,

    /**
     * Ground contact time in milliseconds (running)
     */
    @ColumnInfo(name = "groundContactTime")
    val groundContactTime: Int? = null,

    /**
     * Vertical oscillation in centimeters (running)
     */
    @ColumnInfo(name = "verticalOscillation")
    val verticalOscillation: Float? = null,

    /**
     * Average power output in watts (cycling)
     */
    @ColumnInfo(name = "powerOutput")
    val powerOutput: Int? = null,

    /**
     * Maximum power output in watts (cycling)
     */
    @ColumnInfo(name = "maxPowerOutput")
    val maxPowerOutput: Int? = null,

    /**
     * Normalized power in watts (cycling)
     */
    @ColumnInfo(name = "normalizedPower")
    val normalizedPower: Int? = null,

    /**
     * Stroke count (swimming)
     */
    @ColumnInfo(name = "strokeCount")
    val strokeCount: Int? = null,

    /**
     * Pool length in meters (swimming)
     */
    @ColumnInfo(name = "poolLength")
    val poolLength: Int? = null,

    /**
     * Number of laps completed (swimming)
     */
    @ColumnInfo(name = "laps")
    val laps: Int? = null,

    /**
     * SWOLF score (swimming efficiency)
     */
    @ColumnInfo(name = "swolfScore")
    val swolfScore: Int? = null,

    /**
     * Stroke type (swimming)
     */
    @ColumnInfo(name = "strokeType")
    val strokeType: String? = null,

    /**
     * Training effect score (aerobic, 0.0-5.0)
     */
    @ColumnInfo(name = "aerobicTrainingEffect")
    val aerobicTrainingEffect: Float? = null,

    /**
     * Training effect score (anaerobic, 0.0-5.0)
     */
    @ColumnInfo(name = "anaerobicTrainingEffect")
    val anaerobicTrainingEffect: Float? = null,

    /**
     * Recovery time recommendation in hours
     */
    @ColumnInfo(name = "recoveryTimeHours")
    val recoveryTimeHours: Int? = null,

    /**
     * Relative effort score (0-100)
     */
    @ColumnInfo(name = "effortScore")
    val effortScore: Int? = null,

    /**
     * Fitness level score (VO2 max estimate)
     */
    @ColumnInfo(name = "vo2MaxEstimate")
    val vo2MaxEstimate: Float? = null,

    /**
     * Performance condition score (-20 to +20)
     */
    @ColumnInfo(name = "performanceCondition")
    val performanceCondition: Int? = null,

    /**
     * Weather condition during activity
     */
    @ColumnInfo(name = "weatherCondition")
    val weatherCondition: String? = null,

    /**
     * Temperature during activity in Celsius
     */
    @ColumnInfo(name = "temperature")
    val temperature: Float? = null,

    /**
     * Humidity percentage during activity
     */
    @ColumnInfo(name = "humidity")
    val humidity: Float? = null,

    /**
     * Wind speed in km/h during activity
     */
    @ColumnInfo(name = "windSpeed")
    val windSpeed: Float? = null,

    /**
     * Perceived exertion rating (1-10)
     */
    @ColumnInfo(name = "perceivedExertion")
    val perceivedExertion: Int? = null,

    /**
     * Feeling during activity (1-5, bad to good)
     */
    @ColumnInfo(name = "feelingScore")
    val feelingScore: Int? = null,

    /**
     * Activity goal type (distance, time, calories)
     */
    @ColumnInfo(name = "goalType")
    val goalType: String? = null,

    /**
     * Activity goal target value
     */
    @ColumnInfo(name = "goalTarget")
    val goalTarget: Float? = null,

    /**
     * Goal achievement percentage
     */
    @ColumnInfo(name = "goalAchievement")
    val goalAchievement: Float? = null,

    /**
     * Personal records achieved during this activity as JSON
     * Format: [{"type":"fastest_km", "value":240, "previousBest":245}, ...]
     */
    @ColumnInfo(name = "personalRecords")
    val personalRecords: String? = null,

    /**
     * Active minutes credited for this activity
     */
    @ColumnInfo(name = "activeMinutes")
    val activeMinutes: Int,

    /**
     * Intensity minutes credited for this activity
     * (moderate minutes + vigorous minutes * 2)
     */
    @ColumnInfo(name = "intensityMinutes")
    val intensityMinutes: Int,

    /**
     * User's notes about the activity
     */
    @ColumnInfo(name = "notes")
    val notes: String? = null,

    /**
     * Tags for categorizing activity data
     */
    @ColumnInfo(name = "tags")
    val tags: List<String>? = null,

    /**
     * Confidence level of activity tracking (0.0 to 1.0)
     */
    @ColumnInfo(name = "confidenceLevel")
    val confidenceLevel: Float,

    /**
     * Timestamp when this record was created locally
     */
    @ColumnInfo(name = "createdAt")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    /**
     * Timestamp when this record was last modified locally
     */
    @ColumnInfo(name = "modifiedAt")
    val modifiedAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * Convert entity to domain model
     */
    fun toDomainModel(): Activity {
        return Activity(
            id = id,
            healthDataId = healthDataId,
            timestamp = timestamp,
            userId = userId,
            deviceId = deviceId,
            activityType = ActivityType.valueOf(activityType),
            startTime = startTime,
            endTime = endTime,
            durationSeconds = durationSeconds,
            intensity = ActivityIntensity.valueOf(intensity),
            steps = steps,
            distance = distance,
            pace = pace,
            speed = speed,
            maxSpeed = maxSpeed,
            cadence = cadence,
            maxCadence = maxCadence,
            elevationGain = elevationGain,
            elevationLoss = elevationLoss,
            maxElevation = maxElevation,
            minElevation = minElevation,
            caloriesBurned = caloriesBurned,
            metValue = metValue,
            avgHeartRate = avgHeartRate,
            maxHeartRate = maxHeartRate,
            minHeartRate = minHeartRate,
            fatBurnZoneSeconds = fatBurnZoneSeconds,
            cardioZoneSeconds = cardioZoneSeconds,
            peakZoneSeconds = peakZoneSeconds,
            customZonesData = customZonesData,
            routeData = routeData,
            gpsDataPoints = gpsDataPoints,
            strideLength = strideLength,
            groundContactTime = groundContactTime,
            verticalOscillation = verticalOscillation,
            powerOutput = powerOutput,
            maxPowerOutput = maxPowerOutput,
            normalizedPower = normalizedPower,
            strokeCount = strokeCount,
            poolLength = poolLength,
            laps = laps,
            swolfScore = swolfScore,
            strokeType = strokeType,
            aerobicTrainingEffect = aerobicTrainingEffect,
            anaerobicTrainingEffect = anaerobicTrainingEffect,
            recoveryTimeHours = recoveryTimeHours,
            effortScore = effortScore,
            vo2MaxEstimate = vo2MaxEstimate,
            performanceCondition = performanceCondition,
            weatherCondition = weatherCondition?.let { WeatherCondition.valueOf(it) },
            temperature = temperature,
            humidity = humidity,
            windSpeed = windSpeed,
            perceivedExertion = perceivedExertion,
            feelingScore = feelingScore,
            goalType = goalType,
            goalTarget = goalTarget,
            goalAchievement = goalAchievement,
            personalRecords = personalRecords,
            activeMinutes = activeMinutes,
            intensityMinutes = intensityMinutes,
            notes = notes,
            tags = tags ?: emptyList(),
            confidenceLevel = confidenceLevel,
            createdAt = createdAt,
            modifiedAt = modifiedAt
        )
    }

    companion object {
        /**
         * Convert domain model to entity
         */
        fun fromDomainModel(domainModel: Activity): ActivityEntity {
            return ActivityEntity(
                id = domainModel.id,
                healthDataId = domainModel.healthDataId,
                timestamp = domainModel.timestamp,
                userId = domainModel.userId,
                deviceId = domainModel.deviceId,
                activityType = domainModel.activityType.name,
                startTime = domainModel.startTime,
                endTime = domainModel.endTime,
                durationSeconds = domainModel.durationSeconds,
                intensity = domainModel.intensity.name,
                steps = domainModel.steps,
                distance = domainModel.distance,
                pace = domainModel.pace,
                speed = domainModel.speed,
                maxSpeed = domainModel.maxSpeed,
                cadence = domainModel.cadence,
                maxCadence = domainModel.maxCadence,
                elevationGain = domainModel.elevationGain,
                elevationLoss = domainModel.elevationLoss,
                maxElevation = domainModel.maxElevation,
                minElevation = domainModel.minElevation,
                caloriesBurned = domainModel.caloriesBurned,
                metValue = domainModel.metValue,
                avgHeartRate = domainModel.avgHeartRate,
                maxHeartRate = domainModel.maxHeartRate,
                minHeartRate = domainModel.minHeartRate,
                fatBurnZoneSeconds = domainModel.fatBurnZoneSeconds,
                cardioZoneSeconds = domainModel.cardioZoneSeconds,
                peakZoneSeconds = domainModel.peakZoneSeconds,
                customZonesData = domainModel.customZonesData,
                routeData = domainModel.routeData,
                gpsDataPoints = domainModel.gpsDataPoints,
                strideLength = domainModel.strideLength,
                groundContactTime = domainModel.groundContactTime,
                verticalOscillation = domainModel.verticalOscillation,
                powerOutput = domainModel.powerOutput,
                maxPowerOutput = domainModel.maxPowerOutput,
                normalizedPower = domainModel.normalizedPower,
                strokeCount = domainModel.strokeCount,
                poolLength = domainModel.poolLength,
                laps = domainModel.laps,
                swolfScore = domainModel.swolfScore,
                strokeType = domainModel.strokeType,
                aerobicTrainingEffect = domainModel.aerobicTrainingEffect,
                anaerobicTrainingEffect = domainModel.anaerobicTrainingEffect,
                recoveryTimeHours = domainModel.recoveryTimeHours,
                effortScore = domainModel.effortScore,
                vo2MaxEstimate = domainModel.vo2MaxEstimate,
                performanceCondition = domainModel.performanceCondition,
                weatherCondition = domainModel.weatherCondition?.name,
                temperature = domainModel.temperature,
                humidity = domainModel.humidity,
                windSpeed = domainModel.windSpeed,
                perceivedExertion = domainModel.perceivedExertion,
                feelingScore = domainModel.feelingScore,
                goalType = domainModel.goalType,
                goalTarget = domainModel.goalTarget,
                goalAchievement = domainModel.goalAchievement,
                personalRecords = domainModel.personalRecords,
                activeMinutes = domainModel.activeMinutes,
                intensityMinutes = domainModel.intensityMinutes,
                notes = domainModel.notes,
                tags = domainModel.tags,
                confidenceLevel = domainModel.confidenceLevel,
                createdAt = domainModel.createdAt,
                modifiedAt = domainModel.modifiedAt
            )
        }

        /**
         * Calculate activity duration from start and end times
         * @param startTime Start time of activity
         * @param endTime End time of activity
         * @return Duration in seconds
         */
        fun calculateDuration(startTime: LocalDateTime, endTime: LocalDateTime): Int {
            return Duration.between(startTime, endTime).seconds.toInt()
        }

        /**
         * Calculate pace from distance and duration
         * @param distanceMeters Distance in meters
         * @param durationSeconds Duration in seconds
         * @return Pace in seconds per kilometer
         */
        fun calculatePace(distanceMeters: Float, durationSeconds: Int): Int {
            if (distanceMeters <= 0) return 0
            return (durationSeconds * 1000 / distanceMeters).roundToInt()
        }

        /**
         * Calculate speed from distance and duration
         * @param distanceMeters Distance in meters
         * @param durationSeconds Duration in seconds
         * @return Speed in meters per second
         */
        fun calculateSpeed(distanceMeters: Float, durationSeconds: Int): Float {
            if (durationSeconds <= 0) return 0f
            return distanceMeters / durationSeconds
        }

        /**
         * Calculate cadence from steps and duration
         * @param steps Number of steps
         * @param durationSeconds Duration in seconds
         * @return Cadence in steps per minute
         */
        fun calculateCadence(steps: Int, durationSeconds: Int): Int {
            if (durationSeconds <= 0) return 0
            return (steps * 60 / durationSeconds)
        }

        /**
         * Calculate stride length from distance and steps
         * @param distanceMeters Distance in meters
         * @param steps Number of steps
         * @return Stride length in meters
         */
        fun calculateStrideLength(distanceMeters: Float, steps: Int): Float {
            if (steps <= 0) return 0f
            return distanceMeters / steps
        }

        /**
         * Calculate calories burned based on MET value, duration, and user weight
         * @param metValue Metabolic Equivalent of Task value
         * @param durationSeconds Duration in seconds
         * @param weightKg User's weight in kilograms
         * @return Calories burned
         */
        fun calculateCaloriesBurned(metValue: Float, durationSeconds: Int, weightKg: Float): Int {
            // Calories = MET * weight (kg) * duration (hours)
            val durationHours = durationSeconds / 3600f
            return (metValue * weightKg * durationHours).roundToInt()
        }

        /**
         * Estimate MET value based on activity type and intensity
         * @param activityType Type of activity
         * @param intensity Intensity level
         * @param speed Speed in m/s (optional, for more accurate estimation)
         * @return Estimated MET value
         */
        fun estimateMetValue(
            activityType: ActivityType,
            intensity: ActivityIntensity,
            speed: Float? = null
        ): Float {
            return when (activityType) {
                ActivityType.WALKING -> {
                    // Walking MET values based on speed if available
                    if (speed != null) {
                        val speedKmh = speed * 3.6f
                        when {
                            speedKmh < 3.2 -> 2.0f // Slow walking
                            speedKmh < 4.8 -> 3.0f // Moderate walking
                            speedKmh < 6.4 -> 4.3f // Brisk walking
                            else -> 5.0f // Very brisk walking
                        }
                    } else {
                        // Walking MET values based on intensity
                        when (intensity) {
                            ActivityIntensity.LOW -> 2.0f
                            ActivityIntensity.MODERATE -> 3.5f
                            ActivityIntensity.HIGH -> 4.5f
                            ActivityIntensity.VERY_HIGH -> 5.0f
                        }
                    }
                }
                ActivityType.RUNNING -> {
                    // Running MET values based on speed if available
                    if (speed != null) {
                        val speedKmh = speed * 3.6f
                        when {
                            speedKmh < 8.0 -> 8.0f // Jogging
                            speedKmh < 9.7 -> 9.8f // 10:00 min/mile
                            speedKmh < 11.3 -> 11.0f // 8:30 min/mile
                            speedKmh < 12.9 -> 11.8f // 7:30 min/mile
                            speedKmh < 14.5 -> 12.8f // 6:30 min/mile
                            else -> 14.5f // < 6:00 min/mile
                        }
                    } else {
                        // Running MET values based on intensity
                        when (intensity) {
                            ActivityIntensity.LOW -> 8.0f
                            ActivityIntensity.MODERATE -> 10.0f
                            ActivityIntensity.HIGH -> 12.5f
                            ActivityIntensity.VERY_HIGH -> 15.0f
                        }
                    }
                }
                ActivityType.CYCLING -> {
                    // Cycling MET values
                    when (intensity) {
                        ActivityIntensity.LOW -> 4.0f
                        ActivityIntensity.MODERATE -> 8.0f
                        ActivityIntensity.HIGH -> 12.0f
                        ActivityIntensity.VERY_HIGH -> 16.0f
                    }
                }
                ActivityType.SWIMMING -> {
                    // Swimming MET values
                    when (intensity) {
                        ActivityIntensity.LOW -> 6.0f
                        ActivityIntensity.MODERATE -> 8.3f
                        ActivityIntensity.HIGH -> 10.0f
                        ActivityIntensity.VERY_HIGH -> 11.0f
                    }
                }
                ActivityType.HIKING -> {
                    // Hiking MET values
                    when (intensity) {
                        ActivityIntensity.LOW -> 3.5f
                        ActivityIntensity.MODERATE -> 5.3f
                        ActivityIntensity.HIGH -> 7.0f
                        ActivityIntensity.VERY_HIGH -> 8.0f
                    }
                }
                ActivityType.WEIGHT_TRAINING -> {
                    // Weight training MET values
                    when (intensity) {
                        ActivityIntensity.LOW -> 3.0f
                        ActivityIntensity.MODERATE -> 5.0f
                        ActivityIntensity.HIGH -> 6.0f
                        ActivityIntensity.VERY_HIGH -> 7.0f
                    }
                }
                ActivityType.YOGA -> 2.5f
                ActivityType.PILATES -> 3.0f
                ActivityType.DANCING -> 7.0f
                ActivityType.ELLIPTICAL -> 5.0f
                ActivityType.ROWING -> 7.0f
                ActivityType.STAIR_CLIMBING -> 9.0f
                ActivityType.SKIING -> 7.0f
                ActivityType.SNOWBOARDING -> 5.0f
                ActivityType.TENNIS -> 7.0f
                ActivityType.BASKETBALL -> 8.0f
                ActivityType.SOCCER -> 10.0f
                ActivityType.GOLF -> 4.8f
                ActivityType.OTHER -> 4.0f
            }
        }

        /**
         * Calculate intensity minutes based on activity intensity and duration
         * @param intensity Activity intensity level
         * @param durationMinutes Duration in minutes
         * @return Intensity minutes (moderate minutes + vigorous minutes * 2)
         */
        fun calculateIntensityMinutes(intensity: ActivityIntensity, durationMinutes: Int): Int {
            return when (intensity) {
                ActivityIntensity.LOW -> 0 // Low intensity doesn't count for intensity minutes
                ActivityIntensity.MODERATE -> durationMinutes // Moderate counts 1:1
                ActivityIntensity.HIGH, ActivityIntensity.VERY_HIGH -> durationMinutes * 2 // Vigorous counts 2:1
            }
        }

        /**
         * Calculate active minutes based on activity type and duration
         * @param activityType Type of activity
         * @param durationMinutes Duration in minutes
         * @return Active minutes credited
         */
        fun calculateActiveMinutes(activityType: ActivityType, durationMinutes: Int): Int {
            // Some activities may not count fully toward active minutes
            val multiplier = when (activityType) {
                ActivityType.YOGA, ActivityType.PILATES -> 0.5f // Lower intensity activities count less
                ActivityType.WEIGHT_TRAINING -> 0.7f
                ActivityType.GOLF -> 0.6f
                else -> 1.0f // Most activities count fully
            }
            return (durationMinutes * multiplier).roundToInt()
        }

        /**
         * Classify activity type based on sensor data
         * @param steps Steps per minute
         * @param heartRate Average heart rate
         * @param speed Speed in m/s
         * @param elevationChange Elevation change in meters
         * @return Estimated activity type
         */
        fun classifyActivityType(
            steps: Int?,
            heartRate: Int?,
            speed: Float?,
            elevationChange: Float?
        ): ActivityType {
            // This is a simplified classification algorithm
            // In a real app, this would use machine learning or more complex heuristics
            
            if (steps == null || steps < 10) {
                // Very few or no steps could be cycling, swimming, or stationary exercise
                return if (heartRate != null && heartRate > 100) {
                    if (speed != null && speed > 4) {
                        ActivityType.CYCLING
                    } else {
                        ActivityType.WEIGHT_TRAINING
                    }
                } else {
                    ActivityType.OTHER
                }
            }
            
            // Classify based on steps per minute (cadence)
            return when {
                steps < 70 -> ActivityType.WALKING
                steps < 130 -> {
                    // Could be slow running or fast walking
                    if (speed != null && speed > 2.5) {
                        ActivityType.RUNNING
                    } else {
                        ActivityType.WALKING
                    }
                }
                steps < 180 -> ActivityType.RUNNING
                else -> ActivityType.RUNNING // Very high cadence is likely running
            }
        }

        /**
         * Determine activity intensity based on heart rate and maximum heart rate
         * @param avgHeartRate Average heart rate during activity
         * @param maxHeartRate User's maximum heart rate
         * @return Activity intensity level
         */
        fun determineIntensity(avgHeartRate: Int?, maxHeartRate: Int): ActivityIntensity {
            if (avgHeartRate == null) return ActivityIntensity.MODERATE
            
            val hrPercentage = (avgHeartRate.toFloat() / maxHeartRate) * 100
            
            return when {
                hrPercentage < 50 -> ActivityIntensity.LOW
                hrPercentage < 70 -> ActivityIntensity.MODERATE
                hrPercentage < 85 -> ActivityIntensity.HIGH
                else -> ActivityIntensity.VERY_HIGH
            }
        }

        /**
         * Calculate effort score based on duration, intensity, and relative effort
         * @param durationMinutes Duration in minutes
         * @param intensity Activity intensity
         * @param heartRateReservePercentage Percentage of heart rate reserve used
         * @return Effort score (0-100)
         */
        fun calculateEffortScore(
            durationMinutes: Int,
            intensity: ActivityIntensity,
            heartRateReservePercentage: Float
        ): Int {
            // Base score from duration (0-40 points)
            val durationScore = min(40f, durationMinutes * 0.4f)
            
            // Score from intensity (0-30 points)
            val intensityScore = when (intensity) {
                ActivityIntensity.LOW -> 5f
                ActivityIntensity.MODERATE -> 15f
                ActivityIntensity.HIGH -> 25f
                ActivityIntensity.VERY_HIGH -> 30f
            }
            
            // Score from heart rate reserve usage (0-30 points)
            val hrScore = min(30f, heartRateReservePercentage * 0.3f)
            
            // Calculate final score (0-100)
            return min(100f, durationScore + intensityScore + hrScore).roundToInt()
        }

        /**
         * Calculate VO2 max estimate based on heart rate and speed
         * @param restingHeartRate Resting heart rate in BPM
         * @param maxHeartRate Maximum heart rate in BPM
         * @param avgHeartRate Average heart rate during activity in BPM
         * @param avgSpeed Average speed in m/s
         * @param ageYears User's age in years
         * @param isMale User's biological sex (true for male, false for female)
         * @return Estimated VO2 max in ml/kg/min
         */
        fun estimateVO2Max(
            restingHeartRate: Int,
            maxHeartRate: Int,
            avgHeartRate: Int,
            avgSpeed: Float,
            ageYears: Int,
            isMale: Boolean
        ): Float {
            // This is a simplified VO2 max estimation based on heart rate and speed
            // More accurate models would use additional factors and calibration
            
            // Calculate heart rate reserve
            val hrr = maxHeartRate - restingHeartRate
            
            // Calculate heart rate ratio
            val hrRatio = (avgHeartRate - restingHeartRate) / hrr.toFloat()
            
            // Base VO2 max estimate using speed (in km/h)
            val speedKmh = avgSpeed * 3.6f
            var vo2Max = 15.3f * speedKmh
            
            // Adjust for heart rate ratio
            vo2Max *= (1 - hrRatio * 0.1f)
            
            // Adjust for age
            vo2Max -= (ageYears - 20) * 0.2f
            
            // Adjust for biological sex
            if (!isMale) {
                vo2Max *= 0.9f
            }
            
            // Ensure result is within reasonable range
            return max(20f, min(80f, vo2Max))
        }

        /**
         * Calculate recovery time based on effort and fitness level
         * @param effortScore Effort score (0-100)
         * @param vo2Max VO2 max estimate
         * @return Recommended recovery time in hours
         */
        fun calculateRecoveryTime(effortScore: Int, vo2Max: Float?): Int {
            // Base recovery time based on effort score
            val baseRecovery = when {
                effortScore < 20 -> 0
                effortScore < 40 -> 12
                effortScore < 60 -> 24
                effortScore < 80 -> 36
                else -> 48
            }
            
            // Adjust based on fitness level (higher VO2 max = faster recovery)
            val fitnessAdjustment = if (vo2Max != null) {
                when {
                    vo2Max < 30 -> 1.2f // Poor fitness, slower recovery
                    vo2Max < 40 -> 1.0f // Average fitness, no adjustment
                    vo2Max < 50 -> 0.8f // Good fitness, faster recovery
                    else -> 0.7f // Excellent fitness, much faster recovery
                }
            } else {
                1.0f // No VO2 max data, no adjustment
            }
            
            return (baseRecovery * fitnessAdjustment).roundToInt()
        }

        /**
         * Calculate SWOLF score for swimming
         * @param strokeCount Number of strokes
         * @param durationSeconds Duration in seconds
         * @return SWOLF score (lower is better)
         */
        fun calculateSwolf(strokeCount: Int, durationSeconds: Int): Int {
            // SWOLF = stroke count + seconds
            return strokeCount + durationSeconds
        }

        /**
         * Calculate training effect score based on EPOC
         * @param epoc Excess Post-exercise Oxygen Consumption
         * @param vo2Max VO2 max estimate
         * @param isAerobic Whether to calculate aerobic (true) or anaerobic (false) effect
         * @return Training effect score (0.0-5.0)
         */
        fun calculateTrainingEffect(epoc: Float, vo2Max: Float, isAerobic: Boolean): Float {
            // This is a simplified model of training effect calculation
            // Actual algorithms use more complex physiological models
            
            // Normalize EPOC based on VO2 max
            val normalizedEpoc = epoc / (vo2Max * 0.1f)
            
            // Different scaling for aerobic vs anaerobic
            val scaleFactor = if (isAerobic) 1.0f else 1.5f
            
            // Calculate effect score (0.0-5.0)
            val effect = normalizedEpoc * scaleFactor
            
            // Ensure result is within valid range
            return max(0f, min(5f, effect))
        }

        /**
         * Estimate EPOC based on heart rate and duration
         * @param avgHeartRate Average heart rate during activity
         * @param maxHeartRate Maximum heart rate
         * @param durationMinutes Duration in minutes
         * @return Estimated EPOC value
         */
        fun estimateEpoc(avgHeartRate: Int, maxHeartRate: Int, durationMinutes: Int): Float {
            // Calculate heart rate as percentage of max
            val hrPercentage = avgHeartRate.toFloat() / maxHeartRate
            
            // Base EPOC calculation
            val baseEpoc = when {
                hrPercentage < 0.5 -> 0.1f * durationMinutes
                hrPercentage < 0.6 -> 0.2f * durationMinutes
                hrPercentage < 0.7 -> 0.4f * durationMinutes
                hrPercentage < 0.8 -> 0.7f * durationMinutes
                hrPercentage < 0.9 -> 1.0f * durationMinutes
                else -> 1.5f * durationMinutes
            }
            
            // Apply exponential scaling for longer durations
            return baseEpoc * (1 + (durationMinutes / 60f).pow(0.6f))
        }

        /**
         * Calculate performance condition based on heart rate vs expected heart rate
         * @param actualHeartRate Actual heart rate during activity
         * @param expectedHeartRate Expected heart rate for this effort level
         * @return Performance condition score (-20 to +20)
         */
        fun calculatePerformanceCondition(actualHeartRate: Int, expectedHeartRate: Int): Int {
            // Calculate difference as percentage
            val difference = (expectedHeartRate - actualHeartRate) / expectedHeartRate.toFloat() * 100
            
            // Scale to -20 to +20 range
            return (difference * 0.4f).roundToInt().coerceIn(-20, 20)
        }

        /**
         * Validate activity data for physiological plausibility
         * @param steps Number of steps
         * @param distance Distance in meters
         * @param durationSeconds Duration in seconds
         * @param heartRate Average heart rate
         * @return True if the activity data is within realistic human ranges
         */
        fun isValidActivityData(
            steps: Int?,
            distance: Float?,
            durationSeconds: Int,
            heartRate: Int?
        ): Boolean {
            // Check if duration is reasonable (1 second to 24 hours)
            if (durationSeconds < 1 || durationSeconds > 86400) return false
            
            // Check if steps are reasonable (if provided)
            if (steps != null && (steps < 0 || steps > durationSeconds * 4)) return false
            
            // Check if distance is reasonable (if provided)
            if (distance != null && (distance < 0 || distance > 300000)) return false
            
            // Check if heart rate is reasonable (if provided)
            if (heartRate != null && (heartRate < 30 || heartRate > 220)) return false
            
            // Check if pace is reasonable (if both steps and distance provided)
            if (steps != null && distance != null && distance > 0) {
                val strideLength = distance / steps
                // Stride length should typically be between 0.3 and 3.0 meters
                if (strideLength < 0.3f || strideLength > 3.0f) return false
            }
            
            return true
        }
    }
}
