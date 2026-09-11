package com.example.mislugares.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.location.Location
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.mislugares.R
import com.example.mislugares.data.LugarCercano
import java.util.Locale

/**
 * Adapter para mostrar los lugares cercanos obtenidos de OSM.
 */
class LugaresCercanosAdapter(
    private var lugares: List<LugarCercano>,
    private var userLocation: Location? = null,
    private val onLugarClick: (LugarCercano) -> Unit
) : RecyclerView.Adapter<LugaresCercanosAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvNombre)
        val tvDireccion: TextView = view.findViewById(R.id.tvDireccion)
        val tvTipo: TextView = view.findViewById(R.id.tvTipo)
        val tvDistancia: TextView = view.findViewById(R.id.tvDistancia)
        val ivIcono: ImageView = view.findViewById(R.id.ivIcono)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lugar_cercano, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val lugar = lugares[position]
        val context = holder.itemView.context
        
        holder.tvNombre.text = lugar.nombre
        holder.tvDireccion.text = lugar.direccion ?: context.getString(R.string.address_not_available)
        
        // Mapeo simple de categoría OSM a texto amigable
        holder.tvTipo.text = mapOsmCategory(lugar.categoria, context)

        // Configurar icono y color
        val (iconRes, colorHex) = iconoYColorPara(lugar.categoria)
        holder.ivIcono.setImageResource(iconRes)
        ImageViewCompat.setImageTintList(holder.ivIcono, ColorStateList.valueOf(Color.parseColor(colorHex)))

        // Cálculo de distancia
        val distancia = lugar.distanciaMetros ?: run {
            if (userLocation != null) {
                val results = FloatArray(1)
                Location.distanceBetween(
                    userLocation!!.latitude, userLocation!!.longitude,
                    lugar.geoPunto.latitud, lugar.geoPunto.longitud,
                    results
                )
                lugar.distanciaMetros = results[0]
                results[0]
            } else null
        }

        if (distancia != null) {
            holder.tvDistancia.text = formatDistance(distancia)
            holder.tvDistancia.visibility = View.VISIBLE
        } else {
            holder.tvDistancia.visibility = View.GONE
        }

        holder.itemView.setOnClickListener { onLugarClick(lugar) }
    }

    override fun getItemCount() = lugares.size

    fun updateLugares(newLugares: List<LugarCercano>) {
        this.lugares = newLugares
        notifyDataSetChanged()
    }

    fun updateUserLocation(location: Location) {
        this.userLocation = location
        notifyDataSetChanged()
    }

    private fun iconoYColorPara(categoria: String): Pair<Int, String> {
        return when (categoria.lowercase()) {
            "restaurant", "fast_food" -> Pair(R.drawable.ic_restaurant, "#FF5722")
            "cafe", "bar", "pub" -> Pair(R.drawable.ic_cafe, "#795548")
            "hotel", "hostel" -> Pair(R.drawable.ic_hotel, "#3F51B5")
            "park", "nature", "nature_reserve" -> Pair(R.drawable.ic_park, "#4CAF50")
            "sports", "sports_centre", "gym" -> Pair(R.drawable.ic_location, "#2196F3")
            "fuel" -> Pair(R.drawable.ic_fuel, "#607D8B")
            "pharmacy" -> Pair(R.drawable.ic_pharmacy, "#E91E63")
            "bank" -> Pair(R.drawable.ic_bank, "#FFC107")
            "museum", "attraction", "viewpoint" -> Pair(R.drawable.ic_location, "#9C27B0")
            else -> Pair(R.drawable.ic_location, "#9E9E9E") // Default gray
        }
    }

    private fun mapOsmCategory(category: String, context: android.content.Context): String {
        return when (category.lowercase()) {
            "restaurant", "fast_food" -> context.getString(R.string.type_restaurant)
            "cafe", "bar", "pub" -> context.getString(R.string.type_bar)
            "hotel", "hostel", "motel" -> context.getString(R.string.type_hotel)
            "park", "nature_reserve" -> context.getString(R.string.type_nature)
            "sports_centre", "gym", "stadium" -> context.getString(R.string.type_sport)
            "fuel" -> context.getString(R.string.type_gas_station)
            "pharmacy" -> context.getString(R.string.type_pharmacy)
            "bank" -> context.getString(R.string.type_bank)
            "museum", "attraction", "viewpoint" -> context.getString(R.string.type_others)
            else -> category.replace("_", " ").replaceFirstChar { it.uppercase() }
        }
    }

    private fun formatDistance(meters: Float): String {
        return if (meters < 1000) {
            String.format(Locale.getDefault(), "%.0f m", meters)
        } else {
            String.format(Locale.getDefault(), "%.1f km", meters / 1000)
        }
    }
}
