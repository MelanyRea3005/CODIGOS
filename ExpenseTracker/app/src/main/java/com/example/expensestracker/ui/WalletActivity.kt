package com.example.expensestracker.ui

import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.expensestracker.databinding.ActivityWalletBinding
import com.example.expensestracker.model.SharePermission
import com.example.expensestracker.model.Wallet
import com.example.expensestracker.model.WalletManager
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

        setupViews()
        observeWallets()
    }

    private fun setupViews() {
        walletManager = WalletManager()
        walletAdapter = WalletAdapter(
            onWalletClick = { wallet -> showWalletDetails(wallet) },
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
        val input = EditText(this).apply {
            hint = "Email del usuario"
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Compartir Billetera")
            .setView(input)
            .setPositiveButton("Compartir") { _, _ ->
                val email = input.text.toString()
                if (email.isNotBlank()) {
                    lifecycleScope.launch {
                        try {
                            walletManager.shareWallet(wallet.id, email, SharePermission.READ_WRITE)
                            Toast.makeText(this@WalletActivity, "Billetera compartida", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(this@WalletActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showWalletDetails(wallet: Wallet) {
        Toast.makeText(this, "Balance: $${wallet.balance}", Toast.LENGTH_SHORT).show()
    }
}