package com.sensacare.app.data

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.room.*
import com.sensacare.app.ConnectionManager
import com.veepoo.protocol.VPOperateManager
import com.veepoo.protocol.listener.base.IBleWriteResponse
import kotlinx.coroutines.*
import java.util.*

/**
 * HealthDataManager
 *
 * A comprehensive manager for handling REAL health data from ET492 and ET593 devices.
 * This class is responsible for:
 * - Syncing data from physically connected VeePoo devices
 * - Processing and storing authentic health metrics
 * - Providing access to real health data for UI components
 * 
 * IMPORTANT: This class ONLY processes real data from connected devices.
 * No simulation or fake data is generated under any circumstances.
 */
class HealthDataManager private constructor(private val context: Context) {

    // Room database instance
    private val database: HealthDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            HealthDatabase::class.java,
            "health_database"
        )
            // During active development we allow schema-changing builds to
            // wipe and recreate the local DB instead of crashing.  Remove or
            // replace with proper Migration(s) for production releases.
            .fallbackToDestructiveMigration()
            .build()
    }

    // VeePoo SDK manager
    private val vpOperateManager: VPOperateManager? = VPOperateManager.getInstance()
    
    // Connection manager for verifying real device connections
    private val connectionManager = ConnectionManager.getInstance(context)

    // Coroutine scope for background operations
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // LiveData for real-time health metrics
    private val _heartRate = MutableLiveData<HeartRateData>()
    val heartRate: LiveData<HeartRateData> = _heartRate

    private val _steps = MutableLiveData<StepsData>()
    val steps: LiveData<StepsData> = _steps

    private val _sleep = MutableLiveData<SleepData>()
    val sleep: LiveData<SleepData> = _sleep

    private val _bloodPressure = MutableLiveData<BloodPressureData>()
    val bloodPressure: LiveData<BloodPressureData> = _bloodPressure

    // Blood oxygen (SpO2)
    private val _bloodOxygen = MutableLiveData<BloodOxygenData>()
    val bloodOxygen: LiveData<BloodOxygenData> = _bloodOxygen

    // Stress
    private val _stress = MutableLiveData<StressData>()
    val stress: LiveData<StressData> = _stress

    // Sync status
    private val _isSyncing = MutableLiveData<Boolean>(false)
    val isSyncing: LiveData<Boolean> = _isSyncing

    // Connected device address
    private var connectedDeviceAddress: String? = null
    
    // Flag to track if real device data has been received
    private var hasReceivedRealData = false

    /**
     * Initialize the health data manager with a connected device
     * @param deviceAddress The MAC address of the connected VeePoo device
     */
    fun initialize(deviceAddress: String) {
        if (verifyDeviceConnection(deviceAddress)) {
            connectedDeviceAddress = deviceAddress
            hasReceivedRealData = false
            Log.i(TAG, "HealthDataManager initialized with verified device: $deviceAddress")
        } else {
            Log.e(TAG, "Failed to initialize: Device $deviceAddress not verified")
            connectedDeviceAddress = null
        }
    }

    /**
     * Verify that the device is actually connected via Bluetooth
     * @param deviceAddress The MAC address to verify
     * @return True if device is verified as connected, false otherwise
     */
    private fun verifyDeviceConnection(deviceAddress: String): Boolean {
        // Check if device address is valid
        if (deviceAddress.isBlank()) {
            Log.e(TAG, "Invalid device address")
            return false
        }
        
        // Check if device is connected via ConnectionManager
        val connectedDevice = connectionManager.getConnectedDevice()
        if (connectedDevice == null || connectedDevice.first != deviceAddress) {
            Log.e(TAG, "Device $deviceAddress is not connected according to ConnectionManager")
            return false
        }
        
        // Check if VPOperateManager is initialized
        if (vpOperateManager == null) {
            Log.e(TAG, "VPOperateManager is not initialized")
            return false
        }
        
        return true
    }

    /**
     * Start syncing health data from the connected device
     * @return True if sync started successfully, false otherwise
     */
    fun startSync(): Boolean {
        // Verify device is connected before starting sync
        if (connectedDeviceAddress == null || !verifyDeviceConnection(connectedDeviceAddress!!)) {
            Log.e(TAG, "Cannot start sync: No verified device connection")
            _isSyncing.postValue(false)
            return false
        }

        _isSyncing.postValue(true)
        Log.i(TAG, "Starting real device data sync with device: $connectedDeviceAddress")
        
        // Start real VeePoo-SDK synchronization
        try {
            // Register real-time data listeners with VeePoo SDK
            registerRealTimeDataListeners()
            
            // Request initial data from device
            requestInitialDeviceData()
            
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting sync: ${e.message}")
            _isSyncing.postValue(false)
            return false
        }
    }

    /**
     * Register listeners for real-time data from the VeePoo SDK
     * 
     * NOTE: This is a placeholder implementation that will be replaced
     * with actual VeePoo SDK integration when the SDK is properly available.
     */
    private fun registerRealTimeDataListeners() {
        try {
            vpOperateManager?.let { manager ->
                // PLACEHOLDER: Register heart rate listener
                // The actual VeePoo SDK method will be implemented here
                // when the SDK documentation and methods are available
                
                // For now, we'll just log that we're attempting to register listeners
                Log.d(TAG, "Attempted to register real-time data listeners with VeePoo SDK")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error registering data listeners: ${e.message}")
        }
    }

    /**
     * Request initial data from the connected device
     * 
     * NOTE: This is a placeholder implementation that will be replaced
     * with actual VeePoo SDK integration when the SDK is properly available.
     */
    private fun requestInitialDeviceData() {
        try {
            vpOperateManager?.let { manager ->
                // PLACEHOLDER: Request heart rate monitoring using a safe approach
                try {
                    /*  ─────────────────────────────────────────────────────────
                        The real VeePoo SDK call is commented-out until
                        the final SDK integration is completed.  Keeping it
                        here (commented) reminds us where it belongs while
                        letting the project compile.

                        manager.startDetectHeart(object : IBleWriteResponse {
                            override fun onResponse(code: Int) {
                                Log.d(TAG, "Heart rate monitoring response: $code")
                            }
                        })
                       ───────────────────────────────────────────────────────── */
                    Log.d(TAG, "startDetectHeart() skipped – awaiting SDK integration")
                } catch (e: Exception) {
                    // Log the error but don't crash the app
                    Log.e(TAG, "Error starting heart rate detection: ${e.message}")
                }
                
                // Log that we're attempting to request data
                Log.d(TAG, "Attempted to request initial data from VeePoo device")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting device data: ${e.message}")
        }
    }

    /**
     * Process real heart rate data from the device
     */
    private fun processHeartRateData(data: HeartRateData) {
        // Notify that real data was received
        notifyDataReceived()
        
        // Update LiveData
        _heartRate.postValue(data)
        
        // Save to database
        scope.launch {
            database.heartRateDao().insert(data)
        }
        
        Log.d(TAG, "Processed real heart rate data: ${data.value} bpm")
    }

    /**
     * Process real steps data from the device
     * 
     * NOTE: This is a placeholder implementation that will be replaced
     * with actual VeePoo SDK integration when the SDK is properly available.
     */
    private fun processStepsData(steps: Int, calories: Float, distance: Float) {
        val stepsData = StepsData(
            timestamp = System.currentTimeMillis(),
            steps = steps,
            calories = calories,
            distance = distance,
            deviceAddress = connectedDeviceAddress ?: ""
        )
        
        // Notify that real data was received
        notifyDataReceived()
        
        // Update LiveData
        _steps.postValue(stepsData)
        
        // Save to database
        scope.launch {
            database.stepsDao().insert(stepsData)
        }
        
        Log.d(TAG, "Processed real steps data: steps=$steps, calories=$calories, distance=$distance")
    }

    /**
     * Notify ConnectionManager that real data was received
     * This helps maintain the verified connection state
     */
    private fun notifyDataReceived() {
        hasReceivedRealData = true
        connectionManager.notifyDataReceived()
    }

    /**
     * Stop syncing health data
     */
    fun stopSync() {
        _isSyncing.postValue(false)
        
        // Stop real-time monitoring in VeePoo SDK
        try {
            vpOperateManager?.let { manager ->
                try {
                    /*  ─────────────────────────────────────────────────────────
                        The real VeePoo SDK stop call is commented-out until
                        SDK integration is ready.

                        manager.stopDetectHeart(object : IBleWriteResponse {
                            override fun onResponse(code: Int) {
                                Log.d(TAG, "Heart rate monitoring stopped: $code")
                            }
                        })
                       ───────────────────────────────────────────────────────── */
                    Log.d(TAG, "stopDetectHeart() skipped – awaiting SDK integration")
                } catch (e: Exception) {
                    // Log the error but don't crash the app
                    Log.e(TAG, "Error stopping heart rate detection: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping data sync: ${e.message}")
        }
        
        Log.d(TAG, "Stopped real device data sync")
    }

    /**
     * Add heart rate data manually (only for real device data)
     * @param data The heart rate data to add
     */
    fun addHeartRateData(data: HeartRateData) {
        // Verify this is real data from a connected device
        if (verifyDeviceConnection(data.deviceAddress)) {
            // Update LiveData
            _heartRate.postValue(data)
            
            // Save to database
            scope.launch {
                database.heartRateDao().insert(data)
            }
            
            // Notify data received
            notifyDataReceived()
            
            Log.d(TAG, "Added verified heart rate data: ${data.value} bpm")
        } else {
            Log.w(TAG, "Rejected unverified heart rate data")
        }
    }

    /**
     * Add steps data manually (only for real device data)
     * @param data The steps data to add
     */
    fun addStepsData(data: StepsData) {
        // Verify this is real data from a connected device
        if (verifyDeviceConnection(data.deviceAddress)) {
            // Update LiveData
            _steps.postValue(data)

            // Save to database
            scope.launch {
                database.stepsDao().insert(data)
            }

            // Notify data received
            notifyDataReceived()
            
            Log.d(
                TAG,
                "Added verified steps data: steps=${data.steps}, calories=${data.calories}, distance=${data.distance}"
            )
        } else {
            Log.w(TAG, "Rejected unverified steps data")
        }
    }

    /**
     * Add blood pressure data manually (only for real device data)
     * @param data The blood pressure data to add
     */
    fun addBloodPressureData(data: BloodPressureData) {
        // Verify this is real data from a connected device
        if (verifyDeviceConnection(data.deviceAddress)) {
            // Update LiveData
            _bloodPressure.postValue(data)
            
            // Save to database
            scope.launch {
                database.bloodPressureDao().insert(data)
            }
            
            // Notify data received
            notifyDataReceived()
            
            Log.d(TAG, "Added verified blood pressure data: ${data.systolic}/${data.diastolic}, pulse: ${data.pulse}")
        } else {
            Log.w(TAG, "Rejected unverified blood pressure data")
        }
    }

    /**
     * Add blood oxygen data manually (only for real device data)
     */
    fun addBloodOxygenData(data: BloodOxygenData) {
        // Verify this is real data from a connected device
        if (verifyDeviceConnection(data.deviceAddress)) {
            _bloodOxygen.postValue(data)
            
            scope.launch {
                database.bloodOxygenDao().insert(data)
            }
            
            // Notify data received
            notifyDataReceived()
            
            Log.d(TAG, "Added verified blood oxygen data: ${data.spO2}%, pulse: ${data.pulse}")
        } else {
            Log.w(TAG, "Rejected unverified blood oxygen data")
        }
    }

    /**
     * Add stress data manually (only for real device data)
     */
    fun addStressData(data: StressData) {
        // Verify this is real data from a connected device
        if (verifyDeviceConnection(data.deviceAddress)) {
            _stress.postValue(data)
            
            scope.launch {
                database.stressDao().insert(data)
            }
            
            // Notify data received
            notifyDataReceived()
            
            Log.d(TAG, "Added verified stress data: level=${data.stressLevel}, HRV=${data.heartRateVariability}ms")
        } else {
            Log.w(TAG, "Rejected unverified stress data")
        }
    }

    /**
     * Check if real data has been received from the device
     * @return True if real data has been received, false otherwise
     */
    fun hasReceivedRealData(): Boolean {
        return hasReceivedRealData
    }

    /**
     * Get heart rate data for a specific time period
     * @param startTime Start timestamp (milliseconds)
     * @param endTime End timestamp (milliseconds)
     * @return LiveData list of heart rate data
     */
    fun getHeartRateData(startTime: Long, endTime: Long): LiveData<List<HeartRateData>> {
        return database.heartRateDao().getDataBetween(startTime, endTime)
    }

    /**
     * Get steps data for a specific time period
     * @param startTime Start timestamp (milliseconds)
     * @param endTime End timestamp (milliseconds)
     * @return LiveData list of steps data
     */
    fun getStepsData(startTime: Long, endTime: Long): LiveData<List<StepsData>> {
        return database.stepsDao().getDataBetween(startTime, endTime)
    }

    /**
     * Get sleep data for a specific time period
     * @param startTime Start timestamp (milliseconds)
     * @param endTime End timestamp (milliseconds)
     * @return LiveData list of sleep data
     */
    fun getSleepData(startTime: Long, endTime: Long): LiveData<List<SleepData>> {
        return database.sleepDao().getDataBetween(startTime, endTime)
    }

    /**
     * Get blood pressure data for a specific time period
     * @param startTime Start timestamp (milliseconds)
     * @param endTime End timestamp (milliseconds)
     * @return LiveData list of blood pressure data
     */
    fun getBloodPressureData(startTime: Long, endTime: Long): LiveData<List<BloodPressureData>> {
        return database.bloodPressureDao().getDataBetween(startTime, endTime)
    }

    /**
     * Get blood oxygen data for a specific time period
     */
    fun getBloodOxygenData(startTime: Long, endTime: Long): LiveData<List<BloodOxygenData>> {
        return database.bloodOxygenDao().getDataBetween(startTime, endTime)
    }

    /**
     * Get stress data for a specific time period
     */
    fun getStressData(startTime: Long, endTime: Long): LiveData<List<StressData>> {
        return database.stressDao().getDataBetween(startTime, endTime)
    }

    /**
     * Get latest heart rate data
     * @return LiveData with the latest heart rate data
     */
    fun getLatestHeartRate(): LiveData<HeartRateData?> {
        return database.heartRateDao().getLatest()
    }

    /**
     * Get latest steps data
     * @return LiveData with the latest steps data
     */
    fun getLatestSteps(): LiveData<StepsData?> {
        return database.stepsDao().getLatest()
    }

    /**
     * Get latest sleep data
     * @return LiveData with the latest sleep data
     */
    fun getLatestSleep(): LiveData<SleepData?> {
        return database.sleepDao().getLatest()
    }

    /**
     * Get latest blood pressure data
     * @return LiveData with the latest blood pressure data
     */
    fun getLatestBloodPressure(): LiveData<BloodPressureData?> {
        return database.bloodPressureDao().getLatest()
    }

    /**
     * Get latest blood oxygen data
     */
    fun getLatestBloodOxygen(): LiveData<BloodOxygenData?> {
        return database.bloodOxygenDao().getLatest()
    }

    /**
     * Get latest stress data
     */
    fun getLatestStress(): LiveData<StressData?> {
        return database.stressDao().getLatest()
    }

    /**
     * Clean up resources when the manager is no longer needed
     */
    fun cleanup() {
        stopSync()
        scope.cancel()
    }

    companion object {
        private const val TAG = "HealthDataManager"
        
        @Volatile
        private var INSTANCE: HealthDataManager? = null
        
        /**
         * Get the singleton instance of HealthDataManager
         * @param context Application context
         * @return HealthDataManager instance
         */
        fun getInstance(context: Context): HealthDataManager {
            return INSTANCE ?: synchronized(this) {
                val instance = HealthDataManager(context)
                INSTANCE = instance
                instance
            }
        }
    }
}

/**
 * Room Database for health data
 */
@Database(
    entities = [
        HeartRateData::class,
        StepsData::class,
        SleepData::class,
        BloodPressureData::class,
        BloodOxygenData::class,
        StressData::class
    ],
    version = 2,
    exportSchema = false
)
abstract class HealthDatabase : RoomDatabase() {
    abstract fun heartRateDao(): HeartRateDao
    abstract fun stepsDao(): StepsDao
    abstract fun sleepDao(): SleepDao
    abstract fun bloodPressureDao(): BloodPressureDao
    abstract fun bloodOxygenDao(): BloodOxygenDao
    abstract fun stressDao(): StressDao
}

/**
 * Data Models for Health Metrics
 */

@Entity(tableName = "heart_rate")
data class HeartRateData(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val value: Int,
    val deviceAddress: String
)

/* ----------------------------------------------------------
 * Blood-Oxygen (SpO2) Data
 * ---------------------------------------------------------- */
@Entity(tableName = "blood_oxygen")
data class BloodOxygenData(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val spO2: Int,          // Oxygen saturation percentage
    val pulse: Int,         // Pulse rate during measurement
    val deviceAddress: String
)

/* ----------------------------------------------------------
 * Stress Data
 * ---------------------------------------------------------- */
@Entity(tableName = "stress")
data class StressData(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val stressLevel: Int,    // 0-100 scale
    val heartRateVariability: Int, // HRV in ms
    val deviceAddress: String
)

@Entity(tableName = "steps")
data class StepsData(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val steps: Int,
    val calories: Float,
    val distance: Float,
    val deviceAddress: String
)

@Entity(tableName = "sleep")
data class SleepData(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val deepSleepMinutes: Int,
    val lightSleepMinutes: Int,
    val remSleepMinutes: Int,
    val totalSleepMinutes: Int,
    val sleepQualityScore: Int, // 0-100 score
    val fallAsleepTime: Long,
    val wakeUpTime: Long,
    val averageHeartRate: Int,
    val highestHeartRate: Int,
    val lowestHeartRate: Int,
    val deviceAddress: String
)

@Entity(tableName = "blood_pressure")
data class BloodPressureData(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val systolic: Int,
    val diastolic: Int,
    val pulse: Int,
    val deviceAddress: String
)

/**
 * Data Access Objects (DAOs) for Room Database
 */

@Dao
interface HeartRateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(heartRateData: HeartRateData)
    
    @Query("SELECT * FROM heart_rate WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp ASC")
    fun getDataBetween(startTime: Long, endTime: Long): LiveData<List<HeartRateData>>
    
    @Query("SELECT * FROM heart_rate ORDER BY timestamp DESC LIMIT 1")
    fun getLatest(): LiveData<HeartRateData?>
}

@Dao
interface StepsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stepsData: StepsData)
    
    @Query("SELECT * FROM steps WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp ASC")
    fun getDataBetween(startTime: Long, endTime: Long): LiveData<List<StepsData>>
    
    @Query("SELECT * FROM steps ORDER BY timestamp DESC LIMIT 1")
    fun getLatest(): LiveData<StepsData?>
}

