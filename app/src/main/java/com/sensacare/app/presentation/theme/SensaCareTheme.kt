package com.sensacare.app.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sensacare.app.domain.model.AlertSeverity
import com.sensacare.app.domain.model.HealthStatus
import com.sensacare.app.domain.model.TrendDirection

/**
 * SensaCare Theme System
 *
 * A comprehensive Material 3 theme system optimized for health monitoring and wellness applications.
 * Features include:
 * - Health-focused color palette with semantic meaning
 * - Dark and light theme support
 * - Specialized colors for different health metrics
 * - Typography optimized for health data readability
 * - Custom shapes for health monitoring components
 * - Accessibility-focused design with proper contrast
 * - Dynamic color support for Android 12+
 * - Theme extensions for health-specific components
 */

// Base Colors
val md_theme_light_primary = Color(0xFF0D6EFD)
val md_theme_light_onPrimary = Color(0xFFFFFFFF)
val md_theme_light_primaryContainer = Color(0xFFD8E6FF)
val md_theme_light_onPrimaryContainer = Color(0xFF001C3A)
val md_theme_light_secondary = Color(0xFF7749BD)
val md_theme_light_onSecondary = Color(0xFFFFFFFF)
val md_theme_light_secondaryContainer = Color(0xFFEFDCFF)
val md_theme_light_onSecondaryContainer = Color(0xFF280056)
val md_theme_light_tertiary = Color(0xFF20A779)
val md_theme_light_onTertiary = Color(0xFFFFFFFF)
val md_theme_light_tertiaryContainer = Color(0xFFB3F2D9)
val md_theme_light_onTertiaryContainer = Color(0xFF002116)
val md_theme_light_error = Color(0xFFDC3545)
val md_theme_light_errorContainer = Color(0xFFFFDAD6)
val md_theme_light_onError = Color(0xFFFFFFFF)
val md_theme_light_onErrorContainer = Color(0xFF410002)
val md_theme_light_background = Color(0xFFF8F9FA)
val md_theme_light_onBackground = Color(0xFF1B1B1F)
val md_theme_light_surface = Color(0xFFFFFFFF)
val md_theme_light_onSurface = Color(0xFF1B1B1F)
val md_theme_light_surfaceVariant = Color(0xFFE1E3E9)
val md_theme_light_onSurfaceVariant = Color(0xFF44474F)
val md_theme_light_outline = Color(0xFF74777F)
val md_theme_light_inverseOnSurface = Color(0xFFF2F0F4)
val md_theme_light_inverseSurface = Color(0xFF303033)
val md_theme_light_inversePrimary = Color(0xFFADC7FF)
val md_theme_light_shadow = Color(0xFF000000)
val md_theme_light_surfaceTint = Color(0xFF0D6EFD)
val md_theme_light_outlineVariant = Color(0xFFC4C6D0)
val md_theme_light_scrim = Color(0xFF000000)

val md_theme_dark_primary = Color(0xFFADC7FF)
val md_theme_dark_onPrimary = Color(0xFF00285E)
val md_theme_dark_primaryContainer = Color(0xFF0D47A1)
val md_theme_dark_onPrimaryContainer = Color(0xFFD8E6FF)
val md_theme_dark_secondary = Color(0xFFD9B9FF)
val md_theme_dark_onSecondary = Color(0xFF3F0071)
val md_theme_dark_secondaryContainer = Color(0xFF5B2A98)
val md_theme_dark_onSecondaryContainer = Color(0xFFEFDCFF)
val md_theme_dark_tertiary = Color(0xFF93D7BE)
val md_theme_dark_onTertiary = Color(0xFF003828)
val md_theme_dark_tertiaryContainer = Color(0xFF00513A)
val md_theme_dark_onTertiaryContainer = Color(0xFFB3F2D9)
val md_theme_dark_error = Color(0xFFFFB4AB)
val md_theme_dark_errorContainer = Color(0xFFBF2231)
val md_theme_dark_onError = Color(0xFF690005)
val md_theme_dark_onErrorContainer = Color(0xFFFFDAD6)
val md_theme_dark_background = Color(0xFF1B1B1F)
val md_theme_dark_onBackground = Color(0xFFE3E2E6)
val md_theme_dark_surface = Color(0xFF121212)
val md_theme_dark_onSurface = Color(0xFFE3E2E6)
val md_theme_dark_surfaceVariant = Color(0xFF44474F)
val md_theme_dark_onSurfaceVariant = Color(0xFFC4C6D0)
val md_theme_dark_outline = Color(0xFF8E9099)
val md_theme_dark_inverseOnSurface = Color(0xFF1B1B1F)
val md_theme_dark_inverseSurface = Color(0xFFE3E2E6)
val md_theme_dark_inversePrimary = Color(0xFF0D6EFD)
val md_theme_dark_shadow = Color(0xFF000000)
val md_theme_dark_surfaceTint = Color(0xFFADC7FF)
val md_theme_dark_outlineVariant = Color(0xFF44474F)
val md_theme_dark_scrim = Color(0xFF000000)

