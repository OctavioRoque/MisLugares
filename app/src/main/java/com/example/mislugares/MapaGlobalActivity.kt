package com.example.mislugares

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.mislugares.databinding.ActivityMapaGlobalBinding
import com.example.mislugares.ui.EdicionLugarActivity
import com.example.mislugares.ui.LugaresViewModel
import com.example.mislugares.ui.MapLibreHelper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.geojson.Feature
import org.maplibre.geojson.Point

class MapaGlobalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMapaGlobalBinding
    private val viewModel: LugaresViewModel by viewModels()
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var mapLibreMap: MapLibreMap? = null
    private var mapStyle: Style? = null
    private var currentStyleOption = MapLibreHelper.MapStyleOption.LIBERTY
    private var userLocation: Location = LocationHelper.getFimeLocation()
    private var lugaresGuardados: List<Lugar> = emptyList()
    private var selectedLugarIndex: Int = -1

    companion object {
        private const val USER_SOURCE_ID = "global-user-source"
        private const val USER_LAYER_ID = "global-user-layer"
        private const val PLACES_SOURCE_ID = "global-places-source"
        private const val PLACES_LAYER_ID = "global-places-layer"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(this)

        binding = ActivityMapaGlobalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup Toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.toolbar.navigationIcon?.setTint(resources.getColor(R.color.text_primary, theme))

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        binding.mapView.onCreate(savedInstanceState)
        setupControls()
        observeLugares()
        setupMap()
    }

    private fun setupControls() {
        binding.fabMyLocation.setOnClickListener {
            binding.cardSelectedPlace.visibility = View.GONE
            centrarEnUbicacion(animar = true)
        }

        binding.btnMapStyle.setOnClickListener {
            mostrarSelectorEstilos()
        }

        binding.btnVerDetalleLugar.setOnClickListener {
            if (selectedLugarIndex != -1) {
                val intent = Intent(this, EdicionLugarActivity::class.java).apply {
                    putExtra("LUGAR_INDEX", selectedLugarIndex)
                }
                startActivity(intent)
            }
        }
    }

    private fun observeLugares() {
        viewModel.lugares.observe(this) { lugares ->
            lugaresGuardados = lugares
            renderizarPinesEnMapa()
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

            // Centro inicial sobre Monterrey / FIME antes de recibir GPS
            map.moveCamera(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder()
                        .target(LatLng(userLocation.latitude, userLocation.longitude))
                        .zoom(13.0)
                        .tilt(15.0)
                        .build()
                )
            )

            map.addOnMapClickListener { latLng ->
                manejarToqueEnMapa(map, latLng)
            }

            aplicarEstilo(currentStyleOption)
            centrarEnUbicacion(animar = false)
        }
    }

    private fun aplicarEstilo(option: MapLibreHelper.MapStyleOption) {
        currentStyleOption = option
        binding.tvMapStyleLabel.text = getString(option.labelRes)
        mapLibreMap?.setStyle(Style.Builder().fromUri(option.url)) { style ->
            mapStyle = style
            MapLibreHelper.registerPinImages(this, style)
            renderizarPinesEnMapa()
            actualizarMarcadorUsuario()
        }
    }

    private fun mostrarSelectorEstilos() {
        val options = MapLibreHelper.MapStyleOption.values()
        val labels = options.map { getString(it.labelRes) }.toTypedArray()
        val checkedIdx = options.indexOf(currentStyleOption)

        AlertDialog.Builder(this)
            .setTitle(R.string.choose_map_style)
            .setSingleChoiceItems(labels, checkedIdx) { dialog, which ->
                aplicarEstilo(options[which])
                dialog.dismiss()
            }
            .show()
    }

    private fun renderizarPinesEnMapa() {
        val style = mapStyle ?: return
        if (!style.isFullyLoaded) return

        val features = lugaresGuardados.mapIndexedNotNull { index, lugar ->
            if (lugar.latitud == 0.0 && lugar.longitud == 0.0) return@mapIndexedNotNull null
            Feature.fromGeometry(Point.fromLngLat(lugar.longitud, lugar.latitud)).apply {
                addStringProperty("icon", MapLibreHelper.pinIconId(lugar.tipo))
                addStringProperty("title", lugar.nombre ?: "")
                addStringProperty("address", lugar.direccion ?: "")
                addNumberProperty("index", index)
            }
        }

        MapLibreHelper.setupOrUpdatePlacePins(
            style = style,
            sourceId = PLACES_SOURCE_ID,
            layerId = PLACES_LAYER_ID,
            features = features,
            isDarkStyle = currentStyleOption.isDark,
            showLabels = true
        )
    }

    private fun actualizarMarcadorUsuario() {
        val style = mapStyle ?: return
        if (!style.isFullyLoaded) return

        val esFime = LocationHelper.isEmulatorDefaultLocation(userLocation) ||
            (Math.abs(userLocation.latitude - LocationHelper.LAT_FIME) < 0.001 &&
                Math.abs(userLocation.longitude - LocationHelper.LON_FIME) < 0.001)
        val label = if (esFime) "FIME - UANL" else getString(R.string.current_location_title)

        MapLibreHelper.setupOrUpdateUserLocation(
            style = style,
            sourceId = USER_SOURCE_ID,
            layerId = USER_LAYER_ID,
            lat = userLocation.latitude,
            lon = userLocation.longitude,
            label = label,
            isDarkStyle = currentStyleOption.isDark
        )
    }

    private fun manejarToqueEnMapa(map: MapLibreMap, latLng: LatLng): Boolean {
        val style = mapStyle
        if (style == null || !style.isFullyLoaded) return false

        val screenPoint = map.projection.toScreenLocation(latLng)
        val features = map.queryRenderedFeatures(screenPoint, PLACES_LAYER_ID)
        val feature = features.firstOrNull()

        if (feature != null) {
            val title = feature.getStringProperty("title") ?: ""
            val address = feature.getStringProperty("address") ?: ""
            val indexProp = feature.getNumberProperty("index")?.toInt() ?: -1
            val geometry = feature.geometry() as? Point

            selectedLugarIndex = indexProp
            binding.tvSelectedPlaceTitle.text = title
            binding.tvSelectedPlaceAddress.text = address.ifBlank { getString(R.string.address_not_available) }
            binding.cardSelectedPlace.visibility = View.VISIBLE

            if (geometry != null) {
                map.easeCamera(
                    CameraUpdateFactory.newCameraPosition(
                        CameraPosition.Builder()
                            .target(LatLng(geometry.latitude(), geometry.longitude()))
                            .zoom(16.0)
                            .tilt(20.0)
                            .build()
                    ),
                    700
                )
            }
            return true
        } else {
            binding.cardSelectedPlace.visibility = View.GONE
            selectedLugarIndex = -1
            return false
        }
    }

    private fun centrarEnUbicacion(animar: Boolean = true) {
        val finePerm = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarsePerm = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!finePerm && !coarsePerm) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                1001
            )
            // Centrar de inmediato en fallback FIME para no dejar el mapa vacío
            actualizarMarcadorUsuario()
            moverCamaraUsuario(animar)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            userLocation = LocationHelper.getEffectiveLocation(location)
            actualizarMarcadorUsuario()
            moverCamaraUsuario(animar)
        }.addOnFailureListener {
            Toast.makeText(this, R.string.location_error, Toast.LENGTH_SHORT).show()
            actualizarMarcadorUsuario()
            moverCamaraUsuario(animar)
        }
    }

    private fun moverCamaraUsuario(animar: Boolean) {
        val map = mapLibreMap ?: return
        val position = CameraPosition.Builder()
            .target(LatLng(userLocation.latitude, userLocation.longitude))
            .zoom(16.0)
            .tilt(20.0)
            .build()
        val update = CameraUpdateFactory.newCameraPosition(position)
        if (animar) {
            map.easeCamera(update, 900)
        } else {
            map.moveCamera(update)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.any { it == PackageManager.PERMISSION_GRANTED }) {
            centrarEnUbicacion(animar = true)
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
