package com.sensacare.app.data.mapper

import com.sensacare.app.data.local.entity.*
import com.sensacare.app.domain.model.*
import com.sensacare.app.domain.util.SyncStatus
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Base mapper interface for bidirectional mapping between domain models and database entities
 */
interface EntityMapper<Domain, Entity> {
    fun mapToDomain(entity: Entity): Domain
    fun mapToEntity(domain: Domain): Entity
}

/**
 * HealthDataMapper - Maps between HealthData domain model and HealthDataEntity
 */
@Singleton
class HealthDataMapper @Inject constructor() : EntityMapper<HealthData, HealthDataEntity> {
    
    override fun mapToDomain(entity: HealthDataEntity): HealthData {
        return HealthData(
            id = entity.id,
            userId = entity.userId,
            deviceId = entity.deviceId,
            metricType = MetricType.valueOf(entity.metricType),
            timestamp = entity.timestamp,
            value = entity.value,
            unit = entity.unit,
            notes = entity.notes,
            tags = entity.tags?.split(",") ?: emptyList(),
            syncStatus = SyncStatus.valueOf(entity.syncStatus),
            createdAt = entity.createdAt,
            modifiedAt = entity.modifiedAt
        )
    }
    
    override fun mapToEntity(domain: HealthData): HealthDataEntity {
        return HealthDataEntity(
            id = domain.id.ifEmpty { UUID.randomUUID().toString() },
            userId = domain.userId,
            deviceId = domain.deviceId,
            metricType = domain.metricType.name,
            timestamp = domain.timestamp,
            value = domain.value,
            unit = domain.unit,
            notes = domain.notes,
            tags = domain.tags.joinToString(",").takeIf { it.isNotEmpty() },
            syncStatus = domain.syncStatus.name,
            createdAt = domain.createdAt ?: LocalDateTime.now(),
            modifiedAt = domain.modifiedAt ?: LocalDateTime.now()
        )
    }
}

/**
 * HeartRateMapper - Maps between HeartRate domain model and HeartRateEntity
 */
@Singleton
class HeartRateMapper @Inject constructor() : EntityMapper<HeartRate, HeartRateEntity> {
    
    override fun mapToDomain(entity: HeartRateEntity): HeartRate {
        return HeartRate(
            id = entity.id,
            userId = entity.userId,
            deviceId = entity.deviceId,
            timestamp = entity.timestamp,
            value = entity.value,
            restingHeartRate = entity.restingHeartRate,
            isRestingHeartRate = entity.isRestingHeartRate,
            hrvValue = entity.hrvValue,
            activityLevel = entity.activityLevel,
            zoneInfo = entity.zoneInfo,
            abnormalityDetected = entity.abnormalityDetected,
            abnormalityType = entity.abnormalityType,
            notes = entity.notes,
            tags = entity.tags?.split(",") ?: emptyList(),
            syncStatus = SyncStatus.valueOf(entity.syncStatus),
            createdAt = entity.createdAt,
            modifiedAt = entity.modifiedAt
        )
    }
    
    override fun mapToEntity(domain: HeartRate): HeartRateEntity {
        return HeartRateEntity(
            id = domain.id.ifEmpty { UUID.randomUUID().toString() },
            userId = domain.userId,
            deviceId = domain.deviceId,
            timestamp = domain.timestamp,
            value = domain.value,
            restingHeartRate = domain.restingHeartRate,
            isRestingHeartRate = domain.isRestingHeartRate,
            hrvValue = domain.hrvValue,
            activityLevel = domain.activityLevel,
            zoneInfo = domain.zoneInfo,
            abnormalityDetected = domain.abnormalityDetected,
            abnormalityType = domain.abnormalityType,
            notes = domain.notes,
            tags = domain.tags.joinToString(",").takeIf { it.isNotEmpty() },
            syncStatus = domain.syncStatus.name,
            createdAt = domain.createdAt ?: LocalDateTime.now(),
            modifiedAt = domain.modifiedAt ?: LocalDateTime.now()
        )
    }
}

/**
 * BloodPressureMapper - Maps between BloodPressure domain model and BloodPressureEntity
 */
@Singleton
class BloodPressureMapper @Inject constructor() : EntityMapper<BloodPressure, BloodPressureEntity> {
    
