# Supabase Database Setup Guide

## 🚀 Quick Setup Instructions

### Step 1: Access Your Supabase Project
1. Go to https://supabase.com/dashboard
2. Select your `sensacare-pwa-app` project
3. Navigate to **SQL Editor** in the left sidebar

### Step 2: Run the Database Setup
1. Copy the entire contents of `supabase-setup.sql`
2. Paste it into the SQL Editor
3. Click **Run** to execute the script

### Step 3: Verify Setup
After running the script, you should see:
- ✅ All tables created successfully
- ✅ Enums and types created
- ✅ Indexes and triggers set up
- ✅ RLS policies configured

### Step 4: Test the Setup
1. Go to **Table Editor** in Supabase
2. You should see all the tables listed:
   - `user_profiles`
   - `user_preferences`
   - `health_goals`
   - `connected_devices`
   - `daily_health_summaries`
   - `heart_rate_data`
   - `hrv_data`
   - `spo2_data`
   - `respiration_data`
   - `temperature_data`
   - `sleep_sessions`
   - `sleep_stages`
   - `activity_sessions`
   - `gps_data`
   - `steps_data`
   - `hourly_steps`
   - `mood_entries`
   - `meditation_sessions`
   - `menstrual_cycles`
   - `menstrual_symptoms`
   - `notifications`
   - `device_sync_logs`
   - `health_insights`

## 🔧 Manual Setup (Alternative)

If you prefer to run commands individually, here are the key steps:

### 1. Create Enums
```sql
CREATE TYPE device_type AS ENUM ('ring', 'watch', 'band', 'patch', 'clip', 'other');
CREATE TYPE connection_status AS ENUM ('disconnected', 'connecting', 'connected', 'syncing', 'error');
-- ... (see full script for all enums)
```

### 2. Create Core Tables
```sql
-- User profiles table
CREATE TABLE user_profiles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    auth_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    first_name TEXT NOT NULL,
    last_name TEXT NOT NULL,
    email TEXT NOT NULL UNIQUE,
    -- ... (see full script for complete schema)
);
```

### 3. Enable RLS
```sql
ALTER TABLE user_profiles ENABLE ROW LEVEL SECURITY;
-- ... (enable for all tables)
```

### 4. Create Policies
```sql
CREATE POLICY "Users can view own profile" ON user_profiles FOR SELECT USING (auth.uid() = auth_id);
-- ... (create policies for all tables)
```

## 🧪 Testing the Setup

### Test User Creation
1. Go to **Authentication** → **Users**
2. Create a test user or sign up through your app
3. Check that a profile is automatically created in `user_profiles`

### Test Data Insertion
1. Go to **Table Editor**
2. Select any table (e.g., `user_profiles`)
3. Try inserting a test record
4. Verify RLS policies work correctly

## 🔑 Environment Variables

After setup, ensure these are set in Vercel:
- `VITE_SUPABASE_URL` = Your Supabase project URL
- `VITE_SUPABASE_ANON_KEY` = Your Supabase anon key
- `VITE_USE_MOCK_DATA` = false

## 🚨 Troubleshooting

### Common Issues:
1. **Permission Denied**: Make sure you're using the service role key for admin operations
2. **RLS Errors**: Verify all tables have RLS policies
3. **Foreign Key Errors**: Ensure referenced tables exist before creating relationships

### Reset Database (if needed):
```sql
-- Drop all tables (DANGER - will delete all data)
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO public;
```

## 📊 Database Schema Overview

The database includes:
- **User Management**: Profiles, preferences, authentication
- **Device Management**: Connected devices, sync logs
- **Health Data**: Heart rate, HRV, SpO2, temperature, respiration
- **Activity Tracking**: Steps, workouts, GPS data
- **Sleep Analysis**: Sleep sessions, stages, metrics
- **Wellness Features**: Mood tracking, meditation, menstrual cycles
- **Insights**: Health insights, notifications

## 🔐 Security Features

- **Row Level Security (RLS)**: Users can only access their own data
- **Authentication Integration**: Automatic profile creation on signup
- **Data Validation**: Constraints and checks on all tables
- **Audit Trails**: Created/updated timestamps on all records 