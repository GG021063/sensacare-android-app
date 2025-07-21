package com.sensacare.app.data.local.entity

import androidx.room.*
import com.sensacare.app.domain.model.BloodPressure
import com.sensacare.app.domain.model.BloodPressureCategory
import com.sensacare.app.domain.model.MeasurementPosition
import com.sensacare.app.domain.model.MeasurementMethod
import com.sensacare.app.domain.model.CuffSize
import java.time.LocalDateTime
import java.util.UUID
import kotlin.math.abs

/**
 * BloodPressureEntity - Room database entity for blood pressure measurements
 *
 * This entity stores comprehensive blood pressure data collected from HBand devices, including:
 * - Systolic and diastolic pressure measurements
 * - Pulse pressure (systolic - diastolic)
 * - Blood pressure classification (normal, elevated, hypertension stages)
 * - Mean arterial pressure (MAP)
 * - Measurement context (position, arm, time of day)
 * - Device and measurement method details
 * - Quality indicators and confidence levels
 *
 * The entity maintains a foreign key relationship with the base HealthDataEntity
 * to enable unified queries across all health metrics while providing
 * blood pressure specific details.
 */
@Entity(
    tableName = "blood_pressure",
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
        Index("systolic"),
        Index("diastolic"),
        Index("category"),
        Index("position")
    ]
)
data class BloodPressureEntity(
    /**
     * Primary key - unique identifier for the blood pressure record
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
     * Timestamp when the blood pressure was measured
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
     * Systolic blood pressure in mmHg (the upper number)
     */
    @ColumnInfo(name = "systolic")
    val systolic: Int,

    /**
     * Diastolic blood pressure in mmHg (the lower number)
     */
    @ColumnInfo(name = "diastolic")
    val diastolic: Int,

    /**
     * Pulse pressure (systolic - diastolic) in mmHg
     */
    @ColumnInfo(name = "pulsePressure")
    val pulsePressure: Int = systolic - diastolic,

    /**
     * Mean arterial pressure (MAP) in mmHg
     * Formula: (2 * diastolic + systolic) / 3
     */
    @ColumnInfo(name = "meanArterialPressure")
    val meanArterialPressure: Int = ((2 * diastolic) + systolic) / 3,

    /**
     * Blood pressure category based on AHA/WHO guidelines
     */
    @ColumnInfo(name = "category")
    val category: String,

    /**
     * Heart rate (pulse) during measurement in BPM
     */
    @ColumnInfo(name = "heartRate")
    val heartRate: Int? = null,

    /**
     * Body position during measurement (sitting, standing, lying)
     */
    @ColumnInfo(name = "position")
    val position: String,

    /**
     * Which arm was used for measurement (left, right)
     */
    @ColumnInfo(name = "arm")
    val arm: String,

    /**
     * Cuff size used for measurement
     */
    @ColumnInfo(name = "cuffSize")
    val cuffSize: String? = null,

    /**
     * Measurement method (oscillometric, auscultatory, etc.)
     */
    @ColumnInfo(name = "measurementMethod")
    val measurementMethod: String? = null,

    /**
     * Time of day classification (morning, afternoon, evening, night)
     */
    @ColumnInfo(name = "timeOfDay")
    val timeOfDay: String? = null,

    /**
     * Flag indicating if this was taken before or after medication
     */
    @ColumnInfo(name = "afterMedication")
    val afterMedication: Boolean? = null,

    /**
     * Flag indicating if user reported symptoms during measurement
     */
    @ColumnInfo(name = "hasSymptoms")
    val hasSymptoms: Boolean = false,

    /**
     * Description of symptoms if present
     */
    @ColumnInfo(name = "symptoms")
    val symptoms: String? = null,

    /**
     * Number of readings averaged for this measurement
     */
    @ColumnInfo(name = "numberOfReadings")
    val numberOfReadings: Int = 1,

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
     * Standard deviation between readings if multiple readings were taken
     */
    @ColumnInfo(name = "standardDeviation")
    val standardDeviation: Float? = null,

    /**
     * Irregularity index - indicates potential arrhythmia during measurement
     */
    @ColumnInfo(name = "irregularityIndex")
    val irregularityIndex: Float? = null,

    /**
     * Notes or tags related to this blood pressure measurement
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
    fun toDomainModel(): BloodPressure {
        return BloodPressure(
            id = id,
            healthDataId = healthDataId,
            timestamp = timestamp,
            userId = userId,
            deviceId = deviceId,
            systolic = systolic,
            diastolic = diastolic,
            pulsePressure = pulsePressure,
            meanArterialPressure = meanArterialPressure,
            category = BloodPressureCategory.valueOf(category),
            heartRate = heartRate,
            position = MeasurementPosition.valueOf(position),
            arm = arm,
            cuffSize = cuffSize?.let { CuffSize.valueOf(it) },
            measurementMethod = measurementMethod?.let { MeasurementMethod.valueOf(it) },
            timeOfDay = timeOfDay,
            afterMedication = afterMedication,
            hasSymptoms = hasSymptoms,
            symptoms = symptoms,
            numberOfReadings = numberOfReadings,
            signalQuality = signalQuality,
            confidenceLevel = confidenceLevel,
            standardDeviation = standardDeviation,
            irregularityIndex = irregularityIndex,
            notes = notes,
            createdAt = createdAt,
            modifiedAt = modifiedAt
        )
    }

    companion object {
        /**
         * Convert domain model to entity
         */
        fun fromDomainModel(domainModel: BloodPressure): BloodPressureEntity {
            return BloodPressureEntity(
                id = domainModel.id,
                healthDataId = domainModel.healthDataId,
                timestamp = domainModel.timestamp,
                userId = domainModel.userId,
                deviceId = domainModel.deviceId,
                systolic = domainModel.systolic,
                diastolic = domainModel.diastolic,
                pulsePressure = domainModel.pulsePressure,
                meanArterialPressure = domainModel.meanArterialPressure,
                category = domainModel.category.name,
                heartRate = domainModel.heartRate,
                position = domainModel.position.name,
                arm = domainModel.arm,
                cuffSize = domainModel.cuffSize?.name,
                measurementMethod = domainModel.measurementMethod?.name,
                timeOfDay = domainModel.timeOfDay,
                afterMedication = domainModel.afterMedication,
                hasSymptoms = domainModel.hasSymptoms,
                symptoms = domainModel.symptoms,
                numberOfReadings = domainModel.numberOfReadings,
                signalQuality = domainModel.signalQuality,
                confidenceLevel = domainModel.confidenceLevel,
                standardDeviation = domainModel.standardDeviation,
                irregularityIndex = domainModel.irregularityIndex,
                notes = domainModel.notes,
                createdAt = domainModel.createdAt,
                modifiedAt = domainModel.modifiedAt
            )
        }

        /**
         * Validate blood pressure values for physiological plausibility
         * @param systolic Systolic pressure in mmHg
         * @param diastolic Diastolic pressure in mmHg
         * @return True if the blood pressure values are within realistic human range
         */
        fun isValidBloodPressure(systolic: Int, diastolic: Int): Boolean {
            // Typical human blood pressure ranges
            // Systolic: 70-220 mmHg
            // Diastolic: 40-130 mmHg
            // Systolic should be higher than diastolic
            return systolic in 70..220 && 
                   diastolic in 40..130 && 
                   systolic > diastolic
        }

        /**
         * Calculate blood pressure category based on AHA guidelines
         * @param systolic Systolic pressure in mmHg
         * @param diastolic Diastolic pressure in mmHg
         * @return The appropriate blood pressure category
         */
        fun calculateCategory(systolic: Int, diastolic: Int): BloodPressureCategory {
            return when {
                // Hypertensive crisis
                systolic > 180 || diastolic > 120 -> BloodPressureCategory.HYPERTENSIVE_CRISIS
                
                // Stage 2 hypertension
                systolic >= 140 || diastolic >= 90 -> BloodPressureCategory.HYPERTENSION_STAGE_2
                
                // Stage 1 hypertension
                systolic >= 130 || diastolic >= 80 -> BloodPressureCategory.HYPERTENSION_STAGE_1
                
                // Elevated
                systolic >= 120 && diastolic < 80 -> BloodPressureCategory.ELEVATED
                
                // Normal
                systolic >= 90 && diastolic >= 60 -> BloodPressureCategory.NORMAL
                
                // Low
                else -> BloodPressureCategory.LOW
            }
        }

        /**
         * Calculate mean arterial pressure (MAP)
         * @param systolic Systolic pressure in mmHg
         * @param diastolic Diastolic pressure in mmHg
         * @return Mean arterial pressure in mmHg
         */
        fun calculateMAP(systolic: Int, diastolic: Int): Int {
            return ((2 * diastolic) + systolic) / 3
        }

        /**
         * Calculate pulse pressure
         * @param systolic Systolic pressure in mmHg
         * @param diastolic Diastolic pressure in mmHg
         * @return Pulse pressure in mmHg
         */
        fun calculatePulsePressure(systolic: Int, diastolic: Int): Int {
            return systolic - diastolic
        }

        /**
         * Determine if pulse pressure is abnormal
         * Normal pulse pressure is typically between 40-60 mmHg
         * @param pulsePressure Pulse pressure in mmHg
         * @return True if pulse pressure is outside normal range
         */
        fun isAbnormalPulsePressure(pulsePressure: Int): Boolean {
            return pulsePressure < 40 || pulsePressure > 60
        }

        /**
         * Calculate blood pressure variability between multiple readings
         * @param readings List of systolic and diastolic pairs
         * @return Standard deviation of readings
         */
        fun calculateVariability(readings: List<Pair<Int, Int>>): Float {
            if (readings.size <= 1) return 0f
            
            // Calculate average systolic and diastolic
            val avgSystolic = readings.map { it.first }.average()
            val avgDiastolic = readings.map { it.second }.average()
            
            // Calculate sum of squared differences
            val systolicVariance = readings.map { (it.first - avgSystolic).pow(2) }.average()
            val diastolicVariance = readings.map { (it.second - avgDiastolic).pow(2) }.average()
            
            // Return combined standard deviation
            return Math.sqrt((systolicVariance + diastolicVariance) / 2).toFloat()
        }

        /**
         * Determine recommended cuff size based on arm circumference
         * @param armCircumference Arm circumference in centimeters
         * @return Recommended cuff size
         */
        fun recommendCuffSize(armCircumference: Float): CuffSize {
            return when {
                armCircumference < 22 -> CuffSize.SMALL
                armCircumference < 32 -> CuffSize.REGULAR
                armCircumference < 42 -> CuffSize.LARGE
                else -> CuffSize.EXTRA_LARGE
            }
        }

        /**
         * Helper extension function for squaring a number
         */
        private fun Double.pow(exponent: Int): Double = Math.pow(this, exponent.toDouble())
    }
}
