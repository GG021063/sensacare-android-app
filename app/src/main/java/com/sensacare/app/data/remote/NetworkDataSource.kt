package com.sensacare.app.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.annotations.SerializedName
import com.sensacare.app.data.local.entity.ActivityType
import com.sensacare.app.data.local.entity.MetricType
import com.sensacare.app.data.local.entity.SleepStage
import com.sensacare.app.data.remote.model.*
import com.sensacare.app.domain.model.*
import com.sensacare.app.domain.util.Result
import com.sensacare.app.domain.util.SyncStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import timber.log.Timber
import java.io.IOException
import java.lang.reflect.Type
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Network models for requests and responses
 */
package com.sensacare.app.data.remote.model

/**
 * Base response model with common fields
 */
data class ApiResponse<T>(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: T? = null,
    @SerializedName("errors") val errors: List<ApiError>? = null,
    @SerializedName("timestamp") val timestamp: String? = null
)

/**
 * API Error model
 */
data class ApiError(
    @SerializedName("code") val code: String,
    @SerializedName("message") val message: String,
    @SerializedName("field") val field: String? = null,
    @SerializedName("details") val details: Map<String, Any>? = null
)

/**
 * Authentication models
 */
data class AuthRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("deviceId") val deviceId: String? = null
)

data class AuthResponse(
    @SerializedName("userId") val userId: String,
    @SerializedName("email") val email: String,
    @SerializedName("firstName") val firstName: String? = null,
    @SerializedName("lastName") val lastName: String? = null,
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("refreshToken") val refreshToken: String,
    @SerializedName("expiresIn") val expiresIn: Long
)

data class RefreshTokenRequest(
    @SerializedName("refreshToken") val refreshToken: String
)

data class PasswordResetRequest(
    @SerializedName("email") val email: String
)

/**
 * Sync models
 */
data class SyncRequest(
    @SerializedName("userId") val userId: String,
    @SerializedName("deviceId") val deviceId: String? = null,
    @SerializedName("lastSyncTimestamp") val lastSyncTimestamp: String? = null,
    @SerializedName("dataType") val dataType: String,
    @SerializedName("items") val items: List<Map<String, Any>>,
    @SerializedName("deletedItemIds") val deletedItemIds: List<String>? = null
)

data class SyncResponse(
    @SerializedName("syncId") val syncId: String,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("dataType") val dataType: String,
    @SerializedName("itemsProcessed") val itemsProcessed: Int,
    @SerializedName("itemsFailed") val itemsFailed: Int,
    @SerializedName("failedItemIds") val failedItemIds: List<String>? = null,
    @SerializedName("serverItems") val serverItems: List<Map<String, Any>>? = null,
    @SerializedName("serverDeletedItemIds") val serverDeletedItemIds: List<String>? = null
)

/**
 * Health data models
 */
data class HealthDataDto(
    @SerializedName("id") val id: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("deviceId") val deviceId: String? = null,
    @SerializedName("metricType") val metricType: String,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("value") val value: Double,
    @SerializedName("unit") val unit: String? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("tags") val tags: List<String>? = null,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("modifiedAt") val modifiedAt: String
)

data class HeartRateDto(
    @SerializedName("id") val id: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("deviceId") val deviceId: String? = null,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("value") val value: Int,
    @SerializedName("restingHeartRate") val restingHeartRate: Int? = null,
    @SerializedName("isRestingHeartRate") val isRestingHeartRate: Boolean = false,
    @SerializedName("hrvValue") val hrvValue: Int? = null,
    @SerializedName("activityLevel") val activityLevel: String? = null,
    @SerializedName("zoneInfo") val zoneInfo: String? = null,
    @SerializedName("abnormalityDetected") val abnormalityDetected: Boolean = false,
    @SerializedName("abnormalityType") val abnormalityType: String? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("tags") val tags: List<String>? = null,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("modifiedAt") val modifiedAt: String
)

