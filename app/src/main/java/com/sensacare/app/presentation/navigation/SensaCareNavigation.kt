package com.sensacare.app.presentation.navigation

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.*
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.*
import com.google.accompanist.navigation.material.BottomSheetNavigator
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.ModalBottomSheetLayout
import com.google.accompanist.navigation.material.bottomSheet
import com.sensacare.app.R
import com.sensacare.app.domain.model.*
import com.sensacare.app.presentation.alerts.AlertsScreen
import com.sensacare.app.presentation.alerts.AlertsViewModel
import com.sensacare.app.presentation.alerts.EmergencyContactDetailScreen
import com.sensacare.app.presentation.alerts.EmergencyContactFormScreen
import com.sensacare.app.presentation.alerts.AlertDetailScreen
import com.sensacare.app.presentation.alerts.AlertRuleFormScreen
import com.sensacare.app.presentation.alerts.AlertPreferencesScreen
import com.sensacare.app.presentation.alerts.EmergencyContactsScreen
import com.sensacare.app.presentation.alerts.EmergencyScreen
import com.sensacare.app.presentation.dashboard.HealthDashboardScreen
import com.sensacare.app.presentation.dashboard.HealthDashboardViewModel
import com.sensacare.app.presentation.dashboard.HealthMetricDetailScreen
import com.sensacare.app.presentation.dashboard.HealthHistoryScreen
import com.sensacare.app.presentation.devices.DeviceDetailScreen
import com.sensacare.app.presentation.devices.DeviceManagementScreen
import com.sensacare.app.presentation.devices.DeviceManagementViewModel
import com.sensacare.app.presentation.devices.DeviceScanScreen
import com.sensacare.app.presentation.devices.DeviceSetupScreen
import com.sensacare.app.presentation.devices.DeviceSyncScreen
import com.sensacare.app.presentation.goals.GoalsManagementScreen
import com.sensacare.app.presentation.goals.GoalsManagementViewModel
import com.sensacare.app.presentation.goals.GoalDetailScreen
import com.sensacare.app.presentation.goals.GoalFormScreen
import com.sensacare.app.presentation.goals.GoalCalendarScreen
import com.sensacare.app.presentation.goals.GoalStatisticsScreen
import com.sensacare.app.presentation.goals.GoalSuggestionsScreen
import com.sensacare.app.presentation.goals.GoalAchievementScreen
import com.sensacare.app.presentation.insights.HealthInsightsScreen
import com.sensacare.app.presentation.insights.HealthInsightsViewModel
import com.sensacare.app.presentation.insights.InsightDetailScreen
import com.sensacare.app.presentation.insights.TrendAnalysisScreen
import com.sensacare.app.presentation.profile.ProfileScreen
import com.sensacare.app.presentation.profile.ProfileViewModel
import com.sensacare.app.presentation.profile.UserSettingsScreen
import com.sensacare.app.presentation.profile.UserPreferencesScreen
import com.sensacare.app.presentation.profile.AccountSettingsScreen
import com.sensacare.app.presentation.common.ErrorScreen
import com.sensacare.app.presentation.common.LoadingScreen
import com.sensacare.app.util.toJson
import com.sensacare.app.util.fromJson
import kotlinx.coroutines.flow.collectLatest
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import timber.log.Timber

/**
 * SensaCareNavigation - Comprehensive navigation system for the SensaCare app
 *
 * Features:
 * - Bottom navigation with main sections (Dashboard, Devices, Goals, Alerts, Profile)
 * - Deep linking support for all screens
 * - Proper argument handling for detail screens
 * - Nested navigation graphs for complex flows
 * - Animation transitions between screens
 * - Back stack management
 * - Support for modal screens and bottom sheets
 * - Type-safe navigation with sealed classes
 * - Integration with the existing ViewModels
 * - Navigation events handling
 */

/**
 * Main navigation host for the SensaCare app
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialNavigationApi::class)
@Composable
fun SensaCareNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = SensaCareDestination.Dashboard.route
) {
    // Bottom sheet navigator for modal bottom sheets
    val bottomSheetNavigator = remember { BottomSheetNavigator() }
    val navigator = rememberNavController(bottomSheetNavigator)
    
    // Current selected destination
    val currentDestination by navigator.currentBackStackEntryAsState()
    val currentRoute = currentDestination?.destination?.route
    
    // Bottom navigation items
    val bottomNavItems = listOf(
        SensaCareDestination.Dashboard,
        SensaCareDestination.Devices,
        SensaCareDestination.Goals,
        SensaCareDestination.Alerts,
        SensaCareDestination.Profile
    )
    
    // Scaffold with bottom navigation
    Scaffold(
        bottomBar = {
            // Only show bottom bar for main destinations
            if (currentRoute in bottomNavItems.map { it.route }) {
                SensaCareBottomNavigation(
                    currentDestination = currentDestination?.destination,
                    onNavigateToDestination = { destination ->
                        navigateSingleTop(navigator, destination.route)
                    }
                )
            }
        }
    ) { innerPadding ->
        // Modal bottom sheet layout
        ModalBottomSheetLayout(
            bottomSheetNavigator = bottomSheetNavigator,
            sheetShape = MaterialTheme.shapes.large
        ) {
            // Main navigation host
            NavHost(
                navController = navigator,
                startDestination = startDestination,
                modifier = modifier.padding(innerPadding)
            ) {
                // Dashboard navigation graph
                dashboardNavGraph(navigator)
                
                // Devices navigation graph
                devicesNavGraph(navigator)
                
                // Goals navigation graph
                goalsNavGraph(navigator)
                
                // Alerts navigation graph
                alertsNavGraph(navigator)
                
                // Profile navigation graph
                profileNavGraph(navigator)
            }
        }
    }
}

/**
 * Bottom navigation bar for the main destinations
 */
