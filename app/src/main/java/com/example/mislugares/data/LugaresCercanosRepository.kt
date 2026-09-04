package com.example.mislugares.data

import android.util.Log
import com.example.mislugares.GeoPunto
import com.example.mislugares.data.LugarCercano.Companion.NOMBRE_GENERICO
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Repositorio para gestionar la búsqueda de lugares cercanos usando Overpass API.
 */
class LugaresCercanosRepository {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "MisLugaresAndroidApp/1.0")
                .build()
            chain.proceed(request)
        }
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private val nominatimService: NominatimApiService by lazy {
        createService(NominatimApiService.BASE_URL, NominatimApiService::class.java)
    }

    private fun <T> createService(baseUrl: String, serviceClass: Class<T>): T {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(serviceClass)
    }

    private var cachedLugares: List<LugarCercano>? = null

    suspend fun buscarCercanos(lat: Double, lon: Double, radio: Int = 1000): Result<List<LugarCercano>> {
        val query = OverpassApiService.buildQuery(lat, lon, radio)
        
        // Intentar con el servidor primario
        return try {
            val response = createService(OverpassApiService.BASE_URL_PRIMARY, OverpassApiService::class.java).getNearbyPlaces(query)
            if (response.isSuccessful) {
                val lugares = mapResponse(response.body(), lat, lon, radio)
                cachedLugares = lugares
                Result.success(lugares)
            } else {
                // Intentar con el fallback si el primario falla
                Log.w("OverpassRepo", "Servidor primario falló (${response.code()}), intentando fallback...")
                buscarEnFallback(query, lat, lon, radio)
            }
        } catch (e: Exception) {
            Log.e("OverpassRepo", "Error en servidor primario: ${e.message}, intentando fallback...")
            buscarEnFallback(query, lat, lon, radio)
        }
    }

    private suspend fun buscarEnFallback(query: String, lat: Double, lon: Double, radio: Int): Result<List<LugarCercano>> {
        return try {
            val response = createService(OverpassApiService.BASE_URL_FALLBACK, OverpassApiService::class.java).getNearbyPlaces(query)
            if (response.isSuccessful) {
                val lugares = mapResponse(response.body(), lat, lon, radio)
                cachedLugares = lugares
                Result.success(lugares)
            } else {
                Result.failure(Exception("Error en servidor fallback: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Enriquece de forma secuencial los lugares que no tienen dirección.
     */
    suspend fun enriquecerDirecciones(lugares: List<LugarCercano>, onActualizado: (LugarCercano) -> Unit) {
        val lugaresParaEnriquecer = lugares.filter { it.direccion == null }

        for (lugar in lugaresParaEnriquecer) {
            try {
                val response = nominatimService.getReverseGeocoding(
                    lugar.geoPunto.latitud,
                    lugar.geoPunto.longitud
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    val addr = body?.address
                    if (addr != null) {
                        // Construcción de dirección: road + house_number (o fallbacks)
                        val road = addr.road
                        val number = addr.house_number
                        val zona = addr.neighbourhood ?: addr.suburb ?: addr.city ?: addr.town ?: addr.village ?: ""
                        
                        val direccionFormateada = listOfNotNull(road, number).joinToString(" ")
                        if (direccionFormateada.isNotBlank()) {
                            lugar.direccion = direccionFormateada
                        } else if (zona.isNotBlank()) {
                            lugar.direccion = zona
                        }

                        // Actualizar nombre si es NOMBRE_GENERICO
                        if (lugar.nombre == NOMBRE_GENERICO && zona.isNotBlank()) {
                            lugar.nombre = "${etiquetaCategoria(lugar.categoria)} en $zona"
                        }
                        
                        onActualizado(lugar)
                    }
                } else {
                    Log.w("Nominatim", "Error al enriquecer lugar ${lugar.osmId}: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.w("Nominatim", "Excepción al enriquecer lugar ${lugar.osmId}", e)
            }
            // CRÍTICO: Respetar políticas de uso de Nominatim (1 consulta por segundo)
            delay(1100)
        }
    }

    private fun etiquetaCategoria(categoria: String): String {
        return when (categoria.lowercase()) {
            "restaurant" -> "Restaurante"
            "cafe" -> "Cafetería"
            "bar" -> "Bar"
            "pub" -> "Pub"
            "hotel" -> "Hotel"
            "park" -> "Parque"
            "fuel" -> "Gasolinera"
            "bank" -> "Banco"
            "pharmacy" -> "Farmacia"
            "museum" -> "Museo"
            "attraction" -> "Atracción turística"
            "sports_centre" -> "Centro deportivo"
            else -> "Lugar"
        }
    }

    private fun mapResponse(
        response: OverpassResponse?,
        userLat: Double,
        userLon: Double,
        radio: Int
    ): List<LugarCercano> {
        val userPos = GeoPunto(userLon, userLat)

        return response?.elements?.mapNotNull { element ->
            val lat = element.effectiveLat
            val lon = element.effectiveLon
            
            if (lat == 0.0 && lon == 0.0) return@mapNotNull null

            val tags = element.tags ?: return@mapNotNull null
            val nombre = tags["name"] ?: tags["brand"] ?: tags["operator"] ?: NOMBRE_GENERICO
            
            // Determinar categoría basada en tags
            val categoria = tags["amenity"] ?: tags["tourism"] ?: tags["leisure"] ?: "Otros"
            
            // Dirección simple concatenando tags comunes
            val direccion = listOfNotNull(
                tags["addr:street"],
                tags["addr:housenumber"],
                tags["addr:city"]
            ).joinToString(" ").takeIf { it.isNotBlank() }

            val geoPunto = GeoPunto(lon, lat)
            val distancia = userPos.distancia(geoPunto).toFloat()

            if (distancia > radio) return@mapNotNull null

            LugarCercano(
                osmId = element.id,
                nombre = nombre,
                categoria = categoria,
                geoPunto = geoPunto,
                direccion = direccion,
                distanciaMetros = distancia
            )
        }
        ?.distinctBy {
            val latArr = (it.geoPunto.latitud * 10000).toLong()
            val lonArr = (it.geoPunto.longitud * 10000).toLong()
            "${it.nombre}-$latArr-$lonArr"
        }
        ?.sortedBy { it.distanciaMetros }
        ?: emptyList()
    }

    fun getCachedLugares(): List<LugarCercano>? = cachedLugares
}
