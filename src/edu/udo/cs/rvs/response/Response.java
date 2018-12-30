package edu.udo.cs.rvs.response;

import edu.udo.cs.rvs.HTTPVersion;
import edu.udo.cs.rvs.request.Request;
import edu.udo.cs.rvs.request.RequestMethod;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Stack;

public class Response {

    /**
     * Anfrage an den Server
     */
    private Request request;

    /**
     * Antwort schreiber an den Nutzer
     */
    private PrintWriter outputWriter;

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
     * Körper der Antwort
     */
    private String responseBody = "";

    public Response(Request request, PrintWriter printWriter) {
        this.request = request;
        this.outputWriter = printWriter;

        filePath = new File("").getAbsolutePath();
        filePath = filePath.substring(0, filePath.length() - 4);
        filePath = filePath + "/wwwroot/";

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

        responseBuilder.append(decodeHttpVersion()).append(" ");
        responseBuilder.append(decodeResponseStatus()).append("\r\n");

        if (error){
            responseBody = buildErrorResponseBody();
        }

        responseBuilder.append("Content-Length: ").append(responseBody.getBytes().length).append("\r\n\r\n");

        responseBuilder.append(responseBody).append("\r\n");

        outputWriter.println(responseBuilder.toString());
    }

    private String decodeHttpVersion() {
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

    private String decodeResponseStatus() {
        switch (responseCode){
            case OK_200:
                return "200 OK";
            case NO_CONNTENT_204:
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

    private String buildErrorResponseBody(){
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("<!DOCTYPE html>\n" +
                            "<html>\n" +
                            "<head></head>\n" +
                            "<body>\n");
        stringBuilder.append("Error Status Code: ");
        stringBuilder.append(decodeResponseStatus());
        stringBuilder.append("\n</body>" +
                            "\n</html>");

        return stringBuilder.toString();
    }

    private boolean checkAndBuildNormalResponseBody(){
        String response = "";

        if (!checkPathForSecurity()){
            responseCode = ResponseCode.FORBIDDEN_403;

            return false;
        }

        try {
            Path path = Paths.get(filePath,request.getRequestedPath());

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
                    responseCode = ResponseCode.NO_CONNTENT_204;

                    return false;
                }
            }

            byte[] encoded = Files.readAllBytes(path);

            response = new String(encoded, StandardCharsets.UTF_8);
        } catch (NoSuchFileException e){
            responseCode = ResponseCode.NOT_FOUND_404;

            return false;
        } catch (IOException e) {
            e.printStackTrace();
        }

        responseBody = response;

        return true;
    }

    private boolean checkPathForSecurity(){
        Path path = Paths.get(filePath,request.getRequestedPath());
        String absolutPath = path.toAbsolutePath().toString();
        absolutPath = decodeAbsolutPath(absolutPath);

        if (absolutPath.startsWith(filePath)){
            return true;
        }

        return false;
    }

    private String decodeAbsolutPath(String path){
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
