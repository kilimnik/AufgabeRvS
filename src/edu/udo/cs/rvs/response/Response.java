package edu.udo.cs.rvs.response;

import edu.udo.cs.rvs.HTTPVersion;
import edu.udo.cs.rvs.request.Request;
import edu.udo.cs.rvs.request.RequestMethod;

import java.io.PrintWriter;

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

    public Response(Request request, PrintWriter printWriter) {
        this.request = request;
        this.outputWriter = printWriter;

        if (!request.getRequest().equals("")) {
            initResponse();
        }
    }

    private void initResponse(){
        httpVersion = request.getHttpVersion();

        if (checkHttpVersion() || checkImplementedRequestMethod()){
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

        StringBuilder builder = new StringBuilder();

        builder.append(encodeHttpVersion()).append(" ");
        builder.append(encodeResponseStatus()).append("\r\n");

        builder.append("Content-Length: 0\r\n");

        outputWriter.println(builder.toString());


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
}