@Composable
fun SensaCareBottomNavigation(
    currentDestination: NavDestination?,
    onNavigateToDestination: (SensaCareDestination) -> Unit
) {
    NavigationBar {
        val items = listOf(
            SensaCareDestination.Dashboard,
            SensaCareDestination.Devices,
            SensaCareDestination.Goals,
            SensaCareDestination.Alerts,
            SensaCareDestination.Profile
        )
        
        items.forEach { destination ->
            val selected = currentDestination?.hierarchy?.any { 
                it.route?.startsWith(destination.route) == true 
            } == true
            
            NavigationBarItem(
                icon = {
                    Icon(
                        painter = painterResource(id = destination.iconResId),
                        contentDescription = stringResource(id = destination.labelResId)
                    )
                },
                label = { Text(stringResource(id = destination.labelResId)) },
                selected = selected,
                onClick = { onNavigateToDestination(destination) }
            )
        }
    }
}

/**
 * Dashboard navigation graph
 */
fun NavGraphBuilder.dashboardNavGraph(navController: NavController) {
    navigation(
        route = SensaCareDestination.Dashboard.route,
        startDestination = DashboardDestination.Main.route
    ) {
        // Main dashboard screen
        composable(
            route = DashboardDestination.Main.route,
            deepLinks = listOf(
                navDeepLink { uriPattern = "sensacare://dashboard" }
            ),
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            val viewModel: HealthDashboardViewModel = hiltViewModel()
            
            // Collect navigation events
            LaunchedEffect(viewModel) {
                viewModel.navigationEvent.observe(it.lifecycleOwner) { event ->
                    handleDashboardNavigationEvent(event, navController)
                }
            }
            
            HealthDashboardScreen(viewModel = viewModel)
        }
        
        // Health metric detail screen
        composable(
            route = DashboardDestination.MetricDetail.route,
            arguments = listOf(
                navArgument(DashboardDestination.MetricDetail.ARG_METRIC_TYPE) {
                    type = NavType.StringType
                },
                navArgument(DashboardDestination.MetricDetail.ARG_TIME_RANGE) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            ),
            deepLinks = listOf(
                navDeepLink { 
                    uriPattern = "sensacare://dashboard/metric/{${DashboardDestination.MetricDetail.ARG_METRIC_TYPE}}"
                }
            ),
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { it }) }
        ) { backStackEntry ->
            val viewModel: HealthDashboardViewModel = hiltViewModel()
            val metricType = backStackEntry.arguments?.getString(DashboardDestination.MetricDetail.ARG_METRIC_TYPE)
            val timeRange = backStackEntry.arguments?.getString(DashboardDestination.MetricDetail.ARG_TIME_RANGE)
            
            if (metricType != null) {
                HealthMetricDetailScreen(
                    viewModel = viewModel,
                    metricType = MetricType.valueOf(metricType),
                    timeRange = timeRange?.let { TimeRange.valueOf(it) },
                    onNavigateBack = { navController.navigateUp() }
                )
            } else {
                ErrorScreen(
                    message = "Invalid metric type",
                    onBackClick = { navController.navigateUp() }
                )
            }
        }
        
        // Health history screen
        composable(
            route = DashboardDestination.History.route,
            deepLinks = listOf(
                navDeepLink { uriPattern = "sensacare://dashboard/history" }
            ),
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { it }) }
        ) {
            val viewModel: HealthDashboardViewModel = hiltViewModel()
            
            HealthHistoryScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() }
            )
        }
        
        // Health insights screen
        composable(
            route = DashboardDestination.Insights.route,
            deepLinks = listOf(
                navDeepLink { uriPattern = "sensacare://dashboard/insights" }
            ),
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { it }) }
        ) {
            val viewModel: HealthInsightsViewModel = hiltViewModel()
            
            // Collect navigation events
            LaunchedEffect(viewModel) {
                viewModel.navigationEvent.observe(it.lifecycleOwner) { event ->
                    handleInsightsNavigationEvent(event, navController)
                }
            }
            
            HealthInsightsScreen(viewModel = viewModel)
        }
        
        // Insight detail screen
        composable(
            route = DashboardDestination.InsightDetail.route,
            arguments = listOf(
                navArgument(DashboardDestination.InsightDetail.ARG_INSIGHT_ID) {
                    type = NavType.LongType
                }
            ),
            deepLinks = listOf(
                navDeepLink { 
                    uriPattern = "sensacare://dashboard/insights/{${DashboardDestination.InsightDetail.ARG_INSIGHT_ID}}"
                }
            ),
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { it }) }
        ) { backStackEntry ->
            val viewModel: HealthInsightsViewModel = hiltViewModel()
            val insightId = backStackEntry.arguments?.getLong(DashboardDestination.InsightDetail.ARG_INSIGHT_ID) ?: 0L
            
            InsightDetailScreen(
                viewModel = viewModel,
                insightId = insightId,
                onNavigateBack = { navController.navigateUp() }
            )
        }
        
        // Trend analysis screen
        composable(
            route = DashboardDestination.TrendAnalysis.route,
            arguments = listOf(
                navArgument(DashboardDestination.TrendAnalysis.ARG_METRIC_TYPE) {
                    type = NavType.StringType
                }
            ),
            deepLinks = listOf(
                navDeepLink { 
                    uriPattern = "sensacare://dashboard/trends/{${DashboardDestination.TrendAnalysis.ARG_METRIC_TYPE}}"
                }
            ),
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { it }) }
        ) { backStackEntry ->
            val viewModel: HealthInsightsViewModel = hiltViewModel()
            val metricType = backStackEntry.arguments?.getString(DashboardDestination.TrendAnalysis.ARG_METRIC_TYPE)
            
            if (metricType != null) {
                TrendAnalysisScreen(
                    viewModel = viewModel,
                    metricType = MetricType.valueOf(metricType),
                    onNavigateBack = { navController.navigateUp() }
                )
            } else {
                ErrorScreen(
                    message = "Invalid metric type",
                    onBackClick = { navController.navigateUp() }
                )
            }
        }
    }
}

