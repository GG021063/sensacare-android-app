package com.sensacare.app.domain.usecase

import com.sensacare.app.domain.model.HeartRate
import com.sensacare.app.domain.model.HeartRateStats
import com.sensacare.app.domain.model.HeartRateZones
import com.sensacare.app.domain.model.HeartRateVariabilityAnalysis
import com.sensacare.app.domain.model.HeartRateTrend
import com.sensacare.app.domain.model.AbnormalHeartRateDetection
import com.sensacare.app.domain.model.HeartRateAbnormalityType
import com.sensacare.app.domain.repository.HealthDataRepository
import com.sensacare.app.domain.repository.UserRepository
import com.sensacare.app.domain.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.math.sqrt
import kotlin.math.pow

/**
 * Get heart rate data for a user with filtering options
 *
 * This use case retrieves heart rate data from the repository with various
 * filtering options, including date range, device ID, and pagination.
 */
class GetHeartRateData @Inject constructor(
    private val healthDataRepository: HealthDataRepository
) {
    /**
     * Get heart rate by ID
     * @param id Heart rate ID
     * @return Result containing the heart rate or an error
     */
    suspend operator fun invoke(id: String): Result<HeartRate> {
        return healthDataRepository.getHeartRate(id)
    }

    /**
     * Get heart rate as Flow by ID
     * @param id Heart rate ID
     * @return Flow emitting Result containing the heart rate or an error
     */
    operator fun invoke(id: String, asFlow: Boolean): Flow<Result<HeartRate>> {
        return healthDataRepository.getHeartRateAsFlow(id)
    }

    /**
     * Get heart rates for a user within a date range
     * @param userId User ID
     * @param startDate Start date for filtering
     * @param endDate End date for filtering
     * @return Result containing a list of heart rates or an error
     */
    suspend operator fun invoke(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<List<HeartRate>> {
        if (endDate.isBefore(startDate)) {
            return Result.Error(IllegalArgumentException("End date cannot be before start date"))
        }
        
        return healthDataRepository.getHeartRatesForUser(userId, startDate, endDate)
    }

    /**
     * Get heart rates for a user within a date range as a Flow
     * @param userId User ID
     * @param startDate Start date for filtering
     * @param endDate End date for filtering
     * @return Flow emitting Result containing a list of heart rates or an error
     */
    operator fun invoke(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate,
        asFlow: Boolean
    ): Flow<Result<List<HeartRate>>> {
        if (endDate.isBefore(startDate)) {
            return flow { emit(Result.Error(IllegalArgumentException("End date cannot be before start date"))) }
        }
        
        return healthDataRepository.getHeartRatesForUserAsFlow(userId, startDate, endDate)
    }

    /**
     * Get latest heart rate for a user
     * @param userId User ID
     * @return Result containing the latest heart rate or an error
     */
    suspend fun getLatest(userId: String): Result<HeartRate?> {
        return healthDataRepository.getLatestHeartRate(userId)
    }

    /**
     * Get latest heart rate for a user as a Flow
     * @param userId User ID
     * @return Flow emitting Result containing the latest heart rate or an error
     */
    fun getLatestAsFlow(userId: String): Flow<Result<HeartRate?>> {
        return healthDataRepository.getLatestHeartRateAsFlow(userId)
    }
}

/**
 * Save heart rate data with validation
 *
 * This use case validates and saves heart rate data to the repository,
 * ensuring data integrity and proper error handling.
 */
class SaveHeartRateData @Inject constructor(
    private val healthDataRepository: HealthDataRepository
) {
    /**
     * Save a single heart rate measurement
     * @param heartRate Heart rate to save
     * @return Result containing the saved heart rate ID or an error
     */
    suspend operator fun invoke(heartRate: HeartRate): Result<String> {
        // Validate heart rate data
        val validationResult = validateHeartRate(heartRate)
        if (validationResult is Result.Error) {
            return validationResult
        }
        
        return healthDataRepository.saveHeartRate(heartRate)
    }

    /**
     * Save multiple heart rate measurements in a batch
     * @param heartRates List of heart rates to save
     * @return Result containing the list of saved heart rate IDs or an error
     */
    suspend operator fun invoke(heartRates: List<HeartRate>): Result<List<String>> {
        // Validate all heart rates
        val invalidHeartRates = heartRates.mapNotNull { heartRate ->
            val validationResult = validateHeartRate(heartRate)
            if (validationResult is Result.Error) {
                Pair(heartRate, validationResult.exception)
            } else {
                null
            }
        }
        
        if (invalidHeartRates.isNotEmpty()) {
            val errorMessage = invalidHeartRates.joinToString("\n") { (heartRate, exception) ->
                "Invalid heart rate (timestamp: ${heartRate.timestamp}): ${exception.message}"
            }
            return Result.Error(IllegalArgumentException(errorMessage))
        }
        
        // Save heart rates individually to ensure proper validation and error handling
        val savedIds = mutableListOf<String>()
        val errors = mutableListOf<String>()
        
        for (heartRate in heartRates) {
            when (val result = healthDataRepository.saveHeartRate(heartRate)) {
                is Result.Success -> savedIds.add(result.data)
                is Result.Error -> errors.add("Failed to save heart rate (timestamp: ${heartRate.timestamp}): ${result.exception.message}")
            }
        }
        
        return if (errors.isEmpty()) {
            Result.Success(savedIds)
        } else {
            Result.Error(Exception("Failed to save some heart rates:\n${errors.joinToString("\n")}"))
        }
    }

    /**
     * Update an existing heart rate measurement
     * @param heartRate Heart rate to update
     * @return Result containing success flag or an error
     */
    suspend fun update(heartRate: HeartRate): Result<Boolean> {
        // Validate heart rate data
        val validationResult = validateHeartRate(heartRate)
        if (validationResult is Result.Error) {
            return Result.Error(validationResult.exception)
        }
        
        // Ensure ID is not empty
        if (heartRate.id.isEmpty()) {
            return Result.Error(IllegalArgumentException("Heart rate ID cannot be empty for update operation"))
        }
        
        return healthDataRepository.updateHeartRate(heartRate)
    }

    /**
     * Delete a heart rate measurement
     * @param id Heart rate ID to delete
     * @return Result containing success flag or an error
     */
    suspend fun delete(id: String): Result<Boolean> {
        if (id.isEmpty()) {
            return Result.Error(IllegalArgumentException("Heart rate ID cannot be empty for delete operation"))
        }
        
        return healthDataRepository.deleteHeartRate(id)
    }

    /**
     * Validate heart rate data
     * @param heartRate Heart rate to validate
     * @return Result indicating validation success or error
     */
    private fun validateHeartRate(heartRate: HeartRate): Result<Unit> {
        // Validate user ID
        if (heartRate.userId.isEmpty()) {
            return Result.Error(IllegalArgumentException("User ID cannot be empty"))
        }
        
        // Validate heart rate value
        if (heartRate.value < 20 || heartRate.value > 250) {
            return Result.Error(IllegalArgumentException("Heart rate value must be between 20 and 250 bpm"))
        }
        
        // Validate timestamp
        val now = LocalDateTime.now()
        if (heartRate.timestamp.isAfter(now)) {
            return Result.Error(IllegalArgumentException("Heart rate timestamp cannot be in the future"))
        }
        
        // Validate timestamp is not too old (more than 30 days)
        if (ChronoUnit.DAYS.between(heartRate.timestamp, now) > 30) {
            return Result.Error(IllegalArgumentException("Heart rate timestamp cannot be more than 30 days old"))
        }
        
        // Validate resting heart rate if present
        if (heartRate.restingHeartRate != null && (heartRate.restingHeartRate < 30 || heartRate.restingHeartRate > 120)) {
            return Result.Error(IllegalArgumentException("Resting heart rate must be between 30 and 120 bpm"))
        }
        
        // Validate HRV value if present
        if (heartRate.hrvValue != null && (heartRate.hrvValue < 0 || heartRate.hrvValue > 200)) {
            return Result.Error(IllegalArgumentException("HRV value must be between 0 and 200 ms"))
        }
        
        return Result.Success(Unit)
    }
}

/**
 * Get heart rate statistics for a user
 *
 * This use case retrieves and analyzes heart rate statistics for a specific
 * time period, including averages, minimums, maximums, and time in zones.
 */
class GetHeartRateStats @Inject constructor(
    private val healthDataRepository: HealthDataRepository
) {
    /**
     * Get heart rate statistics for a user within a date range
     * @param userId User ID
     * @param startDate Start date for analysis
     * @param endDate End date for analysis
     * @return Result containing heart rate statistics or an error
     */
    suspend operator fun invoke(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<HeartRateStats> {
        if (endDate.isBefore(startDate)) {
            return Result.Error(IllegalArgumentException("End date cannot be before start date"))
        }
        
        return healthDataRepository.getHeartRateStats(userId, startDate, endDate)
    }

    /**
     * Get heart rate statistics for a user within a date range as a Flow
     * @param userId User ID
     * @param startDate Start date for analysis
     * @param endDate End date for analysis
     * @return Flow emitting Result containing heart rate statistics or an error
     */
    operator fun invoke(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate,
        asFlow: Boolean
    ): Flow<Result<HeartRateStats>> {
        if (endDate.isBefore(startDate)) {
            return flow { emit(Result.Error(IllegalArgumentException("End date cannot be before start date"))) }
        }
        
        return healthDataRepository.getHeartRateStatsAsFlow(userId, startDate, endDate)
    }

    /**
     * Calculate resting heart rate from a list of heart rate measurements
     * @param heartRates List of heart rate measurements
     * @return Calculated resting heart rate
     */
    fun calculateRestingHeartRate(heartRates: List<HeartRate>): Int? {
        if (heartRates.isEmpty()) {
            return null
        }
        
        // Filter for resting heart rate measurements or early morning measurements
        val restingHeartRates = heartRates.filter { it.isRestingHeartRate }
        
        if (restingHeartRates.isNotEmpty()) {
            // Calculate the average of resting heart rates
            return restingHeartRates.map { it.value }.average().toInt()
        }
        
        // If no explicit resting heart rates, use the lowest 10% of heart rates
        val sortedHeartRates = heartRates.sortedBy { it.value }
        val lowestPercentile = sortedHeartRates.take((sortedHeartRates.size * 0.1).toInt().coerceAtLeast(1))
        
        return lowestPercentile.map { it.value }.average().toInt()
    }

    /**
     * Calculate heart rate variability from a list of heart rate measurements
     * @param heartRates List of heart rate measurements with HRV values
     * @return Average HRV value or null if not available
     */
    fun calculateAverageHrv(heartRates: List<HeartRate>): Double? {
        val hrvValues = heartRates.mapNotNull { it.hrvValue }
        
        if (hrvValues.isEmpty()) {
            return null
        }
        
        return hrvValues.average()
    }
}

/**
 * Get heart rate zones based on user age
 *
 * This use case calculates heart rate training zones based on user age
 * following standard formulas and exercise physiology principles.
 */
class GetHeartRateZones @Inject constructor(
    private val userRepository: UserRepository
) {
    /**
     * Calculate heart rate zones for a user
     * @param userId User ID
     * @return Result containing heart rate zones or an error
     */
    suspend operator fun invoke(userId: String): Result<HeartRateZones> {
        // Get user age from user repository
        val userResult = userRepository.getUser(userId)
        
        if (userResult is Result.Error) {
            return Result.Error(userResult.exception)
        }
        
        val user = (userResult as Result.Success).data
        
        if (user.age == null) {
            return Result.Error(IllegalStateException("User age is required to calculate heart rate zones"))
        }
        
        return calculateHeartRateZones(user.age)
    }

    /**
     * Calculate heart rate zones for a specific age
     * @param age User age
     * @return Result containing heart rate zones
     */
    fun calculateHeartRateZones(age: Int): Result<HeartRateZones> {
        if (age < 1 || age > 120) {
            return Result.Error(IllegalArgumentException("Age must be between 1 and 120"))
        }
        
        // Calculate maximum heart rate using the standard formula: 220 - age
        val maxHeartRate = 220 - age
        
        // Calculate heart rate zones based on percentages of max heart rate
        val zone1Range = Pair((maxHeartRate * 0.5).toInt(), (maxHeartRate * 0.6).toInt())
        val zone2Range = Pair((maxHeartRate * 0.6).toInt(), (maxHeartRate * 0.7).toInt())
        val zone3Range = Pair((maxHeartRate * 0.7).toInt(), (maxHeartRate * 0.8).toInt())
        val zone4Range = Pair((maxHeartRate * 0.8).toInt(), (maxHeartRate * 0.9).toInt())
        val zone5Range = Pair((maxHeartRate * 0.9).toInt(), maxHeartRate)
        
        val heartRateZones = HeartRateZones(
            zone1Range = zone1Range.first.toDouble() to zone1Range.second.toDouble(),
            zone2Range = zone2Range.first.toDouble() to zone2Range.second.toDouble(),
            zone3Range = zone3Range.first.toDouble() to zone3Range.second.toDouble(),
            zone4Range = zone4Range.first.toDouble() to zone4Range.second.toDouble(),
            zone5Range = zone5Range.first.toDouble() to zone5Range.second.toDouble()
        )
        
        return Result.Success(heartRateZones)
    }

    /**
     * Calculate Karvonen heart rate zones based on resting heart rate
     * @param age User age
     * @param restingHeartRate User's resting heart rate
     * @return Result containing heart rate zones
     */
    fun calculateKarvonenZones(age: Int, restingHeartRate: Int): Result<HeartRateZones> {
        if (age < 1 || age > 120) {
            return Result.Error(IllegalArgumentException("Age must be between 1 and 120"))
        }
        
        if (restingHeartRate < 30 || restingHeartRate > 120) {
            return Result.Error(IllegalArgumentException("Resting heart rate must be between 30 and 120 bpm"))
        }
        
        // Calculate maximum heart rate using the standard formula: 220 - age
        val maxHeartRate = 220 - age
        
        // Calculate heart rate reserve (HRR)
        val hrr = maxHeartRate - restingHeartRate
        
        // Calculate heart rate zones using Karvonen formula: RHR + (intensity% * HRR)
        val zone1Range = Pair(
            (restingHeartRate + 0.5 * hrr).toInt(),
            (restingHeartRate + 0.6 * hrr).toInt()
        )
        val zone2Range = Pair(
            (restingHeartRate + 0.6 * hrr).toInt(),
            (restingHeartRate + 0.7 * hrr).toInt()
        )
        val zone3Range = Pair(
            (restingHeartRate + 0.7 * hrr).toInt(),
            (restingHeartRate + 0.8 * hrr).toInt()
        )
        val zone4Range = Pair(
            (restingHeartRate + 0.8 * hrr).toInt(),
            (restingHeartRate + 0.9 * hrr).toInt()
        )
        val zone5Range = Pair(
            (restingHeartRate + 0.9 * hrr).toInt(),
            maxHeartRate
        )
        
        val heartRateZones = HeartRateZones(
            zone1Range = zone1Range.first.toDouble() to zone1Range.second.toDouble(),
            zone2Range = zone2Range.first.toDouble() to zone2Range.second.toDouble(),
            zone3Range = zone3Range.first.toDouble() to zone3Range.second.toDouble(),
            zone4Range = zone4Range.first.toDouble() to zone4Range.second.toDouble(),
            zone5Range = zone5Range.first.toDouble() to zone5Range.second.toDouble()
        )
        
        return Result.Success(heartRateZones)
    }
}

/**
 * Detect abnormal heart rate patterns
 *
 * This use case analyzes heart rate data to detect potential abnormalities
 * such as tachycardia, bradycardia, arrhythmias, and other patterns that
 * may indicate health issues.
 */
class DetectAbnormalHeartRate @Inject constructor(
    private val healthDataRepository: HealthDataRepository,
    private val userRepository: UserRepository
) {
    companion object {
        // Clinical thresholds
        private const val TACHYCARDIA_THRESHOLD = 100 // bpm
        private const val BRADYCARDIA_THRESHOLD = 50 // bpm
        private const val RESTING_TACHYCARDIA_THRESHOLD = 90 // bpm
        private const val RESTING_BRADYCARDIA_THRESHOLD = 40 // bpm
        private const val EXERCISE_TACHYCARDIA_MULTIPLIER = 0.9 // 90% of max HR
        private const val SUDDEN_CHANGE_THRESHOLD = 30 // bpm
        private const val SUSTAINED_ELEVATED_MINUTES = 60 // minutes
        private const val IRREGULAR_RHYTHM_VARIANCE = 15 // bpm
    }

    /**
     * Detect abnormal heart rate patterns for a user within a date range
     * @param userId User ID
     * @param startDate Start date for analysis
     * @param endDate End date for analysis
     * @return Result containing abnormal heart rate detection or an error
     */
    suspend operator fun invoke(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<AbnormalHeartRateDetection> {
        if (endDate.isBefore(startDate)) {
            return Result.Error(IllegalArgumentException("End date cannot be before start date"))
        }
        
        // Get user age for max heart rate calculation
        val userResult = userRepository.getUser(userId)
        if (userResult is Result.Error) {
            return Result.Error(userResult.exception)
        }
        
        val user = (userResult as Result.Success).data
        val age = user.age ?: return Result.Error(IllegalStateException("User age is required for abnormal heart rate detection"))
        
        // Get heart rate data
        val heartRatesResult = healthDataRepository.getHeartRatesForUser(userId, startDate, endDate)
        if (heartRatesResult is Result.Error) {
            return Result.Error(heartRatesResult.exception)
        }
        
        val heartRates = (heartRatesResult as Result.Success).data
        if (heartRates.isEmpty()) {
            return Result.Error(IllegalStateException("No heart rate data available for analysis"))
        }
        
        // Calculate max heart rate based on age
        val maxHeartRate = 220 - age
        
        // Analyze heart rates for abnormalities
        val abnormalities = mutableListOf<Pair<HeartRateAbnormalityType, List<HeartRate>>>()
        
        // Check for tachycardia (high heart rate)
        val tachycardiaHeartRates = heartRates.filter { 
            it.value >= TACHYCARDIA_THRESHOLD && 
            (it.activityLevel == null || it.activityLevel == "REST" || it.activityLevel == "SEDENTARY") 
        }
        if (tachycardiaHeartRates.isNotEmpty()) {
            abnormalities.add(HeartRateAbnormalityType.TACHYCARDIA to tachycardiaHeartRates)
        }
        
        // Check for bradycardia (low heart rate)
        val bradycardiaHeartRates = heartRates.filter { 
            it.value <= BRADYCARDIA_THRESHOLD && 
            (it.activityLevel == null || it.activityLevel == "REST" || it.activityLevel == "SEDENTARY") 
        }
        if (bradycardiaHeartRates.isNotEmpty()) {
            abnormalities.add(HeartRateAbnormalityType.BRADYCARDIA to bradycardiaHeartRates)
        }
        
        // Check for exercise tachycardia (heart rate too high during exercise)
        val exerciseTachycardiaHeartRates = heartRates.filter { 
            it.activityLevel == "ACTIVE" && it.value > maxHeartRate * EXERCISE_TACHYCARDIA_MULTIPLIER
        }
        if (exerciseTachycardiaHeartRates.isNotEmpty()) {
            abnormalities.add(HeartRateAbnormalityType.EXERCISE_TACHYCARDIA to exerciseTachycardiaHeartRates)
        }
        
        // Check for irregular rhythm (high variance in short time periods)
        val irregularRhythmHeartRates = detectIrregularRhythm(heartRates)
        if (irregularRhythmHeartRates.isNotEmpty()) {
            abnormalities.add(HeartRateAbnormalityType.IRREGULAR_RHYTHM to irregularRhythmHeartRates)
        }
        
        // Check for sustained elevated heart rate
        val sustainedElevatedHeartRates = detectSustainedElevatedHeartRate(heartRates)
        if (sustainedElevatedHeartRates.isNotEmpty()) {
            abnormalities.add(HeartRateAbnormalityType.SUSTAINED_ELEVATED to sustainedElevatedHeartRates)
        }
        
        // Check for sudden changes in heart rate
        val suddenChangesHeartRates = detectSuddenChanges(heartRates)
        if (suddenChangesHeartRates.isNotEmpty()) {
            abnormalities.add(HeartRateAbnormalityType.SUDDEN_CHANGE to suddenChangesHeartRates)
        }
        
        // Create detection result
        val detection = AbnormalHeartRateDetection(
            userId = userId,
            startDate = startDate,
            endDate = endDate,
            abnormalitiesDetected = abnormalities.isNotEmpty(),
            abnormalities = abnormalities.toMap(),
            analysisTimestamp = LocalDateTime.now()
        )
        
        return Result.Success(detection)
    }

    /**
     * Detect irregular heart rhythm based on variance in short time periods
     * @param heartRates List of heart rate measurements
     * @return List of heart rates indicating irregular rhythm
     */
    private fun detectIrregularRhythm(heartRates: List<HeartRate>): List<HeartRate> {
        if (heartRates.size < 5) {
            return emptyList()
        }
        
        // Group heart rates by time windows (e.g., 5-minute windows)
        val heartRateWindows = heartRates
            .sortedBy { it.timestamp }
            .windowed(5, 1, false)
            .filter { window ->
                // Ensure the window spans no more than 10 minutes
                ChronoUnit.MINUTES.between(window.first().timestamp, window.last().timestamp) <= 10
            }
        
        // Calculate variance in each window
        val irregularWindows = heartRateWindows.filter { window ->
            val values = window.map { it.value }
            val mean = values.average()
            val variance = values.map { (it - mean).pow(2) }.average()
            val stdDev = sqrt(variance)
            
            // Check if standard deviation exceeds threshold
            stdDev > IRREGULAR_RHYTHM_VARIANCE
        }
        
        return irregularWindows.flatten().distinct()
    }

    /**
     * Detect sustained elevated heart rate
     * @param heartRates List of heart rate measurements
     * @return List of heart rates indicating sustained elevation
     */
    private fun detectSustainedElevatedHeartRate(heartRates: List<HeartRate>): List<HeartRate> {
        if (heartRates.size < 3) {
            return emptyList()
        }
        
        // Sort heart rates by timestamp
        val sortedHeartRates = heartRates.sortedBy { it.timestamp }
        
        // Find sequences of elevated heart rates
        val elevatedSequences = mutableListOf<List<HeartRate>>()
        var currentSequence = mutableListOf<HeartRate>()
        
        for (heartRate in sortedHeartRates) {
            if (heartRate.value >= TACHYCARDIA_THRESHOLD && 
                (heartRate.activityLevel == null || heartRate.activityLevel == "REST" || heartRate.activityLevel == "SEDENTARY")) {
                currentSequence.add(heartRate)
            } else {
                if (currentSequence.size >= 3) {
                    elevatedSequences.add(currentSequence.toList())
                }
                currentSequence.clear()
            }
        }
        
        // Add the last sequence if it's significant
        if (currentSequence.size >= 3) {
            elevatedSequences.add(currentSequence.toList())
        }
        
        // Filter for sequences that span at least SUSTAINED_ELEVATED_MINUTES
        return elevatedSequences
            .filter { sequence ->
                val durationMinutes = ChronoUnit.MINUTES.between(
                    sequence.first().timestamp,
                    sequence.last().timestamp
                )
                durationMinutes >= SUSTAINED_ELEVATED_MINUTES
            }
            .flatten()
    }

    /**
     * Detect sudden changes in heart rate
     * @param heartRates List of heart rate measurements
     * @return List of heart rates indicating sudden changes
     */
    private fun detectSuddenChanges(heartRates: List<HeartRate>): List<HeartRate> {
        if (heartRates.size < 2) {
            return emptyList()
        }
        
        // Sort heart rates by timestamp
        val sortedHeartRates = heartRates.sortedBy { it.timestamp }
        
        // Find pairs with sudden changes
        val suddenChanges = mutableListOf<HeartRate>()
        
        for (i in 1 until sortedHeartRates.size) {
            val prev = sortedHeartRates[i - 1]
            val current = sortedHeartRates[i]
            
            // Check if measurements are close in time (within 10 minutes)
            val minutesBetween = ChronoUnit.MINUTES.between(prev.timestamp, current.timestamp)
            if (minutesBetween <= 10) {
                // Check for significant change
                val change = Math.abs(current.value - prev.value)
                if (change >= SUDDEN_CHANGE_THRESHOLD) {
                    suddenChanges.add(prev)
                    suddenChanges.add(current)
                }
            }
        }
        
        return suddenChanges.distinct()
    }
}

/**
 * Analyze heart rate variability
 *
 * This use case performs detailed analysis of heart rate variability (HRV)
 * data, including time-domain and frequency-domain metrics that can indicate
 * autonomic nervous system health.
 */
class AnalyzeHeartRateVariability @Inject constructor(
    private val healthDataRepository: HealthDataRepository
) {
    /**
     * Analyze heart rate variability for a user within a date range
     * @param userId User ID
     * @param startDate Start date for analysis
     * @param endDate End date for analysis
     * @return Result containing HRV analysis or an error
     */
    suspend operator fun invoke(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<HeartRateVariabilityAnalysis> {
        if (endDate.isBefore(startDate)) {
            return Result.Error(IllegalArgumentException("End date cannot be before start date"))
        }
        
        // Get heart rate data with HRV values
        val heartRatesResult = healthDataRepository.getHeartRatesForUser(userId, startDate, endDate)
        if (heartRatesResult is Result.Error) {
            return Result.Error(heartRatesResult.exception)
        }
        
        val heartRates = (heartRatesResult as Result.Success).data
        
        // Filter heart rates with HRV values
        val heartRatesWithHrv = heartRates.filter { it.hrvValue != null }
        
        if (heartRatesWithHrv.isEmpty()) {
            return Result.Error(IllegalStateException("No heart rate variability data available for analysis"))
        }
        
        // Calculate HRV metrics
        val hrvValues = heartRatesWithHrv.mapNotNull { it.hrvValue }
        
        // Time-domain metrics
        val meanNN = hrvValues.average()
        val sdnn = calculateStandardDeviation(hrvValues)
        
        // Calculate RMSSD (Root Mean Square of Successive Differences)
        val rmssd = calculateRmssd(heartRatesWithHrv)
        
        // Calculate pNN50 (percentage of successive NN intervals that differ by more than 50 ms)
        val pnn50 = calculatePnn50(heartRatesWithHrv)
        
        // Analyze HRV trends
        val dailyHrvTrend = analyzeDailyHrvTrend(heartRatesWithHrv)
        
        // Create analysis result
        val analysis = HeartRateVariabilityAnalysis(
            userId = userId,
            startDate = startDate,
            endDate = endDate,
            meanNN = meanNN,
            sdnn = sdnn,
            rmssd = rmssd,
            pnn50 = pnn50,
            sampleCount = heartRatesWithHrv.size,
            dailyAverages = dailyHrvTrend,
            analysisTimestamp = LocalDateTime.now()
        )
        
        return Result.Success(analysis)
    }

    /**
     * Calculate standard deviation
     * @param values List of numeric values
     * @return Standard deviation
     */
    private fun calculateStandardDeviation(values: List<Int>): Double {
        if (values.isEmpty()) return 0.0
        
        val mean = values.average()
        val variance = values.map { (it - mean).pow(2) }.average()
        
        return sqrt(variance)
    }

    /**
     * Calculate RMSSD (Root Mean Square of Successive Differences)
     * @param heartRates List of heart rate measurements with HRV values
     * @return RMSSD value
     */
    private fun calculateRmssd(heartRates: List<HeartRate>): Double {
        if (heartRates.size < 2) return 0.0
        
        // Sort by timestamp
        val sortedHeartRates = heartRates
            .filter { it.hrvValue != null }
            .sortedBy { it.timestamp }
        
        // Calculate successive differences
        val successiveDifferences = mutableListOf<Double>()
        
        for (i in 1 until sortedHeartRates.size) {
            val prev = sortedHeartRates[i - 1].hrvValue!!
            val current = sortedHeartRates[i].hrvValue!!
            
            // Only include pairs that are close in time (within 5 minutes)
            val minutesBetween = ChronoUnit.MINUTES.between(
                sortedHeartRates[i - 1].timestamp,
                sortedHeartRates[i].timestamp
            )
            
            if (minutesBetween <= 5) {
                successiveDifferences.add((current - prev).toDouble().pow(2))
            }
        }
        
        if (successiveDifferences.isEmpty()) return 0.0
        
        // Calculate RMSSD
        return sqrt(successiveDifferences.average())
    }

    /**
     * Calculate pNN50 (percentage of successive NN intervals that differ by more than 50 ms)
     * @param heartRates List of heart rate measurements with HRV values
     * @return pNN50 value
     */
    private fun calculatePnn50(heartRates: List<HeartRate>): Double {
        if (heartRates.size < 2) return 0.0
        
        // Sort by timestamp
        val sortedHeartRates = heartRates
            .filter { it.hrvValue != null }
            .sortedBy { it.timestamp }
        
        // Count intervals with difference > 50ms
        var nn50Count = 0
        var totalIntervals = 0
        
        for (i in 1 until sortedHeartRates.size) {
            val prev = sortedHeartRates[i - 1].hrvValue!!
            val current = sortedHeartRates[i].hrvValue!!
            
            // Only include pairs that are close in time (within 5 minutes)
            val minutesBetween = ChronoUnit.MINUTES.between(
                sortedHeartRates[i - 1].timestamp,
                sortedHeartRates[i].timestamp
            )
            
            if (minutesBetween <= 5) {
                totalIntervals++
                
                if (Math.abs(current - prev) > 50) {
                    nn50Count++
                }
            }
        }
        
        if (totalIntervals == 0) return 0.0
        
        // Calculate pNN50
        return (nn50Count.toDouble() / totalIntervals) * 100
    }

    /**
     * Analyze daily HRV trend
     * @param heartRates List of heart rate measurements with HRV values
     * @return Map of date to average HRV value
     */
    private fun analyzeDailyHrvTrend(heartRates: List<HeartRate>): Map<LocalDate, Double> {
        // Group by date
        return heartRates
            .filter { it.hrvValue != null }
            .groupBy { it.timestamp.toLocalDate() }
            .mapValues { (_, values) ->
                values.mapNotNull { it.hrvValue }.average()
            }
    }
}

/**
 * Get heart rate trends over time
 *
 * This use case analyzes heart rate data to identify trends over time,
 * including resting heart rate trends, exercise recovery patterns,
 * and circadian rhythm analysis.
 */
class GetHeartRateTrends @Inject constructor(
    private val healthDataRepository: HealthDataRepository
) {
    /**
     * Get heart rate trends for a user within a date range
     * @param userId User ID
     * @param startDate Start date for analysis
     * @param endDate End date for analysis
     * @return Result containing heart rate trends or an error
     */
    suspend operator fun invoke(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<HeartRateTrend> {
        if (endDate.isBefore(startDate)) {
            return Result.Error(IllegalArgumentException("End date cannot be before start date"))
        }
        
        // Get heart rate data
        val heartRatesResult = healthDataRepository.getHeartRatesForUser(userId, startDate, endDate)
        if (heartRatesResult is Result.Error) {
            return Result.Error(heartRatesResult.exception)
        }
        
        val heartRates = (heartRatesResult as Result.Success).data
        if (heartRates.isEmpty()) {
            return Result.Error(IllegalStateException("No heart rate data available for trend analysis"))
        }
        
        // Analyze resting heart rate trend
        val restingHeartRateTrend = analyzeRestingHeartRateTrend(heartRates)
        
        // Analyze daily average heart rate trend
        val dailyAverageHeartRateTrend = analyzeDailyAverageHeartRateTrend(heartRates)
        
        // Analyze heart rate recovery trend
        val recoveryTrend = analyzeHeartRateRecoveryTrend(heartRates)
        
        // Analyze circadian rhythm
        val circadianRhythm = analyzeCircadianRhythm(heartRates)
        
        // Create trend result
        val trend = HeartRateTrend(
            userId = userId,
            startDate = startDate,
            endDate = endDate,
            restingHeartRateTrend = restingHeartRateTrend,
            dailyAverageHeartRateTrend = dailyAverageHeartRateTrend,
            recoveryTrend = recoveryTrend,
            circadianRhythm = circadianRhythm,
            analysisTimestamp = LocalDateTime.now()
        )
        
        return Result.Success(trend)
    }

    /**
     * Analyze resting heart rate trend
     * @param heartRates List of heart rate measurements
     * @return Map of date to resting heart rate
     */
    private fun analyzeRestingHeartRateTrend(heartRates: List<HeartRate>): Map<LocalDate, Int> {
        // Group heart rates by date
        return heartRates
            .filter { it.isRestingHeartRate }
            .groupBy { it.timestamp.toLocalDate() }
            .mapValues { (_, values) ->
                // If multiple resting heart rates for a day, take the average
                values.map { it.value }.average().toInt()
            }
    }

    /**
     * Analyze daily average heart rate trend
     * @param heartRates List of heart rate measurements
     * @return Map of date to average heart rate
     */
    private fun analyzeDailyAverageHeartRateTrend(heartRates: List<HeartRate>): Map<LocalDate, Int> {
        // Group heart rates by date
        return heartRates
            .groupBy { it.timestamp.toLocalDate() }
            .mapValues { (_, values) ->
                values.map { it.value }.average().toInt()
            }
    }

    /**
     * Analyze heart rate recovery trend
     * @param heartRates List of heart rate measurements
     * @return Map of date to recovery rate (bpm/minute)
     */
    private fun analyzeHeartRateRecoveryTrend(heartRates: List<HeartRate>): Map<LocalDate, Double> {
        val recoveryRates = mutableMapOf<LocalDate, Double>()
        
        // Group heart rates by date
        val heartRatesByDate = heartRates.groupBy { it.timestamp.toLocalDate() }
        
        // For each day, find active periods followed by recovery periods
        for ((date, dailyHeartRates) in heartRatesByDate) {
            val sortedHeartRates = dailyHeartRates.sortedBy { it.timestamp }
            
            // Find peaks (potential exercise end points)
            val peaks = findHeartRatePeaks(sortedHeartRates)
            
            // Calculate recovery rates for each peak
            val dailyRecoveryRates = mutableListOf<Double>()
            
            for (peak in peaks) {
                val peakIndex = sortedHeartRates.indexOf(peak)
                if (peakIndex >= 0 && peakIndex < sortedHeartRates.size - 1) {
                    // Look at heart rates after the peak
                    val postPeakHeartRates = sortedHeartRates.subList(peakIndex, sortedHeartRates.size)
                    
                    // Find recovery period (up to 10 minutes after peak)
                    val recoveryPeriod = postPeakHeartRates.takeWhile { hr ->
                        ChronoUnit.MINUTES.between(peak.timestamp, hr.timestamp) <= 10
                    }
                    
                    if (recoveryPeriod.size >= 3) {
                        // Calculate recovery rate (bpm/minute)
                        val initialValue = recoveryPeriod.first().value
                        val finalValue = recoveryPeriod.last().value
                        val minutesElapsed = ChronoUnit.MINUTES.between(
                            recoveryPeriod.first().timestamp,
                            recoveryPeriod.last().timestamp
                        ).toDouble().coerceAtLeast(1.0) // Avoid division by zero
                        
                        val recoveryRate = (initialValue - finalValue) / minutesElapsed
                        dailyRecoveryRates.add(recoveryRate)
                    }
                }
            }
            
            // Store average recovery rate for the day
            if (dailyRecoveryRates.isNotEmpty()) {
                recoveryRates[date] = dailyRecoveryRates.average()
            }
        }
        
        return recoveryRates
    }

    /**
     * Find heart rate peaks (potential exercise end points)
     * @param heartRates List of heart rate measurements sorted by timestamp
     * @return List of heart rate peaks
     */
    private fun findHeartRatePeaks(heartRates: List<HeartRate>): List<HeartRate> {
        if (heartRates.size < 3) return emptyList()
        
        val peaks = mutableListOf<HeartRate>()
        
        for (i in 1 until heartRates.size - 1) {
            val prev = heartRates[i - 1]
            val current = heartRates[i]
            val next = heartRates[i + 1]
            
            // Check if current is a peak
            if (current.value > prev.value && current.value > next.value && current.value >= 100) {
                peaks.add(current)
            }
        }
        
        return peaks
    }

    /**
     * Analyze circadian rhythm of heart rate
     * @param heartRates List of heart rate measurements
     * @return Map of hour of day to average heart rate
     */
    private fun analyzeCircadianRhythm(heartRates: List<HeartRate>): Map<Int, Int> {
        // Group heart rates by hour of day
        return heartRates
            .groupBy { it.timestamp.hour }
            .mapValues { (_, values) ->
                values.map { it.value }.average().toInt()
            }
    }
}
