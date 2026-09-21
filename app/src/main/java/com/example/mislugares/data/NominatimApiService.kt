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

    @GET("search")
    suspend fun searchAddress(
        @Query("q") query: String,
        @Query("format") format: String = "jsonv2",
        @Query("addressdetails") addressDetails: Int = 1,
        @Query("limit") limit: Int = 5,
        @Query("countrycodes") countryCodes: String? = null,
        @Query("accept-language") language: String = "es"
    ): Response<List<NominatimSearchResult>>

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

data class NominatimSearchResult(
    val display_name: String? = null,
    val lat: String? = null,
    val lon: String? = null,
    val address: NominatimAddress? = null
)
