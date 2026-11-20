package com.example.practica6.interfaz;

import com.example.practica6.logica.ComparadorOrdenamientos;
import com.example.practica6.logica.DatosMusica;
import com.example.practica6.logica.ResultadoPrueba;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Font;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

public class PanelGraficas {

    private VBox rendimiento;

    public PanelGraficas() {
        Label aviso = new Label("Ejecuta un ordenamiento para ver los tiempos");
        aviso.setTextFill(Color.BLACK);
        rendimiento = new VBox(12, aviso);
        rendimiento.setPadding(new Insets(20));
        rendimiento.setAlignment(Pos.TOP_CENTER);
        rendimiento.setStyle("-fx-background-color: white;");
    }

    public VBox getRendimiento() {
        return rendimiento;
    }

    // Resumen de cada columna, similar a la vista del CSV en Kaggle
    public static VBox crearEncabezado(DatosMusica datos, int indice) {
        String nombre = datos.getColumnas()[indice];
        Perfil perfil=analizar(datos,indice);

        String tipo=perfil.booleano ? "boolean" :
                perfil.fecha ? "date" : perfil.numerico ? "double" : "string";

        Label titulo = new Label(nombre);
        titulo.setFont(Font.font("Arial",13));
        titulo.setTextFill(Color.BLACK);
        titulo.setMaxWidth(200);
        Tooltip.install(titulo, new Tooltip(nombre));

        Label subtitulo=new Label(tipo);
        subtitulo.setTextFill(Color.GRAY);

        VBox cabecera=new VBox(5,titulo,subtitulo);
        cabecera.setPadding(new Insets(5));
        cabecera.setPrefHeight(167);
        cabecera.setMinHeight(167);
        cabecera.setAlignment(Pos.TOP_LEFT);

        if(perfil.validos==0) {
            cabecera.getChildren().add(new Label("Sin datos"));
        } else if(perfil.booleano) {
            cabecera.getChildren().add(crearMiniCircular(perfil));
        } else if(perfil.numerico || perfil.fecha) {
            cabecera.getChildren().add(crearMiniHistograma(datos,indice,perfil));
        } else {
            cabecera.getChildren().add(crearMiniTexto(perfil));
        }

        Label vacios = new Label("Nulos: " + perfil.vacios + " (" + porcentaje(perfil.vacios,perfil.total) + "%)");
        vacios.setFont(Font.font("Arial",10));
        vacios.setTextFill(Color.GRAY);
        cabecera.getChildren().add(vacios);
        return cabecera;
    }

    private static class Perfil {
        int total, validos, vacios;
        boolean numerico=true, booleano=true, fecha;
        boolean enteros=true;
        double minimo=Double.POSITIVE_INFINITY, maximo=Double.NEGATIVE_INFINITY;
        long diaMin=Long.MAX_VALUE, diaMax=Long.MIN_VALUE;
        HashMap<String,Integer> frecuencias=new HashMap<>();
    }

    private static Perfil analizar(DatosMusica datos, int indice) {
        Perfil p=new Perfil();
        p.total=datos.getCantidad();
        String nombre=datos.getColumnas()[indice];
        p.fecha=nombre.equals("first_week") || nombre.equals("last_week");

        for(String[] fila : datos.getRegistros()) {
            String valor=fila[indice]==null ? "" : fila[indice].trim();
            if(valor.isEmpty()) {
                p.vacios++;
                continue;
            }
            p.validos++;
            p.frecuencias.merge(valor,1,Integer::sum);

            if(p.booleano && !valor.equalsIgnoreCase("true") && !valor.equalsIgnoreCase("false")) {
                p.booleano=false;
            }

            if(p.numerico) {
                try {
                    double numero=Double.parseDouble(valor);
                    if(!Double.isFinite(numero)) {
                        p.numerico=false;
                    } else {
                        p.minimo=Math.min(p.minimo,numero);
                        p.maximo=Math.max(p.maximo,numero);
                        if(numero!=Math.rint(numero)) {
                            p.enteros=false;
                        }
                    }
                } catch(NumberFormatException e) {
                    p.numerico=false;
                }
            }

            if(p.fecha) {
                try {
                    long dia=LocalDate.parse(valor).toEpochDay();
                    p.diaMin=Math.min(p.diaMin,dia);
                    p.diaMax=Math.max(p.diaMax,dia);
                } catch(DateTimeParseException e) {
                    p.fecha=false;
                }
            }
        }
        if(p.validos==0) {
            p.booleano=false;
            p.numerico=false;
            p.fecha=false;
        }
        return p;
    }

