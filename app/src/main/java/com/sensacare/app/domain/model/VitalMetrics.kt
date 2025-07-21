package com.sensacare.app.domain.model

import java.time.LocalDateTime
import kotlin.math.abs

/**
 * Base interface for all vital metrics
 * Provides common properties and methods for all vital measurements
 */
interface VitalMetric {
    val id: String
    val userId: String
    val timestamp: LocalDateTime
    val deviceId: String?
    val deviceType: String?
    val measurementContext: MeasurementContext?
    val validationStatus: ValidationStatus
    
    fun isValid(): Boolean = validationStatus == ValidationStatus.VALID
}

/**
 * Measurement context provides additional information about when and how
 * the vital was measured
 */
data class MeasurementContext(
    val activityState: ActivityState = ActivityState.UNKNOWN,
    val bodyPosition: BodyPosition = BodyPosition.UNKNOWN,
    val location: String? = null,
    val notes: String? = null,
    val tags: List<String> = emptyList(),
    val manuallyEntered: Boolean = false
)

/**
 * Activity state during measurement
 */
enum class ActivityState {
    RESTING,
    ACTIVE,
    SLEEPING,
    POST_EXERCISE,
    UNKNOWN
}

/**
 * Body position during measurement
 */
enum class BodyPosition {
    SITTING,
    STANDING,
    LYING,
    UNKNOWN
}

/**
 * Validation status for vital measurements
 */
enum class ValidationStatus {
    VALID,
    INVALID_RANGE,
    INVALID_DEVICE_ERROR,
    INVALID_MEASUREMENT_ERROR,
    INVALID_USER_CANCELLED,
    UNKNOWN
}

/**
 * SPO2 (Blood Oxygen) measurement
 */
data class BloodOxygen(
    override val id: String,
    override val userId: String,
    override val timestamp: LocalDateTime,
    val value: Int, // Percentage (0-100)
    val confidence: Float? = null, // Confidence level (0.0-1.0)
    val pulseRate: Int? = null, // Associated pulse rate during measurement
    override val deviceId: String? = null,
    override val deviceType: String? = null,
    override val measurementContext: MeasurementContext? = null,
    override val validationStatus: ValidationStatus = validateBloodOxygen(value)
) : VitalMetric {
    
    val classification: BloodOxygenClassification
        get() = classifyBloodOxygen(value)
    
    companion object {
        const val MIN_VALID_VALUE = 70
        const val MAX_VALID_VALUE = 100
        const val NORMAL_THRESHOLD = 95
        const val MILD_HYPOXEMIA_THRESHOLD = 90
        const val MODERATE_HYPOXEMIA_THRESHOLD = 85
        const val SEVERE_HYPOXEMIA_THRESHOLD = 80
    }
}

/**
 * Blood Oxygen Classification
 */
enum class BloodOxygenClassification {
    NORMAL,
    MILD_HYPOXEMIA,
    MODERATE_HYPOXEMIA,
    SEVERE_HYPOXEMIA,
    CRITICAL_HYPOXEMIA
}

/**
 * Validate blood oxygen value
 */
fun validateBloodOxygen(value: Int): ValidationStatus {
    return when {
        value in BloodOxygen.MIN_VALID_VALUE..BloodOxygen.MAX_VALID_VALUE -> ValidationStatus.VALID
        else -> ValidationStatus.INVALID_RANGE
    }
}

/**
 * Classify blood oxygen level
 */
fun classifyBloodOxygen(value: Int): BloodOxygenClassification {
    return when {
        value >= BloodOxygen.NORMAL_THRESHOLD -> BloodOxygenClassification.NORMAL
        value >= BloodOxygen.MILD_HYPOXEMIA_THRESHOLD -> BloodOxygenClassification.MILD_HYPOXEMIA
        value >= BloodOxygen.MODERATE_HYPOXEMIA_THRESHOLD -> BloodOxygenClassification.MODERATE_HYPOXEMIA
        value >= BloodOxygen.SEVERE_HYPOXEMIA_THRESHOLD -> BloodOxygenClassification.SEVERE_HYPOXEMIA
        else -> BloodOxygenClassification.CRITICAL_HYPOXEMIA
    }
}

/**
 * Blood Oxygen Stats
 */
