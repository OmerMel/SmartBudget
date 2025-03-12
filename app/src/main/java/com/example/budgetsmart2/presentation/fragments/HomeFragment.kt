package com.example.budgetsmart2.presentation.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.budgetsmart2.R
import com.example.budgetsmart2.databinding.FragmentHomeBinding
import com.example.budgetsmart2.domain.dataClasses.TransactionWithCategory
import com.example.budgetsmart2.domain.enums.TransactionType
import com.example.budgetsmart2.presentation.adapters.TransactionAdapter
import com.example.budgetsmart2.presentation.dialogs.AddTransactionDialog
import com.example.budgetsmart2.presentation.viewModels.HomeViewModel
import com.example.budgetsmart2.utils.CurrencyFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.AndroidEntryPoint

/**
 * HomeFragment - Main dashboard showing financial summary and recent transactions
 * This fragment serves as the main screen of the app, displaying:
 * - Current balance information (income and expenses)
 * - Budget progress overview
 * - Recent transactions list
 * - Quick actions to add income or expenses
 */
@AndroidEntryPoint
class HomeFragment : Fragment() {

    // View binding instance
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // ViewModel instance using Hilt dependency injection
    private val viewModel: HomeViewModel by viewModels()

    // Recent transactions adapter
    private lateinit var transactionAdapter: TransactionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout using view binding
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize UI components
        setupRecyclerView()
        setupClickListeners()
        setupProfileImage()

        // Initialize ViewModel with current user ID
        FirebaseAuth.getInstance().currentUser?.let { user ->
            viewModel.initialize(user.uid)
        } ?: run {
            // Handle not logged in state
            // You might want to navigate to login screen here
            Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show()
        }

        // Observe LiveData from ViewModel
        observeViewModel()
    }

    /**
     * Set up the RecyclerView for recent transactions
     */
    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter(::onTransactionClicked)
        binding.apply {
            // Set up RecyclerView with the adapter
            val recyclerView = recentTransactionsRecyclerView
            recyclerView.adapter = transactionAdapter
            recyclerView.layoutManager = LinearLayoutManager(context)
        }
    }

    /**
     * Set up click listeners for interactive UI elements
     */
    private fun setupClickListeners() {
        binding.apply {
            // Add expense button
            addExpenseButton.setOnClickListener {
                showAddTransactionDialog(TransactionType.EXPENSE)
            }

            // Add income button
            addIncomeButton.setOnClickListener {
                showAddTransactionDialog(TransactionType.INCOME)
            }

            // See all transactions button
            seeAll.setOnClickListener {
                navigateToTransactions()
            }

            // Profile image click (if implementing user profile)
            profileImage.setOnClickListener {
                navigateToSettings()
            }
        }
    }

    /**
     * Set up profile picture for top left user icon
     */
    private fun setupProfileImage() {
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            // Check if user is signed in with Google
            val isGoogleSignIn = currentUser.providerData.any {
                it.providerId == GoogleAuthProvider.PROVIDER_ID
            }

            if (isGoogleSignIn && currentUser.photoUrl != null) {
                // Load Google profile image
                Glide.with(requireContext())
                    .load(currentUser.photoUrl)
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .into(binding.profileImage)
            } else {
                // Load default icon for email/password users
                binding.profileImage.setImageResource(R.drawable.ic_profile)
            }
        } else {
            // Fallback in case user is not logged in (shouldn't happen)
            binding.profileImage.setImageResource(R.drawable.ic_profile)
        }
    }

    /**
     * Observe all LiveData objects from the ViewModel and update UI accordingly
     */
    private fun observeViewModel() {
        // Observe financial summary
        viewModel.financialSummary.observe(viewLifecycleOwner) { summary ->
            updateFinancialSummary(summary)
        }

        // Observe recent transactions
        viewModel.recentTransactions.observe(viewLifecycleOwner) { transactions ->
            updateRecentTransactions(transactions)
        }

        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // Show/hide loading indicator
            // Note: Add a ProgressBar to your layout and control visibility here
        }

        // Observe error state
        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    /**
     * Update UI with financial summary data
     */
    private fun updateFinancialSummary(summary: com.example.budgetsmart2.domain.dataClasses.FinancialSummary) {
        binding.apply {
            // Update expense amount
            expensesBalanceAmount.text = CurrencyFormatter.format(requireContext(), summary.totalExpenses)

            // Update income amount
            incomesBalanceAmount.text = CurrencyFormatter.format(requireContext(), summary.totalIncome)

            // Update budget progress - use budgetedExpenses instead of totalExpenses
            budgetPercentage.text = "${summary.budgetUsedPercentage.toInt()}%"
            budgetProgress.text = "${CurrencyFormatter.format(requireContext(), summary.monthlyBudget)} of ${CurrencyFormatter.format(requireContext(), summary.budgetedExpenses)}"
            budgetProgressBar.progress = summary.budgetUsedPercentage.toInt()
        }
    }

    /**
     * Update UI with recent transactions
     */
    private fun updateRecentTransactions(transactions: List<TransactionWithCategory>) {
        if (transactions.isEmpty()) {
            // Handle empty state
            binding.emptyTransactionsView?.visibility = View.VISIBLE
            binding.recentTransactionsRecyclerView.visibility = View.GONE
            return
        }

        // Show RecyclerView and hide empty state
        binding.emptyTransactionsView?.visibility = View.GONE
        binding.recentTransactionsRecyclerView.visibility = View.VISIBLE

        // Submit the list to adapter - DiffUtil will handle the updates efficiently
        transactionAdapter.submitList(transactions)
    }

    /**
     * Show dialog to add a new transaction
     */
    private fun showAddTransactionDialog(type: TransactionType) {
        // Create and show the dialog to add a transaction
        // This would be implemented in a separate dialog class
        AddTransactionDialog.newInstance(type) { amount, description, categoryId, date ->
            viewModel.addTransaction(amount, description, categoryId, type, date)
        }.show(childFragmentManager, "AddTransactionDialog")
    }

    /**
     * Handle transaction item click
     */
    private fun onTransactionClicked(transaction: TransactionWithCategory) {
        // Navigate to transaction details or edit screen
    }

    /**
     * Navigate to Transactions fragment
     */
    private fun navigateToTransactions() {
        findNavController().navigate(R.id.action_homeFragment_to_transactionsFragment)
    }

    /**
     * Navigate to Settings fragment
     */
    private fun navigateToSettings() {
        findNavController().navigate(R.id.action_homeFragment_to_settingsFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clean up binding reference
        _binding = null
    }
}