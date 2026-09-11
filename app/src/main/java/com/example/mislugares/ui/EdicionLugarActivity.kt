package com.example.mislugares.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.mislugares.GeoPunto
import com.example.mislugares.Lugar
import com.example.mislugares.TipoLugar
import com.example.mislugares.R
import com.example.mislugares.databinding.ActivityEdicionLugarBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

/**
 * Activity para añadir o editar un lugar.
 */
class EdicionLugarActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEdicionLugarBinding
    private val viewModel: LugaresViewModel by viewModels()
    private var lugarIndex: Int = -1
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentGPS: GeoPunto? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityEdicionLugarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        lugarIndex = intent.getIntExtra("LUGAR_INDEX", -1)
        
        // Custom title is handled by the TextView in XML
        val titleText = if (lugarIndex == -1) getString(R.string.add_place) else getString(R.string.edit_place)
        binding.toolbar.findViewById<android.widget.TextView>(R.id.toolbar_title).text = titleText

        setupSpinner()
        loadLugarData()
        prefillFromIntent()
        
        // PRIORIDAD ABSOLUTA: Si el intent trae coordenadas o estamos editando, NO activar GPS.
        val tieneCoordenadas = currentGPS != null
        if (lugarIndex == -1 && !tieneCoordenadas) {
            android.util.Log.d("EdicionLugar", "Nuevo lugar manual sin coordenadas pre-rellenadas. Activando GPS.")
            requestLocation()
        } else {
            android.util.Log.d("EdicionLugar", "Lugar con coordenadas (OSM o Edit). GPS omitido.")
        }

        binding.btnGuardar.setOnClickListener { saveLugar() }
        binding.btnVerCamino.setOnClickListener { verCamino() }
    }

    private fun setupSpinner() {
        val tiposTraducidos = TipoLugar.values().map { tipo ->
            val resId = when (tipo) {
                TipoLugar.OTROS -> R.string.type_others
                TipoLugar.RESTAURANTE -> R.string.type_restaurant
                TipoLugar.BAR -> R.string.type_bar
                TipoLugar.COPAS -> R.string.type_drinks
                TipoLugar.ESPECTACULO -> R.string.type_show
                TipoLugar.HOTEL -> R.string.type_hotel
                TipoLugar.COMPRAS -> R.string.type_shopping
                TipoLugar.EDUCACION -> R.string.type_education
                TipoLugar.DEPORTE -> R.string.type_sport
                TipoLugar.NATURALEZA -> R.string.type_nature
                TipoLugar.GASOLINERA -> R.string.type_gas_station
            }
            getString(resId)
        }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, tiposTraducidos)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.tipo.adapter = adapter
    }

    private fun loadLugarData() {
        if (lugarIndex != -1) {
            val lugar = viewModel.getLugar(lugarIndex)
            lugar?.let {
                binding.nombre.setText(it.nombre)
                binding.telefono.setText(it.telefono.toString())
                binding.direccion.setText(it.direccion)
                binding.url.setText(it.url)
                binding.comentario.setText(it.comentario)
                binding.tipo.setSelection(it.tipo.ordinal)
                currentGPS = it.posicion
            }
        }
    }

    private fun prefillFromIntent() {
        // Obtenemos los extras del intent de forma segura
        val extras = intent.extras
        if (lugarIndex == -1 && extras != null) {
            if (extras.containsKey("PREFILL_NOMBRE")) {
                binding.nombre.setText(extras.getString("PREFILL_NOMBRE"))
            }
            if (extras.containsKey("PREFILL_DIRECCION")) {
                binding.direccion.setText(extras.getString("PREFILL_DIRECCION"))
            }
            
            // Leemos como String y convertimos a Double
            val latStr = extras.getString("PREFILL_LAT")
            val lonStr = extras.getString("PREFILL_LON")
            val lat = latStr?.toDoubleOrNull() ?: Double.NaN
            val lon = lonStr?.toDoubleOrNull() ?: Double.NaN
            
            if (!lat.isNaN() && !lon.isNaN()) {
                currentGPS = GeoPunto(lon, lat)
                android.util.Log.d("EdicionLugar", "Ubicación pre-rellenada desde OSM: $lat, $lon")
            }
            
            val tipoOrdinal = intent.getIntExtra("PREFILL_TIPO", -1)
            if (tipoOrdinal != -1) {
                binding.tipo.setSelection(tipoOrdinal)
            }
        }
    }

    private fun requestLocation() {
        // PRIORIDAD: Si ya tenemos una ubicación (de OSM o de un lugar guardado), NO pedir GPS
        val pos = currentGPS
        if (pos != null) {
            android.util.Log.d("EdicionLugar", "Ubicación ya presente: ${pos.latitud}, ${pos.longitud}. Saltando GPS.")
            return
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    // Doble verificación por si se asignó algo mientras esperábamos al GPS
                    if (currentGPS == null) {
                        currentGPS = GeoPunto(it.longitude, it.latitude)
                        android.util.Log.d("EdicionLugar", "Ubicación GPS obtenida: ${it.latitude}, ${it.longitude}")
                    }
                }
            }
        }
    }

    private fun saveLugar() {
        val nombre = binding.nombre.text.toString()
        val direccion = binding.direccion.text.toString()
        val telefonoStr = binding.telefono.text.toString()
        val url = binding.url.text.toString()
        val comentario = binding.comentario.text.toString()
        val tipo = TipoLugar.values()[binding.tipo.selectedItemPosition]
        val telefono = telefonoStr.toIntOrNull() ?: 0

        if (nombre.isEmpty()) {
            binding.nombre.error = getString(R.string.name_required)
            return
        }

        val posicion = currentGPS ?: GeoPunto(0.0, 0.0)
        val nuevoLugar = Lugar(nombre, direccion, posicion.longitud, posicion.latitud, tipo, telefono, url, comentario, 0)

        if (lugarIndex == -1) {
            viewModel.addLugar(nuevoLugar)
            Toast.makeText(this, R.string.place_added, Toast.LENGTH_SHORT).show()
        } else {
            viewModel.updateLugar(lugarIndex, nuevoLugar)
            Toast.makeText(this, R.string.place_updated, Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    private fun verCamino() {
        val pos = currentGPS ?: return
        val nombre = binding.nombre.text.toString()
        // Usamos el formato q=lat,lon(Nombre) para que marque el punto exacto con etiqueta
        val uri = Uri.parse("geo:${pos.latitud},${pos.longitud}?q=${pos.latitud},${pos.longitud}(${Uri.encode(nombre)})")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.google.android.apps.maps")
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, R.string.maps_not_found, Toast.LENGTH_SHORT).show()
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }
}