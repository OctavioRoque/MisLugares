package com.example.mislugares

import android.location.Location

/**
 * Utilidad centralizada para la gestión de ubicación en MisLugares.
 *
 * Resuelve el problema de la ubicación por defecto del emulador de Android Studio
 * (Mountain View, California: 37.422, -122.084) que generaba 2,430 km de distancia hacia FIME.
 *
 * Si la app corre en un emulador sin GPS configurado o sin permisos, usa FIME (0 km).
 * Si corre en un dispositivo real con GPS, usa la ubicación exacta de la persona.
 */
object LocationHelper {
    const val LAT_FIME = 25.7256
    const val LON_FIME = -100.3152

    /**
     * Retorna la ubicación de referencia fija de FIME - UANL.
     */
    fun getFimeLocation(): Location {
        return Location("fime_uanl").apply {
            latitude = LAT_FIME
            longitude = LON_FIME
            accuracy = 5f
        }
    }

    /**
     * Verifica si una ubicación corresponde a las coordenadas por defecto del emulador de Android Studio
     * (Mountain View, California / Googleplex: ~37.4220, ~-122.0841).
     */
    fun isEmulatorDefaultLocation(location: Location?): Boolean {
        if (location == null) return false
        val isMountainView = Math.abs(location.latitude - 37.4220) < 0.15 &&
                             Math.abs(location.longitude - (-122.0841)) < 0.15
        return isMountainView
    }

    /**
     * Obtiene la ubicación efectiva:
     * - Si la ubicación es nula o es la ubicación por defecto del emulador (Mountain View, CA),
     *   retorna FIME - UANL para que las distancias en Monterrey sean exactas (FIME = 0 km).
     * - Si es una ubicación real de la persona (dispositivo físico o emulador con GPS personalizado),
     *   retorna su ubicación real.
     */
    fun getEffectiveLocation(location: Location?): Location {
        if (location == null || isEmulatorDefaultLocation(location)) {
            return getFimeLocation()
        }
        return location
    }
}
