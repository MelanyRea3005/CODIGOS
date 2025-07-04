package com.example.expensestracker.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.expensestracker.databinding.ItemWalletBinding
import com.example.expensestracker.model.Wallet
import com.example.expensestracker.network.AuthManager

class WalletAdapter(
    private val onWalletClick: (Wallet) -> Unit,
    private val onShareClick: (Wallet) -> Unit
) : ListAdapter<Wallet, WalletAdapter.WalletViewHolder>(WalletDiffCallback()) {

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

        fun bind(wallet: Wallet) {
            binding.walletNameText.text = wallet.name
            binding.walletBalanceText.text = "$ ${wallet.balance}"

            val currentUser = AuthManager.getCurrentUser()
            binding.shareButton.visibility = if (wallet.ownerId == currentUser?.uid) {
                View.VISIBLE
            } else {
                View.GONE
            }

            binding.shareButton.setOnClickListener { onShareClick(wallet) }
            binding.root.setOnClickListener { onWalletClick(wallet) }
        }
    }

    private class WalletDiffCallback : DiffUtil.ItemCallback<Wallet>() {
        override fun areItemsTheSame(oldItem: Wallet, newItem: Wallet): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Wallet, newItem: Wallet): Boolean {
            return oldItem == newItem
        }
    }
}