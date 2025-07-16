package com.example.expensestracker.ui

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.expensestracker.databinding.ActivityWalletDetailsBinding
import com.example.expensestracker.model.ScheduledExpense
import com.example.expensestracker.repository.ScheduledExpenseRepository
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import java.text.SimpleDateFormat
import java.util.*

class WalletDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWalletDetailsBinding
    private lateinit var adapterPendientesPagados: ScheduledExpenseAdapter
    private lateinit var adapterVencidos: ScheduledExpenseAdapter
    private val scheduledExpenseRepository = ScheduledExpenseRepository()
    private var mesSeleccionado: String = SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(Date())
    private var billeteraId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWalletDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        billeteraId = intent.getStringExtra("walletId")
        if (billeteraId.isNullOrBlank()) {
            Toast.makeText(this, "Billetera no especificada", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        adapterPendientesPagados = ScheduledExpenseAdapter(
            emptyList(),
            onMarkAsPaid = { expense -> mostrarDialogoPago(expense) },
            onEdit = { expense -> mostrarDialogoEditarNombre(expense) },
            onDelete = { expense -> mostrarDialogoEliminarGasto(expense) }
        )
        adapterVencidos = ScheduledExpenseAdapter(
            emptyList(),
            onMarkAsPaid = { expense -> mostrarDialogoPago(expense) },
            onEdit = { expense -> mostrarDialogoEditarNombre(expense) },
            onDelete = { expense -> mostrarDialogoEliminarGasto(expense) }
        )

        binding.recyclerPendientesPagados.layoutManager = LinearLayoutManager(this)
        binding.recyclerPendientesPagados.adapter = adapterPendientesPagados
        binding.recyclerVencidos.layoutManager = LinearLayoutManager(this)
        binding.recyclerVencidos.adapter = adapterVencidos

        binding.addTransactionFab.setOnClickListener {
            mostrarDialogoAgregarGastoProgramado()
        }

        binding.tvMesActual.text = mesSeleccionado

        binding.btnMesAnterior.setOnClickListener {
            val sdf = SimpleDateFormat("MM-yyyy", Locale.getDefault())
            val cal = Calendar.getInstance()
            cal.time = sdf.parse(mesSeleccionado)!!
            cal.add(Calendar.MONTH, -1)
            mesSeleccionado = sdf.format(cal.time)
            binding.tvMesActual.text = mesSeleccionado
            cargarGastosProgramadosDeFirestore()
        }

        binding.btnMesSiguiente.setOnClickListener {
            val sdf = SimpleDateFormat("MM-yyyy", Locale.getDefault())
            val cal = Calendar.getInstance()
            cal.time = sdf.parse(mesSeleccionado)!!
            cal.add(Calendar.MONTH, 1)
            mesSeleccionado = sdf.format(cal.time)
            binding.tvMesActual.text = mesSeleccionado
            cargarGastosProgramadosDeFirestore()
        }

        cargarGastosProgramadosDeFirestore()
    }

    private fun cargarGastosProgramadosDeFirestore() {
        val mesActual = mesSeleccionado
        scheduledExpenseRepository.getScheduledExpenses(
            billeteraId = billeteraId!!,
            mes = mesActual,
            onResult = { expenses ->
                val now = System.currentTimeMillis()
                val pendientesPagados = expenses.filter {
                    val pagado = it.fechaPago != null
                    val vencido = !pagado && it.fechaProgramada < now
                    !vencido
                }.sortedBy { it.fechaProgramada }

                val vencidos = expenses.filter {
                    val pagado = it.fechaPago != null
                    val vencido = !pagado && it.fechaProgramada < now
                    vencido
                }.sortedBy { it.fechaProgramada }

                adapterPendientesPagados.updateList(pendientesPagados)
                adapterVencidos.updateList(vencidos)

                binding.tituloVencidos.visibility = if (vencidos.isNotEmpty()) View.VISIBLE else View.GONE
                binding.recyclerVencidos.visibility = if (vencidos.isNotEmpty()) View.VISIBLE else View.GONE

                mostrarPieChartYPorPagar(expenses)
            },
            onError = { e ->
                adapterPendientesPagados.updateList(emptyList())
                adapterVencidos.updateList(emptyList())
                binding.tituloVencidos.visibility = View.GONE
                binding.recyclerVencidos.visibility = View.GONE
                mostrarPieChartYPorPagar(emptyList())
                Toast.makeText(this, "Error cargando gastos programados: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun mostrarPieChartYPorPagar(expenses: List<ScheduledExpense>) {
        val now = System.currentTimeMillis()
        val montoPagado = expenses.filter { it.fechaPago != null }.sumOf { it.montoReal ?: it.montoEstimado }
        val montoPendiente = expenses.filter { it.fechaPago == null && it.fechaProgramada >= now }.sumOf { it.montoEstimado }
        val montoVencido = expenses.filter { it.fechaPago == null && it.fechaProgramada < now }.sumOf { it.montoEstimado }
        val montoPorPagar = montoPendiente + montoVencido

        setupPieChartGastosProgramados(montoPagado, montoPendiente, montoVencido)
        binding.porPagarTextView.text = "Por pagar: $%.2f".format(montoPorPagar)
    }

    private fun setupPieChartGastosProgramados(montoPagado: Double, montoPendiente: Double, montoVencido: Double) {
        val entries = mutableListOf<PieEntry>()
        if (montoPagado > 0) entries.add(PieEntry(montoPagado.toFloat(), "Pagado"))
        if (montoPendiente > 0) entries.add(PieEntry(montoPendiente.toFloat(), "Pendiente"))
        if (montoVencido > 0) entries.add(PieEntry(montoVencido.toFloat(), "Vencido"))

        val dataSet = PieDataSet(entries, "").apply {
            colors = listOf(
                Color.parseColor("#1565C0"),
                Color.parseColor("#FFD600"),
                Color.parseColor("#E57373")
            )
            valueTextSize = 14f
            valueTextColor = Color.WHITE
        }

        binding.pieChartGastosProgramados.data = PieData(dataSet)
        binding.pieChartGastosProgramados.description.isEnabled = false
        binding.pieChartGastosProgramados.setHoleColor(Color.TRANSPARENT)
        binding.pieChartGastosProgramados.invalidate()
    }

    private fun mostrarDialogoPago(expense: ScheduledExpense) {
        val input = EditText(this)
        input.hint = "Monto real pagado"
        input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL

        AlertDialog.Builder(this)
            .setTitle("Registrar pago")
            .setView(input)
            .setPositiveButton("Confirmar") { _, _ ->
                val montoReal = input.text.toString().toDoubleOrNull()
                if (montoReal != null && montoReal > 0) {
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("scheduled_expenses")
                        .document(expense.id)
                        .update(
                            mapOf(
                                "montoReal" to montoReal,
                                "fechaPago" to System.currentTimeMillis()
                            )
                        ).addOnSuccessListener {
                            cargarGastosProgramadosDeFirestore()
                            Toast.makeText(this, "¡Pago registrado!", Toast.LENGTH_SHORT).show()
                        }.addOnFailureListener { e ->
                            Toast.makeText(this, "Error registrando pago: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "Ingresa un monto válido mayor a 0", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoEditarNombre(expense: ScheduledExpense) {
        val input = EditText(this)
        input.hint = "Nuevo nombre/categoría"
        input.setText(expense.categoria)

        AlertDialog.Builder(this)
            .setTitle("Editar nombre")
            .setView(input)
            .setPositiveButton("Guardar") { _, _ ->
                val nuevoNombre = input.text.toString().trim()
                if (nuevoNombre.isNotEmpty()) {
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("scheduled_expenses")
                        .document(expense.id)
                        .update("categoria", nuevoNombre)
                        .addOnSuccessListener {
                            cargarGastosProgramadosDeFirestore()
                            Toast.makeText(this, "Nombre actualizado", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoEliminarGasto(expense: ScheduledExpense) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar gasto")
            .setMessage("¿Seguro que quieres eliminar este gasto programado?")
            .setPositiveButton("Eliminar") { _, _ ->
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("scheduled_expenses")
                    .document(expense.id)
                    .delete()
                    .addOnSuccessListener {
                        cargarGastosProgramadosDeFirestore()
                        Toast.makeText(this, "Gasto eliminado", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoAgregarGastoProgramado() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val inputCategoria = EditText(this).apply {
            hint = "Categoría"
        }
        val inputMonto = EditText(this).apply {
            hint = "Monto estimado"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val inputFecha = EditText(this).apply {
            hint = "Fecha (dd/MM/yyyy)"
            inputType = InputType.TYPE_CLASS_DATETIME
        }

        layout.addView(inputCategoria)
        layout.addView(inputMonto)
        layout.addView(inputFecha)

        AlertDialog.Builder(this)
            .setTitle("Nuevo Gasto Programado")
            .setView(layout)
            .setPositiveButton("Agregar") { _, _ ->
                val categoria = inputCategoria.text.toString().trim()
                val monto = inputMonto.text.toString().toDoubleOrNull()
                val fechaStr = inputFecha.text.toString().trim()

                if (categoria.isEmpty() || monto == null || fechaStr.isEmpty()) {
                    Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val fechaProgramada = try {
                    sdf.parse(fechaStr)?.time ?: 0L
                } catch (e: Exception) {
                    Toast.makeText(this, "Formato de fecha inválido", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val mesActual = SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(Date(fechaProgramada))

                val expenseMap = mapOf(
                    "billeteraId" to billeteraId!!,
                    "categoria" to categoria,
                    "montoEstimado" to monto,
                    "fechaProgramada" to fechaProgramada,
                    "mes" to mesActual,
                    "descripcion" to ""
                )

                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("scheduled_expenses")
                    .add(expenseMap)
                    .addOnSuccessListener {
                        cargarGastosProgramadosDeFirestore()
                        Toast.makeText(this, "¡Gasto programado agregado!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}