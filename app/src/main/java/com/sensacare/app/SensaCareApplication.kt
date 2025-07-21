package com.sensacare.app

import android.app.Application
import android.os.Build
import android.os.StrictMode
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.*
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.sensacare.app.data.local.backup.DatabaseBackupManager
import com.sensacare.app.data.local.encryption.DatabaseEncryptionManager
import com.sensacare.app.data.network.connectivity.NetworkConnectivityMonitor
import com.sensacare.app.data.sync.SyncScheduler
import com.sensacare.app.domain.model.FeatureFlag
import com.sensacare.app.notification.NotificationChannelManager
import com.sensacare.app.permission.NotificationPermissionManager
import com.sensacare.app.util.AppStartupTracker
import com.sensacare.app.util.BackgroundWorkScheduler
import com.sensacare.app.util.DebugTools
import com.sensacare.app.util.ErrorTracker
import com.sensacare.app.util.LocaleManager
import com.sensacare.app.util.MemoryManager
import com.sensacare.app.util.PerformanceMonitor
import com.sensacare.app.util.analytics.AnalyticsManager
import com.sensacare.app.util.config.FeatureFlagManager
import com.sensacare.app.util.security.BiometricAuthManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named

/**
 * SensaCareApplication - Main application class for the SensaCare health monitoring app
 *
 * This class handles application-level initialization and configuration:
 * - Dependency injection setup with Hilt
 * - Logging and error tracking configuration
 * - Notification system initialization
 * - Background work scheduling
 * - Database and security setup
 * - Feature flags and configuration management
 * - Application lifecycle monitoring
 * - Performance optimization
 */
@HiltAndroidApp
class SensaCareApplication : Application(), DefaultLifecycleObserver, Configuration.Provider {

    // Injected dependencies
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var timberTree: Timber.Tree

    @Inject
    lateinit var firebaseCrashlytics: FirebaseCrashlytics

    @Inject
    lateinit var firebaseAnalytics: FirebaseAnalytics

    @Inject
    lateinit var analyticsManager: AnalyticsManager

    @Inject
    lateinit var errorTracker: ErrorTracker

    @Inject
    lateinit var notificationChannelManager: NotificationChannelManager

    @Inject
    lateinit var notificationPermissionManager: NotificationPermissionManager

    @Inject
    lateinit var workManager: WorkManager

    @Inject
    lateinit var backgroundWorkScheduler: BackgroundWorkScheduler

    @Inject
    lateinit var syncScheduler: SyncScheduler

    @Inject
    lateinit var featureFlagManager: FeatureFlagManager

    @Inject
    lateinit var databaseEncryptionManager: DatabaseEncryptionManager

    @Inject
    lateinit var databaseBackupManager: DatabaseBackupManager

    @Inject
    lateinit var biometricAuthManager: BiometricAuthManager

    @Inject
    lateinit var networkConnectivityMonitor: NetworkConnectivityMonitor

    @Inject
    lateinit var localeManager: LocaleManager

    @Inject
    lateinit var debugTools: DebugTools

    @Inject
    lateinit var performanceMonitor: PerformanceMonitor

    @Inject
    lateinit var memoryManager: MemoryManager

    @Inject
    @Named("applicationScope")
    lateinit var applicationScope: CoroutineScope

    // App startup performance tracking
    private val appStartupTracker = AppStartupTracker()

    override fun onCreate() {
        // Start tracking app startup time
        appStartupTracker.trackStartupBegin()

        // Enable strict mode in debug builds
        setupStrictMode()

        super.onCreate()

        // Initialize Timber for logging
        setupLogging()

        // Initialize error tracking and analytics
        setupErrorTracking()

        // Register as lifecycle observer to monitor app state
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        // Create notification channels
        setupNotificationChannels()

        // Initialize feature flags
        initializeFeatureFlags()

        // Schedule background work
        setupBackgroundWork()

        // Initialize security features
        setupSecurity()

        // Configure theme based on settings
        setupTheme()

        // Initialize network monitoring
        networkConnectivityMonitor.startMonitoring()

        // Apply user's locale preference
        localeManager.applyPreferredLocale()

        // Initialize debug tools in debug builds
        if (BuildConfig.DEBUG) {
            debugTools.initialize()
        }

        // Start performance monitoring
        performanceMonitor.startMonitoring()

        // Track app startup completion
        appStartupTracker.trackStartupComplete()
        analyticsManager.trackAppStartupTime(appStartupTracker.getStartupDuration())

        Timber.i("SensaCare application initialized in ${appStartupTracker.getStartupDuration()}ms")
    }