    override fun mapToDomain(entity: BloodPressureEntity): BloodPressure {
        return BloodPressure(
            id = entity.id,
            userId = entity.userId,
            deviceId = entity.deviceId,
            timestamp = entity.timestamp,
            systolic = entity.systolic,
            diastolic = entity.diastolic,
            pulse = entity.pulse,
            bodyPosition = entity.bodyPosition,
            armPosition = entity.armPosition,
            measurementContext = entity.measurementContext,
            classification = entity.classification?.let { BloodPressureClassification.valueOf(it) },
            notes = entity.notes,
            tags = entity.tags?.split(",") ?: emptyList(),
            syncStatus = SyncStatus.valueOf(entity.syncStatus),
            createdAt = entity.createdAt,
            modifiedAt = entity.modifiedAt
        )
    }
    
    override fun mapToEntity(domain: BloodPressure): BloodPressureEntity {
        return BloodPressureEntity(
            id = domain.id.ifEmpty { UUID.randomUUID().toString() },
            userId = domain.userId,
            deviceId = domain.deviceId,
            timestamp = domain.timestamp,
            systolic = domain.systolic,
            diastolic = domain.diastolic,
            pulse = domain.pulse,
            bodyPosition = domain.bodyPosition,
            armPosition = domain.armPosition,
            measurementContext = domain.measurementContext,
            classification = domain.classification?.name,
            notes = domain.notes,
            tags = domain.tags.joinToString(",").takeIf { it.isNotEmpty() },
            syncStatus = domain.syncStatus.name,
            createdAt = domain.createdAt ?: LocalDateTime.now(),
            modifiedAt = domain.modifiedAt ?: LocalDateTime.now()
        )
    }
}

/**
 * SleepMapper - Maps between Sleep domain model and SleepEntity with SleepStageEntity
 */
@Singleton
class SleepMapper @Inject constructor() {
    
    /**
     * Maps SleepEntity with SleepStageEntities to Sleep domain model
     */
    fun mapToDomain(sleepWithStages: SleepDao.SleepWithStages): Sleep {
        val sleepEntity = sleepWithStages.sleep
        val stageEntities = sleepWithStages.stages
        
        val sleepStages = stageEntities.map { stageEntity ->
            SleepStageData(
                id = stageEntity.id,
                sleepId = stageEntity.sleepId,
                stage = SleepStage.valueOf(stageEntity.stage),
                startTime = stageEntity.startTime,
                endTime = stageEntity.endTime,
                durationMinutes = stageEntity.durationMinutes
            )
        }
        
        return Sleep(
            id = sleepEntity.id,
            userId = sleepEntity.userId,
            deviceId = sleepEntity.deviceId,
            startTime = sleepEntity.startTime,
            endTime = sleepEntity.endTime,
            durationMinutes = sleepEntity.durationMinutes,
            sleepScore = sleepEntity.sleepScore,
            sleepEfficiency = sleepEntity.sleepEfficiency,
            deepSleepMinutes = sleepEntity.deepSleepMinutes,
            lightSleepMinutes = sleepEntity.lightSleepMinutes,
            remSleepMinutes = sleepEntity.remSleepMinutes,
            awakeMinutes = sleepEntity.awakeMinutes,
            sleepLatencyMinutes = sleepEntity.sleepLatencyMinutes,
            wakeCount = sleepEntity.wakeCount,
            notes = sleepEntity.notes,
            tags = sleepEntity.tags?.split(",") ?: emptyList(),
            stages = sleepStages,
            syncStatus = SyncStatus.valueOf(sleepEntity.syncStatus),
            createdAt = sleepEntity.createdAt,
            modifiedAt = sleepEntity.modifiedAt
        )
    }
    
