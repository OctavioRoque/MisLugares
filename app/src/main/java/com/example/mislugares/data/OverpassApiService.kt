package com.example.mislugares.data

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

/**
 * Data Transfer Objects para la respuesta de Overpass API.
 */
data class OverpassResponse(
    val elements: List<Element>
)

data class Center(val lat: Double, val lon: Double)

data class Element(
    val id: Long,
    val type: String?,
    val lat: Double?,
    val lon: Double?,
    val center: Center?,
    val tags: Map<String, String>?
) {
    val effectiveLat: Double get() = lat ?: center?.lat ?: 0.0
    val effectiveLon: Double get() = lon ?: center?.lon ?: 0.0
}

/**
 * Interfaz Retrofit para interactuar con la API de Overpass (OpenStreetMap).
 */
interface OverpassApiService {

    @FormUrlEncoded
    @POST("interpreter")
    suspend fun getNearbyPlaces(@Field("data") query: String): Response<OverpassResponse>

    companion object {
        const val BASE_URL_PRIMARY = "https://overpass-api.de/api/"
        const val BASE_URL_SECONDARY = "https://overpass.kumi.systems/api/"
        const val BASE_URL_TERTIARY = "https://overpass.openstreetmap.fr/api/"

        /**
         * Construye una query Overpass QL compacta y rápida para buscar POIs en un radio.
         */
        fun buildQuery(lat: Double, lon: Double, radius: Int = 1000): String {
            return "[out:json][timeout:10];(" +
                    "node[\"amenity\"~\"restaurant|fast_food|cafe|bar|pub|fuel|bank|pharmacy\"](around:$radius,$lat,$lon);" +
                    "node[\"leisure\"~\"park|sports_centre\"](around:$radius,$lat,$lon);" +
                    "node[\"tourism\"~\"museum|attraction|hotel\"](around:$radius,$lat,$lon);" +
                    "way[\"amenity\"~\"restaurant|fast_food|cafe|bar|pub|fuel|bank|pharmacy\"](around:$radius,$lat,$lon);" +
                    "way[\"leisure\"~\"park|sports_centre\"](around:$radius,$lat,$lon);" +
                    "way[\"tourism\"~\"museum|attraction|hotel\"](around:$radius,$lat,$lon);" +
                    ");out center;"
        }
    }
}