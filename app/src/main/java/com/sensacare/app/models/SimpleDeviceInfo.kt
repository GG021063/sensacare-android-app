package com.sensacare.app.models

/**
 * SimpleDeviceInfo
 * 
 * A simplified data class to store Bluetooth device information.
 * This replaces the VeePoo SDK's DeviceInfo class to reduce dependencies.
 */
data class SimpleDeviceInfo(
    // The name of the Bluetooth device (can be null for unnamed devices)
    val deviceName: String = "Unknown Device",
    
    // The MAC address of the Bluetooth device (required for connection)
    val deviceAddress: String,
    
    // The Received Signal Strength Indicator (RSSI) value
    var deviceRssi: Int = 0,
    
    // Connection status flag
    var isConnected: Boolean = false
) {
    // Helper method to get a display name (never null)
    fun getDisplayName(): String {
        return deviceName.ifEmpty { "Unknown Device" }
    }
    
    // Helper method to format RSSI for display
    fun getRssiDisplay(): String {
        return "RSSI: $deviceRssi dBm"
    }
    
    // Check if this represents the same physical device as another SimpleDeviceInfo
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SimpleDeviceInfo) return false
        return deviceAddress == other.deviceAddress
    }
    
    override fun hashCode(): Int {
        return deviceAddress.hashCode()
    }
}
