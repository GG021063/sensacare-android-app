package com.sensacare.app.data.device.mapper

import com.sensacare.app.domain.model.*
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
import kotlin.math.roundToInt

/**
 * Data classes representing raw data from the HBand SDK
 */
data class HeartRateData(
    val value: Int,
    val timestamp: Long,
    val confidence: Float? = null
)

data class BloodPressureData(
    val systolic: Int,
    val diastolic: Int,
    val pulse: Int?,
    val timestamp: Long
)

data class BloodOxygenData(
    val value: Int,
    val timestamp: Long,
    val confidence: Float? = null,
    val pulseRate: Int? = null
)

data class TemperatureData(
    val value: Float,
    val timestamp: Long,
    val site: TemperatureMeasurementSite = TemperatureMeasurementSite.WRIST
)

data class StressData(
    val value: Int,
    val timestamp: Long,
    val hrvValue: Double? = null,
    val confidenceScore: Float? = null
)

data class EcgData(
    val waveformData: List<Float>,
    val samplingRate: Int,
    val timestamp: Long,
    val durationSeconds: Int,
    val classification: EcgClassification? = null,
    val heartRate: Int? = null,
    val annotations: List<EcgAnnotationData>? = null,
    val hrvData: HrvData? = null
)

data class EcgAnnotationData(
    val timeOffsetMs: Int,
    val type: EcgAnnotationType,
    val description: String? = null,
    val confidence: Float? = null
)

data class HrvData(
    val sdnn: Float,
    val rmssd: Float,
    val pnn50: Float,
    val rrIntervals: List<Int>,
    val vlf: Float? = null,
    val lf: Float? = null,
    val hf: Float? = null,
    val lfHfRatio: Float? = null
)

data class GlucoseData(
    val value: Float,
    val timestamp: Long,
    val measurementType: GlucoseMeasurementType = GlucoseMeasurementType.UNKNOWN,
    val mealInfo: MealInfo? = null,
    val medicationInfo: MedicationInfo? = null
)

data class MealInfo(
    val mealType: MealType,
    val timeRelativeToMeal: Int? = null,
    val carbIntake: Float? = null
)

data class MedicationInfo(
    val medicationName: String,
    val dosage: Float,
    val unit: String,
    val timeRelativeToMedication: Int? = null
)

data class ActivityData(
    val type: ActivityType,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val durationSeconds: Int,
    val distanceMeters: Double? = null,
    val caloriesBurned: Double? = null,
    val averageHeartRate: Int? = null,
    val maxHeartRate: Int? = null,
    val steps: Int? = null,
    val intensity: Intensity? = null,
    val speedMetersPerSecond: Double? = null,
    val elevationGainMeters: Double? = null
)

data class SleepData(
    val startTimestamp: Long,
    val endTimestamp: Long,
    val durationMinutes: Int,
    val sleepStages: List<SleepStageData>,
    val sleepScore: Int? = null,
    val deepSleepMinutes: Int? = null,
    val remSleepMinutes: Int? = null,
    val lightSleepMinutes: Int? = null,
    val awakeMinutes: Int? = null,
    val sleepEfficiency: Double? = null,
    val sleepOnsetMinutes: Int? = null,
    val wakeAfterSleepOnsetMinutes: Int? = null
)

data class SleepStageData(
    val stage: SleepStage,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val durationMinutes: Int
)

data class DeviceInfo(
    val model: String,
    val manufacturer: String,
    val serialNumber: String
)

/**
 * Mapper class to convert HBand SDK data to domain models
 */
class HBandDataMapper {
    
    /**
     * Map HeartRateData to HeartRate domain model
     */
    fun mapToHeartRate(
        heartRateData: HeartRateData,
        userId: String,
        deviceId: String? = null,
        deviceType: String? = null
    ): HeartRate {
        val timestamp = convertTimestampToLocalDateTime(heartRateData.timestamp)
        
        // Create measurement context
        val context = MeasurementContext(
            activityState = determineActivityState(heartRateData.value),
            bodyPosition = BodyPosition.UNKNOWN,
            manuallyEntered = false
        )
        
        // Validate heart rate value
        val validationStatus = validateHeartRate(heartRateData.value)
        
        return HeartRate(
            id = generateId(),
            userId = userId,
            timestamp = timestamp,
            value = heartRateData.value,
            deviceId = deviceId,
            deviceType = deviceType,
            measurementContext = context,
            validationStatus = validationStatus
        )
    }
    
