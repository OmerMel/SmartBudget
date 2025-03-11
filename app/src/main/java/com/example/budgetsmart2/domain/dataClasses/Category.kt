package com.example.budgetsmart2.domain.dataClasses

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Category(
    val id: String = "", // Unique identifier for each category
    val name: String = "",
    val icon: String = "", // Icon identifier or emoji
    val color: Int = 0,    // Color resource for the category
    val userId: String = "" // Associates the category with a specific user
) : Parcelable
{
    constructor() : this("", "", "", 0, "")  // For Firestore
}