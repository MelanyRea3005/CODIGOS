package com.example.expensestracker.ui

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.RadioButton
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
import kotlinx.coroutines.launch

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
            onShareClick = { wallet -> showShareDialog(wallet) }
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
                    walletAdapter.submitList(wallets)
                }
            } catch (e: Exception) {
                Toast.makeText(this@WalletActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
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