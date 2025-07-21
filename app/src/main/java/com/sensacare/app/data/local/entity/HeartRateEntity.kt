package com.sensacare.app.data.local.entity

import androidx.room.*
import com.sensacare.app.domain.model.HeartRate
import com.sensacare.app.domain.model.HeartRateContext
import com.sensacare.app.domain.model.HeartRateZone
import java.time.LocalDateTime
import java.util.UUID

/**
 * HeartRateEntity - Room database entity for heart rate measurements
 *
 * This entity stores comprehensive heart rate data collected from HBand devices, including:
 * - Basic heart rate (BPM) measurements
 * - Heart rate variability (HRV) metrics
 * - Heart rate zones classification
 * - Rest vs. active heart rate differentiation
 * - Measurement context (exercise, rest, sleep)
 * - Quality indicators and confidence levels
 *
 * The entity maintains a foreign key relationship with the base HealthDataEntity
 * to enable unified queries across all health metrics while providing
 * heart rate specific details.
 */
@Entity(
    tableName = "heart_rate",
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
        Index("bpm"),
        Index("isRestingHeartRate"),
        Index("heartRateZone"),
        Index("context")
    ]
)
data class HeartRateEntity(
    /**
     * Primary key - unique identifier for the heart rate record
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
     * Timestamp when the heart rate was measured
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
     * Heart rate in beats per minute (BPM)
     */
    @ColumnInfo(name = "bpm")
    val bpm: Int,

    /**
     * Flag indicating if this is a resting heart rate measurement
     */
    @ColumnInfo(name = "isRestingHeartRate")
    val isRestingHeartRate: Boolean = false,

    /**
     * Heart rate zone classification (rest, fat burn, cardio, peak)
     */
    @ColumnInfo(name = "heartRateZone")
    val heartRateZone: String,

    /**
     * Minimum heart rate during the measurement period (if applicable)
     */
    @ColumnInfo(name = "minBpm")
    val minBpm: Int? = null,

    /**
     * Maximum heart rate during the measurement period (if applicable)
     */
    @ColumnInfo(name = "maxBpm")
    val maxBpm: Int? = null,

    /**
     * Average heart rate during the measurement period (if applicable)
     */
    @ColumnInfo(name = "avgBpm")
    val avgBpm: Int? = null,

    /**
     * Heart Rate Variability (RMSSD) in milliseconds
     * Root Mean Square of Successive Differences between normal heartbeats
     */
    @ColumnInfo(name = "hrvRmssd")
    val hrvRmssd: Float? = null,

    /**
     * Heart Rate Variability (SDNN) in milliseconds
     * Standard Deviation of NN intervals
     */
    @ColumnInfo(name = "hrvSdnn")
    val hrvSdnn: Float? = null,

    /**
     * Low Frequency power of HRV (0.04-0.15 Hz)
     */
    @ColumnInfo(name = "hrvLf")
    val hrvLf: Float? = null,

    /**
     * High Frequency power of HRV (0.15-0.4 Hz)
     */
    @ColumnInfo(name = "hrvHf")
    val hrvHf: Float? = null,

    /**
     * LF/HF ratio - balance between sympathetic and parasympathetic activity
     */
    @ColumnInfo(name = "hrvLfHfRatio")
    val hrvLfHfRatio: Float? = null,

    /**
     * Context of the heart rate measurement (rest, sleep, exercise, etc.)
     */
    @ColumnInfo(name = "context")
    val context: String,

    /**
     * Specific activity type if measured during exercise (running, cycling, etc.)
     */
    @ColumnInfo(name = "activityType")
    val activityType: String? = null,

    /**
     * Measurement duration in seconds
     */
    @ColumnInfo(name = "measurementDuration")
    val measurementDuration: Int? = null,

    /**
     * Signal quality indicator (0.0 to 1.0)
     */
    @ColumnInfo(name = "signalQuality")
    val signalQuality: Float,

    /**
     * Confidence level of the measurement (0.0 to 1.0)
     */
    @ColumnInfo(name = "confidenceLevel")
    val confidenceLevel: Float,

    /**
     * Percentage of time spent in fat burn zone
     */
    @ColumnInfo(name = "fatBurnZonePercentage")
    val fatBurnZonePercentage: Float? = null,

    /**
     * Percentage of time spent in cardio zone
     */
    @ColumnInfo(name = "cardioZonePercentage")
    val cardioZonePercentage: Float? = null,

    /**
     * Percentage of time spent in peak zone
     */
    @ColumnInfo(name = "peakZonePercentage")
    val peakZonePercentage: Float? = null,

    /**
     * Estimated calories burned based on heart rate
     */
    @ColumnInfo(name = "caloriesBurned")
    val caloriesBurned: Float? = null,

    /**
     * User's maximum heart rate used for zone calculations
     */
    @ColumnInfo(name = "maxHeartRate")
    val maxHeartRate: Int? = null,

    /**
     * Notes or tags related to this heart rate measurement
     */
    @ColumnInfo(name = "notes")
    val notes: String? = null,

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
    fun toDomainModel(): HeartRate {
        return HeartRate(
            id = id,
            healthDataId = healthDataId,
            timestamp = timestamp,
            userId = userId,
            deviceId = deviceId,
            bpm = bpm,
            isRestingHeartRate = isRestingHeartRate,
            heartRateZone = HeartRateZone.valueOf(heartRateZone),
            minBpm = minBpm,
            maxBpm = maxBpm,
            avgBpm = avgBpm,
            hrvRmssd = hrvRmssd,
            hrvSdnn = hrvSdnn,
            hrvLf = hrvLf,
            hrvHf = hrvHf,
            hrvLfHfRatio = hrvLfHfRatio,
            context = HeartRateContext.valueOf(context),
            activityType = activityType,
            measurementDuration = measurementDuration,
            signalQuality = signalQuality,
            confidenceLevel = confidenceLevel,
            fatBurnZonePercentage = fatBurnZonePercentage,
            cardioZonePercentage = cardioZonePercentage,
            peakZonePercentage = peakZonePercentage,
            caloriesBurned = caloriesBurned,
            maxHeartRate = maxHeartRate,
            notes = notes,
            createdAt = createdAt,
            modifiedAt = modifiedAt
        )
    }

    companion object {
        /**
         * Convert domain model to entity
         */
        fun fromDomainModel(domainModel: HeartRate): HeartRateEntity {
            return HeartRateEntity(
                id = domainModel.id,
                healthDataId = domainModel.healthDataId,
                timestamp = domainModel.timestamp,
                userId = domainModel.userId,
                deviceId = domainModel.deviceId,
                bpm = domainModel.bpm,
                isRestingHeartRate = domainModel.isRestingHeartRate,
                heartRateZone = domainModel.heartRateZone.name,
                minBpm = domainModel.minBpm,
                maxBpm = domainModel.maxBpm,
                avgBpm = domainModel.avgBpm,
                hrvRmssd = domainModel.hrvRmssd,
                hrvSdnn = domainModel.hrvSdnn,
                hrvLf = domainModel.hrvLf,
                hrvHf = domainModel.hrvHf,
                hrvLfHfRatio = domainModel.hrvLfHfRatio,
                context = domainModel.context.name,
                activityType = domainModel.activityType,
                measurementDuration = domainModel.measurementDuration,
                signalQuality = domainModel.signalQuality,
                confidenceLevel = domainModel.confidenceLevel,
                fatBurnZonePercentage = domainModel.fatBurnZonePercentage,
                cardioZonePercentage = domainModel.cardioZonePercentage,
                peakZonePercentage = domainModel.peakZonePercentage,
                caloriesBurned = domainModel.caloriesBurned,
                maxHeartRate = domainModel.maxHeartRate,
                notes = domainModel.notes,
                createdAt = domainModel.createdAt,
                modifiedAt = domainModel.modifiedAt
            )
        }

        /**
         * Validate heart rate value for physiological plausibility
         * @param bpm Heart rate in beats per minute
         * @return True if the heart rate is within realistic human range
         */
        fun isValidHeartRate(bpm: Int): Boolean {
            // Typical human heart rate range: 30-220 BPM
            // Below 30 could be bradycardia, above 220 is extremely rare
            return bpm in 30..220
        }

        /**
         * Calculate heart rate zone based on BPM and max heart rate
         * @param bpm Current heart rate in beats per minute
         * @param maxHeartRate User's maximum heart rate (typically 220 - age)
         * @return The appropriate heart rate zone
         */
        fun calculateHeartRateZone(bpm: Int, maxHeartRate: Int): HeartRateZone {
            val percentage = (bpm.toFloat() / maxHeartRate) * 100

            return when {
                percentage < 50 -> HeartRateZone.REST
                percentage < 70 -> HeartRateZone.FAT_BURN
                percentage < 85 -> HeartRateZone.CARDIO
                else -> HeartRateZone.PEAK
            }
        }

        /**
         * Calculate maximum heart rate based on age
         * @param age User's age in years
         * @return Estimated maximum heart rate
         */
        fun calculateMaxHeartRate(age: Int): Int {
            // Common formula: 220 - age
            return 220 - age
        }

        /**
         * Determine if a heart rate measurement represents resting heart rate
         * @param bpm Heart rate in beats per minute
         * @param context Measurement context
         * @return True if this is likely a resting heart rate
         */
        fun isRestingHeartRate(bpm: Int, context: HeartRateContext): Boolean {
            return (context == HeartRateContext.REST || context == HeartRateContext.SLEEP) && bpm < 100
        }
    }
}
