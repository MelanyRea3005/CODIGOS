package com.example.expensestracker.ui

import com.example.expensestracker.model.Wallet

data class WalletWithCounts(
    val wallet: Wallet,
    val pendingCount: Int = 0,
    val paidCount: Int = 0
)