package com.sensacare.app.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.sensacare.app.data.local.dao.*
import com.sensacare.app.data.local.preferences.UserPreferences
import com.sensacare.app.data.model.auth.AuthCredentials
import com.sensacare.app.data.model.auth.AuthResponse
import com.sensacare.app.data.model.auth.RefreshTokenRequest
import com.sensacare.app.data.remote.api.*
import com.sensacare.app.domain.model.*
import com.sensacare.app.domain.repository.*
import com.sensacare.app.domain.util.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Platform integration manager for SensaCare app
 * Handles platform selection, authentication, API configuration,
 * data synchronization, WebSocket connections, and offline support
 */
@Singleton
class PlatformIntegrationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferences: UserPreferences,
    private val heartRateRepository: HeartRateRepository,
    private val bloodPressureRepository: BloodPressureRepository,
    private val bloodOxygenRepository: BloodOxygenRepository,
    private val bodyTemperatureRepository: BodyTemperatureRepository,
    private val stressLevelRepository: StressLevelRepository,
    private val ecgRepository: EcgRepository,
    private val bloodGlucoseRepository: BloodGlucoseRepository,
    private val activityRepository: ActivityRepository,
    private val sleepRepository: SleepRepository,
    private val deviceCapabilityRepository: DeviceCapabilityRepository
) : PlatformSelector {

    // Coroutine scope for platform operations
    private val platformScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // Current platform configuration
    private var currentPlatform: PlatformConfig = RpmPlatformConfig()
    
    // API service instances for both platforms
    private val apiServices = ConcurrentHashMap<PlatformType, ApiService>()
    
    // WebSocket service instances
    private val webSocketServices = ConcurrentHashMap<PlatformType, WebSocketService>()
    
    // Authentication state
    private val _authState = MutableStateFlow<AuthState>(AuthState.NotAuthenticated)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    // Platform events
    private val _platformEvents = MutableSharedFlow<PlatformEvent>(extraBufferCapacity = 10)
    val platformEvents: SharedFlow<PlatformEvent> = _platformEvents.asSharedFlow()
    
    // Sync status
    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()
    
    // WebSocket events
    private val _webSocketEvents = MutableSharedFlow<WebSocketEvent>(extraBufferCapacity = 50)
    val webSocketEvents: SharedFlow<WebSocketEvent> = _webSocketEvents.asSharedFlow()
    
    // Sync job
    private var syncJob: Job? = null
    
    // Offline queue
    private val offlineQueue = ConcurrentHashMap<String, MutableList<Any>>()
    
    // Initialize
    init {
        Timber.d("Initializing PlatformIntegrationManager")
        
        // Load saved platform type
        platformScope.launch {
            val savedPlatformType = userPreferences.getSelectedPlatform()
            if (savedPlatformType != null) {
                switchPlatform(savedPlatformType)
            }
            
            // Check for saved auth tokens
            val authToken = userPreferences.getAuthToken()
            val refreshToken = userPreferences.getRefreshToken()
            
            if (!authToken.isNullOrEmpty() && !refreshToken.isNullOrEmpty()) {
                _authState.value = AuthState.Authenticated(
                    token = authToken,
                    refreshToken = refreshToken,
                    expiresAt = userPreferences.getTokenExpiryTime() ?: LocalDateTime.now()
                )
                
                // Start token refresh timer
                startTokenRefreshTimer()
            }
        }
        
        // Start connectivity monitoring
        monitorConnectivity()
        
        // Process offline queue when online
        monitorOfflineQueue()
    }
    
    /**
     * Get current platform configuration
     */
    override fun getCurrentPlatform(): PlatformConfig {
        return currentPlatform
    }
    
    /**
     * Switch to a different platform
     */
    override fun switchPlatform(platformType: PlatformType): PlatformConfig {
        Timber.d("Switching to platform: $platformType")
        
        currentPlatform = when (platformType) {
            PlatformType.RPM -> RpmPlatformConfig()
            PlatformType.BUSINESS -> BusinessPlatformConfig()
        }
        
        // Save selected platform
        platformScope.launch {
            userPreferences.saveSelectedPlatform(platformType)
            _platformEvents.emit(PlatformEvent.PlatformSwitched(platformType))
        }
        
        return currentPlatform
    }
    
    /**
     * Check if a feature is supported by the current platform
     */
    override fun isFeatureSupported(feature: PlatformFeature): Boolean {
        return when (feature) {
            PlatformFeature.REAL_TIME_MONITORING -> currentPlatform.supportsRealTimeMonitoring
            PlatformFeature.BATCH_SYNC -> currentPlatform.supportsBatchSync
            PlatformFeature.MFA -> currentPlatform.requiresMfa
            PlatformFeature.CARE_PLANS -> currentPlatform.platformType == PlatformType.RPM
            PlatformFeature.BILLABLE_TIME -> currentPlatform.platformType == PlatformType.RPM
            PlatformFeature.WELLNESS_PROGRAMS -> currentPlatform.platformType == PlatformType.BUSINESS
            PlatformFeature.ADVANCED_ANALYTICS -> currentPlatform.platformType == PlatformType.RPM
        }
    }
    
    /**
     * Get API service for the current platform
     */
    fun getApiService(): ApiService {
        val platformType = currentPlatform.platformType
        
        // Create API service if it doesn't exist
        if (!apiServices.containsKey(platformType)) {
            apiServices[platformType] = createApiService(currentPlatform)
        }
        
        return apiServices[platformType]!!
    }
    
    /**
     * Get WebSocket service for the current platform
     */
    fun getWebSocketService(): WebSocketService? {
        // Only return WebSocket service if real-time monitoring is supported
        if (!isFeatureSupported(PlatformFeature.REAL_TIME_MONITORING)) {
            return null
        }
        
        val platformType = currentPlatform.platformType
        
        // Create WebSocket service if it doesn't exist
        if (!webSocketServices.containsKey(platformType)) {
            webSocketServices[platformType] = createWebSocketService(currentPlatform)
        }
        
        return webSocketServices[platformType]
    }
    
    /**
     * Login to the current platform
     */
    suspend fun login(email: String, password: String): Result<AuthResponse> {
        Timber.d("Logging in to platform: ${currentPlatform.platformType}")
        
        _authState.value = AuthState.Authenticating
        
        try {
            val credentials = AuthCredentials(email, password)
            val result = getApiService().login(credentials)
            
            if (result is Result.Success) {
                // Save auth tokens
                saveAuthTokens(
                    token = result.data.token,
                    refreshToken = result.data.refreshToken,
                    expiresAt = result.data.expiresAt
                )
                
                // Start token refresh timer
                startTokenRefreshTimer()
                
                // Connect to WebSocket if supported
                if (isFeatureSupported(PlatformFeature.REAL_TIME_MONITORING)) {
                    connectWebSocket()
                }
                
                // Emit platform event
                _platformEvents.emit(PlatformEvent.LoggedIn(currentPlatform.platformType))
            } else if (result is Result.Error) {
                _authState.value = AuthState.Error(result.exception.message ?: "Login failed")
                _platformEvents.emit(PlatformEvent.AuthenticationFailed(result.exception.message ?: "Login failed"))
            }
            
            return result
        } catch (e: Exception) {
            Timber.e(e, "Login failed")
            _authState.value = AuthState.Error(e.message ?: "Login failed")
            _platformEvents.emit(PlatformEvent.AuthenticationFailed(e.message ?: "Login failed"))
            return Result.Error(e)
        }
    }
    
    /**
     * Verify MFA code
     */
    suspend fun verifyMfa(code: String): Result<AuthResponse> {
        Timber.d("Verifying MFA code")
        
        try {
            val result = getApiService().verifyMfa(code)
            
            if (result is Result.Success) {
                // Save auth tokens
                saveAuthTokens(
                    token = result.data.token,
                    refreshToken = result.data.refreshToken,
                    expiresAt = result.data.expiresAt
                )
                
                // Start token refresh timer
                startTokenRefreshTimer()
                
                // Connect to WebSocket if supported
                if (isFeatureSupported(PlatformFeature.REAL_TIME_MONITORING)) {
                    connectWebSocket()
                }
                
                // Emit platform event
                _platformEvents.emit(PlatformEvent.LoggedIn(currentPlatform.platformType))
            } else if (result is Result.Error) {
                _authState.value = AuthState.Error(result.exception.message ?: "MFA verification failed")
                _platformEvents.emit(PlatformEvent.AuthenticationFailed(result.exception.message ?: "MFA verification failed"))
            }
            
            return result
        } catch (e: Exception) {
            Timber.e(e, "MFA verification failed")
            _authState.value = AuthState.Error(e.message ?: "MFA verification failed")
            _platformEvents.emit(PlatformEvent.AuthenticationFailed(e.message ?: "MFA verification failed"))
            return Result.Error(e)
        }
    }
    
    /**
     * Logout from the current platform
     */
    suspend fun logout(): Result<Unit> {
        Timber.d("Logging out from platform: ${currentPlatform.platformType}")
        
        try {
            // Only call logout API if authenticated
            if (_authState.value is AuthState.Authenticated) {
                getApiService().logout()
            }
            
            // Clear auth tokens
            clearAuthTokens()
            
            // Disconnect WebSocket
            disconnectWebSocket()
            
            // Cancel sync job
            syncJob?.cancel()
            syncJob = null
            
            // Emit platform event
            _platformEvents.emit(PlatformEvent.LoggedOut(currentPlatform.platformType))
            
            return Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Logout failed")
            
            // Clear auth tokens anyway
            clearAuthTokens()
            
            // Disconnect WebSocket
            disconnectWebSocket()
            
            // Cancel sync job
            syncJob?.cancel()
            syncJob = null
            
            // Emit platform event
            _platformEvents.emit(PlatformEvent.LoggedOut(currentPlatform.platformType))
            
            return Result.Error(e)
        }
    }
    
    /**
     * Refresh authentication token
     */
    suspend fun refreshToken(): Result<AuthResponse> {
        Timber.d("Refreshing auth token")
        
        val currentAuthState = _authState.value
        if (currentAuthState !is AuthState.Authenticated) {
            return Result.Error(Exception("Not authenticated"))
        }
        
        try {
            val request = RefreshTokenRequest(currentAuthState.refreshToken)
            val result = getApiService().refreshToken(request)
            
            if (result is Result.Success) {
                // Save auth tokens
                saveAuthTokens(
                    token = result.data.token,
                    refreshToken = result.data.refreshToken,
                    expiresAt = result.data.expiresAt
                )
                
                // Emit platform event
                _platformEvents.emit(PlatformEvent.TokenRefreshed)
            } else if (result is Result.Error) {
                // Token refresh failed, clear auth tokens
                clearAuthTokens()
                
                // Emit platform event
                _platformEvents.emit(PlatformEvent.TokenRefreshFailed(result.exception.message ?: "Token refresh failed"))
            }
            
            return result
        } catch (e: Exception) {
            Timber.e(e, "Token refresh failed")
            
            // Token refresh failed, clear auth tokens
            clearAuthTokens()
            
            // Emit platform event
            _platformEvents.emit(PlatformEvent.TokenRefreshFailed(e.message ?: "Token refresh failed"))
            
            return Result.Error(e)
        }
    }
    
    /**
     * Start data synchronization
     */
    fun startSync(syncConfig: SyncConfig = SyncConfig()) {
        Timber.d("Starting data sync with config: $syncConfig")
        
        // Cancel existing sync job
        syncJob?.cancel()
        
        // Start new sync job
        syncJob = platformScope.launch {
            try {
                _syncStatus.value = SyncStatus.Syncing(0, 100)
                _platformEvents.emit(PlatformEvent.SyncStarted)
                
                // Check if we're online
                if (!isOnline()) {
                    Timber.d("Device is offline, skipping sync")
                    _syncStatus.value = SyncStatus.Error("Device is offline")
                    _platformEvents.emit(PlatformEvent.SyncFailed("Device is offline"))
                    return@launch
                }
                
                // Check if we're authenticated
                val currentAuthState = _authState.value
                if (currentAuthState !is AuthState.Authenticated) {
                    Timber.d("Not authenticated, skipping sync")
                    _syncStatus.value = SyncStatus.Error("Not authenticated")
                    _platformEvents.emit(PlatformEvent.SyncFailed("Not authenticated"))
                    return@launch
                }
                
                // Sync data based on config
                val result = when {
                    syncConfig.fullSync -> performFullSync()
                    else -> performIncrementalSync(syncConfig)
                }
                
                // Handle result
                if (result is Result.Success) {
                    _syncStatus.value = SyncStatus.Completed(result.data)
                    _platformEvents.emit(PlatformEvent.SyncCompleted(result.data))
                    
                    // Schedule next sync if periodic sync is enabled
                    if (syncConfig.periodicSync) {
                        delay(syncConfig.syncInterval)
                        startSync(syncConfig)
                    }
                } else if (result is Result.Error) {
                    _syncStatus.value = SyncStatus.Error(result.exception.message ?: "Sync failed")
                    _platformEvents.emit(PlatformEvent.SyncFailed(result.exception.message ?: "Sync failed"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Sync failed")
                _syncStatus.value = SyncStatus.Error(e.message ?: "Sync failed")
                _platformEvents.emit(PlatformEvent.SyncFailed(e.message ?: "Sync failed"))
            }
        }
    }
    
    /**
     * Stop data synchronization
     */
    fun stopSync() {
        Timber.d("Stopping data sync")
        
        syncJob?.cancel()
        syncJob = null
        
        _syncStatus.value = SyncStatus.Idle
        
        platformScope.launch {
            _platformEvents.emit(PlatformEvent.SyncStopped)
        }
    }
    
    /**
     * Connect to WebSocket
     */
    fun connectWebSocket() {
        Timber.d("Connecting to WebSocket")
        
        // Check if real-time monitoring is supported
        if (!isFeatureSupported(PlatformFeature.REAL_TIME_MONITORING)) {
            Timber.d("Real-time monitoring not supported by current platform")
            return
        }
        
        // Check if we're authenticated
        val currentAuthState = _authState.value
        if (currentAuthState !is AuthState.Authenticated) {
            Timber.d("Not authenticated, skipping WebSocket connection")
            return
        }
        
        // Get WebSocket service
        val webSocketService = getWebSocketService() ?: return
        
        // Connect to WebSocket
        platformScope.launch {
            webSocketService.connect(currentPlatform, currentAuthState.token)
                .collect { event ->
                    // Forward WebSocket events
                    _webSocketEvents.emit(event)
                    
                    // Handle WebSocket events
                    when (event) {
                        is WebSocketEvent.Connected -> {
                            _platformEvents.emit(PlatformEvent.WebSocketConnected)
                        }
                        is WebSocketEvent.Disconnected -> {
                            _platformEvents.emit(PlatformEvent.WebSocketDisconnected(event.reason))
                        }
                        is WebSocketEvent.Error -> {
                            _platformEvents.emit(PlatformEvent.WebSocketError(event.error.message ?: "WebSocket error"))
                        }
                        is WebSocketEvent.MessageReceived -> {
                            handleWebSocketMessage(event.message)
                        }
                    }
                }
        }
    }
    
    /**
     * Disconnect from WebSocket
     */
    fun disconnectWebSocket() {
        Timber.d("Disconnecting from WebSocket")
        
        // Check if real-time monitoring is supported
        if (!isFeatureSupported(PlatformFeature.REAL_TIME_MONITORING)) {
            return
        }
        
        // Get WebSocket service
        val webSocketService = getWebSocketService() ?: return
        
        // Disconnect from WebSocket
        webSocketService.disconnect()
        
        platformScope.launch {
            _platformEvents.emit(PlatformEvent.WebSocketDisconnected("User requested"))
        }
    }
    
    /**
     * Submit vital reading to platform
     */
    suspend fun submitVitalReading(reading: VitalReadingDto): Result<VitalReadingResponse> {
        Timber.d("Submitting vital reading: ${reading.vitalType}")
        
        // Check if we're online
        if (!isOnline()) {
            Timber.d("Device is offline, queueing vital reading")
            queueOfflineData("vital_reading", reading)
            return Result.Error(Exception("Device is offline"))
        }
        
        // Check if we're authenticated
        val currentAuthState = _authState.value
        if (currentAuthState !is AuthState.Authenticated) {
            Timber.d("Not authenticated, queueing vital reading")
            queueOfflineData("vital_reading", reading)
            return Result.Error(Exception("Not authenticated"))
        }
        
        // Submit vital reading
        return try {
            val result = getApiService().submitVitalReading(reading)
            
            if (result is Result.Success) {
                _platformEvents.emit(PlatformEvent.VitalReadingSubmitted(reading.vitalType))
            } else if (result is Result.Error) {
                Timber.e(result.exception, "Failed to submit vital reading")
                queueOfflineData("vital_reading", reading)
            }
            
            result
        } catch (e: Exception) {
            Timber.e(e, "Failed to submit vital reading")
            queueOfflineData("vital_reading", reading)
            Result.Error(e)
        }
    }
    
    /**
     * Submit batch of vital readings to platform
     */
    suspend fun submitVitalReadingsBatch(readings: List<VitalReadingDto>): Result<BatchSyncResponse> {
        Timber.d("Submitting batch of ${readings.size} vital readings")
        
        // Check if batch sync is supported
        if (!isFeatureSupported(PlatformFeature.BATCH_SYNC)) {
            Timber.d("Batch sync not supported, submitting readings individually")
            
            val results = readings.map { reading ->
                submitVitalReading(reading)
            }
            
            val successCount = results.count { it is Result.Success }
            val failureCount = results.count { it is Result.Error }
            
            val batchResponse = BatchSyncResponse(
                successCount = successCount,
                failureCount = failureCount,
                errors = results.mapIndexedNotNull { index, result ->
                    if (result is Result.Error) {
                        SyncError(
                            index = index,
                            readingId = readings[index].id,
                            errorCode = "ERROR",
                            errorMessage = result.exception.message ?: "Unknown error"
                        )
                    } else null
                },
                timestamp = LocalDateTime.now().toString(),
                syncId = "local-${System.currentTimeMillis()}"
            )
            
            return Result.Success(batchResponse)
        }
        
        // Check if we're online
        if (!isOnline()) {
            Timber.d("Device is offline, queueing batch of vital readings")
            readings.forEach { reading ->
                queueOfflineData("vital_reading", reading)
            }
            return Result.Error(Exception("Device is offline"))
        }
        
        // Check if we're authenticated
        val currentAuthState = _authState.value
        if (currentAuthState !is AuthState.Authenticated) {
            Timber.d("Not authenticated, queueing batch of vital readings")
            readings.forEach { reading ->
                queueOfflineData("vital_reading", reading)
            }
            return Result.Error(Exception("Not authenticated"))
        }
        
        // Submit batch of vital readings
        return try {
            val result = getApiService().submitVitalReadingsBatch(readings)
            
            if (result is Result.Success) {
                _platformEvents.emit(PlatformEvent.VitalReadingBatchSubmitted(readings.size))
            } else if (result is Result.Error) {
                Timber.e(result.exception, "Failed to submit batch of vital readings")
                readings.forEach { reading ->
                    queueOfflineData("vital_reading", reading)
                }
            }
            
            result
        } catch (e: Exception) {
            Timber.e(e, "Failed to submit batch of vital readings")
            readings.forEach { reading ->
                queueOfflineData("vital_reading", reading)
            }
            Result.Error(e)
        }
    }
    
    /**
     * Register device with platform
     */
    suspend fun registerDevice(registration: DeviceRegistrationDto): Result<DeviceRegistrationResponse> {
        Timber.d("Registering device: ${registration.deviceName}")
        
        // Check if we're online
        if (!isOnline()) {
            Timber.d("Device is offline, queueing device registration")
            queueOfflineData("device_registration", registration)
            return Result.Error(Exception("Device is offline"))
        }
        
        // Check if we're authenticated
        val currentAuthState = _authState.value
        if (currentAuthState !is AuthState.Authenticated) {
            Timber.d("Not authenticated, queueing device registration")
            queueOfflineData("device_registration", registration)
            return Result.Error(Exception("Not authenticated"))
        }
        
        // Register device
        return try {
            val result = getApiService().registerDevice(registration)
            
            if (result is Result.Success) {
                _platformEvents.emit(PlatformEvent.DeviceRegistered(registration.deviceId))
            } else if (result is Result.Error) {
                Timber.e(result.exception, "Failed to register device")
                queueOfflineData("device_registration", registration)
            }
            
            result
        } catch (e: Exception) {
            Timber.e(e, "Failed to register device")
            queueOfflineData("device_registration", registration)
            Result.Error(e)
        }
    }
    
    /**
     * Migrate user data between platforms
     */
    suspend fun migrateBetweenPlatforms(
        sourcePlatform: PlatformType,
        targetPlatform: PlatformType,
        migrationConfig: MigrationConfig = MigrationConfig()
    ): Result<MigrationResult> {
        Timber.d("Migrating data from $sourcePlatform to $targetPlatform")
        
        // Emit migration started event
        _platformEvents.emit(PlatformEvent.MigrationStarted(sourcePlatform, targetPlatform))
        
        try {
            // Save current platform
            val currentPlatformType = currentPlatform.platformType
            
            // Switch to source platform
            switchPlatform(sourcePlatform)
            
            // Check if we're authenticated on source platform
            if (_authState.value !is AuthState.Authenticated) {
                Timber.e("Not authenticated on source platform")
                _platformEvents.emit(PlatformEvent.MigrationFailed("Not authenticated on source platform"))
                
                // Switch back to original platform
                switchPlatform(currentPlatformType)
                
                return Result.Error(Exception("Not authenticated on source platform"))
            }
            
            // Fetch data from source platform
            val sourceData = fetchDataForMigration(migrationConfig)
            
            // Switch to target platform
            switchPlatform(targetPlatform)
            
            // Check if we're authenticated on target platform
            if (_authState.value !is AuthState.Authenticated) {
                Timber.e("Not authenticated on target platform")
                _platformEvents.emit(PlatformEvent.MigrationFailed("Not authenticated on target platform"))
                
                // Switch back to original platform
                switchPlatform(currentPlatformType)
                
                return Result.Error(Exception("Not authenticated on target platform"))
            }
            
            // Upload data to target platform
            val migrationResult = uploadDataForMigration(sourceData, migrationConfig)
            
            // Switch back to original platform
            switchPlatform(currentPlatformType)
            
            // Emit migration completed event
            _platformEvents.emit(PlatformEvent.MigrationCompleted(migrationResult))
            
            return Result.Success(migrationResult)
        } catch (e: Exception) {
            Timber.e(e, "Migration failed")
            _platformEvents.emit(PlatformEvent.MigrationFailed(e.message ?: "Migration failed"))
            return Result.Error(e)
        }
    }
    
    /**
     * Check if device is online
     */
    fun isOnline(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
    
    /**
     * Create API service for platform
     */
    private fun createApiService(platformConfig: PlatformConfig): ApiService {
        Timber.d("Creating API service for platform: ${platformConfig.platformType}")
        
        // Create OkHttpClient with auth interceptor
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                
                // Add auth token if available
                val authState = _authState.value
                val request = if (authState is AuthState.Authenticated) {
                    originalRequest.newBuilder()
                        .header("Authorization", "Bearer ${authState.token}")
                        .build()
                } else {
                    originalRequest
                }
                
                chain.proceed(request)
            }
            .build()
        
        // Create Retrofit instance
        val retrofit = Retrofit.Builder()
            .baseUrl("${platformConfig.baseUrl}/api/${platformConfig.apiVersion}/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
        
        // Create API service
        return retrofit.create(ApiService::class.java)
    }
    
    /**
     * Create WebSocket service for platform
     */
    private fun createWebSocketService(platformConfig: PlatformConfig): WebSocketService {
        Timber.d("Creating WebSocket service for platform: ${platformConfig.platformType}")
        
        // Create WebSocket service
        return WebSocketServiceImpl(platformConfig)
    }
    
    /**
     * Save authentication tokens
     */
    private suspend fun saveAuthTokens(token: String, refreshToken: String, expiresAt: LocalDateTime) {
        Timber.d("Saving auth tokens")
        
        // Save tokens to preferences
        userPreferences.saveAuthToken(token)
        userPreferences.saveRefreshToken(refreshToken)
        userPreferences.saveTokenExpiryTime(expiresAt)
        
        // Update auth state
        _authState.value = AuthState.Authenticated(
            token = token,
            refreshToken = refreshToken,
            expiresAt = expiresAt
        )
    }
    
    /**
     * Clear authentication tokens
     */
    private suspend fun clearAuthTokens() {
        Timber.d("Clearing auth tokens")
        
        // Clear tokens from preferences
        userPreferences.clearAuthToken()
        userPreferences.clearRefreshToken()
        userPreferences.clearTokenExpiryTime()
        
        // Update auth state
        _authState.value = AuthState.NotAuthenticated
    }
    
    /**
     * Start token refresh timer
     */
    private fun startTokenRefreshTimer() {
        Timber.d("Starting token refresh timer")
        
        val authState = _authState.value
        if (authState !is AuthState.Authenticated) {
            return
        }
        
        platformScope.launch {
            try {
                // Calculate time until token expires
                val now = LocalDateTime.now()
                val expiresAt = authState.expiresAt
                
                // If token is already expired, refresh immediately
                if (now.isAfter(expiresAt)) {
                    Timber.d("Token already expired, refreshing immediately")
                    refreshToken()
                    return@launch
                }
                
                // Calculate delay until refresh (refresh 5 minutes before expiry)
                val delayMillis = java.time.Duration.between(now, expiresAt).toMillis() - (5 * 60 * 1000)
                
                // If delay is negative, refresh immediately
                if (delayMillis <= 0) {
                    Timber.d("Token expires soon, refreshing immediately")
                    refreshToken()
                    return@launch
                }
                
                // Wait until it's time to refresh
                Timber.d("Token expires in ${delayMillis}ms, scheduling refresh")
                delay(delayMillis)
                
                // Refresh token
                Timber.d("Refreshing token")
                refreshToken()
            } catch (e: Exception) {
                Timber.e(e, "Token refresh timer failed")
            }
        }
    }
    
    /**
     * Monitor device connectivity
     */
    private fun monitorConnectivity() {
        Timber.d("Starting connectivity monitoring")
        
        platformScope.launch {
            var wasOnline = isOnline()
            
            while (isActive) {
                val isOnlineNow = isOnline()
                
                // Emit event if connectivity changed
                if (isOnlineNow != wasOnline) {
                    if (isOnlineNow) {
                        Timber.d("Device is now online")
                        _platformEvents.emit(PlatformEvent.DeviceOnline)
                        
                        // Process offline queue
                        processOfflineQueue()
                        
                        // Reconnect WebSocket if needed
                        if (isFeatureSupported(PlatformFeature.REAL_TIME_MONITORING) && 
                            _authState.value is AuthState.Authenticated) {
                            connectWebSocket()
                        }
                    } else {
                        Timber.d("Device is now offline")
                        _platformEvents.emit(PlatformEvent.DeviceOffline)
                    }
                    
                    wasOnline = isOnlineNow
                }
                
                delay(5000) // Check every 5 seconds
            }
        }
    }
    
    /**
     * Monitor offline queue
     */
    private fun monitorOfflineQueue() {
        Timber.d("Starting offline queue monitoring")
        
        platformScope.launch {
            while (isActive) {
                // Process offline queue if online and authenticated
                if (isOnline() && _authState.value is AuthState.Authenticated) {
                    processOfflineQueue()
                }
                
                delay(60000) // Check every minute
            }
        }
    }
    
    /**
     * Queue data for offline processing
     */
    private fun queueOfflineData(type: String, data: Any) {
        Timber.d("Queueing offline data: $type")
        
        // Initialize queue for type if needed
        if (!offlineQueue.containsKey(type)) {
            offlineQueue[type] = mutableListOf()
        }
        
        // Add data to queue
        offlineQueue[type]!!.add(data)
        
        // Save queue size to preferences
        platformScope.launch {
            userPreferences.saveOfflineQueueSize(getOfflineQueueSize())
        }
    }
    
    /**
     * Process offline queue
     */
    private suspend fun processOfflineQueue() {
        Timber.d("Processing offline queue")
        
        // Check if we're online and authenticated
        if (!isOnline() || _authState.value !is AuthState.Authenticated) {
            Timber.d("Cannot process offline queue: offline or not authenticated")
            return
        }
        
        // Process vital readings
        processOfflineVitalReadings()
        
        // Process device registrations
        processOfflineDeviceRegistrations()
        
        // Save queue size to preferences
        userPreferences.saveOfflineQueueSize(getOfflineQueueSize())
    }
    
    /**
     * Process offline vital readings
     */
    private suspend fun processOfflineVitalReadings() {
        val readings = offlineQueue["vital_reading"]
        if (readings.isNullOrEmpty()) {
            return
        }
        
        Timber.d("Processing ${readings.size} offline vital readings")
        
        // Check if batch sync is supported
        if (isFeatureSupported(PlatformFeature.BATCH_SYNC)) {
            // Submit readings in batches
            val batchSize = 50
            val batches = readings.chunked(batchSize)
            
            for (batch in batches) {
                try {
                    val typedBatch = batch.filterIsInstance<VitalReadingDto>()
                    val result = getApiService().submitVitalReadingsBatch(typedBatch)
                    
                    if (result is Result.Success) {
                        // Remove successful readings from queue
                        readings.removeAll(typedBatch.toSet())
                        
                        // Emit event
                        _platformEvents.emit(PlatformEvent.OfflineDataSynced("vital_readings", typedBatch.size))
                    } else {
                        Timber.e("Failed to submit offline vital readings batch")
                        break
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to submit offline vital readings batch")
                    break
                }
            }
        } else {
            // Submit readings individually
            val iterator = readings.iterator()
            
            while (iterator.hasNext()) {
                val reading = iterator.next()
                
                if (reading is VitalReadingDto) {
                    try {
                        val result = getApiService().submitVitalReading(reading)
                        
                        if (result is Result.Success) {
                            // Remove from queue
                            iterator.remove()
                            
                            // Emit event
                            _platformEvents.emit(PlatformEvent.OfflineDataSynced("vital_reading", 1))
                        } else {
                            Timber.e("Failed to submit offline vital reading")
                            break
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to submit offline vital reading")
                        break
                    }
                } else {
                    // Invalid data type, remove from queue
                    iterator.remove()
                }
            }
        }
    }
    
    /**
     * Process offline device registrations
     */
    private suspend fun processOfflineDeviceRegistrations() {
        val registrations = offlineQueue["device_registration"]
        if (registrations.isNullOrEmpty()) {
            return
        }
        
        Timber.d("Processing ${registrations.size} offline device registrations")
        
        val iterator = registrations.iterator()
        
        while (iterator.hasNext()) {
            val registration = iterator.next()
            
            if (registration is DeviceRegistrationDto) {
                try {
                    val result = getApiService().registerDevice(registration)
                    
                    if (result is Result.Success) {
                        // Remove from queue
                        iterator.remove()
                        
                        // Emit event
                        _platformEvents.emit(PlatformEvent.OfflineDataSynced("device_registration", 1))
                    } else {
                        Timber.e("Failed to submit offline device registration")
                        break
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to submit offline device registration")
                    break
                }
            } else {
                // Invalid data type, remove from queue
                iterator.remove()
            }
        }
    }
    
    /**
     * Get offline queue size
     */
    private fun getOfflineQueueSize(): Int {
        return offlineQueue.values.sumOf { it.size }
    }
    
    /**
     * Perform full sync with platform
     */
    private suspend fun performFullSync(): Result<SyncResult> {
        Timber.d("Performing full sync")
        
        try {
            val startTime = LocalDateTime.now()
            var progress = 0
            
            // Update progress
            _syncStatus.value = SyncStatus.Syncing(progress, 100)
            
            // Fetch client vital configuration
            val clientId = userPreferences.getUserId() ?: return Result.Error(Exception("User ID not found"))
            val configResult = getApiService().getClientVitalConfiguration(clientId)
            
            if (configResult is Result.Error) {
                return Result.Error(Exception("Failed to fetch client vital configuration: ${configResult.exception.message}"))
            }
            
            progress += 10
            _syncStatus.value = SyncStatus.Syncing(progress, 100)
            
            // Fetch client thresholds
            val thresholdsResult = getApiService().getClientThresholds(clientId)
            
            if (thresholdsResult is Result.Error) {
                return Result.Error(Exception("Failed to fetch client thresholds: ${thresholdsResult.exception.message}"))
            }
            
            progress += 10
            _syncStatus.value = SyncStatus.Syncing(progress, 100)
            
            // Fetch client devices
            val devicesResult = getApiService().getClientDevices(clientId)
            
            if (devicesResult is Result.Error) {
                return Result.Error(Exception("Failed to fetch client devices: ${devicesResult.exception.message}"))
            }
            
            progress += 10
            _syncStatus.value = SyncStatus.Syncing(progress, 100)
            
            // Fetch client vitals
            val vitalsResult = getApiService().getClientVitals(
                clientId = clientId,
                timeRange = "30d", // Last 30 days
                includeDeviceStatus = true,
                includeAlerts = true
            )
            
            if (vitalsResult is Result.Error) {
                return Result.Error(Exception("Failed to fetch client vitals: ${vitalsResult.exception.message}"))
            }
            
            progress += 30
            _syncStatus.value = SyncStatus.Syncing(progress, 100)
            
            // Fetch client alerts
            val alertsResult = getApiService().getClientAlerts(
                clientId = clientId,
                startDate = LocalDateTime.now().minusDays(30).toString()
            )
            
            if (alertsResult is Result.Error) {
                return Result.Error(Exception("Failed to fetch client alerts: ${alertsResult.exception.message}"))
            }
            
            progress += 20
            _syncStatus.value = SyncStatus.Syncing(progress, 100)
            
            // Process offline queue
            processOfflineQueue()
            
            progress += 10
            _syncStatus.value = SyncStatus.Syncing(progress, 100)
            
            // Sync complete
            val endTime = LocalDateTime.now()
            val syncDuration = java.time.Duration.between(startTime, endTime).toMillis()
            
            val syncResult = SyncResult(
                startTime = startTime,
                endTime = endTime,
                durationMillis = syncDuration,
                syncType = SyncType.FULL,
                recordsProcessed = (vitalsResult as Result.Success).data.metadata.totalReadings,
                recordsFailed = 0,
                errors = emptyList()
            )
            
            _syncStatus.value = SyncStatus.Completed(syncResult)
            
            return Result.Success(syncResult)
        } catch (e: Exception) {
            Timber.e(e, "Full sync failed")
            return Result.Error(e)
        }
    }
    
    /**
     * Perform incremental sync with platform
     */
    private suspend fun performIncrementalSync(syncConfig: SyncConfig): Result<SyncResult> {
        Timber.d("Performing incremental sync")
        
        try {
            val startTime = LocalDateTime.now()
            var progress = 0
            
            // Update progress
            _syncStatus.value = SyncStatus.Syncing(progress, 100)
            
            // Fetch client vital configuration if needed
            val clientId = userPreferences.getUserId() ?: return Result.Error(Exception("User ID not found"))
            
            if (syncConfig.syncVitalConfiguration) {
                val configResult = getApiService().getClientVitalConfiguration(clientId)
                
                if (configResult is Result.Error) {
                    return Result.Error(Exception("Failed to fetch client vital configuration: ${configResult.exception.message}"))
                }
            }
            
            progress += 20
            _syncStatus.value = SyncStatus.Syncing(progress, 100)
            
            // Fetch client thresholds if needed
            if (syncConfig.syncThresholds) {
                val thresholdsResult = getApiService().getClientThresholds(clientId)
                
                if (thresholdsResult is Result.Error) {
                    return Result.Error(Exception("Failed to fetch client thresholds: ${thresholdsResult.exception.message}"))
                }
            }
            
            progress += 20
            _syncStatus.value = SyncStatus.Syncing(progress, 100)
            
            // Fetch client vitals if needed
            var recordsProcessed = 0
            
            if (syncConfig.syncVitals) {
                val timeRange = when (syncConfig.syncVitalsTimeRange) {
                    SyncTimeRange.LAST_24_HOURS -> "24h"
                    SyncTimeRange.LAST_7_DAYS -> "7d"
                    SyncTimeRange.LAST_30_DAYS -> "30d"
                    SyncTimeRange.CUSTOM -> syncConfig.customTimeRange ?: "24h"
                }
                
                val vitalsResult = getApiService().getClientVitals(
                    clientId = clientId,
                    timeRange = timeRange,
                    includeDeviceStatus = syncConfig.includeDeviceStatus,
                    includeAlerts = syncConfig.includeAlerts
                )
                
                if (vitalsResult is Result.Error) {
                    return Result.Error(Exception("Failed to fetch client vitals: ${vitalsResult.exception.message}"))
                }
                
                recordsProcessed = (vitalsResult as Result.Success).data.metadata.totalReadings
            }
            
            progress += 40
            _syncStatus.value = SyncStatus.Syncing(progress, 100)
            
            // Process offline queue
            processOfflineQueue()
            
            progress = 100
            _syncStatus.value = SyncStatus.Syncing(progress, 100)
            
            // Sync complete
            val endTime = LocalDateTime.now()
            val syncDuration = java.time.Duration.between(startTime, endTime).toMillis()
            
            val syncResult = SyncResult(
                startTime = startTime,
                endTime = endTime,
                durationMillis = syncDuration,
                syncType = SyncType.INCREMENTAL,
                recordsProcessed = recordsProcessed,
                recordsFailed = 0,
                errors = emptyList()
            )
            
            _syncStatus.value = SyncStatus.Completed(syncResult)
            
            return Result.Success(syncResult)
        } catch (e: Exception) {
            Timber.e(e, "Incremental sync failed")
            return Result.Error(e)
        }
    }
    
    /**
     * Handle WebSocket message
     */
    private suspend fun handleWebSocketMessage(message: WebSocketMessage) {
        Timber.d("Handling WebSocket message: ${message.type}")
        
        when (message.type) {
            "vital_threshold_updated" -> {
                _platformEvents.emit(PlatformEvent.ThresholdUpdated)
            }
            "alert_created" -> {
                _platformEvents.emit(PlatformEvent.AlertCreated)
            }
            "device_status_changed" -> {
                _platformEvents.emit(PlatformEvent.DeviceStatusChanged)
            }
            "sync_requested" -> {
                _platformEvents.emit(PlatformEvent.SyncRequested)
                startSync()
            }
            "logout_requested" -> {
                _platformEvents.emit(PlatformEvent.LogoutRequested)
                logout()
            }
            else -> {
                Timber.d("Unknown WebSocket message type: ${message.type}")
            }
        }
    }
    
    /**
     * Fetch data for migration
     */
    private suspend fun fetchDataForMigration(migrationConfig: MigrationConfig): MigrationData {
        Timber.d("Fetching data for migration")
        
        val clientId = userPreferences.getUserId() ?: throw Exception("User ID not found")
        
        // Fetch client vital configuration
        val configResult = getApiService().getClientVitalConfiguration(clientId)
        val configuration = if (configResult is Result.Success) configResult.data.configuration else null
        
        // Fetch client thresholds
        val thresholdsResult = getApiService().getClientThresholds(clientId)
        val thresholds = if (thresholdsResult is Result.Success) thresholdsResult.data.thresholds else null
        
        // Fetch client vitals
        val timeRange = when (migrationConfig.timeRange) {
            SyncTimeRange.LAST_24_HOURS -> "24h"
            SyncTimeRange.LAST_7_DAYS -> "7d"
            SyncTimeRange.LAST_30_DAYS -> "30d"
            SyncTimeRange.CUSTOM -> migrationConfig.customTimeRange ?: "30d"
        }
        
        val vitalsResult = getApiService().getClientVitals(
            clientId = clientId,
            timeRange = timeRange,
            includeDeviceStatus = true,
            includeAlerts = true
        )
        
        val readings = if (vitalsResult is Result.Success) {
            vitalsResult.data.readingsByType.flatMap { (_, typeReadings) ->
                typeReadings.day ?: emptyList()
            }
        } else {
            emptyList()
        }
        
        // Fetch client devices
        val devicesResult = getApiService().getClientDevices(clientId)
        val devices = if (devicesResult is Result.Success) devicesResult.data.devices else emptyList()
        
        return MigrationData(
            clientId = clientId,
            configuration = configuration,
            thresholds = thresholds,
            readings = readings,
            devices = devices
        )
    }
    
    /**
     * Upload data for migration
     */
    private suspend fun uploadDataForMigration(data: MigrationData, migrationConfig: MigrationConfig): MigrationResult {
        Timber.d("Uploading data for migration")
        
        val startTime = LocalDateTime.now()
        val errors = mutableListOf<String>()
        
        // Update client vital configuration
        if (data.configuration != null) {
            try {
                getApiService().updateClientVitalConfiguration(
                    clientId = data.clientId,
                    configuration = data.configuration
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to update client vital configuration")
                errors.add("Failed to update client vital configuration: ${e.message}")
            }
        }
        
        // Update client thresholds
        if (data.thresholds != null) {
            try {
                getApiService().updateClientThresholds(
                    clientId = data.clientId,
                    thresholds = ClientThresholdsDto(
                        clientId = data.clientId,
                        thresholds = data.thresholds
                    )
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to update client thresholds")
                errors.add("Failed to update client thresholds: ${e.message}")
            }
        }
        
        // Register devices
        var devicesRegistered = 0
        
        for (device in data.devices) {
            try {
                // Get device details from source platform
                val deviceDetailsResult = getApiService().getDeviceDetails(device.deviceId)
                
                if (deviceDetailsResult is Result.Success) {
                    val details = deviceDetailsResult.data
                    
                    // Register device on target platform
                    val registration = DeviceRegistrationDto(
                        deviceId = details.deviceId,
                        clientId = data.clientId,
                        deviceName = details.deviceName,
                        manufacturer = details.manufacturer,
                        model = details.model,
                        firmwareVersion = details.firmwareVersion,
                        serialNumber = details.serialNumber,
                        capabilities = details.capabilities
                    )
                    
                    getApiService().registerDevice(registration)
                    devicesRegistered++
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to register device: ${device.deviceId}")
                errors.add("Failed to register device ${device.deviceId}: ${e.message}")
            }
        }
        
        // Upload vital readings
        var readingsUploaded = 0
        
        if (data.readings.isNotEmpty() && isFeatureSupported(PlatformFeature.BATCH_SYNC)) {
            // Upload in batches
            val batchSize = 50
            val batches = data.readings.chunked(batchSize)
            
            for (batch in batches) {
                try {
                    val result = getApiService().submitVitalReadingsBatch(batch)
                    
                    if (result is Result.Success) {
                        readingsUploaded += result.data.successCount
                    } else if (result is Result.Error) {
                        Timber.e(result.exception, "Failed to upload vital readings batch")
                        errors.add("Failed to upload vital readings batch: ${result.exception.message}")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to upload vital readings batch")
                    errors.add("Failed to upload vital readings batch: ${e.message}")
                }
            }
        } else {
            // Upload individually
            for (reading in data.readings) {
                try {
                    val result = getApiService().submitVitalReading(reading)
                    
                    if (result is Result.Success) {
                        readingsUploaded++
                    } else if (result is Result.Error) {
                        Timber.e(result.exception, "Failed to upload vital reading")
                        errors.add("Failed to upload vital reading: ${result.exception.message}")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to upload vital reading")
                    errors.add("Failed to upload vital reading: ${e.message}")
                }
            }
        }
        
        val endTime = LocalDateTime.now()
        val durationMillis = java.time.Duration.between(startTime, endTime).toMillis()
        
        return MigrationResult(
            sourcePlatform = currentPlatform.platformType,
            targetPlatform = currentPlatform.platformType,
            startTime = startTime,
            endTime = endTime,
            durationMillis = durationMillis,
            devicesRegistered = devicesRegistered,
            readingsUploaded = readingsUploaded,
            configurationMigrated = data.configuration != null,
            thresholdsMigrated = data.thresholds != null,
            errors = errors
        )
    }
}

/**
 * WebSocket service implementation
 */
class WebSocketServiceImpl(private val platformConfig: PlatformConfig) : WebSocketService {
    
    private val tag = "WebSocketService"
    
    private var webSocket: okhttp3.WebSocket? = null
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // No timeout for WebSocket
        .build()
    
    private var isConnected = false
    
    override fun connect(platformConfig: PlatformConfig, authToken: String): Flow<WebSocketEvent> = flow {
        Timber.d("Connecting to WebSocket: ${platformConfig.wsEndpoint}")
        
        try {
            // Create flow collector
            val events = MutableSharedFlow<WebSocketEvent>(extraBufferCapacity = 50)
            
            // Create WebSocket request
            val request = okhttp3.Request.Builder()
                .url(platformConfig.wsEndpoint)
                .addHeader("Authorization", "Bearer $authToken")
                .build()
            
            // Create WebSocket listener
            val listener = object : okhttp3.WebSocketListener() {
                override fun onOpen(webSocket: okhttp3.WebSocket, response: okhttp3.Response) {
                    Timber.d("WebSocket connected")
                    isConnected = true
                    
                    // Emit connected event
                    platformScope.launch {
                        events.emit(WebSocketEvent.Connected(LocalDateTime.now()))
                    }
                }
                
                override fun onMessage(webSocket: okhttp3.WebSocket, text: String) {
                    Timber.d("WebSocket message received: $text")
                    
                    // Parse message
                    try {
                        val message = parseWebSocketMessage(text)
                        
                        // Emit message received event
                        platformScope.launch {
                            events.emit(WebSocketEvent.MessageReceived(message, LocalDateTime.now()))
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to parse WebSocket message")
                        
                        // Emit error event
                        platformScope.launch {
                            events.emit(WebSocketEvent.Error(e, LocalDateTime.now()))
                        }
                    }
                }
                
                override fun onClosing(webSocket: okhttp3.WebSocket, code: Int, reason: String) {
                    Timber.d("WebSocket closing: $code, $reason")
                    
                    // Close WebSocket
                    webSocket.close(code, reason)
                }
                
                override fun onClosed(webSocket: okhttp3.WebSocket, code: Int, reason: String) {
                    Timber.d("WebSocket closed: $code, $reason")
                    isConnected = false
                    
                    // Emit disconnected event
                    platformScope.launch {
                        events.emit(WebSocketEvent.Disconnected(reason, LocalDateTime.now()))
                    }
                }
                
                override fun onFailure(webSocket: okhttp3.WebSocket, t: Throwable, response: okhttp3.Response?) {
                    Timber.e(t, "WebSocket failure")
                    isConnected = false
                    
                    // Emit error event
                    platformScope.launch {
                        events.emit(WebSocketEvent.Error(t, LocalDateTime.now()))
                        events.emit(WebSocketEvent.Disconnected("Error: ${t.message}", LocalDateTime.now()))
                    }
                }
            }
            
            // Connect WebSocket
            webSocket = client.newWebSocket(request, listener)
            
            // Collect and forward events
            events.collect {
                emit(it)
            }
        } catch (e: Exception) {
            Timber.e(e, "WebSocket connection failed")
            emit(WebSocketEvent.Error(e, LocalDateTime.now()))
            emit(WebSocketEvent.Disconnected("Connection failed: ${e.message}", LocalDateTime.now()))
        }
    }
    
    override fun disconnect() {
        Timber.d("Disconnecting WebSocket")
        
        webSocket?.close(1000, "User requested")
        webSocket = null
        isConnected = false
    }
    
    override fun sendMessage(message: WebSocketMessage) {
        Timber.d("Sending WebSocket message: ${message.type}")
        
        if (!isConnected || webSocket == null) {
            Timber.e("Cannot send message: WebSocket not connected")
            return
        }
        
        try {
            // Convert message to JSON
            val json = """
                {
                    "type": "${message.type}",
                    "payload": ${message.payload},
                    "timestamp": "${message.timestamp}"
                }
            """.trimIndent()
            
            // Send message
            webSocket?.send(json)
        } catch (e: Exception) {
            Timber.e(e, "Failed to send WebSocket message")
        }
    }
    
    override fun isConnected(): Boolean {
        return isConnected
    }
    
    private fun parseWebSocketMessage(text: String): WebSocketMessage {
        // Parse JSON message
        val json = org.json.JSONObject(text)
        val type = json.getString("type")
        val payload = json.get("payload")
        val timestamp = json.optString("timestamp")
        
        // Parse timestamp
        val parsedTimestamp = if (timestamp.isNotEmpty()) {
            try {
                LocalDateTime.parse(timestamp)
            } catch (e: Exception) {
                LocalDateTime.now()
            }
        } else {
            LocalDateTime.now()
        }
        
        return WebSocketMessage(
            type = type,
            payload = payload,
            timestamp = parsedTimestamp
        )
    }
    
    companion object {
        private val platformScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}

/**
 * Authentication state sealed class
 */
sealed class AuthState {
    object NotAuthenticated : AuthState()
    object Authenticating : AuthState()
    data class Authenticated(
        val token: String,
        val refreshToken: String,
        val expiresAt: LocalDateTime
    ) : AuthState()
    data class Error(val message: String) : AuthState()
}

/**
 * Sync status sealed class
 */
sealed class SyncStatus {
    object Idle : SyncStatus()
    data class Syncing(val progress: Int, val total: Int) : SyncStatus()
    data class Completed(val result: SyncResult) : SyncStatus()
    data class Error(val message: String) : SyncStatus()
}

/**
 * Sync result data class
 */
data class SyncResult(
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val durationMillis: Long,
    val syncType: SyncType,
    val recordsProcessed: Int,
    val recordsFailed: Int,
    val errors: List<String>
)

/**
 * Sync type enum
 */
enum class SyncType {
    FULL,
    INCREMENTAL
}

/**
 * Sync config data class
 */
data class SyncConfig(
    val fullSync: Boolean = false,
    val syncVitals: Boolean = true,
    val syncVitalsTimeRange: SyncTimeRange = SyncTimeRange.LAST_24_HOURS,
    val customTimeRange: String? = null,
    val syncVitalConfiguration: Boolean = true,
    val syncThresholds: Boolean = true,
    val includeDeviceStatus: Boolean = true,
    val includeAlerts: Boolean = true,
    val periodicSync: Boolean = false,
    val syncInterval: Long = 15 * 60 * 1000 // 15 minutes
)

/**
 * Sync time range enum
 */
enum class SyncTimeRange {
    LAST_24_HOURS,
    LAST_7_DAYS,
    LAST_30_DAYS,
    CUSTOM
}

/**
 * Migration config data class
 */
data class MigrationConfig(
    val timeRange: SyncTimeRange = SyncTimeRange.LAST_30_DAYS,
    val customTimeRange: String? = null,
    val includeConfiguration: Boolean = true,
    val includeThresholds: Boolean = true,
    val includeDevices: Boolean = true,
    val includeVitals: Boolean = true
)

/**
 * Migration data data class
 */
data class MigrationData(
    val clientId: String,
    val configuration: ClientVitalConfigurationDto?,
    val thresholds: Map<String, ThresholdValues>?,
    val readings: List<VitalReadingDto>,
    val devices: List<DeviceDto>
)

/**
 * Migration result data class
 */
data class MigrationResult(
    val sourcePlatform: PlatformType,
    val targetPlatform: PlatformType,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val durationMillis: Long,
    val devicesRegistered: Int,
    val readingsUploaded: Int,
    val configurationMigrated: Boolean,
    val thresholdsMigrated: Boolean,
    val errors: List<String>
)

/**
 * Platform event sealed class
 */
sealed class PlatformEvent {
    data class PlatformSwitched(val platformType: PlatformType) : PlatformEvent()
    data class LoggedIn(val platformType: PlatformType) : PlatformEvent()
    data class LoggedOut(val platformType: PlatformType) : PlatformEvent()
    data class AuthenticationFailed(val reason: String) : PlatformEvent()
    object TokenRefreshed : PlatformEvent()
    data class TokenRefreshFailed(val reason: String) : PlatformEvent()
    object SyncStarted : PlatformEvent()
    data class SyncCompleted(val result: SyncResult) : PlatformEvent()
    data class SyncFailed(val reason: String) : PlatformEvent()
    object SyncStopped : PlatformEvent()
    object SyncRequested : PlatformEvent()
    object WebSocketConnected : PlatformEvent()
    data class WebSocketDisconnected(val reason: String) : PlatformEvent()
    data class WebSocketError(val message: String) : PlatformEvent()
    object DeviceOnline : PlatformEvent()
    object DeviceOffline : PlatformEvent()
    data class VitalReadingSubmitted(val vitalType: String) : PlatformEvent()
    data class VitalReadingBatchSubmitted(val count: Int) : PlatformEvent()
    data class DeviceRegistered(val deviceId: String) : PlatformEvent()
    data class OfflineDataSynced(val type: String, val count: Int) : PlatformEvent()
    object ThresholdUpdated : PlatformEvent()
    object AlertCreated : PlatformEvent()
    object DeviceStatusChanged : PlatformEvent()
    object LogoutRequested : PlatformEvent()
    data class MigrationStarted(val sourcePlatform: PlatformType, val targetPlatform: PlatformType) : PlatformEvent()
    data class MigrationCompleted(val result: MigrationResult) : PlatformEvent()
    data class MigrationFailed(val reason: String) : PlatformEvent()
}
