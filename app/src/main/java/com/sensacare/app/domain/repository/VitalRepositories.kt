package com.sensacare.app.domain.repository

import com.sensacare.app.domain.model.*
import com.sensacare.app.domain.util.Result
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * Repository interface for Blood Oxygen data
 */
interface BloodOxygenRepository {
    // Create operations
    suspend fun saveBloodOxygen(bloodOxygen: BloodOxygen): Result<Unit>
    suspend fun saveMultipleBloodOxygen(bloodOxygens: List<BloodOxygen>): Result<Unit>
    
    // Read operations
    suspend fun getBloodOxygenById(userId: String, bloodOxygenId: String): Result<BloodOxygen?>
    suspend fun getLatestBloodOxygen(userId: String): Result<BloodOxygen?>
    suspend fun getBloodOxygenByDateRange(
        userId: String,
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime,
        limit: Int = 100,
        offset: Int = 0
    ): Result<List<BloodOxygen>>
    
    // Observable operations
    fun observeLatestBloodOxygen(userId: String): Flow<Result<BloodOxygen?>>
    fun observeBloodOxygenByDateRange(
        userId: String,
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime,
        limit: Int = 100,
        offset: Int = 0
    ): Flow<Result<List<BloodOxygen>>>
    
    // Delete operations
    suspend fun deleteBloodOxygen(bloodOxygenId: String, userId: String): Result<Unit>
    suspend fun deleteAllBloodOxygen(userId: String): Result<Unit>
    
    // Analytics operations
    suspend fun getAverageBloodOxygen(
        userId: String,
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime
    ): Result<Int?>
    
    suspend fun getMinMaxBloodOxygen(
        userId: String,
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime
    ): Result<Pair<Int?, Int?>>
}

/**
 * Repository interface for Body Temperature data
 */
interface BodyTemperatureRepository {
    // Create operations
    suspend fun saveBodyTemperature(bodyTemperature: BodyTemperature): Result<Unit>
    suspend fun saveMultipleBodyTemperature(bodyTemperatures: List<BodyTemperature>): Result<Unit>
    
    // Read operations
    suspend fun getBodyTemperatureById(userId: String, bodyTemperatureId: String): Result<BodyTemperature?>
    suspend fun getLatestBodyTemperature(userId: String): Result<BodyTemperature?>
    suspend fun getBodyTemperatureByDateRange(
        userId: String,
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime,
        limit: Int = 100,
        offset: Int = 0
    ): Result<List<BodyTemperature>>
    
    // Observable operations
    fun observeLatestBodyTemperature(userId: String): Flow<Result<BodyTemperature?>>
    fun observeBodyTemperatureByDateRange(
        userId: String,
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime,
        limit: Int = 100,
        offset: Int = 0
    ): Flow<Result<List<BodyTemperature>>>
    
    // Delete operations
    suspend fun deleteBodyTemperature(bodyTemperatureId: String, userId: String): Result<Unit>
    suspend fun deleteAllBodyTemperature(userId: String): Result<Unit>
    
    // Analytics operations
    suspend fun getAverageBodyTemperature(
        userId: String,
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime
    ): Result<Float?>
    
    suspend fun getBodyTemperatureByMeasurementSite(
        userId: String,
        measurementSite: TemperatureMeasurementSite,
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime
    ): Result<List<BodyTemperature>>
    
    suspend fun getTemperatureClassificationCounts(
        userId: String,
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime
    ): Result<Map<TemperatureClassification, Int>>
}

/**
 * Repository interface for Stress Level data
 */
interface StressLevelRepository {
    // Create operations
    suspend fun saveStressLevel(stressLevel: StressLevel): Result<Unit>
    suspend fun saveMultipleStressLevel(stressLevels: List<StressLevel>): Result<Unit>
    
    // Read operations
    suspend fun getStressLevelById(userId: String, stressLevelId: String): Result<StressLevel?>
    suspend fun getLatestStressLevel(userId: String): Result<StressLevel?>
    suspend fun getStressLevelByDateRange(
        userId: String,
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime,
        limit: Int = 100,
        offset: Int = 0
    ): Result<List<StressLevel>>
    
    // Observable operations
    fun observeLatestStressLevel(userId: String): Flow<Result<StressLevel?>>
    fun observeStressLevelByDateRange(
        userId: String,
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime,
        limit: Int = 100,
        offset: Int = 0
    ): Flow<Result<List<StressLevel>>>
    
    // Delete operations
    suspend fun deleteStressLevel(stressLevelId: String, userId: String): Result<Unit>
    suspend fun deleteAllStressLevel(userId: String): Result<Unit>
    
    // Analytics operations
    suspend fun getAverageStressLevel(
        userId: String,
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime
    ): Result<Int?>
    
    suspend fun getStressLevelByClassification(
        userId: String,
        classification: StressClassification,
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime
    ): Result<List<StressLevel>>
    
