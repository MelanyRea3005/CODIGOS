package com.example.expensestracker.model

data class ScheduledExpense(
    val id: String = "",
    val billeteraId: String = "",
    val categoria: String = "",
    val descripcion: String = "",
    val fechaProgramada: Long = 0L,
    val mes: String = "",
    val montoEstimado: Double = 0.0,
    val montoReal: Double? = null,
    val fechaPago: Long? = null,
    val estado: String = "pendiente",
    val userId: String = ""
)