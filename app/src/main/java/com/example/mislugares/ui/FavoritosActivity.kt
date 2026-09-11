package com.example.mislugares.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mislugares.databinding.ActivityFavoritosBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker

/**
 * Pantalla de Mis Favoritos con Mapa y Lista.
 */
class FavoritosActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFavoritosBinding
    private val viewModel: LugaresViewModel by viewModels()
    private lateinit var adapter: LugaresAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityFavoritosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupMap()
        setupRecyclerView()
        requestLocation()
        
        binding.fabAdd.setOnClickListener {
            startActivity(Intent(this, EdicionLugarActivity::class.java))
        }
    }

    private fun setupMap() {
        binding.mapView.setTileSource(TileSourceFactory.MAPNIK)
        binding.mapView.setMultiTouchControls(true)
        val mapController = binding.mapView.controller
        mapController.setZoom(15.0)
        
        // Punto inicial (ej. Monterrey)
        val startPoint = GeoPoint(25.6866, -100.3161)
        mapController.setCenter(startPoint)

        viewModel.lugares.observe(this) { lugares ->
            binding.mapView.overlays.clear()
            lugares.forEach { lugar ->
                val marker = Marker(binding.mapView)
                marker.position = GeoPoint(lugar.posicion.latitud, lugar.posicion.longitud)
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                marker.title = lugar.nombre
                marker.snippet = lugar.direccion
                binding.mapView.overlays.add(marker)
            }
            binding.mapView.invalidate()
        }
    }

    private fun setupRecyclerView() {
        adapter = LugaresAdapter(emptyList()) { index ->
            val intent = Intent(this, EdicionLugarActivity::class.java)
            intent.putExtra("LUGAR_INDEX", index)
            startActivity(intent)
        }
        binding.rvFavoritos.layoutManager = LinearLayoutManager(this)
        binding.rvFavoritos.adapter = adapter

        viewModel.lugares.observe(this) { lugares ->
            adapter.updateLugares(lugares)
        }
    }

    private fun requestLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    adapter.updateUserLocation(it)
                    // Centrar mapa en usuario si se desea
                    binding.mapView.controller.animateTo(GeoPoint(it.latitude, it.longitude))
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
        viewModel.cargarLugares()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }
}