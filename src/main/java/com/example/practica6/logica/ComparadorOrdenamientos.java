package com.example.practica6.logica;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ComparadorOrdenamientos {

    public static final String[] METODOS = {"Seleccion directa", "Shell sort", "Quicksort", "Mergesort", "Arrays.sort", "Arrays.parallelSort"};

    private static final int CALENTAMIENTOS= 3;
    private static final int MAX_CALENTAMIENTOS= 100;
    private static final long TIEMPO_CALENTAMIENTO_NS= 1_000_000_000L;
    private static final int MEDICIONES= 9;

    public record Resumen(ArrayList<ResultadoPrueba> pruebas, Integer[] orden, String columnaVista, int cantidad) { }

    public Resumen comparar(DatosMusica datos, String seleccionColumna, String seleccionMetodo, int cantidad, Consumer<String> mensaje, BiConsumer<Integer,Integer> progreso) {

        // por defecto usamos el orden original del csv
        return comparar(datos, seleccionColumna, seleccionMetodo, cantidad, false, mensaje, progreso);
    }

    public Resumen comparar(DatosMusica datos, String seleccionColumna, String seleccionMetodo, int cantidad, boolean mezclarEntrada, Consumer<String> mensaje, BiConsumer<Integer,Integer> progreso) {

        if(cantidad < 1 || cantidad > datos.getCantidad()) {
            throw new IllegalArgumentException("Cantidad de registros no valida");
        }

        ArrayList<ResultadoPrueba> resultados=new ArrayList<>();
        ArrayList<Integer> columnas = new ArrayList<>();
        ArrayList<String> metodos = new ArrayList<>();

        // revisamos si se selecciono una columna o todas
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

            // preparamos los valores y los indices originales
            DatosColumna valores=datos.prepararColumna(indice,cantidad);
            Integer[] original=new Integer[cantidad];

            for(int i=0; i<cantidad; i++) {
                original[i]=i;
            }

            if(mezclarEntrada) {
                Collections.shuffle(Arrays.asList(original), new Random(2026L + indice * 101L + cantidad));
            }

            String tipoEntrada = mezclarEntrada ? "Mezclado" : "Original CSV";

            double[][] tiempos = new double[metodos.size()][MEDICIONES];

            // cambiamos el orden de los algoritmos entre ejecuciones
            ArrayList<Integer> ordenMetodos=new ArrayList<>();

            for(int i=0; i<metodos.size(); i++) {
                ordenMetodos.add(i);
            }

            Random aleatorio = new Random(2026L + indice);

            int[] calentamientos=new int[metodos.size()];
            long[] tiempoCalentamiento=new long[metodos.size()];
            boolean pendientes=true;

            // calentamos los algoritmos antes de medir los tiempos finales
            while(pendientes) {
                revisarCancelacion();
                pendientes=false;
                Collections.shuffle(ordenMetodos,aleatorio);

                for(int m : ordenMetodos) {
                    if(calentamientos[m]>=CALENTAMIENTOS && (tiempoCalentamiento[m]>=TIEMPO_CALENTAMIENTO_NS || calentamientos[m]>=MAX_CALENTAMIENTOS)) {
                        continue;
                    }

                    revisarCancelacion();
                    mensaje.accept("Preparando " + valores.getNombre() + " con " + metodos.get(m));

                    Integer[] copia=original.clone();

                    long inicio=System.nanoTime();
                    ejecutar(metodos.get(m),copia,valores);

                    tiempoCalentamiento[m]+=System.nanoTime()-inicio;
                    calentamientos[m]++;

                    if(!verificar(copia,valores,cantidad)) {
                        throw new IllegalStateException("Ordenamiento incorrecto: " + metodos.get(m));
                    }

                    pendientes=true;
                }
            }

            // realizamos las nueve mediciones de cada algoritmo
            for(int ronda=0; ronda<MEDICIONES; ronda++) {
                revisarCancelacion();
                Collections.shuffle(ordenMetodos,aleatorio);

                for(int m : ordenMetodos) {
                    revisarCancelacion();
                    String metodo=metodos.get(m);

                    mensaje.accept("Midiendo " + valores.getNombre() + " con " + metodo);

                    // cada algoritmo recibe una copia con los mismos datos iniciales
                    Integer[] copia=original.clone();

                    // medimos solamente la ejecucion del algoritmo
                    long inicio=System.nanoTime();

                    ejecutar(metodo,copia,valores);

                    long fin=System.nanoTime();
                    tiempos[m][ronda]=(fin-inicio)/1_000_000.0;

                    // comprobamos el resultado fuera del tiempo medido
                    if(!verificar(copia,valores,cantidad)) {
                        throw new IllegalStateException("Ordenamiento incorrecto: " + metodo);
                    }

                    // guardamos el orden que se mostrara en la tabla
                    if(indice==primeraColumna &&
                            (ordenVista==null || metodo.equals("Arrays.sort"))) {
                        ordenVista=copia;
                        columnaVista=valores.getNombre();
                    }
                }
            }

            // calculamos la mediana de las nueve mediciones
            for(int m=0; m<metodos.size(); m++) {
                double[] ordenados=tiempos[m].clone();
                Arrays.sort(ordenados);
                double mediana=ordenados[MEDICIONES/2];

                // guardamos el resultado y los datos de la prueba
                resultados.add(new ResultadoPrueba(valores.getNombre(),metodos.get(m), cantidad,valores.getVacios(),mediana,true,tipoEntrada, tiempos[m],calentamientos[m],tiempoCalentamiento[m]/1_000_000.0));

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