// Health Metric Colors - Used for specific health metrics visualization
object HealthMetricColors {
    // Heart Rate - Red tones
    val heartRate = Color(0xFFDC3545)
    val heartRateLight = Color(0xFFFFDAD6)
    val heartRateDark = Color(0xFFBF2231)
    
    // Blood Pressure - Purple tones
    val bloodPressure = Color(0xFF7749BD)
    val bloodPressureLight = Color(0xFFEFDCFF)
    val bloodPressureDark = Color(0xFF5B2A98)
    
    // Sleep - Blue tones
    val sleep = Color(0xFF4361EE)
    val sleepLight = Color(0xFFD8E2FF)
    val sleepDark = Color(0xFF3A56D4)
    
    // Activity - Green tones
    val activity = Color(0xFF20A779)
    val activityLight = Color(0xFFB3F2D9)
    val activityDark = Color(0xFF00513A)
    
    // Stress - Orange tones
    val stress = Color(0xFFFD7E14)
    val stressLight = Color(0xFFFFDCC3)
    val stressDark = Color(0xFFBF5E00)
    
    // Nutrition - Yellow tones
    val nutrition = Color(0xFFFFC107)
    val nutritionLight = Color(0xFFFFECB3)
    val nutritionDark = Color(0xFFBF9000)
    
    // Oxygen - Cyan tones
    val oxygen = Color(0xFF17A2B8)
    val oxygenLight = Color(0xFFCFF4FB)
    val oxygenDark = Color(0xFF007A8C)
    
    // Weight - Teal tones
    val weight = Color(0xFF20C997)
    val weightLight = Color(0xFFB3EFDE)
    val weightDark = Color(0xFF00966D)
    
    // Temperature - Red-Orange tones
    val temperature = Color(0xFFFF6B6B)
    val temperatureLight = Color(0xFFFFD6D6)
    val temperatureDark = Color(0xFFBF0000)
    
    // Glucose - Purple-Blue tones
    val glucose = Color(0xFF6F42C1)
    val glucoseLight = Color(0xFFE2D9F3)
    val glucoseDark = Color(0xFF4E2D89)
}

// Chart Colors - For data visualization
object ChartColors {
    val primary = listOf(
        Color(0xFF0D6EFD),
        Color(0xFF20A779),
        Color(0xFF7749BD),
        Color(0xFFDC3545),
        Color(0xFFFD7E14)
    )
    
    val secondary = listOf(
        Color(0xFF4361EE),
        Color(0xFF3A0CA3),
        Color(0xFF7209B7),
        Color(0xFFF72585),
        Color(0xFF4CC9F0)
    )
    
    val pastel = listOf(
        Color(0xFFADC7FF),
        Color(0xFFB3F2D9),
        Color(0xFFD9B9FF),
        Color(0xFFFFB4AB),
        Color(0xFFFFDCC3)
    )
    
    val gradient = listOf(
        listOf(Color(0xFF0D6EFD), Color(0xFF4361EE)),
        listOf(Color(0xFF20A779), Color(0xFF20C997)),
        listOf(Color(0xFF7749BD), Color(0xFF6F42C1)),
        listOf(Color(0xFFDC3545), Color(0xFFFF6B6B)),
        listOf(Color(0xFFFD7E14), Color(0xFFFFC107))
    )
}

