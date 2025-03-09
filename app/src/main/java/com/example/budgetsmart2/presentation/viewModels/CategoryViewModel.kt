package com.example.budgetsmart2.presentation.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetsmart2.domain.dataClasses.Category
import com.example.budgetsmart2.domain.repositories.CategoryRepository
import com.example.budgetsmart2.domain.repositories.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    // Categories list
    private val _categories = MutableLiveData<List<Category>>()
    val categories: LiveData<List<Category>> = _categories

    // Current category being edited
    private val _currentCategory = MutableLiveData<Category?>(null)
    val currentCategory: LiveData<Category?> = _currentCategory

    // Loading state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Error state
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // Event for category saved successfully
    private val _categorySaved = MutableLiveData<Boolean>()
    val categorySaved: LiveData<Boolean> = _categorySaved

    // User ID
    private var userId: String = ""

    /**
     * Initialize the ViewModel with the current user ID
     * @param userId The ID of the current user
     */
    fun initialize(userId: String) {
        this.userId = userId
        refreshCategories()
    }

    /**
     * Refresh categories
     */
    fun refreshCategories() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val userCategories = categoryRepository.getCategories(userId)
                _categories.value = userCategories
            } catch (e: Exception) {
                _error.value = "Failed to load categories: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Create a new category
     * @param name The category name
     * @param icon The category icon (emoji or resource ID)
     * @param color The category color resource
     */
    fun createCategory(name: String, icon: String, color: Int) {
        if (userId.isEmpty()) {
            _error.value = "User not authenticated"
            return
        }

        if (name.isBlank()) {
            _error.value = "Category name cannot be empty"
            return
        }

        viewModelScope.launch {
            try {
                val category = Category(
                    name = name,
                    icon = icon,
                    color = color,
                    userId = userId
                )

                categoryRepository.addCategory(category)
                _categorySaved.value = true
                refreshCategories()
            } catch (e: Exception) {
                _error.value = "Failed to create category: ${e.message}"
                _categorySaved.value = false
            }
        }
    }

    /**
     * Update an existing category
     * @param categoryId The ID of the category to update
     * @param name The new category name
     * @param icon The new category icon
     * @param color The new category color
     */
    fun updateCategory(categoryId: String, name: String, icon: String, color: Int) {
        if (name.isBlank()) {
            _error.value = "Category name cannot be empty"
            return
        }

        viewModelScope.launch {
            try {
                val category = Category(
                    id = categoryId,
                    name = name,
                    icon = icon,
                    color = color,
                    userId = userId
                )

                categoryRepository.updateCategory(category)
                _currentCategory.value = null
                _categorySaved.value = true
                refreshCategories()
            } catch (e: Exception) {
                _error.value = "Failed to update category: ${e.message}"
                _categorySaved.value = false
            }
        }
    }

    /**
     * Delete a category
     * @param categoryId The ID of the category to delete
     */
    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            try {
                // Check if category is used in any transactions
                val transactions = transactionRepository.getTransactionsByCategory(userId, categoryId)

                if (transactions.isNotEmpty()) {
                    _error.value = "Cannot delete category as it is used in transactions"
                    return@launch
                }

                categoryRepository.deleteCategory(categoryId)
                refreshCategories()
            } catch (e: Exception) {
                _error.value = "Failed to delete category: ${e.message}"
            }
        }
    }

    /**
     * Get a category by ID
     * @param categoryId The ID of the category to get
     */
    fun getCategoryById(categoryId: String) {
        viewModelScope.launch {
            try {
                val category = categoryRepository.getCategoryById(categoryId)
                _currentCategory.value = category
            } catch (e: Exception) {
                _error.value = "Failed to get category: ${e.message}"
            }
        }
    }

    /**
     * Set the current category being edited
     * @param category The category to edit, or null to create a new one
     */
    fun setCurrentCategory(category: Category?) {
        _currentCategory.value = category
    }

    /**
     * Clear any error message
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Reset category saved flag
     */
    fun resetCategorySaved() {
        _categorySaved.value = false
    }
}