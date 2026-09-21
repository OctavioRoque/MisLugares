package com.example.mislugares.data

import com.example.mislugares.GeoPunto

/**
 * Modelo para representar un punto de interés obtenido de Overpass API.
 */
data class LugarCercano(
    val osmId: Long,
    var nombre: String = NOMBRE_GENERICO,
    val categoria: String,
    val geoPunto: GeoPunto,
    var direccion: String? = null,
    var distanciaMetros: Float? = null,
    var region: String? = null
) {
    companion object {
        const val NOMBRE_GENERICO = "Punto de interés"
    }
}
