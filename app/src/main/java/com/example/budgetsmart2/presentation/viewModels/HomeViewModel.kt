package com.example.budgetsmart2.presentation.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetsmart2.domain.dataClasses.FinancialSummary
import com.example.budgetsmart2.domain.dataClasses.Transaction
import com.example.budgetsmart2.domain.dataClasses.TransactionWithCategory
import com.example.budgetsmart2.domain.enums.TransactionType
import com.example.budgetsmart2.domain.repositories.BudgetRepository
import com.example.budgetsmart2.domain.repositories.CategoryRepository
import com.example.budgetsmart2.domain.repositories.TransactionRepository
import com.example.budgetsmart2.domain.repositories.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    // Financial summary for the current month
    private val _financialSummary = MutableLiveData<FinancialSummary>()
    val financialSummary: LiveData<FinancialSummary> = _financialSummary

    // Recent transactions with category information
    private val _recentTransactions = MutableLiveData<List<TransactionWithCategory>>()
    val recentTransactions: LiveData<List<TransactionWithCategory>> = _recentTransactions

    // Loading state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Error state
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // Current month and year for filtering
    private val calendar = Calendar.getInstance()
    private var currentMonth = calendar.get(Calendar.MONTH)
    private var currentYear = calendar.get(Calendar.YEAR)

    // User ID
    private var userId: String = ""

    /**
     * Initialize the ViewModel with the current user ID
     * @param userId The ID of the current user
     */
    fun initialize(userId: String) {
        this.userId = userId
        refreshData()
    }

    /**
     * Refresh all data sources
     */
    fun refreshData() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                // Load data in parallel
                loadFinancialSummary()
                loadRecentTransactions()
            } catch (e: Exception) {
                _error.value = "Failed to load data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Add a new transaction
     * @param amount The transaction amount
     * @param description The transaction description
     * @param categoryId The category ID
     * @param type The transaction type (EXPENSE or INCOME)
     * @param date The transaction date
     */
    fun addTransaction(
        amount: Double,
        description: String,
        categoryId: String,
        type: TransactionType,
        date: Date = Date()
    ) {
        if (userId.isEmpty()) {
            _error.value = "User not authenticated"
            return
        }

        viewModelScope.launch {
            try {
                val transaction = Transaction(
                    amount = amount,
                    description = description,
                    categoryId = categoryId,
                    type = type,
                    date = date,
                    userId = userId
                )

                transactionRepository.addTransaction(transaction)
                refreshData()
            } catch (e: Exception) {
                _error.value = "Failed to add transaction: ${e.message}"
            }
        }
    }

    /**
     * Load the financial summary for the current month
     */
    private suspend fun loadFinancialSummary() {
        // Get transactions for current month/year
        val transactions = transactionRepository.getTransactionsByPeriod(
            userId,
            currentMonth,
            currentYear
        )

        // Get budgets for current month/year
        val budgets = budgetRepository.getBudgets(userId).filter {
            it.month == currentMonth && it.year == currentYear
        }

        // Create a set of categories that have budgets
        val categoryBudgetMap = budgets.associate { it.categoryId to it.amount }

        // Calculate total income, expenses, and balance
        var totalIncome = 0.0
        var totalExpenses = 0.0
        var trackedExpenses = 0.0

        transactions.forEach { transaction ->
            when (transaction.type) {
                TransactionType.INCOME -> totalIncome += transaction.amount
                TransactionType.EXPENSE -> {
                    totalExpenses += transaction.amount

                    // Only count expenses for categories that have a budget
                    if (categoryBudgetMap.containsKey(transaction.categoryId)) {
                        trackedExpenses += transaction.amount
                    }
                }
            }
        }

        // Calculate total budget for the month
        val monthlyBudget = budgets.sumOf { it.amount }

        // Calculate budget used percentage
        val budgetUsedPercentage = if (monthlyBudget > 0) {
            (trackedExpenses  / monthlyBudget) * 100
        } else {
            0.0
        }

        _financialSummary.postValue(
            FinancialSummary(
                totalIncome = totalIncome,
                totalExpenses = totalExpenses,
                balance = totalIncome - totalExpenses,
                monthlyBudget = monthlyBudget,
                budgetUsedPercentage = budgetUsedPercentage,
                budgetedExpenses = trackedExpenses
            )
        )
    }

    /**
     * Load recent transactions with category information
     */
    private suspend fun loadRecentTransactions() {
        val transactions = transactionRepository.getTransactions(userId)
            .take(4) // Only get the 4 most recent transactions

        val transactionsWithCategory = mutableListOf<TransactionWithCategory>()

        transactions.forEach { transaction ->
            val category = categoryRepository.getCategoryById(transaction.categoryId)
            if (category != null) {
                transactionsWithCategory.add(
                    TransactionWithCategory(
                        transaction = transaction,
                        category = category
                    )
                )
            }
        }

        _recentTransactions.postValue(transactionsWithCategory)
    }

    /**
     * Clear any error message
     */
    fun clearError() {
        _error.value = null
    }
}