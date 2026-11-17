package com.example.practica6.logica;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class LectorCSV {

    public static final Path RUTA = Path.of("C:\\Users\\danie\\Downloads\\billboard_hot_100_spotify_audio_features_enriched.csv");

    public static DatosMusica leer() throws IOException {
        return leer(RUTA);
    }

    public static DatosMusica leer(Path ruta) throws IOException {

        try (BufferedReader lector=Files.newBufferedReader(ruta,StandardCharsets.UTF_8)) {
            String linea = lector.readLine();
            if(linea==null) {
                throw new IOException("El archivo esta vacio");
            }

            String[] encabezados=separarCampos(linea);
            encabezados[0] = encabezados[0].replace("\uFEFF", "");

            DatosMusica datos = new DatosMusica(encabezados);
            int fila=1;

            while((linea = lector.readLine()) != null) {
                fila++;
                String[] registro=separarCampos(linea);

                if(registro.length!=encabezados.length) {
                    throw new IOException("La fila " + fila + " tiene " + registro.length + " campos");
                }

                datos.agregarRegistro(registro);
            }
            return datos;
        }
    }

    private static String[] separarCampos(String linea) {
        ArrayList<String> campos=new ArrayList<>();
        StringBuilder texto = new StringBuilder();
        boolean comillas=false;

        for(int i=0; i<linea.length(); i++) {
            char c = linea.charAt(i);

            if(c=='"') {
                // Dos comillas seguidas representan una comilla en el texto
                if(comillas && i+1<linea.length() && linea.charAt(i+1)=='"') {
                    texto.append('"');
                    i++;
                } else {
                    comillas=!comillas;
                }
            }
            else if(c==',' && !comillas) {
                campos.add(texto.toString());
                texto.setLength(0);
            }
            else texto.append(c);
        }

        if(comillas) {
            throw new IllegalArgumentException("Comillas sin cerrar en el CSV");
        }

        campos.add(texto.toString());
        return campos.toArray(new String[0]);
    }
}
