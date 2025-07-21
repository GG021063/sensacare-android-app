package com.sensacare.app.presentation.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.sensacare.app.domain.model.*
import com.sensacare.app.presentation.theme.*
import com.sensacare.app.presentation.ui.components.*
import com.sensacare.app.presentation.viewmodel.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Dashboard Screen
 *
 * Main dashboard screen displaying health data summary, goals, and alerts.
 * 
 * @param navigateToHeartRateDetail Navigate to heart rate detail screen
 * @param navigateToBloodPressureDetail Navigate to blood pressure detail screen
 * @param navigateToSleepDetail Navigate to sleep detail screen
 * @param navigateToActivityDetail Navigate to activity detail screen
 * @param navigateToGoalDetail Navigate to goal detail screen
 * @param navigateToAlertDetail Navigate to alert detail screen
 * @param navigateToSettings Navigate to settings screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navigateToHeartRateDetail: () -> Unit,
    navigateToBloodPressureDetail: () -> Unit,
    navigateToSleepDetail: () -> Unit,
    navigateToActivityDetail: () -> Unit,
    navigateToGoalDetail: (String) -> Unit,
    navigateToAlertDetail: (String) -> Unit,
    navigateToSettings: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Collect events
    LaunchedEffect(key1 = viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is DashboardEvent.NavigateToGoalDetail -> navigateToGoalDetail(event.goalId)
                is DashboardEvent.NavigateToAlertDetail -> navigateToAlertDetail(event.alertId)
                is DashboardEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                is DashboardEvent.ShowError -> snackbarHostState.showSnackbar(
                    message = event.message,
                    actionLabel = "Retry",
                    duration = SnackbarDuration.Long
                ).let { result ->
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.processIntent(DashboardIntent.Refresh)
                    }
                }
            }
        }
    }
    
    // Swipe refresh state
    val isRefreshing = uiState is DashboardUiState.Loading
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = isRefreshing)
    
    Scaffold(
        topBar = {
            DashboardTopAppBar(
                onSettingsClick = navigateToSettings,
                onRefreshClick = { viewModel.processIntent(DashboardIntent.Refresh) }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = { viewModel.processIntent(DashboardIntent.Refresh) },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val currentState = uiState) {
                is DashboardUiState.Loading -> {
                    DashboardLoadingContent()
                }
                is DashboardUiState.Success -> {
                    DashboardSuccessContent(
                        healthSummary = currentState.healthSummary,
                        timeRange = currentState.timeRange,
                        onTimeRangeSelected = { rangeType ->
                            viewModel.processIntent(DashboardIntent.SelectPredefinedTimeRange(rangeType))
                        },
                        onCustomTimeRangeSelected = { timeRange ->
                            viewModel.processIntent(DashboardIntent.ChangeTimeRange(timeRange))
                        },
                        onMetricTypeSelected = { metricType ->
                            viewModel.processIntent(DashboardIntent.FilterByMetricType(metricType))
                        },
                        onActivityTypeSelected = { activityType ->
                            viewModel.processIntent(DashboardIntent.FilterByActivityType(activityType))
                        },
                        onHeartRateCardClick = navigateToHeartRateDetail,
                        onBloodPressureCardClick = navigateToBloodPressureDetail,
                        onSleepCardClick = navigateToSleepDetail,
                        onActivityCardClick = navigateToActivityDetail,
                        onGoalClick = { goalId ->
                            viewModel.processIntent(DashboardIntent.SelectHealthGoal(goalId))
                        },
                        onAlertClick = { alertId ->
                            viewModel.processIntent(DashboardIntent.SelectHealthAlert(alertId))
                        }
                    )
                }
                is DashboardUiState.Error -> {
                    DashboardErrorContent(
                        message = currentState.message,
                        onRetry = { viewModel.processIntent(DashboardIntent.Refresh) }
                    )
                }
            }
        }
    }
}

/**
 * Dashboard Top App Bar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardTopAppBar(
    onSettingsClick: () -> Unit,
    onRefreshClick: () -> Unit
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SensaCare",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        },
        actions = {
            IconButton(onClick = onRefreshClick) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh"
                )
            }
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings"
                )
            }
        }
    )
}

/**
 * Dashboard Loading Content
 */
@Composable
private fun DashboardLoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading your health data...",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

/**
 * Dashboard Error Content
 */
