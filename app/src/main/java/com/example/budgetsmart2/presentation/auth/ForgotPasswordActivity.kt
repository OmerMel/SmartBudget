package com.example.budgetsmart2.presentation.auth

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.budgetsmart2.databinding.ActivityForgotPasswordBinding
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    private lateinit var auth: FirebaseAuth

    companion object {
        private const val TAG = "ForgotPasswordActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Set up click listeners
        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Back button click
        binding.backButton.setOnClickListener {
            onBackPressed()
        }

        // Send reset link button click
        binding.sendResetLinkButton.setOnClickListener {
            val email = binding.emailInput.text.toString().trim()

            if (validateEmail(email)) {
                sendPasswordResetEmail(email)
            }
        }

        // Back to login button click (in success state)
        binding.backToLoginButton.setOnClickListener {
            finish()
        }
    }

    private fun validateEmail(email: String): Boolean {
        if (email.isEmpty()) {
            binding.emailInputLayout.error = "Email is required"
            return false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailInputLayout.error = "Enter a valid email address"
            return false
        } else {
            binding.emailInputLayout.error = null
            return true
        }
    }

    private fun sendPasswordResetEmail(email: String) {
        showProgress(true)

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                showProgress(false)

                if (task.isSuccessful) {
                    // Email sent
                    Log.d(TAG, "Email sent.")
                    showSuccessState()
                } else {
                    // Email not sent
                    Log.w(TAG, "sendPasswordResetEmail:failure", task.exception)
                    Toast.makeText(
                        baseContext,
                        "Failed to send reset email: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun showSuccessState() {
        binding.resetFormContainer.visibility = View.GONE
        binding.successContainer.visibility = View.VISIBLE
    }

    private fun showProgress(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE

        // Only disable form elements if we're not showing success state
        if (binding.successContainer.visibility != View.VISIBLE) {
            binding.resetFormContainer.visibility = if (show) View.GONE else View.VISIBLE
        }
    }
}