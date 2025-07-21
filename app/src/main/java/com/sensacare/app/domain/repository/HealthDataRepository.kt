package com.sensacare.app.domain.repository

import androidx.paging.PagingData
import com.sensacare.app.domain.model.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * HealthDataRepository - Interface for health data management operations
 *
 * This repository handles all health-related data operations including:
 * - Heart rate measurements
 * - Blood oxygen measurements
 * - Blood pressure measurements
 * - Step and activity data
 * - Sleep tracking
 * - Temperature measurements
 * - Activity/exercise tracking
 * - Statistical analysis
 * - Sync operations
 */
interface HealthDataRepository {

    /**
     * ====================================
     * Heart Rate Operations
     * ====================================
     */

    /**
     * Get a heart rate measurement by ID
     * @param id The ID of the heart rate measurement
     * @return Flow of the heart rate measurement, or null if not found
     */
    fun getHeartRate(id: Long): Flow<HeartRate?>

    /**
     * Get all heart rate measurements
     * @return Flow of all heart rate measurements
     */
    fun getAllHeartRates(): Flow<List<HeartRate>>

    /**
     * Get heart rate measurements for a device
     * @param deviceId The ID of the device
     * @return Flow of heart rate measurements for the device
     */
    fun getHeartRatesForDevice(deviceId: Long): Flow<List<HeartRate>>

    /**
     * Get heart rate measurements for a date range
     * @param startTime The start of the date range
     * @param endTime The end of the date range
     * @return Flow of heart rate measurements in the date range
     */
    fun getHeartRatesInRange(startTime: LocalDateTime, endTime: LocalDateTime): Flow<List<HeartRate>>

    /**
     * Get heart rate measurements for a device in a date range
     * @param deviceId The ID of the device
     * @param startTime The start of the date range
     * @param endTime The end of the date range
     * @return Flow of heart rate measurements for the device in the date range
     */
    fun getHeartRatesForDeviceInRange(deviceId: Long, startTime: LocalDateTime, endTime: LocalDateTime): Flow<List<HeartRate>>

    /**
     * Get heart rate measurements for a specific date
     * @param date The date to get measurements for
     * @return Flow of heart rate measurements for the date
     */
    fun getHeartRatesForDate(date: LocalDate): Flow<List<HeartRate>>

    /**
     * Get heart rate measurements for a device on a specific date
     * @param deviceId The ID of the device
     * @param date The date to get measurements for
     * @return Flow of heart rate measurements for the device on the date
     */
    fun getHeartRatesForDeviceAndDate(deviceId: Long, date: LocalDate): Flow<List<HeartRate>>

    /**
     * Get paged heart rate measurements
     * @return Flow of paged heart rate measurements
     */
    fun getPagedHeartRates(): Flow<PagingData<HeartRate>>

    /**
     * Get paged heart rate measurements for a device
     * @param deviceId The ID of the device
     * @return Flow of paged heart rate measurements for the device
     */
    fun getPagedHeartRatesForDevice(deviceId: Long): Flow<PagingData<HeartRate>>

    /**
     * Get paged heart rate measurements for a date range
     * @param startTime The start of the date range
     * @param endTime The end of the date range
     * @return Flow of paged heart rate measurements in the date range
     */
    fun getPagedHeartRatesInRange(startTime: LocalDateTime, endTime: LocalDateTime): Flow<PagingData<HeartRate>>

    /**
     * Get the latest heart rate measurement
     * @return Flow of the latest heart rate measurement, or null if none exists
     */
    fun getLatestHeartRate(): Flow<HeartRate?>

    /**
     * Get the latest heart rate measurement for a device
     * @param deviceId The ID of the device
     * @return Flow of the latest heart rate measurement for the device, or null if none exists
     */
    fun getLatestHeartRateForDevice(deviceId: Long): Flow<HeartRate?>

    /**
     * Get heart rate statistics for a date range
     * @param startTime The start of the date range
     * @param endTime The end of the date range
     * @return Flow of heart rate statistics for the date range
     */
    fun getHeartRateStatsInRange(startTime: LocalDateTime, endTime: LocalDateTime): Flow<HeartRateStats?>

    /**
     * Get heart rate statistics by date
     * @param startTime The start of the date range
     * @param endTime The end of the date range
     * @return Flow of heart rate statistics by date
     */
    fun getHeartRateStatsByDate(startTime: LocalDateTime, endTime: LocalDateTime): Flow<List<HeartRateStatsByDate>>

    /**
     * Save a heart rate measurement
     * @param heartRate The heart rate measurement to save
     * @return Result of the save operation
     */
    suspend fun saveHeartRate(heartRate: HeartRate): HealthDataResult<HeartRate>

    /**
     * Save multiple heart rate measurements
     * @param heartRates The heart rate measurements to save
     * @return Result of the save operation
     */
    suspend fun saveHeartRates(heartRates: List<HeartRate>): HealthDataResult<List<HeartRate>>

    /**
     * Delete a heart rate measurement
     * @param heartRate The heart rate measurement to delete
     * @return Result of the delete operation
     */
    suspend fun deleteHeartRate(heartRate: HeartRate): HealthDataResult<Boolean>

