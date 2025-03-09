package com.example.budgetsmart2.data.repositoriesImp

import android.util.Log
import com.example.budgetsmart2.domain.dataClasses.Category
import com.example.budgetsmart2.domain.repositories.CategoryRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

class FirestoreCategoryRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : CategoryRepository {

    // Reference to the "categories" collection in Firestore
    private val categoriesCollection = firestore.collection("categories")

    /**
     * Retrieves all categories for a specific user.
     * Returns categories alphabetically by name for consistent display.
     */
    override suspend fun getCategories(userId: String): List<Category> = withContext(Dispatchers.IO) {
        try {
            // Query categories for the specific user, ordered by name
            val snapshot = categoriesCollection
                .whereEqualTo("userId", userId)
                .orderBy("name", Query.Direction.ASCENDING)
                .get()
                .await()

            // Convert Firestore documents to Category objects
            return@withContext snapshot.toObjects(Category::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting categories: ${e.message}", e)
            return@withContext emptyList()
        }
    }

    /**
     * Retrieves a single category by its unique identifier.
     */
    override suspend fun getCategoryById(id: String): Category? = withContext(Dispatchers.IO) {
        try {
            // Fetch a single document by ID
            val document = categoriesCollection.document(id).get().await()

            // Convert to Category object if document exists
            return@withContext if (document.exists()) {
                document.toObject(Category::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting category by ID: ${e.message}", e)
            return@withContext null
        }
    }

    /**
     * Creates a new category in the database.
     * Generates a new ID if one is not provided.
     */
    override suspend fun addCategory(category: Category): String = withContext(Dispatchers.IO) {
        try {
            // Create a new category with a generated ID if none is provided
            val newCategory = if (category.id.isBlank()) {
                // Generate a new document ID
                val newId = categoriesCollection.document().id
                category.copy(id = newId)
            } else {
                category
            }

            // Save to Firestore using the document ID
            categoriesCollection.document(newCategory.id).set(newCategory).await()

            // Return the ID of the created category
            return@withContext newCategory.id
        } catch (e: Exception) {
            Log.e(TAG, "Error adding category: ${e.message}", e)
            throw e  // Rethrow to handle in the ViewModel
        }
    }

    /**
     * Updates an existing category in the database.
     */
    override suspend fun updateCategory(category: Category): Boolean = withContext(Dispatchers.IO) {
        try {
            // Check that we have a valid ID
            if (category.id.isBlank()) {
                Log.e(TAG, "Cannot update category with empty ID")
                return@withContext false
            }

            // Update the document with the same ID
            categoriesCollection.document(category.id).set(category).await()
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating category: ${e.message}", e)
            return@withContext false
        }
    }

    /**
     * Deletes a category from the database.
     * Note: This does not handle associated transactions or budgets.
     */
    override suspend fun deleteCategory(id: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Delete the document by ID
            categoriesCollection.document(id).delete().await()
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting category: ${e.message}", e)
            return@withContext false
        }
    }

    companion object {
        private const val TAG = "FirestoreCategoryRepo"
    }
}