package com.example.expensestracker.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.expensestracker.databinding.ActivityHomeBinding
import com.example.expensestracker.model.Expense
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var expenseAdapter: ExpenseAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        expenseAdapter = ExpenseAdapter(emptyList()) { expense ->
            val intent = Intent(this, EditExpenseActivity::class.java)
            intent.putExtra("expenseId", expense.id)
            startActivity(intent)
        }
        binding.expensesRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.expensesRecyclerView.adapter = expenseAdapter

        setupListeners()
        cargarDatosDeFirestore()
    }

    private fun setupListeners() {
        binding.addExpenseButton.setOnClickListener {
            startActivity(Intent(this, AddExpenseActivity::class.java))
        }
        binding.viewHistoryButton.setOnClickListener {
            startActivity(Intent(this, ExpenseHistoryActivity::class.java))
        }
        binding.filterButton.setOnClickListener {
            startActivity(Intent(this, FilterActivity::class.java))
        }
        // Nuevo botón para Billeteras
        binding.walletButton.setOnClickListener {
            startActivity(Intent(this, WalletActivity::class.java))
        }
    }

    private fun cargarDatosDeFirestore() {
        val db = FirebaseFirestore.getInstance()
        db.collection("expenses")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                var totalGastos = 0.0
                var totalIngresos = 0.0
                val expenses = result.mapNotNull { doc ->
                    val type = doc.getString("type") ?: ""
                    val amount = doc.getDouble("amount") ?: 0.0
                    if (type == "ingreso") {
                        totalIngresos += amount
                        null
                    } else if (type == "gasto") {
                        totalGastos += amount
                        Expense(
                            id = doc.id,
                            description = doc.getString("description") ?: "",
                            amount = amount,
                            date = doc.getString("date") ?: "",
                            category = doc.getString("category"),
                            userId = doc.getString("userId") ?: "",
                            type = type,
                            createdAt = doc.getTimestamp("createdAt")?.toDate()?.time
                        )
                    } else null
                }
                val balance = totalIngresos - totalGastos

                binding.ingresosTextView.text = "Ingresos: $%.2f USD".format(totalIngresos)
                binding.gastosTextView.text = "Gastos: $%.2f USD".format(totalGastos)
                binding.balanceTextView.text = "Balance: $%.2f USD".format(balance)

                setupPieChartIngresosVsGastos(totalIngresos, totalGastos)

                expenseAdapter.updateList(expenses)
            }
            .addOnFailureListener {
                binding.ingresosTextView.text = "Error"
                binding.gastosTextView.text = ""
                binding.balanceTextView.text = ""
                expenseAdapter.updateList(emptyList())
            }
    }

    private fun setupPieChartIngresosVsGastos(ingresos: Double, gastos: Double) {
        val entries = mutableListOf<PieEntry>()
        if (ingresos > 0) entries.add(PieEntry(ingresos.toFloat(), "Ingresos"))
        if (gastos > 0) entries.add(PieEntry(gastos.toFloat(), "Gastos"))

        val dataSet = PieDataSet(entries, "").apply {
            colors = listOf(
                Color.rgb(76, 175, 80),
                Color.rgb(244, 67, 54)
            )
            valueTextSize = 16f
            valueTextColor = Color.WHITE
        }

        binding.pieChart.data = PieData(dataSet)
        binding.pieChart.description.isEnabled = false
        binding.pieChart.setHoleColor(Color.TRANSPARENT)
        binding.pieChart.invalidate()
    }

    override fun onResume() {
        super.onResume()
        cargarDatosDeFirestore()
    }
}