package com.example.mislugares.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.mislugares.Lugar
import com.example.mislugares.data.LugaresRepository

/**
 * ViewModel para gestionar el estado de los lugares.
 */
class LugaresViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LugaresRepository(application)
    
    private val _lugares = MutableLiveData<List<Lugar>>()
    val lugares: LiveData<List<Lugar>> get() = _lugares

    init {
        cargarLugares()
    }

    fun cargarLugares() {
        _lugares.value = repository.cargarLugares()
    }

    fun addLugar(lugar: Lugar) {
        val currentList = _lugares.value?.toMutableList() ?: mutableListOf()
        currentList.add(lugar)
        _lugares.value = currentList
        repository.guardarLugares(currentList)
    }

    fun updateLugar(index: Int, lugar: Lugar) {
        val currentList = _lugares.value?.toMutableList() ?: return
        if (index in currentList.indices) {
            currentList[index] = lugar
            _lugares.value = currentList
            repository.guardarLugares(currentList)
        }
    }

    fun getLugar(index: Int): Lugar? {
        return _lugares.value?.getOrNull(index)
    }
}