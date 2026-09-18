package com.example.mislugares.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mislugares.R
import com.example.mislugares.TipoLugar
import com.example.mislugares.data.LugarCercano
import com.example.mislugares.databinding.ActivityLugaresCercanosBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

/**
 * Pantalla para buscar y mostrar lugares de interés cercanos usando Overpass API.
 * Incluye filtros de distancia, filtros por tipo de lugar y guardado directo en Favoritos.
 */
class LugaresCercanosActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLugaresCercanosBinding
    private val viewModel: LugaresCercanosViewModel by viewModels()
    private val lugaresViewModel: LugaresViewModel by viewModels()
    private lateinit var adapter: LugaresCercanosAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var lastLocation: Location? = null

    private var radioMaxMetros: Int = 1000
    private var categoriaSeleccionada: String = "TODOS"
    private var listaCercanosOriginal: List<LugarCercano> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityLugaresCercanosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.toolbar.setNavigationOnClickListener { finish() }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupRecyclerView()
        setupFilters()
        observeViewModel()
        requestLocation()
    }

    private fun setupFilters() {
        // Filtros de distancia (1 km, 3 km, 5 km, Todos)
        binding.chip1km.setOnClickListener {
            radioMaxMetros = 1000
            aplicarFiltros()
        }
        binding.chip3km.setOnClickListener {
            radioMaxMetros = 3000
            aplicarFiltros()
        }
        binding.chip5km.setOnClickListener {
            radioMaxMetros = 5000
            aplicarFiltros()
        }
        binding.chipTodosDistancia.setOnClickListener {
            radioMaxMetros = Int.MAX_VALUE
            aplicarFiltros()
        }

        // Filtros por tipo de categoría
        binding.chipCercanoTodos.setOnClickListener {
            categoriaSeleccionada = "TODOS"
            aplicarFiltros()
        }
        binding.chipCercanoComida.setOnClickListener {
            categoriaSeleccionada = "COMIDA"
            aplicarFiltros()
        }
        binding.chipCercanoNaturaleza.setOnClickListener {
            categoriaSeleccionada = "NATURALEZA"
            aplicarFiltros()
        }
        binding.chipCercanoHoteles.setOnClickListener {
            categoriaSeleccionada = "HOTELES"
            aplicarFiltros()
        }
        binding.chipCercanoGasolineras.setOnClickListener {
            categoriaSeleccionada = "GASOLINERAS"
            aplicarFiltros()
        }
        binding.chipCercanoServicios.setOnClickListener {
            categoriaSeleccionada = "SERVICIOS"
            aplicarFiltros()
        }
    }

    private fun aplicarFiltros() {
        val filtrados = listaCercanosOriginal.filter { lugar ->
            // Filtro 1: Distancia máxima seleccionada
            val cumpleDistancia = (lugar.distanciaMetros ?: 0f) <= radioMaxMetros

            // Filtro 2: Categoría seleccionada
            val cat = lugar.categoria.lowercase()
            val cumpleCategoria = when (categoriaSeleccionada) {
                "TODOS" -> true
                "COMIDA" -> cat in listOf("restaurant", "fast_food", "cafe", "bar", "pub")
                "NATURALEZA" -> cat in listOf("park", "nature", "nature_reserve")
                "HOTELES" -> cat in listOf("hotel", "hostel", "motel")
                "GASOLINERAS" -> cat == "fuel"
                "SERVICIOS" -> cat in listOf("bank", "pharmacy", "sports_centre", "gym", "stadium")
                else -> true
            }

            cumpleDistancia && cumpleCategoria
        }
        adapter.updateLugares(filtrados)
        binding.tvEmpty.visibility = if (filtrados.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun setupRecyclerView() {
        adapter = LugaresCercanosAdapter(
            emptyList(),
            null,
            onSaveFavoritoClick = { lugarCercano ->
                val guardado = lugaresViewModel.guardarLugarCercanoComoFavorito(lugarCercano)
                if (guardado) {
                    Toast.makeText(this, "⭐ '${lugarCercano.nombre}' ${getString(R.string.saved_to_favorites)}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "'${lugarCercano.nombre}' ${getString(R.string.already_in_favorites)}", Toast.LENGTH_SHORT).show()
                }
            }
        ) { lugarCercano ->
            // Abrir editor prellenado
            val intent = Intent(this, EdicionLugarActivity::class.java).apply {
                putExtra("PREFILL_NOMBRE", lugarCercano.nombre)
                putExtra("PREFILL_DIRECCION", lugarCercano.direccion)
                putExtra("PREFILL_LAT", lugarCercano.geoPunto.latitud.toString())
                putExtra("PREFILL_LON", lugarCercano.geoPunto.longitud.toString())
                putExtra("PREFILL_TIPO", mapOsmToTipoLugar(lugarCercano.categoria).ordinal)
            }
            startActivity(intent)
        }
        binding.rvLugaresCercanos.layoutManager = LinearLayoutManager(this)
        binding.rvLugaresCercanos.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.lugaresCercanos.observe(this) { lugares ->
            listaCercanosOriginal = lugares
            aplicarFiltros()
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun getFimeFallbackLocation(): Location {
        return Location("fime_uanl").apply {
            latitude = 25.7256
            longitude = -100.3152
        }
    }

    private fun requestLocation() {
        val fimeLoc = getFimeFallbackLocation()
        lastLocation = fimeLoc
        adapter.updateUserLocation(fimeLoc)
        viewModel.buscarLugaresCercanos(fimeLoc, 100000)
    }

    private fun mapOsmToTipoLugar(category: String): TipoLugar {
        return when (category.lowercase()) {
            "restaurant", "fast_food" -> TipoLugar.RESTAURANTE
            "cafe", "bar", "pub" -> TipoLugar.BAR
            "hotel", "hostel", "motel" -> TipoLugar.HOTEL
            "park", "nature", "nature_reserve" -> TipoLugar.NATURALEZA
            "sports_centre", "gym", "stadium" -> TipoLugar.DEPORTE
            "fuel" -> TipoLugar.GASOLINERA
            else -> TipoLugar.OTROS
        }
    }
}
