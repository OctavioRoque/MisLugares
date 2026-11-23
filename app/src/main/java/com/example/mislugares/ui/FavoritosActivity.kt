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
import com.example.mislugares.LocationHelper
import com.example.mislugares.Lugar
import com.example.mislugares.R
import com.example.mislugares.TipoLugar
import com.example.mislugares.databinding.ActivityFavoritosBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.geojson.Feature
import org.maplibre.geojson.Point

/**
 * Pantalla de Mis Favoritos con Mapa vectorial, Filtros, Búsqueda en vivo y Lista.
 */
class FavoritosActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFavoritosBinding
    private val viewModel: LugaresViewModel by viewModels()
    private lateinit var adapter: LugaresAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var mapLibreMap: MapLibreMap? = null
    private var mapStyle: Style? = null

    private var categoriaFavorito: String = "TODOS"
    private var listaFavoritosActual: List<Lugar> = emptyList()
    private var selectedPinRealIndex: Int = -1

    companion object {
        private const val FAV_SOURCE_ID = "favoritos-source"
        private const val FAV_LAYER_ID = "favoritos-layer"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(this)
        binding = ActivityFavoritosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        binding.mapView.onCreate(savedInstanceState)
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

        binding.tvSelectedPin.setOnClickListener {
            if (selectedPinRealIndex != -1) {
                val intent = Intent(this, EdicionLugarActivity::class.java).apply {
                    putExtra("LUGAR_INDEX", selectedPinRealIndex)
                }
                startActivity(intent)
            }
        }
    }

    private fun setupMap() {
        binding.mapView.getMapAsync { map ->
            mapLibreMap = map
            MapLibreHelper.configureUiSettings(
                map = map,
                showCompass = true,
                enableTilt = true,
                enableRotate = true
            )
            // Punto inicial por defecto (Centro de Monterrey)
            map.moveCamera(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder()
                        .target(LatLng(25.670815, -100.309239))
                        .zoom(12.0)
                        .tilt(0.0)
                        .build()
                )
            )

            map.addOnMapClickListener { latLng ->
                manejarToquePin(map, latLng)
            }

            map.setStyle(Style.Builder().fromUri(MapLibreHelper.MapStyleOption.LIBERTY.url)) { style ->
                mapStyle = style
                MapLibreHelper.registerPinImages(this, style)
                actualizarMarcadoresMapa(listaFavoritosActual, animar = false)
            }
        }
    }

    private fun manejarToquePin(map: MapLibreMap, latLng: LatLng): Boolean {
        val style = mapStyle
        if (style == null || !style.isFullyLoaded) return false

        val screenPoint = map.projection.toScreenLocation(latLng)
        val feature = map.queryRenderedFeatures(screenPoint, FAV_LAYER_ID).firstOrNull()
        if (feature != null) {
            val title = feature.getStringProperty("title") ?: ""
            val realIndex = feature.getNumberProperty("realIndex")?.toInt() ?: -1
            val geometry = feature.geometry() as? Point

            selectedPinRealIndex = realIndex
            binding.tvSelectedPin.text = "$title  ›"
            binding.tvSelectedPin.visibility = View.VISIBLE

            if (geometry != null) {
                map.easeCamera(
                    CameraUpdateFactory.newCameraPosition(
                        CameraPosition.Builder()
                            .target(LatLng(geometry.latitude(), geometry.longitude()))
                            .zoom(16.0)
                            .tilt(0.0)
                            .build()
                    ),
                    650
                )
            }
            return true
        } else {
            selectedPinRealIndex = -1
            binding.tvSelectedPin.visibility = View.GONE
            return false
        }
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
            val lugarSeleccionado = adapter.getLugarAt(positionInList)
            if (lugarSeleccionado != null) {
                val todos = viewModel.lugares.value ?: emptyList()
                val realIndex = todos.indexOf(lugarSeleccionado)
                if (realIndex != -1) {
                    val intent = Intent(this, EdicionLugarActivity::class.java).apply {
                        putExtra("LUGAR_INDEX", realIndex)
                    }
                    startActivity(intent)
                }
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
        binding.tvSelectedPin.visibility = View.GONE
        selectedPinRealIndex = -1

        // Estado vacío
        binding.tvEmptyFavoritos.visibility = if (filtrados.isEmpty()) View.VISIBLE else View.GONE

        // 3. Actualizar marcadores en el mapa
        actualizarMarcadoresMapa(filtrados, animar = true)
    }

    private fun actualizarMarcadoresMapa(filtrados: List<Lugar>, animar: Boolean) {
        val style = mapStyle ?: return
        if (!style.isFullyLoaded) return

        val todos = viewModel.lugares.value ?: emptyList()
        val puntosValidos = mutableListOf<LatLng>()

        val features = filtrados.mapNotNull { lugar ->
            if (lugar.latitud == 0.0 && lugar.longitud == 0.0) return@mapNotNull null
            puntosValidos.add(LatLng(lugar.latitud, lugar.longitud))
            val realIndex = todos.indexOf(lugar)
            Feature.fromGeometry(Point.fromLngLat(lugar.longitud, lugar.latitud)).apply {
                addStringProperty("icon", MapLibreHelper.pinIconId(lugar.tipo))
                addStringProperty("title", lugar.nombre ?: "")
                addNumberProperty("realIndex", realIndex)
            }
        }

        MapLibreHelper.setupOrUpdatePlacePins(
            style = style,
            sourceId = FAV_SOURCE_ID,
            layerId = FAV_LAYER_ID,
            features = features,
            isDarkStyle = false,
            showLabels = true
        )

        val map = mapLibreMap ?: return
        if (puntosValidos.isEmpty()) return

        if (puntosValidos.size == 1) {
            val unico = puntosValidos.first()
            val update = CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder()
                    .target(unico)
                    .zoom(15.5)
                    .tilt(0.0)
                    .build()
            )
            if (animar) map.easeCamera(update, 700) else map.moveCamera(update)
        } else {
            val minLat = puntosValidos.minOf { it.latitude }
            val maxLat = puntosValidos.maxOf { it.latitude }
            val minLon = puntosValidos.minOf { it.longitude }
            val maxLon = puntosValidos.maxOf { it.longitude }

            if (maxLat - minLat > 0.0005 || maxLon - minLon > 0.0005) {
                try {
                    val bounds = LatLngBounds.Builder().includes(puntosValidos).build()
                    val padPx = (48 * resources.displayMetrics.density).toInt()
                    val update = CameraUpdateFactory.newLatLngBounds(bounds, padPx)
                    if (animar) map.easeCamera(update, 750) else map.moveCamera(update)
                } catch (_: Exception) {
                    val primero = puntosValidos.first()
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(primero, 13.5))
                }
            } else {
                val primero = puntosValidos.first()
                val update = CameraUpdateFactory.newLatLngZoom(primero, 15.0)
                if (animar) map.easeCamera(update, 700) else map.moveCamera(update)
            }
        }
    }

    private fun requestLocation() {
        // Fallback inmediato con FIME (0 km)
        val fimeLoc = LocationHelper.getFimeLocation()
        adapter.updateUserLocation(fimeLoc)

        val finePerm = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarsePerm = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (finePerm || coarsePerm) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                val effectiveLocation = LocationHelper.getEffectiveLocation(location)
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
        if (requestCode == 101 && grantResults.any { it == PackageManager.PERMISSION_GRANTED }) {
            requestLocation()
        }
    }

    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
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

    override fun onStop() {
        super.onStop()
        binding.mapView.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapView.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.mapView.onDestroy()
    }
}
