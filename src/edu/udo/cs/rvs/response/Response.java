package edu.udo.cs.rvs.response;

import edu.udo.cs.rvs.HTTPVersion;
import edu.udo.cs.rvs.request.Request;
import edu.udo.cs.rvs.request.RequestMethod;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Stack;

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

    public Response(Request request, OutputStream printWriter) {
        this.request = request;
        this.outputWriter = printWriter;

        filePath = new File("wwwroot").getAbsolutePath();

        if (!request.getRequest().equals("")) {
            initResponse();
        }
    }

    private void initResponse(){
        httpVersion = request.getHttpVersion();

        if (checkHttpVersion() || checkImplementedRequestMethod() || !checkAndBuildNormalResponseBody()){
            error = true;
        }


    }

    private boolean checkHttpVersion()
    {
        if (httpVersion == HTTPVersion.HTTP_2_0){
            responseCode = ResponseCode.BAD_REQUEST_400;

            return true;
        }

        return false;
    }

    private boolean checkImplementedRequestMethod()
    {
        if (request.getRequestMethod() != RequestMethod.GET && request.getRequestMethod() != RequestMethod.HEAD && request.getRequestMethod() != RequestMethod.POST){
            responseCode = ResponseCode.NOT_IMPLEMENTED_501;

            return true;
        }

        return false;
    }


    public void sendResponse(){
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
        responseBuilder.append("Content-Type: ").append(contentType).append("\r\n\r\n");

        try {
            outputWriter.write(responseBuilder.toString().getBytes());

            if (request.getRequestMethod() != RequestMethod.HEAD) {
                outputWriter.write(responseBodyBytes);
            }


            outputWriter.write("\r\n".getBytes());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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

    private byte[] buildErrorResponseBodyBytes(){
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("<!DOCTYPE html>\n" +
                            "<html>\n" +
                            "<head></head>\n" +
                            "<body>\n");
        stringBuilder.append("Error Status Code: ");
        stringBuilder.append(encodeResponseStatus());
        stringBuilder.append("\n</body>" +
                            "\n</html>");

        return stringBuilder.toString().getBytes();
    }

    private boolean checkAndBuildNormalResponseBody(){
        byte[] response = new byte[0];

        if (!checkPathForSecurity()){
            responseCode = ResponseCode.FORBIDDEN_403;

            return false;
        }

        try {
            String safeRequestPath = request.getRequestedPath().replace("/", File.separator);

            Path path = Paths.get(filePath, safeRequestPath);

            File file = path.toFile();
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
                    responseCode = ResponseCode.NOT_MODIFIED_304;

                    return false;
                }


                if (!foundIndex){
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
            responseCode = ResponseCode.NOT_FOUND_404;

            return false;
        } catch (IOException e) {
            e.printStackTrace();
        }

        responseBodyBytes = response;

        return true;
    }

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
}