    /**
     * Maps Sleep domain model to SleepEntity and SleepStageEntities
     * @return Pair of SleepEntity and List of SleepStageEntity
     */
    fun mapToEntity(domain: Sleep): Pair<SleepEntity, List<SleepStageEntity>> {
        val sleepId = domain.id.ifEmpty { UUID.randomUUID().toString() }
        
        val sleepEntity = SleepEntity(
            id = sleepId,
            userId = domain.userId,
            deviceId = domain.deviceId,
            startTime = domain.startTime,
            endTime = domain.endTime,
            durationMinutes = domain.durationMinutes,
            sleepScore = domain.sleepScore,
            sleepEfficiency = domain.sleepEfficiency,
            deepSleepMinutes = domain.deepSleepMinutes,
            lightSleepMinutes = domain.lightSleepMinutes,
            remSleepMinutes = domain.remSleepMinutes,
            awakeMinutes = domain.awakeMinutes,
            sleepLatencyMinutes = domain.sleepLatencyMinutes,
            wakeCount = domain.wakeCount,
            notes = domain.notes,
            tags = domain.tags.joinToString(",").takeIf { it.isNotEmpty() },
            syncStatus = domain.syncStatus.name,
            createdAt = domain.createdAt ?: LocalDateTime.now(),
            modifiedAt = domain.modifiedAt ?: LocalDateTime.now()
        )
        
        val stageEntities = domain.stages.map { stage ->
            SleepStageEntity(
                id = stage.id.ifEmpty { UUID.randomUUID().toString() },
                sleepId = sleepId,
                stage = stage.stage.name,
                startTime = stage.startTime,
                endTime = stage.endTime,
                durationMinutes = stage.durationMinutes,
                createdAt = LocalDateTime.now(),
                modifiedAt = LocalDateTime.now()
            )
        }
        
        return Pair(sleepEntity, stageEntities)
    }
}

/**
 * ActivityMapper - Maps between Activity domain model and ActivityEntity with ActivitySessionEntity
 */
@Singleton
class ActivityMapper @Inject constructor() {
    
    /**
     * Maps ActivityEntity with ActivitySessionEntities to Activity domain model
     */
    fun mapToDomain(activityWithSessions: ActivityDao.ActivityWithSessions): Activity {
        val activityEntity = activityWithSessions.activity
        val sessionEntities = activityWithSessions.sessions
        
        val activitySessions = sessionEntities.map { sessionEntity ->
            ActivitySession(
                id = sessionEntity.id,
                activityId = sessionEntity.activityId,
                startTime = sessionEntity.startTime,
                endTime = sessionEntity.endTime,
                durationSeconds = sessionEntity.durationSeconds,
                distance = sessionEntity.distance,
                calories = sessionEntity.calories,
                steps = sessionEntity.steps,
                avgHeartRate = sessionEntity.avgHeartRate,
                maxHeartRate = sessionEntity.maxHeartRate,
                intensity = Intensity.valueOf(sessionEntity.intensity),
                location = sessionEntity.location,
                elevationGain = sessionEntity.elevationGain,
                elevationLoss = sessionEntity.elevationLoss
            )
        }
        
        return Activity(
            id = activityEntity.id,
            userId = activityEntity.userId,
            deviceId = activityEntity.deviceId,
            activityType = ActivityType.valueOf(activityEntity.activityType),
            startTime = activityEntity.startTime,
            endTime = activityEntity.endTime,
            durationSeconds = activityEntity.durationSeconds,
            distance = activityEntity.distance,
            calories = activityEntity.calories,
            steps = activityEntity.steps,
            avgHeartRate = activityEntity.avgHeartRate,
            maxHeartRate = activityEntity.maxHeartRate,
            avgIntensity = activityEntity.avgIntensity?.let { Intensity.valueOf(it) },
            notes = activityEntity.notes,
            tags = activityEntity.tags?.split(",") ?: emptyList(),
            sessions = activitySessions,
            syncStatus = SyncStatus.valueOf(activityEntity.syncStatus),
            createdAt = activityEntity.createdAt,
            modifiedAt = activityEntity.modifiedAt
        )
    }
    
