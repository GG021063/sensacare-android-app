package com.sensacare.app.data.repository

import android.util.LruCache
import com.sensacare.app.data.local.dao.*
import com.sensacare.app.data.local.entity.*
import com.sensacare.app.data.mapper.*
import com.sensacare.app.data.remote.NetworkDataSource
import com.sensacare.app.data.remote.model.SyncRequest
import com.sensacare.app.data.remote.model.SyncResponse
import com.sensacare.app.domain.model.*
import com.sensacare.app.domain.repository.HealthDataRepository
import com.sensacare.app.domain.util.Result
import com.sensacare.app.domain.util.SyncStatus
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of the HealthDataRepository interface that uses Room DAOs
 * to provide a complete data layer for health data management.
 *
 * This repository handles:
 * - CRUD operations for all health data types
 * - Data mapping between domain models and database entities
 * - Error handling and result wrapping
 * - In-memory caching for frequently accessed data
 * - Data synchronization with remote server
 * - Reactive data streams with Flow
 */
@Singleton
class HealthDataRepositoryImpl @Inject constructor(
    private val healthDataDao: HealthDataDao,
    private val heartRateDao: HeartRateDao,
    private val bloodPressureDao: BloodPressureDao,
    private val sleepDao: SleepDao,
    private val activityDao: ActivityDao,
    private val deviceDao: DeviceDao,
    private val healthGoalDao: HealthGoalDao,
    private val alertDao: AlertDao,
    private val networkDataSource: NetworkDataSource,
    private val ioDispatcher: CoroutineDispatcher,
    private val healthDataMapper: HealthDataMapper,
    private val heartRateMapper: HeartRateMapper,
    private val bloodPressureMapper: BloodPressureMapper,
    private val sleepMapper: SleepMapper,
    private val activityMapper: ActivityMapper,
    private val deviceMapper: DeviceMapper,
    private val goalMapper: HealthGoalMapper,
    private val alertMapper: HealthAlertMapper
) : HealthDataRepository {

    companion object {
        private const val TAG = "HealthDataRepository"
        private const val CACHE_SIZE = 100 // Maximum number of items to keep in cache
        private const val SYNC_BATCH_SIZE = 50 // Number of items to sync in a single batch
        private const val MAX_SYNC_ATTEMPTS = 3 // Maximum number of sync attempts
    }

    // In-memory caches for frequently accessed data
    private val healthDataCache = LruCache<String, HealthData>(CACHE_SIZE)
    private val heartRateCache = LruCache<String, HeartRate>(CACHE_SIZE)
    private val bloodPressureCache = LruCache<String, BloodPressure>(CACHE_SIZE)
    private val sleepCache = LruCache<String, Sleep>(CACHE_SIZE)
    private val activityCache = LruCache<String, Activity>(CACHE_SIZE)
    private val deviceCache = LruCache<String, Device>(CACHE_SIZE)
    private val goalCache = LruCache<String, HealthGoal>(CACHE_SIZE)
    private val alertCache = LruCache<String, HealthAlert>(CACHE_SIZE)

    // Last sync timestamps by data type
    private var lastSyncTimestamps = mutableMapOf<String, LocalDateTime>()

    /**
     * General Health Data Operations
     */

    override suspend fun getHealthData(id: String): Result<HealthData> = withContext(ioDispatcher) {
        try {
            // Check cache first
            val cachedData = healthDataCache.get(id)
            if (cachedData != null) {
                return@withContext Result.Success(cachedData)
            }

            // Fetch from database
            val entity = healthDataDao.getById(id)
            if (entity != null) {
                val healthData = healthDataMapper.mapToDomain(entity)
                // Update cache
                healthDataCache.put(id, healthData)
                Result.Success(healthData)
            } else {
                Result.Error(Exception("Health data not found"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting health data: %s", id)
            Result.Error(e)
        }
    }

    override fun getHealthDataAsFlow(id: String): Flow<Result<HealthData>> {
        return healthDataDao.getByIdAsFlow(id)
            .map { entity ->
                if (entity != null) {
                    val healthData = healthDataMapper.mapToDomain(entity)
                    // Update cache
                    healthDataCache.put(id, healthData)
                    Result.Success(healthData)
                } else {
                    Result.Error(Exception("Health data not found"))
                }
            }
            .catch { e ->
                Timber.e(e, "Error getting health data flow: %s", id)
                emit(Result.Error(e))
            }
            .flowOn(ioDispatcher)
    }

    override suspend fun saveHealthData(healthData: HealthData): Result<String> = withContext(ioDispatcher) {
        try {
            val entity = healthDataMapper.mapToEntity(healthData)
            val id = healthDataDao.insert(entity)
            
            // Update cache
            healthDataCache.put(entity.id, healthData)
            
            // Queue for sync if needed
            if (healthData.syncStatus != SyncStatus.SYNCED) {
                queueForSync(entity)
            }
            
            Result.Success(entity.id)
        } catch (e: Exception) {
            Timber.e(e, "Error saving health data: %s", healthData.id)
            Result.Error(e)
        }
    }

    override suspend fun updateHealthData(healthData: HealthData): Result<Boolean> = withContext(ioDispatcher) {
        try {
            val entity = healthDataMapper.mapToEntity(healthData)
            val rowsUpdated = healthDataDao.update(entity)
            
            if (rowsUpdated > 0) {
                // Update cache
                healthDataCache.put(entity.id, healthData)
                
                // Queue for sync if needed
                if (healthData.syncStatus != SyncStatus.SYNCED) {
                    queueForSync(entity)
                }
                
                Result.Success(true)
            } else {
                Result.Error(Exception("No rows updated"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error updating health data: %s", healthData.id)
            Result.Error(e)
        }
    }

    override suspend fun deleteHealthData(id: String): Result<Boolean> = withContext(ioDispatcher) {
        try {
            val rowsDeleted = healthDataDao.deleteById(id)
            
            if (rowsDeleted > 0) {
                // Remove from cache
                healthDataCache.remove(id)
                
                // Mark as deleted for sync
                markAsDeletedForSync(id, "health_data")
                
                Result.Success(true)
            } else {
                Result.Error(Exception("No rows deleted"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error deleting health data: %s", id)
            Result.Error(e)
        }
    }

    override suspend fun getHealthDataForUser(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<List<HealthData>> = withContext(ioDispatcher) {
        try {
            val entities = healthDataDao.getForUserInTimeRange(
                userId,
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay().minusNanos(1)
            )
            
            val healthDataList = entities.map { healthDataMapper.mapToDomain(it) }
            
            // Update cache for each item
            healthDataList.forEach { healthData ->
                healthDataCache.put(healthData.id, healthData)
            }
            
            Result.Success(healthDataList)
        } catch (e: Exception) {
            Timber.e(e, "Error getting health data for user: %s", userId)
            Result.Error(e)
        }
    }

    override fun getHealthDataForUserAsFlow(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<Result<List<HealthData>>> {
        return healthDataDao.getForUserInTimeRangeAsFlow(
            userId,
            startDate.atStartOfDay(),
            endDate.plusDays(1).atStartOfDay().minusNanos(1)
        )
        .map { entities ->
            val healthDataList = entities.map { healthDataMapper.mapToDomain(it) }
            
            // Update cache for each item
            healthDataList.forEach { healthData ->
                healthDataCache.put(healthData.id, healthData)
            }
            
            Result.Success(healthDataList)
        }
        .catch { e ->
            Timber.e(e, "Error getting health data flow for user: %s", userId)
            emit(Result.Error(e))
        }
        .flowOn(ioDispatcher)
    }

    override suspend fun getHealthDataByMetricType(
        userId: String,
        metricType: MetricType,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<List<HealthData>> = withContext(ioDispatcher) {
        try {
            val entities = healthDataDao.getByMetricTypeInTimeRange(
                userId,
                metricType.name,
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay().minusNanos(1)
            )
            
            val healthDataList = entities.map { healthDataMapper.mapToDomain(it) }
            
            // Update cache for each item
            healthDataList.forEach { healthData ->
                healthDataCache.put(healthData.id, healthData)
            }
            
            Result.Success(healthDataList)
        } catch (e: Exception) {
            Timber.e(e, "Error getting health data by metric type: %s", metricType)
            Result.Error(e)
        }
    }

    override fun getHealthDataByMetricTypeAsFlow(
        userId: String,
        metricType: MetricType,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<Result<List<HealthData>>> {
        return healthDataDao.getByMetricTypeInTimeRangeAsFlow(
            userId,
            metricType.name,
            startDate.atStartOfDay(),
            endDate.plusDays(1).atStartOfDay().minusNanos(1)
        )
        .map { entities ->
            val healthDataList = entities.map { healthDataMapper.mapToDomain(it) }
            
            // Update cache for each item
            healthDataList.forEach { healthData ->
                healthDataCache.put(healthData.id, healthData)
            }
            
            Result.Success(healthDataList)
        }
        .catch { e ->
            Timber.e(e, "Error getting health data flow by metric type: %s", metricType)
            emit(Result.Error(e))
        }
        .flowOn(ioDispatcher)
    }

    override suspend fun getLatestHealthData(
        userId: String,
        metricType: MetricType
    ): Result<HealthData?> = withContext(ioDispatcher) {
        try {
            val entity = healthDataDao.getLatestByMetricType(userId, metricType.name)
            
            if (entity != null) {
                val healthData = healthDataMapper.mapToDomain(entity)
                // Update cache
                healthDataCache.put(healthData.id, healthData)
                Result.Success(healthData)
            } else {
                Result.Success(null)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting latest health data for metric type: %s", metricType)
            Result.Error(e)
        }
    }

    override fun getLatestHealthDataAsFlow(
        userId: String,
        metricType: MetricType
    ): Flow<Result<HealthData?>> {
        return healthDataDao.getLatestByMetricTypeAsFlow(userId, metricType.name)
            .map { entity ->
                if (entity != null) {
                    val healthData = healthDataMapper.mapToDomain(entity)
                    // Update cache
                    healthDataCache.put(healthData.id, healthData)
                    Result.Success(healthData)
                } else {
                    Result.Success(null)
                }
            }
            .catch { e ->
                Timber.e(e, "Error getting latest health data flow for metric type: %s", metricType)
                emit(Result.Error(e))
            }
            .flowOn(ioDispatcher)
    }

    override suspend fun saveHealthDataBatch(healthDataList: List<HealthData>): Result<List<String>> = withContext(ioDispatcher) {
        try {
            val entities = healthDataList.map { healthDataMapper.mapToEntity(it) }
            val ids = healthDataDao.insertAll(entities)
            
            // Update cache for each item
            entities.forEachIndexed { index, entity ->
                healthDataCache.put(entity.id, healthDataList[index])
            }
            
            // Queue for sync if needed
            entities.filter { it.syncStatus != SyncStatus.SYNCED.name }.forEach { queueForSync(it) }
            
            Result.Success(entities.map { it.id })
        } catch (e: Exception) {
            Timber.e(e, "Error saving health data batch")
            Result.Error(e)
        }
    }

    override suspend fun getHealthDataAggregates(
        userId: String,
        metricType: MetricType,
        startDate: LocalDate,
        endDate: LocalDate,
        aggregationType: AggregationType
    ): Result<List<HealthDataAggregate>> = withContext(ioDispatcher) {
        try {
            val aggregates = when (aggregationType) {
                AggregationType.DAILY -> healthDataDao.getDailyAggregates(
                    userId,
                    metricType.name,
                    startDate.atStartOfDay(),
                    endDate.plusDays(1).atStartOfDay().minusNanos(1)
                )
                AggregationType.WEEKLY -> healthDataDao.getWeeklyAggregates(
                    userId,
                    metricType.name,
                    startDate.atStartOfDay(),
                    endDate.plusDays(1).atStartOfDay().minusNanos(1)
                )
                AggregationType.MONTHLY -> healthDataDao.getMonthlyAggregates(
                    userId,
                    metricType.name,
                    startDate.atStartOfDay(),
                    endDate.plusDays(1).atStartOfDay().minusNanos(1)
                )
            }
            
            val domainAggregates = aggregates.map { 
                HealthDataAggregate(
                    date = LocalDate.parse(it.periodStart.split(" ")[0]),
                    metricType = metricType,
                    minValue = it.minValue,
                    maxValue = it.maxValue,
                    avgValue = it.avgValue,
                    sumValue = it.sumValue,
                    count = it.count
                )
            }
            
            Result.Success(domainAggregates)
        } catch (e: Exception) {
            Timber.e(e, "Error getting health data aggregates for metric type: %s", metricType)
            Result.Error(e)
        }
    }

    override fun getHealthDataAggregatesAsFlow(
        userId: String,
        metricType: MetricType,
        startDate: LocalDate,
        endDate: LocalDate,
        aggregationType: AggregationType
    ): Flow<Result<List<HealthDataAggregate>>> {
        return flow {
            emit(getHealthDataAggregates(userId, metricType, startDate, endDate, aggregationType))
        }
        .catch { e ->
            Timber.e(e, "Error getting health data aggregates flow for metric type: %s", metricType)
            emit(Result.Error(e))
        }
        .flowOn(ioDispatcher)
    }

    /**
     * Heart Rate Operations
     */

    override suspend fun getHeartRate(id: String): Result<HeartRate> = withContext(ioDispatcher) {
        try {
            // Check cache first
            val cachedData = heartRateCache.get(id)
            if (cachedData != null) {
                return@withContext Result.Success(cachedData)
            }

            // Fetch from database
            val entity = heartRateDao.getById(id)
            if (entity != null) {
                val heartRate = heartRateMapper.mapToDomain(entity)
                // Update cache
                heartRateCache.put(id, heartRate)
                Result.Success(heartRate)
            } else {
                Result.Error(Exception("Heart rate data not found"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting heart rate: %s", id)
            Result.Error(e)
        }
    }

    override fun getHeartRateAsFlow(id: String): Flow<Result<HeartRate>> {
        return heartRateDao.getByIdAsFlow(id)
            .map { entity ->
                if (entity != null) {
                    val heartRate = heartRateMapper.mapToDomain(entity)
                    // Update cache
                    heartRateCache.put(id, heartRate)
                    Result.Success(heartRate)
                } else {
                    Result.Error(Exception("Heart rate data not found"))
                }
            }
            .catch { e ->
                Timber.e(e, "Error getting heart rate flow: %s", id)
                emit(Result.Error(e))
            }
            .flowOn(ioDispatcher)
    }

    override suspend fun saveHeartRate(heartRate: HeartRate): Result<String> = withContext(ioDispatcher) {
        try {
            val entity = heartRateMapper.mapToEntity(heartRate)
            val id = heartRateDao.insert(entity)
            
            // Update cache
            heartRateCache.put(entity.id, heartRate)
            
            // Queue for sync if needed
            if (heartRate.syncStatus != SyncStatus.SYNCED) {
                queueForSync(entity)
            }
            
            Result.Success(entity.id)
        } catch (e: Exception) {
            Timber.e(e, "Error saving heart rate: %s", heartRate.id)
            Result.Error(e)
        }
    }

    override suspend fun updateHeartRate(heartRate: HeartRate): Result<Boolean> = withContext(ioDispatcher) {
        try {
            val entity = heartRateMapper.mapToEntity(heartRate)
            val rowsUpdated = heartRateDao.update(entity)
            
            if (rowsUpdated > 0) {
                // Update cache
                heartRateCache.put(entity.id, heartRate)
                
                // Queue for sync if needed
                if (heartRate.syncStatus != SyncStatus.SYNCED) {
                    queueForSync(entity)
                }
                
                Result.Success(true)
            } else {
                Result.Error(Exception("No rows updated"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error updating heart rate: %s", heartRate.id)
            Result.Error(e)
        }
    }

    override suspend fun deleteHeartRate(id: String): Result<Boolean> = withContext(ioDispatcher) {
        try {
            val rowsDeleted = heartRateDao.deleteById(id)
            
            if (rowsDeleted > 0) {
                // Remove from cache
                heartRateCache.remove(id)
                
                // Mark as deleted for sync
                markAsDeletedForSync(id, "heart_rate")
                
                Result.Success(true)
            } else {
                Result.Error(Exception("No rows deleted"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error deleting heart rate: %s", id)
            Result.Error(e)
        }
    }

    override suspend fun getHeartRatesForUser(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<List<HeartRate>> = withContext(ioDispatcher) {
        try {
            val entities = heartRateDao.getForUserInTimeRange(
                userId,
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay().minusNanos(1)
            )
            
            val heartRateList = entities.map { heartRateMapper.mapToDomain(it) }
            
            // Update cache for each item
            heartRateList.forEach { heartRate ->
                heartRateCache.put(heartRate.id, heartRate)
            }
            
            Result.Success(heartRateList)
        } catch (e: Exception) {
            Timber.e(e, "Error getting heart rates for user: %s", userId)
            Result.Error(e)
        }
    }

    override fun getHeartRatesForUserAsFlow(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<Result<List<HeartRate>>> {
        return heartRateDao.getForUserInTimeRangeAsFlow(
            userId,
            startDate.atStartOfDay(),
            endDate.plusDays(1).atStartOfDay().minusNanos(1)
        )
        .map { entities ->
            val heartRateList = entities.map { heartRateMapper.mapToDomain(it) }
            
            // Update cache for each item
            heartRateList.forEach { heartRate ->
                heartRateCache.put(heartRate.id, heartRate)
            }
            
            Result.Success(heartRateList)
        }
        .catch { e ->
            Timber.e(e, "Error getting heart rates flow for user: %s", userId)
            emit(Result.Error(e))
        }
        .flowOn(ioDispatcher)
    }

    override suspend fun getLatestHeartRate(userId: String): Result<HeartRate?> = withContext(ioDispatcher) {
        try {
            val entity = heartRateDao.getLatestForUser(userId)
            
            if (entity != null) {
                val heartRate = heartRateMapper.mapToDomain(entity)
                // Update cache
                heartRateCache.put(heartRate.id, heartRate)
                Result.Success(heartRate)
            } else {
                Result.Success(null)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting latest heart rate for user: %s", userId)
            Result.Error(e)
        }
    }

    override fun getLatestHeartRateAsFlow(userId: String): Flow<Result<HeartRate?>> {
        return heartRateDao.getLatestForUserAsFlow(userId)
            .map { entity ->
                if (entity != null) {
                    val heartRate = heartRateMapper.mapToDomain(entity)
                    // Update cache
                    heartRateCache.put(heartRate.id, heartRate)
                    Result.Success(heartRate)
                } else {
                    Result.Success(null)
                }
            }
            .catch { e ->
                Timber.e(e, "Error getting latest heart rate flow for user: %s", userId)
                emit(Result.Error(e))
            }
            .flowOn(ioDispatcher)
    }

    override suspend fun getHeartRateStats(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<HeartRateStats> = withContext(ioDispatcher) {
        try {
            val stats = heartRateDao.getHeartRateStats(
                userId,
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay().minusNanos(1)
            )
            
            if (stats != null) {
                val domainStats = HeartRateStats(
                    avgRestingHeartRate = stats.avgRestingHeartRate,
                    minHeartRate = stats.minHeartRate,
                    maxHeartRate = stats.maxHeartRate,
                    avgHeartRate = stats.avgHeartRate,
                    stdDevHeartRate = stats.stdDevHeartRate,
                    timeInZone1 = stats.timeInZone1,
                    timeInZone2 = stats.timeInZone2,
                    timeInZone3 = stats.timeInZone3,
                    timeInZone4 = stats.timeInZone4,
                    timeInZone5 = stats.timeInZone5,
                    hrvAvg = stats.hrvAvg,
                    readingsCount = stats.readingsCount
                )
                Result.Success(domainStats)
            } else {
                Result.Error(Exception("No heart rate stats available"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting heart rate stats for user: %s", userId)
            Result.Error(e)
        }
    }

    override fun getHeartRateStatsAsFlow(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<Result<HeartRateStats>> {
        return flow {
            emit(getHeartRateStats(userId, startDate, endDate))
        }
        .catch { e ->
            Timber.e(e, "Error getting heart rate stats flow for user: %s", userId)
            emit(Result.Error(e))
        }
        .flowOn(ioDispatcher)
    }

    override suspend fun getHeartRateZones(
        userId: String,
        age: Int
    ): Result<HeartRateZones> = withContext(ioDispatcher) {
        try {
            val maxHeartRate = 220 - age
            
            val zones = HeartRateZones(
                zone1Range = Pair(0.5 * maxHeartRate, 0.6 * maxHeartRate),
                zone2Range = Pair(0.6 * maxHeartRate, 0.7 * maxHeartRate),
                zone3Range = Pair(0.7 * maxHeartRate, 0.8 * maxHeartRate),
                zone4Range = Pair(0.8 * maxHeartRate, 0.9 * maxHeartRate),
                zone5Range = Pair(0.9 * maxHeartRate, maxHeartRate.toDouble())
            )
            
            Result.Success(zones)
        } catch (e: Exception) {
            Timber.e(e, "Error calculating heart rate zones for user: %s", userId)
            Result.Error(e)
        }
    }

    /**
     * Blood Pressure Operations
     */

    override suspend fun getBloodPressure(id: String): Result<BloodPressure> = withContext(ioDispatcher) {
        try {
            // Check cache first
            val cachedData = bloodPressureCache.get(id)
            if (cachedData != null) {
                return@withContext Result.Success(cachedData)
            }

            // Fetch from database
            val entity = bloodPressureDao.getById(id)
            if (entity != null) {
                val bloodPressure = bloodPressureMapper.mapToDomain(entity)
                // Update cache
                bloodPressureCache.put(id, bloodPressure)
                Result.Success(bloodPressure)
            } else {
                Result.Error(Exception("Blood pressure data not found"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting blood pressure: %s", id)
            Result.Error(e)
        }
    }

    override fun getBloodPressureAsFlow(id: String): Flow<Result<BloodPressure>> {
        return bloodPressureDao.getByIdAsFlow(id)
            .map { entity ->
                if (entity != null) {
                    val bloodPressure = bloodPressureMapper.mapToDomain(entity)
                    // Update cache
                    bloodPressureCache.put(id, bloodPressure)
                    Result.Success(bloodPressure)
                } else {
                    Result.Error(Exception("Blood pressure data not found"))
                }
            }
            .catch { e ->
                Timber.e(e, "Error getting blood pressure flow: %s", id)
                emit(Result.Error(e))
            }
            .flowOn(ioDispatcher)
    }

    override suspend fun saveBloodPressure(bloodPressure: BloodPressure): Result<String> = withContext(ioDispatcher) {
        try {
            val entity = bloodPressureMapper.mapToEntity(bloodPressure)
            val id = bloodPressureDao.insert(entity)
            
            // Update cache
            bloodPressureCache.put(entity.id, bloodPressure)
            
            // Queue for sync if needed
            if (bloodPressure.syncStatus != SyncStatus.SYNCED) {
                queueForSync(entity)
            }
            
            Result.Success(entity.id)
        } catch (e: Exception) {
            Timber.e(e, "Error saving blood pressure: %s", bloodPressure.id)
            Result.Error(e)
        }
    }

    override suspend fun updateBloodPressure(bloodPressure: BloodPressure): Result<Boolean> = withContext(ioDispatcher) {
        try {
            val entity = bloodPressureMapper.mapToEntity(bloodPressure)
            val rowsUpdated = bloodPressureDao.update(entity)
            
            if (rowsUpdated > 0) {
                // Update cache
                bloodPressureCache.put(entity.id, bloodPressure)
                
                // Queue for sync if needed
                if (bloodPressure.syncStatus != SyncStatus.SYNCED) {
                    queueForSync(entity)
                }
                
                Result.Success(true)
            } else {
                Result.Error(Exception("No rows updated"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error updating blood pressure: %s", bloodPressure.id)
            Result.Error(e)
        }
    }

    override suspend fun deleteBloodPressure(id: String): Result<Boolean> = withContext(ioDispatcher) {
        try {
            val rowsDeleted = bloodPressureDao.deleteById(id)
            
            if (rowsDeleted > 0) {
                // Remove from cache
                bloodPressureCache.remove(id)
                
                // Mark as deleted for sync
                markAsDeletedForSync(id, "blood_pressure")
                
                Result.Success(true)
            } else {
                Result.Error(Exception("No rows deleted"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error deleting blood pressure: %s", id)
            Result.Error(e)
        }
    }

    override suspend fun getBloodPressuresForUser(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<List<BloodPressure>> = withContext(ioDispatcher) {
        try {
            val entities = bloodPressureDao.getForUserInTimeRange(
                userId,
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay().minusNanos(1)
            )
            
            val bloodPressureList = entities.map { bloodPressureMapper.mapToDomain(it) }
            
            // Update cache for each item
            bloodPressureList.forEach { bloodPressure ->
                bloodPressureCache.put(bloodPressure.id, bloodPressure)
            }
            
            Result.Success(bloodPressureList)
        } catch (e: Exception) {
            Timber.e(e, "Error getting blood pressures for user: %s", userId)
            Result.Error(e)
        }
    }

    override fun getBloodPressuresForUserAsFlow(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<Result<List<BloodPressure>>> {
        return bloodPressureDao.getForUserInTimeRangeAsFlow(
            userId,
            startDate.atStartOfDay(),
            endDate.plusDays(1).atStartOfDay().minusNanos(1)
        )
        .map { entities ->
            val bloodPressureList = entities.map { bloodPressureMapper.mapToDomain(it) }
            
            // Update cache for each item
            bloodPressureList.forEach { bloodPressure ->
                bloodPressureCache.put(bloodPressure.id, bloodPressure)
            }
            
            Result.Success(bloodPressureList)
        }
        .catch { e ->
            Timber.e(e, "Error getting blood pressures flow for user: %s", userId)
            emit(Result.Error(e))
        }
        .flowOn(ioDispatcher)
    }

    override suspend fun getLatestBloodPressure(userId: String): Result<BloodPressure?> = withContext(ioDispatcher) {
        try {
            val entity = bloodPressureDao.getLatestForUser(userId)
            
            if (entity != null) {
                val bloodPressure = bloodPressureMapper.mapToDomain(entity)
                // Update cache
                bloodPressureCache.put(bloodPressure.id, bloodPressure)
                Result.Success(bloodPressure)
            } else {
                Result.Success(null)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting latest blood pressure for user: %s", userId)
            Result.Error(e)
        }
    }

    override fun getLatestBloodPressureAsFlow(userId: String): Flow<Result<BloodPressure?>> {
        return bloodPressureDao.getLatestForUserAsFlow(userId)
            .map { entity ->
                if (entity != null) {
                    val bloodPressure = bloodPressureMapper.mapToDomain(entity)
                    // Update cache
                    bloodPressureCache.put(bloodPressure.id, bloodPressure)
                    Result.Success(bloodPressure)
                } else {
                    Result.Success(null)
                }
            }
            .catch { e ->
                Timber.e(e, "Error getting latest blood pressure flow for user: %s", userId)
                emit(Result.Error(e))
            }
            .flowOn(ioDispatcher)
    }

    override suspend fun getBloodPressureStats(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<BloodPressureStats> = withContext(ioDispatcher) {
        try {
            val stats = bloodPressureDao.getBloodPressureStats(
                userId,
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay().minusNanos(1)
            )
            
            if (stats != null) {
                val domainStats = BloodPressureStats(
                    avgSystolic = stats.avgSystolic,
                    avgDiastolic = stats.avgDiastolic,
                    minSystolic = stats.minSystolic,
                    maxSystolic = stats.maxSystolic,
                    minDiastolic = stats.minDiastolic,
                    maxDiastolic = stats.maxDiastolic,
                    avgPulse = stats.avgPulse,
                    readingsCount = stats.readingsCount,
                    hypertensiveReadingsCount = stats.hypertensiveReadingsCount,
                    normalReadingsCount = stats.normalReadingsCount,
                    hypotensiveReadingsCount = stats.hypotensiveReadingsCount,
                    morningAvgSystolic = stats.morningAvgSystolic,
                    morningAvgDiastolic = stats.morningAvgDiastolic,
                    eveningAvgSystolic = stats.eveningAvgSystolic,
                    eveningAvgDiastolic = stats.eveningAvgDiastolic
                )
                Result.Success(domainStats)
            } else {
                Result.Error(Exception("No blood pressure stats available"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting blood pressure stats for user: %s", userId)
            Result.Error(e)
        }
    }

    override fun getBloodPressureStatsAsFlow(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<Result<BloodPressureStats>> {
        return flow {
            emit(getBloodPressureStats(userId, startDate, endDate))
        }
        .catch { e ->
            Timber.e(e, "Error getting blood pressure stats flow for user: %s", userId)
            emit(Result.Error(e))
        }
        .flowOn(ioDispatcher)
    }

    override suspend fun getBloodPressureClassification(
        systolic: Int,
        diastolic: Int
    ): Result<BloodPressureClassification> = withContext(ioDispatcher) {
        try {
            val classification = when {
                systolic >= 180 || diastolic >= 120 -> BloodPressureClassification.HYPERTENSIVE_CRISIS
                systolic >= 140 || diastolic >= 90 -> BloodPressureClassification.HYPERTENSION_STAGE_2
                systolic >= 130 || diastolic >= 80 -> BloodPressureClassification.HYPERTENSION_STAGE_1
                systolic >= 120 && diastolic < 80 -> BloodPressureClassification.ELEVATED
                systolic in 90..119 && diastolic in 60..79 -> BloodPressureClassification.NORMAL
                else -> BloodPressureClassification.HYPOTENSION
            }
            
            Result.Success(classification)
        } catch (e: Exception) {
            Timber.e(e, "Error classifying blood pressure: %d/%d", systolic, diastolic)
            Result.Error(e)
        }
    }

    /**
     * Sleep Operations
     */

    override suspend fun getSleep(id: String): Result<Sleep> = withContext(ioDispatcher) {
        try {
            // Check cache first
            val cachedData = sleepCache.get(id)
            if (cachedData != null) {
                return@withContext Result.Success(cachedData)
            }

            // Fetch from database
            val sleepWithStages = sleepDao.getSleepWithStages(id)
            if (sleepWithStages != null) {
                val sleep = sleepMapper.mapToDomain(sleepWithStages)
                // Update cache
                sleepCache.put(id, sleep)
                Result.Success(sleep)
            } else {
                Result.Error(Exception("Sleep data not found"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting sleep: %s", id)
            Result.Error(e)
        }
    }

    override fun getSleepAsFlow(id: String): Flow<Result<Sleep>> {
        return sleepDao.getSleepWithStagesAsFlow(id)
            .map { sleepWithStages ->
                if (sleepWithStages != null) {
                    val sleep = sleepMapper.mapToDomain(sleepWithStages)
                    // Update cache
                    sleepCache.put(id, sleep)
                    Result.Success(sleep)
                } else {
                    Result.Error(Exception("Sleep data not found"))
                }
            }
            .catch { e ->
                Timber.e(e, "Error getting sleep flow: %s", id)
                emit(Result.Error(e))
            }
            .flowOn(ioDispatcher)
    }

    override suspend fun saveSleep(sleep: Sleep): Result<String> = withContext(ioDispatcher) {
        try {
            val (sleepEntity, stageEntities) = sleepMapper.mapToEntity(sleep)
            
            // Insert sleep record
            val id = sleepDao.insert(sleepEntity)
            
            // Insert sleep stages
            sleepDao.insertAllStages(stageEntities)
            
            // Update cache
            sleepCache.put(sleepEntity.id, sleep)
            
            // Queue for sync if needed
            if (sleep.syncStatus != SyncStatus.SYNCED) {
                queueForSync(sleepEntity)
                stageEntities.forEach { queueForSync(it) }
            }
            
            Result.Success(sleepEntity.id)
        } catch (e: Exception) {
            Timber.e(e, "Error saving sleep: %s", sleep.id)
            Result.Error(e)
        }
    }

    override suspend fun updateSleep(sleep: Sleep): Result<Boolean> = withContext(ioDispatcher) {
        try {
            val (sleepEntity, stageEntities) = sleepMapper.mapToEntity(sleep)
            
            // Update sleep record
            val rowsUpdated = sleepDao.update(sleepEntity)
            
            if (rowsUpdated > 0) {
                // Delete existing stages and insert new ones
                sleepDao.deleteStagesForSleep(sleepEntity.id)
                sleepDao.insertAllStages(stageEntities)
                
                // Update cache
                sleepCache.put(sleepEntity.id, sleep)
                
                // Queue for sync if needed
                if (sleep.syncStatus != SyncStatus.SYNCED) {
                    queueForSync(sleepEntity)
                    stageEntities.forEach { queueForSync(it) }
                }
                
                Result.Success(true)
            } else {
                Result.Error(Exception("No rows updated"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error updating sleep: %s", sleep.id)
            Result.Error(e)
        }
    }

    override suspend fun deleteSleep(id: String): Result<Boolean> = withContext(ioDispatcher) {
        try {
            // Delete sleep stages first
            sleepDao.deleteStagesForSleep(id)
            
            // Delete sleep record
            val rowsDeleted = sleepDao.deleteById(id)
            
            if (rowsDeleted > 0) {
                // Remove from cache
                sleepCache.remove(id)
                
                // Mark as deleted for sync
                markAsDeletedForSync(id, "sleep")
                
                Result.Success(true)
            } else {
                Result.Error(Exception("No rows deleted"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error deleting sleep: %s", id)
            Result.Error(e)
        }
    }

    override suspend fun getSleepForUser(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<List<Sleep>> = withContext(ioDispatcher) {
        try {
            val sleepWithStagesList = sleepDao.getSleepWithStagesForUserInTimeRange(
                userId,
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay().minusNanos(1)
            )
            
            val sleepList = sleepWithStagesList.map { sleepMapper.mapToDomain(it) }
            
            // Update cache for each item
            sleepList.forEach { sleep ->
                sleepCache.put(sleep.id, sleep)
            }
            
            Result.Success(sleepList)
        } catch (e: Exception) {
            Timber.e(e, "Error getting sleep for user: %s", userId)
            Result.Error(e)
        }
    }

    override fun getSleepForUserAsFlow(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<Result<List<Sleep>>> {
        return sleepDao.getSleepWithStagesForUserInTimeRangeAsFlow(
            userId,
            startDate.atStartOfDay(),
            endDate.plusDays(1).atStartOfDay().minusNanos(1)
        )
        .map { sleepWithStagesList ->
            val sleepList = sleepWithStagesList.map { sleepMapper.mapToDomain(it) }
            
            // Update cache for each item
            sleepList.forEach { sleep ->
                sleepCache.put(sleep.id, sleep)
            }
            
            Result.Success(sleepList)
        }
        .catch { e ->
            Timber.e(e, "Error getting sleep flow for user: %s", userId)
            emit(Result.Error(e))
        }
        .flowOn(ioDispatcher)
    }

    override suspend fun getLatestSleep(userId: String): Result<Sleep?> = withContext(ioDispatcher) {
        try {
            val sleepWithStages = sleepDao.getLatestSleepWithStagesForUser(userId)
            
            if (sleepWithStages != null) {
                val sleep = sleepMapper.mapToDomain(sleepWithStages)
                // Update cache
                sleepCache.put(sleep.id, sleep)
                Result.Success(sleep)
            } else {
                Result.Success(null)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting latest sleep for user: %s", userId)
            Result.Error(e)
        }
    }

    override fun getLatestSleepAsFlow(userId: String): Flow<Result<Sleep?>> {
        return sleepDao.getLatestSleepWithStagesForUserAsFlow(userId)
            .map { sleepWithStages ->
                if (sleepWithStages != null) {
                    val sleep = sleepMapper.mapToDomain(sleepWithStages)
                    // Update cache
                    sleepCache.put(sleep.id, sleep)
                    Result.Success(sleep)
                } else {
                    Result.Success(null)
                }
            }
            .catch { e ->
                Timber.e(e, "Error getting latest sleep flow for user: %s", userId)
                emit(Result.Error(e))
            }
            .flowOn(ioDispatcher)
    }

    override suspend fun getSleepStats(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<SleepStats> = withContext(ioDispatcher) {
        try {
            val stats = sleepDao.getSleepStats(
                userId,
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay().minusNanos(1)
            )
            
            if (stats != null) {
                val domainStats = SleepStats(
                    avgDuration = stats.avgDuration,
                    avgSleepEfficiency = stats.avgSleepEfficiency,
                    avgDeepSleepPercentage = stats.avgDeepSleepPercentage,
                    avgRemSleepPercentage = stats.avgRemSleepPercentage,
                    avgLightSleepPercentage = stats.avgLightSleepPercentage,
                    avgAwakeTime = stats.avgAwakeTime,
                    avgSleepScore = stats.avgSleepScore,
                    avgSleepOnset = stats.avgSleepOnset,
                    avgWakeTime = stats.avgWakeTime,
                    totalSleepNights = stats.totalSleepNights
                )
                Result.Success(domainStats)
            } else {
                Result.Error(Exception("No sleep stats available"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting sleep stats for user: %s", userId)
            Result.Error(e)
        }
    }

    override fun getSleepStatsAsFlow(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<Result<SleepStats>> {
        return flow {
            emit(getSleepStats(userId, startDate, endDate))
        }
        .catch { e ->
            Timber.e(e, "Error getting sleep stats flow for user: %s", userId)
            emit(Result.Error(e))
        }
        .flowOn(ioDispatcher)
    }

    override suspend fun calculateSleepScore(sleep: Sleep): Result<Int> = withContext(ioDispatcher) {
        try {
            // Calculate sleep score based on various factors
            
            // Duration factor (optimal is 7-9 hours)
            val durationHours = ChronoUnit.MINUTES.between(sleep.startTime, sleep.endTime) / 60.0
            val durationScore = when {
                durationHours < 5 -> 10 // Very poor
                durationHours < 6 -> 15 // Poor
                durationHours < 7 -> 20 // Fair
                durationHours <= 9 -> 30 // Optimal
                durationHours <= 10 -> 20 // Fair
                else -> 10 // Very poor (too much sleep)
            }
            
            // Sleep efficiency factor (time asleep / time in bed)
            val awakeMinutes = sleep.stages.filter { it.stage == SleepStage.AWAKE }
                .sumOf { ChronoUnit.MINUTES.between(it.startTime, it.endTime) }
            val totalMinutes = ChronoUnit.MINUTES.between(sleep.startTime, sleep.endTime)
            val sleepEfficiency = (totalMinutes - awakeMinutes).toDouble() / totalMinutes
            val efficiencyScore = when {
                sleepEfficiency < 0.65 -> 5 // Very poor
                sleepEfficiency < 0.75 -> 10 // Poor
                sleepEfficiency < 0.85 -> 15 // Fair
                sleepEfficiency <= 0.95 -> 25 // Good to optimal
                else -> 20 // Very good
            }
            
            // Sleep composition factor (optimal percentages of sleep stages)
            val deepSleepMinutes = sleep.stages.filter { it.stage == SleepStage.DEEP }
                .sumOf { ChronoUnit.MINUTES.between(it.startTime, it.endTime) }
            val remSleepMinutes = sleep.stages.filter { it.stage == SleepStage.REM }
                .sumOf { ChronoUnit.MINUTES.between(it.startTime, it.endTime) }
            val lightSleepMinutes = sleep.stages.filter { it.stage == SleepStage.LIGHT }
                .sumOf { ChronoUnit.MINUTES.between(it.startTime, it.endTime) }
            
            val deepSleepPercentage = deepSleepMinutes.toDouble() / (totalMinutes - awakeMinutes)
            val remSleepPercentage = remSleepMinutes.toDouble() / (totalMinutes - awakeMinutes)
            
            // Deep sleep should be 15-25% of total sleep time
            val deepSleepScore = when {
                deepSleepPercentage < 0.10 -> 5 // Very poor
                deepSleepPercentage < 0.15 -> 10 // Poor
                deepSleepPercentage <= 0.25 -> 15 // Optimal
                deepSleepPercentage <= 0.30 -> 10 // Fair
                else -> 5 // Poor (too much deep sleep)
            }
            
            // REM sleep should be 20-25% of total sleep time
            val remSleepScore = when {
                remSleepPercentage < 0.15 -> 5 // Very poor
                remSleepPercentage < 0.20 -> 10 // Poor
                remSleepPercentage <= 0.25 -> 15 // Optimal
                remSleepPercentage <= 0.30 -> 10 // Fair
                else -> 5 // Poor (too much REM sleep)
            }
            
            // Sleep consistency factor (consistent sleep and wake times)
            val consistencyScore = 15 // Default, would need historical data to calculate properly
            
            // Calculate final score
            val totalScore = durationScore + efficiencyScore + deepSleepScore + remSleepScore + consistencyScore
            
            Result.Success(totalScore)
        } catch (e: Exception) {
            Timber.e(e, "Error calculating sleep score: %s", sleep.id)
            Result.Error(e)
        }
    }

    /**
     * Activity Operations
     */

    override suspend fun getActivity(id: String): Result<Activity> = withContext(ioDispatcher) {
        try {
            // Check cache first
            val cachedData = activityCache.get(id)
            if (cachedData != null) {
                return@withContext Result.Success(cachedData)
            }

            // Fetch from database
            val activityWithSessions = activityDao.getActivityWithSessions(id)
            if (activityWithSessions != null) {
                val activity = activityMapper.mapToDomain(activityWithSessions)
                // Update cache
                activityCache.put(id, activity)
                Result.Success(activity)
            } else {
                Result.Error(Exception("Activity data not found"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting activity: %s", id)
            Result.Error(e)
        }
    }

    override fun getActivityAsFlow(id: String): Flow<Result<Activity>> {
        return activityDao.getActivityWithSessionsAsFlow(id)
            .map { activityWithSessions ->
                if (activityWithSessions != null) {
                    val activity = activityMapper.mapToDomain(activityWithSessions)
                    // Update cache
                    activityCache.put(id, activity)
                    Result.Success(activity)
                } else {
                    Result.Error(Exception("Activity data not found"))
                }
            }
            .catch { e ->
                Timber.e(e, "Error getting activity flow: %s", id)
                emit(Result.Error(e))
            }
            .flowOn(ioDispatcher)
    }

    override suspend fun saveActivity(activity: Activity): Result<String> = withContext(ioDispatcher) {
        try {
            val (activityEntity, sessionEntities) = activityMapper.mapToEntity(activity)
            
            // Insert activity record
            val id = activityDao.insert(activityEntity)
            
            // Insert activity sessions
            activityDao.insertAllSessions(sessionEntities)
            
            // Update cache
            activityCache.put(activityEntity.id, activity)
            
            // Queue for sync if needed
            if (activity.syncStatus != SyncStatus.SYNCED) {
                queueForSync(activityEntity)
                sessionEntities.forEach { queueForSync(it) }
            }
            
            Result.Success(activityEntity.id)
        } catch (e: Exception) {
            Timber.e(e, "Error saving activity: %s", activity.id)
            Result.Error(e)
        }
    }

    override suspend fun updateActivity(activity: Activity): Result<Boolean> = withContext(ioDispatcher) {
        try {
            val (activityEntity, sessionEntities) = activityMapper.mapToEntity(activity)
            
            // Update activity record
            val rowsUpdated = activityDao.update(activityEntity)
            
            if (rowsUpdated > 0) {
                // Delete existing sessions and insert new ones
                activityDao.deleteSessionsForActivity(activityEntity.id)
                activityDao.insertAllSessions(sessionEntities)
                
                // Update cache
                activityCache.put(activityEntity.id, activity)
                
                // Queue for sync if needed
                if (activity.syncStatus != SyncStatus.SYNCED) {
                    queueForSync(activityEntity)
                    sessionEntities.forEach { queueForSync(it) }
                }
                
                Result.Success(true)
            } else {
                Result.Error(Exception("No rows updated"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error updating activity: %s", activity.id)
            Result.Error(e)
        }
    }

    override suspend fun deleteActivity(id: String): Result<Boolean> = withContext(ioDispatcher) {
        try {
            // Delete activity sessions first
            activityDao.deleteSessionsForActivity(id)
            
            // Delete activity record
            val rowsDeleted = activityDao.deleteById(id)
            
            if (rowsDeleted > 0) {
                // Remove from cache
                activityCache.remove(id)
                
                // Mark as deleted for sync
                markAsDeletedForSync(id, "activity")
                
                Result.Success(true)
            } else {
                Result.Error(Exception("No rows deleted"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error deleting activity: %s", id)
            Result.Error(e)
        }
    }

    override suspend fun getActivitiesForUser(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<List<Activity>> = withContext(ioDispatcher) {
        try {
            val activityWithSessionsList = activityDao.getActivitiesWithSessionsForUserInTimeRange(
                userId,
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay().minusNanos(1)
            )
            
            val activityList = activityWithSessionsList.map { activityMapper.mapToDomain(it) }
            
            // Update cache for each item
            activityList.forEach { activity ->
                activityCache.put(activity.id, activity)
            }
            
            Result.Success(activityList)
        } catch (e: Exception) {
            Timber.e(e, "Error getting activities for user: %s", userId)
            Result.Error(e)
        }
    }

    override fun getActivitiesForUserAsFlow(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<Result<List<Activity>>> {
        return activityDao.getActivitiesWithSessionsForUserInTimeRangeAsFlow(
            userId,
            startDate.atStartOfDay(),
            endDate.plusDays(1).atStartOfDay().minusNanos(1)
        )
        .map { activityWithSessionsList ->
            val activityList = activityWithSessionsList.map { activityMapper.mapToDomain(it) }
            
            // Update cache for each item
            activityList.forEach { activity ->
                activityCache.put(activity.id, activity)
            }
            
            Result.Success(activityList)
        }
        .catch { e ->
            Timber.e(e, "Error getting activities flow for user: %s", userId)
            emit(Result.Error(e))
        }
        .flowOn(ioDispatcher)
    }

    override suspend fun getActivitiesByType(
        userId: String,
        activityType: ActivityType,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<List<Activity>> = withContext(ioDispatcher) {
        try {
            val activityWithSessionsList = activityDao.getActivitiesWithSessionsByType(
                userId,
                activityType.name,
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay().minusNanos(1)
            )
            
            val activityList = activityWithSessionsList.map { activityMapper.mapToDomain(it) }
            
            // Update cache for each item
            activityList.forEach { activity ->
                activityCache.put(activity.id, activity)
            }
            
            Result.Success(activityList)
        } catch (e: Exception) {
            Timber.e(e, "Error getting activities by type: %s", activityType)
            Result.Error(e)
        }
    }

    override suspend fun getActivityStats(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<ActivityStats> = withContext(ioDispatcher) {
        try {
            val stats = activityDao.getActivityStats(
                userId,
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay().minusNanos(1)
            )
            
            if (stats != null) {
                val domainStats = ActivityStats(
                    totalSteps = stats.totalSteps,
                    totalDistance = stats.totalDistance,
                    totalCalories = stats.totalCalories,
                    totalActiveMinutes = stats.totalActiveMinutes,
                    avgDailySteps = stats.avgDailySteps,
                    avgDailyActiveMinutes = stats.avgDailyActiveMinutes,
                    highIntensityMinutes = stats.highIntensityMinutes,
                    moderateIntensityMinutes = stats.moderateIntensityMinutes,
                    lowIntensityMinutes = stats.lowIntensityMinutes,
                    sedentaryMinutes = stats.sedentaryMinutes,
                    daysWithActivity = stats.daysWithActivity
                )
                Result.Success(domainStats)
            } else {
                Result.Error(Exception("No activity stats available"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting activity stats for user: %s", userId)
            Result.Error(e)
        }
    }

    override fun getActivityStatsAsFlow(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<Result<ActivityStats>> {
        return flow {
            emit(getActivityStats(userId, startDate, endDate))
        }
        .catch { e ->
            Timber.e(e, "Error getting activity stats flow for user: %s", userId)
            emit(Result.Error(e))
        }
        .flowOn(ioDispatcher)
    }

    override suspend fun getActivityStatsByType(
        userId: String,
        activityType: ActivityType,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<ActivityTypeStats> = withContext(ioDispatcher) {
        try {
            val stats = activityDao.getActivityStatsByType(
                userId,
                activityType.name,
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay().minusNanos(1)
            )
            
            if (stats != null) {
                val domainStats = ActivityTypeStats(
                    activityType = activityType,
                    totalSessions = stats.totalSessions,
                    totalDuration = stats.totalDuration,
                    totalDistance = stats.totalDistance,
                    totalCalories = stats.totalCalories,
                    avgDuration = stats.avgDuration,
                    avgDistance = stats.avgDistance,
                    avgCalories = stats.avgCalories,
                    avgHeartRate = stats.avgHeartRate,
                    maxHeartRate = stats.maxHeartRate,
                    avgIntensity = Intensity.valueOf(stats.avgIntensity)
                )
                Result.Success(domainStats)
            } else {
                Result.Error(Exception("No activity type stats available"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting activity stats by type: %s", activityType)
            Result.Error(e)
        }
    }

    /**
     * Device Operations
     */

    override suspend fun getDevice(id: String): Result<Device> = withContext(ioDispatcher) {
        try {
            // Check cache first
            val cachedData = deviceCache.get(id)
            if (cachedData != null) {
                return@withContext Result.Success(cachedData)
            }

            // Fetch from database
            val deviceWithDetails = deviceDao.getDeviceWithDetails(id)
            if (deviceWithDetails != null) {
                val device = deviceMapper.mapToDomain(deviceWithDetails)
                // Update cache
                deviceCache.put(id, device)
                Result.Success(device)
            } else {
                Result.Error(Exception("Device not found"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting device: %s", id)
            Result.Error(e)
        }
    }

    override fun getDeviceAsFlow(id: String): Flow<Result<Device>> {
        return deviceDao.getDeviceWithDetailsAsFlow(id)
            .map { deviceWithDetails ->
                if (deviceWithDetails != null) {
                    val device = deviceMapper.mapToDomain(deviceWithDetails)
                    // Update cache
                    deviceCache.put(id, device)
                    Result.Success(device)
                } else {
                    Result.Error(Exception("Device not found"))
                }
            }
            .catch { e ->
                Timber.e(e, "Error getting device flow: %s", id)
                emit(Result.Error(e))
            }
            .flowOn(ioDispatcher)
    }

    override suspend fun saveDevice(device: Device): Result<String> = withContext(ioDispatcher) {
        try {
            val (deviceEntity, settingEntities, syncHistoryEntities) = deviceMapper.mapToEntity(device)
            
            // Insert device record
            val id = deviceDao.insert(deviceEntity)
            
            // Insert device settings
            deviceDao.insertAllSettings(settingEntities)
            
            // Insert sync history
            deviceDao.insertAllSyncHistory(syncHistoryEntities)
            
            // Update cache
            deviceCache.put(deviceEntity.id, device)
            
            Result.Success(deviceEntity.id)
        } catch (e: Exception) {
            Timber.e(e, "Error saving device: %s", device.id)
            Result.Error(e)
        }
    }

    override suspend fun updateDevice(device: Device): Result<Boolean> = withContext(ioDispatcher) {
        try {
            val (deviceEntity, settingEntities, syncHistoryEntities) = deviceMapper.mapToEntity(device)
            
            // Update device record
            val rowsUpdated = deviceDao.update(deviceEntity)
            
            if (rowsUpdated > 0) {
                // Update device settings
                deviceDao.deleteSettingsForDevice(deviceEntity.id)
                deviceDao.insertAllSettings(settingEntities)
                
                // Insert new sync history (don't delete old history)
                deviceDao.insertAllSyncHistory(syncHistoryEntities)
                
                // Update cache
                deviceCache.put(deviceEntity.id, device)
                
                Result.Success(true)
            } else {
                Result.Error(Exception("No rows updated"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error updating device: %s", device.id)
            Result.Error(e)
        }
    }

    override suspend fun deleteDevice(id: String): Result<Boolean> = withContext(ioDispatcher) {
        try {
            // Delete device settings first
            deviceDao.deleteSettingsForDevice(id)
            
            // Delete sync history
            deviceDao.deleteSyncHistoryForDevice(id)
            
            // Delete device record
            val rowsDeleted = deviceDao.deleteById(id)
            
            if (rowsDeleted > 0) {
                // Remove from cache
                deviceCache.remove(id)
                
                Result.Success(true)
            } else {
                Result.Error(Exception("No rows deleted"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error deleting device: %s", id)
            Result.Error(e)
        }
    }

    override suspend fun getDevicesForUser(userId: String): Result<List<Device>> = withContext(ioDispatcher) {
        try {
            val deviceWithDetailsList = deviceDao.getDevicesWithDetailsForUser(userId)
            
            val deviceList = deviceWithDetailsList.map { deviceMapper.mapToDomain(it) }
            
            // Update cache for each item
            deviceList.forEach { device ->
                deviceCache.put(device.id, device)
            }
            
            Result.Success(deviceList)
        } catch (e: Exception) {
            Timber.e(e, "Error getting devices for user: %s", userId)
            Result.Error(e)
        }
    }

    override fun getDevicesForUserAsFlow(userId: String): Flow<Result<List<Device>>> {
        return deviceDao.getDevicesWithDetailsForUserAsFlow(userId)
            .map { deviceWithDetailsList ->
                val deviceList = deviceWithDetailsList.map { deviceMapper.mapToDomain(it) }
                
                // Update cache for each item
                deviceList.forEach { device ->
                    deviceCache.put(device.id, device)
                }
                
                Result.Success(deviceList)
            }
            .catch { e ->
                Timber.e(e, "Error getting devices flow for user: %s", userId)
                emit(Result.Error(e))
            }
            .flowOn(ioDispatcher)
    }

    override suspend fun getConnectedDevices(userId: String): Result<List<Device>> = withContext(ioDispatcher) {
        try {
            val deviceWithDetailsList = deviceDao.getConnectedDevicesWithDetailsForUser(userId)
            
            val deviceList = deviceWithDetailsList.map { deviceMapper.mapToDomain(it) }
            
            // Update cache for each item
            deviceList.forEach { device ->
                deviceCache.put(device.id, device)
            }
            
            Result.Success(deviceList)
        } catch (e: Exception) {
            Timber.e(e, "Error getting connected devices for user: %s", userId)
            Result.Error(e)
        }
    }

    override suspend fun updateDeviceConnectionStatus(
        deviceId: String,
        isConnected: Boolean
    ): Result<Boolean> = withContext(ioDispatcher) {
        try {
            val rowsUpdated = deviceDao.updateConnectionStatus(
                deviceId,
                isConnected,
                if (isConnected) LocalDateTime.now() else null
            )
            
            if (rowsUpdated > 0) {
                // Update cache if device is in cache
                val cachedDevice = deviceCache.get(deviceId)
                if (cachedDevice != null) {
                    val updatedDevice = cachedDevice.copy(
                        isConnected = isConnected,
                        lastConnectedAt = if (isConnected) LocalDateTime.now() else cachedDevice.lastConnectedAt
                    )
                    deviceCache.put(deviceId, updatedDevice)
                }
                
                Result.Success(true)
            } else {
                Result.Error(Exception("No rows updated"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error updating device connection status: %s", deviceId)
            Result.Error(e)
        }
    }

    override suspend fun updateDeviceBatteryLevel(
        deviceId: String,
        batteryLevel: Int
    ): Result<Boolean> = withContext(ioDispatcher) {
        try {
            val rowsUpdated = deviceDao.updateBatteryLevel(
                deviceId,
                batteryLevel,
                LocalDateTime.now()
            )
            
            if (rowsUpdated > 0) {
                // Update cache if device is in cache
                val cachedDevice = deviceCache.get(deviceId)
                if (cachedDevice != null) {
                    val updatedDevice = cachedDevice.copy(
                        batteryLevel = batteryLevel,
                        batteryLastUpdatedAt = LocalDateTime.now()
                    )
                    deviceCache.put(deviceId, updatedDevice)
                }
                
                Result.Success(true)
            } else {
                Result.Error(Exception("No rows updated"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error updating device battery level: %s", deviceId)
            Result.Error(e)
        }
    }

    override suspend fun recordDeviceSync(
        deviceId: String,
        syncType: String,
        syncStatus: SyncStatus,
        itemsCount: Int,
        errorMessage: String?
    ): Result<String> = withContext(ioDispatcher) {
        try {
            val syncHistory = DeviceSyncHistoryEntity(
                id = 0, // Auto-generated
                deviceId = deviceId,
                syncType = syncType,
                syncStatus = syncStatus.name,
                startedAt = LocalDateTime.now(),
                completedAt = if (syncStatus != SyncStatus.IN_PROGRESS) LocalDateTime.now() else null,
                itemsCount = itemsCount,
                errorMessage = errorMessage,
                createdAt = LocalDateTime.now(),
                modifiedAt = LocalDateTime.now()
            )
            
            val id = deviceDao.insertSyncHistory(syncHistory)
            
            // Update cache if device is in cache
            val cachedDevice = deviceCache.get(deviceId)
            if (cachedDevice != null) {
                val updatedDevice = cachedDevice.copy(
                    lastSyncAt = LocalDateTime.now(),
                    syncStatus = syncStatus
                )
                deviceCache.put(deviceId, updatedDevice)
            }
            
            Result.Success(id.toString())
        } catch (e: Exception) {
            Timber.e(e, "Error recording device sync: %s", deviceId)
            Result.Error(e)
        }
    }

    /**
     * Health Goal Operations
     */

    override suspend fun getHealthGoal(id: String): Result<HealthGoal> = withContext(ioDispatcher) {
        try {
            // Check cache first
            val cachedData = goalCache.get(id)
            if (cachedData != null) {
                return@withContext Result.Success(cachedData)
            }

            // Fetch from database
            val entity = healthGoalDao.getById(id)
            if (entity != null) {
                val healthGoal = goalMapper.mapToDomain(entity)
                // Update cache
                goalCache.put(id, healthGoal)
                Result.Success(healthGoal)
            } else {
                Result.Error(Exception("Health goal not found"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting health goal: %s", id)
            Result.Error(e)
        }
    }

    override fun getHealthGoalAsFlow(id: String): Flow<Result<HealthGoal>> {
        return healthGoalDao.getByIdAsFlow(id)
            .map { entity ->
                if (entity != null) {
                    val healthGoal = goalMapper.mapToDomain(entity)
                    // Update cache
                    goalCache.put(id, healthGoal)
                    Result.Success(healthGoal)
                } else {
                    Result.Error(Exception("Health goal not found"))
                }
            }
            .catch { e ->
                Timber.e(e, "Error getting health goal flow: %s", id)
                emit(Result.Error(e))
            }
            .flowOn(ioDispatcher)
    }

    override suspend fun saveHealthGoal(healthGoal: HealthGoal): Result<String> = withContext(ioDispatcher) {
        try {
            val entity = goalMapper.mapToEntity(healthGoal)
            val id = healthGoalDao.insert(entity)
            
            // Update cache
            goalCache.put(entity.id, healthGoal)
            
            Result.Success(entity.id)
        } catch (e: Exception) {
            Timber.e(e, "Error saving