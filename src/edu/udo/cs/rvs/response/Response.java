package edu.udo.cs.rvs.response;

import edu.udo.cs.rvs.HTTPVersion;
import edu.udo.cs.rvs.HttpServer;
import edu.udo.cs.rvs.request.Request;
import edu.udo.cs.rvs.request.RequestMethod;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Stack;

/**
 * Antwort erstellen und versenden
 */
public class Response {

    /**
     * Anfrage an den Server
     */
    private Request request;

    /**
     * Antwort schreiber an den Nutzer
     */
    private OutputStream outputWriter;

    /**
     * Wenn wahr, dann gibt die Antwort einen Fehler aus
     */
    private boolean error;

    /**
     * Status Code
     */
    private ResponseCode responseCode = ResponseCode.OK_200;

    /**
     * HTTP Version der Antwort
     */
    private HTTPVersion httpVersion;

    /**
     * Pfad zu den Webserver Dateien
     */
    private String filePath;

    /**
     * Körper der Antwort in Byte Array
     */
    private byte[] responseBodyBytes;

    /**
     * MIME-Typ der Antwort
     */
    private String contentType;

    /**
     * Antwort erstellen und Initialisieren
     *
     * @param request Anfrage
     * @param printWriter Objekt um Antwort zu verschicken
     *
     * @throws Exception Wird geworfen falls es zu einem unerwartetem Fehler kommt
     */
    public Response(Request request, OutputStream printWriter) throws Exception{
        this.request = request;
        this.outputWriter = printWriter;

        filePath = HttpServer.wwwroot.getAbsolutePath();

        if (!request.getRequest().equals("")) {
            initResponse();
        }
    }

    /**
     * Initialisierung der Antwort
     *
     * @throws Exception Wird geworfen falls es zu einem unerwartetem Fehler kommt
     */
    private void initResponse() throws Exception{
        httpVersion = request.getHttpVersion();

        if (checkHttpVersion() || checkImplementedRequestMethod() || !checkAndBuildNormalResponseBody()){
            error = true;
        }
    }

    /**
     * Prüft ob die HTTP Version verarbeitet werden kann und setzt den Status Code auf 400 falls die Version nicht verarbeitet werden kann
     *
     * @return Wahr wenn Version 2.0 ist
     */
    private boolean checkHttpVersion()
    {
        if (httpVersion == HTTPVersion.HTTP_2_0){
            responseCode = ResponseCode.BAD_REQUEST_400;

            return true;
        }

        return false;
    }

    /**
     * Prüft ob die Anfrage Methode verarbeitet werden kann und setzt den Status Code auf 501 falls die Methode nicht verarbeitet werden können
     *
     * @return Wahr wenn Methode nicht GET, HEAD oder Post ist.
     */
    private boolean checkImplementedRequestMethod()
    {
        if (request.getRequestMethod() != RequestMethod.GET && request.getRequestMethod() != RequestMethod.HEAD && request.getRequestMethod() != RequestMethod.POST){
            responseCode = ResponseCode.NOT_IMPLEMENTED_501;

            return true;
        }

        return false;
    }

    /**
     * Versendet und erstellt die Antwort
     *
     * @throws Exception Wird geworfen, falls es beim senden zu einem unerwarteten Fehler kam
     */
    public void sendResponse() throws Exception{
        if (request.getRequest().equals("")){
            return;
        }

        StringBuilder responseBuilder = new StringBuilder();

        responseBuilder.append(encodeHttpVersion()).append(" ");
        responseBuilder.append(encodeResponseStatus()).append("\r\n");

        if (error){
            responseBodyBytes = buildErrorResponseBodyBytes();
        }

        responseBuilder.append("Content-Length: ").append(responseBodyBytes.length).append("\r\n");

        if (error){
            responseBuilder.append("Content-Type: text/plain; charset=utf-8\r\n\r\n");
        }else {
            responseBuilder.append("Content-Type: ").append(contentType).append("\r\n\r\n");
        }

        outputWriter.write(responseBuilder.toString().getBytes());

        if (request.getRequestMethod() != RequestMethod.HEAD) {
            outputWriter.write(responseBodyBytes);
        }


        outputWriter.write("\r\n".getBytes());
    }

    /**
     * Encodieren der Http Version zum String
     *
     * @return String der Http Version
     */
    private String encodeHttpVersion() {
        switch (httpVersion){
            case HTTP_1_0:
                return "HTTP/1.0";
            case HTTP_1_1:
                return "HTTP/1.1";
            case HTTP_2_0:
                return "HTTP/2.0";
        }

        return "";
    }

    /**
     * Encodieren der Status Codes
     *
     * @return Status Code als String
     */
    private String encodeResponseStatus() {
        switch (responseCode){
            case OK_200:
                return "200 OK";
            case NO_CONTENT_204:
                return "204 No Content";
            case NOT_MODIFIED_304:
                return "304 Not Modified";
            case BAD_REQUEST_400:
                return "400 Bad Request";
            case FORBIDDEN_403:
                return "403 Forbidden";
            case NOT_FOUND_404:
                return "404 Not Found";
            case INTERNAL_SERVER_ERROR_500:
                return "500 Internal Server Error";
            case NOT_IMPLEMENTED_501:
                return "501 Not Implemented";
        }

        return "";
    }

