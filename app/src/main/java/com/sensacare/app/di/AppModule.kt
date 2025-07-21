package com.sensacare.app.di

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceManager
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.WorkManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.sensacare.app.BuildConfig
import com.sensacare.app.SensaCareApplication
import com.sensacare.app.data.local.encryption.EncryptionManager
import com.sensacare.app.data.local.preferences.PreferencesManager
import com.sensacare.app.data.local.preferences.SecurePreferencesManager
import com.sensacare.app.data.network.connectivity.NetworkConnectivityMonitor
import com.sensacare.app.domain.model.FeatureFlag
import com.sensacare.app.notification.NotificationChannelManager
import com.sensacare.app.notification.NotificationManager
import com.sensacare.app.permission.BlePermissionManager
import com.sensacare.app.permission.LocationPermissionManager
import com.sensacare.app.permission.NotificationPermissionManager
import com.sensacare.app.permission.PermissionManager
import com.sensacare.app.util.*
import com.sensacare.app.util.analytics.AnalyticsManager
import com.sensacare.app.util.analytics.HealthAnalyticsTracker
import com.sensacare.app.util.chart.ChartDataFormatter
import com.sensacare.app.util.chart.HealthDataVisualizer
import com.sensacare.app.util.config.ConfigurationManager
import com.sensacare.app.util.config.FeatureFlagManager
import com.sensacare.app.util.device.BleDeviceCompatibilityChecker
import com.sensacare.app.util.device.DeviceCapabilityChecker
import com.sensacare.app.util.health.HealthDataProcessor
import com.sensacare.app.util.health.HealthMetricsCalculator
import com.sensacare.app.util.health.HealthTrendAnalyzer
import com.sensacare.app.util.security.BiometricAuthManager
import com.sensacare.app.util.security.DataEncryptionHelper
import com.sensacare.app.util.time.DateTimeFormatter
import com.sensacare.app.util.time.TimeZoneHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import timber.log.Timber
import java.time.Clock
import java.time.ZoneId
import java.util.Locale
import javax.inject.Named
import javax.inject.Singleton

