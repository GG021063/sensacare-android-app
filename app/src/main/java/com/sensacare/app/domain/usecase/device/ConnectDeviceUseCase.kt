package com.sensacare.app.domain.usecase.device

import com.sensacare.app.domain.model.Device
import com.sensacare.app.domain.repository.DeviceConnectionState
import com.sensacare.app.domain.repository.DeviceError
import com.sensacare.app.domain.repository.DeviceOperationResult
import com.sensacare.app.domain.repository.DeviceRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * ConnectDeviceUseCase - Handles the connection to a health tracking device
 *
 * This use case orchestrates the entire device connection process:
 * 1. Validates the device and connection prerequisites
 * 2. Attempts connection with timeout handling and retries
 * 3. Performs post-connection setup (feature detection, settings sync)
 * 4. Monitors connection status and handles errors
 * 5. Automatically pairs the device if needed
 *
 * It follows Clean Architecture principles by encapsulating all connection
 * business logic and providing a simple interface to the presentation layer.
 */
class ConnectDeviceUseCase(
    private val deviceRepository: DeviceRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    companion object {
        private const val CONNECTION_TIMEOUT_MS = 30000L // 30 seconds
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 2000L // 2 seconds
    }

    /**
     * Invoke operator to call the use case as a function
     *
     * @param deviceId The ID of the device to connect to
     * @param autoRetry Whether to automatically retry on failure
     * @param autoPair Whether to automatically pair the device if not paired
     * @param timeoutMs Connection timeout in milliseconds
     * @return Flow of DeviceOperationResult with connection states and result
     */
    operator fun invoke(
        deviceId: Long,
        autoRetry: Boolean = true,
        autoPair: Boolean = true,
        timeoutMs: Long = CONNECTION_TIMEOUT_MS
    ): Flow<DeviceOperationResult<Device>> = flow {
        // Emit loading state
        emit(DeviceOperationResult.Loading)

        // Validate device exists
        val device = deviceRepository.getDevice(deviceId).firstOrNull()
        if (device == null) {
            emit(DeviceOperationResult.Error(DeviceError.DeviceNotFoundError(deviceId)))
            return@flow
        }

        Timber.d("Connecting to device: ${device.name} (${device.macAddress})")

        // Check if device is already connected
        val isConnected = deviceRepository.isDeviceConnected(deviceId).firstOrNull() ?: false
        if (isConnected) {
            Timber.d("Device already connected: ${device.name}")
            emit(DeviceOperationResult.Success(device))
            return@flow
        }

        // Check if device is paired
        val isPaired = deviceRepository.isDevicePaired(deviceId).firstOrNull() ?: false
        if (!isPaired && autoPair) {
            Timber.d("Device not paired, attempting to pair: ${device.name}")
            val pairingResult = deviceRepository.pairDevice(device)
            
            if (pairingResult is DeviceOperationResult.Error) {
                emit(DeviceOperationResult.Error(
                    DeviceError.ConnectionError(
                        "Failed to pair device before connecting: ${pairingResult.error.message}",
                        pairingResult.error as? Exception
                    )
                ))
                return@flow
            }
        } else if (!isPaired) {
            emit(DeviceOperationResult.Error(
                DeviceError.PairingError("Device not paired and auto-pairing disabled")
            ))
            return@flow
        }

        // Attempt connection with retry logic
        var retryCount = 0
        var lastError: DeviceError? = null

        while (retryCount <= (if (autoRetry) MAX_RETRY_ATTEMPTS else 0)) {
            if (retryCount > 0) {
                Timber.d("Retrying connection (attempt $retryCount of $MAX_RETRY_ATTEMPTS)")
                kotlinx.coroutines.delay(RETRY_DELAY_MS)
                emit(DeviceOperationResult.Loading) // Re-emit loading state for retry
            }

            try {
                // Attempt connection with timeout
                val connectionResult = withTimeout(timeoutMs) {
                    deviceRepository.connectToDevice(deviceId)
                }

                when (connectionResult) {
                    is DeviceOperationResult.Success -> {
                        // Connection successful
                        val connectedDevice = connectionResult.data
                        
                        // Perform post-connection setup
                        performPostConnectionSetup(connectedDevice)
                        
                        // Emit success
                        emit(DeviceOperationResult.Success(connectedDevice))
                        
                        // Start monitoring connection status in background
                        monitorConnectionStatus(deviceId)
                        
                        return@flow
                    }
                    is DeviceOperationResult.Error -> {
                        lastError = connectionResult.error
                        Timber.e("Connection attempt failed: ${connectionResult.error.message}")
                    }
                    is DeviceOperationResult.Loading -> {
                        // Ignore loading state from repository
                    }
                }
            } catch (e: TimeoutCancellationException) {
                lastError = DeviceError.TimeoutError(
                    "Connection timed out after ${timeoutMs / 1000} seconds",
                    "connect"
                )
                Timber.e("Connection timed out: ${e.message}")
            } catch (e: CancellationException) {
                throw e // Rethrow cancellation exceptions to respect coroutine cancellation
            } catch (e: Exception) {
                lastError = DeviceError.UnknownError(
                    "Unexpected error during connection: ${e.message}",
                    e
                )
                Timber.e(e, "Unexpected error during connection")
            }

            retryCount++
        }

        // If we get here, all connection attempts failed
        emit(DeviceOperationResult.Error(
            lastError ?: DeviceError.ConnectionError("Failed to connect to device after $MAX_RETRY_ATTEMPTS attempts")
        ))
    }
    .catch { e ->
        if (e is CancellationException) throw e
        
        Timber.e(e, "Error in ConnectDeviceUseCase flow")
        emit(DeviceOperationResult.Error(
            DeviceError.UnknownError(
                "Unexpected error in connection flow: ${e.message}",
                e
            )
        ))
    }
    .flowOn(dispatcher)

    /**
     * Performs post-connection setup tasks
     *
     * @param device The connected device
     */
    private suspend fun performPostConnectionSetup(device: Device) {
        withContext(dispatcher) {
            try {
                Timber.d("Performing post-connection setup for device: ${device.name}")
                
                // Update device features if needed
                if (device.features.isEmpty()) {
                    Timber.d("Detecting device features")
                    // This would typically involve querying the device for its capabilities
                    // For now, we'll just use what we have in the device object
                }
                
                // Sync device time
                Timber.d("Syncing device time")
                // This would typically involve setting the device's time to match the phone's time
                
                // Sync device settings
                Timber.d("Syncing device settings")
                val deviceSettings = deviceRepository.getDeviceSettings(device.id).firstOrNull()
                if (deviceSettings != null) {
                    deviceRepository.updateDeviceSettings(device.id, deviceSettings)
                }
                
                // Check battery level
                Timber.d("Checking device battery level")
                // This would typically involve querying the device for its battery level
                
                Timber.d("Post-connection setup completed successfully")
            } catch (e: Exception) {
                Timber.e(e, "Error during post-connection setup")
                // We don't fail the connection if post-setup fails
                // Just log the error and continue
            }
        }
    }

    /**
     * Monitors the connection status of a device
     * This is meant to be called from a coroutine scope that can outlive this use case
     *
     * @param deviceId The ID of the device to monitor
     */
    private suspend fun monitorConnectionStatus(deviceId: Long) {
        // This would typically be implemented to start a background monitoring process
        // For now, we'll just log that monitoring has started
        Timber.d("Started monitoring connection status for device: $deviceId")
        
        // In a real implementation, this would collect from a Flow and react to connection state changes
        // For example:
        /*
        deviceRepository.getDeviceConnectionState(deviceId)
            .collect { connectionState ->
                when (connectionState) {
                    DeviceConnectionState.DISCONNECTED -> {
                        Timber.d("Device disconnected: $deviceId")
                        // Handle disconnection (e.g., notify user, attempt reconnection)
                    }
                    DeviceConnectionState.CONNECTION_FAILED -> {
                        Timber.e("Device connection failed: $deviceId")
                        // Handle connection failure
                    }
                    else -> {
                        // Handle other states
                    }
                }
            }
        */
    }

    /**
     * Extension function to get the first value from a Flow or null if the Flow is empty
     */
    private suspend fun <T> Flow<T>.firstOrNull(): T? {
        var result: T? = null
        this.collect {
            result = it
            throw CollectionDoneException()
        }
        return result
    }

    /**
     * Exception used to short-circuit Flow collection after getting the first value
     */
    private class CollectionDoneException : Exception()
}
