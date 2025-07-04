package com.example.expensestracker.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.expensestracker.databinding.ActivityCategoryBreakdownBinding

class CategoryBreakdownActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCategoryBreakdownBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoryBreakdownBinding.inflate(layoutInflater)
        setContentView(binding.root)


        updateAmounts()
    }

    private fun updateAmounts() {

        binding.foodAmount.text = "$0.00"
        binding.housingAmount.text = "$0.00"
        binding.educationAmount.text = "$0.00"
        binding.healthAmount.text = "$0.00"
        binding.clothingAmount.text = "$0.00"
    }
}