    /**
     * Delete a heart rate measurement by ID
     * @param id The ID of the heart rate measurement to delete
     * @return Result of the delete operation
     */
    suspend fun deleteHeartRateById(id: Long): HealthDataResult<Boolean>

    /**
     * ====================================
     * Blood Oxygen Operations
     * ====================================
     */

    /**
     * Get a blood oxygen measurement by ID
     * @param id The ID of the blood oxygen measurement
     * @return Flow of the blood oxygen measurement, or null if not found
     */
    fun getBloodOxygen(id: Long): Flow<BloodOxygen?>

    /**
     * Get all blood oxygen measurements
     * @return Flow of all blood oxygen measurements
     */
    fun getAllBloodOxygens(): Flow<List<BloodOxygen>>

    /**
     * Get blood oxygen measurements for a device
     * @param deviceId The ID of the device
     * @return Flow of blood oxygen measurements for the device
     */
    fun getBloodOxygensForDevice(deviceId: Long): Flow<List<BloodOxygen>>

    /**
     * Get blood oxygen measurements for a date range
     * @param startTime The start of the date range
     * @param endTime The end of the date range
     * @return Flow of blood oxygen measurements in the date range
     */
    fun getBloodOxygensInRange(startTime: LocalDateTime, endTime: LocalDateTime): Flow<List<BloodOxygen>>

    /**
     * Get blood oxygen measurements for a device in a date range
     * @param deviceId The ID of the device
     * @param startTime The start of the date range
     * @param endTime The end of the date range
     * @return Flow of blood oxygen measurements for the device in the date range
     */
    fun getBloodOxygensForDeviceInRange(deviceId: Long, startTime: LocalDateTime, endTime: LocalDateTime): Flow<List<BloodOxygen>>

    /**
     * Get blood oxygen measurements for a specific date
     * @param date The date to get measurements for
     * @return Flow of blood oxygen measurements for the date
     */
    fun getBloodOxygensForDate(date: LocalDate): Flow<List<BloodOxygen>>

    /**
     * Get paged blood oxygen measurements
     * @return Flow of paged blood oxygen measurements
     */
    fun getPagedBloodOxygens(): Flow<PagingData<BloodOxygen>>

    /**
     * Get paged blood oxygen measurements for a device
     * @param deviceId The ID of the device
     * @return Flow of paged blood oxygen measurements for the device
     */
    fun getPagedBloodOxygensForDevice(deviceId: Long): Flow<PagingData<BloodOxygen>>

    /**
     * Get the latest blood oxygen measurement
     * @return Flow of the latest blood oxygen measurement, or null if none exists
     */
    fun getLatestBloodOxygen(): Flow<BloodOxygen?>

    /**
     * Get the latest blood oxygen measurement for a device
     * @param deviceId The ID of the device
     * @return Flow of the latest blood oxygen measurement for the device, or null if none exists
     */
    fun getLatestBloodOxygenForDevice(deviceId: Long): Flow<BloodOxygen?>

    /**
     * Get blood oxygen statistics for a date range
     * @param startTime The start of the date range
     * @param endTime The end of the date range
     * @return Flow of blood oxygen statistics for the date range
     */
    fun getBloodOxygenStatsInRange(startTime: LocalDateTime, endTime: LocalDateTime): Flow<BloodOxygenStats?>

    /**
     * Get blood oxygen statistics by date
     * @param startTime The start of the date range
     * @param endTime The end of the date range
     * @return Flow of blood oxygen statistics by date
     */
    fun getBloodOxygenStatsByDate(startTime: LocalDateTime, endTime: LocalDateTime): Flow<List<BloodOxygenStatsByDate>>

    /**
     * Save a blood oxygen measurement
     * @param bloodOxygen The blood oxygen measurement to save
     * @return Result of the save operation
     */
    suspend fun saveBloodOxygen(bloodOxygen: BloodOxygen): HealthDataResult<BloodOxygen>

    /**
     * Save multiple blood oxygen measurements
     * @param bloodOxygens The blood oxygen measurements to save
     * @return Result of the save operation
     */
    suspend fun saveBloodOxygens(bloodOxygens: List<BloodOxygen>): HealthDataResult<List<BloodOxygen>>

    /**
     * Delete a blood oxygen measurement
     * @param bloodOxygen The blood oxygen measurement to delete
     * @return Result of the delete operation
     */
    suspend fun deleteBloodOxygen(bloodOxygen: BloodOxygen): HealthDataResult<Boolean>

    /**
     * Delete a blood oxygen measurement by ID
     * @param id The ID of the blood oxygen measurement to delete
     * @return Result of the delete operation
     */
    suspend fun deleteBloodOxygenById(id: Long): HealthDataResult<Boolean>

    /**
     * ====================================
     * Blood Pressure Operations
     * ====================================
     */

    /**
     * Get a blood pressure measurement by ID
     * @param id The ID of the blood pressure measurement
     * @return Flow of the blood pressure measurement, or null if not found
     */
    fun getBloodPressure(id: Long): Flow<BloodPressure?>

