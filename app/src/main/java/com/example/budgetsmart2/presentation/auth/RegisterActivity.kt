package com.example.budgetsmart2.presentation.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.budgetsmart2.MainActivity
import com.example.budgetsmart2.R
import com.example.budgetsmart2.databinding.ActivityRegisterBinding
import com.example.budgetsmart2.domain.dataClasses.User
import com.example.budgetsmart2.domain.repositories.UserRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject

@AndroidEntryPoint
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    @Inject
    lateinit var userRepository: UserRepository

    companion object {
        private const val TAG = "RegisterActivity"
    }

    // Register for Google Sign-In result
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleGoogleSignInResult(task)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Set up click listeners
        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Back button click
        binding.backButton.setOnClickListener {
            onBackPressed()
        }

        // Register button click
        binding.registerButton.setOnClickListener {
            val name = binding.nameInput.text.toString().trim()
            val email = binding.emailInput.text.toString().trim()
            val password = binding.passwordInput.text.toString()
            val confirmPassword = binding.confirmPasswordInput.text.toString()

            if (validateRegisterInputs(name, email, password, confirmPassword)) {
                registerWithEmailPassword(name, email, password)
            }
        }

        // Google sign up button click
        binding.googleSignUpButton.setOnClickListener {
            signUpWithGoogle()
        }

        // Login link click
        binding.loginLink.setOnClickListener {
            finish()
        }
    }

    private fun validateRegisterInputs(
        name: String,
        email: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        var isValid = true

        // Validate name
        if (name.isEmpty()) {
            binding.nameInputLayout.error = "Name is required"
            isValid = false
        } else {
            binding.nameInputLayout.error = null
        }

        // Validate email
        if (email.isEmpty()) {
            binding.emailInputLayout.error = "Email is required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailInputLayout.error = "Enter a valid email address"
            isValid = false
        } else {
            binding.emailInputLayout.error = null
        }

        // Validate password
        if (password.isEmpty()) {
            binding.passwordInputLayout.error = "Password is required"
            isValid = false
        } else if (password.length < 8) {
            binding.passwordInputLayout.error = "Password must be at least 8 characters"
            isValid = false
        } else {
            binding.passwordInputLayout.error = null
        }

        // Validate confirm password
        if (confirmPassword.isEmpty()) {
            binding.confirmPasswordInputLayout.error = "Please confirm your password"
            isValid = false
        } else if (password != confirmPassword) {
            binding.confirmPasswordInputLayout.error = "Passwords do not match"
            isValid = false
        } else {
            binding.confirmPasswordInputLayout.error = null
        }

        // Validate privacy policy agreement
        if (!binding.privacyPolicyCheckbox.isChecked) {
            Toast.makeText(
                this,
                "Please agree to the Terms and Privacy Policy",
                Toast.LENGTH_SHORT
            ).show()
            isValid = false
        }

        return isValid
    }

    private fun registerWithEmailPassword(name: String, email: String, password: String) {
        showProgress(true)

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign up success
                    Log.d(TAG, "createUserWithEmail:success")
                    val firebaseUser = auth.currentUser

                    // Update user profile with name
                    val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()

                    firebaseUser?.updateProfile(profileUpdates)
                        ?.addOnCompleteListener { profileTask ->
                            if (profileTask.isSuccessful) {
                                Log.d(TAG, "User profile updated.")
                            } else {
                                Log.w(TAG, "Failed to update user profile", profileTask.exception)
                            }

                            // Create user in our database
                            if (firebaseUser != null) {
                                createUserInDatabase(firebaseUser.uid)
                            } else {
                                showProgress(false)
                                navigateToMainActivity()
                            }
                        }
                } else {
                    // Sign up failed
                    showProgress(false)
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)

                    val errorMessage = when (task.exception) {
                        is FirebaseAuthUserCollisionException -> "Email is already in use"
                        is FirebaseAuthWeakPasswordException -> "Password is too weak"
                        else -> "Registration failed: ${task.exception?.message}"
                    }

                    Toast.makeText(baseContext, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun signUpWithGoogle() {
        showProgress(true)
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun handleGoogleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            Log.d(TAG, "Google Sign-In successful")
            firebaseAuthWithGoogle(account)
        } catch (e: ApiException) {
            // Google Sign In failed
            Log.w(TAG, "Google sign in failed", e)
            showProgress(false)
            Toast.makeText(this, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success
                    Log.d(TAG, "signInWithCredential:success")
                    val user = auth.currentUser

                    // Check if this is a new user
                    val isNewUser = task.result?.additionalUserInfo?.isNewUser ?: false

                    if (isNewUser && user != null) {
                        // Create user in our database
                        createUserInDatabase(user.uid)
                    } else {
                        navigateToMainActivity()
                    }
                } else {
                    // Sign in failed
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    showProgress(false)
                    Toast.makeText(baseContext, "Authentication Failed: ${task.exception?.message}",
                        Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun createUserInDatabase(userId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val newUser = User(
                    id = userId,
                    defaultCurrency = "USD",
                    createdAt = Date()
                )

                userRepository.createUser(newUser)

                withContext(Dispatchers.Main) {
                    navigateToMainActivity()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating user in database", e)
                withContext(Dispatchers.Main) {
                    showProgress(false)
                    Toast.makeText(
                        baseContext,
                        "Error setting up your account: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun navigateToMainActivity() {
        showProgress(false)
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showProgress(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.registerFormContainer.visibility = if (show) View.GONE else View.VISIBLE
        binding.orDivider.visibility = if (show) View.GONE else View.VISIBLE
        binding.googleSignUpButton.visibility = if (show) View.GONE else View.VISIBLE
        binding.loginPrompt.visibility = if (show) View.GONE else View.VISIBLE
        binding.loginLink.visibility = if (show) View.GONE else View.VISIBLE
    }
}