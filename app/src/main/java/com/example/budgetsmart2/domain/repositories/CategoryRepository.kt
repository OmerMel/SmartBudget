package com.example.budgetsmart2.domain.repositories

import com.example.budgetsmart2.domain.dataClasses.Category

interface CategoryRepository {
    /**
     * Retrieves all categories for a specific user.
     * @param userId The unique identifier of the user
     * @return List of categories belonging to the user
     */
    suspend fun getCategories(userId: String): List<Category>

    /**
     * Retrieves a single category by its unique identifier.
     * @param id The unique identifier of the category to fetch
     * @return The category if found, null otherwise
     */
    suspend fun getCategoryById(id: String): Category?

    /**
     * Creates a new category in the database.
     * @param category The category object to be saved
     * @return The ID of the newly created category
     */
    suspend fun addCategory(category: Category): String

    /**
     * Updates an existing category in the database.
     * @param category The category object with updated information
     * @return true if update was successful, false otherwise
     */
    suspend fun updateCategory(category: Category): Boolean

    /**
     * Deletes a category from the database.
     * @param id The unique identifier of the category to delete
     * @return true if deletion was successful, false otherwise
     * @note Consider how to handle transactions with this category
     */
    suspend fun deleteCategory(id: String): Boolean
}