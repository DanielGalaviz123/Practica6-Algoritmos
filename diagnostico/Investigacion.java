import com.example.practica6.logica.*;
import java.nio.file.*;
import java.io.*;
import java.lang.management.*;
import java.util.*;
import java.util.concurrent.atomic.LongAdder;
import jdk.jfr.*;

/** Pruebas independientes de JavaFX; nunca modifica el CSV. */
public class Investigacion {
    static final String[] METODOS=ComparadorOrdenamientos.METODOS;
    static final com.sun.management.ThreadMXBean MEMORIA=
            (com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
    static long verificaciones;

    @Name("practica.Muestra")
    @Label("Ordenamiento")
    static class Muestra extends Event {
        String metodo;
        int ronda;
        int cantidad;
        double ms;
    }

    static class Contador extends DatosColumna {
        final LongAdder comparaciones=new LongAdder();
        Contador(String nombre, String[] valores) {
            super(nombre,valores);
        }
        @Override public int comparar(int a, int b) {
            comparaciones.increment();
            return super.comparar(a,b);
        }
    }

    static void ejecutar(int m, Integer[] a, DatosColumna c) {
        switch(m) {
            case 0 -> Ordenamientos.seleccion(a,c);
            case 1 -> Ordenamientos.shell(a,c);
            case 2 -> Ordenamientos.quicksort(a,c);
            case 3 -> Ordenamientos.mergesort(a,c);
            case 4 -> Ordenamientos.sortJava(a,c);
            case 5 -> Ordenamientos.parallelSort(a,c);
            default -> throw new IllegalArgumentException();
        }
    }

    static Integer[] indices(int n) {
        Integer[] a=new Integer[n];
        for(int i=0; i<n; i++) {
            a[i]=i;
        }
        return a;
    }

    static void verificar(Integer[] a, DatosColumna c) {
        if(a.length!=c.getCantidad()) {
            throw new AssertionError("Longitud");
        }
        boolean[] vistos=new boolean[a.length];
        for(int i=0; i<a.length; i++) {
            Integer k=a[i];
            if(k==null || k<0 || k>=a.length || vistos[k]) {
                throw new AssertionError("No es una permutacion");
            }
            vistos[k]=true;
            if(i>0 && c.comparar(a[i-1],k)>0) {
                throw new AssertionError("Desorden: "+i);
            }
        }
        verificaciones++;
    }

    static long gc() {
        long n=0;
        for(GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            n+=Math.max(0,bean.getCollectionCount());
        }
        return n;
    }

    static long compilacion() {
        CompilationMXBean bean=ManagementFactory.getCompilationMXBean();
        return bean==null ? 0 : bean.getTotalCompilationTime();
    }

    static void funcional() throws Exception {
        List<String[]> casos=new ArrayList<>();
        casos.add(new String[0]);
        casos.add(new String[]{"1"});
        casos.add(new String[]{"1","2"});
        casos.add(new String[]{"2","1"});
        casos.add(new String[]{"-5","-0.0","0","3.14","-2.5","","  "});
        casos.add(new String[]{"z","A","a","50 Cent","B2","b10","","  "});
        casos.add(new String[]{"NaN","Infinity","-Infinity","1",""});
        Random r=new Random(2026);
        for(int n : new int[]{2,10,100,500,1000,3000,5000}) {
            for(int tipo=0; tipo<7; tipo++) {
                String[] v=new String[n];
                for(int i=0; i<n; i++) {
                    v[i]=switch(tipo) {
                        case 0 -> ""+i;
                        case 1 -> ""+(n-i);
                        case 2 -> "7";
                        case 3 -> "";
                        case 4 -> ""+(r.nextInt(200000)-100000)/100.0;
                        case 5 -> i%5==0 ? "" : ""+r.nextInt(5);
                        default -> "artista "+r.nextInt(n);
                    };
                }
                casos.add(v);
            }
        }
        // Exhaustivo sobre tres valores, incluidos vacios.
        for(int n=0; n<=7; n++) {
            int limite=(int)Math.pow(3,n);
            for(int k=0; k<limite; k++) {
                String[] v=new String[n];
                int x=k;
                for(int i=0; i<n; i++) {
                    v[i]=new String[]{"","-1","2"}[x%3];
                    x/=3;
                }
                casos.add(v);
            }
        }
        for(String[] v : casos) {
            DatosColumna c=new DatosColumna("caso",v);
            Integer[] esperado=indices(v.length);
            Arrays.sort(esperado,c::comparar);
            for(int m=0; m<6; m++) {
                Integer[] a=indices(v.length);
                ejecutar(m,a,c);
                verificar(a,c);
                for(int i=0; i<a.length; i++) {
                    if(c.comparar(a[i],esperado[i])!=0) {
                        throw new AssertionError("Diferencia con referencia");
                    }
                }
            }
        }
        System.out.println("OK: "+casos.size()+" casos, "+verificaciones+" ordenamientos");
    }

    static void repetir(DatosMusica datos, String nombre, int n, int rondas,
                        int solo, Path salida, boolean grabar) throws Exception {
        DatosColumna c=datos.prepararColumna(datos.buscarColumna(nombre),n);
        Integer[] original=indices(n);
        Recording recording=null;
        if(grabar) {
            recording=new Recording();
            recording.enable("jdk.Compilation").withThreshold(java.time.Duration.ZERO);
            recording.enable("jdk.GarbageCollection");
            recording.enable("jdk.GCPhasePause");
            recording.enable("practica.Muestra").withThreshold(java.time.Duration.ZERO);
            recording.start();
        }
        try(PrintWriter out=new PrintWriter(Files.newBufferedWriter(salida))) {
            out.println("columna,n,ronda,metodo,ms,gc_delta,jit_ms_delta,bytes_hilo,hash_entrada");
            Random random=new Random(2026);
            List<Integer> orden=new ArrayList<>();
            for(int m=0; m<6; m++) {
                if((solo==-1 || solo==-2 && m>0) || solo==m) {
                    orden.add(m);
                }
            }
            for(int ronda=0; ronda<rondas; ronda++) {
                Collections.shuffle(orden,random);
                for(int m : orden) {
                    Integer[] a=original.clone();
                    long gcAntes=gc();
                    long jit=compilacion();
                    long bytes=MEMORIA.getThreadAllocatedBytes(Thread.currentThread().threadId());
                    Muestra evento=new Muestra();
                    evento.metodo=METODOS[m]; evento.ronda=ronda; evento.cantidad=n;
                    evento.begin();
                    long inicio=System.nanoTime();
                    ejecutar(m,a,c);
                    long fin=System.nanoTime();
                    evento.end();
                    evento.ms=(fin-inicio)/1e6;
                    evento.commit();
                    long asignados=MEMORIA.getThreadAllocatedBytes(Thread.currentThread().threadId())-bytes;
                    long compilacion=compilacion()-jit;
                    long recolecciones=gc()-gcAntes;
                    verificar(a,c);
                    out.printf(Locale.US,"%s,%d,%d,%s,%.6f,%d,%d,%d,%d%n",
                            nombre,n,ronda,METODOS[m],evento.ms,recolecciones,compilacion,asignados,Arrays.hashCode(original));
                }
                out.flush();
            }
        } finally {
            if(recording!=null) {
                recording.stop();
                recording.dump(Path.of(salida+".jfr"));
                recording.close();
            }
        }
        System.out.println("OK "+salida+" vacios="+c.getVacios()+" numerica="+c.esNumerica());
    }

    static void conteos(DatosMusica datos, Path salida) throws Exception {
        try(PrintWriter out=new PrintWriter(Files.newBufferedWriter(salida))) {
            out.println("columna,n,metodo,comparaciones");
            for(String nombre : new String[]{"danceability","tempo","spotify_track_popularity","performer","song","spotify_genre","spotify_track_id","iguales","ascendente","descendente"}) {
                int n=nombre.equals("descendente") || nombre.equals("ascendente") ? 5000 : datos.getCantidad();
                String[] v=new String[n];
                int indice=datos.buscarColumna(nombre);
                for(int i=0; i<n; i++) {
                    v[i]=indice>=0 ? datos.getRegistros().get(i)[indice] :
                            nombre.equals("iguales") ? "" : ""+(nombre.equals("ascendente") ? i : n-i);
                }
                for(int m=0; m<6; m++) {
                    if(m==0 && n>5000 && !nombre.equals("danceability")) {
                        continue;
                    }
                    Contador c=new Contador(nombre,v);
                    Integer[] a=indices(n);
                    ejecutar(m,a,c);
                    long total=c.comparaciones.sum();
                    verificar(a,c);
                    out.printf("%s,%d,%s,%d%n",nombre,n,METODOS[m],total);
                }
            }
        }
        System.out.println("OK conteos");
    }

    static void integracion() throws Exception {
        DatosMusica datos=new DatosMusica(new String[]{"numero","texto"});
        for(String[] fila : new String[][]{{"3","z"},{"",""},{"-2","A"},{"3","a"}}) {
            datos.agregarRegistro(fila);
        }
        var resumen=new ComparadorOrdenamientos().comparar(datos,"Todas las columnas","Todos",4,s -> {},(a,b) -> {});
        if(resumen.pruebas().size()!=12) {
            throw new AssertionError("Faltan resultados");
        }
        for(var resultado : resumen.pruebas()) {
            double[] muestras=resultado.getMuestras();
            double[] copia=muestras.clone();
            Arrays.sort(copia);
            if(muestras.length!=9 || resultado.getTiempo()!=copia[4] || !resultado.isCorrecto() ||
                    resultado.getCalentamientos()<3 || resultado.getCalentamientos()>100 ||
                    resultado.getCalentamientos()<100 && resultado.getTiempoCalentamiento()<1000) {
                throw new AssertionError("Medicion o calentamiento");
            }
            muestras[0]=-999;
            if(resultado.getMuestras()[0]==-999) {
                throw new AssertionError("Muestras expuestas");
            }
        }
        DatosColumna c=datos.prepararColumna(0,4);
        var verificador=ComparadorOrdenamientos.class.getDeclaredMethod("verificar",Integer[].class,DatosColumna.class,int.class);
        verificador.setAccessible(true);
        for(Integer[] invalido : new Integer[][]{{2,0,1},{2,0,0,1},{2,null,0,1},{2,0,4,1},{0,2,3,1}}) {
            if((boolean)verificador.invoke(new ComparadorOrdenamientos(),invalido,c,4)) {
                throw new AssertionError("Verificacion insuficiente");
            }
        }
        DatosMusica defectuosos=new DatosMusica(new String[]{"invalida"}) {
            @Override public DatosColumna prepararColumna(int indice, int cantidad) {
                return new DatosColumna("invalida",new String[]{"1","2"}) {
                    @Override public int comparar(int a, int b) {
                        return 1;
                    }
                };
            }
        };
        defectuosos.agregarRegistro(new String[]{"1"});
        defectuosos.agregarRegistro(new String[]{"2"});
        try {
            new ComparadorOrdenamientos().comparar(defectuosos,"invalida","Arrays.sort",2,s -> {},(a,b) -> {});
            throw new AssertionError("Publico una prueba incorrecta");
        } catch(IllegalStateException esperado) {
            // Un comparador defectuoso no debe producir resultados publicados.
        }
        Thread.currentThread().interrupt();
        try {
            new ComparadorOrdenamientos().comparar(datos,"numero","Todos",4,s -> {},(a,b) -> {});
            throw new AssertionError("No cancelo");
        } catch(java.util.concurrent.CancellationException esperado) {
            // Se espera cancelar antes de comenzar.
        } finally {
            Thread.interrupted();
        }
        System.out.println("OK integracion: 12 resultados, muestras, medianas, limites, verificacion y cancelacion");
    }

    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.US);
        System.out.println("Java="+System.getProperty("java.runtime.version")+" CPU="+
                Runtime.getRuntime().availableProcessors()+" pool="+java.util.concurrent.ForkJoinPool.getCommonPoolParallelism());
        if(args[0].equals("funcional")) {
            funcional();
            return;
        }
        if(args[0].equals("integracion")) {
            integracion();
            return;
        }
        DatosMusica datos=LectorCSV.leer();
        System.out.println("CSV="+datos.getCantidad()+" x "+datos.getColumnas().length);
        if(args[0].equals("conteos")) {
            conteos(datos,Path.of(args[1]));
        } else if(args[0].equals("gui") || args[0].equals("gui3")) {
            try(PrintWriter out=new PrintWriter(Files.newBufferedWriter(Path.of(args[1])))) {
                out.println("ejecucion,metodo,ms,correcto,calentamientos,calentamiento_ms,muestras_ms");
                for(int i=0; i<5; i++) {
                    List<ResultadoPrueba> pruebas;
                    if(args[0].equals("gui3")) {
                        pruebas=new ComparadorTresCalentamientos().comparar(datos,"danceability","Todos",5000,s -> {},(a,b) -> {}).pruebas();
                    } else {
                        pruebas=new ComparadorOrdenamientos().comparar(datos,"danceability","Todos",5000,s -> {},(a,b) -> {}).pruebas();
                    }
                    for(var r : pruebas) {
                        String muestras=Arrays.toString(r.getMuestras()).replace(", ",";");
                        out.printf(Locale.US,"%d,%s,%.6f,%s,%d,%.6f,%s%n",i,r.getMetodo(),r.getTiempo(),r.isCorrecto(),
                                r.getCalentamientos(),r.getTiempoCalentamiento(),muestras);
                    }
                    out.flush();
                }
            }
        } else if(args[0].equals("matriz")) {
            for(String nombre : new String[]{"danceability","tempo","spotify_track_popularity","performer","song","spotify_genre","spotify_track_id"}) {
                for(int n : new int[]{100,500,1000,3000,5000,10000,32503}) {
                    repetir(datos,nombre,n,15,n<=1000 ? -1 : -2,
                            Path.of(args[1],nombre+"-"+n+".csv"),false);
                }
            }
        } else {
            repetir(datos,args[1],Integer.parseInt(args[2]),Integer.parseInt(args[3]),
                    Integer.parseInt(args[4]),Path.of(args[5]),args.length>6);
        }
    }
}
