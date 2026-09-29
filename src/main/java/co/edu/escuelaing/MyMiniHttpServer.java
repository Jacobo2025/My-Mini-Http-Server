package co.edu.escuelaing;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class MyMiniHttpServer {

    private static final String WEBROOT = "webroot";
    private static final Map<String, String> CONTENT_TYPE = Map.of(
            "html", "text/html",
            "css", "text/css",
            "js", "application/javascript",
            "png", "image/png",
            "jpg", "image/jpg",
            "jpeg", "image/jpeg"
    );

    public static void main(String[] args) throws IOException {
        int port = 8080;
        try (ServerSocket serverSocket = new ServerSocket(port)){

            while(true){
                try (Socket client = serverSocket.accept()){

                    handleRequest(client);

                } catch (IOException e){
                    System.out.println("Error: " + e.getMessage());
                }
            }
        }
    }

    private static void handleRequest(Socket client) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
        OutputStream out = client.getOutputStream();
        String readLine = in.readLine();

        if(readLine == null || readLine.isBlank()){
            String statusText = "Bad Request";
            sendResponse(out, 400, statusText, "text/plain, UTF-8", statusText.getBytes());
            return;
        }

        String[] parts = readLine.split(" ");
        if (parts.length < 2){
            String statusText = "Bad Request";
            sendResponse(out, 400, statusText, "text/plain, UTF-8", statusText.getBytes());
            return;
        }

        String method = parts[0];
        String rawPath = parts[1];

        if(!method.equals("GET")){
            String statusText = "Method Not Allowed";
            sendResponse(out, 405, statusText, "text/plain, UTF-8", statusText.getBytes());
            return;
        }

        String path = rawPath;
        String query = "";
        int qIndex = rawPath.indexOf('?');
        if (qIndex !=-1 ){
            path = rawPath.substring(0, qIndex);
            query = rawPath.substring(qIndex + 1);
        }

        if (path.equals("/greeting")){
            handleGreeting(quary);
        }

        serveStaticFile(out, path);
    }

    private static void serveStaticFile(OutputStream out, String path) throws IOException {
        Path base = Paths.get(WEBROOT).toAbsolutePath().normalize();
        Path resolved = base.resolve("." + path).normalize();
        if(!Files.exists(resolved) || !resolved.startsWith(base) || Files.isDirectory(resolved)){
            String statusText = "Bad Request";
            sendResponse(out, 400, statusText, "text/plain, UTF-8", statusText.getBytes());
            return;
        }
        byte[] filesByte = Files.readAllBytes(resolved);
        String extension = getExtension(resolved.toString());
        String contentType = CONTENT_TYPE.getOrDefault(extension, "application/octect-stream");
        sendResponse(out, 200, "OK", contentType, filesByte);

    }
    private static void sendResponse(OutputStream out, int code, String statusText, String typeContent, byte[] body) throws IOException {
        String header = "HTTP/1.1 " + code + " " + statusText + "\r\n" +
                "content-type: " + typeContent + "\r\n" +
                "content-length" + body.length + "\r\n" +
                "\r\n";
        out.write(header.getBytes("UTF-8"));
        out.write(body);
        out.flush();
    }

    private static String getExtension(String fileName){
        int dot = fileName.indexOf('.');
        return dot == -1 ?  "" : fileName.substring(dot + 1);
    }

    private static Map<String, String> parseQuery(String query){
        Map<String,String> params = new HashMap<>();
        if(query == null || query.isBlank()){
            return params;
        }
        for ()
    }
}