package com.sensacare.app.data.local.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sensacare.app.data.local.entity.ActivityType
import com.sensacare.app.data.local.entity.Intensity
import com.sensacare.app.data.local.entity.MetricType
import com.sensacare.app.data.local.entity.SleepStage
import com.sensacare.app.domain.model.SyncStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * LocalDateTimeConverter - Converts between LocalDateTime and String for Room database
 *
 * Uses ISO-8601 format for consistent date-time representation
 */
class LocalDateTimeConverter {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime?): String? {
        return value?.format(formatter)
    }

    @TypeConverter
    fun toLocalDateTime(value: String?): LocalDateTime? {
        return value?.let { LocalDateTime.parse(it, formatter) }
    }
}

/**
 * LocalDateConverter - Converts between LocalDate and String for Room database
 *
 * Uses ISO-8601 format for consistent date representation
 */
class LocalDateConverter {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE

    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? {
        return value?.format(formatter)
    }

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? {
        return value?.let { LocalDate.parse(it, formatter) }
    }
}

/**
 * LocalTimeConverter - Converts between LocalTime and String for Room database
 *
 * Uses ISO-8601 format for consistent time representation
 */
class LocalTimeConverter {
    private val formatter = DateTimeFormatter.ISO_LOCAL_TIME

    @TypeConverter
    fun fromLocalTime(value: LocalTime?): String? {
        return value?.format(formatter)
    }

    @TypeConverter
    fun toLocalTime(value: String?): LocalTime? {
        return value?.let { LocalTime.parse(it, formatter) }
    }
}

/**
 * StringListConverter - Converts between List<String> and String for Room database
 *
 * Uses comma-separated values for simple list representation
 */
class StringListConverter {
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.joinToString(",")
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.split(",")?.filter { it.isNotEmpty() }
    }
}

/**
 * SyncStatusConverter - Converts between SyncStatus enum and String for Room database
 */
class SyncStatusConverter {
    @TypeConverter
    fun fromSyncStatus(value: SyncStatus?): String? {
        return value?.name
    }

    @TypeConverter
    fun toSyncStatus(value: String?): SyncStatus? {
        return value?.let { SyncStatus.valueOf(it) }
    }
}

/**
 * MetricTypeConverter - Converts between MetricType enum and String for Room database
 */
class MetricTypeConverter {
    @TypeConverter
    fun fromMetricType(value: MetricType?): String? {
        return value?.name
    }

    @TypeConverter
    fun toMetricType(value: String?): MetricType? {
        return value?.let { MetricType.valueOf(it) }
    }
}

/**
 * ActivityTypeConverter - Converts between ActivityType enum and String for Room database
 */
class ActivityTypeConverter {
    @TypeConverter
    fun fromActivityType(value: ActivityType?): String? {
        return value?.name
    }

    @TypeConverter
    fun toActivityType(value: String?): ActivityType? {
        return value?.let { ActivityType.valueOf(it) }
    }
}

/**
 * IntensityConverter - Converts between Intensity enum and String for Room database
 */
class IntensityConverter {
    @TypeConverter
    fun fromIntensity(value: Intensity?): String? {
        return value?.name
    }

    @TypeConverter
    fun toIntensity(value: String?): Intensity? {
        return value?.let { Intensity.valueOf(it) }
    }
}

/**
 * SleepStageConverter - Converts between SleepStage enum and String for Room database
 */
class SleepStageConverter {
    @TypeConverter
    fun fromSleepStage(value: SleepStage?): String? {
        return value?.name
    }

    @TypeConverter
    fun toSleepStage(value: String?): SleepStage? {
        return value?.let { SleepStage.valueOf(it) }
    }
}

/**
 * JsonConverter - Converts between complex objects and JSON strings for Room database
 *
 * Uses Gson for JSON serialization/deserialization
 */
class JsonConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromMapToJson(map: Map<String, Any>?): String? {
        return map?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toMapFromJson(json: String?): Map<String, Any>? {
        if (json == null) return null
        val type = object : TypeToken<Map<String, Any>>() {}.type
        return gson.fromJson(json, type)
    }

    @TypeConverter
    fun fromListToJson(list: List<Any>?): String? {
        return list?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toListFromJson(json: String?): List<Any>? {
        if (json == null) return null
        val type = object : TypeToken<List<Any>>() {}.type
        return gson.fromJson(json, type)
    }

    @TypeConverter
    fun fromObjectToJson(obj: Any?): String? {
        return obj?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun <T> toObjectFromJson(json: String?, clazz: Class<T>): T? {
        return json?.let { gson.fromJson(it, clazz) }
    }
}
