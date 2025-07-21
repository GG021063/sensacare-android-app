package com.sensacare.app.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Base interface for all domain models
 */
interface DomainModel {
    val id: Long
}

/**
 * Sync status enum for tracking synchronization state
 */
enum class SyncStatus {
    PENDING,
    SYNCING,
    SYNCED,
    FAILED,
    NOT_SYNCED;

    fun isPendingSync(): Boolean = this == PENDING || this == FAILED
}

/**
 * Gender enum
 */
enum class Gender {
    MALE,
    FEMALE,
    OTHER;

    companion object {
        fun fromString(value: String?): Gender {
            return when (value?.uppercase()) {
                "MALE", "M" -> MALE
                "FEMALE", "F" -> FEMALE
                else -> OTHER
            }
        }
    }
}

/**
 * Day of week enum
 */
enum class DayOfWeek {
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY,
    SUNDAY;

    companion object {
        fun fromJavaTime(dayOfWeek: java.time.DayOfWeek): DayOfWeek {
            return when (dayOfWeek) {
                java.time.DayOfWeek.MONDAY -> MONDAY
                java.time.DayOfWeek.TUESDAY -> TUESDAY
                java.time.DayOfWeek.WEDNESDAY -> WEDNESDAY
                java.time.DayOfWeek.THURSDAY -> THURSDAY
                java.time.DayOfWeek.FRIDAY -> FRIDAY
                java.time.DayOfWeek.SATURDAY -> SATURDAY
                java.time.DayOfWeek.SUNDAY -> SUNDAY
            }
        }

        fun today(): DayOfWeek {
            return fromJavaTime(LocalDate.now().dayOfWeek)
        }
    }

    fun toJavaTime(): java.time.DayOfWeek {
        return when (this) {
            MONDAY -> java.time.DayOfWeek.MONDAY
            TUESDAY -> java.time.DayOfWeek.TUESDAY
            WEDNESDAY -> java.time.DayOfWeek.WEDNESDAY
            THURSDAY -> java.time.DayOfWeek.THURSDAY
            FRIDAY -> java.time.DayOfWeek.FRIDAY
            SATURDAY -> java.time.DayOfWeek.SATURDAY
            SUNDAY -> java.time.DayOfWeek.SUNDAY
        }
    }

    fun isWeekend(): Boolean = this == SATURDAY || this == SUNDAY
    fun isWeekday(): Boolean = !isWeekend()
}

/**
 * Device model representing a connected health device
 */
data class Device(
    override val id: Long,
    val name: String,
    val macAddress: String,
    val deviceType: String,
    val firmwareVersion: String,
    val hardwareVersion: String,
    val lastConnected: LocalDateTime? = null,
    val batteryLevel: Int? = null,
    val isActive: Boolean = true,
    val features: Set<DeviceFeature> = emptySet(),
    val syncStatus: SyncStatus = SyncStatus.NOT_SYNCED
) : DomainModel {

    /**
     * Check if the device is currently connected
     * @return true if the device is connected, false otherwise
     */
    fun isConnected(): Boolean {
        return lastConnected != null && 
               ChronoUnit.MINUTES.between(lastConnected, LocalDateTime.now()) < 5
    }

    /**
     * Check if the device has a low battery
     * @param threshold Battery level threshold (default: 20%)
     * @return true if the device has a low battery, false otherwise
     */
    fun hasLowBattery(threshold: Int = 20): Boolean {
        return batteryLevel != null && batteryLevel < threshold
    }

    /**
     * Check if the device supports a specific feature
     * @param feature Feature to check
     * @return true if the device supports the feature, false otherwise
     */
    fun supportsFeature(feature: DeviceFeature): Boolean {
        return features.contains(feature)
    }

    /**
     * Get the connection status as a string
     * @return Connection status as a string
     */
    fun getConnectionStatus(): String {
        return when {
            isConnected() -> "Connected"
            lastConnected == null -> "Never Connected"
            else -> {
                val minutes = ChronoUnit.MINUTES.between(lastConnected, LocalDateTime.now())
                when {
                    minutes < 60 -> "$minutes minutes ago"
                    minutes < 24 * 60 -> "${minutes / 60} hours ago"
                    else -> "${minutes / (24 * 60)} days ago"
                }
            }
        }
    }

    /**
     * Get the battery status as a string
     * @return Battery status as a string
     */
    fun getBatteryStatus(): String {
        return when {
            batteryLevel == null -> "Unknown"
            batteryLevel <= 10 -> "Critical"
            batteryLevel <= 20 -> "Low"
            batteryLevel <= 50 -> "Medium"
            else -> "Good"
        }
    }

    companion object {
        /**
         * Create a device from a MAC address and name
         * @param macAddress MAC address
         * @param name Device name
         * @return Device with default values
         */
        fun createFromMacAddress(macAddress: String, name: String): Device {
            return Device(
                id = 0,
                name = name,
                macAddress = macAddress,
                deviceType = "Unknown",
                firmwareVersion = "Unknown",
                hardwareVersion = "Unknown",
                isActive = true
            )
        }
    }
}

/**
 * Device feature enum
 */
enum class DeviceFeature {
    HEART_RATE,
    BLOOD_OXYGEN,
    BLOOD_PRESSURE,
    STEPS,
    SLEEP,
    ECG,
    TEMPERATURE,
    NOTIFICATIONS,
    WEATHER,
    FIND_PHONE,
    CAMERA_CONTROL,
    MUSIC_CONTROL;

    companion object {
        fun fromString(value: String): DeviceFeature? {
            return try {
                valueOf(value.uppercase())
            } catch (e: IllegalArgumentException) {
                null
            }
        }

        fun fromStringSet(values: Set<String>): Set<DeviceFeature> {
            return values.mapNotNull { fromString(it) }.toSet()
        }
    }
}

/**
 * User model representing a user profile
 */
data class User(
    override val id: Long,
    val name: String,
    val email: String,
    val dateOfBirth: LocalDate? = null,
    val gender: Gender? = null,
    val heightCm: Int? = null,
    val weightKg: Float? = null,
    val stepLengthCm: Int? = null,
    val targetSteps: Int = 10000,
    val profileImageUri: String? = null,
    val preferredDeviceId: Long? = null
) : DomainModel {

    /**
     * Get the user's age
     * @return User's age in years, or null if date of birth is not set
     */
    fun getAge(): Int? {
        return dateOfBirth?.let { ChronoUnit.YEARS.between(it, LocalDate.now()).toInt() }
    }

    /**
     * Get the user's BMI (Body Mass Index)
     * @return User's BMI, or null if height or weight is not set
     */
    fun getBMI(): Float? {
        return if (heightCm != null && weightKg != null && heightCm > 0) {
            val heightMeters = heightCm / 100f
            weightKg / (heightMeters * heightMeters)
        } else {
            null
        }
    }

    /**
     * Get the user's BMI category
     * @return User's BMI category, or null if BMI is not available
     */
    fun getBMICategory(): String? {
        return getBMI()?.let { bmi ->
            when {
                bmi < 18.5f -> "Underweight"
                bmi < 25f -> "Normal"
                bmi < 30f -> "Overweight"
                else -> "Obese"
            }
        }
    }

    /**
     * Get the user's estimated basal metabolic rate (BMR)
     * @return User's BMR in calories per day, or null if required data is not available
     */
    fun getBasalMetabolicRate(): Float? {
        if (heightCm == null || weightKg == null || gender == null || getAge() == null) {
            return null
        }

        // Mifflin-St Jeor Equation
        return when (gender) {
            Gender.MALE -> (10f * weightKg) + (6.25f * heightCm) - (5f * getAge()!!) + 5f
            Gender.FEMALE -> (10f * weightKg) + (6.25f * heightCm) - (5f * getAge()!!) - 161f
            else -> (10f * weightKg) + (6.25f * heightCm) - (5f * getAge()!!) - 78f // Average
        }
    }

    /**
     * Get the user's estimated maximum heart rate
     * @return User's maximum heart rate in beats per minute, or null if age is not available
     */
    fun getMaxHeartRate(): Int? {
        return getAge()?.let { 220 - it }
    }

    /**
     * Get the user's heart rate zones
     * @return User's heart rate zones, or null if maximum heart rate is not available
     */
    fun getHeartRateZones(): HeartRateZones? {
        return getMaxHeartRate()?.let { maxHr ->
            HeartRateZones(
                zone1Range = Pair(0, (maxHr * 0.6).toInt()),
                zone2Range = Pair((maxHr * 0.6).toInt() + 1, (maxHr * 0.7).toInt()),
                zone3Range = Pair((maxHr * 0.7).toInt() + 1, (maxHr * 0.8).toInt()),
                zone4Range = Pair((maxHr * 0.8).toInt() + 1, (maxHr * 0.9).toInt()),
                zone5Range = Pair((maxHr * 0.9).toInt() + 1, maxHr)
            )
        }
    }

    /**
     * Check if the user profile is complete
     * @return true if the user profile is complete, false otherwise
     */
    fun isProfileComplete(): Boolean {
        return name.isNotBlank() && email.isNotBlank() && dateOfBirth != null && 
               gender != null && heightCm != null && weightKg != null
    }

    /**
     * Validate the user profile
     * @return List of validation errors, or empty list if the profile is valid
     */
    fun validate(): List<String> {
        val errors = mutableListOf<String>()

        if (name.isBlank()) {
            errors.add("Name is required")
        }

        if (email.isBlank()) {
            errors.add("Email is required")
        } else if (!email.matches(Regex("^[A-Za-z0-9+_.-]+@(.+)\$"))) {
            errors.add("Email is invalid")
        }

        if (dateOfBirth != null && dateOfBirth.isAfter(LocalDate.now())) {
            errors.add("Date of birth cannot be in the future")
        }

        if (heightCm != null && (heightCm < 50 || heightCm > 250)) {
            errors.add("Height must be between 50 and 250 cm")
        }

        if (weightKg != null && (weightKg < 20 || weightKg > 300)) {
            errors.add("Weight must be between 20 and 300 kg")
        }

        return errors
    }
}

