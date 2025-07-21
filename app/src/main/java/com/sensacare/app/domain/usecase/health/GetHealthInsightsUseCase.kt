package com.sensacare.app.domain.usecase.health

import com.sensacare.app.domain.model.*
import com.sensacare.app.domain.repository.HealthDataRepository
import com.sensacare.app.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * GetHealthInsightsUseCase - Generates comprehensive health insights from user data
 *
 * This use case analyzes health data across multiple metrics to generate AI-powered insights,
 * identify trends, detect anomalies, assess health risks, and provide personalized recommendations.
 * It implements sophisticated analysis algorithms while maintaining medical accuracy with appropriate
 * disclaimers.
 *
 * Key features:
 * - Multi-metric correlation analysis
 * - Trend detection across various time periods
 * - Anomaly detection with clinical thresholds
 * - Risk assessment based on medical guidelines
 * - Goal achievement tracking
 * - Personalized recommendations
 * - Health scoring with detailed breakdowns
 * - Performance optimization with caching
 */
class GetHealthInsightsUseCase(
    private val healthDataRepository: HealthDataRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    companion object {
        // Cache configuration
        private const val CACHE_EXPIRY_MINUTES = 30L
        private const val MAX_CACHE_ENTRIES = 10

        // Analysis thresholds
        private const val SIGNIFICANT_HEART_RATE_CHANGE = 10 // bpm
        private const val SIGNIFICANT_BLOOD_OXYGEN_CHANGE = 3 // percentage points
        private const val SIGNIFICANT_BLOOD_PRESSURE_CHANGE_SYSTOLIC = 10 // mmHg
        private const val SIGNIFICANT_BLOOD_PRESSURE_CHANGE_DIASTOLIC = 5 // mmHg
        private const val SIGNIFICANT_SLEEP_DURATION_CHANGE = 60 // minutes
        private const val SIGNIFICANT_STEP_COUNT_CHANGE = 2000 // steps
        private const val SIGNIFICANT_TEMPERATURE_CHANGE = 0.5 // degrees

        // Health risk thresholds
        private const val HIGH_HEART_RATE_THRESHOLD = 100 // bpm
        private const val LOW_HEART_RATE_THRESHOLD = 50 // bpm
        private const val LOW_BLOOD_OXYGEN_THRESHOLD = 95 // percentage
        private const val HIGH_SYSTOLIC_THRESHOLD = 140 // mmHg
        private const val HIGH_DIASTOLIC_THRESHOLD = 90 // mmHg
        private const val LOW_SLEEP_DURATION_THRESHOLD = 6 * 60 // minutes
        private const val HIGH_TEMPERATURE_THRESHOLD = 37.5 // degrees Celsius

        // Analysis periods
        private val DAILY_ANALYSIS_PERIOD = Duration.ofDays(1)
        private val WEEKLY_ANALYSIS_PERIOD = Duration.ofDays(7)
        private val MONTHLY_ANALYSIS_PERIOD = Duration.ofDays(30)
        private val QUARTERLY_ANALYSIS_PERIOD = Duration.ofDays(90)

        // Medical disclaimer
        private const val MEDICAL_DISCLAIMER = "The health insights provided are for informational purposes only " +
                "and do not constitute medical advice. Always consult with a healthcare professional before making " +
                "any health-related decisions or changes to your health regimen."
    }

    // Simple cache implementation
    private val insightsCache = LruCache<CacheKey, CacheEntry>(MAX_CACHE_ENTRIES)

    /**
     * Invoke operator to call the use case as a function
     *
     * @param userId The ID of the user to generate insights for
     * @param timeRange The time range to analyze
     * @param categories The categories of insights to generate (all by default)
     * @param forceRefresh Whether to force a refresh of the insights
     * @return Flow of HealthInsightsResult with loading state and insights
     */
    operator fun invoke(
        userId: Long,
        timeRange: TimeRange = TimeRange.MONTH,
        categories: Set<InsightCategory> = InsightCategory.values().toSet(),
        forceRefresh: Boolean = false
    ): Flow<HealthInsightsResult> = flow {
        emit(HealthInsightsResult.Loading)

        // Check cache first (if not forcing refresh)
        val cacheKey = CacheKey(userId, timeRange, categories)
        if (!forceRefresh) {
            val cachedInsights = insightsCache.get(cacheKey)
            if (cachedInsights != null && !cachedInsights.isExpired()) {
                Timber.d("Using cached insights for user $userId")
                emit(HealthInsightsResult.Success(cachedInsights.insights, true))
                return@flow
            }
        }

        Timber.d("Generating fresh insights for user $userId")

        try {
            // Get user preferences for personalized analysis
            val userPreferences = userPreferencesRepository.getUserPreferences(userId).first()
            
            // Calculate date range based on timeRange
            val (startDate, endDate) = calculateDateRange(timeRange)
            
            // Collect all required health data
            val healthData = collectHealthData(userId, startDate, endDate)
            
            // Generate insights
            val insights = generateInsights(
                userId = userId,
                healthData = healthData,
                userPreferences = userPreferences,
                timeRange = timeRange,
                categories = categories
            )
            
            // Cache the results
            insightsCache.put(cacheKey, CacheEntry(insights))
            
            // Emit success result
            emit(HealthInsightsResult.Success(insights, false))
            
        } catch (e: Exception) {
            Timber.e(e, "Error generating health insights")
            emit(HealthInsightsResult.Error(
                InsightError.ProcessingError("Failed to generate insights: ${e.message}")
            ))
        }
    }.flowOn(dispatcher)

    /**
     * Calculates the date range based on the specified time range
     */
    private fun calculateDateRange(timeRange: TimeRange): Pair<LocalDateTime, LocalDateTime> {
        val endDate = LocalDateTime.now()
        val startDate = when (timeRange) {
            TimeRange.DAY -> endDate.minusDays(1)
            TimeRange.WEEK -> endDate.minusDays(7)
            TimeRange.MONTH -> endDate.minusDays(30)
            TimeRange.QUARTER -> endDate.minusDays(90)
            TimeRange.YEAR -> endDate.minusDays(365)
            is TimeRange.CUSTOM -> timeRange.startDate
        }
        return Pair(startDate, endDate)
    }

    /**
     * Collects all health data needed for analysis
     */
    private suspend fun collectHealthData(
        userId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): HealthDataSet = withContext(dispatcher) {
        Timber.d("Collecting health data for user $userId from $startDate to $endDate")
        
        // Collect all data in parallel for efficiency
        return@withContext HealthDataSet(
            heartRate = healthDataRepository.getHeartRateMeasurements(
                userId = userId,
                startTime = startDate,
                endTime = endDate
            ).first(),
            
            bloodOxygen = healthDataRepository.getBloodOxygenMeasurements(
                userId = userId,
                startTime = startDate,
                endTime = endDate
            ).first(),
            
            bloodPressure = healthDataRepository.getBloodPressureMeasurements(
                userId = userId,
                startTime = startDate,
                endTime = endDate
            ).first(),
            
            sleep = healthDataRepository.getSleepRecords(
                userId = userId,
                startTime = startDate,
                endTime = endDate
            ).first(),
            
            steps = healthDataRepository.getStepRecords(
                userId = userId,
                startTime = startDate,
                endTime = endDate
            ).first(),
            
            activities = healthDataRepository.getActivityRecords(
                userId = userId,
                startTime = startDate,
                endTime = endDate
            ).first(),
            
            temperature = healthDataRepository.getTemperatureMeasurements(
                userId = userId,
                startTime = startDate,
                endTime = endDate
            ).first()
        )
    }

    /**
     * Generates insights from health data
     */
    private suspend fun generateInsights(
        userId: Long,
        healthData: HealthDataSet,
        userPreferences: UserPreferences,
        timeRange: TimeRange,
        categories: Set<InsightCategory>
    ): HealthInsights = withContext(dispatcher) {
        val insights = mutableListOf<HealthInsight>()
        val (startDate, endDate) = calculateDateRange(timeRange)
        
        // Generate insights based on requested categories
        if (InsightCategory.TRENDS in categories) {
            insights.addAll(generateTrendInsights(healthData, timeRange))
        }
        
        if (InsightCategory.ANOMALIES in categories) {
            insights.addAll(generateAnomalyInsights(healthData))
        }
        
        if (InsightCategory.CORRELATIONS in categories) {
            insights.addAll(generateCorrelationInsights(healthData))
        }
        
        if (InsightCategory.RISK_ASSESSMENT in categories) {
            insights.addAll(generateRiskAssessmentInsights(healthData, userPreferences))
        }
        
        if (InsightCategory.RECOMMENDATIONS in categories) {
            insights.addAll(generateRecommendationInsights(healthData, userPreferences))
        }
        
        if (InsightCategory.GOALS in categories) {
            insights.addAll(generateGoalInsights(userId, healthData, userPreferences))
        }
        
        // Calculate health scores
        val healthScore = calculateHealthScore(healthData, userPreferences)
        
        return@withContext HealthInsights(
            userId = userId,
            generatedAt = LocalDateTime.now(),
            timeRange = timeRange,
            startDate = startDate,
            endDate = endDate,
            insights = insights,
            healthScore = healthScore,
            disclaimer = MEDICAL_DISCLAIMER
        )
    }

    /**
     * Generates trend insights by analyzing changes over time
     */
    private fun generateTrendInsights(
        healthData: HealthDataSet,
        timeRange: TimeRange
    ): List<TrendInsight> {
        val insights = mutableListOf<TrendInsight>()
        
        // Heart Rate trends
        if (healthData.heartRate.isNotEmpty()) {
            val heartRateTrend = analyzeTrend(
                healthData.heartRate,
                { it.value.toDouble() },
                { it.timestamp },
                SIGNIFICANT_HEART_RATE_CHANGE.toDouble()
            )
            
            if (heartRateTrend != TrendDirection.STABLE) {
                insights.add(
                    TrendInsight(
                        id = generateInsightId(),
                        title = "Heart Rate ${heartRateTrend.description} Trend",
                        description = "Your average heart rate has been ${heartRateTrend.description.lowercase()} " +
                                "over the ${timeRange.description.lowercase()}. " +
                                getHeartRateTrendDescription(heartRateTrend, healthData.heartRate),
                        severity = when (heartRateTrend) {
                            TrendDirection.INCREASING -> InsightSeverity.MODERATE
                            TrendDirection.DECREASING -> InsightSeverity.MODERATE
                            else -> InsightSeverity.LOW
                        },
                        category = InsightCategory.TRENDS,
                        metricType = MetricType.HEART_RATE,
                        trendDirection = heartRateTrend,
                        percentageChange = calculatePercentageChange(healthData.heartRate) { it.value.toDouble() },
                        relatedInsightIds = emptyList()
                    )
                )
            }
        }
        
        // Blood Oxygen trends
        if (healthData.bloodOxygen.isNotEmpty()) {
            val bloodOxygenTrend = analyzeTrend(
                healthData.bloodOxygen,
                { it.percentage.toDouble() },
                { it.timestamp },
                SIGNIFICANT_BLOOD_OXYGEN_CHANGE.toDouble()
            )
            
            if (bloodOxygenTrend != TrendDirection.STABLE) {
                val severity = when {
                    bloodOxygenTrend == TrendDirection.DECREASING && 
                    healthData.bloodOxygen.any { it.percentage < LOW_BLOOD_OXYGEN_THRESHOLD } -> 
                        InsightSeverity.HIGH
                    bloodOxygenTrend == TrendDirection.DECREASING -> 
                        InsightSeverity.MODERATE
                    else -> 
                        InsightSeverity.LOW
                }
                
                insights.add(
                    TrendInsight(
                        id = generateInsightId(),
                        title = "Blood Oxygen ${bloodOxygenTrend.description} Trend",
                        description = "Your average blood oxygen level has been ${bloodOxygenTrend.description.lowercase()} " +
                                "over the ${timeRange.description.lowercase()}. " +
                                getBloodOxygenTrendDescription(bloodOxygenTrend, healthData.bloodOxygen),
                        severity = severity,
                        category = InsightCategory.TRENDS,
                        metricType = MetricType.BLOOD_OXYGEN,
                        trendDirection = bloodOxygenTrend,
                        percentageChange = calculatePercentageChange(healthData.bloodOxygen) { it.percentage.toDouble() },
                        relatedInsightIds = emptyList()
                    )
                )
            }
        }
        
        // Blood Pressure trends
        if (healthData.bloodPressure.isNotEmpty()) {
            val systolicTrend = analyzeTrend(
                healthData.bloodPressure,
                { it.systolic.toDouble() },
                { it.timestamp },
                SIGNIFICANT_BLOOD_PRESSURE_CHANGE_SYSTOLIC.toDouble()
            )
            
            val diastolicTrend = analyzeTrend(
                healthData.bloodPressure,
                { it.diastolic.toDouble() },
                { it.timestamp },
                SIGNIFICANT_BLOOD_PRESSURE_CHANGE_DIASTOLIC.toDouble()
            )
            
            if (systolicTrend != TrendDirection.STABLE || diastolicTrend != TrendDirection.STABLE) {
                val severity = when {
                    (systolicTrend == TrendDirection.INCREASING && 
                     healthData.bloodPressure.any { it.systolic > HIGH_SYSTOLIC_THRESHOLD }) ||
                    (diastolicTrend == TrendDirection.INCREASING && 
                     healthData.bloodPressure.any { it.diastolic > HIGH_DIASTOLIC_THRESHOLD }) -> 
                        InsightSeverity.HIGH
                    systolicTrend == TrendDirection.INCREASING || diastolicTrend == TrendDirection.INCREASING -> 
                        InsightSeverity.MODERATE
                    else -> 
                        InsightSeverity.LOW
                }
                
                insights.add(
                    TrendInsight(
                        id = generateInsightId(),
                        title = "Blood Pressure Trend",
                        description = "Your blood pressure has been showing changes " +
                                "over the ${timeRange.description.lowercase()}. " +
                                getBloodPressureTrendDescription(systolicTrend, diastolicTrend, healthData.bloodPressure),
                        severity = severity,
                        category = InsightCategory.TRENDS,
                        metricType = MetricType.BLOOD_PRESSURE,
                        trendDirection = if (systolicTrend == TrendDirection.INCREASING || 
                                             diastolicTrend == TrendDirection.INCREASING) 
                                          TrendDirection.INCREASING else TrendDirection.DECREASING,
                        percentageChange = calculatePercentageChange(healthData.bloodPressure) { 
                            (it.systolic + it.diastolic) / 2.0 
                        },
                        relatedInsightIds = emptyList()
                    )
                )
            }
        }
        
        // Sleep trends
        if (healthData.sleep.isNotEmpty()) {
            val sleepDurationTrend = analyzeTrend(
                healthData.sleep,
                { it.durationMinutes.toDouble() },
                { it.startTime },
                SIGNIFICANT_SLEEP_DURATION_CHANGE.toDouble()
            )
            
            if (sleepDurationTrend != TrendDirection.STABLE) {
                val severity = when {
                    sleepDurationTrend == TrendDirection.DECREASING && 
                    healthData.sleep.any { it.durationMinutes < LOW_SLEEP_DURATION_THRESHOLD } -> 
                        InsightSeverity.HIGH
                    sleepDurationTrend == TrendDirection.DECREASING -> 
                        InsightSeverity.MODERATE
                    else -> 
                        InsightSeverity.LOW
                }
                
                insights.add(
                    TrendInsight(
                        id = generateInsightId(),
                        title = "Sleep Duration ${sleepDurationTrend.description} Trend",
                        description = "Your average sleep duration has been ${sleepDurationTrend.description.lowercase()} " +
                                "over the ${timeRange.description.lowercase()}. " +
                                getSleepTrendDescription(sleepDurationTrend, healthData.sleep),
                        severity = severity,
                        category = InsightCategory.TRENDS,
                        metricType = MetricType.SLEEP,
                        trendDirection = sleepDurationTrend,
                        percentageChange = calculatePercentageChange(healthData.sleep) { it.durationMinutes.toDouble() },
                        relatedInsightIds = emptyList()
                    )
                )
            }
        }
        
        // Steps trends
        if (healthData.steps.isNotEmpty()) {
            val stepsTrend = analyzeTrend(
                healthData.steps,
                { it.count.toDouble() },
                { it.date.atStartOfDay() },
                SIGNIFICANT_STEP_COUNT_CHANGE.toDouble()
            )
            
            if (stepsTrend != TrendDirection.STABLE) {
                insights.add(
                    TrendInsight(
                        id = generateInsightId(),
                        title = "Step Count ${stepsTrend.description} Trend",
                        description = "Your daily step count has been ${stepsTrend.description.lowercase()} " +
                                "over the ${timeRange.description.lowercase()}. " +
                                getStepsTrendDescription(stepsTrend, healthData.steps),
                        severity = when (stepsTrend) {
                            TrendDirection.INCREASING -> InsightSeverity.LOW
                            TrendDirection.DECREASING -> InsightSeverity.MODERATE
                            else -> InsightSeverity.LOW
                        },
                        category = InsightCategory.TRENDS,
                        metricType = MetricType.STEPS,
                        trendDirection = stepsTrend,
                        percentageChange = calculatePercentageChange(healthData.steps) { it.count.toDouble() },
                        relatedInsightIds = emptyList()
                    )
                )
            }
        }
        
        return insights
    }

    /**
     * Generates anomaly insights by detecting outliers in health data
     */
    private fun generateAnomalyInsights(healthData: HealthDataSet): List<AnomalyInsight> {
        val insights = mutableListOf<AnomalyInsight>()
        
        // Heart Rate anomalies
        if (healthData.heartRate.isNotEmpty()) {
            val heartRateAnomalies = detectAnomalies(
                healthData.heartRate,
                { it.value.toDouble() },
                { it.timestamp }
            )
            
            if (heartRateAnomalies.isNotEmpty()) {
                // Group anomalies by day to avoid too many insights
                val groupedAnomalies = heartRateAnomalies.groupBy { 
                    it.timestamp.toLocalDate() 
                }
                
                for ((date, anomalies) in groupedAnomalies) {
                    // Only report significant anomalies
                    val significantAnomalies = anomalies.filter { 
                        it.value > HIGH_HEART_RATE_THRESHOLD || it.value < LOW_HEART_RATE_THRESHOLD 
                    }
                    
                    if (significantAnomalies.isNotEmpty()) {
                        val maxHeartRate = significantAnomalies.maxByOrNull { it.value }
                        val minHeartRate = significantAnomalies.minByOrNull { it.value }
                        
                        val severity = when {
                            significantAnomalies.any { it.value > HIGH_HEART_RATE_THRESHOLD * 1.2 || 
                                                      it.value < LOW_HEART_RATE_THRESHOLD * 0.8 } -> 
                                InsightSeverity.HIGH
                            significantAnomalies.any { it.value > HIGH_HEART_RATE_THRESHOLD || 
                                                      it.value < LOW_HEART_RATE_THRESHOLD } -> 
                                InsightSeverity.MODERATE
                            else -> 
                                InsightSeverity.LOW
                        }
                        
                        insights.add(
                            AnomalyInsight(
                                id = generateInsightId(),
                                title = "Unusual Heart Rate Detected",
                                description = "On ${date}, your heart rate showed unusual patterns. " +
                                        "Your heart rate reached as high as ${maxHeartRate?.value ?: "N/A"} bpm " +
                                        "and as low as ${minHeartRate?.value ?: "N/A"} bpm. " +
                                        getHeartRateAnomalyDescription(significantAnomalies),
                                severity = severity,
                                category = InsightCategory.ANOMALIES,
                                metricType = MetricType.HEART_RATE,
                                anomalyType = AnomalyType.OUTLIER,
                                detectedAt = date.atStartOfDay(),
                                affectedValue = if (significantAnomalies.any { it.value > HIGH_HEART_RATE_THRESHOLD })
                                                maxHeartRate?.value?.toDouble() ?: 0.0
                                            else
                                                minHeartRate?.value?.toDouble() ?: 0.0,
                                expectedRange = Pair(LOW_HEART_RATE_THRESHOLD.toDouble(), HIGH_HEART_RATE_THRESHOLD.toDouble()),
                                relatedInsightIds = emptyList()
                            )
                        )
                    }
                }
            }
        }
        
        // Blood Oxygen anomalies
        if (healthData.bloodOxygen.isNotEmpty()) {
            val bloodOxygenAnomalies = detectAnomalies(
                healthData.bloodOxygen,
                { it.percentage.toDouble() },
                { it.timestamp }
            )
            
            if (bloodOxygenAnomalies.isNotEmpty()) {
                // Group anomalies by day
                val groupedAnomalies = bloodOxygenAnomalies.groupBy { 
                    it.timestamp.toLocalDate() 
                }
                
                for ((date, anomalies) in groupedAnomalies) {
                    // Only report low oxygen levels as they're clinically significant
                    val lowOxygenAnomalies = anomalies.filter { 
                        it.percentage < LOW_BLOOD_OXYGEN_THRESHOLD 
                    }
                    
                    if (lowOxygenAnomalies.isNotEmpty()) {
                        val lowestOxygen = lowOxygenAnomalies.minByOrNull { it.percentage }
                        
                        val severity = when {
                            lowOxygenAnomalies.any { it.percentage < 90 } -> InsightSeverity.CRITICAL
                            lowOxygenAnomalies.any { it.percentage < 92 } -> InsightSeverity.HIGH
                            else -> InsightSeverity.MODERATE
                        }
                        
                        insights.add(
                            AnomalyInsight(
                                id = generateInsightId(),
                                title = "Low Blood Oxygen Detected",
                                description = "On ${date}, your blood oxygen level dropped below the normal range. " +
                                        "Your lowest recorded level was ${lowestOxygen?.percentage ?: "N/A"}%. " +
                                        getBloodOxygenAnomalyDescription(lowOxygenAnomalies),
                                severity = severity,
                                category = InsightCategory.ANOMALIES,
                                metricType = MetricType.BLOOD_OXYGEN,
                                anomalyType = AnomalyType.BELOW_THRESHOLD,
                                detectedAt = date.atStartOfDay(),
                                affectedValue = lowestOxygen?.percentage?.toDouble() ?: 0.0,
                                expectedRange = Pair(LOW_BLOOD_OXYGEN_THRESHOLD.toDouble(), 100.0),
                                relatedInsightIds = emptyList()
                            )
                        )
                    }
                }
            }
        }
        
        // Blood Pressure anomalies
        if (healthData.bloodPressure.isNotEmpty()) {
            val bloodPressureAnomalies = detectAnomalies(
                healthData.bloodPressure,
                { (it.systolic + it.diastolic) / 2.0 },
                { it.timestamp }
            )
            
            if (bloodPressureAnomalies.isNotEmpty()) {
                // Group anomalies by day
                val groupedAnomalies = bloodPressureAnomalies.groupBy { 
                    it.timestamp.toLocalDate() 
                }
                
                for ((date, anomalies) in groupedAnomalies) {
                    // Filter for high blood pressure readings
                    val highBpAnomalies = anomalies.filter { 
                        it.systolic > HIGH_SYSTOLIC_THRESHOLD || it.diastolic > HIGH_DIASTOLIC_THRESHOLD 
                    }
                    
                    if (highBpAnomalies.isNotEmpty()) {
                        val highestReading = highBpAnomalies.maxByOrNull { 
                            it.systolic + it.diastolic 
                        }
                        
                        val severity = when {
                            highBpAnomalies.any { it.systolic >= 180 || it.diastolic >= 120 } -> 
                                InsightSeverity.CRITICAL // Hypertensive crisis
                            highBpAnomalies.any { it.systolic >= 160 || it.diastolic >= 100 } -> 
                                InsightSeverity.HIGH // Stage 2 hypertension
                            else -> 
                                InsightSeverity.MODERATE // Stage 1 hypertension
                        }
                        
                        insights.add(
                            AnomalyInsight(
                                id = generateInsightId(),
                                title = "Elevated Blood Pressure Detected",
                                description = "On ${date}, your blood pressure was above the normal range. " +
                                        "Your highest reading was ${highestReading?.systolic}/${highestReading?.diastolic} mmHg. " +
                                        getBloodPressureAnomalyDescription(highBpAnomalies),
                                severity = severity,
                                category = InsightCategory.ANOMALIES,
                                metricType = MetricType.BLOOD_PRESSURE,
                                anomalyType = AnomalyType.ABOVE_THRESHOLD,
                                detectedAt = date.atStartOfDay(),
                                affectedValue = highestReading?.let { it.systolic.toDouble() } ?: 0.0,
                                expectedRange = Pair(0.0, HIGH_SYSTOLIC_THRESHOLD.toDouble()),
                                relatedInsightIds = emptyList()
                            )
                        )
                    }
                }
            }
        }
        
        // Sleep anomalies
        if (healthData.sleep.isNotEmpty()) {
            val sleepAnomalies = detectAnomalies(
                healthData.sleep,
                { it.durationMinutes.toDouble() },
                { it.startTime }
            )
            
            if (sleepAnomalies.isNotEmpty()) {
                // Filter for short sleep durations
                val shortSleepAnomalies = sleepAnomalies.filter { 
                    it.durationMinutes < LOW_SLEEP_DURATION_THRESHOLD 
                }
                
                if (shortSleepAnomalies.isNotEmpty()) {
                    // Group by week to identify patterns
                    val weeklySleepIssues = shortSleepAnomalies.groupBy { 
                        it.startTime.toLocalDate().let { date ->
                            date.minusDays(date.dayOfWeek.value.toLong() - 1)
                        }
                    }
                    
                    for ((weekStart, weekAnomalies) in weeklySleepIssues) {
                        if (weekAnomalies.size >= 3) { // 3 or more poor sleep nights in a week
                            val averageDuration = weekAnomalies.map { it.durationMinutes }.average()
                            
                            insights.add(
                                AnomalyInsight(
                                    id = generateInsightId(),
                                    title = "Sleep Pattern Disruption",
                                    description = "During the week of ${weekStart}, you had ${weekAnomalies.size} nights " +
                                            "with less than ${LOW_SLEEP_DURATION_THRESHOLD / 60} hours of sleep. " +
                                            "Your average sleep duration was ${String.format("%.1f", averageDuration / 60)} hours. " +
                                            getSleepAnomalyDescription(weekAnomalies),
                                    severity = if (weekAnomalies.size >= 5) InsightSeverity.HIGH else InsightSeverity.MODERATE,
                                    category = InsightCategory.ANOMALIES,
                                    metricType = MetricType.SLEEP,
                                    anomalyType = AnomalyType.PATTERN,
                                    detectedAt = weekStart.atStartOfDay(),
                                    affectedValue = averageDuration,
                                    expectedRange = Pair(LOW_SLEEP_DURATION_THRESHOLD.toDouble(), 9 * 60.0),
                                    relatedInsightIds = emptyList()
                                )
                            )
                        }
                    }
                }
            }
        }
        
        return insights
    }

    /**
     * Generates correlation insights by analyzing relationships between different health metrics
     */
    private fun generateCorrelationInsights(healthData: HealthDataSet): List<CorrelationInsight> {
        val insights = mutableListOf<CorrelationInsight>()
        
        // Correlate sleep and heart rate
        if (healthData.sleep.isNotEmpty() && healthData.heartRate.isNotEmpty()) {
            val sleepHeartRateCorrelation = analyzeCorrelation(
                healthData.sleep,
                healthData.heartRate,
                { it.startTime.toLocalDate() },
                { it.timestamp.toLocalDate() },
                { it.durationMinutes.toDouble() },
                { it.value.toDouble() },
                correlationType = CorrelationType.INVERSE // Expect inverse correlation
            )
            
            if (sleepHeartRateCorrelation.strength > 0.5) {
                insights.add(
                    CorrelationInsight(
                        id = generateInsightId(),
                        title = "Sleep and Heart Rate Correlation",
                        description = "There appears to be a ${sleepHeartRateCorrelation.type.description.lowercase()} " +
                                "correlation between your sleep duration and resting heart rate. " +
                                "On days when you sleep more, your resting heart rate tends to be lower. " +
                                "This suggests that better sleep may be contributing to improved cardiovascular recovery.",
                        severity = InsightSeverity.MODERATE,
                        category = InsightCategory.CORRELATIONS,
                        primaryMetricType = MetricType.SLEEP,
                        secondaryMetricType = MetricType.HEART_RATE,
                        correlationType = sleepHeartRateCorrelation.type,
                        correlationStrength = sleepHeartRateCorrelation.strength,
                        relatedInsightIds = emptyList()
                    )
                )
            }
        }
        
        // Correlate activity and blood pressure
        if (healthData.steps.isNotEmpty() && healthData.bloodPressure.isNotEmpty()) {
            val activityBpCorrelation = analyzeCorrelation(
                healthData.steps,
                healthData.bloodPressure,
                { it.date },
                { it.timestamp.toLocalDate() },
                { it.count.toDouble() },
                { (it.systolic + it.diastolic) / 2.0 },
                correlationType = CorrelationType.INVERSE // Expect inverse correlation
            )
            
            if (activityBpCorrelation.strength > 0.5) {
                insights.add(
                    CorrelationInsight(
                        id = generateInsightId(),
                        title = "Activity and Blood Pressure Correlation",
                        description = "There appears to be a ${activityBpCorrelation.type.description.lowercase()} " +
                                "correlation between your daily activity level and blood pressure. " +
                                "On days when you're more active, your blood pressure tends to be lower. " +
                                "Regular physical activity is known to help maintain healthy blood pressure levels.",
                        severity = InsightSeverity.MODERATE,
                        category = InsightCategory.CORRELATIONS,
                        primaryMetricType = MetricType.STEPS,
                        secondaryMetricType = MetricType.BLOOD_PRESSURE,
                        correlationType = activityBpCorrelation.type,
                        correlationStrength = activityBpCorrelation.strength,
                        relatedInsightIds = emptyList()
                    )
                )
            }
        }
        
        // Correlate sleep and activity
        if (healthData.sleep.isNotEmpty() && healthData.steps.isNotEmpty()) {
            val sleepActivityCorrelation = analyzeCorrelation(
                healthData.sleep,
                healthData.steps,
                { it.startTime.toLocalDate() },
                { it.date },
                { it.durationMinutes.toDouble() },
                { it.count.toDouble() },
                correlationType = CorrelationType.POSITIVE // Expect positive correlation
            )
            
            if (sleepActivityCorrelation.strength > 0.5) {
                insights.add(
                    CorrelationInsight(
                        id = generateInsightId(),
                        title = "Sleep and Activity Correlation",
                        description = "There appears to be a ${sleepActivityCorrelation.type.description.lowercase()} " +
                                "correlation between your sleep duration and physical activity. " +
                                "On days following good sleep, you tend to be more active. " +
                                "This suggests that prioritizing sleep may help improve your energy levels and activity.",
                        severity = InsightSeverity.LOW,
                        category = InsightCategory.CORRELATIONS,
                        primaryMetricType = MetricType.SLEEP,
                        secondaryMetricType = MetricType.STEPS,
                        correlationType = sleepActivityCorrelation.type,
                        correlationStrength = sleepActivityCorrelation.strength,
                        relatedInsightIds = emptyList()
                    )
                )
            }
        }
        
        return insights
    }

    /**
     * Generates risk assessment insights based on health data and medical guidelines
     */
    private fun generateRiskAssessmentInsights(
        healthData: HealthDataSet,
        userPreferences: UserPreferences
    ): List<RiskAssessmentInsight> {
        val insights = mutableListOf<RiskAssessmentInsight>()
        
        // Cardiovascular risk assessment based on heart rate and blood pressure
        if (healthData.heartRate.isNotEmpty() && healthData.bloodPressure.isNotEmpty()) {
            val avgRestingHeartRate = healthData.heartRate
                .filter { it.measurementContext == MeasurementContext.RESTING }
                .takeIf { it.isNotEmpty() }
                ?.map { it.value }
                ?.average()
                ?: healthData.heartRate.map { it.value }.average()
                
            val elevatedBpReadings = healthData.bloodPressure
                .count { it.systolic >= HIGH_SYSTOLIC_THRESHOLD || it.diastolic >= HIGH_DIASTOLIC_THRESHOLD }
                
            val totalBpReadings = healthData.bloodPressure.size
            val elevatedBpPercentage = if (totalBpReadings > 0) 
                                       (elevatedBpReadings.toDouble() / totalBpReadings) * 100 
                                     else 0.0
            
            // Assess cardiovascular risk
            if (avgRestingHeartRate > HIGH_HEART_RATE_THRESHOLD && elevatedBpPercentage > 30) {
                insights.add(
                    RiskAssessmentInsight(
                        id = generateInsightId(),
                        title = "Elevated Cardiovascular Risk Factors",
                        description = "Your data shows potential cardiovascular risk factors. " +
                                "Your average resting heart rate of ${String.format("%.1f", avgRestingHeartRate)} bpm is above " +
                                "the recommended range, and ${String.format("%.1f", elevatedBpPercentage)}% of your blood pressure " +
                                "readings were elevated. These factors together may indicate increased cardiovascular strain.",
                        severity = InsightSeverity.HIGH,
                        category = InsightCategory.RISK_ASSESSMENT,
                        metricType = MetricType.MULTIPLE,
                        riskLevel = RiskLevel.ELEVATED,
                        riskFactors = listOf(
                            "Elevated resting heart rate",
                            "Frequent high blood pressure readings"
                        ),
                        recommendedActions = listOf(
                            "Consult with a healthcare provider",
                            "Consider lifestyle modifications",
                            "Monitor blood pressure regularly"
                        ),
                        relatedInsightIds = emptyList()
                    )
                )
            }
        }
        
        // Sleep apnea risk assessment based on sleep patterns and blood oxygen
        if (healthData.sleep.isNotEmpty() && healthData.bloodOxygen.isNotEmpty()) {
            val nighttimeOxygenDips = healthData.bloodOxygen
                .filter { 
                    // Filter for nighttime readings (between 10 PM and 7 AM)
                    val hour = it.timestamp.hour
                    hour < 7 || hour >= 22
                }
                .count { it.percentage < 90 }
                
            val sleepDisruptions = healthData.sleep
                .count { it.sleepQuality == SleepQuality.POOR || it.awakeningCount > 3 }
                
            // Assess sleep apnea risk
            if (nighttimeOxygenDips >= 3 && sleepDisruptions >= 2) {
                insights.add(
                    RiskAssessmentInsight(
                        id = generateInsightId(),
                        title = "Potential Sleep Breathing Disruption",
                        description = "Your data shows patterns that may be associated with sleep breathing issues. " +
                                "We've detected $nighttimeOxygenDips instances of nighttime oxygen level drops " +
                                "and $sleepDisruptions nights with sleep disruptions. These patterns can sometimes " +
                                "be associated with conditions like sleep apnea.",
                        severity = InsightSeverity.HIGH,
                        category = InsightCategory.RISK_ASSESSMENT,
                        metricType = MetricType.MULTIPLE,
                        riskLevel = RiskLevel.ELEVATED,
                        riskFactors = listOf(
                            "Nighttime oxygen level drops",
                            "Frequent sleep disruptions",
                            "Poor sleep quality"
                        ),
                        recommendedActions = listOf(
                            "Discuss these findings with a sleep specialist",
                            "Consider a formal sleep assessment",
                            "Maintain regular sleep schedule"
                        ),
                        relatedInsightIds = emptyList()
                    )
                )
            }
        }
        
        // Inactivity risk assessment
        if (healthData.steps.isNotEmpty()) {
            val lowActivityDays = healthData.steps
                .count { it.count < 5000 }
                
            val totalDays = healthData.steps.size
            val inactivityPercentage = if (totalDays > 0) 
                                      (lowActivityDays.toDouble() / totalDays) * 100 
                                    else 0.0
            
            // Assess inactivity risk
            if (inactivityPercentage > 70 && totalDays >= 7) {
                insights.add(
                    RiskAssessmentInsight(
                        id = generateInsightId(),
                        title = "Physical Inactivity Risk",
                        description = "Your activity data shows that you've been less active than recommended " +
                                "on ${String.format("%.1f", inactivityPercentage)}% of days. " +
                                "Regular physical activity is important for cardiovascular health, " +
                                "metabolism, and overall wellbeing.",
                        severity = InsightSeverity.MODERATE,
                        category = InsightCategory.RISK_ASSESSMENT,
                        metricType = MetricType.STEPS,
                        riskLevel = RiskLevel.MODERATE,
                        riskFactors = listOf(
                            "Consistently low daily step count",
                            "Insufficient physical activity"
                        ),
                        recommendedActions = listOf(
                            "Aim for at least 7,500 steps daily",
                            "Incorporate short walks throughout your day",
                            "Set reminders to move every hour"
                        ),
                        relatedInsightIds = emptyList()
                    )
                )
            }
        }
        
        return insights
    }

    /**
     * Generates personalized recommendation insights based on health data and user preferences
     */
    private fun generateRecommendationInsights(
        healthData: HealthDataSet,
        userPreferences: UserPreferences
    ): List<RecommendationInsight> {
        val insights = mutableListOf<RecommendationInsight>()
        
        // Sleep improvement recommendations
        if (healthData.sleep.isNotEmpty()) {
            val avgSleepDuration = healthData.sleep.map { it.durationMinutes }.average()
            val avgSleepQuality = healthData.sleep.map { it.sleepQuality.ordinal }.average()
            
            if (avgSleepDuration < 7 * 60) { // Less than 7 hours
                insights.add(
                    RecommendationInsight(
                        id = generateInsightId(),
                        title = "Sleep Duration Improvement",
                        description = "Your average sleep duration of ${String.format("%.1f", avgSleepDuration / 60)} hours " +
                                "is below the recommended 7-9 hours for adults. " +
                                "Improving your sleep duration may help with energy levels, cognitive function, and overall health.",
                        severity = InsightSeverity.MODERATE,
                        category = InsightCategory.RECOMMENDATIONS,
                        metricType = MetricType.SLEEP,
                        recommendationActions = listOf(
                            "Establish a consistent sleep schedule",
                            "Create a relaxing bedtime routine",
                            "Limit screen time before bed",
                            "Ensure your bedroom is dark, quiet, and cool"
                        ),
                        expectedBenefits = listOf(
                            "Improved energy levels",
                            "Better cognitive function",
                            "Enhanced mood stability",
                            "Strengthened immune system"
                        ),
                        relatedInsightIds = emptyList()
                    )
                )
            }
            
            // Sleep quality recommendations
            if (avgSleepQuality < SleepQuality.GOOD.ordinal) {
                insights.add(
                    RecommendationInsight(
                        id = generateInsightId(),
                        title = "Sleep Quality Enhancement",
                        description = "Your sleep quality data suggests room for improvement. " +
                                "While you may be getting adequate sleep duration, the quality of your sleep " +
                                "affects how restorative it is for your body and mind.",
                        severity = InsightSeverity.LOW,
                        category = InsightCategory.RECOMMENDATIONS,
                        metricType = MetricType.SLEEP,
                        recommendationActions = listOf(
                            "Limit caffeine after noon",
                            "Exercise regularly, but not close to bedtime",
                            "Consider relaxation techniques like meditation before bed",
                            "Evaluate your mattress and pillow comfort"
                        ),
                        expectedBenefits = listOf(
                            "More restorative sleep",
                            "Reduced daytime fatigue",
                            "Improved concentration",
                            "Better recovery from exercise"
                        ),
                        relatedInsightIds = emptyList()
                    )
                )
            }
        }
        
        // Activity recommendations
        if (healthData.steps.isNotEmpty()) {
            val avgSteps = healthData.steps.map { it.count }.average()
            
            if (avgSteps < 7500) { // Below recommended minimum
                insights.add(
                    RecommendationInsight(
                        id = generateInsightId(),
                        title = "Increase Daily Activity",
                        description = "Your average daily step count of ${avgSteps.toInt()} is below " +
                                "the recommended minimum of 7,500 steps. Regular physical activity " +
                                "is essential for cardiovascular health, weight management, and overall wellbeing.",
                        severity = InsightSeverity.MODERATE,
                        category = InsightCategory.RECOMMENDATIONS,
                        metricType = MetricType.STEPS,
                        recommendationActions = listOf(
                            "Take short walking breaks during the day",
                            "Use stairs instead of elevators when possible",
                            "Park farther from entrances",
                            "Consider a daily walking routine"
                        ),
                        expectedBenefits = listOf(
                            "Improved cardiovascular health",
                            "Better weight management",
                            "Reduced stress levels",
                            "Enhanced mood and energy"
                        ),
                        relatedInsightIds = emptyList()
                    )
                )
            }
        }
        
        // Heart health recommendations
        if (healthData.heartRate.isNotEmpty() && healthData.bloodPressure.isNotEmpty()) {
            val avgRestingHeartRate = healthData.heartRate
                .filter { it.measurementContext == MeasurementContext.RESTING }
                .takeIf { it.isNotEmpty() }
                ?.map { it.value }
                ?.average()
                ?: healthData.heartRate.map { it.value }.average()
                
            val avgSystolic = healthData.bloodPressure.map { it.systolic }.average()
            val avgDiastolic = healthData.bloodPressure.map { it.diastolic }.average()
            
            if (avgRestingHeartRate > 80 || avgSystolic > 130 || avgDiastolic > 85) {
                insights.add(
                    RecommendationInsight(
                        id = generateInsightId(),
                        title = "Heart Health Optimization",
                        description = "Your cardiovascular metrics show room for improvement. " +
                                "Your average resting heart rate is ${avgRestingHeartRate.toInt()} bpm " +
                                "and your average blood pressure is ${avgSystolic.toInt()}/${avgDiastolic.toInt()} mmHg. " +
                                "Optimizing these metrics can reduce long-term cardiovascular risk.",
                        severity = InsightSeverity.MODERATE,
                        category = InsightCategory.RECOMMENDATIONS,
                        metricType = MetricType.MULTIPLE,
                        recommendationActions = listOf(
                            "Incorporate regular aerobic exercise",
                            "Maintain a heart-healthy diet low in sodium and saturated fats",
                            "Practice stress management techniques",
                            "Ensure adequate hydration",
                            "Limit alcohol consumption"
                        ),
                        expectedBenefits = listOf(
                            "Improved cardiovascular efficiency",
                            "Reduced risk of heart disease",
                            "Better blood pressure control",
                            "Enhanced overall heart health"
                        ),
                        relatedInsightIds = emptyList()
                    )
                )
            }
        }
        
        return insights
    }

    /**
     * Generates goal-related insights based on user preferences and health data
     */
    private fun generateGoalInsights(
        userId: Long,
        healthData: HealthDataSet,
        userPreferences: UserPreferences
    ): List<GoalInsight> {
        val insights = mutableListOf<GoalInsight>()
        
        // Step goal insights
        val stepGoal = userPreferences.stepGoal ?: 10000
        if (healthData.steps.isNotEmpty()) {
            val recentSteps = healthData.steps.sortedByDescending { it.date }.take(7)
            val goalAchievements = recentSteps.count { it.count >= stepGoal }
            val avgSteps = recentSteps.map { it.count }.average()
            val goalProgress = (avgSteps / stepGoal) * 100
            
            if (recentSteps.size >= 5) { // Only provide insight if we have enough data
                if (goalAchievements >= 5) { // 5 or more days of goal achievement
                    insights.add(
                        GoalInsight(
                            id = generateInsightId(),
                            title = "Step Goal Achievement",
                            description = "Great job! You've reached your step goal of $stepGoal steps " +
                                    "on $goalAchievements out of the last ${recentSteps.size} days. " +
                                    "Your average daily steps were ${avgSteps.toInt()}, which is " +
                                    "${String.format("%.1f", goalProgress)}% of your goal.",
                            severity = InsightSeverity.LOW,
                            category = InsightCategory.GOALS,
                            metricType = MetricType.STEPS,
                            goalType = GoalType.STEPS,
                            goalTarget = stepGoal.toDouble(),
                            currentValue = avgSteps,
                            progressPercentage = goalProgress,
                            isAchieved = goalAchievements >= recentSteps.size * 0.7, // 70% achievement rate
                            streakDays = calculateConsecutiveGoalAchievements(recentSteps) { it.count >= stepGoal },
                            relatedInsightIds = emptyList()
                        )
                    )
                } else if (goalAchievements <= 2) { // 2 or fewer days of goal achievement
                    insights.add(
                        GoalInsight(
                            id = generateInsightId(),
                            title = "Step Goal Progress",
                            description = "You're making progress toward your step goal of $stepGoal steps, " +
                                    "but reached it on only $goalAchievements out of the last ${recentSteps.size} days. " +
                                    "Your average daily steps were ${avgSteps.toInt()}, which is " +
                                    "${String.format("%.1f", goalProgress)}% of your goal.",
                            severity = InsightSeverity.LOW,
                            category = InsightCategory.GOALS,
                            metricType = MetricType.STEPS,
                            goalType = GoalType.STEPS,
                            goalTarget = stepGoal.toDouble(),
                            currentValue = avgSteps,
                            progressPercentage = goalProgress,
                            isAchieved = false,
                            streakDays = calculateConsecutiveGoalAchievements(recentSteps) { it.count >= stepGoal },
                            relatedInsightIds = emptyList()
                        )
                    )
                }
            }
        }
        
        // Sleep goal insights
        val sleepGoal = userPreferences.sleepGoalMinutes ?: (8 * 60) // 8 hours in minutes
        if (healthData.sleep.isNotEmpty()) {
            val recentSleep = healthData.sleep.sortedByDescending { it.startTime }.take(7)
            val goalAchievements = recentSleep.count { it.durationMinutes >= sleepGoal }
            val avgSleepDuration = recentSleep.map { it.durationMinutes }.average()
            val goalProgress = (avgSleepDuration / sleepGoal) * 100
            
            if (recentSleep.size >= 5) { // Only provide insight if we have enough data
                if (goalAchievements >= 5) { // 5 or more days of goal achievement
                    insights.add(
                        GoalInsight(
                            id = generateInsightId(),
                            title = "Sleep Goal Achievement",
                            description = "Great job! You've reached your sleep goal of ${sleepGoal / 60} hours " +
                                    "on $goalAchievements out of the last ${recentSleep.size} nights. " +
                                    "Your average sleep duration was ${String.format("%.1f", avgSleepDuration / 60)} hours, " +
                                    "which is ${String.format("%.1f", goalProgress)}% of your goal.",
                            severity = InsightSeverity.LOW,
                            category = InsightCategory.GOALS,
                            metricType = MetricType.SLEEP,
                            goalType = GoalType.SLEEP_DURATION,
                            goalTarget = sleepGoal.toDouble(),
                            currentValue = avgSleepDuration,
                            progressPercentage = goalProgress,
                            isAchieved = goalAchievements >= recentSleep.size * 0.7, // 70% achievement rate
                            streakDays = calculateConsecutiveGoalAchievements(recentSleep) { it.durationMinutes >= sleepGoal },
                            relatedInsightIds = emptyList()
                        )
                    )
                } else if (goalAchievements <= 2) { // 2 or fewer days of goal achievement
                    insights.add(
                        GoalInsight(
                            id = generateInsightId(),
                            title = "Sleep Goal Progress",
                            description = "You're making progress toward your sleep goal of ${sleepGoal / 60} hours, " +
                                    "but reached it on only $goalAchievements out of the last ${recentSleep.size} nights. " +
                                    "Your average sleep duration was ${String.format("%.1f", avgSleepDuration / 60)} hours, " +
                                    "which is ${String.format("%.1f", goalProgress)}% of your goal.",
                            severity = InsightSeverity.MODERATE,
                            category = InsightCategory.GOALS,
                            metricType = MetricType.SLEEP,
                            goalType = GoalType.SLEEP_DURATION,
                            goalTarget = sleepGoal.toDouble(),
                            currentValue = avgSleepDuration,
                            progressPercentage = goalProgress,
                            isAchieved = false,
                            streakDays = calculateConsecutiveGoalAchievements(recentSleep) { it.durationMinutes >= sleepGoal },
                            relatedInsightIds = emptyList()
                        )
                    )
                }
            }
        }
        
        return insights
    }

    /**
     * Calculates an overall health score based on all health metrics
     */
    private fun calculateHealthScore(
        healthData: HealthDataSet,
        userPreferences: UserPreferences
    ): HealthScore {
        // Initialize score components
        val scoreComponents = mutableMapOf<MetricType, HealthScoreComponent>()
        
        // Heart Rate Score (15% of total)
        if (healthData.heartRate.isNotEmpty()) {
            val avgRestingHeartRate = healthData.heartRate
                .filter { it.measurementContext == MeasurementContext.RESTING }
                .takeIf { it.isNotEmpty() }
                ?.map { it.value }
                ?.average()
                ?: healthData.heartRate.map { it.value }.average()
                
            val heartRateScore = when {
                avgRestingHeartRate < 60 -> 95 // Excellent
                avgRestingHeartRate < 70 -> 85 // Very good
                avgRestingHeartRate < 80 -> 75 // Good
                avgRestingHeartRate < 90 -> 60 // Fair
                else -> 40 // Poor
            }
            
            scoreComponents[MetricType.HEART_RATE] = HealthScoreComponent(
                metricType = MetricType.HEART_RATE,
                score = heartRateScore,
                weight = 0.15,
                description = "Your average resting heart rate of ${avgRestingHeartRate.toInt()} bpm " +
                        "is ${getHeartRateScoreDescription(heartRateScore)}."
            )
        }
        
        // Blood Pressure Score (15% of total)
        if (healthData.bloodPressure.isNotEmpty()) {
            val avgSystolic = healthData.bloodPressure.map { it.systolic }.average()
            val avgDiastolic = healthData.bloodPressure.map { it.diastolic }.average()
            
            val bpScore = when {
                avgSystolic < 120 && avgDiastolic < 80 -> 95 // Optimal
                avgSystolic < 130 && avgDiastolic < 85 -> 85 // Normal
                avgSystolic < 140 && avgDiastolic < 90 -> 70 // High normal
                avgSystolic < 160 && avgDiastolic < 100 -> 50 // Hypertension stage 1
                else -> 30 // Hypertension stage 2+
            }
            
            scoreComponents[MetricType.BLOOD_PRESSURE] = HealthScoreComponent(
                metricType = MetricType.BLOOD_PRESSURE,
                score = bpScore,
                weight = 0.15,
                description = "Your average blood pressure of ${avgSystolic.toInt()}/${avgDiastolic.toInt()} mmHg " +
                        "is ${getBloodPressureScoreDescription(bpScore)}."
            )
        }
        
        // Blood Oxygen Score (10% of total)
        if (healthData.bloodOxygen.isNotEmpty()) {
            val avgOxygen = healthData.bloodOxygen.map { it.percentage }.average()
            
            val oxygenScore = when {
                avgOxygen >= 98 -> 95 // Excellent
                avgOxygen >= 96 -> 85 // Very good
                avgOxygen >= 94 -> 70 // Good
                avgOxygen >= 92 -> 50 // Fair
                else -> 30 // Poor
            }
            
            scoreComponents[MetricType.BLOOD_OXYGEN] = HealthScoreComponent(
                metricType = MetricType.BLOOD_OXYGEN,
                score = oxygenScore,
                weight = 0.10,
                description = "Your average blood oxygen level of ${String.format("%.1f", avgOxygen)}% " +
                        "is ${getBloodOxygenScoreDescription(oxygenScore)}."
            )
        }
        
        // Sleep Score (20% of total)
        if (healthData.sleep.isNotEmpty()) {
            val avgDuration = healthData.sleep.map { it.durationMinutes }.average()
            val avgQuality = healthData.sleep.map { it.sleepQuality.ordinal }.average() / 
                             (SleepQuality.values().size - 1).toDouble()
            
            // Combine duration and quality for sleep score
            val durationScore = when {
                avgDuration >= 8 * 60 -> 95 // Excellent (8+ hours)
                avgDuration >= 7 * 60 -> 85 // Very good (7-8 hours)
                avgDuration >= 6 * 60 -> 70 // Good (6-7 hours)
                avgDuration >= 5 * 60 -> 50 // Fair (5-6 hours)
                else -> 30 // Poor (<5 hours)
            }
            
            val qualityScore = (avgQuality * 100).toInt()
            val sleepScore = (durationScore * 0.6 + qualityScore * 0.4).toInt() // 60% duration, 40% quality
            
            scoreComponents[MetricType.SLEEP] = HealthScoreComponent(
                metricType = MetricType.SLEEP,
                score = sleepScore,
                weight = 0.20,
                description = "Your sleep score is based on an average duration of " +
                        "${String.format("%.1f", avgDuration / 60)} hours and ${getSleepQualityDescription(avgQuality)}."
            )
        }
        
        // Activity Score (20% of total)
        if (healthData.steps.isNotEmpty()) {
            val avgSteps = healthData.steps.map { it.count }.average()
            val stepGoal = userPreferences.stepGoal ?: 10000
            
            val stepScore = when {
                avgSteps >= stepGoal * 1.2 -> 95 // Excellent (>120% of goal)
                avgSteps >= stepGoal -> 85 // Very good (100-120% of goal)
                avgSteps >= stepGoal * 0.8 -> 75 // Good (80-100% of goal)
                avgSteps >= stepGoal * 0.6 -> 60 // Fair (60-80% of goal)
                avgSteps >= stepGoal * 0.4 -> 45 // Below average (40-60% of goal)
                else -> 30 // Poor (<40% of goal)
            }
            
            scoreComponents[MetricType.STEPS] = HealthScoreComponent(
                metricType = MetricType.STEPS,
                score = stepScore,
                weight = 0.20,
                description = "Your activity score is based on an average of ${avgSteps.toInt()} steps per day, " +
                        "which is ${String.format("%.1f", (avgSteps / stepGoal) * 100)}% of your goal."
            )
        }
        
        // Exercise Score (10% of total)
        if (healthData.activities.isNotEmpty()) {
            val weeklyExerciseMinutes = healthData.activities
                .groupBy { it.startTime.toLocalDate().let { date -> 
                    date.minusDays(date.dayOfWeek.value.toLong() - 1) 
                }}
                .map { (_, activities) -> activities.sumOf { it.durationMinutes } }
                .takeIf { it.isNotEmpty() }
                ?.average()
                ?: 0.0
            
            val exerciseScore = when {
                weeklyExerciseMinutes >= 150 -> 95 // Excellent (meets WHO recommendation)
                weeklyExerciseMinutes >= 120 -> 85 // Very good
                weeklyExerciseMinutes >= 90 -> 75 // Good
                weeklyExerciseMinutes >= 60 -> 60 // Fair
                weeklyExerciseMinutes >= 30 -> 45 // Below average
                else -> 30 // Poor
            }
            
            scoreComponents[MetricType.ACTIVITY] = HealthScoreComponent(
                metricType = MetricType.ACTIVITY,
                score = exerciseScore,
                weight = 0.10,
                description = "Your exercise score is based on an average of " +
                        "${String.format("%.1f", weeklyExerciseMinutes)} minutes of exercise per week."
            )
        }
        
        // Temperature Score (10% of total)
        if (healthData.temperature.isNotEmpty()) {
            val avgTemperature = healthData.temperature.map { it.value }.average()
            val temperatureVariability = calculateStandardDeviation(healthData.temperature.map { it.value })
            
            val temperatureScore = when {
                avgTemperature in 36.5..37.2 && temperatureVariability < 0.3 -> 95 // Excellent
                avgTemperature in 36.3..37.4 && temperatureVariability < 0.5 -> 85 // Very good
                avgTemperature in 36.1..37.6 && temperatureVariability < 0.7 -> 70 // Good
                avgTemperature in 35.9..37.8 && temperatureVariability < 0.9 -> 50 // Fair
                else -> 30 // Poor
            }
            
            scoreComponents[MetricType.TEMPERATURE] = HealthScoreComponent(
                metricType = MetricType.TEMPERATURE,
                score = temperatureScore,
                weight = 0.10,
                description = "Your temperature score is based on an average of " +
                        "${String.format("%.1f", avgTemperature)}°C with a variability of " +
                        "${String.format("%.2f", temperatureVariability)}°C."
            )
        }
        
        // Calculate overall score
        var totalScore = 0.0
        var totalWeight = 0.0
        
        for (component in scoreComponents.values) {
            totalScore += component.score * component.weight
            totalWeight += component.weight
        }
        
        val overallScore = if (totalWeight > 0) (totalScore / totalWeight).toInt() else 0
        val healthGrade = getHealthGradeFromScore(overallScore)
        
        return HealthScore(
            score = overallScore,
            grade = healthGrade,
            components = scoreComponents.values.toList(),
            description = getHealthScoreDescription(overallScore, healthGrade, scoreComponents.values.toList())
        )
    }

    /**
     * Analyzes a trend in a list of data points
     */
    private fun <T> analyzeTrend(
        data: List<T>,
        valueExtractor: (T) -> Double,
        timeExtractor: (T) -> LocalDateTime,
        significanceThreshold: Double
    ): TrendDirection {
        if (data.size < 3) return TrendDirection.STABLE // Not enough data
        
        // Sort data by time
        val sortedData = data.sortedBy { timeExtractor(it) }
        
        // Split data into segments for trend analysis
        val segments = when {
            sortedData.size >= 30 -> 5 // 5 segments for 30+ data points
            sortedData.size >= 14 -> 3 // 3 segments for 14+ data points
            else -> 2 // 2 segments for fewer data points
        }
        
        val segmentSize = sortedData.size / segments
        val segmentAverages = (0 until segments).map { segmentIndex ->
            val segmentStart = segmentIndex * segmentSize
            val segmentEnd = if (segmentIndex == segments - 1) sortedData.size else (segmentIndex + 1) * segmentSize
            val segmentData = sortedData.subList(segmentStart, segmentEnd)
            segmentData.map { valueExtractor(it) }.average()
        }
        
        // Calculate first and last segment averages
        val firstSegmentAvg = segmentAverages.first()
        val lastSegmentAvg = segmentAverages.last()
        val difference = lastSegmentAvg - firstSegmentAvg
        
        // Determine trend direction based on significance threshold
        return when {
            abs(difference) < significanceThreshold -> TrendDirection.STABLE
            difference > 0 -> TrendDirection.INCREASING
            else -> TrendDirection.DECREASING
        }
    }

    /**
     * Detects anomalies in a list of data points
     */
    private fun <T> detectAnomalies(
        data: List<T>,
        valueExtractor: (T) -> Double,
        timeExtractor: (T) -> LocalDateTime
    ): List<T> {
        if (data.size < 5) return emptyList() // Not enough data
        
        // Extract values
        val values = data.map { valueExtractor(it) }
        
        // Calculate mean and standard deviation
        val mean = values.average()
        val stdDev = calculateStandardDeviation(values)
        
        // Define anomaly threshold (Z-score > 2.5)
        val anomalyThreshold = 2.5
        
        // Find anomalies
        return data.filter {
            val value = valueExtractor(it)
            val zScore = abs(value - mean) / stdDev
            zScore > anomalyThreshold
        }
    }

    /**
     * Analyzes correlation between two lists of data
     */
    private fun <T, U> analyzeCorrelation(
        primaryData: List<T>,
        secondaryData: List<U>,
        primaryDateExtractor: (T) -> LocalDate,
        secondaryDateExtractor: (U) -> LocalDate,
        primaryValueExtractor: (T) -> Double,
        secondaryValueExtractor: (U) -> Double,
        correlationType: CorrelationType
    ): CorrelationResult {
        // Group data by date
        val primaryByDate = primaryData.groupBy { primaryDateExtractor(it) }
        val secondaryByDate = secondaryData.groupBy { secondaryDateExtractor(it) }
        
        // Find common dates
        val commonDates = primaryByDate.keys.intersect(secondaryByDate.keys)
        
        if (commonDates.size < 5) {
            return CorrelationResult(CorrelationType.NONE, 0.0) // Not enough data
        }
        
        // Create paired data points
        val pairedData = commonDates.map { date ->
            val primaryAvg = primaryByDate[date]!!.map { primaryValueExtractor(it) }.average()
            val secondaryAvg = secondaryByDate[date]!!.map { secondaryValueExtractor(it) }.average()
            Pair(primaryAvg, secondaryAvg)
        }
        
        // Calculate correlation coefficient
        val correlation = calculateCorrelation(pairedData)
        
        // Determine correlation type and strength
        val correlationStrength = abs(correlation)
        val actualCorrelationType = when {
            correlationStrength < 0.3 -> CorrelationType.NONE
            correlation > 0 -> CorrelationType.POSITIVE
            else -> CorrelationType.INVERSE
        }
        
        // Check if the detected correlation matches the expected type
        val matchesExpected = when (correlationType) {
            CorrelationType.NONE -> actualCorrelationType == CorrelationType.NONE
            CorrelationType.POSITIVE -> actualCorrelationType == CorrelationType.POSITIVE
            CorrelationType.INVERSE -> actualCorrelationType == CorrelationType.INVERSE
        }
        
        // If correlation doesn't match expected type, reduce the strength
        val adjustedStrength = if (matchesExpected) correlationStrength else correlationStrength * 0.5
        
        return CorrelationResult(actualCorrelationType, adjustedStrength)
    }

    /**
     * Calculates the percentage change in a list of data points
     */
    private fun <T> calculatePercentageChange(
        data: List<T>,
        valueExtractor: (T) -> Double
    ): Double {
        if (data.size < 2) return 0.0
        
        // Sort data chronologically (assuming first item is oldest)
        val sortedData = data.sortedBy { 
            if (it is TimeBasedMeasurement) it.timestamp
            else if (it is StepRecord) it.date.atStartOfDay()
            else LocalDateTime.MIN
        }
        
        // Split into first and second half
        val midpoint = sortedData.size / 2
        val firstHalf = sortedData.subList(0, midpoint)
        val secondHalf = sortedData.subList(midpoint, sortedData.size)
        
        // Calculate averages
        val firstHalfAvg = firstHalf.map { valueExtractor(it) }.average()
        val secondHalfAvg = secondHalf.map { valueExtractor(it) }.average()
        
        // Calculate percentage change
        return if (firstHalfAvg != 0.0) {
            ((secondHalfAvg - firstHalfAvg) / firstHalfAvg) * 100
        } else {
            0.0
        }
    }

    /**
     * Calculates the standard deviation of a list of values
     */
    private fun calculateStandardDeviation(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        
        val mean = values.average()
        val variance = values.map { (it - mean).pow(2) }.average()
        return kotlin.math.sqrt(variance)
    }

    /**
     * Calculates the correlation coefficient between paired data points
     */
    private fun calculateCorrelation(pairedData: List<Pair<Double, Double>>): Double {
        if (pairedData.size < 3) return 0.0
        
        val n = pairedData.size
        
        // Calculate means
        val meanX = pairedData.map { it.first }.average()
        val meanY = pairedData.map { it.second }.average()
        
        // Calculate covariance and variances
        var covariance = 0.0
        var varianceX = 0.0
        var varianceY = 0.0
        
        for (pair in pairedData) {
            val diffX = pair.first - meanX
            val diffY = pair.second - meanY
            covariance += diffX * diffY
            varianceX += diffX * diffX
            varianceY += diffY * diffY
        }
        
        // Normalize by n
        covariance /= n
        varianceX /= n
        varianceY /= n
        
        // Calculate correlation coefficient
        return if (varianceX > 0 && varianceY > 0) {
            covariance / (kotlin.math.sqrt(varianceX) * kotlin.math.sqrt(varianceY))
        } else {
            0.0
        }
    }

    /**
     * Calculates consecutive days of goal achievement
     */
    private fun <T> calculateConsecutiveGoalAchievements(
        data: List<T>,
        achievementPredicate: (T) -> Boolean
    ): Int {
        if (data.isEmpty()) return 0
        
        // For simplicity, we'll just count the current streak
        var currentStreak = 0
        
        // Assume data is sorted by date (most recent first)
        for (item in data) {
            if (achievementPredicate(item)) {
                currentStreak++
            } else {
                break
            }
        }
        
        return currentStreak
    }

    /**
     * Generates a unique ID for an insight
     */
    private fun generateInsightId(): String {
        return "insight_${System.currentTimeMillis()}_${(0..999).random()}"
    }

    /**
     * Gets a description for heart rate trend
     */
    private fun getHeartRateTrendDescription(
        trend: TrendDirection,
        data: List<HeartRateMeasurement>
    ): String {
        val avgHeartRate = data.map { it.value }.average()
        
        return when (trend) {
            TrendDirection.INCREASING -> {
                if (avgHeartRate > HIGH_HEART_RATE_THRESHOLD) {
                    "This increasing trend may indicate increased cardiovascular stress. " +
                    "Consider factors like stress, caffeine intake, or medication changes that might be affecting your heart