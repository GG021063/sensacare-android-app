package com.sensacare.app.di

import android.content.Context
import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.sensacare.app.BuildConfig
import com.sensacare.app.data.network.api.*
import com.sensacare.app.data.network.auth.AuthInterceptor
import com.sensacare.app.data.network.auth.TokenAuthenticator
import com.sensacare.app.data.network.auth.TokenManager
import com.sensacare.app.data.network.connectivity.ConnectivityInterceptor
import com.sensacare.app.data.network.connectivity.NetworkConnectivityMonitor
import com.sensacare.app.data.network.error.ErrorResponseConverter
import com.sensacare.app.data.network.error.NetworkErrorHandler
import com.sensacare.app.data.network.interceptor.CompressionInterceptor
import com.sensacare.app.data.network.interceptor.HeadersInterceptor
import com.sensacare.app.data.network.interceptor.RequestResponseLoggingInterceptor
import com.sensacare.app.data.network.mock.MockHealthDataService
import com.sensacare.app.data.network.mock.MockUserService
import com.sensacare.app.data.network.mock.MockDeviceService
import com.sensacare.app.data.network.ssl.CertificatePinnerFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

/**
 * NetworkModule - Comprehensive network dependency injection setup for SensaCare app
 *
 * This module provides all network-related dependencies:
 * - Retrofit configuration with JSON serialization
 * - HTTP client with logging, timeout, and retry policies
 * - API interceptors for authentication, headers, and request/response logging
 * - Network connectivity monitoring
 * - SSL/TLS certificate pinning for security
 * - API service interfaces for health data sync, user management, device connectivity
 * - Error handling and response parsing
 * - Caching strategies for offline support
 * - Request/response compression
 * - Base URL configuration with environment support (dev/staging/prod)
 * - Mock API services for testing and development
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // Network timeout constants
    private const val CONNECT_TIMEOUT = 30L
    private const val READ_TIMEOUT = 30L
    private const val WRITE_TIMEOUT = 30L
    
    // Cache size for network responses (10 MB)
    private const val CACHE_SIZE = 10 * 1024 * 1024L
    
    // API base URLs for different environments
    private const val BASE_URL_DEV = "https://dev-api.sensacare.com/"
    private const val BASE_URL_STAGING = "https://staging-api.sensacare.com/"
    private const val BASE_URL_PROD = "https://api.sensacare.com/"
    
    // Certificate pinning hosts and pins
    private val CERTIFICATE_PINS = mapOf(
        "api.sensacare.com" to listOf(
            "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
            "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB="
        ),
        "dev-api.sensacare.com" to listOf(
            "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
            "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB="
        ),
        "staging-api.sensacare.com" to listOf(
            "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
            "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB="
        )
    )

    /**
     * Provides the base URL based on the current build environment
     */
    @Provides
    @Singleton
    @Named("baseUrl")
    fun provideBaseUrl(): String {
        return when {
            BuildConfig.DEBUG && BuildConfig.BUILD_TYPE == "dev" -> BASE_URL_DEV
            BuildConfig.DEBUG && BuildConfig.BUILD_TYPE == "staging" -> BASE_URL_STAGING
            else -> BASE_URL_PROD
        }
    }

    /**
     * Provides Moshi for JSON serialization/deserialization
     */
    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            // Add custom adapters for date/time, enums, etc.
            .build()
    }

    /**
     * Provides the HTTP cache for offline support
     */
    @Provides
    @Singleton
    fun provideCache(@ApplicationContext context: Context): Cache {
        val cacheDir = File(context.cacheDir, "http_cache")
        return Cache(cacheDir, CACHE_SIZE)
    }

    /**
     * Provides certificate pinner for SSL/TLS pinning
     */
    @Provides
    @Singleton
    fun provideCertificatePinner(): CertificatePinner {
        return CertificatePinnerFactory.create(CERTIFICATE_PINS)
    }

    /**
     * Provides network connectivity monitor
     */
    @Provides
    @Singleton
    fun provideNetworkConnectivityMonitor(
        @ApplicationContext context: Context
    ): NetworkConnectivityMonitor {
        return NetworkConnectivityMonitor(context)
    }

    /**
     * Provides connectivity interceptor for offline handling
     */
    @Provides
    @Singleton
    fun provideConnectivityInterceptor(
        networkConnectivityMonitor: NetworkConnectivityMonitor
    ): ConnectivityInterceptor {
        return ConnectivityInterceptor(networkConnectivityMonitor)
    }

    /**
     * Provides authentication interceptor for adding auth tokens
     */
    @Provides
    @Singleton
    fun provideAuthInterceptor(tokenManager: TokenManager): AuthInterceptor {
        return AuthInterceptor(tokenManager)
    }

    /**
     * Provides token authenticator for handling 401 responses
     */
    @Provides
    @Singleton
    fun provideTokenAuthenticator(tokenManager: TokenManager): TokenAuthenticator {
        return TokenAuthenticator(tokenManager)
    }

    /**
     * Provides headers interceptor for adding common headers
     */
    @Provides
    @Singleton
    fun provideHeadersInterceptor(): HeadersInterceptor {
        return HeadersInterceptor()
    }

    /**
     * Provides compression interceptor for request/response compression
     */
    @Provides
    @Singleton
    fun provideCompressionInterceptor(): CompressionInterceptor {
        return CompressionInterceptor()
    }

    /**
     * Provides logging interceptor for debugging
     */
    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor { message ->
            Timber.tag("OkHttp").d(message)
        }.apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    /**
     * Provides Chucker interceptor for network inspection in debug builds
     */
    @Provides
    @Singleton
    fun provideChuckerInterceptor(@ApplicationContext context: Context): ChuckerInterceptor {
        return ChuckerInterceptor.Builder(context)
            .collector(ChuckerCollector(context))
            .maxContentLength(250_000L)
            .redactHeaders(listOf("Authorization", "Cookie"))
            .alwaysReadResponseBody(false)
            .build()
    }

    /**
     * Provides request/response logging interceptor
     */
    @Provides
    @Singleton
    fun provideRequestResponseLoggingInterceptor(): RequestResponseLoggingInterceptor {
        return RequestResponseLoggingInterceptor()
    }

    /**
     * Provides OkHttpClient with all interceptors and configurations
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        @ApplicationContext context: Context,
        cache: Cache,
        certificatePinner: CertificatePinner,
        connectivityInterceptor: ConnectivityInterceptor,
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
        headersInterceptor: HeadersInterceptor,
        compressionInterceptor: CompressionInterceptor,
        httpLoggingInterceptor: HttpLoggingInterceptor,
        chuckerInterceptor: ChuckerInterceptor,
        requestResponseLoggingInterceptor: RequestResponseLoggingInterceptor
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .cache(cache)
            .certificatePinner(certificatePinner)
            .addInterceptor(connectivityInterceptor)
            .addInterceptor(headersInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(compressionInterceptor)
            .authenticator(tokenAuthenticator)
            .addInterceptor(requestResponseLoggingInterceptor)
            
        // Add debug interceptors only in debug builds
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(httpLoggingInterceptor)
            builder.addInterceptor(chuckerInterceptor)
        }
        
        return builder.build()
    }

    /**
     * Provides Retrofit instance with Moshi converter
     */
    @Provides
    @Singleton
    fun provideRetrofit(
        @Named("baseUrl") baseUrl: String,
        okHttpClient: OkHttpClient,
        moshi: Moshi
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    /**
     * Provides network error handler
     */
    @Provides
    @Singleton
    fun provideNetworkErrorHandler(
        errorResponseConverter: ErrorResponseConverter
    ): NetworkErrorHandler {
        return NetworkErrorHandler(errorResponseConverter)
    }

    /**
     * Provides error response converter
     */
    @Provides
    @Singleton
    fun provideErrorResponseConverter(moshi: Moshi): ErrorResponseConverter {
        return ErrorResponseConverter(moshi)
    }

    /**
     * Provides token manager for authentication
     */
    @Provides
    @Singleton
    fun provideTokenManager(@ApplicationContext context: Context): TokenManager {
        return TokenManager(context)
    }

    /**
     * API Service Providers
     */
    
    /**
     * Provides health data API service
     */
    @Provides
    @Singleton
    fun provideHealthDataService(retrofit: Retrofit): HealthDataService {
        return if (BuildConfig.USE_MOCK_SERVICES) {
            MockHealthDataService()
        } else {
            retrofit.create(HealthDataService::class.java)
        }
    }

    /**
     * Provides user API service
     */
    @Provides
    @Singleton
    fun provideUserService(retrofit: Retrofit): UserService {
        return if (BuildConfig.USE_MOCK_SERVICES) {
            MockUserService()
        } else {
            retrofit.create(UserService::class.java)
        }
    }

    /**
     * Provides device API service
     */
    @Provides
    @Singleton
    fun provideDeviceService(retrofit: Retrofit): DeviceService {
        return if (BuildConfig.USE_MOCK_SERVICES) {
            MockDeviceService()
        } else {
            retrofit.create(DeviceService::class.java)
        }
    }

    /**
     * Provides health insights API service
     */
    @Provides
    @Singleton
    fun provideHealthInsightsService(retrofit: Retrofit): HealthInsightsService {
        return retrofit.create(HealthInsightsService::class.java)
    }

    /**
     * Provides alert API service
     */
    @Provides
    @Singleton
    fun provideAlertService(retrofit: Retrofit): AlertService {
        return retrofit.create(AlertService::class.java)
    }

    /**
     * Provides goal API service
     */
    @Provides
    @Singleton
    fun provideGoalService(retrofit: Retrofit): GoalService {
        return retrofit.create(GoalService::class.java)
    }

    /**
     * Provides emergency contact API service
     */
    @Provides
    @Singleton
    fun provideEmergencyContactService(retrofit: Retrofit): EmergencyContactService {
        return retrofit.create(EmergencyContactService::class.java)
    }

    /**
     * Provides firmware update API service
     */
    @Provides
    @Singleton
    fun provideFirmwareUpdateService(retrofit: Retrofit): FirmwareUpdateService {
        return retrofit.create(FirmwareUpdateService::class.java)
    }
}
