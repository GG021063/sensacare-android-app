package com.sensacare.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sensacare.app.data.local.converter.*
import com.sensacare.app.data.local.dao.*
import com.sensacare.app.data.local.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * SensaCareDatabase - Main Room Database for the SensaCare application
 *
 * This database serves as the single source of truth for all local data storage,
 * providing a robust and efficient way to store and retrieve health data, device
 * information, and user preferences.
 *
 * Features:
 * - Comprehensive health data storage with specialized tables for different metrics
 * - Device management and synchronization tracking
 * - Goal tracking and alert management
 * - Type conversion for complex data types
 * - Migration strategies for database schema updates
 * - Thread-safe singleton access pattern
 */
@Database(
    entities = [
        // Health data entities
        HealthDataEntity::class,
        HeartRateEntity::class,
        BloodPressureEntity::class,
        SleepEntity::class,
        SleepStageEntity::class,
        ActivityEntity::class,
        ActivitySessionEntity::class,
        
        // Device management entities
        DeviceEntity::class,
        DeviceSettingEntity::class,
        DeviceSyncHistoryEntity::class,
        
        // Goals and alerts entities
        HealthGoalEntity::class,
        GoalProgressEntity::class,
        HealthAlertEntity::class,
        AlertRuleEntity::class,
        EmergencyContactEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(
    LocalDateTimeConverter::class,
    LocalDateConverter::class,
    LocalTimeConverter::class,
    StringListConverter::class,
    SyncStatusConverter::class,
    MetricTypeConverter::class,
    ActivityTypeConverter::class,
    IntensityConverter::class,
    SleepStageConverter::class,
    JsonConverter::class
)
abstract class SensaCareDatabase : RoomDatabase() {

    /**
     * DAOs for accessing database tables
     */
    abstract fun healthDataDao(): HealthDataDao
    abstract fun heartRateDao(): HeartRateDao
    abstract fun bloodPressureDao(): BloodPressureDao
    abstract fun sleepDao(): SleepDao
    abstract fun activityDao(): ActivityDao
    abstract fun deviceDao(): DeviceDao
    abstract fun healthGoalDao(): HealthGoalDao
    abstract fun alertDao(): AlertDao

    companion object {
        private const val DATABASE_NAME = "sensacare_db"
        
        @Volatile
        private var INSTANCE: SensaCareDatabase? = null
        
        /**
         * Get the singleton database instance
         * @param context Application context
         * @param scope CoroutineScope for database operations
         * @return SensaCareDatabase instance
         */
        fun getDatabase(
            context: Context,
            scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
        ): SensaCareDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SensaCareDatabase::class.java,
                    DATABASE_NAME
                )
                    .addCallback(SensaCareDatabaseCallback(scope))
                    .addMigrations(*ALL_MIGRATIONS)
                    .fallbackToDestructiveMigration() // Only in development
                    .build()
                
                INSTANCE = instance
                instance
            }
        }
        
        /**
         * Database callback for initialization and pre-population
         */
        private class SensaCareDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                
                // Pre-populate database in a coroutine
                INSTANCE?.let { database ->
                    scope.launch {
                        try {
                            // Initialize with default data if needed
                            initializeDatabase(database)
                        } catch (e: Exception) {
                            Timber.e(e, "Error initializing database")
                        }
                    }
                }
            }
            
            /**
             * Initialize the database with default data
             * @param database SensaCareDatabase instance
             */
            private suspend fun initializeDatabase(database: SensaCareDatabase) {
                // Add default settings, device types, or other initial data if needed
                
                // Example: Add default health metric types if needed
                // database.metricTypeDao().insertAll(defaultMetricTypes)
                
                Timber.d("Database initialized successfully")
            }
        }
        
        /**
         * Migrations between database versions
         */
        
        // Migration from version 1 to 2
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Example migration:
                // database.execSQL("ALTER TABLE health_data ADD COLUMN source TEXT")
            }
        }
        
        // Migration from version 2 to 3
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Future migration code
            }
        }
        
        // Array of all migrations
        private val ALL_MIGRATIONS = arrayOf(
            MIGRATION_1_2,
            MIGRATION_2_3
        )
    }
}
