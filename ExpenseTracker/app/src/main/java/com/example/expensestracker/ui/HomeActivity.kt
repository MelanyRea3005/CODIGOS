package com.example.expensestracker.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.expensestracker.databinding.ActivityHomeBinding
import com.example.expensestracker.model.Expense
import com.example.expensestracker.model.ScheduledExpense
import com.example.expensestracker.model.Wallet
import com.example.expensestracker.repository.ScheduledExpenseRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var expenseAdapter: ExpenseAdapter
    private lateinit var scheduledExpenseAdapter: ScheduledExpenseAdapter
    private lateinit var scheduledExpenseRepository: ScheduledExpenseRepository

    private var billeteras: List<Wallet> = emptyList()
    private var billeteraActivaId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        scheduledExpenseRepository = ScheduledExpenseRepository()

        expenseAdapter = ExpenseAdapter(emptyList()) { expense ->
            val intent = Intent(this, EditExpenseActivity::class.java)
            intent.putExtra("expenseId", expense.id)
            startActivity(intent)
        }
        binding.expensesRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.expensesRecyclerView.adapter = expenseAdapter

        scheduledExpenseAdapter = ScheduledExpenseAdapter(
            emptyList(),
            onMarkAsPaid = { expense -> mostrarDialogoPago(expense) },
            onEdit = { _ ->
                Toast.makeText(this, "Solo puedes editar desde Wallet Details.", Toast.LENGTH_SHORT).show()
            },
            onDelete = { _ ->
                Toast.makeText(this, "Solo puedes eliminar desde Wallet Details.", Toast.LENGTH_SHORT).show()
            }
        )
        binding.scheduledExpensesRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.scheduledExpensesRecyclerView.adapter = scheduledExpenseAdapter

        setupListeners()
        cargarBilleteras() // Carga las billeteras y configura el spinner
        cargarDatosDeFirestore()
    }

    private fun setupListeners() {
        binding.addExpenseButton.setOnClickListener {
            startActivity(Intent(this, AddExpenseActivity::class.java))
        }
        binding.walletButton.setOnClickListener {
            startActivity(Intent(this, WalletActivity::class.java))
        }
    }

    private fun cargarBilleteras() {
        val db = FirebaseFirestore.getInstance()
        db.collection("wallets")
            .get()
            .addOnSuccessListener { result ->
                billeteras = result.documents.mapNotNull { doc ->
                    val wallet = doc.toObject(Wallet::class.java)
                    wallet?.copy(id = doc.id)
                }
                val nombres = billeteras.map { it.name }
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, nombres)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerBilletera.adapter = adapter


                if (billeteras.isNotEmpty()) {
                    billeteraActivaId = billeteras[0].id
                    cargarGastosProgramadosDeFirestore()
                }

                binding.spinnerBilletera.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: android.widget.AdapterView<*>,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        billeteraActivaId = billeteras[position].id
                        cargarGastosProgramadosDeFirestore()
                    }

                    override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error cargando billeteras", Toast.LENGTH_SHORT).show()
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
            .addOnFailureListener { e ->
                binding.ingresosTextView.text = "Error"
                binding.gastosTextView.text = ""
                binding.balanceTextView.text = ""
                expenseAdapter.updateList(emptyList())
                Toast.makeText(this, "Error cargando gastos: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun cargarGastosProgramadosDeFirestore() {
        if (billeteraActivaId.isEmpty()) {
            scheduledExpenseAdapter.updateList(emptyList())
            return
        }
        val mesActual = SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(Date())
        scheduledExpenseRepository.getScheduledExpenses(
            billeteraId = billeteraActivaId,
            mes = mesActual,
            onResult = { expenses ->
                scheduledExpenseAdapter.updateList(expenses)
            },
            onError = {
                scheduledExpenseAdapter.updateList(emptyList())
                Toast.makeText(this, "Error cargando gastos programados", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun setupPieChartIngresosVsGastos(ingresos: Double, gastos: Double) {
        val entries = mutableListOf<com.github.mikephil.charting.data.PieEntry>()
        if (ingresos > 0) entries.add(com.github.mikephil.charting.data.PieEntry(ingresos.toFloat(), "Ingresos"))
        if (gastos > 0) entries.add(com.github.mikephil.charting.data.PieEntry(gastos.toFloat(), "Gastos"))

        val dataSet = com.github.mikephil.charting.data.PieDataSet(entries, "").apply {
            colors = listOf(
                Color.parseColor("#1565C0"),
                Color.parseColor("#90A4AE")
            )
            valueTextSize = 16f
            valueTextColor = Color.WHITE
        }

        binding.pieChart.data = com.github.mikephil.charting.data.PieData(dataSet)
        binding.pieChart.description.isEnabled = false
        binding.pieChart.setHoleColor(Color.TRANSPARENT)
        binding.pieChart.invalidate()
    }

    private fun mostrarDialogoPago(expense: ScheduledExpense) {
        val dialog = android.app.AlertDialog.Builder(this)
        dialog.setTitle("Registrar pago")
        val input = android.widget.EditText(this)
        input.hint = "Monto real pagado"
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        dialog.setView(input)
        dialog.setPositiveButton("Confirmar") { _, _ ->
            val montoReal = input.text.toString().toDoubleOrNull()
            if (montoReal != null) {
                scheduledExpenseRepository.marcarComoPagado(
                    id = expense.id,
                    montoReal = montoReal,
                    fechaPago = System.currentTimeMillis()
                ) { exito ->
                    if (exito) {
                        cargarGastosProgramadosDeFirestore()
                        Toast.makeText(this, "¡Pago registrado!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Error registrando pago", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Ingresa un monto válido", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.setNegativeButton("Cancelar", null)
        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        cargarDatosDeFirestore()
        cargarGastosProgramadosDeFirestore()
    }
}