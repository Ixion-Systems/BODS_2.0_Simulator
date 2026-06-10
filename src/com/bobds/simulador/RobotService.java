package com.bobds.simulador;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RobotService {

    private final ConcurrentHashMap<String, ExecutorService> unidadExecutors = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, java.util.concurrent.atomic.AtomicInteger> unidadPendientes = new ConcurrentHashMap<>();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private static final String BACKEND_ORDERS_URL = "http://localhost:8081/api/orders/status";
    private static final String BACKEND_UNITS_URL = "http://localhost:8081/api/units/status";

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
            try {
                enviarEstadoAlBackend(idOrden, "En Curso");
                ejecutarOrden(idUnidad, idOrden, orden);
                enviarEstadoAlBackend(idOrden, "Finalizada");
            } catch (Exception e) {
                System.err.println("Error procesando orden en simulador: " + e.getMessage());
            } finally {
                int restantes = count.decrementAndGet();
                if (restantes == 0) {
                    enviarEstadoUnidadAlBackend(idUnidad, "Inactivo");
                }
            }
        });
    }

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

    private void ejecutarOrden(String idUnidad, int idOrden, String orden) {
        Random rand = new Random();
        int duracion = 10 + rand.nextInt(11); // Entre 10 y 20 segundos
        long inicio = System.currentTimeMillis();

        System.out.println(">>> INICIANDO: Unidad " + idUnidad + " Orden ID " + idOrden +
            " (" + orden + ") - Duración: " + duracion + "s");

        while ((System.currentTimeMillis() - inicio) < duracion * 1000) {
            // simula trabajo
        }

        System.out.println(">>> FINALIZADO: Unidad " + idUnidad);
    }
}