// Health Status Colors - For indicating health conditions
object HealthStatusColors {
    val excellent = Color(0xFF20A779)
    val good = Color(0xFF0D6EFD)
    val fair = Color(0xFFFD7E14)
    val poor = Color(0xFFDC3545)
}

// Alert Severity Colors - For indicating alert levels
object AlertSeverityColors {
    val low = Color(0xFF0D6EFD)
    val medium = Color(0xFFFD7E14)
    val high = Color(0xFFDC3545)
    val emergency = Color(0xFFDC3545)
}

// Trend Direction Colors - For indicating trends
object TrendDirectionColors {
    val up = Color(0xFFDC3545)
    val down = Color(0xFF20A779)
    val stable = Color(0xFF0D6EFD)
}

// Typography
val SensaCareTypography = Typography(
    // Display styles - Used for main headings
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    
    // Headline styles - Used for section headers
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    
    // Title styles - Used for card titles and smaller headers
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    
    // Body styles - Used for general text content
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    
    // Label styles - Used for smaller text elements
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

// Shapes
val SensaCareShapes = Shapes(
    extraSmall = ShapeDefaults.ExtraSmall,
    small = ShapeDefaults.Small,
    medium = ShapeDefaults.Medium,
    large = ShapeDefaults.Large,
    extraLarge = ShapeDefaults.ExtraLarge
)

// Elevations
object SensaCareElevation {
    val Level0 = 0.dp
    val Level1 = 1.dp
    val Level2 = 3.dp
    val Level3 = 6.dp
    val Level4 = 8.dp
    val Level5 = 12.dp
}

@Composable
fun SensaCareTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme(
            primary = md_theme_dark_primary,
            onPrimary = md_theme_dark_onPrimary,
            primaryContainer = md_theme_dark_primaryContainer,
            onPrimaryContainer = md_theme_dark_onPrimaryContainer,
            secondary = md_theme_dark_secondary,
            onSecondary = md_theme_dark_onSecondary,
            secondaryContainer = md_theme_dark_secondaryContainer,
            onSecondaryContainer = md_theme_dark_onSecondaryContainer,
            tertiary = md_theme_dark_tertiary,
            onTertiary = md_theme_dark_onTertiary,
            tertiaryContainer = md_theme_dark_tertiaryContainer,
            onTertiaryContainer = md_theme_dark_onTertiaryContainer,
            error = md_theme_dark_error,
            errorContainer = md_theme_dark_errorContainer,
            onError = md_theme_dark_onError,
            onErrorContainer = md_theme_dark_onErrorContainer,
            background = md_theme_dark_background,
            onBackground = md_theme_dark_onBackground,
            surface = md_theme_dark_surface,
            onSurface = md_theme_dark_onSurface,
            surfaceVariant = md_theme_dark_surfaceVariant,
            onSurfaceVariant = md_theme_dark_onSurfaceVariant,
            outline = md_theme_dark_outline,
            inverseOnSurface = md_theme_dark_inverseOnSurface,
            inverseSurface = md_theme_dark_inverseSurface,
            inversePrimary = md_theme_dark_inversePrimary,
            surfaceTint = md_theme_dark_surfaceTint,
            outlineVariant = md_theme_dark_outlineVariant,
            scrim = md_theme_dark_scrim,
        )
        else -> lightColorScheme(
            primary = md_theme_light_primary,
            onPrimary = md_theme_light_onPrimary,
            primaryContainer = md_theme_light_primaryContainer,
            onPrimaryContainer = md_theme_light_onPrimaryContainer,
            secondary = md_theme_light_secondary,
            onSecondary = md_theme_light_onSecondary,
            secondaryContainer = md_theme_light_secondaryContainer,
            onSecondaryContainer = md_theme_light_onSecondaryContainer,
            tertiary = md_theme_light_tertiary,
            onTertiary = md_theme_light_onTertiary,
            tertiaryContainer = md_theme_light_tertiaryContainer,
            onTertiaryContainer = md_theme_light_onTertiaryContainer,
            error = md_theme_light_error,
            errorContainer = md_theme_light_errorContainer,
            onError = md_theme_light_onError,
            onErrorContainer = md_theme_light_onErrorContainer,
            background = md_theme_light_background,
            onBackground = md_theme_light_onBackground,
            surface = md_theme_light_surface,
            onSurface = md_theme_light_onSurface,
            surfaceVariant = md_theme_light_surfaceVariant,
            onSurfaceVariant = md_theme_light_onSurfaceVariant,
            outline = md_theme_light_outline,
            inverseOnSurface = md_theme_light_inverseOnSurface,
            inverseSurface = md_theme_light_inverseSurface,
            inversePrimary = md_theme_light_inversePrimary,
            surfaceTint = md_theme_light_surfaceTint,
            outlineVariant = md_theme_light_outlineVariant,
            scrim = md_theme_light_scrim,
        )
    }

    // Create a CompositionLocal for health-specific colors
    val healthMetricColors = if (darkTheme) {
        HealthMetricColorsImpl(
            heartRate = HealthMetricColors.heartRate,
            heartRateContainer = HealthMetricColors.heartRateDark,
            bloodPressure = HealthMetricColors.bloodPressure,
            bloodPressureContainer = HealthMetricColors.bloodPressureDark,
            sleep = HealthMetricColors.sleep,
            sleepContainer = HealthMetricColors.sleepDark,
            activity = HealthMetricColors.activity,
            activityContainer = HealthMetricColors.activityDark,
            stress = HealthMetricColors.stress,
            stressContainer = HealthMetricColors.stressDark,
            nutrition = HealthMetricColors.nutrition,
            nutritionContainer = HealthMetricColors.nutritionDark,
            oxygen = HealthMetricColors.oxygen,
            oxygenContainer = HealthMetricColors.oxygenDark,
            weight = HealthMetricColors.weight,
            weightContainer = HealthMetricColors.weightDark,
            temperature = HealthMetricColors.temperature,
            temperatureContainer = HealthMetricColors.temperatureDark,
            glucose = HealthMetricColors.glucose,
            glucoseContainer = HealthMetricColors.glucoseDark
        )
    } else {
        HealthMetricColorsImpl(
            heartRate = HealthMetricColors.heartRate,
            heartRateContainer = HealthMetricColors.heartRateLight,
            bloodPressure = HealthMetricColors.bloodPressure,
            bloodPressureContainer = HealthMetricColors.bloodPressureLight,
            sleep = HealthMetricColors.sleep,
            sleepContainer = HealthMetricColors.sleepLight,
            activity = HealthMetricColors.activity,
            activityContainer = HealthMetricColors.activityLight,
            stress = HealthMetricColors.stress,
            stressContainer = HealthMetricColors.stressLight,
            nutrition = HealthMetricColors.nutrition,
            nutritionContainer = HealthMetricColors.nutritionLight,
            oxygen = HealthMetricColors.oxygen,
            oxygenContainer = HealthMetricColors.oxygenLight,
            weight = HealthMetricColors.weight,
            weightContainer = HealthMetricColors.weightLight,
            temperature = HealthMetricColors.temperature,
            temperatureContainer = HealthMetricColors.temperatureLight,
            glucose = HealthMetricColors.glucose,
            glucoseContainer = HealthMetricColors.glucoseLight
        )
    }

    // Create a CompositionLocal for health status colors
    val healthStatusColors = HealthStatusColorsImpl(
        excellent = HealthStatusColors.excellent,
        good = HealthStatusColors.good,
        fair = HealthStatusColors.fair,
        poor = HealthStatusColors.poor
    )

    // Create a CompositionLocal for alert severity colors
    val alertSeverityColors = AlertSeverityColorsImpl(
        low = AlertSeverityColors.low,
        medium = AlertSeverityColors.medium,
        high = AlertSeverityColors.high,
        emergency = AlertSeverityColors.emergency
    )

    // Create a CompositionLocal for trend direction colors
    val trendDirectionColors = TrendDirectionColorsImpl(
        up = TrendDirectionColors.up,
        down = TrendDirectionColors.down,
        stable = TrendDirectionColors.stable
    )

    // Provide the custom color sets through CompositionLocal
    CompositionLocalProvider(
        LocalHealthMetricColors provides healthMetricColors,
        LocalHealthStatusColors provides healthStatusColors,
        LocalAlertSeverityColors provides alertSeverityColors,
        LocalTrendDirectionColors provides trendDirectionColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SensaCareTypography,
            shapes = SensaCareShapes,
            content = content
        )
    }
}

