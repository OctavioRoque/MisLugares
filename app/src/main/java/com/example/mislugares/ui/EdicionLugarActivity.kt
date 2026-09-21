package com.example.mislugares.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.mislugares.GeoPunto
import com.example.mislugares.Lugar
import com.example.mislugares.TipoLugar
import com.example.mislugares.R
import com.example.mislugares.data.NominatimApiService
import com.example.mislugares.data.NominatimSearchResult
import com.example.mislugares.databinding.ActivityEdicionLugarBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Activity para añadir o editar un lugar.
 */
class EdicionLugarActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEdicionLugarBinding
    private val viewModel: LugaresViewModel by viewModels()
    private var lugarIndex: Int = -1
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentGPS: GeoPunto? = null

    // Autocompletado de dirección con Nominatim
    private val debounceHandler = Handler(Looper.getMainLooper())
    private var debounceRunnable: Runnable? = null
    private var searchResults: List<NominatimSearchResult> = emptyList()
    private var isSelectingSuggestion = false

    private val nominatimService: NominatimApiService by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "MisLugaresAndroidApp/1.0")
                    .build()
                chain.proceed(request)
            }
            .build()

        Retrofit.Builder()
            .baseUrl(NominatimApiService.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NominatimApiService::class.java)
    }

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

        // El botón de compartir solo funciona y está disponible en lugares ya guardados
        val esLugarGuardado = lugarIndex != -1
        if (esLugarGuardado) {
            binding.btnCompartirLugar.visibility = View.VISIBLE
            binding.btnCompartirLugar.isEnabled = true
            binding.btnCompartirLugar.alpha = 1.0f
            binding.btnCompartirLugar.setOnClickListener { compartirLugar() }
        } else {
            binding.btnCompartirLugar.visibility = View.GONE
        }

        setupAddressAutocomplete()
    }

    private fun compartirLugar() {
        val nombre = binding.nombre.text.toString().trim()
        val direccion = binding.direccion.text.toString().trim()
        val pos = currentGPS
        val tipoOrdinal = binding.tipo.selectedItemPosition
        val tel = binding.telefono.text.toString().trim()
        val url = binding.url.text.toString().trim()
        val comentario = binding.comentario.text.toString().trim()

        val queryParams = buildString {
            append("nombre=").append(Uri.encode(nombre))
            if (direccion.isNotBlank()) append("&direccion=").append(Uri.encode(direccion))
            if (pos != null && (pos.latitud != 0.0 || pos.longitud != 0.0)) {
                append("&lat=").append(pos.latitud)
                append("&lon=").append(pos.longitud)
            }
            append("&tipo=").append(tipoOrdinal)
            if (tel.isNotBlank() && tel != "0") append("&tel=").append(Uri.encode(tel))
            if (url.isNotBlank()) append("&url=").append(Uri.encode(url))
            if (comentario.isNotBlank()) append("&comentario=").append(Uri.encode(comentario))
        }

        // Enlaces que permiten abrir directamente el lugar en la app de otro usuario
        val deepLinkApp = "mislugares://lugar?$queryParams"
        val deepLinkWeb = "https://mislugares.app/lugar?$queryParams"

        val mensaje = buildString {
            append(nombre)
            if (direccion.isNotBlank()) append("\nDirección: $direccion")
            if (tel.isNotBlank() && tel != "0") append("\nTel: $tel")
            if (comentario.isNotBlank()) append("\nComentario: $comentario")
            if (pos != null && (pos.latitud != 0.0 || pos.longitud != 0.0)) {
                append("\nGoogle Maps: https://maps.google.com/?q=${pos.latitud},${pos.longitud}")
            }
            append("\n\n${getString(R.string.open_in_app)}:\n$deepLinkWeb\n(O: $deepLinkApp)")
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, nombre)
            putExtra(Intent.EXTRA_TEXT, mensaje)
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_place)))
    }

    private fun setupAddressAutocomplete() {
        val direccionView = binding.direccion

        // Listener para cuando el usuario selecciona una sugerencia
        direccionView.setOnItemClickListener { _, _, position, _ ->
            if (position < searchResults.size) {
                val selected = searchResults[position]
                isSelectingSuggestion = true

                // Formatear dirección legible desde los componentes
                val addr = selected.address
                val formatted = listOfNotNull(
                    addr?.road,
                    addr?.house_number,
                    addr?.neighbourhood ?: addr?.suburb,
                    addr?.city ?: addr?.town ?: addr?.village
                ).joinToString(", ")

                direccionView.setText(formatted.ifBlank { selected.display_name ?: "" })
                direccionView.setSelection(direccionView.text.length)

                // Actualizar coordenadas GPS del lugar seleccionado
                val lat = selected.lat?.toDoubleOrNull()
                val lon = selected.lon?.toDoubleOrNull()
                if (lat != null && lon != null) {
                    currentGPS = GeoPunto(lon, lat)
                    android.util.Log.d("EdicionLugar", "Coordenadas actualizadas desde autocompletado: $lat, $lon")
                }

                isSelectingSuggestion = false
            }
        }

        // TextWatcher con debounce para buscar direcciones
        direccionView.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (isSelectingSuggestion) return

                debounceRunnable?.let { debounceHandler.removeCallbacks(it) }

                val query = s?.toString()?.trim() ?: ""
                if (query.length < 3) return

                debounceRunnable = Runnable {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val response = nominatimService.searchAddress(query)
                            if (response.isSuccessful) {
                                val results = response.body() ?: emptyList()
                                withContext(Dispatchers.Main) {
                                    searchResults = results
                                    val suggestions = results.mapNotNull { it.display_name }
                                    val adapter = ArrayAdapter(
                                        this@EdicionLugarActivity,
                                        android.R.layout.simple_dropdown_item_1line,
                                        suggestions
                                    )
                                    direccionView.setAdapter(adapter)
                                    if (suggestions.isNotEmpty() && direccionView.hasFocus()) {
                                        direccionView.showDropDown()
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.w("EdicionLugar", "Error buscando direcciones: ${e.message}")
                        }
                    }
                }
                debounceHandler.postDelayed(debounceRunnable!!, 500)
            }
        })
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
                binding.ratingBar.rating = it.valoracion
                currentGPS = GeoPunto(it.longitud, it.latitud)
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

        // Manejar Deep Link si se abre desde enlace compartido
        val dataUri = intent.data
        if (lugarIndex == -1 && dataUri != null) {
            val nombreUri = dataUri.getQueryParameter("nombre")
            val dirUri = dataUri.getQueryParameter("direccion")
            val latUri = dataUri.getQueryParameter("lat")?.toDoubleOrNull()
            val lonUri = dataUri.getQueryParameter("lon")?.toDoubleOrNull()
            val tipoUri = dataUri.getQueryParameter("tipo")?.toIntOrNull()
            val telUri = dataUri.getQueryParameter("tel")
            val urlUri = dataUri.getQueryParameter("url")
            val comentarioUri = dataUri.getQueryParameter("comentario")

            if (!nombreUri.isNullOrBlank()) binding.nombre.setText(nombreUri)
            if (!dirUri.isNullOrBlank()) binding.direccion.setText(dirUri)
            if (!telUri.isNullOrBlank() && telUri != "0") binding.telefono.setText(telUri)
            if (!urlUri.isNullOrBlank()) binding.url.setText(urlUri)
            if (!comentarioUri.isNullOrBlank()) binding.comentario.setText(comentarioUri)

            if (latUri != null && lonUri != null && (latUri != 0.0 || lonUri != 0.0)) {
                currentGPS = GeoPunto(lonUri, latUri)
            }
            if (tipoUri != null && tipoUri in 0 until binding.tipo.adapter.count) {
                binding.tipo.setSelection(tipoUri)
            }
            Toast.makeText(this, getString(R.string.shared_place_received), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        lugarIndex = intent.getIntExtra("LUGAR_INDEX", -1)
        loadLugarData()
        prefillFromIntent()
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
        val esFav = if (lugarIndex != -1) {
            viewModel.getLugar(lugarIndex)?.esFavorito() ?: false
        } else {
            intent.getBooleanExtra("PREFILL_FAVORITO", false)
        }
        val valoracion = binding.ratingBar.rating
        val nuevoLugar = Lugar(nombre, direccion, posicion.longitud, posicion.latitud, tipo, telefono, url, comentario, valoracion.toInt(), esFav)
        nuevoLugar.valoracion = valoracion

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