/**
 * AppModule - Main application dependency injection module for SensaCare app
 *
 * This module provides application-level dependencies and singletons:
 * - Coroutine dispatchers and scopes for background processing
 * - Logging and debugging tools
 * - Error tracking and analytics
 * - Notification management
 * - Background work configuration
 * - Date/time utilities
 * - Permission management
 * - Security utilities
 * - Feature flags and configuration
 * - Health data processing utilities
 * - Chart and visualization helpers
 * - Preferences and settings
 * - Device compatibility checking
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Coroutine Dispatchers and Scopes
     */
    
    @Provides
    @Singleton
    @Named("ioDispatcher")
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
    
    @Provides
    @Singleton
    @Named("defaultDispatcher")
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
    
    @Provides
    @Singleton
    @Named("mainDispatcher")
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main
    
    @Provides
    @Singleton
    @Named("applicationScope")
    fun provideApplicationScope(
        @Named("defaultDispatcher") defaultDispatcher: CoroutineDispatcher
    ): CoroutineScope = CoroutineScope(SupervisorJob() + defaultDispatcher)
    
    /**
     * Logging and Debugging Tools
     */
    
    @Provides
    @Singleton
    fun provideTimberTree(): Timber.Tree {
        return if (BuildConfig.DEBUG) {
            Timber.DebugTree()
        } else {
            object : Timber.Tree() {
                override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                    // In production, we might want to log only warnings and errors to Crashlytics
                    if (priority >= android.util.Log.WARN) {
                        FirebaseCrashlytics.getInstance().log("$tag: $message")
                        t?.let { FirebaseCrashlytics.getInstance().recordException(it) }
                    }
                }
            }
        }
    }
    
    @Provides
    @Singleton
    fun provideDebugTools(@ApplicationContext context: Context): DebugTools {
        return DebugTools(context, BuildConfig.DEBUG)
    }
    
    /**
     * Error Tracking and Analytics
     */
    
    @Provides
    @Singleton
    fun provideFirebaseCrashlytics(): FirebaseCrashlytics {
        val crashlytics = FirebaseCrashlytics.getInstance()
        // Configure Crashlytics
        crashlytics.setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
        return crashlytics
    }
    
    @Provides
    @Singleton
    fun provideFirebaseAnalytics(@ApplicationContext context: Context): FirebaseAnalytics {
        val analytics = FirebaseAnalytics.getInstance(context)
        // Configure Analytics
        analytics.setAnalyticsCollectionEnabled(!BuildConfig.DEBUG)
        return analytics
    }
    
    @Provides
    @Singleton
    fun provideAnalyticsManager(
        firebaseAnalytics: FirebaseAnalytics,
        @Named("applicationScope") applicationScope: CoroutineScope
    ): AnalyticsManager {
        return AnalyticsManager(firebaseAnalytics, applicationScope)
    }
    
    @Provides
    @Singleton
    fun provideHealthAnalyticsTracker(
        analyticsManager: AnalyticsManager,
        @Named("applicationScope") applicationScope: CoroutineScope
    ): HealthAnalyticsTracker {
        return HealthAnalyticsTracker(analyticsManager, applicationScope)
    }
    
    @Provides
    @Singleton
    fun provideErrorTracker(
        firebaseCrashlytics: FirebaseCrashlytics,
        @Named("applicationScope") applicationScope: CoroutineScope
    ): ErrorTracker {
        return ErrorTracker(firebaseCrashlytics, applicationScope)
    }
    
    /**
     * Notification Management
     */
    
    @Provides
    @Singleton
    fun provideNotificationManagerCompat(@ApplicationContext context: Context): NotificationManagerCompat {
        return NotificationManagerCompat.from(context)
    }
    
    @Provides
    @Singleton
    fun provideNotificationChannelManager(
        @ApplicationContext context: Context,
        notificationManagerCompat: NotificationManagerCompat
    ): NotificationChannelManager {
        return NotificationChannelManager(context, notificationManagerCompat)
    }
    
    @Provides
    @Singleton
    fun provideNotificationManager(
        @ApplicationContext context: Context,
        notificationManagerCompat: NotificationManagerCompat,
        notificationChannelManager: NotificationChannelManager,
        @Named("applicationScope") applicationScope: CoroutineScope
    ): NotificationManager {
        return NotificationManager(
            context, 
            notificationManagerCompat, 
            notificationChannelManager,
            applicationScope
        )
    }
    
    /**
     * Background Work Configuration
     */
    
    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }
    
    @Provides
    @Singleton
    fun provideWorkConstraints(): Constraints {
        return Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
    }
    
    @Provides
    @Singleton
    fun provideBackgroundWorkScheduler(
        workManager: WorkManager,
        constraints: Constraints,
        @Named("applicationScope") applicationScope: CoroutineScope
    ): BackgroundWorkScheduler {
        return BackgroundWorkScheduler(workManager, constraints, applicationScope)
    }
    
    /**
     * Date/Time Utilities
     */
    
    @Provides
    @Singleton
    fun provideClock(): Clock {
        return Clock.systemDefaultZone()
    }
    
    @Provides
    @Singleton
    fun provideZoneId(): ZoneId {
        return ZoneId.systemDefault()
    }
    
    @Provides
    @Singleton
    fun provideDateTimeFormatter(
        clock: Clock,
        zoneId: ZoneId
    ): DateTimeFormatter {
        return DateTimeFormatter(clock, zoneId)
    }
    
    @Provides
    @Singleton
    fun provideTimeZoneHelper(): TimeZoneHelper {
        return TimeZoneHelper()
    }
    
    /**
     * Permission Management
     */
    
    @Provides
    @Singleton
    fun providePermissionManager(@ApplicationContext context: Context): PermissionManager {
        return PermissionManager(context)
    }
    
    @Provides
    @Singleton
    fun provideBlePermissionManager(
        @ApplicationContext context: Context,
        permissionManager: PermissionManager
    ): BlePermissionManager {
        return BlePermissionManager(context, permissionManager)
    }
    
    @Provides
    @Singleton
    fun provideLocationPermissionManager(
        @ApplicationContext context: Context,
        permissionManager: PermissionManager
    ): LocationPermissionManager {
        return LocationPermissionManager(context, permissionManager)
    }
    
    @Provides
    @Singleton
    fun provideNotificationPermissionManager(
        @ApplicationContext context: Context,
        permissionManager: PermissionManager
    ): NotificationPermissionManager {
        return NotificationPermissionManager(context, permissionManager)
    }
    
    /**
     * Security Utilities
     */
    
    @Provides
    @Singleton
    fun provideMasterKey(@ApplicationContext context: Context): MasterKey {
        return MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }
    
    @Provides
    @Singleton
    @Named("encryptedSharedPreferences")
    fun provideEncryptedSharedPreferences(
        @ApplicationContext context: Context,
        masterKey: MasterKey
    ): SharedPreferences {
        return EncryptedSharedPreferences.create(
            context,
            "sensacare_encrypted_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    @Provides
    @Singleton
    fun provideEncryptionManager(
        @ApplicationContext context: Context,
        masterKey: MasterKey
    ): EncryptionManager {
        return EncryptionManager(context, masterKey)
    }
    
    @Provides
    @Singleton
    fun provideDataEncryptionHelper(
        encryptionManager: EncryptionManager
    ): DataEncryptionHelper {
        return DataEncryptionHelper(encryptionManager)
    }
    
    @Provides
    @Singleton
    fun provideBiometricAuthManager(@ApplicationContext context: Context): BiometricAuthManager {
        return BiometricAuthManager(context)
    }
    
    /**
     * Feature Flags and Configuration
     */
    
    @Provides
    @Singleton
    @Named("defaultSharedPreferences")
    fun provideDefaultSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }
    
    @Provides
    @Singleton
    fun providePreferencesManager(
        @Named("defaultSharedPreferences") sharedPreferences: SharedPreferences
    ): PreferencesManager {
        return PreferencesManager(sharedPreferences)
    }
    
    @Provides
    @Singleton
    fun provideSecurePreferencesManager(
        @Named("encryptedSharedPreferences") encryptedSharedPreferences: SharedPreferences
    ): SecurePreferencesManager {
        return SecurePreferencesManager(encryptedSharedPreferences)
    }
    
    @Provides
    @Singleton
    fun provideFeatureFlagManager(
        preferencesManager: PreferencesManager,
        @Named("applicationScope") applicationScope: CoroutineScope
    ): FeatureFlagManager {
        val defaultFlags = mapOf(
            FeatureFlag.ENABLE_HEALTH_INSIGHTS to true,
            FeatureFlag.ENABLE_EMERGENCY_ALERTS to true,
            FeatureFlag.ENABLE_SLEEP_TRACKING to true,
            FeatureFlag.ENABLE_STRESS_MONITORING to true,
            FeatureFlag.ENABLE_ADVANCED_ANALYTICS to false,
            FeatureFlag.ENABLE_MULTI_DEVICE_SYNC to true,
            FeatureFlag.ENABLE_DARK_MODE to true,
            FeatureFlag.ENABLE_BIOMETRIC_AUTH to true,
            FeatureFlag.ENABLE_CLOUD_BACKUP to false,
            FeatureFlag.ENABLE_GOAL_RECOMMENDATIONS to true,
            FeatureFlag.ENABLE_DEVICE_FIRMWARE_UPDATES to true
        )
        return FeatureFlagManager(preferencesManager, defaultFlags, applicationScope)
    }
    
    @Provides
    @Singleton
    fun provideConfigurationManager(
        @ApplicationContext context: Context,
        preferencesManager: PreferencesManager,
        @Named("applicationScope") applicationScope: CoroutineScope
    ): ConfigurationManager {
        return ConfigurationManager(context, preferencesManager, applicationScope)
    }
    
    /**
     * Health Data Processing Utilities
     */
    
    @Provides
    @Singleton
    fun provideHealthDataProcessor(
        @Named("defaultDispatcher") defaultDispatcher: CoroutineDispatcher
    ): HealthDataProcessor {
        return HealthDataProcessor(defaultDispatcher)
    }
    
    @Provides
    @Singleton
    fun provideHealthMetricsCalculator(
        healthDataProcessor: HealthDataProcessor
    ): HealthMetricsCalculator {
        return HealthMetricsCalculator(healthDataProcessor)
    }
    
    @Provides
    @Singleton
    fun provideHealthTrendAnalyzer(
        healthMetricsCalculator: HealthMetricsCalculator,
        @Named("defaultDispatcher") defaultDispatcher: CoroutineDispatcher
    ): HealthTrendAnalyzer {
        return HealthTrendAnalyzer(healthMetricsCalculator, defaultDispatcher)
    }
    
    /**
     * Chart and Visualization Helpers
     */
    
    @Provides
    @Singleton
    fun provideChartDataFormatter(
        dateTimeFormatter: DateTimeFormatter,
        @ApplicationContext context: Context
    ): ChartDataFormatter {
        return ChartDataFormatter(dateTimeFormatter, context)
    }
    
    @Provides
    @Singleton
    fun provideHealthDataVisualizer(
        chartDataFormatter: ChartDataFormatter,
        @Named("defaultDispatcher") defaultDispatcher: CoroutineDispatcher
    ): HealthDataVisualizer {
        return HealthDataVisualizer(chartDataFormatter, defaultDispatcher)
    }
    
    /**
     * Device Compatibility Checkers
     */
    
    @Provides
    @Singleton
    fun provideDeviceCapabilityChecker(
        @ApplicationContext context: Context,
        packageManager: PackageManager
    ): DeviceCapabilityChecker {
        return DeviceCapabilityChecker(context, packageManager)
    }
    
    @Provides
    @Singleton
    fun providePackageManager(@ApplicationContext context: Context): PackageManager {
        return context.packageManager
    }
    
    @Provides
    @Singleton
    fun provideBleDeviceCompatibilityChecker(
        deviceCapabilityChecker: DeviceCapabilityChecker,
        @ApplicationContext context: Context
    ): BleDeviceCompatibilityChecker {
        return BleDeviceCompatibilityChecker(deviceCapabilityChecker, context)
    }
    
    /**
     * Locale and Language
     */
    
    @Provides
    @Singleton
    fun provideLocale(): Locale {
        return Locale.getDefault()
    }
    
    @Provides
    @Singleton
    fun provideLocaleManager(
        @ApplicationContext context: Context,
        preferencesManager: PreferencesManager
    ): LocaleManager {
        return LocaleManager(context, preferencesManager)
    }
    
    /**
     * Network Connectivity
     */
    
    @Provides
    @Singleton
    fun provideNetworkConnectivityMonitor(
        @ApplicationContext context: Context,
        @Named("applicationScope") applicationScope: CoroutineScope
    ): NetworkConnectivityMonitor {
        return NetworkConnectivityMonitor(context, applicationScope)
    }
    
    /**
     * App Version and Build Info
     */
    
    @Provides
    @Singleton
    fun provideAppVersionInfo(@ApplicationContext context: Context): AppVersionInfo {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName, 
                PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
        
        return AppVersionInfo(
            versionName = packageInfo.versionName,
            versionCode = versionCode,
            buildType = BuildConfig.BUILD_TYPE,
            isDebugBuild = BuildConfig.DEBUG,
            applicationId = BuildConfig.APPLICATION_ID
        )
    }
    
    /**
     * Resource Providers
     */
    
    @Provides
    @Singleton
    fun provideResourceProvider(@ApplicationContext context: Context): ResourceProvider {
        return ResourceProvider(context)
    }
    
    /**
     * Device Information
     */
    
    @Provides
    @Singleton
    fun provideDeviceInfo(): DeviceInfo {
        return DeviceInfo(
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            osVersion = Build.VERSION.RELEASE,
            sdkVersion = Build.VERSION.SDK_INT,
            deviceId = Build.ID
        )
    }
}