data class BloodOxygenStats(
    val avgValue: Int,
    val minValue: Int,
    val maxValue: Int,
    val stdDeviation: Double,
    val timeUnderThreshold: Int, // Minutes under normal threshold
    val readingsCount: Int,
    val timeRange: Pair<LocalDateTime, LocalDateTime>
) {
    val classification: BloodOxygenClassification
        get() = classifyBloodOxygen(avgValue)
    
    val stabilityScore: Int
        get() = calculateStabilityScore()
    
    private fun calculateStabilityScore(): Int {
        // Higher score means more stable
        val rangeScore = when (maxValue - minValue) {
            in 0..2 -> 10
            in 3..5 -> 8
            in 6..8 -> 6
            in 9..12 -> 4
            else -> 2
        }
        
        val stdDevScore = when (stdDeviation) {
            in 0.0..1.0 -> 10
            in 1.1..2.0 -> 8
            in 2.1..3.0 -> 6
            in 3.1..4.0 -> 4
            else -> 2
        }
        
        val thresholdScore = when (timeUnderThreshold) {
            0 -> 10
            in 1..5 -> 8
            in 6..15 -> 6
            in 16..30 -> 4
            else -> 2
        }
        
        return ((rangeScore * 0.4) + (stdDevScore * 0.4) + (thresholdScore * 0.2)).toInt()
    }
}

/**
 * Body Temperature measurement
 */
data class BodyTemperature(
    override val id: String,
    override val userId: String,
    override val timestamp: LocalDateTime,
    val value: Float, // Temperature in Celsius
    val measurementSite: TemperatureMeasurementSite = TemperatureMeasurementSite.UNKNOWN,
    override val deviceId: String? = null,
    override val deviceType: String? = null,
    override val measurementContext: MeasurementContext? = null,
    override val validationStatus: ValidationStatus = validateTemperature(value)
) : VitalMetric {
    
    val valueInFahrenheit: Float
        get() = (value * 9/5) + 32
    
    val classification: TemperatureClassification
        get() = classifyTemperature(value, measurementSite)
    
    companion object {
        const val MIN_VALID_VALUE_CELSIUS = 30.0f
        const val MAX_VALID_VALUE_CELSIUS = 45.0f
        
        // Normal ranges vary by measurement site
        val NORMAL_RANGE_ORAL = 36.1f..37.2f
        val NORMAL_RANGE_ARMPIT = 35.9f..36.7f
        val NORMAL_RANGE_EAR = 35.8f..37.5f
        val NORMAL_RANGE_FOREHEAD = 35.8f..37.3f
        val NORMAL_RANGE_WRIST = 35.4f..37.0f
    }
}

/**
 * Temperature Measurement Site
 */
enum class TemperatureMeasurementSite {
    ORAL,
    ARMPIT,
    EAR,
    FOREHEAD,
    WRIST,
    UNKNOWN
}

/**
 * Temperature Classification
 */
enum class TemperatureClassification {
    HYPOTHERMIA,
    NORMAL,
    MILD_FEVER,
    MODERATE_FEVER,
    HIGH_FEVER,
    HYPERPYREXIA
}

/**
 * Validate temperature value
 */
fun validateTemperature(value: Float): ValidationStatus {
    return when {
        value in BodyTemperature.MIN_VALID_VALUE_CELSIUS..BodyTemperature.MAX_VALID_VALUE_CELSIUS -> ValidationStatus.VALID
        else -> ValidationStatus.INVALID_RANGE
    }
}

/**
 * Classify temperature based on value and measurement site
 */
fun classifyTemperature(value: Float, site: TemperatureMeasurementSite): TemperatureClassification {
    // Get normal range based on measurement site
    val normalRange = when (site) {
        TemperatureMeasurementSite.ORAL -> BodyTemperature.NORMAL_RANGE_ORAL
        TemperatureMeasurementSite.ARMPIT -> BodyTemperature.NORMAL_RANGE_ARMPIT
        TemperatureMeasurementSite.EAR -> BodyTemperature.NORMAL_RANGE_EAR
        TemperatureMeasurementSite.FOREHEAD -> BodyTemperature.NORMAL_RANGE_FOREHEAD
        TemperatureMeasurementSite.WRIST -> BodyTemperature.NORMAL_RANGE_WRIST
        TemperatureMeasurementSite.UNKNOWN -> BodyTemperature.NORMAL_RANGE_ORAL // Default to oral
    }
    
    // Classify based on value
    return when {
        value < 35.0f -> TemperatureClassification.HYPOTHERMIA
        value in normalRange -> TemperatureClassification.NORMAL
        value < 38.0f -> TemperatureClassification.MILD_FEVER
        value < 39.0f -> TemperatureClassification.MODERATE_FEVER
        value < 40.0f -> TemperatureClassification.HIGH_FEVER
        else -> TemperatureClassification.HYPERPYREXIA
    }
}