/**
 * Heart rate status enum
 */
enum class HeartRateStatus {
    NORMAL,
    EXERCISE,
    RESTING,
    UNKNOWN;

    companion object {
        fun fromString(value: String?): HeartRateStatus {
            return when (value?.uppercase()) {
                "NORMAL" -> NORMAL
                "EXERCISE" -> EXERCISE
                "RESTING" -> RESTING
                else -> UNKNOWN
            }
        }
    }
}

/**
 * Heart rate model representing a heart rate measurement
 */
data class HeartRate(
    override val id: Long,
    val deviceId: Long,
    val timestamp: LocalDateTime,
    val heartRate: Int,
    val status: HeartRateStatus = HeartRateStatus.NORMAL,
    val isManualEntry: Boolean = false,
    val notes: String? = null,
    val syncStatus: SyncStatus = SyncStatus.PENDING
) : DomainModel {

    /**
     * Get the heart rate zone
     * @param maxHeartRate Maximum heart rate
     * @return Heart rate zone (1-5)
     */
    fun getZone(maxHeartRate: Int): Int {
        val percentage = heartRate.toDouble() / maxHeartRate.toDouble()
        return when {
            percentage < 0.6 -> 1
            percentage < 0.7 -> 2
            percentage < 0.8 -> 3
            percentage < 0.9 -> 4
            else -> 5
        }
    }

    /**
     * Get the heart rate intensity
     * @return Heart rate intensity as a string
     */
    fun getIntensity(): String {
        return when {
            heartRate < 60 -> "Low"
            heartRate < 100 -> "Normal"
            heartRate < 140 -> "Elevated"
            heartRate < 170 -> "High"
            else -> "Very High"
        }
    }

    /**
     * Check if the heart rate is in a healthy range
     * @param age User's age
     * @return true if the heart rate is in a healthy range, false otherwise
     */
    fun isInHealthyRange(age: Int): Boolean {
        val maxHeartRate = 220 - age
        return when (status) {
            HeartRateStatus.RESTING -> heartRate in 40..100
            HeartRateStatus.EXERCISE -> heartRate < maxHeartRate * 0.9
            else -> heartRate in 40..100
        }
    }

    /**
     * Validate the heart rate measurement
     * @return true if the heart rate measurement is valid, false otherwise
     */
    fun isValid(): Boolean {
        return heartRate in 30..250
    }

    companion object {
        /**
         * Create a heart rate measurement from a timestamp and value
         * @param deviceId Device ID
         * @param timestamp Timestamp
         * @param heartRate Heart rate value
         * @return Heart rate measurement with default values
         */
        fun create(deviceId: Long, timestamp: LocalDateTime, heartRate: Int): HeartRate {
            return HeartRate(
                id = 0,
                deviceId = deviceId,
                timestamp = timestamp,
                heartRate = heartRate,
                status = HeartRateStatus.NORMAL
            )
        }
    }
}

/**
 * Heart rate zones model
 */
data class HeartRateZones(
    val zone1Range: Pair<Int, Int>, // 50-60% of max HR - Very Light
    val zone2Range: Pair<Int, Int>, // 60-70% of max HR - Light
    val zone3Range: Pair<Int, Int>, // 70-80% of max HR - Moderate
    val zone4Range: Pair<Int, Int>, // 80-90% of max HR - Hard
    val zone5Range: Pair<Int, Int>  // 90-100% of max HR - Maximum
) {
    /**
     * Get the zone for a heart rate value
     * @param heartRate Heart rate value
     * @return Zone number (1-5)
     */
    fun getZoneForHeartRate(heartRate: Int): Int {
        return when {
            heartRate <= zone1Range.second -> 1
            heartRate <= zone2Range.second -> 2
            heartRate <= zone3Range.second -> 3
            heartRate <= zone4Range.second -> 4
            else -> 5
        }
    }

    /**
     * Get the zone name for a heart rate value
     * @param heartRate Heart rate value
     * @return Zone name
     */
    fun getZoneName(heartRate: Int): String {
        return when (getZoneForHeartRate(heartRate)) {
            1 -> "Very Light"
            2 -> "Light"
            3 -> "Moderate"
            4 -> "Hard"
            else -> "Maximum"
        }
    }

    /**
     * Get the zone range for a zone number
     * @param zone Zone number (1-5)
     * @return Zone range as a pair of integers
     */
    fun getZoneRange(zone: Int): Pair<Int, Int> {
        return when (zone) {
            1 -> zone1Range
            2 -> zone2Range
            3 -> zone3Range
            4 -> zone4Range
            else -> zone5Range
        }
    }
}

/**
 * Heart rate statistics model
 */
data class HeartRateStatistics(
    val date: LocalDate,
    val minHeartRate: Int,
    val maxHeartRate: Int,
    val avgHeartRate: Double,
    val restingHeartRate: Int?,
    val measurementCount: Int,
    val timeInZone1Minutes: Int = 0,
    val timeInZone2Minutes: Int = 0,
    val timeInZone3Minutes: Int = 0,
    val timeInZone4Minutes: Int = 0,
    val timeInZone5Minutes: Int = 0
) {
    /**
     * Get the total time in all zones
     * @return Total time in all zones in minutes
     */
    fun getTotalTimeInZones(): Int {
        return timeInZone1Minutes + timeInZone2Minutes + timeInZone3Minutes + 
               timeInZone4Minutes + timeInZone5Minutes
    }

    /**
     * Get the heart rate range
     * @return Heart rate range as a string
     */
    fun getHeartRateRange(): String {
        return "$minHeartRate - $maxHeartRate bpm"
    }

    /**
     * Get the heart rate variability (max - min)
     * @return Heart rate variability
     */
    fun getHeartRateVariability(): Int {
        return maxHeartRate - minHeartRate
    }

    /**
     * Get the resting heart rate status
     * @return Resting heart rate status as a string
     */
    fun getRestingHeartRateStatus(): String? {
        return restingHeartRate?.let {
            when {
                it < 60 -> "Excellent"
                it < 70 -> "Good"
                it < 80 -> "Average"
                it < 90 -> "Fair"
                else -> "Poor"
            }
        }
    }
}