    /**
     * Get all blood pressure measurements
     * @return Flow of all blood pressure measurements
     */
    fun getAllBloodPressures(): Flow<List<BloodPressure>>

    /**
     * Get blood pressure measurements for a device
     * @param deviceId The ID of the device
     * @return Flow of blood pressure measurements for the device
     */
    fun getBloodPressuresForDevice(deviceId: Long): Flow<List<BloodPressure>>

    /**
     * Get blood pressure measurements for a date range
     * @param startTime The start of the date range
     * @param endTime The end of the date range
     * @return Flow of blood pressure measurements in the date range
     */
    fun getBloodPressuresInRange(startTime: LocalDateTime, endTime: LocalDateTime): Flow<List<BloodPressure>>

    /**
     * Get blood pressure measurements for a device in a date range
     * @param deviceId The ID of the device
     * @param startTime The start of the date range
     * @param endTime The end of the date range
     * @return Flow of blood pressure measurements for the device in the date range
     */
    fun getBloodPressuresForDeviceInRange(deviceId: Long, startTime: LocalDateTime, endTime: LocalDateTime): Flow<List<BloodPressure>>

    /**
     * Get blood pressure measurements for a specific date
     * @param date The date to get measurements for
     * @return Flow of blood pressure measurements for the date
     */
    fun getBloodPressuresForDate(date: LocalDate): Flow<List<BloodPressure>>

    /**
     * Get paged blood pressure measurements
     * @return Flow of paged blood pressure measurements
     */
    fun getPagedBloodPressures(): Flow<PagingData<BloodPressure>>

    /**
     * Get paged blood pressure measurements for a device
     * @param deviceId The ID of the device
     * @return Flow of paged blood pressure measurements for the device
     */
    fun getPagedBloodPressuresForDevice(deviceId: Long): Flow<PagingData<BloodPressure>>

    /**
     * Get the latest blood pressure measurement
     * @return Flow of the latest blood pressure measurement, or null if none exists
     */
    fun getLatestBloodPressure(): Flow<BloodPressure?>

    /**
     * Get the latest blood pressure measurement for a device
     * @param deviceId The ID of the device
     * @return Flow of the latest blood pressure measurement for the device, or null if none exists
     */
    fun getLatestBloodPressureForDevice(deviceId: Long): Flow<BloodPressure?>

    /**
     * Get blood pressure statistics for a date range
     * @param startTime The start of the date range
     * @param endTime The end of the date range
     * @return Flow of blood pressure statistics for the date range
     */
    fun getBloodPressureStatsInRange(startTime: LocalDateTime, endTime: LocalDateTime): Flow<BloodPressureStats?>

    /**
     * Get blood pressure statistics by date
     * @param startTime The start of the date range
     * @param endTime The end of the date range
     * @return Flow of blood pressure statistics by date
     */
    fun getBloodPressureStatsByDate(startTime: LocalDateTime, endTime: LocalDateTime): Flow<List<BloodPressureStatsByDate>>

    /**
     * Save a blood pressure measurement
     * @param bloodPressure The blood pressure measurement to save
     * @return Result of the save operation
     */
    suspend fun saveBloodPressure(bloodPressure: BloodPressure): HealthDataResult<BloodPressure>

    /**
     * Save multiple blood pressure measurements
     * @param bloodPressures The blood pressure measurements to save
     * @return Result of the save operation
     */
    suspend fun saveBloodPressures(bloodPressures: List<BloodPressure>): HealthDataResult<List<BloodPressure>>

    /**
     * Delete a blood pressure measurement
     * @param bloodPressure The blood pressure measurement to delete
     * @return Result of the delete operation
     */
    suspend fun deleteBloodPressure(bloodPressure: BloodPressure): HealthDataResult<Boolean>

    /**
     * Delete a blood pressure measurement by ID
     * @param id The ID of the blood pressure measurement to delete
     * @return Result of the delete operation
     */
    suspend fun deleteBloodPressureById(id: Long): HealthDataResult<Boolean>

    /**
     * ====================================
     * Step and Activity Data Operations
     * ====================================
     */

    /**
     * Get a step record by ID
     * @param id The ID of the step record
     * @return Flow of the step record, or null if not found
     */
    fun getStepData(id: Long): Flow<StepData?>

    /**
     * Get all step records
     * @return Flow of all step records
     */
    fun getAllStepData(): Flow<List<StepData>>

    /**
     * Get step records for a device
     * @param deviceId The ID of the device
     * @return Flow of step records for the device
     */
    fun getStepDataForDevice(deviceId: Long): Flow<List<StepData>>

    /**
     * Get step records for a date range
     * @param startDate The start of the date range
     * @param endDate The end of the date range
     * @return Flow of step records in the date range
     */
    fun getStepDataInRange(startDate: LocalDate, endDate: LocalDate): Flow<List<StepData>>

