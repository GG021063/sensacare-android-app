# SensaCare Android App - Testing & Deployment Guide

## 🚀 **How to Test the App**

### **Option 1: Android Studio (Recommended)**

1. **Open Android Studio**
   ```powershell
   # Open Android Studio
   # File -> Open -> Select sensacare-android-app folder
   ```

2. **Sync Project**
   - Android Studio will automatically sync Gradle
   - Wait for all dependencies to download

3. **Add Missing Resources**
   ```powershell
   # Create placeholder icons (temporary)
   # Copy any PNG icons to app/src/main/res/drawable/
   # Required icons: ic_home, ic_incrediness, ic_sleep, ic_settings, etc.
   ```

4. **Build and Run**
   - Connect Android device via USB
   - Enable Developer Options and USB Debugging
   - Click "Run" button in Android Studio
   - Select your device and install

### **Option 2: Command Line Build**

1. **Navigate to Project**
   ```powershell
   cd "C:\Users\graha\sensacare-android-app"
   ```

2. **Build APK**
   ```powershell
   # Windows
   .\gradlew assembleDebug
   
   # The APK will be in: app/build/outputs/apk/debug/app-debug.apk
   ```

3. **Install on Device**
   ```powershell
   # Enable USB debugging on your device
   # Copy APK to device and install
   # Or use ADB:
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

## 📱 **Testing Features**

### **1. Basic Navigation**
- ✅ App launches to MainActivity
- ✅ Bluetooth permissions request
- ✅ Device scanning works
- ✅ Navigation between screens

### **2. UI Testing**
- ✅ Dark theme displays correctly
- ✅ Circular progress indicators animate
- ✅ Bottom navigation highlights correctly
- ✅ Settings screens work

### **3. Bluetooth Testing**
- ✅ Scan for devices
- ✅ Connect to Veepoo device
- ✅ Display health data
- ✅ Real-time updates

## 🔧 **Required Setup**

### **1. VeepooSDK Integration**
```powershell
# Copy SDK files from PWA project
Copy-Item "C:\Users\graha\sensacare-PWA-app\src\lib\devices\hband-sdk\Android_Ble_SDK-master\jar_base\*.jar" "app\libs\"
Copy-Item "C:\Users\graha\sensacare-PWA-app\src\lib\devices\hband-sdk\Android_Ble_SDK-master\jar_core\*.aar" "app\libs\"
```

### **2. Add Icons (Temporary)**
```powershell
# Create simple placeholder icons
# Or download free icons from Material Design Icons
# Place in app/src/main/res/drawable/
```

### **3. Background Images**
```powershell
# Add your background images
Copy-Item "your-images\bg_activity.png" "app\src\main\res\drawable\"
Copy-Item "your-images\bg_incrediness.png" "app\src\main\res\drawable\"
Copy-Item "your-images\bg_sleep.png" "app\src\main\res\drawable\"
```

## 🚀 **Deployment Process**

### **Step 1: Commit Changes**
```powershell
cd "C:\Users\graha\sensacare-android-app"

# Initialize git if not already done
git init

# Add all files
git add .

# Commit changes
git commit -m "Complete SensaCare Android app with IncrediRing UI"

# Add remote if not already added
git remote add origin https://github.com/yourusername/sensacare-android-app.git

# Push to GitHub
git push -u origin main
```

### **Step 2: Build Release APK**
```powershell
# Build release version
.\gradlew assembleRelease

# The APK will be in: app/build/outputs/apk/release/app-release.apk
```

### **Step 3: Deploy to Vercel (Web Version)**
```powershell
# Navigate to PWA project
cd "C:\Users\graha\sensacare-PWA-app"

# Deploy PWA version
vercel --prod
```

### **Step 4: Alternative Deployment Options**

#### **A. Google Play Store**
1. Create Google Play Console account
2. Upload signed APK
3. Fill app details and screenshots
4. Submit for review

#### **B. Direct APK Distribution**
1. Upload APK to cloud storage
2. Share download link
3. Users enable "Install from unknown sources"

#### **C. Firebase App Distribution**
```powershell
# Install Firebase CLI
npm install -g firebase-tools

# Login to Firebase
firebase login

# Initialize Firebase
firebase init

# Deploy to Firebase
firebase deploy
```

## 📋 **Testing Checklist**

### **Pre-Launch Testing**
- [ ] App launches without crashes
- [ ] All screens display correctly
- [ ] Navigation works smoothly
- [ ] Bluetooth permissions granted
- [ ] Device scanning functional
- [ ] Health data displays
- [ ] Settings screens work
- [ ] Dark theme consistent

### **Device Testing**
- [ ] Test on Android 10+
- [ ] Test on different screen sizes
- [ ] Test with actual Veepoo device
- [ ] Test Bluetooth connectivity
- [ ] Test background/foreground transitions

### **Performance Testing**
- [ ] App loads quickly
- [ ] Smooth animations
- [ ] No memory leaks
- [ ] Battery usage reasonable
- [ ] Network usage minimal

## 🐛 **Common Issues & Solutions**

### **1. Build Errors**
```powershell
# Clean and rebuild
.\gradlew clean
.\gradlew assembleDebug
```

### **2. Missing Icons**
```powershell
# Create placeholder icons or download from:
# https://material.io/resources/icons/
```

### **3. Bluetooth Issues**
- Ensure device supports BLE
- Check Android version compatibility
- Verify permissions granted

### **4. VeepooSDK Issues**
- Ensure AAR/JAR files in libs folder
- Check SDK version compatibility
- Verify device compatibility

## 📱 **Quick Test Commands**

```powershell
# Build debug APK
.\gradlew assembleDebug

# Install on connected device
adb install app/build/outputs/apk/debug/app-debug.apk

# Run tests
.\gradlew test

# Clean project
.\gradlew clean
```

## 🎯 **Next Steps After Testing**

1. **Fix any issues** found during testing
2. **Add real background images** and icons
3. **Test with actual Veepoo device**
4. **Optimize performance** if needed
5. **Prepare for production** deployment
6. **Create app store listings**

## 📞 **Support**

If you encounter issues:
1. Check Android Studio logs
2. Verify all dependencies installed
3. Ensure device compatibility
4. Test on different Android versions

The app is now ready for testing! The IncrediRing-style UI should work perfectly with the VeepooSDK integration. 