data class BloodPressureDto(
    @SerializedName("id") val id: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("deviceId") val deviceId: String? = null,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("systolic") val systolic: Int,
    @SerializedName("diastolic") val diastolic: Int,
    @SerializedName("pulse") val pulse: Int? = null,
    @SerializedName("bodyPosition") val bodyPosition: String? = null,
    @SerializedName("armPosition") val armPosition: String? = null,
    @SerializedName("measurementContext") val measurementContext: String? = null,
    @SerializedName("classification") val classification: String? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("tags") val tags: List<String>? = null,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("modifiedAt") val modifiedAt: String
)

data class SleepDto(
    @SerializedName("id") val id: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("deviceId") val deviceId: String? = null,
    @SerializedName("startTime") val startTime: String,
    @SerializedName("endTime") val endTime: String,
    @SerializedName("durationMinutes") val durationMinutes: Int,
    @SerializedName("sleepScore") val sleepScore: Int? = null,
    @SerializedName("sleepEfficiency") val sleepEfficiency: Double? = null,
    @SerializedName("deepSleepMinutes") val deepSleepMinutes: Int? = null,
    @SerializedName("lightSleepMinutes") val lightSleepMinutes: Int? = null,
    @SerializedName("remSleepMinutes") val remSleepMinutes: Int? = null,
    @SerializedName("awakeMinutes") val awakeMinutes: Int? = null,
    @SerializedName("sleepLatencyMinutes") val sleepLatencyMinutes: Int? = null,
    @SerializedName("wakeCount") val wakeCount: Int? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("tags") val tags: List<String>? = null,
    @SerializedName("stages") val stages: List<SleepStageDto>? = null,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("modifiedAt") val modifiedAt: String
)

data class SleepStageDto(
    @SerializedName("id") val id: String,
    @SerializedName("sleepId") val sleepId: String,
    @SerializedName("stage") val stage: String,
    @SerializedName("startTime") val startTime: String,
    @SerializedName("endTime") val endTime: String,
    @SerializedName("durationMinutes") val durationMinutes: Int
)

data class ActivityDto(
    @SerializedName("id") val id: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("deviceId") val deviceId: String? = null,
    @SerializedName("activityType") val activityType: String,
    @SerializedName("startTime") val startTime: String,
    @SerializedName("endTime") val endTime: String,
    @SerializedName("durationSeconds") val durationSeconds: Int,
    @SerializedName("distance") val distance: Double? = null,
    @SerializedName("calories") val calories: Double? = null,
    @SerializedName("steps") val steps: Int? = null,
    @SerializedName("avgHeartRate") val avgHeartRate: Int? = null,
    @SerializedName("maxHeartRate") val maxHeartRate: Int? = null,
    @SerializedName("avgIntensity") val avgIntensity: String? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("tags") val tags: List<String>? = null,
    @SerializedName("sessions") val sessions: List<ActivitySessionDto>? = null,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("modifiedAt") val modifiedAt: String
)

data class ActivitySessionDto(
    @SerializedName("id") val id: String,
    @SerializedName("activityId") val activityId: String,
    @SerializedName("startTime") val startTime: String,
    @SerializedName("endTime") val endTime: String,
    @SerializedName("durationSeconds") val durationSeconds: Int,
    @SerializedName("distance") val distance: Double? = null,
    @SerializedName("calories") val calories: Double? = null,
    @SerializedName("steps") val steps: Int? = null,
    @SerializedName("avgHeartRate") val avgHeartRate: Int? = null,
    @SerializedName("maxHeartRate") val maxHeartRate: Int? = null,
    @SerializedName("intensity") val intensity: String,
    @SerializedName("location") val location: String? = null,
    @SerializedName("elevationGain") val elevationGain: Double? = null,
    @SerializedName("elevationLoss") val elevationLoss: Double? = null
)

/**
 * Device models
 */