/**
 * Devices navigation graph
 */
fun NavGraphBuilder.devicesNavGraph(navController: NavController) {
    navigation(
        route = SensaCareDestination.Devices.route,
        startDestination = DevicesDestination.Main.route
    ) {
        // Main devices screen
        composable(
            route = DevicesDestination.Main.route,
            deepLinks = listOf(
                navDeepLink { uriPattern = "sensacare://devices" }
            ),
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            val viewModel: DeviceManagementViewModel = hiltViewModel()
            
            // Collect navigation events
            LaunchedEffect(viewModel) {
                viewModel.navigationEvent.observe(it.lifecycleOwner) { event ->
                    handleDeviceNavigationEvent(event, navController)
                }
            }
            
            DeviceManagementScreen(viewModel = viewModel)
        }
        
        // Device scan screen
        composable(
            route = DevicesDestination.Scan.route,
            deepLinks = listOf(
                navDeepLink { uriPattern = "sensacare://devices/scan" }
            ),
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { it }) }
        ) {
            val viewModel: DeviceManagementViewModel = hiltViewModel()
            
            DeviceScanScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() }
            )
        }
        
        // Device detail screen
        composable(
            route = DevicesDestination.DeviceDetail.route,
            arguments = listOf(
                navArgument(DevicesDestination.DeviceDetail.ARG_DEVICE_ID) {
                    type = NavType.StringType
                }
            ),
            deepLinks = listOf(
                navDeepLink { 
                    uriPattern = "sensacare://devices/{${DevicesDestination.DeviceDetail.ARG_DEVICE_ID}}"
                }
            ),
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { it }) }
        ) { backStackEntry ->
            val viewModel: DeviceManagementViewModel = hiltViewModel()
            val deviceId = backStackEntry.arguments?.getString(DevicesDestination.DeviceDetail.ARG_DEVICE_ID) ?: ""
            
            DeviceDetailScreen(
                viewModel = viewModel,
                deviceId = deviceId,
                onNavigateBack = { navController.navigateUp() }
            )
        }
        
        // Device setup screen
        composable(
            route = DevicesDestination.Setup.route,
            arguments = listOf(
                navArgument(DevicesDestination.Setup.ARG_DEVICE_ID) {
                    type = NavType.StringType
                }
            ),
            deepLinks = listOf(
                navDeepLink { 
                    uriPattern = "sensacare://devices/setup/{${DevicesDestination.Setup.ARG_DEVICE_ID}}"
                }
            ),
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { it }) }
        ) { backStackEntry ->
            val viewModel: DeviceManagementViewModel = hiltViewModel()
            val deviceId = backStackEntry.arguments?.getString(DevicesDestination.Setup.ARG_DEVICE_ID) ?: ""
            
            DeviceSetupScreen(
                viewModel = viewModel,
                deviceId = deviceId,
                onNavigateBack = { navController.navigateUp() },
                onSetupComplete = {
                    navController.navigate(DevicesDestination.Main.route) {
                        popUpTo(DevicesDestination.Main.route) { inclusive = true }
                    }
                }
            )
        }
        
        // Device sync screen
        composable(
            route = DevicesDestination.Sync.route,
            arguments = listOf(
                navArgument(DevicesDestination.Sync.ARG_DEVICE_ID) {
                    type = NavType.StringType
                }
            ),
            deepLinks = listOf(
                navDeepLink { 
                    uriPattern = "sensacare://devices/sync/{${DevicesDestination.Sync.ARG_DEVICE_ID}}"
                }
            ),
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { it }) }
        ) { backStackEntry ->
            val viewModel: DeviceManagementViewModel = hiltViewModel()
            val deviceId = backStackEntry.arguments?.getString(DevicesDestination.Sync.ARG_DEVICE_ID) ?: ""
            
            DeviceSyncScreen(
                viewModel = viewModel,
                deviceId = deviceId,
                onNavigateBack = { navController.navigateUp() }
            )
        }
    }
}

/**
 * Goals navigation graph
 */
