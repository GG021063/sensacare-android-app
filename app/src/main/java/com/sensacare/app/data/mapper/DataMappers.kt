package com.sensacare.app.data.mapper

import android.util.Log
import com.sensacare.app.data.entity.*
import com.sensacare.app.domain.model.*
import com.sensacare.veepoo.model.*
import com.veepoo.protocol.model.datas.*
import com.veepoo.protocol.model.enums.*
import timber.log.Timber
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * DataMappers.kt - Comprehensive data mappers for the SensaCare app
 *
 * This file contains all the mapping functions between:
 * - VeepooSDK models and Room entities
 * - Room entities and domain models
 * - Domain models and Room entities
 *
 * It also includes extension functions for easy conversion, null safety,
 * validation, collection mapping, and error handling.
 */

private const val TAG = "DataMappers"

/**
 * ====================================
 * VeepooSDK → Entity Mappers
 * ====================================
 */

/**
 * Maps a VeepooSDK DeviceInfo to a DeviceEntity
 */
fun com.veepoo.protocol.model.datas.DeviceInfo.toDeviceEntity(): DeviceEntity {
    return try {
        DeviceEntity(
            id = 0,
            name = this.deviceName ?: "Unknown Device",
            macAddress = this.deviceAddress ?: "",
            deviceType = this.deviceNumber ?: "Unknown",
            firmwareVersion = this.firmwareVersion ?: "Unknown",
            hardwareVersion = this.hardwareVersion ?: "Unknown",
            lastConnected = System.currentTimeMillis(),
            features = getSupportedFeatures().joinToString(","),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.NOT_SYNCED
        )
    } catch (e: Exception) {
        Timber.e(e, "Error mapping DeviceInfo to DeviceEntity")
        DeviceEntity(
            id = 0,
            name = "Error Device",
            macAddress = "",
            deviceType = "Unknown",
            firmwareVersion = "Unknown",
            hardwareVersion = "Unknown",
            lastConnected = System.currentTimeMillis()
        )
    }
}

/**
 * Gets the supported features from a VeepooSDK DeviceInfo
 */
private fun com.veepoo.protocol.model.datas.DeviceInfo.getSupportedFeatures(): List<String> {
    val features = mutableListOf<String>()
    
    // Add features based on device capabilities
    if (this.isHeartRate == 1) features.add(DeviceFeature.HEART_RATE.name)
    if (this.isBloodPressure == 1) features.add(DeviceFeature.BLOOD_PRESSURE.name)
    if (this.isBloodOxygen == 1) features.add(DeviceFeature.BLOOD_OXYGEN.name)
    if (this.isFindPhone == 1) features.add(DeviceFeature.FIND_PHONE.name)
    if (this.isECG == 1) features.add(DeviceFeature.ECG.name)
    if (this.isSleepMonitor == 1) features.add(DeviceFeature.SLEEP.name)
    
    // Always add STEPS as it's a basic feature
    features.add(DeviceFeature.STEPS.name)
    
    // Add more features based on device type or other properties
    if (this.deviceNumber?.contains("HB-", ignoreCase = true) == true) {
        features.add(DeviceFeature.NOTIFICATIONS.name)
    }
    
    return features
}

/**
 * Maps a VeepooSDK HeartData to a HeartRateEntity
 */
fun HeartData.toHeartRateEntity(deviceId: Long): HeartRateEntity {
    return try {
        val timestamp = this.time ?: System.currentTimeMillis()
        val date = HeartRateEntity.fromTimestamp(timestamp)
        
        HeartRateEntity(
            id = 0,
            deviceId = deviceId,
            timestamp = timestamp,
            date = date,
            heartRate = this.heartRate ?: 0,
            status = mapHeartRateStatus(this.status),
            isManualEntry = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING
        )
    } catch (e: Exception) {
        Timber.e(e, "Error mapping HeartData to HeartRateEntity")
        HeartRateEntity(
            id = 0,
            deviceId = deviceId,
            timestamp = System.currentTimeMillis(),
            date = HeartRateEntity.fromTimestamp(System.currentTimeMillis()),
            heartRate = 0,
            status = HeartRateStatus.UNKNOWN.name,
            isManualEntry = false
        )
    }
}

/**
 * Maps a VeepooSDK heart rate status to a HeartRateStatus string
 */
private fun mapHeartRateStatus(status: Int?): String {
    return when (status) {
        0 -> HeartRateStatus.NORMAL.name
        1 -> HeartRateStatus.EXERCISE.name
        2 -> HeartRateStatus.RESTING.name
        else -> HeartRateStatus.UNKNOWN.name
    }
}

/**
 * Maps a VeepooSDK BloodOxygenData to a BloodOxygenEntity
 */
fun com.veepoo.protocol.model.datas.BloodOxygenData.toBloodOxygenEntity(deviceId: Long): BloodOxygenEntity {
    return try {
        val timestamp = this.bloodOxygenVaueTime ?: System.currentTimeMillis()
        val date = BloodOxygenEntity.fromTimestamp(timestamp)
        
        BloodOxygenEntity(
            id = 0,
            deviceId = deviceId,
            timestamp = timestamp,
            date = date,
            bloodOxygen = this.bloodOxygenVaue ?: 0,
            isManualEntry = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING
        )
    } catch (e: Exception) {
        Timber.e(e, "Error mapping BloodOxygenData to BloodOxygenEntity")
        BloodOxygenEntity(
            id = 0,
            deviceId = deviceId,
            timestamp = System.currentTimeMillis(),
            date = BloodOxygenEntity.fromTimestamp(System.currentTimeMillis()),
            bloodOxygen = 0,
            isManualEntry = false
        )
    }
}

/**
 * Maps a VeepooSDK SportData to a StepEntity
 */
fun SportData.toStepEntity(deviceId: Long, goalSteps: Int = 10000): StepEntity {
    return try {
        val timestamp = this.timeStamp ?: System.currentTimeMillis()
        val date = StepEntity.fromTimestamp(timestamp)
        val steps = this.step ?: 0
        
        StepEntity(
            id = 0,
            deviceId = deviceId,
            timestamp = timestamp,
            date = date,
            steps = steps,
            distance = this.distance?.toFloat() ?: 0f,
            calories = this.calorie?.toFloat() ?: 0f,
            activeMinutes = calculateActiveMinutes(steps),
            goalSteps = goalSteps,
            goalAchieved = steps >= goalSteps,
            isManualEntry = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING
        )
    } catch (e: Exception) {
        Timber.e(e, "Error mapping SportData to StepEntity")
        StepEntity(
            id = 0,
            deviceId = deviceId,
            timestamp = System.currentTimeMillis(),
            date = StepEntity.fromTimestamp(System.currentTimeMillis()),
            steps = 0,
            distance = 0f,
            calories = 0f,
            activeMinutes = 0,
            goalSteps = goalSteps,
            goalAchieved = false,
            isManualEntry = false
        )
    }
}