// Health Metric Colors
interface HealthMetricColorsInterface {
    val heartRate: Color
    val heartRateContainer: Color
    val bloodPressure: Color
    val bloodPressureContainer: Color
    val sleep: Color
    val sleepContainer: Color
    val activity: Color
    val activityContainer: Color
    val stress: Color
    val stressContainer: Color
    val nutrition: Color
    val nutritionContainer: Color
    val oxygen: Color
    val oxygenContainer: Color
    val weight: Color
    val weightContainer: Color
    val temperature: Color
    val temperatureContainer: Color
    val glucose: Color
    val glucoseContainer: Color
}

data class HealthMetricColorsImpl(
    override val heartRate: Color,
    override val heartRateContainer: Color,
    override val bloodPressure: Color,
    override val bloodPressureContainer: Color,
    override val sleep: Color,
    override val sleepContainer: Color,
    override val activity: Color,
    override val activityContainer: Color,
    override val stress: Color,
    override val stressContainer: Color,
    override val nutrition: Color,
    override val nutritionContainer: Color,
    override val oxygen: Color,
    override val oxygenContainer: Color,
    override val weight: Color,
    override val weightContainer: Color,
    override val temperature: Color,
    override val temperatureContainer: Color,
    override val glucose: Color,
    override val glucoseContainer: Color
) : HealthMetricColorsInterface

