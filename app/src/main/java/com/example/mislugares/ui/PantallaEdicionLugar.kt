package com.example.mislugares.ui

import android.widget.ArrayAdapter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidViewBinding
import com.example.mislugares.Lugar
import com.example.mislugares.TipoLugar
import com.example.mislugares.databinding.EdicionLugarBinding

/**
 * Pantalla para añadir o editar un lugar.
 * Utiliza ViewBinding para integrar el layout XML existente.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaEdicionLugar(
    lugarIndex: Int?,
    viewModel: LugaresViewModel,
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    val lugar = lugarIndex?.let { viewModel.getLugar(it) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (lugar == null) "NUEVO LUGAR" else "EDITAR LUGAR",
                        color = MisLugaresColors.OnPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Atrás",
                            tint = MisLugaresColors.OnPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MisLugaresColors.Primary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MisLugaresColors.Background)
                .padding(paddingValues)
        ) {
            AndroidViewBinding(EdicionLugarBinding::inflate) {
                // Configurar Spinner de Tipos
                val adapter = ArrayAdapter(
                    root.context,
                    android.R.layout.simple_spinner_item,
                    TipoLugar.getNombres()
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                tipo.adapter = adapter

                // Si estamos editando, rellenar campos
                lugar?.let { l ->
                    nombre.setText(l.nombre)
                    direccion.setText(l.direccion)
                    telefono.setText(l.telefono.toString())
                    url.setText(l.url)
                    comentario.setText(l.comentario)
                    tipo.setSelection(l.tipo.ordinal)
                }

                // Configurar acción de guardado
                btnGuardar.setOnClickListener {
                    val nuevoLugar = Lugar(
                        nombre.text.toString(),
                        direccion.text.toString(),
                        lugar?.posicion?.longitud ?: 0.0,
                        lugar?.posicion?.latitud ?: 0.0,
                        TipoLugar.values()[tipo.selectedItemPosition],
                        telefono.text.toString().toIntOrNull() ?: 0,
                        url.text.toString(),
                        comentario.text.toString(),
                        lugar?.valoracion?.toInt() ?: 3
                    )

                    if (lugarIndex == null) {
                        viewModel.addLugar(nuevoLugar)
                    } else {
                        viewModel.updateLugar(lugarIndex, nuevoLugar)
                    }
                    onSaveClick()
                }
            }
        }
    }
}