/**
 * Calculates active minutes based on steps
 * This is a simple heuristic, can be improved with actual activity data
 */
private fun calculateActiveMinutes(steps: Int): Int {
    // Assuming average 100 steps per minute of activity
    return steps / 100
}

/**
 * Maps a VeepooSDK SleepData to a SleepEntity
 */
fun com.veepoo.protocol.model.datas.SleepData.toSleepEntity(deviceId: Long): SleepEntity {
    return try {
        val date = LocalDate.now().toString()
        val startTime = this.dateTime ?: System.currentTimeMillis()
        val endTime = startTime + (this.sleepTotalTime ?: 0) * 60 * 1000
        
        SleepEntity(
            id = 0,
            deviceId = deviceId,
            date = date,
            startTime = startTime,
            endTime = endTime,
            deepSleepMinutes = this.deepSleepTime ?: 0,
            lightSleepMinutes = this.lightSleepTime ?: 0,
            remSleepMinutes = 0, // VeepooSDK doesn't provide REM sleep data
            awakeSleepMinutes = this.awakeSleepTime ?: 0,
            totalSleepMinutes = this.sleepTotalTime ?: 0,
            sleepQuality = this.sleepQulity ?: 0,
            isManualEntry = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING
        )
    } catch (e: Exception) {
        Timber.e(e, "Error mapping SleepData to SleepEntity")
        SleepEntity(
            id = 0,
            deviceId = deviceId,
            date = LocalDate.now().toString(),
            startTime = System.currentTimeMillis(),
            endTime = System.currentTimeMillis(),
            deepSleepMinutes = 0,
            lightSleepMinutes = 0,
            remSleepMinutes = 0,
            awakeSleepMinutes = 0,
            totalSleepMinutes = 0,
            sleepQuality = 0,
            isManualEntry = false
        )
    }
}

/**
 * Maps VeepooSDK SleepData to a list of SleepDetailEntity objects
 */
fun com.veepoo.protocol.model.datas.SleepData.toSleepDetailEntities(sleepId: Long): List<SleepDetailEntity> {
    return try {
        val sleepDetails = mutableListOf<SleepDetailEntity>()
        val baseTimestamp = this.dateTime ?: System.currentTimeMillis()
        
        // Each element in sleepLine represents a 5-minute interval
        this.sleepLine?.forEachIndexed { index, sleepType ->
            val timestamp = baseTimestamp + (index * 5 * 60 * 1000)
            sleepDetails.add(
                SleepDetailEntity(
                    id = 0,
                    sleepId = sleepId,
                    timestamp = timestamp,
                    sleepType = mapSleepType(sleepType),
                    durationMinutes = 5,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    syncStatus = SyncStatus.PENDING
                )
            )
        }
        
        sleepDetails
    } catch (e: Exception) {
        Timber.e(e, "Error mapping SleepData to SleepDetailEntities")
        emptyList()
    }
}

/**
 * Maps a VeepooSDK sleep type to a SleepType string
 */
private fun mapSleepType(sleepType: Int?): String {
    return when (sleepType) {
        1 -> SleepType.DEEP_SLEEP.name
        2 -> SleepType.LIGHT_SLEEP.name
        3 -> SleepType.REM_SLEEP.name
        4 -> SleepType.AWAKE.name
        else -> SleepType.UNKNOWN.name
    }
}

/**
 * Maps a VeepooSDK BpData to a BloodPressureEntity
 */
fun BpData.toBloodPressureEntity(deviceId: Long): BloodPressureEntity {
    return try {
        val timestamp = this.measureTime ?: System.currentTimeMillis()
        val date = BloodPressureEntity.fromTimestamp(timestamp)
        
        BloodPressureEntity(
            id = 0,
            deviceId = deviceId,
            timestamp = timestamp,
            date = date,
            systolic = this.hightPressure ?: 0,
            diastolic = this.lowPressure ?: 0,
            heartRate = this.heartRate,
            isManualEntry = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING
        )
    } catch (e: Exception) {
        Timber.e(e, "Error mapping BpData to BloodPressureEntity")
        BloodPressureEntity(
            id = 0,
            deviceId = deviceId,
            timestamp = System.currentTimeMillis(),
            date = BloodPressureEntity.fromTimestamp(System.currentTimeMillis()),
            systolic = 0,
            diastolic = 0,
            heartRate = null,
            isManualEntry = false
        )
    }
}

/**
 * Maps a VeepooSDK TemperatureData to a TemperatureEntity
 */
fun com.veepoo.protocol.model.datas.TemperatureData.toTemperatureEntity(deviceId: Long): TemperatureEntity {
    return try {
        val timestamp = this.measureTime ?: System.currentTimeMillis()
        val date = TemperatureEntity.fromTimestamp(timestamp)
        
        TemperatureEntity(
            id = 0,
            deviceId = deviceId,
            timestamp = timestamp,
            date = date,
            temperatureCelsius = this.temperature?.toFloat() ?: 0f,
            bodyLocation = "wrist",
            isManualEntry = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING
        )
    } catch (e: Exception) {
        Timber.e(e, "Error mapping TemperatureData to TemperatureEntity")
        TemperatureEntity(
            id = 0,
            deviceId = deviceId,
            timestamp = System.currentTimeMillis(),
            date = TemperatureEntity.fromTimestamp(System.currentTimeMillis()),
            temperatureCelsius = 0f,
            bodyLocation = "wrist",
            isManualEntry = false
        )
    }
}

/**
 * Maps a VeepooSDK ExerciseData to an ActivityEntity
 */
fun ExerciseData.toActivityEntity(deviceId: Long): ActivityEntity {
    return try {
        val timestamp = this.startTime ?: System.currentTimeMillis()
        val date = ActivityEntity.fromTimestamp(timestamp)
        val endTime = this.endTime ?: (timestamp + (this.exerciseMinutes ?: 0) * 60 * 1000)
        val durationSeconds = ((endTime - timestamp) / 1000).toInt()
        
        ActivityEntity(
            id = 0,
            deviceId = deviceId,
            activityType = mapActivityType(this.exerciseType),
            date = date,
            startTime = timestamp,
            endTime = endTime,
            durationSeconds = durationSeconds,
            steps = this.exerciseSteps,
            distance = this.exerciseKilometer?.times(1000)?.toFloat(), // Convert km to meters
            calories = this.exerciseCalories?.toFloat(),
            avgHeartRate = this.exerciseAverageHeartRate,
            maxHeartRate = this.exerciseMaxHeartRate,
            isManualEntry = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING
        )
    } catch (e: Exception) {
        Timber.e(e, "Error mapping ExerciseData to ActivityEntity")
        ActivityEntity(
            id = 0,
            deviceId = deviceId,
            activityType = ActivityType.OTHER.name,
            date = ActivityEntity.fromTimestamp(System.currentTimeMillis()),
            startTime = System.currentTimeMillis(),
            endTime = System.currentTimeMillis(),
            durationSeconds = 0,
            isManualEntry = false
        )
    }
}

