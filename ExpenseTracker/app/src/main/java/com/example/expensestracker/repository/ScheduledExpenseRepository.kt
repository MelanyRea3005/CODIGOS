package com.example.expensestracker.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.example.expensestracker.model.ScheduledExpense
import java.text.SimpleDateFormat
import java.util.*

class ScheduledExpenseRepository {
    private val db = FirebaseFirestore.getInstance()

    fun getScheduledExpenses(
        billeteraId: String,
        mes: String,
        onResult: (List<ScheduledExpense>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("scheduled_expenses")
            .whereEqualTo("billeteraId", billeteraId)
            .whereEqualTo("mes", mes)
            .orderBy("fechaProgramada", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { result ->
                val expenses = result.documents.mapNotNull { it.toObject(ScheduledExpense::class.java)?.copy(id = it.id) }
                onResult(expenses)
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }

    fun getAllScheduledExpenses(
        billeteraId: String,
        onResult: (List<ScheduledExpense>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("scheduled_expenses")
            .whereEqualTo("billeteraId", billeteraId)
            .orderBy("fechaProgramada", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { result ->
                val expenses = result.documents.mapNotNull { it.toObject(ScheduledExpense::class.java)?.copy(id = it.id) }
                onResult(expenses)
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }

    fun crearGastoProgramado(
        expense: ScheduledExpense,
        onComplete: (Boolean) -> Unit
    ) {
        val expenseMap = hashMapOf(
            "billeteraId" to expense.billeteraId,
            "categoria" to expense.categoria,
            "descripcion" to expense.descripcion,
            "fechaProgramada" to expense.fechaProgramada,
            "mes" to expense.mes,
            "montoEstimado" to expense.montoEstimado,
            "estado" to "pendiente",
            "montoReal" to null,
            "fechaPago" to null,
            "userId" to expense.userId
        )
        db.collection("scheduled_expenses")
            .add(expenseMap)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun marcarComoPagado(
        id: String,
        montoReal: Double,
        fechaPago: Long,
        onComplete: (Boolean) -> Unit
    ) {
        db.collection("scheduled_expenses").document(id)
            .update(
                mapOf(
                    "montoReal" to montoReal,
                    "fechaPago" to fechaPago,
                    "estado" to "pagado"
                )
            )
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun editarCategoria(
        id: String,
        nuevaCategoria: String,
        onComplete: (Boolean) -> Unit
    ) {
        db.collection("scheduled_expenses").document(id)
            .update("categoria", nuevaCategoria)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun eliminarGasto(
        id: String,
        onComplete: (Boolean) -> Unit
    ) {
        db.collection("scheduled_expenses").document(id)
            .delete()
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }
}