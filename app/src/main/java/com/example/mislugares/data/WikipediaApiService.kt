package com.example.mislugares.data

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Data Transfer Objects para la respuesta de la API de Wikipedia (Geosearch).
 */
data class WikipediaResponse(
    val query: WikipediaQuery?
)

data class WikipediaQuery(
    val geosearch: List<GeosearchItem>?
)

data class GeosearchItem(
    val pageid: Long,
    val title: String,
    val lat: Double,
    val lon: Double,
    val dist: Double,
    val ns: Int
)

/**
 * Interfaz Retrofit para interactuar con la API de Wikipedia.
 */
interface WikipediaApiService {

    @GET("w/api.php?action=query&list=geosearch&format=json")
    suspend fun getNearbyPlaces(
        @Query("gscoord") coords: String, // formato "lat|lon"
        @Query("gsradius") radius: Int,
        @Query("gslimit") limit: Int = 50
    ): Response<WikipediaResponse>

    companion object {
        const val BASE_URL = "https://es.wikipedia.org/"
    }
}
