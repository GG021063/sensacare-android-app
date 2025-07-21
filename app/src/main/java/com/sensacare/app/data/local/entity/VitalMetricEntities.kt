package com.sensacare.app.data.local.entity

import androidx.room.*
import java.time.LocalDate
import java.time.LocalDateTime
import com.sensacare.app.domain.model.*

/**
 * Base entity interface for all vital metric entities
 */
interface VitalMetricEntity {
    val id: String
    val userId: String
    val timestamp: LocalDateTime
    val deviceId: String?
    val deviceType: String?
    val validationStatus: ValidationStatus
}

/**
 * Blood Oxygen Entity
 */
@Entity(
    tableName = "blood_oxygen",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("userId"),
        Index("timestamp"),
        Index("value")
    ]
)
data class BloodOxygenEntity(
    @PrimaryKey
    override val id: String,
    override val userId: String,
    override val timestamp: LocalDateTime,
    val value: Int, // Percentage (0-100)
    val confidence: Float?,
    val pulseRate: Int?,
    override val deviceId: String?,
    override val deviceType: String?,
    val measurementContextJson: String?, // JSON string of MeasurementContext
    override val validationStatus: ValidationStatus
) : VitalMetricEntity

/**
 * Body Temperature Entity
 */
@Entity(
    tableName = "body_temperature",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("userId"),
        Index("timestamp"),
        Index("value"),
        Index("measurementSite")
    ]
)
data class BodyTemperatureEntity(
    @PrimaryKey
    override val id: String,
    override val userId: String,
    override val timestamp: LocalDateTime,
    val value: Float, // Temperature in Celsius
    val measurementSite: TemperatureMeasurementSite,
    override val deviceId: String?,
    override val deviceType: String?,
    val measurementContextJson: String?, // JSON string of MeasurementContext
    override val validationStatus: ValidationStatus
) : VitalMetricEntity

/**
 * Stress Level Entity
 */
@Entity(
    tableName = "stress_level",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("userId"),
        Index("timestamp"),
        Index("value")
    ]
)
data class StressLevelEntity(
    @PrimaryKey
    override val id: String,
    override val userId: String,
    override val timestamp: LocalDateTime,
    val value: Int, // 0-100 scale
    val hrvValue: Double?,
    val confidenceScore: Float?,
    override val deviceId: String?,
    override val deviceType: String?,
    val measurementContextJson: String?, // JSON string of MeasurementContext
    override val validationStatus: ValidationStatus
) : VitalMetricEntity

/**
 * ECG Entity
 * Main table for ECG metadata
 */
@Entity(
    tableName = "ecg",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("userId"),
        Index("timestamp"),
        Index("classification")
    ]
)
data class EcgEntity(
    @PrimaryKey
    override val id: String,
    override val userId: String,
    override val timestamp: LocalDateTime,
    val samplingRate: Int, // Hz
    val leadType: EcgLeadType,
    val durationSeconds: Int, // Duration of recording in seconds
    val annotationsJson: String?, // JSON string of List<EcgAnnotation>
    val classification: EcgClassification?,
    val heartRate: Int?,
    val hrvDataJson: String?, // JSON string of HeartRateVariabilityData
    override val deviceId: String?,
    override val deviceType: String?,
    val measurementContextJson: String?, // JSON string of MeasurementContext
    override val validationStatus: ValidationStatus
) : VitalMetricEntity

/**
 * ECG Waveform Data Entity
 * Separate table for waveform data to improve performance
 */
@Entity(
    tableName = "ecg_waveform",
    foreignKeys = [
        ForeignKey(
            entity = EcgEntity::class,
            parentColumns = ["id"],
            childColumns = ["ecgId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("ecgId")
    ]
)
data class EcgWaveformEntity(
    @PrimaryKey
    val id: String,
    val ecgId: String,
    val waveformDataJson: String, // JSON string of List<Float>
    val chunkIndex: Int, // For large waveforms split into chunks
    val totalChunks: Int
)

/**
 * Blood Glucose Entity
 */
@Entity(
    tableName = "blood_glucose",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("userId"),
        Index("timestamp"),
        Index("value"),
        Index("measurementType")
    ]
)
data class BloodGlucoseEntity(
    @PrimaryKey
    override val id: String,
    override val userId: String,
    override val timestamp: LocalDateTime,
    val value: Float, // mg/dL
    val measurementType: GlucoseMeasurementType,
    val mealContextJson: String?, // JSON string of MealContext
    val medicationContextJson: String?, // JSON string of MedicationContext
    override val deviceId: String?,
    override val deviceType: String?,
    val measurementContextJson: String?, // JSON string of MeasurementContext
    override val validationStatus: ValidationStatus
) : VitalMetricEntity

/**
 * Device Capability Entity
 */
@Entity(
    tableName = "device_capability",
    indices = [
        Index("deviceName"),
        Index("deviceModel")
    ]
)
data class DeviceCapabilityEntity(
    @PrimaryKey
    val deviceId: String,
    val deviceName: String,
    val deviceModel: String,
    val supportedMetricsJson: String, // JSON string of Set<SupportedMetric>
    val samplingRatesJson: String?, // JSON string of Map<SupportedMetric, Int>
    val accuracyRatingsJson: String?, // JSON string of Map<SupportedMetric, Float>
    val batteryLevel: Int?,
    val firmwareVersion: String?,
    val lastSyncTimestamp: LocalDateTime
)

/**
 * User Entity
 */
@Entity(
    tableName = "user",
    indices = [
        Index("email", unique = true)
    ]
)
data class UserEntity(
    @PrimaryKey
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
)

/**
 * Type Converters for Room
 */
class VitalMetricTypeConverters {
    
    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime?): String? {
        return value?.toString()
    }
    
    @TypeConverter
    fun toLocalDateTime(value: String?): LocalDateTime? {
        return value?.let { LocalDateTime.parse(it) }
    }
    
    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? {
        return value?.toString()
    }
    
    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? {
        return value?.let { LocalDate.parse(it) }
    }
    
    @TypeConverter
    fun fromValidationStatus(value: ValidationStatus): String {
        return value.name
    }
    
    @TypeConverter
    fun toValidationStatus(value: String): ValidationStatus {
        return ValidationStatus.valueOf(value)
    }
    
    @TypeConverter
    fun fromTemperatureMeasurementSite(value: TemperatureMeasurementSite): String {
        return value.name
    }
    
    @TypeConverter
    fun toTemperatureMeasurementSite(value: String): TemperatureMeasurementSite {
        return TemperatureMeasurementSite.valueOf(value)
    }
    
    @TypeConverter
    fun fromEcgLeadType(value: EcgLeadType): String {
        return value.name
    }
    
    @TypeConverter
    fun toEcgLeadType(value: String): EcgLeadType {
        return EcgLeadType.valueOf(value)
    }
    
    @TypeConverter
    fun fromEcgClassification(value: EcgClassification?): String? {
        return value?.name
    }
    
    @TypeConverter
    fun toEcgClassification(value: String?): EcgClassification? {
        return value?.let { EcgClassification.valueOf(it) }
    }
    
    @TypeConverter
    fun fromGlucoseMeasurementType(value: GlucoseMeasurementType): String {
        return value.name
    }
    
    @TypeConverter
    fun toGlucoseMeasurementType(value: String): GlucoseMeasurementType {
        return GlucoseMeasurementType.valueOf(value)
    }
}