/**
 * Blood oxygen model representing a blood oxygen measurement
 */
data class BloodOxygen(
    override val id: Long,
    val deviceId: Long,
    val timestamp: LocalDateTime,
    val bloodOxygen: Int, // SpO2 percentage (0-100)
    val isManualEntry: Boolean = false,
    val notes: String? = null,
    val syncStatus: SyncStatus = SyncStatus.PENDING
) : DomainModel {

    /**
     * Get the blood oxygen status
     * @return Blood oxygen status as a string
     */
    fun getStatus(): String {
        return when {
            bloodOxygen >= 95 -> "Normal"
            bloodOxygen >= 90 -> "Slightly Low"
            bloodOxygen >= 80 -> "Low"
            else -> "Very Low"
        }
    }

    /**
     * Check if the blood oxygen level is in a healthy range
     * @return true if the blood oxygen level is in a healthy range, false otherwise
     */
    fun isInHealthyRange(): Boolean {
        return bloodOxygen >= 95
    }

    /**
     * Validate the blood oxygen measurement
     * @return true if the blood oxygen measurement is valid, false otherwise
     */
    fun isValid(): Boolean {
        return bloodOxygen in 70..100
    }

    companion object {
        /**
         * Create a blood oxygen measurement from a timestamp and value
         * @param deviceId Device ID
         * @param timestamp Timestamp
         * @param bloodOxygen Blood oxygen value
         * @return Blood oxygen measurement with default values
         */
        fun create(deviceId: Long, timestamp: LocalDateTime, bloodOxygen: Int): BloodOxygen {
            return BloodOxygen(
                id = 0,
                deviceId = deviceId,
                timestamp = timestamp,
                bloodOxygen = bloodOxygen
            )
        }
    }
}

/**
 * Blood oxygen statistics model
 */
data class BloodOxygenStatistics(
    val date: LocalDate,
    val minBloodOxygen: Int,
    val maxBloodOxygen: Int,
    val avgBloodOxygen: Double,
    val measurementCount: Int,
    val lowOxygenEventCount: Int = 0
) {
    /**
     * Get the blood oxygen range
     * @return Blood oxygen range as a string
     */
    fun getBloodOxygenRange(): String {
        return "$minBloodOxygen - $maxBloodOxygen%"
    }

    /**
     * Get the blood oxygen status
     * @return Blood oxygen status as a string
     */
    fun getStatus(): String {
        return when {
            avgBloodOxygen >= 95 -> "Normal"
            avgBloodOxygen >= 90 -> "Slightly Low"
            avgBloodOxygen >= 80 -> "Low"
            else -> "Very Low"
        }
    }

    /**
     * Check if there are any low oxygen events
     * @return true if there are low oxygen events, false otherwise
     */
    fun hasLowOxygenEvents(): Boolean {
        return lowOxygenEventCount > 0
    }
}

/**
 * Step data model representing daily step count and activity
 */
data class StepData(
    override val id: Long,
    val deviceId: Long,
    val date: LocalDate,
    val steps: Int,
    val distance: Float, // in meters
    val calories: Float, // in kcal
    val activeMinutes: Int = 0,
    val goalSteps: Int = 10000,
    val isManualEntry: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.PENDING
) : DomainModel {

    /**
     * Check if the goal is achieved
     * @return true if the goal is achieved, false otherwise
     */
    fun isGoalAchieved(): Boolean {
        return steps >= goalSteps
    }

    /**
     * Get the goal progress percentage
     * @return Goal progress percentage
     */
    fun getGoalProgressPercentage(): Int {
        return min((steps.toFloat() / goalSteps.toFloat() * 100).toInt(), 100)
    }

    /**
     * Get the distance in kilometers
     * @return Distance in kilometers
     */
    fun getDistanceKm(): Float {
        return distance / 1000f
    }

    /**
     * Get the distance in miles
     * @return Distance in miles
     */
    fun getDistanceMiles(): Float {
        return distance / 1609.34f
    }

    /**
     * Get the pace (minutes per kilometer)
     * @return Pace in minutes per kilometer, or null if active minutes is 0
     */
    fun getPaceMinPerKm(): Float? {
        return if (activeMinutes > 0 && distance > 0) {
            activeMinutes / getDistanceKm()
        } else {
            null
        }
    }

    /**
     * Get the pace (minutes per mile)
     * @return Pace in minutes per mile, or null if active minutes is 0
     */
    fun getPaceMinPerMile(): Float? {
        return if (activeMinutes > 0 && distance > 0) {
            activeMinutes / getDistanceMiles()
        } else {
            null
        }
    }

    /**
     * Get the activity level
     * @return Activity level as a string
     */
    fun getActivityLevel(): String {
        return when {
            steps < 5000 -> "Sedentary"
            steps < 7500 -> "Lightly Active"
            steps < 10000 -> "Moderately Active"
            steps < 12500 -> "Active"
            else -> "Very Active"
        }
    }

    companion object {
        /**
         * Create a step data record from a date and steps
         * @param deviceId Device ID
         * @param date Date
         * @param steps Steps count
         * @param distance Distance in meters
         * @param calories Calories burned
         * @return Step data record with default values
         */
        fun create(
            deviceId: Long,
            date: LocalDate,
            steps: Int,
            distance: Float,
            calories: Float
        ): StepData {
            return StepData(
                id = 0,
                deviceId = deviceId,
                date = date,
                steps = steps,
                distance = distance,
                calories = calories
            )
        }
    }
}

/**
 * Hourly step data model representing hourly step count
 */
data class HourlyStepData(
    override val id: Long,
    val deviceId: Long,
    val date: LocalDate,
    val hour: Int, // 0-23
    val steps: Int,
    val distance: Float, // in meters
    val calories: Float, // in kcal
    val syncStatus: SyncStatus = SyncStatus.PENDING
) : DomainModel {

    /**
     * Get the time range as a string
     * @return Time range as a string (e.g., "08:00 - 09:00")
     */
    fun getTimeRange(): String {
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        val startTime = LocalDateTime.of(date.year, date.month, date.dayOfMonth, hour, 0)
        val endTime = startTime.plusHours(1)
        return "${startTime.format(formatter)} - ${endTime.format(formatter)}"
    }

    /**
     * Get the activity level
     * @return Activity level as a string
     */
    fun getActivityLevel(): String {
        return when {
            steps < 250 -> "Inactive"
            steps < 500 -> "Lightly Active"
            steps < 1000 -> "Moderately Active"
            steps < 1500 -> "Active"
            else -> "Very Active"
        }
    }

    companion object {
        /**
         * Create an hourly step data record from a date, hour, and steps
         * @param deviceId Device ID
         * @param date Date
         * @param hour Hour (0-23)
         * @param steps Steps count
         * @param distance Distance in meters
         * @param calories Calories burned
         * @return Hourly step data record with default values
         */
        fun create(
            deviceId: Long,
            date: LocalDate,
            hour: Int,
            steps: Int,
            distance: Float,
            calories: Float
        ): HourlyStepData {
            return HourlyStepData(
                id = 0,
                deviceId = deviceId,
                date = date,
                hour = hour,
                steps = steps,
                distance = distance,
                calories = calories
            )
        }
    }
}

/**
 * Step statistics model
 */
