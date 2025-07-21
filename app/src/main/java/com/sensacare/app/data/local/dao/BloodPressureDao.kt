package com.sensacare.app.data.local.dao

import androidx.room.*
import com.sensacare.app.data.local.entity.BloodPressureEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * BloodPressureDao - Specialized Data Access Object for blood pressure metrics
 *
 * This DAO extends the core functionality with blood pressure specific
 * queries and analysis capabilities, including:
 * - Hypertension analysis and categorization
 * - Blood pressure trends and patterns
 * - Systolic and diastolic pressure analysis
 * - Blood pressure medication timing correlation
 * - Morning vs evening blood pressure patterns
 * - Blood pressure risk assessment
 * - Pulse pressure analysis
 * - Blood pressure variability metrics
 *
 * The BloodPressureDao provides comprehensive access to blood pressure data for both
 * real-time monitoring and long-term trend analysis.
 */
@Dao
interface BloodPressureDao {

    /**
     * Basic CRUD Operations
     */

    /**
     * Insert a single blood pressure record
     * @param bloodPressure The blood pressure entity to insert
     * @return The row ID of the inserted item
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bloodPressure: BloodPressureEntity): Long

    /**
     * Insert multiple blood pressure records in a single transaction
     * @param bloodPressures List of blood pressure entities to insert
     * @return List of row IDs for the inserted items
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(bloodPressures: List<BloodPressureEntity>): List<Long>

    /**
     * Update a blood pressure record
     * @param bloodPressure The blood pressure entity to update
     * @return Number of rows updated
     */
    @Update
    suspend fun update(bloodPressure: BloodPressureEntity): Int

    /**
     * Delete a blood pressure record
     * @param bloodPressure The blood pressure entity to delete
     * @return Number of rows deleted
     */
    @Delete
    suspend fun delete(bloodPressure: BloodPressureEntity): Int

    /**
     * Basic Queries
     */

    /**
     * Get blood pressure by ID
     * @param id The ID of the blood pressure to retrieve
     * @return The blood pressure entity or null if not found
     */
    @Query("SELECT * FROM blood_pressure WHERE id = :id")
    suspend fun getById(id: String): BloodPressureEntity?

    /**
     * Get blood pressure by ID as Flow for reactive updates
     * @param id The ID of the blood pressure to retrieve
     * @return Flow emitting the blood pressure entity
     */
    @Query("SELECT * FROM blood_pressure WHERE id = :id")
    fun getByIdAsFlow(id: String): Flow<BloodPressureEntity?>

