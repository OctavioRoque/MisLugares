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
        const val BASE_URL_FALLBACK = "https://overpass.kumi.systems/api/"

        /**
         * Construye una query Overpass QL para buscar POIs en un radio.
         */
        fun buildQuery(lat: Double, lon: Double, radius: Int = 1000): String {
            // Categorías a buscar (amenity, tourism, leisure)
            val categories = listOf(
                "amenity=restaurant", "amenity=cafe", "amenity=bar", "amenity=pub",
                "leisure=park", "tourism=museum", "tourism=attraction", "tourism=hotel",
                "amenity=fuel", "amenity=bank", "amenity=pharmacy", "leisure=sports_centre"
            )

            val filter = categories.joinToString("") { "node[$it](around:$radius,$lat,$lon);" } +
                    categories.joinToString("") { "way[$it](around:$radius,$lat,$lon);" }

            return "[out:json][timeout:25];($filter);out center;"
        }
    }
}