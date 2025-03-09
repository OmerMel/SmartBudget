package com.example.budgetsmart2.di

import com.example.budgetsmart2.data.repositoriesImp.FirestoreBudgetRepository
import com.example.budgetsmart2.data.repositoriesImp.FirestoreCategoryRepository
import com.example.budgetsmart2.data.repositoriesImp.FirestoreTransactionRepository
import com.example.budgetsmart2.data.repositoriesImp.FirestoreUserRepository
import com.example.budgetsmart2.domain.repositories.BudgetRepository
import com.example.budgetsmart2.domain.repositories.CategoryRepository
import com.example.budgetsmart2.domain.repositories.TransactionRepository
import com.example.budgetsmart2.domain.repositories.UserRepository
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    /**
     * Provides FirebaseFirestore instance
     */
    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    /**
     * Provides BudgetRepository implementation
     */
    @Provides
    @Singleton
    fun provideBudgetRepository(firestore: FirebaseFirestore): BudgetRepository {
        return FirestoreBudgetRepository(firestore)
    }

    /**
     * Provides CategoryRepository implementation
     */
    @Provides
    @Singleton
    fun provideCategoryRepository(firestore: FirebaseFirestore): CategoryRepository {
        return FirestoreCategoryRepository(firestore)
    }

    /**
     * Provides TransactionRepository implementation
     */
    @Provides
    @Singleton
    fun provideTransactionRepository(firestore: FirebaseFirestore): TransactionRepository {
        return FirestoreTransactionRepository(firestore)
    }

    /**
     * Provides UserRepository implementation
     */
    @Provides
    @Singleton
    fun provideUserRepository(firestore: FirebaseFirestore): UserRepository {
        return FirestoreUserRepository(firestore)
    }
}
