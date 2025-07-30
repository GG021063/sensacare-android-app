# SensaCare Android App

This Android app provides native Bluetooth connectivity to Veepoo health devices using the official Veepoo SDK.

## Features

- **Bluetooth Device Scanning**: Automatically discovers nearby Veepoo health devices
- **Device Connection**: Establishes secure connections with health devices
- **Real-time Status Updates**: Shows connection status and device information
- **Permission Management**: Handles all required Bluetooth and location permissions
- **Error Handling**: Robust error handling with timeouts and user feedback

## Architecture

### Main Components

1. **MainActivity**: Entry point with device scanning and connection logic
2. **DeviceConnectionActivity**: Handles device authentication and setup
3. **DashboardActivity**: Main dashboard for viewing health data
4. **HealthDataActivity**: Detailed health metrics display

### Key Features

- **Veepoo SDK Integration**: Uses official Veepoo protocol for device communication
- **Bluetooth State Management**: Monitors Bluetooth state changes
- **Connection Timeouts**: Prevents hanging connections with 15-second timeouts
- **Scan Timeouts**: 30-second scan timeout to prevent infinite scanning
- **Permission Handling**: Runtime permission requests for Android 6.0+

## Setup Instructions

### Prerequisites

- Android Studio Arctic Fox or later
- Android SDK 21+ (API level 21)
- Target SDK 34 (Android 14)
- Veepoo SDK files (included in project)

### Build Configuration

The app uses the following key dependencies:

```gradle
// Veepoo SDK dependencies
implementation files('../src/lib/devices/hband-sdk/Android_Ble_SDK-master/android_sdk_source/jar_core/vpprotocol-2.3.20.15.aar')
implementation files('../src/lib/devices/hband-sdk/Android_Ble_SDK-master/android_sdk_source/jar_core/vpbluetooth-1.16.aar')
// ... other Veepoo SDK files

// AndroidX and Material Design
implementation 'androidx.core:core-ktx:1.12.0'
implementation 'com.google.android.material:material:1.11.0'
```

### Permissions

The app requires the following permissions:

```xml
<!-- Bluetooth permissions -->
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

## Usage Flow

1. **App Launch**: MainActivity starts and checks Bluetooth permissions
2. **Permission Request**: If needed, requests runtime permissions
3. **Bluetooth Check**: Ensures Bluetooth is enabled
4. **Device Scanning**: User taps "Scan for Devices" to discover nearby devices
5. **Device Selection**: User selects a device from the list
6. **Connection**: App attempts to connect to the selected device
7. **Authentication**: Navigates to DeviceConnectionActivity for device setup
8. **Dashboard**: After successful connection, shows health data dashboard

## Error Handling

### Connection Errors

- **Timeout**: 15-second connection timeout with user notification
- **Failed Connection**: Clear error messages for different failure types
- **Device Out of Range**: Specific message for range issues

### Scanning Errors

- **Scan Timeout**: 30-second timeout with automatic stop
- **Bluetooth Disabled**: Automatic detection and user guidance
- **Permission Denied**: Clear instructions for enabling permissions

## Development Notes

### Veepoo SDK Integration

The app uses the official Veepoo SDK with the following key classes:

- `VPOperateManager`: Main SDK manager for device operations
- `IBleConnectStatusListener`: Connection status callbacks
- `IBleNotifyResponse`: Device data notification handling
- `SearchResponse`: Device discovery callbacks

### Threading

All UI updates are properly handled on the main thread using `runOnUiThread()` to prevent crashes.

### Memory Management

- Proper cleanup in `onDestroy()`
- Broadcast receiver unregistration with exception handling
- Connection and scan timeout management

## Testing

### Manual Testing Checklist

- [ ] Bluetooth permissions granted
- [ ] Bluetooth enabled on device
- [ ] Veepoo device in range and discoverable
- [ ] Device scanning works (30-second timeout)
- [ ] Device connection successful
- [ ] Error handling for failed connections
- [ ] App handles Bluetooth state changes
- [ ] Proper cleanup on app exit

### Debug Features

- Real-time status updates in UI
- Toast messages for user feedback
- Connection state logging
- Error code reporting

## Troubleshooting

### Common Issues

1. **No devices found**: Ensure device is in pairing mode and Bluetooth is enabled
2. **Connection fails**: Check device battery and proximity
3. **Permission errors**: Grant all required permissions in device settings
4. **Scan timeout**: Device may not be discoverable or out of range

### Debug Information

The app provides detailed status messages in the UI to help diagnose issues:
- Bluetooth state changes
- Scan progress and results
- Connection attempts and outcomes
- Error codes and descriptions

## Future Enhancements

- [ ] Background service for continuous device monitoring
- [ ] Data synchronization with cloud services
- [ ] Advanced health metrics visualization
- [ ] Device firmware update support
- [ ] Multi-device support
- [ ] Offline data storage 