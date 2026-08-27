package com.example.mislugares

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mislugares.databinding.ActivityMainBinding
import com.example.mislugares.ui.EdicionLugarActivity
import com.example.mislugares.ui.FavoritosActivity
import com.example.mislugares.ui.LugaresAdapter
import com.example.mislugares.ui.LugaresViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

/**
 * Activity principal refactorizada usando Views y XML.
 */
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: LugaresViewModel by viewModels()
    private lateinit var adapter: LugaresAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupRecyclerView()
        setupListeners()
        requestLocation()
    }

    private fun setupRecyclerView() {
        adapter = LugaresAdapter(emptyList()) { index ->
            val intent = Intent(this, EdicionLugarActivity::class.java)
            intent.putExtra("LUGAR_INDEX", index)
            startActivity(intent)
        }
        binding.rvSitiosInteres.layoutManager = LinearLayoutManager(this)
        binding.rvSitiosInteres.adapter = adapter

        viewModel.lugares.observe(this) { lugares ->
            adapter.updateLugares(lugares)
        }
    }

    private fun setupListeners() {
        binding.btnFavoritos.setOnClickListener {
            startActivity(Intent(this, FavoritosActivity::class.java))
        }

        binding.btnAnadir.setOnClickListener {
            val intent = Intent(this, EdicionLugarActivity::class.java)
            startActivity(intent)
        }
    }

    private fun requestLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                adapter.updateUserLocation(it)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            requestLocation()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.cargarLugares() // Recargar datos al volver
    }
}