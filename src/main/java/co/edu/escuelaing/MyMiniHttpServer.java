package co.edu.escuelaing;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyMiniHttpServer {
    // dos atributos
    private final static String WEBROOT = "webroot";
    private final static Map<String, String> CONTENT_TYPE = Map.of(
            "html", "text/html",
            "js", "application/javascript",
            "css", "text/css",
            "png", "image/png",
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg"
    );
    private static volatile boolean running = true;
    private static ServerSocket serverSocket;




    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

        // Pool de hilos: ahora varias peticiones se atienden EN PARALELO,
        // a diferencia del servidor secuencial del Día 1.
        ExecutorService pool = Executors.newFixedThreadPool(10);

        serverSocket = new ServerSocket(port);
        System.out.println("Servidor escuchando en puerto " + port);

        while (running) {
            try {
                Socket client = serverSocket.accept();
                pool.submit(() -> {
                    try {
                        handleRequest(client);
                    } catch (IOException e) {
                        System.out.println("Error manejando request: " + e.getMessage());
                    } finally {
                        try {
                            client.close();
                        } catch (IOException ignored) {
                        }
                    }
                });
            } catch (IOException e) {
                // Si running ya es false, esta excepción la causó a propósito
                // el cierre del serverSocket dentro de handleShutdown().
                if (running) {
                    System.out.println("Error aceptando conexión: " + e.getMessage());
                }
            }
        }

        pool.shutdown();
        System.out.println("Servidor detenido de forma ordenada.");
    }


    private static void handleRequest(Socket client) throws IOException{
        BufferedReader in = new BufferedReader( new InputStreamReader(client.getInputStream()));
        OutputStream out = client.getOutputStream();
        String readLine = in.readLine();

        if(readLine == null || readLine.isBlank()){
            String statusText = "Bad Request";
            sendResponse(out, 400, statusText, "text/plain; charset=utf-8", statusText.getBytes());
            return;
        }

        String[] params = readLine.split(" ");
        if(params.length < 2){
            String statusText = "Bad Request";
            sendResponse(out, 400, statusText, "text/plain; charset=utf-8", statusText.getBytes());
            return;
        }

        String method = params[0];
        String rawPath = params[1];
        if(!method.equals("GET")){
            String statusText = "Method Not Allowed";
            sendResponse(out, 405, statusText, "text/plain; charset=utf-8", statusText.getBytes());
            return;
        }

        String path = rawPath;
        String query = "";
        int qIndex = rawPath.indexOf('?');
        if(qIndex != -1){
            path = rawPath.substring(0, qIndex);
            query = rawPath.substring(qIndex + 1);
        }

        if (path.equals("/")){
            path = "/index.html";
        }

        if (path.equals("/greeting")){
            handleGreeting(out, query);
            return;
        }

        if (path.equals("/square")){
            handleSquare(out, query);
            return;
        }

        if(path.equals("/servertime")){
            handleServerTime(out);
            return;
        }

        serveStaticFile(out, path);
        if(path.equals("/shutdown")){
            String appEnv = System.getenv().getOrDefault("APP_ENV", "development");

            if(appEnv.equals("production")){
                String statusText = "Not Found";
                sendResponse(out, 404, statusText, "text/plain; charset=utf-8", statusText.getBytes());
                return;
            }

            handleShutdown(out);
            return;
        }


    }

    private static void serveStaticFile(OutputStream out, String path) throws IOException{
        Path base = Paths.get(WEBROOT).toAbsolutePath().normalize();
        Path resolved = base.resolve("." + path).normalize();
        if(!resolved.startsWith(base) || Files.isDirectory(resolved) || !Files.exists(resolved)){
            String statusText = "Not Found";
            sendResponse(out, 404, statusText, "text/plain; charset=utf-8", statusText.getBytes());
            return;
        }

        byte[] filesByte = Files.readAllBytes(resolved);
        String extension = getExtension(resolved.toString());
        String contentType = CONTENT_TYPE.getOrDefault(extension, "application/octet-stream");
        sendResponse(out, 200, "OK", contentType, filesByte);
    }

    private static String getExtension(String filename){
        int dot = filename.lastIndexOf('.');
        return dot == -1 ? "" : filename.substring(dot + 1).toLowerCase();
    }

    private static void sendResponse(OutputStream out, int code, String statusText, String contentType, byte[] body) throws IOException{
        String headers = "HTTP/1.1 " + code + " " + statusText + "\r\n" +
                "Content-Type: " + contentType + "\r\n" +
                "Content-Length: " + body.length + "\r\n" +
                "Connection: close" + "\r\n" +
                "\r\n";
        out.write(headers.getBytes("UTF-8"));
        out.write(body);
        out.flush();
    }

    private static String decode(String s){
        try {
            return URLDecoder.decode(s, "UTF-8");
        } catch (UnsupportedEncodingException e){
            return s;
        }
    }

    private static Map<String, String> parseQuery(String query){
        Map<String, String> params = new HashMap<>();
        if(query == null || query.isBlank()){
            return params;
        }

        for(String parts: query.split("&")){
            String[] kv = parts.split("=", 2);
            String key = decode(kv[0]);
            String value = kv.length > 1 ? decode(kv[1]) : "";
            params.put(key, value);
        }
        return params;
    }

    private static void handleGreeting(OutputStream out, String query) throws IOException {
        Map<String, String> params = parseQuery(query);
        String name = params.get("name");
        if(name == null || name.isBlank()){
            String statusText = "Bad Request";
            sendResponse(out, 400, statusText, "text/plain; charset=utf-8", statusText.getBytes());
            return;
        }
        String json = "{ \"message\" : \"Hola, " + escapeJson(name) + "!\"}";
        sendResponse(out, 200, "OK", "application/json; charset=utf-8", json.getBytes());
    }

    private static String escapeJson(String s){
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }


    private static void handleSquare(OutputStream out, String query) throws IOException{
        Map<String, String> params = parseQuery(query);
        String numberString = params.get("number");
        try {
            int number = Integer.parseInt(numberString);
            int numberSquare = number * number;
            String json = "{ \"input\" : " + number + "," +
                    "\"square\" : " + numberSquare + "}";


            sendResponse(out, 200, "OK", "application/json; charset=UTF-8", json.getBytes());
        } catch (NumberFormatException e){
            String statusText = "Bad Request";
            sendResponse(out, 400, statusText, "text/plain; charset=UTF-8", statusText.getBytes());
        }

    }

    private static void handleServerTime(OutputStream out) throws IOException{
        LocalDateTime now = LocalDateTime.now();
        String json = "{\"serverTime\" : \"" + now + "\"}";
        sendResponse(out, 200, "OK", "application/json; charset=UTF-8", json.getBytes());
    }

    private static void handleShutdown(OutputStream out) throws IOException{
        String statusText = "Server shutting down";
        sendResponse(out, 200, "OK", "text/plain; charset=utf-8", statusText.getBytes());

        running = false;
        serverSocket.close();
    }
}
