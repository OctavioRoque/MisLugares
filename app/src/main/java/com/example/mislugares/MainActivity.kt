package com.example.mislugares

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.mislugares.ui.LugaresViewModel
import com.example.mislugares.ui.MisLugaresTheme
import com.example.mislugares.ui.PantallaEdicionLugar
import com.example.mislugares.ui.PantallaLugares
import com.example.mislugares.ui.PantallaPrincipal

/**
 * Activity principal de la aplicación.
 */
class MainActivity : ComponentActivity() {
    private val viewModel: LugaresViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MisLugaresTheme {
                // Estado simple para navegación entre pantallas
                var pantallaActual by remember { mutableStateOf("principal") }
                var lugarSeleccionadoIndex by remember { mutableStateOf<Int?>(null) }

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
                        lugares = viewModel.lugares,
                        onHomeClick = {
                            pantallaActual = "principal"
                        },
                        onLugarClick = { index ->
                            lugarSeleccionadoIndex = index
                            pantallaActual = "edicion"
                        },
                        onAddLugarClick = {
                            lugarSeleccionadoIndex = null
                            pantallaActual = "edicion"
                        },
                        onCargarMasClick = {
                            Toast.makeText(this, "Cargar más", Toast.LENGTH_SHORT).show()
                        }
                    )

                    "edicion" -> PantallaEdicionLugar(
                        lugarIndex = lugarSeleccionadoIndex,
                        viewModel = viewModel,
                        onBackClick = {
                            pantallaActual = "lugares"
                        },
                        onSaveClick = {
                            pantallaActual = "lugares"
                            Toast.makeText(this, "Lugar guardado", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}
