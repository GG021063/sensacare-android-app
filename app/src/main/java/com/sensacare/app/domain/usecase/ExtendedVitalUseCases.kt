package com.sensacare.app.domain.usecase

import com.sensacare.app.domain.model.*
import com.sensacare.app.domain.repository.*
import com.sensacare.app.domain.util.Result
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow
import java.lang.Exception
import java.time.temporal.ChronoUnit

/**
 * Blood Oxygen Use Cases
 */
interface GetBloodOxygenData {
    suspend operator fun invoke(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate,
        limit: Int = 100,
        offset: Int = 0
    ): Result<List<BloodOxygen>>
    
    suspend fun getLatest(userId: String): Result<BloodOxygen?>
    
    suspend fun getByDateRange(
        userId: String,
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime
    ): Result<List<BloodOxygen>>
    
    fun observeLatest(userId: String): Flow<Result<BloodOxygen?>>
    
    fun observeByDateRange(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<Result<List<BloodOxygen>>>
}

class GetBloodOxygenDataImpl(
    private val bloodOxygenRepository: BloodOxygenRepository
) : GetBloodOxygenData {
    
    override suspend fun invoke(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate,
        limit: Int,
        offset: Int
    ): Result<List<BloodOxygen>> {
        return try {
            val startDateTime = startDate.atStartOfDay()
            val endDateTime = endDate.atTime(23, 59, 59)
            
            bloodOxygenRepository.getBloodOxygenByDateRange(
                userId = userId,
                startDateTime = startDateTime,
                endDateTime = endDateTime,
                limit = limit,
                offset = offset
            )
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    override suspend fun getLatest(userId: String): Result<BloodOxygen?> {
        return try {
            bloodOxygenRepository.getLatestBloodOxygen(userId)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    override suspend fun getByDateRange(
        userId: String,
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime
    ): Result<List<BloodOxygen>> {
        return try {
            bloodOxygenRepository.getBloodOxygenByDateRange(
                userId = userId,
                startDateTime = startDateTime,
                endDateTime = endDateTime
            )
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    override fun observeLatest(userId: String): Flow<Result<BloodOxygen?>> {
        return bloodOxygenRepository.observeLatestBloodOxygen(userId)
    }
    
    override fun observeByDateRange(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<Result<List<BloodOxygen>>> {
        val startDateTime = startDate.atStartOfDay()
        val endDateTime = endDate.atTime(23, 59, 59)
        
        return bloodOxygenRepository.observeBloodOxygenByDateRange(
            userId = userId,
            startDateTime = startDateTime,
            endDateTime = endDateTime
        )
    }
}

interface SaveBloodOxygenData {
    suspend operator fun invoke(bloodOxygen: BloodOxygen): Result<Unit>
    suspend fun saveMultiple(bloodOxygens: List<BloodOxygen>): Result<Unit>
}

class SaveBloodOxygenDataImpl(
    private val bloodOxygenRepository: BloodOxygenRepository,
    private val deviceCapabilityRepository: DeviceCapabilityRepository
) : SaveBloodOxygenData {
    
    override suspend fun invoke(bloodOxygen: BloodOxygen): Result<Unit> {
        return try {
            // Validate if the device supports blood oxygen measurement
            if (bloodOxygen.deviceId != null) {
                val deviceCapabilityResult = deviceCapabilityRepository.getDeviceCapability(bloodOxygen.deviceId)
                
                if (deviceCapabilityResult is Result.Success) {
                    val deviceCapability = deviceCapabilityResult.data
                    if (!deviceCapability.supportsMetric(SupportedMetric.BLOOD_OXYGEN)) {
                        return Result.Error(Exception("Device does not support blood oxygen measurement"))
                    }
                }
            }
            
            // Validate the blood oxygen value
            if (bloodOxygen.validationStatus != ValidationStatus.VALID) {
                return Result.Error(Exception("Invalid blood oxygen value: ${bloodOxygen.value}"))
            }
            
            bloodOxygenRepository.saveBloodOxygen(bloodOxygen)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    override suspend fun saveMultiple(bloodOxygens: List<BloodOxygen>): Result<Unit> {
        return try {
            // Filter out invalid measurements
            val validBloodOxygens = bloodOxygens.filter { it.validationStatus == ValidationStatus.VALID }
            
            if (validBloodOxygens.isEmpty()) {
                return Result.Error(Exception("No valid blood oxygen measurements to save"))
            }
            
            bloodOxygenRepository.saveMultipleBloodOxygen(validBloodOxygens)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

interface GetBloodOxygenStats {
    suspend operator fun invoke(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<BloodOxygenStats>
}

class GetBloodOxygenStatsImpl(
    private val bloodOxygenRepository: BloodOxygenRepository
) : GetBloodOxygenStats {
    
    override suspend fun invoke(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<BloodOxygenStats> {
        return try {
            val startDateTime = startDate.atStartOfDay()
            val endDateTime = endDate.atTime(23, 59, 59)
            
            val bloodOxygensResult = bloodOxygenRepository.getBloodOxygenByDateRange(
                userId = userId,
                startDateTime = startDateTime,
                endDateTime = endDateTime
            )
            
            if (bloodOxygensResult is Result.Error) {
                return bloodOxygensResult
            }
            
            val bloodOxygens = (bloodOxygensResult as Result.Success).data
            
            if (bloodOxygens.isEmpty()) {
                return Result.Error(Exception("No blood oxygen data available for the specified date range"))
            }
            
            // Calculate statistics
            val values = bloodOxygens.map { it.value }
            val avgValue = values.average().toInt()
            val minValue = values.minOrNull() ?: 0
            val maxValue = values.maxOrNull() ?: 0
            
            // Calculate standard deviation
            val variance = values.map { (it - avgValue) * (it - avgValue) }.average()
            val stdDeviation = kotlin.math.sqrt(variance)
            
            // Calculate time under normal threshold (95%)
            val timeUnderThreshold = bloodOxygens.count { it.value < BloodOxygen.NORMAL_THRESHOLD }
            
            val bloodOxygenStats = BloodOxygenStats(
                avgValue = avgValue,
                minValue = minValue,
                maxValue = maxValue,
                stdDeviation = stdDeviation,
                timeUnderThreshold = timeUnderThreshold,
                readingsCount = bloodOxygens.size,
                timeRange = Pair(startDateTime, endDateTime)
            )
            
            Result.Success(bloodOxygenStats)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

interface AnalyzeBloodOxygenTrends {
    suspend operator fun invoke(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<BloodOxygenTrendAnalysis>
}

class AnalyzeBloodOxygenTrendsImpl(
    private val bloodOxygenRepository: BloodOxygenRepository
) : AnalyzeBloodOxygenTrends {
    
    override suspend fun invoke(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<BloodOxygenTrendAnalysis> {
        return try {
            val startDateTime = startDate.atStartOfDay()
            val endDateTime = endDate.atTime(23, 59, 59)
            
            val bloodOxygensResult = bloodOxygenRepository.getBloodOxygenByDateRange(
                userId = userId,
                startDateTime = startDateTime,
                endDateTime = endDateTime
            )
            
            if (bloodOxygensResult is Result.Error) {
                return bloodOxygensResult
            }
            
            val bloodOxygens = (bloodOxygensResult as Result.Success).data
            
            if (bloodOxygens.isEmpty()) {
                return Result.Error(Exception("No blood oxygen data available for the specified date range"))
            }
            
            // Group by day
            val dailyAverages = bloodOxygens
                .groupBy { it.timestamp.toLocalDate() }
                .mapValues { (_, values) -> values.map { it.value }.average().toInt() }
            
            // Calculate trend slope
            val trendSlope = calculateTrendSlope(dailyAverages)
            
            // Calculate stability score
            val stabilityScore = calculateStabilityScore(bloodOxygens)
            
            // Calculate hypoxic events
            val hypoxicEvents = calculateHypoxicEvents(bloodOxygens)
            
            val trendAnalysis = BloodOxygenTrendAnalysis(
                userId = userId,
                startDate = startDate,
                endDate = endDate,
                dailyAverages = dailyAverages,
                trendSlope = trendSlope,
                stabilityScore = stabilityScore,
                hypoxicEvents = hypoxicEvents,
                analysisTimestamp = LocalDateTime.now()
            )
            
            Result.Success(trendAnalysis)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    private fun calculateTrendSlope(dailyAverages: Map<LocalDate, Int>): Double {
        if (dailyAverages.size < 2) return 0.0
        
        val sortedEntries = dailyAverages.entries.sortedBy { it.key }
        val xValues = sortedEntries.mapIndexed { index, _ -> index.toDouble() }
        val yValues = sortedEntries.map { it.value.toDouble() }
        
        // Simple linear regression
        val n = xValues.size
        val sumX = xValues.sum()
        val sumY = yValues.sum()
        val sumXY = xValues.zip(yValues).sumOf { it.first * it.second }
        val sumXX = xValues.sumOf { it * it }
        
        return (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX)
    }
    
    private fun calculateStabilityScore(bloodOxygens: List<BloodOxygen>): Int {
        val values = bloodOxygens.map { it.value }
        val range = (values.maxOrNull() ?: 0) - (values.minOrNull() ?: 0)
        
        // Calculate variance
        val mean = values.average()
        val variance = values.map { (it - mean) * (it - mean) }.average()
        
        // Score from 0-10, higher is more stable
        return when {
            range <= 2 && variance < 1.0 -> 10
            range <= 3 && variance < 2.0 -> 9
            range <= 4 && variance < 3.0 -> 8
            range <= 5 && variance < 4.0 -> 7
            range <= 6 && variance < 5.0 -> 6
            range <= 7 && variance < 6.0 -> 5
            range <= 8 && variance < 7.0 -> 4
            range <= 10 && variance < 8.0 -> 3
            range <= 12 && variance < 10.0 -> 2
            else -> 1
        }
    }
    
    private fun calculateHypoxicEvents(bloodOxygens: List<BloodOxygen>): List<HypoxicEvent> {
        val events = mutableListOf<HypoxicEvent>()
        val sortedReadings = bloodOxygens.sortedBy { it.timestamp }
        
        var currentEvent: HypoxicEvent? = null
        
        for (reading in sortedReadings) {
            if (reading.value < BloodOxygen.NORMAL_THRESHOLD) {
                if (currentEvent == null) {
                    // Start a new event
                    currentEvent = HypoxicEvent(
                        startTime = reading.timestamp,
                        endTime = reading.timestamp,
                        lowestValue = reading.value,
                        duration = 0
                    )
                } else {
                    // Update existing event
                    currentEvent = currentEvent.copy(
                        endTime = reading.timestamp,
                        lowestValue = minOf(currentEvent.lowestValue, reading.value),
                        duration = ChronoUnit.MINUTES.between(currentEvent.startTime, reading.timestamp).toInt()
                    )
                }
            } else if (currentEvent != null) {
                // End the current event
                events.add(currentEvent)
                currentEvent = null
            }
        }
        
        // Add the last event if it's still ongoing
        currentEvent?.let { events.add(it) }
        
        return events
    }
}

data class BloodOxygenTrendAnalysis(
    val userId: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val dailyAverages: Map<LocalDate, Int>,
    val trendSlope: Double,
    val stabilityScore: Int,
    val hypoxicEvents: List<HypoxicEvent>,
    val analysisTimestamp: LocalDateTime
) {
    val isImproving: Boolean
        get() = trendSlope > 0
    
    val isStable: Boolean
        get() = stabilityScore >= 7
    
    val hasFrequentHypoxicEvents: Boolean
        get() = hypoxicEvents.size >= 3
}

data class HypoxicEvent(
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val lowestValue: Int,
    val duration: Int // in minutes
) {
    val severity: HypoxicEventSeverity
        get() = when {
            lowestValue < BloodOxygen.SEVERE_HYPOXEMIA_THRESHOLD -> HypoxicEventSeverity.SEVERE
            lowestValue < BloodOxygen.MODERATE_HYPOXEMIA_THRESHOLD -> HypoxicEventSeverity.MODERATE
            else -> HypoxicEventSeverity.MILD
        }
}

enum class HypoxicEventSeverity {
    MILD,
    MODERATE,
    SEVERE
}

/**
 * Body Temperature Use Cases
 */
interface GetBodyTemperatureData {
    suspend operator fun invoke(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate,
        limit: Int = 100,
        offset: Int = 0
    ): Result<List<BodyTemperature>>
    
    suspend fun getLatest(userId: String): Result<BodyTemperature?>
    
    suspend fun getByDateRange(
        userId: String,
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime
    ): Result<List<BodyTemperature>>
    
    fun observeLatest(userId: String): Flow<Result<BodyTemperature?>>
    
    fun observeByDateRange(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<Result<List<BodyTemperature>>>
}

class GetBodyTemperatureDataImpl(
    private val bodyTemperatureRepository: BodyTemperatureRepository
) : GetBodyTemperatureData {
    
    override suspend fun invoke(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate,
        limit: Int,
        offset: Int
    ): Result<List<BodyTemperature>> {
        return try {
            val startDateTime = startDate.atStartOfDay()
            val endDateTime = endDate.atTime(23, 59, 59)
            
            bodyTemperatureRepository.getBodyTemperatureByDateRange(
                userId = userId,
                startDateTime = startDateTime,
                endDateTime = endDateTime,
                limit = limit,
                offset = offset
            )
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    override suspend fun getLatest(userId: String): Result<BodyTemperature?> {
        return try {
            bodyTemperatureRepository.getLatestBodyTemperature(userId)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    override suspend fun getByDateRange(
        userId: String,
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime
    ): Result<List<BodyTemperature>> {
        return try {
            bodyTemperatureRepository.getBodyTemperatureByDateRange(
                userId = userId,
                startDateTime = startDateTime,
                endDateTime = endDateTime
            )
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    override fun observeLatest(userId: String): Flow<Result<BodyTemperature?>> {
        return bodyTemperatureRepository.observeLatestBodyTemperature(userId)
    }
    
    override fun observeByDateRange(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<Result<List<BodyTemperature>>> {
        val startDateTime = startDate.atStartOfDay()
        val endDateTime = endDate.atTime(23, 59, 59)
        
        return bodyTemperatureRepository.observeBodyTemperatureByDateRange(
            userId = userId,
            startDateTime = startDateTime,
            endDateTime = endDateTime
        )
    }
}

interface SaveBodyTemperatureData {
    suspend operator fun invoke(bodyTemperature: BodyTemperature): Result<Unit>
    suspend fun saveMultiple(bodyTemperatures: List<BodyTemperature>): Result<Unit>
}

class SaveBodyTemperatureDataImpl(
    private val bodyTemperatureRepository: BodyTemperatureRepository,
    private val deviceCapabilityRepository: DeviceCapabilityRepository
) : SaveBodyTemperatureData {
    
    override suspend fun invoke(bodyTemperature: BodyTemperature): Result<Unit> {
        return try {
            // Validate if the device supports temperature measurement
            if (bodyTemperature.deviceId != null) {
                val deviceCapabilityResult = deviceCapabilityRepository.getDeviceCapability(bodyTemperature.deviceId)
                
                if (deviceCapabilityResult is Result.Success) {
                    val deviceCapability = deviceCapabilityResult.data
                    if (!deviceCapability.supportsMetric(SupportedMetric.BODY_TEMPERATURE)) {
                        return Result.Error(Exception("Device does not support temperature measurement"))
                    }
                }
            }
            
            // Validate the temperature value
            if (bodyTemperature.validationStatus != ValidationStatus.VALID) {
                return Result.Error(Exception("Invalid temperature value: ${bodyTemperature.value}"))
            }
            
            bodyTemperatureRepository.saveBodyTemperature(bodyTemperature)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    override suspend fun saveMultiple(bodyTemperatures: List<BodyTemperature>): Result<Unit> {
        return try {
            // Filter out invalid measurements
            val validBodyTemperatures = bodyTemperatures.filter { it.validationStatus == ValidationStatus.VALID }
            
            if (validBodyTemperatures.isEmpty()) {
                return Result.Error(Exception("No valid temperature measurements to save"))
            }
            
            bodyTemperatureRepository.saveMultipleBodyTemperature(validBodyTemperatures)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

interface GetBodyTemperatureStats {
    suspend operator fun invoke(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<TemperatureStats>
}

class GetBodyTemperatureStatsImpl(
    private val bodyTemperatureRepository: BodyTemperatureRepository
) : GetBodyTemperatureStats {
    
    override suspend fun invoke(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<TemperatureStats> {
        return try {
            val startDateTime = startDate.atStartOfDay()
            val endDateTime = endDate.atTime(23, 59, 59)
            
            val temperaturesResult = bodyTemperatureRepository.getBodyTemperatureByDateRange(
                userId = userId,
                startDateTime = startDateTime,
                endDateTime = endDateTime
            )
            
            if (temperaturesResult is Result.Error) {
                return temperaturesResult
            }
            
            val temperatures = (temperaturesResult as Result.Success).data
            
            if (temperatures.isEmpty()) {
                return Result.Error(Exception("No temperature data available for the specified date range"))
            }
            
            // Calculate statistics
            val values = temperatures.map { it.value }
            val avgValue = values.average().toFloat()
            val minValue = values.minOrNull() ?: 0f
            val maxValue = values.maxOrNull() ?: 0f
            
            // Calculate standard deviation
            val variance = values.map { (it - avgValue) * (it - avgValue) }.average()
            val stdDeviation = kotlin.math.sqrt(variance)
            
            // Determine the most common measurement site
            val measurementSite = temperatures
                .groupBy { it.measurementSite }
                .maxByOrNull { it.value.size }
                ?.key ?: TemperatureMeasurementSite.UNKNOWN
            
            val temperatureStats = TemperatureStats(
                avgValue = avgValue,
                minValue = minValue,
                maxValue = maxValue,
                stdDeviation = stdDeviation,
                readingsCount = temperatures.size,
                timeRange = Pair(startDateTime, endDateTime),
                measurementSite = measurementSite
            )
            
            Result.Success(temperatureStats)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

interface DetectFever {
    suspend operator fun invoke(
        userId: String,
        lookbackDays: Int = 3
    ): Result<FeverDetectionResult>
}

class DetectFeverImpl(
    private val bodyTemperatureRepository: BodyTemperatureRepository
) : DetectFever {
    
    override suspend fun invoke(
        userId: String,
        lookbackDays: Int
    ): Result<FeverDetectionResult> {
        return try {
            val endDateTime = LocalDateTime.now()
            val startDateTime = endDateTime.minusDays(lookbackDays.toLong())
            
            val temperaturesResult = bodyTemperatureRepository.getBodyTemperatureByDateRange(
                userId = userId,
                startDateTime = startDateTime,
                endDateTime = endDateTime
            )
            
            if (temperaturesResult is Result.Error) {
                return temperaturesResult
            }
            
            val temperatures = (temperaturesResult as Result.Success).data
            
            if (temperatures.isEmpty()) {
                return Result.Error(Exception("No temperature data available for fever detection"))
            }
            
            // Sort by timestamp
            val sortedTemperatures = temperatures.sortedBy { it.timestamp }
            
            // Check for fever patterns
            val feverReadings = temperatures.filter { 
                it.classification == TemperatureClassification.MILD_FEVER ||
                it.classification == TemperatureClassification.MODERATE_FEVER ||
                it.classification == TemperatureClassification.HIGH_FEVER ||
                it.classification == TemperatureClassification.HYPERPYREXIA
            }
            
            val hasFever = feverReadings.isNotEmpty()
            
            // Check for fever pattern (rising temperature)
            val hasFeverPattern = if (sortedTemperatures.size >= 3) {
                val recentReadings = sortedTemperatures.takeLast(3)
                recentReadings[0].value < recentReadings[1].value && 
                recentReadings[1].value < recentReadings[2].value &&
                recentReadings[2].value >= 37.5f
            } else {
                false
            }
            
            // Calculate peak temperature
            val peakTemperature = temperatures.maxByOrNull { it.value }
            
            // Calculate fever duration
            val feverDuration = if (feverReadings.isNotEmpty()) {
                val firstFeverReading = feverReadings.minByOrNull { it.timestamp }
                val lastFeverReading = feverReadings.maxByOrNull { it.timestamp }
                
                if (firstFeverReading != null && lastFeverReading != null) {
                    ChronoUnit.HOURS.between(firstFeverReading.timestamp, lastFeverReading.timestamp).toInt()
                } else {
                    0
                }
            } else {
                0
            }
            
            val feverDetectionResult = FeverDetectionResult(
                userId = userId,
                hasFever = hasFever,
                hasFeverPattern = hasFeverPattern,
                feverClassification = peakTemperature?.classification ?: TemperatureClassification.NORMAL,
                peakTemperature = peakTemperature?.value,
                feverDuration = feverDuration,
                feverReadings = feverReadings,
                detectionTimestamp = LocalDateTime.now()
            )
            
            Result.Success(feverDetectionResult)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

data class FeverDetectionResult(
    val userId: String,
    val hasFever: Boolean,
    val hasFeverPattern: Boolean,
    val feverClassification: TemperatureClassification,
    val peakTemperature: Float?,
    val feverDuration: Int, // in hours
    val feverReadings: List<BodyTemperature>,
    val detectionTimestamp: LocalDateTime
) {
    val requiresMedicalAttention: Boolean
        get() = hasFever && (
            feverClassification == TemperatureClassification.HIGH_FEVER ||
            feverClassification == TemperatureClassification.HYPERPYREXIA ||
            feverDuration >= 72
        )
}

/**
 * Stress Level Use Cases
 */
interface GetStressLevelData {
    suspend operator fun invoke(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate,
        limit: Int = 100,
        offset: Int = 0
    ): Result<List<StressLevel>>
    
    suspend fun getLatest(userId: String): Result<StressLevel?>
    
    suspend fun getByDateRange(
        userId: String,
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime
    ): Result<List<StressLevel>>
    
    fun observeLatest(userId: String): Flow<Result<StressLevel?>>
    
    fun observeByDateRange(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<Result<List<StressLevel>>>
}

class GetStressLevelDataImpl(
    private val stressLevelRepository: StressLevelRepository
) : GetStressLevelData {
    
    override suspend fun invoke(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate,
        limit: Int,
        offset: Int
    ): Result<List<StressLevel>> {
        return try {
            val startDateTime = startDate.atStartOfDay()
            val endDateTime = endDate.atTime(23, 59, 59)
            
            stressLevelRepository.getStressLevelByDateRange(
                userId = userId,
                startDateTime = startDateTime,
                endDateTime = endDateTime,
                limit = limit,
                offset = offset
            )
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    override suspend fun getLatest(userId: String): Result<StressLevel?> {
        return try {
            stressLevelRepository.getLatestStressLevel(userId)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    override suspend fun getByDateRange(
        userId: String,
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime
    ): Result<List<StressLevel>> {
        return try {
            stressLevelRepository.getStressLevelByDateRange(
                userId = userId,
                startDateTime = startDateTime,
                endDateTime = endDateTime
            )
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    override fun observeLatest(userId: String): Flow<Result<StressLevel?>> {
        return stressLevelRepository.observeLatestStressLevel(userId)
    }
    
    override fun observeByDateRange(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<Result<List<StressLevel>>> {
        val startDateTime = startDate.atStartOfDay()
        val endDateTime = endDate.atTime(23, 59, 59)
        
        return stressLevelRepository.observeStressLevelByDateRange(
            userId = userId,
            startDateTime = startDateTime,
            endDateTime = endDateTime
        )
    }
}

interface SaveStressLevelData {
    suspend operator fun invoke(stressLevel: StressLevel): Result<Unit>
    suspend fun saveMultiple(stressLevels: List<StressLevel>): Result<Unit>
}

class SaveStressLevelDataImpl(
    private val stressLevelRepository: StressLevelRepository,
    private val deviceCapabilityRepository: DeviceCapabilityRepository
) : SaveStressLevelData {
    
    override suspend fun invoke(stressLevel: StressLevel): Result<Unit> {
        return try {
            // Validate if the device supports stress measurement
            if (stressLevel.deviceId != null) {
                val deviceCapabilityResult = deviceCapabilityRepository.getDeviceCapability(stressLevel.deviceId)
                
                if (deviceCapabilityResult is Result.Success) {
                    val deviceCapability = deviceCapabilityResult.data
                    if (!deviceCapability.supportsMetric(SupportedMetric.STRESS_LEVEL)) {
                        return Result.Error(Exception("Device does not support stress level measurement"))
                    }
                }
            }
            
            // Validate the stress level value
            if (stressLevel.validationStatus != ValidationStatus.VALID) {
                return Result.Error(Exception("Invalid stress level value: ${stressLevel.value}"))
            }
            
            stressLevelRepository.saveStressLevel(stressLevel)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    override suspend fun saveMultiple(stressLevels: List<StressLevel>): Result<Unit> {
        return try {
            // Filter out invalid measurements
            val validStressLevels = stressLevels.filter { it.validationStatus == ValidationStatus.VALID }
            
            if (validStressLevels.isEmpty()) {
                return Result.Error(Exception("No valid stress level measurements to save"))
            }
            
            stressLevelRepository.saveMultipleStressLevel(validStressLevels)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

interface GetStressStats {
    suspend operator fun invoke(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<StressStats>
}

class GetStressStatsImpl(
    private val stressLevelRepository: StressLevelRepository
) : GetStressStats {
    
    override suspend fun invoke(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<StressStats> {
        return try {
            val startDateTime = startDate.atStartOfDay()
            val endDateTime = endDate.atTime(23, 59, 59)
            
            val stressLevelsResult = stressLevelRepository.getStressLevelByDateRange(
                userId = userId,
                startDateTime = startDateTime,
                endDateTime = endDateTime
            )
            
            if (stressLevelsResult is Result.Error) {
                return stressLevelsResult
            }
            
            val stressLevels = (stressLevelsResult as Result.Success).data
            
            if (stressLevels.isEmpty()) {
                return Result.Error(Exception("No stress level data available for the specified date range"))
            }
            
            // Calculate statistics
            val values = stressLevels.map { it.value }
            val avgValue = values.average().toInt()
            val minValue = values.minOrNull() ?: 0
            val maxValue = values.maxOrNull() ?: 0
            
            // Calculate standard deviation
            val variance = values.map { (it - avgValue) * (it - avgValue) }.average()
            val stdDeviation = kotlin.math.sqrt(variance)
            
            // Calculate time in high stress
            val highStressReadings = stressLevels.filter { 
                it.value >= StressLevel.HIGH_STRESS_THRESHOLD 
            }
            
            // Estimate minutes in high stress (assuming readings are taken periodically)
            val timeInHighStress = if (highStressReadings.size > 1) {
                // Estimate based on time between readings
                val sortedReadings = highStressReadings.sortedBy { it.timestamp }
                var totalMinutes = 0
                
                for (i in 0 until sortedReadings.size - 1) {
                    val current = sortedReadings[i]
                    val next = sortedReadings[i + 1]
                    val minutesBetween = ChronoUnit.MINUTES.between(current.timestamp, next.timestamp)
                    
                    // If readings are more than 30 minutes apart, assume 15 minutes of high stress
                    // Otherwise, count the full duration
                    if (minutesBetween <= 30) {
                        totalMinutes += minutesBetween.toInt()
                    } else {
                        totalMinutes += 15
                    }
                }
                
                // Add 15 minutes for the last reading
                totalMinutes + 15
            } else if (highStressReadings.size == 1) {
                15 // Assume 15 minutes for a single reading
            } else {
                0
            }
            
            val stressStats = StressStats(
                avgValue = avgValue,
                minValue = minValue,
                maxValue = maxValue,
                stdDeviation = stdDeviation,
                timeInHighStress = timeInHighStress,
                readingsCount = stressLevels.size,
                timeRange = Pair(startDateTime, endDateTime)
            )
            
            Result.Success(stressStats)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

interface AnalyzeStressPatterns {
    suspend operator fun invoke(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<StressPatternAnalysis>
}

class AnalyzeStressPatternsImpl(
    private val stressLevelRepository: StressLevelRepository
) : AnalyzeStressPatterns {
    
    override suspend fun invoke(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<StressPatternAnalysis> {
        return try {
            val startDateTime = startDate.atStartOfDay()
            val endDateTime = endDate.atTime(23, 59, 59)
            
            val stressLevelsResult = stressLevelRepository.getStressLevelByDateRange(
                userId = userId,
                startDateTime = startDateTime,
                endDateTime = endDateTime
            )
            
            if (stressLevelsResult is Result.Error) {
                return stressLevelsResult
            }
            
            val stressLevels = (stressLevelsResult as Result.Success).data
            
            if (stressLevels.isEmpty()) {
                return Result.Error(Exception("No stress level data available for the specified date range"))
            }
            
            // Group by day
            val dailyAverages = stressLevels
                .groupBy { it.timestamp.toLocalDate() }
                .mapValues { (_, values) -> values.map { it.value }.average().toInt() }
            
            // Group by hour of day to find patterns
            val hourlyAverages = stressLevels
                .groupBy { it.timestamp.hour }
                .mapValues { (_, values) -> values.map { it.value }.average().toInt() }
            
            // Identify peak stress hours (top 3)
            val peakStressHours = hourlyAverages
                .entries
                .sortedByDescending { it.value }
                .take(3)
                .map { it.key }
            
            // Identify stress events (periods of high stress)
            val stressEvents = identifyStressEvents(stressLevels)
            
            // Calculate weekly pattern (is stress higher on weekdays or weekends?)
            val weekdayStress = stressLevels
                .filter { it.timestamp.dayOfWeek.value <= 5 } // Monday to Friday
                .map { it.value }
                .average()
                .toInt()
            
            val weekendStress = stressLevels
                .filter { it.timestamp.dayOfWeek.value > 5 } // Saturday and Sunday
                .map { it.value }
                .average()
                .toInt()
            
            val workdayPattern = when {
                weekdayStress > weekendStress + 10 -> WorkdayStressPattern.HIGHER_ON_WEEKDAYS
                weekendStress > weekdayStress + 10 -> WorkdayStressPattern.HIGHER_ON_WEEKENDS
                else -> WorkdayStressPattern.NO_SIGNIFICANT_DIFFERENCE
            }
            
            val stressPatternAnalysis = StressPatternAnalysis(
                userId = userId,
                startDate = startDate,
                endDate = endDate,
                dailyAverages = dailyAverages,
                hourlyAverages = hourlyAverages,
                peakStressHours = peakStressHours,
                stressEvents = stressEvents,
                weekdayAverage = weekdayStress,
                weekendAverage = weekendStress,
                workdayPattern = workdayPattern,
                analysisTimestamp = LocalDateTime.now()
            )
            
            Result.Success(stressPatternAnalysis)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    private fun identifyStressEvents(stressLevels: List<StressLevel>): List<StressEvent> {
        val events = mutableListOf<StressEvent>()
        val sortedReadings = stressLevels.sortedBy { it.timestamp }
        
        var currentEvent: StressEvent? = null
        
        for (reading in sortedReadings) {
            if (reading.value >= StressLevel.HIGH_STRESS_THRESHOLD) {
                if (currentEvent == null) {
                    // Start a new event
                    currentEvent = StressEvent(
                        startTime = reading.timestamp,
                        endTime = reading.timestamp,
                        peakValue = reading.value,
                        duration = 0
                    )
                } else {
                    // Update existing event
                    currentEvent = currentEvent.copy(
                        endTime = reading.timestamp,
                        peakValue = maxOf(currentEvent.peakValue, reading.value),
                        duration = ChronoUnit.MINUTES.between(currentEvent.startTime, reading.timestamp).toInt()
                    )
                }
            } else if (currentEvent != null) {
                // End the current event
                events.add(currentEvent)
                currentEvent = null
            }
        }
        
        // Add the last event if it's still ongoing
        currentEvent?.let { events.add(it) }
        
        return events
    }
}

data class StressPatternAnalysis(
    val userId: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val dailyAverages: Map<LocalDate, Int>,
    val hourlyAverages: Map<Int, Int>,
    val peakStressHours: List<Int>,
    val stressEvents: List<StressEvent>,
    val weekdayAverage: Int,
    val weekendAverage: Int,
    val workdayPattern: WorkdayStressPattern,
    val analysisTimestamp: LocalDateTime
) {
    val hasRegularStressPattern: Boolean
        get() = peakStressHours.size >= 2 && stressEvents.size >= 3
    
    val mostStressfulHour: Int?
        get() = hourlyAverages.entries.maxByOrNull { it.value }?.key
    
    val stressEventFrequency: Double
        get() {
            val days = ChronoUnit.DAYS.between(startDate, endDate) + 1
            return stressEvents.size.toDouble() / days.toDouble()
        }
}

data class StressEvent(
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val peakValue: Int,
    val duration: Int // in minutes
) {
    val severity: StressEventSeverity
        get() = when {
            peakValue >= StressLevel.VERY_HIGH_STRESS_THRESHOLD -> StressEventSeverity.VERY_HIGH
            peakValue >= StressLevel.HIGH_STRESS_THRESHOLD -> StressEventSeverity.HIGH
            else -> StressEventSeverity.MODERATE
        }
}

enum class StressEventSeverity {
    MODERATE,
    HIGH,
    VERY_HIGH
}

enum class WorkdayStressPattern {
    HIGHER_ON_WEEKDAYS,
    HIGHER_ON_WEEKENDS,
    NO_SIGNIFICANT_DIFFERENCE
}

/**
 * ECG Use Cases
 */
interface GetEcgData {
    suspend operator fun invoke(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate,
        limit: Int = 20,
        offset: Int = 0
    ): Result<List<Ecg>>
    
    suspend fun getLatest(userId: String): Result<Ecg?>
    
    suspend fun getById(userId: String, ecgId: String): Result<Ecg?>
    
    suspend fun getByDateRange(
        userId: String,
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime
    ): Result<List<Ecg>>
    
    fun observeLatest(userId: String): Flow<Result<Ecg?>>
}

class GetEcgDataImpl(
    private val ecgRepository: EcgRepository
) : GetEcgData {
    
    override suspend fun invoke(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate,
        limit: Int,
        offset: Int
    ): Result<List<Ecg>> {
        return try {
            val startDateTime = startDate.atStartOfDay()
            val endDateTime = endDate.atTime(23, 59, 59)
            
            ecgRepository.getEcgByDateRange(
                userId = userId,
                startDateTime = startDateTime,
                endDateTime = endDateTime,
                limit = limit,
                offset = offset
            )
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    override suspend fun getLatest(userId: String): Result<Ecg?> {
        return try {
            ecgRepository.getLatestEcg(userId)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    override suspend fun getById(userId: String, ecgId: String): Result<Ecg?> {
        return try {
            ecgRepository.getEcgById(userId, ecgId)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    override suspend fun getByDateRange(
        userId: String,
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime
    ): Result<List<Ecg>> {
        return try {
            ecgRepository.getEcgByDateRange(
                userId = userId,
                startDateTime = startDateTime,
                endDateTime = endDateTime
            )
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    override fun observeLatest(userId: String): Flow<Result<Ecg?>> {
        return ecgRepository.observeLatestEcg(userId)
    }
}

interface SaveEcgData {
    suspend operator fun invoke(ecg: Ecg): Result<Unit>
}

class SaveEcgDataImpl(
    private val ecgRepository: EcgRepository,
    private val deviceCapabilityRepository: DeviceCapabilityRepository
) : SaveEcgData {
    
    override suspend fun invoke(ecg: Ecg): Result<Unit> {
        return try {
            // Validate if the device supports ECG measurement
            if (ecg.deviceId != null) {
                val deviceCapabilityResult = deviceCapabilityRepository.getDeviceCapability(ecg.deviceId)
                
                if (deviceCapabilityResult is Result.Success) {
                    val deviceCapability = deviceCapabilityResult.data
                    if (!deviceCapability.supportsMetric(SupportedMetric.ECG)) {
                        return Result.Error(Exception("Device does not support ECG measurement"))
                    }
                }
            }
            
            // Validate the ECG data
            if (ecg.validationStatus != ValidationStatus.VALID) {
                return Result.Error(Exception("Invalid ECG data"))
            }
            
            ecgRepository.saveEcg(ecg)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

interface AnalyzeEcgRhythm {
    suspend operator fun invoke(ecgId: String, userId: String): Result<EcgRhythmAnalysis>
}

class AnalyzeEcgRhythmImpl(
    private val ecgRepository: EcgRepository
) : AnalyzeEcgRhythm {
    
    override suspend fun invoke(ecgId: String, userId: String): Result<EcgRhythmAnalysis> {
        return try {
            val ecgResult = ecgRepository.getEcgById(userId, ecgId)
            
            if (ecgResult is Result.Error) {
                return ecgResult
            }
            
            val ecg = (ecgResult as Result.Success).data ?: 
                return Result.Error(Exception("ECG not found"))
            
            // Check if the ECG already has a classification
            val classification = ecg.classification ?: 
                return Result.Error(Exception("ECG does not have a classification"))
            
            // Extract R-R intervals if available
            val rrIntervals = ecg.hrvData?.rrIntervals ?: emptyList()
            
            // Calculate heart rate variability metrics if not already available
            val hrvMetrics = ecg.hrvData ?: calculateHrvMetrics(ecg.waveformData, ecg.samplingRate)
            
            // Analyze rhythm regularity
            val rhythmRegularity = analyzeRhythmRegularity(rrIntervals)
            
            // Create rhythm analysis
            val rhythmAnalysis = EcgRhythmAnalysis(
                ecgId = ecgId,
                userId = userId,
                classification = classification,
                rhythmRegularity = rhythmRegularity,
                heartRate = ecg.heartRate ?: calculateHeartRate(rrIntervals),
                hrvMetrics = hrvMetrics,
                qrsWidth = calculateQrsWidth(ecg.waveformData, ecg.samplingRate),
                prInterval = calculatePrInterval(ecg.waveformData, ecg.samplingRate),
                qtInterval = calculateQtInterval(ecg.waveformData, ecg.samplingRate),
                analysisTimestamp = LocalDateTime.now()
            )
            
            Result.Success(rhythmAnalysis)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    private fun calculateHrvMetrics(waveformData: List<Float>, samplingRate: Int): HeartRateVariabilityData {
        // This is a simplified implementation
        // In a real app, this would use more sophisticated signal processing
        
        // Detect R peaks (simplified)
        val rPeaks = detectRPeaks(waveformData, samplingRate)
        
        // Calculate RR intervals
        val rrIntervals = calculateRrIntervals(rPeaks, samplingRate)
        
        // Calculate time-domain HRV metrics
        val sdnn = calculateSdnn(rrIntervals)
        val rmssd = calculateRmssd(rrIntervals)
        val pnn50 = calculatePnn50(rrIntervals)
        
        return HeartRateVariabilityData(
            sdnn = sdnn,
            rmssd = rmssd,
            pnn50 = pnn50,
            rrIntervals = rrIntervals,
            frequencyDomain = null // Frequency domain analysis is more complex
        )
    }
    
    private fun detectRPeaks(waveformData: List<Float>, samplingRate: Int): List<Int> {
        // Simplified R peak detection
        // In a real implementation, this would use more sophisticated algorithms
        
        val peaks = mutableListOf<Int>()
        val windowSize = samplingRate / 10 // 100ms window
        
        for (i in windowSize until waveformData.size - windowSize) {
            val window = waveformData.subList(i - windowSize, i + windowSize)
            val current = waveformData[i]
            
            // Check if current point is a local maximum
            if (current == window.maxOrNull() && current > 0.5f) {
                peaks.add(i)
                
                // Skip ahead to avoid detecting the same peak multiple times
                i += windowSize
            }
        }
        
        return peaks
    }
    
    private fun calculateRrIntervals(rPeaks: List<Int>, samplingRate: Int): List<Int> {
        val rrIntervals = mutableListOf<Int>()
        
        for (i in 1 until rPeaks.size) {
            val interval = rPeaks[i] - rPeaks[i - 1]
            val intervalMs = (interval.toFloat() / samplingRate * 1000).toInt()
            rrIntervals.add(intervalMs)
        }
        
        return rrIntervals
    }
    
    private fun calculateSdnn(rrIntervals: List<Int>): Float {
        if (rrIntervals.isEmpty()) return 0f
        
        val mean = rrIntervals.average()
        val variance = rrIntervals.map { (it - mean) * (it - mean) }.average()
        
        return kotlin.math.sqrt(variance).toFloat()
    }
    
    private fun calculateRmssd(rrIntervals: List<Int>): Float {
        if (rrIntervals.size < 2) return 0f
        
        val successiveDifferences = mutableListOf<Double>()
        
        for (i in 1 until rrIntervals.size) {
            val diff = (rrIntervals[i] - rrIntervals[i - 1]).toDouble()
            successiveDifferences.add(diff * diff)
        }
        
        val meanSquaredDiff = successiveDifferences.average()
        
        return kotlin.math.sqrt(meanSquaredDiff).toFloat()
    }
    
    private fun calculatePnn50(rrIntervals: List<Int>): Float {
        if (rrIntervals.size < 2) return 0f
        
        var nn50Count = 0
        
        for (i in 1 until rrIntervals.size) {
            val diff = kotlin.math.abs(rrIntervals[i] - rrIntervals[i - 1])
            if (diff > 50) {
                nn50Count++
            }
        }
        
        return (nn50Count.toFloat() / (rrIntervals.size - 1)) * 100
    }
    
    private fun analyzeRhythmRegularity(rrIntervals: List<Int>): RhythmRegularity {
        if (rrIntervals.isEmpty()) return RhythmRegularity.UNKNOWN
        
        val sdnn = calculateSdnn(rrIntervals)
        
        return when {
            sdnn < 20 -> RhythmRegularity.VERY_REGULAR
            sdnn < 50 -> RhythmRegularity.REGULAR
            sdnn < 100 -> RhythmRegularity.SLIGHTLY_IRREGULAR
            sdnn < 150 -> RhythmRegularity.MODERATELY_IRREGULAR
            else -> RhythmRegularity.HIGHLY_IRREGULAR
        }
    }
    
    private fun calculateHeartRate(rrIntervals: List<Int>): Int {
        if (rrIntervals.isEmpty()) return 0
        
        val avgRrMs = rrIntervals.average()
        return (60000 / avgRrMs).toInt()
    }
    
    private fun calculateQrsWidth(waveformData: List<Float>, samplingRate: Int): Float? {
        // This is a placeholder for a more sophisticated implementation
        // In a real app, this would use more advanced signal processing
        return null
    }
    
    private fun calculatePrInterval(waveformData: List<Float>, samplingRate: Int): Float? {
        // This is a placeholder for a more sophisticated implementation
        return null
    }
    
    private fun calculateQtInterval(waveformData: List<Float>, samplingRate: Int): Float? {
        // This is a placeholder for a more sophisticated implementation
        return null
    }
}

data class EcgRhythmAnalysis(
    val ecgId: String,
    val userId: String,
    val classification: EcgClassification,
    val rhythmRegularity: RhythmRegularity,
    val heartRate: Int,
    val hrvMetrics: HeartRateVariabilityData?,
    val qrsWidth: Float?, // in milliseconds
    val prInterval: Float?, // in milliseconds
    val qtInterval: Float?, // in milliseconds
    val analysisTimestamp: LocalDateTime
) {
    val isNormal: Boolean
        get() = classification == EcgClassification.NORMAL_SINUS_RHYTHM && 
                rhythmRegularity == RhythmRegularity.REGULAR || 
                rhythmRegularity == RhythmRegularity.VERY_REGULAR
    
    val requiresAttention: Boolean
        get() = classification == EcgClassification.ATRIAL_FIBRILLATION || 
                classification == EcgClassification.VENTRICULAR_TACHYCARDIA ||
                classification == EcgClassification.ST_ELEVATION
}

enum class RhythmRegularity {
    VERY_REGULAR,
    REGULAR,
    SLIGHTLY_IRREGULAR,
    MODERATELY_IRREGULAR,
    HIGHLY_IRREGULAR,
    UNKNOWN
}

interface DetectEcgAbnormalities {
    suspend operator fun invoke(
        userId: String,
        lookbackDays: Int = 30
    ): Result<EcgAbnormalityDetection>
}

class DetectEcgAbnormalitiesImpl(
    private val ecgRepository: EcgRepository
) : DetectEcgAbnormalities {
    
    override suspend fun invoke(
        userId: String,
        lookbackDays: Int
    ): Result<EcgAbnormalityDetection> {
        return try {
            val endDateTime = LocalDateTime.now()
            val startDateTime = endDateTime.minusDays(lookbackDays.toLong())
            
            val ecgsResult = ecgRepository.getEcgByDateRange(
                userId = userId,
                startDateTime = startDateTime,
                endDateTime = endDateTime
            )
            
            if (ecgsResult is Result.Error) {
                return ecgsResult
            }
            
            val ecgs = (ecgsResult as Result.Success).data
            
            if (ecgs.isEmpty()) {
                return Result.Error(Exception("No ECG data available for abnormality detection"))
            }
            
            // Group ECGs by classification
            val ecgsByClassification = ecgs
                .filter { it.classification != null }
                .groupBy { it.classification!! }
            
            // Check for abnormalities
            val abnormalities = mutableMapOf<EcgClassification, List<Ecg>>()
            
            for ((classification, classificationEcgs) in ecgsByClassification) {
                if (classification != EcgClassification.NORMAL_SINUS_RHYTHM && 
                    classification != EcgClassification.INCONCLUSIVE &&
                    classification != EcgClassification.POOR_SIGNAL_QUALITY) {
                    abnormalities[classification] = classificationEcgs
                }
            }
            
            val abnormalitiesDetected = abnormalities.isNotEmpty()
            
            // Calculate severity score
            val severityScore = calculateSeverityScore(abnormalities)
            
            val abnormalityDetection = EcgAbnormalityDetection(
                userId = userId,
                startDate = startDateTime.toLocalDate(),
                endDate = endDateTime.toLocalDate(),
                abnormalitiesDetected = abnormalitiesDetected,
                abnormalities = abnormalities,
                severityScore = severityScore,
                detectionTimestamp = LocalDateTime.now()
            )
            
            Result.Success(abnormalityDetection)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    private fun calculateSeverityScore(abnormalities: Map<EcgClassification, List<Ecg>>): Int {
        var score = 0
        
        // Score based on abnormality type
        for ((classification, ecgs) in abnormalities) {
            val classificationScore = when (classification) {
                EcgClassification.ATRIAL_FIBRILLATION -> 5
                EcgClassification.VENTRICULAR_TACHYCARDIA -> 8
                EcgClassification.ST_ELEVATION -> 9
                EcgClassification.BRADYCARDIA -> 3
                EcgClassification.TACHYCARDIA -> 3
                EcgClassification.PREMATURE_VENTRICULAR_CONTRACTION -> 2
                EcgClassification.PREMATURE_ATRIAL_CONTRACTION -> 1
                EcgClassification.HEART_BLOCK -> 7
                EcgClassification.ST_DEPRESSION -> 6
                else -> 0
            }
            
            // Increase score based on frequency
            val frequencyMultiplier = when {
                ecgs.size >= 5 -> 2.0
                ecgs.size >= 3 -> 1.5
                else -> 1.0
            }
            
            score += (classificationScore * frequencyMultiplier).toInt()
        }
        
        return score.coerceAtMost(10)
    }
}

data class EcgAbnormalityDetection(
    val userId: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val abnormalitiesDetected: Boolean,
    val abnormalities: Map<EcgClassification, List<Ecg>>,
    val severityScore: Int, // 0-10 scale
    val detectionTimestamp: LocalDateTime
) {
    val requiresMedicalAttention: Boolean
        get() = severityScore >= 7 || 
                abnormalities.containsKey(EcgClassification.ATRIAL_FIBRILLATION) ||
                abnormalities.containsKey(EcgClassification.VENTRICULAR_TACHYCARDIA) ||
                abnormalities.containsKey(EcgClassification.ST_ELEVATION)
}

/**
 * Blood Glucose Use Cases
 */
interface GetBloodGlucoseData {
    suspend operator fun invoke(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate,
        limit: Int = 100,
        offset: Int = 0
    ): Result<List<BloodGlucose>>
    
    suspend fun getLatest(userId: String): Result<BloodGlucose?>
    
    suspend fun getByDateRange(
        userId: String,
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime
    ): Result<List<BloodGlucose>>
    
    fun observeLatest(userId: String): Flow<Result<BloodGlucose?>>
    
    fun observeByDateRange(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<Result<List<BloodGlucose>>>
}

class GetBloodGlucoseDataImpl(
    private val bloodGlucoseRepository: BloodGlucoseRepository
) : GetBloodGlucoseData {
    
    override suspend fun invoke(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate,
        limit: Int,
        offset: Int
    ): Result<List<BloodGlucose>> {
        return try {
            val startDateTime = startDate.atStartOfDay()
            val endDateTime = endDate.atTime(23, 59, 59)
            
            bloodGlucoseRepository.getBloodGlucoseByDateRange(
                userId = userId,
                startDateTime = startDateTime,
                endDateTime = endDateTime,
                limit = limit,
                offset = offset
            )
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    override suspend fun getLatest(userId: String): Result<BloodGlucose?> {
        return try {
            bloodGlucoseRepository.getLatestBloodGlucose(userId)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    override suspend fun getByDateRange(
        userId: String,
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime
    ): Result<List<BloodGlucose>> {
        return try {
            bloodGlucoseRepository.getBloodGlucoseByDateRange(
                userId = userId,
                startDateTime = startDateTime,
                endDateTime = endDateTime
            )
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    override fun observeLatest(userId: String): Flow<Result<BloodGlucose?>> {
        return bloodGlucoseRepository.observeLatestBloodGlucose(userId)
    }
    
    override fun observeByDateRange(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<Result<List<BloodGlucose>>> {
        val startDateTime = startDate.atStartOfDay()
        val endDateTime = endDate.atTime(23, 59, 59)
        
        return bloodGlucoseRepository.observeBloodGlucoseByDateRange(
            userId = userId,
            startDateTime = startDateTime,
            endDateTime = endDateTime
        )
    }
}

interface SaveBloodGlucoseData {
    suspend operator fun invoke(bloodGlucose: BloodGlucose): Result<Unit>
    suspend fun saveMultiple(bloodGlucoses: List<BloodGlucose>): Result<Unit>
}

class SaveBloodGlucoseDataImpl(
    private val bloodGlucoseRepository: BloodGlucoseRepository,
    private val deviceCapabilityRepository: DeviceCapabilityRepository
) : SaveBloodGlucoseData {
    
    override suspend fun invoke(bloodGlucose: BloodGlucose): Result<Unit> {
        return try {
            // Validate if the device supports blood glucose measurement
            if (bloodGlucose.deviceId != null) {
                val deviceCapabilityResult = deviceCapabilityRepository.getDeviceCapability(bloodGlucose.deviceId)
                
                if (deviceCapabilityResult is Result.Success) {
                    val deviceCapability = deviceCapabilityResult.data
                    if (!deviceCapability.supportsMetric(SupportedMetric.BLOOD_GLUCOSE)) {
                        return Result.Error(Exception("Device does not support blood glucose measurement"))
                    }
                }
            }
            
            // Validate the blood glucose value
            if (bloodGlucose.validationStatus != ValidationStatus.VALID) {
                return Result.Error(Exception("Invalid blood glucose value: ${bloodGlucose.value}"))
            }
            
            bloodGlucoseRepository.saveBloodGlucose(bloodGlucose)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    override suspend fun saveMultiple(bloodGlucoses: List<BloodGlucose>): Result<Unit> {
        return try {
            // Filter out invalid measurements
            val validBloodGlucoses = bloodGlucoses.filter { it.validationStatus == ValidationStatus.VALID }
            
            if (validBloodGlucoses.isEmpty()) {
                return Result.Error(Exception("No valid blood glucose measurements to save"))
            }
            
            bloodGlucoseRepository.saveMultipleBloodGlucose(validBloodGlucoses)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

interface GetBloodGlucoseStats {
    suspend operator fun invoke(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<GlucoseStats>
}

class GetBloodGlucoseStatsImpl(
    private val bloodGlucoseRepository: BloodGlucoseRepository
) : GetBloodGlucoseStats {
    
    override suspend fun invoke(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<GlucoseStats> {
        return try {
            val startDateTime = startDate.atStartOfDay()
            val endDateTime = endDate.atTime(23, 59, 59)
            
            val bloodGlucosesResult = bloodGlucoseRepository.getBloodGlucoseByDateRange(
                userId = userId,
                startDateTime = startDateTime,
                endDateTime = endDateTime
            )
            
            if (bloodGlucosesResult is Result.Error) {
                return bloodGlucosesResult
            }
            
            val bloodGlucoses = (bloodGlucosesResult as Result.Success).data
            
            if (bloodGlucoses.isEmpty()) {
                return Result.Error(Exception("No blood glucose data available for the specified date range"))
            }
            
            // Calculate statistics
            val values = bloodGlucoses.map { it.value }
            val avgValue = values.average().toFloat()
            val minValue = values.minOrNull() ?: 0f
            val maxValue = values.maxOrNull() ?: 0f
            
            // Calculate standard deviation
            val variance = values.map { (it - avgValue) * (it - avgValue) }.average()
            val stdDeviation = kotlin.math.sqrt(variance)
            
            // Calculate time in range (70-180 mg/dL)
            val inRangeReadings = bloodGlucoses.filter { it.value in 70f..180f }
            val aboveRangeReadings = bloodGlucoses.filter { it.value > 180f }
            val belowRangeReadings = bloodGlucoses.filter { it.value < 70f }
            
            // Estimate minutes in each range (assuming readings are taken periodically)
            val timeInRange = estimateTimeInState(inRangeReadings)
            val timeAboveRange = estimateTimeInState(aboveRangeReadings)
            val timeBelowRange = estimateTimeInState(belowRangeReadings)
            
            // Estimate A1c based on average glucose
            val estimatedA1c = (avgValue + 46.7f) / 28.7f
            
            val glucoseStats = GlucoseStats(
                avgValue = avgValue,
                minValue = minValue,
                maxValue = maxValue,
                stdDeviation = stdDeviation,
                timeInRange = timeInRange,
                timeAboveRange = timeAboveRange,
                timeBelowRange = timeBelowRange,
                estimatedA1c = estimatedA1c,
                readingsCount = bloodGlucoses.size,
                timeRange = Pair(startDateTime, endDateTime)
            )
            
            Result.Success(glucoseStats)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    private fun estimateTimeInState(readings: List<BloodGlucose>): Int {
        if (readings.isEmpty()) return 0
        
        // Sort readings by timestamp
        val sortedReadings = readings.sortedBy { it.timestamp }
        var totalMinutes = 0
        
        for (i in 0 until sortedReadings.size - 1) {
            val current = sortedReadings[i]
            val next = sortedReadings[i + 1]
            val minutesBetween = ChronoUnit.MINUTES.between(current.timestamp, next.timestamp)
            
            // If readings are more than 60 minutes apart, assume 30 minutes in this state
            // Otherwise, count the full duration
            if (minutesBetween <= 60) {
                totalMinutes += minutesBetween.toInt()
            } else {
                totalMinutes += 30
            }
        }
        
        // Add 30 minutes for the last reading
        totalMinutes += 30
        
        return totalMinutes
    }
}

interface AnalyzeGlucoseTrends {
    suspend operator fun invoke(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<GlucoseTrendAnalysis>
}

class AnalyzeGlucoseTrendsImpl(
    private val bloodGlucoseRepository: BloodGlucoseRepository
) : AnalyzeGlucoseTrends {
    
    override suspend fun invoke(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<GlucoseTrendAnalysis> {
        return try {
            val startDateTime = startDate.atStartOfDay()
            val endDateTime = endDate.atTime(23, 59, 59)
            
            val bloodGlucosesResult = bloodGlucoseRepository.getBloodGlucoseByDateRange(
                userId = userId,
                startDateTime = startDateTime,
                endDateTime = endDateTime
            )
            
            if (bloodGlucosesResult is Result.Error) {
                return bloodGlucosesResult
            }
            
            val bloodGlucoses = (bloodGlucosesResult as Result.Success).data
            
            if (bloodGlucoses.isEmpty()) {
                return Result.Error(Exception("No blood glucose data available for the specified date range"))
            }
            
            // Group by day
            val dailyAverages = bloodGlucoses
                .groupBy { it.timestamp.toLocalDate() }
                .mapValues { (_, values) -> values.map { it.value }.average().toFloat() }
            
            // Calculate fasting glucose trend
            val fastingGlucoses = bloodGlucoses
                .filter { it.measurementType == GlucoseMeasurementType.FASTING }
                .groupBy { it.timestamp.toLocalDate() }
                .mapValues { (_, values) -> values.map { it.value }.average().toFloat() }
            
            // Calculate post-meal glucose trend
            val postMealGlucoses = bloodGlucoses
                .filter { it.measurementType == GlucoseMeasurementType.POSTPRANDIAL }
                .groupBy { it.timestamp.toLocalDate() }
                .mapValues { (_, values) -> values.map { it.value }.average().toFloat() }
            
            // Calculate trend slopes
            val overallTrendSlope = calculateTrendSlope(dailyAverages)
            val fastingTrendSlope = calculateTrendSlope(fastingGlucoses)
            val postMealTrendSlope = calculateTrendSlope(postMealGlucoses)
            
            // Calculate glucose variability
            val variabilityIndex = calculateGlucoseVariabilityIndex(bloodGlucoses)
            
            // Calculate estimated A1c
            val estimatedA1c = calculateEstimatedA1c(bloodGlucoses)
            
            // Identify glucose patterns
            val mealPatterns = identifyMealPatterns(bloodGlucoses)
            
            val trendAnalysis = GlucoseTrendAnalysis(
                userId = userId,
                startDate = startDate,
                endDate = endDate,
                dailyAverages = dailyAverages,
                fastingGlucoses = fastingGlucoses,
                postMealGlucoses = postMealGlucoses,
                overallTrendSlope = overallTrendSlope,
                fastingTrendSlope = fastingTrendSlope,
                postMealTrendSlope = postMealTrendSlope,
                variabilityIndex = variabilityIndex,
                estimatedA1c = estimatedA1c,
                mealPatterns = mealPatterns,
                analysisTimestamp = LocalDateTime.now()
            )
            
            Result.Success(trendAnalysis)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    private fun calculateTrendSlope(dailyValues: Map<LocalDate, Float>): Double {
        if (dailyValues.size < 2) return 0.0
        
        val sortedEntries = dailyValues.entries.sortedBy { it.key }
        val xValues = sortedEntries.mapIndexed { index, _ -> index.toDouble() }
        val yValues = sortedEntries.map { it.value.toDouble() }
        
        // Simple linear regression
        val n = xValues.size
        val sumX = xValues.sum()
        val sumY = yValues.sum()
        val sumXY = xValues.zip(yValues).sumOf { it.first * it.second }
        val sumXX = xValues.sumOf { it * it }
        
        return (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX)
    }
    
    private fun calculateGlucoseVariabilityIndex(bloodGlucoses: List<BloodGlucose>): Int {
        val values = bloodGlucoses.map { it.value }
        val range = (values.maxOrNull() ?: 0f) - (values.minOrNull() ?: 0f)
        
        // Calculate coefficient of variation (CV)
        val mean = values.average()
        val standardDeviation = kotlin.math.sqrt(values.map { (it - mean) * (it - mean) }.average())
        val cv = (standardDeviation / mean) * 100
        
        // Calculate variability index (0-100)
        val rangeScore = when {
            range < 50 -> 10
            range < 100 -> 20
            range < 150 -> 30
            range < 200 -> 40
            else -> 50
        }
        
        val cvScore = when {
            cv < 15 -> 10
            cv < 25 -> 20
            cv < 35 -> 30
            cv < 45 -> 40
            else -> 50
        }
        
        return rangeScore +