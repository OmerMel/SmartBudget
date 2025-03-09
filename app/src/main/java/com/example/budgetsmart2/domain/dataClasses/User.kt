package com.example.budgetsmart2.domain.dataClasses

import java.util.Date

data class User(
    val id: String = "",
    val defaultCurrency: String = "USD",
    val createdAt: Date = Date()
) {
    constructor() : this("", "USD", Date())
}
