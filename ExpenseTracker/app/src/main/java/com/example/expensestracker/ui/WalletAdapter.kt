package com.example.expensestracker.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.expensestracker.databinding.ItemWalletBinding
import com.example.expensestracker.model.Wallet
import com.example.expensestracker.network.AuthManager
import com.example.expensestracker.R

class WalletAdapter(
    private val onWalletClick: (Wallet) -> Unit,
    private val onShareClick: (Wallet) -> Unit,
    private val onEditWallet: (Wallet) -> Unit,
    private val onDeleteWallet: (Wallet) -> Unit
) : ListAdapter<WalletWithCounts, WalletAdapter.WalletViewHolder>(WalletDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WalletViewHolder {
        val binding = ItemWalletBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return WalletViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WalletViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class WalletViewHolder(
        private val binding: ItemWalletBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(walletWithCounts: WalletWithCounts) {
            val wallet = walletWithCounts.wallet
            binding.walletNameText.text = wallet.name



            binding.walletPendingCount.text = "Pendientes por pagar: ${walletWithCounts.pendingCount}"
            binding.walletPaidCount.text = "Pagados: ${walletWithCounts.paidCount}"

            val currentUser = AuthManager.getCurrentUser()
            binding.shareButton.visibility = if (wallet.ownerId == currentUser?.uid) {
                View.VISIBLE
            } else {
                View.GONE
            }

            binding.shareButton.setOnClickListener { onShareClick(wallet) }
            binding.root.setOnClickListener { onWalletClick(wallet) }


            binding.walletMenuButton.setOnClickListener { view ->
                val popup = PopupMenu(view.context, view)
                popup.menuInflater.inflate(R.menu.menu_wallet, popup.menu)
                popup.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.action_edit_wallet -> {
                            onEditWallet(wallet)
                            true
                        }
                        R.id.action_delete_wallet -> {
                            onDeleteWallet(wallet)
                            true
                        }
                        else -> false
                    }
                }
                popup.show()
            }
        }
    }

    private class WalletDiffCallback : DiffUtil.ItemCallback<WalletWithCounts>() {
        override fun areItemsTheSame(oldItem: WalletWithCounts, newItem: WalletWithCounts): Boolean {
            return oldItem.wallet.id == newItem.wallet.id
        }

        override fun areContentsTheSame(oldItem: WalletWithCounts, newItem: WalletWithCounts): Boolean {
            return oldItem == newItem
        }
    }
}