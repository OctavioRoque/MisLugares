package com.example.mislugares.data

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

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

interface OverpassApiService {
    @FormUrlEncoded
    @POST("interpreter")
    suspend fun getNearbyPlaces(@Field("data") query: String): Response<OverpassResponse>

    companion object {
        const val BASE_URL_PRIMARY = "https://overpass-api.de/api/"
        const val BASE_URL_FALLBACK = "https://overpass.kumi.systems/api/"

        fun buildQuery(lat: Double, lon: Double, radius: Int = 1000): String {
            // Simplificado sin expresiones regulares (~), mucho más rápido
            return "[out:json][timeout:10];(" +
                    "node[\"amenity\"](around:$radius,$lat,$lon);" +
                    "node[\"leisure\"](around:$radius,$lat,$lon);" +
                    "node[\"tourism\"](around:$radius,$lat,$lon);" +
                    ");out center;"
        }
    }
}