    /**
     * Map BloodPressureData to BloodPressure domain model
     */
    fun mapToBloodPressure(
        bloodPressureData: BloodPressureData,
        userId: String,
        deviceId: String? = null,
        deviceType: String? = null
    ): BloodPressure {
        val timestamp = convertTimestampToLocalDateTime(bloodPressureData.timestamp)
        
        // Create measurement context
        val context = MeasurementContext(
            activityState = ActivityState.RESTING, // Blood pressure typically measured at rest
            bodyPosition = BodyPosition.SITTING, // Typical position for BP measurement
            manuallyEntered = false
        )
        
        // Validate blood pressure values
        val validationStatus = validateBloodPressure(
            bloodPressureData.systolic,
            bloodPressureData.diastolic
        )
        
        return BloodPressure(
            id = generateId(),
            userId = userId,
            timestamp = timestamp,
            systolic = bloodPressureData.systolic,
            diastolic = bloodPressureData.diastolic,
            pulse = bloodPressureData.pulse,
            deviceId = deviceId,
            deviceType = deviceType,
            measurementContext = context,
            validationStatus = validationStatus
        )
    }
    
    /**
     * Map BloodOxygenData to BloodOxygen domain model
     */
    fun mapToBloodOxygen(
        bloodOxygenData: BloodOxygenData,
        userId: String,
        deviceId: String? = null,
        deviceType: String? = null
    ): BloodOxygen {
        val timestamp = convertTimestampToLocalDateTime(bloodOxygenData.timestamp)
        
        // Create measurement context
        val context = MeasurementContext(
            activityState = ActivityState.RESTING, // SpO2 typically measured at rest
            bodyPosition = BodyPosition.UNKNOWN,
            manuallyEntered = false
        )
        
        // Validate blood oxygen value
        val validationStatus = validateBloodOxygen(bloodOxygenData.value)
        
        return BloodOxygen(
            id = generateId(),
            userId = userId,
            timestamp = timestamp,
            value = bloodOxygenData.value,
            confidence = bloodOxygenData.confidence,
            pulseRate = bloodOxygenData.pulseRate,
            deviceId = deviceId,
            deviceType = deviceType,
            measurementContext = context,
            validationStatus = validationStatus
        )
    }
    
    /**
     * Map TemperatureData to BodyTemperature domain model
     */
    fun mapToBodyTemperature(
        temperatureData: TemperatureData,
        userId: String,
        deviceId: String? = null,
        deviceType: String? = null
    ): BodyTemperature {
        val timestamp = convertTimestampToLocalDateTime(temperatureData.timestamp)
        
        // Create measurement context
        val context = MeasurementContext(
            activityState = ActivityState.RESTING,
            bodyPosition = BodyPosition.UNKNOWN,
            manuallyEntered = false
        )
        
        // Validate temperature value
        val validationStatus = validateTemperature(temperatureData.value)
        
        return BodyTemperature(
            id = generateId(),
            userId = userId,
            timestamp = timestamp,
            value = temperatureData.value,
            measurementSite = temperatureData.site,
            deviceId = deviceId,
            deviceType = deviceType,
            measurementContext = context,
            validationStatus = validationStatus
        )
    }
    
    /**
     * Map StressData to StressLevel domain model
     */
    fun mapToStressLevel(
        stressData: StressData,
        userId: String,
        deviceId: String? = null,
        deviceType: String? = null
    ): StressLevel {
        val timestamp = convertTimestampToLocalDateTime(stressData.timestamp)
        
        // Create measurement context
        val context = MeasurementContext(
            activityState = ActivityState.UNKNOWN,
            bodyPosition = BodyPosition.UNKNOWN,
            manuallyEntered = false
        )
        
        // Validate stress level value
        val validationStatus = validateStressLevel(stressData.value)
        
        return StressLevel(
            id = generateId(),
            userId = userId,
            timestamp = timestamp,
            value = stressData.value,
            hrvValue = stressData.hrvValue,
            confidenceScore = stressData.confidenceScore,
            deviceId = deviceId,
            deviceType = deviceType,
            measurementContext = context,
            validationStatus = validationStatus
        )
    }
    
