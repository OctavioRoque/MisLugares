package com.example.mislugares.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mislugares.R
import com.example.mislugares.TipoLugar
import com.example.mislugares.data.LugarCercano
import com.example.mislugares.databinding.ActivityLugaresCercanosBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

/**
 * Pantalla para buscar y mostrar lugares de interés cercanos usando Overpass API.
 */
class LugaresCercanosActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLugaresCercanosBinding
    private val viewModel: LugaresCercanosViewModel by viewModels()
    private lateinit var adapter: LugaresCercanosAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var lastLocation: Location? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLugaresCercanosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupRecyclerView()
        setupFilters()
        observeViewModel()
        requestLocation()
    }

    private fun setupFilters() {
        binding.cgDistancia.setOnCheckedChangeListener { _, checkedId ->
            lastLocation?.let { location ->
                val radio = when (checkedId) {
                    R.id.chip1km -> 1000
                    R.id.chip3km -> 3000
                    R.id.chip5km -> 5000
                    else -> 1000
                }
                viewModel.buscarLugaresCercanos(location, radio)
            }
        }
        // Seleccionar 1km por defecto
        binding.chip1km.isChecked = true
    }

    private fun setupRecyclerView() {
        adapter = LugaresCercanosAdapter(emptyList()) { lugarCercano ->
            // Abrir editor prellenado
            val intent = Intent(this, EdicionLugarActivity::class.java).apply {
                putExtra("PREFILL_NOMBRE", lugarCercano.nombre)
                putExtra("PREFILL_DIRECCION", lugarCercano.direccion)
                // Pasamos como String para asegurar compatibilidad total en el Intent
                putExtra("PREFILL_LAT", lugarCercano.geoPunto.latitud.toString())
                putExtra("PREFILL_LON", lugarCercano.geoPunto.longitud.toString())
                putExtra("PREFILL_TIPO", mapOsmToTipoLugar(lugarCercano.categoria).ordinal)
            }
            startActivity(intent)
        }
        binding.rvLugaresCercanos.layoutManager = LinearLayoutManager(this)
        binding.rvLugaresCercanos.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.lugaresCercanos.observe(this) { lugares ->
            adapter.updateLugares(lugares)
            binding.tvEmpty.visibility = if (lugares.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun requestLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 101)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                lastLocation = it
                adapter.updateUserLocation(it)
                val radio = when (binding.cgDistancia.checkedChipId) {
                    R.id.chip1km -> 1000
                    R.id.chip3km -> 3000
                    R.id.chip5km -> 5000
                    else -> 1000
                }
                viewModel.buscarLugaresCercanos(it, radio)
            } ?: run {
                Toast.makeText(this, R.string.location_error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mapOsmToTipoLugar(category: String): TipoLugar {
        return when (category.lowercase()) {
            "restaurant", "fast_food" -> TipoLugar.RESTAURANTE
            "cafe", "bar", "pub" -> TipoLugar.BAR
            "hotel", "hostel", "motel" -> TipoLugar.HOTEL
            "park", "nature_reserve" -> TipoLugar.NATURALEZA
            "sports_centre", "gym", "stadium" -> TipoLugar.DEPORTE
            "fuel" -> TipoLugar.GASOLINERA
            else -> TipoLugar.OTROS
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            requestLocation()
        }
    }
}
