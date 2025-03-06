package com.example.budgetsmart2

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.budgetsmart2.presentation.BudgetFragment
import com.example.budgetsmart2.presentation.HomeFragment
import com.example.budgetsmart2.presentation.ReportsFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var homeFragment: HomeFragment
    private lateinit var budgetFragment: BudgetFragment
    private lateinit var reportsFragment: ReportsFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        homeFragment = HomeFragment()
        budgetFragment = BudgetFragment()
        reportsFragment = ReportsFragment()

        findViews()
        replaceFragment(homeFragment)
        setupBottomNavigation()
    }

    private fun findViews() {
        bottomNavigationView = findViewById(R.id.bottom_navigation)
    }

    private fun setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.Home -> replaceFragment(homeFragment)
                R.id.budgets -> replaceFragment(budgetFragment)
                R.id.reports -> replaceFragment(reportsFragment)
                else -> replaceFragment(homeFragment)
            }
            true
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, fragment)
        transaction.commit()
    }
}