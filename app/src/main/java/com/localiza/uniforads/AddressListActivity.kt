package com.localiza.uniforads

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.localiza.uniforads.adapter.AddressAdapter
import com.localiza.uniforads.databinding.ActivityAddressListBinding
import com.localiza.uniforads.model.Address

class AddressListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddressListBinding
    private lateinit var adapter: AddressAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    
    private val addressList = mutableListOf<Address>()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            optimizeAndReorder()
        } else {
            Toast.makeText(this, "Permissão de localização necessária para organizar rotas", Toast.LENGTH_SHORT).show()
        }
    }

    private val detailAddressLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            loadAddressesFromFirestore()
        }
    }

    private val addAddressLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val intent = result.data
            val newAddress = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent?.getSerializableExtra("new_address", Address::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent?.getSerializableExtra("new_address") as? Address
            }

            newAddress?.let { saveAddressToFirestore(it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        
        binding = ActivityAddressListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Ajuste para o toolbar não ficar sob a status bar
        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, v.paddingBottom)
            insets
        }
        
        // Ajuste para os botões não ficarem sob a navigation bar
        ViewCompat.setOnApplyWindowInsetsListener(binding.layoutButtons) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, systemBars.bottom)
            insets
        }

        setupRecyclerView()
        setupListeners()
        loadAddressesFromFirestore()
    }

    private fun setupRecyclerView() {
        adapter = AddressAdapter(mutableListOf(), { addressToDelete ->
            deleteAddressFromFirestore(addressToDelete)
        }, { addressClicked ->
            val intent = Intent(this, AddressDetailActivity::class.java)
            intent.putExtra("address", addressClicked)
            detailAddressLauncher.launch(intent)
        })
        binding.rvAddresses.layoutManager = LinearLayoutManager(this)
        binding.rvAddresses.adapter = adapter

        // Adiciona suporte a drag and drop para reordenar
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                adapter.onItemMove(viewHolder.adapterPosition, target.adapterPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
        })
        itemTouchHelper.attachToRecyclerView(binding.rvAddresses)
    }

    private fun setupListeners() {
        binding.btnAddAddress.setOnClickListener {
            val intent = Intent(this, AddAddressActivity::class.java)
            addAddressLauncher.launch(intent)
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterList(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnOptimize.setOnClickListener {
            checkPermissionsAndOptimize()
        }

        binding.btnStartDeliveries.setOnClickListener {
            startDeliveries()
        }
    }

    private fun checkPermissionsAndOptimize() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        } else {
            optimizeAndReorder()
        }
    }

    private fun optimizeAndReorder() {
        val currentList = adapter.getList()
        if (currentList.isEmpty()) {
            Toast.makeText(this, "Nenhum endereço na lista", Toast.LENGTH_SHORT).show()
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val optimizedList = optimizeRoute(location, currentList)
                adapter.updateList(optimizedList)
                Toast.makeText(this, "Rota organizada automaticamente!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Não foi possível obter sua localização atual.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startDeliveries() {
        val currentOrder = adapter.getList()
        if (currentOrder.isEmpty()) {
            Toast.makeText(this, "Nenhum endereço na lista", Toast.LENGTH_SHORT).show()
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                openGoogleMapsRoute(location, currentOrder)
            } else {
                Toast.makeText(this, "Ligue o GPS para iniciar a rota.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Algoritmo do Vizinho Mais Próximo
    private fun optimizeRoute(startLocation: Location, list: List<Address>): List<Address> {
        val unvisited = list.toMutableList()
        val result = mutableListOf<Address>()
        
        var currentLat = startLocation.latitude
        var currentLng = startLocation.longitude

        while (unvisited.isNotEmpty()) {
            val nearest = unvisited.minByOrNull { address ->
                if (address.latitude != null && address.longitude != null) {
                    val results = FloatArray(1)
                    Location.distanceBetween(currentLat, currentLng, address.latitude, address.longitude, results)
                    results[0]
                } else {
                    Float.MAX_VALUE
                }
            }

            nearest?.let {
                result.add(it)
                unvisited.remove(it)
                currentLat = it.latitude ?: currentLat
                currentLng = it.longitude ?: currentLng
            } ?: break
        }
        return result
    }

    private fun openGoogleMapsRoute(currentLocation: Location, sortedAddresses: List<Address>) {
        if (sortedAddresses.isEmpty()) return

        // A origem continua sendo a localização atual por coordenadas
        val origin = "${currentLocation.latitude},${currentLocation.longitude}"
        
        // Função para formatar o endereço completo para o Maps
        fun formatAddress(addr: Address): String {
            val numberPart = if (addr.number.isNullOrBlank() || addr.number.equals("S/N", ignoreCase = true)) "" else ", ${addr.number}"
            return "${addr.street}$numberPart, ${addr.neighborhood}, ${addr.city} - ${addr.state}"
        }

        // O destino final (último da lista)
        val destination = formatAddress(sortedAddresses.last())
        
        // Os pontos intermediários (waypoints)
        val waypoints = sortedAddresses.dropLast(1).joinToString("|") { 
            formatAddress(it)
        }

        val uriBuilder = Uri.parse("https://www.google.com/maps/dir/?api=1")
            .buildUpon()
            .appendQueryParameter("origin", origin)
            .appendQueryParameter("destination", destination)
            .appendQueryParameter("travelmode", "driving")

        if (waypoints.isNotEmpty()) {
            uriBuilder.appendQueryParameter("waypoints", waypoints)
        }

        val intent = Intent(Intent.ACTION_VIEW, uriBuilder.build())
        intent.setPackage("com.google.android.apps.maps")
        
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            // Backup caso não tenha o app oficial instalado
            startActivity(Intent(Intent.ACTION_VIEW, uriBuilder.build()))
        }
    }

    private fun loadAddressesFromFirestore() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).collection("addresses")
            .get()
            .addOnSuccessListener { result ->
                addressList.clear()
                for (document in result) {
                    val address = document.toObject(Address::class.java)
                    addressList.add(address)
                }
                adapter.updateList(addressList)
                if (addressList.isEmpty()) {
                    Toast.makeText(
                        this,
                        "Nenhum endereço cadastrado",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao carregar endereços", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveAddressToFirestore(address: Address) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).collection("addresses")
            .add(address)
            .addOnSuccessListener {
                loadAddressesFromFirestore()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao salvar endereço", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteAddressFromFirestore(address: Address) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).collection("addresses")
            .whereEqualTo("cep", address.cep)
            .whereEqualTo("street", address.street)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    db.collection("users").document(userId).collection("addresses")
                        .document(document.id).delete()
                }
                loadAddressesFromFirestore()
            }
    }

    private fun filterList(query: String) {
        val filteredList = if (query.isEmpty()) {
            addressList
        } else {
            addressList.filter { 
                it.street.contains(query, ignoreCase = true) || 
                it.neighborhood.contains(query, ignoreCase = true) ||
                it.city.contains(query, ignoreCase = true)
            }
        }
        adapter.updateList(filteredList)
    }
}
