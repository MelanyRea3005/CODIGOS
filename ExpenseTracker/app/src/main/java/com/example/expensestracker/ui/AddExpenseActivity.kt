package com.example.expensestracker.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.expensestracker.databinding.ActivityAddExpenseBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import java.util.*

class AddExpenseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddExpenseBinding

    private val categorias = listOf("Alimentación", "Vestimenta", "Educación", "Vivienda", "Salud")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddExpenseBinding.inflate(layoutInflater)
        setContentView(binding.root)


        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categorias)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategoria.adapter = adapter


        binding.rgTipo.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == binding.radioGasto.id) {
                binding.spinnerCategoria.visibility = View.VISIBLE
            } else {
                binding.spinnerCategoria.visibility = View.GONE
            }
        }

        if (binding.radioIngreso.isChecked) {
            binding.spinnerCategoria.visibility = View.GONE
        } else {
            binding.spinnerCategoria.visibility = View.VISIBLE
        }


        binding.etFecha.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePicker = DatePickerDialog(this, { _, year, month, dayOfMonth ->
                val fechaStr = "%02d/%02d/%04d".format(dayOfMonth, month + 1, year)
                binding.etFecha.setText(fechaStr)
            },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH))
            datePicker.show()
        }

        binding.btnGuardar.setOnClickListener {
            guardarNuevoGasto()
        }
    }

    private fun guardarNuevoGasto() {
        val db = FirebaseFirestore.getInstance()
        val type = if (binding.radioGasto.isChecked) "gasto" else "ingreso"
        val categoriaSeleccionada = if (binding.radioGasto.isChecked) binding.spinnerCategoria.selectedItem.toString() else ""

        val data = hashMapOf(
            "description" to binding.etDescripcion.text.toString(),
            "amount" to (binding.etMonto.text.toString().toDoubleOrNull() ?: 0.0),
            "date" to binding.etFecha.text.toString(),
            "category" to categoriaSeleccionada,
            "type" to type,
            "createdAt" to Timestamp(Date()),
            "userId" to "demoUser"
        )

        db.collection("expenses")
            .add(data)
            .addOnSuccessListener {
                Toast.makeText(this, "Gasto/ingreso agregado", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show()
            }
    }
}