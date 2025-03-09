package com.example.budgetsmart2.domain.repositories

import com.example.budgetsmart2.domain.dataClasses.User

interface UserRepository {
    /**
     * Retrieves user information by ID.
     * @param id The unique identifier of the user (matches Firebase Auth UID)
     * @return The user if found, null otherwise
     */
    suspend fun getUser(id: String): User?

    /**
     * Creates a new user record in the database.
     * @param user The user object to be saved
     * @return true if creation was successful, false otherwise
     * @note Typically called after successful Firebase Authentication
     */
    suspend fun createUser(user: User): Boolean

    /**
     * Updates an existing user's information.
     * @param user The user object with updated information
     * @return true if update was successful, false otherwise
     */
    suspend fun updateUser(user: User): Boolean
}