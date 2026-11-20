package com.example.practica6.interfaz;

import com.example.practica6.logica.*;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Locale;

public class VentanaPrincipal extends BorderPane {

    private ComboBox<String> columnas, metodos;
    private ComboBox<Integer> tamanos;
    private Button btnOrdenar, btnCancelar, btnRecargar;
    private TablaMusica tabla;
    private PanelGraficas graficas;
    private TableView<ResultadoPrueba> tablaTiempos;
    private TabPane pestanas;
    private Tab tabDatos, tabTiempos;
    private ScrollPane panelRendimiento;

    private DatosMusica datos;
    private Path archivoActual=LectorCSV.RUTA;
    private Task<?> tareaActual;
    private ArrayList<ResultadoPrueba> historial=new ArrayList<>();

    public VentanaPrincipal() {
        crearInterfaz();
        cargarArchivo(archivoActual);
    }

    private void crearInterfaz() {
        setBackground(new Background(new BackgroundFill(Color.WHITE,null,null)));

        Label titulo=new Label("Ordenamientos");
        titulo.setFont(Font.font("Arial",25));
        titulo.setTextFill(Color.WHITE);

        HBox superior=new HBox(titulo);
        superior.setAlignment(Pos.CENTER_LEFT);
        superior.setPadding(new Insets(19,25,19,25));
        superior.setBackground(new Background(new BackgroundFill(Color.BLACK,null,null)));
        setTop(superior);

        Label opciones=new Label("Opciones de prueba");
        opciones.setFont(Font.font("Arial",19));
        opciones.setTextFill(Color.BLACK);

        btnRecargar=new Button("Recargar CSV");
        configurarBoton(btnRecargar,false);

        columnas=new ComboBox<>();
        columnas.setPromptText("Seleccionar columna");
        columnas.setMaxWidth(Double.MAX_VALUE);
        columnas.setDisable(true);
        configurarCombo(columnas);

        metodos=new ComboBox<>();
        metodos.getItems().addAll(ComparadorOrdenamientos.METODOS);
        metodos.getItems().add("Todos");
        metodos.setValue("Todos");
        metodos.setMaxWidth(Double.MAX_VALUE);
        configurarCombo(metodos);

        tamanos=new ComboBox<>();
        tamanos.setMaxWidth(Double.MAX_VALUE);
        tamanos.setDisable(true);
        configurarCombo(tamanos);

        btnOrdenar=new Button("Ejecutar ordenamiento");
        configurarBoton(btnOrdenar,true);
        btnOrdenar.setDisable(true);

        btnCancelar=new Button("Detener prueba");
        configurarBoton(btnCancelar,false);
        btnCancelar.setDisable(true);

        VBox lateral=new VBox(12,
                opciones, btnRecargar,
                new Separator(), new Label("Columna:"), columnas,
                new Label("Metodo:"), metodos,
                new Label("Cantidad de registros:"), tamanos,
                btnOrdenar, btnCancelar
        );
        lateral.setPadding(new Insets(23));
        lateral.setPrefWidth(275);
        lateral.setBackground(new Background(new BackgroundFill(Color.WHITE,null,null)));

        ScrollPane panelLateral=new ScrollPane(lateral);
        panelLateral.setFitToWidth(true);
        panelLateral.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        panelLateral.setPrefWidth(280);
        panelLateral.setStyle("-fx-background-color: white; -fx-border-color: #bbbbbb;");
        setLeft(panelLateral);

        tabla=new TablaMusica();
        graficas=new PanelGraficas();
        tablaTiempos=crearTablaTiempos();

        tabDatos=new Tab("Datos del CSV",tabla);

        panelRendimiento=new ScrollPane(graficas.getRendimiento());
        panelRendimiento.setFitToWidth(true);
        panelRendimiento.setStyle("-fx-background-color: white;");

        SplitPane resultados=new SplitPane(tablaTiempos,panelRendimiento);
        resultados.setOrientation(javafx.geometry.Orientation.VERTICAL);
        resultados.setDividerPositions(0.48);
        tabTiempos=new Tab("Rendimiento",resultados);

        pestanas=new TabPane(tabDatos,tabTiempos);
        for(Tab t : pestanas.getTabs()) {
            t.setClosable(false);
        }
        pestanas.setStyle("-fx-accent: black; -fx-focus-color: black; -fx-faint-focus-color: transparent;");
        setCenter(pestanas);

        btnRecargar.setOnAction(e -> cargarArchivo(archivoActual));
        btnOrdenar.setOnAction(e -> ejecutarPrueba());
        btnCancelar.setOnAction(e -> {
            if(tareaActual!=null) {
                tareaActual.cancel(true);
            }
        });
    }

    private void configurarBoton(Button boton, boolean principal) {
        boton.setMaxWidth(Double.MAX_VALUE);
        boton.setTextFill(principal ? Color.WHITE : Color.BLACK);
        boton.setBackground(new Background(new BackgroundFill(
                principal ? Color.BLACK : Color.WHITE,new CornerRadii(3),null)));
        boton.setBorder(new Border(new BorderStroke(Color.BLACK,
                BorderStrokeStyle.SOLID,new CornerRadii(3),new BorderWidths(1))));
        boton.setPadding(new Insets(10));
    }

    private void configurarCombo(ComboBox<?> combo) {
        combo.setBackground(new Background(new BackgroundFill(Color.WHITE,null,null)));
        combo.setBorder(new Border(new BorderStroke(Color.GRAY,
                BorderStrokeStyle.SOLID,CornerRadii.EMPTY,new BorderWidths(1))));
    }

