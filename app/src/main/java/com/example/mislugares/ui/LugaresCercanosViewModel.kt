package com.example.mislugares.ui

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.mislugares.data.LugarCercano
import com.example.mislugares.data.LugaresCercanosRepository
import kotlinx.coroutines.launch

/**
 * ViewModel para gestionar la búsqueda y visualización de lugares cercanos.
 */
class LugaresCercanosViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = LugaresCercanosRepository()
    
    private val _lugaresCercanos = MutableLiveData<List<LugarCercano>>()
    val lugaresCercanos: LiveData<List<LugarCercano>> get() = _lugaresCercanos
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    fun buscarLugaresCercanos(location: Location, radio: Int = 1000) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            val result = repository.buscarCercanos(location.latitude, location.longitude, radio)
            
            result.onSuccess {
                _lugaresCercanos.value = it
                // Enriquecimiento de direcciones en segundo plano
                viewModelScope.launch {
                    repository.enriquecerDirecciones(it) { actualizado ->
                        _lugaresCercanos.value = _lugaresCercanos.value?.map {
                            if (it.osmId == actualizado.osmId) actualizado else it
                        }
                    }
                }
            }.onFailure {
                _error.value = it.message ?: getApplication<android.app.Application>().getString(com.example.mislugares.R.string.unknown_error)
            }
            
            _isLoading.value = false
        }
    }
}
