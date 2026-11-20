import com.example.practica6.logica.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.nio.ByteBuffer;
import java.util.*;

public class PruebaCSV {
    static void agregar(MessageDigest md, String[] fila) {
        for(String campo : fila) {
            byte[] bytes=campo.getBytes(StandardCharsets.UTF_8);
            md.update(ByteBuffer.allocate(4).putInt(bytes.length).array());
            md.update(bytes);
        }
    }
    public static void main(String[] args) throws Exception {
        Path temporal=Files.createTempFile(Path.of("diagnostico/resultados"),"csv-",".csv");
        try {
            Files.writeString(temporal,"\uFEFFa,b,c\n\"x,y\",\"dijo \"\"hola\"\"\",\n",StandardCharsets.UTF_8);
            DatosMusica d=LectorCSV.leer(temporal);
            if(!Arrays.equals(d.getColumnas(),new String[]{"a","b","c"}) ||
                    !Arrays.equals(d.getRegistros().get(0),new String[]{"x,y","dijo \"hola\"",""})) {
                throw new AssertionError("Campos CSV");
            }
            Files.writeString(temporal,"a,b\n1\n");
            try {
                LectorCSV.leer(temporal);
                throw new AssertionError("Acepto fila incompleta");
            } catch(java.io.IOException esperado) {
                System.out.println("OK detecta columnas incompletas");
            }
            Files.writeString(temporal,"a,b\n\"dos\nlineas\",x\n");
            try {
                LectorCSV.leer(temporal);
                throw new AssertionError("Cambio el soporte multilinea");
            } catch(IllegalArgumentException esperado) {
                System.out.println("LIMITACION CONFIRMADA: no admite campos multilinea");
            }
        } finally {
            Files.deleteIfExists(temporal);
        }
        DatosMusica datos=LectorCSV.leer();
        MessageDigest md=MessageDigest.getInstance("SHA-256");
        agregar(md,datos.getColumnas());
        for(String[] fila : datos.getRegistros()) {
            agregar(md,fila);
        }
        System.out.println("SHA256 campos Java="+HexFormat.of().formatHex(md.digest()));
    }
}
