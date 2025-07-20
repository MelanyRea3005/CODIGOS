package com.example.expensestracker.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.expensestracker.databinding.ActivityEditExpenseBinding
import com.example.expensestracker.network.AuthManager
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class EditExpenseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditExpenseBinding
    private var expenseId: String? = null

    private val categorias = listOf("Alimentación", "Vestimenta", "Educación", "Vivienda", "Salud")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditExpenseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categorias)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategoria.adapter = adapter

        expenseId = intent.getStringExtra("expenseId")
        if (expenseId.isNullOrEmpty()) {
            Toast.makeText(this, "No se encontró el ID del gasto", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        cargarDatosGasto()

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
            guardarCambios()
        }
    }

    private fun cargarDatosGasto() {
        val db = FirebaseFirestore.getInstance()
        db.collection("expenses").document(expenseId!!)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    Toast.makeText(this, "El gasto no existe", Toast.LENGTH_LONG).show()
                    finish()
                    return@addOnSuccessListener
                }
                binding.etDescripcion.setText(doc.getString("description") ?: "")
                binding.etMonto.setText((doc.getDouble("amount") ?: 0.0).toString())
                binding.etFecha.setText(doc.getString("date") ?: "")

                val categoriaDoc = doc.getString("category") ?: ""
                val index = categorias.indexOfFirst { it.equals(categoriaDoc, ignoreCase = true) }
                if (index >= 0) {
                    binding.spinnerCategoria.setSelection(index)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar el gasto", Toast.LENGTH_LONG).show()
                finish()
            }
    }

    private fun guardarCambios() {
        val db = FirebaseFirestore.getInstance()
        val userId = AuthManager.getCurrentUserUid() ?: "demoUser"

        val updates = mapOf(
            "description" to binding.etDescripcion.text.toString(),
            "amount" to (binding.etMonto.text.toString().toDoubleOrNull() ?: 0.0),
            "date" to binding.etFecha.text.toString(),
            "category" to binding.spinnerCategoria.selectedItem.toString(),
            "userId" to userId
        )
        db.collection("expenses").document(expenseId!!)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Gasto actualizado", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al actualizar", Toast.LENGTH_SHORT).show()
            }
    }
}