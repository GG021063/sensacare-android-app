package com.sensacare.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sensacare.app.data.local.SensaCareDatabase
import com.sensacare.app.data.local.dao.*
import com.sensacare.app.data.local.converter.*
import com.sensacare.app.data.local.backup.DatabaseBackupManager
import com.sensacare.app.data.local.encryption.DatabaseEncryptionManager
import com.sensacare.app.data.local.inspector.DatabaseInspectorConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import timber.log.Timber
import java.util.concurrent.Executors
import javax.inject.Singleton

/**
 * DatabaseModule - Comprehensive database dependency injection setup for SensaCare app
 *
 * This module provides all database-related dependencies:
 * - Room database instance (encrypted, properly configured)
 * - All DAO instances for data access
 * - Type converters for complex data types
 * - Migration strategies for database versioning
 * - Database inspector for debugging
 * - Connection pool and threading optimization
 * - Backup and restore capabilities
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    // Database version - increment with schema changes
    private const val DATABASE_VERSION = 1
    private const val DATABASE_NAME = "sensacare_db"
    
    // Database encryption passphrase - in production, this would be securely stored
    // and potentially derived from user authentication
    private const val PASSPHRASE = "SensaCare_Health_Data_Encryption_Key"
    
    // Number of threads in the database query executor pool
    private const val QUERY_EXECUTOR_POOL_SIZE = 4

    /**
     * Database migrations to handle schema changes between versions
     */
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Example migration from version 1 to 2
            // database.execSQL("ALTER TABLE health_data ADD COLUMN new_field TEXT")
            Timber.d("Migrating database from version 1 to 2")
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Example migration from version 2 to 3
            // database.execSQL("CREATE TABLE IF NOT EXISTS new_table (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT)")
            Timber.d("Migrating database from version 2 to 3")
        }
    }

    /**
     * Provides Room database instance as a singleton
     * Configured with:
     * - Encryption for sensitive health data
     * - Custom query executor for optimized threading
     * - Migration strategies for version updates
     * - Database inspector for debugging
     * - Type converters for complex data types
     */
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        dateTimeConverter: DateTimeConverter,
        enumConverters: EnumConverters,
        jsonConverter: JsonConverter,
        listConverters: ListConverters,
        databaseEncryptionManager: DatabaseEncryptionManager,
        databaseInspectorConfig: DatabaseInspectorConfig
    ): SensaCareDatabase {
        // Create encryption factory with passphrase
        val passphrase = SQLiteDatabase.getBytes(PASSPHRASE.toCharArray())
        val factory = SupportFactory(passphrase)
        
        // Custom query executor for optimized threading
        val queryExecutor = Executors.newFixedThreadPool(QUERY_EXECUTOR_POOL_SIZE)
        
        // Build and return the database instance
        return Room.databaseBuilder(context, SensaCareDatabase::class.java, DATABASE_NAME)
            // Set version
            .fallbackToDestructiveMigration() // Only for development - remove in production
            // Add migrations
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            // Configure encryption
            .openHelperFactory(factory)
            // Configure query executor
            .setQueryExecutor(queryExecutor)
            // Enable database inspector in debug builds
            .apply { 
                if (databaseInspectorConfig.isInspectorEnabled()) {
                    this.setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                }
            }
            // Add callback for database creation/opening
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    Timber.d("SensaCare database created")
                }
                
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    Timber.d("SensaCare database opened")
                    // Enable foreign keys
                    db.execSQL("PRAGMA foreign_keys = ON")
                }
            })
            .build()
    }
    
    /**
     * Provides database encryption manager
     */
    @Provides
    @Singleton
    fun provideDatabaseEncryptionManager(): DatabaseEncryptionManager {
        return DatabaseEncryptionManager(PASSPHRASE)
    }
    
    /**
     * Provides database inspector configuration
     */
    @Provides
    @Singleton
    fun provideDatabaseInspectorConfig(@ApplicationContext context: Context): DatabaseInspectorConfig {
        return DatabaseInspectorConfig(context)
    }
    
    /**
     * Provides database backup manager
     */
    @Provides
    @Singleton
    fun provideDatabaseBackupManager(
        @ApplicationContext context: Context,
        database: SensaCareDatabase,
        databaseEncryptionManager: DatabaseEncryptionManager
    ): DatabaseBackupManager {
        return DatabaseBackupManager(context, database, databaseEncryptionManager)
    }
    
    /**
     * Type converters for complex data types
     */
    @Provides
    @Singleton
    fun provideDateTimeConverter(): DateTimeConverter {
        return DateTimeConverter()
    }
    
    @Provides
    @Singleton
    fun provideEnumConverters(): EnumConverters {
        return EnumConverters()
    }
    
    @Provides
    @Singleton
    fun provideJsonConverter(): JsonConverter {
        return JsonConverter()
    }
    
    @Provides
    @Singleton
    fun provideListConverters(): ListConverters {
        return ListConverters()
    }
    
    /**
     * DAO Providers - Each DAO is provided as a singleton
     */
    
    @Provides
    @Singleton
    fun provideHealthDataDao(database: SensaCareDatabase): HealthDataDao {
        return database.healthDataDao()
    }
    
    @Provides
    @Singleton
    fun provideHeartRateDao(database: SensaCareDatabase): HeartRateDao {
        return database.heartRateDao()
    }
    
    @Provides
    @Singleton
    fun provideBloodPressureDao(database: SensaCareDatabase): BloodPressureDao {
        return database.bloodPressureDao()
    }
    
    @Provides
    @Singleton
    fun provideSleepDao(database: SensaCareDatabase): SleepDao {
        return database.sleepDao()
    }
    
    @Provides
    @Singleton
    fun provideActivityDao(database: SensaCareDatabase): ActivityDao {
        return database.activityDao()
    }
    
    @Provides
    @Singleton
    fun provideStressDao(database: SensaCareDatabase): StressDao {
        return database.stressDao()
    }
    
    @Provides
    @Singleton
    fun provideWeightDao(database: SensaCareDatabase): WeightDao {
        return database.weightDao()
    }
    
    @Provides
    @Singleton
    fun provideWaterIntakeDao(database: SensaCareDatabase): WaterIntakeDao {
        return database.waterIntakeDao()
    }
    
    @Provides
    @Singleton
    fun provideHealthGoalDao(database: SensaCareDatabase): HealthGoalDao {
        return database.healthGoalDao()
    }
    
    @Provides
    @Singleton
    fun provideGoalProgressDao(database: SensaCareDatabase): GoalProgressDao {
        return database.goalProgressDao()
    }
    
    @Provides
    @Singleton
    fun provideHealthAlertDao(database: SensaCareDatabase): HealthAlertDao {
        return database.healthAlertDao()
    }
    
    @Provides
    @Singleton
    fun provideAlertRuleDao(database: SensaCareDatabase): AlertRuleDao {
        return database.alertRuleDao()
    }
    
    @Provides
    @Singleton
    fun provideEmergencyContactDao(database: SensaCareDatabase): EmergencyContactDao {
        return database.emergencyContactDao()
    }
    
    @Provides
    @Singleton
    fun provideDeviceDao(database: SensaCareDatabase): DeviceDao {
        return database.deviceDao()
    }
    
    @Provides
    @Singleton
    fun provideDeviceSyncDao(database: SensaCareDatabase): DeviceSyncDao {
        return database.deviceSyncDao()
    }
    
    @Provides
    @Singleton
    fun provideHealthInsightDao(database: SensaCareDatabase): HealthInsightDao {
        return database.healthInsightDao()
    }
    
    @Provides
    @Singleton
    fun provideUserPreferencesDao(database: SensaCareDatabase): UserPreferencesDao {
        return database.userPreferencesDao()
    }
    
    @Provides
    @Singleton
    fun provideAlertPreferencesDao(database: SensaCareDatabase): AlertPreferencesDao {
        return database.alertPreferencesDao()
    }
}
