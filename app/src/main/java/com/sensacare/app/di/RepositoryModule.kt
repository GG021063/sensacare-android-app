package com.sensacare.app.di

import android.content.Context
import com.sensacare.app.data.repository.*
import com.sensacare.app.data.local.dao.*
import com.sensacare.app.data.local.SensaCareDatabase
import com.sensacare.app.data.network.api.*
import com.sensacare.app.data.network.connectivity.NetworkConnectivityMonitor
import com.sensacare.app.data.network.error.NetworkErrorHandler
import com.sensacare.app.data.sync.DataSyncManager
import com.sensacare.app.data.sync.ConflictResolutionStrategy
import com.sensacare.app.data.sync.SyncScheduler
import com.sensacare.app.domain.repository.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import timber.log.Timber
import javax.inject.Named
import javax.inject.Singleton

/**
 * RepositoryModule - Comprehensive repository dependency injection setup for SensaCare app
 *
 * This module provides all repository implementations that bridge the data layer with the domain layer:
 * - Health data repositories for various health metrics (heart rate, blood pressure, sleep, etc.)
 * - Device management repository for BLE connectivity and device operations
 * - Goals repository for health goals tracking and management
 * - Alerts repository for health alerts and notifications
 * - User preferences repository for app settings and user preferences
 * - Emergency contacts repository for managing emergency contacts
 * - Health insights repository for AI-driven health insights
 * - Data synchronization repository for managing sync between local and remote data sources
 *
 * Features:
 * - Offline-first architecture with local caching
 * - Conflict resolution strategies for data synchronization
 * - Background data sync scheduling
 * - Repository error handling and retry logic
 * - Data transformation between entities and domain models
 * - Caching and refresh strategies
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    /**
     * Provides IO dispatcher for repository operations
     */
    @Provides
    @Singleton
    @Named("ioDispatcher")
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    /**
     * Provides conflict resolution strategy for data synchronization
     */
    @Provides
    @Singleton
    fun provideConflictResolutionStrategy(): ConflictResolutionStrategy {
        return ConflictResolutionStrategy.REMOTE_WINS // Default strategy, can be changed based on user preferences
    }

    /**
     * Provides data sync manager for coordinating synchronization between local and remote data sources
     */
    @Provides
    @Singleton
    fun provideDataSyncManager(
        @ApplicationContext context: Context,
        networkConnectivityMonitor: NetworkConnectivityMonitor,
        conflictResolutionStrategy: ConflictResolutionStrategy,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): DataSyncManager {
        return DataSyncManager(
            context = context,
            networkConnectivityMonitor = networkConnectivityMonitor,
            conflictResolutionStrategy = conflictResolutionStrategy,
            coroutineDispatcher = ioDispatcher
        )
    }

    /**
     * Provides sync scheduler for background data synchronization
     */
    @Provides
    @Singleton
    fun provideSyncScheduler(
        @ApplicationContext context: Context,
        dataSyncManager: DataSyncManager
    ): SyncScheduler {
        return SyncScheduler(context, dataSyncManager)
    }

    /**
     * Health Data Repositories
     */

    /**
     * Provides health data repository for accessing and managing all health metrics
     */
    @Provides
    @Singleton
    fun provideHealthDataRepository(
        healthDataDao: HealthDataDao,
        healthDataService: HealthDataService,
        dataSyncManager: DataSyncManager,
        networkErrorHandler: NetworkErrorHandler,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): HealthDataRepository {
        return HealthDataRepositoryImpl(
            healthDataDao = healthDataDao,
            healthDataService = healthDataService,
            dataSyncManager = dataSyncManager,
            networkErrorHandler = networkErrorHandler,
            ioDispatcher = ioDispatcher
        )
    }

    /**
     * Provides heart rate repository for heart rate specific operations
     */
    @Provides
    @Singleton
    fun provideHeartRateRepository(
        heartRateDao: HeartRateDao,
        healthDataService: HealthDataService,
        dataSyncManager: DataSyncManager,
        networkErrorHandler: NetworkErrorHandler,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): HeartRateRepository {
        return HeartRateRepositoryImpl(
            heartRateDao = heartRateDao,
            healthDataService = healthDataService,
            dataSyncManager = dataSyncManager,
            networkErrorHandler = networkErrorHandler,
            ioDispatcher = ioDispatcher
        )
    }

    /**
     * Provides blood pressure repository for blood pressure specific operations
     */
    @Provides
    @Singleton
    fun provideBloodPressureRepository(
        bloodPressureDao: BloodPressureDao,
        healthDataService: HealthDataService,
        dataSyncManager: DataSyncManager,
        networkErrorHandler: NetworkErrorHandler,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): BloodPressureRepository {
        return BloodPressureRepositoryImpl(
            bloodPressureDao = bloodPressureDao,
            healthDataService = healthDataService,
            dataSyncManager = dataSyncManager,
            networkErrorHandler = networkErrorHandler,
            ioDispatcher = ioDispatcher
        )
    }

    /**
     * Provides sleep repository for sleep data operations
     */
    @Provides
    @Singleton
    fun provideSleepRepository(
        sleepDao: SleepDao,
        healthDataService: HealthDataService,
        dataSyncManager: DataSyncManager,
        networkErrorHandler: NetworkErrorHandler,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): SleepRepository {
        return SleepRepositoryImpl(
            sleepDao = sleepDao,
            healthDataService = healthDataService,
            dataSyncManager = dataSyncManager,
            networkErrorHandler = networkErrorHandler,
            ioDispatcher = ioDispatcher
        )
    }

    /**
     * Provides activity repository for physical activity data operations
     */
    @Provides
    @Singleton
    fun provideActivityRepository(
        activityDao: ActivityDao,
        healthDataService: HealthDataService,
        dataSyncManager: DataSyncManager,
        networkErrorHandler: NetworkErrorHandler,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): ActivityRepository {
        return ActivityRepositoryImpl(
            activityDao = activityDao,
            healthDataService = healthDataService,
            dataSyncManager = dataSyncManager,
            networkErrorHandler = networkErrorHandler,
            ioDispatcher = ioDispatcher
        )
    }

    /**
     * Provides stress repository for stress level data operations
     */
    @Provides
    @Singleton
    fun provideStressRepository(
        stressDao: StressDao,
        healthDataService: HealthDataService,
        dataSyncManager: DataSyncManager,
        networkErrorHandler: NetworkErrorHandler,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): StressRepository {
        return StressRepositoryImpl(
            stressDao = stressDao,
            healthDataService = healthDataService,
            dataSyncManager = dataSyncManager,
            networkErrorHandler = networkErrorHandler,
            ioDispatcher = ioDispatcher
        )
    }

    /**
     * Provides weight repository for weight data operations
     */
    @Provides
    @Singleton
    fun provideWeightRepository(
        weightDao: WeightDao,
        healthDataService: HealthDataService,
        dataSyncManager: DataSyncManager,
        networkErrorHandler: NetworkErrorHandler,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): WeightRepository {
        return WeightRepositoryImpl(
            weightDao = weightDao,
            healthDataService = healthDataService,
            dataSyncManager = dataSyncManager,
            networkErrorHandler = networkErrorHandler,
            ioDispatcher = ioDispatcher
        )
    }

    /**
     * Provides water intake repository for hydration data operations
     */
    @Provides
    @Singleton
    fun provideWaterIntakeRepository(
        waterIntakeDao: WaterIntakeDao,
        healthDataService: HealthDataService,
        dataSyncManager: DataSyncManager,
        networkErrorHandler: NetworkErrorHandler,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): WaterIntakeRepository {
        return WaterIntakeRepositoryImpl(
            waterIntakeDao = waterIntakeDao,
            healthDataService = healthDataService,
            dataSyncManager = dataSyncManager,
            networkErrorHandler = networkErrorHandler,
            ioDispatcher = ioDispatcher
        )
    }

    /**
     * Device Management Repository
     */

    /**
     * Provides device repository for device management operations
     */
    @Provides
    @Singleton
    fun provideDeviceRepository(
        @ApplicationContext context: Context,
        deviceDao: DeviceDao,
        deviceSyncDao: DeviceSyncDao,
        deviceService: DeviceService,
        dataSyncManager: DataSyncManager,
        networkErrorHandler: NetworkErrorHandler,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): DeviceRepository {
        return DeviceRepositoryImpl(
            context = context,
            deviceDao = deviceDao,
            deviceSyncDao = deviceSyncDao,
            deviceService = deviceService,
            dataSyncManager = dataSyncManager,
            networkErrorHandler = networkErrorHandler,
            ioDispatcher = ioDispatcher
        )
    }

    /**
     * Provides firmware update repository for device firmware updates
     */
    @Provides
    @Singleton
    fun provideFirmwareUpdateRepository(
        deviceDao: DeviceDao,
        firmwareUpdateService: FirmwareUpdateService,
        networkErrorHandler: NetworkErrorHandler,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): FirmwareUpdateRepository {
        return FirmwareUpdateRepositoryImpl(
            deviceDao = deviceDao,
            firmwareUpdateService = firmwareUpdateService,
            networkErrorHandler = networkErrorHandler,
            ioDispatcher = ioDispatcher
        )
    }

    /**
     * Goals Repository
     */

    /**
     * Provides health goal repository for health goals operations
     */
    @Provides
    @Singleton
    fun provideHealthGoalRepository(
        healthGoalDao: HealthGoalDao,
        goalProgressDao: GoalProgressDao,
        goalService: GoalService,
        dataSyncManager: DataSyncManager,
        networkErrorHandler: NetworkErrorHandler,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): HealthGoalRepository {
        return HealthGoalRepositoryImpl(
            healthGoalDao = healthGoalDao,
            goalProgressDao = goalProgressDao,
            goalService = goalService,
            dataSyncManager = dataSyncManager,
            networkErrorHandler = networkErrorHandler,
            ioDispatcher = ioDispatcher
        )
    }

    /**
     * Provides goal progress repository for tracking progress towards health goals
     */
    @Provides
    @Singleton
    fun provideGoalProgressRepository(
        goalProgressDao: GoalProgressDao,
        goalService: GoalService,
        dataSyncManager: DataSyncManager,
        networkErrorHandler: NetworkErrorHandler,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): GoalProgressRepository {
        return GoalProgressRepositoryImpl(
            goalProgressDao = goalProgressDao,
            goalService = goalService,
            dataSyncManager = dataSyncManager,
            networkErrorHandler = networkErrorHandler,
            ioDispatcher = ioDispatcher
        )
    }

    /**
     * Alerts Repository
     */

    /**
     * Provides health alert repository for health alerts operations
     */
    @Provides
    @Singleton
    fun provideHealthAlertRepository(
        healthAlertDao: HealthAlertDao,
        alertRuleDao: AlertRuleDao,
        alertService: AlertService,
        dataSyncManager: DataSyncManager,
        networkErrorHandler: NetworkErrorHandler,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): HealthAlertRepository {
        return HealthAlertRepositoryImpl(
            healthAlertDao = healthAlertDao,
            alertRuleDao = alertRuleDao,
            alertService = alertService,
            dataSyncManager = dataSyncManager,
            networkErrorHandler = networkErrorHandler,
            ioDispatcher = ioDispatcher
        )
    }

    /**
     * Provides alert rule repository for managing alert rules and thresholds
     */
    @Provides
    @Singleton
    fun provideAlertRuleRepository(
        alertRuleDao: AlertRuleDao,
        alertService: AlertService,
        dataSyncManager: DataSyncManager,
        networkErrorHandler: NetworkErrorHandler,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): AlertRuleRepository {
        return AlertRuleRepositoryImpl(
            alertRuleDao = alertRuleDao,
            alertService = alertService,
            dataSyncManager = dataSyncManager,
            networkErrorHandler = networkErrorHandler,
            ioDispatcher = ioDispatcher
        )
    }

    /**
     * Provides alert preferences repository for managing alert settings
     */
    @Provides
    @Singleton
    fun provideAlertPreferencesRepository(
        alertPreferencesDao: AlertPreferencesDao,
        alertService: AlertService,
        dataSyncManager: DataSyncManager,
        networkErrorHandler: NetworkErrorHandler,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): AlertPreferencesRepository {
        return AlertPreferencesRepositoryImpl(
            alertPreferencesDao = alertPreferencesDao,
            alertService = alertService,
            dataSyncManager = dataSyncManager,
            networkErrorHandler = networkErrorHandler,
            ioDispatcher = ioDispatcher
        )
    }

    /**
     * User Preferences Repository
     */

    /**
     * Provides user repository for user account operations
     */
    @Provides
    @Singleton
    fun provideUserRepository(
        @ApplicationContext context: Context,
        userService: UserService,
        networkErrorHandler: NetworkErrorHandler,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): UserRepository {
        return UserRepositoryImpl(
            context = context,
            userService = userService,
            networkErrorHandler = networkErrorHandler,
            ioDispatcher = ioDispatcher
        )
    }

    /**
     * Provides user preferences repository for app settings and preferences
     */
    @Provides
    @Singleton
    fun provideUserPreferencesRepository(
        userPreferencesDao: UserPreferencesDao,
        userService: UserService,
        dataSyncManager: DataSyncManager,
        networkErrorHandler: NetworkErrorHandler,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): UserPreferencesRepository {
        return UserPreferencesRepositoryImpl(
            userPreferencesDao = userPreferencesDao,
            userService = userService,
            dataSyncManager = dataSyncManager,
            networkErrorHandler = networkErrorHandler,
            ioDispatcher = ioDispatcher
        )
    }

    /**
     * Emergency Contacts Repository
     */

    /**
     * Provides emergency contact repository for managing emergency contacts
     */
    @Provides
    @Singleton
    fun provideEmergencyContactRepository(
        emergencyContactDao: EmergencyContactDao,
        emergencyContactService: EmergencyContactService,
        dataSyncManager: DataSyncManager,
        networkErrorHandler: NetworkErrorHandler,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): EmergencyContactRepository {
        return EmergencyContactRepositoryImpl(
            emergencyContactDao = emergencyContactDao,
            emergencyContactService = emergencyContactService,
            dataSyncManager = dataSyncManager,
            networkErrorHandler = networkErrorHandler,
            ioDispatcher = ioDispatcher
        )
    }

    /**
     * Health Insights Repository
     */

    /**
     * Provides health insights repository for AI-driven health insights
     */
    @Provides
    @Singleton
    fun provideHealthInsightRepository(
        healthInsightDao: HealthInsightDao,
        healthInsightsService: HealthInsightsService,
        dataSyncManager: DataSyncManager,
        networkErrorHandler: NetworkErrorHandler,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): HealthInsightRepository {
        return HealthInsightRepositoryImpl(
            healthInsightDao = healthInsightDao,
            healthInsightsService = healthInsightsService,
            dataSyncManager = dataSyncManager,
            networkErrorHandler = networkErrorHandler,
            ioDispatcher = ioDispatcher
        )
    }
}
