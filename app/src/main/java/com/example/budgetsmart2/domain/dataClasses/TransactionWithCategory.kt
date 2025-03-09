package com.example.budgetsmart2.domain.dataClasses

// For transaction list with category details
data class TransactionWithCategory(
    val transaction: Transaction,
    val category: Category
)
