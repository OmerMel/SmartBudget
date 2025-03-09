package com.example.budgetsmart2.domain.dataClasses

// For financial summary (shown on home screen)
data class FinancialSummary(
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val balance: Double = 0.0,
    val monthlyBudget: Double = 0.0,
    val budgetUsedPercentage: Double = 0.0
)
