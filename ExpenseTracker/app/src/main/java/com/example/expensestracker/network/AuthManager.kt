package com.example.expensestracker.network

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object AuthManager {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    // Tus funciones existentes
    suspend fun signUp(email: String, password: String): FirebaseUser? {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        // Agregar creación del documento de usuario en Firestore
        result.user?.let { user ->
            val userData = hashMapOf(
                "email" to email,
                "uid" to user.uid
            )
            db.collection("users").document(user.uid).set(userData).await()
        }
        return result.user
    }

    suspend fun signIn(email: String, password: String): FirebaseUser? {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        return result.user
    }

    fun signOut() {
        auth.signOut()
    }

    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    // Nuevas funciones necesarias para la gestión de billeteras
    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    fun getCurrentUserEmail(): String? {
        return auth.currentUser?.email
    }

    suspend fun findUserByEmail(email: String): String? {
        return try {
            val querySnapshot = db.collection("users")
                .whereEqualTo("email", email)
                .get()
                .await()

            querySnapshot.documents.firstOrNull()?.id
        } catch (e: Exception) {
            null
        }
    }

    suspend fun checkWalletAccess(walletId: String): Boolean {
        val currentUserId = getCurrentUserId() ?: return false

        try {
            val walletDoc = db.collection("wallets")
                .document(walletId)
                .get()
                .await()

            val accessibleTo = walletDoc.get("accessibleTo") as? List<String>
            return accessibleTo?.contains(currentUserId) == true
        } catch (e: Exception) {
            return false
        }
    }
}