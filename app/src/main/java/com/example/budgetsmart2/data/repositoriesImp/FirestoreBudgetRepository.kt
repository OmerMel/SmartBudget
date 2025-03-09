package com.example.budgetsmart2.data.repositoriesImp

import android.util.Log
import com.example.budgetsmart2.domain.dataClasses.Budget
import com.example.budgetsmart2.domain.repositories.BudgetRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

class FirestoreBudgetRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : BudgetRepository {

    // Reference to the "budgets" collection in Firestore
    private val budgetsCollection = firestore.collection("budgets")

    /**
     * Retrieves all budgets for a specific user.
     * Useful for showing budget overview and reports.
     */
    override suspend fun getBudgets(userId: String): List<Budget> = withContext(Dispatchers.IO) {
        try {
            // Query budgets for the specific user
            val snapshot = budgetsCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()

            return@withContext snapshot.toObjects(Budget::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting budgets: ${e.message}", e)
            return@withContext emptyList()
        }
    }

    /**
     * Retrieves a single budget by its unique identifier.
     * Used when editing a specific budget.
     */
    override suspend fun getBudgetById(id: String): Budget? = withContext(Dispatchers.IO) {
        try {
            // Fetch a single document by ID
            val document = budgetsCollection.document(id).get().await()

            return@withContext if (document.exists()) {
                document.toObject(Budget::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting budget by ID: ${e.message}", e)
            return@withContext null
        }
    }

    /**
     * Retrieves a budget for a specific category and user.
     * Useful when calculating budget status for a particular category.
     */
    override suspend fun getBudgetByCategory(userId: String, categoryId: String): Budget? =
        withContext(Dispatchers.IO) {
            try {
                // Query for budget with specific category and user
                val snapshot = budgetsCollection
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("categoryId", categoryId)
                    .limit(1)  // We only need one result
                    .get()
                    .await()

                // Return the first document or null if none exists
                return@withContext if (!snapshot.isEmpty) {
                    snapshot.documents[0].toObject(Budget::class.java)
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting budget by category: ${e.message}", e)
                return@withContext null
            }
        }

    /**
     * Creates a new budget in the database.
     * Generates a new ID if one is not provided.
     */
    override suspend fun addBudget(budget: Budget): String = withContext(Dispatchers.IO) {
        try {
            // Check if a budget already exists for this category
            val existingBudget = getBudgetByCategory(budget.userId, budget.categoryId)

            // If a budget already exists, throw an exception or update it
            if (existingBudget != null) {
                Log.w(TAG, "Budget already exists for this category, updating instead")
                // Option: Update the existing budget instead
                val updatedBudget = existingBudget.copy(amount = budget.amount)
                updateBudget(updatedBudget)
                return@withContext existingBudget.id
            }

            // Create a new budget with a generated ID if none is provided
            val newBudget = if (budget.id.isBlank()) {
                val newId = budgetsCollection.document().id
                budget.copy(id = newId)
            } else {
                budget
            }

            // Save to Firestore
            budgetsCollection.document(newBudget.id).set(newBudget).await()

            return@withContext newBudget.id
        } catch (e: Exception) {
            Log.e(TAG, "Error adding budget: ${e.message}", e)
            throw e
        }
    }

    /**
     * Updates an existing budget in the database.
     */
    override suspend fun updateBudget(budget: Budget): Boolean = withContext(Dispatchers.IO) {
        try {
            // Check that we have a valid ID
            if (budget.id.isBlank()) {
                Log.e(TAG, "Cannot update budget with empty ID")
                return@withContext false
            }

            // Update the document
            budgetsCollection.document(budget.id).set(budget).await()
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating budget: ${e.message}", e)
            return@withContext false
        }
    }

    /**
     * Deletes a budget from the database.
     */
    override suspend fun deleteBudget(id: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Delete the document by ID
            budgetsCollection.document(id).delete().await()
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting budget: ${e.message}", e)
            return@withContext false
        }
    }

    companion object {
        private const val TAG = "FirestoreBudgetRepo"
    }
}