    /**
     * Get all blood pressure data for a specific user
     * @param userId The user ID
     * @return List of blood pressure entities
     */
    @Query("SELECT * FROM blood_pressure WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getAllForUser(userId: String): List<BloodPressureEntity>

    /**
     * Get all blood pressure data for a specific user as Flow for reactive updates
     * @param userId The user ID
     * @return Flow emitting list of blood pressure entities
     */
    @Query("SELECT * FROM blood_pressure WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllForUserAsFlow(userId: String): Flow<List<BloodPressureEntity>>

    /**
     * Get blood pressure data for a specific time range
     * @param userId The user ID
     * @param startTime Start of the time range
     * @param endTime End of the time range
     * @return List of blood pressure entities
     */
    @Query("SELECT * FROM blood_pressure WHERE userId = :userId AND timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    suspend fun getByTimeRange(
        userId: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): List<BloodPressureEntity>

    /**
     * Get blood pressure data for a specific time range as Flow for reactive updates
     * @param userId The user ID
     * @param startTime Start of the time range
     * @param endTime End of the time range
     * @return Flow emitting list of blood pressure entities
     */
    @Query("SELECT * FROM blood_pressure WHERE userId = :userId AND timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getByTimeRangeAsFlow(
        userId: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): Flow<List<BloodPressureEntity>>

    /**
     * Get the latest blood pressure record
     * @param userId The user ID
     * @return The latest blood pressure entity or null if not found
     */
    @Query("SELECT * FROM blood_pressure WHERE userId = :userId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(userId: String): BloodPressureEntity?

    /**
     * Get the latest blood pressure record as Flow for reactive updates
     * @param userId The user ID
     * @return Flow emitting the latest blood pressure entity
     */
    @Query("SELECT * FROM blood_pressure WHERE userId = :userId ORDER BY timestamp DESC LIMIT 1")
    fun getLatestAsFlow(userId: String): Flow<BloodPressureEntity?>

    /**
     * Hypertension Analysis and Categorization
     */

    /**
     * Categorize blood pressure readings according to standard classifications
     * - Normal: <120/80 mmHg
     * - Elevated: 120-129/<80 mmHg
     * - Hypertension Stage 1: 130-139/80-89 mmHg
     * - Hypertension Stage 2: ≥140/≥90 mmHg
     * - Hypertensive Crisis: >180/>120 mmHg
     *
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return Distribution of blood pressure categories
     */
    @Query("""
        SELECT
            CASE
                WHEN systolic < 120 AND diastolic < 80 THEN 'Normal'
                WHEN systolic BETWEEN 120 AND 129 AND diastolic < 80 THEN 'Elevated'
                WHEN (systolic BETWEEN 130 AND 139) OR (diastolic BETWEEN 80 AND 89) THEN 'Hypertension Stage 1'
                WHEN (systolic >= 140 AND systolic <= 180) OR (diastolic >= 90 AND diastolic <= 120) THEN 'Hypertension Stage 2'
                WHEN systolic > 180 OR diastolic > 120 THEN 'Hypertensive Crisis'
                ELSE 'Unknown'
            END as category,
            COUNT(*) as count,
            AVG(systolic) as avgSystolic,
            AVG(diastolic) as avgDiastolic,
            MIN(systolic) as minSystolic,
            MAX(systolic) as maxSystolic,
            MIN(diastolic) as minDiastolic,
            MAX(diastolic) as maxDiastolic
        FROM blood_pressure
        WHERE userId = :userId
        AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
        GROUP BY category
        ORDER BY 
            CASE category
                WHEN 'Normal' THEN 1
                WHEN 'Elevated' THEN 2
                WHEN 'Hypertension Stage 1' THEN 3
                WHEN 'Hypertension Stage 2' THEN 4
                WHEN 'Hypertensive Crisis' THEN 5
                ELSE 6
            END
    """)
    suspend fun getHypertensionCategorization(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<BpCategory>

    /**
     * Get hypertension categorization as Flow for reactive updates
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return Flow emitting distribution of blood pressure categories
     */
    @Query("""
        SELECT
            CASE
                WHEN systolic < 120 AND diastolic < 80 THEN 'Normal'
                WHEN systolic BETWEEN 120 AND 129 AND diastolic < 80 THEN 'Elevated'
                WHEN (systolic BETWEEN 130 AND 139) OR (diastolic BETWEEN 80 AND 89) THEN 'Hypertension Stage 1'
                WHEN (systolic >= 140 AND systolic <= 180) OR (diastolic >= 90 AND diastolic <= 120) THEN 'Hypertension Stage 2'
                WHEN systolic > 180 OR diastolic > 120 THEN 'Hypertensive Crisis'
                ELSE 'Unknown'
            END as category,
            COUNT(*) as count,
            AVG(systolic) as avgSystolic,
            AVG(diastolic) as avgDiastolic,
            MIN(systolic) as minSystolic,
            MAX(systolic) as maxSystolic,
            MIN(diastolic) as minDiastolic,
            MAX(diastolic) as maxDiastolic
        FROM blood_pressure
        WHERE userId = :userId
        AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
        GROUP BY category
        ORDER BY 
            CASE category
                WHEN 'Normal' THEN 1
                WHEN 'Elevated' THEN 2
                WHEN 'Hypertension Stage 1' THEN 3
                WHEN 'Hypertension Stage 2' THEN 4
                WHEN 'Hypertensive Crisis' THEN 5
                ELSE 6
            END
    """)
    fun getHypertensionCategorizationAsFlow(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<BpCategory>>

    /**
     * Get percentage of readings in hypertension range
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return Percentage of readings in each category
     */
    @Query("""
        WITH categorized AS (
            SELECT
                CASE
                    WHEN systolic < 120 AND diastolic < 80 THEN 'Normal'
                    WHEN systolic BETWEEN 120 AND 129 AND diastolic < 80 THEN 'Elevated'
                    WHEN (systolic BETWEEN 130 AND 139) OR (diastolic BETWEEN 80 AND 89) THEN 'Hypertension Stage 1'
                    WHEN (systolic >= 140 AND systolic <= 180) OR (diastolic >= 90 AND diastolic <= 120) THEN 'Hypertension Stage 2'
                    WHEN systolic > 180 OR diastolic > 120 THEN 'Hypertensive Crisis'
                    ELSE 'Unknown'
                END as category
            FROM blood_pressure
            WHERE userId = :userId
            AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
        ),
        totals AS (
            SELECT COUNT(*) as total FROM categorized
        )
        SELECT 
            category,
            COUNT(*) as count,
            (COUNT(*) * 100.0 / (SELECT total FROM totals)) as percentage
        FROM categorized
        GROUP BY category
        ORDER BY 
            CASE category
                WHEN 'Normal' THEN 1
                WHEN 'Elevated' THEN 2
                WHEN 'Hypertension Stage 1' THEN 3
                WHEN 'Hypertension Stage 2' THEN 4
                WHEN 'Hypertensive Crisis' THEN 5
                ELSE 6
            END
    """)
    suspend fun getHypertensionPercentage(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<BpCategoryPercentage>

    /**
     * Get count of hypertensive crisis events
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return Count of hypertensive crisis events
     */
    @Query("""
        SELECT COUNT(*)
        FROM blood_pressure
        WHERE userId = :userId
        AND (systolic > 180 OR diastolic > 120)
        AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
    """)
    suspend fun getHypertensiveCrisisCount(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Int

    /**
     * Get all hypertensive crisis events
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return List of hypertensive crisis events
     */
    @Query("""
        SELECT *
        FROM blood_pressure
        WHERE userId = :userId
        AND (systolic > 180 OR diastolic > 120)
        AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
        ORDER BY timestamp DESC
    """)
    suspend fun getHypertensiveCrisisEvents(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<BloodPressureEntity>

    /**
     * Blood Pressure Trends and Patterns
     */

    /**
     * Get daily blood pressure summary
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return List of daily blood pressure summaries
     */
    @Query("""
        SELECT 
            date(timestamp) as date,
            AVG(systolic) as avgSystolic,
            AVG(diastolic) as avgDiastolic,
            MIN(systolic) as minSystolic,
            MAX(systolic) as maxSystolic,
            MIN(diastolic) as minDiastolic,
            MAX(diastolic) as maxDiastolic,
            AVG(pulse) as avgPulse,
            COUNT(*) as readingCount
        FROM blood_pressure
        WHERE userId = :userId
        AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
        GROUP BY date(timestamp)
        ORDER BY date(timestamp)
    """)
    suspend fun getDailyBloodPressureSummary(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<DailyBpSummary>

    /**
     * Get daily blood pressure summary as Flow for reactive updates
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return Flow emitting list of daily blood pressure summaries
     */
    @Query("""
        SELECT 
            date(timestamp) as date,
            AVG(systolic) as avgSystolic,
            AVG(diastolic) as avgDiastolic,
            MIN(systolic) as minSystolic,
            MAX(systolic) as maxSystolic,
            MIN(diastolic) as minDiastolic,
            MAX(diastolic) as maxDiastolic,
            AVG(pulse) as avgPulse,
            COUNT(*) as readingCount
        FROM blood_pressure
        WHERE userId = :userId
        AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
        GROUP BY date(timestamp)
        ORDER BY date(timestamp)
    """)
    fun getDailyBloodPressureSummaryAsFlow(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<DailyBpSummary>>

    /**
     * Get weekly blood pressure trends
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return List of weekly blood pressure summaries
     */
    @Query("""
        SELECT 
            strftime('%Y-%W', timestamp) as week,
            AVG(systolic) as avgSystolic,
            AVG(diastolic) as avgDiastolic,
            MIN(systolic) as minSystolic,
            MAX(systolic) as maxSystolic,
            MIN(diastolic) as minDiastolic,
            MAX(diastolic) as maxDiastolic,
            AVG(pulse) as avgPulse,
            COUNT(*) as readingCount
        FROM blood_pressure
        WHERE userId = :userId
        AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
        GROUP BY strftime('%Y-%W', timestamp)
        ORDER BY week
    """)
    suspend fun getWeeklyBloodPressureTrends(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<WeeklyBpSummary>

    /**
     * Get monthly blood pressure trends
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return List of monthly blood pressure summaries
     */
    @Query("""
        SELECT 
            strftime('%Y-%m', timestamp) as month,
            AVG(systolic) as avgSystolic,
            AVG(diastolic) as avgDiastolic,
            MIN(systolic) as minSystolic,
            MAX(systolic) as maxSystolic,
            MIN(diastolic) as minDiastolic,
            MAX(diastolic) as maxDiastolic,
            AVG(pulse) as avgPulse,
            COUNT(*) as readingCount
        FROM blood_pressure
        WHERE userId = :userId
        AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
        GROUP BY strftime('%Y-%m', timestamp)
        ORDER BY month
    """)
    suspend fun getMonthlyBloodPressureTrends(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<MonthlyBpSummary>

    /**
     * Get blood pressure pattern by day of week
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return List of day-of-week blood pressure averages
     */
    @Query("""
        SELECT 
            strftime('%w', timestamp) as dayOfWeek,
            AVG(systolic) as avgSystolic,
            AVG(diastolic) as avgDiastolic,
            MIN(systolic) as minSystolic,
            MAX(systolic) as maxSystolic,
            MIN(diastolic) as minDiastolic,
            MAX(diastolic) as maxDiastolic,
            AVG(pulse) as avgPulse,
            COUNT(*) as readingCount
        FROM blood_pressure
        WHERE userId = :userId
        AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
        GROUP BY strftime('%w', timestamp)
        ORDER BY dayOfWeek
    """)
    suspend fun getBloodPressureByDayOfWeek(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<DayOfWeekBpSummary>

    /**
     * Get blood pressure pattern by hour of day
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return List of hour-of-day blood pressure averages
     */
    @Query("""
        SELECT 
            strftime('%H', timestamp) as hourOfDay,
            AVG(systolic) as avgSystolic,
            AVG(diastolic) as avgDiastolic,
            AVG(pulse) as avgPulse,
            COUNT(*) as readingCount
        FROM blood_pressure
        WHERE userId = :userId
        AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
        GROUP BY strftime('%H', timestamp)
        ORDER BY hourOfDay
    """)
    suspend fun getBloodPressureByHourOfDay(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<HourlyBpSummary>

    /**
     * Systolic and Diastolic Pressure Analysis
     */

    /**
     * Get isolated systolic hypertension events
     * (Systolic ≥ 140 mmHg and Diastolic < 90 mmHg)
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return List of isolated systolic hypertension events
     */
    @Query("""
        SELECT *
        FROM blood_pressure
        WHERE userId = :userId
        AND systolic >= 140
        AND diastolic < 90
        AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
        ORDER BY timestamp DESC
    """)
    suspend fun getIsolatedSystolicHypertension(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<BloodPressureEntity>

    /**
     * Get isolated diastolic hypertension events
     * (Systolic < 140 mmHg and Diastolic ≥ 90 mmHg)
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return List of isolated diastolic hypertension events
     */
    @Query("""
        SELECT *
        FROM blood_pressure
        WHERE userId = :userId
        AND systolic < 140
        AND diastolic >= 90
        AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
        ORDER BY timestamp DESC
    """)
    suspend fun getIsolatedDiastolicHypertension(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<BloodPressureEntity>

    /**
     * Get systolic and diastolic correlation
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return Correlation coefficient between systolic and diastolic
     */
    @Query("""
        WITH bp_data AS (
            SELECT 
                systolic,
                diastolic,
                AVG(systolic) OVER () as avg_systolic,
                AVG(diastolic) OVER () as avg_diastolic
            FROM blood_pressure
            WHERE userId = :userId
            AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
        ),
        covariance AS (
            SELECT 
                SUM((systolic - avg_systolic) * (diastolic - avg_diastolic)) / COUNT(*) as cov
            FROM bp_data
        ),
        std_dev AS (
            SELECT 
                SQRT(SUM((systolic - avg_systolic) * (systolic - avg_systolic)) / COUNT(*)) as std_systolic,
                SQRT(SUM((diastolic - avg_diastolic) * (diastolic - avg_diastolic)) / COUNT(*)) as std_diastolic
            FROM bp_data
        )
        SELECT 
            (SELECT cov FROM covariance) / ((SELECT std_systolic FROM std_dev) * (SELECT std_diastolic FROM std_dev)) as correlation
    """)
    suspend fun getSystolicDiastolicCorrelation(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Double?

    /**
     * Get systolic to diastolic ratio trend
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return List of daily systolic to diastolic ratios
     */
    @Query("""
        SELECT 
            date(timestamp) as date,
            AVG(systolic) / AVG(diastolic) as ratio,
            AVG(systolic) as avgSystolic,
            AVG(diastolic) as avgDiastolic,
            COUNT(*) as readingCount
        FROM blood_pressure
        WHERE userId = :userId
        AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
        GROUP BY date(timestamp)
        ORDER BY date(timestamp)
    """)
    suspend fun getSystolicDiastolicRatioTrend(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<DailySystolicDiastolicRatio>

    /**
     * Get systolic and diastolic pressure difference
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return List of daily systolic and diastolic pressure differences
     */
    @Query("""
        SELECT 
            date(timestamp) as date,
            AVG(systolic - diastolic) as avgDifference,
            MIN(systolic - diastolic) as minDifference,
            MAX(systolic - diastolic) as maxDifference
        FROM blood_pressure
        WHERE userId = :userId
        AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
        GROUP BY date(timestamp)
        ORDER BY date(timestamp)
    """)
    suspend fun getSystolicDiastolicDifference(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<DailySystolicDiastolicDifference>

    /**
     * Blood Pressure Medication Timing Correlation
     */

    /**
     * Get blood pressure readings around medication time
     * @param userId The user ID
     * @param medicationTime The time when medication is typically taken
     * @param hoursBefore Hours before medication time to include
     * @param hoursAfter Hours after medication time to include
     * @param startDate Start date
     * @param endDate End date
     * @return List of blood pressure readings around medication time
     */
    @Query("""
        SELECT *
        FROM blood_pressure
        WHERE userId = :userId
        AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
        AND (
            (
                time(timestamp) BETWEEN time(time(:medicationTime), '-' || :hoursBefore || ' hours') 
                AND time(:medicationTime)
            )
            OR
            (
                time(timestamp) BETWEEN time(:medicationTime) 
                AND time(time(:medicationTime), '+' || :hoursAfter || ' hours')
            )
        )
        ORDER BY timestamp
    """)
    suspend fun getReadingsAroundMedicationTime(
        userId: String,
        medicationTime: LocalTime,
        hoursBefore: Int = 2,
        hoursAfter: Int = 6,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<BloodPressureEntity>

    /**
     * Compare blood pressure before and after medication
     * @param userId The user ID
     * @param medicationTime The time when medication is typically taken
     * @param hoursBefore Hours before medication time to include
     * @param hoursAfter Hours after medication time to include
     * @param startDate Start date
     * @param endDate End date
     * @return Comparison of blood pressure before and after medication
     */
    @Query("""
        WITH before_med AS (
            SELECT 
                AVG(systolic) as avgSystolicBefore,
                AVG(diastolic) as avgDiastolicBefore,
                COUNT(*) as countBefore
            FROM blood_pressure
            WHERE userId = :userId
            AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
            AND time(timestamp) BETWEEN time(time(:medicationTime), '-' || :hoursBefore || ' hours') 
            AND time(:medicationTime)
        ),
        after_med AS (
            SELECT 
                AVG(systolic) as avgSystolicAfter,
                AVG(diastolic) as avgDiastolicAfter,
                COUNT(*) as countAfter
            FROM blood_pressure
            WHERE userId = :userId
            AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
            AND time(timestamp) BETWEEN time(:medicationTime) 
            AND time(time(:medicationTime), '+' || :hoursAfter || ' hours')
        )
        SELECT 
            (SELECT avgSystolicBefore FROM before_med) as avgSystolicBefore,
            (SELECT avgDiastolicBefore FROM before_med) as avgDiastolicBefore,
            (SELECT countBefore FROM before_med) as countBefore,
            (SELECT avgSystolicAfter FROM after_med) as avgSystolicAfter,
            (SELECT avgDiastolicAfter FROM after_med) as avgDiastolicAfter,
            (SELECT countAfter FROM after_med) as countAfter,
            (SELECT avgSystolicBefore - avgSystolicAfter FROM before_med, after_med) as systolicReduction,
            (SELECT avgDiastolicBefore - avgDiastolicAfter FROM before_med, after_med) as diastolicReduction
    """)
    suspend fun compareBpBeforeAfterMedication(
        userId: String,
        medicationTime: LocalTime,
        hoursBefore: Int = 2,
        hoursAfter: Int = 6,
        startDate: LocalDate,
        endDate: LocalDate
    ): MedicationEffectiveness?

    /**
     * Get medication effectiveness over time
     * @param userId The user ID
     * @param medicationTime The time when medication is typically taken
     * @param hoursBefore Hours before medication time to include
     * @param hoursAfter Hours after medication time to include
     * @param startDate Start date
     * @param endDate End date
     * @return Daily medication effectiveness metrics
     */
    @Query("""
        WITH daily_data AS (
            SELECT 
                date(timestamp) as date,
                CASE 
                    WHEN time(timestamp) BETWEEN time(time(:medicationTime), '-' || :hoursBefore || ' hours') 
                    AND time(:medicationTime) THEN 'before'
                    WHEN time(timestamp) BETWEEN time(:medicationTime) 
                    AND time(time(:medicationTime), '+' || :hoursAfter || ' hours') THEN 'after'
                    ELSE 'other'
                END as period,
                AVG(systolic) as avgSystolic,
                AVG(diastolic) as avgDiastolic
            FROM blood_pressure
            WHERE userId = :userId
            AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
            AND period IN ('before', 'after')
            GROUP BY date(timestamp), period
        ),
        before_data AS (
            SELECT date, avgSystolic as beforeSystolic, avgDiastolic as beforeDiastolic
            FROM daily_data
            WHERE period = 'before'
        ),
        after_data AS (
            SELECT date, avgSystolic as afterSystolic, avgDiastolic as afterDiastolic
            FROM daily_data
            WHERE period = 'after'
        )
        SELECT 
            b.date,
            b.beforeSystolic,
            b.beforeDiastolic,
            a.afterSystolic,
            a.afterDiastolic,
            (b.beforeSystolic - a.afterSystolic) as systolicReduction,
            (b.beforeDiastolic - a.afterDiastolic) as diastolicReduction
        FROM before_data b
        JOIN after_data a ON b.date = a.date
        ORDER BY b.date
    """)
    suspend fun getMedicationEffectivenessOverTime(
        userId: String,
        medicationTime: LocalTime,
        hoursBefore: Int = 2,
        hoursAfter: Int = 6,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<DailyMedicationEffectiveness>

    /**
     * Morning vs Evening Blood Pressure Patterns
     */

    /**
     * Compare morning and evening blood pressure
     * @param userId The user ID
     * @param morningStart Start time for morning period
     * @param morningEnd End time for morning period
     * @param eveningStart Start time for evening period
     * @param eveningEnd End time for evening period
     * @param startDate Start date
     * @param endDate End date
     * @return Comparison of morning and evening blood pressure
     */
    @Query("""
        WITH morning_bp AS (
            SELECT 
                AVG(systolic) as avgSystolicMorning,
                AVG(diastolic) as avgDiastolicMorning,
                COUNT(*) as countMorning
            FROM blood_pressure
            WHERE userId = :userId
            AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
            AND time(timestamp) BETWEEN :morningStart AND :morningEnd
        ),
        evening_bp AS (
            SELECT 
                AVG(systolic) as avgSystolicEvening,
                AVG(diastolic) as avgDiastolicEvening,
                COUNT(*) as countEvening
            FROM blood_pressure
            WHERE userId = :userId
            AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
            AND time(timestamp) BETWEEN :eveningStart AND :eveningEnd
        )
        SELECT 
            (SELECT avgSystolicMorning FROM morning_bp) as avgSystolicMorning,
            (SELECT avgDiastolicMorning FROM morning_bp) as avgDiastolicMorning,
            (SELECT countMorning FROM morning_bp) as countMorning,
            (SELECT avgSystolicEvening FROM evening_bp) as avgSystolicEvening,
            (SELECT avgDiastolicEvening FROM evening_bp) as avgDiastolicEvening,
            (SELECT countEvening FROM evening_bp) as countEvening,
            (SELECT avgSystolicMorning - avgSystolicEvening FROM morning_bp, evening_bp) as systolicDifference,
            (SELECT avgDiastolicMorning - avgDiastolicEvening FROM morning_bp, evening_bp) as diastolicDifference
    """)
    suspend fun compareMorningEveningBp(
        userId: String,
        morningStart: LocalTime = LocalTime.of(6, 0),
        morningEnd: LocalTime = LocalTime.of(10, 0),
        eveningStart: LocalTime = LocalTime.of(18, 0),
        eveningEnd: LocalTime = LocalTime.of(22, 0),
        startDate: LocalDate,
        endDate: LocalDate
    ): MorningEveningComparison?

    /**
     * Get morning-evening difference over time
     * @param userId The user ID
     * @param morningStart Start time for morning period
     * @param morningEnd End time for morning period
     * @param eveningStart Start time for evening period
     * @param eveningEnd End time for evening period
     * @param startDate Start date
     * @param endDate End date
     * @return Daily morning-evening difference metrics
     */
    @Query("""
        WITH daily_data AS (
            SELECT 
                date(timestamp) as date,
                CASE 
                    WHEN time(timestamp) BETWEEN :morningStart AND :morningEnd THEN 'morning'
                    WHEN time(timestamp) BETWEEN :eveningStart AND :eveningEnd THEN 'evening'
                    ELSE 'other'
                END as period,
                AVG(systolic) as avgSystolic,
                AVG(diastolic) as avgDiastolic
            FROM blood_pressure
            WHERE userId = :userId
            AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
            AND period IN ('morning', 'evening')
            GROUP BY date(timestamp), period
        ),
        morning_data AS (
            SELECT date, avgSystolic as morningSystolic, avgDiastolic as morningDiastolic
            FROM daily_data
            WHERE period = 'morning'
        ),
        evening_data AS (
            SELECT date, avgSystolic as eveningSystolic, avgDiastolic as eveningDiastolic
            FROM daily_data
            WHERE period = 'evening'
        )
        SELECT 
            m.date,
            m.morningSystolic,
            m.morningDiastolic,
            e.eveningSystolic,
            e.eveningDiastolic,
            (m.morningSystolic - e.eveningSystolic) as systolicDifference,
            (m.morningDiastolic - e.eveningDiastolic) as diastolicDifference
        FROM morning_data m
        JOIN evening_data e ON m.date = e.date
        ORDER BY m.date
    """)
    suspend fun getMorningEveningDifferenceOverTime(
        userId: String,
        morningStart: LocalTime = LocalTime.of(6, 0),
        morningEnd: LocalTime = LocalTime.of(10, 0),
        eveningStart: LocalTime = LocalTime.of(18, 0),
        eveningEnd: LocalTime = LocalTime.of(22, 0),
        startDate: LocalDate,
        endDate: LocalDate
    ): List<DailyMorningEveningDifference>

    /**
     * Detect morning hypertension (morning surge)
     * @param userId The user ID
     * @param morningStart Start time for morning period
     * @param morningEnd End time for morning period
     * @param threshold Threshold for systolic increase to consider as morning surge
     * @param startDate Start date
     * @param endDate End date
     * @return List of days with morning hypertension
     */
    @Query("""
        WITH daily_data AS (
            SELECT 
                date(timestamp) as date,
                CASE 
                    WHEN time(timestamp) BETWEEN :morningStart AND :morningEnd THEN 'morning'
                    ELSE 'other'
                END as period,
                AVG(systolic) as avgSystolic,
                AVG(diastolic) as avgDiastolic
            FROM blood_pressure
            WHERE userId = :userId
            AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
            GROUP BY date(timestamp), period
        ),
        morning_data AS (
            SELECT date, avgSystolic as morningSystolic, avgDiastolic as morningDiastolic
            FROM daily_data
            WHERE period = 'morning'
        ),
        other_data AS (
            SELECT date, avgSystolic as otherSystolic, avgDiastolic as otherDiastolic
            FROM daily_data
            WHERE period = 'other'
        )
        SELECT 
            m.date,
            m.morningSystolic,
            m.morningDiastolic,
            o.otherSystolic,
            o.otherDiastolic,
            (m.morningSystolic - o.otherSystolic) as systolicSurge,
            (m.morningDiastolic - o.otherDiastolic) as diastolicSurge
        FROM morning_data m
        JOIN other_data o ON m.date = o.date
        WHERE (m.morningSystolic - o.otherSystolic) > :threshold
        ORDER BY systolicSurge DESC
    """)
    suspend fun detectMorningHypertension(
        userId: String,
        morningStart: LocalTime = LocalTime.of(6, 0),
        morningEnd: LocalTime = LocalTime.of(10, 0),
        threshold: Int = 15,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<MorningHypertension>

    /**
     * Blood Pressure Risk Assessment
     */

    /**
     * Calculate cardiovascular risk score based on blood pressure
     * This is a simplified risk score based on blood pressure categories
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return Risk score and category
     */
    @Query("""
        WITH bp_categories AS (
            SELECT
                CASE
                    WHEN systolic < 120 AND diastolic < 80 THEN 0
                    WHEN systolic BETWEEN 120 AND 129 AND diastolic < 80 THEN 1
                    WHEN (systolic BETWEEN 130 AND 139) OR (diastolic BETWEEN 80 AND 89) THEN 2
                    WHEN (systolic BETWEEN 140 AND 159) OR (diastolic BETWEEN 90 AND 99) THEN 3
                    WHEN (systolic BETWEEN 160 AND 180) OR (diastolic BETWEEN 100 AND 120) THEN 4
                    WHEN systolic > 180 OR diastolic > 120 THEN 5
                    ELSE 0
                END as risk_score
            FROM blood_pressure
            WHERE userId = :userId
            AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
        )
        SELECT 
            AVG(risk_score) as riskScore,
            CASE
                WHEN AVG(risk_score) < 0.5 THEN 'Low Risk'
                WHEN AVG(risk_score) < 1.5 THEN 'Moderate Risk'
                WHEN AVG(risk_score) < 2.5 THEN 'Increased Risk'
                WHEN AVG(risk_score) < 3.5 THEN 'High Risk'
                WHEN AVG(risk_score) < 4.5 THEN 'Very High Risk'
                ELSE 'Severe Risk'
            END as riskCategory,
            COUNT(*) as readingCount
        FROM bp_categories
    """)
    suspend fun calculateBpRiskScore(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): BpRiskAssessment?

    /**
     * Calculate blood pressure load
     * BP load is the percentage of readings that exceed a threshold
     * @param userId The user ID
     * @param systolicThreshold Systolic threshold (typically 140 mmHg)
     * @param diastolicThreshold Diastolic threshold (typically 90 mmHg)
     * @param startDate Start date
     * @param endDate End date
     * @return Blood pressure load metrics
     */
    @Query("""
        WITH bp_data AS (
            SELECT 
                CASE WHEN systolic >= :systolicThreshold THEN 1 ELSE 0 END as systolic_high,
                CASE WHEN diastolic >= :diastolicThreshold THEN 1 ELSE 0 END as diastolic_high,
                CASE WHEN systolic >= :systolicThreshold OR diastolic >= :diastolicThreshold THEN 1 ELSE 0 END as either_high
            FROM blood_pressure
            WHERE userId = :userId
            AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
        )
        SELECT 
            COUNT(*) as totalReadings,
            SUM(systolic_high) as systolicHighCount,
            SUM(diastolic_high) as diastolicHighCount,
            SUM(either_high) as eitherHighCount,
            (SUM(systolic_high) * 100.0 / COUNT(*)) as systolicLoadPercent,
            (SUM(diastolic_high) * 100.0 / COUNT(*)) as diastolicLoadPercent,
            (SUM(either_high) * 100.0 / COUNT(*)) as totalLoadPercent
        FROM bp_data
    """)
    suspend fun calculateBpLoad(
        userId: String,
        systolicThreshold: Int = 140,
        diastolicThreshold: Int = 90,
        startDate: LocalDate,
        endDate: LocalDate
    ): BpLoad?

    /**
     * Check for nocturnal hypertension
     * @param userId The user ID
     * @param nightStart Start time for night period
     * @param nightEnd End time for night period
     * @param systolicThreshold Systolic threshold (typically 120 mmHg at night)
     * @param diastolicThreshold Diastolic threshold (typically 70 mmHg at night)
     * @param startDate Start date
     * @param endDate End date
     * @return Nocturnal hypertension metrics
     */
    @Query("""
        WITH night_bp AS (
            SELECT *
            FROM blood_pressure
            WHERE userId = :userId
            AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
            AND (
                (time(timestamp) >= :nightStart) OR
                (time(timestamp) < :nightEnd)
            )
        ),
        high_readings AS (
            SELECT 
                COUNT(*) as highCount
            FROM night_bp
            WHERE systolic >= :systolicThreshold OR diastolic >= :diastolicThreshold
        )
        SELECT 
            COUNT(*) as totalNightReadings,
            (SELECT highCount FROM high_readings) as highReadings,
            ((SELECT highCount FROM high_readings) * 100.0 / COUNT(*)) as percentHigh,
            AVG(systolic) as avgNightSystolic,
            AVG(diastolic) as avgNightDiastolic
        FROM night_bp
    """)
    suspend fun checkNocturnalHypertension(
        userId: String,
        nightStart: LocalTime = LocalTime.of(22, 0),
        nightEnd: LocalTime = LocalTime.of(6, 0),
        systolicThreshold: Int = 120,
        diastolicThreshold: Int = 70,
        startDate: LocalDate,
        endDate: LocalDate
    ): NocturnalHypertension?

    /**
     * Check for white coat hypertension
     * (Higher BP at doctor's office compared to home)
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return Comparison of clinical vs home measurements
     */
    @Query("""
        WITH clinical_bp AS (
            SELECT 
                AVG(systolic) as avgSystolicClinical,
                AVG(diastolic) as avgDiastolicClinical,
                COUNT(*) as countClinical
            FROM blood_pressure
            WHERE userId = :userId
            AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
            AND tags LIKE '%clinical%'
        ),
        home_bp AS (
            SELECT 
                AVG(systolic) as avgSystolicHome,
                AVG(diastolic) as avgDiastolicHome,
                COUNT(*) as countHome
            FROM blood_pressure
            WHERE userId = :userId
            AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
            AND tags LIKE '%home%'
        )
        SELECT 
            (SELECT avgSystolicClinical FROM clinical_bp) as avgSystolicClinical,
            (SELECT avgDiastolicClinical FROM clinical_bp) as avgDiastolicClinical,
            (SELECT countClinical FROM clinical_bp) as countClinical,
            (SELECT avgSystolicHome FROM home_bp) as avgSystolicHome,
            (SELECT avgDiastolicHome FROM home_bp) as avgDiastolicHome,
            (SELECT countHome FROM home_bp) as countHome,
            (SELECT avgSystolicClinical - avgSystolicHome FROM clinical_bp, home_bp) as systolicDifference,
            (SELECT avgDiastolicClinical - avgDiastolicHome FROM clinical_bp, home_bp) as diastolicDifference,
            CASE 
                WHEN (avgSystolicClinical - avgSystolicHome >= 20) OR (avgDiastolicClinical - avgDiastolicHome >= 10) 
                THEN 'Likely White Coat Hypertension'
                WHEN (avgSystolicClinical - avgSystolicHome >= 10) OR (avgDiastolicClinical - avgDiastolicHome >= 5) 
                THEN 'Possible White Coat Hypertension'
                ELSE 'No White Coat Effect'
            END as assessment
        FROM clinical_bp, home_bp
        WHERE countClinical > 0 AND countHome > 0
    """)
    suspend fun checkWhiteCoatHypertension(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): WhiteCoatAssessment?

    /**
     * Pulse Pressure Analysis
     */

    /**
     * Calculate pulse pressure (systolic - diastolic)
     * Normal pulse pressure is typically 40-60 mmHg
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return Pulse pressure metrics
     */
    @Query("""
        SELECT 
            AVG(systolic - diastolic) as avgPulsePressure,
            MIN(systolic - diastolic) as minPulsePressure,
            MAX(systolic - diastolic) as maxPulsePressure,
            COUNT(*) as readingCount,
            SUM(CASE WHEN (systolic - diastolic) < 40 THEN 1 ELSE 0 END) as lowPpCount,
            SUM(CASE WHEN (systolic - diastolic) BETWEEN 40 AND 60 THEN 1 ELSE 0 END) as normalPpCount,
            SUM(CASE WHEN (systolic - diastolic) > 60 THEN 1 ELSE 0 END) as highPpCount,
            (SUM(CASE WHEN (systolic - diastolic) < 40 THEN 1 ELSE 0 END) * 100.0 / COUNT(*)) as percentLowPp,
            (SUM(CASE WHEN (systolic - diastolic) BETWEEN 40 AND 60 THEN 1 ELSE 0 END) * 100.0 / COUNT(*)) as percentNormalPp,
            (SUM(CASE WHEN (systolic - diastolic) > 60 THEN 1 ELSE 0 END) * 100.0 / COUNT(*)) as percentHighPp
        FROM blood_pressure
        WHERE userId = :userId
        AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
    """)
    suspend fun analyzePulsePressure(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): PulsePressureMetrics?

    /**
     * Get pulse pressure trend over time
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return List of daily pulse pressure values
     */
    @Query("""
        SELECT 
            date(timestamp) as date,
            AVG(systolic - diastolic) as avgPulsePressure,
            MIN(systolic - diastolic) as minPulsePressure,
            MAX(systolic - diastolic) as maxPulsePressure,
            COUNT(*) as readingCount
        FROM blood_pressure
        WHERE userId = :userId
        AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
        GROUP BY date(timestamp)
        ORDER BY date(timestamp)
    """)
    suspend fun getPulsePressureTrend(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<DailyPulsePressure>

    /**
     * Get widened pulse pressure events
     * @param userId The user ID
     * @param threshold Threshold for widened pulse pressure (typically > 60 mmHg)
     * @param startDate Start date
     * @param endDate End date
     * @return List of readings with widened pulse pressure
     */
    @Query("""
        SELECT *
        FROM blood_pressure
        WHERE userId = :userId
        AND (systolic - diastolic) > :threshold
        AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
        ORDER BY (systolic - diastolic) DESC
    """)
    suspend fun getWidenedPulsePressureEvents(
        userId: String,
        threshold: Int = 60,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<BloodPressureEntity>

    /**
     * Get narrowed pulse pressure events
     * @param userId The user ID
     * @param threshold Threshold for narrowed pulse pressure (typically < 40 mmHg)
     * @param startDate Start date
     * @param endDate End date
     * @return List of readings with narrowed pulse pressure
     */
    @Query("""
        SELECT *
        FROM blood_pressure
        WHERE userId = :userId
        AND (systolic - diastolic) < :threshold
        AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
        ORDER BY (systolic - diastolic) ASC
    """)
    suspend fun getNarrowedPulsePressureEvents(
        userId: String,
        threshold: Int = 40,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<BloodPressureEntity>

    /**
     * Blood Pressure Variability Metrics
     */

    /**
     * Calculate blood pressure variability
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return Blood pressure variability metrics
     */
    @Query("""
        WITH bp_data AS (
            SELECT 
                date(timestamp) as date,
                systolic,
                diastolic
            FROM blood_pressure
            WHERE userId = :userId
            AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
        ),
        daily_stats AS (
            SELECT 
                date,
                AVG(systolic) as avg_systolic,
                AVG(diastolic) as avg_diastolic,
                SQRT(AVG((systolic - AVG(systolic)) * (systolic - AVG(systolic)))) as std_systolic,
                SQRT(AVG((diastolic - AVG(diastolic)) * (diastolic - AVG(diastolic)))) as std_diastolic,
                MAX(systolic) - MIN(systolic) as range_systolic,
                MAX(diastolic) - MIN(diastolic) as range_diastolic,
                COUNT(*) as reading_count
            FROM bp_data
            GROUP BY date
        )
        SELECT 
            AVG(std_systolic) as avgSystolicSD,
            AVG(std_diastolic) as avgDiastolicSD,
            AVG(range_systolic) as avgSystolicRange,
            AVG(range_diastolic) as avgDiastolicRange,
            MAX(std_systolic) as maxSystolicSD,
            MAX(std_diastolic) as maxDiastolicSD,
            (AVG(std_systolic) / AVG(avg_systolic) * 100) as systolicCoefficientOfVariation,
            (AVG(std_diastolic) / AVG(avg_diastolic) * 100) as diastolicCoefficientOfVariation
        FROM daily_stats
        WHERE reading_count > 1
    """)
    suspend fun calculateBpVariability(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): BpVariabilityMetrics?

    /**
     * Get day-to-day blood pressure variability
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return List of day-to-day blood pressure changes
     */
    @Query("""
        WITH daily_avg AS (
            SELECT 
                date(timestamp) as date,
                AVG(systolic) as avg_systolic,
                AVG(diastolic) as avg_diastolic
            FROM blood_pressure
            WHERE userId = :userId
            AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
            GROUP BY date(timestamp)
        ),
        day_pairs AS (
            SELECT 
                d1.date as date1,
                d2.date as date2,
                d1.avg_systolic as systolic1,
                d2.avg_systolic as systolic2,
                d1.avg_diastolic as diastolic1,
                d2.avg_diastolic as diastolic2,
                ABS(d2.avg_systolic - d1.avg_systolic) as systolic_change,
                ABS(d2.avg_diastolic - d1.avg_diastolic) as diastolic_change
            FROM daily_avg d1
            JOIN daily_avg d2 ON d2.date = date(d1.date, '+1 day')
        )
        SELECT 
            date1 as date,
            systolic1 as systolicDay1,
            systolic2 as systolicDay2,
            diastolic1 as diastolicDay1,
            diastolic2 as diastolicDay2,
            systolic_change as systolicChange,
            diastolic_change as diastolicChange
        FROM day_pairs
        ORDER BY date1
    """)
    suspend fun getDayToDayBpVariability(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<DayToDayBpChange>

    /**
     * Detect excessive blood pressure variability
     * @param userId The user ID
     * @param systolicThreshold Threshold for excessive systolic variability
     * @param diastolicThreshold Threshold for excessive diastolic variability
     * @param startDate Start date
     * @param endDate End date
     * @return List of days with excessive blood pressure variability
     */
    @Query("""
        WITH daily_stats AS (
            SELECT 
                date(timestamp) as date,
                SQRT(AVG((systolic - AVG(systolic)) * (systolic - AVG(systolic)))) as std_systolic,
                SQRT(AVG((diastolic - AVG(diastolic)) * (diastolic - AVG(diastolic)))) as std_diastolic,
                MAX(systolic) - MIN(systolic) as range_systolic,
                MAX(diastolic) - MIN(diastolic) as range_diastolic,
                COUNT(*) as reading_count
            FROM blood_pressure
            WHERE userId = :userId
            AND date(timestamp) BETWEEN date(:startDate) AND date(:endDate)
            GROUP BY date(timestamp)
        )
        SELECT 
            date,
            std_systolic as systolicSD,
            std_diastolic as diastolicSD,
            range_systolic as systolicRange,
            range_diastolic as diastolicRange,
            reading_count as readingCount
        FROM daily_stats
        WHERE (std_systolic > :systolicThreshold OR std_diastolic > :diastolicThreshold)
        AND reading_count > 1
        ORDER BY (std_systolic + std_diastolic) DESC
    """)
    suspend fun detectExcessiveBpVariability(
        userId: String,
        systolicThreshold: Double = 15.0,
        diastolicThreshold: Double = 10.0,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<DailyBpVariability>

    /**
     * Data classes for query results
     */

    /**
     * Blood pressure category distribution
     */
    data class BpCategory(
        val category: String,
        val count: Int,
        val avgSystolic: Double,
        val avgDiastolic: Double,
        val minSystolic: Int,
        val maxSystolic: Int,
        val minDiastolic: Int,
        val maxDiastolic: Int
    )

    /**
     * Blood pressure category percentage
     */
    data class BpCategoryPercentage(
        val category: String,
        val count: Int,
        val percentage: Double
    )

    /**
     * Daily blood pressure summary
     */
    data class DailyBpSummary(
        val date: LocalDate,
        val avgSystolic: Double,
        val avgDiastolic: Double,
        val minSystolic: Int,
        val maxSystolic: Int,
        val minDiastolic: Int,
        val maxDiastolic: Int,
        val avgPulse: Double?,
        val readingCount: Int
    )

    /**
     * Weekly blood pressure summary
     */
    data class WeeklyBpSummary(
        val week: String,
        val avgSystolic: Double,
        val avgDiastolic: Double,
        val minSystolic: Int,
        val maxSystolic: Int,
        val minDiastolic: Int,
        val maxDiastolic: Int,
        val avgPulse: Double?,
        val readingCount: Int
    )

    /**
     * Monthly blood pressure summary
     */
    data class MonthlyBpSummary(
        val month: String,
        val avgSystolic: Double,
        val avgDiastolic: Double,
        val minSystolic: Int,
        val maxSystolic: Int,
        val minDiastolic: Int,
        val maxDiastolic: Int,
        val avgPulse: Double?,
        val readingCount: Int
    )

    /**
     * Day of week blood pressure summary
     */
    data class DayOfWeekBpSummary(
        val dayOfWeek: String,
        val avgSystolic: Double,
        val avgDiastolic: Double,
        val minSystolic: Int,
        val maxSystolic: Int,
        val minDiastolic: Int,
        val maxDiastolic: Int,
        val avgPulse: Double?,
        val readingCount: Int
    )

    /**
     * Hourly blood pressure summary
     */
    data class HourlyBpSummary(
        val hourOfDay: String,
        val avgSystolic: Double,
        val avgDiastolic: Double,
        val avgPulse: Double?,
        val readingCount: Int
    )

    /**
     * Daily systolic to diastolic ratio
     */
    data class DailySystolicDiastolicRatio(
        val date: LocalDate,
        val ratio: Double,
        val avgSystolic: Double,
        val avgDiastolic: Double,
        val readingCount: Int
    )

    /**
     * Daily systolic to diastolic difference
     */
    data class DailySystolicDiastolicDifference(
        val date: LocalDate,
        val avgDifference: Double,
        val minDifference: Int,
        val maxDifference: Int
    )

    /**
     * Medication effectiveness metrics
     */
    data class MedicationEffectiveness(
        val avgSystolicBefore: Double?,
        val avgDiastolicBefore: Double?,
        val countBefore: Int?,
        val avgSystolicAfter: Double?,
        val avgDiastolicAfter: Double?,
        val countAfter: Int?,
        val systolicReduction: Double?,
        val diastolicReduction: Double?
    )

    /**
     * Daily medication effectiveness
     */
    data class DailyMedicationEffectiveness(
        val date: LocalDate,
        val beforeSystolic: Double,
        val beforeDiastolic: Double,
        val afterSystolic: Double,
        val afterDiastolic: Double,
        val systolicReduction: Double,
        val diastolicReduction: Double
    )

    /**
     * Morning vs evening comparison
     */
    data class MorningEveningComparison(
        val avgSystolicMorning: Double?,
        val avgDiastolicMorning: Double?,
        val countMorning: Int?,
        val avgSystolicEvening: Double?,
        val avgDiastolicEvening: Double?,
        val countEvening: Int?,
        val systolicDifference: Double?,
        val diastolicDifference: Double?
    )

    /**
     * Daily morning vs evening difference
     */
    data class DailyMorningEveningDifference(
        val date: LocalDate,
        val morningSystolic: Double,
        val morningDiastolic: Double,
        val eveningSystolic: Double,
        val eveningDiastolic: Double,
        val systolicDifference: Double,
        val diastolicDifference: Double
    )

    /**
     * Morning hypertension metrics
     */
    data class MorningHypertension(
        val date: LocalDate,
        val morningSystolic: Double,
        val morningDiastolic: Double,
        val otherSystolic: Double,
        val otherDiastolic: Double,
        val systolicSurge: Double,
        val diastolicSurge: Double
    )

    /**
     * Blood pressure risk assessment
     */
    data class BpRiskAssessment(
        val riskScore: Double,
        val riskCategory: String,
        val readingCount: Int
    )

    /**
     * Blood pressure load metrics
     */
    data class BpLoad(
        val totalReadings: Int,
        val systolicHighCount: Int,
        val diastolicHighCount: Int,
        val eitherHighCount: Int,
        val systolicLoadPercent: Double,
        val diastolicLoadPercent: Double,
        val totalLoadPercent: Double
    )

    /**
     * Nocturnal hypertension metrics
     */
    data class NocturnalHypertension(
        val totalNightReadings: Int,
        val highReadings: Int,
        val percentHigh: Double,
        val avgNightSystolic: Double,
        val avgNightDiastolic: Double
    )

    /**
     * White coat hypertension assessment
     */
    data class WhiteCoatAssessment(
        val avgSystolicClinical: Double?,
        val avgDiastolicClinical: Double?,
        val countClinical: Int?,
        val avgSystolicHome: Double?,
        val avgDiastolicHome: Double?,
        val countHome: Int?,
        val systolicDifference: Double?,
        val diastolicDifference: Double?,
        val assessment: String
    )

    /**
     * Pulse pressure metrics
     */
    data class PulsePressureMetrics(
        val avgPulsePressure: Double,
        val minPulsePressure: Int,
        val maxPulsePressure: Int,
        val readingCount: Int,
        val lowPpCount: Int,
        val normalPpCount: Int,
        val highPpCount: Int,
        val percentLowPp: Double,
        val percentNormalPp: Double,
        val percentHighPp: Double
    )

    /**
     * Daily pulse pressure
     */
    data class DailyPulsePressure(
        val date: LocalDate,
        val avgPulsePressure: Double,
        val minPulsePressure: Int,
        val maxPulsePressure: Int,
        val readingCount: Int
    )

    /**
     * Blood pressure variability metrics
     */
    data class BpVariabilityMetrics(
        val avgSystolicSD: Double,
        val avgDiastolicSD: Double,
        val avgSystolicRange: Double,
        val avgDiastolicRange: Double,
        val maxSystolicSD: Double,
        val maxDiastolicSD: Double,
        val systolicCoefficientOfVariation: Double,
        val diastolicCoefficientOfVariation: Double
    )

    /**
     * Day-to-day blood pressure change
     */
    data class DayToDayBpChange(
        val date: LocalDate,
        val systolicDay1: Double,
        val systolicDay2: Double,
        val diastolicDay1: Double,
        val diastolicDay2: Double,
        val systolicChange: Double,
        val diastolicChange: Double
    )

    /**
     * Daily blood pressure variability
     */
    data class DailyBpVariability(
        val date: LocalDate,
        val systolicSD: Double,
        val diastolicSD: Double,
        val systolicRange: Int,
        val diastolicRange: Int,
        val readingCount: Int
    )
}