    /**
     * Get step records for a device in a date range
     * @param deviceId The ID of the device
     * @param startDate The start of the date range
     * @param endDate The end of the date range
     * @return Flow of step records for the device in the date range
     */
    fun getStepDataForDeviceInRange(deviceId: Long, startDate: LocalDate, endDate: LocalDate): Flow<List<StepData>>

    /**
     * Get step record for a specific date
     * @param date The date to get the record for
     * @return Flow of the step record for the date, or null if not found
     */
    fun getStepDataForDate(date: LocalDate): Flow<StepData?>

    /**
     * Get step record for a device on a specific date
     * @param deviceId The ID of the device
     * @param date The date to get the record for
     * @return Flow of the step record for the device on the date, or null if not found
     */
    fun getStepDataForDeviceAndDate(deviceId: Long, date: LocalDate): Flow<StepData?>

    /**
     * Get paged step records
     * @return Flow of paged step records
     */
    fun getPagedStepData(): Flow<PagingData<StepData>>

    /**
     * Get paged step records for a device
     * @param deviceId The ID of the device
     * @return Flow of paged step records for the device
     */
    fun getPagedStepDataForDevice(deviceId: Long): Flow<PagingData<StepData>>

    /**
     * Get the latest step record
     * @return Flow of the latest step record, or null if none exists
     */
    fun getLatestStepData(): Flow<StepData?>

    /**
     * Get the latest step record for a device
     * @param deviceId The ID of the device
     * @return Flow of the latest step record for the device, or null if none exists
     */
    fun getLatestStepDataForDevice(deviceId: Long): Flow<StepData?>

    /**
     * Get step statistics by date
     * @param startDate The start of the date range
     * @param endDate The end of the date range
     * @return Flow of step statistics by date
     */
    fun getStepStatsByDate(startDate: LocalDate, endDate: LocalDate): Flow<List<StepStatsByDate>>

    /**
     * Get step statistics by week
     * @param startDate The start of the date range
     * @param endDate The end of the date range
     * @return Flow of step statistics by week
     */
    fun getStepStatsByWeek(startDate: LocalDate, endDate: LocalDate): Flow<List<StepStatsByWeek>>

    /**
     * Get step statistics by month
     * @param startDate The start of the date range
     * @param endDate The end of the date range
     * @return Flow of step statistics by month
     */
    fun getStepStatsByMonth(startDate: LocalDate, endDate: LocalDate): Flow<List<StepStatsByMonth>>

    /**
     * Get current streak of days with goal achieved
     * @param deviceId The ID of the device
     * @return Flow of the current streak, or null if no streak exists
     */
    fun getCurrentGoalAchievedStreak(deviceId: Long): Flow<StepStreak?>

    /**
     * Get best streak of days with goal achieved
     * @param deviceId The ID of the device
     * @return Flow of the best streak, or null if no streak exists
     */
    fun getBestGoalAchievedStreak(deviceId: Long): Flow<StepStreak?>

    /**
     * Get hourly step data for a specific date
     * @param date The date to get hourly data for
     * @return Flow of hourly step data for the date
     */
    fun getHourlyStepDataForDate(date: LocalDate): Flow<List<HourlyStepData>>

    /**
     * Get hourly step data for a device on a specific date
     * @param deviceId The ID of the device
     * @param date The date to get hourly data for
     * @return Flow of hourly step data for the device on the date
     */
    fun getHourlyStepDataForDeviceAndDate(deviceId: Long, date: LocalDate): Flow<List<HourlyStepData>>

    /**
     * Save a step record
     * @param stepData The step record to save
     * @return Result of the save operation
     */
    suspend fun saveStepData(stepData: StepData): HealthDataResult<StepData>

    /**
     * Save multiple step records
     * @param stepDataList The step records to save
     * @return Result of the save operation
     */
    suspend fun saveStepDataList(stepDataList: List<StepData>): HealthDataResult<List<StepData>>

    /**
     * Save hourly step data
     * @param hourlyStepData The hourly step data to save
     * @return Result of the save operation
     */
    suspend fun saveHourlyStepData(hourlyStepData: HourlyStepData): HealthDataResult<HourlyStepData>

    /**
     * Save multiple hourly step data records
     * @param hourlyStepDataList The hourly step data records to save
     * @return Result of the save operation
     */
    suspend fun saveHourlyStepDataList(hourlyStepDataList: List<HourlyStepData>): HealthDataResult<List<HourlyStepData>>

    /**
     * Delete a step record
     * @param stepData The step record to delete
     * @return Result of the delete operation
     */
    suspend fun deleteStepData(stepData: StepData): HealthDataResult<Boolean>

    /**
     * Delete a step record by ID
     * @param id The ID of the step record to delete
     * @return Result of the delete operation
     */
    suspend fun deleteStepDataById(id: Long): HealthDataResult<Boolean>

    /**
     * ====================================
     * Sleep Tracking Operations
     * ====================================
     */

    /**
     * Get a sleep record by ID
     * @param id The ID of the sleep record
     * @return Flow of the sleep record, or null if not found
     */
    fun getSleep(id: Long): Flow<Sleep?>

