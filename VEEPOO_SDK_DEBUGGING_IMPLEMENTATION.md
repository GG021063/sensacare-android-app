# VeePoo SDK — Debugging Implementation Guide  
_Sensacare Android App – July 2025_

---

## 1  Overview

The new “debugging layer” added to `VeePooDeviceManager.kt` turns the previous integration scaffold into a traceable, verifiable bridge between the app and any VeePoo/HBand device (ET 4922, ET 593, etc.).

Core aims:

• Remove all silent fall-backs so that **real SDK paths are attempted first**.  
• Surface every reflective call, success or failure, through structured logs.  
• Allow simulation as a last resort (toggle via `FORCE_REAL_SDK_CALLS`).  
• Provide a single place to monitor connection life-cycle, listener registration, and real-time data flow.

---

## 2  Key Debugging Features Added

1. **`FORCE_REAL_SDK_CALLS` Flag**  
   • When `true` (default) simulation is _disabled_; any missing SDK method results in an error and the UI is notified.  
   • Set to `false` during UI development to keep fake data.

2. **Deep Reflection Checks**  
   • `findMethodByNamePrefix` and `findFieldByNameSuffix` verify method/field existence before invocation.  
   • Class lookup helper `findClassByNameSuffix` searches common VeePoo packages.

3. **Verbose Logging**  
   • Tagged `VeePooDeviceManager` with emoji-free, grep-friendly lines.  
   • Every public call (`startScan`, `connectDevice`, etc.) prints intent, parameters, and result.  
   • Each callback (`heartDataCallback`, `connectResponse`, …) logs both raw input and derived values.

4. **Dynamic Listener Registration**  
   • After successful connect, `registerDataListeners()` attempts to attach *all* known listeners (heart, SPO2, BP, steps, sleep, temp, HRV, real-time).  
   • Missing methods are reported but do not crash the app.

5. **Heartbeat & Data Timeout Watchdog**  
   • `verifyConnection()` runs every 5 s.  
   • If no data for 30 s or the SDK reports `isConnected == false` the manager auto-disconnects and raises a UI event.

6. **Connection Response Compatibility**  
   • Handles both `connectState(state,newState)` and `connectState(state,profile,newState)` signatures.  
   • Hard-coded constant `DISCONNECT_STATE = 3` used when the enum class is not available.

---

## 3  Logging Details

Log tag: **`VeePooDeviceManager`**

Typical flow:

1. Initialization  
   D/VeePooDeviceManager : Initializing…  
   D/VeePooDeviceManager : Loaded SDK class = com.veepoo.protocol.VPOperateManager  

2. Scan start  
   D/VeePooDeviceManager : Starting scan for VeePoo devices  
   D/VeePooDeviceManager : Using real SDK scan method  

3. Device found  
   D/VeePooDeviceManager : Device found: ET593 … RSSI -65  

4. Connection  
   D/VeePooDeviceManager : Connecting to device: ET593 (AA:BB:CC)  
   D/VeePooDeviceManager : Using real SDK connect method  

5. Listener registration  
   D/VeePooDeviceManager : Registering data listeners  
   D/VeePooDeviceManager : Heart rate listener registered successfully  
   … (one line per listener) …

6. Live data  
   D/VeePooDeviceManager : Heart rate data received: 72 BPM  
   D/VeePooDeviceManager : Temperature data received: 36.8 °C  

7. Heartbeat issues  
   W/VeePooDeviceManager : No data received for 30000 ms, disconnecting  
   or  
   W/VeePooDeviceManager : SDK reports disconnected state, disconnecting  

All errors include stack traces for fast triage.

---

## 4  Real SDK Verification Methods

Method                            | Purpose
---------------------------------|--------
`initializeSdk()`                 | Runs once; logs SDK version and presence of core methods.
`findMethod(...)` / `findMethodByNamePrefix(...)` | Runtime discovery, returns `null` if absent (no crash).
`registerDataListeners()`         | Attempts each `read*` method; success/failure logged individually.
`syncDeviceTime()`                | Invoked right after connect; confirms `syncTime()` is callable.
`verifyConnection()` (heartbeat)  | Uses `isConnected()` reflection every 5 s to verify link.

