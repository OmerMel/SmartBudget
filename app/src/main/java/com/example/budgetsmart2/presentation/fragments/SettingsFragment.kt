package com.example.budgetsmart2.presentation.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.budgetsmart2.R
import com.example.budgetsmart2.databinding.FragmentSettingsBinding
import com.example.budgetsmart2.presentation.auth.LoginActivity
import com.example.budgetsmart2.utils.CurrencyHelper
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var currentUser: FirebaseUser? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase services
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        currentUser = auth.currentUser

        // Set up UI elements
        setupUI()

        // Set up click listeners
        setupClickListeners()
    }

    private fun setupUI() {
        // Check if user is signed in with email and password
        val isEmailPasswordUser = currentUser?.let { user ->
            user.providerData.any { it.providerId == EmailAuthProvider.PROVIDER_ID }
        } ?: false

        // Show change password only for email/password users
        binding.changePasswordContainer.visibility = if (isEmailPasswordUser) View.VISIBLE else View.GONE
        binding.passwordDivider.visibility = if (isEmailPasswordUser) View.VISIBLE else View.GONE

        // Show app version - using package info instead of BuildConfig
        try {
            val packageInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            binding.versionValue.text = packageInfo.versionName
        } catch (e: Exception) {
            binding.versionValue.text = "1.0.0"  // Fallback version
        }

        // Load current currency
        val currencyCode = CurrencyHelper.getUserCurrencyCode(requireContext())
        binding.currencyValue.text = CurrencyHelper.getCurrencyDisplayText(currencyCode)

        // Load user data from Firestore to get currency if available
        loadUserData()
    }

    private fun loadUserData() {
        currentUser?.let { user ->
            firestore.collection("users").document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val defaultCurrency = document.getString("defaultCurrency")
                        if (!defaultCurrency.isNullOrEmpty()) {
                            // Update local preference
                            CurrencyHelper.saveUserCurrencyCode(requireContext(), defaultCurrency)

                            // Update UI
                            binding.currencyValue.text = CurrencyHelper.getCurrencyDisplayText(defaultCurrency)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    // Handle error silently
                }
        }
    }

    private fun setupClickListeners() {
        // Currency preference
        binding.currencyPreference.setOnClickListener {
            showCurrencyPicker()
        }

        // Change password
        binding.changePasswordPreference.setOnClickListener {
            showChangePasswordDialog()
        }

        // Logout
        binding.logoutPreference.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun showCurrencyPicker() {
        val currencies = CurrencyHelper.SUPPORTED_CURRENCIES
        val currencyNames = currencies.map { "${it.code} - ${it.name}" }.toTypedArray()

        // Get current currency code
        val currentCurrencyCode = CurrencyHelper.getUserCurrencyCode(requireContext())
        val currentIndex = CurrencyHelper.getCurrencyIndex(currentCurrencyCode)

        AlertDialog.Builder(requireContext())
            .setTitle("Select Currency")
            .setSingleChoiceItems(currencyNames, currentIndex) { dialog, which ->
                val selectedCurrency = currencies[which]

                // Update UI immediately
                binding.currencyValue.text = currencyNames[which]

                // Save locally
                CurrencyHelper.saveUserCurrencyCode(requireContext(), selectedCurrency.code)

                // Update in Firestore
                updateUserCurrency(selectedCurrency.code)

                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun updateUserCurrency(currencyCode: String) {
        currentUser?.let { user ->
            firestore.collection("users").document(user.uid)
                .update("defaultCurrency", currencyCode)
                .addOnSuccessListener {
                    Toast.makeText(context, "Currency updated", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    // If the user document doesn't exist yet, create it
                    val userData = hashMapOf(
                        "defaultCurrency" to currencyCode,
                        "id" to user.uid
                    )

                    firestore.collection("users").document(user.uid)
                        .set(userData)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Currency updated", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { error ->
                            Toast.makeText(context, "Failed to update currency: ${error.message}", Toast.LENGTH_SHORT).show()
                        }
                }
        }
    }

    private fun showChangePasswordDialog() {
        // Create dialog with old password and new password fields
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_change_password, null)
        val oldPasswordEditText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.old_password_input)
        val newPasswordEditText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.new_password_input)
        val confirmPasswordEditText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.confirm_password_input)

        AlertDialog.Builder(requireContext())
            .setTitle("Change Password")
            .setView(dialogView)
            .setPositiveButton("Change") { dialog, _ ->
                val oldPassword = oldPasswordEditText.text.toString()
                val newPassword = newPasswordEditText.text.toString()
                val confirmPassword = confirmPasswordEditText.text.toString()

                if (oldPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                    Toast.makeText(context, "All fields are required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (newPassword != confirmPassword) {
                    Toast.makeText(context, "New passwords don't match", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (newPassword.length < 6) {
                    Toast.makeText(context, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                changePassword(oldPassword, newPassword)
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun changePassword(oldPassword: String, newPassword: String) {
        currentUser?.let { user ->
            // Get email
            val email = user.email
            if (email != null) {
                // Reauthenticate
                val credential = EmailAuthProvider.getCredential(email, oldPassword)

                user.reauthenticate(credential)
                    .addOnSuccessListener {
                        // Change password
                        user.updatePassword(newPassword)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Password updated successfully", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { exception ->
                                Toast.makeText(context, "Failed to update password: ${exception.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(context, "Current password is incorrect", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                logoutUser()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun logoutUser() {
        // Sign out from Firebase
        auth.signOut()

        // Navigate to Login activity
        startActivity(Intent(requireContext(), LoginActivity::class.java))
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}