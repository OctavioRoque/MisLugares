package com.example.mislugares

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.os.LocaleListCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mislugares.databinding.ActivityMainBinding
import com.example.mislugares.ui.EdicionLugarActivity
import com.example.mislugares.ui.FavoritosActivity
import com.example.mislugares.ui.LugaresAdapter
import com.example.mislugares.ui.LugaresViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: LugaresViewModel by viewModels()
    private lateinit var adapter: LugaresAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Variables para el Menú Desplegable
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle

    private var radioKmMaximo: Int = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupDrawer()
        setupRecyclerView()
        setupListeners()
        requestLocation()
    }

    private fun setupDrawer() {
        drawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView

        toggle = ActionBarDrawerToggle(
            this, drawerLayout, binding.toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Lógica de selección del Menú Desplegable (Favoritos, Categorías, etc.)
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_inicio -> {
                    viewModel.cargarLugares()
                }
                R.id.nav_favoritos -> {
                    val todos = viewModel.lugares.value ?: emptyList()
                    val favoritos = todos.filter { it.esFavorito() }
                    adapter.updateLugares(favoritos)
                }
                R.id.nav_filtrar_tipo -> {
                    showFilterDialog()
                }
                R.id.nav_cercanos -> {
                    startActivity(Intent(this, com.example.mislugares.ui.LugaresCercanosActivity::class.java))
                }
            }
            drawerLayout.closeDrawers()
            true
        }
    }

    private fun setupRecyclerView() {
        adapter = LugaresAdapter(emptyList()) { index ->
            val intent = Intent(this, EdicionLugarActivity::class.java)
            intent.putExtra("LUGAR_INDEX", index)
            startActivity(intent)
        }
        binding.rvSitiosInteres.layoutManager = LinearLayoutManager(this)
        binding.rvSitiosInteres.adapter = adapter

        viewModel.lugares.observe(this) { lugares ->
            adapter.updateLugares(lugares)
        }
    }

    private fun setupListeners() {
        binding.btnAnadir.setOnClickListener {
            val intent = Intent(this, EdicionLugarActivity::class.java)
            startActivity(intent)
        }

        // Control deslizante de distancia
        binding.seekBarDistancia.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                radioKmMaximo = if (progress == 0) 1 else progress
                binding.tvDistanciaLabel.text = "Radio: $radioKmMaximo km"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                Toast.makeText(this@MainActivity, "Radio ajustado a $radioKmMaximo km", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Diálogo emergente para filtrar por Categoría (TipoLugar)
    private fun showFilterDialog() {
        val nombresTipos = TipoLugar.getNombres()
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Seleccionar Categoría")
        builder.setItems(nombresTipos) { _, which ->
            val tipoSeleccionado = TipoLugar.values()[which]
            val todos = viewModel.lugares.value ?: emptyList()
            val filtrados = todos.filter { it.tipo == tipoSeleccionado }
            adapter.updateLugares(filtrados)
        }
        builder.setNeutralButton("Mostrar todos") { _, _ ->
            viewModel.cargarLugares()
        }
        builder.show()
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_favorites -> {
                startActivity(Intent(this, FavoritosActivity::class.java))
                true
            }
            R.id.action_nearby -> {
                startActivity(Intent(this, com.example.mislugares.ui.LugaresCercanosActivity::class.java))
                true
            }
            R.id.action_settings -> {
                showLanguageDialog()
                true
            }
            R.id.action_about -> {
                Toast.makeText(this, R.string.about, Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                adapter.updateUserLocation(it)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            requestLocation()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.cargarLugares()
    }
}
