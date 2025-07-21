package com.sensacare.app.data.local.entity

import androidx.room.*
import com.sensacare.app.domain.model.HealthData
import com.sensacare.app.domain.model.SyncStatus
import java.time.LocalDateTime
import java.util.UUID

/**
 * HealthDataEntity - Base entity for all health metrics
 *
 * This is the foundation entity for all health-related data in the SensaCare app.
 * It contains common fields that all health metrics share, such as:
 * - Unique identifier
 * - Timestamp of measurement
 * - User identifier (for multi-user support)
 * - Device source information
 * - Sync status (local, syncing, synced, error)
 * - Creation and modification metadata
 *
 * Other health-specific entities (heart rate, blood pressure, etc.) will
 * reference this entity through foreign keys or inherit from it.
 */
@Entity(
    tableName = "health_data",
    indices = [
        Index("timestamp"),
        Index("userId"),
        Index("deviceId"),
        Index("syncStatus"),
        Index("metricType")
    ]
)
data class HealthDataEntity(
    /**
     * Primary key - unique identifier for the health data record
     */
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    /**
     * Timestamp when the measurement was taken
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
     * Type of health metric (heart rate, blood pressure, etc.)
     */
    @ColumnInfo(name = "metricType")
    val metricType: String,

    /**
     * Value of the health metric (generic field, specific entities will have more detailed fields)
     */
    @ColumnInfo(name = "value")
    val value: Double,

    /**
     * Unit of measurement (bpm, mmHg, steps, etc.)
     */
    @ColumnInfo(name = "unit")
    val unit: String,

    /**
     * Optional notes about this measurement
     */
    @ColumnInfo(name = "notes")
    val notes: String? = null,

    /**
     * Tags for categorizing or filtering health data
     */
    @ColumnInfo(name = "tags")
    val tags: List<String>? = null,

    /**
     * Accuracy level of the measurement (0.0 to 1.0)
     */
    @ColumnInfo(name = "accuracy")
    val accuracy: Float? = null,

    /**
     * Sync status - tracks whether this data has been synced to the cloud
     */
    @ColumnInfo(name = "syncStatus")
    val syncStatus: String = SyncStatus.LOCAL.name,

    /**
     * Last sync attempt timestamp
     */
    @ColumnInfo(name = "lastSyncAttempt")
    val lastSyncAttempt: LocalDateTime? = null,

    /**
     * Sync error message, if any
     */
    @ColumnInfo(name = "syncErrorMessage")
    val syncErrorMessage: String? = null,

    /**
     * Remote ID from the backend after syncing
     */
    @ColumnInfo(name = "remoteId")
    val remoteId: String? = null,

    /**
     * Timestamp when this record was created locally
     */
    @ColumnInfo(name = "createdAt")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    /**
     * Timestamp when this record was last modified locally
     */
    @ColumnInfo(name = "modifiedAt")
    val modifiedAt: LocalDateTime = LocalDateTime.now(),

    /**
     * Version for optimistic locking and conflict resolution
     */
    @ColumnInfo(name = "version")
    val version: Long = 1
) {
    /**
     * Convert entity to domain model
     */
    fun toDomainModel(): HealthData {
        return HealthData(
            id = id,
            timestamp = timestamp,
            userId = userId,
            deviceId = deviceId,
            metricType = metricType,
            value = value,
            unit = unit,
            notes = notes,
            tags = tags ?: emptyList(),
            accuracy = accuracy ?: 1.0f,
            syncStatus = SyncStatus.valueOf(syncStatus),
            lastSyncAttempt = lastSyncAttempt,
            syncErrorMessage = syncErrorMessage,
            remoteId = remoteId,
            createdAt = createdAt,
            modifiedAt = modifiedAt,
            version = version
        )
    }

    companion object {
        /**
         * Convert domain model to entity
         */
        fun fromDomainModel(domainModel: HealthData): HealthDataEntity {
            return HealthDataEntity(
                id = domainModel.id,
                timestamp = domainModel.timestamp,
                userId = domainModel.userId,
                deviceId = domainModel.deviceId,
                metricType = domainModel.metricType,
                value = domainModel.value,
                unit = domainModel.unit,
                notes = domainModel.notes,
                tags = domainModel.tags,
                accuracy = domainModel.accuracy,
                syncStatus = domainModel.syncStatus.name,
                lastSyncAttempt = domainModel.lastSyncAttempt,
                syncErrorMessage = domainModel.syncErrorMessage,
                remoteId = domainModel.remoteId,
                createdAt = domainModel.createdAt,
                modifiedAt = domainModel.modifiedAt,
                version = domainModel.version
            )
        }
    }
}

/**
 * Enum class defining the possible metric types
 * This helps ensure consistency across the app
 */
enum class MetricType {
    HEART_RATE,
    BLOOD_PRESSURE,
    SLEEP,
    ACTIVITY,
    STEPS,
    DISTANCE,
    CALORIES,
    STRESS,
    WEIGHT,
    WATER_INTAKE,
    OXYGEN_SATURATION,
    TEMPERATURE,
    GLUCOSE,
    CUSTOM
}