data class StepStatistics(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val totalSteps: Int,
    val totalDistance: Float, // in meters
    val totalCalories: Float, // in kcal
    val totalActiveMinutes: Int,
    val avgSteps: Double,
    val goalAchievedDays: Int,
    val currentStreak: Int,
    val bestStreak: Int,
    val dailySteps: List<Pair<LocalDate, Int>> = emptyList()
) {
    /**
     * Get the total distance in kilometers
     * @return Total distance in kilometers
     */
    fun getTotalDistanceKm(): Float {
        return totalDistance / 1000f
    }

    /**
     * Get the total distance in miles
     * @return Total distance in miles
     */
    fun getTotalDistanceMiles(): Float {
        return totalDistance / 1609.34f
    }

    /**
     * Get the average distance per day in kilometers
     * @return Average distance per day in kilometers
     */
    fun getAvgDistanceKmPerDay(): Float {
        val days = ChronoUnit.DAYS.between(startDate, endDate.plusDays(1)).toInt()
        return if (days > 0) getTotalDistanceKm() / days else 0f
    }

    /**
     * Get the average active minutes per day
     * @return Average active minutes per day
     */
    fun getAvgActiveMinutesPerDay(): Int {
        val days = ChronoUnit.DAYS.between(startDate, endDate.plusDays(1)).toInt()
        return if (days > 0) totalActiveMinutes / days else 0
    }

    /**
     * Get the goal achievement percentage
     * @return Goal achievement percentage
     */
    fun getGoalAchievementPercentage(): Int {
        val days = ChronoUnit.DAYS.between(startDate, endDate.plusDays(1)).toInt()
        return if (days > 0) (goalAchievedDays * 100) / days else 0
    }

    /**
     * Get the activity trend
     * @return Activity trend as a string
     */
    fun getActivityTrend(): String {
        if (dailySteps.size < 3) {
            return "Insufficient Data"
        }

        val firstHalf = dailySteps.subList(0, dailySteps.size / 2)
        val secondHalf = dailySteps.subList(dailySteps.size / 2, dailySteps.size)

        val firstHalfAvg = firstHalf.sumOf { it.second } / firstHalf.size
        val secondHalfAvg = secondHalf.sumOf { it.second } / secondHalf.size

        val percentChange = ((secondHalfAvg - firstHalfAvg) * 100) / firstHalfAvg.toDouble()

        return when {
            percentChange > 10 -> "Increasing"
            percentChange < -10 -> "Decreasing"
            else -> "Stable"
        }
    }
}

/**
 * Sleep type enum
 */
enum class SleepType {
    DEEP_SLEEP,
    LIGHT_SLEEP,
    REM_SLEEP,
    AWAKE,
    UNKNOWN;

    companion object {
        fun fromString(value: String?): SleepType {
            return when (value?.uppercase()) {
                "DEEP_SLEEP" -> DEEP_SLEEP
                "LIGHT_SLEEP" -> LIGHT_SLEEP
                "REM_SLEEP" -> REM_SLEEP
                "AWAKE" -> AWAKE
                else -> UNKNOWN
            }
        }
    }
}

/**
 * Sleep detail model representing a sleep stage
 */
data class SleepDetail(
    override val id: Long,
    val sleepId: Long,
    val timestamp: LocalDateTime,
    val sleepType: SleepType,
    val durationMinutes: Int = 5 // Default 5-minute intervals
) : DomainModel {

    /**
     * Get the sleep type name
     * @return Sleep type name as a string
     */
    fun getSleepTypeName(): String {
        return when (sleepType) {
            SleepType.DEEP_SLEEP -> "Deep Sleep"
            SleepType.LIGHT_SLEEP -> "Light Sleep"
            SleepType.REM_SLEEP -> "REM Sleep"
            SleepType.AWAKE -> "Awake"
            SleepType.UNKNOWN -> "Unknown"
        }
    }

    companion object {
        /**
         * Create a sleep detail record from a timestamp and sleep type
         * @param sleepId Sleep record ID
         * @param timestamp Timestamp
         * @param sleepType Sleep type
         * @param durationMinutes Duration in minutes
         * @return Sleep detail record with default values
         */
        fun create(
            sleepId: Long,
            timestamp: LocalDateTime,
            sleepType: SleepType,
            durationMinutes: Int = 5
        ): SleepDetail {
            return SleepDetail(
                id = 0,
                sleepId = sleepId,
                timestamp = timestamp,
                sleepType = sleepType,
                durationMinutes = durationMinutes
            )
        }
    }
}

/**
 * Sleep model representing a sleep record
 */
data class Sleep(
    override val id: Long,
    val deviceId: Long,
    val date: LocalDate,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val deepSleepMinutes: Int,
    val lightSleepMinutes: Int,
    val remSleepMinutes: Int = 0,
    val awakeSleepMinutes: Int,
    val totalSleepMinutes: Int,
    val sleepQuality: Int, // 0-100
    val sleepDetails: List<SleepDetail> = emptyList(),
    val isManualEntry: Boolean = false,
    val notes: String? = null,
    val syncStatus: SyncStatus = SyncStatus.PENDING
) : DomainModel {

    /**
     * Get the sleep duration as a string
     * @return Sleep duration as a string (e.g., "8h 30m")
     */
    fun getSleepDurationString(): String {
        val hours = totalSleepMinutes / 60
        val minutes = totalSleepMinutes % 60
        return "${hours}h ${minutes}m"
    }

    /**
     * Get the sleep efficiency (deep sleep percentage)
     * @return Sleep efficiency as a percentage
     */
    fun getSleepEfficiency(): Int {
        return if (totalSleepMinutes > 0) {
            (deepSleepMinutes * 100) / totalSleepMinutes
        } else {
            0
        }
    }

    /**
     * Get the sleep quality category
     * @return Sleep quality category as a string
     */
    fun getSleepQualityCategory(): String {
        return when {
            sleepQuality >= 80 -> "Excellent"
            sleepQuality >= 60 -> "Good"
            sleepQuality >= 40 -> "Fair"
            sleepQuality >= 20 -> "Poor"
            else -> "Very Poor"
        }
    }

    /**
     * Get the sleep stage percentages
     * @return Map of sleep stage percentages
     */
    fun getSleepStagePercentages(): Map<SleepType, Int> {
        return if (totalSleepMinutes > 0) {
            mapOf(
                SleepType.DEEP_SLEEP to (deepSleepMinutes * 100) / totalSleepMinutes,
                SleepType.LIGHT_SLEEP to (lightSleepMinutes * 100) / totalSleepMinutes,
                SleepType.REM_SLEEP to (remSleepMinutes * 100) / totalSleepMinutes,
                SleepType.AWAKE to (awakeSleepMinutes * 100) / totalSleepMinutes
            )
        } else {
            mapOf(
                SleepType.DEEP_SLEEP to 0,
                SleepType.LIGHT_SLEEP to 0,
                SleepType.REM_SLEEP to 0,
                SleepType.AWAKE to 0
            )
        }
    }

    /**
     * Check if the sleep duration is in a healthy range
     * @return true if the sleep duration is in a healthy range, false otherwise
     */
    fun isInHealthyDurationRange(): Boolean {
        return totalSleepMinutes in 420..540 // 7-9 hours
    }

    /**
     * Check if the sleep efficiency is in a healthy range
     * @return true if the sleep efficiency is in a healthy range, false otherwise
     */
    fun isInHealthyEfficiencyRange(): Boolean {
        return getSleepEfficiency() >= 20 // At least 20% deep sleep
    }

    /**
     * Get the sleep debt (difference between actual and target sleep)
     * @param targetSleepMinutes Target sleep duration in minutes (default: 480 minutes = 8 hours)
     * @return Sleep debt in minutes
     */
    fun getSleepDebt(targetSleepMinutes: Int = 480): Int {
        return targetSleepMinutes - totalSleepMinutes
    }

    companion object {
        /**
         * Create a sleep record from start and end times
         * @param deviceId Device ID
         * @param date Date
         * @param startTime Start time
         * @param endTime End time
         * @return Sleep record with default values
         */
        fun create(
            deviceId: Long,
            date: LocalDate,
            startTime: LocalDateTime,
            endTime: LocalDateTime
        ): Sleep {
            val totalMinutes = ChronoUnit.MINUTES.between(startTime, endTime).toInt()
            return Sleep(
                id = 0,
                deviceId = deviceId,
                date = date,
                startTime = startTime,
                endTime = endTime,
                deepSleepMinutes = 0,
                lightSleepMinutes = 0,
                remSleepMinutes = 0,
                awakeSleepMinutes = 0,
                totalSleepMinutes = totalMinutes,
                sleepQuality = 0
            )
        }
    }
}

/**
 * Sleep statistics model
 */
