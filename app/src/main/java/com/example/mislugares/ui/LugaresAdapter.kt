package com.example.mislugares.ui

import android.location.Location
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LugarViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_lugar, parent, false)
        return LugarViewHolder(view)
    }

    override fun onBindViewHolder(holder: LugarViewHolder, position: Int) {
        val lugar = lugares[position]
        holder.tvNombre.text = lugar.nombre
        holder.tvDireccion.text = lugar.direccion
        holder.tvTipo.text = lugar.tipo.texto

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

        holder.itemView.setOnClickListener { onLugarClick(position) }
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