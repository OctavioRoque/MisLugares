package com.example.mislugares.data

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface NominatimApiService {
    @GET("reverse")
    suspend fun getReverseGeocoding(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("format") format: String = "jsonv2",
        @Query("zoom") zoom: Int = 18,
        @Query("addressdetails") addressDetails: Int = 1
    ): Response<NominatimResponse>

    companion object {
        const val BASE_URL = "https://nominatim.openstreetmap.org/"
    }
}

data class NominatimResponse(
    val display_name: String? = null,
    val address: NominatimAddress? = null
)

data class NominatimAddress(
    val road: String? = null,
    val house_number: String? = null,
    val neighbourhood: String? = null,
    val suburb: String? = null,
    val city: String? = null,
    val town: String? = null,
    val village: String? = null
)