data class SleepStatistics(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val avgTotalSleepMinutes: Double,
    val avgDeepSleepMinutes: Double,
    val avgLightSleepMinutes: Double,
    val avgRemSleepMinutes: Double,
    val avgAwakeSleepMinutes: Double,
    val avgSleepQuality: Double,
    val recordCount: Int,
    val dailySleep: List<Pair<LocalDate, Int>> = emptyList() // Date, total sleep minutes
) {
    /**
     * Get the average sleep duration as a string
     * @return Average sleep duration as a string (e.g., "8h 30m")
     */
    fun getAvgSleepDurationString(): String {
        val hours = avgTotalSleepMinutes.toInt() / 60
        val minutes = avgTotalSleepMinutes.toInt() % 60
        return "${hours}h ${minutes}m"
    }

    /**
     * Get the average sleep efficiency (deep sleep percentage)
     * @return Average sleep efficiency as a percentage
     */
    fun getAvgSleepEfficiency(): Int {
        return if (avgTotalSleepMinutes > 0) {
            (avgDeepSleepMinutes * 100 / avgTotalSleepMinutes).toInt()
        } else {
            0
        }
    }

    /**
     * Get the average sleep quality category
     * @return Average sleep quality category as a string
     */
    fun getAvgSleepQualityCategory(): String {
        return when {
            avgSleepQuality >= 80 -> "Excellent"
            avgSleepQuality >= 60 -> "Good"
            avgSleepQuality >= 40 -> "Fair"
            avgSleepQuality >= 20 -> "Poor"
            else -> "Very Poor"
        }
    }

    /**
     * Get the sleep trend
     * @return Sleep trend as a string
     */
    fun getSleepTrend(): String {
        if (dailySleep.size < 3) {
            return "Insufficient Data"
        }

        val firstHalf = dailySleep.subList(0, dailySleep.size / 2)
        val secondHalf = dailySleep.subList(dailySleep.size / 2, dailySleep.size)

        val firstHalfAvg = firstHalf.sumOf { it.second } / firstHalf.size
        val secondHalfAvg = secondHalf.sumOf { it.second } / secondHalf.size

        val percentChange = ((secondHalfAvg - firstHalfAvg) * 100) / firstHalfAvg.toDouble()

        return when {
            percentChange > 10 -> "Improving"
            percentChange < -10 -> "Declining"
            else -> "Stable"
        }
    }

    /**
     * Get the average sleep stage percentages
     * @return Map of average sleep stage percentages
     */
    fun getAvgSleepStagePercentages(): Map<SleepType, Int> {
        return if (avgTotalSleepMinutes > 0) {
            mapOf(
                SleepType.DEEP_SLEEP to (avgDeepSleepMinutes * 100 / avgTotalSleepMinutes).toInt(),
                SleepType.LIGHT_SLEEP to (avgLightSleepMinutes * 100 / avgTotalSleepMinutes).toInt(),
                SleepType.REM_SLEEP to (avgRemSleepMinutes * 100 / avgTotalSleepMinutes).toInt(),
                SleepType.AWAKE to (avgAwakeSleepMinutes * 100 / avgTotalSleepMinutes).toInt()
            )
        } else {
            mapOf(
                SleepType.DEEP_SLEEP to 0,
                SleepType.LIGHT_SLEEP to 0,
                SleepType.REM_SLEEP to 0,
                SleepType.AWAKE to 0
            )
        }
    }
}

/**
 * Blood pressure model representing a blood pressure measurement
 */
data class BloodPressure(
    override val id: Long,
    val deviceId: Long,
    val timestamp: LocalDateTime,
    val systolic: Int, // mmHg
    val diastolic: Int, // mmHg
    val heartRate: Int? = null,
    val isManualEntry: Boolean = false,
    val notes: String? = null,
    val syncStatus: SyncStatus = SyncStatus.PENDING
) : DomainModel {

    /**
     * Get the blood pressure category
     * @return Blood pressure category as a string
     */
    fun getCategory(): String {
        return when {
            systolic < 120 && diastolic < 80 -> "Normal"
            systolic < 130 && diastolic < 80 -> "Elevated"
            systolic < 140 || diastolic < 90 -> "Hypertension Stage 1"
            systolic < 180 || diastolic < 120 -> "Hypertension Stage 2"
            else -> "Hypertensive Crisis"
        }
    }

    /**
     * Get the blood pressure as a string
     * @return Blood pressure as a string (e.g., "120/80")
     */
    fun getBloodPressureString(): String {
        return "$systolic/$diastolic"
    }

    /**
     * Check if the blood pressure is in a healthy range
     * @return true if the blood pressure is in a healthy range, false otherwise
     */
    fun isInHealthyRange(): Boolean {
        return systolic < 130 && diastolic < 80
    }

    /**
     * Validate the blood pressure measurement
     * @return true if the blood pressure measurement is valid, false otherwise
     */
    fun isValid(): Boolean {
        return systolic in 70..250 && diastolic in 40..150 && systolic > diastolic
    }

    companion object {
        /**
         * Create a blood pressure measurement from a timestamp and values
         * @param deviceId Device ID
         * @param timestamp Timestamp
         * @param systolic Systolic pressure
         * @param diastolic Diastolic pressure
         * @param heartRate Heart rate
         * @return Blood pressure measurement with default values
         */
        fun create(
            deviceId: Long,
            timestamp: LocalDateTime,
            systolic: Int,
            diastolic: Int,
            heartRate: Int? = null
        ): BloodPressure {
            return BloodPressure(
                id = 0,
                deviceId = deviceId,
                timestamp = timestamp,
                systolic = systolic,
                diastolic = diastolic,
                heartRate = heartRate
            )
        }
    }
}

/**
 * Blood pressure statistics model
 */
data class BloodPressureStatistics(
    val date: LocalDate,
    val avgSystolic: Double,
    val avgDiastolic: Double,
    val minSystolic: Int,
    val maxSystolic: Int,
    val minDiastolic: Int,
    val maxDiastolic: Int,
    val measurementCount: Int,
    val highBloodPressureEventCount: Int = 0
) {
    /**
     * Get the average blood pressure category
     * @return Average blood pressure category as a string
     */
    fun getAvgCategory(): String {
        return when {
            avgSystolic < 120 && avgDiastolic < 80 -> "Normal"
            avgSystolic < 130 && avgDiastolic < 80 -> "Elevated"
            avgSystolic < 140 || avgDiastolic < 90 -> "Hypertension Stage 1"
            avgSystolic < 180 || avgDiastolic < 120 -> "Hypertension Stage 2"
            else -> "Hypertensive Crisis"
        }
    }

    /**
     * Get the average blood pressure as a string
     * @return Average blood pressure as a string (e.g., "120/80")
     */
    fun getAvgBloodPressureString(): String {
        return "${avgSystolic.roundToInt()}/${avgDiastolic.roundToInt()}"
    }

    /**
     * Get the blood pressure range as a string
     * @return Blood pressure range as a string (e.g., "110/70 - 130/90")
     */
    fun getBloodPressureRangeString(): String {
        return "$minSystolic/$minDiastolic - $maxSystolic/$maxDiastolic"
    }

    /**
     * Check if there are any high blood pressure events
     * @return true if there are high blood pressure events, false otherwise
     */
    fun hasHighBloodPressureEvents(): Boolean {
        return highBloodPressureEventCount > 0
    }
}

/**
 * Temperature model representing a temperature measurement
 */
