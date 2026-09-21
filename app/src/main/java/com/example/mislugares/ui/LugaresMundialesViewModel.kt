package com.example.mislugares.ui

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.mislugares.data.LugarCercano
import com.example.mislugares.data.LugaresMundialesRepository
import com.example.mislugares.data.NominatimApiService
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * ViewModel para gestionar la búsqueda de lugares de interés mundial.
 */
class LugaresMundialesViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = LugaresMundialesRepository()
    
    private val nominatimService: NominatimApiService by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
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

    private val _lugares = MutableLiveData<List<LugarCercano>>()
    val lugares: LiveData<List<LugarCercano>> get() = _lugares
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    fun buscarPorRegion(nombreRegion: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val response = nominatimService.searchAddress(nombreRegion)
                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    val result = response.body()!![0]
                    val lat = result.lat?.toDoubleOrNull()
                    val lon = result.lon?.toDoubleOrNull()
                    
                    if (lat != null && lon != null) {
                        val placesResult = repository.buscarInteresMundial(lat, lon, 3000) // 3km es ideal para velocidad y relevancia
                        placesResult.onSuccess {
                            _lugares.value = it
                        }.onFailure {
                            _error.value = it.message
                        }
                    } else {
                        _error.value = "No se pudieron obtener coordenadas para esa región"
                    }
                } else {
                    _error.value = "No se encontró la región: $nombreRegion"
                }
            } catch (e: Exception) {
                _error.value = "Error de red: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun buscarLugaresMundiales(location: Location, radio: Int = 5000) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            val result = repository.buscarInteresMundial(location.latitude, location.longitude, radio)
            
            result.onSuccess {
                _lugares.value = it
            }.onFailure {
                _error.value = it.message ?: "Error desconocido"
            }
            
            _isLoading.value = false
        }
    }
}
