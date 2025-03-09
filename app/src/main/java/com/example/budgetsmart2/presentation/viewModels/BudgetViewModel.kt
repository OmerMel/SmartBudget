package com.example.budgetsmart2.presentation.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetsmart2.domain.dataClasses.Budget
import com.example.budgetsmart2.domain.dataClasses.BudgetStatus
import com.example.budgetsmart2.domain.dataClasses.Category
import com.example.budgetsmart2.domain.enums.TransactionType
import com.example.budgetsmart2.domain.repositories.BudgetRepository
import com.example.budgetsmart2.domain.repositories.CategoryRepository
import com.example.budgetsmart2.domain.repositories.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    // Budget statuses for all categories
    private val _budgetStatuses = MutableLiveData<List<BudgetStatus>>()
    val budgetStatuses: LiveData<List<BudgetStatus>> = _budgetStatuses

    // Available categories for budget creation
    private val _categories = MutableLiveData<List<Category>>()
    val categories: LiveData<List<Category>> = _categories

    // Total budget and spent amount
    private val _totalBudget = MutableLiveData<Double>()
    val totalBudget: LiveData<Double> = _totalBudget

    private val _totalSpent = MutableLiveData<Double>()
    val totalSpent: LiveData<Double> = _totalSpent

    private val _totalRemaining = MutableLiveData<Double>()
    val totalRemaining: LiveData<Double> = _totalRemaining

    private val _budgetPercentage = MutableLiveData<Double>()
    val budgetPercentage: LiveData<Double> = _budgetPercentage

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
        loadCategories()
        refreshBudgetData()
    }

    /**
     * Refresh budget data
     */
    fun refreshBudgetData() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                loadBudgetStatuses()
            } catch (e: Exception) {
                _error.value = "Failed to load budget data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Load all categories for the current user
     */
    private fun loadCategories() {
        viewModelScope.launch {
            try {
                val userCategories = categoryRepository.getCategories(userId)
                _categories.value = userCategories
            } catch (e: Exception) {
                _error.value = "Failed to load categories: ${e.message}"
            }
        }
    }

    /**
     * Load budget statuses for all categories
     */
    private suspend fun loadBudgetStatuses() {
        val budgetsList = budgetRepository.getBudgets(userId).filter {
            it.month == currentMonth && it.year == currentYear
        }

        // Get all categories
        val categoriesMap = categoryRepository.getCategories(userId).associateBy { it.id }

        // Get all transactions for the current month
        val transactions = transactionRepository.getTransactionsByPeriod(userId, currentMonth, currentYear)
            .filter { it.type == TransactionType.EXPENSE }

        // Calculate spent amount for each budget
        val budgetStatuses = mutableListOf<BudgetStatus>()
        var totalBudgetAmount = 0.0
        var totalSpentAmount = 0.0

        for (budget in budgetsList) {
            val category = categoriesMap[budget.categoryId] ?: continue

            // Calculate spent amount for this category
            val spentAmount = transactions
                .filter { it.categoryId == budget.categoryId }
                .sumOf { it.amount }

            val remainingAmount = budget.amount - spentAmount
            val percentage = if (budget.amount > 0) (spentAmount / budget.amount) * 100 else 0.0

            budgetStatuses.add(
                BudgetStatus(
                    budget = budget,
                    category = category,
                    spent = spentAmount,
                    remaining = remainingAmount,
                    percentage = percentage
                )
            )

            totalBudgetAmount += budget.amount
            totalSpentAmount += spentAmount
        }

        // Sort by percentage (highest first)
        budgetStatuses.sortByDescending { it.percentage }

        _budgetStatuses.postValue(budgetStatuses)
        _totalBudget.postValue(totalBudgetAmount)
        _totalSpent.postValue(totalSpentAmount)
        _totalRemaining.postValue(totalBudgetAmount - totalSpentAmount)

        val overallPercentage = if (totalBudgetAmount > 0) {
            (totalSpentAmount / totalBudgetAmount) * 100
        } else {
            0.0
        }
        _budgetPercentage.postValue(overallPercentage)
    }

    /**
     * Create or update a budget
     * @param categoryId The category ID
     * @param amount The budget amount
     */
    fun saveBudget(categoryId: String, amount: Double) {
        if (userId.isEmpty()) {
            _error.value = "User not authenticated"
            return
        }

        viewModelScope.launch {
            try {
                // Check if a budget already exists for this category and month
                val existingBudget = budgetRepository.getBudgetByCategory(userId, categoryId)?.let {
                    if (it.month == currentMonth && it.year == currentYear) it else null
                }

                if (existingBudget != null) {
                    // Update existing budget
                    val updatedBudget = existingBudget.copy(amount = amount)
                    budgetRepository.updateBudget(updatedBudget)
                } else {
                    // Create new budget
                    val newBudget = Budget(
                        categoryId = categoryId,
                        amount = amount,
                        month = currentMonth,
                        year = currentYear,
                        userId = userId
                    )
                    budgetRepository.addBudget(newBudget)
                }

                refreshBudgetData()
            } catch (e: Exception) {
                _error.value = "Failed to save budget: ${e.message}"
            }
        }
    }

    /**
     * Delete a budget
     * @param budgetId The budget ID to delete
     */
    fun deleteBudget(budgetId: String) {
        viewModelScope.launch {
            try {
                budgetRepository.deleteBudget(budgetId)
                refreshBudgetData()
            } catch (e: Exception) {
                _error.value = "Failed to delete budget: ${e.message}"
            }
        }
    }

    /**
     * Change the current month for filtering
     * @param monthOffset The number of months to add/subtract
     */
    fun changeMonth(monthOffset: Int) {
        calendar.set(currentYear, currentMonth, 1)
        calendar.add(Calendar.MONTH, monthOffset)

        currentMonth = calendar.get(Calendar.MONTH)
        currentYear = calendar.get(Calendar.YEAR)

        refreshBudgetData()
    }

    /**
     * Get month year string for display
     */
    fun getCurrentMonthYearString(): String {
        val monthNames = arrayOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        return "${monthNames[currentMonth]} $currentYear"
    }

    /**
     * Clear any error message
     */
    fun clearError() {
        _error.value = null
    }
}