@Composable
private fun DashboardErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Oops! Something went wrong",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Try Again")
            }
        }
    }
}

/**
 * Dashboard Success Content
 */
@Composable
private fun DashboardSuccessContent(
    healthSummary: HealthSummary,
    timeRange: TimeRange,
    onTimeRangeSelected: (TimeRangeType) -> Unit,
    onCustomTimeRangeSelected: (TimeRange) -> Unit,
    onMetricTypeSelected: (MetricType) -> Unit,
    onActivityTypeSelected: (ActivityType?) -> Unit,
    onHeartRateCardClick: () -> Unit,
    onBloodPressureCardClick: () -> Unit,
    onSleepCardClick: () -> Unit,
    onActivityCardClick: () -> Unit,
    onGoalClick: (String) -> Unit,
    onAlertClick: (String) -> Unit
) {
    val scrollState = rememberLazyListState()
    
    LazyColumn(
        state = scrollState,
        contentPadding = PaddingValues(bottom = 16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Time Range Selector
        item {
            TimeRangeSelector(
                timeRange = timeRange,
                onTimeRangeSelected = onTimeRangeSelected,
                onCustomTimeRangeSelected = onCustomTimeRangeSelected
            )
        }
        
        // Active Alerts (if any)
        if (healthSummary.activeAlerts.isNotEmpty()) {
            item {
                ActiveAlertsSection(
                    alerts = healthSummary.activeAlerts,
                    onAlertClick = onAlertClick
                )
            }
        }
        
        // Health Metrics Summary
        item {
            HealthMetricsSummary(
                heartRateData = healthSummary.heartRateData,
                bloodPressureData = healthSummary.bloodPressureData,
                sleepData = healthSummary.sleepData,
                activityData = healthSummary.activityData,
                onHeartRateCardClick = onHeartRateCardClick,
                onBloodPressureCardClick = onBloodPressureCardClick,
                onSleepCardClick = onSleepCardClick,
                onActivityCardClick = onActivityCardClick
            )
        }
        
        // Health Goals
        if (healthSummary.healthGoals.isNotEmpty()) {
            item {
                HealthGoalsSection(
                    goals = healthSummary.healthGoals,
                    onGoalClick = onGoalClick
                )
            }
        }
    }
}

/**
 * Time Range Selector
 */
@Composable
private fun TimeRangeSelector(
    timeRange: TimeRange,
    onTimeRangeSelected: (TimeRangeType) -> Unit,
    onCustomTimeRangeSelected: (TimeRange) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Time Range",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Date range display
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDatePicker = true }
                .padding(vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = formatDateRange(timeRange.startDate, timeRange.endDate),
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Select Date Range"
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Time range chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(TimeRangeType.values().filter { it != TimeRangeType.CUSTOM }) { rangeType ->
                TimeRangeChip(
                    rangeType = rangeType,
                    isSelected = timeRange.rangeType == rangeType,
                    onClick = { onTimeRangeSelected(rangeType) }
                )
            }
        }
    }
    
    // Date picker dialog
    if (showDatePicker) {
        DateRangePickerDialog(
            initialStartDate = timeRange.startDate,
            initialEndDate = timeRange.endDate,
            onDismiss = { showDatePicker = false },
            onDateRangeSelected = { startDate, endDate ->
                onCustomTimeRangeSelected(
                    TimeRange(
                        startDate = startDate,
                        endDate = endDate,
                        rangeType = TimeRangeType.CUSTOM
                    )
                )
                showDatePicker = false
            }
        )
    }
}

/**
 * Time Range Chip
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeRangeChip(
    rangeType: TimeRangeType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val label = when (rangeType) {
        TimeRangeType.DAY -> "Today"
        TimeRangeType.WEEK -> "Week"
        TimeRangeType.MONTH -> "Month"
        TimeRangeType.THREE_MONTHS -> "3 Months"
        TimeRangeType.SIX_MONTHS -> "6 Months"
        TimeRangeType.YEAR -> "Year"
        TimeRangeType.CUSTOM -> "Custom"
    }
    
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

/**
 * Health Metrics Summary
 */
