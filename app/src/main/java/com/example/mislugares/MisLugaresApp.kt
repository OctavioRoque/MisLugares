package com.example.mislugares

import android.app.Application
import org.maplibre.android.MapLibre

/**
 * Clase Application para inicializaciones globales.
 */
class MisLugaresApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Inicializa MapLibre Native para mapas vectoriales OpenStreetMap (OpenFreeMap)
        MapLibre.getInstance(this)
    }
}