    /**
     * Get all sleep records
     * @return Flow of all sleep records
     */
    fun getAllSleeps(): Flow<List<Sleep>>

    /**
     * Get sleep records for a device
     * @param deviceId The ID of the device
     * @return Flow of sleep records for the device
     */
    fun getSleepsForDevice(deviceId: Long): Flow<List<Sleep>>

    /**
     * Get sleep records for a date range
     * @param startDate The start of the date range
     * @param endDate The end of the date range
     * @return Flow of sleep records in the date range
     */
    fun getSleepsInRange(startDate: LocalDate, endDate: LocalDate): Flow<List<Sleep>>

    /**
     * Get sleep records for a device in a date range
     * @param deviceId The ID of the device
     * @param startDate The start of the date range
     * @param endDate The end of the date range
     * @return Flow of sleep records for the device in the date range
     */
    fun getSleepsForDeviceInRange(deviceId: Long, startDate: LocalDate, endDate: LocalDate): Flow<List<Sleep>>

    /**
     * Get sleep record for a specific date
     * @param date The date to get the record for
     * @return Flow of the sleep record for the date, or null if not found
     */
    fun getSleepForDate(date: LocalDate): Flow<Sleep?>

    /**
     * Get sleep record for a device on a specific date
     * @param deviceId The ID of the device
     * @param date The date to get the record for
     * @return Flow of the sleep record for the device on the date, or null if not found
     */
    fun getSleepForDeviceAndDate(deviceId: Long, date: LocalDate): Flow<Sleep?>

    /**
     * Get paged sleep records
     * @return Flow of paged sleep records
     */
    fun getPagedSleeps(): Flow<PagingData<Sleep>>

    /**
     * Get paged sleep records for a device
     * @param deviceId The ID of the device
     * @return Flow of paged sleep records for the device
     */
    fun getPagedSleepsForDevice(deviceId: Long): Flow<PagingData<Sleep>>

    /**
     * Get the latest sleep record
     * @return Flow of the latest sleep record, or null if none exists
     */
    fun getLatestSleep(): Flow<Sleep?>

    /**
     * Get the latest sleep record for a device
     * @param deviceId The ID of the device
     * @return Flow of the latest sleep record for the device, or null if none exists
     */
    fun getLatestSleepForDevice(deviceId: Long): Flow<Sleep?>

    /**
     * Get sleep statistics for a date range
     * @param startDate The start of the date range
     * @param endDate The end of the date range
     * @return Flow of sleep statistics for the date range
     */
    fun getSleepStatsInRange(startDate: LocalDate, endDate: LocalDate): Flow<SleepStats?>

    /**
     * Get sleep statistics by date
     * @param startDate The start of the date range
     * @param endDate The end of the date range
     * @return Flow of sleep statistics by date
     */
    fun getSleepStatsByDate(startDate: LocalDate, endDate: LocalDate): Flow<List<SleepStatsByDate>>

    /**
     * Get sleep statistics by week
     * @param startDate The start of the date range
     * @param endDate The end of the date range
     * @return Flow of sleep statistics by week
     */
    fun getSleepStatsByWeek(startDate: LocalDate, endDate: LocalDate): Flow<List<SleepStatsByWeek>>

    /**
     * Save a sleep record
     * @param sleep The sleep record to save
     * @return Result of the save operation
     */
    suspend fun saveSleep(sleep: Sleep): HealthDataResult<Sleep>

    /**
     * Save multiple sleep records
     * @param sleeps The sleep records to save
     * @return Result of the save operation
     */
    suspend fun saveSleeps(sleeps: List<Sleep>): HealthDataResult<List<Sleep>>

    /**
     * Delete a sleep record
     * @param sleep The sleep record to delete
     * @return Result of the delete operation
     */
    suspend fun deleteSleep(sleep: Sleep): HealthDataResult<Boolean>

    /**
     * Delete a sleep record by ID
     * @param id The ID of the sleep record to delete
     * @return Result of the delete operation
     */
    suspend fun deleteSleepById(id: Long): HealthDataResult<Boolean>

    /**
     * ====================================
     * Temperature Operations
     * ====================================
     */

    /**
     * Get a temperature measurement by ID
     * @param id The ID of the temperature measurement
     * @return Flow of the temperature measurement, or null if not found
     */
    fun getTemperature(id: Long): Flow<Temperature?>

    /**
     * Get all temperature measurements
     * @return Flow of all temperature measurements
     */
    fun getAllTemperatures(): Flow<List<Temperature>>

    /**
     * Get temperature measurements for a device
     * @param deviceId The ID of the device
     * @return Flow of temperature measurements for the device
     */
    fun getTemperaturesForDevice(deviceId: Long): Flow<List<Temperature>>

    /**
     * Get temperature measurements for a date range
     * @param startTime The start of the date range
     * @param endTime The end of the date range
     * @return Flow of temperature measurements in the date range
     */
    fun getTemperaturesInRange(startTime: LocalDateTime, endTime: LocalDateTime): Flow<List<Temperature>>

