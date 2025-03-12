package com.example.budgetsmart2

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.budgetsmart2.databinding.ActivityMainBinding
import com.example.budgetsmart2.presentation.auth.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize view binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Set up Navigation Controller with the fragment container
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.fragment_container) as NavHostFragment
        navController = navHostFragment.navController

        // Connect the bottom navigation with the navigation controller
        binding.bottomNavigation.setupWithNavController(navController)

        // Handle navigation UI state based on destination changes
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.homeFragment, R.id.budgetFragment, R.id.reportsFragment, R.id.categoryManagementFragment -> {
                    // Show bottom navigation for main screens
                    binding.bottomNavigation.visibility = android.view.View.VISIBLE
                }
                else -> {
                    // Default to showing the bottom navigation
                    binding.bottomNavigation.visibility = android.view.View.VISIBLE
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly
        val currentUser = auth.currentUser
        if (currentUser == null) {
             startActivity(Intent(this, LoginActivity::class.java))
             finish()
        }
    }

    // Back button handling for nested fragments
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}