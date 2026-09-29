package org.example;

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



    public static void main(String[] args) throws IOException {
        int port = 8080;

        try (ServerSocket serverSocket = new ServerSocket(port)){

            while(true){
                try(Socket client = serverSocket.accept()){

                    handleRequest(client);
                } catch (IOException e){

                    System.out.println("Error: " + e.getMessage());

                }
            }
        }
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
}