    /**
     * Maps Activity domain model to ActivityEntity and ActivitySessionEntities
     * @return Pair of ActivityEntity and List of ActivitySessionEntity
     */
    fun mapToEntity(domain: Activity): Pair<ActivityEntity, List<ActivitySessionEntity>> {
        val activityId = domain.id.ifEmpty { UUID.randomUUID().toString() }
        
        val activityEntity = ActivityEntity(
            id = activityId,
            userId = domain.userId,
            deviceId = domain.deviceId,
            activityType = domain.activityType.name,
            startTime = domain.startTime,
            endTime = domain.endTime,
            durationSeconds = domain.durationSeconds,
            distance = domain.distance,
            calories = domain.calories,
            steps = domain.steps,
            avgHeartRate = domain.avgHeartRate,
            maxHeartRate = domain.maxHeartRate,
            avgIntensity = domain.avgIntensity?.name,
            notes = domain.notes,
            tags = domain.tags.joinToString(",").takeIf { it.isNotEmpty() },
            syncStatus = domain.syncStatus.name,
            createdAt = domain.createdAt ?: LocalDateTime.now(),
            modifiedAt = domain.modifiedAt ?: LocalDateTime.now()
        )
        
        val sessionEntities = domain.sessions.map { session ->
            ActivitySessionEntity(
                id = session.id.ifEmpty { UUID.randomUUID().toString() },
                activityId = activityId,
                startTime = session.startTime,
                endTime = session.endTime,
                durationSeconds = session.durationSeconds,
                distance = session.distance,
                calories = session.calories,
                steps = session.steps,
                avgHeartRate = session.avgHeartRate,
                maxHeartRate = session.maxHeartRate,
                intensity = session.intensity.name,
                location = session.location,
                elevationGain = session.elevationGain,
                elevationLoss = session.elevationLoss,
                createdAt = LocalDateTime.now(),
                modifiedAt = LocalDateTime.now()
            )
        }
        
        return Pair(activityEntity, sessionEntities)
    }
}

/**
 * DeviceMapper - Maps between Device domain model and DeviceEntity with related entities
 */
@Singleton
class DeviceMapper @Inject constructor() {
    
    /**
     * Maps DeviceEntity with settings and sync history to Device domain model
     */
    fun mapToDomain(deviceWithDetails: DeviceDao.DeviceWithDetails): Device {
        val deviceEntity = deviceWithDetails.device
        val settingEntities = deviceWithDetails.settings
        val syncHistoryEntities = deviceWithDetails.syncHistory
        
        val deviceSettings = settingEntities.map { settingEntity ->
            DeviceSetting(
                id = settingEntity.id,
                deviceId = settingEntity.deviceId,
                key = settingEntity.key,
                value = settingEntity.value,
                dataType = settingEntity.dataType,
                description = settingEntity.description,
                createdAt = settingEntity.createdAt,
                modifiedAt = settingEntity.modifiedAt
            )
        }
        
        val syncHistory = syncHistoryEntities.map { historyEntity ->
            DeviceSyncHistory(
                id = historyEntity.id.toString(),
                deviceId = historyEntity.deviceId,
                syncType = historyEntity.syncType,
                syncStatus = SyncStatus.valueOf(historyEntity.syncStatus),
                startedAt = historyEntity.startedAt,
                completedAt = historyEntity.completedAt,
                itemsCount = historyEntity.itemsCount,
                errorMessage = historyEntity.errorMessage,
                createdAt = historyEntity.createdAt,
                modifiedAt = historyEntity.modifiedAt
            )
        }
        
        return Device(
            id = deviceEntity.id,
            userId = deviceEntity.userId,
            name = deviceEntity.name,
            manufacturer = deviceEntity.manufacturer,
            model = deviceEntity.model,
            serialNumber = deviceEntity.serialNumber,
            firmwareVersion = deviceEntity.firmwareVersion,
            batteryLevel = deviceEntity.batteryLevel,
            batteryLastUpdatedAt = deviceEntity.batteryLastUpdatedAt,
            isConnected = deviceEntity.isConnected,
            lastConnectedAt = deviceEntity.lastConnectedAt,
            lastSyncAt = deviceEntity.lastSyncAt,
            syncStatus = deviceEntity.syncStatus?.let { SyncStatus.valueOf(it) },
            capabilities = deviceEntity.capabilities?.split(",") ?: emptyList(),
            settings = deviceSettings,
            syncHistory = syncHistory,
            createdAt = deviceEntity.createdAt,
            modifiedAt = deviceEntity.modifiedAt
        )
    }
    
