package com.example.mislugares.data

import android.content.Context
import android.content.SharedPreferences
import com.example.mislugares.Lugar
import com.example.mislugares.TipoLugar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Repositorio para gestionar la persistencia de los lugares.
 * Utiliza SharedPreferences y Gson para almacenamiento local simple.
 */
class LugaresRepository(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("MisLugaresPrefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val KEY_LUGARES = "lista_lugares"

    fun guardarLugares(lugares: List<Lugar>) {
        val json = gson.toJson(lugares)
        sharedPreferences.edit().putString(KEY_LUGARES, json).apply()
    }

    fun cargarLugares(): MutableList<Lugar> {
        val json = sharedPreferences.getString(KEY_LUGARES, null)
        return if (json != null) {
            val type = object : TypeToken<MutableList<Lugar>>() {}.type
            gson.fromJson(json, type)
        } else {
            // Datos iniciales por defecto si no hay nada guardado
            mutableListOf(
                Lugar("Parque Fundidora", "Col. Obrera, Monterrey", -100.28, 25.67, TipoLugar.NATURALEZA, 818126700, "https://www.parquefundidora.org", "Un gran parque industrial convertido en recreativo.", 5),
                Lugar("Macroplaza", "Centro, Monterrey", -100.31, 25.66, TipoLugar.OTROS, 0, "", "La plaza principal de Monterrey.", 4),
                Lugar("Cerro de la Silla", "Guadalupe, N.L.", -100.24, 25.63, TipoLugar.NATURALEZA, 0, "", "Icono de la ciudad.", 5),
                Lugar("Paseo Santa Lucía", "Centro, Monterrey", -100.30, 25.67, TipoLugar.NATURALEZA, 0, "", "Río artificial navegable.", 4)
            )
        }
    }
}
