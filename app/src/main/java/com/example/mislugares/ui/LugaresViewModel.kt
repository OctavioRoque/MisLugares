package com.example.mislugares.ui

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import com.example.mislugares.Lugar
import com.example.mislugares.data.LugaresRepository

/**
 * ViewModel para gestionar el estado de los lugares en la UI.
 */
class LugaresViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LugaresRepository(application)
    
    // Lista observable para la UI
    private val _lugares = mutableStateListOf<Lugar>()
    val lugares: List<Lugar> get() = _lugares

    init {
        _lugares.addAll(repository.cargarLugares())
    }

    fun addLugar(lugar: Lugar) {
        _lugares.add(lugar)
        repository.guardarLugares(_lugares)
    }

    fun updateLugar(index: Int, lugar: Lugar) {
        if (index in _lugares.indices) {
            _lugares[index] = lugar
            repository.guardarLugares(_lugares)
        }
    }

    fun getLugar(index: Int): Lugar? {
        return _lugares.getOrNull(index)
    }
}
