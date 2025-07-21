package com.sensacare.app.di

import com.sensacare.app.domain.repository.*
import com.sensacare.app.domain.usecase.*
import com.sensacare.app.domain.usecase.alert.*
import com.sensacare.app.domain.usecase.device.*
import com.sensacare.app.domain.usecase.emergency.*
import com.sensacare.app.domain.usecase.goal.*
import com.sensacare.app.domain.usecase.health.*
import com.sensacare.app.domain.usecase.insight.*
import com.sensacare.app.domain.usecase.notification.*
import com.sensacare.app.domain.usecase.sync.*
import com.sensacare.app.domain.usecase.user.*
import com.sensacare.app.domain.usecase.validation.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Named
import javax.inject.Singleton

/**
 * UseCaseModule - Comprehensive use case dependency injection setup for SensaCare app
 *
 * This module provides all domain use cases that implement the business logic of the app:
 * - Health data use cases for managing and analyzing health metrics
 * - Device management use cases for BLE device operations
 * - Goals use cases for setting and tracking health goals
 * - Alert use cases for managing health alerts and notifications
 * - User management use cases for authentication and profile management
 * - Emergency contact use cases for managing emergency contacts
 * - Health insights use cases for AI-driven health analysis
 * - Background sync use cases for data synchronization
 * - Data validation use cases for ensuring data integrity
 * - Notification use cases for managing user notifications
 *
 * Each use case follows the Single Responsibility Principle and encapsulates
 * a specific business operation or rule of the application.
 */