    /**
     * Get temperature measurements for a device in a date range
     * @param deviceId The ID of the device
     * @param startTime The start of the date range
     * @param endTime The end of the date range
     * @return Flow of temperature measurements for the device in the date range
     */
    fun getTemperaturesForDeviceInRange(deviceId: Long, startTime: LocalDateTime, endTime: LocalDateTime): Flow<List<Temperature>>

    /**
     * Get temperature measurements for a specific date
     * @param date The date to get measurements for
     * @return Flow of temperature measurements for the date
     */
    fun getTemperaturesForDate(date: LocalDate): Flow<List<Temperature>>

    /**
     * Get paged temperature measurements
     * @return Flow of paged temperature measurements
     */
    fun getPagedTemperatures(): Flow<PagingData<Temperature>>

    /**
     * Get the latest temperature measurement
     * @return Flow of the latest temperature measurement, or null if none exists
     */
    fun getLatestTemperature(): Flow<Temperature?>

    /**
     * Get the latest temperature measurement for a device
     * @param deviceId The ID of the device
     * @return Flow of the latest temperature measurement for the device, or null if none exists
     */
    fun getLatestTemperatureForDevice(deviceId: Long): Flow<Temperature?>

    /**
     * Save a temperature measurement
     * @param temperature The temperature measurement to save
     * @return Result of the save operation
     */
    suspend fun saveTemperature(temperature: Temperature): HealthDataResult<Temperature>

    /**
     * Save multiple temperature measurements
     * @param temperatures The temperature measurements to save
     * @return Result of the save operation
     */
    suspend fun saveTemperatures(temperatures: List<Temperature>): HealthDataResult<List<Temperature>>

    /**
     * Delete a temperature measurement
     * @param temperature The temperature measurement to delete
     * @return Result of the delete operation
     */
    suspend fun deleteTemperature(temperature: Temperature): HealthDataResult<Boolean>

    /**
     * Delete a temperature measurement by ID
     * @param id The ID of the temperature measurement to delete
     * @return Result of the delete operation
     */
    suspend fun deleteTemperatureById(id: Long): HealthDataResult<Boolean>

    /**
     * ====================================
     * Activity/Exercise Operations
     * ====================================
     */

    /**
     * Get an activity record by ID
     * @param id The ID of the activity record
     * @return Flow of the activity record, or null if not found
     */
    fun getActivity(id: Long): Flow<Activity?>

    /**
     * Get all activity records
     * @return Flow of all activity records
     */
    fun getAllActivities(): Flow<List<Activity>>

    /**
     * Get activity records for a device
     * @param deviceId The ID of the device
     * @return Flow of activity records for the device
     */
    fun getActivitiesForDevice(deviceId: Long): Flow<List<Activity>>

    /**
     * Get activity records for a date range
     * @param startDate The start of the date range
     * @param endDate The end of the date range
     * @return Flow of activity records in the date range
     */
    fun getActivitiesInRange(startDate: LocalDate, endDate: LocalDate): Flow<List<Activity>>

    /**
     * Get activity records for a device in a date range
     * @param deviceId The ID of the device
     * @param startDate The start of the date range
     * @param endDate The end of the date range
     * @return Flow of activity records for the device in the date range
     */
    fun getActivitiesForDeviceInRange(deviceId: Long, startDate: LocalDate, endDate: LocalDate): Flow<List<Activity>>

    /**
     * Get activity records for a specific date
     * @param date The date to get records for
     * @return Flow of activity records for the date
     */
    fun getActivitiesForDate(date: LocalDate): Flow<List<Activity>>

    /**
     * Get activity records for a device on a specific date
     * @param deviceId The ID of the device
     * @param date The date to get records for
     * @return Flow of activity records for the device on the date
     */
    fun getActivitiesForDeviceAndDate(deviceId: Long, date: LocalDate): Flow<List<Activity>>

    /**
     * Get activity records by type
     * @param activityType The type of activity
     * @return Flow of activity records of the specified type
     */
    fun getActivitiesByType(activityType: ActivityType): Flow<List<Activity>>

    /**
     * Get activity records for a device by type
     * @param deviceId The ID of the device
     * @param activityType The type of activity
     * @return Flow of activity records for the device of the specified type
     */
    fun getActivitiesForDeviceByType(deviceId: Long, activityType: ActivityType): Flow<List<Activity>>

    /**
     * Get paged activity records
     * @return Flow of paged activity records
     */
    fun getPagedActivities(): Flow<PagingData<Activity>>

    /**
     * Get paged activity records for a device
     * @param deviceId The ID of the device
     * @return Flow of paged activity records for the device
     */
    fun getPagedActivitiesForDevice(deviceId: Long): Flow<PagingData<Activity>>

    /**
     * Get the latest activity record
     * @return Flow of the latest activity record, or null if none exists
     */
    fun getLatestActivity(): Flow<Activity?>

    /**
     * Get the latest activity record for a device
     * @param deviceId The ID of the device
     * @return Flow of the latest activity record for the device, or null if none exists
     */
    fun getLatestActivityForDevice(deviceId: Long): Flow<Activity?>

