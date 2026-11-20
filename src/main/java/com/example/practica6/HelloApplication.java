package com.example.practica6;

import com.example.practica6.interfaz.VentanaPrincipal;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) {

        VentanaPrincipal ventana = new VentanaPrincipal();
        Scene scene = new Scene(ventana, 1150, 750);

        stage.setTitle("Practica 6");
        stage.setScene(scene);
        stage.setMinWidth(950);
        stage.setMinHeight(650);
        stage.show();
    }
}