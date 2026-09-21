package com.example.mislugares.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mislugares.Lugar
import com.example.mislugares.R
import com.example.mislugares.TipoLugar
import com.example.mislugares.databinding.ActivityFavoritosBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker

/**
 * Pantalla de Mis Favoritos con Mapa, Filtros, Búsqueda en vivo y Lista.
 */
class FavoritosActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFavoritosBinding
    private val viewModel: LugaresViewModel by viewModels()
    private lateinit var adapter: LugaresAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var categoriaFavorito: String = "TODOS"
    private var listaFavoritosActual: List<Lugar> = emptyList()

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
        setupFilters()
        setupSearchBar()
        setupRecyclerView()
        requestLocation()

        binding.fabAdd.setOnClickListener {
            val intent = Intent(this, EdicionLugarActivity::class.java).apply {
                putExtra("PREFILL_FAVORITO", true)
            }
            startActivity(intent)
        }
    }

    private fun setupMap() {
        binding.mapView.setTileSource(TileSourceFactory.MAPNIK)
        binding.mapView.setMultiTouchControls(true)
        val mapController = binding.mapView.controller
        mapController.setZoom(15.0)

        // Punto inicial por defecto
        val startPoint = GeoPoint(25.6866, -100.3161)
        mapController.setCenter(startPoint)
    }

    private fun setupFilters() {
        binding.chipFavTodos.setOnClickListener {
            categoriaFavorito = "TODOS"
            actualizarFavoritos()
        }
        binding.chipFavNaturaleza.setOnClickListener {
            categoriaFavorito = "NATURALEZA"
            actualizarFavoritos()
        }
        binding.chipFavCultura.setOnClickListener {
            categoriaFavorito = "CULTURA"
            actualizarFavoritos()
        }
        binding.chipFavGastronomia.setOnClickListener {
            categoriaFavorito = "GASTRONOMIA"
            actualizarFavoritos()
        }
        binding.chipFavHoteles.setOnClickListener {
            categoriaFavorito = "HOTELES"
            actualizarFavoritos()
        }
    }

    /**
     * Configura la barra de búsqueda con filtrado instantáneo.
     */
    private fun setupSearchBar() {
        binding.etSearchFavoritos.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                adapter.filterByText(s?.toString() ?: "")
            }
        })
    }

    private fun setupRecyclerView() {
        adapter = LugaresAdapter(
            emptyList(),
            null,
            onFavoritoClick = { lugar ->
                viewModel.toggleFavorito(lugar)
                Toast.makeText(this, getString(R.string.removed_from_favorites), Toast.LENGTH_SHORT).show()
            }
        ) { positionInList ->
            val lugarSeleccionado = listaFavoritosActual.getOrNull(positionInList)
            if (lugarSeleccionado != null) {
                val todos = viewModel.lugares.value ?: emptyList()
                val realIndex = todos.indexOf(lugarSeleccionado)
                val intent = Intent(this, EdicionLugarActivity::class.java).apply {
                    putExtra("LUGAR_INDEX", realIndex)
                }
                startActivity(intent)
            }
        }
        binding.rvFavoritos.layoutManager = LinearLayoutManager(this)
        binding.rvFavoritos.adapter = adapter

        viewModel.lugares.observe(this) {
            actualizarFavoritos()
        }
    }

    private fun actualizarFavoritos() {
        val todos = viewModel.lugares.value ?: emptyList()
        // 1. Filtrar solo los que son favoritos
        val soloFavoritos = todos.filter { it.esFavorito() }

        // 2. Aplicar filtro de categoría de favoritos
        val filtrados = soloFavoritos.filter { lugar ->
            when (categoriaFavorito) {
                "TODOS" -> true
                "NATURALEZA" -> lugar.tipo == TipoLugar.NATURALEZA
                "CULTURA" -> lugar.tipo == TipoLugar.ESPECTACULO || lugar.tipo == TipoLugar.EDUCACION
                "GASTRONOMIA" -> lugar.tipo == TipoLugar.RESTAURANTE || lugar.tipo == TipoLugar.BAR || lugar.tipo == TipoLugar.COPAS
                "HOTELES" -> lugar.tipo == TipoLugar.HOTEL
                else -> true
            }
        }

        listaFavoritosActual = filtrados
        adapter.updateLugares(filtrados)

        // Estado vacío
        binding.tvEmptyFavoritos.visibility = if (filtrados.isEmpty()) View.VISIBLE else View.GONE

        // 3. Actualizar marcadores en el mapa
        binding.mapView.overlays.clear()
        filtrados.forEach { lugar ->
            if (lugar.latitud != 0.0 || lugar.longitud != 0.0) {
                val marker = Marker(binding.mapView)
                marker.position = GeoPoint(lugar.latitud, lugar.longitud)
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                marker.title = lugar.nombre
                marker.snippet = lugar.direccion
                binding.mapView.overlays.add(marker)
            }
        }
        binding.mapView.invalidate()

        // Si hay favoritos, centrar el mapa en el primero
        if (filtrados.isNotEmpty()) {
            val primerLugar = filtrados.first()
            if (primerLugar.latitud != 0.0 || primerLugar.longitud != 0.0) {
                binding.mapView.controller.animateTo(GeoPoint(primerLugar.latitud, primerLugar.longitud))
            }
        }
    }

    private fun requestLocation() {
        // Fallback inmediato con FIME (0 km)
        val fimeLoc = com.example.mislugares.LocationHelper.getFimeLocation()
        adapter.updateUserLocation(fimeLoc)

        val finePerm = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarsePerm = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (finePerm || coarsePerm) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                val effectiveLocation = com.example.mislugares.LocationHelper.getEffectiveLocation(location)
                adapter.updateUserLocation(effectiveLocation)
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                101
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        requestLocation()
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