    private TableView<ResultadoPrueba> crearTablaTiempos() {
        TableView<ResultadoPrueba> vista=new TableView<>();
        vista.setPlaceholder(new Label("Ejecuta una prueba para ver los tiempos"));
        vista.setItems(FXCollections.observableArrayList());
        vista.setStyle("-fx-accent: #222222; -fx-focus-color: black; " +
                "-fx-selection-bar: #222222; -fx-selection-bar-non-focused: #666666;");

        TableColumn<ResultadoPrueba,String> cColumna=new TableColumn<>("Columna");
        cColumna.setPrefWidth(190);
        cColumna.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getColumna()));

        TableColumn<ResultadoPrueba,String> cMetodo=new TableColumn<>("Metodo");
        cMetodo.setPrefWidth(165);
        cMetodo.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getMetodo()));

        TableColumn<ResultadoPrueba,String> cCantidad=new TableColumn<>("Registros");
        cCantidad.setPrefWidth(100);
        cCantidad.setCellValueFactory(c -> new ReadOnlyStringWrapper(String.valueOf(c.getValue().getCantidad())));

        TableColumn<ResultadoPrueba,String> cTiempo=new TableColumn<>("Tiempo (ms)");
        cTiempo.setPrefWidth(120);
        cTiempo.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                String.format(Locale.US,"%.3f",c.getValue().getTiempo())));

        vista.getColumns().addAll(cColumna,cMetodo,cCantidad,cTiempo);
        return vista;
    }

    private void cargarArchivo(Path ruta) {
        if(tareaActual!=null && tareaActual.isRunning()) {
            return;
        }

        btnOrdenar.setDisable(true);
        btnRecargar.setDisable(true);
        btnRecargar.setText("Leyendo CSV...");

        Task<DatosMusica> carga=new Task<>() {
            @Override
            protected DatosMusica call() throws Exception {
                return LectorCSV.leer(ruta);
            }
        };

        tareaActual=carga;
        carga.setOnSucceeded(e -> {
            archivoActual=ruta;
            datos=carga.getValue();
            tabla.mostrarDatos(datos);

            columnas.getItems().clear();
            columnas.getItems().add("Todas las columnas");
            columnas.getItems().addAll(datos.getColumnas());
            columnas.setDisable(false);

            String inicial=datos.buscarColumna("danceability")>=0 ? "danceability" : datos.getColumnas()[0];
            columnas.setValue(inicial);

            tamanos.getItems().clear();
            int[] opciones={20000,datos.getCantidad()};
            for(int n : opciones) {
                if(n<=datos.getCantidad() && !tamanos.getItems().contains(n)) {
                    tamanos.getItems().add(n);
                }
            }
            tamanos.setValue(datos.getCantidad()>=1000 ? 1000 : datos.getCantidad());
            tamanos.setDisable(false);

            btnRecargar.setDisable(false);
            btnOrdenar.setDisable(false);
            btnRecargar.setText("Recargar CSV");

            historial.clear();
            tablaTiempos.getItems().clear();
            graficas=new PanelGraficas();
            panelRendimiento.setContent(graficas.getRendimiento());
            pestanas.getSelectionModel().select(tabDatos);
        });

        carga.setOnFailed(e -> {
            btnRecargar.setText("Recargar CSV");
            btnRecargar.setDisable(false);
            btnOrdenar.setDisable(datos==null);
            mostrarError("Error al leer el CSV",carga.getException().getMessage());
        });

        Thread hilo=new Thread(carga,"lectura-csv");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void ejecutarPrueba() {
        if(datos==null || tamanos.getValue()==null || columnas.getValue()==null) {
            return;
        }

        String columna=columnas.getValue();
        String metodo=metodos.getValue();
        int n=tamanos.getValue();

        btnOrdenar.setDisable(true);
        btnOrdenar.setText("Ordenando...");
        btnCancelar.setDisable(false);
        btnRecargar.setDisable(true);
        columnas.setDisable(true);
        metodos.setDisable(true);
        tamanos.setDisable(true);

        Task<ComparadorOrdenamientos.Resumen> tarea=new Task<>() {
            @Override
            protected ComparadorOrdenamientos.Resumen call() {
                // Siempre usa el orden original del CSV
                return new ComparadorOrdenamientos().comparar(datos,columna,metodo,n,
                        texto -> updateMessage(texto),
                        (actual,total) -> updateProgress(actual,total));
            }
        };

        tareaActual=tarea;
        tarea.setOnSucceeded(e -> {
            ComparadorOrdenamientos.Resumen resumen=tarea.getValue();
            ArrayList<ResultadoPrueba> resultados=resumen.pruebas();
            historial.addAll(resultados);

            tablaTiempos.getItems().addAll(resultados);
            graficas.mostrarTiempos(historial,resultados);
            tabla.mostrarOrden(datos,resumen.orden());
            pestanas.getSelectionModel().select(tabTiempos);
            terminarPrueba();
        });

        tarea.setOnCancelled(e -> terminarPrueba());

        tarea.setOnFailed(e -> {
            Throwable error=tarea.getException();
            terminarPrueba();
            mostrarError("Error al ordenar",error==null ? "Error desconocido" : error.getMessage());
        });

        Thread hilo=new Thread(tarea,"prueba-ordenamientos");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void terminarPrueba() {
        btnOrdenar.setText("Ejecutar ordenamiento");
        btnOrdenar.setDisable(false);
        btnCancelar.setDisable(true);
        btnRecargar.setDisable(false);
        columnas.setDisable(false);
        metodos.setDisable(false);
        tamanos.setDisable(false);
    }

    private void mostrarError(String titulo, String detalle) {
        Alert aviso=new Alert(Alert.AlertType.ERROR);
        aviso.setTitle(titulo);
        aviso.setHeaderText(titulo);
        aviso.setContentText(detalle);
        aviso.showAndWait();
    }
}
