package com.example.practica6.logica;

public class ResultadoPrueba {

    private String columna, metodo;
    private int cantidad, vacios;
    private double tiempo;
    private boolean correcto;
    private String entrada;
    private double[] muestras;
    private int calentamientos;
    private double tiempoCalentamiento;

    public ResultadoPrueba(String columna, String metodo, int cantidad, int vacios, double tiempo, boolean correcto) {
        this(columna, metodo, cantidad, vacios, tiempo, correcto, "Original CSV");
    }

    public ResultadoPrueba(String columna, String metodo, int cantidad, int vacios, double tiempo, boolean correcto, String entrada) {
        this(columna,metodo,cantidad,vacios,tiempo,correcto,entrada,new double[0],0,0);
    }

    public ResultadoPrueba(String columna, String metodo, int cantidad, int vacios, double tiempo, boolean correcto, String entrada,
                           double[] muestras, int calentamientos, double tiempoCalentamiento) {
        this.columna = columna;
        this.metodo=metodo;
        this.cantidad=cantidad;
        this.vacios = vacios;
        this.tiempo=tiempo;
        this.correcto=correcto;
        this.entrada=entrada;
        this.muestras=muestras.clone();
        this.calentamientos=calentamientos;
        this.tiempoCalentamiento=tiempoCalentamiento;
    }

    public String getColumna() { return columna; }
    public String getMetodo() { return metodo; }
    public int getCantidad() { return cantidad; }
    public int getVacios() { return vacios; }
    public double getTiempo() { return tiempo; }
    public boolean isCorrecto() { return correcto; }
    public String getEntrada() { return entrada; }
    public double[] getMuestras() { return muestras.clone(); }
    public int getCalentamientos() { return calentamientos; }
    public double getTiempoCalentamiento() { return tiempoCalentamiento; }
}
