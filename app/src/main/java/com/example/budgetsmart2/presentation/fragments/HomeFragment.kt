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
import com.example.budgetsmart2.R
import com.example.budgetsmart2.databinding.FragmentHomeBinding
import com.example.budgetsmart2.domain.dataClasses.TransactionWithCategory
import com.example.budgetsmart2.domain.enums.TransactionType
import com.example.budgetsmart2.presentation.adapters.RecentTransactionAdapter
import com.example.budgetsmart2.presentation.dialogs.AddTransactionDialog
import com.example.budgetsmart2.presentation.viewModels.HomeViewModel
import com.example.budgetsmart2.utils.CurrencyFormatter
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.util.*

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
    private lateinit var transactionAdapter: RecentTransactionAdapter

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
        transactionAdapter = RecentTransactionAdapter(::onTransactionClicked)
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

            // Update budget progress
            budgetPercentage.text = "${summary.budgetUsedPercentage.toInt()}%"
            budgetProgress.text = "${CurrencyFormatter.format(requireContext(), summary.totalExpenses)} of ${CurrencyFormatter.format(requireContext(), summary.monthlyBudget)}"
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