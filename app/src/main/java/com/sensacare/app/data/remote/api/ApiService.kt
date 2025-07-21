package com.sensacare.app.data.remote.api

import com.sensacare.app.data.model.auth.AuthCredentials
import com.sensacare.app.data.model.auth.AuthResponse
import com.sensacare.app.data.model.auth.RefreshTokenRequest
import com.sensacare.app.data.model.device.DeviceCapabilities
import com.sensacare.app.data.model.device.DeviceRegistration
import com.sensacare.app.data.model.device.DeviceConfiguration
import com.sensacare.app.data.model.vitals.*
import com.sensacare.app.domain.model.*
import com.sensacare.app.domain.util.Result
import kotlinx.coroutines.flow.Flow
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*
import java.time.LocalDateTime

/**
 * Platform type enum for selecting between SensaCare RPM and Business platforms
 */
enum class PlatformType {
    RPM,
    BUSINESS
}

/**
 * Platform configuration interface
 */
interface PlatformConfig {
    val baseUrl: String
    val apiVersion: String
    val wsEndpoint: String
    val platformType: PlatformType
    val requiresMfa: Boolean
    val supportsBatchSync: Boolean
    val supportsRealTimeMonitoring: Boolean
    val supportedVitals: List<String>
}

/**
 * SensaCare RPM Platform Configuration
 */
data class RpmPlatformConfig(
    override val baseUrl: String = "https://api.sensacare-rpm.com",
    override val apiVersion: String = "v1",
    override val wsEndpoint: String = "wss://api.sensacare-rpm.com/ws",
    override val platformType: PlatformType = PlatformType.RPM,
    override val requiresMfa: Boolean = true,
    override val supportsBatchSync: Boolean = true,
    override val supportsRealTimeMonitoring: Boolean = true,
    override val supportedVitals: List<String> = listOf(
        "heart_rate", 
        "blood_pressure_systolic", 
        "blood_pressure_diastolic", 
        "blood_oxygen", 
        "temperature", 
        "blood_glucose", 
        "weight", 
        "respiratory_rate", 
        "steps", 
        "sleep_duration", 
        "stress_level",
        "hrv"
    )
) : PlatformConfig

/**
 * SensaCare Business Platform Configuration
 */
data class BusinessPlatformConfig(
    override val baseUrl: String = "https://api.sensacare-business.com",
    override val apiVersion: String = "v1",
    override val wsEndpoint: String = "wss://api.sensacare-business.com/ws",
    override val platformType: PlatformType = PlatformType.BUSINESS,
    override val requiresMfa: Boolean = false,
    override val supportsBatchSync: Boolean = true,
    override val supportsRealTimeMonitoring: Boolean = false,
    override val supportedVitals: List<String> = listOf(
        "heart_rate", 
        "blood_pressure_systolic", 
        "blood_pressure_diastolic", 
        "blood_oxygen", 
        "temperature"
    )
) : PlatformConfig

/**
 * API Service interface for SensaCare platforms
 */
interface ApiService {
    
    /**
     * Authentication API
     */
    @POST("auth/login")
    suspend fun login(@Body credentials: AuthCredentials): Result<AuthResponse>
    
    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Result<AuthResponse>
    
    @POST("auth/logout")
    suspend fun logout(): Result<Unit>
    
    @POST("auth/mfa/verify")
    suspend fun verifyMfa(@Body code: String): Result<AuthResponse>
    
    @GET("auth/mfa/setup")
    suspend fun setupMfa(): Result<String> // Returns QR code URL
    
    /**
     * User Profile API
     */
    @GET("profile")
    suspend fun getUserProfile(): Result<UserProfile>
    
    @PUT("profile")
    suspend fun updateUserProfile(@Body profile: UserProfile): Result<UserProfile>
    
    /**
     * Vitals API
     */
    @POST("vitals")
    suspend fun submitVitalReading(@Body reading: VitalReadingDto): Result<VitalReadingResponse>
    
    @POST("vitals/batch")
    suspend fun submitVitalReadingsBatch(@Body readings: List<VitalReadingDto>): Result<BatchSyncResponse>
    
    @GET("vitals/client/{clientId}")
    suspend fun getClientVitals(
        @Path("clientId") clientId: String,
        @Query("timeRange") timeRange: String = "24h",
        @Query("vitalTypes") vitalTypes: String? = null,
        @Query("includeDeviceStatus") includeDeviceStatus: Boolean = false,
        @Query("includeAlerts") includeAlerts: Boolean = false
    ): Result<ClientVitalsResponse>
    
