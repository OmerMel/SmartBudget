package com.example.budgetsmart2.data.repositoriesImp

import android.util.Log
import com.example.budgetsmart2.domain.dataClasses.User
import com.example.budgetsmart2.domain.repositories.UserRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

class FirestoreUserRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : UserRepository {

    // Reference to the "users" collection in Firestore
    private val usersCollection = firestore.collection("users")

    /**
     * Retrieves user information by ID.
     * @param id The unique identifier of the user (matches Firebase Auth UID)
     * @return The user if found, null otherwise
     */
    override suspend fun getUser(id: String): User? = withContext(Dispatchers.IO) {
        try {
            // Query for the user document by ID
            val document = usersCollection.document(id).get().await()

            // Convert to User object if document exists
            return@withContext if (document.exists()) {
                document.toObject(User::class.java)
            } else {
                Log.d(TAG, "No user found with ID: $id")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user: ${e.message}", e)
            return@withContext null
        }
    }

    /**
     * Creates a new user record in the database.
     * Typically called after successful Firebase Authentication.
     * The document ID in Firestore should match the Firebase Auth UID.
     */
    override suspend fun createUser(user: User): Boolean = withContext(Dispatchers.IO) {
        try {
            // Ensure the user has an ID
            if (user.id.isBlank()) {
                Log.e(TAG, "Cannot create user with empty ID")
                return@withContext false
            }

            // Check if user already exists
            val existingUser = getUser(user.id)
            if (existingUser != null) {
                Log.w(TAG, "User already exists, consider updating instead")
                return@withContext false
            }

            // Create the user document with the ID as the document ID
            usersCollection.document(user.id).set(user).await()

            Log.d(TAG, "User created successfully with ID: ${user.id}")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error creating user: ${e.message}", e)
            return@withContext false
        }
    }

    /**
     * Updates an existing user's information.
     * Used for updating preferences like default currency.
     */
    override suspend fun updateUser(user: User): Boolean = withContext(Dispatchers.IO) {
        try {
            // Ensure the user has an ID
            if (user.id.isBlank()) {
                Log.e(TAG, "Cannot update user with empty ID")
                return@withContext false
            }

            // Check if user exists before updating
            val existingUser = getUser(user.id)
            if (existingUser == null) {
                Log.w(TAG, "User doesn't exist, consider creating instead")
                return@withContext false
            }

            // Update the user document
            usersCollection.document(user.id).set(user).await()

            Log.d(TAG, "User updated successfully with ID: ${user.id}")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user: ${e.message}", e)
            return@withContext false
        }
    }

    companion object {
        private const val TAG = "FirestoreUserRepo"
    }
}