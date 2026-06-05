package com.localiza.uniforads

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.localiza.uniforads.databinding.ActivityRegisterPlaceBinding
import com.localiza.uniforads.model.Place

class RegisterPlaceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterPlaceBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRegisterPlaceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnBack.setOnClickListener { finish() }

        binding.btnSavePlace.setOnClickListener {
            val name = binding.etPlaceName.text.toString()
            val type = binding.etPlaceType.text.toString()

            if (name.isNotEmpty() && type.isNotEmpty()) {
                val newPlace = Place(name, type)
                val resultIntent = Intent()
                resultIntent.putExtra("new_place", newPlace)
                setResult(RESULT_OK, resultIntent)
                finish()
            }
        }
    }
}