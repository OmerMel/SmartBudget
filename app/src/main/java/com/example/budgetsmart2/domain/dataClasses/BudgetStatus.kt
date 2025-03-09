package com.example.budgetsmart2.domain.dataClasses

// For budget status tracking
data class BudgetStatus(
    val budget: Budget,
    val category: Category,
    val spent: Double,
    val remaining: Double,
    val percentage: Double
) {
    val isOverBudget: Boolean get() = spent > budget.amount
}
