import com.example.practica6.logica.*;
import java.util.*;
import java.io.*;
import java.nio.file.*;

public class CompararVersiones {
    public static void main(String[] args) throws Exception {
        DatosMusica datos=LectorCSV.leer();
        try(PrintWriter out=new PrintWriter(Files.newBufferedWriter(Path.of(args[0])))) {
            out.println("entrada,n,version,comparaciones");
            for(String nombre : new String[]{"danceability","todos_iguales"}) {
                int n=datos.getCantidad();
                String[] valores=new String[n];
                for(int i=0; i<n; i++) {
                    valores[i]=nombre.equals("danceability") ?
                            datos.getRegistros().get(i)[datos.buscarColumna(nombre)] : "";
                }
                for(boolean anterior : new boolean[]{true,false}) {
                    Investigacion.Contador c=new Investigacion.Contador(nombre,valores);
                    Integer[] a=Investigacion.indices(n);
                    if(anterior) {
                        ReferenciaAnterior.quicksort(a,c);
                    } else {
                        Ordenamientos.quicksort(a,c);
                    }
                    long total=c.comparaciones.sum();
                    Investigacion.verificar(a,c);
                    out.printf("%s,%d,%s,%d%n",nombre,n,anterior ? "HEAD_dos_vias" : "actual_tres_vias",total);
                    out.flush();
                }
            }
        }
        System.out.println("OK variantes anteriores y actuales");
    }
}