data class DeviceDto(
    @SerializedName("id") val id: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("name") val name: String,
    @SerializedName("manufacturer") val manufacturer: String? = null,
    @SerializedName("model") val model: String? = null,
    @SerializedName("serialNumber") val serialNumber: String? = null,
    @SerializedName("firmwareVersion") val firmwareVersion: String? = null,
    @SerializedName("batteryLevel") val batteryLevel: Int? = null,
    @SerializedName("batteryLastUpdatedAt") val batteryLastUpdatedAt: String? = null,
    @SerializedName("isConnected") val isConnected: Boolean = false,
    @SerializedName("lastConnectedAt") val lastConnectedAt: String? = null,
    @SerializedName("lastSyncAt") val lastSyncAt: String? = null,
    @SerializedName("syncStatus") val syncStatus: String? = null,
    @SerializedName("capabilities") val capabilities: List<String>? = null,
    @SerializedName("settings") val settings: List<DeviceSettingDto>? = null,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("modifiedAt") val modifiedAt: String
)

data class DeviceSettingDto(
    @SerializedName("id") val id: String,
    @SerializedName("deviceId") val deviceId: String,
    @SerializedName("key") val key: String,
    @SerializedName("value") val value: String,
    @SerializedName("dataType") val dataType: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("modifiedAt") val modifiedAt: String
)

/**
 * Goal models
 */
data class HealthGoalDto(
    @SerializedName("id") val id: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("metricType") val metricType: String,
    @SerializedName("startValue") val startValue: Double,
    @SerializedName("targetValue") val targetValue: Double,
    @SerializedName("currentValue") val currentValue: Double? = null,
    @SerializedName("isIncremental") val isIncremental: Boolean = true,
    @SerializedName("startDate") val startDate: String,
    @SerializedName("deadline") val deadline: String? = null,
    @SerializedName("durationDays") val durationDays: Int? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("priority") val priority: String? = null,
    @SerializedName("isActive") val isActive: Boolean = true,
    @SerializedName("isCompleted") val isCompleted: Boolean = false,
    @SerializedName("completedAt") val completedAt: String? = null,
    @SerializedName("isRecurring") val isRecurring: Boolean = false,
    @SerializedName("recurringFrequency") val recurringFrequency: String? = null,
    @SerializedName("reminderTime") val reminderTime: String? = null,
    @SerializedName("reminderDays") val reminderDays: List<String>? = null,
    @SerializedName("tags") val tags: List<String>? = null,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("modifiedAt") val modifiedAt: String
)

data class GoalProgressDto(
    @SerializedName("id") val id: String,
    @SerializedName("goalId") val goalId: String,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("currentValue") val currentValue: Double,
    @SerializedName("progressPercentage") val progressPercentage: Double,
    @SerializedName("isCompleted") val isCompleted: Boolean = false,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("modifiedAt") val modifiedAt: String
)

/**
 * Alert models
 */
data class HealthAlertDto(
    @SerializedName("id") val id: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("deviceId") val deviceId: String? = null,
    @SerializedName("ruleId") val ruleId: String? = null,
    @SerializedName("metricType") val metricType: String,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("value") val value: Double,
    @SerializedName("severity") val severity: String,
    @SerializedName("status") val status: String,
    @SerializedName("title") val title: String,
    @SerializedName("message") val message: String,
    @SerializedName("requiresImmediate") val requiresImmediate: Boolean = false,
    @SerializedName("isClinicallySignificant") val isClinicallySignificant: Boolean = false,
    @SerializedName("requiresMedicalReview") val requiresMedicalReview: Boolean = false,
    @SerializedName("isNotified") val isNotified: Boolean = false,
    @SerializedName("notificationTime") val notificationTime: String? = null,
    @SerializedName("acknowledgedAt") val acknowledgedAt: String? = null,
    @SerializedName("resolvedAt") val resolvedAt: String? = null,
    @SerializedName("resolution") val resolution: String? = null,
    @SerializedName("escalatedAt") val escalatedAt: String? = null,
    @SerializedName("escalationLevel") val escalationLevel: Int? = null,
    @SerializedName("escalatedToContactIds") val escalatedToContactIds: List<String>? = null,
    @SerializedName("isMedicallyReviewed") val isMedicallyReviewed: Boolean = false,
    @SerializedName("medicallyReviewedAt") val medicallyReviewedAt: String? = null,
    @SerializedName("medicallyReviewedBy") val medicallyReviewedBy: String? = null,
    @SerializedName("medicalReviewNotes") val medicalReviewNotes: String? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("modifiedAt") val modifiedAt: String
)

