package com.example.mislugares

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
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
import com.example.mislugares.ui.LugaresViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: LugaresViewModel by viewModels()
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Coordenadas fijas de FIME - UANL (Facultad de Ingeniería Mecánica y Eléctrica)
    private val LAT_FIME = 25.7256
    private val LON_FIME = -100.3152

    private var userLocation: Location = Location("fime_uanl").apply {
        latitude = LAT_FIME
        longitude = LON_FIME
        accuracy = 5f
    }
    private var userMarker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

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
        binding.mainMapView.setTileSource(TileSourceFactory.MAPNIK)
        binding.mainMapView.setMultiTouchControls(true)
        val mapController = binding.mainMapView.controller
        mapController.setZoom(17.0)

        // Centrado directamente en FIME - UANL
        val fimePoint = GeoPoint(LAT_FIME, LON_FIME)
        mapController.setCenter(fimePoint)

        // Marcador visible en FIME
        userMarker = Marker(binding.mainMapView).apply {
            position = fimePoint
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Mi Ubicación: FIME - UANL"
            snippet = "Facultad de Ingeniería Mecánica y Eléctrica (Cd. Universitaria)"
            binding.mainMapView.overlays.add(this)
            showInfoWindow()
        }
        binding.mainMapView.invalidate()
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

        // 3. Botón / Tarjeta: Añadir Lugar
        binding.cardAnadirLugar.setOnClickListener {
            startActivity(Intent(this, EdicionLugarActivity::class.java))
        }

        // 4. Botón / Tarjeta: ¿Dónde estoy? -> Centra el mapa en la ubicación actual con zoom y abre la info
        binding.cardDondeEstoy.setOnClickListener {
            centrarEnUbicacion(true)
        }

        // 5. Botón / Tarjeta: Cambiar Idioma
        binding.cardCambiarIdioma.setOnClickListener {
            showLanguageDialog()
        }
    }

    private fun centrarEnUbicacion(mostrarMensaje: Boolean = false) {
        val geoPoint = GeoPoint(userLocation.latitude, userLocation.longitude)
        binding.mainMapView.controller.animateTo(geoPoint)
        binding.mainMapView.controller.setZoom(17.5)

        val esFime = LocationHelper.isEmulatorDefaultLocation(userLocation) || 
                     (Math.abs(userLocation.latitude - LAT_FIME) < 0.001 && Math.abs(userLocation.longitude - LON_FIME) < 0.001)

        userMarker?.apply {
            position = geoPoint
            title = if (esFime) "Mi Ubicación: FIME - UANL" else "Mi Ubicación Actual"
            snippet = if (esFime) "Facultad de Ingeniería Mecánica y Eléctrica" else String.format(java.util.Locale.getDefault(), "Lat: %.4f, Lon: %.4f", userLocation.latitude, userLocation.longitude)
            showInfoWindow()
        }
        binding.mainMapView.invalidate()

        if (mostrarMensaje) {
            val mensaje = if (esFime) {
                "Ubicación: FIME - UANL (Cd. Universitaria, San Nicolás)"
            } else {
                String.format(java.util.Locale.getDefault(), "Ubicación actual: %.4f, %.4f", userLocation.latitude, userLocation.longitude)
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
                centrarEnUbicacion(false)
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
        requestLocation()
    }

    override fun onResume() {
        super.onResume()
        binding.mainMapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mainMapView.onPause()
    }
}