data class Temperature(
    override val id: Long,
    val deviceId: Long,
    val timestamp: LocalDateTime,
    val temperatureCelsius: Float,
    val bodyLocation: String = "wrist",
    val isManualEntry: Boolean = false,
    val notes: String? = null,
    val syncStatus: SyncStatus = SyncStatus.PENDING
) : DomainModel {

    /**
     * Get the temperature in Fahrenheit
     * @return Temperature in Fahrenheit
     */
    fun getTemperatureFahrenheit(): Float {
        return temperatureCelsius * 9f / 5f + 32f
    }

    /**
     * Get the temperature status
     * @return Temperature status as a string
     */
    fun getStatus(): String {
        return when {
            temperatureCelsius < 35.0f -> "Hypothermia"
            temperatureCelsius < 36.0f -> "Low"
            temperatureCelsius < 37.5f -> "Normal"
            temperatureCelsius < 38.0f -> "Elevated"
            temperatureCelsius < 39.0f -> "Fever"
            temperatureCelsius < 40.0f -> "High Fever"
            else -> "Very High Fever"
        }
    }

    /**
     * Check if the temperature is in a healthy range
     * @return true if the temperature is in a healthy range, false otherwise
     */
    fun isInHealthyRange(): Boolean {
        return temperatureCelsius in 36.0f..37.5f
    }

    /**
     * Validate the temperature measurement
     * @return true if the temperature measurement is valid, false otherwise
     */
    fun isValid(): Boolean {
        return temperatureCelsius in 30.0f..45.0f
    }

    companion object {
        /**
         * Create a temperature measurement from a timestamp and value
         * @param deviceId Device ID
         * @param timestamp Timestamp
         * @param temperatureCelsius Temperature in Celsius
         * @param bodyLocation Body location
         * @return Temperature measurement with default values
         */
        fun create(
            deviceId: Long,
            timestamp: LocalDateTime,
            temperatureCelsius: Float,
            bodyLocation: String = "wrist"
        ): Temperature {
            return Temperature(
                id = 0,
                deviceId = deviceId,
                timestamp = timestamp,
                temperatureCelsius = temperatureCelsius,
                bodyLocation = bodyLocation
            )
        }
    }
}

/**
 * Temperature statistics model
 */
data class TemperatureStatistics(
    val date: LocalDate,
    val minTemperature: Float,
    val maxTemperature: Float,
    val avgTemperature: Double,
    val measurementCount: Int,
    val feverEventCount: Int = 0
) {
    /**
     * Get the temperature range as a string
     * @return Temperature range as a string (e.g., "36.5°C - 37.5°C")
     */
    fun getTemperatureRangeString(): String {
        return "${minTemperature}°C - ${maxTemperature}°C"
    }

    /**
     * Get the temperature range in Fahrenheit as a string
     * @return Temperature range in Fahrenheit as a string (e.g., "97.7°F - 99.5°F")
     */
    fun getTemperatureRangeFahrenheitString(): String {
        val minF = minTemperature * 9f / 5f + 32f
        val maxF = maxTemperature * 9f / 5f + 32f
        return "${minF}°F - ${maxF}°F"
    }

    /**
     * Get the average temperature status
     * @return Average temperature status as a string
     */
    fun getAvgStatus(): String {
        return when {
            avgTemperature < 35.0 -> "Hypothermia"
            avgTemperature < 36.0 -> "Low"
            avgTemperature < 37.5 -> "Normal"
            avgTemperature < 38.0 -> "Elevated"
            avgTemperature < 39.0 -> "Fever"
            avgTemperature < 40.0 -> "High Fever"
            else -> "Very High Fever"
        }
    }

    /**
     * Check if there are any fever events
     * @return true if there are fever events, false otherwise
     */
    fun hasFeverEvents(): Boolean {
        return feverEventCount > 0
    }
}

/**
 * Activity type enum
 */
enum class ActivityType {
    WALKING,
    RUNNING,
    CYCLING,
    SWIMMING,
    ELLIPTICAL,
    ROWING,
    OTHER;

    companion object {
        fun fromString(value: String?): ActivityType {
            return try {
                valueOf(value?.uppercase() ?: "OTHER")
            } catch (e: IllegalArgumentException) {
                OTHER
            }
        }
    }
}

/**
 * Activity model representing a workout/exercise session
 */
data class Activity(
    override val id: Long,
    val deviceId: Long,
    val activityType: ActivityType,
    val date: LocalDate,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val durationSeconds: Int,
    val steps: Int? = null,
    val distance: Float? = null, // in meters
    val calories: Float? = null, // in kcal
    val avgHeartRate: Int? = null,
    val maxHeartRate: Int? = null,
    val avgPace: Float? = null, // in min/km
    val isManualEntry: Boolean = false,
    val notes: String? = null,
    val syncStatus: SyncStatus = SyncStatus.PENDING
) : DomainModel {

    /**
     * Get the activity duration as a string
     * @return Activity duration as a string (e.g., "1h 30m")
     */
    fun getDurationString(): String {
        val hours = durationSeconds / 3600
        val minutes = (durationSeconds % 3600) / 60
        val seconds = durationSeconds % 60
        return if (hours > 0) {
            "${hours}h ${minutes}m ${seconds}s"
        } else {
            "${minutes}m ${seconds}s"
        }
    }

    /**
     * Get the distance in kilometers
     * @return Distance in kilometers, or null if distance is not available
     */
    fun getDistanceKm(): Float? {
        return distance?.let { it / 1000f }
    }

    /**
     * Get the distance in miles
     * @return Distance in miles, or null if distance is not available
     */
    fun getDistanceMiles(): Float? {
        return distance?.let { it / 1609.34f }
    }

    /**
     * Get the pace as a string
     * @return Pace as a string (e.g., "5:30 min/km"), or null if pace is not available
     */
    fun getPaceString(): String? {
        return avgPace?.let {
            val minutes = it.toInt()
            val seconds = ((it - minutes) * 60).toInt()
            "${minutes}:${seconds.toString().padStart(2, '0')} min/km"
        }
    }

    /**
     * Get the activity intensity
     * @return Activity intensity as a string
     */
    fun getIntensity(): String {
        return when {
            avgHeartRate == null -> "Unknown"
            avgHeartRate < 100 -> "Low"
            avgHeartRate < 120 -> "Moderate"
            avgHeartRate < 140 -> "High"
            else -> "Very High"
        }
    }

    /**
     * Calculate the calories burned if not provided
     * @param weight Weight in kg
     * @return Calories burned, or null if required data is not available
     */
    fun calculateCalories(weight: Float): Float? {
        if (durationSeconds <= 0 || weight <= 0) {
            return null
        }

        val durationHours = durationSeconds / 3600f
        
        // MET values (Metabolic Equivalent of Task)
        val met = when (activityType) {
            ActivityType.WALKING -> 3.5f
            ActivityType.RUNNING -> 8.0f
            ActivityType.CYCLING -> 7.0f
            ActivityType.SWIMMING -> 6.0f
            ActivityType.ELLIPTICAL -> 5.0f
            ActivityType.ROWING -> 6.0f
            ActivityType.OTHER -> 4.0f
        }
        
        // Calories = MET * weight (kg) * duration (hours)
        return met * weight * durationHours
    }

    companion object {
        /**
         * Create an activity record from start and end times
         * @param deviceId Device ID
         * @param activityType Activity type
         * @param date Date
         * @param startTime Start time
         * @param endTime End time
         * @return Activity record with default values
         */
        fun create(
            deviceId: Long,
            activityType: ActivityType,
            date: LocalDate,
            startTime: LocalDateTime,
            endTime: LocalDateTime
        ): Activity {
            val durationSeconds = ChronoUnit.SECONDS.between(startTime, endTime).toInt()
            return Activity(
                id = 0,
                deviceId = deviceId,
                activityType = activityType,
                date = date,
                startTime = startTime,
                endTime = endTime,
                durationSeconds = durationSeconds
            )
        }
    }
}

/**
 * Activity statistics model
 */
