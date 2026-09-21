package com.example.mislugares.ui

import android.app.Application
import android.location.Location
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.mislugares.data.LugarCercano
import com.example.mislugares.data.LugaresMundialesRepository
import com.example.mislugares.data.NominatimApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * ViewModel para gestionar la búsqueda de lugares de interés mundial utilizando Wikipedia.
 */
class LugaresMundialesViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = LugaresMundialesRepository()
    
    private val nominatimService: NominatimApiService by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "MisLugaresAndroidApp/1.0")
                    .build()
                chain.proceed(request)
            }
            .build()

        Retrofit.Builder()
            .baseUrl(NominatimApiService.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NominatimApiService::class.java)
    }

    private val _lugares = MutableLiveData<List<LugarCercano>>(emptyList())
    val lugares: LiveData<List<LugarCercano>> get() = _lugares
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    fun buscarPorRegion(nombreRegion: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.postValue(true)
            _error.postValue(null)
            _lugares.postValue(emptyList())
            
            try {
                // 1. Obtener coordenadas de la ciudad vía Nominatim
                val response = nominatimService.searchAddress(nombreRegion, language = "es")
                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    val result = response.body()!![0]
                    val lat = result.lat?.toDoubleOrNull()
                    val lon = result.lon?.toDoubleOrNull()
                    
                    if (lat != null && lon != null) {
                        // 2. Buscar en Wikipedia (mucho más rápido y sin timeouts)
                        repository.buscarInteresMundialIncremental(lat, lon, 10000).collect { lista ->
                            _lugares.postValue(lista)
                        }
                    }
                } else {
                    _error.postValue("No se encontró la ciudad: $nombreRegion")
                }
            } catch (e: Exception) {
                Log.e("LugaresMundiales", "Error: ${e.message}")
                _error.postValue("Error al conectar con los servicios de búsqueda")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun buscarLugaresMundiales(location: Location, radio: Int = 10000) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.postValue(true)
            _error.postValue(null)
            _lugares.postValue(emptyList())

            repository.buscarInteresMundialIncremental(location.latitude, location.longitude, radio).collect { lista ->
                _lugares.postValue(lista)
            }
            _isLoading.postValue(false)
        }
    }
}
