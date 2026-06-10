package com.bobds.simulador;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;

public class Main {
    public static void main(String[] args) throws Exception {
        RobotService robotService = new RobotService();

        HttpServer server = HttpServer.create(new InetSocketAddress(7777), 0);

        server.createContext("/robot/ejecutar", exchange -> {
            if ("POST".equals(exchange.getRequestMethod())) {
                // Leer el body del request
                String body = new String(exchange.getRequestBody().readAllBytes());
                String idUnidad = extraerParam(body, "idUnidad");
                String idOrdenStr = extraerParam(body, "idOrden");
                String orden = extraerParam(body, "orden");

                int idOrden = -1;
                try {
                    idOrden = Integer.parseInt(idOrdenStr);
                } catch (NumberFormatException e) {
                    System.err.println("ID de orden inválido: " + idOrdenStr);
                }

                System.out.println("Orden recibida → Unidad: " + idUnidad + " | Orden ID: " + idOrden + " | Cmd: " + orden);

                // Procesar en hilo separado para no bloquear el servidor
                final int finalIdOrden = idOrden;
                new Thread(() -> robotService.procesarOrden(idUnidad, finalIdOrden, orden)).start();

                // Responder inmediatamente al backend
                String respuesta = "Orden recibida";
                exchange.sendResponseHeaders(200, respuesta.length());
                exchange.getResponseBody().write(respuesta.getBytes());
                exchange.getResponseBody().close();
            } else {
                exchange.sendResponseHeaders(405, -1); // Method Not Allowed
                exchange.close();
            }
        });

        server.start();
        System.out.println("Simulador corriendo en puerto 7777...");
    }

    private static String extraerParam(String body, String param) {
        for (String par : body.split("&")) {
            String[] kv = par.split("=");
            if (kv.length == 2 && kv[0].equals(param)) return kv[1];
        }
        return "";
    }
}