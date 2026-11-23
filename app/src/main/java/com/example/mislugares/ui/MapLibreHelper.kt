package com.example.mislugares.ui

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.Gravity
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import com.example.mislugares.R
import com.example.mislugares.TipoLugar
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

/**
 * Utilidad compartida para estilizar mapas vectoriales OpenStreetMap con MapLibre + OpenFreeMap.
 * Genera pines vectoriales nítidos en alta densidad, gestiona estilos y capas de marcadores.
 */
object MapLibreHelper {

    enum class MapStyleOption(
        val url: String,
        @StringRes val labelRes: Int,
        val isDark: Boolean = false
    ) {
        LIBERTY("https://tiles.openfreemap.org/styles/liberty", R.string.map_style_liberty, false),
        BRIGHT("https://tiles.openfreemap.org/styles/bright", R.string.map_style_bright, false),
        POSITRON("https://tiles.openfreemap.org/styles/positron", R.string.map_style_positron, false),
        DARK("https://tiles.openfreemap.org/styles/dark", R.string.map_style_dark, true)
    }

    const val ICON_USER_PUCK = "icon-user-puck"
    private const val PIN_PREFIX = "icon-pin-"

    fun pinIconId(tipo: TipoLugar?): String = "$PIN_PREFIX${(tipo ?: TipoLugar.OTROS).name}"

    @ColorInt
    fun colorForTipo(tipo: TipoLugar?): Int = when (tipo) {
        TipoLugar.NATURALEZA -> Color.parseColor("#2E8B57")
        TipoLugar.RESTAURANTE -> Color.parseColor("#E06A3B")
        TipoLugar.BAR, TipoLugar.COPAS -> Color.parseColor("#C85A32")
        TipoLugar.ESPECTACULO -> Color.parseColor("#7B5EA7")
        TipoLugar.EDUCACION -> Color.parseColor("#4F65C7")
        TipoLugar.HOTEL -> Color.parseColor("#2E7DB3")
        TipoLugar.COMPRAS -> Color.parseColor("#D69426")
        TipoLugar.DEPORTE -> Color.parseColor("#00897B")
        TipoLugar.GASOLINERA -> Color.parseColor("#546E7A")
        TipoLugar.OTROS, null -> Color.parseColor("#5B8E42")
    }

    /**
     * Configura gestos e interfaz limpia del mapa.
     */
    fun configureUiSettings(
        map: MapLibreMap,
        showCompass: Boolean = true,
        enableTilt: Boolean = true,
        enableRotate: Boolean = true
    ) {
        map.uiSettings.apply {
            isScrollGesturesEnabled = true
            isZoomGesturesEnabled = true
            isDoubleTapGesturesEnabled = true
            isQuickZoomGesturesEnabled = true
            isTiltGesturesEnabled = enableTilt
            isRotateGesturesEnabled = enableRotate
            isCompassEnabled = showCompass
            isLogoEnabled = false
            isAttributionEnabled = true
            val marginPx = (8f * Resources.getSystem().displayMetrics.density).toInt()
            attributionGravity = Gravity.BOTTOM or Gravity.START
            setAttributionMargins(marginPx, 0, 0, marginPx)
        }
    }

    /**
     * Registra los bitmaps de pines personalizados (usuario + cada categoría de TipoLugar) en el estilo.
     */
    fun registerPinImages(context: Context, style: Style) {
        style.addImage(ICON_USER_PUCK, createUserLocationPuck(context))
        TipoLugar.values().forEach { tipo ->
            style.addImage(pinIconId(tipo), createPlacePinBitmap(context, colorForTipo(tipo)))
        }
    }

    /**
     * Crea o actualiza la capa de ubicación del usuario (punto brillante moderno).
     */
    fun setupOrUpdateUserLocation(
        style: Style,
        sourceId: String,
        layerId: String,
        lat: Double,
        lon: Double,
        label: String? = null,
        isDarkStyle: Boolean = false
    ) {
        val feature = Feature.fromGeometry(Point.fromLngLat(lon, lat)).apply {
            if (!label.isNullOrBlank()) {
                addStringProperty("title", label)
            }
        }
        val collection = FeatureCollection.fromFeature(feature)

        val existingSource = style.getSourceAs<GeoJsonSource>(sourceId)
        if (existingSource != null) {
            existingSource.setGeoJson(collection)
        } else {
            style.addSource(GeoJsonSource(sourceId, collection))
        }

        if (style.getLayer(layerId) == null) {
            val textColor = if (isDarkStyle) Color.parseColor("#F2F5F0") else Color.parseColor("#1A2D13")
            val haloColor = if (isDarkStyle) Color.parseColor("#181C20") else Color.parseColor("#FFFFFF")

            val layer = SymbolLayer(layerId, sourceId).withProperties(
                PropertyFactory.iconImage(ICON_USER_PUCK),
                PropertyFactory.iconAnchor(Property.ICON_ANCHOR_CENTER),
                PropertyFactory.iconPitchAlignment(Property.ICON_PITCH_ALIGNMENT_MAP),
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconIgnorePlacement(true),
                PropertyFactory.textField(Expression.get("title")),
                PropertyFactory.textFont(arrayOf("Noto Sans Bold")),
                PropertyFactory.textSize(11.5f),
                PropertyFactory.textAnchor(Property.TEXT_ANCHOR_TOP),
                PropertyFactory.textOffset(arrayOf(0f, 1.35f)),
                PropertyFactory.textColor(textColor),
                PropertyFactory.textHaloColor(haloColor),
                PropertyFactory.textHaloWidth(1.8f),
                PropertyFactory.textOptional(true)
            )
            style.addLayer(layer)
        }
    }