@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    /**
     * Health Data Use Cases
     */

    @Provides
    @Singleton
    fun provideGetHealthMetricsUseCase(
        healthDataRepository: HealthDataRepository,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): GetHealthMetricsUseCase {
        return GetHealthMetricsUseCase(healthDataRepository, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideSaveHealthDataUseCase(
        healthDataRepository: HealthDataRepository,
        dataValidationUseCase: ValidateHealthDataUseCase,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): SaveHealthDataUseCase {
        return SaveHealthDataUseCase(healthDataRepository, dataValidationUseCase, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideAnalyzeHealthTrendsUseCase(
        healthDataRepository: HealthDataRepository,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): AnalyzeHealthTrendsUseCase {
        return AnalyzeHealthTrendsUseCase(healthDataRepository, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideGetHeartRateDataUseCase(
        heartRateRepository: HeartRateRepository,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): GetHeartRateDataUseCase {
        return GetHeartRateDataUseCase(heartRateRepository, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideSaveHeartRateDataUseCase(
        heartRateRepository: HeartRateRepository,
        dataValidationUseCase: ValidateHealthDataUseCase,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): SaveHeartRateDataUseCase {
        return SaveHeartRateDataUseCase(heartRateRepository, dataValidationUseCase, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideGetBloodPressureDataUseCase(
        bloodPressureRepository: BloodPressureRepository,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): GetBloodPressureDataUseCase {
        return GetBloodPressureDataUseCase(bloodPressureRepository, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideSaveBloodPressureDataUseCase(
        bloodPressureRepository: BloodPressureRepository,
        dataValidationUseCase: ValidateHealthDataUseCase,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): SaveBloodPressureDataUseCase {
        return SaveBloodPressureDataUseCase(bloodPressureRepository, dataValidationUseCase, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideGetSleepDataUseCase(
        sleepRepository: SleepRepository,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): GetSleepDataUseCase {
        return GetSleepDataUseCase(sleepRepository, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideSaveSleepDataUseCase(
        sleepRepository: SleepRepository,
        dataValidationUseCase: ValidateHealthDataUseCase,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): SaveSleepDataUseCase {
        return SaveSleepDataUseCase(sleepRepository, dataValidationUseCase, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideGetActivityDataUseCase(
        activityRepository: ActivityRepository,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): GetActivityDataUseCase {
        return GetActivityDataUseCase(activityRepository, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideSaveActivityDataUseCase(
        activityRepository: ActivityRepository,
        dataValidationUseCase: ValidateHealthDataUseCase,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): SaveActivityDataUseCase {
        return SaveActivityDataUseCase(activityRepository, dataValidationUseCase, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideGetStressDataUseCase(
        stressRepository: StressRepository,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): GetStressDataUseCase {
        return GetStressDataUseCase(stressRepository, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideSaveStressDataUseCase(
        stressRepository: StressRepository,
        dataValidationUseCase: ValidateHealthDataUseCase,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): SaveStressDataUseCase {
        return SaveStressDataUseCase(stressRepository, dataValidationUseCase, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideGetWeightDataUseCase(
        weightRepository: WeightRepository,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): GetWeightDataUseCase {
        return GetWeightDataUseCase(weightRepository, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideSaveWeightDataUseCase(
        weightRepository: WeightRepository,
        dataValidationUseCase: ValidateHealthDataUseCase,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): SaveWeightDataUseCase {
        return SaveWeightDataUseCase(weightRepository, dataValidationUseCase, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideGetWaterIntakeDataUseCase(
        waterIntakeRepository: WaterIntakeRepository,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): GetWaterIntakeDataUseCase {
        return GetWaterIntakeDataUseCase(waterIntakeRepository, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideSaveWaterIntakeDataUseCase(
        waterIntakeRepository: WaterIntakeRepository,
        dataValidationUseCase: ValidateHealthDataUseCase,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): SaveWaterIntakeDataUseCase {
        return SaveWaterIntakeDataUseCase(waterIntakeRepository, dataValidationUseCase, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideCalculateHealthScoreUseCase(
        healthDataRepository: HealthDataRepository,
        heartRateRepository: HeartRateRepository,
        sleepRepository: SleepRepository,
        activityRepository: ActivityRepository,
        stressRepository: StressRepository,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): CalculateHealthScoreUseCase {
        return CalculateHealthScoreUseCase(
            healthDataRepository,
            heartRateRepository,
            sleepRepository,
            activityRepository,
            stressRepository,
            ioDispatcher
        )
    }

    /**
     * Device Management Use Cases
     */

    @Provides
    @Singleton
    fun provideScanForDevicesUseCase(
        deviceRepository: DeviceRepository,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): ScanForDevicesUseCase {
        return ScanForDevicesUseCase(deviceRepository, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideConnectDeviceUseCase(
        deviceRepository: DeviceRepository,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): ConnectDeviceUseCase {
        return ConnectDeviceUseCase(deviceRepository, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideDisconnectDeviceUseCase(
        deviceRepository: DeviceRepository,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): DisconnectDeviceUseCase {
        return DisconnectDeviceUseCase(deviceRepository, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideForgetDeviceUseCase(
        deviceRepository: DeviceRepository,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): ForgetDeviceUseCase {
        return ForgetDeviceUseCase(deviceRepository, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideSyncDeviceDataUseCase(
        deviceRepository: DeviceRepository,
        healthDataRepository: HealthDataRepository,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): SyncDeviceDataUseCase {
        return SyncDeviceDataUseCase(deviceRepository, healthDataRepository, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideGetConnectedDevicesUseCase(
        deviceRepository: DeviceRepository,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): GetConnectedDevicesUseCase {
        return GetConnectedDevicesUseCase(deviceRepository, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideGetDeviceBatteryLevelUseCase(
        deviceRepository: DeviceRepository,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): GetDeviceBatteryLevelUseCase {
        return GetDeviceBatteryLevelUseCase(deviceRepository, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideCheckFirmwareUpdateUseCase(
        firmwareUpdateRepository: FirmwareUpdateRepository,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): CheckFirmwareUpdateUseCase {
        return CheckFirmwareUpdateUseCase(firmwareUpdateRepository, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideUpdateDeviceFirmwareUseCase(
        firmwareUpdateRepository: FirmwareUpdateRepository,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): UpdateDeviceFirmwareUseCase {
        return UpdateDeviceFirmwareUseCase(firmwareUpdateRepository, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideConfigureDeviceSettingsUseCase(
        deviceRepository: DeviceRepository,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): ConfigureDeviceSettingsUseCase {
        return ConfigureDeviceSettingsUseCase(deviceRepository, ioDispatcher)
    }

    /**
     * Goals Use Cases
     */

    @Provides
    @Singleton
    fun provideCreateHealthGoalUseCase(
        healthGoalRepository: HealthGoalRepository,
        validateGoalUseCase: ValidateGoalUseCase,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): CreateHealthGoalUseCase {
        return CreateHealthGoalUseCase(healthGoalRepository, validateGoalUseCase, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideUpdateHealthGoalUseCase(
        healthGoalRepository: HealthGoalRepository,
        validateGoalUseCase: ValidateGoalUseCase,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): UpdateHealthGoalUseCase {
        return UpdateHealthGoalUseCase(healthGoalRepository, validateGoalUseCase, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideDeleteHealthGoalUseCase(
        healthGoalRepository: HealthGoalRepository,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): DeleteHealthGoalUseCase {
        return DeleteHealthGoalUseCase(healthGoalRepository, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideGetHealthGoalsUseCase(
        healthGoalRepository: HealthGoalRepository,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): GetHealthGoalsUseCase {
        return GetHealthGoalsUseCase(healthGoalRepository, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideTrackGoalProgressUseCase(
        goalProgressRepository: GoalProgressRepository,
        healthDataRepository: HealthDataRepository,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): TrackGoalProgressUseCase {
        return TrackGoalProgressUseCase(goalProgressRepository, healthDataRepository, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideGetGoalProgressUseCase(
        goalProgressRepository: GoalProgressRepository,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): GetGoalProgressUseCase {
        return GetGoalProgressUseCase(goalProgressRepository, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideUpdateGoalProgressUseCase(
        goalProgressRepository: GoalProgressRepository,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): UpdateGoalProgressUseCase {
        return UpdateGoalProgressUseCase(goalProgressRepository, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideGenerateGoalRecommendationsUseCase(
        healthDataRepository: HealthDataRepository,
        healthGoalRepository: HealthGoalRepository,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): GenerateGoalRecommendationsUseCase {
        return GenerateGoalRecommendationsUseCase(healthDataRepository, healthGoalRepository, ioDispatcher)
    }

    /**
     * Alert Use Cases
     */

    @Provides
    @Singleton
    fun provideCreateAlertRuleUseCase(
        alertRuleRepository: AlertRuleRepository,
        validateAlertRuleUseCase: ValidateAlertRuleUseCase,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): CreateAlertRuleUseCase {
        return CreateAlertRuleUseCase(alertRuleRepository, validateAlertRuleUseCase, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideUpdateAlertRuleUseCase(
        alertRuleRepository: AlertRuleRepository,
        validateAlertRuleUseCase: ValidateAlertRuleUseCase,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): UpdateAlertRuleUseCase {
        return UpdateAlertRuleUseCase(alertRuleRepository, validateAlertRuleUseCase, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideDeleteAlertRuleUseCase(
        alertRuleRepository: AlertRuleRepository,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): DeleteAlertRuleUseCase {
        return DeleteAlertRuleUseCase(alertRuleRepository, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideGetAlertRulesUseCase(
        alertRuleRepository: AlertRuleRepository,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): GetAlertRulesUseCase {
        return GetAlertRulesUseCase(alertRuleRepository, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideCheckHealthAlertsUseCase(
        healthAlertRepository: HealthAlertRepository,
        healthDataRepository: HealthDataRepository,
        alertRuleRepository: AlertRuleRepository,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): CheckHealthAlertsUseCase {
        return CheckHealthAlertsUseCase(
            healthAlertRepository,
            healthDataRepository,
            alertRuleRepository,
            ioDispatcher
        )
    }

    @Provides
    @Singleton
    fun provideGetHealthAlertsUseCase(
        healthAlertRepository: HealthAlertRepository,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): GetHealthAlertsUseCase {
        return GetHealthAlertsUseCase(healthAlertRepository, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideMarkAlertAsReadUseCase(
        healthAlertRepository: HealthAlertRepository,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): MarkAlertAsReadUseCase {
        return MarkAlertAsReadUseCase(healthAlertRepository, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideUpdateAlertPreferencesUseCase(
        alertPreferencesRepository: AlertPreferencesRepository,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): UpdateAlertPreferencesUseCase {
        return UpdateAlertPreferencesUseCase(alertPreferencesRepository, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideGetAlertPreferencesUseCase(
        alertPreferencesRepository: AlertPreferencesRepository,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): GetAlertPreferencesUseCase {
        return GetAlertPreferencesUseCase(alertPreferencesRepository, ioDispatcher)
    }

    /**
     * User Management Use Cases
     */

    @Provides
    @Singleton
    fun provideLoginUserUseCase(
        userRepository: UserRepository,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): LoginUserUseCase {
        return LoginUserUseCase(userRepository, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideRegisterUserUseCase(
        userRepository: UserRepository,
        validateUserDataUseCase: ValidateUserDataUseCase,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): RegisterUserUseCase {
        return RegisterUserUseCase(userRepository, validateUserDataUseCase, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideLogoutUserUseCase(
        userRepository: UserRepository,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): LogoutUserUseCase {
        return LogoutUserUseCase(userRepository, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideGetUserProfileUseCase(
        userRepository: UserRepository,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): GetUserProfileUseCase {
        return GetUserProfileUseCase(userRepository, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideUpdateUserProfileUseCase(
        userRepository: UserRepository,
        validateUserDataUseCase: ValidateUserDataUseCase,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): UpdateUserProfileUseCase {
        return UpdateUserProfileUseCase(userRepository, validateUserDataUseCase, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideGetUserPreferencesUseCase(
        userPreferencesRepository: UserPreferencesRepository,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): GetUserPreferencesUseCase {
        return GetUserPreferencesUseCase(userPreferencesRepository, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideUpdateUserPreferencesUseCase(
        userPreferencesRepository: UserPreferencesRepository,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): UpdateUserPreferencesUseCase {
        return UpdateUserPreferencesUseCase(userPreferencesRepository, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideResetPasswordUseCase(
        userRepository: UserRepository,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): ResetPasswordUseCase {
        return ResetPasswordUseCase(userRepository, ioDispatcher)
    }

    /**
     * Emergency Contact Use Cases
     */

    @Provides
    @Singleton
    fun provideAddEmergencyContactUseCase(
        emergencyContactRepository: EmergencyContactRepository,
        validateContactUseCase: ValidateContactUseCase,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): AddEmergencyContactUseCase {
        return AddEmergencyContactUseCase(emergencyContactRepository, validateContactUseCase, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideUpdateEmergencyContactUseCase(
        emergencyContactRepository: EmergencyContactRepository,
        validateContactUseCase: ValidateContactUseCase,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): UpdateEmergencyContactUseCase {
        return UpdateEmergencyContactUseCase(emergencyContactRepository, validateContactUseCase, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideDeleteEmergencyContactUseCase(
        emergencyContactRepository: EmergencyContactRepository,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): DeleteEmergencyContactUseCase {
        return DeleteEmergencyContactUseCase(emergencyContactRepository, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideGetEmergencyContactsUseCase(
        emergencyContactRepository: EmergencyContactRepository,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): GetEmergencyContactsUseCase {
        return GetEmergencyContactsUseCase(emergencyContactRepository, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideNotifyEmergencyContactsUseCase(
        emergencyContactRepository: EmergencyContactRepository,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): NotifyEmergencyContactsUseCase {
        return NotifyEmergencyContactsUseCase(emergencyContactRepository, ioDispatcher)
    }

    /**
     * Health Insights Use Cases
     */

    @Provides
    @Singleton
    fun provideGenerateHealthInsightsUseCase(
        healthInsightRepository: HealthInsightRepository,
        healthDataRepository: HealthDataRepository,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): GenerateHealthInsightsUseCase {
        return GenerateHealthInsightsUseCase(healthInsightRepository, healthDataRepository, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideGetHealthInsightsUseCase(
        healthInsightRepository: HealthInsightRepository,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): GetHealthInsightsUseCase {
        return GetHealthInsightsUseCase(healthInsightRepository, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideAnalyzeHealthPatternsUseCase(
        healthInsightRepository: HealthInsightRepository,
        healthDataRepository: HealthDataRepository,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): AnalyzeHealthPatternsUseCase {
        return AnalyzeHealthPatternsUseCase(healthInsightRepository, healthDataRepository, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideGenerateHealthReportUseCase(
        healthInsightRepository: HealthInsightRepository,
        healthDataRepository: HealthDataRepository,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): GenerateHealthReportUseCase {
        return GenerateHealthReportUseCase(healthInsightRepository, healthDataRepository, ioDispatcher)
    }

    /**
     * Background Sync Use Cases
     */

    @Provides
    @Singleton
    fun provideScheduleSyncUseCase(
        dataSyncManager: DataSyncManager,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): ScheduleSyncUseCase {
        return ScheduleSyncUseCase(dataSyncManager, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideSyncAllDataUseCase(
        dataSyncManager: DataSyncManager,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): SyncAllDataUseCase {
        return SyncAllDataUseCase(dataSyncManager, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideHandleSyncConflictsUseCase(
        dataSyncManager: DataSyncManager,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): HandleSyncConflictsUseCase {
        return HandleSyncConflictsUseCase(dataSyncManager, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideGetSyncStatusUseCase(
        dataSyncManager: DataSyncManager,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): GetSyncStatusUseCase {
        return GetSyncStatusUseCase(dataSyncManager, ioDispatcher)
    }

    /**
     * Data Validation Use Cases
     */

    @Provides
    @Singleton
    fun provideValidateHealthDataUseCase(
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): ValidateHealthDataUseCase {
        return ValidateHealthDataUseCase(ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideValidateGoalUseCase(
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): ValidateGoalUseCase {
        return ValidateGoalUseCase(ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideValidateAlertRuleUseCase(
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): ValidateAlertRuleUseCase {
        return ValidateAlertRuleUseCase(ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideValidateUserDataUseCase(
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): ValidateUserDataUseCase {
        return ValidateUserDataUseCase(ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideValidateContactUseCase(
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): ValidateContactUseCase {
        return ValidateContactUseCase(ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideSanitizeDataUseCase(
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): SanitizeDataUseCase {
        return SanitizeDataUseCase(ioDispatcher)
    }

    /**
     * Notification Use Cases
     */

    @Provides
    @Singleton
    fun provideScheduleNotificationUseCase(
        notificationManager: NotificationManager,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): ScheduleNotificationUseCase {
        return ScheduleNotificationUseCase(notificationManager, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideCancelNotificationUseCase(
        notificationManager: NotificationManager,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): CancelNotificationUseCase {
        return CancelNotificationUseCase(notificationManager, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideGetNotificationHistoryUseCase(
        notificationManager: NotificationManager,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): GetNotificationHistoryUseCase {
        return GetNotificationHistoryUseCase(notificationManager, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideUpdateNotificationSettingsUseCase(
        notificationManager: NotificationManager,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): UpdateNotificationSettingsUseCase {
        return UpdateNotificationSettingsUseCase(notificationManager, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideCreateReminderNotificationUseCase(
        notificationManager: NotificationManager,
        @Named("ioDispatcher") ioDispatcher: CoroutineDispatcher
    ): CreateReminderNotificationUseCase {
        return CreateReminderNotificationUseCase(notificationManager, ioDispatcher)
    }
}
