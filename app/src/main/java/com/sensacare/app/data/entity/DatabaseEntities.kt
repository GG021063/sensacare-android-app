package com.sensacare.app.data.entity

import androidx.room.*
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Base entity interface with common properties for all entities
 */
interface BaseEntity {
    val id: Long
    val createdAt: Long
    val updatedAt: Long
    val syncStatus: SyncStatus
    val version: Int
}

/**
 * Sync status enum for tracking synchronization state
 */
enum class SyncStatus {
    PENDING,
    SYNCING,
    SYNCED,
    FAILED,
    NOT_SYNCED
}

/**
 * Device entity for storing paired device information
 */
@Entity(
    tableName = "devices",
    indices = [
        Index(value = ["macAddress"], unique = true),
        Index(value = ["name"])
    ]
)
data class DeviceEntity(
    @PrimaryKey(autoGenerate = true)
    override val id: Long = 0,
    val name: String,
    val macAddress: String,
    val deviceType: String,
    val firmwareVersion: String,
    val hardwareVersion: String,
    val lastConnected: Long? = null,
    val batteryLevel: Int? = null,
    val isActive: Boolean = true,
    val features: String = "", // Comma-separated list of supported features
    override val createdAt: Long = System.currentTimeMillis(),
    override val updatedAt: Long = System.currentTimeMillis(),
    override val syncStatus: SyncStatus = SyncStatus.NOT_SYNCED,
    override val version: Int = 1
) : BaseEntity

/**
 * User entity for storing user profile information
 */
@Entity(
    tableName = "users",
    indices = [
        Index(value = ["email"], unique = true)
    ]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    override val id: Long = 0,
    val name: String,
    val email: String,
    val dateOfBirth: Long? = null,
    val gender: String? = null,
    val heightCm: Int? = null,
    val weightKg: Float? = null,
    val stepLengthCm: Int? = null,
    val targetSteps: Int = 10000,
    val profileImageUri: String? = null,
    val preferredDeviceId: Long? = null,
    override val createdAt: Long = System.currentTimeMillis(),
    override val updatedAt: Long = System.currentTimeMillis(),
    override val syncStatus: SyncStatus = SyncStatus.NOT_SYNCED,
    override val version: Int = 1
) : BaseEntity

/**
 * Heart rate entity for storing heart rate measurements
 */