fun NavGraphBuilder.goalsNavGraph(navController: NavController) {
    navigation(
        route = SensaCareDestination.Goals.route,
        startDestination = GoalsDestination.Main.route
    ) {
        // Main goals screen
        composable(
            route = GoalsDestination.Main.route,
            deepLinks = listOf(
                navDeepLink { uriPattern = "sensacare://goals" }
            ),
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            val viewModel: GoalsManagementViewModel = hiltViewModel()
            
            // Collect navigation events
            LaunchedEffect(viewModel) {
                viewModel.navigationEvent.observe(it.lifecycleOwner) { event ->
                    handleGoalsNavigationEvent(event, navController)
                }
            }
            
            GoalsManagementScreen(viewModel = viewModel)
        }
        
        // Goal detail screen
        composable(
            route = GoalsDestination.GoalDetail.route,
            arguments = listOf(
                navArgument(GoalsDestination.GoalDetail.ARG_GOAL_ID) {
                    type = NavType.LongType
                }
            ),
            deepLinks = listOf(
                navDeepLink { 
                    uriPattern = "sensacare://goals/{${GoalsDestination.GoalDetail.ARG_GOAL_ID}}"
                }
            ),
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { it }) }
        ) { backStackEntry ->
            val viewModel: GoalsManagementViewModel = hiltViewModel()
            val goalId = backStackEntry.arguments?.getLong(GoalsDestination.GoalDetail.ARG_GOAL_ID) ?: 0L
            
            GoalDetailScreen(
                viewModel = viewModel,
                goalId = goalId,
                onNavigateBack = { navController.navigateUp() }
            )
        }
        
        // Goal form screen (create/edit)
        composable(
            route = GoalsDestination.GoalForm.route,
            arguments = listOf(
                navArgument(GoalsDestination.GoalForm.ARG_GOAL_ID) {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            ),
            deepLinks = listOf(
                navDeepLink { uriPattern = "sensacare://goals/create" },
                navDeepLink { 
                    uriPattern = "sensacare://goals/edit/{${GoalsDestination.GoalForm.ARG_GOAL_ID}}"
                }
            ),
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { it }) }
        ) { backStackEntry ->
            val viewModel: GoalsManagementViewModel = hiltViewModel()
            val goalId = backStackEntry.arguments?.getLong(GoalsDestination.GoalForm.ARG_GOAL_ID) ?: 0L
            
            GoalFormScreen(
                viewModel = viewModel,
                goalId = goalId,
                onNavigateBack = { navController.navigateUp() }
            )
        }
        
        // Goal calendar screen
        composable(
            route = GoalsDestination.Calendar.route,
            deepLinks = listOf(
                navDeepLink { uriPattern = "sensacare://goals/calendar" }
            ),
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { it }) }
        ) {
            val viewModel: GoalsManagementViewModel = hiltViewModel()
            
            GoalCalendarScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() }
            )
        }
        
        // Goal statistics screen
        composable(
            route = GoalsDestination.Statistics.route,
            deepLinks = listOf(
                navDeepLink { uriPattern = "sensacare://goals/statistics" }
            ),
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { it }) }
        ) {
            val viewModel: GoalsManagementViewModel = hiltViewModel()
            
            GoalStatisticsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() }
            )
        }
        
        // Goal suggestions screen
        composable(
            route = GoalsDestination.Suggestions.route,
            deepLinks = listOf(
                navDeepLink { uriPattern = "sensacare://goals/suggestions" }
            ),
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { it }) }
        ) {
            val viewModel: GoalsManagementViewModel = hiltViewModel()
            
            GoalSuggestionsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() }
            )
        }
        
        // Goal achievement screen
        composable(
            route = GoalsDestination.Achievement.route,
            arguments = listOf(
                navArgument(GoalsDestination.Achievement.ARG_GOAL_ID) {
                    type = NavType.LongType
                }
            ),
            deepLinks = listOf(
                navDeepLink { 
                    uriPattern = "sensacare://goals/achievement/{${GoalsDestination.Achievement.ARG_GOAL_ID}}"
                }
            ),
            enterTransition = { 
                scaleIn(initialScale = 0.8f) + fadeIn()
            },
            exitTransition = { 
                scaleOut(targetScale = 0.8f) + fadeOut()
            }
        ) { backStackEntry ->
            val viewModel: GoalsManagementViewModel = hiltViewModel()
            val goalId = backStackEntry.arguments?.getLong(GoalsDestination.Achievement.ARG_GOAL_ID) ?: 0L
            
            GoalAchievementScreen(
                viewModel = viewModel,
                goalId = goalId,
                onNavigateBack = { navController.navigateUp() }
            )
        }
    }
}

/**
 * Alerts navigation graph
 */