---

## 5  Data Listener Registrations

Listener (object)        | SDK method searched          | Data channel emitted to UI
-------------------------|------------------------------|---------------------------
`heartDataCallback`      | `readHeartData*` or prefix `readHeart` | `heartRate`
`spo2Callback`           | `readBloodOxygen*`           | `bloodOxygen`
`bpCallback`             | `readBloodPressure*`         | `bloodPressure`
`stepCallback`           | `readStep*`                  | `steps`
`sleepCallback`          | `readSleep*`                 | `sleep`
`temperatureCallback`    | `readTemptureData*` or prefix `readTemp` | `temperature`
`hrvCallback`            | `readHRVOriginData*`         | `hrv`
`realTimeCallback`       | `setRealTimeDataListener*`   | connection status

If the SDK lacks any of these methods the log will state  
E/VeePooDeviceManager : Failed to find readTemptureData() method

---

## 6  Interpreting the Debug Logs

1. **Green path** – All listeners registered, data flowing:  
   • Look for “… listener registered successfully” lines.  
   • Continuous “… data received …” lines every few seconds.

2. **Missing feature** – Method not found:  
   • Error line appears once per listener during registration.  
   • No data for that metric, app UI will show placeholders.

3. **Reflection crash** – Stack trace with `invoke` exception:  
   • Check parameter signature mismatch.  
   • Update `findMethodByNamePrefix` or provide alternative signature.

4. **Heartbeat disconnect** – Watchdog triggers:  
   • Data stopped; verify device still in range & connected.

Use Android Studio Logcat with filter `tag:VeePooDeviceManager` for a clean stream.

---

## 7  Next Steps for Hardware Testing

1. Build **debug** flavour with `FORCE_REAL_SDK_CALLS = true`.  
2. Enable Bluetooth & location on the test phone.  
3. Install and run the app, start scan:  
   • Expect to see raw scan logs and device list in UI.  
4. Connect to real ET 492/593 device.  
5. Observe logcat: confirm each listener registration succeeds.  
6. Move through app screens (HRV, Temperature…) and verify live values change.  
7. Simulate edge cases:  
   • Walk out of range → heartbeat disconnect should trigger within ~30 s.  
   • Power-off watch → `connectState` should log state change.

---

## 8  Integration Guide for Real Devices

1. **Device Firmware**  
   • Ensure the VeePoo device runs latest firmware supporting _Real-time Data_ and _HRV_.  
   • Some budget variants ship with HRV disabled; confirm with vendor.

2. **Permissions**  
   • Android 12+ requires `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`, `BLUETOOTH_ADVERTISE` runtime grants.  
   • Location permission must be granted for scanning.

3. **Pairing**  
   • Device **must be bonded** at OS level before calling `connectDevice()`.  
   • Bonding dialog appears automatically on first connect.

4. **Step-by-step Integration**  
   a. Call `VeePooDeviceManager.getInstance(context)` early (Application.onCreate).  
   b. Start scan → display results in dialog.  
   c. User selects device → pass address/name to `connectDevice()`.  
   d. Use `ConnectionCallback` to update UI connection status.  
   e. On `onDataReceived`, route metrics to appropriate view-models.  
   f. On `onDisconnected`, revert UI to idle state.

5. **Feature Availability**  
   • Call `isDeviceSupportFunction("temperature")` et al. after connect to hide unsupported cards.

6. **Time Sync**  
   • `syncDeviceTime()` is executed automatically; no extra work required.

7. **Battery / DND**  
   • For long-running services whitelist the app from battery optimization.

8. **Release Builds**  
   • Set `FORCE_REAL_SDK_CALLS = true`.  
   • Optionally reduce log level to `INFO` or remove debug logs with ProGuard.

---

_End of document_