val LocalHealthMetricColors = compositionLocalOf<HealthMetricColorsInterface> {
    error("No HealthMetricColors provided")
}

// Health Status Colors
interface HealthStatusColorsInterface {
    val excellent: Color
    val good: Color
    val fair: Color
    val poor: Color
}

data class HealthStatusColorsImpl(
    override val excellent: Color,
    override val good: Color,
    override val fair: Color,
    override val poor: Color
) : HealthStatusColorsInterface

val LocalHealthStatusColors = compositionLocalOf<HealthStatusColorsInterface> {
    error("No HealthStatusColors provided")
}

// Alert Severity Colors
interface AlertSeverityColorsInterface {
    val low: Color
    val medium: Color
    val high: Color
    val emergency: Color
}

data class AlertSeverityColorsImpl(
    override val low: Color,
    override val medium: Color,
    override val high: Color,
    override val emergency: Color
) : AlertSeverityColorsInterface

val LocalAlertSeverityColors = compositionLocalOf<AlertSeverityColorsInterface> {
    error("No AlertSeverityColors provided")
}

// Trend Direction Colors
interface TrendDirectionColorsInterface {
    val up: Color
    val down: Color
    val stable: Color
}

data class TrendDirectionColorsImpl(
    override val up: Color,
    override val down: Color,
    override val stable: Color
) : TrendDirectionColorsInterface

val LocalTrendDirectionColors = compositionLocalOf<TrendDirectionColorsInterface> {
    error("No TrendDirectionColors provided")
}

// Extension functions for accessing theme colors

/**
 * Get the color for a specific health metric
 */
@Composable
fun getMetricColor(metricType: MetricType): Color {
    val healthMetricColors = LocalHealthMetricColors.current
    return when (metricType) {
        MetricType.HEART_RATE -> healthMetricColors.heartRate
        MetricType.BLOOD_PRESSURE -> healthMetricColors.bloodPressure
        MetricType.SLEEP_DURATION -> healthMetricColors.sleep
        MetricType.STEPS -> healthMetricColors.activity
        MetricType.ACTIVITY_MINUTES -> healthMetricColors.activity
        MetricType.STRESS -> healthMetricColors.stress
        MetricType.CALORIES -> healthMetricColors.nutrition
        MetricType.WATER_INTAKE -> healthMetricColors.nutrition
        MetricType.OXYGEN -> healthMetricColors.oxygen
        MetricType.WEIGHT -> healthMetricColors.weight
        MetricType.TEMPERATURE -> healthMetricColors.temperature
        MetricType.GLUCOSE -> healthMetricColors.glucose
        MetricType.NONE -> MaterialTheme.colorScheme.primary
    }
}

