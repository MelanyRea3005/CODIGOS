package com.example.expensestracker.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.expensestracker.R
import com.example.expensestracker.model.ScheduledExpense
import java.text.SimpleDateFormat
import java.util.*

class ScheduledExpenseAdapter(
    private var items: List<ScheduledExpense>,
    private val onMarkAsPaid: (ScheduledExpense) -> Unit,
    private val onEdit: (ScheduledExpense) -> Unit,
    private val onDelete: (ScheduledExpense) -> Unit
) : RecyclerView.Adapter<ScheduledExpenseAdapter.ViewHolder>() {

    fun updateList(newList: List<ScheduledExpense>) {
        items = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_scheduled_expense, parent, false)
        return ViewHolder(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], onMarkAsPaid, onEdit, onDelete)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val categoria: TextView = itemView.findViewById(R.id.scheduledCategory)
        private val monto: TextView = itemView.findViewById(R.id.scheduledAmount)
        private val fecha: TextView = itemView.findViewById(R.id.scheduledDate)
        private val estado: TextView = itemView.findViewById(R.id.scheduledStatus)
        private val btnPagar: Button = itemView.findViewById(R.id.scheduledPayButton)
        private val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)

        fun bind(
            expense: ScheduledExpense,
            onMarkAsPaid: (ScheduledExpense) -> Unit,
            onEdit: (ScheduledExpense) -> Unit,
            onDelete: (ScheduledExpense) -> Unit
        ) {
            categoria.text = expense.categoria
            monto.text = "Programado: $%.2f".format(expense.montoEstimado)
            fecha.text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(expense.fechaProgramada))

            val now = System.currentTimeMillis()
            val pagado = expense.fechaPago != null
            val vencido = !pagado && expense.fechaProgramada < now
            val pendiente = !pagado && !vencido

            when {
                pagado -> {
                    estado.text = "Pagado: $%.2f".format(expense.montoReal ?: 0.0)
                    estado.setTextColor(itemView.context.getColor(R.color.blue_500))
                    btnPagar.visibility = View.GONE
                }
                vencido -> {
                    estado.text = "Vencido"
                    estado.setTextColor(itemView.context.getColor(R.color.red_400))
                    btnPagar.visibility = View.VISIBLE
                }
                pendiente -> {
                    estado.text = "Pendiente"
                    estado.setTextColor(0xFFFFA000.toInt())
                    btnPagar.visibility = View.VISIBLE
                }
            }

            btnPagar.setOnClickListener { onMarkAsPaid(expense) }
            btnEdit.setOnClickListener { onEdit(expense) }
            btnDelete.setOnClickListener { onDelete(expense) }
        }
    }
}