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

    fun crearGastoProgramado(
        expense: ScheduledExpense,
        onComplete: (Boolean) -> Unit
    ) {
        // Siempre agrega el campo estado
        val expenseMap = hashMapOf(
            "billeteraId" to expense.billeteraId,
            "categoria" to expense.categoria,
            "descripcion" to expense.descripcion,
            "fechaProgramada" to expense.fechaProgramada,
            "mes" to expense.mes,
            "montoEstimado" to expense.montoEstimado,
            "estado" to "pendiente",
            "montoReal" to null,
            "fechaPago" to null
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

    fun clonarGastosProgramadosDeMesAnterior(
        billeteraId: String,
        mesAnterior: String,
        mesNuevo: String,
        onComplete: (Boolean) -> Unit
    ) {
        db.collection("scheduled_expenses")
            .whereEqualTo("billeteraId", billeteraId)
            .whereEqualTo("mes", mesAnterior)
            .get()
            .addOnSuccessListener { result ->
                val batch = db.batch()
                for (doc in result.documents) {
                    val data = doc.data?.toMutableMap() ?: continue
                    data["mes"] = mesNuevo
                    data["estado"] = "pendiente"
                    data["montoReal"] = null
                    data["fechaPago"] = null

                    val cal = Calendar.getInstance()
                    cal.timeInMillis = (data["fechaProgramada"] as? Long) ?: 0L
                    val sdf = SimpleDateFormat("MM-yyyy", Locale.getDefault())
                    val nuevoDate = sdf.parse(mesNuevo)
                    if (nuevoDate != null) {
                        val calNuevo = Calendar.getInstance()
                        calNuevo.time = nuevoDate
                        cal.set(Calendar.MONTH, calNuevo.get(Calendar.MONTH))
                        cal.set(Calendar.YEAR, calNuevo.get(Calendar.YEAR))
                    }
                    data["fechaProgramada"] = cal.timeInMillis
                    data.remove("id")
                    db.collection("scheduled_expenses").document().let { batch.set(it, data) }
                }
                batch.commit().addOnCompleteListener { task ->
                    onComplete(task.isSuccessful)
                }
            }
            .addOnFailureListener {
                onComplete(false)
            }
    }
}