package com.example.expensestracker.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.expensestracker.R
import com.example.expensestracker.model.ScheduledExpense
import java.text.SimpleDateFormat
import java.util.*

class ScheduledExpenseAdapter(
    private var expenses: List<ScheduledExpense>,
    private val onMarkAsPaid: (ScheduledExpense) -> Unit,
    private val onEdit: (ScheduledExpense) -> Unit,
    private val onDelete: (ScheduledExpense) -> Unit,
    private val isReadOnly: Boolean
) : RecyclerView.Adapter<ScheduledExpenseAdapter.ViewHolder>() {

    fun updateList(newList: List<ScheduledExpense>) {
        expenses = newList
        notifyDataSetChanged()
    }

    override fun getItemCount() = expenses.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_scheduled_expense, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val expense = expenses[position]
        holder.bind(expense, isReadOnly)

        // Solo asigna listeners si NO es solo lectura
        if (!isReadOnly) {
            holder.btnPagar.setOnClickListener { onMarkAsPaid(expense) }
            holder.btnEditar.setOnClickListener { onEdit(expense) }
            holder.btnEliminar.setOnClickListener { onDelete(expense) }
        } else {
            // Listeners nulos para evitar interacción
            holder.btnPagar.setOnClickListener(null)
            holder.btnEditar.setOnClickListener(null)
            holder.btnEliminar.setOnClickListener(null)
            // Además, asegura que no puedan interactuar (opcional):
            holder.btnPagar.isEnabled = false
            holder.btnEditar.isEnabled = false
            holder.btnEliminar.isEnabled = false
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCategoria: TextView = view.findViewById(R.id.tvCategoria)
        val tvEstado: TextView = view.findViewById(R.id.tvEstado)
        val tvFecha: TextView = view.findViewById(R.id.tvFecha)
        val tvMonto: TextView = view.findViewById(R.id.tvMonto)
        val btnPagar: ImageButton = view.findViewById(R.id.btnPagar)
        val btnEditar: ImageButton = view.findViewById(R.id.btnEditar)
        val btnEliminar: ImageButton = view.findViewById(R.id.btnEliminar)

        fun bind(expense: ScheduledExpense, isReadOnly: Boolean) {
            tvCategoria.text = expense.categoria
            tvFecha.text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(expense.fechaProgramada))
            tvMonto.text = "Programado: $%.2f".format(expense.montoEstimado)

            if (expense.fechaPago != null) {
                tvEstado.text = "Pagado"
                tvEstado.setBackgroundResource(R.drawable.bg_estado_pagado)
                btnPagar.visibility = View.GONE
            } else {
                val now = System.currentTimeMillis()
                if (expense.fechaProgramada < now) {
                    tvEstado.text = "Vencido"
                    tvEstado.setBackgroundResource(R.drawable.bg_estado_vencido)
                } else {
                    tvEstado.text = "Pendiente"
                    tvEstado.setBackgroundResource(R.drawable.bg_estado_pendiente)
                }
                btnPagar.visibility = if (isReadOnly) View.GONE else View.VISIBLE
            }

            btnEditar.visibility = if (isReadOnly) View.GONE else View.VISIBLE
            btnEliminar.visibility = if (isReadOnly) View.GONE else View.VISIBLE
        }
    }
}