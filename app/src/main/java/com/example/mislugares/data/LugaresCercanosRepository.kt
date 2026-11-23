package com.example.mislugares.data

import com.example.mislugares.GeoPunto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import android.location.Location

class LugaresCercanosRepository {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "MisLugaresApp/1.0 (roque.octavio@gmail.com)")
                .build()
            chain.proceed(request)
        }
        .build()

    private val apiServicePrimary: OverpassApiService by lazy {
        Retrofit.Builder()
            .baseUrl(OverpassApiService.BASE_URL_PRIMARY)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OverpassApiService::class.java)
    }

    private val apiServiceFallback: OverpassApiService by lazy {
        Retrofit.Builder()
            .baseUrl(OverpassApiService.BASE_URL_FALLBACK)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OverpassApiService::class.java)
    }

    private var cachedLugares: List<LugarCercano>? = null

    private fun calcularDistancia(startLat: Double, startLon: Double, endLat: Double, endLon: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(startLat, startLon, endLat, endLon, results)
        return results[0]
    }

    suspend fun buscarCercanos(lat: Double, lon: Double, radio: Int = 1000): Result<List<LugarCercano>> {
        return withContext(Dispatchers.IO) {
            val query = OverpassApiService.buildQuery(lat, lon, radio)
            
            try {
                var response = apiServicePrimary.getNearbyPlaces(query)
                if (!response.isSuccessful) {
                    response = apiServiceFallback.getNearbyPlaces(query)
                }

                if (response.isSuccessful) {
                    val elements = response.body()?.elements ?: emptyList()
                    val lugaresReales = elements.mapNotNull { el ->
                        val name = el.tags?.get("name")
                        if (name.isNullOrBlank()) return@mapNotNull null
                        val category = el.tags["amenity"] ?: el.tags["leisure"] ?: el.tags["tourism"] ?: "unknown"
                        val street = el.tags["addr:street"]
                        val housenumber = el.tags["addr:housenumber"]
                        val address = if (street != null) {
                            if (housenumber != null) "$street $housenumber" else street
                        } else {
                            "Cerca de tu ubicación"
                        }
                        
                        val distancia = calcularDistancia(lat, lon, el.effectiveLat, el.effectiveLon)
                        
                        LugarCercano(
                            osmId = el.id,
                            nombre = name,
                            categoria = category,
                            geoPunto = GeoPunto(el.effectiveLon, el.effectiveLat),
                            direccion = address,
                            distanciaMetros = distancia,
                            region = "Local"
                        )
                    }
                    
                    val sorted = lugaresReales.sortedBy { it.distanciaMetros }.take(100)
                    cachedLugares = sorted
                    Result.success(sorted)
                } else {
                    Result.failure(Exception("Error OSM: ${response.code()}"))
                }
            } catch (e: Exception) {
                try {
                    val fbResp = apiServiceFallback.getNearbyPlaces(query)
                    if (fbResp.isSuccessful) {
                        val elements = fbResp.body()?.elements ?: emptyList()
                        val lugaresReales = elements.mapNotNull { el ->
                            val name = el.tags?.get("name")
                            if (name.isNullOrBlank()) return@mapNotNull null
                            val category = el.tags["amenity"] ?: el.tags["leisure"] ?: el.tags["tourism"] ?: "unknown"
                            val distancia = calcularDistancia(lat, lon, el.effectiveLat, el.effectiveLon)
                            LugarCercano(osmId = el.id, nombre = name, categoria = category, geoPunto = GeoPunto(el.effectiveLon, el.effectiveLat), direccion = "Cerca", distanciaMetros = distancia, region = "Local")
                        }
                        val sorted = lugaresReales.sortedBy { it.distanciaMetros }.take(100)
                        cachedLugares = sorted
                        return@withContext Result.success(sorted)
                    }
                } catch (e2: Exception) {
                    android.util.Log.e("LugaresCercanos", "Fallback falló: ${e2.message}")
                }
                cachedLugares?.let { Result.success(it) } ?: Result.failure(e)
            }
        }
    }
}
