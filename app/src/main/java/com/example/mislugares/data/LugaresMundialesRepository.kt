package com.example.mislugares.data

import com.example.mislugares.GeoPunto
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Repositorio para buscar lugares de interés real (museos, teatros, monumentos)
 * utilizando la API de Overpass de OpenStreetMap.
 */
class LugaresMundialesRepository {

    private fun createApiService(baseUrl: String): OverpassApiService {
        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .header("Accept-Language", "es")
                    .build()
                chain.proceed(request)
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OverpassApiService::class.java)
    }

    private var apiService = createApiService(OverpassApiService.BASE_URL_PRIMARY)

    /**
     * Busca POIs de "verdadero interés" en un radio determinado.
     * Filtra categorías aburridas como farmacias, gasolineras o bancos.
     */
    suspend fun buscarInteresMundial(lat: Double, lon: Double, radio: Int = 3000): Result<List<LugarCercano>> {
        return try {
            val query = buildInteresQuery(lat, lon, radio)
            var response = apiService.getNearbyPlaces(query)
            
            // Si falla el servidor primario o hay timeout, intentamos con el fallback
            if (!response.isSuccessful) {
                apiService = createApiService(OverpassApiService.BASE_URL_FALLBACK)
                response = apiService.getNearbyPlaces(query)
            }

            if (response.isSuccessful) {
                val elements = response.body()?.elements ?: emptyList()
                val list = elements.map { element ->
                    val tags = element.tags ?: emptyMap()
                    val nombre = tags["name:es"] ?: tags["name"] ?: LugarCercano.NOMBRE_GENERICO
                    
                    // Prioridad de categoría
                    val categoria = tags["tourism"] ?: tags["historic"] ?: tags["amenity"] ?: "monument"
                    
                    val gp = GeoPunto(element.effectiveLon, element.effectiveLat)
                    
                    val city = tags["addr:city:es"] ?: tags["addr:city"]
                    val country = tags["addr:country:es"] ?: tags["addr:country"]
                    val regionInfo = when {
                        !city.isNullOrBlank() && !country.isNullOrBlank() -> "$city, $country"
                        !city.isNullOrBlank() -> city
                        !country.isNullOrBlank() -> country
                        else -> null
                    }

                    LugarCercano(
                        osmId = element.id,
                        nombre = nombre,
                        categoria = categoria,
                        geoPunto = gp,
                        direccion = tags["addr:street"]?.let { street -> 
                            "$street ${tags["addr:housenumber"] ?: ""}".trim() 
                        } ?: tags["operator"],
                        region = regionInfo
                    )
                }
                Result.success(list)
            } else {
                Result.failure(Exception("Error Overpass: ${response.code()}"))
            }
        } catch (e: Exception) {
            // En caso de Exception (como Timeout), intentamos forzar el cambio de servidor para la próxima
            apiService = createApiService(OverpassApiService.BASE_URL_FALLBACK)
            Result.failure(e)
        }
    }

    private fun buildInteresQuery(lat: Double, lon: Double, radius: Int): String {
        // Query ultra optimizada: Menos categorías y radio reducido para evitar timeout.
        // Limitamos a 50 resultados para que el servidor responda rápido.
        return "[out:json][timeout:25];(" +
                "node[\"tourism\"~\"museum|viewpoint|gallery\"](around:$radius,$lat,$lon);" +
                "node[\"historic\"~\"monument|castle|ruins\"](around:$radius,$lat,$lon);" +
                "node[\"amenity\"~\"theatre\"](around:$radius,$lat,$lon);" +
                "way[\"tourism\"~\"museum|viewpoint|gallery\"](around:$radius,$lat,$lon);" +
                "way[\"historic\"~\"monument|castle|ruins\"](around:$radius,$lat,$lon);" +
                ");out center 50;"
    }
}