    private static VBox crearMiniHistograma(DatosMusica datos, int indice, Perfil p) {
        int grupos=20;
        if(p.numerico && p.enteros && p.maximo-p.minimo<=15 && p.maximo>p.minimo) {
            grupos=(int)(p.maximo-p.minimo)+1;
        }
        if(p.numerico && p.maximo==p.minimo) {
            grupos=1;
        }
        grupos=Math.max(1,grupos);
        int[] conteos=new int[grupos];

        for(String[] fila : datos.getRegistros()) {
            String texto=fila[indice]==null ? "" : fila[indice].trim();
            if(texto.isEmpty()) { continue; }

            double valor;
            try {
                valor=p.fecha ? LocalDate.parse(texto).toEpochDay() : Double.parseDouble(texto);
            } catch(DateTimeParseException | NumberFormatException e) {
                continue;
            }
            double min=p.fecha ? p.diaMin : p.minimo;
            double max=p.fecha ? p.diaMax : p.maximo;
            int k=max==min ? 0 : (int)((valor-min)/(max-min)*grupos);
            k=Math.max(0,Math.min(grupos-1,k));
            conteos[k]++;
        }

        Canvas grafica = new Canvas(202,72);
        GraphicsContext g=grafica.getGraphicsContext2D();
        g.setStroke(Color.GRAY);
        g.strokeLine(1,70,201,70);
        int mayor=Arrays.stream(conteos).max().orElse(1);
        double ancho=200.0/grupos;
        g.setFill(Color.BLACK);
        for(int i=0; i<grupos; i++) {
            double alto=mayor==0 ? 0 : 65.0*conteos[i]/mayor;
            g.fillRect(1+i*ancho,70-alto,Math.max(1,ancho-1),alto);
        }

        String desde=p.fecha ? LocalDate.ofEpochDay(p.diaMin).toString() : formato(p.minimo);
        String hasta=p.fecha ? LocalDate.ofEpochDay(p.diaMax).toString() : formato(p.maximo);
        return new VBox(2,grafica,extremos(desde,hasta));
    }

    private static HBox crearMiniCircular(Perfil p) {
        int verdadero=0, falso=0;
        for(Map.Entry<String,Integer> e : p.frecuencias.entrySet()) {
            if(e.getKey().equalsIgnoreCase("true")) {
                verdadero+=e.getValue();
            } else if(e.getKey().equalsIgnoreCase("false")) {
                falso+=e.getValue();
            }
        }

        Canvas circulo = new Canvas(78,78);
        GraphicsContext g=circulo.getGraphicsContext2D();
        double angulo=90;
        int[] cantidades={verdadero,falso,p.vacios};
        Color[] tonos={Color.BLACK,Color.GRAY,Color.LIGHTGRAY};
        for(int i=0; i<cantidades.length; i++) {
            double arco=360.0*cantidades[i]/Math.max(1,p.total);
            g.setFill(tonos[i]);
            g.fillArc(4,4,70,70,angulo,-arco,ArcType.ROUND);
            angulo-=arco;
        }

        VBox etiquetas = new VBox(4,
                new Label("true: " + porcentaje(verdadero,p.total) + "%"),
                new Label("false: " + porcentaje(falso,p.total) + "%"),
                new Label("nulos: " + porcentaje(p.vacios,p.total) + "%"));
        etiquetas.setAlignment(Pos.CENTER_LEFT);
        return new HBox(6,circulo,etiquetas);
    }

    private static VBox crearMiniTexto(Perfil p) {
        int mayor=p.frecuencias.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        boolean muchosUnicos=p.frecuencias.size()>2000 &&
                p.vacios<p.total*0.03 && mayor<p.total*0.02;

        if(muchosUnicos) {
            Label numero=new Label(String.valueOf(p.frecuencias.size()));
            numero.setFont(Font.font("Arial",22));
            return new VBox(1,numero,new Label("valores unicos"));
        }

        ArrayList<Map.Entry<String,Integer>> principales=new ArrayList<>(p.frecuencias.entrySet());
        if(p.vacios>0) {
            principales.add(new AbstractMap.SimpleEntry<>("[nulo]",p.vacios));
        }
        principales.sort((a,b) -> {
            int cmp=Integer.compare(b.getValue(),a.getValue());
            return cmp!=0 ? cmp : a.getKey().compareToIgnoreCase(b.getKey());
        });

        VBox vista=new VBox(5);
        int acumulado=0;
        for(int i=0; i<Math.min(2,principales.size()); i++) {
            Map.Entry<String,Integer> e=principales.get(i);
            acumulado+=e.getValue();
            vista.getChildren().add(filaCategoria(e.getKey(),e.getValue(),p.total));
        }
        if(principales.size()>2) {
            vista.getChildren().add(filaCategoria("Otros",p.total-acumulado,p.total));
        }
        return vista;
    }