/**
 * Maps a VeepooSDK exercise type to an ActivityType string
 */
private fun mapActivityType(exerciseType: Int?): String {
    return when (exerciseType) {
        1 -> ActivityType.WALKING.name
        2 -> ActivityType.RUNNING.name
        3 -> ActivityType.CYCLING.name
        4 -> ActivityType.SWIMMING.name
        else -> ActivityType.OTHER.name
    }
}

/**
 * ====================================
 * Entity → Domain Mappers
 * ====================================
 */

/**
 * Maps a DeviceEntity to a Device domain model
 */
fun DeviceEntity.toDomain(): Device {
    return try {
        Device(
            id = this.id,
            name = this.name,
            macAddress = this.macAddress,
            deviceType = this.deviceType,
            firmwareVersion = this.firmwareVersion,
            hardwareVersion = this.hardwareVersion,
            lastConnected = this.lastConnected?.toLocalDateTime(),
            batteryLevel = this.batteryLevel,
            isActive = this.isActive,
            features = this.features.split(",")
                .filter { it.isNotBlank() }
                .mapNotNull { DeviceFeature.fromString(it) }
                .toSet(),
            syncStatus = SyncStatus.valueOf(this.syncStatus.name)
        )
    } catch (e: Exception) {
        Timber.e(e, "Error mapping DeviceEntity to Device")
        Device(
            id = this.id,
            name = this.name,
            macAddress = this.macAddress,
            deviceType = "Unknown",
            firmwareVersion = "Unknown",
            hardwareVersion = "Unknown",
            isActive = this.isActive
        )
    }
}

/**
 * Maps a UserEntity to a User domain model
 */
fun UserEntity.toDomain(): User {
    return try {
        User(
            id = this.id,
            name = this.name,
            email = this.email,
            dateOfBirth = this.dateOfBirth?.toLocalDate(),
            gender = this.gender?.let { Gender.fromString(it) },
            heightCm = this.heightCm,
            weightKg = this.weightKg,
            stepLengthCm = this.stepLengthCm,
            targetSteps = this.targetSteps,
            profileImageUri = this.profileImageUri,
            preferredDeviceId = this.preferredDeviceId
        )
    } catch (e: Exception) {
        Timber.e(e, "Error mapping UserEntity to User")
        User(
            id = this.id,
            name = this.name,
            email = this.email,
            targetSteps = this.targetSteps
        )
    }
}

/**
 * Maps a HeartRateEntity to a HeartRate domain model
 */
fun HeartRateEntity.toDomain(): HeartRate {
    return try {
        HeartRate(
            id = this.id,
            deviceId = this.deviceId,
            timestamp = this.timestamp.toLocalDateTime(),
            heartRate = this.heartRate,
            status = HeartRateStatus.fromString(this.status),
            isManualEntry = this.isManualEntry,
            notes = this.notes,
            syncStatus = SyncStatus.valueOf(this.syncStatus.name)
        )
    } catch (e: Exception) {
        Timber.e(e, "Error mapping HeartRateEntity to HeartRate")
        HeartRate(
            id = this.id,
            deviceId = this.deviceId,
            timestamp = LocalDateTime.now(),
            heartRate = this.heartRate,
            status = HeartRateStatus.UNKNOWN
        )
    }
}

/**
 * Maps a BloodOxygenEntity to a BloodOxygen domain model
 */
fun BloodOxygenEntity.toDomain(): BloodOxygen {
    return try {
        BloodOxygen(
            id = this.id,
            deviceId = this.deviceId,
            timestamp = this.timestamp.toLocalDateTime(),
            bloodOxygen = this.bloodOxygen,
            isManualEntry = this.isManualEntry,
            notes = this.notes,
            syncStatus = SyncStatus.valueOf(this.syncStatus.name)
        )
    } catch (e: Exception) {
        Timber.e(e, "Error mapping BloodOxygenEntity to BloodOxygen")
        BloodOxygen(
            id = this.id,
            deviceId = this.deviceId,
            timestamp = LocalDateTime.now(),
            bloodOxygen = this.bloodOxygen
        )
    }
}

/**
 * Maps a StepEntity to a StepData domain model
 */
fun StepEntity.toDomain(): StepData {
    return try {
        StepData(
            id = this.id,
            deviceId = this.deviceId,
            date = this.date.toLocalDate(),
            steps = this.steps,
            distance = this.distance,
            calories = this.calories,
            activeMinutes = this.activeMinutes,
            goalSteps = this.goalSteps,
            isManualEntry = this.isManualEntry,
            syncStatus = SyncStatus.valueOf(this.syncStatus.name)
        )
    } catch (e: Exception) {
        Timber.e(e, "Error mapping StepEntity to StepData")
        StepData(
            id = this.id,
            deviceId = this.deviceId,
            date = LocalDate.now(),
            steps = this.steps,
            distance = this.distance,
            calories = this.calories,
            goalSteps = this.goalSteps
        )
    }
}

/**
 * Maps a HourlyStepEntity to a HourlyStepData domain model
 */
fun HourlyStepEntity.toDomain(): HourlyStepData {
    return try {
        HourlyStepData(
            id = this.id,
            deviceId = this.deviceId,
            date = this.date.toLocalDate(),
            hour = this.hour,
            steps = this.steps,
            distance = this.distance,
            calories = this.calories,
            syncStatus = SyncStatus.valueOf(this.syncStatus.name)
        )
    } catch (e: Exception) {
        Timber.e(e, "Error mapping HourlyStepEntity to HourlyStepData")
        HourlyStepData(
            id = this.id,
            deviceId = this.deviceId,
            date = LocalDate.now(),
            hour = this.hour,
            steps = this.steps,
            distance = this.distance,
            calories = this.calories
        )
    }
}

/**
 * Maps a SleepEntity to a Sleep domain model
 */
fun SleepEntity.toDomain(sleepDetails: List<SleepDetailEntity> = emptyList()): Sleep {
    return try {
        Sleep(
            id = this.id,
            deviceId = this.deviceId,
            date = this.date.toLocalDate(),
            startTime = this.startTime.toLocalDateTime(),
            endTime = this.endTime.toLocalDateTime(),
            deepSleepMinutes = this.deepSleepMinutes,
            lightSleepMinutes = this.lightSleepMinutes,
            remSleepMinutes = this.remSleepMinutes,
            awakeSleepMinutes = this.awakeSleepMinutes,
            totalSleepMinutes = this.totalSleepMinutes,
            sleepQuality = this.sleepQuality,
            sleepDetails = sleepDetails.map { it.toDomain() },
            isManualEntry = this.isManualEntry,
            notes = this.notes,
            syncStatus = SyncStatus.valueOf(this.syncStatus.name)
        )
    } catch (e: Exception) {
        Timber.e(e, "Error mapping SleepEntity to Sleep")
        Sleep(
            id = this.id,
            deviceId = this.deviceId,
            date = LocalDate.now(),
            startTime = LocalDateTime.now().minusHours(8),
            endTime = LocalDateTime.now(),
            deepSleepMinutes = this.deepSleepMinutes,
            lightSleepMinutes = this.lightSleepMinutes,
            remSleepMinutes = this.remSleepMinutes,
            awakeSleepMinutes = this.awakeSleepMinutes,
            totalSleepMinutes = this.totalSleepMinutes,
            sleepQuality = this.sleepQuality
        )
    }
}