    /**
     * Configure WorkManager for background tasks
     */
    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) Log.DEBUG else Log.INFO)
            .setJobSchedulerJobIdRange(1000, 2000)
            .build()
    }

    /**
     * Set up StrictMode for detecting potential issues in debug builds
     */
    private fun setupStrictMode() {
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .detectActivityLeaks()
                    .detectFileUriExposure()
                    .apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            detectNonSdkApiUsage()
                        }
                    }
                    .penaltyLog()
                    .build()
            )
        }
    }

    /**
     * Initialize Timber logging
     */
    private fun setupLogging() {
        Timber.plant(timberTree)
        Timber.d("Logging initialized")
    }

    /**
     * Configure error tracking and crash reporting
     */
    private fun setupErrorTracking() {
        // Set user properties for crash reports
        firebaseCrashlytics.setCustomKey("build_type", BuildConfig.BUILD_TYPE)
        firebaseCrashlytics.setCustomKey("version_name", BuildConfig.VERSION_NAME)
        firebaseCrashlytics.setCustomKey("version_code", BuildConfig.VERSION_CODE)

        // Set up global exception handler
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Timber.e(throwable, "Uncaught exception in thread: ${thread.name}")
            errorTracker.trackFatalError(throwable)
            firebaseCrashlytics.recordException(throwable)
            // Allow the default handler to terminate the app
            Thread.getDefaultUncaughtExceptionHandler()?.uncaughtException(thread, throwable)
        }

        Timber.d("Error tracking initialized")
    }

    /**
     * Create notification channels for different types of notifications
     */
    private fun setupNotificationChannels() {
        applicationScope.launch {
            notificationChannelManager.createAllChannels()
            
            // Request notification permission on Android 13+ if needed
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionManager.requestNotificationPermissionIfNeeded()
            }
            
            Timber.d("Notification channels created")
        }
    }

    /**
     * Initialize feature flags and configurations
     */
    private fun initializeFeatureFlags() {
        applicationScope.launch {
            featureFlagManager.initialize()
            
            // Log enabled features for debugging
            featureFlagManager.getAllFeatureFlags().forEach { (flag, enabled) ->
                Timber.d("Feature flag: $flag = $enabled")
            }
            
            // Apply feature-specific configurations
            applyFeatureConfigurations()
        }
    }

    /**
     * Apply configurations based on enabled features
     */
    private fun applyFeatureConfigurations() {
        // Configure features based on flags
        if (featureFlagManager.isFeatureEnabled(FeatureFlag.ENABLE_CLOUD_BACKUP)) {
            applicationScope.launch {
                databaseBackupManager.scheduleAutomaticBackups()
                Timber.d("Cloud backup scheduled")
            }
        }
        
        if (featureFlagManager.isFeatureEnabled(FeatureFlag.ENABLE_ADVANCED_ANALYTICS)) {
            analyticsManager.setDetailedAnalyticsEnabled(true)
            Timber.d("Advanced analytics enabled")
        }
    }

    /**
     * Schedule background work for data synchronization and maintenance
     */
    private fun setupBackgroundWork() {
        applicationScope.launch {
            // Schedule health data sync
            syncScheduler.schedulePeriodicSync(
                intervalMinutes = 15,
                requiresCharging = false,
                requiresDeviceIdle = false
            )
            
            // Schedule database maintenance
            backgroundWorkScheduler.scheduleDatabaseMaintenance(
                repeatInterval = 1,
                repeatIntervalTimeUnit = TimeUnit.DAYS
            )
            
            // Schedule daily health report generation
            backgroundWorkScheduler.scheduleHealthReportGeneration(
                repeatInterval = 1,
                repeatIntervalTimeUnit = TimeUnit.DAYS
            )
            
            Timber.d("Background work scheduled")
        }
    }

    /**
     * Initialize security features
     */
    private fun setupSecurity() {
        applicationScope.launch {
            // Initialize biometric authentication if enabled
            if (featureFlagManager.isFeatureEnabled(FeatureFlag.ENABLE_BIOMETRIC_AUTH)) {
                biometricAuthManager.initialize()
                Timber.d("Biometric authentication initialized")
            }
            
            // Verify database encryption
            databaseEncryptionManager.verifyEncryption()
            Timber.d("Database encryption verified")
        }
    }

    /**
     * Configure app theme based on settings
     */
    private fun setupTheme() {
        applicationScope.launch {
            // Set night mode based on user preference
            val darkModeEnabled = featureFlagManager.isFeatureEnabled(FeatureFlag.ENABLE_DARK_MODE)
            val nightMode = if (darkModeEnabled) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_NO
            }
            AppCompatDelegate.setDefaultNightMode(nightMode)
            Timber.d("Theme configured, dark mode: $darkModeEnabled")
        }
    }

    /**
     * Application lifecycle event: app in foreground
     */
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        Timber.d("App moved to foreground")
        
        // Refresh data when app comes to foreground
        applicationScope.launch {
            syncScheduler.requestImmediateSync()
        }
        
        // Track app open event
        analyticsManager.trackAppOpen()
    }

    /**
     * Application lifecycle event: app in background
     */
    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        Timber.d("App moved to background")
        
        // Perform cleanup when app goes to background
        applicationScope.launch {
            databaseBackupManager.backupIfNeeded()
        }
    }

    /**
     * Handle low memory situations
     */
    override fun onLowMemory() {
        super.onLowMemory()
        Timber.w("Low memory warning received")
        
        // Handle low memory situation
        memoryManager.handleLowMemory()
    }

    /**
     * Handle trim memory requests
     */
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        Timber.d("Trim memory request received, level: $level")
        
        // Handle trim memory request
        memoryManager.handleTrimMemory(level)
    }

    /**
     * Clean up resources when application is terminated
     */
    override fun onTerminate() {
        super.onTerminate()
        Timber.d("Application terminating")
        
        // Unregister lifecycle observer
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
        
        // Stop monitoring
        networkConnectivityMonitor.stopMonitoring()
        performanceMonitor.stopMonitoring()
    }

    companion object {
        // Constants for the application
        const val DATABASE_NAME = "sensacare_db"
        const val SHARED_PREFS_NAME = "sensacare_prefs"
        const val WORK_TAG_SYNC = "health_data_sync"
        const val WORK_TAG_BACKUP = "database_backup"
        const val WORK_TAG_MAINTENANCE = "database_maintenance"
    }
}
