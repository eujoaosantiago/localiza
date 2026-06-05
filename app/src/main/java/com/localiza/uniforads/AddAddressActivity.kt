package com.localiza.uniforads

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.localiza.uniforads.databinding.ActivityAddAddressBinding
import com.localiza.uniforads.model.Address
import com.localiza.uniforads.network.CepResponse
import com.localiza.uniforads.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class AddAddressActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddAddressBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLatitude: Double? = null
    private var currentLongitude: Double? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            getCurrentLocation()
        } else {
            Toast.makeText(this, "Permissão de localização negada", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAddAddressBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnCurrentLocation.setOnClickListener {
            checkLocationPermissions()
        }

        binding.etCep.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s?.length == 8) {
                    buscarCep(s.toString())
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnRegister.setOnClickListener {
            val address = Address(
                cep = binding.etCep.text.toString(),
                street = binding.etStreet.text.toString(),
                number = binding.etNumber.text.toString(),
                neighborhood = binding.etNeighborhood.text.toString(),
                city = binding.etCity.text.toString(),
                state = binding.etState.text.toString(),
                latitude = currentLatitude,
                longitude = currentLongitude
            )

            if (validate(address)) {
                val intent = Intent()
                intent.putExtra("new_address", address)
                setResult(RESULT_OK, intent)
                finish()
            }
        }
    }

    private fun checkLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            getCurrentLocation()
        }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                currentLatitude = location.latitude
                currentLongitude = location.longitude
                
                val geocoder = Geocoder(this, Locale.getDefault())
                try {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    if (addresses != null && addresses.isNotEmpty()) {
                        val address = addresses[0]
                        binding.etCep.setText(address.postalCode?.replace("-", "") ?: "")
                        binding.etStreet.setText(address.thoroughfare ?: "")
                        binding.etNumber.setText(address.subThoroughfare ?: "")
                        binding.etNeighborhood.setText(address.subLocality ?: "")
                        binding.etCity.setText(address.locality ?: "")
                        binding.etState.setText(address.adminArea ?: "")
                        
                        Toast.makeText(this, "Endereço atual preenchido!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Erro ao obter endereço: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Não foi possível obter a localização. Tente ligar o GPS.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun buscarCep(cep: String) {
        RetrofitClient.instance.getCep(cep).enqueue(object : Callback<CepResponse> {
            override fun onResponse(call: Call<CepResponse>, response: Response<CepResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let {
                        binding.etStreet.setText(it.street)
                        binding.etNeighborhood.setText(it.neighborhood)
                        binding.etCity.setText(it.city)
                        binding.etState.setText(it.state)
                        
                        // Busca coordenadas pelo Geocoder usando o endereço retornado pelo CEP
                        buscarCoordenadasPorEndereco("${it.street}, ${it.city}, ${it.state}")
                    }
                } else {
                    Toast.makeText(this@AddAddressActivity, "CEP não encontrado", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<CepResponse>, t: Throwable) {
                Toast.makeText(this@AddAddressActivity, "Erro ao buscar CEP", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun buscarCoordenadasPorEndereco(enderecoCompleto: String) {
        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocationName(enderecoCompleto, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                currentLatitude = addresses[0].latitude
                currentLongitude = addresses[0].longitude
            }
        } catch (e: Exception) {
            Log.e("Geocoder", "Erro ao buscar coordenadas: ${e.message}")
        }
    }

    private fun validate(address: Address): Boolean {

    if (address.cep.isEmpty()) {
        binding.etCep.error = "CEP obrigatório"
        return false
    }

    if (address.street.isEmpty()) {
        binding.etStreet.error = "Rua obrigatória"
        return false
    }

    if (address.city.isEmpty()) {
        binding.etCity.error = "Cidade obrigatória"
        return false
    }

    return true
}
}
