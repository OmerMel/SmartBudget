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
import com.example.budgetsmart2.databinding.ActivityLoginBinding
import com.example.budgetsmart2.domain.dataClasses.User
import com.example.budgetsmart2.domain.repositories.UserRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    @Inject
    lateinit var userRepository: UserRepository

    companion object {
        private const val TAG = "LoginActivity"
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
        binding = ActivityLoginBinding.inflate(layoutInflater)
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

    override fun onStart() {
        super.onStart()
        // Check if user is already signed in
        val currentUser = auth.currentUser
        if (currentUser != null) {
            navigateToMainActivity()
        }
    }

    private fun setupClickListeners() {
        // Login button click
        binding.loginButton.setOnClickListener {
            val email = binding.emailInput.text.toString().trim()
            val password = binding.passwordInput.text.toString().trim()

            if (validateLoginInputs(email, password)) {
                loginWithEmailPassword(email, password)
            }
        }

        // Forgot password click
        binding.forgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        // Google sign in button click
        binding.googleSignInButton.setOnClickListener {
            signInWithGoogle()
        }

        // Register link click
        binding.registerLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun validateLoginInputs(email: String, password: String): Boolean {
        var isValid = true

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
        } else {
            binding.passwordInputLayout.error = null
        }

        return isValid
    }

    private fun loginWithEmailPassword(email: String, password: String) {
        showProgress(true)

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success
                    Log.d(TAG, "signInWithEmail:success")
                    navigateToMainActivity()
                } else {
                    // Sign in failed
                    showProgress(false)
                    Log.w(TAG, "signInWithEmail:failure", task.exception)

                    val errorMessage = when (task.exception) {
                        is FirebaseAuthInvalidUserException -> "User does not exist"
                        is FirebaseAuthInvalidCredentialsException -> "Invalid password"
                        else -> "Authentication failed: ${task.exception?.message}"
                    }

                    Toast.makeText(baseContext, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun signInWithGoogle() {
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
        binding.loginFormContainer.visibility = if (show) View.GONE else View.VISIBLE
        binding.orDivider.visibility = if (show) View.GONE else View.VISIBLE
        binding.googleSignInButton.visibility = if (show) View.GONE else View.VISIBLE
        binding.registerPrompt.visibility = if (show) View.GONE else View.VISIBLE
        binding.registerLink.visibility = if (show) View.GONE else View.VISIBLE
    }
}