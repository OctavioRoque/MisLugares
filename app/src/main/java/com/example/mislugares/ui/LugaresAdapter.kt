package com.example.mislugares.ui

import android.location.Location
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mislugares.Lugar
import com.example.mislugares.R
import java.util.Locale

/**
 * Adapter para mostrar la lista de lugares con cálculo de distancia.
 */
class LugaresAdapter(
    private var lugares: List<Lugar>,
    private var userLocation: Location? = null,
    private val onLugarClick: (Int) -> Unit
) : RecyclerView.Adapter<LugaresAdapter.LugarViewHolder>() {

    class LugarViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvNombre)
        val tvDireccion: TextView = view.findViewById(R.id.tvDireccion)
        val tvTipo: TextView = view.findViewById(R.id.tvTipo)
        val tvDistancia: TextView = view.findViewById(R.id.tvDistancia)
        val ivIcon: ImageView = view.findViewById(R.id.ivIcon)
        val btnFavorito: ImageView = view.findViewById(R.id.btnFavorito) // <-- 1. Añadido aquí
        val viewConnector: View = view.findViewById(R.id.viewConnector)
        val cardContainer: View = view.findViewById(R.id.cardContainer)
        val cardIcon: View = view.findViewById(R.id.cardIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LugarViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_lugar, parent, false)
        return LugarViewHolder(view)
    }

    override fun onBindViewHolder(holder: LugarViewHolder, position: Int) {
        val lugar = lugares[position]
        val context = holder.itemView.context
        holder.tvNombre.text = lugar.nombre
        holder.tvDireccion.text = lugar.direccion
        
        // Hide connector for last item
        holder.viewConnector.visibility = if (position == itemCount - 1) View.INVISIBLE else View.VISIBLE

        // Icon based on type
        val iconRes = when (lugar.tipo) {
            com.example.mislugares.TipoLugar.NATURALEZA -> R.drawable.ic_mountain
            com.example.mislugares.TipoLugar.RESTAURANTE -> R.drawable.ic_restaurant
            com.example.mislugares.TipoLugar.BAR -> R.drawable.ic_restaurant
            com.example.mislugares.TipoLugar.HOTEL -> R.drawable.ic_hotel
            com.example.mislugares.TipoLugar.GASOLINERA -> R.drawable.ic_fuel
            com.example.mislugares.TipoLugar.OTROS -> R.drawable.ic_compass
            else -> R.drawable.ic_location
        }
        holder.ivIcon.setImageResource(iconRes)

        // Traducción de TipoLugar
        val tipoRes = when (lugar.tipo) {
            com.example.mislugares.TipoLugar.OTROS -> R.string.type_others
            com.example.mislugares.TipoLugar.RESTAURANTE -> R.string.type_restaurant
            com.example.mislugares.TipoLugar.BAR -> R.string.type_bar
            com.example.mislugares.TipoLugar.COPAS -> R.string.type_drinks
            com.example.mislugares.TipoLugar.ESPECTACULO -> R.string.type_show
            com.example.mislugares.TipoLugar.HOTEL -> R.string.type_hotel
            com.example.mislugares.TipoLugar.COMPRAS -> R.string.type_shopping
            com.example.mislugares.TipoLugar.EDUCACION -> R.string.type_education
            com.example.mislugares.TipoLugar.DEPORTE -> R.string.type_sport
            com.example.mislugares.TipoLugar.NATURALEZA -> R.string.type_nature
            com.example.mislugares.TipoLugar.GASOLINERA -> R.string.type_gas_station
            else -> R.string.type_others
        }
        holder.tvTipo.text = context.getString(tipoRes).uppercase(Locale.getDefault())

        // Configurar estado inicial del botón de favorito (activo / inactivo)
        // Ajusta el método según cómo lo tengas definido en tu clase Lugar (ej. lugar.esFavorito() o lugar.favorito)
        val esFav = lugar.esFavorito() // O lugar.favorito
        if (esFav) {
            holder.btnFavorito.setImageResource(android:drawable.btn_star_big_on)
        } else {
            holder.btnFavorito.setImageResource(android:drawable.btn_star_big_off)
        }

        // Evento para cambiar de favorito al hacer clic en el icono
        holder.btnFavorito.setOnClickListener {
            val nuevoEstado = !lugar.esFavorito() // O lugar.favorito
            lugar.setFavorito(nuevoEstado) // O lugar.favorito = nuevoEstado
            
            if (nuevoEstado) {
                holder.btnFavorito.setImageResource(android:drawable.btn_star_big_on)
            } else {
                holder.btnFavorito.setImageResource(android:drawable.btn_star_big_off)
            }
        }

        // Cálculo de distancia nativo
        if (userLocation != null && lugar.posicion != null) {
            val results = FloatArray(1)
            Location.distanceBetween(
                userLocation!!.latitude, userLocation!!.longitude,
                lugar.posicion.latitud, lugar.posicion.longitud,
                results
            )
            val distance = results[0]
            holder.tvDistancia.text = formatDistance(distance)
            holder.tvDistancia.visibility = View.VISIBLE
        } else {
            holder.tvDistancia.visibility = View.GONE
        }

        // Toda la tarjeta y el icono son clickeables
        val clickListener = View.OnClickListener { onLugarClick(position) }
        holder.cardContainer.setOnClickListener(clickListener)
        holder.cardIcon.setOnClickListener(clickListener)
    }

    override fun getItemCount() = lugares.size

    fun updateLugares(newLugares: List<Lugar>) {
        this.lugares = newLugares
        notifyDataSetChanged()
    }

    fun updateUserLocation(location: Location) {
        this.userLocation = location
        notifyDataSetChanged()
    }

    private fun formatDistance(meters: Float): String {
        return if (meters < 1000) {
            String.format(Locale.getDefault(), "%.0f m", meters)
        } else {
            String.format(Locale.getDefault(), "%.1f km", meters / 1000)
        }
    }
}