/**
 * Get the container color for a specific health metric
 */
@Composable
fun getMetricContainerColor(metricType: MetricType): Color {
    val healthMetricColors = LocalHealthMetricColors.current
    return when (metricType) {
        MetricType.HEART_RATE -> healthMetricColors.heartRateContainer
        MetricType.BLOOD_PRESSURE -> healthMetricColors.bloodPressureContainer
        MetricType.SLEEP_DURATION -> healthMetricColors.sleepContainer
        MetricType.STEPS -> healthMetricColors.activityContainer
        MetricType.ACTIVITY_MINUTES -> healthMetricColors.activityContainer
        MetricType.STRESS -> healthMetricColors.stressContainer
        MetricType.CALORIES -> healthMetricColors.nutritionContainer
        MetricType.WATER_INTAKE -> healthMetricColors.nutritionContainer
        MetricType.OXYGEN -> healthMetricColors.oxygenContainer
        MetricType.WEIGHT -> healthMetricColors.weightContainer
        MetricType.TEMPERATURE -> healthMetricColors.temperatureContainer
        MetricType.GLUCOSE -> healthMetricColors.glucoseContainer
        MetricType.NONE -> MaterialTheme.colorScheme.primaryContainer
    }
}

/**
 * Get the color for a health status
 */
@Composable
fun getHealthStatusColor(status: HealthStatus): Color {
    val healthStatusColors = LocalHealthStatusColors.current
    return when (status) {
        HealthStatus.EXCELLENT -> healthStatusColors.excellent
        HealthStatus.GOOD -> healthStatusColors.good
        HealthStatus.FAIR -> healthStatusColors.fair
        HealthStatus.POOR -> healthStatusColors.poor
    }
}

/**
 * Get the color for an alert severity
 */
@Composable
fun getAlertSeverityColor(severity: AlertSeverity): Color {
    val alertSeverityColors = LocalAlertSeverityColors.current
    return when (severity) {
        AlertSeverity.LOW -> alertSeverityColors.low
        AlertSeverity.MEDIUM -> alertSeverityColors.medium
        AlertSeverity.HIGH -> alertSeverityColors.high
        AlertSeverity.EMERGENCY -> alertSeverityColors.emergency
    }
}

/**
 * Get the color for a trend direction
 */
@Composable
fun getTrendDirectionColor(trend: TrendDirection): Color {
    val trendDirectionColors = LocalTrendDirectionColors.current
    return when (trend) {
        TrendDirection.UP -> trendDirectionColors.up
        TrendDirection.DOWN -> trendDirectionColors.down
        TrendDirection.STABLE -> trendDirectionColors.stable
    }
}

/**
 * Get a chart color palette
 */
@Composable
fun getChartColorPalette(index: Int = 0, type: ChartColorType = ChartColorType.PRIMARY): List<Color> {
    return when (type) {
        ChartColorType.PRIMARY -> ChartColors.primary
        ChartColorType.SECONDARY -> ChartColors.secondary
        ChartColorType.PASTEL -> ChartColors.pastel
        ChartColorType.GRADIENT -> ChartColors.gradient[index % ChartColors.gradient.size]
    }
}

/**
 * Enum for chart color types
 */
enum class ChartColorType {
    PRIMARY,
    SECONDARY,
    PASTEL,
    GRADIENT
}

/**
 * Enum for metric types
 */
enum class MetricType {
    HEART_RATE,
    BLOOD_PRESSURE,
    SLEEP_DURATION,
    STEPS,
    ACTIVITY_MINUTES,
    STRESS,
    CALORIES,
    WATER_INTAKE,
    OXYGEN,
    WEIGHT,
    TEMPERATURE,
    GLUCOSE,
    NONE
}