    /**
     * Maps Device domain model to DeviceEntity and related entities
     * @return Triple of DeviceEntity, List of DeviceSettingEntity, and List of DeviceSyncHistoryEntity
     */
    fun mapToEntity(domain: Device): Triple<DeviceEntity, List<DeviceSettingEntity>, List<DeviceSyncHistoryEntity>> {
        val deviceId = domain.id.ifEmpty { UUID.randomUUID().toString() }
        
        val deviceEntity = DeviceEntity(
            id = deviceId,
            userId = domain.userId,
            name = domain.name,
            manufacturer = domain.manufacturer,
            model = domain.model,
            serialNumber = domain.serialNumber,
            firmwareVersion = domain.firmwareVersion,
            batteryLevel = domain.batteryLevel,
            batteryLastUpdatedAt = domain.batteryLastUpdatedAt,
            isConnected = domain.isConnected,
            lastConnectedAt = domain.lastConnectedAt,
            lastSyncAt = domain.lastSyncAt,
            syncStatus = domain.syncStatus?.name,
            capabilities = domain.capabilities.joinToString(",").takeIf { it.isNotEmpty() },
            createdAt = domain.createdAt ?: LocalDateTime.now(),
            modifiedAt = domain.modifiedAt ?: LocalDateTime.now()
        )
        
        val settingEntities = domain.settings.map { setting ->
            DeviceSettingEntity(
                id = setting.id.ifEmpty { UUID.randomUUID().toString() },
                deviceId = deviceId,
                key = setting.key,
                value = setting.value,
                dataType = setting.dataType,
                description = setting.description,
                createdAt = setting.createdAt ?: LocalDateTime.now(),
                modifiedAt = setting.modifiedAt ?: LocalDateTime.now()
            )
        }
        
        // Only map new sync history entries (those without an ID)
        val newSyncHistoryEntities = domain.syncHistory
            .filter { it.id.isEmpty() || it.id == "0" }
            .map { history ->
                DeviceSyncHistoryEntity(
                    id = 0, // Auto-generated
                    deviceId = deviceId,
                    syncType = history.syncType,
                    syncStatus = history.syncStatus.name,
                    startedAt = history.startedAt ?: LocalDateTime.now(),
                    completedAt = history.completedAt,
                    itemsCount = history.itemsCount,
                    errorMessage = history.errorMessage,
                    createdAt = history.createdAt ?: LocalDateTime.now(),
                    modifiedAt = history.modifiedAt ?: LocalDateTime.now()
                )
            }
        
        return Triple(deviceEntity, settingEntities, newSyncHistoryEntities)
    }
}

/**
 * HealthGoalMapper - Maps between HealthGoal domain model and HealthGoalEntity
 */
@Singleton
class HealthGoalMapper @Inject constructor() : EntityMapper<HealthGoal, HealthGoalEntity> {
    
    override fun mapToDomain(entity: HealthGoalEntity): HealthGoal {
        return HealthGoal(
            id = entity.id,
            userId = entity.userId,
            title = entity.title,
            description = entity.description,
            metricType = MetricType.valueOf(entity.metricType),
            startValue = entity.startValue,
            targetValue = entity.targetValue,
            currentValue = entity.currentValue,
            isIncremental = entity.isIncremental,
            startDate = entity.startDate,
            deadline = entity.deadline,
            durationDays = entity.durationDays,
            category = entity.category,
            priority = entity.priority,
            isActive = entity.isActive,
            isCompleted = entity.isCompleted,
            completedAt = entity.completedAt,
            isRecurring = entity.isRecurring,
            recurringFrequency = entity.recurringFrequency,
            reminderTime = entity.reminderTime,
            reminderDays = entity.reminderDays?.split(",") ?: emptyList(),
            tags = entity.tags?.split(",") ?: emptyList(),
            createdAt = entity.createdAt,
            modifiedAt = entity.modifiedAt
        )
    }
    
    override fun mapToEntity(domain: HealthGoal): HealthGoalEntity {
        return HealthGoalEntity(
            id = domain.id.ifEmpty { UUID.randomUUID().toString() },
            userId = domain.userId,
            title = domain.title,
            description = domain.description,
            metricType = domain.metricType.name,
            startValue = domain.startValue,
            targetValue = domain.targetValue,
            currentValue = domain.currentValue,
            isIncremental = domain.isIncremental,
            startDate = domain.startDate,
            deadline = domain.deadline,
            durationDays = domain.durationDays,
            category = domain.category,
            priority = domain.priority,
            isActive = domain.isActive,
            isCompleted = domain.isCompleted,
            completedAt = domain.completedAt,
            isRecurring = domain.isRecurring,
            recurringFrequency = domain.recurringFrequency,
            reminderTime = domain.reminderTime,
            reminderDays = domain.reminderDays.joinToString(",").takeIf { it.isNotEmpty() },
            tags = domain.tags.joinToString(",").takeIf { it.isNotEmpty() },
            createdAt = domain.createdAt ?: LocalDateTime.now(),
            modifiedAt = domain.modifiedAt ?: LocalDateTime.now()
        )
    }
}

