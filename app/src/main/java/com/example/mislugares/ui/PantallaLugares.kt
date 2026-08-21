package com.example.mislugares.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// --- Modelo de datos simple para representar un lugar ---
data class LugarItem(
    val nombre: String,
    val ubicacion: String,
    val distancia: String
)

// Los colores se importan desde MisLugaresColors en Theme.kt

/**
 * Pantalla principal que muestra la lista de lugares.
 *
 * Es una función @Composable sin estado: toda la información
 * llega por parámetros y todos los eventos salen por lambdas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaLugares(
    lugares: List<LugarItem>,
    onHomeClick: () -> Unit,
    onLugarClick: (LugarItem) -> Unit,
    onCargarMasClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "LUGARES DE MONTERREY",
                        color = MisLugaresColors.OnPrimary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onHomeClick) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Inicio",
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
        // Contenido principal con fondo beige
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MisLugaresColors.Background)
                .padding(paddingValues)
        ) {
            // Lista de tarjetas de lugares
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(lugares) { lugar ->
                    ItemLugarCard(
                        lugar = lugar,
                        onLugarClick = onLugarClick
                    )
                }
            }

            // Botón "Cargar más" fijo en la parte inferior
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = onCargarMasClick,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MisLugaresColors.Outline),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MisLugaresColors.Primary
                    )
                ) {
                    Text(
                        text = "CARGAR MÁS",
                        color = MisLugaresColors.OnPrimary
                    )
                }
            }
        }
    }
}

/**
 * Tarjeta individual que representa un lugar en la lista.
 *
 * Muestra un placeholder de imagen, nombre, ubicación y distancia.
 */
@Composable
fun ItemLugarCard(
    lugar: LugarItem,
    onLugarClick: (LugarItem) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onLugarClick(lugar) },
        colors = CardDefaults.cardColors(
            containerColor = MisLugaresColors.Primary
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, MisLugaresColors.Outline)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Placeholder de imagen
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MisLugaresColors.OnPrimary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "FOTO",
                    color = MisLugaresColors.Outline,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Información del lugar
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = lugar.nombre,
                    color = MisLugaresColors.OnPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Text(
                    text = lugar.ubicacion,
                    color = MisLugaresColors.OnPrimary.copy(alpha = 0.85f),
                    fontSize = 13.sp
                )

                // Fila con ícono de ubicación y distancia
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Distancia",
                        tint = MisLugaresColors.OnPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = lugar.distancia,
                        color = MisLugaresColors.OnPrimary,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

// --- Vista previa con datos de ejemplo ---
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PantallaLugaresPreview() {
    val sampleLugares = listOf(
        LugarItem("Parque Fundidora", "Col. Obrera, Monterrey", "2.3 km"),
        LugarItem("Macroplaza", "Centro, Monterrey", "1.1 km"),
        LugarItem("Cerro de la Silla", "Guadalupe, N.L.", "8.7 km"),
        LugarItem("Paseo Santa Lucía", "Centro, Monterrey", "1.5 km")
    )

    PantallaLugares(
        lugares = sampleLugares,
        onHomeClick = {},
        onLugarClick = {},
        onCargarMasClick = {}
    )
}
