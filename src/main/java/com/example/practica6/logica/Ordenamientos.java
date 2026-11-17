
package com.example.practica6.logica;

import java.util.Arrays;
import java.util.concurrent.CancellationException;

public class Ordenamientos {

    public static void seleccion(Integer[] a, DatosColumna columna) {

        int menor, k;

        for(int i=0; i < a.length-1; i++) {
            revisarCancelacion();
            menor = a[i];
            k=i;

            // Buscar el menor entre los elementos restantes
            for (int j=i+1; j<a.length; j++) {
                if(columna.comparar(a[j],menor)<0) {
                    menor=a[j];
                    k=j;
                }
            }

            a[k] = a[i];
            a[i]=menor;
        }
    }


    public static void shell(Integer[] a, DatosColumna columna) {

        int inter = a.length+1;
        int band, i, aux;

        while(inter>1) {
            inter=inter/2;
            band = 1;

            while(band==1) {
                revisarCancelacion();
                band=0;
                i=0;

                // Comparar elementos con el intervalo actual
                while(i+inter < a.length) {
                    if(columna.comparar(a[i],a[i+inter])>0) {
                        aux = a[i];
                        a[i]=a[i+inter];
                        a[i+inter] = aux;
                        band=1;
                    }
                    i++;
                }
            }
        }
    }


    public static void quicksort(Integer[] a, DatosColumna columna) {

        if(a.length>1) {
            quick(a,columna,0,a.length-1);
        }
    }

    private static void quick(Integer[] a, DatosColumna columna, int ini, int fin) {

        while(ini < fin) {
            revisarCancelacion();

            int izq=ini, der=fin, pos=ini;
            boolean band=true;
            int aux;

            // El pivote inicia en la primera posicion
            while (band) {
                band=false;

                while(pos!=der && columna.comparar(a[pos],a[der])<=0) {
                    der--;
                }

                if(pos!=der) {
                    aux=a[pos];
                    a[pos] = a[der];
                    a[der]=aux;
                    pos=der;

                    while(pos!=izq && columna.comparar(a[pos],a[izq])>=0) {
                        izq++;
                    }

                    if(pos!=izq) {
                        band=true;
                        aux = a[pos];
                        a[pos]=a[izq];
                        a[izq]=aux;
                        pos = izq;
                    }
                }
            }

            // Seguir con las partes que faltan
            if(pos-ini < fin-pos) {
                if(ini<pos-1) {
                    quick(a,columna,ini,pos-1);
                }

                ini=pos+1;
            }
            else {
                if(pos+1<fin) {
                    quick(a,columna,pos+1,fin);
                }

                fin = pos-1;
            }
        }
    }


    public static void mergesort(Integer[] a, DatosColumna columna) {

        Integer[] aux=new Integer[a.length];
        merge(a,aux,columna,0,a.length-1);
    }

    private static void merge(Integer[] a, Integer[] aux,
                              DatosColumna columna, int ini, int fin) {

        if(ini >= fin) {
            return;
        }

        revisarCancelacion();

        int medio=ini+(fin-ini)/2;

        // Dividir el arreglo en dos mitades
        merge(a,aux,columna,ini,medio);
        merge(a,aux,columna,medio+1,fin);

        int i=ini, j = medio+1, k=ini;

        // Unir ambas partes en orden
        while(i<=medio && j<=fin) {
            if(columna.comparar(a[i],a[j])<=0) {
                aux[k++] = a[i++];
            }else {
                aux[k++]=a[j++];
            }
        }

        while(i <= medio) {
            aux[k++]=a[i++];
        }

        while(j<=fin) {
            aux[k++] = a[j++];
        }

        for(int p=ini; p <= fin; p++) {
            a[p]=aux[p];
        }
    }


    public static void sortJava(Integer[] a, DatosColumna columna) {
        Arrays.sort(a, (x,y) -> columna.comparar(x,y));
    }

    public static void parallelSort(Integer[] a, DatosColumna columna) {
        Arrays.parallelSort(a, (x,y) -> columna.comparar(x,y));
    }


    private static void revisarCancelacion() {

        if(Thread.currentThread().isInterrupted()) {
            throw new CancellationException("Prueba cancelada");
        }
    }
}
