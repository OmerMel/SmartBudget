package com.example.budgetsmart2.domain.repositories

import com.example.budgetsmart2.domain.dataClasses.Transaction

interface TransactionRepository {
    /**
     * Retrieves all transactions for a specific user.
     * @param userId The unique identifier of the user
     * @return List of transactions belonging to the user, typically sorted by date (newest first)
     */
    suspend fun getTransactions(userId: String): List<Transaction>

    /**
     * Retrieves a single transaction by its unique identifier.
     * @param id The unique identifier of the transaction to fetch
     * @return The transaction if found, null otherwise
     */
    suspend fun getTransactionById(id: String): Transaction?

    /**
     * Creates a new transaction in the database.
     * @param transaction The transaction object to be saved
     * @return The ID of the newly created transaction
     * @throws Exception if the transaction cannot be added
     */
    suspend fun addTransaction(transaction: Transaction): String

    /**
     * Updates an existing transaction in the database.
     * @param transaction The transaction object with updated information
     * @return true if update was successful, false otherwise
     */
    suspend fun updateTransaction(transaction: Transaction): Boolean

    /**
     * Deletes a transaction from the database.
     * @param id The unique identifier of the transaction to delete
     * @return true if deletion was successful, false otherwise
     */
    suspend fun deleteTransaction(id: String): Boolean

    /**
     * Retrieves transactions for a specific user filtered by category.
     * @param userId The unique identifier of the user
     * @param categoryId The category to filter transactions by
     * @return List of transactions matching the criteria
     */
    suspend fun getTransactionsByCategory(userId: String, categoryId: String): List<Transaction>

    /**
     * Retrieves transactions for a specific user within a given month and year.
     * @param userId The unique identifier of the user
     * @param month The month (0-11, where 0 is January)
     * @param year The year
     * @return List of transactions within the specified time period
     */
    suspend fun getTransactionsByPeriod(userId: String, month: Int, year: Int): List<Transaction>
}