/**
 * Maps a SleepDetailEntity to a SleepDetail domain model
 */
fun SleepDetailEntity.toDomain(): SleepDetail {
    return try {
        SleepDetail(
            id = this.id,
            sleepId = this.sleepId,
            timestamp = this.timestamp.toLocalDateTime(),
            sleepType = SleepType.fromString(this.sleepType),
            durationMinutes = this.durationMinutes
        )
    } catch (e: Exception) {
        Timber.e(e, "Error mapping SleepDetailEntity to SleepDetail")
        SleepDetail(
            id = this.id,
            sleepId = this.sleepId,
            timestamp = LocalDateTime.now(),
            sleepType = SleepType.UNKNOWN,
            durationMinutes = this.durationMinutes
        )
    }
}

/**
 * Maps a BloodPressureEntity to a BloodPressure domain model
 */
fun BloodPressureEntity.toDomain(): BloodPressure {
    return try {
        BloodPressure(
            id = this.id,
            deviceId = this.deviceId,
            timestamp = this.timestamp.toLocalDateTime(),
            systolic = this.systolic,
            diastolic = this.diastolic,
            heartRate = this.heartRate,
            isManualEntry = this.isManualEntry,
            notes = this.notes,
            syncStatus = SyncStatus.valueOf(this.syncStatus.name)
        )
    } catch (e: Exception) {
        Timber.e(e, "Error mapping BloodPressureEntity to BloodPressure")
        BloodPressure(
            id = this.id,
            deviceId = this.deviceId,
            timestamp = LocalDateTime.now(),
            systolic = this.systolic,
            diastolic = this.diastolic,
            heartRate = this.heartRate
        )
    }
}

/**
 * Maps a TemperatureEntity to a Temperature domain model
 */
fun TemperatureEntity.toDomain(): Temperature {
    return try {
        Temperature(
            id = this.id,
            deviceId = this.deviceId,
            timestamp = this.timestamp.toLocalDateTime(),
            temperatureCelsius = this.temperatureCelsius,
            bodyLocation = this.bodyLocation,
            isManualEntry = this.isManualEntry,
            notes = this.notes,
            syncStatus = SyncStatus.valueOf(this.syncStatus.name)
        )
    } catch (e: Exception) {
        Timber.e(e, "Error mapping TemperatureEntity to Temperature")
        Temperature(
            id = this.id,
            deviceId = this.deviceId,
            timestamp = LocalDateTime.now(),
            temperatureCelsius = this.temperatureCelsius,
            bodyLocation = this.bodyLocation
        )
    }
}

/**
 * Maps an ActivityEntity to an Activity domain model
 */
fun ActivityEntity.toDomain(): Activity {
    return try {
        Activity(
            id = this.id,
            deviceId = this.deviceId,
            activityType = ActivityType.fromString(this.activityType),
            date = this.date.toLocalDate(),
            startTime = this.startTime.toLocalDateTime(),
            endTime = this.endTime.toLocalDateTime(),
            durationSeconds = this.durationSeconds,
            steps = this.steps,
            distance = this.distance,
            calories = this.calories,
            avgHeartRate = this.avgHeartRate,
            maxHeartRate = this.maxHeartRate,
            avgPace = calculatePace(this.distance, this.durationSeconds),
            isManualEntry = this.isManualEntry,
            notes = this.notes,
            syncStatus = SyncStatus.valueOf(this.syncStatus.name)
        )
    } catch (e: Exception) {
        Timber.e(e, "Error mapping ActivityEntity to Activity")
        Activity(
            id = this.id,
            deviceId = this.deviceId,
            activityType = ActivityType.OTHER,
            date = LocalDate.now(),
            startTime = LocalDateTime.now().minusHours(1),
            endTime = LocalDateTime.now(),
            durationSeconds = this.durationSeconds
        )
    }
}

/**
 * Calculates pace in minutes per kilometer
 */
private fun calculatePace(distanceMeters: Float?, durationSeconds: Int): Float? {
    if (distanceMeters == null || distanceMeters <= 0f || durationSeconds <= 0) {
        return null
    }
    
    val distanceKm = distanceMeters / 1000f
    val durationMinutes = durationSeconds / 60f
    
    return durationMinutes / distanceKm
}

/**
 * Maps a HealthInsightEntity to a HealthInsight domain model
 */
fun HealthInsightEntity.toDomain(): HealthInsight {
    return try {
        HealthInsight(
            id = this.id,
            userId = this.userId,
            date = this.date.toLocalDate(),
            insightType = HealthInsightType.fromString(this.insightType) ?: HealthInsightType.OVERALL_HEALTH,
            score = this.score,
            value = this.value,
            trend = HealthInsightTrend.fromString(this.trend),
            message = this.message,
            severity = HealthInsightSeverity.fromString(this.severity)
        )
    } catch (e: Exception) {
        Timber.e(e, "Error mapping HealthInsightEntity to HealthInsight")
        HealthInsight(
            id = this.id,
            userId = this.userId,
            date = LocalDate.now(),
            insightType = HealthInsightType.OVERALL_HEALTH
        )
    }
}

/**
 * Maps a NotificationEntity to a Notification domain model
 */
fun NotificationEntity.toDomain(): Notification {
    return try {
        Notification(
            id = this.id,
            deviceId = this.deviceId,
            type = NotificationType.fromString(this.type),
            title = this.title,
            content = this.content,
            timestamp = this.timestamp.toLocalDateTime(),
            isRead = this.isRead,
            isDelivered = this.isDelivered,
            syncStatus = SyncStatus.valueOf(this.syncStatus.name)
        )
    } catch (e: Exception) {
        Timber.e(e, "Error mapping NotificationEntity to Notification")
        Notification(
            id = this.id,
            deviceId = this.deviceId,
            type = NotificationType.SYSTEM_ALERT,
            title = this.title,
            content = this.content,
            timestamp = LocalDateTime.now()
        )
    }
}

/**
 * Maps a SettingEntity to a Setting domain model
 */
fun SettingEntity.toDomain(): Setting {
    return try {
        Setting(
            id = this.id,
            key = this.key,
            value = this.value,
            deviceId = this.deviceId,
            category = this.category
        )
    } catch (e: Exception) {
        Timber.e(e, "Error mapping SettingEntity to Setting")
        Setting(
            id = this.id,
            key = this.key,
            value = this.value
        )
    }
}

/**
 * ====================================
 * Domain → Entity Mappers
 * ====================================
 */

/**
 * Maps a Device domain model to a DeviceEntity
 */