    /**
     * Map EcgData to Ecg domain model
     */
    fun mapToEcg(
        ecgData: EcgData,
        userId: String,
        deviceId: String? = null,
        deviceType: String? = null,
        durationSeconds: Int
    ): Ecg {
        val timestamp = convertTimestampToLocalDateTime(ecgData.timestamp)
        
        // Create measurement context
        val context = MeasurementContext(
            activityState = ActivityState.RESTING, // ECG typically measured at rest
            bodyPosition = BodyPosition.UNKNOWN,
            manuallyEntered = false
        )
        
        // Map annotations if available
        val annotations = ecgData.annotations?.map { 
            EcgAnnotation(
                timeOffsetMs = it.timeOffsetMs,
                annotationType = it.type,
                description = it.description,
                confidence = it.confidence
            )
        } ?: emptyList()
        
        // Map HRV data if available
        val hrvData = ecgData.hrvData?.let {
            val frequencyDomain = if (it.vlf != null && it.lf != null && it.hf != null && it.lfHfRatio != null) {
                FrequencyDomainData(
                    vlf = it.vlf,
                    lf = it.lf,
                    hf = it.hf,
                    lfHfRatio = it.lfHfRatio
                )
            } else {
                null
            }
            
            HeartRateVariabilityData(
                sdnn = it.sdnn,
                rmssd = it.rmssd,
                pnn50 = it.pnn50,
                rrIntervals = it.rrIntervals,
                frequencyDomain = frequencyDomain
            )
        }
        
        // Validate ECG data
        val validationStatus = validateEcg(ecgData.waveformData, ecgData.samplingRate)
        
        return Ecg(
            id = generateId(),
            userId = userId,
            timestamp = timestamp,
            waveformData = ecgData.waveformData,
            samplingRate = ecgData.samplingRate,
            leadType = EcgLeadType.SINGLE_LEAD, // Most wearables use single lead
            durationSeconds = durationSeconds,
            annotations = annotations,
            classification = ecgData.classification,
            heartRate = ecgData.heartRate,
            hrvData = hrvData,
            deviceId = deviceId,
            deviceType = deviceType,
            measurementContext = context,
            validationStatus = validationStatus
        )
    }
    
    /**
     * Map GlucoseData to BloodGlucose domain model
     */
    fun mapToBloodGlucose(
        glucoseData: GlucoseData,
        userId: String,
        deviceId: String? = null,
        deviceType: String? = null
    ): BloodGlucose {
        val timestamp = convertTimestampToLocalDateTime(glucoseData.timestamp)
        
        // Create measurement context
        val context = MeasurementContext(
            activityState = ActivityState.UNKNOWN,
            bodyPosition = BodyPosition.UNKNOWN,
            manuallyEntered = false
        )
        
        // Map meal context if available
        val mealContext = glucoseData.mealInfo?.let {
            MealContext(
                mealType = it.mealType,
                timeRelativeToMeal = it.timeRelativeToMeal,
                carbIntake = it.carbIntake
            )
        }
        
        // Map medication context if available
        val medicationContext = glucoseData.medicationInfo?.let {
            MedicationContext(
                medicationName = it.medicationName,
                dosage = it.dosage,
                unit = it.unit,
                timeRelativeToMedication = it.timeRelativeToMedication
            )
        }
        
        // Validate blood glucose value
        val validationStatus = validateBloodGlucose(glucoseData.value)
        
        return BloodGlucose(
            id = generateId(),
            userId = userId,
            timestamp = timestamp,
            value = glucoseData.value,
            measurementType = glucoseData.measurementType,
            mealContext = mealContext,
            medicationContext = medicationContext,
            deviceId = deviceId,
            deviceType = deviceType,
            measurementContext = context,
            validationStatus = validationStatus
        )
    }
    
