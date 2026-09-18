package com.example.mislugares.data

import com.example.mislugares.GeoPunto

/**
 * Repositorio para gestionar los lugares de interés cercanos.
 * Proporciona puntos de interés reales en Ciudad Universitaria / FIME y Monterrey.
 */
class LugaresCercanosRepository {

    private var cachedLugares: List<LugarCercano>? = null

    suspend fun buscarCercanos(lat: Double, lon: Double, radio: Int = 1000): Result<List<LugarCercano>> {
        val lugares = getLugaresFime(lat, lon, radio)
        cachedLugares = lugares
        return Result.success(lugares)
    }

    private fun getLugaresFime(userLat: Double, userLon: Double, radio: Int): List<LugarCercano> {
        val userPos = GeoPunto(userLon, userLat)
        val data = listOf(
            // --- 0 a 1 km (Campus FIME / Ciudad Universitaria / Alrededores inmediatos) ---
            Triple("Cafetería Central FIME", "cafe", Pair(25.7252, -100.3148) to "Av. Universidad s/n, Ciudad Universitaria"),
            Triple("Gimnasio Área Deportiva FIME", "sports_centre", Pair(25.7250, -100.3155) to "Ciudad Universitaria, San Nicolás"),
            Triple("OXXO Campus FIME", "fuel", Pair(25.7258, -100.3142) to "Pedro de Alba s/n, Ciudad Universitaria"),
            Triple("Subway Ciudad Universitaria", "fast_food", Pair(25.7260, -100.3135) to "Pedro de Alba s/n, San Nicolás"),
            Triple("Polideportivo Tigres UANL", "sports_centre", Pair(25.7240, -100.3160) to "Av. Manuel L. Barragán s/n, Ciudad Universitaria"),
            Triple("Estadio Universitario (El Volcán)", "sports_centre", Pair(25.7236, -100.3121) to "Av. Universidad s/n, Niños Héroes"),
            Triple("BBVA Sucursal Universidad", "bank", Pair(25.7265, -100.3115) to "Av. Universidad 950, San Nicolás"),
            Triple("Taquería La Mexicana CU", "restaurant", Pair(25.7275, -100.3110) to "Av. Universidad 1205, San Nicolás"),
            Triple("Biblioteca Magna Raúl Rangel Frías", "attraction", Pair(25.7208, -100.3175) to "Parque Niños Héroes, Alfonso Reyes"),
            Triple("Farmacia Guadalajara CU", "pharmacy", Pair(25.7270, -100.3095) to "Av. Múnich 100, San Nicolás"),
            Triple("El Pollo Loco Universidad", "restaurant", Pair(25.7295, -100.3110) to "Av. Universidad 1020, San Nicolás"),
            Triple("Parque Niños Héroes", "park", Pair(25.7200, -100.3180) to "Av. Alfonso Reyes s/n, Bella Vista"),
            Triple("Gasolinera Pemex Universidad", "fuel", Pair(25.7285, -100.3090) to "Av. Universidad y Múnich, San Nicolás"),
            Triple("Starbucks San Nicolás Universidad", "cafe", Pair(25.7310, -100.3115) to "Av. Universidad 700, San Nicolás"),
            Triple("Estadio Mobil Super (Sultanes)", "sports_centre", Pair(25.7185, -100.3165) to "Av. Manuel L. Barragán s/n, Niños Héroes"),
            Triple("Hotel Holiday Inn Monterrey Norte", "hotel", Pair(25.7330, -100.3105) to "Av. Universidad 101, San Nicolás"),

            // --- 1 km a 3 km (San Nicolás Centro / Barragán / Sendero) ---
            Triple("Centro de Alto Rendimiento (CARE)", "sports_centre", Pair(25.7170, -100.3200) to "Parque Niños Héroes, San Nicolás"),
            Triple("Museo de Autos y Transportes", "museum", Pair(25.7160, -100.3190) to "Parque Niños Héroes, Monterrey"),
            Triple("Sierra Madre Brewing Co. Anáhuac", "bar", Pair(25.7380, -100.3190) to "Av. Manuel L. Barragán 550, San Nicolás"),
            Triple("Carl's Jr. San Nicolás Universidad", "restaurant", Pair(25.7390, -100.3120) to "Av. Universidad 820, San Nicolás"),
            Triple("Plaza Fiesta Anáhuac", "attraction", Pair(25.7420, -100.3195) to "Av. Manuel L. Barragán 325, San Nicolás"),
            Triple("Hotel Fiesta Inn Monterrey Norte", "hotel", Pair(25.7440, -100.3100) to "Av. Manuel L. Barragán 400, San Nicolás"),
            Triple("Oxxo Gas Sendero San Nicolás", "fuel", Pair(25.7450, -100.3080) to "Av. Sendero Divisorio 200, San Nicolás"),
            Triple("Parque República Mexicana", "park", Pair(25.7450, -100.2950) to "República Mexicana y Sendero, San Nicolás"),
            Triple("Hospital Metropolitano", "pharmacy", Pair(25.7280, -100.2890) to "Av. Adolfo López Mateos 4600, San Nicolás"),

            // --- 3 km a 5 km (Norte de Monterrey / Sendero) ---
            Triple("Cervecería Cuauhtémoc Moctezuma", "bar", Pair(25.6960, -100.3160) to "Alfonso Reyes 2202 Norte, Monterrey"),
            Triple("Plaza Bella San Nicolás", "attraction", Pair(25.7560, -100.2950) to "Av. Sendero Divisorio 1001, San Nicolás"),
            Triple("Hotel City Express Monterrey Norte", "hotel", Pair(25.7590, -100.3050) to "Carretera a Laredo km 10, San Nicolás"),
            Triple("Mobil Gasolinera Barragán", "fuel", Pair(25.7580, -100.3200) to "Av. Manuel L. Barragán 800, San Nicolás"),
            Triple("Parque Fundidora", "park", Pair(25.6880, -100.2880) to "Avenida Fundidora y Adolfo Prieto, Obrera"),
            Triple("Museo del Acero Horno 3", "museum", Pair(25.6860, -100.2830) to "Parque Fundidora, Monterrey"),
            Triple("Vips Sendero San Nicolás", "restaurant", Pair(25.7570, -100.3000) to "Av. Sendero 100, San Nicolás"),

            // --- +5 km (Monterrey Centro y Emblemáticos) ---
            Triple("Paseo Santa Lucía", "attraction", Pair(25.6705, -100.3045) to "Centro, Monterrey"),
            Triple("El Rey del Cabrito", "restaurant", Pair(25.6690, -100.3105) to "Constitución 817 Oriente, Centro"),
            Triple("MARCO Museo de Arte Contemporáneo", "museum", Pair(25.6653, -100.3097) to "Zuazua y Jardón s/n, Centro"),
            Triple("Macroplaza Monterrey", "park", Pair(25.6685, -100.3100) to "Zaragoza y 5 de Mayo, Centro"),
            Triple("Hotel Safi Royal Luxury Downtown", "hotel", Pair(25.6670, -100.3150) to "Pino Suárez 444 Sur, Centro")
        )

        var idCounter = 2000L
        val list = data.map { (nombre, categoria, info) ->
            val (coords, direccion) = info
            val gp = GeoPunto(coords.second, coords.first)
            val dist = userPos.distancia(gp).toFloat()
            LugarCercano(
                osmId = idCounter++,
                nombre = nombre,
                categoria = categoria,
                geoPunto = gp,
                direccion = direccion,
                distanciaMetros = dist
            )
        }

        val enRadio = if (radio >= 50000) list else list.filter { (it.distanciaMetros ?: 0f) <= radio }
        return enRadio.sortedBy { it.distanciaMetros }
    }

    suspend fun enriquecerDirecciones(lugares: List<LugarCercano>, onActualizado: (LugarCercano) -> Unit) {
        // Direcciones ya están disponibles localmente
    }

    fun getCachedLugares(): List<LugarCercano>? = cachedLugares
}
