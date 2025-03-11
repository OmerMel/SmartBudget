package com.example.budgetsmart2.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.budgetsmart2.R
import com.example.budgetsmart2.databinding.FragmentTransactionsBinding
import com.example.budgetsmart2.domain.dataClasses.TransactionWithCategory
import com.example.budgetsmart2.domain.enums.TransactionType
import com.example.budgetsmart2.presentation.adapters.TransactionAdapter
import com.example.budgetsmart2.presentation.dialogs.AddTransactionDialog
import com.example.budgetsmart2.presentation.dialogs.TransactionDetailsDialog
import com.example.budgetsmart2.presentation.viewModels.TransactionsViewModel
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

/**
 * TransactionsFragment - Displays the full list of transactions with filtering options
 * This fragment allows users to:
 * - View all transactions for a selected month
 * - Filter transactions by type (Income, Expense, All)
 * - Add new transactions via FAB
 * - View transaction details by clicking on an item
 */
@AndroidEntryPoint
class TransactionsFragment : Fragment() {

    // View binding instance
    private var _binding: FragmentTransactionsBinding? = null
    private val binding get() = _binding!!

    // ViewModel instance using Hilt dependency injection
    private val viewModel: TransactionsViewModel by viewModels()

    // Transactions adapter
    private lateinit var transactionAdapter: TransactionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout using view binding
        _binding = FragmentTransactionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize UI components
        setupRecyclerView()
        setupMonthSelector()
        setupFilterChips()

        // Initialize ViewModel with current user ID
        FirebaseAuth.getInstance().currentUser?.let { user ->
            viewModel.initialize(user.uid)
        } ?: run {
            // Handle not logged in state
            Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show()
        }

        // Observe LiveData from ViewModel
        observeViewModel()
    }

    /**
     * Set up the RecyclerView for transactions
     */
    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter(::onTransactionClicked)
        binding.apply {
            transactionsRecyclerView.apply {
                adapter = transactionAdapter
                layoutManager = LinearLayoutManager(context)
                // Optional: Add item decoration for spacing/dividers
                // addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            }
        }
    }

    /**
     * Set up the month selector navigation
     */
    private fun setupMonthSelector() {
        binding.apply {
            // Previous month button
            monthPrevious.setOnClickListener {
                viewModel.changeMonth(-1)
            }

            // Next month button
            monthNext.setOnClickListener {
                viewModel.changeMonth(1)
            }
        }
    }

    /**
     * Set up the filter chips for transaction types
     */
    private fun setupFilterChips() {
        binding.apply {
            // Set up click listeners for filter chips
            chipAll.setOnClickListener {
                updateChipSelection(it as Chip)
                viewModel.filterTransactions(TransactionsViewModel.TransactionFilterType.ALL)
            }

            chipIncome.setOnClickListener {
                updateChipSelection(it as Chip)
                viewModel.filterTransactions(TransactionsViewModel.TransactionFilterType.INCOME)
            }

            chipExpenses.setOnClickListener {
                updateChipSelection(it as Chip)
                viewModel.filterTransactions(TransactionsViewModel.TransactionFilterType.EXPENSE)
            }
        }
    }

    /**
     * Update the UI for selected chip
     */
    private fun updateChipSelection(selectedChip: Chip) {
        binding.apply {
            // Reset all chips
            chipAll.isChecked = false
            chipIncome.isChecked = false
            chipExpenses.isChecked = false

            // Set chip colors to default
            chipAll.setChipBackgroundColorResource(R.color.white)
            chipIncome.setChipBackgroundColorResource(R.color.white)
            chipExpenses.setChipBackgroundColorResource(R.color.white)

            chipAll.setTextColor(resources.getColor(R.color.text_secondary, null))
            chipIncome.setTextColor(resources.getColor(R.color.text_secondary, null))
            chipExpenses.setTextColor(resources.getColor(R.color.text_secondary, null))

            // Update selected chip
            selectedChip.isChecked = true
            selectedChip.setChipBackgroundColorResource(R.color.primary)
            selectedChip.setTextColor(resources.getColor(R.color.white, null))
        }
    }

    /**
     * Observe all LiveData objects from the ViewModel and update UI accordingly
     */
    private fun observeViewModel() {
        // Observe transactions
        viewModel.transactions.observe(viewLifecycleOwner) { transactions ->
            updateTransactionsList(transactions)
        }

        // Observe month display
        viewModel.getCurrentMonthYearString().let { monthYear ->
            binding.monthDisplay.text = monthYear
        }

        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Observe error state
        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        // Observe transaction saved event
        viewModel.transactionSaved.observe(viewLifecycleOwner) { saved ->
            if (saved) {
                Toast.makeText(context, "Transaction saved successfully", Toast.LENGTH_SHORT).show()
                viewModel.resetTransactionSaved()
            }
        }
    }

    /**
     * Update UI with transactions list
     */
    private fun updateTransactionsList(transactions: List<TransactionWithCategory>) {
        if (transactions.isEmpty()) {
            // Show empty state
            binding.apply {
                emptyStateView.visibility = View.VISIBLE
                transactionsRecyclerView.visibility = View.GONE
            }
            return
        }

        // Show transactions list
        binding.apply {
            emptyStateView.visibility = View.GONE
            transactionsRecyclerView.visibility = View.VISIBLE
        }

        // Update the adapter
        transactionAdapter.submitList(transactions)
    }

    /**
     * Handle transaction item click
     */
    private fun onTransactionClicked(transaction: TransactionWithCategory) {
        // Show transaction details dialog
        TransactionDetailsDialog.newInstance(transaction) { action, updatedTransaction ->
            when (action) {
                TransactionDetailsDialog.Action.EDIT -> {
                    // Handle edit action
                    viewModel.updateTransaction(updatedTransaction)
                }
                TransactionDetailsDialog.Action.DELETE -> {
                    // Handle delete action
                    viewModel.deleteTransaction(transaction.transaction.id)
                }
                else -> { /* No action needed */ }
            }
        }.show(childFragmentManager, "TransactionDetailsDialog")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clean up binding reference
        _binding = null
    }
}