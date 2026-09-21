package com.example.mislugares;

public class Lugar {
    private String nombre;
    private String direccion;
    private GeoPunto posicion;
    private String foto;
    private int telefono;
    private String url;
    private String comentario;
    private long fecha;
    private float valoracion;
    private TipoLugar tipo;
    private boolean esFavorito;

    public Lugar() {
        this.fecha = System.currentTimeMillis();
        this.posicion = new GeoPunto(0, 0);
        this.tipo = TipoLugar.OTROS;
        this.esFavorito = false;
    }

    public Lugar(String nombre, String direccion, double longitud, double latitud, TipoLugar tipo, int telefono, String url, String comentario, int valoracion) {
        this.fecha = System.currentTimeMillis();
        this.posicion = new GeoPunto(longitud, latitud);
        this.nombre = nombre;
        this.direccion = direccion;
        this.tipo = tipo;
        this.telefono = telefono;
        this.url = url;
        this.comentario = comentario;
        this.valoracion = valoracion;
        this.esFavorito = false;
    }

    public Lugar(String nombre, String direccion, double longitud, double latitud, TipoLugar tipo, int telefono, String url, String comentario, int valoracion, boolean esFavorito) {
        this(nombre, direccion, longitud, latitud, tipo, telefono, url, comentario, valoracion);
        this.esFavorito = esFavorito;
    }

    public double getLatitud() {
        return posicion != null ? posicion.getLatitud() : 0.0;
    }

    public void setLatitud(double latitud) {
        if (posicion == null) {
            posicion = new GeoPunto(0.0, latitud);
        } else {
            posicion.setLatitud(latitud);
        }
    }

    public double getLongitud() {
        return posicion != null ? posicion.getLongitud() : 0.0;
    }

    public void setLongitud(double longitud) {
        if (posicion == null) {
            posicion = new GeoPunto(longitud, 0.0);
        } else {
            posicion.setLongitud(longitud);
        }
    }

    public boolean esFavorito() {
        return esFavorito;
    }

    public boolean isEsFavorito() {
        return esFavorito;
    }

    public boolean getEsFavorito() {
        return esFavorito;
    }

    public void setFavorito(boolean esFavorito) {
        this.esFavorito = esFavorito;
    }

    public void setEsFavorito(boolean esFavorito) {
        this.esFavorito = esFavorito;
    }

    public TipoLugar getTipo() {
        return tipo;
    }

    public void setTipo(TipoLugar tipo) {
        this.tipo = tipo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDireccion() {
        return direccion;
    }

    public GeoPunto getPosicion() {
        return posicion;
    }

    public String getFoto() {
        return foto;
    }

    public float getValoracion() {
        return valoracion;
    }

    public void setValoracion(float valoracion) {
        this.valoracion = valoracion;
    }

    public long getFecha() {
        return fecha;
    }

    public void setFecha(long fecha) {
        this.fecha = fecha;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getTelefono() {
        return telefono;
    }

    public void setTelefono(int telefono) {
        this.telefono = telefono;
    }

    public void setFoto(String foto) {
        this.foto = foto;
    }

    public void setPosicion(GeoPunto posicion) {
        this.posicion = posicion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    @Override
    public String toString() {
        return " {" +
                "nombre=" + nombre +
                ", direccion=" + direccion +
                ", posicion=" + posicion +
                ", tipo=" + tipo +
                ", foto=" + foto +
                ", telefono=" + telefono +
                ", url=" + url +
                ", comentario=" + comentario +
                ", fecha=" + fecha +
                ", valoracion=" + valoracion +
                ", esFavorito=" + esFavorito +
                '}';
    }
}
