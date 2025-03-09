package com.example.budgetsmart2.data.repositoriesImp

import android.icu.util.Calendar
import android.util.Log
import com.example.budgetsmart2.domain.dataClasses.Transaction
import com.example.budgetsmart2.domain.repositories.TransactionRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

class FirestoreTransactionRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : TransactionRepository {

    // Reference to the "transactions" collection in Firestore
    private val transactionsCollection = firestore.collection("transactions")

    override suspend fun getTransactions(userId: String): List<Transaction> = withContext(
        Dispatchers.IO) {
        try {
            // Query transactions for the specific user, ordered by date (newest first)
            val snapshot = transactionsCollection
                .whereEqualTo("userId", userId)
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .await() // Suspends until data is fetched

            // Convert Firestore documents to Transaction objects
            return@withContext snapshot.toObjects(Transaction::class.java)
        } catch (e: Exception) {
            // Log error and return empty list on failure
            Log.e(TAG, "Error getting transactions: ${e.message}", e)
            return@withContext emptyList()
        }
    }

    override suspend fun getTransactionById(id: String): Transaction? = withContext(Dispatchers.IO) {
        try {
            // Fetch a single document by ID
            val document = transactionsCollection.document(id).get().await()

            // Convert to Transaction object if document exists
            return@withContext if (document.exists()) {
                document.toObject(Transaction::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting transaction by ID: ${e.message}", e)
            return@withContext null
        }
    }

    override suspend fun addTransaction(transaction: Transaction): String = withContext(Dispatchers.IO) {
        try {
            // Create a new transaction with a generated ID if none is provided
            val newTransaction = if (transaction.id.isBlank()) {
                // Generate a new document ID
                val newId = transactionsCollection.document().id
                transaction.copy(id = newId)
            } else {
                transaction
            }

            // Save to Firestore using the document ID from the transaction
            transactionsCollection.document(newTransaction.id).set(newTransaction).await()

            // Return the ID of the created transaction
            return@withContext newTransaction.id
        } catch (e: Exception) {
            Log.e(TAG, "Error adding transaction: ${e.message}", e)
            throw e  // Rethrow to handle in the ViewModel
        }
    }

    override suspend fun updateTransaction(transaction: Transaction): Boolean = withContext(Dispatchers.IO) {
        try {
            // Update the document with the same ID
            transactionsCollection.document(transaction.id).set(transaction).await()
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating transaction: ${e.message}", e)
            return@withContext false
        }
    }

    override suspend fun deleteTransaction(id: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Delete the document by ID
            transactionsCollection.document(id).delete().await()
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting transaction: ${e.message}", e)
            return@withContext false
        }
    }

    override suspend fun getTransactionsByCategory(userId: String, categoryId: String): List<Transaction> =
        withContext(Dispatchers.IO) {
            try {
                // Query transactions for specific user and category
                val snapshot = transactionsCollection
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("categoryId", categoryId)
                    .orderBy("date", Query.Direction.DESCENDING)
                    .get()
                    .await()

                return@withContext snapshot.toObjects(Transaction::class.java)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting transactions by category: ${e.message}", e)
                return@withContext emptyList()
            }
        }

    override suspend fun getTransactionsByPeriod(userId: String, month: Int, year: Int): List<Transaction> =
        withContext(Dispatchers.IO) {
            try {
                // Create date range for the specified month
                val calendar = Calendar.getInstance()

                // Set to start of month: YYYY-MM-01 00:00:00.000
                calendar.set(year, month, 1, 0, 0, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startDate = calendar.time

                // Set to end of month: YYYY-MM-[last_day] 23:59:59.999
                calendar.set(year, month, calendar.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val endDate = calendar.time

                // Query transactions within the date range
                val snapshot = transactionsCollection
                    .whereEqualTo("userId", userId)
                    .whereGreaterThanOrEqualTo("date", startDate)
                    .whereLessThanOrEqualTo("date", endDate)
                    .orderBy("date", Query.Direction.DESCENDING)
                    .get()
                    .await()

                return@withContext snapshot.toObjects(Transaction::class.java)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting transactions by period: ${e.message}", e)
                return@withContext emptyList()
            }
        }

    companion object {
        private const val TAG = "FirestoreTransactionRepo"
    }
}