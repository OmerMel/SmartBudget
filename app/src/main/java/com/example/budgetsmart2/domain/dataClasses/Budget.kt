package com.example.budgetsmart2.domain.dataClasses

import android.icu.util.Calendar
import java.util.Date

data class Budget(
    val id: String = "", // Unique identifier for the budget
    val categoryId: String = "",
    val amount: Double = 0.0,
    val month: Int = Calendar.getInstance().get(Calendar.MONTH),
    val year: Int = Calendar.getInstance().get(Calendar.YEAR),
    val userId: String = "" // Associates the budget with a specific user
) {
    constructor() : this("", "", 0.0,
        Calendar.getInstance().get(Calendar.MONTH),
        Calendar.getInstance().get(Calendar.YEAR),
        "")
}

