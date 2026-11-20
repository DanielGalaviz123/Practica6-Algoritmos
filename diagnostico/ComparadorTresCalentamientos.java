package com.example.practica6.logica;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ComparadorTresCalentamientos {

    public static final String[] METODOS = {
            "Seleccion directa", "Shell sort", "Quicksort",
            "Mergesort", "Arrays.sort", "Arrays.parallelSort"
    };

    private static final int CALENTAMIENTOS = 3;
    private static final int MEDICIONES = 9;

    public record Resumen(ArrayList<ResultadoPrueba> pruebas, Integer[] orden,
                          String columnaVista, int cantidad) { }

    public Resumen comparar(DatosMusica datos, String seleccionColumna, String seleccionMetodo,
                            int cantidad, Consumer<String> mensaje,
                            BiConsumer<Integer,Integer> progreso) {

        return comparar(datos, seleccionColumna, seleccionMetodo, cantidad, false, mensaje, progreso);
    }

    // Ambos escenarios son validos: orden original o una mezcla fija para todos
    public Resumen comparar(DatosMusica datos, String seleccionColumna, String seleccionMetodo,
                            int cantidad, boolean mezclarEntrada, Consumer<String> mensaje,
                            BiConsumer<Integer,Integer> progreso) {

        if(cantidad < 1 || cantidad > datos.getCantidad()) {
            throw new IllegalArgumentException("Cantidad de registros no valida");
        }

        ArrayList<ResultadoPrueba> resultados=new ArrayList<>();
        ArrayList<Integer> columnas = new ArrayList<>();
        ArrayList<String> metodos = new ArrayList<>();

        if(seleccionColumna.equals("Todas las columnas")) {
            for(int i=0; i<datos.getColumnas().length; i++) {
                columnas.add(i);
            }
        } else {
            int indice=datos.buscarColumna(seleccionColumna);
            if(indice < 0) {
                throw new IllegalArgumentException("Columna no encontrada");
            }
            columnas.add(indice);
        }

        if(seleccionMetodo.equals("Todos")) {
            for(String metodo : METODOS) {
                metodos.add(metodo);
            }
        } else {
            metodos.add(seleccionMetodo);
        }

        int total = columnas.size() * metodos.size();
        int paso=0;
        Integer[] ordenVista=null;
        String columnaVista="";
        int primeraColumna=columnas.get(0);

        for(int indice : columnas) {
            revisarCancelacion();

            DatosColumna valores=datos.prepararColumna(indice,cantidad);
            Integer[] original=new Integer[cantidad];
            for(int i=0; i<cantidad; i++) {
                original[i]=i;
            }

            // La misma mezcla se reutiliza en todos los algoritmos y en todas las rondas.
            // La semilla fija permite repetir el experimento sin cambiar la entrada.
            if(mezclarEntrada) {
                Collections.shuffle(Arrays.asList(original), new Random(2026L + indice * 101L + cantidad));
            }
            String tipoEntrada = mezclarEntrada ? "Mezclado" : "Original CSV";

            double[][] tiempos = new double[metodos.size()][MEDICIONES];

            // Alternar el orden de los algoritmos para no favorecer siempre al mismo
            ArrayList<Integer> ordenMetodos=new ArrayList<>();
            for(int i=0; i<metodos.size(); i++) {
                ordenMetodos.add(i);
            }
            Random aleatorio = new Random(2026L + indice);

            for(int ronda=0; ronda<CALENTAMIENTOS+MEDICIONES; ronda++) {
                revisarCancelacion();
                Collections.shuffle(ordenMetodos,aleatorio);

                for(int m : ordenMetodos) {
                    revisarCancelacion();
                    String metodo=metodos.get(m);

                    mensaje.accept("Midiendo " + valores.getNombre() + " con " + metodo);

                    // Todos reciben una copia nueva y con el mismo orden inicial
                    Integer[] copia=original.clone();
                    long inicio=System.nanoTime();

                    ejecutar(metodo,copia,valores);

                    long fin=System.nanoTime();
                    if(ronda<CALENTAMIENTOS) { continue; }
                    tiempos[m][ronda-CALENTAMIENTOS]=(fin-inicio)/1_000_000.0;

                    // Verificamos fuera del tiempo medido
                    if(!verificar(copia,valores,cantidad)) {
                        throw new IllegalStateException("Ordenamiento incorrecto: " + metodo);
                    }

                    if(indice==primeraColumna &&
                            (ordenVista==null || metodo.equals("Arrays.sort"))) {
                        ordenVista=copia;
                        columnaVista=valores.getNombre();
                    }
                }
            }

            // Una mediana por algoritmo, usando la misma cantidad de mediciones
            for(int m=0; m<metodos.size(); m++) {
                double[] ordenados=tiempos[m].clone();
                Arrays.sort(ordenados);
                double mediana=ordenados[MEDICIONES/2];

                resultados.add(new ResultadoPrueba(valores.getNombre(),metodos.get(m),
                        cantidad,valores.getVacios(),mediana,true,tipoEntrada));

                paso++;
                progreso.accept(paso,total);
            }
        }

        return new Resumen(resultados,ordenVista,columnaVista,cantidad);
    }

    private void ejecutar(String metodo, Integer[] a, DatosColumna columna) {
        switch(metodo) {
            case "Seleccion directa" -> Ordenamientos.seleccion(a,columna);
            case "Shell sort" -> Ordenamientos.shell(a,columna);
            case "Quicksort" -> Ordenamientos.quicksort(a,columna);
            case "Mergesort" -> Ordenamientos.mergesort(a,columna);
            case "Arrays.sort" -> Ordenamientos.sortJava(a,columna);
            case "Arrays.parallelSort" -> Ordenamientos.parallelSort(a,columna);
            default -> throw new IllegalArgumentException("Metodo desconocido: " + metodo);
        }
    }

    private boolean verificar(Integer[] a, DatosColumna columna, int cantidad) {
        if(a.length!=cantidad) {
            return false;
        }
        boolean[] vistos=new boolean[cantidad];

        for(Integer posicion : a) {
            if(posicion==null || posicion<0 || posicion>=cantidad || vistos[posicion]) {
                return false;
            }
            vistos[posicion]=true;
        }

        for(int i=1; i<a.length; i++) {
            if(columna.comparar(a[i-1],a[i]) > 0) {
                return false;
            }
        }
        return true;
    }

    private void revisarCancelacion() {
        if(Thread.currentThread().isInterrupted()) {
            throw new CancellationException("Prueba cancelada");
        }
    }
}

