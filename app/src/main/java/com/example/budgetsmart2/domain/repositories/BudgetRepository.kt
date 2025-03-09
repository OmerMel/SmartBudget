package com.example.budgetsmart2.domain.repositories

import com.example.budgetsmart2.domain.dataClasses.Budget

interface BudgetRepository {
    /**
     * Retrieves all budgets for a specific user.
     * @param userId The unique identifier of the user
     * @return List of budgets belonging to the user
     */
    suspend fun getBudgets(userId: String): List<Budget>

    /**
     * Retrieves a single budget by its unique identifier.
     * @param id The unique identifier of the budget to fetch
     * @return The budget if found, null otherwise
     */
    suspend fun getBudgetById(id: String): Budget?

    /**
     * Retrieves a budget for a specific category and user.
     * @param userId The unique identifier of the user
     * @param categoryId The category ID to get the budget for
     * @return The budget for the specified category if found, null otherwise
     */
    suspend fun getBudgetByCategory(userId: String, categoryId: String): Budget?

    /**
     * Creates a new budget in the database.
     * @param budget The budget object to be saved
     * @return The ID of the newly created budget
     */
    suspend fun addBudget(budget: Budget): String

    /**
     * Updates an existing budget in the database.
     * @param budget The budget object with updated information
     * @return true if update was successful, false otherwise
     */
    suspend fun updateBudget(budget: Budget): Boolean

    /**
     * Deletes a budget from the database.
     * @param id The unique identifier of the budget to delete
     * @return true if deletion was successful, false otherwise
     */
    suspend fun deleteBudget(id: String): Boolean
}