fun Device.toEntity(): DeviceEntity {
    return try {
        DeviceEntity(
            id = this.id,
            name = this.name,
            macAddress = this.macAddress,
            deviceType = this.deviceType,
            firmwareVersion = this.firmwareVersion,
            hardwareVersion = this.hardwareVersion,
            lastConnected = this.lastConnected?.toEpochMilli(),
            batteryLevel = this.batteryLevel,
            isActive = this.isActive,
            features = this.features.joinToString(",") { it.name },
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.valueOf(this.syncStatus.name)
        )
    } catch (e: Exception) {
        Timber.e(e, "Error mapping Device to DeviceEntity")
        DeviceEntity(
            id = this.id,
            name = this.name,
            macAddress = this.macAddress,
            deviceType = this.deviceType,
            firmwareVersion = this.firmwareVersion,
            hardwareVersion = this.hardwareVersion,
            isActive = this.isActive
        )
    }
}

/**
 * Maps a User domain model to a UserEntity
 */
fun User.toEntity(): UserEntity {
    return try {
        UserEntity(
            id = this.id,
            name = this.name,
            email = this.email,
            dateOfBirth = this.dateOfBirth?.toEpochMilli(),
            gender = this.gender?.name,
            heightCm = this.heightCm,
            weightKg = this.weightKg,
            stepLengthCm = this.stepLengthCm,
            targetSteps = this.targetSteps,
            profileImageUri = this.profileImageUri,
            preferredDeviceId = this.preferredDeviceId,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.NOT_SYNCED
        )
    } catch (e: Exception) {
        Timber.e(e, "Error mapping User to UserEntity")
        UserEntity(
            id = this.id,
            name = this.name,
            email = this.email,
            targetSteps = this.targetSteps
        )
    }
}

/**
 * Maps a HeartRate domain model to a HeartRateEntity
 */
fun HeartRate.toEntity(): HeartRateEntity {
    return try {
        val timestamp = this.timestamp.toEpochMilli()
        val date = HeartRateEntity.fromTimestamp(timestamp)
        
        HeartRateEntity(
            id = this.id,
            deviceId = this.deviceId,
            timestamp = timestamp,
            date = date,
            heartRate = this.heartRate,
            status = this.status.name,
            isManualEntry = this.isManualEntry,
            notes = this.notes,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.valueOf(this.syncStatus.name)
        )
    } catch (e: Exception) {
        Timber.e(e, "Error mapping HeartRate to HeartRateEntity")
        HeartRateEntity(
            id = this.id,
            deviceId = this.deviceId,
            timestamp = System.currentTimeMillis(),
            date = HeartRateEntity.fromTimestamp(System.currentTimeMillis()),
            heartRate = this.heartRate,
            status = HeartRateStatus.UNKNOWN.name,
            isManualEntry = this.isManualEntry
        )
    }
}

/**
 * Maps a BloodOxygen domain model to a BloodOxygenEntity
 */
fun BloodOxygen.toEntity(): BloodOxygenEntity {
    return try {
        val timestamp = this.timestamp.toEpochMilli()
        val date = BloodOxygenEntity.fromTimestamp(timestamp)
        
        BloodOxygenEntity(
            id = this.id,
            deviceId = this.deviceId,
            timestamp = timestamp,
            date = date,
            bloodOxygen = this.bloodOxygen,
            isManualEntry = this.isManualEntry,
            notes = this.notes,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.valueOf(this.syncStatus.name)
        )
    } catch (e: Exception) {
        Timber.e(e, "Error mapping BloodOxygen to BloodOxygenEntity")
        BloodOxygenEntity(
            id = this.id,
            deviceId = this.deviceId,
            timestamp = System.currentTimeMillis(),
            date = BloodOxygenEntity.fromTimestamp(System.currentTimeMillis()),
            bloodOxygen = this.bloodOxygen,
            isManualEntry = this.isManualEntry
        )
    }
}

/**
 * Maps a StepData domain model to a StepEntity
 */
fun StepData.toEntity(): StepEntity {
    return try {
        val date = this.date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val timestamp = this.date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        
        StepEntity(
            id = this.id,
            deviceId = this.deviceId,
            timestamp = timestamp,
            date = date,
            steps = this.steps,
            distance = this.distance,
            calories = this.calories,
            activeMinutes = this.activeMinutes,
            goalSteps = this.goalSteps,
            goalAchieved = this.steps >= this.goalSteps,
            isManualEntry = this.isManualEntry,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.valueOf(this.syncStatus.name)
        )
    } catch (e: Exception) {
        Timber.e(e, "Error mapping StepData to StepEntity")
        StepEntity(
            id = this.id,
            deviceId = this.deviceId,
            timestamp = System.currentTimeMillis(),
            date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
            steps = this.steps,
            distance = this.distance,
            calories = this.calories,
            goalSteps = this.goalSteps,
            goalAchieved = this.steps >= this.goalSteps,
            isManualEntry = this.isManualEntry
        )
    }
}

/**
 * Maps a HourlyStepData domain model to a HourlyStepEntity
 */
fun HourlyStepData.toEntity(): HourlyStepEntity {
    return try {
        val date = this.date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val timestamp = this.date.atStartOfDay(ZoneId.systemDefault()).plusHours(this.hour.toLong()).toInstant().toEpochMilli()
        
        HourlyStepEntity(
            id = this.id,
            deviceId = this.deviceId,
            timestamp = timestamp,
            date = date,
            hour = this.hour,
            steps = this.steps,
            distance = this.distance,
            calories = this.calories,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.valueOf(this.syncStatus.name)
        )
    } catch (e: Exception) {
        Timber.e(e, "Error mapping HourlyStepData to HourlyStepEntity")
        HourlyStepEntity(
            id = this.id,
            deviceId = this.deviceId,
            timestamp = System.currentTimeMillis(),
            date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
            hour = this.hour,
            steps = this.steps,
            distance = this.distance,
            calories = this.calories
        )
    }
}

/**
 * Maps a Sleep domain model to a SleepEntity
 */
fun Sleep.toEntity(): SleepEntity {
    return try {
        val date = this.date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        
        SleepEntity(
            id = this.id,
            deviceId = this.deviceId,
            date = date,
            startTime = this.startTime.toEpochMilli(),
            endTime = this.endTime.toEpochMilli(),
            deepSleepMinutes = this.deepSleepMinutes,
            lightSleepMinutes = this.lightSleepMinutes,
            remSleepMinutes = this.remSleepMinutes,
            awakeSleepMinutes = this.awakeSleepMinutes,
            totalSleepMinutes = this.totalSleepMinutes,
            sleepQuality = this.sleepQuality,
            isManualEntry = this.isManualEntry,
            notes = this.notes,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.valueOf(this.syncStatus.name)
        )
    } catch (e: Exception) {
        Timber.e(e, "Error mapping Sleep to SleepEntity")
        SleepEntity(
            id = this.id,
            deviceId = this.deviceId,
            date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
            startTime = System.currentTimeMillis() - (8 * 60 * 60 * 1000),
            endTime = System.currentTimeMillis(),
            deepSleepMinutes = this.deepSleepMinutes,
            lightSleepMinutes = this.lightSleepMinutes,
            remSleepMinutes = this.remSleepMinutes,
            awakeSleepMinutes = this.awakeSleepMinutes,
            totalSleepMinutes = this.totalSleepMinutes,
            sleepQuality = this.sleepQuality,
            isManualEntry = this.isManualEntry
        )
    }
}

