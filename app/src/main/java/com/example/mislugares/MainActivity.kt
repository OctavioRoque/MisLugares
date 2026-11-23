package com.example.mislugares

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.os.LocaleListCompat
import com.example.mislugares.databinding.ActivityMainBinding
import com.example.mislugares.ui.EdicionLugarActivity
import com.example.mislugares.ui.FavoritosActivity
import com.example.mislugares.ui.LugaresCercanosActivity
import com.example.mislugares.ui.LugaresMundialesActivity
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
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: LugaresViewModel by viewModels()
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Coordenadas fijas de FIME - UANL (Facultad de Ingeniería Mecánica y Eléctrica)
    private val LAT_FIME = LocationHelper.LAT_FIME
    private val LON_FIME = LocationHelper.LON_FIME

    private var userLocation: Location = LocationHelper.getFimeLocation()
    private var mapLibreMap: MapLibreMap? = null
    private var mapStyle: Style? = null

    companion object {
        private const val USER_SOURCE_ID = "main-user-source"
        private const val USER_LAYER_ID = "main-user-layer"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        binding.mainMapView.onCreate(savedInstanceState)
        setupMap()
        setupMenuCards()
        setupCounters()
        requestLocation()
    }

    /**
     * Muestra contadores dinámicos en las tarjetas del dashboard.
     */
    private fun setupCounters() {
        viewModel.lugares.observe(this) { lugares ->
            val favCount = lugares.count { it.esFavorito() }
            if (favCount > 0) {
                binding.tvFavoritosCount.text = String.format(getString(R.string.favorites_count), favCount)
            } else {
                binding.tvFavoritosCount.text = getString(R.string.my_favorites_desc)
            }
        }
        // Mostrar conteo fijo de puntos cercanos a FIME (del repositorio local)
        binding.tvCercanosCount.text = String.format(getString(R.string.nearby_count), 35)
    }

    private fun setupMap() {
        binding.pillLocationBadge.setOnClickListener {
            centrarEnUbicacion(mostrarMensaje = true, animar = true)
        }

        binding.mainMapView.getMapAsync { map ->
            mapLibreMap = map
            MapLibreHelper.configureUiSettings(
                map = map,
                showCompass = false,
                enableTilt = true,
                enableRotate = true
            )
            map.setStyle(Style.Builder().fromUri(MapLibreHelper.MapStyleOption.LIBERTY.url)) { style ->
                mapStyle = style
                MapLibreHelper.registerPinImages(this, style)
                centrarEnUbicacion(mostrarMensaje = false, animar = false)
            }
        }
    }

    private fun setupMenuCards() {
        // 1. Botón / Tarjeta: Mis Lugares Favoritos
        binding.cardFavoritos.setOnClickListener {
            startActivity(Intent(this, FavoritosActivity::class.java))
        }

        // 2. Botón / Tarjeta: Lugares Cercanos
        binding.cardCercanos.setOnClickListener {
            startActivity(Intent(this, LugaresCercanosActivity::class.java))
        }

        // Botón / Tarjeta: Lugares Mundiales
        binding.cardMundiales.setOnClickListener {
            startActivity(Intent(this, LugaresMundialesActivity::class.java))
        }

        // 3. Botón / Tarjeta: Añadir Lugar
        binding.cardAnadirLugar.setOnClickListener {
            startActivity(Intent(this, EdicionLugarActivity::class.java))
        }

        // 4. Botón / Tarjeta: Explorar Mapa Global -> Abre la actividad con mapa completo
        binding.cardDondeEstoy.setOnClickListener {
            startActivity(Intent(this, MapaGlobalActivity::class.java))
        }

        // 5. Botón / Tarjeta: Cambiar Idioma
        binding.cardCambiarIdioma.setOnClickListener {
            showLanguageDialog()
        }
    }

    private fun centrarEnUbicacion(mostrarMensaje: Boolean = false, animar: Boolean = true) {
        val esFime = LocationHelper.isEmulatorDefaultLocation(userLocation) ||
            (Math.abs(userLocation.latitude - LAT_FIME) < 0.001 && Math.abs(userLocation.longitude - LON_FIME) < 0.001)

        val badgeLabel = if (esFime) "FIME - UANL" else getString(R.string.current_location_title)
        binding.tvLocationBadge.text = badgeLabel

        val style = mapStyle
        if (style != null && style.isFullyLoaded) {
            MapLibreHelper.setupOrUpdateUserLocation(
                style = style,
                sourceId = USER_SOURCE_ID,
                layerId = USER_LAYER_ID,
                lat = userLocation.latitude,
                lon = userLocation.longitude,
                label = badgeLabel,
                isDarkStyle = false
            )
        }

        val map = mapLibreMap
        if (map != null) {
            val position = CameraPosition.Builder()
                .target(LatLng(userLocation.latitude, userLocation.longitude))
                .zoom(16.5)
                .tilt(15.0)
                .build()
            val update = CameraUpdateFactory.newCameraPosition(position)
            if (animar) {
                map.easeCamera(update, 800)
            } else {
                map.moveCamera(update)
            }
        }

        if (mostrarMensaje) {
            val mensaje = if (esFime) {
                "Ubicación: FIME - UANL (Cd. Universitaria, San Nicolás)"
            } else {
                String.format(Locale.getDefault(), "Ubicación actual: %.4f, %.4f", userLocation.latitude, userLocation.longitude)
            }
            Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLanguageDialog() {
        val languages = arrayOf(getString(R.string.spanish), getString(R.string.english))
        val langTags = arrayOf("es", "en")
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.select_language)
        builder.setItems(languages) { _, which ->
            val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(langTags[which])
            AppCompatDelegate.setApplicationLocales(appLocale)
        }
        builder.show()
    }

    private fun requestLocation() {
        val finePerm = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarsePerm = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (finePerm || coarsePerm) {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc: Location? ->
                val effective = LocationHelper.getEffectiveLocation(loc)
                userLocation = effective
                centrarEnUbicacion(mostrarMensaje = false, animar = true)
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                100
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.any { it == PackageManager.PERMISSION_GRANTED }) {
            requestLocation()
        }
    }

    override fun onStart() {
        super.onStart()
        binding.mainMapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        binding.mainMapView.onResume()
        viewModel.cargarLugares()
    }

    override fun onPause() {
        super.onPause()
        binding.mainMapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        binding.mainMapView.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mainMapView.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mainMapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.mainMapView.onDestroy()
    }
}
