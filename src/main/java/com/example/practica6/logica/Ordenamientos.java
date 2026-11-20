package com.example.practica6.logica;

import java.util.Arrays;
import java.util.concurrent.CancellationException;

public class Ordenamientos {

    public static void seleccion(Integer[] a, DatosColumna columna) {
        Integer menor;
        int k;

        for(int i=0; i<a.length-1; i++) {
            revisarCancelacion();
            menor=a[i];
            k=i;

            // Buscar el menor entre los elementos restantes
            for(int j=i+1; j<a.length; j++) {
                if(columna.comparar(a[j],menor)<0) {
                    menor=a[j];
                    k=j;
                }
            }
            a[k]=a[i];
            a[i]=menor;
        }
    }

    public static void shell(Integer[] a, DatosColumna columna) {
        int inter=a.length+1;
        int band,i;
        Integer aux;

        while(inter>1) {
            inter=inter/2;
            band=1;

            while(band==1) {
                revisarCancelacion();
                band=0;
                i=0;

                // Comparar elementos con el intervalo actual
                while(i+inter<a.length) {
                    if(columna.comparar(a[i],a[i+inter])>0) {
                        aux=a[i];
                        a[i]=a[i+inter];
                        a[i+inter]=aux;
                        band=1;
                    }
                    i++;
                }
            }
        }
    }

    public static void quicksort(Integer[] a, DatosColumna columna) {
        if(a.length > 1) {
            quick(a,columna,0,a.length-1);
        }
    }

    private static void quick(Integer[] a, DatosColumna columna, int ini, int fin) {
        while(ini < fin) {
            revisarCancelacion();

            Integer pivote=a[ini];
            int izq=ini, i=ini+1, der=fin;

            while(i <= der) {
                int comparacion=columna.comparar(a[i],pivote);

                if(comparacion < 0) {
                    Integer aux=a[izq];
                    a[izq]=a[i];
                    a[i]=aux;
                    izq++;
                    i++;
                }
                else if(comparacion > 0) {
                    Integer aux=a[i];
                    a[i]=a[der];
                    a[der]=aux;
                    der--;
                }
                else {
                    i++;
                }
            }

            if(izq-ini < fin-der) {
                if(ini < izq-1) {
                    quick(a,columna,ini,izq-1);
                }
                ini=der+1;
            }
            else {
                if(der+1 < fin) {
                    quick(a,columna,der+1,fin);
                }
                fin=izq-1;
            }
        }
    }

    public static void mergesort(Integer[] a, DatosColumna columna) {
        if(a.length>1) {
            merge(a,columna,0,a.length-1);
        }
    }

    private static void merge(Integer[] a, DatosColumna columna, int ini, int fin) {
        if(ini>=fin) {
            return;
        }

        revisarCancelacion();
        int medio=(ini+fin)/2;

        merge(a,columna,ini,medio);
        merge(a,columna,medio+1,fin);

        combinar(a,columna,ini,medio,fin);
    }

    private static void combinar(Integer[] a, DatosColumna columna, int ini, int medio, int fin) {
        int n1=medio-ini+1;
        int n2=fin-medio;

        Integer[] izquierda=new Integer[n1];
        Integer[] derecha=new Integer[n2];

        for(int i=0; i<n1; i++) {
            izquierda[i]=a[ini+i];
        }
        for(int j=0; j<n2; j++) {
            derecha[j]=a[medio+1+j];
        }

        int i=0, j=0, k=ini;

        while(i<n1 && j<n2) {
            if(columna.comparar(izquierda[i],derecha[j])<=0) {
                a[k]=izquierda[i];
                i++;
            } else {
                a[k]=derecha[j];
                j++;
            }
            k++;
        }

        while(i<n1) {
            a[k]=izquierda[i];
            i++;
            k++;
        }

        while(j<n2) {
            a[k]=derecha[j];
            j++;
            k++;
        }
    }

    public static void sortJava(Integer[] a, DatosColumna columna) {
        Arrays.sort(a,(x,y) -> columna.comparar(x,y));
    }

    public static void parallelSort(Integer[] a, DatosColumna columna) {
        Arrays.parallelSort(a,(x,y) -> columna.comparar(x,y));
    }

    private static void revisarCancelacion() {
        if(Thread.currentThread().isInterrupted()) {
            throw new CancellationException("Prueba cancelada");
        }
    }
}
