package com.example.expensestracker.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.Timestamp
import java.util.Date

data class Transaction(
    @DocumentId
    val id: String = "",
    val walletId: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val amount: Double = 0.0,
    val description: String = "",
    val category: String = "",
    val date: Timestamp = Timestamp(Date()),
    val userId: String = ""
)

enum class TransactionType {
    INCOME, EXPENSE
}