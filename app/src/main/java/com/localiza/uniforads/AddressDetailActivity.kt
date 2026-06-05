package com.localiza.uniforads

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import coil.transform.CircleCropTransformation
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.localiza.uniforads.adapter.PlaceAdapter
import com.localiza.uniforads.databinding.ActivityAddressDetailBinding
import com.localiza.uniforads.model.Address
import com.localiza.uniforads.model.Place

class AddressDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddressDetailBinding
    private lateinit var address: Address
    private lateinit var placeAdapter: PlaceAdapter
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val registerPlaceLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val newPlace = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getSerializableExtra("new_place", Place::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getSerializableExtra("new_place") as? Place
            }

            newPlace?.let {
                savePlaceToFirestore(it)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAddressDetailBinding.inflate(layoutInflater)
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
        setupRecyclerView()
        setupListeners()
    }

    private fun setupUI() {
        binding.tvBairro.text = "${getString(R.string.bairro_label)} ${address.neighborhood}"
        
        // Exibe Rua + Número ou S/N
        val streetDisplay = if (address.number.isNullOrBlank()) {
            "${address.street}, S/N"
        } else {
            "${address.street}, ${address.number}"
        }
        binding.tvStreet.text = "${getString(R.string.rua_label)} $streetDisplay"

        binding.tvCityState.text = getString(R.string.cidade_estado_label, address.city, address.state)
        
        val user = auth.currentUser
        user?.let {
            val firstName = it.displayName?.split(" ")?.get(0) ?: "Usuário"
            binding.tvGreeting.text = getString(R.string.ola_nome_bem_vindo, firstName)
            
            binding.ivProfile.load(it.photoUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_launcher_foreground)
                transformations(CircleCropTransformation())
            }
        }
    }

    private fun setupRecyclerView() {
        placeAdapter = PlaceAdapter(address.places)
        binding.rvPlaces.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvPlaces.adapter = placeAdapter
    }

    private fun setupListeners() {
        binding.btnRegisterPlace.setOnClickListener {
            val intent = Intent(this, RegisterPlaceActivity::class.java)
            registerPlaceLauncher.launch(intent)
        }
        
        binding.ivIllustration.setOnClickListener {
            val intent = Intent(this, ShareLocationActivity::class.java)
            intent.putExtra("address", address)
            startActivity(intent)
        }

        binding.ivProfile.setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Sair")
            .setMessage("Deseja realmente sair da conta?")
            .setPositiveButton("Sim") { _, _ ->
                logout()
            }
            .setNegativeButton("Não", null)
            .show()
    }

    private fun logout() {
        auth.signOut()
        
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val googleSignInClient = GoogleSignIn.getClient(this, gso)
        googleSignInClient.signOut().addOnCompleteListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun savePlaceToFirestore(place: Place) {
        val userId = auth.currentUser?.uid ?: return
        
        db.collection("users").document(userId).collection("addresses")
            .whereEqualTo("cep", address.cep)
            .whereEqualTo("street", address.street)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val updatedPlaces = address.places.toMutableList()
                    updatedPlaces.add(place)
                    
                    db.collection("users").document(userId).collection("addresses")
                        .document(document.id)
                        .update("places", updatedPlaces)
                        .addOnSuccessListener {
                            address.places.add(place)
                            placeAdapter.notifyItemInserted(address.places.size - 1)
                            Toast.makeText(this, "Local salvo com sucesso!", Toast.LENGTH_SHORT).show()
                            
                            val resultIntent = Intent()
                            resultIntent.putExtra("updated_address", address)
                            setResult(RESULT_OK, resultIntent)
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Erro ao salvar local no Firestore", Toast.LENGTH_SHORT).show()
                        }
                }
            }
    }
}