fun NavGraphBuilder.alertsNavGraph(navController: NavController) {
    navigation(
        route = SensaCareDestination.Alerts.route,
        startDestination = AlertsDestination.Main.route
    ) {
        // Main alerts screen
        composable(
            route = AlertsDestination.Main.route,
            deepLinks = listOf(
                navDeepLink { uriPattern = "sensacare://alerts" }
            ),
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            val viewModel: AlertsViewModel = hiltViewModel()
            
            // Collect navigation events
            LaunchedEffect(viewModel) {
                viewModel.navigationEvent.observe(it.lifecycleOwner) { event ->
                    handleAlertsNavigationEvent(event, navController)
                }
            }
            
            AlertsScreen(viewModel = viewModel)
        }
        
        // Alert detail screen
        composable(
            route = AlertsDestination.AlertDetail.route,
            arguments = listOf(
                navArgument(AlertsDestination.AlertDetail.ARG_ALERT_ID) {
                    type = NavType.LongType
                }
            ),
            deepLinks = listOf(
                navDeepLink { 
                    uriPattern = "sensacare://alerts/{${AlertsDestination.AlertDetail.ARG_ALERT_ID}}"
                }
            ),
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { it }) }
        ) { backStackEntry ->
            val viewModel: AlertsViewModel = hiltViewModel()
            val alertId = backStackEntry.arguments?.getLong(AlertsDestination.AlertDetail.ARG_ALERT_ID) ?: 0L
            
            AlertDetailScreen(
                viewModel = viewModel,
                alertId = alertId,
                onNavigateBack = { navController.navigateUp() }
            )
        }
        
        // Alert rule form screen
        composable(
            route = AlertsDestination.RuleForm.route,
            arguments = listOf(
                navArgument(AlertsDestination.RuleForm.ARG_RULE_ID) {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            ),
            deepLinks = listOf(
                navDeepLink { uriPattern = "sensacare://alerts/rules/create" },
                navDeepLink { 
                    uriPattern = "sensacare://alerts/rules/edit/{${AlertsDestination.RuleForm.ARG_RULE_ID}}"
                }
            ),
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { it }) }
        ) { backStackEntry ->
            val viewModel: AlertsViewModel = hiltViewModel()
            val ruleId = backStackEntry.arguments?.getLong(AlertsDestination.RuleForm.ARG_RULE_ID) ?: 0L
            
            AlertRuleFormScreen(
                viewModel = viewModel,
                ruleId = ruleId,
                onNavigateBack = { navController.navigateUp() }
            )
        }
        
        // Alert preferences screen
        composable(
            route = AlertsDestination.Preferences.route,
            deepLinks = listOf(
                navDeepLink { uriPattern = "sensacare://alerts/preferences" }
            ),
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { it }) }
        ) {
            val viewModel: AlertsViewModel = hiltViewModel()
            
            AlertPreferencesScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() }
            )
        }
        
        // Emergency contacts screen
        composable(
            route = AlertsDestination.EmergencyContacts.route,
            deepLinks = listOf(
                navDeepLink { uriPattern = "sensacare://alerts/emergency-contacts" }
            ),
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { it }) }
        ) {
            val viewModel: AlertsViewModel = hiltViewModel()
            
            EmergencyContactsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() }
            )
        }
        
        // Emergency contact detail screen
        composable(
            route = AlertsDestination.EmergencyContactDetail.route,
            arguments = listOf(
                navArgument(AlertsDestination.EmergencyContactDetail.ARG_CONTACT_ID) {
                    type = NavType.LongType
                }
            ),
            deepLinks = listOf(
                navDeepLink { 
                    uriPattern = "sensacare://alerts/emergency-contacts/{${AlertsDestination.EmergencyContactDetail.ARG_CONTACT_ID}}"
                }
            ),
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { it }) }
        ) { backStackEntry ->
            val viewModel: AlertsViewModel = hiltViewModel()
            val contactId = backStackEntry.arguments?.getLong(AlertsDestination.EmergencyContactDetail.ARG_CONTACT_ID) ?: 0L
            
            EmergencyContactDetailScreen(
                viewModel = viewModel,
                contactId = contactId,
                onNavigateBack = { navController.navigateUp() }
            )
        }
        
        // Emergency contact form screen
        composable(
            route = AlertsDestination.EmergencyContactForm.route,
            arguments = listOf(
                navArgument(AlertsDestination.EmergencyContactForm.ARG_CONTACT_ID) {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            ),
            deepLinks = listOf(
                navDeepLink { uriPattern = "sensacare://alerts/emergency-contacts/create" },
                navDeepLink { 
                    uriPattern = "sensacare://alerts/emergency-contacts/edit/{${AlertsDestination.EmergencyContactForm.ARG_CONTACT_ID}}"
                }
            ),
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { it }) }
        ) { backStackEntry ->
            val viewModel: AlertsViewModel = hiltViewModel()
            val contactId = backStackEntry.arguments?.getLong(AlertsDestination.EmergencyContactForm.ARG_CONTACT_ID) ?: 0L
            
            EmergencyContactFormScreen(
                viewModel = viewModel,
                contactId = contactId,
                onNavigateBack = { navController.navigateUp() }
            )
        }
        
        // Emergency screen (modal)
        composable(
            route = AlertsDestination.Emergency.route,
            arguments = listOf(
                navArgument(AlertsDestination.Emergency.ARG_ALERT_ID) {
                    type = NavType.LongType
                }
            ),
            deepLinks = listOf(
                navDeepLink { 
                    uriPattern = "sensacare://alerts/emergency/{${AlertsDestination.Emergency.ARG_ALERT_ID}}"
                }
            ),
            enterTransition = { 
                scaleIn(initialScale = 0.8f) + fadeIn(animationSpec = tween(200))
            },
            exitTransition = { 
                scaleOut(targetScale = 0.8f) + fadeOut(animationSpec = tween(200))
            }
        ) { backStackEntry ->
            val viewModel: AlertsViewModel = hiltViewModel()
            val alertId = backStackEntry.arguments?.getLong(AlertsDestination.Emergency.ARG_ALERT_ID) ?: 0L
            
            EmergencyScreen(
                viewModel = viewModel,
                alertId = alertId,
                onDismiss = { navController.navigateUp() }
            )
        }
    }
}

/**
 * Profile navigation graph
 */
