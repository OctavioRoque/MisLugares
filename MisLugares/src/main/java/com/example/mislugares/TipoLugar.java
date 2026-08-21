package com.example.mislugares;

public enum TipoLugar {
    OTROS("Otros", 0),
    RESTAURANTE("Restaurante", 1),
    BAR("Bar", 2),
    COPAS("Copas", 3),
    ESPECTACULO("Espectáculo", 4),
    HOTEL("Hotel", 5),
    COMPRAS("Compras", 6),
    EDUCACION("Educación", 7),
    DEPORTE("Deporte", 8),
    NATURALEZA("Naturaleza", 9),
    GASOLINERA("Gasolinera", 10);

    private final String texto;
    private final int recurso;

    TipoLugar(String texto, int recurso) {
        this.texto = texto;
        this.recurso = recurso;
    }

    public String getTexto() {
        return texto;
    }

    public int getRecurso() {
        return recurso;
    }

    public static String[] getNombres() {
        String[] resultado = new String[TipoLugar.values().length];
        for (TipoLugar tipo : TipoLugar.values()) {
            resultado[tipo.ordinal()] = tipo.texto;
        }
        return resultado;
    }
}
