
package com.example.practica6.logica;

import java.util.ArrayList;

public class DatosMusica {

    private String[] columnas;
    private ArrayList<String[]> registros;

    public DatosMusica(String[] columnas) {
        this.columnas = columnas;
        registros=new ArrayList<>();
    }

    // Cada registro corresponde a una fila del archivo
    public void agregarRegistro(String[] datos) {
        registros.add(datos);
    }

    public String[] getColumnas() {
        return columnas;
    }

    public ArrayList<String[]> getRegistros() {
        return registros;
    }

    public int getCantidad() {
        return registros.size();
    }

    public int buscarColumna(String nombre) {
        for(int i=0; i<columnas.length; i++) {
            if(columnas[i].equals(nombre)) {
                return i;
            }
        }
        return -1;
    }

    public DatosColumna prepararColumna(int indice, int cantidad) {
        String[] valores = new String[cantidad];

        for (int i = 0; i<cantidad; i++) {
            valores[i]=registros.get(i)[indice];
        }
        return new DatosColumna(columnas[indice],valores);
    }
}