data class AlertRuleDto(
    @SerializedName("id") val id: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("metricType") val metricType: String,
    @SerializedName("conditionType") val conditionType: String,
    @SerializedName("thresholdValue") val thresholdValue: Double,
    @SerializedName("secondaryThresholdValue") val secondaryThresholdValue: Double? = null,
    @SerializedName("severity") val severity: String,
    @SerializedName("isActive") val isActive: Boolean = true,
    @SerializedName("priority") val priority: Int = 0,
    @SerializedName("message") val message: String? = null,
    @SerializedName("requiresImmediate") val requiresImmediate: Boolean = false,
    @SerializedName("isClinicallySignificant") val isClinicallySignificant: Boolean = false,
    @SerializedName("requiresMedicalReview") val requiresMedicalReview: Boolean = false,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("modifiedAt") val modifiedAt: String
)

/**
 * Custom type adapters for LocalDateTime and other complex types
 */
class LocalDateTimeAdapter : JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    override fun serialize(
        src: LocalDateTime?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        return JsonPrimitive(src?.format(formatter))
    }

    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): LocalDateTime? {
        return json?.asString?.let { LocalDateTime.parse(it, formatter) }
    }
}

class LocalDateAdapter : JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE

    override fun serialize(
        src: LocalDate?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        return JsonPrimitive(src?.format(formatter))
    }

    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): LocalDate? {
        return json?.asString?.let { LocalDate.parse(it, formatter) }
    }
}

/**
 * API interface with Retrofit annotations
 */
interface SensaCareApi {
    /**
     * Authentication endpoints
     */
    @POST("auth/login")
    suspend fun login(@Body request: AuthRequest): Response<ApiResponse<AuthResponse>>

    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<ApiResponse<AuthResponse>>

    @POST("auth/password-reset")
    suspend fun requestPasswordReset(@Body request: PasswordResetRequest): Response<ApiResponse<Unit>>

    /**
     * Health data synchronization endpoints
     */
    @POST("sync")
    suspend fun syncData(@Body request: SyncRequest): Response<ApiResponse<SyncResponse>>

    @GET("health-data")
    suspend fun getHealthData(
        @Query("userId") userId: String,
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String,
        @Query("metricType") metricType: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null
    ): Response<ApiResponse<List<HealthDataDto>>>

    @GET("heart-rate")
    suspend fun getHeartRates(
        @Query("userId") userId: String,
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null
    ): Response<ApiResponse<List<HeartRateDto>>>

    @GET("blood-pressure")
    suspend fun getBloodPressures(
        @Query("userId") userId: String,
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null
    ): Response<ApiResponse<List<BloodPressureDto>>>

    @GET("sleep")
    suspend fun getSleeps(
        @Query("userId") userId: String,
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null
    ): Response<ApiResponse<List<SleepDto>>>

    @GET("activity")
    suspend fun getActivities(
        @Query("userId") userId: String,
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String,
        @Query("activityType") activityType: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null
    ): Response<ApiResponse<List<ActivityDto>>>

    /**
     * Device management endpoints
     */
    @GET("devices")
    suspend fun getDevices(
        @Query("userId") userId: String
    ): Response<ApiResponse<List<DeviceDto>>>

    @GET("devices/{deviceId}")
    suspend fun getDevice(
        @Path("deviceId") deviceId: String
    ): Response<ApiResponse<DeviceDto>>

    @POST("devices")
    suspend fun registerDevice(
        @Body device: DeviceDto
    ): Response<ApiResponse<DeviceDto>>

    @PUT("devices/{deviceId}")
    suspend fun updateDevice(
        @Path("deviceId") deviceId: String,
        @Body device: DeviceDto
    ): Response<ApiResponse<DeviceDto>>

    @DELETE("devices/{deviceId}")
    suspend fun deleteDevice(
        @Path("deviceId") deviceId: String
    ): Response<ApiResponse<Unit>>

    @POST("devices/{deviceId}/sync-history")
    suspend fun recordDeviceSync(
        @Path("deviceId") deviceId: String,
        @Body syncHistory: Map<String, Any>
    ): Response<ApiResponse<Map<String, Any>>>