    @GET("vitals/client/{clientId}/{vitalType}")
    suspend fun getClientVitalsByType(
        @Path("clientId") clientId: String,
        @Path("vitalType") vitalType: String,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null,
        @Query("limit") limit: Int = 100
    ): Result<VitalReadingsResponse>
    
    @POST("vitals/client/{clientId}/configuration")
    suspend fun updateClientVitalConfiguration(
        @Path("clientId") clientId: String,
        @Body configuration: ClientVitalConfigurationDto
    ): Result<ClientVitalConfigurationResponse>
    
    @GET("vitals/client/{clientId}/configuration")
    suspend fun getClientVitalConfiguration(
        @Path("clientId") clientId: String
    ): Result<ClientVitalConfigurationResponse>
    
    /**
     * Thresholds API
     */
    @GET("thresholds/client/{clientId}")
    suspend fun getClientThresholds(
        @Path("clientId") clientId: String
    ): Result<ClientThresholdsResponse>
    
    @POST("thresholds/client/{clientId}")
    suspend fun updateClientThresholds(
        @Path("clientId") clientId: String,
        @Body thresholds: ClientThresholdsDto
    ): Result<ClientThresholdsResponse>
    
    @POST("thresholds/adjustment/request")
    suspend fun requestThresholdAdjustment(
        @Body request: ThresholdAdjustmentRequestDto
    ): Result<ThresholdAdjustmentResponse>
    
    /**
     * Device Management API
     */
    @POST("devices/register")
    suspend fun registerDevice(@Body registration: DeviceRegistrationDto): Result<DeviceRegistrationResponse>
    
    @GET("devices/client/{clientId}")
    suspend fun getClientDevices(
        @Path("clientId") clientId: String
    ): Result<ClientDevicesResponse>
    
    @GET("devices/{deviceId}")
    suspend fun getDeviceDetails(
        @Path("deviceId") deviceId: String
    ): Result<DeviceDetailsResponse>
    
    @POST("devices/{deviceId}/configuration")
    suspend fun updateDeviceConfiguration(
        @Path("deviceId") deviceId: String,
        @Body configuration: DeviceConfigurationDto
    ): Result<DeviceConfigurationResponse>
    
    @POST("devices/{deviceId}/sync")
    suspend fun syncDeviceData(
        @Path("deviceId") deviceId: String,
        @Body syncRequest: DeviceSyncRequestDto
    ): Result<DeviceSyncResponse>
    
    @POST("devices/{deviceId}/status")
    suspend fun updateDeviceStatus(
        @Path("deviceId") deviceId: String,
        @Body status: DeviceStatusUpdateDto
    ): Result<DeviceStatusResponse>
    
    /**
     * Alerts API
     */
    @GET("alerts/client/{clientId}")
    suspend fun getClientAlerts(
        @Path("clientId") clientId: String,
        @Query("status") status: String? = null,
        @Query("priority") priority: String? = null,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null
    ): Result<ClientAlertsResponse>
    
    @POST("alerts/{alertId}/acknowledge")
    suspend fun acknowledgeAlert(
        @Path("alertId") alertId: String,
        @Body acknowledgement: AlertAcknowledgementDto
    ): Result<AlertResponse>
    
    @POST("alerts/{alertId}/resolve")
    suspend fun resolveAlert(
        @Path("alertId") alertId: String,
        @Body resolution: AlertResolutionDto
    ): Result<AlertResponse>
    
    /**
     * Notifications API
     */
    @GET("notifications")
    suspend fun getNotifications(
        @Query("status") status: String? = null,
        @Query("limit") limit: Int = 50
    ): Result<NotificationsResponse>
    
    @POST("notifications/{notificationId}/read")
    suspend fun markNotificationAsRead(
        @Path("notificationId") notificationId: String
    ): Result<NotificationResponse>
    
    @POST("notifications/device-token")
    suspend fun registerDeviceToken(
        @Body request: DeviceTokenRegistrationDto
    ): Result<DeviceTokenResponse>
    
    /**
     * Platform-specific APIs
     */
    
    // RPM-specific endpoints
    @GET("rpm/care-plan/{clientId}")
    suspend fun getClientCarePlan(
        @Path("clientId") clientId: String
    ): Result<CarePlanResponse>
    
    @POST("rpm/care-plan/{clientId}")
    suspend fun updateClientCarePlan(
        @Path("clientId") clientId: String,
        @Body carePlan: CarePlanUpdateDto
    ): Result<CarePlanResponse>
    
    @POST("rpm/billable-time/{clientId}")
    suspend fun submitBillableTime(
        @Path("clientId") clientId: String,
        @Body timeEntry: BillableTimeEntryDto
    ): Result<BillableTimeResponse>
    
