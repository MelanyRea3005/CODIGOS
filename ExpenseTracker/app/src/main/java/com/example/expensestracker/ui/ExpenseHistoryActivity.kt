package com.example.expensestracker.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.expensestracker.databinding.ActivityExpenseHistoryBinding

class ExpenseHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExpenseHistoryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExpenseHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)


    }
}
