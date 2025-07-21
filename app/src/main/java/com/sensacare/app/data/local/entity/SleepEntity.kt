package com.sensacare.app.data.local.entity

import androidx.room.*
import com.sensacare.app.domain.model.Sleep
import com.sensacare.app.domain.model.SleepQuality
import com.sensacare.app.domain.model.SleepStage
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * SleepEntity - Room database entity for sleep tracking data
 *
 * This entity stores comprehensive sleep data collected from HBand devices, including:
 * - Sleep session details (bedtime, wake time, total sleep duration)
 * - Sleep architecture (light, deep, REM, awake stages)
 * - Sleep quality metrics (efficiency, interruptions, movements)
 * - Physiological measurements during sleep (heart rate, respiratory rate)
 * - Environmental factors (temperature, noise level)
 * - Sleep consistency and patterns
 * - Sleep debt and goal comparison
 *
 * The entity maintains a foreign key relationship with the base HealthDataEntity
 * to enable unified queries across all health metrics while providing
 * sleep-specific details.
 */
@Entity(
    tableName = "sleep",
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
        Index("bedtime"),
        Index("wakeTime"),
        Index("sleepQuality"),
        Index("sleepEfficiency")
    ]
)
data class SleepEntity(
    /**
     * Primary key - unique identifier for the sleep record
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
     * Timestamp when the sleep data was recorded (typically wake time)
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
     * Time when the user went to bed
     */
    @ColumnInfo(name = "bedtime")
    val bedtime: LocalDateTime,

    /**
     * Time when the user woke up
     */
    @ColumnInfo(name = "wakeTime")
    val wakeTime: LocalDateTime,

    /**
     * Total time in bed in minutes
     */
    @ColumnInfo(name = "timeInBed")
    val timeInBed: Int,

    /**
     * Total sleep time in minutes (excludes time awake)
     */
    @ColumnInfo(name = "totalSleepTime")
    val totalSleepTime: Int,

    /**
     * Time to fall asleep in minutes (sleep latency)
     */
    @ColumnInfo(name = "timeToFallAsleep")
    val timeToFallAsleep: Int,

    /**
     * Sleep efficiency percentage (total sleep time / time in bed * 100)
     */
    @ColumnInfo(name = "sleepEfficiency")
    val sleepEfficiency: Float,

    /**
     * Overall sleep quality score (0-100)
     */
    @ColumnInfo(name = "sleepQuality")
    val sleepQuality: Int,

    /**
     * Sleep quality category (POOR, FAIR, GOOD, EXCELLENT)
     */
    @ColumnInfo(name = "sleepQualityCategory")
    val sleepQualityCategory: String,

    /**
     * Time spent in light sleep in minutes
     */
    @ColumnInfo(name = "lightSleepDuration")
    val lightSleepDuration: Int,

    /**
     * Percentage of total sleep time spent in light sleep
     */
    @ColumnInfo(name = "lightSleepPercentage")
    val lightSleepPercentage: Float,

    /**
     * Time spent in deep sleep in minutes
     */
    @ColumnInfo(name = "deepSleepDuration")
    val deepSleepDuration: Int,

    /**
     * Percentage of total sleep time spent in deep sleep
     */
    @ColumnInfo(name = "deepSleepPercentage")
    val deepSleepPercentage: Float,

    /**
     * Time spent in REM sleep in minutes
     */
    @ColumnInfo(name = "remSleepDuration")
    val remSleepDuration: Int,

    /**
     * Percentage of total sleep time spent in REM sleep
     */
    @ColumnInfo(name = "remSleepPercentage")
    val remSleepPercentage: Float,

    /**
     * Time spent awake during sleep period in minutes
     */
    @ColumnInfo(name = "awakeTime")
    val awakeTime: Int,

    /**
     * Percentage of time in bed spent awake
     */
    @ColumnInfo(name = "awakePercentage")
    val awakePercentage: Float,

    /**
     * Number of times the sleep was interrupted
     */
    @ColumnInfo(name = "interruptionCount")
    val interruptionCount: Int,

    /**
     * Detailed sleep stage transitions as JSON string
     * Format: [{"stage":"LIGHT", "startTime":"2023-01-01T23:15:00", "durationMinutes":30}, ...]
     */
    @ColumnInfo(name = "sleepStages")
    val sleepStages: String,

    /**
     * Number of significant movements detected during sleep
     */
    @ColumnInfo(name = "movementCount")
    val movementCount: Int,

    /**
     * Movement intensity score (0-100)
     */
    @ColumnInfo(name = "movementIntensity")
    val movementIntensity: Int? = null,

    /**
     * Average heart rate during sleep in BPM
     */
    @ColumnInfo(name = "avgHeartRate")
    val avgHeartRate: Int? = null,

    /**
     * Minimum heart rate during sleep in BPM
     */
    @ColumnInfo(name = "minHeartRate")
    val minHeartRate: Int? = null,

    /**
     * Maximum heart rate during sleep in BPM
     */
    @ColumnInfo(name = "maxHeartRate")
    val maxHeartRate: Int? = null,

    /**
     * Heart rate variability during sleep (RMSSD in ms)
     */
    @ColumnInfo(name = "heartRateVariability")
    val heartRateVariability: Float? = null,

    /**
     * Average respiratory rate during sleep (breaths per minute)
     */
    @ColumnInfo(name = "avgRespiratoryRate")
    val avgRespiratoryRate: Float? = null,

    /**
     * Minimum respiratory rate during sleep (breaths per minute)
     */
    @ColumnInfo(name = "minRespiratoryRate")
    val minRespiratoryRate: Float? = null,

    /**
     * Maximum respiratory rate during sleep (breaths per minute)
     */
    @ColumnInfo(name = "maxRespiratoryRate")
    val maxRespiratoryRate: Float? = null,

    /**
     * Snoring duration in minutes
     */
    @ColumnInfo(name = "snoringDuration")
    val snoringDuration: Int? = null,

    /**
     * Snoring episodes count
     */
    @ColumnInfo(name = "snoringEpisodes")
    val snoringEpisodes: Int? = null,

    /**
     * Average ambient temperature during sleep in Celsius
     */
    @ColumnInfo(name = "ambientTemperature")
    val ambientTemperature: Float? = null,

    /**
     * Average ambient noise level during sleep in decibels
     */
    @ColumnInfo(name = "noiseLevel")
    val noiseLevel: Float? = null,

    /**
     * Average ambient light level during sleep (0-100)
     */
    @ColumnInfo(name = "lightLevel")
    val lightLevel: Int? = null,

    /**
     * Humidity level during sleep as percentage
     */
    @ColumnInfo(name = "humidity")
    val humidity: Float? = null,

    /**
     * User's sleep goal in minutes
     */
    @ColumnInfo(name = "sleepGoal")
    val sleepGoal: Int? = null,

    /**
     * Sleep debt in minutes (difference between ideal and actual sleep duration)
     */
    @ColumnInfo(name = "sleepDebt")
    val sleepDebt: Int? = null,

    /**
     * Sleep midpoint time (halfway between sleep onset and wake time)
     */
    @ColumnInfo(name = "sleepMidpoint")
    val sleepMidpoint: LocalTime? = null,

    /**
     * Sleep consistency score (0-100) - measures regularity of sleep schedule
     */
    @ColumnInfo(name = "sleepConsistency")
    val sleepConsistency: Int? = null,

    /**
     * Social jet lag in minutes (difference between weekday and weekend sleep midpoints)
     */
    @ColumnInfo(name = "socialJetLag")
    val socialJetLag: Int? = null,

    /**
     * Deviation from user's typical bedtime in minutes
     */
    @ColumnInfo(name = "bedtimeDeviation")
    val bedtimeDeviation: Int? = null,

    /**
     * Deviation from user's typical wake time in minutes
     */
    @ColumnInfo(name = "wakeTimeDeviation")
    val wakeTimeDeviation: Int? = null,

    /**
     * User's subjective sleep quality rating (1-10)
     */
    @ColumnInfo(name = "subjectiveQuality")
    val subjectiveQuality: Int? = null,

    /**
     * User-reported sleep notes
     */
    @ColumnInfo(name = "notes")
    val notes: String? = null,

    /**
     * Tags for categorizing sleep data
     */
    @ColumnInfo(name = "tags")
    val tags: List<String>? = null,

    /**
     * Confidence level of the sleep tracking data (0.0 to 1.0)
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
    fun toDomainModel(): Sleep {
        return Sleep(
            id = id,
            healthDataId = healthDataId,
            timestamp = timestamp,
            userId = userId,
            deviceId = deviceId,
            bedtime = bedtime,
            wakeTime = wakeTime,
            timeInBed = timeInBed,
            totalSleepTime = totalSleepTime,
            timeToFallAsleep = timeToFallAsleep,
            sleepEfficiency = sleepEfficiency,
            sleepQuality = sleepQuality,
            sleepQualityCategory = SleepQuality.valueOf(sleepQualityCategory),
            lightSleepDuration = lightSleepDuration,
            lightSleepPercentage = lightSleepPercentage,
            deepSleepDuration = deepSleepDuration,
            deepSleepPercentage = deepSleepPercentage,
            remSleepDuration = remSleepDuration,
            remSleepPercentage = remSleepPercentage,
            awakeTime = awakeTime,
            awakePercentage = awakePercentage,
            interruptionCount = interruptionCount,
            sleepStages = sleepStages,
            movementCount = movementCount,
            movementIntensity = movementIntensity,
            avgHeartRate = avgHeartRate,
            minHeartRate = minHeartRate,
            maxHeartRate = maxHeartRate,
            heartRateVariability = heartRateVariability,
            avgRespiratoryRate = avgRespiratoryRate,
            minRespiratoryRate = minRespiratoryRate,
            maxRespiratoryRate = maxRespiratoryRate,
            snoringDuration = snoringDuration,
            snoringEpisodes = snoringEpisodes,
            ambientTemperature = ambientTemperature,
            noiseLevel = noiseLevel,
            lightLevel = lightLevel,
            humidity = humidity,
            sleepGoal = sleepGoal,
            sleepDebt = sleepDebt,
            sleepMidpoint = sleepMidpoint,
            sleepConsistency = sleepConsistency,
            socialJetLag = socialJetLag,
            bedtimeDeviation = bedtimeDeviation,
            wakeTimeDeviation = wakeTimeDeviation,
            subjectiveQuality = subjectiveQuality,
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
        fun fromDomainModel(domainModel: Sleep): SleepEntity {
            return SleepEntity(
                id = domainModel.id,
                healthDataId = domainModel.healthDataId,
                timestamp = domainModel.timestamp,
                userId = domainModel.userId,
                deviceId = domainModel.deviceId,
                bedtime = domainModel.bedtime,
                wakeTime = domainModel.wakeTime,
                timeInBed = domainModel.timeInBed,
                totalSleepTime = domainModel.totalSleepTime,
                timeToFallAsleep = domainModel.timeToFallAsleep,
                sleepEfficiency = domainModel.sleepEfficiency,
                sleepQuality = domainModel.sleepQuality,
                sleepQualityCategory = domainModel.sleepQualityCategory.name,
                lightSleepDuration = domainModel.lightSleepDuration,
                lightSleepPercentage = domainModel.lightSleepPercentage,
                deepSleepDuration = domainModel.deepSleepDuration,
                deepSleepPercentage = domainModel.deepSleepPercentage,
                remSleepDuration = domainModel.remSleepDuration,
                remSleepPercentage = domainModel.remSleepPercentage,
                awakeTime = domainModel.awakeTime,
                awakePercentage = domainModel.awakePercentage,
                interruptionCount = domainModel.interruptionCount,
                sleepStages = domainModel.sleepStages,
                movementCount = domainModel.movementCount,
                movementIntensity = domainModel.movementIntensity,
                avgHeartRate = domainModel.avgHeartRate,
                minHeartRate = domainModel.minHeartRate,
                maxHeartRate = domainModel.maxHeartRate,
                heartRateVariability = domainModel.heartRateVariability,
                avgRespiratoryRate = domainModel.avgRespiratoryRate,
                minRespiratoryRate = domainModel.minRespiratoryRate,
                maxRespiratoryRate = domainModel.maxRespiratoryRate,
                snoringDuration = domainModel.snoringDuration,
                snoringEpisodes = domainModel.snoringEpisodes,
                ambientTemperature = domainModel.ambientTemperature,
                noiseLevel = domainModel.noiseLevel,
                lightLevel = domainModel.lightLevel,
                humidity = domainModel.humidity,
                sleepGoal = domainModel.sleepGoal,
                sleepDebt = domainModel.sleepDebt,
                sleepMidpoint = domainModel.sleepMidpoint,
                sleepConsistency = domainModel.sleepConsistency,
                socialJetLag = domainModel.socialJetLag,
                bedtimeDeviation = domainModel.bedtimeDeviation,
                wakeTimeDeviation = domainModel.wakeTimeDeviation,
                subjectiveQuality = domainModel.subjectiveQuality,
                notes = domainModel.notes,
                tags = domainModel.tags,
                confidenceLevel = domainModel.confidenceLevel,
                createdAt = domainModel.createdAt,
                modifiedAt = domainModel.modifiedAt
            )
        }

        /**
         * Calculate sleep duration from bedtime and wake time
         * @param bedtime Time when the user went to bed
         * @param wakeTime Time when the user woke up
         * @return Sleep duration in minutes
         */
        fun calculateSleepDuration(bedtime: LocalDateTime, wakeTime: LocalDateTime): Int {
            return Duration.between(bedtime, wakeTime).toMinutes().toInt()
        }

        /**
         * Calculate sleep efficiency
         * @param totalSleepTime Total time spent asleep in minutes
         * @param timeInBed Total time spent in bed in minutes
         * @return Sleep efficiency as a percentage (0-100)
         */
        fun calculateSleepEfficiency(totalSleepTime: Int, timeInBed: Int): Float {
            if (timeInBed <= 0) return 0f
            return (totalSleepTime.toFloat() / timeInBed) * 100
        }

        /**
         * Calculate sleep quality score based on multiple factors
         * @param sleepEfficiency Sleep efficiency percentage
         * @param deepSleepPercentage Percentage of sleep spent in deep sleep
         * @param remSleepPercentage Percentage of sleep spent in REM sleep
         * @param interruptionCount Number of sleep interruptions
         * @param totalSleepTime Total sleep time in minutes
         * @param timeToFallAsleep Time to fall asleep in minutes
         * @return Sleep quality score (0-100)
         */
        fun calculateSleepQuality(
            sleepEfficiency: Float,
            deepSleepPercentage: Float,
            remSleepPercentage: Float,
            interruptionCount: Int,
            totalSleepTime: Int,
            timeToFallAsleep: Int
        ): Int {
            // Base score from sleep efficiency (0-40 points)
            val efficiencyScore = min(40f, sleepEfficiency * 0.4f)
            
            // Score for deep sleep (0-20 points)
            // Ideal deep sleep is 15-25% of total sleep
            val deepSleepScore = when {
                deepSleepPercentage < 10 -> deepSleepPercentage
                deepSleepPercentage < 15 -> 10 + (deepSleepPercentage - 10) * 2
                deepSleepPercentage <= 25 -> 20.0f // Optimal range
                deepSleepPercentage < 30 -> 20 - (deepSleepPercentage - 25)
                else -> 15.0f // Too much deep sleep may indicate recovery needs
            }
            
            // Score for REM sleep (0-20 points)
            // Ideal REM sleep is 20-25% of total sleep
            val remSleepScore = when {
                remSleepPercentage < 15 -> remSleepPercentage * 0.67f
                remSleepPercentage < 20 -> 10 + (remSleepPercentage - 15) * 2
                remSleepPercentage <= 25 -> 20.0f // Optimal range
                remSleepPercentage < 30 -> 20 - (remSleepPercentage - 25)
                else -> 15.0f // Too much REM sleep may indicate stress
            }
            
            // Penalty for interruptions (0-10 points deduction)
            val interruptionPenalty = min(10f, interruptionCount * 2f)
            
            // Score for total sleep duration (0-15 points)
            // Ideal sleep duration is 7-9 hours (420-540 minutes)
            val durationScore = when {
                totalSleepTime < 360 -> totalSleepTime * 0.033f // Less than 6 hours
                totalSleepTime < 420 -> 12 + (totalSleepTime - 360) * 0.05f // 6-7 hours
                totalSleepTime <= 540 -> 15.0f // 7-9 hours (optimal)
                totalSleepTime < 600 -> 15 - (totalSleepTime - 540) * 0.05f // 9-10 hours
                else -> 12.0f // More than 10 hours may indicate oversleeping
            }
            
            // Score for sleep latency (0-5 points)
            // Ideal time to fall asleep is 10-20 minutes
            val latencyScore = when {
                timeToFallAsleep < 5 -> 3.0f // Falling asleep too quickly may indicate exhaustion
                timeToFallAsleep < 10 -> 4.0f // Quick but not too quick
                timeToFallAsleep <= 20 -> 5.0f // Optimal range
                timeToFallAsleep < 30 -> 4.0f // Slightly delayed
                timeToFallAsleep < 45 -> 3.0f // Moderately delayed
                timeToFallAsleep < 60 -> 2.0f // Significantly delayed
                else -> 0.0f // Severe insomnia
            }
            
            // Calculate final score (0-100)
            val totalScore = efficiencyScore + deepSleepScore + remSleepScore - interruptionPenalty + durationScore + latencyScore
            
            // Ensure score is within 0-100 range
            return min(100f, max(0f, totalScore)).roundToInt()
        }

        /**
         * Determine sleep quality category based on numerical score
         * @param sleepQualityScore Sleep quality score (0-100)
         * @return Sleep quality category
         */
        fun determineSleepQualityCategory(sleepQualityScore: Int): SleepQuality {
            return when {
                sleepQualityScore >= 85 -> SleepQuality.EXCELLENT
                sleepQualityScore >= 70 -> SleepQuality.GOOD
                sleepQualityScore >= 50 -> SleepQuality.FAIR
                else -> SleepQuality.POOR
            }
        }

        /**
         * Calculate sleep debt based on recommended sleep duration
         * @param totalSleepTime Total sleep time in minutes
         * @param recommendedSleepTime Recommended sleep time in minutes (default: 480 = 8 hours)
         * @return Sleep debt in minutes (negative if sleep deficit, positive if sleep surplus)
         */
        fun calculateSleepDebt(totalSleepTime: Int, recommendedSleepTime: Int = 480): Int {
            return totalSleepTime - recommendedSleepTime
        }

        /**
         * Calculate sleep midpoint (halfway between sleep onset and wake time)
         * @param bedtime Time when the user went to bed
         * @param timeToFallAsleep Time to fall asleep in minutes
         * @param wakeTime Time when the user woke up
         * @return Sleep midpoint as LocalTime
         */
        fun calculateSleepMidpoint(
            bedtime: LocalDateTime,
            timeToFallAsleep: Int,
            wakeTime: LocalDateTime
        ): LocalTime {
            val sleepOnset = bedtime.plusMinutes(timeToFallAsleep.toLong())
            val midpointDateTime = sleepOnset.plus(Duration.between(sleepOnset, wakeTime).dividedBy(2))
            return midpointDateTime.toLocalTime()
        }

        /**
         * Calculate social jet lag (difference between weekday and weekend sleep midpoints)
         * @param weekdaySleepMidpoint Average sleep midpoint on weekdays
         * @param weekendSleepMidpoint Average sleep midpoint on weekends
         * @return Social jet lag in minutes
         */
        fun calculateSocialJetLag(weekdaySleepMidpoint: LocalTime, weekendSleepMidpoint: LocalTime): Int {
            val weekdayMinutes = weekdaySleepMidpoint.hour * 60 + weekdaySleepMidpoint.minute
            val weekendMinutes = weekendSleepMidpoint.hour * 60 + weekendSleepMidpoint.minute
            
            // Handle cases where one midpoint is before midnight and the other after
            var difference = weekendMinutes - weekdayMinutes
            if (difference > 720) difference -= 1440 // More than 12 hours difference means we crossed midnight
            if (difference < -720) difference += 1440
            
            return abs(difference)
        }

        /**
         * Calculate sleep consistency score based on bedtime and wake time regularity
         * @param bedtimeDeviation Standard deviation of bedtimes in minutes
         * @param wakeTimeDeviation Standard deviation of wake times in minutes
         * @return Sleep consistency score (0-100)
         */
        fun calculateSleepConsistency(bedtimeDeviation: Int, wakeTimeDeviation: Int): Int {
            // Lower deviation means higher consistency
            // Max score at 0 deviation, min score at 120+ minutes deviation
            val bedtimeScore = max(0, 50 - (bedtimeDeviation * 50 / 120))
            val wakeTimeScore = max(0, 50 - (wakeTimeDeviation * 50 / 120))
            
            return bedtimeScore + wakeTimeScore
        }

        /**
         * Validate sleep data for physiological plausibility
         * @param timeInBed Total time in bed in minutes
         * @param totalSleepTime Total sleep time in minutes
         * @param deepSleepPercentage Percentage of sleep spent in deep sleep
         * @param remSleepPercentage Percentage of sleep spent in REM sleep
         * @param lightSleepPercentage Percentage of sleep spent in light sleep
         * @return True if the sleep data is within realistic human ranges
         */
        fun isValidSleepData(
            timeInBed: Int,
            totalSleepTime: Int,
            deepSleepPercentage: Float,
            remSleepPercentage: Float,
            lightSleepPercentage: Float
        ): Boolean {
            // Check if time in bed is reasonable (2-16 hours)
            if (timeInBed < 120 || timeInBed > 960) return false
            
            // Check if total sleep time is less than or equal to time in bed
            if (totalSleepTime > timeInBed) return false
            
            // Check if sleep stage percentages sum to approximately 100%
            val totalPercentage = deepSleepPercentage + remSleepPercentage + lightSleepPercentage
            if (totalPercentage < 95 || totalPercentage > 105) return false
            
            // Check if individual sleep stages are within typical ranges
            if (deepSleepPercentage < 5 || deepSleepPercentage > 35) return false
            if (remSleepPercentage < 5 || remSleepPercentage > 35) return false
            if (lightSleepPercentage < 40 || lightSleepPercentage > 80) return false
            
            return true
        }

        /**
         * Get recommended sleep duration based on age
         * @param ageYears User's age in years
         * @return Recommended sleep duration in minutes
         */
        fun getRecommendedSleepDuration(ageYears: Int): Int {
            return when {
                ageYears < 1 -> 840 // 14 hours
                ageYears < 3 -> 780 // 13 hours
                ageYears < 6 -> 720 // 12 hours
                ageYears < 13 -> 660 // 11 hours
                ageYears < 18 -> 600 // 10 hours
                ageYears < 65 -> 480 // 8 hours
                else -> 450 // 7.5 hours
            }
        }

        /**
         * Calculate optimal bedtime based on desired wake time and recommended sleep duration
         * @param desiredWakeTime Desired wake time
         * @param recommendedSleepDuration Recommended sleep duration in minutes
         * @param avgTimeToFallAsleep Average time to fall asleep in minutes
         * @return Optimal bedtime
         */
        fun calculateOptimalBedtime(
            desiredWakeTime: LocalDateTime,
            recommendedSleepDuration: Int,
            avgTimeToFallAsleep: Int = 15
        ): LocalDateTime {
            return desiredWakeTime.minusMinutes((recommendedSleepDuration + avgTimeToFallAsleep).toLong())
        }
    }
}
