package com.sensacare.app

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * RegisterActivity
 *
 * Handles user registration for the SensaCare app.
 * Provides a comprehensive registration form with validation
 * and creates a new user account.
 */
class RegisterActivity : AppCompatActivity() {

    // UI Components - Input fields
    private lateinit var firstNameInputLayout: TextInputLayout
    private lateinit var firstNameEditText: TextInputEditText
    private lateinit var lastNameInputLayout: TextInputLayout
    private lateinit var lastNameEditText: TextInputEditText
    private lateinit var emailInputLayout: TextInputLayout
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordInputLayout: TextInputLayout
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var confirmPasswordInputLayout: TextInputLayout
    private lateinit var confirmPasswordEditText: TextInputEditText

    // UI Components - Buttons and indicators
    private lateinit var showPasswordButton: ImageButton
    private lateinit var showConfirmPasswordButton: ImageButton
    private lateinit var registerButton: Button
    private lateinit var signInText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var backButton: ImageButton

    // Password visibility states
    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialize UI components
        initializeViews()

        // Set up listeners
        setupListeners()
    }

    /**
     * Initialize all UI components
     */
    private fun initializeViews() {
        // Input fields
        firstNameInputLayout = findViewById(R.id.tilFirstName)
        firstNameEditText = findViewById(R.id.etFirstName)
        lastNameInputLayout = findViewById(R.id.tilLastName)
        lastNameEditText = findViewById(R.id.etLastName)
        emailInputLayout = findViewById(R.id.tilEmail)
        emailEditText = findViewById(R.id.etEmail)
        passwordInputLayout = findViewById(R.id.tilPassword)
        passwordEditText = findViewById(R.id.etPassword)
        confirmPasswordInputLayout = findViewById(R.id.tilConfirmPassword)
        confirmPasswordEditText = findViewById(R.id.etConfirmPassword)

        // Buttons and indicators
        showPasswordButton = findViewById(R.id.btnShowPassword)
        showConfirmPasswordButton = findViewById(R.id.btnShowConfirmPassword)
        registerButton = findViewById(R.id.btnRegister)
        signInText = findViewById(R.id.tvSignIn)
        progressBar = findViewById(R.id.progressBar)
        backButton = findViewById(R.id.btnBack)

        // Set SensaCare yellow color for the register button
        registerButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.sensacare_yellow)
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

        // First name field validation
        firstNameEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateFirstName(s.toString())
            }
        })

        // Last name field validation
        lastNameEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateLastName(s.toString())
            }
        })

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
                // Also validate confirm password field if it's not empty
                if (!confirmPasswordEditText.text.isNullOrEmpty()) {
                    validateConfirmPassword(confirmPasswordEditText.text.toString())
                }
            }
        })

        // Confirm password field validation
        confirmPasswordEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateConfirmPassword(s.toString())
            }
        })

        // Show/hide password
        showPasswordButton.setOnClickListener {
            togglePasswordVisibility()
        }

        // Show/hide confirm password
        showConfirmPasswordButton.setOnClickListener {
            toggleConfirmPasswordVisibility()
        }

        // Register button
        registerButton.setOnClickListener {
            attemptRegistration()
        }

        // Sign in link
        signInText.setOnClickListener {
            navigateToLogin()
        }
    }

    /**
     * Validate first name
     */
    private fun validateFirstName(firstName: String): Boolean {
        return if (firstName.isEmpty()) {
            firstNameInputLayout.error = "First name cannot be empty"
            false
        } else {
            firstNameInputLayout.error = null
            true
        }
    }

    /**
     * Validate last name
     */
    private fun validateLastName(lastName: String): Boolean {
        return if (lastName.isEmpty()) {
            lastNameInputLayout.error = "Last name cannot be empty"
            false
        } else {
            lastNameInputLayout.error = null
            true
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
     * Validate confirm password
     */
    private fun validateConfirmPassword(confirmPassword: String): Boolean {
        val password = passwordEditText.text.toString()
        
        return if (confirmPassword.isEmpty()) {
            confirmPasswordInputLayout.error = "Please confirm your password"
            false
        } else if (confirmPassword != password) {
            confirmPasswordInputLayout.error = "Passwords do not match"
            false
        } else {
            confirmPasswordInputLayout.error = null
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
     * Toggle confirm password visibility
     */
    private fun toggleConfirmPasswordVisibility() {
        isConfirmPasswordVisible = !isConfirmPasswordVisible
        
        if (isConfirmPasswordVisible) {
            // Show password
            confirmPasswordEditText.transformationMethod = null
            showConfirmPasswordButton.setImageResource(R.drawable.ic_visibility_off)
        } else {
            // Hide password
            confirmPasswordEditText.transformationMethod = PasswordTransformationMethod.getInstance()
            showConfirmPasswordButton.setImageResource(R.drawable.ic_visibility)
        }
        
        // Move cursor to the end of text
        confirmPasswordEditText.setSelection(confirmPasswordEditText.text?.length ?: 0)
    }

    /**
     * Attempt to register with provided information
     */
    private fun attemptRegistration() {
        val firstName = firstNameEditText.text.toString()
        val lastName = lastNameEditText.text.toString()
        val email = emailEditText.text.toString()
        val password = passwordEditText.text.toString()
        val confirmPassword = confirmPasswordEditText.text.toString()
        
        // Validate all input fields
        val isFirstNameValid = validateFirstName(firstName)
        val isLastNameValid = validateLastName(lastName)
        val isEmailValid = validateEmail(email)
        val isPasswordValid = validatePassword(password)
        val isConfirmPasswordValid = validateConfirmPassword(confirmPassword)
        
        if (isFirstNameValid && isLastNameValid && isEmailValid && 
            isPasswordValid && isConfirmPasswordValid) {
            
            // Show loading state
            showLoading(true)
            
            // Simulate network delay (replace with actual registration in production)
            simulateRegistration(firstName, lastName, email, password)
        }
    }

    /**
     * Simulate registration process
     * In a real app, this would make an API call to a backend
     */
    private fun simulateRegistration(firstName: String, lastName: String, email: String, password: String) {
        // Simulate network delay
        registerButton.postDelayed({
            // For demo purposes, any valid input is accepted
            // Save user data
            saveUserData(firstName, lastName, email)
            
            // Show success message
            Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
            
            // Navigate to home screen
            navigateToHome()
            
            // Hide loading state
            showLoading(false)
        }, 2000) // 2 second delay to simulate network
    }

    /**
     * Save user data to SharedPreferences
     */
    private fun saveUserData(firstName: String, lastName: String, email: String) {
        val sharedPreferences = getSharedPreferences("sensacare_prefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean("is_logged_in", true)
        editor.putString("user_email", email)
        editor.putString("user_first_name", firstName)
        editor.putString("user_last_name", lastName)
        editor.putString("user_full_name", "$firstName $lastName")
        editor.apply()
    }

    /**
     * Show or hide loading state
     */
    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            progressBar.visibility = View.VISIBLE
            registerButton.isEnabled = false
            registerButton.text = ""
        } else {
            progressBar.visibility = View.GONE
            registerButton.isEnabled = true
            registerButton.text = getString(R.string.register)
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
            // Fallback to MainActivity if HomeActivity fails
            try {
                val fallbackIntent = Intent(this, MainActivity::class.java)
                fallbackIntent.flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(fallbackIntent)
                finish()
            } catch (e2: Exception) {
                Toast.makeText(
                    this,
                    "Error navigating to home: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                showLoading(false)
            }
        }
    }

    /**
     * Navigate to LoginActivity
     */
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        // Optional slide animation
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        finish() // Close registration screen
    }

    /**
     * Handle back button press
     */
    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    companion object {
        private const val TAG = "RegisterActivity"
    }
}
