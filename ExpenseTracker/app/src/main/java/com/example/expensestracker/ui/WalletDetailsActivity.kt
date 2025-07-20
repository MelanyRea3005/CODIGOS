package com.example.expensestracker.ui

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.expensestracker.databinding.ActivityWalletDetailsBinding
import com.example.expensestracker.model.ScheduledExpense
import com.example.expensestracker.repository.ScheduledExpenseRepository
import com.example.expensestracker.network.AuthManager
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import java.text.SimpleDateFormat
import java.util.*
import com.example.expensestracker.R

class WalletDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWalletDetailsBinding
    private lateinit var adapterPagados: ScheduledExpenseAdapter
    private lateinit var adapterGenerales: ScheduledExpenseAdapter

    private val scheduledExpenseRepository = ScheduledExpenseRepository()
    private var mesSeleccionado: String = SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(Date())
    private var billeteraId: String? = null

    private var mostrarPendientesGenerales = true
    private var listaGenerales: List<ScheduledExpense> = emptyList()

    private var walletPermission: String = "READ_WRITE" // Se recibe por intent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWalletDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        billeteraId = intent.getStringExtra("walletId")
        walletPermission = intent.getStringExtra("walletPermission") ?: "READ_WRITE"

        if (billeteraId.isNullOrBlank()) {
            Toast.makeText(this, "Billetera no especificada", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // PASA PERMISO AL ADAPTER
        adapterPagados = ScheduledExpenseAdapter(
            emptyList(),
            onMarkAsPaid = { expense -> mostrarDialogoPago(expense) },
            onEdit = { expense -> mostrarDialogoEditarNombre(expense) },
            onDelete = { expense -> mostrarDialogoEliminarGasto(expense) },
            isReadOnly = walletPermission == "READ_ONLY"
        )
        adapterGenerales = ScheduledExpenseAdapter(
            emptyList(),
            onMarkAsPaid = { expense -> mostrarDialogoPago(expense) },
            onEdit = { expense -> mostrarDialogoEditarNombre(expense) },
            onDelete = { expense -> mostrarDialogoEliminarGasto(expense) },
            isReadOnly = walletPermission == "READ_ONLY"
        )

        binding.recyclerPagados.layoutManager = LinearLayoutManager(this)
        binding.recyclerPagados.adapter = adapterPagados

        binding.recyclerGenerales.layoutManager = LinearLayoutManager(this)
        binding.recyclerGenerales.adapter = adapterGenerales

        binding.addTransactionFab.setOnClickListener { mostrarDialogoAgregarGastoProgramado() }

        binding.tvMesActual.text = mesSeleccionado

        binding.btnMesAnterior.setOnClickListener {
            val sdf = SimpleDateFormat("MM-yyyy", Locale.getDefault())
            val cal = Calendar.getInstance()
            cal.time = sdf.parse(mesSeleccionado)!!
            cal.add(Calendar.MONTH, -1)
            mesSeleccionado = sdf.format(cal.time)
            binding.tvMesActual.text = mesSeleccionado
            cargarGastosPagadosDelMes()
        }

        binding.btnMesSiguiente.setOnClickListener {
            val sdf = SimpleDateFormat("MM-yyyy", Locale.getDefault())
            val cal = Calendar.getInstance()
            cal.time = sdf.parse(mesSeleccionado)!!
            cal.add(Calendar.MONTH, 1)
            mesSeleccionado = sdf.format(cal.time)
            binding.tvMesActual.text = mesSeleccionado
            cargarGastosPagadosDelMes()
        }

        binding.tvMesActual.setOnClickListener {
            val sdf = SimpleDateFormat("MM-yyyy", Locale.getDefault())
            val calendar = Calendar.getInstance()
            val fechaActual = sdf.parse(mesSeleccionado)
            calendar.time = fechaActual!!

            val datePicker = DatePickerDialog(
                this,
                { _, year, month, _ ->
                    mesSeleccionado = String.format("%02d-%04d", month + 1, year)
                    binding.tvMesActual.text = mesSeleccionado
                    cargarGastosPagadosDelMes()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            try {
                val daySpinner = datePicker.datePicker.findViewById<View>(
                    resources.getIdentifier("day", "id", "android")
                )
                if (daySpinner != null) {
                    daySpinner.visibility = View.GONE
                }
            } catch (_: Exception) { }
            datePicker.show()
        }

        cargarGastosPagadosDelMes()
        cargarGastosGenerales()

        // --- GASTOS GENERALES BOTONES Y FILTRO ---
        binding.btnPendientesGenerales.setOnClickListener {
            mostrarPendientesGenerales = true
            actualizarListaGenerales()
            setBotonGeneralesActivo(true)
        }
        binding.btnVencidosGenerales.setOnClickListener {
            mostrarPendientesGenerales = false
            actualizarListaGenerales()
            setBotonGeneralesActivo(false)
        }
        setBotonGeneralesActivo(true)

        // Deshabilitar FAB si solo lectura
        if (walletPermission == "READ_ONLY") {
            binding.addTransactionFab.isEnabled = false
            binding.addTransactionFab.alpha = 0.5f
            binding.addTransactionFab.setOnClickListener(null)
        }
    }

    private fun cargarGastosPagadosDelMes() {
        scheduledExpenseRepository.getScheduledExpenses(
            billeteraId = billeteraId!!,
            mes = mesSeleccionado,
            onResult = { expenses ->
                val pagados = expenses.filter { it.fechaPago != null }
                adapterPagados.updateList(pagados)
                mostrarPieChartYPorPagar(expenses)
            },
            onError = { e ->
                adapterPagados.updateList(emptyList())
                mostrarPieChartYPorPagar(emptyList())
                Toast.makeText(this, "Error cargando gastos pagados del mes: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun cargarGastosGenerales() {
        scheduledExpenseRepository.getAllScheduledExpenses(
            billeteraId = billeteraId!!,
            onResult = { expenses ->
                listaGenerales = expenses.sortedBy { it.fechaProgramada }
                actualizarListaGenerales()
            },
            onError = { e ->
                listaGenerales = emptyList()
                actualizarListaGenerales()
            }
        )
    }

    private fun actualizarListaGenerales() {
        val now = System.currentTimeMillis()
        val list = if (mostrarPendientesGenerales) {
            listaGenerales.filter { it.fechaPago == null && it.fechaProgramada >= now }
        } else {
            listaGenerales.filter { it.fechaPago == null && it.fechaProgramada < now }
        }
        adapterGenerales.updateList(list)
    }

    private fun setBotonGeneralesActivo(pendientesActivo: Boolean) {
        if (pendientesActivo) {
            binding.btnPendientesGenerales.setBackgroundColor(Color.parseColor("#1976D2"))
            binding.btnPendientesGenerales.setTextColor(Color.WHITE)
            binding.btnPendientesGenerales.setTypeface(null, android.graphics.Typeface.BOLD)

            binding.btnVencidosGenerales.setBackgroundColor(Color.WHITE)
            binding.btnVencidosGenerales.setTextColor(Color.parseColor("#1976D2"))
            binding.btnVencidosGenerales.setTypeface(null, android.graphics.Typeface.BOLD)
        } else {
            binding.btnPendientesGenerales.setBackgroundColor(Color.WHITE)
            binding.btnPendientesGenerales.setTextColor(Color.parseColor("#1976D2"))
            binding.btnPendientesGenerales.setTypeface(null, android.graphics.Typeface.BOLD)

            binding.btnVencidosGenerales.setBackgroundColor(Color.parseColor("#1976D2"))
            binding.btnVencidosGenerales.setTextColor(Color.WHITE)
            binding.btnVencidosGenerales.setTypeface(null, android.graphics.Typeface.BOLD)
        }
    }

    private fun mostrarDialogoPago(expense: ScheduledExpense) {
        if (walletPermission == "READ_ONLY") {
            Toast.makeText(this, "Solo puedes ver esta billetera", Toast.LENGTH_SHORT).show()
            return
        }
        val input = EditText(this)
        input.hint = "Monto real pagado"
        input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL

        AlertDialog.Builder(this)
            .setTitle("Registrar pago")
            .setView(input)
            .setPositiveButton("Confirmar") { _, _ ->
                val montoReal = input.text.toString().toDoubleOrNull()
                if (montoReal != null && montoReal > 0) {
                    scheduledExpenseRepository.marcarComoPagado(
                        expense.id,
                        montoReal,
                        System.currentTimeMillis()
                    ) { exito: Boolean ->
                        if (exito) {
                            cargarGastosPagadosDelMes()
                            cargarGastosGenerales()
                            Toast.makeText(this, "¡Pago registrado!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Error registrando pago", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Ingresa un monto válido mayor a 0", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoEditarNombre(expense: ScheduledExpense) {
        if (walletPermission == "READ_ONLY") {
            Toast.makeText(this, "Solo puedes ver esta billetera", Toast.LENGTH_SHORT).show()
            return
        }
        val input = EditText(this)
        input.hint = "Nuevo nombre/categoría"
        input.setText(expense.categoria)

        AlertDialog.Builder(this)
            .setTitle("Editar nombre")
            .setView(input)
            .setPositiveButton("Guardar") { _, _ ->
                val nuevoNombre = input.text.toString().trim()
                if (nuevoNombre.isNotEmpty()) {
                    scheduledExpenseRepository.editarCategoria(
                        expense.id,
                        nuevoNombre
                    ) { exito: Boolean ->
                        if (exito) {
                            cargarGastosPagadosDelMes()
                            cargarGastosGenerales()
                            Toast.makeText(this, "Nombre actualizado", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Error al actualizar", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoEliminarGasto(expense: ScheduledExpense) {
        if (walletPermission == "READ_ONLY") {
            Toast.makeText(this, "Solo puedes ver esta billetera", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Eliminar gasto")
            .setMessage("¿Seguro que quieres eliminar este gasto programado?")
            .setPositiveButton("Eliminar") { _, _ ->
                scheduledExpenseRepository.eliminarGasto(
                    expense.id
                ) { exito: Boolean ->
                    if (exito) {
                        cargarGastosPagadosDelMes()
                        cargarGastosGenerales()
                        Toast.makeText(this, "Gasto eliminado", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Error al eliminar", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    private fun mostrarDialogoAgregarGastoProgramado() {
        if (walletPermission == "READ_ONLY") {
            Toast.makeText(this, "Solo puedes ver esta billetera", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_add_scheduled_expense, null)

        val inputCategoria = dialogView.findViewById<EditText>(R.id.inputCategoria)
        val inputMonto = dialogView.findViewById<EditText>(R.id.inputMonto)
        val inputFecha = dialogView.findViewById<EditText>(R.id.inputFecha)
        val btnAgregar = dialogView.findViewById<Button>(R.id.btnAgregar)
        val btnCancelar = dialogView.findViewById<Button>(R.id.btnCancelar)

        var fechaSeleccionada: Long? = null

        inputFecha.setOnClickListener {
            val calendario = Calendar.getInstance()
            val datePicker = DatePickerDialog(
                this, { _, year, month, dayOfMonth ->
                    val fechaStr = "%02d/%02d/%04d".format(dayOfMonth, month + 1, year)
                    inputFecha.setText(fechaStr)
                    val calendar = Calendar.getInstance()
                    calendar.set(year, month, dayOfMonth, 0, 0, 0)
                    fechaSeleccionada = calendar.timeInMillis
                },
                calendario.get(Calendar.YEAR),
                calendario.get(Calendar.MONTH),
                calendario.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.show()
        }

        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        btnCancelar.setOnClickListener {
            alertDialog.dismiss()
        }

        btnAgregar.setOnClickListener {
            val categoria = inputCategoria.text.toString().trim()
            val monto = inputMonto.text.toString().toDoubleOrNull()
            if (categoria.isEmpty() || monto == null || fechaSeleccionada == null) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val mesActual = SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(Date(fechaSeleccionada!!))
            val userId = AuthManager.getCurrentUserUid() ?: "demoUser"
            val expense = ScheduledExpense(
                billeteraId = billeteraId!!,
                categoria = categoria,
                descripcion = "",
                fechaProgramada = fechaSeleccionada!!,
                mes = mesActual,
                montoEstimado = monto,
                estado = "pendiente",
                userId = userId
            )
            scheduledExpenseRepository.crearGastoProgramado(
                expense
            ) { exito: Boolean ->
                if (exito) {
                    cargarGastosPagadosDelMes()
                    cargarGastosGenerales()
                    Toast.makeText(this, "¡Gasto programado agregado!", Toast.LENGTH_SHORT).show()
                    alertDialog.dismiss()
                } else {
                    Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show()
                }
            }
        }

        alertDialog.show()
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
}