# Environment Variables Setup Guide

## 🎯 **CRITICAL FOR PRODUCTION**

### **Required Environment Variables (Must Set in Vercel)**

```bash
# Supabase Configuration
VITE_SUPABASE_URL=your_supabase_project_url
VITE_SUPABASE_ANON_KEY=your_supabase_anon_key

# Production Mode
VITE_USE_MOCK_DATA=false

# App Configuration
VITE_APP_NAME=Sensacare Mobile App
VITE_APP_VERSION=1.0.0
VITE_APP_DESCRIPTION=Health tracking app for wearable devices
VITE_APP_URL=https://mobile.sensacare.health

# PWA Configuration
VITE_PWA_ENABLED=true
VITE_PWA_CACHE_VERSION=v1

# Device Connectivity
VITE_BLE_SCAN_TIMEOUT=10000
VITE_BLE_RECONNECT_ATTEMPTS=3
VITE_DATA_SYNC_INTERVAL=300000

# API Configuration
VITE_API_TIMEOUT=30000
VITE_API_RETRY_ATTEMPTS=3

# Security & Compliance
VITE_ENCRYPT_HEALTH_DATA=true
VITE_HIPAA_COMPLIANCE_MODE=true
VITE_DATA_RETENTION_DAYS=2555

# Features
VITE_ENABLE_ANALYTICS=true
VITE_ENABLE_NOTIFICATIONS=true
VITE_ENABLE_BIOMETRIC_LOGIN=false

# Debug & Logging
VITE_DEBUG_MODE=false
VITE_LOG_LEVEL=info
VITE_SHOW_DEV_TOOLS=false
```

## 🔧 **Setup Instructions**

### **1. Vercel Dashboard Setup**
1. Go to your Vercel project dashboard
2. Navigate to Settings → Environment Variables
3. Add each variable above with appropriate values

### **2. Supabase Setup**
1. Create a Supabase project at https://supabase.com
2. Get your project URL and anon key from Settings → API
3. Set up your database schema (see database.types.ts)

### **3. Production vs Development**
- **Development**: Use mock data (`VITE_USE_MOCK_DATA=true`)
- **Production**: Use real Supabase (`VITE_USE_MOCK_DATA=false`)

## 🚨 **Security Notes**
- Never commit real environment variables to git
- Use Vercel's environment variable encryption
- Rotate keys regularly
- Monitor API usage

## 📱 **Device Testing Requirements**
- HTTPS required for Bluetooth API
- PWA must be installed for device permissions
- User must grant Bluetooth permissions 