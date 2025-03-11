package com.example.budgetsmart2.domain.dataClasses

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// For transaction list with category details
@Parcelize
data class TransactionWithCategory(
    val transaction: Transaction,
    val category: Category
) : Parcelable