    /**
     * Goal and alert endpoints
     */
    @GET("goals")
    suspend fun getGoals(
        @Query("userId") userId: String,
        @Query("isActive") isActive: Boolean? = null,
        @Query("isCompleted") isCompleted: Boolean? = null,
        @Query("metricType") metricType: String? = null
    ): Response<ApiResponse<List<HealthGoalDto>>>

    @GET("goals/{goalId}")
    suspend fun getGoal(
        @Path("goalId") goalId: String
    ): Response<ApiResponse<HealthGoalDto>>

    @POST("goals")
    suspend fun createGoal(
        @Body goal: HealthGoalDto
    ): Response<ApiResponse<HealthGoalDto>>

    @PUT("goals/{goalId}")
    suspend fun updateGoal(
        @Path("goalId") goalId: String,
        @Body goal: HealthGoalDto
    ): Response<ApiResponse<HealthGoalDto>>

    @DELETE("goals/{goalId}")
    suspend fun deleteGoal(
        @Path("goalId") goalId: String
    ): Response<ApiResponse<Unit>>

    @GET("goals/{goalId}/progress")
    suspend fun getGoalProgress(
        @Path("goalId") goalId: String
    ): Response<ApiResponse<List<GoalProgressDto>>>

    @POST("goals/{goalId}/progress")
    suspend fun recordGoalProgress(
        @Path("goalId") goalId: String,
        @Body progress: GoalProgressDto
    ): Response<ApiResponse<GoalProgressDto>>

    @GET("alerts")
    suspend fun getAlerts(
        @Query("userId") userId: String,
        @Query("status") status: String? = null,
        @Query("severity") severity: String? = null,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null
    ): Response<ApiResponse<List<HealthAlertDto>>>

    @GET("alerts/{alertId}")
    suspend fun getAlert(
        @Path("alertId") alertId: String
    ): Response<ApiResponse<HealthAlertDto>>

    @POST("alerts")
    suspend fun createAlert(
        @Body alert: HealthAlertDto
    ): Response<ApiResponse<HealthAlertDto>>

    @PUT("alerts/{alertId}")
    suspend fun updateAlert(
        @Path("alertId") alertId: String,
        @Body alert: HealthAlertDto
    ): Response<ApiResponse<HealthAlertDto>>

    @PUT("alerts/{alertId}/acknowledge")
    suspend fun acknowledgeAlert(
        @Path("alertId") alertId: String,
        @Body data: Map<String, String?>
    ): Response<ApiResponse<HealthAlertDto>>

    @PUT("alerts/{alertId}/resolve")
    suspend fun resolveAlert(
        @Path("alertId") alertId: String,
        @Body data: Map<String, String>
    ): Response<ApiResponse<HealthAlertDto>>

    @GET("alert-rules")
    suspend fun getAlertRules(
        @Query("userId") userId: String,
        @Query("isActive") isActive: Boolean? = null,
        @Query("metricType") metricType: String? = null
    ): Response<ApiResponse<List<AlertRuleDto>>>

    @POST("alert-rules")
    suspend fun createAlertRule(
        @Body rule: AlertRuleDto
    ): Response<ApiResponse<AlertRuleDto>>

    @PUT("alert-rules/{ruleId}")
    suspend fun updateAlertRule(
        @Path("ruleId") ruleId: String,
        @Body rule: AlertRuleDto
    ): Response<ApiResponse<AlertRuleDto>>

    @DELETE("alert-rules/{ruleId}")
    suspend fun deleteAlertRule(
        @Path("ruleId") ruleId: String
    ): Response<ApiResponse<Unit>>
}

/**
 * Network error types
 */
sealed class NetworkError : Exception() {
    data class HttpError(val code: Int, val errorBody: String?, val response: Response<*>?) : NetworkError()
    data class ApiError(val errors: List<com.sensacare.app.data.remote.model.ApiError>?) : NetworkError()
    data class NetworkConnectionError(override val cause: Throwable?) : NetworkError()
    data class UnknownError(override val cause: Throwable?) : NetworkError()
    object UnauthorizedError : NetworkError()
    object ForbiddenError : NetworkError()
    object NotFoundError : NetworkError()
    object ServerError : NetworkError()
    data class TimeoutError(override val cause: Throwable?) : NetworkError()
    data class ParseError(override val cause: Throwable?) : NetworkError()
}

