
package com.example.practica6.logica;

import java.util.Arrays;
import java.util.Random;

public class Prueba {

    public static void main(String[] args) {
        int[] tamanos = {10,1000,3000,5000};
        Random random=new Random(42);

        for (int n : tamanos) {
            String[] numeros=new String[n];
            for(int i=0; i<n; i++) {
                numeros[i]=String.valueOf(random.nextInt(100000)/100.0);
            }

            DatosColumna datos = new DatosColumna("Numeros de prueba",numeros);
            Integer[] original=new Integer[n];
            for(int i=0; i<n; i++) original[i]=i;

            System.out.println("\nCantidad de datos: " + n);
            if(n==10) {
                System.out.println("Original: " + Arrays.toString(numeros));
            }

            for(String metodo : ComparadorOrdenamientos.METODOS) {
                // Mismos numeros para cada ordenamiento
                Integer[] copia=original.clone();
                long inicio=System.nanoTime();

                switch(metodo) {
                    case "Seleccion directa" -> Ordenamientos.seleccion(copia,datos);
                    case "Shell sort" -> Ordenamientos.shell(copia,datos);
                    case "Quicksort" -> Ordenamientos.quicksort(copia,datos);
                    case "Mergesort" -> Ordenamientos.mergesort(copia,datos);
                    case "Arrays.sort" -> Ordenamientos.sortJava(copia,datos);
                    case "Arrays.parallelSort" -> Ordenamientos.parallelSort(copia,datos);
                }

                long fin=System.nanoTime();
                boolean correcto=true;
                for(int i=1; i<n; i++) {
                    if(datos.comparar(copia[i-1],copia[i])>0) {
                        correcto=false;
                    }
                }

                System.out.printf("%-20s %9.3f ms   %s%n",metodo, (fin-inicio)/1000000.0, correcto ? "OK" : "ERROR");

                if(n==10) {
                    String[] ordenados=new String[n];
                    for(int i=0; i<n; i++) ordenados[i]=numeros[copia[i]];
                    System.out.println(Arrays.toString(ordenados));
                }
            }
        }
    }
}