/**
 * Temperature Stats
 */
data class TemperatureStats(
    val avgValue: Float,
    val minValue: Float,
    val maxValue: Float,
    val stdDeviation: Double,
    val readingsCount: Int,
    val timeRange: Pair<LocalDateTime, LocalDateTime>,
    val measurementSite: TemperatureMeasurementSite = TemperatureMeasurementSite.UNKNOWN
) {
    val classification: TemperatureClassification
        get() = classifyTemperature(avgValue, measurementSite)
    
    val variabilityScore: Int
        get() = calculateVariabilityScore()
    
    private fun calculateVariabilityScore(): Int {
        // Lower score means less variability (which is good for temperature)
        val rangeScore = when ((maxValue - minValue) * 10) {
            in 0.0f..2.0f -> 10
            in 2.1f..5.0f -> 8
            in 5.1f..10.0f -> 6
            in 10.1f..15.0f -> 4
            else -> 2
        }
        
        val stdDevScore = when (stdDeviation) {
            in 0.0..0.2 -> 10
            in 0.21..0.5 -> 8
            in 0.51..0.8 -> 6
            in 0.81..1.2 -> 4
            else -> 2
        }
        
        return ((rangeScore * 0.6) + (stdDevScore * 0.4)).toInt()
    }
}

/**
 * Stress Level measurement
 */
data class StressLevel(
    override val id: String,
    override val userId: String,
    override val timestamp: LocalDateTime,
    val value: Int, // 0-100 scale
    val hrvValue: Double? = null, // Associated HRV value
    val confidenceScore: Float? = null, // 0.0-1.0
    override val deviceId: String? = null,
    override val deviceType: String? = null,
    override val measurementContext: MeasurementContext? = null,
    override val validationStatus: ValidationStatus = validateStressLevel(value)
) : VitalMetric {
    
    val classification: StressClassification
        get() = classifyStress(value)
    
    companion object {
        const val MIN_VALID_VALUE = 0
        const val MAX_VALID_VALUE = 100
        
        const val LOW_STRESS_THRESHOLD = 30
        const val MODERATE_STRESS_THRESHOLD = 50
        const val HIGH_STRESS_THRESHOLD = 70
        const val VERY_HIGH_STRESS_THRESHOLD = 85
    }
}

/**
 * Stress Classification
 */
enum class StressClassification {
    LOW,
    MODERATE,
    HIGH,
    VERY_HIGH,
    EXTREME
}

/**
 * Validate stress level value
 */
fun validateStressLevel(value: Int): ValidationStatus {
    return when {
        value in StressLevel.MIN_VALID_VALUE..StressLevel.MAX_VALID_VALUE -> ValidationStatus.VALID
        else -> ValidationStatus.INVALID_RANGE
    }
}

/**
 * Classify stress level
 */
fun classifyStress(value: Int): StressClassification {
    return when {
        value < StressLevel.LOW_STRESS_THRESHOLD -> StressClassification.LOW
        value < StressLevel.MODERATE_STRESS_THRESHOLD -> StressClassification.MODERATE
        value < StressLevel.HIGH_STRESS_THRESHOLD -> StressClassification.HIGH
        value < StressLevel.VERY_HIGH_STRESS_THRESHOLD -> StressClassification.VERY_HIGH
        else -> StressClassification.EXTREME
    }
}

/**
 * Stress Level Stats
 */
data class StressStats(
    val avgValue: Int,
    val minValue: Int,
    val maxValue: Int,
    val stdDeviation: Double,
    val timeInHighStress: Int, // Minutes in high stress
    val readingsCount: Int,
    val timeRange: Pair<LocalDateTime, LocalDateTime>
) {
    val classification: StressClassification
        get() = classifyStress(avgValue)
    
    val stressLoadScore: Int
        get() = calculateStressLoadScore()
    
    private fun calculateStressLoadScore(): Int {
        // Higher score means higher stress load (worse)
        val avgScore = when (avgValue) {
            in 0..20 -> 1
            in 21..40 -> 2
            in 41..60 -> 3
            in 61..80 -> 4
            else -> 5
        }
        
        val peakScore = when (maxValue) {
            in 0..30 -> 1
            in 31..50 -> 2
            in 51..70 -> 3
            in 71..90 -> 4
            else -> 5
        }
        
        val durationScore = when (timeInHighStress) {
            0 -> 0
            in 1..15 -> 1
            in 16..30 -> 2
            in 31..60 -> 3
            in 61..120 -> 4
            else -> 5
        }
        
        return avgScore + peakScore + durationScore
    }
}

