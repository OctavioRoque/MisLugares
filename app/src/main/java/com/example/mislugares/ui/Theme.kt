package com.example.mislugares.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

// ── Paleta de colores personalizada ──────────────────────────────────────────

/**
 * Colores principales de la aplicación MisLugares.
 *
 * La paleta está inspirada en tonos naturales y cálidos
 * que evocan un cuaderno de viaje o boceto a mano.
 */
object MisLugaresColors {
    /** Fondo general — beige claro */
    val Background = Color(0xFFF4F0E6)

    /** Color primario — verde oliva */
    val Primary = Color(0xFF8A9A73)

    /** Texto/icono sobre el color primario — blanco */
    val OnPrimary = Color(0xFFFFFFFF)

    /** Bordes y trazos estilo boceto — negro casi puro */
    val Outline = Color(0xFF1A1A1A)

    /** Texto principal sobre fondo claro — negro suave */
    val TextPrimary = Color(0xFF1C1C1C)
}

// ── Esquema de colores Material 3 ────────────────────────────────────────────

/**
 * Esquema claro construido a partir de [MisLugaresColors].
 *
 * Se asignan los roles semánticos de Material 3 usando
 * los valores de la paleta personalizada.
 */
private val LightColorScheme = lightColorScheme(
    primary = MisLugaresColors.Primary,
    onPrimary = MisLugaresColors.OnPrimary,
    background = MisLugaresColors.Background,
    surface = MisLugaresColors.Background,
    onBackground = MisLugaresColors.TextPrimary,
    onSurface = MisLugaresColors.TextPrimary,
    outline = MisLugaresColors.Outline,
)

// ── Tema principal ───────────────────────────────────────────────────────────

/**
 * Tema raíz de MisLugares.
 *
 * Envuelve el contenido en un [MaterialTheme] con la paleta personalizada
 * y lo coloca dentro de un [Surface] que ocupa toda la pantalla,
 * garantizando que el color de fondo se aplique correctamente.
 *
 * Uso típico:
 * ```
 * MisLugaresTheme {
 *     // Pantallas y componentes de la app
 * }
 * ```
 *
 * @param content Contenido composable que recibirá el tema.
 */
@Composable
fun MisLugaresTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        // TODO: Definir tipografía personalizada cuando se elijan las fuentes
        // typography = MisLugaresTypography,
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            content = content,
        )
    }
}
