package com.fintech.backend.service.benchmark;

import com.fintech.backend.structures.hash.TablaHash;
import com.fintech.backend.structures.list.ListaEnlazada;
import com.fintech.backend.structures.queue.ColaPrioridad;
import com.fintech.backend.structures.tree.ArbolAVL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Benchmark de estructuras propias contra referencias de {@code java.util}.
 * El código de comparación está aislado aquí; no afecta las estructuras usadas
 * por el resto de la aplicación.
 */
@Service
public class BenchmarkService {

    private static final int[] TAMANOS = {1_000, 10_000, 100_000};
    private static final int CALENTAMIENTOS = 2;
    private static final int MUESTRAS = 3;
    private static final int CONSULTAS_ID = 10;
    private static volatile long sumidero;

    /** Ejecuta y devuelve los benchmarks completos para los tres tamaños. */
    public BenchmarkReport ejecutar() {
        List<BenchmarkMeasurement> mediciones = new ArrayList<>();
        for (int n : TAMANOS) {
            medirBusquedasId(n, mediciones);
            medirRangos(n, mediciones);
            medirTopK(n, mediciones);
            medirHistorial(n, mediciones);
        }
        return new BenchmarkReport(Instant.now().toString(), System.getProperty("java.version"),
                CALENTAMIENTOS, MUESTRAS, List.copyOf(mediciones));
    }

    private void medirBusquedasId(int n, List<BenchmarkMeasurement> salida) {
        TablaHash<Long, UsuarioBench> tabla = new TablaHash<>();
        ListaEnlazada<UsuarioBench> lista = new ListaEnlazada<>();
        for (int i = 0; i < n; i++) {
            UsuarioBench usuario = new UsuarioBench(i, (i * 37) % (n + 17));
            tabla.poner(usuario.id(), usuario);
            lista.agregar(usuario);
        }
        long idBuscado = n - 1L;
        Runnable hash = () -> {
            long suma = 0;
            for (int i = 0; i < CONSULTAS_ID; i++) suma += tabla.obtener(idBuscado).id();
            consumir(suma);
        };
        Runnable busquedaLista = () -> {
            long suma = 0;
            for (int consulta = 0; consulta < CONSULTAS_ID; consulta++) {
                for (UsuarioBench usuario : lista) {
                    if (usuario.id() == idBuscado) {
                        suma += usuario.id();
                        break;
                    }
                }
            }
            consumir(suma);
        };
        salida.add(medir(n, "Buscar usuario por id", "TablaHash", "ListaEnlazada",
                CONSULTAS_ID, hash, busquedaLista));
    }

    private void medirRangos(int n, List<BenchmarkMeasurement> salida) {
        ArbolAVL<PuntosId, UsuarioBench> arbol = new ArbolAVL<>();
        ListaEnlazada<UsuarioBench> lista = new ListaEnlazada<>();
        for (int i = 0; i < n; i++) {
            UsuarioBench usuario = new UsuarioBench(i, (i * 37) % (n + 17));
            lista.agregar(usuario);
            arbol.insertar(new PuntosId(usuario.puntos(), usuario.id()), usuario);
        }
        int minimo = n / 3;
        int maximo = Math.min(n + 16, minimo + Math.max(10, n / 100));
        PuntosId desde = new PuntosId(minimo, Long.MIN_VALUE);
        PuntosId hasta = new PuntosId(maximo, Long.MAX_VALUE);
        Runnable rangoArbol = () -> consumir(arbol.rango(desde, hasta).size());
        Runnable rangoLista = () -> {
            int coincidencias = 0;
            for (UsuarioBench usuario : lista) {
                if (usuario.puntos() >= minimo && usuario.puntos() <= maximo) coincidencias++;
            }
            consumir(coincidencias);
        };
        salida.add(medir(n, "Buscar usuarios por rango de puntos", "ArbolAVL", "RecorrerLista",
                1, rangoArbol, rangoLista));
    }

