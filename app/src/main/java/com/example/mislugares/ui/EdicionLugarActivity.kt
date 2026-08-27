package com.example.mislugares.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.mislugares.GeoPunto
import com.example.mislugares.Lugar
import com.example.mislugares.TipoLugar
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
        super.onCreate(savedInstanceState)
        binding = ActivityEdicionLugarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        lugarIndex = intent.getIntExtra("LUGAR_INDEX", -1)

        setupSpinner()
        loadLugarData()
        requestLocation()

        binding.btnGuardar.setOnClickListener { saveLugar() }
        binding.btnVerCamino.setOnClickListener { verCamino() }
    }

    private fun setupSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, TipoLugar.values().map { it.texto })
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

    private fun requestLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    currentGPS = GeoPunto(it.longitude, it.latitude)
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
            binding.nombre.error = "El nombre es obligatorio"
            return
        }

        // Si no hay GPS previo y capturamos uno nuevo, lo usamos
        val posicion = currentGPS ?: GeoPunto(0.0, 0.0)

        val nuevoLugar = Lugar(nombre, direccion, posicion.longitud, posicion.latitud, tipo, telefono, url, comentario, 0)

        if (lugarIndex == -1) {
            viewModel.addLugar(nuevoLugar)
            Toast.makeText(this, "Lugar añadido", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.updateLugar(lugarIndex, nuevoLugar)
            Toast.makeText(this, "Lugar actualizado", Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    private fun verCamino() {
        val pos = currentGPS ?: return
        val uri = Uri.parse("geo:${pos.latitud},${pos.longitud}?q=${Uri.encode(binding.nombre.text.toString())}")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.google.android.apps.maps")
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            // Fallback si Google Maps no está instalado
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }
}