package com.example.budgetsmart2.presentation.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetsmart2.domain.dataClasses.Category
import com.example.budgetsmart2.domain.dataClasses.Transaction
import com.example.budgetsmart2.domain.enums.TransactionType
import com.example.budgetsmart2.domain.repositories.BudgetRepository
import com.example.budgetsmart2.domain.repositories.CategoryRepository
import com.example.budgetsmart2.domain.repositories.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    // Monthly expense data for bar chart
    private val _monthlyExpenseData = MutableLiveData<List<MonthlyExpenseData>>()
    val monthlyExpenseData: LiveData<List<MonthlyExpenseData>> = _monthlyExpenseData

    // Category expense data for pie chart
    private val _categoryExpenseData = MutableLiveData<List<CategoryExpenseData>>()
    val categoryExpenseData: LiveData<List<CategoryExpenseData>> = _categoryExpenseData

    // Top expense categories
    private val _topExpenseCategories = MutableLiveData<List<CategoryExpenseData>>()
    val topExpenseCategories: LiveData<List<CategoryExpenseData>> = _topExpenseCategories

    // Current report type
    private var currentReportType: ReportType = ReportType.EXPENSES

    // Current time period
    private var currentTimePeriod: TimePeriod = TimePeriod.LAST_3_MONTHS

    // Loading state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Error state
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // User ID
    private var userId: String = ""

    /**
     * Initialize the ViewModel with the current user ID
     * @param userId The ID of the current user
     */
    fun initialize(userId: String) {
        this.userId = userId
        refreshReportData()
    }

    /**
     * Refresh report data
     */
    fun refreshReportData() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                when (currentReportType) {
                    ReportType.EXPENSES -> loadExpenseReports()
                    ReportType.INCOME -> loadIncomeReports()
                    ReportType.ALL -> loadAllTransactionReports()
                }
            } catch (e: Exception) {
                _error.value = "Failed to load report data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Load expense reports
     */
    private suspend fun loadExpenseReports() {
        val (startDate, endDate) = getDateRangeForPeriod(currentTimePeriod)

        // Get all transactions within the date range
        val transactions = getAllTransactionsInDateRange(startDate, endDate)
            .filter { it.type == TransactionType.EXPENSE }

        // Get all categories
        val categories = categoryRepository.getCategories(userId).associateBy { it.id }

        loadMonthlyData(transactions)
        loadCategoryData(transactions, categories)
    }

    /**
     * Load income reports
     */
    private suspend fun loadIncomeReports() {
        val (startDate, endDate) = getDateRangeForPeriod(currentTimePeriod)

        // Get all transactions within the date range
        val transactions = getAllTransactionsInDateRange(startDate, endDate)
            .filter { it.type == TransactionType.INCOME }

        // Get all categories
        val categories = categoryRepository.getCategories(userId).associateBy { it.id }

        loadMonthlyData(transactions)
        loadCategoryData(transactions, categories)
    }

    /**
     * Load all transaction reports
     */
    private suspend fun loadAllTransactionReports() {
        val (startDate, endDate) = getDateRangeForPeriod(currentTimePeriod)

        // Get all transactions within the date range
        val transactions = getAllTransactionsInDateRange(startDate, endDate)

        // Get all categories
        val categories = categoryRepository.getCategories(userId).associateBy { it.id }

        loadMonthlyData(transactions)
        loadCategoryData(transactions, categories)
    }

    /**
     * Load monthly data for bar chart
     */
    private fun loadMonthlyData(transactions: List<Transaction>) {
        // Group transactions by month and year
        val monthlyData = mutableMapOf<String, Double>()

        transactions.forEach { transaction ->
            val calendar = Calendar.getInstance()
            calendar.time = transaction.date

            val month = calendar.get(Calendar.MONTH)
            val year = calendar.get(Calendar.YEAR)
            val key = "$year-${month+1}" // +1 because Calendar.MONTH is 0-based

            monthlyData[key] = (monthlyData[key] ?: 0.0) + transaction.amount
        }

        // Convert to list of MonthlyExpenseData objects
        val monthlyExpenseDataList = monthlyData.map { (key, amount) ->
            val parts = key.split("-")
            val year = parts[0].toInt()
            val month = parts[1].toInt() - 1 // -1 to convert back to 0-based month

            MonthlyExpenseData(
                year = year,
                month = month,
                amount = amount
            )
        }.sortedBy { it.year * 100 + it.month } // Sort by year and month

        _monthlyExpenseData.postValue(monthlyExpenseDataList)
    }

    /**
     * Load category data for pie chart
     */
    private fun loadCategoryData(transactions: List<Transaction>, categories: Map<String, Category>) {
        // Group transactions by category
        val categoryData = mutableMapOf<String, Double>()

        transactions.forEach { transaction ->
            categoryData[transaction.categoryId] = (categoryData[transaction.categoryId] ?: 0.0) + transaction.amount
        }

        // Convert to list of CategoryExpenseData objects
        val categoryExpenseDataList = categoryData.mapNotNull { (categoryId, amount) ->
            val category = categories[categoryId] ?: return@mapNotNull null

            CategoryExpenseData(
                categoryId = categoryId,
                categoryName = category.name,
                categoryColor = category.color,
                amount = amount
            )
        }.sortedByDescending { it.amount } // Sort by amount (highest first)

        _categoryExpenseData.postValue(categoryExpenseDataList)

        // Top 4 categories for the breakdown section
        _topExpenseCategories.postValue(categoryExpenseDataList.take(4))
    }

    /**
     * Get all transactions in a date range
     */
    private suspend fun getAllTransactionsInDateRange(startDate: Date, endDate: Date): List<Transaction> {
        // Get all transactions
        val allTransactions = transactionRepository.getTransactions(userId)

        // Filter by date range
        return allTransactions.filter { transaction ->
            transaction.date in startDate..endDate
        }
    }

    /**
     * Get start and end dates for a time period
     */
    private fun getDateRangeForPeriod(period: TimePeriod): Pair<Date, Date> {
        val calendar = Calendar.getInstance()
        val endDate = calendar.time

        // Set the start date based on the period
        calendar.time = endDate
        when (period) {
            TimePeriod.LAST_MONTH -> calendar.add(Calendar.MONTH, -1)
            TimePeriod.LAST_3_MONTHS -> calendar.add(Calendar.MONTH, -3)
            TimePeriod.LAST_6_MONTHS -> calendar.add(Calendar.MONTH, -6)
            TimePeriod.LAST_YEAR -> calendar.add(Calendar.YEAR, -1)
            TimePeriod.CUSTOM -> {} // Custom period handled separately
        }

        val startDate = calendar.time
        return Pair(startDate, endDate)
    }

    /**
     * Change the report type
     * @param reportType The new report type
     */
    fun setReportType(reportType: ReportType) {
        if (currentReportType != reportType) {
            currentReportType = reportType
            refreshReportData()
        }
    }

    /**
     * Change the time period
     * @param timePeriod The new time period
     */
    fun setTimePeriod(timePeriod: TimePeriod) {
        if (currentTimePeriod != timePeriod) {
            currentTimePeriod = timePeriod
            refreshReportData()
        }
    }

    /**
     * Clear any error message
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Report types
     */
    enum class ReportType {
        EXPENSES,
        INCOME,
        ALL
    }

    /**
     * Time periods for reports
     */
    enum class TimePeriod {
        LAST_MONTH,
        LAST_3_MONTHS,
        LAST_6_MONTHS,
        LAST_YEAR,
        CUSTOM
    }

    /**
     * Data class for monthly expense data
     */
    data class MonthlyExpenseData(
        val year: Int,
        val month: Int,
        val amount: Double
    ) {
        fun getMonthName(): String {
            val monthNames = arrayOf(
                "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
            )
            return monthNames[month]
        }
    }

    /**
     * Data class for category expense data
     */
    data class CategoryExpenseData(
        val categoryId: String,
        val categoryName: String,
        val categoryColor: Int,
        val amount: Double,
        val percentage: Double = 0.0 // Will be calculated later
    )
}