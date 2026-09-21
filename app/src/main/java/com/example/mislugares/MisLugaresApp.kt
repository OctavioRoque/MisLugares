package com.example.mislugares

import android.app.Application
import org.osmdroid.config.Configuration
import android.preference.PreferenceManager

/**
 * Clase Application para inicializaciones globales.
 */
class MisLugaresApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Configuración de osmdroid según la política de OpenStreetMap
        // El User-Agent debe ser único para evitar el bloqueo 403
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        Configuration.getInstance().userAgentValue = "MisLugaresApp/1.0 (com.example.mislugares)"
    }
}
