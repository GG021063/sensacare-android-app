package com.sensacare.app.presentation.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.sensacare.app.R
import com.sensacare.app.domain.model.*
import com.sensacare.app.domain.usecase.health.TimeRange
import com.sensacare.app.presentation.common.AnimatedCounter
import com.sensacare.app.presentation.common.ErrorView
import com.sensacare.app.presentation.common.LoadingView
import com.sensacare.app.presentation.common.PulsingEffect
import com.sensacare.app.presentation.theme.*
import com.sensacare.app.util.formatDate
import com.sensacare.app.util.formatDateTime
import com.sensacare.app.util.formatTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * HealthDashboardScreen - Main dashboard for health monitoring
 *
 * This screen displays a comprehensive overview of the user's health data, including:
 * - Current health status with color-coded indicators
 * - Real-time health metrics (heart rate, blood pressure, activity, sleep)
 * - Interactive charts and visualizations
 * - AI-powered health insights and recommendations
 * - Recent alerts and notifications
 * - Quick actions for device management and goals
 * - Daily/weekly/monthly health summaries
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun HealthDashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: HealthDashboardViewModel = hiltViewModel()
) {
    // State
    val uiState by viewModel.uiState.collectAsState()
    val healthMetrics by viewModel.healthMetrics.collectAsState()
    val healthSummary by viewModel.healthSummary.collectAsState()
    val healthInsights by viewModel.healthInsights.collectAsState()
    val recentAlerts by viewModel.recentAlerts.collectAsState()
    val connectedDevices by viewModel.connectedDevices.collectAsState()
    val activeGoals by viewModel.activeGoals.collectAsState()
    val selectedTimeRange by viewModel.selectedTimeRange.collectAsState()
    
    // Local state
    val refreshing by viewModel.isRefreshing.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    
    // Pull-to-refresh state
    val pullRefreshState = rememberPullRefreshState(
        refreshing = refreshing,
        onRefresh = {
            coroutineScope.launch {
                viewModel.refreshDashboard()
            }
        }
    )
    
    // Collect error events
    LaunchedEffect(viewModel) {
        viewModel.errorEvents.collect { error ->
            // Handle error events
        }
    }
    
    // Main content
    Box(
        modifier = modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        when (uiState) {
            is HealthDashboardUiState.Loading -> {
                LoadingView(
                    message = stringResource(R.string.loading_health_data)
                )
            }
            
            is HealthDashboardUiState.Error -> {
                val errorState = uiState as HealthDashboardUiState.Error
                ErrorView(
                    message = errorState.message,
                    onRetry = { viewModel.refreshDashboard() }
                )
            }
            
            is HealthDashboardUiState.Success -> {
                DashboardContent(
                    healthMetrics = healthMetrics,
                    healthSummary = healthSummary,
                    healthInsights = healthInsights,
                    recentAlerts = recentAlerts,
                    connectedDevices = connectedDevices,
                    activeGoals = activeGoals,
                    selectedTimeRange = selectedTimeRange,
                    onTimeRangeSelected = { viewModel.selectTimeRange(it) },
                    onMetricSelected = { viewModel.selectMetric(it) },
                    onInsightSelected = { viewModel.selectInsight(it) },
                    onAlertSelected = { viewModel.selectAlert(it) },
                    onDeviceSelected = { viewModel.selectDevice(it) },
                    onGoalSelected = { viewModel.selectGoal(it) },
                    onSyncDevices = { viewModel.syncAllDevices() },
                    onManageDevices = { viewModel.navigateToDeviceManagement() },
                    onManageGoals = { viewModel.navigateToGoalsManagement() },
                    onViewAllInsights = { viewModel.navigateToInsights() },
                    onViewAllAlerts = { viewModel.navigateToAlerts() },
                    onViewHistory = { viewModel.navigateToHistory() }
                )
            }
            
            is HealthDashboardUiState.Empty -> {
                EmptyDashboardContent(
                    onSetupDevices = { viewModel.navigateToDeviceManagement() }
                )
            }
        }
        
        // Pull-to-refresh indicator
        PullRefreshIndicator(
            refreshing = refreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            backgroundColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * Main dashboard content when data is available
 */
@Composable
private fun DashboardContent(
    healthMetrics: HealthMetrics,
    healthSummary: HealthSummary,
    healthInsights: List<HealthInsight>,
    recentAlerts: List<HealthAlert>,
    connectedDevices: List<HBandDevice>,
    activeGoals: List<HealthGoal>,
    selectedTimeRange: TimeRange,
    onTimeRangeSelected: (TimeRange) -> Unit,
    onMetricSelected: (MetricType) -> Unit,
    onInsightSelected: (Long) -> Unit,
    onAlertSelected: (Long) -> Unit,
    onDeviceSelected: (String) -> Unit,
    onGoalSelected: (Long) -> Unit,
    onSyncDevices: () -> Unit,
    onManageDevices: () -> Unit,
    onManageGoals: () -> Unit,
    onViewAllInsights: () -> Unit,
    onViewAllAlerts: () -> Unit,
    onViewHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animation for content appearance
    val contentAlpha = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        contentAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 500)
        )
    }
    
    // Scroll state
    val scrollState = rememberLazyListState()
    
    LazyColumn(
        state = scrollState,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .alpha(contentAlpha.value),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // Dashboard header
        item {
            DashboardHeader(
                healthSummary = healthSummary,
                selectedTimeRange = selectedTimeRange,
                onTimeRangeSelected = onTimeRangeSelected
            )
        }
        
        // Health status overview
        item {
            HealthStatusOverview(
                healthSummary = healthSummary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        
        // Real-time metrics
        item {
            RealTimeMetricsSection(
                healthMetrics = healthMetrics,
                onMetricSelected = onMetricSelected,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        
        // Health trends charts
        item {
            HealthTrendsSection(
                healthSummary = healthSummary,
                selectedTimeRange = selectedTimeRange,
                onViewHistory = onViewHistory,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        
        // Health insights
        item {
            HealthInsightsSection(
                insights = healthInsights,
                onInsightSelected = onInsightSelected,
                onViewAllInsights = onViewAllInsights,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        
        // Recent alerts
        if (recentAlerts.isNotEmpty()) {
            item {
                RecentAlertsSection(
                    alerts = recentAlerts,
                    onAlertSelected = onAlertSelected,
                    onViewAllAlerts = onViewAllAlerts,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
        
        // Connected devices
        item {
            ConnectedDevicesSection(
                devices = connectedDevices,
                onDeviceSelected = onDeviceSelected,
                onSyncDevices = onSyncDevices,
                onManageDevices = onManageDevices,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        
        // Active goals
        if (activeGoals.isNotEmpty()) {
            item {
                ActiveGoalsSection(
                    goals = activeGoals,
                    onGoalSelected = onGoalSelected,
                    onManageGoals = onManageGoals,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
        
        // Daily summary
        item {
            DailySummarySection(
                healthSummary = healthSummary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

/**
 * Empty dashboard content when no data is available
 */
@Composable
private fun EmptyDashboardContent(
    onSetupDevices: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.HealthAndSafety,
            contentDescription = null,
            modifier = Modifier
                .size(120.dp)
                .padding(bottom = 24.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        
        Text(
            text = stringResource(R.string.welcome_to_sensacare),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = stringResource(R.string.empty_dashboard_message),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onSetupDevices,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(56.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(text = stringResource(R.string.connect_device))
        }
    }
}

/**
 * Dashboard header with user greeting and time range selector
 */
@Composable
private fun DashboardHeader(
    healthSummary: HealthSummary,
    selectedTimeRange: TimeRange,
    onTimeRangeSelected: (TimeRange) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .padding(16.dp)
    ) {
        // Greeting
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.hello_user, healthSummary.userName),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                
                Text(
                    text = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d")),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
            }
            
            // User avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .border(2.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = healthSummary.userName.first().toString(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Time range selector
        TimeRangeSelector(
            selectedTimeRange = selectedTimeRange,
            onTimeRangeSelected = onTimeRangeSelected,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Time range selector for filtering dashboard data
 */
@Composable
private fun TimeRangeSelector(
    selectedTimeRange: TimeRange,
    onTimeRangeSelected: (TimeRange) -> Unit,
    modifier: Modifier = Modifier
) {
    val timeRanges = listOf(
        TimeRange.TODAY,
        TimeRange.WEEK,
        TimeRange.MONTH,
        TimeRange.YEAR
    )
    
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        timeRanges.forEach { timeRange ->
            val isSelected = timeRange == selectedTimeRange
            
            FilterChip(
                selected = isSelected,
                onClick = { onTimeRangeSelected(timeRange) },
                label = {
                    Text(
                        text = when (timeRange) {
                            TimeRange.TODAY -> stringResource(R.string.today)
                            TimeRange.WEEK -> stringResource(R.string.this_week)
                            TimeRange.MONTH -> stringResource(R.string.this_month)
                            TimeRange.YEAR -> stringResource(R.string.this_year)
                            TimeRange.CUSTOM -> stringResource(R.string.custom)
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.onPrimary,
                    selectedLabelColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

/**
 * Health status overview with color-coded indicators
 */
@Composable
private fun HealthStatusOverview(
    healthSummary: HealthSummary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.health_status),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // Overall health status indicator
                HealthStatusIndicator(
                    status = healthSummary.overallStatus,
                    modifier = Modifier.size(36.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Health status categories
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Heart health
                HealthStatusCategory(
                    title = stringResource(R.string.heart),
                    status = healthSummary.heartStatus,
                    icon = Icons.Outlined.Favorite,
                    modifier = Modifier.weight(1f)
                )
                
                // Activity
                HealthStatusCategory(
                    title = stringResource(R.string.activity),
                    status = healthSummary.activityStatus,
                    icon = Icons.Outlined.DirectionsRun,
                    modifier = Modifier.weight(1f)
                )
                
                // Sleep
                HealthStatusCategory(
                    title = stringResource(R.string.sleep),
                    status = healthSummary.sleepStatus,
                    icon = Icons.Outlined.Bedtime,
                    modifier = Modifier.weight(1f)
                )
                
                // Stress
                HealthStatusCategory(
                    title = stringResource(R.string.stress),
                    status = healthSummary.stressStatus,
                    icon = Icons.Outlined.Psychology,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Health status indicator with color-coded circle
 */
@Composable
private fun HealthStatusIndicator(
    status: HealthStatus,
    modifier: Modifier = Modifier
) {
    val statusColor = when (status) {
        HealthStatus.EXCELLENT -> MaterialTheme.colorScheme.tertiary
        HealthStatus.GOOD -> MaterialTheme.colorScheme.primary
        HealthStatus.FAIR -> MaterialTheme.colorScheme.secondary
        HealthStatus.POOR -> MaterialTheme.colorScheme.error
    }
    
    val statusText = when (status) {
        HealthStatus.EXCELLENT -> stringResource(R.string.excellent)
        HealthStatus.GOOD -> stringResource(R.string.good)
        HealthStatus.FAIR -> stringResource(R.string.fair)
        HealthStatus.POOR -> stringResource(R.string.poor)
    }
    
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(statusColor.copy(alpha = 0.2f))
            .border(2.dp, statusColor, CircleShape)
            .semantics {
                contentDescription = stringResource(R.string.health_status_indicator, statusText)
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(statusColor)
        )
    }
}

/**
 * Health status category with icon and status indicator
 */
@Composable
private fun HealthStatusCategory(
    title: String,
    status: HealthStatus,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val statusColor = when (status) {
        HealthStatus.EXCELLENT -> MaterialTheme.colorScheme.tertiary
        HealthStatus.GOOD -> MaterialTheme.colorScheme.primary
        HealthStatus.FAIR -> MaterialTheme.colorScheme.secondary
        HealthStatus.POOR -> MaterialTheme.colorScheme.error
    }
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = statusColor,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(statusColor)
        )
    }
}

/**
 * Real-time health metrics section
 */
@Composable
private fun RealTimeMetricsSection(
    healthMetrics: HealthMetrics,
    onMetricSelected: (MetricType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        // Section header
        SectionHeader(
            title = stringResource(R.string.real_time_metrics),
            subtitle = stringResource(R.string.tap_for_details),
            showViewAll = false
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Metrics grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Heart rate
            MetricCard(
                title = stringResource(R.string.heart_rate),
                value = "${healthMetrics.heartRate.current}",
                unit = stringResource(R.string.bpm),
                icon = Icons.Outlined.Favorite,
                color = MaterialTheme.colorScheme.error,
                isRealTime = true,
                trend = healthMetrics.heartRate.trend,
                onClick = { onMetricSelected(MetricType.HEART_RATE) },
                modifier = Modifier.weight(1f)
            )
            
            // Blood pressure
            MetricCard(
                title = stringResource(R.string.blood_pressure),
                value = "${healthMetrics.bloodPressure.systolic}/${healthMetrics.bloodPressure.diastolic}",
                unit = stringResource(R.string.mmhg),
                icon = Icons.Outlined.BloodType,
                color = MaterialTheme.colorScheme.secondary,
                isRealTime = false,
                lastUpdated = healthMetrics.bloodPressure.timestamp,
                trend = healthMetrics.bloodPressure.trend,
                onClick = { onMetricSelected(MetricType.BLOOD_PRESSURE) },
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Steps
            MetricCard(
                title = stringResource(R.string.steps),
                value = "${healthMetrics.steps.current}",
                unit = "",
                icon = Icons.Outlined.DirectionsWalk,
                color = MaterialTheme.colorScheme.primary,
                isRealTime = true,
                progress = healthMetrics.steps.current.toFloat() / healthMetrics.steps.target.toFloat(),
                onClick = { onMetricSelected(MetricType.STEPS) },
                modifier = Modifier.weight(1f)
            )
            
            // Sleep
            MetricCard(
                title = stringResource(R.string.sleep),
                value = "${healthMetrics.sleep.hoursSlept}",
                unit = stringResource(R.string.hours),
                icon = Icons.Outlined.Bedtime,
                color = MaterialTheme.colorScheme.tertiary,
                isRealTime = false,
                quality = healthMetrics.sleep.quality,
                onClick = { onMetricSelected(MetricType.SLEEP_DURATION) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Metric card for displaying health metrics
 */
@Composable
private fun MetricCard(
    title: String,
    value: String,
    unit: String,
    icon: ImageVector,
    color: Color,
    isRealTime: Boolean,
    modifier: Modifier = Modifier,
    trend: TrendDirection? = null,
    progress: Float? = null,
    quality: SleepQuality? = null,
    lastUpdated: LocalDateTime? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(140.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header with icon and title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // Icon with background
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            // Value with unit
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                // Main value
                if (isRealTime) {
                    // Animated counter for real-time values
                    AnimatedCounter(
                        count = value.toIntOrNull() ?: 0,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                } else {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // Unit
                if (unit.isNotEmpty()) {
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                }
            }
            
            // Footer with trend, progress, quality or last updated
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when {
                    // Show trend indicator
                    trend != null -> {
                        TrendIndicator(
                            trend = trend,
                            color = color
                        )
                    }
                    
                    // Show progress
                    progress != null -> {
                        LinearProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp),
                            color = color,
                            trackColor = color.copy(alpha = 0.2f)
                        )
                    }
                    
                    // Show sleep quality
                    quality != null -> {
                        SleepQualityIndicator(
                            quality = quality,
                            color = color
                        )
                    }
                }
                
                // Real-time indicator or last updated time
                if (isRealTime) {
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // Pulsing real-time indicator
                    PulsingEffect {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                    }
                    
                    Text(
                        text = stringResource(R.string.real_time),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(start = 4.dp)
                    )
                } else if (lastUpdated != null) {
                    Spacer(modifier = Modifier.weight(1f))
                    
                    Text(
                        text = stringResource(
                            R.string.updated_at,
                            lastUpdated.formatTime()
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

/**
 * Trend indicator for showing metric trends
 */
@Composable
private fun TrendIndicator(
    trend: TrendDirection,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Trend icon
        Icon(
            imageVector = when (trend) {
                TrendDirection.UP -> Icons.Default.TrendingUp
                TrendDirection.DOWN -> Icons.Default.TrendingDown
                TrendDirection.STABLE -> Icons.Default.TrendingFlat
            },
            contentDescription = when (trend) {
                TrendDirection.UP -> stringResource(R.string.trending_up)
                TrendDirection.DOWN -> stringResource(R.string.trending_down)
                TrendDirection.STABLE -> stringResource(R.string.trending_stable)
            },
            tint = when (trend) {
                TrendDirection.UP -> MaterialTheme.colorScheme.error
                TrendDirection.DOWN -> MaterialTheme.colorScheme.tertiary
                TrendDirection.STABLE -> MaterialTheme.colorScheme.primary
            },
            modifier = Modifier.size(16.dp)
        )
        
        // Trend text
        Text(
            text = when (trend) {
                TrendDirection.UP -> stringResource(R.string.increasing)
                TrendDirection.DOWN -> stringResource(R.string.decreasing)
                TrendDirection.STABLE -> stringResource(R.string.stable)
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

/**
 * Sleep quality indicator
 */
@Composable
private fun SleepQualityIndicator(
    quality: SleepQuality,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Quality stars
        repeat(5) { index ->
            val filled = index < quality.stars
            Icon(
                imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.Star,
                contentDescription = null,
                tint = if (filled) color else color.copy(alpha = 0.3f),
                modifier = Modifier.size(12.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(4.dp))
        
        // Quality text
        Text(
            text = when (quality) {
                SleepQuality.EXCELLENT -> stringResource(R.string.excellent)
                SleepQuality.GOOD -> stringResource(R.string.good)
                SleepQuality.FAIR -> stringResource(R.string.fair)
                SleepQuality.POOR -> stringResource(R.string.poor)
                SleepQuality.BAD -> stringResource(R.string.bad)
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

/**
 * Health trends section with charts
 */
@Composable
private fun HealthTrendsSection(
    healthSummary: HealthSummary,
    selectedTimeRange: TimeRange,
    onViewHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        // Section header
        SectionHeader(
            title = stringResource(R.string.health_trends),
            subtitle = stringResource(R.string.swipe_for_more),
            showViewAll = true,
            onViewAll = onViewHistory
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Charts
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Chart tabs
                var selectedTab by remember { mutableStateOf(0) }
                val tabs = listOf(
                    stringResource(R.string.heart_rate),
                    stringResource(R.string.activity),
                    stringResource(R.string.sleep),
                    stringResource(R.string.stress)
                )
                
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            height = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Chart content based on selected tab
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when (selectedTab) {
                        0 -> HeartRateChart(
                            data = healthSummary.heartRateData,
                            timeRange = selectedTimeRange
                        )
                        1 -> ActivityChart(
                            data = healthSummary.activityData,
                            timeRange = selectedTimeRange
                        )
                        2 -> SleepChart(
                            data = healthSummary.sleepData,
                            timeRange = selectedTimeRange
                        )
                        3 -> StressChart(
                            data = healthSummary.stressData,
                            timeRange = selectedTimeRange
                        )
                    }
                }
            }
        }
    }
}

/**
 * Heart rate chart
 */
@Composable
private fun HeartRateChart(
    data: List<HeartRateDataPoint>,
    timeRange: TimeRange,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        EmptyChart(message = stringResource(R.string.no_heart_rate_data))
        return
    }
    
    // Chart implementation
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                // Draw chart axes
                drawLine(
                    color = Color.Gray.copy(alpha = 0.3f),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
                
                // Draw y-axis
                drawLine(
                    color = Color.Gray.copy(alpha = 0.3f),
                    start = Offset(0f, 0f),
                    end = Offset(0f, size.height),
                    strokeWidth = 1.dp.toPx()
                )
                
                // Draw grid lines
                val gridLines = 5
                val gridSpacing = size.height / gridLines
                
                repeat(gridLines) { i ->
                    val y = gridSpacing * i
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.1f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 0.5.dp.toPx()
                    )
                }
                
                // Draw heart rate line
                if (data.size > 1) {
                    // Find min and max values for scaling
                    val minValue = data.minOf { it.value }.coerceAtLeast(40f)
                    val maxValue = data.maxOf { it.value }.coerceAtMost(200f)
                    val valueRange = maxValue - minValue
                    
                    // Calculate points
                    val points = data.mapIndexed { index, point ->
                        val x = (index.toFloat() / (data.size - 1)) * size.width
                        val normalizedValue = (point.value - minValue) / valueRange
                        val y = size.height - (normalizedValue * size.height)
                        Offset(x, y)
                    }
                    
                    // Draw line
                    val path = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        points.drop(1).forEach { point ->
                            lineTo(point.x, point.y)
                        }
                    }
                    
                    // Draw gradient under the line
                    val gradientPath = Path().apply {
                        moveTo(points.first().x, size.height)
                        lineTo(points.first().x, points.first().y)
                        points.drop(1).forEach { point ->
                            lineTo(point.x, point.y)
                        }
                        lineTo(points.last().x, size.height)
                        close()
                    }
                    
                    // Draw gradient fill
                    drawPath(
                        path = gradientPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.error.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.error.copy(alpha = 0.0f)
                            )
                        )
                    )
                    
                    // Draw line
                    drawPath(
                        path = path,
                        color = MaterialTheme.colorScheme.error,
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.cornerPathEffect(8.dp.toPx())
                        )
                    )
                    
                    // Draw points
                    points.forEach { point ->
                        drawCircle(
                            color = MaterialTheme.colorScheme.error,
                            radius = 2.dp.toPx(),
                            center = point
                        )
                    }
                }
            }
    ) {
        // Y-axis labels
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(end = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "180",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            
            Text(
                text = "150",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            
            Text(
                text = "120",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            
            Text(
                text = "90",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            
            Text(
                text = "60",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        
        // X-axis labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Show appropriate time labels based on selected time range
            when (timeRange) {
                TimeRange.TODAY -> {
                    listOf("6AM", "12PM", "6PM", "12AM").forEach { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                TimeRange.WEEK -> {
                    listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                TimeRange.MONTH -> {
                    listOf("Week 1", "Week 2", "Week 3", "Week 4").forEach { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                else -> {
                    listOf("Jan", "Apr", "Jul", "Oct", "Dec").forEach { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
        
        // Current value indicator
        if (data.isNotEmpty()) {
            val latestValue = data.last().value.roundToInt()
            
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$latestValue bpm",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

/**
 * Activity chart
 */
@Composable
private fun ActivityChart(
    data: List<ActivityDataPoint>,
    timeRange: TimeRange,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        EmptyChart(message = stringResource(R.string.no_activity_data))
        return
    }
    
    // Chart implementation with bar chart for activity
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                // Draw chart axes
                drawLine(
                    color = Color.Gray.copy(alpha = 0.3f),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
                
                // Draw y-axis
                drawLine(
                    color = Color.Gray.copy(alpha = 0.3f),
                    start = Offset(0f, 0f),
                    end = Offset(0f, size.height),
                    strokeWidth = 1.dp.toPx()
                )
                
                // Draw grid lines
                val gridLines = 5
                val gridSpacing = size.height / gridLines
                
                repeat(gridLines) { i ->
                    val y = gridSpacing * i
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.1f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 0.5.dp.toPx()
                    )
                }
                
                // Draw activity bars
                if (data.isNotEmpty()) {
                    // Find max value for scaling
                    val maxValue = data.maxOf { it.value }.coerceAtLeast(1f)
                    
                    // Calculate bar width and spacing
                    val barCount = data.size
                    val totalSpacing = (barCount - 1) * (size.width * 0.02f)
                    val barWidth = (size.width - totalSpacing) / barCount
                    
                    // Draw bars
                    data.forEachIndexed { index, point ->
                        val normalizedValue = point.value / maxValue
                        val barHeight = normalizedValue * size.height * 0.9f
                        
                        val x = index * (barWidth + (size.width * 0.02f))
                        val y = size.height - barHeight
                        
                        // Draw bar
                        drawRect(
                            color = MaterialTheme.colorScheme.primary,
                            topLeft = Offset(x, y),
                            size = androidx.compose.ui.geometry.Size(
                                width = barWidth,
                                height = barHeight
                            ),
                            alpha = 0.8f
                        )
                    }
                }
            }
    ) {
        // Y-axis labels
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(end = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "10k",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            
            Text(
                text = "7.5k",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            
            Text(
                text = "5k",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            
            Text(
                text = "2.5k",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            
            Text(
                text = "0",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        
        // X-axis labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Show appropriate time labels based on selected time range
            when (timeRange) {
                TimeRange.TODAY -> {
                    listOf("6AM", "12PM", "6PM", "12AM").forEach { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                TimeRange.WEEK -> {
                    listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                TimeRange.MONTH -> {
                    listOf("Week 1", "Week 2", "Week 3", "Week 4").forEach { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                else -> {
                    listOf("Jan", "Apr", "Jul", "Oct", "Dec").forEach { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
        
        // Current value indicator
        if (data.isNotEmpty()) {
            val latestValue = data.last().value.roundToInt()
            
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DirectionsWalk,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$latestValue steps",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

/**
 * Sleep chart
 */
@Composable
private fun SleepChart(
    data: List<SleepDataPoint>,
    timeRange: TimeRange,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        EmptyChart(message = stringResource(R.string.no_sleep_data))
        return
    }
    
    // Chart implementation
    // Sleep chart implementation would go here
    // For now, showing a placeholder
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.sleep_chart_coming_soon),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

/**
 * Stress chart
 */
@Composable
private fun StressChart(
    data: List<StressDataPoint>,
    timeRange: TimeRange,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        EmptyChart(message = stringResource(R.string.no_stress_data))
        return
    }
    
    // Chart implementation
    // Stress chart implementation would go here
    // For now, showing a placeholder
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.stress_chart_coming_soon),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

/**
 * Empty chart placeholder
 */
@Composable
private fun EmptyChart(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.DataUsage,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Health insights section
 */
@Composable
private fun HealthInsightsSection(
    insights: List<HealthInsight>,
    onInsightSelected: (Long) -> Unit,
    onViewAllInsights: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        // Section header
        SectionHeader(
            title = stringResource(R.string.health_insights),
            subtitle = stringResource(R.string.ai_powered_analysis),
            showViewAll = true,
            onViewAll = onViewAllInsights
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Insights list
        if (insights.isEmpty()) {
            // Empty state
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 2.dp
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_insights_available),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Insights cards
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(insights) { insight ->
                    InsightCard(
                        insight = insight,
                        onClick = { onInsightSelected(insight.id) },
                        modifier = Modifier
                            .width(280.dp)
                            .height(160.dp)
                    )
                }
            }
        }
    }
}

/**
 * Insight card
 */
@Composable
private fun InsightCard(
    insight: HealthInsight,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = when (insight.category) {
                InsightCategory.ACTIVITY -> MaterialTheme.colorScheme.primaryContainer
                InsightCategory.HEART -> MaterialTheme.colorScheme.errorContainer
                InsightCategory.SLEEP -> MaterialTheme.colorScheme.tertiaryContainer
                InsightCategory.NUTRITION -> MaterialTheme.colorScheme.secondaryContainer
                InsightCategory.STRESS -> MaterialTheme.colorScheme.surfaceVariant
                InsightCategory.GENERAL -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header with category and icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category chip
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = when (insight.category) {
                        InsightCategory.ACTIVITY -> MaterialTheme.colorScheme.primary
                        InsightCategory.HEART -> MaterialTheme.colorScheme.error
                        InsightCategory.SLEEP -> MaterialTheme.colorScheme.tertiary
                        InsightCategory.NUTRITION -> MaterialTheme.colorScheme.secondary
                        InsightCategory.STRESS -> MaterialTheme.colorScheme.outline
                        InsightCategory.GENERAL -> MaterialTheme.colorScheme.primary
                    }.copy(alpha = 0.8f),
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Text(
                        text = when (insight.category) {
                            InsightCategory.ACTIVITY -> stringResource(R.string.activity)
                            InsightCategory.HEART -> stringResource(R.string.heart)
                            InsightCategory.SLEEP -> stringResource(R.string.sleep)
                            InsightCategory.NUTRITION -> stringResource(R.string.nutrition)
                            InsightCategory.STRESS -> stringResource(R.string.stress)
                            InsightCategory.GENERAL -> stringResource(R.string.general)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                
                // AI icon
                Icon(
                    imageVector = Icons.Outlined.Psychology,
                    contentDescription = null,
                    tint = when (insight.category) {
                        InsightCategory.ACTIVITY -> MaterialTheme.colorScheme.primary
                        InsightCategory.HEART -> MaterialTheme.colorScheme.error
                        InsightCategory.SLEEP -> MaterialTheme.colorScheme.tertiary
                        InsightCategory.NUTRITION -> MaterialTheme.colorScheme.secondary
                        InsightCategory.STRESS -> MaterialTheme.colorScheme.outline
                        InsightCategory.GENERAL -> MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Insight title
            Text(
                text = insight.title,
                style = MaterialTheme.typography.titleMedium,
                color = when (insight.category) {
                    InsightCategory.ACTIVITY -> MaterialTheme.colorScheme.onPrimaryContainer
                    InsightCategory.HEART -> MaterialTheme.colorScheme.onErrorContainer
                    InsightCategory.SLEEP -> MaterialTheme.colorScheme.onTertiaryContainer
                    InsightCategory.NUTRITION -> MaterialTheme.colorScheme.onSecondaryContainer
                    InsightCategory.STRESS -> MaterialTheme.colorScheme.onSurfaceVariant
                    InsightCategory.GENERAL -> MaterialTheme.colorScheme.onSurface
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Insight summary
            Text(
                text = insight.summary,
                style = MaterialTheme.typography.bodySmall,
                color = when (insight.category) {
                    InsightCategory.ACTIVITY -> MaterialTheme.colorScheme.onPrimaryContainer
                    InsightCategory.HEART -> MaterialTheme.colorScheme.onErrorContainer
                    InsightCategory.SLEEP -> MaterialTheme.colorScheme.onTertiaryContainer
                    InsightCategory.NUTRITION -> MaterialTheme.colorScheme.onSecondaryContainer
                    InsightCategory.STRESS -> MaterialTheme.colorScheme.onSurfaceVariant
                    InsightCategory.GENERAL -> MaterialTheme.colorScheme.onSurface
                }.copy(alpha = 0.8f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Footer with date and confidence
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Date
                Text(
                    text = insight.timestamp.formatDate(),
                    style = MaterialTheme.typography.labelSmall,
                    color = when (insight.category) {
                        InsightCategory.ACTIVITY -> MaterialTheme.colorScheme.onPrimaryContainer
                        InsightCategory.HEART -> MaterialTheme.colorScheme.onErrorContainer
                        InsightCategory.SLEEP -> MaterialTheme.colorScheme.onTertiaryContainer
                        InsightCategory.NUTRITION -> MaterialTheme.colorScheme.onSecondaryContainer
                        InsightCategory.STRESS -> MaterialTheme.colorScheme.onSurfaceVariant
                        InsightCategory.GENERAL -> MaterialTheme.colorScheme.onSurface
                    }.copy(alpha = 0.6f)
                )
                
                // Confidence indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.confidence),
                        style = MaterialTheme.typography.labelSmall,
                        color = when (insight.category) {
                            InsightCategory.ACTIVITY -> MaterialTheme.colorScheme.onPrimaryContainer
                            InsightCategory.HEART -> MaterialTheme.colorScheme.onErrorContainer
                            InsightCategory.SLEEP -> MaterialTheme.colorScheme.onTertiaryContainer
                            InsightCategory.NUTRITION -> MaterialTheme.colorScheme.onSecondaryContainer
                            InsightCategory.STRESS -> MaterialTheme.colorScheme.onSurfaceVariant
                            InsightCategory.GENERAL -> MaterialTheme.colorScheme.onSurface
                        }.copy(alpha = 0.6f)
                    )
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    // Confidence level indicator
                    Row {
                        repeat(3) { index ->
                            val filled = index < insight.confidenceLevel
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .padding(horizontal = 1.dp)
                                    .background(
                                        color = when (insight.category) {
                                            InsightCategory.ACTIVITY -> MaterialTheme.colorScheme.primary
                                            InsightCategory.HEART -> MaterialTheme.colorScheme.error
                                            InsightCategory.SLEEP -> MaterialTheme.colorScheme.tertiary
                                            InsightCategory.NUTRITION -> MaterialTheme.colorScheme.secondary
                                            InsightCategory.STRESS -> MaterialTheme.colorScheme.outline
                                            InsightCategory.GENERAL -> MaterialTheme.colorScheme.primary
                                        }.copy(alpha = if (filled) 0.8f else 0.2f),
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Recent alerts section
 */
@Composable
private fun RecentAlertsSection(
    alerts: List<HealthAlert>,
    onAlertSelected: (Long) -> Unit,
    onViewAllAlerts: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        // Section header
        SectionHeader(
            title = stringResource(R.string.recent_alerts),
            subtitle = stringResource(R.string.tap_to_view_details),
            showViewAll = true,
            onViewAll = onViewAllAlerts
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Alerts list
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            alerts.take(3).forEach { alert ->
                AlertItem(
                    alert = alert,
                    onClick = { onAlertSelected(alert.id) }
                )
            }
        }
    }
}

/**
 * Alert item
 */
@Composable
private fun AlertItem(
    alert: HealthAlert,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val alertColor = when (alert.severity) {
        AlertSeverity.LOW -> MaterialTheme.colorScheme.primary
        AlertSeverity.MEDIUM -> MaterialTheme.colorScheme.secondary
        AlertSeverity.HIGH -> MaterialTheme.colorScheme.error
        AlertSeverity.EMERGENCY -> MaterialTheme.colorScheme.error
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Alert severity indicator
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(alertColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (alert.severity) {
                        AlertSeverity.LOW -> Icons.Outlined.Info
                        AlertSeverity.MEDIUM -> Icons.Outlined.Warning
                        AlertSeverity.HIGH -> Icons.Filled.Warning
                        AlertSeverity.EMERGENCY -> Icons.Filled.Error
                    },
                    contentDescription = null,
                    tint = alertColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Alert content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = alert.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = alert.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Alert timestamp
            Text(
                text = alert.createdAt.formatTime(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * Connected devices section
 */
@Composable
private fun ConnectedDevicesSection(
    devices: List<HBandDevice>,
    onDeviceSelected: (String) -> Unit,
    onSyncDevices: () -> Unit,
    onManageDevices: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        // Section header
        SectionHeader(
            title = stringResource(R.string.connected_devices),
            subtitle = stringResource(R.string.tap_to_manage),
            showViewAll = true,
            onViewAll = onManageDevices
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Devices list
        if (devices.isEmpty()) {
            // Empty state
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 2.dp
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeviceUnknown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.size(24.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(
                            text = stringResource(R.string.no_devices_connected),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        } else {
            // Connected devices
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Device cards
                devices.take(2).forEach { device ->
                    DeviceCard(
                        device = device,
                        onClick = { onDeviceSelected(device.deviceId) },
                        modifier = Modifier.weight(1f)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                }
                
                // If there are more than 2 devices, show a count
                if (devices.size > 2) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(80.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable(onClick = onManageDevices),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.plus_more_devices, devices.size - 2),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Sync button
            Button(
                onClick = onSyncDevices,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(text = stringResource(R.string.sync_all_devices))
            }
        }
    }
}

/**
 * Device card
 */
@Composable
private fun DeviceCard(
    device: HBandDevice,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(80.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Device icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (device.isConnected) 
                            MaterialTheme.colorScheme.primary