/**
 * ECG (Electrocardiogram) measurement
 */
data class Ecg(
    override val id: String,
    override val userId: String,
    override val timestamp: LocalDateTime,
    val waveformData: List<Float>, // Raw ECG waveform data
    val samplingRate: Int, // Hz
    val leadType: EcgLeadType = EcgLeadType.SINGLE_LEAD,
    val durationSeconds: Int, // Duration of recording in seconds
    val annotations: List<EcgAnnotation> = emptyList(),
    val classification: EcgClassification? = null,
    val heartRate: Int? = null, // Derived heart rate from ECG
    val hrvData: HeartRateVariabilityData? = null, // Derived HRV data
    override val deviceId: String? = null,
    override val deviceType: String? = null,
    override val measurementContext: MeasurementContext? = null,
    override val validationStatus: ValidationStatus = validateEcg(waveformData, samplingRate)
) : VitalMetric {
    
    val dataQualityScore: Float
        get() = calculateDataQualityScore()
    
    private fun calculateDataQualityScore(): Float {
        // Simple quality score based on signal characteristics
        // In a real implementation, this would use more sophisticated signal processing
        if (waveformData.isEmpty()) return 0f
        
        val mean = waveformData.average()
        val variance = waveformData.map { (it - mean) * (it - mean) }.average()
        
        // Check for flatlines or excessive noise
        val hasFlatlines = waveformData.windowed(samplingRate / 2).any { window ->
            window.distinct().size < 3
        }
        
        val hasExcessiveNoise = variance > 1000
        
        return when {
            hasFlatlines -> 0.2f
            hasExcessiveNoise -> 0.4f
            variance < 10 -> 0.6f
            else -> 0.9f
        }
    }
    
    companion object {
        const val MIN_SAMPLING_RATE = 100 // Minimum acceptable sampling rate in Hz
        const val MIN_DURATION = 5 // Minimum duration in seconds
    }
}

/**
 * ECG Lead Type
 */
enum class EcgLeadType {
    SINGLE_LEAD,
    THREE_LEAD,
    FIVE_LEAD,
    TWELVE_LEAD
}

/**
 * ECG Classification
 */
enum class EcgClassification {
    NORMAL_SINUS_RHYTHM,
    ATRIAL_FIBRILLATION,
    BRADYCARDIA,
    TACHYCARDIA,
    PREMATURE_VENTRICULAR_CONTRACTION,
    PREMATURE_ATRIAL_CONTRACTION,
    VENTRICULAR_TACHYCARDIA,
    HEART_BLOCK,
    ST_ELEVATION,
    ST_DEPRESSION,
    INCONCLUSIVE,
    POOR_SIGNAL_QUALITY
}

/**
 * ECG Annotation
 */
data class EcgAnnotation(
    val timeOffsetMs: Int, // Offset from start of recording in milliseconds
    val annotationType: EcgAnnotationType,
    val description: String? = null,
    val confidence: Float? = null // 0.0-1.0
)

/**
 * ECG Annotation Type
 */
enum class EcgAnnotationType {
    R_PEAK,
    P_WAVE,
    QRS_COMPLEX,
    T_WAVE,
    PREMATURE_BEAT,
    ARTIFACT,
    PAUSE,
    USER_MARKED,
    OTHER
}

/**
 * Heart Rate Variability Data
 */
data class HeartRateVariabilityData(
    val sdnn: Float, // Standard deviation of NN intervals (ms)
    val rmssd: Float, // Root mean square of successive differences (ms)
    val pnn50: Float, // Percentage of NN intervals differing by >50ms
    val rrIntervals: List<Int>, // List of RR intervals in ms
    val frequencyDomain: FrequencyDomainData? = null
)

/**
 * Frequency Domain Data for HRV
 */
data class FrequencyDomainData(
    val vlf: Float, // Very low frequency power (ms²)
    val lf: Float, // Low frequency power (ms²)
    val hf: Float, // High frequency power (ms²)
    val lfHfRatio: Float // LF/HF ratio
)

