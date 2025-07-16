package com.example.expensestracker.ui

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.expensestracker.databinding.ActivityWalletBinding
import com.example.expensestracker.databinding.DialogShareWalletBinding
import com.example.expensestracker.model.SharePermission
import com.example.expensestracker.model.Wallet
import com.example.expensestracker.model.WalletManager
import com.example.expensestracker.network.AuthManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class WalletActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWalletBinding
    private lateinit var walletManager: WalletManager
    private lateinit var walletAdapter: WalletAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWalletBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (AuthManager.getCurrentUser() == null) {
            Toast.makeText(this, "Debes iniciar sesión", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupViews()
        observeWallets()
    }

    private fun setupViews() {
        walletManager = WalletManager()
        walletAdapter = WalletAdapter(
            onWalletClick = { wallet -> openWalletDetails(wallet) },
            onShareClick = { wallet -> showShareDialog(wallet) },
            onEditWallet = { wallet -> showEditWalletDialog(wallet) },
            onDeleteWallet = { wallet -> showDeleteWalletDialog(wallet) }
        )

        binding.walletsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@WalletActivity)
            adapter = walletAdapter
        }

        binding.addWalletFab.setOnClickListener {
            showCreateWalletDialog()
        }
    }

    private fun observeWallets() {
        lifecycleScope.launch {
            try {
                walletManager.getUserWallets().collect { wallets ->
                    val walletsWithCounts = cargarContadoresPorWallet(wallets)
                    walletAdapter.submitList(walletsWithCounts)
                }
            } catch (e: Exception) {
                Toast.makeText(this@WalletActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun cargarContadoresPorWallet(wallets: List<Wallet>): List<WalletWithCounts> = withContext(Dispatchers.IO) {
        val db = FirebaseFirestore.getInstance()
        wallets.map { wallet ->
            val expResult = db.collection("scheduled_expenses")
                .whereEqualTo("billeteraId", wallet.id)
                .get()
                .await()
            val pendientes = expResult.count { it.getString("estado") == "pendiente" }
            val pagados = expResult.count { it.getString("estado") == "pagado" }
            WalletWithCounts(wallet, pendientes, pagados)
        }
    }

    private fun showCreateWalletDialog() {
        val input = EditText(this).apply {
            hint = "Nombre de la billetera"
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Nueva Billetera")
            .setView(input)
            .setPositiveButton("Crear") { _, _ ->
                val name = input.text.toString()
                if (name.isNotBlank()) {
                    lifecycleScope.launch {
                        try {
                            walletManager.createWallet(name)
                            Toast.makeText(this@WalletActivity, "Billetera creada", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(this@WalletActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showEditWalletDialog(wallet: Wallet) {
        val input = EditText(this).apply {
            setText(wallet.name)
            hint = "Nuevo nombre de la billetera"
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Editar Billetera")
            .setView(input)
            .setPositiveButton("Guardar") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotBlank() && newName != wallet.name) {
                    lifecycleScope.launch {
                        try {
                            walletManager.updateWalletName(wallet.id, newName)
                            Toast.makeText(this@WalletActivity, "Nombre actualizado", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(this@WalletActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else if (newName == wallet.name) {
                    Toast.makeText(this, "El nombre es igual al actual", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showDeleteWalletDialog(wallet: Wallet) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Eliminar Billetera")
            .setMessage("¿Seguro que quieres eliminar la billetera \"${wallet.name}\"? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                lifecycleScope.launch {
                    try {
                        walletManager.deleteWallet(wallet.id)
                        Toast.makeText(this@WalletActivity, "Billetera eliminada", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@WalletActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showShareDialog(wallet: Wallet) {
        val dialogBinding = DialogShareWalletBinding.inflate(layoutInflater)

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Compartir Billetera")
            .setView(dialogBinding.root)
            .setNegativeButton("Cancelar", null)
            .create()

        dialogBinding.shareButton.setOnClickListener {
            val email = dialogBinding.editTextEmail.text.toString().trim()
            val permission = when {
                dialogBinding.radioWrite.isChecked -> SharePermission.READ_WRITE
                dialogBinding.radioRead.isChecked -> SharePermission.READ_ONLY
                else -> null
            }

            if (email.isNotBlank() && permission != null) {
                lifecycleScope.launch {
                    try {
                        walletManager.shareWallet(wallet.id, email, permission)
                        Toast.makeText(this@WalletActivity, "Billetera compartida", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    } catch (e: Exception) {
                        Toast.makeText(this@WalletActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun openWalletDetails(wallet: Wallet) {
        val intent = Intent(this, WalletDetailsActivity::class.java)
        intent.putExtra("walletId", wallet.id)
        startActivity(intent)
    }
}