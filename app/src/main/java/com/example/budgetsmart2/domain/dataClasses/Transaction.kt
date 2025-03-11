package com.example.budgetsmart2.domain.dataClasses

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import com.example.budgetsmart2.domain.enums.TransactionType
import java.util.Date

@Parcelize
data class Transaction(
    val id: String = "", // Unique identifier for each transaction
    val amount: Double = 0.0,
    val description: String = "",
    val categoryId: String = "",  // Reference to the Category's ID
    val date: Date = Date(),
    val type: TransactionType = TransactionType.EXPENSE,
    val userId: String = "" //  Links the transaction to a specific user for multi-user support
) : Parcelable
{
    constructor() : this("", 0.0, "", "", Date(), TransactionType.EXPENSE, "")
}