/**
 * Validate ECG data
 */
fun validateEcg(waveformData: List<Float>, samplingRate: Int): ValidationStatus {
    return when {
        waveformData.isEmpty() -> ValidationStatus.INVALID_RANGE
        samplingRate < Ecg.MIN_SAMPLING_RATE -> ValidationStatus.INVALID_MEASUREMENT_ERROR
        else -> ValidationStatus.VALID
    }
}

/**
 * Blood Glucose measurement
 */
data class BloodGlucose(
    override val id: String,
    override val userId: String,
    override val timestamp: LocalDateTime,
    val value: Float, // mg/dL
    val measurementType: GlucoseMeasurementType = GlucoseMeasurementType.UNKNOWN,
    val mealContext: MealContext? = null,
    val medicationContext: MedicationContext? = null,
    override val deviceId: String? = null,
    override val deviceType: String? = null,
    override val measurementContext: MeasurementContext? = null,
    override val validationStatus: ValidationStatus = validateBloodGlucose(value)
) : VitalMetric {
    
    val valueInMmolL: Float
        get() = value / 18.0f
    
    val classification: GlucoseClassification
        get() = classifyGlucose(value, measurementType)
    
    companion object {
        const val MIN_VALID_VALUE_MGDL = 20.0f
        const val MAX_VALID_VALUE_MGDL = 600.0f
        
        // Normal ranges vary by measurement type
        val NORMAL_RANGE_FASTING = 70.0f..99.0f
        val NORMAL_RANGE_PREPRANDIAL = 70.0f..110.0f
        val NORMAL_RANGE_POSTPRANDIAL = 70.0f..140.0f
    }
}

/**
 * Glucose Measurement Type
 */
enum class GlucoseMeasurementType {
    FASTING,
    PREPRANDIAL, // Before meal
    POSTPRANDIAL, // After meal
    BEDTIME,
    RANDOM,
    CONTINUOUS_MONITORING,
    UNKNOWN
}

/**
 * Glucose Classification
 */
enum class GlucoseClassification {
    HYPOGLYCEMIA,
    NORMAL,
    PREDIABETES,
    DIABETES,
    HYPERGLYCEMIA
}

/**
 * Meal Context
 */
data class MealContext(
    val mealType: MealType,
    val timeRelativeToMeal: Int? = null, // Minutes before/after meal (negative = before)
    val carbIntake: Float? = null // Carbohydrates in grams
)

/**
 * Meal Type
 */
enum class MealType {
    BREAKFAST,
    LUNCH,
    DINNER,
    SNACK
}

/**
 * Medication Context
 */
data class MedicationContext(
    val medicationName: String,
    val dosage: Float,
    val unit: String,
    val timeRelativeToMedication: Int? = null // Minutes before/after medication (negative = before)
)

/**
 * Validate blood glucose value
 */
fun validateBloodGlucose(value: Float): ValidationStatus {
    return when {
        value in BloodGlucose.MIN_VALID_VALUE_MGDL..BloodGlucose.MAX_VALID_VALUE_MGDL -> ValidationStatus.VALID
        else -> ValidationStatus.INVALID_RANGE
    }
}

/**
 * Classify blood glucose level based on value and measurement type
 */
fun classifyGlucose(value: Float, type: GlucoseMeasurementType): GlucoseClassification {
    // Get normal range based on measurement type
    val normalRange = when (type) {
        GlucoseMeasurementType.FASTING -> BloodGlucose.NORMAL_RANGE_FASTING
        GlucoseMeasurementType.PREPRANDIAL -> BloodGlucose.NORMAL_RANGE_PREPRANDIAL
        GlucoseMeasurementType.POSTPRANDIAL -> BloodGlucose.NORMAL_RANGE_POSTPRANDIAL
        else -> BloodGlucose.NORMAL_RANGE_FASTING // Default to fasting
    }
    
    // Classify based on value
    return when {
        value < 70.0f -> GlucoseClassification.HYPOGLYCEMIA
        value in normalRange -> GlucoseClassification.NORMAL
        value < 126.0f -> GlucoseClassification.PREDIABETES
        value < 200.0f -> GlucoseClassification.DIABETES
        else -> GlucoseClassification.HYPERGLYCEMIA
    }
}

/**
 * Blood Glucose Stats
 */