    // Business-specific endpoints
    @GET("business/reports/wellness")
    suspend fun getWellnessReport(
        @Query("departmentId") departmentId: String? = null,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null
    ): Result<WellnessReportResponse>
    
    @POST("business/wellness-program/enrollment")
    suspend fun enrollInWellnessProgram(
        @Body enrollment: WellnessProgramEnrollmentDto
    ): Result<WellnessProgramResponse>
}

/**
 * WebSocket service for real-time communication
 */
interface WebSocketService {
    
    /**
     * Connect to the WebSocket server
     * @param platformConfig The platform configuration
     * @param authToken The authentication token
     * @return A flow of WebSocket events
     */
    fun connect(platformConfig: PlatformConfig, authToken: String): Flow<WebSocketEvent>
    
    /**
     * Disconnect from the WebSocket server
     */
    fun disconnect()
    
    /**
     * Send a message to the WebSocket server
     * @param message The message to send
     */
    fun sendMessage(message: WebSocketMessage)
    
    /**
     * Check if the WebSocket is connected
     * @return True if connected, false otherwise
     */
    fun isConnected(): Boolean
}

/**
 * WebSocket event sealed class
 */
sealed class WebSocketEvent {
    data class Connected(val timestamp: LocalDateTime) : WebSocketEvent()
    data class Disconnected(val reason: String, val timestamp: LocalDateTime) : WebSocketEvent()
    data class MessageReceived(val message: WebSocketMessage, val timestamp: LocalDateTime) : WebSocketEvent()
    data class Error(val error: Throwable, val timestamp: LocalDateTime) : WebSocketEvent()
}

/**
 * WebSocket message data class
 */
