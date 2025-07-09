package com.example.expensestracker.model

import com.example.expensestracker.network.AuthManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
class WalletManager {
    private val db = FirebaseFirestore.getInstance()
    private val walletsCollection = db.collection("wallets")
    private val transactionsCollection = db.collection("transactions")

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

    fun observeTransactions(walletId: String, onUpdate: (List<Transaction>) -> Unit) {
        transactionsCollection
            .whereEqualTo("walletId", walletId)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                CoroutineScope(Dispatchers.Main).launch {
                    val txs = snapshot?.documents?.mapNotNull { it.toObject<Transaction>() } ?: emptyList()
                    onUpdate(txs)
                }
            }
    }
    suspend fun shareWallet(walletId: String, email: String, permission: SharePermission) {
        val targetUserId = AuthManager.findUserByEmail(email)
            ?: throw IllegalArgumentException("Usuario no encontrado")

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

    suspend fun addTransaction(walletId: String, transaction: Transaction) {
        if (!AuthManager.checkWalletAccess(walletId)) {
            throw IllegalStateException("No tienes acceso a esta billetera")
        }

        val transactionWithId = transaction.copy(
            id = UUID.randomUUID().toString(),
            walletId = walletId,
            userId = AuthManager.getCurrentUserId() ?: ""
        )

        transactionsCollection.document(transactionWithId.id).set(transactionWithId).await()

        val isExpense = transaction.type == TransactionType.EXPENSE
        updateWalletBalance(walletId, transaction.amount, isExpense)
    }

    suspend fun getWalletTransactions(walletId: String): List<Transaction> {
        if (!AuthManager.checkWalletAccess(walletId)) {
            throw IllegalStateException("No tienes acceso a esta billetera")
        }

        return try {
            val querySnapshot = transactionsCollection
                .whereEqualTo("walletId", walletId)
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .await()

            querySnapshot.documents.mapNotNull {
                it.toObject<Transaction>()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getWallet(walletId: String): Wallet? {
        if (!AuthManager.checkWalletAccess(walletId)) {
            return null
        }

        return try {
            val document = walletsCollection.document(walletId).get().await()
            document.toObject<Wallet>()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getTransactionSummary(walletId: String): Pair<Double, Double> {
        val transactions = getWalletTransactions(walletId)
        val income = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val expense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        return Pair(income, expense)
    }


    suspend fun getUserPermission(walletId: String, userId: String?): SharePermission? {
        if (userId == null) return null
        val wallet = getWallet(walletId) ?: return null
        if (wallet.ownerId == userId) return SharePermission.READ_WRITE
        return wallet.sharedWith[userId]
    }
}