data class GlucoseStats(
    val avgValue: Float,
    val minValue: Float,
    val maxValue: Float,
    val stdDeviation: Double,
    val timeInRange: Int, // Minutes in target range
    val timeAboveRange: Int, // Minutes above target range
    val timeBelowRange: Int, // Minutes below target range
    val estimatedA1c: Float? = null, // Estimated HbA1c based on average glucose
    val readingsCount: Int,
    val timeRange: Pair<LocalDateTime, LocalDateTime>
) {
    val glucoseManagementIndicator: Float
        get() = calculateGMI()
    
    val glycemicVariabilityIndex: Int
        get() = calculateGVI()
    
    private fun calculateGMI(): Float {
        // Estimated HbA1c based on average glucose
        // Formula: (average glucose + 46.7) / 28.7
        return (avgValue + 46.7f) / 28.7f
    }
    
    private fun calculateGVI(): Int {
        // Glycemic Variability Index (0-100)
        // Higher score means more variability (worse)
        
        val rangeScore = when (maxValue - minValue) {
            in 0.0f..30.0f -> 10
            in 30.1f..60.0f -> 20
            in 60.1f..90.0f -> 30
            in 90.1f..120.0f -> 40
            else -> 50
        }
        
        val stdDevScore = when (stdDeviation) {
            in 0.0..10.0 -> 10
            in 10.1..20.0 -> 20
            in 20.1..30.0 -> 30
            in 30.1..40.0 -> 40
            else -> 50
        }
        
        return rangeScore + stdDevScore
    }
}

/**
 * Device Capability represents what vital metrics a specific device can measure
 */
data class DeviceCapability(
    val deviceId: String,
    val deviceName: String,
    val deviceModel: String,
    val supportedMetrics: Set<SupportedMetric>,
    val samplingRates: Map<SupportedMetric, Int> = emptyMap(), // Hz
    val accuracyRatings: Map<SupportedMetric, Float> = emptyMap(), // 0.0-1.0
    val batteryLevel: Int? = null, // Percentage
    val firmwareVersion: String? = null
)

/**
 * Supported Metric types
 */
enum class SupportedMetric {
    HEART_RATE,
    BLOOD_PRESSURE,
    BLOOD_OXYGEN,
    BODY_TEMPERATURE,
    STRESS_LEVEL,
    ECG,
    BLOOD_GLUCOSE,
    SLEEP,
    ACTIVITY,
    RESPIRATORY_RATE,
    WEIGHT
}

/**
 * Metric Type for filtering and UI
 */
enum class MetricType {
    HEART_RATE,
    BLOOD_PRESSURE,
    BLOOD_OXYGEN,
    TEMPERATURE,
    STRESS,
    ECG,
    BLOOD_GLUCOSE,
    SLEEP,
    ACTIVITY,
    WEIGHT,
    OTHER
}

/**
 * Helper extension to check if a device supports a specific metric
 */
fun DeviceCapability.supportsMetric(metric: SupportedMetric): Boolean {
    return supportedMetrics.contains(metric)
}

/**
 * Helper extension to get available metrics for a device
 */
fun DeviceCapability.getAvailableMetrics(): List<MetricType> {
    val metricTypes = mutableListOf<MetricType>()
    
    if (supportsMetric(SupportedMetric.HEART_RATE)) metricTypes.add(MetricType.HEART_RATE)
    if (supportsMetric(SupportedMetric.BLOOD_PRESSURE)) metricTypes.add(MetricType.BLOOD_PRESSURE)
    if (supportsMetric(SupportedMetric.BLOOD_OXYGEN)) metricTypes.add(MetricType.BLOOD_OXYGEN)
    if (supportsMetric(SupportedMetric.BODY_TEMPERATURE)) metricTypes.add(MetricType.TEMPERATURE)
    if (supportsMetric(SupportedMetric.STRESS_LEVEL)) metricTypes.add(MetricType.STRESS)
    if (supportsMetric(SupportedMetric.ECG)) metricTypes.add(MetricType.ECG)
    if (supportsMetric(SupportedMetric.BLOOD_GLUCOSE)) metricTypes.add(MetricType.BLOOD_GLUCOSE)
    if (supportsMetric(SupportedMetric.SLEEP)) metricTypes.add(MetricType.SLEEP)
    if (supportsMetric(SupportedMetric.ACTIVITY)) metricTypes.add(MetricType.ACTIVITY)
    if (supportsMetric(SupportedMetric.WEIGHT)) metricTypes.add(MetricType.WEIGHT)
    
    return metricTypes
}
