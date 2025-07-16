package com.example.expensestracker.model

data class ScheduledExpense(
    val id: String = "",
    val billeteraId: String = "",
    val categoria: String = "",
    val montoEstimado: Double = 0.0,
    val fechaProgramada: Long = 0L,
    val montoReal: Double? = null,
    val fechaPago: Long? = null,
    val estado: String = "pendiente",
    val mes: String = "",
    val descripcion: String = ""
)