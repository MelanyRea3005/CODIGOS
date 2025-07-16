package com.example.expensestracker.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.expensestracker.R
import com.example.expensestracker.model.Expense

class ExpenseAdapter(
    private var items: List<Expense>,
    private val onItemClick: (Expense) -> Unit
) : RecyclerView.Adapter<ExpenseAdapter.ViewHolder>() {

    fun updateList(newList: List<Expense>) {
        items = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_expense, parent, false)
        return ViewHolder(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], onItemClick)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val description: TextView = itemView.findViewById(R.id.expenseDescription)
        private val amount: TextView = itemView.findViewById(R.id.expenseAmount)
        private val date: TextView = itemView.findViewById(R.id.expenseDate)
        private val category: TextView = itemView.findViewById(R.id.expenseCategory)

        fun bind(expense: Expense, onItemClick: (Expense) -> Unit) {
            description.text = expense.description
            amount.text = "$%.2f".format(expense.amount)
            date.text = expense.date
            category.text = expense.category ?: ""
            itemView.setOnClickListener { onItemClick(expense) }
        }
    }
}