@Entity(
    tableName = "heart_rates",
    indices = [
        Index(value = ["deviceId"]),
        Index(value = ["timestamp"]),
        Index(value = ["date"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = DeviceEntity::class,
            parentColumns = ["id"],
            childColumns = ["deviceId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class HeartRateEntity(
    @PrimaryKey(autoGenerate = true)
    override val id: Long = 0,
    val deviceId: Long,
    val timestamp: Long,
    val date: String, // YYYY-MM-DD format for easy querying
    val heartRate: Int,
    val status: String, // NORMAL, EXERCISE, RESTING, UNKNOWN
    val isManualEntry: Boolean = false,
    val notes: String? = null,
    override val createdAt: Long = System.currentTimeMillis(),
    override val updatedAt: Long = System.currentTimeMillis(),
    override val syncStatus: SyncStatus = SyncStatus.PENDING,
    override val version: Int = 1
) : BaseEntity {
    companion object {
        fun fromTimestamp(timestamp: Long): String {
            val instant = Instant.ofEpochMilli(timestamp)
            val localDate = LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).toLocalDate()
            return localDate.toString()
        }
    }
}

/**
 * Blood oxygen entity for storing SpO2 measurements
 */
@Entity(
    tableName = "blood_oxygen",
    indices = [
        Index(value = ["deviceId"]),
        Index(value = ["timestamp"]),
        Index(value = ["date"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = DeviceEntity::class,
            parentColumns = ["id"],
            childColumns = ["deviceId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class BloodOxygenEntity(
    @PrimaryKey(autoGenerate = true)
    override val id: Long = 0,
    val deviceId: Long,
    val timestamp: Long,
    val date: String, // YYYY-MM-DD format for easy querying
    val bloodOxygen: Int, // SpO2 percentage (0-100)
    val isManualEntry: Boolean = false,
    val notes: String? = null,
    override val createdAt: Long = System.currentTimeMillis(),
    override val updatedAt: Long = System.currentTimeMillis(),
    override val syncStatus: SyncStatus = SyncStatus.PENDING,
    override val version: Int = 1
) : BaseEntity {
    companion object {
        fun fromTimestamp(timestamp: Long): String {
            val instant = Instant.ofEpochMilli(timestamp)
            val localDate = LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).toLocalDate()
            return localDate.toString()
        }
    }
}

/**
 * Step data entity for storing daily step counts and activity
 */
@Entity(
    tableName = "steps",
    indices = [
        Index(value = ["deviceId"]),
        Index(value = ["timestamp"]),
        Index(value = ["date"], unique = true)
    ],
    foreignKeys = [
        ForeignKey(
            entity = DeviceEntity::class,
            parentColumns = ["id"],
            childColumns = ["deviceId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class StepEntity(
    @PrimaryKey(autoGenerate = true)
    override val id: Long = 0,
    val deviceId: Long,
    val timestamp: Long,
    val date: String, // YYYY-MM-DD format for easy querying
    val steps: Int,
    val distance: Float, // in meters
    val calories: Float, // in kcal
    val activeMinutes: Int = 0,
    val goalSteps: Int = 10000,
    val goalAchieved: Boolean = false,
    val isManualEntry: Boolean = false,
    override val createdAt: Long = System.currentTimeMillis(),
    override val updatedAt: Long = System.currentTimeMillis(),
    override val syncStatus: SyncStatus = SyncStatus.PENDING,
    override val version: Int = 1
) : BaseEntity {
    companion object {
        fun fromTimestamp(timestamp: Long): String {
            val instant = Instant.ofEpochMilli(timestamp)
            val localDate = LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).toLocalDate()
            return localDate.toString()
        }
    }
}

/**
 * Hourly step data for detailed activity tracking throughout the day
 */
@Entity(
    tableName = "hourly_steps",
    indices = [
        Index(value = ["deviceId"]),
        Index(value = ["timestamp"]),
        Index(value = ["date"]),
        Index(value = ["hour"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = DeviceEntity::class,
            parentColumns = ["id"],
            childColumns = ["deviceId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class HourlyStepEntity(
    @PrimaryKey(autoGenerate = true)
    override val id: Long = 0,
    val deviceId: Long,
    val timestamp: Long,
    val date: String, // YYYY-MM-DD format
    val hour: Int, // 0-23
    val steps: Int,
    val distance: Float, // in meters
    val calories: Float, // in kcal
    override val createdAt: Long = System.currentTimeMillis(),
    override val updatedAt: Long = System.currentTimeMillis(),
    override val syncStatus: SyncStatus = SyncStatus.PENDING,
    override val version: Int = 1
) : BaseEntity

/**
 * Sleep data entity for storing sleep records
 */
@Entity(
    tableName = "sleep",
    indices = [
        Index(value = ["deviceId"]),
        Index(value = ["date"], unique = true)
    ],
    foreignKeys = [
        ForeignKey(
            entity = DeviceEntity::class,
            parentColumns = ["id"],
            childColumns = ["deviceId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SleepEntity(
    @PrimaryKey(autoGenerate = true)
    override val id: Long = 0,
    val deviceId: Long,
    val date: String, // YYYY-MM-DD format for the night of sleep
    val startTime: Long, // Sleep start timestamp
    val endTime: Long, // Sleep end timestamp
    val deepSleepMinutes: Int,
    val lightSleepMinutes: Int,
    val remSleepMinutes: Int = 0,
    val awakeSleepMinutes: Int,
    val totalSleepMinutes: Int,
    val sleepQuality: Int, // 0-100
    val isManualEntry: Boolean = false,
    val notes: String? = null,
    override val createdAt: Long = System.currentTimeMillis(),
    override val updatedAt: Long = System.currentTimeMillis(),
    override val syncStatus: SyncStatus = SyncStatus.PENDING,
    override val version: Int = 1
) : BaseEntity

/**
 * Sleep detail entity for storing detailed sleep stages
 */
@Entity(
    tableName = "sleep_details",
    indices = [
        Index(value = ["sleepId"]),
        Index(value = ["timestamp"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = SleepEntity::class,
            parentColumns = ["id"],
            childColumns = ["sleepId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SleepDetailEntity(
    @PrimaryKey(autoGenerate = true)
    override val id: Long = 0,
    val sleepId: Long,
    val timestamp: Long,
    val sleepType: String, // DEEP_SLEEP, LIGHT_SLEEP, REM_SLEEP, AWAKE, UNKNOWN
    val durationMinutes: Int = 5, // Default 5-minute intervals
    override val createdAt: Long = System.currentTimeMillis(),
    override val updatedAt: Long = System.currentTimeMillis(),
    override val syncStatus: SyncStatus = SyncStatus.PENDING,
    override val version: Int = 1
) : BaseEntity

/**
 * Blood pressure entity for storing blood pressure measurements
 */
@Entity(
    tableName = "blood_pressure",
    indices = [
        Index(value = ["deviceId"]),
        Index(value = ["timestamp"]),
        Index(value = ["date"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = DeviceEntity::class,
            parentColumns = ["id"],
            childColumns = ["deviceId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class BloodPressureEntity(
    @PrimaryKey(autoGenerate = true)
    override val id: Long = 0,
    val deviceId: Long,
    val timestamp: Long,
    val date: String, // YYYY-MM-DD format
    val systolic: Int, // mmHg
    val diastolic: Int, // mmHg
    val heartRate: Int? = null,
    val isManualEntry: Boolean = false,
    val notes: String? = null,
    override val createdAt: Long = System.currentTimeMillis(),
    override val updatedAt: Long = System.currentTimeMillis(),
    override val syncStatus: SyncStatus = SyncStatus.PENDING,
    override val version: Int = 1
) : BaseEntity {
    companion object {
        fun fromTimestamp(timestamp: Long): String {
            val instant = Instant.ofEpochMilli(timestamp)
            val localDate = LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).toLocalDate()
            return localDate.toString()
        }
    }
}

/**
 * Temperature entity for storing body temperature measurements
 */
@Entity(
    tableName = "temperature",
    indices = [
        Index(value = ["deviceId"]),
        Index(value = ["timestamp"]),
        Index(value = ["date"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = DeviceEntity::class,
            parentColumns = ["id"],
            childColumns = ["deviceId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TemperatureEntity(
    @PrimaryKey(autoGenerate = true)
    override val id: Long = 0,
    val deviceId: Long,
    val timestamp: Long,
    val date: String, // YYYY-MM-DD format
    val temperatureCelsius: Float,
    val bodyLocation: String = "wrist",
    val isManualEntry: Boolean = false,
    val notes: String? = null,
    override val createdAt: Long = System.currentTimeMillis(),
    override val updatedAt: Long = System.currentTimeMillis(),
    override val syncStatus: SyncStatus = SyncStatus.PENDING,
    override val version: Int = 1
) : BaseEntity {
    companion object {
        fun fromTimestamp(timestamp: Long): String {
            val instant = Instant.ofEpochMilli(timestamp)
            val localDate = LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).toLocalDate()
            return localDate.toString()
        }
    }
}

/**
 * Activity entity for storing workout/exercise sessions
 */
@Entity(
    tableName = "activities",
    indices = [
        Index(value = ["deviceId"]),
        Index(value = ["startTime"]),
        Index(value = ["date"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = DeviceEntity::class,
            parentColumns = ["id"],
            childColumns = ["deviceId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ActivityEntity(
    @PrimaryKey(autoGenerate = true)
    override val id: Long = 0,
    val deviceId: Long,
    val activityType: String, // WALKING, RUNNING, CYCLING, etc.
    val date: String, // YYYY-MM-DD format
    val startTime: Long,
    val endTime: Long,
    val durationSeconds: Int,
    val steps: Int? = null,
    val distance: Float? = null, // in meters
    val calories: Float? = null, // in kcal
    val avgHeartRate: Int? = null,
    val maxHeartRate: Int? = null,
    val avgPace: Float? = null, // in min/km
    val isManualEntry: Boolean = false,
    val notes: String? = null,
    override val createdAt: Long = System.currentTimeMillis(),
    override val updatedAt: Long = System.currentTimeMillis(),
    override val syncStatus: SyncStatus = SyncStatus.PENDING,
    override val version: Int = 1
) : BaseEntity {
    companion object {
        fun fromTimestamp(timestamp: Long): String {
            val instant = Instant.ofEpochMilli(timestamp)
            val localDate = LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).toLocalDate()
            return localDate.toString()
        }
    }
}

/**
 * Sync status entity for tracking synchronization status
 */
@Entity(
    tableName = "sync_status",
    indices = [
        Index(value = ["deviceId"]),
        Index(value = ["dataType"]),
        Index(value = ["lastSyncAttempt"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = DeviceEntity::class,
            parentColumns = ["id"],
            childColumns = ["deviceId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SyncStatusEntity(
    @PrimaryKey(autoGenerate = true)
    override val id: Long = 0,
    val deviceId: Long,
    val dataType: String, // HEART_RATE, STEPS, SLEEP, etc.
    val lastSyncSuccess: Long? = null,
    val lastSyncAttempt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val errorMessage: String? = null,
    val retryCount: Int = 0,
    val nextSyncAttempt: Long? = null,
    override val createdAt: Long = System.currentTimeMillis(),
    override val updatedAt: Long = System.currentTimeMillis(),
    override val version: Int = 1
) : BaseEntity {
    // Override syncStatus to use the same property name
    override val syncStatus: SyncStatus
        get() = syncStatus
}

/**
 * Notification entity for storing device notifications
 */
@Entity(
    tableName = "notifications",
    indices = [
        Index(value = ["deviceId"]),
        Index(value = ["timestamp"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = DeviceEntity::class,
            parentColumns = ["id"],
            childColumns = ["deviceId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    override val id: Long = 0,
    val deviceId: Long,
    val type: String, // CALL, MESSAGE, etc.
    val title: String,
    val content: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val isDelivered: Boolean = false,
    override val createdAt: Long = System.currentTimeMillis(),
    override val updatedAt: Long = System.currentTimeMillis(),
    override val syncStatus: SyncStatus = SyncStatus.PENDING,
    override val version: Int = 1
) : BaseEntity

/**
 * Settings entity for storing app and device settings
 */
@Entity(
    tableName = "settings",
    indices = [
        Index(value = ["key"], unique = true)
    ]
)
data class SettingEntity(
    @PrimaryKey(autoGenerate = true)
    override val id: Long = 0,
    val key: String,
    val value: String,
    val deviceId: Long? = null, // Null for app-wide settings
    val category: String? = null,
    override val createdAt: Long = System.currentTimeMillis(),
    override val updatedAt: Long = System.currentTimeMillis(),
    override val syncStatus: SyncStatus = SyncStatus.NOT_SYNCED,
    override val version: Int = 1
) : BaseEntity

/**
 * Health insights entity for storing calculated health insights and trends
 */
@Entity(
    tableName = "health_insights",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["date"]),
        Index(value = ["insightType"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class HealthInsightEntity(
    @PrimaryKey(autoGenerate = true)
    override val id: Long = 0,
    val userId: Long,
    val date: String, // YYYY-MM-DD format
    val insightType: String, // SLEEP_QUALITY, RESTING_HEART_RATE, ACTIVITY_LEVEL, etc.
    val score: Int? = null, // 0-100 score if applicable
    val value: Float? = null, // Numerical value if applicable
    val trend: String? = null, // IMPROVING, DECLINING, STABLE
    val message: String? = null, // User-friendly message
    val severity: String? = null, // INFO, WARNING, ALERT
    override val createdAt: Long = System.currentTimeMillis(),
    override val updatedAt: Long = System.currentTimeMillis(),
    override val syncStatus: SyncStatus = SyncStatus.NOT_SYNCED,
    override val version: Int = 1
) : BaseEntity