/**
 * Maps a SleepDetail domain model to a SleepDetailEntity
 */
fun SleepDetail.toEntity(): SleepDetailEntity {
    return try {
        SleepDetailEntity(
            id = this.id,
            sleepId = this.sleepId,
            timestamp = this.timestamp.toEpochMilli(),
            sleepType = this.sleepType.name,
            durationMinutes = this.durationMinutes,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING
        )
    } catch (e: Exception) {
        Timber.e(e, "Error mapping SleepDetail to SleepDetailEntity")
        SleepDetailEntity(
            id = this.id,
            sleepId = this.sleepId,
            timestamp = System.currentTimeMillis(),
            sleepType = SleepType.UNKNOWN.name,
            durationMinutes = this.durationMinutes
        )
    }
}

/**
 * Maps a BloodPressure domain model to a BloodPressureEntity
 */
fun BloodPressure.toEntity(): BloodPressureEntity {
    return try {
        val timestamp = this.timestamp.toEpochMilli()
        val date = BloodPressureEntity.fromTimestamp(timestamp)
        
        BloodPressureEntity(
            id = this.id,
            deviceId = this.deviceId,
            timestamp = timestamp,
            date = date,
            systolic = this.systolic,
            diastolic = this.diastolic,
            heartRate = this.heartRate,
            isManualEntry = this.isManualEntry,
            notes = this.notes,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.valueOf(this.syncStatus.name)
        )
    } catch (e: Exception) {
        Timber.e(e, "Error mapping BloodPressure to BloodPressureEntity")
        BloodPressureEntity(
            id = this.id,
            deviceId = this.deviceId,
            timestamp = System.currentTimeMillis(),
            date = BloodPressureEntity.fromTimestamp(System.currentTimeMillis()),
            systolic = this.systolic,
            diastolic = this.diastolic,
            heartRate = this.heartRate,
            isManualEntry = this.isManualEntry
        )
    }
}

/**
 * Maps a Temperature domain model to a TemperatureEntity
 */
fun Temperature.toEntity(): TemperatureEntity {
    return try {
        val timestamp = this.timestamp.toEpochMilli()
        val date = TemperatureEntity.fromTimestamp(timestamp)
        
        TemperatureEntity(
            id = this.id,
            deviceId = this.deviceId,
            timestamp = timestamp,
            date = date,
            temperatureCelsius = this.temperatureCelsius,
            bodyLocation = this.bodyLocation,
            isManualEntry = this.isManualEntry,
            notes = this.notes,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.valueOf(this.syncStatus.name)
        )
    } catch (e: Exception) {
        Timber.e(e, "Error mapping Temperature to TemperatureEntity")
        TemperatureEntity(
            id = this.id,
            deviceId = this.deviceId,
            timestamp = System.currentTimeMillis(),
            date = TemperatureEntity.fromTimestamp(System.currentTimeMillis()),
            temperatureCelsius = this.temperatureCelsius,
            bodyLocation = this.bodyLocation,
            isManualEntry = this.isManualEntry
        )
    }
}

/**
 * Maps an Activity domain model to an ActivityEntity
 */
fun Activity.toEntity(): ActivityEntity {
    return try {
        val date = this.date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        
        ActivityEntity(
            id = this.id,
            deviceId = this.deviceId,
            activityType = this.activityType.name,
            date = date,
            startTime = this.startTime.toEpochMilli(),
            endTime = this.endTime.toEpochMilli(),
            durationSeconds = this.durationSeconds,
            steps = this.steps,
            distance = this.distance,
            calories = this.calories,
            avgHeartRate = this.avgHeartRate,
            maxHeartRate = this.maxHeartRate,
            isManualEntry = this.isManualEntry,
            notes = this.notes,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.valueOf(this.syncStatus.name)
        )
    } catch (e: Exception) {
        Timber.e(e, "Error mapping Activity to ActivityEntity")
        ActivityEntity(
            id = this.id,
            deviceId = this.deviceId,
            activityType = this.activityType.name,
            date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
            startTime = System.currentTimeMillis() - (60 * 60 * 1000),
            endTime = System.currentTimeMillis(),
            durationSeconds = this.durationSeconds,
            isManualEntry = this.isManualEntry
        )
    }
}

/**
 * Maps a HealthInsight domain model to a HealthInsightEntity
 */
fun HealthInsight.toEntity(): HealthInsightEntity {
    return try {
        val date = this.date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        
        HealthInsightEntity(
            id = this.id,
            userId = this.userId,
            date = date,
            insightType = this.insightType.name,
            score = this.score,
            value = this.value,
            trend = this.trend.name,
            message = this.message,
            severity = this.severity.name,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.NOT_SYNCED
        )
    } catch (e: Exception) {
        Timber.e(e, "Error mapping HealthInsight to HealthInsightEntity")
        HealthInsightEntity(
            id = this.id,
            userId = this.userId,
            date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
            insightType = this.insightType.name,
            score = this.score,
            value = this.value,
            trend = this.trend.name,
            message = this.message,
            severity = this.severity.name
        )
    }
}

/**
 * Maps a Notification domain model to a NotificationEntity
 */
fun Notification.toEntity(): NotificationEntity {
    return try {
        NotificationEntity(
            id = this.id,
            deviceId = this.deviceId,
            type = this.type.name,
            title = this.title,
            content = this.content,
            timestamp = this.timestamp.toEpochMilli(),
            isRead = this.isRead,
            isDelivered = this.isDelivered,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.valueOf(this.syncStatus.name)
        )
    } catch (e: Exception) {
        Timber.e(e, "Error mapping Notification to NotificationEntity")
        NotificationEntity(
            id = this.id,
            deviceId = this.deviceId,
            type = this.type.name,
            title = this.title,
            content = this.content,
            timestamp = System.currentTimeMillis(),
            isRead = this.isRead,
            isDelivered = this.isDelivered
        )
    }
}

/**
 * Maps a Setting domain model to a SettingEntity
 */
fun Setting.toEntity(): SettingEntity {
    return try {
        SettingEntity(
            id = this.id,
            key = this.key,
            value = this.value,
            deviceId = this.deviceId,
            category = this.category,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.NOT_SYNCED
        )
    } catch (e: Exception) {
        Timber.e(e, "Error mapping Setting to SettingEntity")
        SettingEntity(
            id = this.id,
            key = this.key,
            value = this.value,
            deviceId = this.deviceId,
            category = this.category
        )
    }
}

/**
 * ====================================
 * Helper Extension Functions
 * ====================================
 */