/**
 * HealthAlertMapper - Maps between HealthAlert domain model and HealthAlertEntity
 */
@Singleton
class HealthAlertMapper @Inject constructor() : EntityMapper<HealthAlert, HealthAlertEntity> {
    
    override fun mapToDomain(entity: HealthAlertEntity): HealthAlert {
        return HealthAlert(
            id = entity.id,
            userId = entity.userId,
            deviceId = entity.deviceId,
            ruleId = entity.ruleId,
            metricType = MetricType.valueOf(entity.metricType),
            timestamp = entity.timestamp,
            value = entity.value,
            severity = entity.severity,
            status = entity.status,
            title = entity.title,
            message = entity.message,
            requiresImmediate = entity.requiresImmediate,
            isClinicallySignificant = entity.isClinicallySignificant,
            requiresMedicalReview = entity.requiresMedicalReview,
            isNotified = entity.isNotified,
            notificationTime = entity.notificationTime,
            acknowledgedAt = entity.acknowledgedAt,
            resolvedAt = entity.resolvedAt,
            resolution = entity.resolution,
            escalatedAt = entity.escalatedAt,
            escalationLevel = entity.escalationLevel,
            escalatedToContactIds = entity.escalatedToContactIds?.split(",") ?: emptyList(),
            isMedicallyReviewed = entity.isMedicallyReviewed,
            medicallyReviewedAt = entity.medicallyReviewedAt,
            medicallyReviewedBy = entity.medicallyReviewedBy,
            medicalReviewNotes = entity.medicalReviewNotes,
            notes = entity.notes,
            createdAt = entity.createdAt,
            modifiedAt = entity.modifiedAt
        )
    }
    
    override fun mapToEntity(domain: HealthAlert): HealthAlertEntity {
        return HealthAlertEntity(
            id = domain.id.ifEmpty { UUID.randomUUID().toString() },
            userId = domain.userId,
            deviceId = domain.deviceId,
            ruleId = domain.ruleId,
            metricType = domain.metricType.name,
            timestamp = domain.timestamp,
            value = domain.value,
            severity = domain.severity,
            status = domain.status,
            title = domain.title,
            message = domain.message,
            requiresImmediate = domain.requiresImmediate,
            isClinicallySignificant = domain.isClinicallySignificant,
            requiresMedicalReview = domain.requiresMedicalReview,
            isNotified = domain.isNotified,
            notificationTime = domain.notificationTime,
            acknowledgedAt = domain.acknowledgedAt,
            resolvedAt = domain.resolvedAt,
            resolution = domain.resolution,
            escalatedAt = domain.escalatedAt,
            escalationLevel = domain.escalationLevel,
            escalatedToContactIds = domain.escalatedToContactIds.joinToString(",").takeIf { it.isNotEmpty() },
            isMedicallyReviewed = domain.isMedicallyReviewed,
            medicallyReviewedAt = domain.medicallyReviewedAt,
            medicallyReviewedBy = domain.medicallyReviewedBy,
            medicalReviewNotes = domain.medicalReviewNotes,
            notes = domain.notes,
            createdAt = domain.createdAt ?: LocalDateTime.now(),
            modifiedAt = domain.modifiedAt ?: LocalDateTime.now()
        )
    }
}

/**
 * Extension function to help with mapping lists of entities to domain models
 */
fun <T, E> List<E>.mapToDomainList(mapper: EntityMapper<T, E>): List<T> {
    return this.map { mapper.mapToDomain(it) }
}

/**
 * Extension function to help with mapping lists of domain models to entities
 */
fun <T, E> List<T>.mapToEntityList(mapper: EntityMapper<T, E>): List<E> {
    return this.map { mapper.mapToEntity(it) }
}

/**
 * Helper function to queue an entity for sync
 * This would be implemented in the repository
 */
fun queueForSync(entity: Any) {
    // Implementation would be in the repository
}

/**
 * Helper function to mark an entity as deleted for sync
 * This would be implemented in the repository
 */
fun markAsDeletedForSync(id: String, entityType: String) {
    // Implementation would be in the repository
}