data class ActivityStatistics(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val totalActivities: Int,
    val totalDurationSeconds: Int,
    val totalSteps: Int?,
    val totalDistance: Float?, // in meters
    val totalCalories: Float?, // in kcal
    val avgHeartRate: Double?,
    val maxHeartRate: Int?,
    val activityTypeBreakdown: Map<ActivityType, Int> = emptyMap()
) {
    /**
     * Get the total duration as a string
     * @return Total duration as a string (e.g., "10h 30m")
     */
    fun getTotalDurationString(): String {
        val hours = totalDurationSeconds / 3600
        val minutes = (totalDurationSeconds % 3600) / 60
        return "${hours}h ${minutes}m"
    }

    /**
     * Get the average duration per activity as a string
     * @return Average duration per activity as a string (e.g., "45m")
     */
    fun getAvgDurationPerActivityString(): String {
        if (totalActivities <= 0) {
            return "0m"
        }
        
        val avgSeconds = totalDurationSeconds / totalActivities
        val hours = avgSeconds / 3600
        val minutes = (avgSeconds % 3600) / 60
        
        return if (hours > 0) {
            "${hours}h ${minutes}m"
        } else {
            "${minutes}m"
        }
    }

    /**
     * Get the total distance in kilometers
     * @return Total distance in kilometers, or null if distance is not available
     */
    fun getTotalDistanceKm(): Float? {
        return totalDistance?.let { it / 1000f }
    }

    /**
     * Get the most frequent activity type
     * @return Most frequent activity type, or null if no activities
     */
    fun getMostFrequentActivityType(): ActivityType? {
        return activityTypeBreakdown.entries.maxByOrNull { it.value }?.key
    }

    /**
     * Get the activity type breakdown percentages
     * @return Map of activity type percentages
     */
    fun getActivityTypePercentages(): Map<ActivityType, Int> {
        return if (totalActivities > 0) {
            activityTypeBreakdown.mapValues { (it.value * 100) / totalActivities }
        } else {
            emptyMap()
        }
    }

    /**
     * Get the average activities per week
     * @return Average activities per week
     */
    fun getAvgActivitiesPerWeek(): Double {
        val days = ChronoUnit.DAYS.between(startDate, endDate.plusDays(1)).toInt()
        return if (days > 0) {
            (totalActivities * 7.0) / days
        } else {
            0.0
        }
    }
}

/**
 * Health insight type enum
 */
enum class HealthInsightType {
    SLEEP_QUALITY,
    RESTING_HEART_RATE,
    ACTIVITY_LEVEL,
    BLOOD_PRESSURE,
    BLOOD_OXYGEN,
    TEMPERATURE,
    STRESS_LEVEL,
    OVERALL_HEALTH;