fun NavGraphBuilder.profileNavGraph(navController: NavController) {
    navigation(
        route = SensaCareDestination.Profile.route,
        startDestination = ProfileDestination.Main.route
    ) {
        // Main profile screen
        composable(
            route = ProfileDestination.Main.route,
            deepLinks = listOf(
                navDeepLink { uriPattern = "sensacare://profile" }
            ),
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            val viewModel: ProfileViewModel = hiltViewModel()
            
            // Collect navigation events
            LaunchedEffect(viewModel) {
                viewModel.navigationEvent.observe(it.lifecycleOwner) { event ->
                    handleProfileNavigationEvent(event, navController)
                }
            }
            
            ProfileScreen(viewModel = viewModel)
        }
        
        // User settings screen
        composable(
            route = ProfileDestination.Settings.route,
            deepLinks = listOf(
                navDeepLink { uriPattern = "sensacare://profile/settings" }
            ),
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { it }) }
        ) {
            val viewModel: ProfileViewModel = hiltViewModel()
            
            UserSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() }
            )
        }
        
        // User preferences screen
        composable(
            route = ProfileDestination.Preferences.route,
            deepLinks = listOf(
                navDeepLink { uriPattern = "sensacare://profile/preferences" }
            ),
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { it }) }
        ) {
            val viewModel: ProfileViewModel = hiltViewModel()
            
            UserPreferencesScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() }
            )
        }
        
        // Account settings screen
        composable(
            route = ProfileDestination.Account.route,
            deepLinks = listOf(
                navDeepLink { uriPattern = "sensacare://profile/account" }
            ),
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { it }) }
        ) {
            val viewModel: ProfileViewModel = hiltViewModel()
            
            AccountSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() }
            )
        }
    }
}

/**
 * Bottom sheet destinations
 */
@OptIn(ExperimentalMaterialNavigationApi::class)
fun NavGraphBuilder.bottomSheetDestinations(navController: NavController) {
    // Goal filters bottom sheet
    bottomSheet(
        route = BottomSheetDestination.GoalFilters.route
    ) {
        val viewModel: GoalsManagementViewModel = hiltViewModel()
        
        // Goal filters content
        // Implementation will be added later
    }
    
    // Alert filters bottom sheet
    bottomSheet(
        route = BottomSheetDestination.AlertFilters.route
    ) {
        val viewModel: AlertsViewModel = hiltViewModel()
        
        // Alert filters content
        // Implementation will be added later
    }
    
    // Device options bottom sheet
    bottomSheet(
        route = BottomSheetDestination.DeviceOptions.route,
        arguments = listOf(
            navArgument(BottomSheetDestination.DeviceOptions.ARG_DEVICE_ID) {
                type = NavType.StringType
            }
        )
    ) { backStackEntry ->
        val viewModel: DeviceManagementViewModel = hiltViewModel()
        val deviceId = backStackEntry.arguments?.getString(BottomSheetDestination.DeviceOptions.ARG_DEVICE_ID) ?: ""
        
        // Device options content
        // Implementation will be added later
    }
}

/**
 * Handle dashboard navigation events
 */
private fun handleDashboardNavigationEvent(
    event: Any?,
    navController: NavController
) {
    when (event) {
        // Add dashboard navigation event handling
    }
}

/**
 * Handle insights navigation events
 */
private fun handleInsightsNavigationEvent(
    event: Any?,
    navController: NavController
) {
    when (event) {
        // Add insights navigation event handling
    }
}

/**
 * Handle device navigation events
 */
private fun handleDeviceNavigationEvent(
    event: Any?,
    navController: NavController
) {
    when (event) {
        is DeviceNavigationEvent.ToDeviceDetail -> {
            navController.navigate(
                DevicesDestination.DeviceDetail.createRoute(event.device.deviceId)
            )
        }
        is DeviceNavigationEvent.ToDeviceScan -> {
            navController.navigate(DevicesDestination.Scan.route)
        }
        is DeviceNavigationEvent.ToDeviceSetup -> {
            navController.navigate(
                DevicesDestination.Setup.createRoute(event.deviceId)
            )
        }
        is DeviceNavigationEvent.ToDeviceSync -> {
            navController.navigate(
                DevicesDestination.Sync.createRoute(event.deviceId)
            )
        }
        is DeviceNavigationEvent.BackToDevicesList -> {
            navController.navigateUp()
        }
    }
}

/**
 * Handle goals navigation events
 */
private fun handleGoalsNavigationEvent(
    event: Any?,
    navController: NavController
) {
    when (event) {
        is GoalsNavigationEvent.ToGoalDetail -> {
            navController.navigate(
                GoalsDestination.GoalDetail.createRoute(event.goal.id)
            )
        }
        is GoalsNavigationEvent.ToGoalForm -> {
            val goalId = event.goal?.id ?: 0L
            navController.navigate(
                GoalsDestination.GoalForm.createRoute(goalId)
            )
        }
        is GoalsNavigationEvent.ToDateView -> {
            // Handle date view navigation
        }
        is GoalsNavigationEvent.ToStatisticsView -> {
            navController.navigate(GoalsDestination.Statistics.route)
        }
        is GoalsNavigationEvent.ToSuggestionsView -> {
            navController.navigate(GoalsDestination.Suggestions.route)
        }
        is GoalsNavigationEvent.ToCalendarView -> {
            navController.navigate(GoalsDestination.Calendar.route)
        }
        is GoalsNavigationEvent.BackToGoalsList -> {
            navController.navigateUp()
        }
        is GoalsNavigationEvent.ShowAchievementCelebration -> {
            navController.navigate(
                GoalsDestination.Achievement.createRoute(event.goalId)
            )
        }
        is GoalsNavigationEvent.ShareGoalAchievement -> {
            // Handle sharing via Intent
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, event.title)
                putExtra(Intent.EXTRA_TEXT, event.message)
            }
            val context = navController.context
            context.startActivity(Intent.createChooser(intent, "Share Goal Achievement"))
        }
    }
}

