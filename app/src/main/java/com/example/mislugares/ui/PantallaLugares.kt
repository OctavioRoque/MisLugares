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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import com.example.mislugares.Lugar
import com.example.mislugares.TipoLugar

/**
 * Pantalla que muestra la lista de lugares guardados.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaLugares(
    lugares: List<Lugar>,
    onHomeClick: () -> Unit,
    onLugarClick: (Int) -> Unit,
    onAddLugarClick: () -> Unit,
    onCargarMasClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "MIS LUGARES",
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddLugarClick,
                containerColor = MisLugaresColors.Primary,
                contentColor = MisLugaresColors.OnPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Añadir lugar")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MisLugaresColors.Background)
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(lugares) { index, lugar ->
                    ItemLugarCard(
                        lugar = lugar,
                        onLugarClick = { onLugarClick(index) }
                    )
                }
            }

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

@Composable
fun ItemLugarCard(
    lugar: Lugar,
    onLugarClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onLugarClick() },
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
                    text = lugar.direccion,
                    color = MisLugaresColors.OnPrimary.copy(alpha = 0.85f),
                    fontSize = 13.sp
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Tipo",
                        tint = MisLugaresColors.OnPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = lugar.tipo.texto,
                        color = MisLugaresColors.OnPrimary,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PantallaLugaresPreview() {
    val sampleLugares = listOf(
        Lugar("Parque Fundidora", "Col. Obrera, Monterrey", -100.28, 25.67, TipoLugar.NATURALEZA, 818126700, "", "", 5),
        Lugar("Macroplaza", "Centro, Monterrey", -100.31, 25.66, TipoLugar.OTROS, 0, "", "", 4)
    )

    PantallaLugares(
        lugares = sampleLugares,
        onHomeClick = {},
        onLugarClick = {},
        onAddLugarClick = {},
        onCargarMasClick = {}
    )
}