    /**
     * Map ActivityData to Activity domain model
     */
    fun mapToActivity(
        activityData: ActivityData,
        userId: String,
        deviceId: String? = null,
        deviceType: String? = null
    ): Activity {
        val startTime = convertTimestampToLocalDateTime(activityData.startTimestamp)
        val endTime = convertTimestampToLocalDateTime(activityData.endTimestamp)
        
        // Calculate calories if not provided
        val calories = activityData.caloriesBurned ?: estimateCalories(
            activityData.type,
            activityData.durationSeconds,
            activityData.averageHeartRate
        )
        
        // Determine intensity if not provided
        val intensity = activityData.intensity ?: determineIntensity(
            activityData.averageHeartRate,
            activityData.type
        )
        
        return Activity(
            id = generateId(),
            userId = userId,
            startTime = startTime,
            endTime = endTime,
            activityType = activityData.type,
            durationSeconds = activityData.durationSeconds,
            distanceMeters = activityData.distanceMeters,
            caloriesBurned = calories,
            averageHeartRate = activityData.averageHeartRate,
            maxHeartRate = activityData.maxHeartRate,
            steps = activityData.steps,
            intensity = intensity,
            speedMetersPerSecond = activityData.speedMetersPerSecond,
            elevationGainMeters = activityData.elevationGainMeters,
            deviceId = deviceId,
            deviceType = deviceType
        )
    }
    
    /**
     * Map SleepData to Sleep domain model
     */
    fun mapToSleep(
        sleepData: SleepData,
        userId: String,
        deviceId: String? = null,
        deviceType: String? = null
    ): Sleep {
        val startTime = convertTimestampToLocalDateTime(sleepData.startTimestamp)
        val endTime = convertTimestampToLocalDateTime(sleepData.endTimestamp)
        
        // Map sleep stages
        val stages = sleepData.sleepStages.map { stage ->
            SleepStageRecord(
                stage = stage.stage,
                startTime = convertTimestampToLocalDateTime(stage.startTimestamp),
                endTime = convertTimestampToLocalDateTime(stage.endTimestamp),
                durationMinutes = stage.durationMinutes
            )
        }
        
        // Calculate sleep efficiency if not provided
        val efficiency = sleepData.sleepEfficiency ?: calculateSleepEfficiency(
            sleepData.durationMinutes,
            sleepData.awakeMinutes ?: 0
        )
        
        return Sleep(
            id = generateId(),
            userId = userId,
            startTime = startTime,
            endTime = endTime,
            durationMinutes = sleepData.durationMinutes,
            stages = stages,
            sleepScore = sleepData.sleepScore,
            deepSleepMinutes = sleepData.deepSleepMinutes,
            remSleepMinutes = sleepData.remSleepMinutes,
            lightSleepMinutes = sleepData.lightSleepMinutes,
            awakeMinutes = sleepData.awakeMinutes,
            sleepEfficiency = efficiency,
            sleepOnsetMinutes = sleepData.sleepOnsetMinutes,
            wakeAfterSleepOnsetMinutes = sleepData.wakeAfterSleepOnsetMinutes,
            deviceId = deviceId,
            deviceType = deviceType
        )
    }
    
    /**
     * Helper functions
     */
    
    private fun generateId(): String {
        return UUID.randomUUID().toString()
    }
    
    private fun convertTimestampToLocalDateTime(timestamp: Long): LocalDateTime {
        return Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
    }
    
    private fun determineActivityState(heartRate: Int): ActivityState {
        return when {
            heartRate < 60 -> ActivityState.RESTING
            heartRate < 100 -> ActivityState.ACTIVE
            else -> ActivityState.ACTIVE
        }
    }
    
    private fun validateHeartRate(value: Int): ValidationStatus {
        return when {
            value in 30..220 -> ValidationStatus.VALID
            else -> ValidationStatus.INVALID_RANGE
        }
    }
    
    private fun validateBloodPressure(systolic: Int, diastolic: Int): ValidationStatus {
        return when {
            systolic in 70..250 && diastolic in 40..150 && systolic > diastolic -> ValidationStatus.VALID
            else -> ValidationStatus.INVALID_RANGE
        }
    }
    
    private fun validateBloodOxygen(value: Int): ValidationStatus {
        return when {
            value in BloodOxygen.MIN_VALID_VALUE..BloodOxygen.MAX_VALID_VALUE -> ValidationStatus.VALID
            else -> ValidationStatus.INVALID_RANGE
        }
    }
    
