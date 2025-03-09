package com.example.budgetsmart2.domain.dataClasses

data class Category(
    val id: String = "", // Unique identifier for each category, Used as a reference in transactions (via categoryId)
    val name: String = "",
    val icon: String = "", // Icon identifier or emoji
    val color: Int = 0,    // Color resource for the category
    val userId: String = "" //Associates the category with a specific user
) {
    constructor() : this("", "", "", 0, "")  // For Firestore
}