    private void medirTopK(int n, List<BenchmarkMeasurement> salida) {
        List<TransaccionBench> transacciones = new ArrayList<>(n);
        Comparator<TransaccionBench> mayorValorPrimero =
                Comparator.comparingLong(TransaccionBench::valor).reversed();
        for (int i = 0; i < n; i++) {
            transacciones.add(new TransaccionBench(i, (i * 48_271L + 12_345) % 1_000_003));
        }
        int k = Math.min(100, n);
        Runnable colaPrioridad = () -> {
            ColaPrioridad<TransaccionBench> monticulo = new ColaPrioridad<>(mayorValorPrimero);
            for (TransaccionBench transaccion : transacciones) monticulo.insertar(transaccion);
            long suma = 0;
            for (int i = 0; i < k; i++) suma += monticulo.extraer().valor();
            consumir(suma);
        };
        Runnable ordenarCompleto = () -> {
            List<TransaccionBench> ordenadas = new ArrayList<>(transacciones);
            ordenadas.sort(mayorValorPrimero);
            long suma = 0;
            for (int i = 0; i < k; i++) suma += ordenadas.get(i).valor();
            consumir(suma);
        };
        salida.add(medir(n, "Top-K transacciones por valor", "ColaPrioridad", "OrdenarListaCompleta",
                k, colaPrioridad, ordenarCompleto));
    }

    private void medirHistorial(int n, List<BenchmarkMeasurement> salida) {
        Object[] datos = new Object[n];
        for (int i = 0; i < n; i++) datos[i] = i;
        Runnable insertarLista = () -> {
            ListaEnlazada<Object> historial = new ListaEnlazada<>();
            for (Object dato : datos) historial.agregar(dato);
            consumir(historial.size());
        };
        Runnable insertarArreglo = () -> {
            Object[] historial = new Object[n];
            for (int i = 0; i < n; i++) historial[i] = datos[i];
            consumir(historial.length);
        };
        salida.add(medir(n, "Historial: inserción", "ListaEnlazada", "ArregloPreasignado",
                n, insertarLista, insertarArreglo));

        ListaEnlazada<Object> historialLista = new ListaEnlazada<>();
        for (Object dato : datos) historialLista.agregar(dato);
        Runnable recorrerLista = () -> {
            long suma = 0;
            for (Object dato : historialLista) suma += (Integer) dato;
            consumir(suma);
        };
        Runnable recorrerArreglo = () -> {
            long suma = 0;
            for (Object dato : datos) suma += (Integer) dato;
            consumir(suma);
        };
        salida.add(medir(n, "Historial: recorrido", "ListaEnlazada", "Arreglo",
                n, recorrerLista, recorrerArreglo));
    }

    private BenchmarkMeasurement medir(int n, String caso, String propia, String referencia,
                                       int operaciones, Runnable tareaPropia, Runnable tareaReferencia) {
        for (int i = 0; i < CALENTAMIENTOS; i++) {
            tareaPropia.run();
            tareaReferencia.run();
        }
        long[] propias = new long[MUESTRAS];
        long[] referencias = new long[MUESTRAS];
        for (int i = 0; i < MUESTRAS; i++) {
            if ((i & 1) == 0) {
                propias[i] = medirUna(tareaPropia);
                referencias[i] = medirUna(tareaReferencia);
            } else {
                referencias[i] = medirUna(tareaReferencia);
                propias[i] = medirUna(tareaPropia);
            }
        }
        return new BenchmarkMeasurement(n, caso, propia, referencia, operaciones,
                mediana(propias), mediana(referencias));
    }

    private static long medirUna(Runnable tarea) {
        long inicio = System.nanoTime();
        tarea.run();
        return System.nanoTime() - inicio;
    }

    private static long mediana(long[] muestras) {
        long[] copia = Arrays.copyOf(muestras, muestras.length);
        Arrays.sort(copia);
        return copia[copia.length / 2];
    }

    private static void consumir(long valor) {
        sumidero = sumidero ^ valor;
    }

    private record UsuarioBench(long id, int puntos) {
    }

    private record TransaccionBench(long id, long valor) {
    }

    private record PuntosId(int puntos, long id) implements Comparable<PuntosId> {
        @Override
        public int compareTo(PuntosId otra) {
            int porPuntos = Integer.compare(puntos, otra.puntos);
            return porPuntos == 0 ? Long.compare(id, otra.id) : porPuntos;
        }
    }

    public record BenchmarkMeasurement(int n, String caso, String estructuraPropia,
                                      String referencia, int operacionesMedidas,
                                      long nanosPropia, long nanosReferencia) {
    }

    public record BenchmarkReport(String fechaEjecucion, String versionJava,
                                  int calentamientos, int muestras,
                                  List<BenchmarkMeasurement> mediciones) {
    }
}