    /**
     * Get activity statistics by type
     * @param startDate The start of the date range
     * @param endDate The end of the date range
     * @return Flow of activity statistics by type
     */
    fun getActivityStatsByType(startDate: LocalDate, endDate: LocalDate): Flow<Map<ActivityType, ActivityStats>>

    /**
     * Save an activity record
     * @param activity The activity record to save
     * @return Result of the save operation
     */
    suspend fun saveActivity(activity: Activity): HealthDataResult<Activity>

    /**
     * Save multiple activity records
     * @param activities The activity records to save
     * @return Result of the save operation
     */
    suspend fun saveActivities(activities: List<Activity>): HealthDataResult<List<Activity>>

    /**
     * Delete an activity record
     * @param activity The activity record to delete
     * @return Result of the delete operation
     */
    suspend fun deleteActivity(activity: Activity): HealthDataResult<Boolean>

    /**
     * Delete an activity record by ID
     * @param id The ID of the activity record to delete
     * @return Result of the delete operation
     */
    suspend fun deleteActivityById(id: Long): HealthDataResult<Boolean>

    /**
     * ====================================
     * Health Insights Operations
     * ====================================
     */

    /**
     * Get a health insight by ID
     * @param id The ID of the health insight
     * @return Flow of the health insight, or null if not found
     */
    fun getHealthInsight(id: Long): Flow<HealthInsight?>

    /**
     * Get all health insights
     * @return Flow of all health insights
     */
    fun getAllHealthInsights(): Flow<List<HealthInsight>>

    /**
     * Get health insights for a user
     * @param userId The ID of the user
     * @return Flow of health insights for the user
     */
    fun getHealthInsightsForUser(userId: Long): Flow<List<HealthInsight>>

    /**
     * Get health insights for a date range
     * @param startDate The start of the date range
     * @param endDate The end of the date range
     * @return Flow of health insights in the date range
     */
    fun getHealthInsightsInRange(startDate: LocalDate, endDate: LocalDate): Flow<List<HealthInsight>>

    /**
     * Get health insights for a user in a date range
     * @param userId The ID of the user
     * @param startDate The start of the date range
     * @param endDate The end of the date range
     * @return Flow of health insights for the user in the date range
     */
    fun getHealthInsightsForUserInRange(userId: Long, startDate: LocalDate, endDate: LocalDate): Flow<List<HealthInsight>>

    /**
     * Get health insights for a specific date
     * @param date The date to get insights for
     * @return Flow of health insights for the date
     */
    fun getHealthInsightsForDate(date: LocalDate): Flow<List<HealthInsight>>

    /**
     * Get health insights by type
     * @param insightType The type of insight
     * @return Flow of health insights of the specified type
     */
    fun getHealthInsightsByType(insightType: HealthInsightType): Flow<List<HealthInsight>>

    /**
     * Get health insights by severity
     * @param severity The severity level
     * @return Flow of health insights with the specified severity
     */
    fun getHealthInsightsBySeverity(severity: HealthInsightSeverity): Flow<List<HealthInsight>>

    /**
     * Get the latest health insight for each type
     * @param userId The ID of the user
     * @return Flow of the latest health insight for each type
     */
    fun getLatestHealthInsightsByType(userId: Long): Flow<Map<HealthInsightType, HealthInsight>>

    /**
     * Save a health insight
     * @param healthInsight The health insight to save
     * @return Result of the save operation
     */
    suspend fun saveHealthInsight(healthInsight: HealthInsight): HealthDataResult<HealthInsight>

    /**
     * Save multiple health insights
     * @param healthInsights The health insights to save
     * @return Result of the save operation
     */
    suspend fun saveHealthInsights(healthInsights: List<HealthInsight>): HealthDataResult<List<HealthInsight>>

    /**
     * Delete a health insight
     * @param healthInsight The health insight to delete
     * @return Result of the delete operation
     */
    suspend fun deleteHealthInsight(healthInsight: HealthInsight): HealthDataResult<Boolean>

    /**
     * Delete a health insight by ID
     * @param id The ID of the health insight to delete
     * @return Result of the delete operation
     */
    suspend fun deleteHealthInsightById(id: Long): HealthDataResult<Boolean>

    /**
     * ====================================
     * Sync Operations
     * ====================================
     */

    /**
     * Get health data by sync status
     * @param syncStatus The sync status to filter by
     * @param dataType The type of health data
     * @return Flow of health data with the specified sync status
     */
    fun <T> getHealthDataBySyncStatus(syncStatus: SyncStatus, dataType: HealthDataType): Flow<List<T>>

    /**
     * Update sync status for health data
     * @param id The ID of the health data
     * @param syncStatus The new sync status
     * @param dataType The type of health data
     * @return Result of the update operation
     */
    suspend fun updateHealthDataSyncStatus(id: Long, syncStatus: SyncStatus, dataType: HealthDataType): HealthDataResult<Boolean>

    /**
     * Update sync status for multiple health data items
     * @param ids The IDs of the health data items
     * @param syncStatus The new sync status
     * @param dataType The type of health data
     * @return Result of the update operation
     */
    suspend fun updateHealthDataSyncStatuses(ids: List<Long>, syncStatus: SyncStatus, dataType: HealthDataType): HealthDataResult<Boolean>