@Composable
private fun HealthMetricsSummary(
    heartRateData: HeartRateData?,
    bloodPressureData: BloodPressureData?,
    sleepData: SleepData?,
    activityData: ActivityData?,
    onHeartRateCardClick: () -> Unit,
    onBloodPressureCardClick: () -> Unit,
    onSleepCardClick: () -> Unit,
    onActivityCardClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Health Metrics",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Heart Rate Card
        if (heartRateData != null) {
            HeartRateCard(
                heartRateData = heartRateData,
                onClick = onHeartRateCardClick
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Blood Pressure Card
        if (bloodPressureData != null) {
            BloodPressureCard(
                bloodPressureData = bloodPressureData,
                onClick = onBloodPressureCardClick
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Sleep Card
        if (sleepData != null) {
            SleepCard(
                sleepData = sleepData,
                onClick = onSleepCardClick
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Activity Card
        if (activityData != null) {
            ActivityCard(
                activityData = activityData,
                onClick = onActivityCardClick
            )
        }
    }
}

/**
 * Heart Rate Card
 */
@Composable
private fun HeartRateCard(
    heartRateData: HeartRateData,
    onClick: () -> Unit
) {
    val latestHeartRate = heartRateData.latestHeartRate
    val heartRateStats = heartRateData.heartRateStats
    val abnormalDetection = heartRateData.abnormalDetection
    
    DashboardCard(
        title = "Heart Rate",
        icon = Icons.Default.Favorite,
        iconTint = Color(0xFFE57373),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (latestHeartRate != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${latestHeartRate.value}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = " bpm",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    Text(
                        text = formatTimeAgo(latestHeartRate.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Resting heart rate
                if (heartRateStats?.avgRestingHeartRate != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Bedtime,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Resting: ${heartRateStats.avgRestingHeartRate} bpm",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                // Abnormal detection warning
                if (abnormalDetection?.abnormalitiesDetected == true) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Abnormal patterns detected",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Heart rate stats
                if (heartRateStats != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        StatItem(
                            label = "Min",
                            value = "${heartRateStats.minHeartRate}",
                            unit = "bpm",
                            modifier = Modifier.weight(1f)
                        )
                        StatItem(
                            label = "Avg",
                            value = "${heartRateStats.avgHeartRate}",
                            unit = "bpm",
                            modifier = Modifier.weight(1f)
                        )
                        StatItem(
                            label = "Max",
                            value = "${heartRateStats.maxHeartRate}",
                            unit = "bpm",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else {
                Text(
                    text = "No heart rate data available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Blood Pressure Card
 */
@Composable
private fun BloodPressureCard(
    bloodPressureData: BloodPressureData,
    onClick: () -> Unit
) {
    val latestBloodPressure = bloodPressureData.latestBloodPressure
    val bloodPressureStats = bloodPressureData.bloodPressureStats
    
    DashboardCard(
        title = "Blood Pressure",
        icon = Icons.Default.Speed,
        iconTint = Color(0xFF64B5F6),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (latestBloodPressure != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${latestBloodPressure.systolic}/${latestBloodPressure.diastolic}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = " mmHg",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    Text(
                        text = formatTimeAgo(latestBloodPressure.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Classification
                val classification = latestBloodPressure.classification
                if (classification != null) {
                    val (color, label) = when (classification) {
                        BloodPressureClassification.HYPOTENSION -> 
                            Pair(Color(0xFF90CAF9), "Low Blood Pressure")
                        BloodPressureClassification.NORMAL -> 
                            Pair(Color(0xFF81C784), "Normal")
                        BloodPressureClassification.ELEVATED -> 
                            Pair(Color(0xFFFFD54F), "Elevated")
                        BloodPressureClassification.HYPERTENSION_STAGE_1 -> 
                            Pair(Color(0xFFFFB74D), "Hypertension Stage 1")
                        BloodPressureClassification.HYPERTENSION_STAGE_2 -> 
                            Pair(Color(0xFFEF5350), "Hypertension Stage 2")
                        BloodPressureClassification.HYPERTENSIVE_CRISIS -> 
                            Pair(Color(0xFFD32F2F), "Hypertensive Crisis")
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(color = color, shape = CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                if (latestBloodPressure.pulse != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Timeline,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Pulse: ${latestBloodPressure.pulse} bpm",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Blood pressure stats
                if (bloodPressureStats != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        StatItem(
                            label = "Avg Systolic",
                            value = "${bloodPressureStats.avgSystolic}",
                            unit = "mmHg",
                            modifier = Modifier.weight(1f)
                        )
                        StatItem(
                            label = "Avg Diastolic",
                            value = "${bloodPressureStats.avgDiastolic}",
                            unit = "mmHg",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else {
                Text(
                    text = "No blood pressure data available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Sleep Card
 */
@Composable
private fun SleepCard(
    sleepData: SleepData,
    onClick: () -> Unit
) {
    val latestSleep = sleepData.latestSleep
    val sleepStats = sleepData.sleepStats
    
    DashboardCard(
        title = "Sleep",
        icon = Icons.Default.Bedtime,
        iconTint = Color(0xFF9575CD),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (latestSleep != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val hours = latestSleep.durationMinutes / 60
                    val minutes = latestSleep.durationMinutes % 60
                    
                    Text(
                        text = "$hours",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "h $minutes",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "m",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    Text(
                        text = formatDate(latestSleep.startTime.toLocalDate()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Sleep time
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTime(latestSleep.startTime) + " - " + formatTime(latestSleep.endTime),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                // Sleep score
                if (latestSleep.sleepScore != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val scoreColor = when {
                            latestSleep.sleepScore >= 80 -> Color(0xFF81C784)
                            latestSleep.sleepScore >= 60 -> Color(0xFFFFD54F)
                            else -> Color(0xFFEF5350)
                        }
                        
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = scoreColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Sleep Score: ${latestSleep.sleepScore}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Sleep stages
                if (latestSleep.stages.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val deepSleepMinutes = latestSleep.deepSleepMinutes ?: 0
                        val remSleepMinutes = latestSleep.remSleepMinutes ?: 0
                        val lightSleepMinutes = latestSleep.lightSleepMinutes ?: 0
                        val awakeSleepMinutes = latestSleep.awakeMinutes ?: 0
                        
                        SleepStageItem(
                            stage = "Deep",
                            minutes = deepSleepMinutes,
                            color = Color(0xFF5E35B1),
                            modifier = Modifier.weight(1f)
                        )
                        SleepStageItem(
                            stage = "REM",
                            minutes = remSleepMinutes,
                            color = Color(0xFF7986CB),
                            modifier = Modifier.weight(1f)
                        )
                        SleepStageItem(
                            stage = "Light",
                            minutes = lightSleepMinutes,
                            color = Color(0xFF64B5F6),
                            modifier = Modifier.weight(1f)
                        )
                        SleepStageItem(
                            stage = "Awake",
                            minutes = awakeSleepMinutes,
                            color = Color(0xFFFFB74D),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else {
                Text(
                    text = "No sleep data available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Activity Card
 */
@Composable
private fun ActivityCard(
    activityData: ActivityData,
    onClick: () -> Unit
) {
    val activityStats = activityData.activityStats
    
    DashboardCard(
        title = "Activity",
        icon = Icons.Default.DirectionsRun,
        iconTint = Color(0xFF4CAF50),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (activityStats != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${activityStats.totalSteps}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = " steps",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // Active minutes badge
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${activityStats.totalActiveMinutes} active min",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Distance
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Straighten,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    // Convert meters to km with 2 decimal places
                    val distanceKm = (activityStats.totalDistance / 1000.0)
                    val formattedDistance = String.format("%.2f", distanceKm)
                    
                    Text(
                        text = "Distance: $formattedDistance km",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                // Calories
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocalFireDepartment,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    // Format calories to whole number
                    val calories = activityStats.totalCalories.toInt()
                    
                    Text(
                        text = "Calories: $calories kcal",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Activity intensity breakdown
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IntensityItem(
                        label = "High",
                        minutes = activityStats.highIntensityMinutes,
                        color = Color(0xFFEF5350),
                        modifier = Modifier.weight(1f)
                    )
                    IntensityItem(
                        label = "Moderate",
                        minutes = activityStats.moderateIntensityMinutes,
                        color = Color(0xFFFFB74D),
                        modifier = Modifier.weight(1f)
                    )
                    IntensityItem(
                        label = "Low",
                        minutes = activityStats.lowIntensityMinutes,
                        color = Color(0xFF81C784),
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // WHO guidelines badge
                if (activityStats.meetsWhoGuidelines) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Meets WHO activity guidelines",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            } else {
                Text(
                    text = "No activity data available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Health Goals Section
 */
@Composable
private fun HealthGoalsSection(
    goals: List<HealthGoal>,
    onGoalClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Health Goals",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        goals.forEach { goal ->
            HealthGoalItem(
                goal = goal,
                onClick = { onGoalClick(goal.id) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

/**
 * Health Goal Item
 */
@Composable
private fun HealthGoalItem(
    goal: HealthGoal,
    onClick: () -> Unit
) {
    val progressPercentage = calculateGoalProgress(goal)
    val progressAnimated by animateFloatAsState(targetValue = progressPercentage / 100f)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
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
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Goal icon based on metric type
                val (icon, iconTint) = getMetricTypeIconAndColor(goal.metricType)
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = goal.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    if (goal.deadline != null) {
                        Text(
                            text = "Due ${formatDate(goal.deadline.toLocalDate())}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Progress percentage
                Text(
                    text = "${progressPercentage.toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Progress description
            Text(
                text = getGoalProgressDescription(goal),
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Progress bar
            LinearProgressIndicator(
                progress = { progressAnimated },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = when {
                    progressPercentage >= 75 -> Color(0xFF4CAF50)
                    progressPercentage >= 50 -> Color(0xFFFFA726)
                    else -> Color(0xFFEF5350)
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

/**
 * Active Alerts Section
 */
@Composable
private fun ActiveAlertsSection(
    alerts: List<HealthAlert>,
    onAlertClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Active Alerts",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        alerts.forEach { alert ->
            HealthAlertItem(
                alert = alert,
                onClick = { onAlertClick(alert.id) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/**
 * Health Alert Item
 */
@Composable
private fun HealthAlertItem(
    alert: HealthAlert,
    onClick: () -> Unit
) {
    val backgroundColor = when (alert.severity) {
        "CRITICAL" -> MaterialTheme.colorScheme.errorContainer
        "HIGH" -> Color(0xFFFFECB3)
        "MEDIUM" -> Color(0xFFE1F5FE)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    
    val contentColor = when (alert.severity) {
        "CRITICAL" -> MaterialTheme.colorScheme.onErrorContainer
        "HIGH" -> Color(0xFF795548)
        "MEDIUM" -> Color(0xFF0288D1)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Alert icon
            Icon(
                imageVector = when (alert.severity) {
                    "CRITICAL" -> Icons.Default.Warning
                    "HIGH" -> Icons.Default.Report
                    else -> Icons.Default.Info
                },
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = alert.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = alert.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "View Alert",
                tint = contentColor
            )
        }
    }
}

/**
 * Dashboard Card
 */
@Composable
private fun DashboardCard(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Card header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "View Details",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Card content
            content()
        }
    }
}

/**
 * Stat Item
 */
@Composable
private fun StatItem(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = unit,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Sleep Stage Item
 */
@Composable
private fun SleepStageItem(
    stage: String,
    minutes: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color = color, shape = CircleShape)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = stage,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = "${minutes}m",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Intensity Item
 */
@Composable
private fun IntensityItem(
    label: String,
    minutes: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color = color, shape = CircleShape)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = "${minutes}m",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Helper functions
 */

/**
 * Format date range
 */
private fun formatDateRange(startDate: LocalDate, endDate: LocalDate): String {
    val startFormatter = DateTimeFormatter.ofPattern("MMM d")
    val endFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
    
    return "${startDate.format(startFormatter)} - ${endDate.format(endFormatter)}"
}

/**
 * Format date
 */
private fun formatDate(date: LocalDate): String {
    return date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
}

/**
 * Format time
 */
private fun formatTime(dateTime: LocalDateTime): String {
    return dateTime.format(DateTimeFormatter.ofPattern("h:mm a"))
}

/**
 * Format time ago
 */
private fun formatTimeAgo(dateTime: LocalDateTime): String {
    val now = LocalDateTime.now()
    val minutes = ChronoUnit.MINUTES.between(dateTime, now)
    val hours = ChronoUnit.HOURS.between(dateTime, now)
    val days = ChronoUnit.DAYS.between(dateTime, now)
    
    return when {
        minutes < 60 -> "$minutes min ago"
        hours < 24 -> "$hours hr ago"
        days < 30 -> "$days days ago"
        else -> formatDate(dateTime.toLocalDate())
    }
}

/**
 * Calculate goal progress
 */
private fun calculateGoalProgress(goal: HealthGoal): Float {
    val currentValue = goal.currentValue ?: goal.startValue
    
    return if (goal.isIncremental) {
        // For incremental goals (e.g., increase steps)
        val progress = (currentValue - goal.startValue) / (goal.targetValue - goal.startValue) * 100
        progress.coerceIn(0f, 100f)
    } else {
        // For decremental goals (e.g., reduce weight)
        val progress = (goal.startValue - currentValue) / (goal.startValue - goal.targetValue) * 100
        progress.coerceIn(0f, 100f)
    }
}

/**
 * Get goal progress description
 */
private fun getGoalProgressDescription(goal: HealthGoal): String {
    val currentValue = goal.currentValue ?: goal.startValue
    
    return if (goal.isIncremental) {
        "${currentValue.toInt()} / ${goal.targetValue.toInt()} ${getUnitForMetricType(goal.metricType)}"
    } else {
        "${currentValue.toInt()} → ${goal.targetValue.toInt()} ${getUnitForMetricType(goal.metricType)}"
    }
}

/**
 * Get unit for metric type
 */
private fun getUnitForMetricType(metricType: MetricType): String {
    return when (metricType) {
        MetricType.HEART_RATE -> "bpm"
        MetricType.BLOOD_PRESSURE -> "mmHg"
        MetricType.BLOOD_OXYGEN -> "%"
        MetricType.STEPS -> "steps"
        MetricType.DISTANCE -> "km"
        MetricType.CALORIES -> "kcal"
        MetricType.SLEEP -> "hours"
        MetricType.WEIGHT -> "kg"
        MetricType.ACTIVITY -> "min"
        MetricType.TEMPERATURE -> "°C"
        MetricType.WATER -> "ml"
        MetricType.OTHER -> ""
    }
}

/**
 * Get metric type icon and color
 */
private fun getMetricTypeIconAndColor(metricType: MetricType): Pair<ImageVector, Color> {
    return when (metricType) {
        MetricType.HEART_RATE -> Pair(Icons.Default.Favorite, Color(0xFFE57373))
        MetricType.BLOOD_PRESSURE -> Pair(Icons.Default.Speed, Color(0xFF64B5F6))
        MetricType.BLOOD_OXYGEN -> Pair(Icons.Default.Air, Color(0xFF4FC3F7))
        MetricType.STEPS -> Pair(Icons.Default.DirectionsWalk, Color(0xFF4CAF50))
        MetricType.DISTANCE -> Pair(Icons.Default.Straighten, Color(0xFF66BB6A))
        MetricType.CALORIES -> Pair(Icons.Default.LocalFireDepartment, Color(0xFFFF7043))
        MetricType.SLEEP -> Pair(Icons.Default.Bedtime, Color(0xFF9575CD))
        MetricType.WEIGHT -> Pair(Icons.Default.FitnessCenter, Color(0xFF78909C))
        MetricType.ACTIVITY -> Pair(Icons.Default.DirectionsRun, Color(0xFF4CAF50))
        MetricType.TEMPERATURE -> Pair(Icons.Default.Thermostat, Color(0xFFFF7043))
        MetricType.WATER -> Pair(Icons.Default.WaterDrop, Color(0xFF29B6F6))
        MetricType.OTHER -> Pair(Icons.Default.HealthAndSafety, Color(0xFF9E9E9E))
    }
}

/**
 * Date Range Picker Dialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangePickerDialog(
    initialStartDate: LocalDate,
    initialEndDate: LocalDate,
    onDismiss: () -> Unit,
    onDateRangeSelected: (LocalDate, LocalDate) -> Unit
) {
    var startDate by remember { mutableStateOf(initialStartDate) }
    var endDate by remember { mutableStateOf(initialEndDate) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Date Range") },
        text = {
            Column {
                // Start date picker
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "Start Date:",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.width(80.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            // In a real app, show a DatePicker here
                            // For this example, we'll just simulate date selection
                            startDate = startDate.minusDays(1)
                        }
                    ) {
                        Text(formatDate(startDate))
                    }
                }
                
                // End date picker
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "End Date:",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.width(80.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            // In a real app, show a DatePicker here
                            // For this example, we'll just simulate date selection
                            endDate = endDate.plusDays(1)
                        }
                    ) {
                        Text(formatDate(endDate))
                    }
                }
                
                // Error message if start date is after end date
                if (startDate.isAfter(endDate)) {
                    Text(
                        text = "Start date cannot be after end date",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!startDate.isAfter(endDate)) {
                        onDateRangeSelected(startDate, endDate)
                    }
                },
                enabled = !startDate.isAfter(endDate)
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
