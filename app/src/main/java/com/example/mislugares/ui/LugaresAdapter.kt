package com.example.mislugares.ui

import android.content.Intent
import android.location.Location
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.mislugares.Lugar
import com.example.mislugares.R
import java.util.Locale

/**
 * Adapter para mostrar la lista de lugares con cálculo de distancia,
 * estrellas de valoración y botones de acción rápida (Ruta, Llamar, Compartir).
 */
class LugaresAdapter(
    private var lugares: List<Lugar>,
    private var userLocation: Location? = null,
    private val onFavoritoClick: ((Lugar) -> Unit)? = null,
    private val onLugarClick: (Int) -> Unit
) : RecyclerView.Adapter<LugaresAdapter.LugarViewHolder>() {

    // Copia de la lista completa para filtrado por búsqueda
    private var lugaresCompletos: List<Lugar> = lugares
    private var searchQuery: String = ""

    class LugarViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvNombre)
        val tvDireccion: TextView = view.findViewById(R.id.tvDireccion)
        val tvTipo: TextView = view.findViewById(R.id.tvTipo)
        val tvDistancia: TextView = view.findViewById(R.id.tvDistancia)
        val ivIcon: ImageView = view.findViewById(R.id.ivIcon)
        val btnFavorito: ImageView = view.findViewById(R.id.btnFavorito)
        val viewConnector: View = view.findViewById(R.id.viewConnector)
        val cardContainer: View = view.findViewById(R.id.cardContainer)
        val cardIcon: View = view.findViewById(R.id.cardIcon)
        val ratingBar: RatingBar = view.findViewById(R.id.ratingBar)
        val btnRoute: LinearLayout = view.findViewById(R.id.btnRoute)
        val btnCall: LinearLayout = view.findViewById(R.id.btnCall)
        val btnShare: LinearLayout = view.findViewById(R.id.btnShare)
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

        // ⭐ RatingBar - mostrar valoración del lugar
        holder.ratingBar.rating = lugar.valoracion

        // Configurar estado inicial del botón de favorito (activo / inactivo)
        val esFav = lugar.esFavorito()
        if (esFav) {
            holder.btnFavorito.setImageResource(android.R.drawable.btn_star_big_on)
        } else {
            holder.btnFavorito.setImageResource(android.R.drawable.btn_star_big_off)
        }

        // Evento para cambiar de favorito al hacer clic en el icono
        holder.btnFavorito.setOnClickListener {
            val nuevoEstado = !lugar.esFavorito()
            lugar.setFavorito(nuevoEstado)
            
            if (nuevoEstado) {
                holder.btnFavorito.setImageResource(android.R.drawable.btn_star_big_on)
            } else {
                holder.btnFavorito.setImageResource(android.R.drawable.btn_star_big_off)
            }
            onFavoritoClick?.invoke(lugar)
        }

        // Cálculo de distancia nativo
        if (userLocation != null) {
            val results = FloatArray(1)
            Location.distanceBetween(
                userLocation!!.latitude, userLocation!!.longitude,
                lugar.latitud, lugar.longitud,
                results
            )
            val distance = results[0]
            holder.tvDistancia.text = formatDistance(distance)
            holder.tvDistancia.visibility = View.VISIBLE
        } else {
            holder.tvDistancia.visibility = View.GONE
        }

        // 🧭 Botón de Ruta GPS - Abre Google Maps con la ruta trazada
        holder.btnRoute.setOnClickListener {
            if (lugar.latitud != 0.0 || lugar.longitud != 0.0) {
                val uri = Uri.parse("geo:${lugar.latitud},${lugar.longitud}?q=${lugar.latitud},${lugar.longitud}(${Uri.encode(lugar.nombre)})")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                intent.setPackage("com.google.android.apps.maps")
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                } else {
                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                }
            }
        }

        // 📞 Botón de Llamar - Abre el marcador telefónico
        holder.btnCall.setOnClickListener {
            val tel = lugar.telefono
            if (tel != 0) {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$tel"))
                context.startActivity(intent)
            } else {
                Toast.makeText(context, R.string.no_phone_available, Toast.LENGTH_SHORT).show()
            }
        }

        // 📤 Botón de Compartir
        holder.btnShare.setOnClickListener {
            val mensaje = buildString {
                append("📍 ${lugar.nombre}")
                if (!lugar.direccion.isNullOrBlank()) append("\n📫 ${lugar.direccion}")
                if (lugar.telefono != 0) append("\n📞 Tel: ${lugar.telefono}")
                if (lugar.latitud != 0.0 || lugar.longitud != 0.0) {
                    append("\n🗺️ Google Maps: https://maps.google.com/?q=${lugar.latitud},${lugar.longitud}")
                }
            }
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, lugar.nombre)
                putExtra(Intent.EXTRA_TEXT, mensaje)
            }
            context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_place)))
        }

        // Toda la tarjeta y el icono son clickeables
        val clickListener = View.OnClickListener { onLugarClick(position) }
        holder.cardContainer.setOnClickListener(clickListener)
        holder.cardIcon.setOnClickListener(clickListener)
    }

    override fun getItemCount() = lugares.size

    fun updateLugares(newLugares: List<Lugar>) {
        this.lugaresCompletos = newLugares
        applySearchFilter()
    }

    fun updateUserLocation(location: Location) {
        this.userLocation = location
        notifyDataSetChanged()
    }

    /**
     * Filtra los lugares por texto de búsqueda (nombre o dirección).
     */
    fun filterByText(query: String) {
        searchQuery = query.trim().lowercase(Locale.getDefault())
        applySearchFilter()
    }

    private fun applySearchFilter() {
        lugares = if (searchQuery.isBlank()) {
            lugaresCompletos
        } else {
            lugaresCompletos.filter { lugar ->
                lugar.nombre?.lowercase(Locale.getDefault())?.contains(searchQuery) == true ||
                lugar.direccion?.lowercase(Locale.getDefault())?.contains(searchQuery) == true
            }
        }
        notifyDataSetChanged()
    }

    private fun formatDistance(meters: Float): String {
        val km = meters / 1000f
        return when {
            meters < 25 -> "0 km"
            km < 1.0f -> String.format(Locale.getDefault(), "%.2f km", km)
            else -> String.format(Locale.getDefault(), "%.1f km", km)
        }
    }
}
