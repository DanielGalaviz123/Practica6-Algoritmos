package com.example.practica6.interfaz;

import com.example.practica6.logica.DatosMusica;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class TablaMusica extends TableView<String[]> {

    private DatosMusica datosActuales;

    public TablaMusica() {
        setPlaceholder(new Label("No hay datos para mostrar"));
        setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        setStyle("-fx-accent: #222222; -fx-focus-color: black; " +
                "-fx-selection-bar: #222222; -fx-selection-bar-non-focused: #666666;");
    }

    public void mostrarDatos(DatosMusica datos) {
        // Los encabezados se generan una sola vez por archivo cargado
        if(datosActuales != datos || getColumns().isEmpty()) {
            getColumns().clear();
            String[] nombres = datos.getColumnas();

            for(int i=0; i<nombres.length; i++) {
                int indice=i;
                TableColumn<String[],String> columna=new TableColumn<>();

                columna.setGraphic(PanelGraficas.crearEncabezado(datos,indice));
                columna.setPrefWidth(225);
                columna.setSortable(false);

                if(nombres[i].equals("song") || nombres[i].equals("performer")) {
                    columna.setPrefWidth(245);
                }

                columna.setCellValueFactory(fila ->
                        new ReadOnlyStringWrapper(fila.getValue()[indice]));

                getColumns().add(columna);
            }
            datosActuales=datos;
        }

        setItems(FXCollections.observableArrayList(datos.getRegistros()));
    }

    public void mostrarOrden(DatosMusica datos, Integer[] posiciones) {
        ObservableList<String[]> filas=FXCollections.observableArrayList();

        // Se mueven las filas completas usando sus posiciones originales
        for(int posicion : posiciones) {
            filas.add(datos.getRegistros().get(posicion));
        }
        setItems(filas);
    }
}
