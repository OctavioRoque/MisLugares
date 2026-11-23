package com.example.mislugares.data

import android.content.Context
import android.content.SharedPreferences
import com.example.mislugares.LocationHelper
import com.example.mislugares.Lugar
import com.example.mislugares.TipoLugar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Repositorio para gestionar la persistencia de los lugares.
 * Utiliza SharedPreferences y Gson para almacenamiento local.
 */
class LugaresRepository(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("MisLugaresPrefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val KEY_LUGARES = "lista_lugares_v3" // Clave actualizada para cargar FIME UANL
    private val KEY_COORDS_MIGRATED_V4 = "coords_osm_precision_v4"

    fun guardarLugares(lugares: List<Lugar>) {
        val json = gson.toJson(lugares)
        sharedPreferences.edit().putString(KEY_LUGARES, json).apply()
    }

    fun cargarLugares(): MutableList<Lugar> {
        val json = sharedPreferences.getString(KEY_LUGARES, null)
        val lista = if (json != null) {
            val type = object : TypeToken<MutableList<Lugar>>() {}.type
            gson.fromJson<MutableList<Lugar>>(json, type) ?: mutableListOf()
        } else {
            mutableListOf()
        }

        // Si la lista está vacía o ninguno está marcado como favorito, cargamos los lugares icónicos de Monterrey en favoritos
        if (lista.isEmpty() || lista.none { it.esFavorito() }) {
            val monterreyLugares = getLugaresMonterrey()
            guardarLugares(monterreyLugares)
            sharedPreferences.edit().putBoolean(KEY_COORDS_MIGRATED_V4, true).apply()
            return monterreyLugares
        }

        // Migración no destructiva: corrige coordenadas desfasadas de los lugares precargados sin borrar los del usuario
        if (!sharedPreferences.getBoolean(KEY_COORDS_MIGRATED_V4, false)) {
            val exactCoordsByName = getLugaresMonterrey().associateBy { it.nombre }
            var modified = false
            lista.forEach { lugar ->
                val exact = exactCoordsByName[lugar.nombre]
                if (exact != null) {
                    lugar.latitud = exact.latitud
                    lugar.longitud = exact.longitud
                    modified = true
                }
            }
            if (modified) {
                guardarLugares(lista)
            }
            sharedPreferences.edit().putBoolean(KEY_COORDS_MIGRATED_V4, true).apply()
        }

        return lista
    }

    private fun getLugaresMonterrey(): MutableList<Lugar> {
        // Coordenadas exactas verificadas en OpenStreetMap
        return mutableListOf(
            Lugar(
                "FIME - UANL",
                "Av. Universidad s/n, Ciudad Universitaria, San Nicolás de los Garza, N.L.",
                LocationHelper.LON_FIME,
                LocationHelper.LAT_FIME,
                TipoLugar.EDUCACION,
                818329400,
                "https://www.fime.uanl.mx",
                "Facultad de Ingeniería Mecánica y Eléctrica de la UANL. Campus Ciudad Universitaria.",
                5,
                true
            ),
            Lugar(
                "Parque Fundidora",
                "Avenida Fundidora y Adolfo Prieto, Col. Obrera, Monterrey, N.L.",
                -100.282186,
                25.677103,
                TipoLugar.NATURALEZA,
                818126700,
                "https://www.parquefundidora.org",
                "Parque urbano e histórico industrial icónico con pista, lagos y áreas culturales.",
                5,
                true
            ),
            Lugar(
                "Paseo Santa Lucía",
                "Dr. José Ma. Coss 445, Centro, Monterrey, N.L.",
                -100.307105,
                25.671526,
                TipoLugar.NATURALEZA,
                818126700,
                "https://www.monterrey.gob.mx",
                "Hermoso canal navegable artificial de 2.5 km que une la Macroplaza con Fundidora.",
                5,
                true
            ),
            Lugar(
                "Macroplaza (Gran Plaza)",
                "Zaragoza y 5 de Mayo, Centro, Monterrey, N.L.",
                -100.309239,
                25.670815,
                TipoLugar.OTROS,
                0,
                "",
                "La plaza cívica principal de Monterrey con el Faro del Comercio y la Fuente de Neptuno.",
                5,
                true
            ),
            Lugar(
                "Cerro de la Silla",
                "Monumento Natural Cerro de la Silla, Guadalupe / Monterrey, N.L.",
                -100.236544,
                25.607955,
                TipoLugar.NATURALEZA,
                0,
                "",
                "El majestuoso monumento natural y máxima insignia geográfica de los regiomontanos.",
                5,
                true
            ),
            Lugar(
                "Museo de Arte Contemporáneo (MARCO)",
                "Juan Zuazua y Padre Raymundo Jardón, Centro, Monterrey, N.L.",
                -100.309825,
                25.664613,
                TipoLugar.ESPECTACULO,
                818262450,
                "https://www.marco.org.mx",
                "Uno de los museos de arte contemporáneo más prestigiosos de México y América Latina.",
                5,
                true
            ),
            Lugar(
                "Mirador del Obispado",
                "Rafael José Verger s/n, Col. Obispado, Monterrey, N.L.",
                -100.346481,
                25.676305,
                TipoLugar.NATURALEZA,
                0,
                "",
                "Asta bandera monumental con vista panorámica espectacular de 360 grados de la ciudad y las montañas.",
                5,
                true
            ),
            Lugar(
                "Parque Ecológico Chipinque",
                "Carretera a Chipinque Km 2.5, San Pedro Garza García, N.L.",
                -100.355432,
                25.607759,
                TipoLugar.NATURALEZA,
                818303000,
                "https://www.chipinque.org.mx",
                "Espectacular reserva ecológica en la Sierra Madre Oriental ideal para senderismo y miradores.",
                5,
                true
            ),
            Lugar(
                "Barrio Antiguo",
                "Calle Morelos y Padre Mier, Centro Histórico, Monterrey, N.L.",
                -100.306786,
                25.665527,
                TipoLugar.BAR,
                0,
                "",
                "Corazón bohemio y colonial de Monterrey con cafeterías, galerías de arte, bares y música en vivo.",
                5,
                true
            ),
            Lugar(
                "El Rey del Cabrito",
                "Av. Constitución 817 Ote., Centro, Monterrey, N.L.",
                -100.309211,
                25.663725,
                TipoLugar.RESTAURANTE,
                818345323,
                "",
                "Restaurante emblemático para disfrutar el tradicional cabrito al pastor y cortes regiomontanos.",
                5,
                true
            ),
            Lugar(
                "Grutas de García",
                "Carretera a Villa de García Km 10, García, N.L.",
                -100.523834,
                25.849356,
                TipoLugar.NATURALEZA,
                812033840,
                "",
                "Impresionantes cavernas subterráneas milenarias con estalagmitas y teleférico panorámico.",
                5,
                true
            )
        )
    }
}
