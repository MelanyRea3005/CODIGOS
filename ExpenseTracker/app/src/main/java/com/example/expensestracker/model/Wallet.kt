package com.example.expensestracker.model

import com.google.firebase.firestore.DocumentId

data class Wallet(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val ownerId: String = "",
    val balance: Double = 0.0,
    val sharedWith: Map<String, SharePermission> = mapOf(),
    val isPrivate: Boolean = true,
    val accessibleTo: List<String> = listOf()
)

enum class SharePermission {
    READ_ONLY,
    READ_WRITE
}