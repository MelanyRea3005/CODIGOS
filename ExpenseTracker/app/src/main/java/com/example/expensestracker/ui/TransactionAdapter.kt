package com.example.expensestracker.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.expensestracker.databinding.ItemTransactionBinding
import com.example.expensestracker.model.Transaction
import com.example.expensestracker.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Locale

class TransactionAdapter(
    private var transactions: List<Transaction>
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    inner class TransactionViewHolder(val binding: ItemTransactionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(transaction: Transaction) {
            binding.transactionDescription.text = transaction.description
            binding.transactionCategory.text = transaction.category


            val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
            binding.transactionDate.text = dateFormat.format(transaction.date.toDate())


            val sign = if (transaction.type == TransactionType.INCOME) "+" else "-"
            binding.transactionAmount.text = "$sign$${String.format("%.2f", transaction.amount)}"


            val color = if (transaction.type == TransactionType.INCOME) {
                ContextCompat.getColor(binding.root.context, android.R.color.holo_green_dark)
            } else {
                ContextCompat.getColor(binding.root.context, android.R.color.holo_red_dark)
            }
            binding.transactionAmount.setTextColor(color)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TransactionViewHolder(binding)
    }

    override fun getItemCount(): Int = transactions.size

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(transactions[position])
    }

    fun updateList(newTransactions: List<Transaction>) {
        transactions = newTransactions
        notifyDataSetChanged()
    }
}