    /**
     * Erstellt eine Fehlermeldungs Body für die Antwort
     *
     * @return byte Array des Bodys
     */
    private byte[] buildErrorResponseBodyBytes(){
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("Error Status Code: ");
        stringBuilder.append(encodeResponseStatus());

        return stringBuilder.toString().getBytes();
    }

    /**
     * Prüft ob ein normaler Antwort body erstellt werden kann ohne einen Fehler.
     *
     * @return Gibt zurück ob es zu einem Fehler gekommen ist
     *
     * @throws Exception Wird geworfen falls es zu einem unverarbeitbaren Fehler gekommen ist
     */
    private boolean checkAndBuildNormalResponseBody() throws Exception{
        byte[] response = new byte[0];

        //Prüfen ob Pfad außerhalb von wwwroot
        if (!checkPathForSecurity()){
            responseCode = ResponseCode.FORBIDDEN_403;

            return false;
        }

        try {
            String safeRequestPath = request.getRequestedPath().replace("/", File.separator);

            Path path = Paths.get(filePath, safeRequestPath);

            File file = path.toFile();

            //Prüfen ob Datei gelsen werden draf
            if (!file.canRead()){
                responseCode = ResponseCode.FORBIDDEN_403;

                return false;
            }

            if (file.isDirectory()){
                File[] files = file.listFiles();

                boolean foundIndex = false;

                for (File f : files){
                    if (f.getName().startsWith("index.") && f.isFile()){
                        file = f;
                        path = f.toPath();

                        foundIndex = true;
                        break;
                    }
                }

                if (request.getIsModifiedSinceDate() > file.lastModified()){
                    //Datei ist abgelaufen

                    responseCode = ResponseCode.NOT_MODIFIED_304;

                    return false;
                }


                if (!foundIndex){
                    //Pfad führt zu einem Ordner und nicht zu einer Datei

                    responseCode = ResponseCode.NO_CONTENT_204;

                    return false;
                }
            }

            decodeContentType(file);

            response = Files.readAllBytes(path);

            if (contentType.startsWith("text")){
                response = new String(response, StandardCharsets.UTF_8).getBytes();
            }


        } catch (NoSuchFileException e){
            //Keine Datei unter diesem Pfad gefunden. Datei Existiert nicht

            responseCode = ResponseCode.NOT_FOUND_404;

            return false;
        }

        responseBodyBytes = response;

        return true;
    }

    /**
     * Decodieren des Dateityps
     *
     * @param file Datei Mime Typ als String
     */
    private void decodeContentType(File file) {
        if (file.getName().endsWith(".txt")){
            contentType = "text/plain; charset=utf-8";
        }else if (file.getName().endsWith(".html") || file.getName().endsWith(".htm")){
            contentType = "text/html; charset=utf-8";
        }else if (file.getName().endsWith("css")){
            contentType = "text/css; charset=utf-8";
        }else if (file.getName().endsWith(".ico")){
            contentType = " image/x-icon";
        }else if (file.getName().endsWith(".pdf")){
            contentType = "application/pdf";
        }else {
            contentType = "application/octet-stream";
        }
    }

    /**
     * Prüft ob auf Datei zugergriffen werden darf.
     *
     * @return Gibt zurück ob Pfad außerhalb des erlaubten Bereichs
     */
    private boolean checkPathForSecurity(){
        String safeFilePath = filePath.replace("\\", "/");

        Path path = Paths.get(safeFilePath, request.getRequestedPath());
        String absolutePath = path.toAbsolutePath().toString();
        absolutePath = decodeAbsolutePath(absolutePath);

        if (absolutePath.startsWith(filePath)){
            return true;
        }

        return false;
    }

    /**
     * Decodiert den Absoluten Pfad zu einem Pfad ohne Backtracking
     *
     * @param path Ganzer Pfad mit Backtracking
     *
     * @return Neuer Pfad ohne Backtracking
     */
    private String decodeAbsolutePath(String path){
        Stack<String> stringStack = new Stack<>();

        String[] pathParts = path.split("/");
        for (int i=0; i<pathParts.length; i++){
            if (pathParts[i].equals("..")){
                stringStack.pop();
            }else {
                stringStack.add(pathParts[i]);
            }
        }

        StringBuilder builder = new StringBuilder();
        stringStack.forEach(s -> {
            builder.append(s).append("/");});

        return builder.toString();
    }

    /**
     * Sendet Fehler Antwort falls es zu einem unerwarteten und unverarbeitbaren Fehler gekommen ist
     *
     * @param out Objekt um Antwort zu versenden
     * @param e Fehler welcher geworfen wurde
     */
    public static void sendErrorResponse(PrintWriter out, Exception e){
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        e.printStackTrace(printWriter);
        String stackTrace = stringWriter.toString();

        StringBuilder responseBuilder = new StringBuilder();

        responseBuilder.append("HTTP/1.0 ");
        responseBuilder.append("501 Not Implemented\r\n");

        responseBuilder.append("Content-Length: ").append(stackTrace.length()).append("\r\n");
        responseBuilder.append("Content-Type: ").append("text/plain; charset=utf-8").append("\r\n\r\n");

        responseBuilder.append(stackTrace);

        out.println(responseBuilder.toString());

    }
}