    private fun validateTemperature(value: Float): ValidationStatus {
        return when {
            value in BodyTemperature.MIN_VALID_VALUE_CELSIUS..BodyTemperature.MAX_VALID_VALUE_CELSIUS -> ValidationStatus.VALID
            else -> ValidationStatus.INVALID_RANGE
        }
    }
    
    private fun validateStressLevel(value: Int): ValidationStatus {
        return when {
            value in StressLevel.MIN_VALID_VALUE..StressLevel.MAX_VALID_VALUE -> ValidationStatus.VALID
            else -> ValidationStatus.INVALID_RANGE
        }
    }
    
    private fun validateEcg(waveformData: List<Float>, samplingRate: Int): ValidationStatus {
        return when {
            waveformData.isEmpty() -> ValidationStatus.INVALID_RANGE
            samplingRate < Ecg.MIN_SAMPLING_RATE -> ValidationStatus.INVALID_MEASUREMENT_ERROR
            else -> ValidationStatus.VALID
        }
    }
    
    private fun validateBloodGlucose(value: Float): ValidationStatus {
        return when {
            value in BloodGlucose.MIN_VALID_VALUE_MGDL..BloodGlucose.MAX_VALID_VALUE_MGDL -> ValidationStatus.VALID
            else -> ValidationStatus.INVALID_RANGE
        }
    }
    
    private fun estimateCalories(
        activityType: ActivityType,
        durationSeconds: Int,
        averageHeartRate: Int?
    ): Double {
        // Simple MET-based calorie estimation
        // MET values from Compendium of Physical Activities
        val metValue = when (activityType) {
            ActivityType.WALKING -> 3.5
            ActivityType.RUNNING -> 8.0
            ActivityType.CYCLING -> 6.0
            ActivityType.SWIMMING -> 7.0
            ActivityType.STRENGTH_TRAINING -> 5.0
            ActivityType.YOGA -> 3.0
            ActivityType.HIIT -> 9.0
            ActivityType.ELLIPTICAL -> 5.0
            ActivityType.ROWING -> 6.0
            ActivityType.STAIR_CLIMBING -> 7.0
            ActivityType.HIKING -> 5.5
            ActivityType.SPORTS -> 6.5
            else -> 4.0
        }
        
        // Assume 70kg weight if no user data available
        val weight = 70.0
        
        // Calories = MET * weight (kg) * duration (hours)
        val durationHours = durationSeconds / 3600.0
        return metValue * weight * durationHours
    }
    
    private fun determineIntensity(averageHeartRate: Int?, activityType: ActivityType): Intensity {
        if (averageHeartRate == null) {
            // Estimate based on activity type if heart rate not available
            return when (activityType) {
                ActivityType.WALKING -> Intensity.LOW
                ActivityType.RUNNING -> Intensity.HIGH
                ActivityType.CYCLING -> Intensity.MODERATE
                ActivityType.SWIMMING -> Intensity.HIGH
                ActivityType.STRENGTH_TRAINING -> Intensity.MODERATE
                ActivityType.YOGA -> Intensity.LOW
                ActivityType.HIIT -> Intensity.VERY_HIGH
                ActivityType.ELLIPTICAL -> Intensity.MODERATE
                ActivityType.ROWING -> Intensity.HIGH
                ActivityType.STAIR_CLIMBING -> Intensity.HIGH
                ActivityType.HIKING -> Intensity.MODERATE
                ActivityType.SPORTS -> Intensity.HIGH
                else -> Intensity.MODERATE
            }
        }
        
        // Determine based on heart rate
        return when {
            averageHeartRate < 90 -> Intensity.LOW
            averageHeartRate < 120 -> Intensity.MODERATE
            averageHeartRate < 150 -> Intensity.HIGH
            else -> Intensity.VERY_HIGH
        }
    }
    
    private fun calculateSleepEfficiency(totalMinutes: Int, awakeMinutes: Int): Double {
        if (totalMinutes <= 0) return 0.0
        val sleepMinutes = totalMinutes - awakeMinutes
        return (sleepMinutes.toDouble() / totalMinutes.toDouble()) * 100.0
    }
}