/**
 * Handle alerts navigation events
 */
private fun handleAlertsNavigationEvent(
    event: Any?,
    navController: NavController
) {
    when (event) {
        is AlertNavigationEvent.ToAlertDetail -> {
            navController.navigate(
                AlertsDestination.AlertDetail.createRoute(event.alert.id)
            )
        }
        is AlertNavigationEvent.ToRuleDetail -> {
            // Handle rule detail navigation
        }
        is AlertNavigationEvent.ToRuleForm -> {
            val ruleId = event.rule?.id ?: 0L
            navController.navigate(
                AlertsDestination.RuleForm.createRoute(ruleId)
            )
        }
        is AlertNavigationEvent.ToContactDetail -> {
            navController.navigate(
                AlertsDestination.EmergencyContactDetail.createRoute(event.contact.id)
            )
        }
        is AlertNavigationEvent.ToContactForm -> {
            val contactId = event.contact?.id ?: 0L
            navController.navigate(
                AlertsDestination.EmergencyContactForm.createRoute(contactId)
            )
        }
        is AlertNavigationEvent.ToAlertPreferences -> {
            navController.navigate(AlertsDestination.Preferences.route)
        }
        is AlertNavigationEvent.ToAlertRules -> {
            // Handle alert rules navigation
        }
        is AlertNavigationEvent.ToEmergencyContacts -> {
            navController.navigate(AlertsDestination.EmergencyContacts.route)
        }
        is AlertNavigationEvent.ToEmergencyScreen -> {
            navController.navigate(
                AlertsDestination.Emergency.createRoute(event.alert.id)
            )
        }
        is AlertNavigationEvent.BackToAlertsList -> {
            navController.navigateUp()
        }
        is AlertNavigationEvent.BackToRulesList -> {
            navController.navigateUp()
        }
        is AlertNavigationEvent.BackToContactsList -> {
            navController.navigateUp()
        }
    }
}

/**
 * Handle profile navigation events
 */
private fun handleProfileNavigationEvent(
    event: Any?,
    navController: NavController
) {
    when (event) {
        // Add profile navigation event handling
    }
}

/**
 * Navigate to a destination, clearing the back stack
 */
private fun navigateSingleTop(navController: NavController, route: String) {
    navController.navigate(route) {
        // Pop up to the start destination of the graph
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        // Avoid multiple copies of the same destination
        launchSingleTop = true
        // Restore state when reselecting a previously selected item
        restoreState = true
    }
}

/**
 * Main navigation destinations
 */
sealed class SensaCareDestination(
    val route: String,
    val iconResId: Int,
    val labelResId: Int
) {
    object Dashboard : SensaCareDestination(
        route = "dashboard",
        iconResId = R.drawable.ic_dashboard,
        labelResId = R.string.dashboard
    )
    
    object Devices : SensaCareDestination(
        route = "devices",
        iconResId = R.drawable.ic_devices,
        labelResId = R.string.devices
    )
    
    object Goals : SensaCareDestination(
        route = "goals",
        iconResId = R.drawable.ic_goals,
        labelResId = R.string.goals
    )
    
    object Alerts : SensaCareDestination(
        route = "alerts",
        iconResId = R.drawable.ic_alerts,
        labelResId = R.string.alerts
    )
    
    object Profile : SensaCareDestination(
        route = "profile",
        iconResId = R.drawable.ic_profile,
        labelResId = R.string.profile
    )
}

/**
 * Dashboard destinations
 */
sealed class DashboardDestination(val route: String) {
    object Main : DashboardDestination("dashboard/main")
    
    object MetricDetail : DashboardDestination("dashboard/metric/{$ARG_METRIC_TYPE}?timeRange={$ARG_TIME_RANGE}") {
        const val ARG_METRIC_TYPE = "metricType"
        const val ARG_TIME_RANGE = "timeRange"
        
        fun createRoute(metricType: MetricType, timeRange: TimeRange? = null): String {
            return if (timeRange != null) {
                "dashboard/metric/${metricType.name}?timeRange=${timeRange.name}"
            } else {
                "dashboard/metric/${metricType.name}"
            }
        }
    }
    
    object History : DashboardDestination("dashboard/history")
    
    object Insights : DashboardDestination("dashboard/insights")
    
    object InsightDetail : DashboardDestination("dashboard/insights/{$ARG_INSIGHT_ID}") {
        const val ARG_INSIGHT_ID = "insightId"
        
        fun createRoute(insightId: Long): String {
            return "dashboard/insights/$insightId"
        }
    }
    
    object TrendAnalysis : DashboardDestination("dashboard/trends/{$ARG_METRIC_TYPE}") {
        const val ARG_METRIC_TYPE = "metricType"
        
