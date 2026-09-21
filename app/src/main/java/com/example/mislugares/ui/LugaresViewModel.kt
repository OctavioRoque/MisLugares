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

    fun toggleFavorito(lugar: Lugar) {
        val currentList = _lugares.value?.toMutableList() ?: return
        val index = currentList.indexOfFirst { it.nombre == lugar.nombre && it.latitud == lugar.latitud && it.longitud == lugar.longitud }
        if (index != -1) {
            val nuevoEstado = !currentList[index].esFavorito()
            currentList[index].setFavorito(nuevoEstado)
            _lugares.value = currentList
            repository.guardarLugares(currentList)
        }
    }

    fun guardarLugarCercanoComoFavorito(lugarCercano: com.example.mislugares.data.LugarCercano): Boolean {
        val currentList = _lugares.value?.toMutableList() ?: mutableListOf()
        val existe = currentList.any { it.nombre == lugarCercano.nombre && it.latitud == lugarCercano.geoPunto.latitud && it.longitud == lugarCercano.geoPunto.longitud }
        if (!existe) {
            val tipo = when (lugarCercano.categoria.lowercase()) {
                "restaurant", "fast_food" -> com.example.mislugares.TipoLugar.RESTAURANTE
                "cafe", "bar", "pub" -> com.example.mislugares.TipoLugar.BAR
                "hotel", "hostel", "motel" -> com.example.mislugares.TipoLugar.HOTEL
                "park", "nature", "nature_reserve" -> com.example.mislugares.TipoLugar.NATURALEZA
                "sports_centre", "gym", "stadium" -> com.example.mislugares.TipoLugar.DEPORTE
                "fuel" -> com.example.mislugares.TipoLugar.GASOLINERA
                "bank", "pharmacy" -> com.example.mislugares.TipoLugar.COMPRAS
                "museum", "attraction" -> com.example.mislugares.TipoLugar.ESPECTACULO
                else -> com.example.mislugares.TipoLugar.OTROS
            }
            val nuevoLugar = Lugar(
                lugarCercano.nombre,
                lugarCercano.direccion ?: "",
                lugarCercano.geoPunto.longitud,
                lugarCercano.geoPunto.latitud,
                tipo,
                0,
                "",
                "",
                0,
                true // Guardado directamente como favorito
            )
            currentList.add(nuevoLugar)
            _lugares.value = currentList
            repository.guardarLugares(currentList)
            return true
        }
        return false
    }
}