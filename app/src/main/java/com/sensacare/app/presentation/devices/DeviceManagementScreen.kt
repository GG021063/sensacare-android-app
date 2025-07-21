package com.sensacare.app.presentation.devices

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.sensacare.app.presentation.common.*
import com.sensacare.app.presentation.theme.*
import com.sensacare.app.util.formatDateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * DeviceManagementScreen - Comprehensive device management interface
 *
 * This screen provides a complete interface for managing HBand devices, including:
 * - Connected devices list with status indicators
 * - Device scanning and discovery with BLE animation
 * - Device connection/disconnection controls
 * - Battery level indicators for connected devices
 * - Device sync status and last sync time
 * - Device information cards with model details
 * - Connection troubleshooting help
 * - Add new device flow with setup wizard
 * - Device settings and preferences
 * - Real-time connection status updates
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DeviceManagementScreen(
    modifier: Modifier = Modifier,
    viewModel: DeviceManagementViewModel = hiltViewModel()
) {
    // State
    val uiState by viewModel.uiState.collectAsState()
    val connectedDevices by viewModel.connectedDevices.collectAsState()
    val availableDevices by viewModel.availableDevices.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val selectedDevice by viewModel.selectedDevice.collectAsState()
    val syncProgress by viewModel.syncProgress.collectAsState()
    val lastSyncTime by viewModel.lastSyncTime.collectAsState()
    val bluetoothEnabled by viewModel.bluetoothEnabled.collectAsState()
    val locationEnabled by viewModel.locationEnabled.collectAsState()
    
    // Local state
    val refreshing by viewModel.isRefreshing.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberLazyListState()
    
    // Pull-to-refresh state
    val pullRefreshState = rememberPullRefreshState(
        refreshing = refreshing,
        onRefresh = {
            coroutineScope.launch {
                viewModel.refreshDevices()
            }
        }
    )
    
    // Show scan dialog
    var showScanDialog by remember { mutableStateOf(false) }
    
    // Show troubleshooting dialog
    var showTroubleshootingDialog by remember { mutableStateOf(false) }
    
    // Collect error events
    LaunchedEffect(viewModel) {
        viewModel.errorEvents.collect { error ->
            // Handle error events (would typically show a snackbar)
        }
    }
    
    // Main content
    Box(
        modifier = modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        when (uiState) {
            is DeviceManagementUiState.Loading -> {
                LoadingView(
                    message = stringResource(R.string.loading_devices)
                )
            }
            
            is DeviceManagementUiState.Error -> {
                val errorState = uiState as DeviceManagementUiState.Error
                ErrorView(
                    message = errorState.message,
                    onRetry = { viewModel.refreshDevices() }
                )
            }
            
            is DeviceManagementUiState.Success -> {
                DeviceManagementContent(
                    connectedDevices = connectedDevices,
                    availableDevices = availableDevices,
                    isScanning = isScanning,
                    isSyncing = isSyncing,
                    syncProgress = syncProgress,
                    lastSyncTime = lastSyncTime,
                    bluetoothEnabled = bluetoothEnabled,
                    locationEnabled = locationEnabled,
                    onAddDeviceClick = { showScanDialog = true },
                    onDeviceClick = { viewModel.selectDevice(it) },
                    onConnectDevice = { viewModel.connectDevice(it) },
                    onDisconnectDevice = { viewModel.disconnectDevice(it) },
                    onForgetDevice = { viewModel.forgetDevice(it) },
                    onSyncDevice = { viewModel.syncDevice(it) },
                    onSyncAllDevices = { viewModel.syncAllDevices() },
                    onTroubleshootingClick = { showTroubleshootingDialog = true },
                    onEnableBluetooth = { viewModel.enableBluetooth() },
                    onEnableLocation = { viewModel.enableLocation() },
                    scrollState = scrollState
                )
            }
            
            is DeviceManagementUiState.Empty -> {
                EmptyDevicesContent(
                    bluetoothEnabled = bluetoothEnabled,
                    locationEnabled = locationEnabled,
                    onAddDeviceClick = { showScanDialog = true },
                    onEnableBluetooth = { viewModel.enableBluetooth() },
                    onEnableLocation = { viewModel.enableLocation() },
                    onTroubleshootingClick = { showTroubleshootingDialog = true }
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
        
        // Device scanning dialog
        if (showScanDialog) {
            DeviceScanDialog(
                availableDevices = availableDevices,
                isScanning = isScanning,
                onStartScan = { viewModel.startScan() },
                onStopScan = { viewModel.stopScan() },
                onConnectDevice = { 
                    viewModel.connectDevice(it)
                    showScanDialog = false
                },
                onDismiss = { showScanDialog = false }
            )
        }
        
        // Troubleshooting dialog
        if (showTroubleshootingDialog) {
            TroubleshootingDialog(
                onDismiss = { showTroubleshootingDialog = false }
            )
        }
    }
}

/**
 * Main content for the device management screen when devices are available
 */
@Composable
private fun DeviceManagementContent(
    connectedDevices: List<HBandDevice>,
    availableDevices: List<HBandDevice>,
    isScanning: Boolean,
    isSyncing: Boolean,
    syncProgress: Float,
    lastSyncTime: LocalDateTime?,
    bluetoothEnabled: Boolean,
    locationEnabled: Boolean,
    onAddDeviceClick: () -> Unit,
    onDeviceClick: (HBandDevice) -> Unit,
    onConnectDevice: (HBandDevice) -> Unit,
    onDisconnectDevice: (HBandDevice) -> Unit,
    onForgetDevice: (HBandDevice) -> Unit,
    onSyncDevice: (HBandDevice) -> Unit,
    onSyncAllDevices: () -> Unit,
    onTroubleshootingClick: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onEnableLocation: () -> Unit,
    scrollState: LazyListState,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = scrollState,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // Header
        item {
            DeviceManagementHeader(
                connectedDeviceCount = connectedDevices.size,
                lastSyncTime = lastSyncTime
            )
        }
        
        // Bluetooth and location warnings if needed
        if (!bluetoothEnabled || !locationEnabled) {
            item {
                PermissionsWarningCard(
                    bluetoothEnabled = bluetoothEnabled,
                    locationEnabled = locationEnabled,
                    onEnableBluetooth = onEnableBluetooth,
                    onEnableLocation = onEnableLocation,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
        
        // Connected devices section
        item {
            SectionHeader(
                title = stringResource(R.string.connected_devices),
                subtitle = if (connectedDevices.isNotEmpty()) 
                    stringResource(R.string.tap_to_manage_device) 
                else null,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        
        // Connected devices list
        if (connectedDevices.isNotEmpty()) {
            items(connectedDevices) { device ->
                ConnectedDeviceCard(
                    device = device,
                    isSyncing = isSyncing && device == connectedDevices.firstOrNull { it.isSyncing },
                    syncProgress = if (device.isSyncing) syncProgress else null,
                    onClick = { onDeviceClick(device) },
                    onDisconnectClick = { onDisconnectDevice(device) },
                    onForgetClick = { onForgetDevice(device) },
                    onSyncClick = { onSyncDevice(device) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            
            // Sync all devices button
            if (connectedDevices.size > 1) {
                item {
                    SyncAllDevicesButton(
                        onSyncAllDevices = onSyncAllDevices,
                        isSyncing = isSyncing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        } else {
            item {
                EmptyConnectedDevicesCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
        
        // Add device section
        item {
            SectionHeader(
                title = stringResource(R.string.add_device),
                subtitle = stringResource(R.string.scan_for_nearby_devices),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        
        item {
            AddDeviceCard(
                onAddDeviceClick = onAddDeviceClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        
        // Troubleshooting section
        item {
            SectionHeader(
                title = stringResource(R.string.troubleshooting),
                subtitle = stringResource(R.string.connection_issues_help),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        
        item {
            TroubleshootingCard(
                onTroubleshootingClick = onTroubleshootingClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

/**
 * Empty state content when no devices are available
 */
@Composable
private fun EmptyDevicesContent(
    bluetoothEnabled: Boolean,
    locationEnabled: Boolean,
    onAddDeviceClick: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onEnableLocation: () -> Unit,
    onTroubleshootingClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Permissions warnings if needed
        if (!bluetoothEnabled || !locationEnabled) {
            PermissionsWarningCard(
                bluetoothEnabled = bluetoothEnabled,
                locationEnabled = locationEnabled,
                onEnableBluetooth = onEnableBluetooth,
                onEnableLocation = onEnableLocation,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
        
        // Empty state icon and message
        Icon(
            imageVector = Icons.Outlined.Watch,
            contentDescription = null,
            modifier = Modifier
                .size(120.dp)
                .padding(bottom = 24.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        
        Text(
            text = stringResource(R.string.no_devices_connected),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = stringResource(R.string.add_device_instructions),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Add device button
        Button(
            onClick = onAddDeviceClick,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(56.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(text = stringResource(R.string.add_device))
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Troubleshooting link
        TextButton(
            onClick = onTroubleshootingClick
        ) {
            Icon(
                imageVector = Icons.Default.Help,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(R.string.troubleshooting))
        }
    }
}

/**
 * Header for the device management screen
 */
@Composable
private fun DeviceManagementHeader(
    connectedDeviceCount: Int,
    lastSyncTime: LocalDateTime?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .padding(16.dp)
    ) {
        // Title
        Text(
            text = stringResource(R.string.device_management),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onPrimary
        )
        
        // Subtitle with connected device count
        Text(
            text = if (connectedDeviceCount == 0) {
                stringResource(R.string.no_devices_connected_subtitle)
            } else {
                stringResource(
                    R.string.connected_devices_count,
                    connectedDeviceCount,
                    if (connectedDeviceCount == 1) "" else "s"
                )
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
        )
        
        // Last sync time if available
        if (lastSyncTime != null) {
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
                
                Spacer(modifier = Modifier.width(4.dp))
                
                Text(
                    text = stringResource(
                        R.string.last_synced,
                        lastSyncTime.formatDateTime()
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
            }
        }
    }
}

/**
 * Card for warnings about required permissions
 */
@Composable
private fun PermissionsWarningCard(
    bluetoothEnabled: Boolean,
    locationEnabled: Boolean,
    onEnableBluetooth: () -> Unit,
    onEnableLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.required_permissions),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (!bluetoothEnabled) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.BluetoothDisabled,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Text(
                        text = stringResource(R.string.bluetooth_required),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    
                    TextButton(
                        onClick = onEnableBluetooth,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Text(text = stringResource(R.string.enable))
                    }
                }
            }
            
            if (!locationEnabled) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Text(
                        text = stringResource(R.string.location_required),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    
                    TextButton(
                        onClick = onEnableLocation,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Text(text = stringResource(R.string.enable))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = stringResource(R.string.permissions_explanation),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Card for connected device
 */
@Composable
private fun ConnectedDeviceCard(
    device: HBandDevice,
    isSyncing: Boolean,
    syncProgress: Float?,
    onClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onForgetClick: () -> Unit,
    onSyncClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier
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
            // Header with device name and menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Device name and model
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Text(
                        text = device.model,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                
                // Menu button
                Box {
                    IconButton(
                        onClick = { showMenu = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.more_options)
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sync)) },
                            onClick = {
                                onSyncClick()
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null
                                )
                            }
                        )
                        
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.disconnect)) },
                            onClick = {
                                onDisconnectClick()
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.LinkOff,
                                    contentDescription = null
                                )
                            }
                        )
                        
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.forget)) },
                            onClick = {
                                onForgetClick()
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Device info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Connection status
                ConnectionStatusIndicator(
                    connected = device.isConnected,
                    modifier = Modifier.padding(end = 16.dp)
                )
                
                // Battery level
                BatteryLevelIndicator(
                    level = device.batteryLevel,
                    charging = device.isCharging,
                    modifier = Modifier.padding(end = 16.dp)
                )
                
                // Last sync time
                if (device.lastSyncTime != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        Text(
                            text = device.lastSyncTime.formatDateTime(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
            
            // Sync progress if syncing
            if (isSyncing && syncProgress != null) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.syncing_data),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Text(
                            text = "${(syncProgress * 100).roundToInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LinearProgressIndicator(
                        progress = { syncProgress },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
                }
            } else {
                // Sync button if not syncing
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onSyncClick,
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(text = stringResource(R.string.sync_now))
                }
            }
        }
    }
}

/**
 * Empty connected devices card
 */
@Composable
private fun EmptyConnectedDevicesCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
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
                    text = stringResource(R.string.no_connected_devices),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

/**
 * Button to sync all devices
 */
@Composable
private fun SyncAllDevicesButton(
    onSyncAllDevices: () -> Unit,
    isSyncing: Boolean,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onSyncAllDevices,
        modifier = modifier,
        enabled = !isSyncing,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Icon(
            imageVector = Icons.Default.SyncAlt,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(text = stringResource(R.string.sync_all_devices))
    }
}

/**
 * Card for adding a new device
 */
@Composable
private fun AddDeviceCard(
    onAddDeviceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable(onClick = onAddDeviceClick),
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
            // Add icon with background
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Text
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = stringResource(R.string.add_new_device),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = stringResource(R.string.scan_for_nearby_devices_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            // Arrow icon
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Card for troubleshooting
 */
@Composable
private fun TroubleshootingCard(
    onTroubleshootingClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable(onClick = onTroubleshootingClick),
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
            // Help icon with background
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Help,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Text
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = stringResource(R.string.troubleshooting),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = stringResource(R.string.troubleshooting_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            // Arrow icon
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

/**
 * Dialog for scanning for devices
 */
@Composable
private fun DeviceScanDialog(
    availableDevices: List<HBandDevice>,
    isScanning: Boolean,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onConnectDevice: (HBandDevice) -> Unit,
    onDismiss: () -> Unit
) {
    // Start scanning automatically when dialog opens
    LaunchedEffect(Unit) {
        onStartScan()
    }
    
    // Stop scanning when dialog closes
    DisposableEffect(Unit) {
        onDispose {
            onStopScan()
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.scan_for_devices))
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
            ) {
                // Scanning animation
                if (isScanning) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BleScanningAnimation()
                    }
                    
                    Text(
                        text = stringResource(R.string.scanning_for_devices),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Available devices list
                if (availableDevices.isEmpty()) {
                    if (!isScanning) {
                        Text(
                            text = stringResource(R.string.no_devices_found),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.available_devices),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LazyColumn {
                        items(availableDevices) { device ->
                            AvailableDeviceItem(
                                device = device,
                                onConnectClick = { onConnectDevice(device) }
                            )
                            
                            Divider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                thickness = 1.dp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss
            ) {
                Text(text = stringResource(R.string.done))
            }
        },
        dismissButton = {
            TextButton(
                onClick = if (isScanning) onStopScan else onStartScan
            ) {
                Text(
                    text = if (isScanning) 
                        stringResource(R.string.stop_scanning) 
                    else 
                        stringResource(R.string.start_scanning)
                )
            }
        }
    )
}

/**
 * Item for an available device in the scan list
 */
@Composable
private fun AvailableDeviceItem(
    device: HBandDevice,
    onConnectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Device icon
        Icon(
            imageVector = Icons.Outlined.Watch,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Device info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = device.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = device.deviceId,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        
        // Connect button
        TextButton(
            onClick = onConnectClick
        ) {
            Text(text = stringResource(R.string.connect))
        }
    }
}

/**
 * Troubleshooting dialog
 */
@Composable
private fun TroubleshootingDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.troubleshooting))
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                TroubleshootingItem(
                    title = stringResource(R.string.device_not_found),
                    description = stringResource(R.string.device_not_found_solution)
                )
                
                Divider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                
                TroubleshootingItem(
                    title = stringResource(R.string.connection_failed),
                    description = stringResource(R.string.connection_failed_solution)
                )
                
                Divider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                
                TroubleshootingItem(
                    title = stringResource(R.string.sync_issues),
                    description = stringResource(R.string.sync_issues_solution)
                )
                
                Divider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                
                TroubleshootingItem(
                    title = stringResource(R.string.battery_draining),
                    description = stringResource(R.string.battery_draining_solution)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss
            ) {
                Text(text = stringResource(R.string.got_it))
            }
        }
    )
}

/**
 * Troubleshooting item for the dialog
 */
@Composable
private fun TroubleshootingItem(
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

/**
 * BLE scanning animation
 */
@Composable
private fun BleScanningAnimation(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ScanningTransition")
    
    // Pulse animation
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseScale"
    )
    
    // Rotation animation
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rotation"
    )
    
    Box(
        modifier = modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Pulse circles
        repeat(3) { index ->
            val delay = index * 0.3f
            val adjustedScale = (pulseScale + delay) % 1f
            
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(adjustedScale * 2f + 0.5f)
                    .alpha((1f - adjustedScale) * 0.6f)
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        shape = CircleShape
                    )
            )
        }
        
        // Center device icon
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .rotate(rotation),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Bluetooth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}
