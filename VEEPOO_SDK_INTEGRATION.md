# VeePoo SDK Integration Guide  
_Sensacare Android – July 2025_

---

## 1  Solution Architecture

```
┌──────────────────────┐
│  UI  (Activities)    │           ┌───────────────────┐
│  ─ MainActivity      │  uses     │ ConnectionManager │
│  ─ HomeActivity      ├──────────►│  (high-level API) │
│  ─ …                 │           └────────┬──────────┘
└──────────────────────┘                    │delegates
                                            ▼
                                   ┌──────────────────────┐
                                   │ VeePooDeviceManager  │
                                   │  (low-level SDK)     │
                                   └────────┬─────────────┘
                                            │calls (reflect./future SDK)
                                            ▼
                                   ┌──────────────────────┐
                                   │   VeePoo SDK libs    │
                                   └──────────────────────┘
```

* **ConnectionManager** – single façade exposed to the rest of the app.  
  * Keeps persistent connection state (SharedPreferences).  
  * Performs high-level validation: Bluetooth on, device paired, heartbeat timeout.  
  * Periodic verification every 5 s.  
* **VeePooDeviceManager** – contains all SDK-specific work:  
  * Scanning, connecting, data listeners, disconnect & heartbeat.  
  * Wrapped with reflection so code compiles even if method signatures change.  
  * Provides simulation mode when a method is missing.  
* **Flow**  
  1. UI requests scan ⇢ ConnectionManager ⇢ VeePooDeviceManager.startScan().  
  2. User picks device ⇢ connectDevice() chain.  
  3. DeviceManager raises callbacks (`onConnected`, `onDataReceived` …).  
  4. ConnectionManager updates timestamps and propagates to UI.

---

## 2  Supported Devices

| Family            | Prefixes recognised | Notes                                    |
|-------------------|---------------------|-------------------------------------------|
| ET4922 series     | `ET492`             | needs longer connection timeout (30 s).   |
| ET593 series      | `ET593`             | standard timeout (15 s).                  |
| Other HBand/VPB   | `ET`, `ID`, `VPB`, `HBand` | Generic handling.                       |

The prefix filter lives in both managers for fast identification; behaviour differences (e.g. time-outs, retries) sit inside VeePooDeviceManager.

---

## 3  Error Handling & Fallbacks

1. **Reflection guarded calls** – every SDK invocation is discovered at run-time.  
   * If a method or class is missing ➜ log warning ➜ fall back to simulation or alternative call signature.  
2. **Timeouts**  
   * Scan timeout 10 s; connection timeout 15 s (30 s for ET492x).  
   * Heartbeat verifies data every 5 s; if no data in 30 s ➜ disconnect.  
3. **State flags (AtomicBoolean)** to prevent double scans / connections.  
4. Any exception immediately:  
   * Logs → `Log.e` with full stack.  
   * Cleans internal flags.  
   * Notifies UI via callback failure.

---

## 4  Extending with Real SDK Calls

The code was organised so **only three areas** need to be swapped from simulation to real calls:

| Area | Current Implementation | What to replace with |
|------|-----------------------|----------------------|
| Scanning | `simulateScanResults()` | Call `VPOperateManager.startScanDevice(…)` and forward results. |
| Connection | `simulateConnection()` | Invoke `connectDevice()` overload that matches your SDK JAR; supply `IConnectResponse` + `INotifyResponse`. |
| Data listeners | `startSimulatedDataUpdates()` | Register real listeners:<br>`readHeartData`, `readBloodOxygen`, `readBloodPressure`, `readSleepData`, `readStepData` etc. |

Because the reflection helpers (`findMethod`, `findClassByNameSuffix` …) already exist, you can progressively turn simulations off:

```kotlin
// 1. Try real SDK call
val method = findMethod(vpOperateManager.javaClass,
                        "startScanDevice",
                        SearchRequest::class.java,
                        SearchResponse::class.java)
if (method != null) {
    // real path
} else {
    simulateScanResults(callback)
}
```

Once verified on real hardware simply delete the simulation branch.

---

## 5  Reflection-based Compatibility

Why reflection?

* The public AARs differ across VeePoo releases (parameter count, package moves).  
* Reflection lets us compile without hard dependencies, yet still access classes if present at runtime.  
* Helpers provided:  
  * `findMethod` / `findMethodByNamePrefix` – tolerant lookup.  
  * `findClassByNameSuffix`, `findFieldByNameSuffix` – dynamic model access.  
* All reflection failures are caught and logged; they never crash the app.

---

## 6  Simulated vs Real Connections

* **Simulated mode (default)** – activates when required SDK symbols are missing or running on emulator:  
  * Generates fake heart-rate, SpO₂, BP, steps, sleep every 5 s.  
  * Lets QA test UI without hardware.  
* **Real mode** – once a method is found, simulation is bypassed.  
* Toggle detection lives inside each feature block; mixing is allowed (e.g. real HR but simulated sleep if that endpoint missing).

---

## 7  Data Flow & Callbacks

```
VeePoo hardware
     │  notify
     ▼
VeePoo SDK (BLE)
     │  listener
     ▼
VeePooDeviceManager
     │  onDataReceived(type,value)
     ▼
ConnectionManager.notifyDataReceived()
     │  updates timestamps
     ▼
UI (HomeActivity, Charts, …)
```

Callback interfaces:

```kotlin
interface ScanCallback {
    fun onScanResult(devices: List<DeviceInfo>)
    fun onScanFinished()
    fun onScanError(msg: String)
}

interface ConnectionCallback {
    fun onConnected(device: DeviceInfo)
    fun onDisconnected()
    fun onConnectionFailed(msg: String)
    fun onDataReceived(dataType: String, value: String)
}
```

---

## 8  Next Steps for Production

1. **Bring in latest VeePoo AAR/JAR**  
   * Copy to `app/libs/` and update `build.gradle`.
2. **Gradually replace simulations** following §4.  
3. **Device certification**  
   * Test ET4922 & ET593 on multiple firmware versions.  
   * Validate data accuracy and frequency.
4. **Expand Function Support Matrix**  
   * Replace stub in `ConnectionManager.isDeviceSupportFunction()`.  
5. **Power & reconnect optimisation**  
   * Implement background reconnection, Android 12 BT-permissions workflow.  
6. **Security review**  
   * Ensure no reflection leaks stack-traces in release builds.  
   * Add ProGuard rules if minification enabled.
7. **CI pipeline**  
   * Unit-test simulation path.  
   * Instrumentation tests with mocked BLE layer.

---

### Contact

For questions ping _mobile@your-team.io_ or open an issue in the `sensacare-android` repository.  
Happy coding! 🎉
