package com.example.expensestracker.ui

import android.graphics.Color
import android.os.Bundle
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.expensestracker.R
import com.example.expensestracker.databinding.ActivityWalletDetailsBinding
import com.example.expensestracker.databinding.DialogShareWalletBinding
import com.example.expensestracker.model.SharePermission
import com.example.expensestracker.model.Transaction
import com.example.expensestracker.model.TransactionType
import com.example.expensestracker.model.Wallet
import com.example.expensestracker.model.WalletManager
import com.example.expensestracker.network.AuthManager
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.util.Date

class WalletDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWalletDetailsBinding
    private lateinit var walletManager: WalletManager
    private lateinit var transactionAdapter: TransactionAdapter
    private var walletId: String = ""
    private var currentWallet: Wallet? = null
    private var currentPermission: SharePermission = SharePermission.READ_WRITE // default, will update

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWalletDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        walletId = intent.getStringExtra("walletId") ?: ""
        if (walletId.isEmpty()) {
            finish()
            return
        }

        setupViews()
        loadWalletData()
    }

    private fun setupViews() {
        walletManager = WalletManager()
        transactionAdapter = TransactionAdapter(emptyList())

        binding.transactionsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@WalletDetailsActivity)
            adapter = transactionAdapter
        }

        binding.addTransactionFab.setOnClickListener {
            if (currentPermission == SharePermission.READ_WRITE) {
                showAddTransactionDialog()
            } else {
                Toast.makeText(this, "No tienes permiso para agregar transacciones", Toast.LENGTH_SHORT).show()
            }
        }

        binding.shareWalletButton.setOnClickListener {
            showShareWalletDialog()
        }
    }
    private fun loadWalletData() {
        walletManager = WalletManager()
        lifecycleScope.launch {
            try {
                currentWallet = walletManager.getWallet(walletId)
                currentWallet?.let { wallet ->
                    binding.walletNameText.text = wallet.name
                    binding.walletBalanceText.text = "Balance: $${String.format("%.2f", wallet.balance)}"

                    if (wallet.ownerId == AuthManager.getCurrentUserId()) {
                        currentPermission = SharePermission.READ_WRITE
                        binding.shareWalletButton.visibility = android.view.View.VISIBLE
                        binding.addTransactionFab.show()
                    } else {
                        val permission = walletManager.getUserPermission(walletId, AuthManager.getCurrentUserId())
                        currentPermission = permission ?: SharePermission.READ_ONLY
                        binding.shareWalletButton.visibility = android.view.View.GONE
                        if (currentPermission == SharePermission.READ_WRITE) {
                            binding.addTransactionFab.show()
                        } else {
                            binding.addTransactionFab.hide()
                        }
                    }
                }


                walletManager.observeTransactions(walletId) { transactions ->
                    transactionAdapter.updateList(transactions)
                    setupPieChart(transactions)
                }

            } catch (e: Exception) {
                Toast.makeText(this@WalletDetailsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupPieChart(transactions: List<Transaction>) {
        val entries = mutableListOf<PieEntry>()
        val categoryMap = mutableMapOf<String, Float>()
        transactions.forEach { tx ->
            val key = "${if (tx.type == TransactionType.INCOME) "Ingreso" else "Gasto"}: ${tx.category}"
            categoryMap[key] = categoryMap.getOrDefault(key, 0f) + tx.amount.toFloat()
        }
        for ((key, value) in categoryMap) {
            entries.add(PieEntry(value, key))
        }
        if (entries.isEmpty()) {
            entries.add(PieEntry(1f, "Sin datos"))
        }

        val dataSet = PieDataSet(entries, "Transacciones por Categoría")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList() + ColorTemplate.VORDIPLOM_COLORS.toList()
        val data = PieData(dataSet)
        data.setValueTextSize(12f)
        data.setValueTextColor(Color.BLACK)

        binding.pieChart.apply {
            this.data = data
            description.isEnabled = false
            legend.isEnabled = true
            setUsePercentValues(true)
            animateY(1000)
            invalidate()
        }
    }
    private fun showAddTransactionDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_transaction, null)

        val amountEditText = dialogView.findViewById<EditText>(R.id.editTextAmount)
        val descriptionEditText = dialogView.findViewById<EditText>(R.id.editTextDescription)
        val categoryEditText = dialogView.findViewById<EditText>(R.id.editTextCategory)
        val typeRadioGroup = dialogView.findViewById<RadioGroup>(R.id.radioGroupType)

        MaterialAlertDialogBuilder(this)
            .setTitle("Agregar Transacción")
            .setView(dialogView)
            .setPositiveButton("Agregar") { _, _ ->
                val amount = amountEditText.text.toString().toDoubleOrNull() ?: 0.0
                val description = descriptionEditText.text.toString()
                val category = categoryEditText.text.toString()
                val isIncome = typeRadioGroup.checkedRadioButtonId == R.id.radioButtonIncome

                if (amount > 0 && description.isNotEmpty()) {
                    val transaction = Transaction(
                        walletId = walletId,
                        type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE,
                        amount = amount,
                        description = description,
                        category = category.ifEmpty { "General" },
                        date = Timestamp(Date())
                    )

                    lifecycleScope.launch {
                        try {
                            walletManager.addTransaction(walletId, transaction)
                            Toast.makeText(this@WalletDetailsActivity, "Transacción agregada", Toast.LENGTH_SHORT).show()
                            loadWalletData() // Recargar datos
                        } catch (e: Exception) {
                            Toast.makeText(this@WalletDetailsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showShareWalletDialog() {
        val dialogBinding = DialogShareWalletBinding.inflate(layoutInflater)

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Compartir Billetera")
            .setView(dialogBinding.root)
            .setNegativeButton("Cancelar", null)
            .create()

        dialogBinding.shareButton.setOnClickListener {
            val email = dialogBinding.editTextEmail.text.toString().trim()
            val permission = when (dialogBinding.radioGroupPermission.checkedRadioButtonId) {
                R.id.radioRead -> SharePermission.READ_ONLY
                R.id.radioWrite -> SharePermission.READ_WRITE
                else -> null
            }

            if (email.isNotBlank() && permission != null) {
                lifecycleScope.launch {
                    try {
                        walletManager.shareWallet(walletId, email, permission)
                        Toast.makeText(this@WalletDetailsActivity, "Billetera compartida", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    } catch (e: Exception) {
                        Toast.makeText(this@WalletDetailsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }
}