package com.sensacare.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sensacare.app.data.local.preferences.UserPreferences
import com.sensacare.app.data.model.auth.AuthResponse
import com.sensacare.app.data.remote.AuthState
import com.sensacare.app.data.remote.PlatformEvent
import com.sensacare.app.data.remote.PlatformIntegrationManager
import com.sensacare.app.data.remote.api.PlatformType
import com.sensacare.app.data.remote.api.UserProfile
import com.sensacare.app.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * Authentication ViewModel for SensaCare app
 * Handles authentication, platform selection, and user profile management
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val platformIntegrationManager: PlatformIntegrationManager,
    private val userPreferences: UserPreferences
) : ViewModel() {

    // Authentication UI state
    private val _authUiState = MutableStateFlow<AuthUiState>(AuthUiState.Initial)
    val authUiState: StateFlow<AuthUiState> = _authUiState.asStateFlow()
    
    // User profile state
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()
    
    // Platform selection state
    private val _selectedPlatform = MutableStateFlow(PlatformType.RPM)
    val selectedPlatform: StateFlow<PlatformType> = _selectedPlatform.asStateFlow()
    
    // MFA state
    private val _mfaState = MutableStateFlow<MfaState>(MfaState.NotRequired)
    val mfaState: StateFlow<MfaState> = _mfaState.asStateFlow()
    
    // One-time events
    private val _authEvents = MutableSharedFlow<AuthEvent>()
    val authEvents: SharedFlow<AuthEvent> = _authEvents.asSharedFlow()
    
    // Input validation state
    private val _inputValidationState = MutableStateFlow(InputValidationState())
    val inputValidationState: StateFlow<InputValidationState> = _inputValidationState.asStateFlow()
    
    init {
        Timber.d("Initializing AuthViewModel")
        
        // Observe auth state from platform integration manager
        viewModelScope.launch {
            platformIntegrationManager.authState.collect { authState ->
                Timber.d("Auth state changed: $authState")
                
                when (authState) {
                    is AuthState.NotAuthenticated -> {
                        _authUiState.value = AuthUiState.NotAuthenticated
                        _userProfile.value = null
                    }
                    is AuthState.Authenticating -> {
                        _authUiState.value = AuthUiState.Authenticating
                    }
                    is AuthState.Authenticated -> {
                        // Fetch user profile if authenticated
                        fetchUserProfile()
                        
                        _authUiState.value = AuthUiState.Authenticated(
                            expiresAt = authState.expiresAt
                        )
                    }
                    is AuthState.Error -> {
                        _authUiState.value = AuthUiState.Error(authState.message)
                    }
                }
            }
        }
        
        // Observe platform events
        viewModelScope.launch {
            platformIntegrationManager.platformEvents.collect { event ->
                when (event) {
                    is PlatformEvent.LoggedIn -> {
                        _authEvents.emit(AuthEvent.LoginSuccess)
                    }
                    is PlatformEvent.LoggedOut -> {
                        _authEvents.emit(AuthEvent.LogoutSuccess)
                    }
                    is PlatformEvent.AuthenticationFailed -> {
                        _authEvents.emit(AuthEvent.LoginError(event.reason))
                    }
                    is PlatformEvent.TokenRefreshFailed -> {
                        _authEvents.emit(AuthEvent.SessionExpired(event.reason))
                    }
                    is PlatformEvent.LogoutRequested -> {
                        _authEvents.emit(AuthEvent.LogoutRequested)
                    }
                    else -> {
                        // Ignore other events
                    }
                }
            }
        }
        
        // Load saved platform
        viewModelScope.launch {
            val savedPlatform = userPreferences.getSelectedPlatform()
            if (savedPlatform != null) {
                _selectedPlatform.value = savedPlatform
                platformIntegrationManager.switchPlatform(savedPlatform)
            }
        }
    }
    
    /**
     * Login with email and password
     */
    fun login(email: String, password: String) {
        Timber.d("Login attempt: $email")
        
        // Validate input
        val validationResult = validateLoginInput(email, password)
        _inputValidationState.value = validationResult
        
        if (!validationResult.isValid) {
            Timber.d("Login input validation failed")
            return
        }
        
        // Update UI state
        _authUiState.value = AuthUiState.Authenticating
        
        // Perform login
        viewModelScope.launch {
            try {
                val result = platformIntegrationManager.login(email, password)
                
                when (result) {
                    is Result.Success -> {
                        Timber.d("Login successful")
                        
                        // Check if MFA is required
                        if (result.data.requiresMfa) {
                            _mfaState.value = MfaState.Required
                            _authUiState.value = AuthUiState.MfaRequired
                            _authEvents.emit(AuthEvent.MfaRequired)
                        } else {
                            _mfaState.value = MfaState.NotRequired
                            _authEvents.emit(AuthEvent.LoginSuccess)
                        }
                    }
                    is Result.Error -> {
                        Timber.e(result.exception, "Login failed")
                        _authUiState.value = AuthUiState.Error(result.exception.message ?: "Login failed")
                        _authEvents.emit(AuthEvent.LoginError(result.exception.message ?: "Login failed"))
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Login failed with exception")
                _authUiState.value = AuthUiState.Error(e.message ?: "Login failed")
                _authEvents.emit(AuthEvent.LoginError(e.message ?: "Login failed"))
            }
        }
    }
    
    /**
     * Verify MFA code
     */
    fun verifyMfa(code: String) {
        Timber.d("Verifying MFA code")
        
        // Validate input
        if (code.isBlank() || code.length < 6) {
            _authEvents.emit(AuthEvent.MfaError("Invalid MFA code"))
            return
        }
        
        // Update UI state
        _authUiState.value = AuthUiState.Authenticating
        
        // Verify MFA code
        viewModelScope.launch {
            try {
                val result = platformIntegrationManager.verifyMfa(code)
                
                when (result) {
                    is Result.Success -> {
                        Timber.d("MFA verification successful")
                        _mfaState.value = MfaState.Verified
                        _authEvents.emit(AuthEvent.MfaVerified)
                        _authEvents.emit(AuthEvent.LoginSuccess)
                    }
                    is Result.Error -> {
                        Timber.e(result.exception, "MFA verification failed")
                        _mfaState.value = MfaState.Error(result.exception.message ?: "MFA verification failed")
                        _authEvents.emit(AuthEvent.MfaError(result.exception.message ?: "MFA verification failed"))
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "MFA verification failed with exception")
                _mfaState.value = MfaState.Error(e.message ?: "MFA verification failed")
                _authEvents.emit(AuthEvent.MfaError(e.message ?: "MFA verification failed"))
            }
        }
    }
    
    /**
     * Logout
     */
    fun logout() {
        Timber.d("Logging out")
        
        viewModelScope.launch {
            try {
                val result = platformIntegrationManager.logout()
                
                when (result) {
                    is Result.Success -> {
                        Timber.d("Logout successful")
                        _authEvents.emit(AuthEvent.LogoutSuccess)
                    }
                    is Result.Error -> {
                        Timber.e(result.exception, "Logout failed")
                        _authEvents.emit(AuthEvent.LogoutError(result.exception.message ?: "Logout failed"))
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Logout failed with exception")
                _authEvents.emit(AuthEvent.LogoutError(e.message ?: "Logout failed"))
            }
        }
    }
    
    /**
     * Switch platform
     */
    fun switchPlatform(platformType: PlatformType) {
        Timber.d("Switching to platform: $platformType")
        
        // Update selected platform
        _selectedPlatform.value = platformType
        
        // Switch platform in integration manager
        platformIntegrationManager.switchPlatform(platformType)
        
        // Save selected platform
        viewModelScope.launch {
            userPreferences.saveSelectedPlatform(platformType)
        }
    }
    
    /**
     * Fetch user profile
     */
    fun fetchUserProfile() {
        Timber.d("Fetching user profile")
        
        viewModelScope.launch {
            try {
                val result = platformIntegrationManager.getApiService().getUserProfile()
                
                when (result) {
                    is Result.Success -> {
                        Timber.d("User profile fetched successfully")
                        _userProfile.value = result.data
                    }
                    is Result.Error -> {
                        Timber.e(result.exception, "Failed to fetch user profile")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch user profile with exception")
            }
        }
    }
    
    /**
     * Update user profile
     */
    fun updateUserProfile(profile: UserProfile) {
        Timber.d("Updating user profile")
        
        viewModelScope.launch {
            try {
                val result = platformIntegrationManager.getApiService().updateUserProfile(profile)
                
                when (result) {
                    is Result.Success -> {
                        Timber.d("User profile updated successfully")
                        _userProfile.value = result.data
                        _authEvents.emit(AuthEvent.ProfileUpdated)
                    }
                    is Result.Error -> {
                        Timber.e(result.exception, "Failed to update user profile")
                        _authEvents.emit(AuthEvent.ProfileUpdateError(result.exception.message ?: "Failed to update profile"))
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to update user profile with exception")
                _authEvents.emit(AuthEvent.ProfileUpdateError(e.message ?: "Failed to update profile"))
            }
        }
    }
    
    /**
     * Reset MFA state
     */
    fun resetMfaState() {
        _mfaState.value = MfaState.NotRequired
    }
    
    /**
     * Reset error state
     */
    fun resetErrorState() {
        if (_authUiState.value is AuthUiState.Error) {
            _authUiState.value = AuthUiState.NotAuthenticated
        }
    }
    
    /**
     * Reset input validation state
     */
    fun resetInputValidationState() {
        _inputValidationState.value = InputValidationState()
    }
    
    /**
     * Validate login input
     */
    private fun validateLoginInput(email: String, password: String): InputValidationState {
        val emailError = when {
            email.isBlank() -> "Email is required"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Invalid email format"
            else -> null
        }
        
        val passwordError = when {
            password.isBlank() -> "Password is required"
            password.length < 8 -> "Password must be at least 8 characters"
            else -> null
        }
        
        return InputValidationState(
            emailError = emailError,
            passwordError = passwordError,
            isValid = emailError == null && passwordError == null
        )
    }
}

/**
 * Authentication UI state
 */
sealed class AuthUiState {
    object Initial : AuthUiState()
    object NotAuthenticated : AuthUiState()
    object Authenticating : AuthUiState()
    data class Authenticated(val expiresAt: LocalDateTime) : AuthUiState()
    object MfaRequired : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

/**
 * MFA state
 */
sealed class MfaState {
    object NotRequired : MfaState()
    object Required : MfaState()
    object Verified : MfaState()
    data class Error(val message: String) : MfaState()
}

/**
 * Authentication events
 */
sealed class AuthEvent {
    object LoginSuccess : AuthEvent()
    data class LoginError(val message: String) : AuthEvent()
    object LogoutSuccess : AuthEvent()
    data class LogoutError(val message: String) : AuthEvent()
    object MfaRequired : AuthEvent()
    object MfaVerified : AuthEvent()
    data class MfaError(val message: String) : AuthEvent()
    data class SessionExpired(val message: String) : AuthEvent()
    object ProfileUpdated : AuthEvent()
    data class ProfileUpdateError(val message: String) : AuthEvent()
    object LogoutRequested : AuthEvent()
}

/**
 * Input validation state
 */
data class InputValidationState(
    val emailError: String? = null,
    val passwordError: String? = null,
    val isValid: Boolean = true
)
