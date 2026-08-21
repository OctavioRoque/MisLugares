package com.example.mislugares.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Pantalla principal de la aplicación MisLugares.
 *
 * Composable sin estado que muestra el menú principal con opciones
 * para ver lugares, preferencias, acerca de, enviar correo y salir.
 * Toda la navegación se delega mediante callbacks lambda.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPrincipal(
    onCorreoClick: () -> Unit,
    onMostrarLugaresClick: () -> Unit,
    onPreferenciasClick: () -> Unit,
    onAcercaDeClick: () -> Unit,
    onSalirClick: () -> Unit
) {
    Scaffold(
        topBar = {
            // Barra superior con fondo verde oliva y botón de correo a la derecha
            CenterAlignedTopAppBar(
                title = { /* Sin título */ },
                actions = {
                    IconButton(onClick = onCorreoClick) {
                        Icon(
                            imageVector = Icons.Outlined.Email,
                            contentDescription = "Enviar correo",
                            tint = MisLugaresColors.OnPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MisLugaresColors.Primary
                )
            )
        }
    ) { paddingValues ->
        // Contenido principal con fondo beige claro
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MisLugaresColors.Background)
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Título de bienvenida
            Text(
                text = "BIENVENIDO",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MisLugaresColors.TextPrimary,
                letterSpacing = 4.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Botón principal — Mostrar Lugares
            Button(
                onClick = onMostrarLugaresClick,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, MisLugaresColors.Outline),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MisLugaresColors.Primary,
                    contentColor = MisLugaresColors.OnPrimary
                ),
                modifier = Modifier.padding(horizontal = 32.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(
                        horizontal = 24.dp,
                        vertical = 12.dp
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Ubicación",
                        tint = MisLugaresColors.OnPrimary,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "MOSTRAR",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "LUGARES",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botones secundarios — Preferencias y Acerca de
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onPreferenciasClick,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MisLugaresColors.Outline),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MisLugaresColors.Primary,
                        contentColor = MisLugaresColors.OnPrimary
                    )
                ) {
                    Text(
                        text = "PREFERENCIAS",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(
                            horizontal = 20.dp,
                            vertical = 10.dp
                        )
                    )
                }

                Button(
                    onClick = onAcercaDeClick,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MisLugaresColors.Outline),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MisLugaresColors.Primary,
                        contentColor = MisLugaresColors.OnPrimary
                    )
                ) {
                    Text(
                        text = "ACERCA DE",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(
                            horizontal = 20.dp,
                            vertical = 10.dp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón terciario — Salir
            Button(
                onClick = onSalirClick,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MisLugaresColors.Outline),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MisLugaresColors.Primary,
                    contentColor = MisLugaresColors.OnPrimary
                )
            ) {
                Text(
                    text = "SALIR",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(
                        horizontal = 20.dp,
                        vertical = 10.dp
                    )
                )
            }
        }
    }
}

// Vista previa de la pantalla principal
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PantallaPrincipalPreview() {
    PantallaPrincipal(
        onCorreoClick = {},
        onMostrarLugaresClick = {},
        onPreferenciasClick = {},
        onAcercaDeClick = {},
        onSalirClick = {}
    )
}
