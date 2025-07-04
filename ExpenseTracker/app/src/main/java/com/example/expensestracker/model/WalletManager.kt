package com.example.expensestracker.model

import com.example.expensestracker.network.AuthManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class WalletManager {
    private val db = FirebaseFirestore.getInstance()
    private val walletsCollection = db.collection("wallets")

    suspend fun createWallet(name: String, isPrivate: Boolean = true): String {
        val currentUser = AuthManager.getCurrentUser() ?: throw IllegalStateException("No hay usuario autenticado")
        val userId = currentUser.uid

        val wallet = Wallet(
            id = UUID.randomUUID().toString(),
            name = name,
            ownerId = userId,
            balance = 0.0,
            isPrivate = isPrivate,
            accessibleTo = listOf(userId)
        )

        walletsCollection.document(wallet.id).set(wallet).await()
        return wallet.id
    }

    suspend fun updateWalletBalance(walletId: String, amount: Double, isExpense: Boolean) {
        val wallet = getWallet(walletId) ?: throw IllegalStateException("Billetera no encontrada")
        val currentUser = AuthManager.getCurrentUser() ?: throw IllegalStateException("No hay usuario autenticado")

        if (wallet.ownerId != currentUser.uid &&
            wallet.sharedWith[currentUser.uid] != SharePermission.READ_WRITE) {
            throw IllegalStateException("No tienes permiso para modificar esta billetera")
        }

        val newBalance = if (isExpense) {
            wallet.balance - amount
        } else {
            wallet.balance + amount
        }

        walletsCollection.document(walletId)
            .update("balance", newBalance)
            .await()
    }

    fun getUserWallets(): Flow<List<Wallet>> = callbackFlow {
        val currentUser = AuthManager.getCurrentUser() ?: throw IllegalStateException("No hay usuario autenticado")
        val userId = currentUser.uid

        val listenerRegistration = walletsCollection
            .whereArrayContains("accessibleTo", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val wallets = snapshot?.documents?.mapNotNull {
                    it.toObject<Wallet>()
                } ?: emptyList()

                trySend(wallets)
            }

        awaitClose { listenerRegistration.remove() }
    }

    suspend fun shareWallet(walletId: String, email: String, permission: SharePermission) {
        // Buscar el usuario por email en la colección de usuarios
        val targetUserDoc = db.collection("users")
            .whereEqualTo("email", email)
            .get()
            .await()
            .documents
            .firstOrNull() ?: throw IllegalArgumentException("Usuario no encontrado")

        val targetUserId = targetUserDoc.id
        val wallet = getWallet(walletId) ?: throw IllegalStateException("Billetera no encontrada")
        val currentUser = AuthManager.getCurrentUser() ?: throw IllegalStateException("No hay usuario autenticado")

        if (wallet.ownerId != currentUser.uid) {
            throw IllegalStateException("No tienes permiso para compartir esta billetera")
        }

        val updatedSharedWith = wallet.sharedWith.toMutableMap()
        updatedSharedWith[targetUserId] = permission

        val updatedAccessibleTo = (wallet.accessibleTo + targetUserId).distinct()

        walletsCollection.document(walletId)
            .update(
                mapOf(
                    "sharedWith" to updatedSharedWith,
                    "accessibleTo" to updatedAccessibleTo
                )
            )
            .await()
    }

    private suspend fun getWallet(walletId: String): Wallet? {
        return walletsCollection.document(walletId)
            .get()
            .await()
            .toObject()
    }
}