    companion object {
        fun fromString(value: String?): HealthInsightType? {
            return try {
                valueOf(value?.uppercase() ?: "")
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }
}

/**
 * Health insight trend enum
 */
enum class HealthInsightTrend {
    IMPROVING,
    STABLE,
    DECLINING,
    INSUFFICIENT_DATA;

    companion object {
        fun fromString(value: String?): HealthInsightTrend {
            return when (value?.uppercase()) {
                "IMPROVING" -> IMPROVING
                "STABLE" -> STABLE
                "DECLINING" -> DECLINING
                else -> INSUFFICIENT_DATA
            }
        }
    }
}

/**
 * Health insight severity enum
 */
enum class HealthInsightSeverity {
    INFO,
    WARNING,
    ALERT;

    companion object {
        fun fromString(value: String?): HealthInsightSeverity {
            return when (value?.uppercase()) {
                "WARNING" -> WARNING
                "ALERT" -> ALERT
                else -> INFO
            }
        }
    }
}

/**
 * Health insight model representing a health insight or trend
 */
data class HealthInsight(
    override val id: Long,
    val userId: Long,
    val date: LocalDate,
    val insightType: HealthInsightType,
    val score: Int? = null, // 0-100 score if applicable
    val value: Float? = null, // Numerical value if applicable
    val trend: HealthInsightTrend = HealthInsightTrend.INSUFFICIENT_DATA,
    val message: String? = null, // User-friendly message
    val severity: HealthInsightSeverity = HealthInsightSeverity.INFO
) : DomainModel {

    /**
     * Get the insight title
     * @return Insight title as a string
     */
    fun getTitle(): String {
        return when (insightType) {
            HealthInsightType.SLEEP_QUALITY -> "Sleep Quality"
            HealthInsightType.RESTING_HEART_RATE -> "Resting Heart Rate"
            HealthInsightType.ACTIVITY_LEVEL -> "Activity Level"
            HealthInsightType.BLOOD_PRESSURE -> "Blood Pressure"
            HealthInsightType.BLOOD_OXYGEN -> "Blood Oxygen"
            HealthInsightType.TEMPERATURE -> "Body Temperature"
            HealthInsightType.STRESS_LEVEL -> "Stress Level"
            HealthInsightType.OVERALL_HEALTH -> "Overall Health"
        }
    }

    /**
     * Get the insight score category
     * @return Insight score category as a string
     */
    fun getScoreCategory(): String? {
        return score?.let {
            when {
                it >= 80 -> "Excellent"
                it >= 60 -> "Good"
                it >= 40 -> "Fair"
                it >= 20 -> "Poor"
                else -> "Very Poor"
            }
        }
    }

    /**
     * Get the trend icon
     * @return Trend icon as a string
     */
    fun getTrendIcon(): String {
        return when (trend) {
            HealthInsightTrend.IMPROVING -> "↗️"
            HealthInsightTrend.STABLE -> "→"
            HealthInsightTrend.DECLINING -> "↘️"
            HealthInsightTrend.INSUFFICIENT_DATA -> "?"
        }
    }

    /**
     * Get the severity icon
     * @return Severity icon as a string
     */
    fun getSeverityIcon(): String {
        return when (severity) {
            HealthInsightSeverity.INFO -> "ℹ️"
            HealthInsightSeverity.WARNING -> "⚠️"
            HealthInsightSeverity.ALERT -> "🚨"
        }
    }

    companion object {
        /**
         * Create a health insight from a type and score
         * @param userId User ID
         * @param date Date
         * @param insightType Insight type
         * @param score Score
         * @param trend Trend
         * @param severity Severity
         * @return Health insight with default values
         */
        fun create(
            userId: Long,
            date: LocalDate,
            insightType: HealthInsightType,
            score: Int? = null,
            trend: HealthInsightTrend = HealthInsightTrend.INSUFFICIENT_DATA,
            severity: HealthInsightSeverity = HealthInsightSeverity.INFO
        ): HealthInsight {
            return HealthInsight(
                id = 0,
                userId = userId,
                date = date,
                insightType = insightType,
                score = score,
                trend = trend,
                severity = severity
            )
        }
    }
}

/**
 * Notification type enum
 */
enum class NotificationType {
    CALL,
    MESSAGE,
    HEALTH_ALERT,
    DEVICE_ALERT,
    SYSTEM_ALERT;

    companion object {
        fun fromString(value: String?): NotificationType {
            return when (value?.uppercase()) {
                "CALL" -> CALL
                "MESSAGE" -> MESSAGE
                "HEALTH_ALERT" -> HEALTH_ALERT
                "DEVICE_ALERT" -> DEVICE_ALERT
                else -> SYSTEM_ALERT
            }
        }
    }
}

/**
 * Notification model representing a device notification
 */
data class Notification(
    override val id: Long,
    val deviceId: Long,
    val type: NotificationType,
    val title: String,
    val content: String? = null,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val isRead: Boolean = false,
    val isDelivered: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.PENDING
) : DomainModel {

    /**
     * Get the notification icon
     * @return Notification icon as a string
     */
    fun getIcon(): String {
        return when (type) {
            NotificationType.CALL -> "📞"
            NotificationType.MESSAGE -> "💬"
            NotificationType.HEALTH_ALERT -> "❤️"
            NotificationType.DEVICE_ALERT -> "⌚"
            NotificationType.SYSTEM_ALERT -> "🔔"
        }
    }

    /**
     * Get the time elapsed since the notification
     * @return Time elapsed as a string
     */
    fun getTimeElapsed(): String {
        val now = LocalDateTime.now()
        val minutes = ChronoUnit.MINUTES.between(timestamp, now)
        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "$minutes minutes ago"
            minutes < 24 * 60 -> "${minutes / 60} hours ago"
            else -> "${minutes / (24 * 60)} days ago"
        }
    }

    companion object {
        /**
         * Create a notification from a type and title
         * @param deviceId Device ID
         * @param type Notification type
         * @param title Notification title
         * @param content Notification content
         * @return Notification with default values
         */
        fun create(
            deviceId: Long,
            type: NotificationType,
            title: String,
            content: String? = null
        ): Notification {
            return Notification(
                id = 0,
                deviceId = deviceId,
                type = type,
                title = title,
                content = content
            )
        }
    }
}

/**
 * Setting model representing an app or device setting
 */
data class Setting(
    override val id: Long,
    val key: String,
    val value: String,
    val deviceId: Long? = null, // Null for app-wide settings
    val category: String? = null
) : DomainModel {

    /**
     * Get the value as a boolean
     * @return Value as a boolean, or null if not a boolean
     */
    fun getBooleanValue(): Boolean? {
        return when (value.lowercase()) {
            "true", "yes", "1", "on" -> true
            "false", "no", "0", "off" -> false
            else -> null
        }
    }

    /**
     * Get the value as an integer
     * @return Value as an integer, or null if not an integer
     */
    fun getIntValue(): Int? {
        return value.toIntOrNull()
    }

    /**
     * Get the value as a float
     * @return Value as a float, or null if not a float
     */
    fun getFloatValue(): Float? {
        return value.toFloatOrNull()
    }

    companion object {
        /**
         * Create a setting from a key and value
         * @param key Setting key
         * @param value Setting value
         * @param deviceId Device ID
         * @param category Setting category
         * @return Setting with default values
         */
        fun create(
            key: String,
            value: String,
            deviceId: Long? = null,
            category: String? = null
        ): Setting {
            return Setting(
                id = 0,
                key = key,
                value = value,
                deviceId = deviceId,
                category = category
            )
        }

        // Common setting keys
        const val KEY_FIRST_LAUNCH = "first_launch"
        const val KEY_LAST_SYNC = "last_sync"
        const val KEY_THEME = "theme"
        const val KEY_UNITS = "units"
        const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        const val KEY_HEALTH_ALERTS_ENABLED = "health_alerts_enabled"
        const val KEY_DEVICE_ALERTS_ENABLED = "device_alerts_enabled"
        const val KEY_SYNC_FREQUENCY = "sync_frequency"
        const val KEY_AUTO_CONNECT = "auto_connect"
        const val KEY_LAST_CONNECTED_DEVICE = "last_connected_device"
    }
}

/**
 * Health summary model representing a summary of health data
 */
data class HealthSummary(
    val date: LocalDate,
    val steps: Int? = null,
    val distance: Float? = null, // in meters
    val calories: Float? = null, // in kcal
    val activeMinutes: Int? = null,
    val avgHeartRate: Double? = null,
    val minHeartRate: Int? = null,
    val maxHeartRate: Int? = null,
    val restingHeartRate: Int? = null,
    val avgBloodOxygen: Double? = null,
    val minBloodOxygen: Int? = null,
    val maxBloodOxygen: Int? = null,
    val avgSystolic: Double? = null,
    val avgDiastolic: Double? = null,
    val avgTemperature: Double? = null,
    val sleepDuration: Int? = null, // in minutes
    val sleepQuality: Int? = null, // 0-100
    val activityCount: Int = 0,
    val insights: List<HealthInsight> = emptyList()
) {
    /**
     * Get the steps progress percentage
     * @param goalSteps Goal steps
     * @return Steps progress percentage
     */
    fun getStepsProgressPercentage(goalSteps: Int): Int? {
        return steps?.let { min((it.toFloat() / goalSteps.toFloat() * 100).toInt(), 100) }
    }

    /**
     * Get the distance in kilometers
     * @return Distance in kilometers, or null if distance is not available
     */
    fun getDistanceKm(): Float? {
        return distance?.let { it / 1000f }
    }

    /**
     * Get the sleep duration as a string
     * @return Sleep duration as a string (e.g., "8h 30m"), or null if sleep duration is not available
     */
    fun getSleepDurationString(): String? {
        return sleepDuration?.let {
            val hours = it / 60
            val minutes = it % 60
            "${hours}h ${minutes}m"
        }
    }

    /**
     * Get the overall health score
     * @return Overall health score (0-100), or null if required data is not available
     */
    fun getOverallHealthScore(): Int? {
        var scoreSum = 0
        var scoreCount = 0

        // Steps score (0-100)
        steps?.let {
            val stepsScore = min((it.toFloat() / 10000f * 100).toInt(), 100)
            scoreSum += stepsScore
            scoreCount++
        }

        // Heart rate score (0-100)
        restingHeartRate?.let {
            val heartRateScore = when {
                it < 60 -> 100
                it < 70 -> 80
                it < 80 -> 60
                it < 90 -> 40
                else -> 20
            }
            scoreSum += heartRateScore
            scoreCount++
        }

        // Blood oxygen score (0-100)
        avgBloodOxygen?.let {
            val bloodOxygenScore = when {
                it >= 95 -> 100
                it >= 90 -> 60
                else -> 20
            }
            scoreSum += bloodOxygenScore
            scoreCount++
        }

        // Sleep score (0-100)
        sleepQuality?.let {
            scoreSum += it
            scoreCount++
        }

        // Blood pressure score (0-100)
        if (avgSystolic != null && avgDiastolic != null) {
            val bloodPressureScore = when {
                avgSystolic < 120 && avgDiastolic < 80 -> 100
                avgSystolic < 130 && avgDiastolic < 80 -> 80
                avgSystolic < 140 && avgDiastolic < 90 -> 60
                avgSystolic < 180 && avgDiastolic < 120 -> 40
                else -> 20
            }
            scoreSum += bloodPressureScore
            scoreCount++
        }

        return if (scoreCount > 0) {
            scoreSum / scoreCount
        } else {
            null
        }
    }

    /**
     * Get the overall health status
     * @return Overall health status as a string, or null if overall health score is not available
     */
    fun getOverallHealthStatus(): String? {
        return getOverallHealthScore()?.let {
            when {
                it >= 80 -> "Excellent"
                it >= 60 -> "Good"
                it >= 40 -> "Fair"
                it >= 20 -> "Poor"
                else -> "Very Poor"
            }
        }
    }
}

/**
 * Extension functions for LocalDate
 */
fun LocalDate.toEpochMilli(): Long {
    return this.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

fun LocalDate.toFormattedString(pattern: String = "yyyy-MM-dd"): String {
    return this.format(DateTimeFormatter.ofPattern(pattern))
}

fun LocalDate.isToday(): Boolean {
    return this == LocalDate.now()
}

fun LocalDate.isYesterday(): Boolean {
    return this == LocalDate.now().minusDays(1)
}

fun LocalDate.isTomorrow(): Boolean {
    return this == LocalDate.now().plusDays(1)
}

fun LocalDate.isInCurrentWeek(): Boolean {
    val today = LocalDate.now()
    val startOfWeek = today.minusDays(today.dayOfWeek.value - 1L)
    val endOfWeek = startOfWeek.plusDays(6)
    return !this.isBefore(startOfWeek) && !this.isAfter(endOfWeek)
}

fun LocalDate.isInCurrentMonth(): Boolean {
    val today = LocalDate.now()
    return this.year == today.year && this.month == today.month
}

fun LocalDate.getWeekOfYear(): Int {
    return this.get(java.time.temporal.WeekFields.ISO.weekOfYear())
}

/**
 * Extension functions for LocalDateTime
 */
fun LocalDateTime.toEpochMilli(): Long {
    return this.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

fun LocalDateTime.toFormattedString(pattern: String = "yyyy-MM-dd HH:mm:ss"): String {
    return this.format(DateTimeFormatter.ofPattern(pattern))
}

fun LocalDateTime.isToday(): Boolean {
    return this.toLocalDate() == LocalDate.now()
}

fun LocalDateTime.isYesterday(): Boolean {
    return this.toLocalDate() == LocalDate.now().minusDays(1)
}

fun LocalDateTime.getTimeAgo(): String {
    val now = LocalDateTime.now()
    val minutes = ChronoUnit.MINUTES.between(this, now)
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "$minutes minutes ago"
        minutes < 24 * 60 -> "${minutes / 60} hours ago"
        minutes < 48 * 60 -> "Yesterday"
        else -> "${minutes / (24 * 60)} days ago"
    }
}

/**
 * Extension functions for Long (timestamp)
 */
fun Long.toLocalDate(): LocalDate {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
}

fun Long.toLocalDateTime(): LocalDateTime {
    return Instant.ofEpochMilli(this)
        .atZone