@Dao
interface SleepDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sleepData: SleepData)
    
    @Query("SELECT * FROM sleep WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp ASC")
    fun getDataBetween(startTime: Long, endTime: Long): LiveData<List<SleepData>>
    
    @Query("SELECT * FROM sleep ORDER BY timestamp DESC LIMIT 1")
    fun getLatest(): LiveData<SleepData?>
}

@Dao
interface BloodPressureDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bloodPressureData: BloodPressureData)
    
    @Query("SELECT * FROM blood_pressure WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp ASC")
    fun getDataBetween(startTime: Long, endTime: Long): LiveData<List<BloodPressureData>>
    
    @Query("SELECT * FROM blood_pressure ORDER BY timestamp DESC LIMIT 1")
    fun getLatest(): LiveData<BloodPressureData?>
}

@Dao
interface BloodOxygenDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bloodOxygenData: BloodOxygenData)

    @Query("SELECT * FROM blood_oxygen WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp ASC")
    fun getDataBetween(startTime: Long, endTime: Long): LiveData<List<BloodOxygenData>>

    @Query("SELECT * FROM blood_oxygen ORDER BY timestamp DESC LIMIT 1")
    fun getLatest(): LiveData<BloodOxygenData?>
}

@Dao
interface StressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stressData: StressData)
    
    @Query("SELECT * FROM stress WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp ASC")
    fun getDataBetween(startTime: Long, endTime: Long): LiveData<List<StressData>>
    
    @Query("SELECT * FROM stress ORDER BY timestamp DESC LIMIT 1")
    fun getLatest(): LiveData<StressData?>
}