    private static HBox filaCategoria(String texto, int cantidad, int total) {
        String corto=texto.length()>18 ? texto.substring(0,15)+"..." : texto;
        Label nombre=new Label(corto);
        nombre.setPrefWidth(143);
        nombre.setMaxWidth(143);
        Tooltip.install(nombre,new Tooltip(texto));
        Label porcentaje=new Label(porcentaje(cantidad,total) + "%");
        return new HBox(4,nombre,porcentaje);
    }

    private static HBox extremos(String izquierda, String derecha) {
        Label a=new Label(izquierda);
        Label b=new Label(derecha);
        a.setFont(Font.font("Arial",10));
        b.setFont(Font.font("Arial",10));
        Region espacio=new Region();
        HBox.setHgrow(espacio,Priority.ALWAYS);
        HBox fila=new HBox(a,espacio,b);
        fila.setPrefWidth(202);
        return fila;
    }

    private static int porcentaje(int cantidad, int total) {
        return total==0 ? 0 : (int)Math.round(100.0*cantidad/total);
    }

    private static String formato(double valor) {
        if(Math.abs(valor)>=10000) {
            return String.format(Locale.US,"%.1fk",valor/1000.0);
        }
        if(valor==Math.rint(valor) && Math.abs(valor)<1000) {
            return String.format(Locale.US,"%.0f",valor);
        }
        return String.format(Locale.US,"%.2f",valor);
    }

    // Mostramos la comparacion completa y una vista de detalle cuando
    // seleccion directa hace que las demas barras casi no se distingan.
    public void mostrarTiempos(ArrayList<ResultadoPrueba> historial,
                               ArrayList<ResultadoPrueba> ultimaPrueba) {
        rendimiento.getChildren().clear();

        if(ultimaPrueba.isEmpty()) {
            rendimiento.getChildren().add(new Label("No hay resultados"));
            return;
        }

        Label titulo=new Label("Comparacion de tiempos");
        titulo.setFont(Font.font("Arial",19));
        titulo.setTextFill(Color.BLACK);
        rendimiento.getChildren().addAll(titulo,crearBarras(ultimaPrueba,false));

        boolean haySeleccion=false, hayOtros=false;
        for(ResultadoPrueba r : ultimaPrueba) {
            if(r.getMetodo().equals("Seleccion directa")) {
                haySeleccion=true;
            } else {
                hayOtros=true;
            }
        }

        if(haySeleccion && hayOtros) {
            Label detalle=new Label("Detalle");
            detalle.setFont(Font.font("Arial",17));
            detalle.setTextFill(Color.BLACK);

            Label explicacion=new Label("");
            explicacion.setTextFill(Color.GRAY);
            explicacion.setWrapText(true);

            rendimiento.getChildren().addAll(detalle,explicacion,crearBarras(ultimaPrueba,true));
        }
    }

    private BarChart<String,Number> crearBarras(ArrayList<ResultadoPrueba> pruebas,
                                                boolean sinSeleccion) {
        CategoryAxis ejeX=new CategoryAxis();
        ejeX.setLabel("Algoritmo");
        ejeX.setTickLabelRotation(-20);
        ejeX.setTickLabelFill(Color.BLACK);

        NumberAxis ejeY=new NumberAxis();
        ejeY.setLabel("Tiempo (ms)");
        ejeY.setTickLabelFill(Color.BLACK);
        ejeY.setForceZeroInRange(true);

        BarChart<String,Number> barras=new BarChart<>(ejeX,ejeY);
        barras.setAnimated(false);
        barras.setLegendVisible(false);
        barras.setCategoryGap(13);
        barras.setBarGap(5);
        barras.setPrefHeight(360);
        barras.setMinHeight(325);

        XYChart.Series<String,Number> serie=new XYChart.Series<>();
        for(String metodo : ComparadorOrdenamientos.METODOS) {
            if(sinSeleccion && metodo.equals("Seleccion directa")) {
                continue;
            }

            double suma=0;
            int cuenta=0;
            for(ResultadoPrueba r : pruebas) {
                if(r.getMetodo().equals(metodo)) {
                    suma+=r.getTiempo();
                    cuenta++;
                }
            }
            if(cuenta==0) {
                continue;
            }

            double tiempo=suma/cuenta;
            XYChart.Data<String,Number> dato=new XYChart.Data<>(metodo,tiempo);
            Tooltip aviso=new Tooltip(String.format(Locale.US,"%s: %.3f ms",metodo,tiempo));
            dato.nodeProperty().addListener((obs,anterior,actual) -> {
                if(actual!=null) {
                    actual.setStyle("-fx-bar-fill: #181818;");
                    Tooltip.install(actual,aviso);
                }
            });
            serie.getData().add(dato);
        }

        barras.getData().add(serie);
        return barras;
    }
}
