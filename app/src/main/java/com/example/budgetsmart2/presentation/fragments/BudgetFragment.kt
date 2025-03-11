package com.example.budgetsmart2.presentation.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.budgetsmart2.databinding.FragmentBudgetBinding
import com.example.budgetsmart2.domain.dataClasses.BudgetStatus
import com.example.budgetsmart2.presentation.adapters.BudgetAdapter
import com.example.budgetsmart2.presentation.dialogs.AddEditBudgetDialog
import com.example.budgetsmart2.presentation.viewModels.BudgetViewModel
import com.example.budgetsmart2.utils.CurrencyFormatter
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.util.*

/**
 * BudgetFragment - Allows users to view and manage their budget categories
 * This fragment enables users to:
 * - View budget progress by category
 * - Navigate through different months
 * - Add/edit/delete budgets
 * - See overall budget status
 */
@AndroidEntryPoint
class BudgetFragment : Fragment() {

    // View binding instance
    private var _binding: FragmentBudgetBinding? = null
    private val binding get() = _binding!!

    // ViewModel instance using Hilt dependency injection
    private val viewModel: BudgetViewModel by viewModels()

    // Budget adapter
    private lateinit var budgetAdapter: BudgetAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout using view binding
        _binding = FragmentBudgetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize UI components
        setupRecyclerView()
        setupMonthSelector()
        setupClickListeners()

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
     * Set up the RecyclerView for budget categories
     */
    private fun setupRecyclerView() {
        budgetAdapter = BudgetAdapter(::onBudgetClicked)
        binding.apply {
            // Using proper IDs from the layout
            budgetCategoriesRecyclerView.adapter = budgetAdapter
            budgetCategoriesRecyclerView.layoutManager = LinearLayoutManager(context)
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

            // Set current month
            monthDisplay.text = viewModel.getCurrentMonthYearString()
        }
    }

    /**
     * Set up click listeners for interactive UI elements
     */
    private fun setupClickListeners() {
        binding.apply {
            // Edit button in toolbar (to add new budgets)
            editBudgets.setOnClickListener {
                showEditBudgetDialog()
            }
        }
    }

    /**
     * Observe all LiveData objects from the ViewModel and update UI accordingly
     */
    private fun observeViewModel() {
        // Observe budget statuses
        viewModel.budgetStatuses.observe(viewLifecycleOwner) { budgetStatuses ->
            updateBudgetsList(budgetStatuses)
        }

        // Observe total budget and spent values
        viewModel.totalBudget.observe(viewLifecycleOwner) { totalBudget ->
            binding.apply {
                // Update remaining value in budget summary card
                val remaining = viewModel.totalRemaining.value ?: 0.0
                remainingValue.text = CurrencyFormatter.format(requireContext(), remaining)
            }
        }

        viewModel.totalSpent.observe(viewLifecycleOwner) { totalSpent ->
            binding.apply {
                // Update spent value in budget summary card
                spentValue.text = CurrencyFormatter.format(requireContext(), totalSpent)
            }
        }

        viewModel.budgetPercentage.observe(viewLifecycleOwner) { percentage ->
            binding.apply {
                // Update progress in budget summary card
                budgetPercentage.text = "${percentage.toInt()}%"
                budgetProgress.progress = percentage.toFloat()
            }
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
    }

    /**
     * Update UI with budget statuses list
     */
    private fun updateBudgetsList(budgetStatuses: List<BudgetStatus>) {
        if (budgetStatuses.isEmpty()) {
            // Show empty state
            binding.apply {
                emptyStateView.visibility = View.VISIBLE
                budgetCategoriesRecyclerView.visibility = View.GONE
            }
            return
        }

        // Show budget list
        binding.apply {
            emptyStateView.visibility = View.GONE
            budgetCategoriesRecyclerView.visibility = View.VISIBLE
        }

        // Update the adapter
        budgetAdapter.submitList(budgetStatuses)
    }

    /**
     * Show dialog to edit a budget
     */
    private fun showEditBudgetDialog() {
        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            if (categories.isNotEmpty()) {
                AddEditBudgetDialog.newInstance(categories) { categoryId, amount ->
                    viewModel.saveBudget(categoryId, amount)
                }.show(childFragmentManager, "AddBudgetDialog")
            } else {
                Toast.makeText(
                    context,
                    "Please create categories first",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * Handle budget item click
     */
    private fun onBudgetClicked(budgetStatus: BudgetStatus) {
        // Show edit budget dialog
        AddEditBudgetDialog.newInstance(
            viewModel.categories.value ?: emptyList(),
            budgetStatus.category.id,
            budgetStatus.budget.amount
        ) { categoryId, amount ->
            viewModel.saveBudget(categoryId, amount)
        }.show(childFragmentManager, "EditBudgetDialog")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clean up binding reference
        _binding = null
    }
}