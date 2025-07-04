package com.example.expensestracker.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.expensestracker.databinding.ActivityFilterBinding

class FilterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFilterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFilterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Filtros
    }
}