    /**
     * Get health data that needs syncing
     * @param dataType The type of health data
     * @param limit Maximum number of items to return
     * @return Flow of health data that needs syncing
     */
    fun <T> getHealthDataNeedingSync(dataType: HealthDataType, limit: Int = 100): Flow<List<T>>

    /**
     * Count health data that needs syncing
     * @param dataType The type of health data
     * @return Flow of the count of health data that needs syncing
     */
    fun getHealthDataNeedingSyncCount(dataType: HealthDataType): Flow<Int>

    /**
     * ====================================
     * Aggregated Health Data Operations
     * ====================================
     */

    /**
     * Get daily health summary
     * @param date The date to get the summary for
     * @param deviceId Optional device ID to filter by
     * @return Flow of the daily health summary
     */
    fun getDailyHealthSummary(date: LocalDate, deviceId: Long? = null): Flow<DailyHealthSummary>

    /**
     * Get weekly health summary
     * @param startDate The start of the week
     * @param deviceId Optional device ID to filter by
     * @return Flow of the weekly health summary
     */
    fun getWeeklyHealthSummary(startDate: LocalDate, deviceId: Long? = null): Flow<WeeklyHealthSummary>

    /**
     * Get monthly health summary
     * @param month The month to get the summary for (format: YYYY-MM)
     * @param deviceId Optional device ID to filter by
     * @return Flow of the monthly health summary
     */
    fun getMonthlyHealthSummary(month: String, deviceId: Long? = null): Flow<MonthlyHealthSummary>
}

/**
 * Sealed class representing the result of a health data operation
 */
sealed class HealthDataResult<out T> {
    data class Success<T>(val data: T) : HealthDataResult<T>()
    data class Error(val error: HealthDataError) : HealthDataResult<Nothing>()
    data object Loading : HealthDataResult<Nothing>()
}

/**
 * Sealed class representing health data errors
 */
sealed class HealthDataError {
    data class DatabaseError(val message: String, val cause: Throwable? = null) : HealthDataError()
    data class ValidationError(val message: String, val field: String? = null) : HealthDataError()
    data class SyncError(val message: String, val dataType: HealthDataType? = null, val cause: Throwable? = null) : HealthDataError()
    data class DataNotFoundError(val id: Long, val dataType: HealthDataType, val message: String = "Data not found") : HealthDataError()
    data class DeviceNotFoundError(val deviceId: Long, val message: String = "Device not found") : HealthDataError()
    data class PermissionError(val message: String, val permission: String) : HealthDataError()
    data class TimeoutError(val message: String, val operation: String) : HealthDataError()
    data class UnknownError(val message: String, val cause: Throwable? = null) : HealthDataError()
}

/**
 * Enum representing the types of health data
 */
enum class HealthDataType {
    HEART_RATE,
    BLOOD_OXYGEN,
    BLOOD_PRESSURE,
    STEPS,
    HOURLY_STEPS,
    SLEEP,
    SLEEP_DETAIL,
    TEMPERATURE,
    ACTIVITY,
    HEALTH_INSIGHT
}

/**
 * Data class representing a daily health summary
 */
data class DailyHealthSummary(
    val date: LocalDate,
    val deviceId: Long?,
    val steps: Int?,
    val distance: Float?,
    val calories: Float?,
    val activeMinutes: Int?,
    val averageHeartRate: Float?,
    val minHeartRate: Int?,
    val maxHeartRate: Int?,
    val averageBloodOxygen: Float?,
    val bloodPressure: Pair<Int, Int>?, // Systolic, Diastolic
    val sleepDuration: Int?,
    val sleepQuality: Int?,
    val activities: List<Activity>
)

/**
 * Data class representing a weekly health summary
 */
data class WeeklyHealthSummary(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val deviceId: Long?,
    val dailySummaries: List<DailyHealthSummary>,
    val totalSteps: Int,
    val averageDailySteps: Float,
    val totalDistance: Float,
    val totalCalories: Float,
    val totalActiveMinutes: Int,
    val averageHeartRate: Float,
    val minHeartRate: Int,
    val maxHeartRate: Int,
    val averageBloodOxygen: Float,
    val averageSleepDuration: Float,
    val averageSleepQuality: Float,
    val activities: List<Activity>
)

/**
 * Data class representing a monthly health summary
 */
data class MonthlyHealthSummary(
    val month: String, // Format: YYYY-MM
    val deviceId: Long?,
    val weeklySummaries: List<WeeklyHealthSummary>,
    val totalSteps: Int,
    val averageDailySteps: Float,
    val totalDistance: Float,
    val totalCalories: Float,
    val totalActiveMinutes: Int,
    val averageHeartRate: Float,
    val minHeartRate: Int,
    val maxHeartRate: Int,
    val averageBloodOxygen: Float,
    val averageSleepDuration: Float,
    val averageSleepQuality: Float,
    val activities: Map<ActivityType, Int> // Activity type and count
)
