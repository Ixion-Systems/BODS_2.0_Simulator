package com.bobds.simulador;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/* logica de simulacion robotica */
public class RobotService {

    /* gestores de estado concurrente */
    private final ConcurrentHashMap<String, ExecutorService> unidadExecutors = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, java.util.concurrent.atomic.AtomicInteger> unidadPendientes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Thread> executingThreads = new ConcurrentHashMap<>();
    private final java.util.Set<Integer> cancelledOrders = ConcurrentHashMap.newKeySet();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private static final String BACKEND_BASE_URL = System.getenv("BACKEND_URL") != null ? System.getenv("BACKEND_URL") : "http://localhost:8081";
    private static final String BACKEND_ORDERS_URL = BACKEND_BASE_URL + "/api/orders/status";
    private static final String BACKEND_UNITS_URL = BACKEND_BASE_URL + "/api/units/status";

    /* recepcion y encolamiento de ordenes */
    public void procesarOrden(String idUnidad, int idOrden, String orden) {
        ExecutorService executor = unidadExecutors.computeIfAbsent(idUnidad, 
            k -> Executors.newSingleThreadExecutor());

        java.util.concurrent.atomic.AtomicInteger count = unidadPendientes.computeIfAbsent(idUnidad, 
            k -> new java.util.concurrent.atomic.AtomicInteger(0));

        int actuales = count.incrementAndGet();
        if (actuales == 1) {
            enviarEstadoUnidadAlBackend(idUnidad, "Activo");
        }

        System.out.println("Encolando orden para la Unidad " + idUnidad + "...");

        executor.submit(() -> {
            executingThreads.put(idOrden, Thread.currentThread());
            try {
                if (cancelledOrders.contains(idOrden)) {
                    throw new InterruptedException("Cancelada en cola");
                }
                enviarEstadoAlBackend(idOrden, "En Curso");
                ejecutarOrden(idUnidad, idOrden, orden);
                if (!cancelledOrders.contains(idOrden)) {
                    enviarEstadoAlBackend(idOrden, "Finalizada");
                }
            } catch (InterruptedException e) {
                System.out.println("Orden " + idOrden + " interrumpida (cancelada).");
            } catch (Exception e) {
                System.err.println("Error procesando orden en simulador: " + e.getMessage());
            } finally {
                executingThreads.remove(idOrden);
                cancelledOrders.remove(idOrden);
                int restantes = count.decrementAndGet();
                if (restantes == 0) {
                    enviarEstadoUnidadAlBackend(idUnidad, "Inactivo");
                }
            }
        });
    }

    /* notificaciones al servidor base */
    private void enviarEstadoAlBackend(int idOrden, String status) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BACKEND_ORDERS_URL + "?idOrden=" + idOrden + "&status=" + java.net.URLEncoder.encode(status, "UTF-8")))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

            HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString());

            System.out.println("[!] Orden " + idOrden + " reportada como '" + status + "'. Backend respondió: " + response.statusCode());
        } catch (Exception e) {
            System.err.println("Error avisando al backend: " + e.getMessage());
        }
    }

    private void enviarEstadoUnidadAlBackend(String idUnidad, String status) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BACKEND_UNITS_URL + "?idUnidad=" + java.net.URLEncoder.encode(idUnidad, "UTF-8") + "&status=" + java.net.URLEncoder.encode(status, "UTF-8")))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

            HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString());

            System.out.println("[!] Unidad " + idUnidad + " reportada como '" + status + "'. Backend respondió: " + response.statusCode());
        } catch (Exception e) {
            System.err.println("Error avisando al backend sobre unidad: " + e.getMessage());
        }
    }

    /* ejecucion simulada de hardware */
    private void ejecutarOrden(String idUnidad, int idOrden, String orden) throws InterruptedException {
        Random rand = new Random();
        int duracion = 10 + rand.nextInt(11); 
        long inicio = System.currentTimeMillis();

        System.out.println(">>> INICIANDO: Unidad " + idUnidad + " Orden ID " + idOrden +
            " (" + orden + ") - Duración: " + duracion + "s");

        while ((System.currentTimeMillis() - inicio) < duracion * 1000) {
            Thread.sleep(100);
            if (cancelledOrders.contains(idOrden) || Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Orden cancelada");
            }
        }

        System.out.println(">>> FINALIZADO: Unidad " + idUnidad);
    }

    public void cancelOrder(int idOrden) {
        cancelledOrders.add(idOrden);
        Thread t = executingThreads.get(idOrden);
        if (t != null) {
            t.interrupt();
        }
    }
}