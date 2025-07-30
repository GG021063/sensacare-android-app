package com.sensacare.app

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * LoginActivity
 *
 * Handles user authentication for the SensaCare app.
 * Provides email/password login functionality with validation
 * and navigation to appropriate screens.
 */
class LoginActivity : AppCompatActivity() {

    // UI Components
    private lateinit var emailInputLayout: TextInputLayout
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordInputLayout: TextInputLayout
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var showPasswordButton: ImageButton
    private lateinit var loginButton: Button
    private lateinit var forgotPasswordText: TextView
    private lateinit var signUpText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var backButton: ImageButton

    // Password visibility state
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize UI components
        initializeViews()

        // Set up listeners
        setupListeners()

        // Check if user is already logged in
        if (checkExistingLogin()) {
            navigateToHome()
        }
    }

    /**
     * Initialize all UI components
     */
    private fun initializeViews() {
        // Input fields
        emailInputLayout = findViewById(R.id.tilEmail)
        emailEditText = findViewById(R.id.etEmail)
        passwordInputLayout = findViewById(R.id.tilPassword)
        passwordEditText = findViewById(R.id.etPassword)
        showPasswordButton = findViewById(R.id.btnShowPassword)

        // Buttons and text views
        loginButton = findViewById(R.id.btnLogin)
        forgotPasswordText = findViewById(R.id.tvForgotPassword)
        signUpText = findViewById(R.id.tvSignUp)
        progressBar = findViewById(R.id.progressBar)
        backButton = findViewById(R.id.btnBack)

        // Set SensaCare blue color for the login button
        loginButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.sensacare_blue)
    }

    /**
     * Set up listeners for user interactions
     */
    private fun setupListeners() {
        // Back button
        backButton.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        // Email field validation
        emailEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateEmail(s.toString())
            }
        })

        // Password field validation
        passwordEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validatePassword(s.toString())
            }
        })

        // Show/hide password
        showPasswordButton.setOnClickListener {
            togglePasswordVisibility()
        }

        // Login button
        loginButton.setOnClickListener {
            attemptLogin()
        }

        // Forgot password
        forgotPasswordText.setOnClickListener {
            // For now, just show a toast message
            Toast.makeText(this, "Password reset feature coming soon!", Toast.LENGTH_SHORT).show()
        }

        // Sign up link
        signUpText.setOnClickListener {
            navigateToRegister()
        }
    }

    /**
     * Validate email format
     */
    private fun validateEmail(email: String): Boolean {
        return if (email.isEmpty()) {
            emailInputLayout.error = "Email cannot be empty"
            false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout.error = "Please enter a valid email address"
            false
        } else {
            emailInputLayout.error = null
            true
        }
    }

    /**
     * Validate password
     */
    private fun validatePassword(password: String): Boolean {
        return if (password.isEmpty()) {
            passwordInputLayout.error = "Password cannot be empty"
            false
        } else if (password.length < 6) {
            passwordInputLayout.error = "Password must be at least 6 characters"
            false
        } else {
            passwordInputLayout.error = null
            true
        }
    }

    /**
     * Toggle password visibility
     */
    private fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible
        
        if (isPasswordVisible) {
            // Show password
            passwordEditText.transformationMethod = null
            showPasswordButton.setImageResource(R.drawable.ic_visibility_off)
        } else {
            // Hide password
            passwordEditText.transformationMethod = PasswordTransformationMethod.getInstance()
            showPasswordButton.setImageResource(R.drawable.ic_visibility)
        }
        
        // Move cursor to the end of text
        passwordEditText.setSelection(passwordEditText.text?.length ?: 0)
    }

    /**
     * Attempt to login with provided credentials
     */
    private fun attemptLogin() {
        val email = emailEditText.text.toString()
        val password = passwordEditText.text.toString()
        
        // Validate input
        val isEmailValid = validateEmail(email)
        val isPasswordValid = validatePassword(password)
        
        if (isEmailValid && isPasswordValid) {
            // Show loading state
            showLoading(true)
            
            // Simulate network delay (replace with actual authentication in production)
            simulateAuthentication(email, password)
        }
    }

    /**
     * Simulate authentication process
     * In a real app, this would make an API call to a backend
     */
    private fun simulateAuthentication(email: String, password: String) {
        // Simulate network delay
        loginButton.postDelayed({
            // For demo purposes, any non-empty credentials are accepted
            if (email.isNotEmpty() && password.isNotEmpty()) {
                // Save login state
                saveLoginState(email)
                
                // Navigate to home screen
                navigateToHome()
            } else {
                // Show error
                Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show()
            }
            
            // Hide loading state
            showLoading(false)
        }, 1500) // 1.5 second delay to simulate network
    }

    /**
     * Save login state to SharedPreferences
     */
    private fun saveLoginState(email: String) {
        val sharedPreferences = getSharedPreferences("sensacare_prefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean("is_logged_in", true)
        editor.putString("user_email", email)
        editor.apply()
    }

    /**
     * Check if user is already logged in
     */
    private fun checkExistingLogin(): Boolean {
        val sharedPreferences = getSharedPreferences("sensacare_prefs", MODE_PRIVATE)
        return sharedPreferences.getBoolean("is_logged_in", false)
    }

    /**
     * Show or hide loading state
     */
    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            progressBar.visibility = View.VISIBLE
            loginButton.isEnabled = false
            loginButton.text = ""
        } else {
            progressBar.visibility = View.GONE
            loginButton.isEnabled = true
            loginButton.text = getString(R.string.login)
        }
    }

    /**
     * Navigate to HomeActivity
     */
    private fun navigateToHome() {
        try {
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            // Fallback to MainActivity if HomeActivity fails for any reason
            try {
                val fallbackIntent = Intent(this, MainActivity::class.java)
                fallbackIntent.flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(fallbackIntent)
                finish()
            } catch (e2: Exception) {
                // Show an error message and allow the user to try logging in again
                Toast.makeText(
                    this,
                    "Error navigating to home: ${e.message ?: "Unknown error"}",
                    Toast.LENGTH_LONG
                ).show()
                showLoading(false)
            }
        }
    }

    /**
     * Navigate to RegisterActivity
     */
    private fun navigateToRegister() {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
        // Optional slide animation
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    /**
     * Handle back button press
     */
    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    companion object {
        private const val TAG = "LoginActivity"
    }
}
