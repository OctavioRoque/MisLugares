package com.example.mislugares.ui

import android.content.Intent
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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mislugares.R
import com.example.mislugares.TipoLugar
import com.example.mislugares.data.NominatimApiService
import com.example.mislugares.databinding.ActivityLugaresMundialesBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Pantalla para buscar lugares de interés real (museos, teatros, monumentos)
 * a nivel mundial utilizando la API de Overpass.
 */
class LugaresMundialesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLugaresMundialesBinding
    private val viewModel: LugaresMundialesViewModel by viewModels()
    private val lugaresViewModel: LugaresViewModel by viewModels()
    private lateinit var adapter: LugaresCercanosAdapter

    // Autocompletado de región con Nominatim
    private val debounceHandler = Handler(Looper.getMainLooper())
    private var debounceRunnable: Runnable? = null
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
        binding = ActivityLugaresMundialesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.toolbar.setNavigationOnClickListener { finish() }

        setupRecyclerView()
        setupSearchBar()
        observeViewModel()
        
        // Mensaje inicial para orientar al usuario
        Toast.makeText(this, "Escribe una ciudad para empezar", Toast.LENGTH_LONG).show()
    }

    private fun setupSearchBar() {
        val regionView = binding.etSearchMundiales

        // Listener para cuando el usuario selecciona una sugerencia
        regionView.setOnItemClickListener { _, _, _, _ ->
            isSelectingSuggestion = true
            val selected = regionView.text.toString()
            viewModel.buscarPorRegion(selected)
            
            // Ocultar teclado
            val imm = getSystemService(android.view.inputmethod.InputMethodManager::class.java)
            imm.hideSoftInputFromWindow(regionView.windowToken, 0)
            isSelectingSuggestion = false
        }

        // TextWatcher con debounce para buscar regiones sugeridas
        regionView.addTextChangedListener(object : android.text.TextWatcher {
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
                            // En esta pantalla buscamos a nivel mundial (countryCodes = null)
                            val response = nominatimService.searchAddress(query, countryCodes = null)
                            if (response.isSuccessful) {
                                val results = response.body() ?: emptyList()
                                withContext(Dispatchers.Main) {
                                    val suggestions = results.mapNotNull { it.display_name }
                                    val adapterArr = ArrayAdapter(
                                        this@LugaresMundialesActivity,
                                        android.R.layout.simple_dropdown_item_1line,
                                        suggestions
                                    )
                                    regionView.setAdapter(adapterArr)
                                    if (suggestions.isNotEmpty() && regionView.hasFocus()) {
                                        regionView.showDropDown()
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.w("LugaresMundiales", "Error buscando regiones: ${e.message}")
                        }
                    }
                }
                debounceHandler.postDelayed(debounceRunnable!!, 500)
            }
        })

        binding.etSearchMundiales.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.etSearchMundiales.text.toString().trim()
                if (query.length >= 3) {
                    viewModel.buscarPorRegion(query)
                    // Ocultar teclado
                    val imm = getSystemService(android.view.inputmethod.InputMethodManager::class.java)
                    imm.hideSoftInputFromWindow(v.windowToken, 0)
                } else {
                    Toast.makeText(this, "Escribe al menos 3 letras", Toast.LENGTH_SHORT).show()
                }
                true
            } else {
                false
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = LugaresCercanosAdapter(
            emptyList(),
            null,
            false, // Desactivar distancia para mostrar Región en su lugar
            onSaveFavoritoClick = { lugarCercano ->
                val guardado = lugaresViewModel.guardarLugarCercanoComoFavorito(lugarCercano)
                if (guardado) {
                    Toast.makeText(this, "'${lugarCercano.nombre}' ${getString(R.string.saved_to_favorites)}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "'${lugarCercano.nombre}' ${getString(R.string.already_in_favorites)}", Toast.LENGTH_SHORT).show()
                }
            }
        ) { lugarCercano ->
            val intent = Intent(this, EdicionLugarActivity::class.java).apply {
                putExtra("PREFILL_NOMBRE", lugarCercano.nombre)
                putExtra("PREFILL_DIRECCION", lugarCercano.direccion)
                putExtra("PREFILL_LAT", lugarCercano.geoPunto.latitud.toString())
                putExtra("PREFILL_LON", lugarCercano.geoPunto.longitud.toString())
                putExtra("PREFILL_TIPO", mapOsmToTipoLugar(lugarCercano.categoria).ordinal)
            }
            startActivity(intent)
        }
        binding.rvLugaresMundiales.layoutManager = LinearLayoutManager(this)
        binding.rvLugaresMundiales.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.lugares.observe(this) { lugares ->
            adapter.updateLugares(lugares)
            binding.tvEmpty.visibility = if (lugares.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, "Información: $it", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun mapOsmToTipoLugar(category: String): TipoLugar {
        return when (category.lowercase()) {
            "museum", "gallery", "artwork" -> TipoLugar.OTROS
            "theatre", "arts_centre" -> TipoLugar.ESPECTACULO
            "monument", "memorial", "castle", "ruins", "archaeological_site" -> TipoLugar.OTROS
            "attraction", "viewpoint" -> TipoLugar.NATURALEZA
            else -> TipoLugar.OTROS
        }
    }
}
