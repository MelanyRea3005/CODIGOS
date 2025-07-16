package com.example.expensestracker.model

data class Expense(
    val id: String = "",
    val description: String = "",
    val amount: Double = 0.0,
    val date: String = "",
    val category: String? = null,
    val userId: String = "",
    val type: String = "",
    val createdAt: Long? = null
)