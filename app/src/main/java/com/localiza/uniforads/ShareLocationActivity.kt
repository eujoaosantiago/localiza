package com.localiza.uniforads

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.localiza.uniforads.databinding.ActivityShareLocationBinding
import com.localiza.uniforads.model.Address

class ShareLocationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityShareLocationBinding
    private lateinit var address: Address

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityShareLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val receivedAddress = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("address", Address::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("address") as? Address
        }

        if (receivedAddress == null) {
            finish()
            return
        }
        address = receivedAddress

        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        val numberDisplay = if (address.number.isNullOrBlank()) "S/N" else address.number
        binding.tvAddressText.text = "${address.street}, $numberDisplay\n${address.city}- ${address.state}\nBrasil"
    }

    private fun setupListeners() {
        val numberDisplay = if (address.number.isNullOrBlank()) "S/N" else address.number
        val fullAddress = "${address.street}, $numberDisplay, ${address.city}, ${address.state}"

        binding.btnMaps.setOnClickListener {
            openMaps("geo:0,0?q=", fullAddress)
        }

        binding.btnRoute.setOnClickListener {
            openMaps("google.navigation:q=", fullAddress)
        }

        binding.btnShare.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Minha Localização")
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Confira este local: $fullAddress")
            startActivity(Intent.createChooser(shareIntent, "Compartilhar via"))
        }

        binding.btnRegisterPlace.setOnClickListener {
            finish() // Voltar para a tela de detalhes que já tem o botão de cadastrar local
        }
    }

    private fun openMaps(queryPrefix: String, addressStr: String) {
        val gmmIntentUri = Uri.parse("$queryPrefix${Uri.encode(addressStr)}")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")
        if (mapIntent.resolveActivity(packageManager) != null) {
            startActivity(mapIntent)
        } else {
            // Caso não tenha o app do maps, abre no browser
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(addressStr)}"))
            startActivity(webIntent)
        }
    }
}