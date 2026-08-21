package com.example.mislugares

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.mislugares.ui.LugarItem
import com.example.mislugares.ui.MisLugaresTheme
import com.example.mislugares.ui.PantallaLugares
import com.example.mislugares.ui.PantallaPrincipal

/**
 * Activity principal de la aplicación.
 *
 * Utiliza Jetpack Compose para renderizar las pantallas.
 * La navegación temporal entre pantallas se maneja con un estado simple;
 * esto se puede reemplazar después con Navigation Compose cuando lo integren.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MisLugaresTheme {
                // Estado simple para navegación entre pantallas
                var pantallaActual by remember { mutableStateOf("principal") }

                // Datos de ejemplo para la lista de lugares
                val lugaresEjemplo = listOf(
                    LugarItem("Parque Fundidora", "Col. Obrera, Monterrey", "2.3 km"),
                    LugarItem("Macroplaza", "Centro, Monterrey", "1.1 km"),
                    LugarItem("Cerro de la Silla", "Guadalupe, N.L.", "8.7 km"),
                    LugarItem("Paseo Santa Lucía", "Centro, Monterrey", "1.5 km")
                )

                when (pantallaActual) {
                    "principal" -> PantallaPrincipal(
                        onCorreoClick = {
                            Toast.makeText(this, "Correo", Toast.LENGTH_SHORT).show()
                        },
                        onMostrarLugaresClick = {
                            pantallaActual = "lugares"
                        },
                        onPreferenciasClick = {
                            Toast.makeText(this, "Preferencias", Toast.LENGTH_SHORT).show()
                        },
                        onAcercaDeClick = {
                            Toast.makeText(this, "Acerca de", Toast.LENGTH_SHORT).show()
                        },
                        onSalirClick = {
                            finish()
                        }
                    )

                    "lugares" -> PantallaLugares(
                        lugares = lugaresEjemplo,
                        onHomeClick = {
                            pantallaActual = "principal"
                        },
                        onLugarClick = { lugar ->
                            Toast.makeText(this, lugar.nombre, Toast.LENGTH_SHORT).show()
                        },
                        onCargarMasClick = {
                            Toast.makeText(this, "Cargar más", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}
