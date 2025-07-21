package com.sensacare.app.data.repository

import android.database.sqlite.SQLiteException
import com.sensacare.app.data.local.dao.*
import com.sensacare.app.data.local.entity.*
import com.sensacare.app.domain.model.*
import com.sensacare.app.domain.repository.*
import com.sensacare.app.domain.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of BloodOxygenRepository
 */
@Singleton
class BloodOxygenRepositoryImpl @Inject constructor(
    private val bloodOxygenDao: BloodOxygenDao
) : BloodOxygenRepository {

    override suspend fun saveBloodOxygen(bloodOxygen: BloodOxygen): Result<Unit> {
        return try {
            val entity = bloodOxygen.toEntity()
            bloodOxygenDao.insert(entity)
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error saving blood oxygen data")
            Result.Error(e)
        }
    }

    override suspend fun saveMultipleBloodOxygen(bloodOxygens: List<BloodOxygen>): Result<Unit> {
        return try {
            val entities = bloodOxygens.map { it.toEntity() }
            bloodOxygenDao.insertAll(entities)
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error saving multiple blood oxygen data")
            Result.Error(e)
        }
    }

    override suspend fun getBloodOxygen(id: String): Result<BloodOxygen> {
        return try {
            val entity = bloodOxygenDao.getById(id)
            if (entity != null) {
                Result.Success(entity.toDomainModel())
            } else {
                Result.Error(Exception("Blood oxygen data not found"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting blood oxygen data")
            Result.Error(e)
        }
    }

    override suspend fun deleteBloodOxygen(id: String): Result<Unit> {
        return try {
            bloodOxygenDao.deleteById(id)
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error deleting blood oxygen data")
            Result.Error(e)
        }
    }

    override fun getBloodOxygenForUser(userId: String): Flow<List<BloodOxygen>> {
        return bloodOxygenDao.getForUser(userId)
            .map { entities -> entities.map { it.toDomainModel() } }
            .catch { e ->
                Timber.e(e, "Error getting blood oxygen data for user")
                emit(emptyList())
            }
    }

    override fun getBloodOxygenForUserInTimeRange(
        userId: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): Flow<List<BloodOxygen>> {
        return bloodOxygenDao.getForUserInTimeRange(userId, startTime, endTime)
            .map { entities -> entities.map { it.toDomainModel() } }
            .catch { e ->
                Timber.e(e, "Error getting blood oxygen data for user in time range")
                emit(emptyList())
            }
    }

    override fun getLatestBloodOxygenForUser(userId: String): Flow<BloodOxygen?> {
        return bloodOxygenDao.getLatestForUser(userId)
            .map { entity -> entity?.toDomainModel() }
            .catch { e ->
                Timber.e(e, "Error getting latest blood oxygen data for user")
                emit(null)
            }
    }

    override fun getBloodOxygenStats(
        userId: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): Flow<BloodOxygenStats?> {
        return bloodOxygenDao.getStatsForUserInTimeRange(userId, startTime, endTime)
            .map { stats ->
                if (stats != null) {
                    BloodOxygenStats(
                        averageValue = stats.averageValue,
                        minValue = stats.minValue,
                        maxValue = stats.maxValue,
                        count = stats.count,
                        timeBelow90 = stats.timeBelow90,
                        timeBelow95 = stats.timeBelow95,
                        hypoxicEvents = stats.hypoxicEvents
                    )
                } else null
            }
            .catch { e ->
                Timber.e(e, "Error getting blood oxygen stats for user")
                emit(null)
            }
    }

    override fun getBloodOxygenWithClassification(
        userId: String,
        classification: BloodOxygenClassification
    ): Flow<List<BloodOxygen>> {
        return bloodOxygenDao.getWithClassification(userId, classification)
            .map { entities -> entities.map { it.toDomainModel() } }
            .catch { e ->
                Timber.e(e, "Error getting blood oxygen data with classification")
                emit(emptyList())
            }
    }

    private fun BloodOxygen.toEntity(): BloodOxygenEntity {
        return BloodOxygenEntity(
            id = id,
            userId = userId,
            timestamp = timestamp,
            value = value,
            confidence = confidence,
            pulseRate = pulseRate,
            classification = classification,
            deviceId = deviceId,
            deviceType = deviceType,
            activityState = measurementContext.activityState,
            bodyPosition = measurementContext.bodyPosition,
            manuallyEntered = measurementContext.manuallyEntered,
            validationStatus = validationStatus,
            notes = notes
        )
    }

    private fun BloodOxygenEntity.toDomainModel(): BloodOxygen {
        val context = MeasurementContext(
            activityState = activityState,
            bodyPosition = bodyPosition,
            manuallyEntered = manuallyEntered
        )

        return BloodOxygen(
            id = id,
            userId = userId,
            timestamp = timestamp,
            value = value,
            confidence = confidence,
            pulseRate = pulseRate,
            classification = classification,
            deviceId = deviceId,
            deviceType = deviceType,
            measurementContext = context,
            validationStatus = validationStatus,
            notes = notes
        )
    }
}

/**
 * Implementation of BodyTemperatureRepository
 */
@Singleton
class BodyTemperatureRepositoryImpl @Inject constructor(
    private val bodyTemperatureDao: BodyTemperatureDao
) : BodyTemperatureRepository {

    override suspend fun saveBodyTemperature(bodyTemperature: BodyTemperature): Result<Unit> {
        return try {
            val entity = bodyTemperature.toEntity()
            bodyTemperatureDao.insert(entity)
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error saving body temperature data")
            Result.Error(e)
        }
    }

    override suspend fun saveMultipleBodyTemperature(bodyTemperatures: List<BodyTemperature>): Result<Unit> {
        return try {
            val entities = bodyTemperatures.map { it.toEntity() }
            bodyTemperatureDao.insertAll(entities)
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error saving multiple body temperature data")
            Result.Error(e)
        }
    }

    override suspend fun getBodyTemperature(id: String): Result<BodyTemperature> {
        return try {
            val entity = bodyTemperatureDao.getById(id)
            if (entity != null) {
                Result.Success(entity.toDomainModel())
            } else {
                Result.Error(Exception("Body temperature data not found"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting body temperature data")
            Result.Error(e)
        }
    }

    override suspend fun deleteBodyTemperature(id: String): Result<Unit> {
        return try {
            bodyTemperatureDao.deleteById(id)
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error deleting body temperature data")
            Result.Error(e)
        }
    }

    override fun getBodyTemperatureForUser(userId: String): Flow<List<BodyTemperature>> {
        return bodyTemperatureDao.getForUser(userId)
            .map { entities -> entities.map { it.toDomainModel() } }
            .catch { e ->
                Timber.e(e, "Error getting body temperature data for user")
                emit(emptyList())
            }
    }

    override fun getBodyTemperatureForUserInTimeRange(
        userId: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): Flow<List<BodyTemperature>> {
        return bodyTemperatureDao.getForUserInTimeRange(userId, startTime, endTime)
            .map { entities -> entities.map { it.toDomainModel() } }
            .catch { e ->
                Timber.e(e, "Error getting body temperature data for user in time range")
                emit(emptyList())
            }
    }

    override fun getLatestBodyTemperatureForUser(userId: String): Flow<BodyTemperature?> {
        return bodyTemperatureDao.getLatestForUser(userId)
            .map { entity -> entity?.toDomainModel() }
            .catch { e ->
                Timber.e(e, "Error getting latest body temperature data for user")
                emit(null)
            }
    }

    override fun getBodyTemperatureStats(
        userId: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): Flow<BodyTemperatureStats?> {
        return bodyTemperatureDao.getStatsForUserInTimeRange(userId, startTime, endTime)
            .map { stats ->
                if (stats != null) {
                    BodyTemperatureStats(
                        averageValue = stats.averageValue,
                        minValue = stats.minValue,
                        maxValue = stats.maxValue,
                        count = stats.count,
                        feverCount = stats.feverCount,
                        maxFeverValue = stats.maxFeverValue
                    )
                } else null
            }
            .catch { e ->
                Timber.e(e, "Error getting body temperature stats for user")
                emit(null)
            }
    }

    override fun getBodyTemperatureWithFeverStatus(
        userId: String,
        feverStatus: FeverStatus
    ): Flow<List<BodyTemperature>> {
        return bodyTemperatureDao.getWithFeverStatus(userId, feverStatus)
            .map { entities -> entities.map { it.toDomainModel() } }
            .catch { e ->
                Timber.e(e, "Error getting body temperature data with fever status")
                emit(emptyList())
            }
    }

    override fun getBodyTemperatureBySite(
        userId: String,
        measurementSite: TemperatureMeasurementSite
    ): Flow<List<BodyTemperature>> {
        return bodyTemperatureDao.getBySite(userId, measurementSite)
            .map { entities -> entities.map { it.toDomainModel() } }
            .catch { e ->
                Timber.e(e, "Error getting body temperature data by site")
                emit(emptyList())
            }
    }

    private fun BodyTemperature.toEntity(): BodyTemperatureEntity {
        return BodyTemperatureEntity(
            id = id,
            userId = userId,
            timestamp = timestamp,
            value = value,
            measurementSite = measurementSite,
            feverStatus = feverStatus,
            deviceId = deviceId,
            deviceType = deviceType,
            activityState = measurementContext.activityState,
            bodyPosition = measurementContext.bodyPosition,
            manuallyEntered = measurementContext.manuallyEntered,
            validationStatus = validationStatus,
            notes = notes
        )
    }

    private fun BodyTemperatureEntity.toDomainModel(): BodyTemperature {
        val context = MeasurementContext(
            activityState = activityState,
            bodyPosition = bodyPosition,
            manuallyEntered = manuallyEntered
        )

        return BodyTemperature(
            id = id,
            userId = userId,
            timestamp = timestamp,
            value = value,
            measurementSite = measurementSite,
            feverStatus = feverStatus,
            deviceId = deviceId,
            deviceType = deviceType,
            measurementContext = context,
            validationStatus = validationStatus,
            notes = notes
        )
    }
}

/**
 * Implementation of StressLevelRepository
 */
@Singleton
class StressLevelRepositoryImpl @Inject constructor(
    private val stressLevelDao: StressLevelDao
) : StressLevelRepository {

    override suspend fun saveStressLevel(stressLevel: StressLevel): Result<Unit> {
        return try {
            val entity = stressLevel.toEntity()
            stressLevelDao.insert(entity)
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error saving stress level data")
            Result.Error(e)
        }
    }

    override suspend fun saveMultipleStressLevel(stressLevels: List<StressLevel>): Result<Unit> {
        return try {
            val entities = stressLevels.map { it.toEntity() }
            stressLevelDao.insertAll(entities)
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error saving multiple stress level data")
            Result.Error(e)
        }
    }

    override suspend fun getStressLevel(id: String): Result<StressLevel> {
        return try {
            val entity = stressLevelDao.getById(id)
            if (entity != null) {
                Result.Success(entity.toDomainModel())
            } else {
                Result.Error(Exception("Stress level data not found"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting stress level data")
            Result.Error(e)
        }
    }

    override suspend fun deleteStressLevel(id: String): Result<Unit> {
        return try {
            stressLevelDao.deleteById(id)
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error deleting stress level data")
            Result.Error(e)
        }
    }

    override fun getStressLevelForUser(userId: String): Flow<List<StressLevel>> {
        return stressLevelDao.getForUser(userId)
            .map { entities -> entities.map { it.toDomainModel() } }
            .catch { e ->
                Timber.e(e, "Error getting stress level data for user")
                emit(emptyList())
            }
    }

    override fun getStressLevelForUserInTimeRange(
        userId: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): Flow<List<StressLevel>> {
        return stressLevelDao.getForUserInTimeRange(userId, startTime, endTime)
            .map { entities -> entities.map { it.toDomainModel() } }
            .catch { e ->
                Timber.e(e, "Error getting stress level data for user in time range")
                emit(emptyList())
            }
    }

    override fun getLatestStressLevelForUser(userId: String): Flow<StressLevel?> {
        return stressLevelDao.getLatestForUser(userId)
            .map { entity -> entity?.toDomainModel() }
            .catch { e ->
                Timber.e(e, "Error getting latest stress level data for user")
                emit(null)
            }
    }

    override fun getStressLevelStats(
        userId: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): Flow<StressLevelStats?> {
        return stressLevelDao.getStatsForUserInTimeRange(userId, startTime, endTime)
            .map { stats ->
                if (stats != null) {
                    StressLevelStats(
                        averageValue = stats.averageValue,
                        minValue = stats.minValue,
                        maxValue = stats.maxValue,
                        count = stats.count,
                        highStressCount = stats.highStressCount,
                        lowStressCount = stats.lowStressCount,
                        mediumStressCount = stats.mediumStressCount
                    )
                } else null
            }
            .catch { e ->
                Timber.e(e, "Error getting stress level stats for user")
                emit(null)
            }
    }

    override fun getStressLevelWithClassification(
        userId: String,
        classification: StressClassification
    ): Flow<List<StressLevel>> {
        return stressLevelDao.getWithClassification(userId, classification)
            .map { entities -> entities.map { it.toDomainModel() } }
            .catch { e ->
                Timber.e(e, "Error getting stress level data with classification")
                emit(emptyList())
            }
    }

    override fun getStressLevelByHrvRange(
        userId: String,
        minHrv: Double,
        maxHrv: Double
    ): Flow<List<StressLevel>> {
        return stressLevelDao.getByHrvRange(userId, minHrv, maxHrv)
            .map { entities -> entities.map { it.toDomainModel() } }
            .catch { e ->
                Timber.e(e, "Error getting stress level data by HRV range")
                emit(emptyList())
            }
    }

    private fun StressLevel.toEntity(): StressLevelEntity {
        return StressLevelEntity(
            id = id,
            userId = userId,
            timestamp = timestamp,
            value = value,
            classification = classification,
            hrvValue = hrvValue,
            confidenceScore = confidenceScore,
            deviceId = deviceId,
            deviceType = deviceType,
            activityState = measurementContext.activityState,
            bodyPosition = measurementContext.bodyPosition,
            manuallyEntered = measurementContext.manuallyEntered,
            validationStatus = validationStatus,
            notes = notes
        )
    }

    private fun StressLevelEntity.toDomainModel(): StressLevel {
        val context = MeasurementContext(
            activityState = activityState,
            bodyPosition = bodyPosition,
            manuallyEntered = manuallyEntered
        )

        return StressLevel(
            id = id,
            userId = userId,
            timestamp = timestamp,
            value = value,
            classification = classification,
            hrvValue = hrvValue,
            confidenceScore = confidenceScore,
            deviceId = deviceId,
            deviceType = deviceType,
            measurementContext = context,
            validationStatus = validationStatus,
            notes = notes
        )
    }
}

/**
 * Implementation of EcgRepository
 */
@Singleton
class EcgRepositoryImpl @Inject constructor(
    private val ecgDao: EcgDao,
    private val ecgWaveformDao: EcgWaveformDao
) : EcgRepository {

    override suspend fun saveEcg(ecg: Ecg): Result<Unit> {
        return try {
            // Begin transaction
            ecgDao.beginTransaction()
            
            try {
                // Save ECG entity
                val ecgEntity = ecg.toEntity()
                ecgDao.insert(ecgEntity)
                
                // Save waveform data
                val waveformEntity = EcgWaveformEntity(
                    ecgId = ecg.id,
                    waveformData = ecg.waveformData,
                    samplingRate = ecg.samplingRate
                )
                ecgWaveformDao.insert(waveformEntity)
                
                // Set transaction successful
                ecgDao.setTransactionSuccessful()
                
                Result.Success(Unit)
            } finally {
                // End transaction
                ecgDao.endTransaction()
            }
        } catch (e: Exception) {
            Timber.e(e, "Error saving ECG data")
            Result.Error(e)
        }
    }

    override suspend fun getEcg(id: String): Result<Ecg> {
        return try {
            val ecgEntity = ecgDao.getById(id)
            if (ecgEntity != null) {
                val waveformEntity = ecgWaveformDao.getByEcgId(id)
                if (waveformEntity != null) {
                    Result.Success(ecgEntity.toDomainModel(waveformEntity))
                } else {
                    Result.Error(Exception("ECG waveform data not found"))
                }
            } else {
                Result.Error(Exception("ECG data not found"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting ECG data")
            Result.Error(e)
        }
    }

    override suspend fun deleteEcg(id: String): Result<Unit> {
        return try {
            // Begin transaction
            ecgDao.beginTransaction()
            
            try {
                // Delete waveform data first (due to foreign key constraint)
                ecgWaveformDao.deleteByEcgId(id)
                
                // Delete ECG entity
                ecgDao.deleteById(id)
                
                // Set transaction successful
                ecgDao.setTransactionSuccessful()
                
                Result.Success(Unit)
            } finally {
                // End transaction
                ecgDao.endTransaction()
            }
        } catch (e: Exception) {
            Timber.e(e, "Error deleting ECG data")
            Result.Error(e)
        }
    }

    override fun getEcgForUser(userId: String): Flow<List<Ecg>> {
        return ecgDao.getForUser(userId)
            .map { ecgEntities ->
                ecgEntities.mapNotNull { ecgEntity ->
                    val waveformEntity = ecgWaveformDao.getByEcgId(ecgEntity.id)
                    if (waveformEntity != null) {
                        ecgEntity.toDomainModel(waveformEntity)
                    } else null
                }
            }
            .catch { e ->
                Timber.e(e, "Error getting ECG data for user")
                emit(emptyList())
            }
    }

    override fun getEcgForUserInTimeRange(
        userId: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): Flow<List<Ecg>> {
        return ecgDao.getForUserInTimeRange(userId, startTime, endTime)
            .map { ecgEntities ->
                ecgEntities.mapNotNull { ecgEntity ->
                    val waveformEntity = ecgWaveformDao.getByEcgId(ecgEntity.id)
                    if (waveformEntity != null) {
                        ecgEntity.toDomainModel(waveformEntity)
                    } else null
                }
            }
            .catch { e ->
                Timber.e(e, "Error getting ECG data for user in time range")
                emit(emptyList())
            }
    }

    override fun getLatestEcgForUser(userId: String): Flow<Ecg?> {
        return ecgDao.getLatestForUser(userId)
            .map { ecgEntity ->
                if (ecgEntity != null) {
                    val waveformEntity = ecgWaveformDao.getByEcgId(ecgEntity.id)
                    if (waveformEntity != null) {
                        ecgEntity.toDomainModel(waveformEntity)
                    } else null
                } else null
            }
            .catch { e ->
                Timber.e(e, "Error getting latest ECG data for user")
                emit(null)
            }
    }

    override fun getEcgWithClassification(
        userId: String,
        classification: EcgClassification
    ): Flow<List<Ecg>> {
        return ecgDao.getWithClassification(userId, classification)
            .map { ecgEntities ->
                ecgEntities.mapNotNull { ecgEntity ->
                    val waveformEntity = ecgWaveformDao.getByEcgId(ecgEntity.id)
                    if (waveformEntity != null) {
                        ecgEntity.toDomainModel(waveformEntity)
                    } else null
                }
            }
            .catch { e ->
                Timber.e(e, "Error getting ECG data with classification")
                emit(emptyList())
            }
    }

    override fun getEcgWithAbnormalClassification(userId: String): Flow<List<Ecg>> {
        return ecgDao.getWithAbnormalClassification(userId)
            .map { ecgEntities ->
                ecgEntities.mapNotNull { ecgEntity ->
                    val waveformEntity = ecgWaveformDao.getByEcgId(ecgEntity.id)
                    if (waveformEntity != null) {
                        ecgEntity.toDomainModel(waveformEntity)
                    } else null
                }
            }
            .catch { e ->
                Timber.e(e, "Error getting ECG data with abnormal classification")
                emit(emptyList())
            }
    }

    private fun Ecg.toEntity(): EcgEntity {
        return EcgEntity(
            id = id,
            userId = userId,
            timestamp = timestamp,
            leadType = leadType,
            durationSeconds = durationSeconds,
            classification = classification,
            heartRate = heartRate,
            annotations = annotations,
            hrvData = hrvData,
            deviceId = deviceId,
            deviceType = deviceType,
            activityState = measurementContext.activityState,
            bodyPosition = measurementContext.bodyPosition,
            manuallyEntered = measurementContext.manuallyEntered,
            validationStatus = validationStatus,
            notes = notes
        )
    }

    private fun EcgEntity.toDomainModel(waveformEntity: EcgWaveformEntity): Ecg {
        val context = MeasurementContext(
            activityState = activityState,
            bodyPosition = bodyPosition,
            manuallyEntered = manuallyEntered
        )

        return Ecg(
            id = id,
            userId = userId,
            timestamp = timestamp,
            waveformData = waveformEntity.waveformData,
            samplingRate = waveformEntity.samplingRate,
            leadType = leadType,
            durationSeconds = durationSeconds,
            classification = classification,
            heartRate = heartRate,
            annotations = annotations,
            hrvData = hrvData,
            deviceId = deviceId,
            deviceType = deviceType,
            measurementContext = context,
            validationStatus = validationStatus,
            notes = notes
        )
    }
}

/**
 * Implementation of BloodGlucoseRepository
 */
@Singleton
class BloodGlucoseRepositoryImpl @Inject constructor(
    private val bloodGlucoseDao: BloodGlucoseDao
) : BloodGlucoseRepository {

    override suspend fun saveBloodGlucose(bloodGlucose: BloodGlucose): Result<Unit> {
        return try {
            val entity = bloodGlucose.toEntity()
            bloodGlucoseDao.insert(entity)
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error saving blood glucose data")
            Result.Error(e)
        }
    }

    override suspend fun saveMultipleBloodGlucose(bloodGlucoses: List<BloodGlucose>): Result<Unit> {
        return try {
            val entities = bloodGlucoses.map { it.toEntity() }
            bloodGlucoseDao.insertAll(entities)
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error saving multiple blood glucose data")
            Result.Error(e)
        }
    }

    override suspend fun getBloodGlucose(id: String): Result<BloodGlucose> {
        return try {
            val entity = bloodGlucoseDao.getById(id)
            if (entity != null) {
                Result.Success(entity.toDomainModel())
            } else {
                Result.Error(Exception("Blood glucose data not found"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting blood glucose data")
            Result.Error(e)
        }
    }

    override suspend fun deleteBloodGlucose(id: String): Result<Unit> {
        return try {
            bloodGlucoseDao.deleteById(id)
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error deleting blood glucose data")
            Result.Error(e)
        }
    }

    override fun getBloodGlucoseForUser(userId: String): Flow<List<BloodGlucose>> {
        return bloodGlucoseDao.getForUser(userId)
            .map { entities -> entities.map { it.toDomainModel() } }
            .catch { e ->
                Timber.e(e, "Error getting blood glucose data for user")
                emit(emptyList())
            }
    }

    override fun getBloodGlucoseForUserInTimeRange(
        userId: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): Flow<List<BloodGlucose>> {
        return bloodGlucoseDao.getForUserInTimeRange(userId, startTime, endTime)
            .map { entities -> entities.map { it.toDomainModel() } }
            .catch { e ->
                Timber.e(e, "Error getting blood glucose data for user in time range")
                emit(emptyList())
            }
    }

    override fun getLatestBloodGlucoseForUser(userId: String): Flow<BloodGlucose?> {
        return bloodGlucoseDao.getLatestForUser(userId)
            .map { entity -> entity?.toDomainModel() }
            .catch { e ->
                Timber.e(e, "Error getting latest blood glucose data for user")
                emit(null)
            }
    }

    override fun getBloodGlucoseStats(
        userId: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): Flow<BloodGlucoseStats?> {
        return bloodGlucoseDao.getStatsForUserInTimeRange(userId, startTime, endTime)
            .map { stats ->
                if (stats != null) {
                    BloodGlucoseStats(
                        averageValue = stats.averageValue,
                        minValue = stats.minValue,
                        maxValue = stats.maxValue,
                        count = stats.count,
                        highCount = stats.highCount,
                        lowCount = stats.lowCount,
                        normalCount = stats.normalCount,
                        estimatedA1c = stats.estimatedA1c
                    )
                } else null
            }
            .catch { e ->
                Timber.e(e, "Error getting blood glucose stats for user")
                emit(null)
            }
    }

    override fun getBloodGlucoseByMealType(
        userId: String,
        mealType: MealType
    ): Flow<List<BloodGlucose>> {
        return bloodGlucoseDao.getByMealType(userId, mealType)
            .map { entities -> entities.map { it.toDomainModel() } }
            .catch { e ->
                Timber.e(e, "Error getting blood glucose data by meal type")
                emit(emptyList())
            }
    }

    override fun getBloodGlucoseByMeasurementType(
        userId: String,
        measurementType: GlucoseMeasurementType
    ): Flow<List<BloodGlucose>> {
        return bloodGlucoseDao.getByMeasurementType(userId, measurementType)
            .map { entities -> entities.map { it.toDomainModel() } }
            .catch { e ->
                Timber.e(e, "Error getting blood glucose data by measurement type")
                emit(emptyList())
            }
    }

    override fun getBloodGlucoseWithRange(
        userId: String,
        minValue: Float,
        maxValue: Float
    ): Flow<List<BloodGlucose>> {
        return bloodGlucoseDao.getWithRange(userId, minValue, maxValue)
            .map { entities -> entities.map { it.toDomainModel() } }
            .catch { e ->
                Timber.e(e, "Error getting blood glucose data with range")
                emit(emptyList())
            }
    }

    private fun BloodGlucose.toEntity(): BloodGlucoseEntity {
        return BloodGlucoseEntity(
            id = id,
            userId = userId,
            timestamp = timestamp,
            value = value,
            measurementType = measurementType,
            mealType = mealContext?.mealType,
            timeRelativeToMeal = mealContext?.timeRelativeToMeal,
            carbIntake = mealContext?.carbIntake,
            medicationName = medicationContext?.medicationName,
            medicationDosage = medicationContext?.dosage,
            medicationUnit = medicationContext?.unit,
            timeRelativeToMedication = medicationContext?.timeRelativeToMedication,
            deviceId = deviceId,
            deviceType = deviceType,
            activityState = measurementContext.activityState,
            bodyPosition = measurementContext.bodyPosition,
            manuallyEntered = measurementContext.manuallyEntered,
            validationStatus = validationStatus,
            notes = notes
        )
    }

    private fun BloodGlucoseEntity.toDomainModel(): BloodGlucose {
        val context = MeasurementContext(
            activityState = activityState,
            bodyPosition = bodyPosition,
            manuallyEntered = manuallyEntered
        )

        val mealCtx = if (mealType != null) {
            MealContext(
                mealType = mealType,
                timeRelativeToMeal = timeRelativeToMeal,
                carbIntake = carbIntake
            )
        } else null

        val medicationCtx = if (medicationName != null) {
            MedicationContext(
                medicationName = medicationName,
                dosage = medicationDosage,
                unit = medicationUnit ?: "",
                timeRelativeToMedication = timeRelativeToMedication
            )
        } else null

        return BloodGlucose(
            id = id,
            userId = userId,
            timestamp = timestamp,
            value = value,
            measurementType = measurementType,
            mealContext = mealCtx,
            medicationContext = medicationCtx,
            deviceId = deviceId,
            deviceType = deviceType,
            measurementContext = context,
            validationStatus = validationStatus,
            notes = notes
        )
    }
}

/**
 * Implementation of DeviceCapabilityRepository
 */
@Singleton
class DeviceCapabilityRepositoryImpl @Inject constructor(
    private val deviceCapabilityDao: DeviceCapabilityDao
) : DeviceCapabilityRepository {

    override suspend fun saveDeviceCapability(deviceCapability: DeviceCapability): Result<Unit> {
        return try {
            val entity = deviceCapability.toEntity()
            deviceCapabilityDao.insert(entity)
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error saving device capability data")
            Result.Error(e)
        }
    }

    override suspend fun getDeviceCapability(deviceId: String): Result<DeviceCapability> {
        return try {
            val entity = deviceCapabilityDao.getById(deviceId)
            if (entity != null) {
                Result.Success(entity.toDomainModel())
            } else {
                Result.Error(Exception("Device capability data not found"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting device capability data")
            Result.Error(e)
        }
    }

    override suspend fun updateDeviceBatteryLevel(deviceId: String, batteryLevel: Int): Result<Unit> {
        return try {
            deviceCapabilityDao.updateBatteryLevel(deviceId, batteryLevel)
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error updating device battery level")
            Result.Error(e)
        }
    }

    override suspend fun updateDeviceFirmware(deviceId: String, firmwareVersion: String): Result<Unit> {
        return try {
            deviceCapabilityDao.updateFirmwareVersion(deviceId, firmwareVersion)
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error updating device firmware version")
            Result.Error(e)
        }
    }

    override suspend fun deleteDeviceCapability(deviceId: String): Result<Unit> {
        return try {
            deviceCapabilityDao.deleteById(deviceId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error deleting device capability data")
            Result.Error(e)
        }
    }

    override fun getAllDeviceCapabilities(): Flow<List<DeviceCapability>> {
        return deviceCapabilityDao.getAll()
            .map { entities -> entities.map { it.toDomainModel() } }
            .catch { e ->
                Timber.e(e, "Error getting all device capabilities")
                emit(emptyList())
            }
    }

    override fun getDeviceCapabilitiesByModel(deviceModel: String): Flow<List<DeviceCapability>> {
        return deviceCapabilityDao.getByModel(deviceModel)
            .map { entities -> entities.map { it.toDomainModel() } }
            .catch { e ->
                Timber.e(e, "Error getting device capabilities by model")
                emit(emptyList())
            }
    }

    override fun getDeviceCapabilitiesWithMetric(metric: SupportedMetric): Flow<List<DeviceCapability>> {
        return deviceCapabilityDao.getWithMetric(metric)
            .map { entities -> entities.map { it.toDomainModel() } }
            .catch { e ->
                Timber.e(e, "Error getting device capabilities with metric")
                emit(emptyList())
            }
    }

    private fun DeviceCapability.toEntity(): DeviceCapabilityEntity {
        return DeviceCapabilityEntity(
            deviceId = deviceId,
            deviceName = deviceName,
            deviceModel = deviceModel,
            supportedMetrics = supportedMetrics,
            samplingRates = samplingRates,
            accuracyRatings = accuracyRatings,
            batteryLevel = batteryLevel,
            firmwareVersion = firmwareVersion,
            lastConnected = lastConnected
        )
    }

    private fun DeviceCapabilityEntity.toDomainModel(): DeviceCapability {
        return DeviceCapability(
            deviceId = deviceId,
            deviceName = deviceName,
            deviceModel = deviceModel,
            supportedMetrics = supportedMetrics,
            samplingRates = samplingRates,
            accuracyRatings = accuracyRatings,
            batteryLevel = batteryLevel,
            firmwareVersion = firmwareVersion,
            lastConnected = lastConnected
        )
    }
}
