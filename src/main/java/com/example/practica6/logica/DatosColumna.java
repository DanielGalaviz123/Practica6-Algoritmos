
package com.example.practica6.logica;

public class DatosColumna {

    private String nombre;
    private String[] valores;
    private double[] numeros;
    private boolean[] vacio;
    private boolean numerica;
    private int cantidadVacios;

    public DatosColumna(String nombre, String[] valores) {
        this.nombre=nombre;
        this.valores=valores;
        numeros = new double[valores.length];
        vacio = new boolean[valores.length];
        numerica=true;

        for(int i = 0; i<valores.length; i++) {
            String dato = valores[i].trim();

            if(dato.isEmpty()) {
                vacio[i]=true;
                cantidadVacios++;
                continue;
            }

            try {
                numeros[i] = Double.parseDouble(dato);
                if(!Double.isFinite(numeros[i])) {
                    numerica=false;
                }
            } catch(NumberFormatException e) {
                numerica=false;
            }
        }

        if(cantidadVacios==valores.length) {
            numerica=false;
        }
    }

    // Se comparan las posiciones para conservar completa cada fila
    public int comparar(int a, int b) {
        if(vacio[a]) {
            return vacio[b] ? 0 : 1;
        }
        if(vacio[b]) {
            return -1;
        }

        if(numerica) {
            return Double.compare(numeros[a],numeros[b]);
        }

        return valores[a].compareToIgnoreCase(valores[b]);
    }

    public String getNombre() { return nombre; }
    public int getCantidad() { return valores.length; }
    public boolean esNumerica() { return numerica; }
    public int getVacios() { return cantidadVacios; }
    public String getValor(int i) { return valores[i]; }
    public double getNumero(int i) { return numeros[i]; }
    public boolean esVacio(int i) { return vacio[i]; }
}
