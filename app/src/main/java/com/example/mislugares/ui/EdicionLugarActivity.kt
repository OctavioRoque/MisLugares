package com.example.mislugares.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.mislugares.GeoPunto
import com.example.mislugares.LocationHelper
import com.example.mislugares.Lugar
import com.example.mislugares.R
import com.example.mislugares.TipoLugar
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
import org.json.JSONObject
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.geojson.Feature
import org.maplibre.geojson.Point
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

    private var mapLibreMap: MapLibreMap? = null
    private var mapStyle: Style? = null

    // Autocompletado de dirección con Nominatim
    private val debounceHandler = Handler(Looper.getMainLooper())
    private var debounceRunnable: Runnable? = null
    private var searchResults: List<NominatimSearchResult> = emptyList()
    private var isSelectingSuggestion = false

    companion object {
        private const val PREVIEW_SOURCE_ID = "edicion-preview-source"
        private const val PREVIEW_LAYER_ID = "edicion-preview-layer"
    }

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
        MapLibre.getInstance(this)
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

        binding.mapPreview.onCreate(savedInstanceState)
        setupMapPreview()
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

        // Los botones de acciones adicionales (Ruta, Compartir, Copiar) solo están disponibles para lugares guardados
        val esLugarGuardado = lugarIndex != -1
        if (esLugarGuardado) {
            binding.btnVerCamino.visibility = View.VISIBLE
            binding.btnVerCamino.setOnClickListener { verCamino() }

            binding.btnCompartirLugar.visibility = View.VISIBLE
            binding.btnCompartirLugar.isEnabled = true
            binding.btnCompartirLugar.alpha = 1.0f
            binding.btnCompartirLugar.setOnClickListener { compartirLugar() }

            binding.btnCopiarEnlace.visibility = View.VISIBLE
            binding.btnCopiarEnlace.isEnabled = true
            binding.btnCopiarEnlace.alpha = 1.0f
            binding.btnCopiarEnlace.setOnClickListener { copiarEnlaceAlPortapapeles() }
        } else {
            binding.btnVerCamino.visibility = View.GONE
            binding.btnCompartirLugar.visibility = View.GONE
            binding.btnCopiarEnlace.visibility = View.GONE
        }

        setupAddressAutocomplete()
    }

    private fun setupMapPreview() {
        binding.mapPreview.getMapAsync { map ->
            mapLibreMap = map
            MapLibreHelper.configureUiSettings(
                map = map,
                showCompass = false,
                enableTilt = false,
                enableRotate = false
            )

            // Permite tocar el mapa para ajustar la ubicación exacta del pin manualmente
            map.addOnMapClickListener { latLng ->
                currentGPS = GeoPunto(latLng.longitude, latLng.latitude)
                updateMapPreview(animate = true)
                true
            }

            map.setStyle(Style.Builder().fromUri(MapLibreHelper.MapStyleOption.LIBERTY.url)) { style ->
                mapStyle = style
                MapLibreHelper.registerPinImages(this, style)
                if (currentGPS != null) {
                    updateMapPreview(animate = false)
                } else {
                    // Vista inicial centrada en Monterrey / FIME en lugar del océano (0,0)
                    map.moveCamera(
                        CameraUpdateFactory.newCameraPosition(
                            CameraPosition.Builder()
                                .target(LatLng(LocationHelper.LAT_FIME, LocationHelper.LON_FIME))
                                .zoom(16.0)
                                .tilt(0.0)
                                .build()
                        )
                    )
                }
            }
        }
    }

    private fun generarDeepLink(): String {
        val nombre = binding.nombre.text.toString().trim()
        val direccion = binding.direccion.text.toString().trim()
        val pos = currentGPS
        val tipoOrdinal = binding.tipo.selectedItemPosition
        val tel = binding.telefono.text.toString().trim()
        val url = binding.url.text.toString().trim()
        val comentario = binding.comentario.text.toString().trim()

        val json = JSONObject().apply {
            put("nombre", nombre)
            if (direccion.isNotBlank()) put("direccion", direccion)
            if (pos != null && (pos.latitud != 0.0 || pos.longitud != 0.0)) {
                put("lat", pos.latitud)
                put("lon", pos.longitud)
            }
            put("tipo", tipoOrdinal)
            if (tel.isNotBlank() && tel != "0") put("tel", tel)
            if (url.isNotBlank()) put("url", url)
            if (comentario.isNotBlank()) put("comentario", comentario)
        }

        val encoded = Base64.encodeToString(json.toString().toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP)
        return "mislugares://lugar?data=$encoded"
    }

    private fun copiarEnlaceAlPortapapeles() {
        val deepLink = generarDeepLink()
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Lugar compartido", deepLink)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, getString(R.string.link_copied), Toast.LENGTH_SHORT).show()
    }

    private fun compartirLugar() {
        val nombre = binding.nombre.text.toString().trim()
        val direccion = binding.direccion.text.toString().trim()
        val pos = currentGPS
        val tel = binding.telefono.text.toString().trim()
        val comentario = binding.comentario.text.toString().trim()

        val deepLinkApp = generarDeepLink()
        val encodedData = deepLinkApp.substringAfter("data=")
        val bridgeUrl = "https://octavioroque.github.io/MisLugares/?data=$encodedData"

        val mensaje = buildString {
            append(nombre)
            if (direccion.isNotBlank()) append("\nDirección: $direccion")
            if (tel.isNotBlank() && tel != "0") append("\nTel: $tel")
            if (comentario.isNotBlank()) append("\nComentario: $comentario")
            if (pos != null && (pos.latitud != 0.0 || pos.longitud != 0.0)) {
                append("\nGoogle Maps: https://maps.google.com/?q=${pos.latitud},${pos.longitud}")
            }
            append("\n\n${getString(R.string.open_in_app)}:\n$bridgeUrl")
            append("\n\n${getString(R.string.share_link_disclaimer)}")
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

                direccionView.setText(selected.display_name ?: "")
                direccionView.setSelection(direccionView.text.length)

                // Actualizar coordenadas GPS del lugar seleccionado
                val lat = selected.lat?.toDoubleOrNull()
                val lon = selected.lon?.toDoubleOrNull()
                if (lat != null && lon != null) {
                    currentGPS = GeoPunto(lon, lat)
                    updateMapPreview(animate = true)
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
                            val response = nominatimService.searchAddress(query, countryCodes = "mx")
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
                debounceHandler.postDelayed(debounceRunnable!!, 300)
            }
        })
    }

    private fun updateMapPreview(animate: Boolean = true) {
        val gps = currentGPS ?: return
        val style = mapStyle
        val tipoActual = TipoLugar.values().getOrNull(binding.tipo.selectedItemPosition) ?: TipoLugar.OTROS
        val markerTitle = binding.nombre.text.toString().takeIf { it.isNotBlank() } ?: ""

        if (style != null && style.isFullyLoaded) {
            val feature = Feature.fromGeometry(Point.fromLngLat(gps.longitud, gps.latitud)).apply {
                addStringProperty("icon", MapLibreHelper.pinIconId(tipoActual))
                addStringProperty("title", markerTitle)
            }
            MapLibreHelper.setupOrUpdatePlacePins(
                style = style,
                sourceId = PREVIEW_SOURCE_ID,
                layerId = PREVIEW_LAYER_ID,
                features = listOf(feature),
                isDarkStyle = false,
                showLabels = markerTitle.isNotBlank()
            )
        }

        val map = mapLibreMap ?: return
        val position = CameraPosition.Builder()
            .target(LatLng(gps.latitud, gps.longitud))
            .zoom(16.5)
            .tilt(0.0)
            .build()
        val update = CameraUpdateFactory.newCameraPosition(position)
        if (animate) {
            map.easeCamera(update, 650)
        } else {
            map.moveCamera(update)
        }
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

        binding.tipo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (currentGPS != null) {
                    updateMapPreview(animate = false)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
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
                updateMapPreview(animate = false)
            }
        }
    }

    private fun prefillFromIntent() {
        val extras = intent.extras
        if (lugarIndex == -1 && extras != null) {
            if (extras.containsKey("PREFILL_NOMBRE")) {
                binding.nombre.setText(extras.getString("PREFILL_NOMBRE"))
            }
            if (extras.containsKey("PREFILL_DIRECCION")) {
                binding.direccion.setText(extras.getString("PREFILL_DIRECCION"))
            }

            val latStr = extras.getString("PREFILL_LAT")
            val lonStr = extras.getString("PREFILL_LON")
            val lat = latStr?.toDoubleOrNull() ?: Double.NaN
            val lon = lonStr?.toDoubleOrNull() ?: Double.NaN

            if (!lat.isNaN() && !lon.isNaN()) {
                currentGPS = GeoPunto(lon, lat)
                updateMapPreview(animate = false)
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
            val encodedData = dataUri.getQueryParameter("data")
            if (encodedData != null) {
                try {
                    val decodedBytes = Base64.decode(encodedData, Base64.URL_SAFE or Base64.NO_WRAP)
                    val jsonStr = String(decodedBytes, Charsets.UTF_8)
                    val json = JSONObject(jsonStr)

                    val nombreUri = json.optString("nombre")
                    val dirUri = json.optString("direccion")
                    val latUri = if (json.has("lat")) json.optDouble("lat") else null
                    val lonUri = if (json.has("lon")) json.optDouble("lon") else null
                    val tipoUri = if (json.has("tipo")) json.optInt("tipo") else null
                    val telUri = json.optString("tel")
                    val urlUri = json.optString("url")
                    val comentarioUri = json.optString("comentario")

                    if (!nombreUri.isNullOrBlank()) binding.nombre.setText(nombreUri)
                    if (!dirUri.isNullOrBlank()) binding.direccion.setText(dirUri)
                    if (!telUri.isNullOrBlank() && telUri != "0") binding.telefono.setText(telUri)
                    if (!urlUri.isNullOrBlank()) binding.url.setText(urlUri)
                    if (!comentarioUri.isNullOrBlank()) binding.comentario.setText(comentarioUri)

                    if (latUri != null && lonUri != null && (latUri != 0.0 || lonUri != 0.0)) {
                        currentGPS = GeoPunto(lonUri, latUri)
                        updateMapPreview(animate = false)
                    }
                    if (tipoUri != null && tipoUri in 0 until binding.tipo.adapter.count) {
                        binding.tipo.setSelection(tipoUri)
                    }
                    Toast.makeText(this, getString(R.string.shared_place_received), Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    android.util.Log.e("EdicionLugar", "Error al parsear deep link: ${e.message}")
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        binding.mapPreview.onStart()
    }

    override fun onResume() {
        super.onResume()
        binding.mapPreview.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapPreview.onPause()
    }

    override fun onStop() {
        super.onStop()
        binding.mapPreview.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapPreview.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapPreview.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        debounceRunnable?.let { debounceHandler.removeCallbacks(it) }
        binding.mapPreview.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        lugarIndex = intent.getIntExtra("LUGAR_INDEX", -1)
        loadLugarData()
        prefillFromIntent()
    }

    private fun requestLocation() {
        val pos = currentGPS
        if (pos != null) {
            android.util.Log.d("EdicionLugar", "Ubicación ya presente: ${pos.latitud}, ${pos.longitud}. Saltando GPS.")
            return
        }

        val finePerm = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarsePerm = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (finePerm || coarsePerm) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                val effective = LocationHelper.getEffectiveLocation(location)
                if (currentGPS == null) {
                    currentGPS = GeoPunto(effective.longitude, effective.latitude)
                    updateMapPreview(animate = true)
                    android.util.Log.d("EdicionLugar", "Ubicación GPS obtenida: ${effective.latitude}, ${effective.longitude}")
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
            intent.getBooleanExtra("PREFILL_FAVORITO", true)
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
