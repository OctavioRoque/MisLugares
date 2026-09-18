package com.example.mislugares

data class Lugar(
    val nombre: String,
    val direccion: String,
    val longitud: Double = 0.0,
    val latitud: Double = 0.0,
    val tipo: TipoLugar = TipoLugar.OTROS,
    val telefono: Int = 0,
    val url: String = "",
    val comentario: String = "",
    var valoracion: Int = 3,
    var esFavorito: Boolean = false // <--- La propiedad de favoritos que te faltaba
) {
    // Métodos opcionales por si alguna parte del código los busca
    fun esFavorito(): Boolean = esFavorito
    fun setFavorito(valor: Boolean) {
        esFavorito = valor
    }
}
