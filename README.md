# SensaCare Android

SensaCare is a modern, privacy-focused health & fitness application that pairs with **HBand** smart-watches/trackers via **VeepooSDK**.  
It collects vitals (heart-rate, SpO₂, sleep, steps, temperature …), visualises trends, and syncs data to the SensaCare cloud.

---

## ✨ Key Features
* BLE scan / auto-reconnect to HBand devices  
* Real-time vitals: heart-rate, SpO₂, ECG (device capability-dependent)  
* Background data sync using WorkManager (no battery-hogging polling)  
* Local storage with Room + optional cloud backup  
* Material 3 UI with Compose & dark-mode support  
* Rich charts (MPAndroidChart / Vico) & health insights  
* Privacy first – full end-to-end encryption, no advertising SDKs  
* Play-Store compliant permission & Data-Safety declarations

---

## 🏗 Technical Stack
| Layer | Tech |
|-------|------|
| Language | Kotlin 1.9, Coroutines, Flow |
| Build   | Gradle 8.7, AGP 8.4, JDK 17 |
| UI      | Jetpack Compose (Material 3), Navigation-Compose |
| BLE / Device | **VeepooSDK** (`vpbluetooth`, `vpprotocol`), custom Kotlin wrapper |
| DI      | Koin |
| DB      | Room + DataStore |
| BG work | WorkManager + foreground service |
| Charts  | MPAndroidChart, Vico |
| Logging | Timber |
| Tests   | JUnit5, MockK, Turbine, Espresso, Robolectric |
| CI      | GitHub Actions (lint ➔ unit ➔ instrumentation ➔ assembleRelease) |

**Minimum SDK:** 24    **Target SDK:** 34

---

## 📦 VeepooSDK Integration
SDK binaries live in `Devices/` and are copied into the `:veepoo-sdk` Gradle module:

```
Devices/
 ├─ vpbluetooth_3.5.0.jar
 ├─ vpprotocol_2.1.20.aar
 ├─ gson-2.10.jar
 └─ (optional) libble-0.4.aar, libcomx-0.3.jar
```

`VeepooSdkManager` wraps the low-level callbacks with suspend/Flow APIs:

```kotlin
// Initialise once in Application
VeepooSdkManager.initialize(context, enableLogging = BuildConfig.DEBUG)

// Scan & connect (example)
lifecycleScope.launch {
    VeepooSdkManager.scanForDevices().first().let { device ->
        VeepooSdkManager.connectToDevice(device.macAddress)
    }
}
```

Runtime permissions handled via Accompanist-Permissions (Android ≤30: Location; Android 12+: `BLUETOOTH_SCAN|CONNECT`).

---

## ⚙️ Build & Setup

Prerequisites  
1. **JDK 17**  
2. **Android Studio Hedgehog (2023.3) or newer**  
3. Android SDK platforms 24–34 + Google USB driver

Steps  
```bash
git clone https://github.com/YourOrg/sensacare-android.git
cd sensacare-android
./gradlew clean assembleDebug
```

First build downloads the 8.7 Gradle distribution (~180 MB).

---

## 🚀 Running the App

1. Enable Bluetooth on the device/emulator (BLE not supported on emulator).  
2. Grant permissions when prompted.  
3. Tap **Pair Device** ➔ select your *HB-xxx* watch.  
4. Data sync starts automatically; navigate to **Dashboard** for charts.

---

## 🧪 Testing

### Local Unit Tests
```
./gradlew testStandardDebugUnitTest
```

### Instrumentation / UI
```
./gradlew connectedStandardDebugAndroidTest
```

### Continuous Integration
GitHub Actions workflow `.github/workflows/android.yml`:

1. `./gradlew lint ktlint detekt`
2. `./gradlew test`
3. `./gradlew connectedAndroidTest` (Firebase Test Lab recommended)
4. `./gradlew bundleStandardRelease`  
   On success, artifacts are uploaded to the internal Play track.

---

## 📦 Deployment

1. Set up **Play App-Signing**; store `keystore.properties` in secure secrets.  
2. Run `./gradlew bundleStandardRelease` or let CI produce the bundle.  
3. Upload to **Play Console → Internal testing**.  
4. Fill Data-Safety / Bluetooth permission declarations:  
   *Data collected: health vitals (encrypted), not shared.*  
5. After QA, promote to production.

---

## 🤝 Contributing

1. Fork, create feature branch (`feat/awesome-thing`)  
2. Follow **Kotlin Style Guide** & run `./gradlew ktlintFormat`  
3. Ensure **all tests pass**  
4. Submit Pull Request – describe context & screenshots/GIFs  
5. One aproval + CI green ✅ → merge (squash)  

We follow Conventional Commits and semantic versioning.

---

## 📜 License
```
MIT License
Copyright (c) 2025 SensaCare

Permission is hereby granted, free of charge, to any person obtaining a copy
...
```
(See full text in [`LICENSE`](LICENSE).)

Happy building ❤️‍🩹