/**
 * Authentication interceptor for adding authorization headers
 */
class AuthInterceptor(private val tokenProvider: TokenProvider) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val originalRequest = chain.request()
        
        // Skip authentication for login and refresh token endpoints
        if (originalRequest.url.encodedPath.contains("/auth/login") ||
            originalRequest.url.encodedPath.contains("/auth/refresh")) {
            return chain.proceed(originalRequest)
        }
        
        val token = tokenProvider.getAccessToken()
        if (token.isNullOrEmpty()) {
            return chain.proceed(originalRequest)
        }
        
        val authenticatedRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
        
        return chain.proceed(authenticatedRequest)
    }
}

/**
 * Token provider interface
 */
interface TokenProvider {
    fun getAccessToken(): String?
    fun getRefreshToken(): String?
    fun saveTokens(accessToken: String, refreshToken: String, expiresIn: Long)
    fun clearTokens()
}

/**
 * Network data source implementation
 */
@Singleton
class NetworkDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tokenProvider: TokenProvider,
    private val ioDispatcher: CoroutineDispatcher
) {
    companion object {
        private const val BASE_URL = "https://api.sensacare.com/v1/"
        private const val CONNECT_TIMEOUT = 30L
        private const val READ_TIMEOUT = 30L
        private const val WRITE_TIMEOUT = 30L
    }
    
    private val gson: Gson by lazy {
        GsonBuilder()
            .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
            .registerTypeAdapter(LocalDate::class.java, LocalDateAdapter())
            .create()
    }
    
    private val loggingInterceptor: HttpLoggingInterceptor by lazy {
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }
    
    private val authInterceptor: AuthInterceptor by lazy {
        AuthInterceptor(tokenProvider)
    }
    
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }
    
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    private val api: SensaCareApi by lazy {
        retrofit.create(SensaCareApi::class.java)
    }
    
    /**
     * Check if the device is connected to the internet
     */
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        return networkCapabilities != null &&
                (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                 networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                 networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
    }
    
    /**
     * Safe API call with error handling
     */
    private suspend fun <T> safeApiCall(apiCall: suspend () -> Response<ApiResponse<T>>): Result<T> {
        return withContext(ioDispatcher) {
            try {
                if (!isNetworkAvailable()) {
                    return@withContext Result.Error(NetworkError.NetworkConnectionError(IOException("No internet connection")))
                }
                
                val response = apiCall()
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true) {
                        body.data?.let {
                            Result.Success(it)
                        } ?: Result.Error(NetworkError.ApiError(body.errors))
                    } else {
                        Result.Error(NetworkError.ApiError(body?.errors))
                    }
                } else {
                    when (response.code()) {
                        401 -> Result.Error(NetworkError.UnauthorizedError)
                        403 -> Result.Error(NetworkError.ForbiddenError)
                        404 -> Result.Error(NetworkError.NotFoundError)
                        in 500..599 -> Result.Error(NetworkError.ServerError)
                        else -> Result.Error(NetworkError.HttpError(response.code(), response.errorBody()?.string(), response))
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Network error: ${e.message}")
                val error = when (e) {
                    is HttpException -> NetworkError.HttpError(e.code(), e.response()?.errorBody()?.string(), e.response())
                    is IOException -> NetworkError.NetworkConnectionError(e)
                    else -> NetworkError.UnknownError(e)
                }
                Result.Error(error)
            }
        }
    }
    
    /**
     * Authentication methods
     */
    suspend fun login(email: String, password: String, deviceId: String? = null): Result<AuthResponse> {
        return safeApiCall {
            api.login(AuthRequest(email, password, deviceId))
        }.also { result ->
            if (result is Result.Success) {
                tokenProvider.saveTokens(
                    result.data.accessToken,
                    result.data.refreshToken,
                    result.data.expiresIn
                )
            }
        }
    }
    
    suspend fun refreshToken(): Result<AuthResponse> {
        val refreshToken = tokenProvider.getRefreshToken()
        if (refreshToken.isNullOrEmpty()) {
            return Result.Error(NetworkError.UnauthorizedError)
        }
        
        return safeApiCall {
            api.refreshToken(RefreshTokenRequest(refreshToken))
        }.also { result ->
            if (result is Result.Success) {
                tokenProvider.saveTokens(
                    result.data.accessToken,
                    result.data.refreshToken,
                    result.data.expiresIn
                )
            }
        }
    }
    
    suspend fun requestPasswordReset(email: String): Result<Unit> {
        return safeApiCall {
            api.requestPasswordReset(PasswordResetRequest(email))
        }
    }
    
    /**
     * Sync methods
     */
    suspend fun syncData(request: SyncRequest): Result<SyncResponse> {
        return safeApiCall {
            api.syncData(request)
        }
    }
    
    /**
     * Health data methods
     */
    suspend fun getHealthData(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate,
        metricType: MetricType? = null,
        limit: Int? = null,
        offset: Int? = null
    ): Result<List<HealthDataDto>> {
        return safeApiCall {
            api.getHealthData(
                userId,
                startDate.toString(),
                endDate.toString(),
                metricType?.name,
                limit,
                offset
            )
        }
    }
    
    fun getHealthDataAsFlow(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate,
        metricType: MetricType? = null,
        limit: Int? = null,
        offset: Int? = null
    ): Flow<Result<List<HealthDataDto>>> = flow {
        emit(getHealthData(userId, startDate, endDate, metricType, limit, offset))
    }.flowOn(ioDispatcher)
    
    suspend fun getHeartRates(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate,
        limit: Int? = null,
        offset: Int? = null
    ): Result<List<HeartRateDto>> {
        return safeApiCall {
            api.getHeartRates(
                userId,
                startDate.toString(),
                endDate.toString(),
                limit,
                offset
            )
        }
    }
    
    suspend fun getBloodPressures(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate,
        limit: Int? = null,
        offset: Int? = null
    ): Result<List<BloodPressureDto>> {
        return safeApiCall {
            api.getBloodPressures(
                userId,
                startDate.toString(),
                endDate.toString(),
                limit,
                offset
            )
        }
    }
    
    suspend fun getSleeps(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate,
        limit: Int? = null,
        offset: Int? = null
    ): Result<List<SleepDto>> {
        return safeApiCall {
            api.getSleeps(
                userId,
                startDate.toString(),
                endDate.toString(),
                limit,
                offset
            )
        }
    }
    
    suspend fun getActivities(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate,
        activityType: ActivityType? = null,
        limit: Int? = null,
        offset: Int? = null
    ): Result<List<ActivityDto>> {
        return safeApiCall {
            api.getActivities(
                userId,
                startDate.toString(),
                endDate.toString(),
                activityType?.name,
                limit,
                offset
            )
        }
    }
    
    /**
     * Device methods
     */
    suspend fun getDevices(userId: String): Result<List<DeviceDto>> {
        return safeApiCall {
            api.getDevices(userId)
        }
    }
    
    suspend fun getDevice(deviceId: String): Result<DeviceDto> {
        return safeApiCall {
            api.getDevice(deviceId)
        }
    }
    
    suspend fun registerDevice(device: DeviceDto): Result<DeviceDto> {
        return safeApiCall {
            api.registerDevice(device)
        }
    }
    
    suspend fun updateDevice(deviceId: String, device: DeviceDto): Result<DeviceDto> {
        return safeApiCall {
            api.updateDevice(deviceId, device)
        }
    }
    
    suspend fun deleteDevice(deviceId: String): Result<Unit> {
        return safeApiCall {
            api.deleteDevice(deviceId)
        }
    }
    
    suspend fun recordDeviceSync(
        deviceId: String,
        syncType: String,
        syncStatus: SyncStatus,
        itemsCount: Int,
        errorMessage: String? = null
    ): Result<Map<String, Any>> {
        val syncData = mapOf(
            "syncType" to syncType,
            "syncStatus" to syncStatus.name,
            "startedAt" to LocalDateTime.now().toString(),
            "completedAt" to if (syncStatus != SyncStatus.IN_PROGRESS) LocalDateTime.now().toString() else null,
            "itemsCount" to itemsCount,
            "errorMessage" to errorMessage
        )
        
        return safeApiCall {
            api.recordDeviceSync(deviceId, syncData)
        }
    }
    
    /**
     * Goal methods
     */
    suspend fun getGoals(
        userId: String,
        isActive: Boolean? = null,
        isCompleted: Boolean? = null,
        metricType: MetricType? = null
    ): Result<List<HealthGoalDto>> {
        return safeApiCall {
            api.getGoals(
                userId,
                isActive,
                isCompleted,
                metricType?.name
            )
        }
    }
    
    suspend fun getGoal(goalId: String): Result<HealthGoalDto> {
        return safeApiCall {
            api.getGoal(goalId)
        }
    }
    
    suspend fun createGoal(goal: HealthGoalDto): Result<HealthGoalDto> {
        return safeApiCall {
            api.createGoal(goal)
        }
    }
    
    suspend fun updateGoal(goalId: String, goal: HealthGoalDto): Result<HealthGoalDto> {
        return safeApiCall {
            api.updateGoal(goalId, goal)
        }
    }
    
    suspend fun deleteGoal(goalId: String): Result<Unit> {
        return safeApiCall {
            api.deleteGoal(goalId)
        }
    }
    
    suspend fun getGoalProgress(goalId: String): Result<List<GoalProgressDto>> {
        return safeApiCall {
            api.getGoalProgress(goalId)
        }
    }
    
    suspend fun recordGoalProgress(goalId: String, progress: GoalProgressDto): Result<GoalProgressDto> {
        return safeApiCall {
            api.recordGoalProgress(goalId, progress)
        }
    }
    
    /**
     * Alert methods
     */
    suspend fun getAlerts(
        userId: String,
        status: String? = null,
        severity: String? = null,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null,
        limit: Int? = null,
        offset: Int? = null
    ): Result<List<HealthAlertDto>> {
        return safeApiCall {
            api.getAlerts(
                userId,
                status,
                severity,
                startDate?.toString(),
                endDate?.toString(),
                limit,
                offset
            )
        }
    }
    
    suspend fun getAlert(alertId: String): Result<HealthAlertDto> {
        return safeApiCall {
            api.getAlert(alertId)
        }
    }
    
    suspend fun createAlert(alert: HealthAlertDto): Result<HealthAlertDto> {
        return safeApiCall {
            api.createAlert(alert)
        }
    }
    
    suspend fun updateAlert(alertId: String, alert: HealthAlertDto): Result<HealthAlertDto> {
        return safeApiCall {
            api.updateAlert(alertId, alert)
        }
    }
    
    suspend fun acknowledgeAlert(alertId: String, notes: String? = null): Result<HealthAlertDto> {
        return safeApiCall {
            api.acknowledgeAlert(alertId, mapOf("notes" to notes))
        }
    }
    
    suspend fun resolveAlert(alertId: String, resolution: String): Result<HealthAlertDto> {
        return safeApiCall {
            api.resolveAlert(alertId, mapOf("resolution" to resolution))
        }
    }
    
    suspend fun getAlertRules(
        userId: String,
        isActive: Boolean? = null,
        metricType: MetricType? = null
    ): Result<List<AlertRuleDto>> {
        return safeApiCall {
            api.getAlertRules(
                userId,
                isActive,
                metricType?.name
            )
        }
    }
    
    suspend fun createAlertRule(rule: AlertRuleDto): Result<AlertRuleDto> {
        return safeApiCall {
            api.createAlertRule(rule)
        }
    }
    
    suspend fun updateAlertRule(ruleId: String, rule: AlertRuleDto): Result<AlertRuleDto> {
        return safeApiCall {
            api.updateAlertRule(ruleId, rule)
        }
    }
    
    suspend fun deleteAlertRule(ruleId: String): Result<Unit> {
        return safeApiCall {
            api.deleteAlertRule(ruleId)
        }
    }
}