    /**
     * Crea o actualiza una capa de marcadores de lugares (pines teardrop por categoría + etiquetas nítidas).
     */
    fun setupOrUpdatePlacePins(
        style: Style,
        sourceId: String,
        layerId: String,
        features: List<Feature>,
        isDarkStyle: Boolean = false,
        showLabels: Boolean = true
    ) {
        val collection = FeatureCollection.fromFeatures(features)
        val existingSource = style.getSourceAs<GeoJsonSource>(sourceId)
        if (existingSource != null) {
            existingSource.setGeoJson(collection)
        } else {
            style.addSource(GeoJsonSource(sourceId, collection))
        }

        if (style.getLayer(layerId) == null) {
            val textColor = if (isDarkStyle) Color.parseColor("#F2F5F0") else Color.parseColor("#1C2419")
            val haloColor = if (isDarkStyle) Color.parseColor("#181C20") else Color.parseColor("#FFFFFF")

            val layer = SymbolLayer(layerId, sourceId).withProperties(
                PropertyFactory.iconImage(Expression.get("icon")),
                PropertyFactory.iconAnchor(Property.ICON_ANCHOR_BOTTOM),
                PropertyFactory.iconPitchAlignment(Property.ICON_PITCH_ALIGNMENT_VIEWPORT),
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconIgnorePlacement(true)
            )
            if (showLabels) {
                layer.withProperties(
                    PropertyFactory.textField(Expression.get("title")),
                    PropertyFactory.textFont(arrayOf("Noto Sans Bold")),
                    PropertyFactory.textSize(11f),
                    PropertyFactory.textAnchor(Property.TEXT_ANCHOR_TOP),
                    PropertyFactory.textOffset(arrayOf(0f, 0.25f)),
                    PropertyFactory.textMaxWidth(11f),
                    PropertyFactory.textColor(textColor),
                    PropertyFactory.textHaloColor(haloColor),
                    PropertyFactory.textHaloWidth(1.8f),
                    PropertyFactory.textOptional(true)
                )
            }
            style.addLayer(layer)
        }
    }

    /**
     * Genera un marcador circular moderno con halo suave para la ubicación actual del usuario.
     */
    private fun createUserLocationPuck(context: Context): Bitmap {
        val metrics = context.resources.displayMetrics
        val density = metrics.density
        val size = (40f * density).toInt().coerceAtLeast(40)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
            this.density = metrics.densityDpi
        }
        val canvas = Canvas(bitmap)
        val cx = size / 2f
        val cy = size / 2f

        // Halo exterior translúcido verde/esmeralda
        val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#3B2E7D32")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, 17f * density, haloPaint)

        // Sombra suave bajo el anillo blanco
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#30000000")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy + 1.2f * density, 10.5f * density, shadowPaint)

        // Anillo blanco exterior
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, 10f * density, borderPaint)

        // Núcleo principal verde esmeralda vibrante
        val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2E8B57")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, 7f * density, corePaint)

        // Destello interior sutil
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#90FFFFFF")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx - 2f * density, cy - 2f * density, 1.8f * density, dotPaint)

        return bitmap
    }

    /**
     * Genera un pin moderno tipo gota (teardrop) con borde blanco, sombra y punto interior.
     */
    private fun createPlacePinBitmap(context: Context, @ColorInt fillColor: Int): Bitmap {
        val metrics = context.resources.displayMetrics
        val density = metrics.density
        val width = (34f * density).toInt().coerceAtLeast(34)
        val height = (44f * density).toInt().coerceAtLeast(44)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            this.density = metrics.densityDpi
        }
        val canvas = Canvas(bitmap)

        val cx = width / 2f
        val headRadius = 11.5f * density
        val headCy = 14.5f * density
        // Punta alineada exactamente al borde inferior del bitmap (anclaje ICON_ANCHOR_BOTTOM)
        val tipY = height - 0.5f * density

        // Sombra suave justo detrás de la punta sin desplazar el anclaje vertical
        val groundShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#35000000")
            style = Paint.Style.FILL
        }
        canvas.drawOval(
            cx - 6.5f * density,
            tipY - 3.5f * density,
            cx + 6.5f * density,
            tipY,
            groundShadowPaint
        )

        // Forma de gota exterior (borde blanco)
        val outerPath = buildTeardropPath(cx, headCy, headRadius + 2f * density, tipY)
        val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawPath(outerPath, whitePaint)

        // Forma de gota interior (color de categoría)
        val innerPath = buildTeardropPath(cx, headCy, headRadius, tipY - 2f * density)
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fillColor
            style = Paint.Style.FILL
        }
        canvas.drawPath(innerPath, fillPaint)

        // Punto central blanco
        val centerDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, headCy, 4.2f * density, centerDotPaint)

        return bitmap
    }

    private fun buildTeardropPath(cx: Float, cy: Float, radius: Float, tipY: Float): Path {
        return Path().apply {
            // Arco superior del pin
            val left = cx - radius
            val top = cy - radius
            val right = cx + radius
            val bottom = cy + radius
            addArc(left, top, right, bottom, 150f, 240f)
            // Curvas suaves hacia la punta inferior
            quadTo(cx + radius * 0.55f, cy + radius * 1.15f, cx, tipY)
            quadTo(cx - radius * 0.55f, cy + radius * 1.15f, cx - radius * 0.866f, cy + radius * 0.5f)
            close()
        }
    }
}
