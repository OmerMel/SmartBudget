package com.example.budgetsmart2.presentation.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetsmart2.domain.dataClasses.Category
import com.example.budgetsmart2.domain.dataClasses.Transaction
import com.example.budgetsmart2.domain.dataClasses.TransactionWithCategory
import com.example.budgetsmart2.domain.enums.TransactionType
import com.example.budgetsmart2.domain.repositories.CategoryRepository
import com.example.budgetsmart2.domain.repositories.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    // Transactions with category information
    private val _transactions = MutableLiveData<List<TransactionWithCategory>>()
    val transactions: LiveData<List<TransactionWithCategory>> = _transactions

    // Filter type for transactions (ALL, INCOME, EXPENSE)
    private var currentFilterType: TransactionFilterType = TransactionFilterType.ALL

    // Loading state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Error state
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // Available categories for transaction creation
    private val _categories = MutableLiveData<List<Category>>()
    val categories: LiveData<List<Category>> = _categories

    // Current month and year for filtering
    private val calendar = Calendar.getInstance()
    private var currentMonth = calendar.get(Calendar.MONTH)
    private var currentYear = calendar.get(Calendar.YEAR)

    // User ID
    private var userId: String = ""

    // Current transaction being edited
    private val _currentTransaction = MutableLiveData<Transaction?>(null)
    val currentTransaction: LiveData<Transaction?> = _currentTransaction

    // Event for transaction added/updated successfully
    private val _transactionSaved = MutableLiveData<Boolean>()
    val transactionSaved: LiveData<Boolean> = _transactionSaved

    /**
     * Initialize the ViewModel with the current user ID
     * @param userId The ID of the current user
     */
    fun initialize(userId: String) {
        this.userId = userId
        loadCategories()
        refreshTransactions()
    }

    /**
     * Refresh transaction data
     */
    fun refreshTransactions() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                loadTransactions()
            } catch (e: Exception) {
                _error.value = "Failed to load transactions: ${e.message}"
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
     * Load transactions with category information
     */
    private suspend fun loadTransactions() {
        // Get transactions for the current month
        val monthlyTransactions = transactionRepository.getTransactionsByPeriod(
            userId,
            currentMonth,
            currentYear
        )

        // Apply filter if needed
        val filteredTransactions = when (currentFilterType) {
            TransactionFilterType.ALL -> monthlyTransactions
            TransactionFilterType.INCOME -> monthlyTransactions.filter { it.type == TransactionType.INCOME }
            TransactionFilterType.EXPENSE -> monthlyTransactions.filter { it.type == TransactionType.EXPENSE }
        }

        // Get all categories
        val categories = categoryRepository.getCategories(userId).associateBy { it.id }

        // Combine transactions with category information
        val transactionsWithCategory = filteredTransactions.mapNotNull { transaction ->
            val category = categories[transaction.categoryId] ?: return@mapNotNull null
            TransactionWithCategory(transaction, category)
        }

        _transactions.postValue(transactionsWithCategory)
    }

    /**
     * Filter transactions by type
     * @param filterType The type to filter by
     */
    fun filterTransactions(filterType: TransactionFilterType) {
        currentFilterType = filterType
        refreshTransactions()
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
                _transactionSaved.value = true
                refreshTransactions()
            } catch (e: Exception) {
                _error.value = "Failed to add transaction: ${e.message}"
                _transactionSaved.value = false
            }
        }
    }

    /**
     * Update an existing transaction
     * @param transaction The transaction to update
     */
    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            try {
                transactionRepository.updateTransaction(transaction)
                _currentTransaction.value = null
                _transactionSaved.value = true
                refreshTransactions()
            } catch (e: Exception) {
                _error.value = "Failed to update transaction: ${e.message}"
                _transactionSaved.value = false
            }
        }
    }

    /**
     * Delete a transaction
     * @param transactionId The ID of the transaction to delete
     */
    fun deleteTransaction(transactionId: String) {
        viewModelScope.launch {
            try {
                transactionRepository.deleteTransaction(transactionId)
                refreshTransactions()
            } catch (e: Exception) {
                _error.value = "Failed to delete transaction: ${e.message}"
            }
        }
    }

    /**
     * Set the current transaction for editing
     * @param transaction The transaction to edit, or null to create a new one
     */
    fun setCurrentTransaction(transaction: Transaction?) {
        _currentTransaction.value = transaction
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

        refreshTransactions()
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

    /**
     * Reset transaction saved flag
     */
    fun resetTransactionSaved() {
        _transactionSaved.value = false
    }

    /**
     * Filter types for transactions
     */
    enum class TransactionFilterType {
        ALL,
        INCOME,
        EXPENSE
    }
}