/**
 * Convert a Long timestamp to a LocalDateTime
 */
fun Long.toLocalDateTime(): LocalDateTime {
    return try {
        Instant.ofEpochMilli(this)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
    } catch (e: Exception) {
        Timber.e(e, "Error converting timestamp to LocalDateTime")
        LocalDateTime.now()
    }
}

/**
 * Convert a String date to a LocalDate
 */
fun String.toLocalDate(): LocalDate {
    return try {
        LocalDate.parse(this)
    } catch (e: Exception) {
        Timber.e(e, "Error converting string to LocalDate")
        LocalDate.now()
    }
}

/**
 * Convert a LocalDateTime to a timestamp
 */
fun LocalDateTime.toEpochMilli(): Long {
    return try {
        this.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    } catch (e: Exception) {
        Timber.e(e, "Error converting LocalDateTime to timestamp")
        System.currentTimeMillis()
    }
}

/**
 * Convert a LocalDate to a timestamp
 */
fun LocalDate.toEpochMilli(): Long {
    return try {
        this.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    } catch (e: Exception) {
        Timber.e(e, "Error converting LocalDate to timestamp")
        System.currentTimeMillis()
    }
}

/**
 * ====================================
 * Collection Mapping Utilities
 * ====================================
 */

/**
 * Map a list of DeviceEntity to a list of Device domain models
 */
fun List<DeviceEntity>.toDomainList(): List<Device> {
    return this.mapNotNull { 
        try {
            it.toDomain()
        } catch (e: Exception) {
            Timber.e(e, "Error mapping DeviceEntity to Device")
            null
        }
    }
}

/**
 * Map a list of UserEntity to a list of User domain models
 */
fun List<UserEntity>.toDomainList(): List<User> {
    return this.mapNotNull { 
        try {
            it.toDomain()
        } catch (e: Exception) {
            Timber.e(e, "Error mapping UserEntity to User")
            null
        }
    }
}

/**
 * Map a list of HeartRateEntity to a list of HeartRate domain models
 */
fun List<HeartRateEntity>.toDomainList(): List<HeartRate> {
    return this.mapNotNull { 
        try {
            it.toDomain()
        } catch (e: Exception) {
            Timber.e(e, "Error mapping HeartRateEntity to HeartRate")
            null
        }
    }
}

/**
 * Map a list of BloodOxygenEntity to a list of BloodOxygen domain models
 */
fun List<BloodOxygenEntity>.toDomainList(): List<BloodOxygen> {
    return this.mapNotNull { 
        try {
            it.toDomain()
        } catch (e: Exception) {
            Timber.e(e, "Error mapping BloodOxygenEntity to BloodOxygen")
            null
        }
    }
}

/**
 * Map a list of StepEntity to a list of StepData domain models
 */
fun List<StepEntity>.toDomainList(): List<StepData> {
    return this.mapNotNull { 
        try {
            it.toDomain()
        } catch (e: Exception) {
            Timber.e(e, "Error mapping StepEntity to StepData")
            null
        }
    }
}

/**
 * Map a list of HourlyStepEntity to a list of HourlyStepData domain models
 */
fun List<HourlyStepEntity>.toDomainList(): List<HourlyStepData> {
    return this.mapNotNull { 
        try {
            it.toDomain()
        } catch (e: Exception) {
            Timber.e(e, "Error mapping HourlyStepEntity to HourlyStepData")
            null
        }
    }
}

/**
 * Map a list of SleepEntity to a list of Sleep domain models
 */
fun List<SleepEntity>.toDomainList(sleepDetailsMap: Map<Long, List<SleepDetailEntity>> = emptyMap()): List<Sleep> {
    return this.mapNotNull { 
        try {
            val sleepDetails = sleepDetailsMap[it.id] ?: emptyList()
            it.toDomain(sleepDetails)
        } catch (e: Exception) {
            Timber.e(e, "Error mapping SleepEntity to Sleep")
            null
        }
    }
}

/**
 * Map a list of SleepDetailEntity to a list of SleepDetail domain models
 */
fun List<SleepDetailEntity>.toDomainList(): List<SleepDetail> {
    return this.mapNotNull { 
        try {
            it.toDomain()
        } catch (e: Exception) {
            Timber.e(e, "Error mapping SleepDetailEntity to SleepDetail")
            null
        }
    }
}

/**
 * Map a list of BloodPressureEntity to a list of BloodPressure domain models
 */
fun List<BloodPressureEntity>.toDomainList(): List<BloodPressure> {
    return this.mapNotNull { 
        try {
            it.toDomain()
        } catch (e: Exception) {
            Timber.e(e, "Error mapping BloodPressureEntity to BloodPressure")
            null
        }
    }
}

/**
 * Map a list of TemperatureEntity to a list of Temperature domain models
 */
fun List<TemperatureEntity>.toDomainList(): List<Temperature> {
    return this.mapNotNull { 
        try {
            it.toDomain()
        } catch (e: Exception) {
            Timber.e(e, "Error mapping TemperatureEntity to Temperature")
            null
        }
    }
}

/**
 * Map a list of ActivityEntity to a list of Activity domain models
 */
fun List<ActivityEntity>.toDomainList(): List<Activity> {
    return this.mapNotNull { 
        try {
            it.toDomain()
        } catch (e: Exception) {
            Timber.e(e, "Error mapping ActivityEntity to Activity")
            null
        }
    }
}

/**
 * Map a list of HealthInsightEntity to a list of HealthInsight domain models
 */
fun List<HealthInsightEntity>.toDomainList(): List<HealthInsight> {
    return this.mapNotNull { 
        try {
            it.toDomain()
        } catch (e: Exception) {
            Timber.e(e, "Error mapping HealthInsightEntity to HealthInsight")
            null
        }
    }
}

/**
 * Map a list of NotificationEntity to a list of Notification domain models
 */
fun List<NotificationEntity>.toDomainList(): List<Notification> {
    return this.mapNotNull { 
        try {
            it.toDomain()
        } catch (e: Exception) {
            Timber.e(e, "Error mapping NotificationEntity to Notification")
            null
        }
    }
}

/**
 * Map a list of SettingEntity to a list of Setting domain models
 */
fun List<SettingEntity>.toDomainList(): List<Setting> {
    return this.mapNotNull { 
        try {
            it.toDomain()
        } catch (e: Exception) {
            Timber.e(e, "Error mapping SettingEntity to Setting")
            null
        }
    }
}

/**
 * Map a list of Device domain models to a list of DeviceEntity
 */
fun List<Device>.toEntityList(): List<DeviceEntity> {
    return this.mapNotNull { 
        try {
            it.toEntity()
        } catch (e: Exception) {
            Timber.e(e, "Error mapping Device to DeviceEntity")
            null
        }
    }
}

/**
 * Map a list of User domain models to a list of UserEntity
 */
fun List<User>.toEntityList(): List<UserEntity> {
    return this.mapNotNull { 
        try {
            it.toEntity()
        } catch (e: Exception) {
            Timber.e(e, "Error mapping User to UserEntity")
            null
        }
    }
}