data class WebSocketMessage(
    val type: String,
    val payload: Any,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

/**
 * Platform selector interface
 */
interface PlatformSelector {
    /**
     * Get the current platform configuration
     * @return The current platform configuration
     */
    fun getCurrentPlatform(): PlatformConfig
    
    /**
     * Switch to a different platform
     * @param platformType The platform type to switch to
     * @return The new platform configuration
     */
    fun switchPlatform(platformType: PlatformType): PlatformConfig
    
    /**
     * Check if a feature is supported by the current platform
     * @param feature The feature to check
     * @return True if supported, false otherwise
     */
    fun isFeatureSupported(feature: PlatformFeature): Boolean
}

/**
 * Platform feature enum
 */
enum class PlatformFeature {
    REAL_TIME_MONITORING,
    BATCH_SYNC,
    MFA,
    CARE_PLANS,
    BILLABLE_TIME,
    WELLNESS_PROGRAMS,
    ADVANCED_ANALYTICS
}

/**
 * API data transfer objects
 */

// Auth DTOs
data class UserProfile(
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: String,
    val clientId: String?,
    val organizationId: String?,
    val profileImageUrl: String?,
    val phoneNumber: String?,
    val preferences: Map<String, Any>?,
    val lastLogin: String?,
    val mfaEnabled: Boolean
)

// Vitals DTOs
data class VitalReadingDto(
    val id: String? = null,
    val clientId: String,
    val deviceId: String? = null,
    val vitalType: String,
    val value: Any,
    val unit: String,
    val timestamp: String,
    val source: String,
    val deviceType: String? = null,
    val rawData: Any? = null,
    val metadata: Map<String, Any>? = null,
    val qualityScore: Double? = null
)

data class VitalReadingResponse(
    val id: String,
    val status: String,
    val timestamp: String,
    val reading: VitalReadingDto
)

data class BatchSyncResponse(
    val successCount: Int,
    val failureCount: Int,
    val errors: List<SyncError>?,
    val timestamp: String,
    val syncId: String
)

data class SyncError(
    val index: Int,
    val readingId: String?,
    val errorCode: String,
    val errorMessage: String
)

data class ClientVitalsResponse(
    val client: ClientDto,
    val readingsByType: Map<String, VitalTypeReadings>,
    val latestReadingsByType: Map<String, VitalReadingDto?>,
    val timeRange: TimeRange,
    val metadata: VitalsMetadata
)

data class ClientDto(
    val id: String,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: String?,
    val gender: String?,
    val profileImageUrl: String?
)

data class VitalTypeReadings(
    val type: String,
    val unit: String,
    val day: List<VitalReadingDto>?,
    val week: List<VitalReadingDto>?,
    val month: List<VitalReadingDto>?,
    val year: List<VitalReadingDto>?
)

data class TimeRange(
    val start: String,
    val end: String,
    val requested: String
)

data class VitalsMetadata(
    val totalReadings: Int,
    val vitalTypesCount: Int
)

data class VitalReadingsResponse(
    val vitalType: String,
    val readings: List<VitalReadingDto>,
    val summary: VitalSummary,
    val timeRange: TimeRange
)

data class VitalSummary(
    val count: Int,
    val average: Double?,
    val min: Double?,
    val max: Double?,
    val trend: String?
)

data class ClientVitalConfigurationDto(
    val clientId: String,
    val enabledVitals: List<String>,
    val customThresholds: Map<String, CustomThreshold>?,
    val monitoringSchedule: Map<String, MonitoringSchedule>?,
    val alertPreferences: AlertPreferences
)

data class CustomThreshold(
    val normalMin: Double?,
    val normalMax: Double?,
    val warningLow: Double?,
    val warningHigh: Double?,
    val criticalLow: Double?,
    val criticalHigh: Double?,
    val customRules: Any?
)

data class MonitoringSchedule(
    val frequency: Int,
    val activeHours: ActiveHours?,
    val priority: String
)

data class ActiveHours(
    val start: String,
    val end: String
)

data class AlertPreferences(
    val immediateAlerts: List<String>,
    val dailySummary: Boolean,
    val weeklyTrends: Boolean
)

data class ClientVitalConfigurationResponse(
    val clientId: String,
    val configuration: ClientVitalConfigurationDto,
    val updatedAt: String
)

// Thresholds DTOs
data class ClientThresholdsDto(
    val clientId: String,
    val thresholds: Map<String, ThresholdValues>
)

data class ThresholdValues(
    val lower: Double?,
    val upper: Double?,
    val unit: String,
    val requiresAuthorization: Boolean,
    val warningLow: Double?,
    val warningHigh: Double?,
    val criticalLow: Double?,
    val criticalHigh: Double?
)

data class ClientThresholdsResponse(
    val clientId: String,
    val thresholds: Map<String, ThresholdValues>,
    val lastUpdated: String,
    val updatedBy: String?
)

data class ThresholdAdjustmentRequestDto(
    val id: String? = null,
    val clientId: String,
    val vitalType: String,
    val currentLower: Double?,
    val currentUpper: Double?,
    val requestedLower: Double?,
    val requestedUpper: Double?,
    val justification: String,
    val requestedBy: String
)

data class ThresholdAdjustmentResponse(
    val id: String,
    val status: String,
    val requestedAt: String,
    val request: ThresholdAdjustmentRequestDto
)

// Device DTOs
data class DeviceRegistrationDto(
    val deviceId: String,
    val clientId: String,
    val deviceName: String,
    val manufacturer: String,
    val model: String,
    val firmwareVersion: String?,
    val serialNumber: String?,
    val capabilities: DeviceCapabilitiesDto
)

data class DeviceCapabilitiesDto(
    val supportedVitals: List<String>,
    val samplingFrequencies: Map<String, Int>,
    val batteryLife: Int?,
    val connectivity: String
)

data class DeviceRegistrationResponse(
    val deviceId: String,
    val registrationTimestamp: String,
    val status: String,
    val configuration: DeviceConfigurationDto
)

data class DeviceConfigurationDto(
    val samplingIntervals: Map<String, Int>,
    val dataRetentionDays: Int,
    val autoSync: Boolean,
    val batteryAlertThreshold: Int,
    val qualityThreshold: Double,
    val unitPreferences: Map<String, String>,
    val customFieldMappings: Map<String, String>?
)

data class ClientDevicesResponse(
    val clientId: String,
    val devices: List<DeviceDto>,
    val count: Int
)

data class DeviceDto(
    val deviceId: String,
    val deviceName: String,
    val manufacturer: String,
    val model: String,
    val status: String,
    val lastSync: String?,
    val batteryLevel: Int?
)

data class DeviceDetailsResponse(
    val deviceId: String,
    val clientId: String,
    val deviceName: String,
    val manufacturer: String,
    val model: String,
    val firmwareVersion: String?,
    val serialNumber: String?,
    val registrationTimestamp: String,
    val lastSync: String?,
    val capabilities: DeviceCapabilitiesDto,
    val configuration: DeviceConfigurationDto,
    val status: String
)

data class DeviceConfigurationResponse(
    val deviceId: String,
    val configuration: DeviceConfigurationDto,
    val updatedAt: String
)

data class DeviceSyncRequestDto(
    val syncStartTime: String,
    val syncEndTime: String,
    val vitalTypes: List<String>?,
    val forceFull: Boolean = false
)

data class DeviceSyncResponse(
    val syncId: String,
    val deviceId: String,
    val syncStartTime: String,
    val syncEndTime: String,
    val syncDurationMillis: Long,
    val recordCounts: Map<String, Int>,
    val totalRecords: Int,
    val status: String,
    val errors: List<String>?
)

data class DeviceStatusUpdateDto(
    val status: String,
    val batteryLevel: Int?,
    val connectionStatus: String?,
    val lastSeen: String?,
    val errorCode: String?,
    val errorMessage: String?
)

data class DeviceStatusResponse(
    val deviceId: String,
    val status: String,
    val timestamp: String
)

// Alerts DTOs
data class ClientAlertsResponse(
    val clientId: String,
    val alerts: List<AlertDto>,
    val count: Int,
    val unacknowledgedCount: Int
)

data class AlertDto(
    val id: String,
    val clientId: String,
    val vitalType: String?,
    val alertType: String,
    val message: String,
    val timestamp: String,
    val priority: String,
    val status: String,
    val relatedReadingId: String?,
    val acknowledgedBy: String?,
    val acknowledgedAt: String?,
    val resolvedBy: String?,
    val resolvedAt: String?,
    val notes: String?
)

data class AlertAcknowledgementDto(
    val acknowledgedBy: String,
    val notes: String?
)

data class AlertResolutionDto(
    val resolvedBy: String,
    val resolutionAction: String,
    val notes: String?
)

data class AlertResponse(
    val id: String,
    val status: String,
    val timestamp: String,
    val alert: AlertDto
)

// Notifications DTOs
data class NotificationsResponse(
    val notifications: List<NotificationDto>,
    val count: Int,
    val unreadCount: Int
)

data class NotificationDto(
    val id: String,
    val userId: String,
    val type: String,
    val title: String,
    val message: String,
    val timestamp: String,
    val read: Boolean,
    val readAt: String?,
    val data: Map<String, Any>?
)

data class NotificationResponse(
    val id: String,
    val status: String,
    val notification: NotificationDto
)

data class DeviceTokenRegistrationDto(
    val userId: String,
    val deviceToken: String,
    val deviceType: String,
    val appVersion: String
)

data class DeviceTokenResponse(
    val userId: String,
    val status: String,
    val timestamp: String
)

// Platform-specific DTOs
data class CarePlanResponse(
    val clientId: String,
    val carePlan: CarePlanDto,
    val lastUpdated: String,
    val updatedBy: String
)

data class CarePlanDto(
    val id: String,
    val clientId: String,
    val startDate: String,
    val endDate: String?,
    val goals: List<CarePlanGoalDto>,
    val vitalsToMonitor: List<String>,
    val monitoringFrequency: Map<String, Int>,
    val careTeam: List<CareTeamMemberDto>,
    val notes: String?
)

data class CarePlanGoalDto(
    val id: String,
    val description: String,
    val targetDate: String?,
    val status: String,
    val relatedVital: String?,
    val targetValue: Double?
)

data class CareTeamMemberDto(
    val id: String,
    val name: String,
    val role: String,
    val contactInfo: String?
)

data class CarePlanUpdateDto(
    val startDate: String,
    val endDate: String?,
    val goals: List<CarePlanGoalDto>,
    val vitalsToMonitor: List<String>,
    val monitoringFrequency: Map<String, Int>,
    val careTeam: List<CareTeamMemberDto>,
    val notes: String?
)

data class BillableTimeEntryDto(
    val clientId: String,
    val providerId: String,
    val durationMinutes: Int,
    val activityType: String,
    val notes: String,
    val timestamp: String
)

data class BillableTimeResponse(
    val id: String,
    val status: String,
    val timestamp: String,
    val entry: BillableTimeEntryDto
)

data class WellnessReportResponse(
    val departmentId: String?,
    val timeRange: TimeRange,
    val participantCount: Int,
    val averageEngagement: Double,
    val vitalTrends: Map<String, VitalTrendDto>,
    val wellnessScore: Double,
    val improvementAreas: List<String>
)

data class VitalTrendDto(
    val vitalType: String,
    val averageValue: Double,
    val trend: String,
    val percentChange: Double
)

data class WellnessProgramEnrollmentDto(
    val userId: String,
    val programId: String,
    val startDate: String,
    val goals: List<WellnessGoalDto>
)

data class WellnessGoalDto(
    val type: String,
    val targetValue: Double,
    val deadline: String?
)

data class WellnessProgramResponse(
    val userId: String,
    val programId: String,
    val status: String,
    val startDate: String,
    val estimatedEndDate: String
)