    suspend fun getHighStressEpisodes(
        userId: String,
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime,
        threshold: Int = StressLevel.HIGH_STRESS_THRESHOLD
    ): Result<List<Pair<LocalDateTime, LocalDateTime>>>
}

/**
 * Repository interface for ECG data
 */
interface EcgRepository {
    // Create operations
    suspend fun saveEcg(ecg: Ecg): Result<Unit>
    
    // Read operations
    suspend fun getEcgById(userId: String, ecgId: String): Result<Ecg?>
    suspend fun getLatestEcg(userId: String): Result<Ecg?>
    suspend fun getEcgByDateRange(
        userId: String,
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime,
        limit: Int = 20, // Limit lower due to larger data size
        offset: Int = 0
    ): Result<List<Ecg>>
    
    // Observable operations
    fun observeLatestEcg(userId: String): Flow<Result<Ecg?>>
    fun observeEcgByDateRange(
        userId: String,
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime,
        limit: Int = 20,
        offset: Int = 0
    ): Flow<Result<List<Ecg>>>
    
    // Delete operations
    suspend fun deleteEcg(ecgId: String, userId: String): Result<Unit>
    suspend fun deleteAllEcg(userId: String): Result<Unit>
    
    // Analytics operations
    suspend fun getEcgByClassification(
        userId: String,
        classification: EcgClassification,
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime
    ): Result<List<Ecg>>
    
    suspend fun getEcgClassificationCounts(
        userId: String,
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime
    ): Result<Map<EcgClassification, Int>>
    
    suspend fun getEcgWithAbnormalities(
        userId: String,
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime
    ): Result<List<Ecg>>
}

/**
 * Repository interface for Blood Glucose data
 */
interface BloodGlucoseRepository {
    // Create operations
    suspend fun saveBloodGlucose(bloodGlucose: BloodGlucose): Result<Unit>
    suspend fun saveMultipleBloodGlucose(bloodGlucoses: List<BloodGlucose>): Result<Unit>
    
    // Read operations
    suspend fun getBloodGlucoseById(userId: String, bloodGlucoseId: String): Result<BloodGlucose?>
    suspend fun getLatestBloodGlucose(userId: String): Result<BloodGlucose?>
    suspend fun getBloodGlucoseByDateRange(
        userId: String,
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime,
        limit: Int = 100,
        offset: Int = 0
    ): Result<List<BloodGlucose>>
    
    // Observable operations
    fun observeLatestBloodGlucose(userId: String): Flow<Result<BloodGlucose?>>
    fun observeBloodGlucoseByDateRange(
        userId: String,
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime,
        limit: Int = 100,
        offset: Int = 0
    ): Flow<Result<List<BloodGlucose>>>
    
    // Delete operations
    suspend fun deleteBloodGlucose(bloodGlucoseId: String, userId: String): Result<Unit>
    suspend fun deleteAllBloodGlucose(userId: String): Result<Unit>
    
    // Analytics operations
    suspend fun getAverageBloodGlucose(
        userId: String,
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime,
        measurementType: GlucoseMeasurementType? = null
    ): Result<Float?>
    
    suspend fun getBloodGlucoseByMeasurementType(
        userId: String,
        measurementType: GlucoseMeasurementType,
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime
    ): Result<List<BloodGlucose>>
    
    suspend fun getBloodGlucoseByMealContext(
        userId: String,
        mealType: MealType,
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime
    ): Result<List<BloodGlucose>>
    
    suspend fun getGlucoseClassificationCounts(
        userId: String,
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime
    ): Result<Map<GlucoseClassification, Int>>
}

/**
 * Repository interface for Device Capability data
 */
interface DeviceCapabilityRepository {
    // Create operations
    suspend fun saveDeviceCapability(deviceCapability: DeviceCapability): Result<Unit>
    
    // Read operations
    suspend fun getDeviceCapability(deviceId: String): Result<DeviceCapability>
    suspend fun getAllDeviceCapabilities(): Result<List<DeviceCapability>>
    suspend fun getDeviceCapabilitiesByMetric(supportedMetric: SupportedMetric): Result<List<DeviceCapability>>
    
    // Update operations
    suspend fun updateDeviceCapability(deviceCapability: DeviceCapability): Result<Unit>
    suspend fun updateDeviceBatteryLevel(deviceId: String, batteryLevel: Int): Result<Unit>
    suspend fun updateDeviceFirmware(deviceId: String, firmwareVersion: String): Result<Unit>
    
    // Delete operations
    suspend fun deleteDeviceCapability(deviceId: String): Result<Unit>
    
    // Query operations
    suspend fun deviceSupportsMetric(deviceId: String, metric: SupportedMetric): Result<Boolean>
    suspend fun getDevicesByMetrics(requiredMetrics: Set<SupportedMetric>): Result<List<DeviceCapability>>
    
    // Observable operations
    fun observeDeviceCapability(deviceId: String): Flow<Result<DeviceCapability?>>
    fun observeAllDeviceCapabilities(): Flow<Result<List<DeviceCapability>>>
}