        fun createRoute(metricType: MetricType): String {
            return "dashboard/trends/${metricType.name}"
        }
    }
}

/**
 * Devices destinations
 */
sealed class DevicesDestination(val route: String) {
    object Main : DevicesDestination("devices/main")
    
    object Scan : DevicesDestination("devices/scan")
    
    object DeviceDetail : DevicesDestination("devices/detail/{$ARG_DEVICE_ID}") {
        const val ARG_DEVICE_ID = "deviceId"
        
        fun createRoute(deviceId: String): String {
            return "devices/detail/$deviceId"
        }
    }
    
    object Setup : DevicesDestination("devices/setup/{$ARG_DEVICE_ID}") {
        const val ARG_DEVICE_ID = "deviceId"
        
        fun createRoute(deviceId: String): String {
            return "devices/setup/$deviceId"
        }
    }
    
    object Sync : DevicesDestination("devices/sync/{$ARG_DEVICE_ID}") {
        const val ARG_DEVICE_ID = "deviceId"
        
        fun createRoute(deviceId: String): String {
            return "devices/sync/$deviceId"
        }
    }
}

/**
 * Goals destinations
 */
sealed class GoalsDestination(val route: String) {
    object Main : GoalsDestination("goals/main")
    
    object GoalDetail : GoalsDestination("goals/detail/{$ARG_GOAL_ID}") {
        const val ARG_GOAL_ID = "goalId"
        
        fun createRoute(goalId: Long): String {
            return "goals/detail/$goalId"
        }
    }
    
    object GoalForm : GoalsDestination("goals/form?goalId={$ARG_GOAL_ID}") {
        const val ARG_GOAL_ID = "goalId"
        
        fun createRoute(goalId: Long = 0L): String {
            return "goals/form?goalId=$goalId"
        }
    }
    
    object Calendar : GoalsDestination("goals/calendar")
    
    object Statistics : GoalsDestination("goals/statistics")
    
    object Suggestions : GoalsDestination("goals/suggestions")
    
    object Achievement : GoalsDestination("goals/achievement/{$ARG_GOAL_ID}") {
        const val ARG_GOAL_ID = "goalId"
        
        fun createRoute(goalId: Long): String {
            return "goals/achievement/$goalId"
        }
    }
}

/**
 * Alerts destinations
 */
sealed class AlertsDestination(val route: String) {
    object Main : AlertsDestination("alerts/main")
    
    object AlertDetail : AlertsDestination("alerts/detail/{$ARG_ALERT_ID}") {
        const val ARG_ALERT_ID = "alertId"
        
        fun createRoute(alertId: Long): String {
            return "alerts/detail/$alertId"
        }
    }
    
    object RuleForm : AlertsDestination("alerts/rules/form?ruleId={$ARG_RULE_ID}") {
        const val ARG_RULE_ID = "ruleId"
        
        fun createRoute(ruleId: Long = 0L): String {
            return "alerts/rules/form?ruleId=$ruleId"
        }
    }
    
    object Preferences : AlertsDestination("alerts/preferences")
    
    object EmergencyContacts : AlertsDestination("alerts/emergency-contacts")
    
    object EmergencyContactDetail : AlertsDestination("alerts/emergency-contacts/detail/{$ARG_CONTACT_ID}") {
        const val ARG_CONTACT_ID = "contactId"
        
        fun createRoute(contactId: Long): String {
            return "alerts/emergency-contacts/detail/$contactId"
        }
    }
    
    object EmergencyContactForm : AlertsDestination("alerts/emergency-contacts/form?contactId={$ARG_CONTACT_ID}") {
        const val ARG_CONTACT_ID = "contactId"
        
        fun createRoute(contactId: Long = 0L): String {
            return "alerts/emergency-contacts/form?contactId=$contactId"
        }
    }
    
    object Emergency : AlertsDestination("alerts/emergency/{$ARG_ALERT_ID}") {
        const val ARG_ALERT_ID = "alertId"
        
        fun createRoute(alertId: Long): String {
            return "alerts/emergency/$alertId"
        }
    }
}

/**
 * Profile destinations
 */
sealed class ProfileDestination(val route: String) {
    object Main : ProfileDestination("profile/main")
    
    object Settings : ProfileDestination("profile/settings")
    
    object Preferences : ProfileDestination("profile/preferences")
    
    object Account : ProfileDestination("profile/account")
}

/**
 * Bottom sheet destinations
 */
sealed class BottomSheetDestination(val route: String) {
    object GoalFilters : BottomSheetDestination("bottomsheet/goal-filters")
    
    object AlertFilters : BottomSheetDestination("bottomsheet/alert-filters")
    
    object DeviceOptions : BottomSheetDestination("bottomsheet/device-options/{$ARG_DEVICE_ID}") {
        const val ARG_DEVICE_ID = "deviceId"
        
        fun createRoute(deviceId: String): String {
            return "bottomsheet/device-options/$deviceId"
        }
    }
}

/**
 * Extension function to encode objects for navigation
 */
inline fun <reified T> T.encodeForNavigation(): String {
    val json = this.toJson()
    return URLEncoder.encode(json, StandardCharsets.UTF_8.toString())
}

/**
 * Extension function to decode objects from navigation
 */
inline fun <reified T> String.decodeFromNavigation(): T? {
    return try {
        this.fromJson<T>()
    } catch (e: Exception) {
        Timber.e(e, "Failed to decode navigation argument")
        null
    }
}