/**
 * Map a list of HeartRate domain models to a list of HeartRateEntity
 */
fun List<HeartRate>.toEntityList(): List<HeartRateEntity> {
    return this.mapNotNull { 
        try {
            it.toEntity()
        } catch (e: Exception) {
            Timber.e(e, "Error mapping HeartRate to HeartRateEntity")
            null
        }
    }
}

/**
 * Map a list of BloodOxygen domain models to a list of BloodOxygenEntity
 */
fun List<BloodOxygen>.toEntityList(): List<BloodOxygenEntity> {
    return this.mapNotNull { 
        try {
            it.toEntity()
        } catch (e: Exception) {
            Timber.e(e, "Error mapping BloodOxygen to BloodOxygenEntity")
            null
        }
    }
}

/**
 * Map a list of StepData domain models to a list of StepEntity
 */
fun List<StepData>.toEntityList(): List<StepEntity> {
    return this.mapNotNull { 
        try {
            it.toEntity()
        } catch (e: Exception) {
            Timber.e(e, "Error mapping StepData to StepEntity")
            null
        }
    }
}

/**
 * Map a list of HourlyStepData domain models to a list of HourlyStepEntity
 */
fun List<HourlyStepData>.toEntityList(): List<HourlyStepEntity> {
    return this.mapNotNull { 
        try {
            it.toEntity()
        } catch (e: Exception) {
            Timber.e(e, "Error mapping HourlyStepData to HourlyStepEntity")
            null
        }
    }
}

/**
 * Map a list of Sleep domain models to a list of SleepEntity
 */
fun List<Sleep>.toEntityList(): List<SleepEntity> {
    return this.mapNotNull { 
        try {
            it.toEntity()
        } catch (e: Exception) {
            Timber.e(e, "Error mapping Sleep to SleepEntity")
            null
        }
    }
}

/**
 * Map a list of SleepDetail domain models to a list of SleepDetailEntity
 */
fun List<SleepDetail>.toEntityList(): List<SleepDetailEntity> {
    return this.mapNotNull { 
        try {
            it.toEntity()
        } catch (e: Exception) {
            Timber.e(e, "Error mapping SleepDetail to SleepDetailEntity")
            null
        }
    }
}

/**
 * Map a list of BloodPressure domain models to a list of BloodPressureEntity
 */
fun List<BloodPressure>.toEntityList(): List<BloodPressureEntity> {
    return this.mapNotNull { 
        try {
            it.toEntity()
        } catch (e: Exception) {
            Timber.e(e, "Error mapping BloodPressure to BloodPressureEntity")
            null
        }
    }
}

/**
 * Map a list of Temperature domain models to a list of TemperatureEntity
 */
fun List<Temperature>.toEntityList(): List<TemperatureEntity> {
    return this.mapNotNull { 
        try {
            it.toEntity()
        } catch (e: Exception) {
            Timber.e(e, "Error mapping Temperature to TemperatureEntity")
            null
        }
    }
}

/**
 * Map a list of Activity domain models to a list of ActivityEntity
 */
fun List<Activity>.toEntityList(): List<ActivityEntity> {
    return this.mapNotNull { 
        try {
            it.toEntity()
        } catch (e: Exception) {
            Timber.e(e, "Error mapping Activity to ActivityEntity")
            null
        }
    }
}

/**
 * Map a list of HealthInsight domain models to a list of HealthInsightEntity
 */
fun List<HealthInsight>.toEntityList(): List<HealthInsightEntity> {
    return this.mapNotNull { 
        try {
            it.toEntity()
        } catch (e: Exception) {
            Timber.e(e, "Error mapping HealthInsight to HealthInsightEntity")
            null
        }
    }
}

/**
 * Map a list of Notification domain models to a list of NotificationEntity
 */
fun List<Notification>.toEntityList(): List<NotificationEntity> {
    return this.mapNotNull { 
        try {
            it.toEntity()
        } catch (e: Exception) {
            Timber.e(e, "Error mapping Notification to NotificationEntity")
            null
        }
    }
}

/**
 * Map a list of Setting domain models to a list of SettingEntity
 */
fun List<Setting>.toEntityList(): List<SettingEntity> {
    return this.mapNotNull { 
        try {
            it.toEntity()
        } catch (e: Exception) {
            Timber.e(e, "Error mapping Setting to SettingEntity")
            null
        }
    }
}

/**
 * Map a list of HeartData to a list of HeartRateEntity
 */
fun List<HeartData>.toHeartRateEntityList(deviceId: Long): List<HeartRateEntity> {
    return this.mapNotNull { 
        try {
            it.toHeartRateEntity(deviceId)
        } catch (e: Exception) {
            Timber.e(e, "Error mapping HeartData to HeartRateEntity")
            null
        }
    }
}

/**
 * Map a list of BloodOxygenData to a list of BloodOxygenEntity
 */
fun List<com.veepoo.protocol.model.datas.BloodOxygenData>.toBloodOxygenEntityList(deviceId: Long): List<BloodOxygenEntity> {
    return this.mapNotNull { 
        try {
            it.toBloodOxygenEntity(deviceId)
        } catch (e: Exception) {
            Timber.e(e, "Error mapping BloodOxygenData to BloodOxygenEntity")
            null
        }
    }
}

/**
 * Map a list of SportData to a list of StepEntity
 */
fun List<SportData>.toStepEntityList(deviceId: Long, goalSteps: Int = 10000): List<StepEntity> {
    return this.mapNotNull { 
        try {
            it.toStepEntity(deviceId, goalSteps)
        } catch (e: Exception) {
            Timber.e(e, "Error mapping SportData to StepEntity")
            null
        }
    }
}

/**
 * Map a list of BpData to a list of BloodPressureEntity
 */
fun List<BpData>.toBloodPressureEntityList(deviceId: Long): List<BloodPressureEntity> {
    return this.mapNotNull { 
        try {
            it.toBloodPressureEntity(deviceId)
        } catch (e: Exception) {
            Timber.e(e, "Error mapping BpData to BloodPressureEntity")
            null
        }
    }
}

/**
 * Map a list of TemperatureData to a list of TemperatureEntity
 */
fun List<com.veepoo.protocol.model.datas.TemperatureData>.toTemperatureEntityList(deviceId: Long): List<TemperatureEntity> {
    return this.mapNotNull { 
        try {
            it.toTemperatureEntity(deviceId)
        } catch (e: Exception) {
            Timber.e(e, "Error mapping TemperatureData to TemperatureEntity")
            null
        }
    }
}

/**
 * Map a list of ExerciseData to a list of ActivityEntity
 */
fun List<ExerciseData>.toActivityEntityList(deviceId: Long): List<ActivityEntity> {
    return this.mapNotNull { 
        try {
            it.toActivityEntity(deviceId)
        } catch (e: Exception) {
            Timber.e(e, "Error mapping ExerciseData to ActivityEntity")
            null
        }
    }
}
