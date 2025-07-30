package com.sensacare.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * WelcomeActivity
 *
 * The landing screen and main entry point for the SensaCare app.
 * Displays app branding, feature highlights, and provides navigation
 * to login and registration screens.
 */
class WelcomeActivity : AppCompatActivity() {

    // UI Components
    private var welcomeTextView: TextView? = null
    private var brandNameTextView: TextView? = null
    private var subtitleTextView: TextView? = null
    private var getStartedButton: Button? = null
    private var registerTextView: TextView? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)
        
        // Initialize UI components
        initializeViews()
        
        // Set up navigation buttons
        setupButtons()
    }
    
    /**
     * Initialize all UI components
     */
    private fun initializeViews() {
        try {
            // Main components from new layout
            welcomeTextView = findViewById(R.id.tvWelcome)
            brandNameTextView = findViewById(R.id.tvBrandName)
            subtitleTextView = findViewById(R.id.tvSubtitle)

            // Buttons
            getStartedButton = findViewById(R.id.btnGetStarted)
            registerTextView = findViewById(R.id.tvRegister)
        } catch (e: Exception) {
            Log.e(TAG, "initializeViews: view binding failed", e)
        }
    }
    
    /**
     * Set up navigation buttons
     */
    private fun setupButtons() {
        // Get Started button -> Login
        getStartedButton?.setOnClickListener {
            try {
                navigateToLogin()
            } catch (e: Exception) {
                Log.e(TAG, "Error navigating to Login", e)
            }
        }
        
        // Register text -> Register
        registerTextView?.setOnClickListener {
            try {
                navigateToRegister()
            } catch (e: Exception) {
                Log.e(TAG, "Error navigating to Register", e)
            }
        }
    }
    
    /**
     * Navigate to RegisterActivity
     */
    private fun navigateToRegister() {
        val intent = Intent(this, RegisterActivity::class.java)
        safeStartActivity(intent)
    }
    
    /**
     * Navigate to LoginActivity
     */
    private fun navigateToLogin() {
        // TEMP-DEBUG: Skip authentication flow while we stabilise HomeActivity.
        // Directly launch HomeActivity so we can debug crashes that happen after landing.
        // TODO: Re-enable LoginActivity when authentication is ready.
        val intent = Intent(this, HomeActivity::class.java)
        safeStartActivity(intent)
    }
    
    /**
     * Skip login/register for development purposes
     * This can be removed in production
     */
    private fun skipToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        safeStartActivity(intent)
        finish() // Close welcome screen
    }

    /**
     * Helper to start activity safely with exception handling
     */
    private fun safeStartActivity(intent: Intent) {
        try {
            startActivity(intent)
            // Removed custom transition while debugging crashes
        } catch (e: Exception) {
            Log.e(TAG, "safeStartActivity: failed to start ${intent.component}", e)
        }
    }
    
    /**
     * Check if user is already logged in
     * This would typically check shared preferences or a user session
     */
    private fun checkExistingLogin(): Boolean {
        // Placeholder for actual login check logic
        val sharedPreferences = getSharedPreferences("sensacare_prefs", MODE_PRIVATE)
        return sharedPreferences.getBoolean("is_logged_in", false)
    }
    
    companion object {
        private const val TAG = "WelcomeActivity"
    }
}
