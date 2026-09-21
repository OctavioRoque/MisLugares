package com.example.mislugares.data

import com.example.mislugares.GeoPunto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Repositorio para buscar lugares de interés real utilizando la API de Wikipedia.
 * Wikipedia filtra automáticamente el "ruido" y devuelve solo puntos de interés
 * histórico, cultural o geográfico de relevancia.
 */
class LugaresMundialesRepository {

    private val apiService: WikipediaApiService by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "MisLugares/1.5 (roque.octavio@gmail.com)")
                    .build()
                chain.proceed(request)
            }
            .build()

        Retrofit.Builder()
            .baseUrl(WikipediaApiService.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WikipediaApiService::class.java)
    }

    /**
     * Busca lugares de interés mundial mediante la API de Wikipedia Geosearch.
     * Devuelve un Flow por compatibilidad con el flujo incremental, aunque la respuesta es casi instantánea.
     */
    fun buscarInteresMundialIncremental(lat: Double, lon: Double, radio: Int = 10000): Flow<List<LugarCercano>> = flow {
        try {
            val response = apiService.getNearbyPlaces("$lat|$lon", radio)
            if (response.isSuccessful) {
                val items = response.body()?.query?.geosearch ?: emptyList()
                val list = items.map { item ->
                    LugarCercano(
                        osmId = item.pageid,
                        nombre = item.title,
                        categoria = "museum", // Categoría genérica interesante
                        geoPunto = GeoPunto(item.lon, item.lat),
                        direccion = "Punto de interés histórico/cultural",
                        region = "Wikipedia"
                    )
                }
                if (list.isNotEmpty()) {
                    emit(list)
                }
            }
        } catch (e: Exception) {
            // Error silencioso en el flujo
        }
    }.flowOn(Dispatchers.IO)

    suspend fun buscarInteresMundial(lat: Double, lon: Double, radio: Int = 10000): Result<List<LugarCercano>> {
        return try {
            val response = apiService.getNearbyPlaces("$lat|$lon", radio)
            if (response.isSuccessful) {
                val items = response.body()?.query?.geosearch ?: emptyList()
                val list = items.map { item ->
                    LugarCercano(
                        osmId = item.pageid,
                        nombre = item.title,
                        categoria = "museum",
                        geoPunto = GeoPunto(item.lon, item.lat),
                        direccion = "Punto de interés histórico/cultural",
                        region = "Wikipedia"
                    )
                }
                Result.success(list)
            } else {
                Result.failure(Exception("Error Wikipedia: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
