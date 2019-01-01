package edu.udo.cs.rvs.request;

import edu.udo.cs.rvs.DateFormatter;
import edu.udo.cs.rvs.HTTPVersion;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Date;

public class Request {

    /**
     * Ganzer String der Request
     */
    private String request;

    /**
     * Anfragemethode
     */
    private RequestMethod requestMethod;

    /**
     * HTTP Version der Anfrage
     */
    private HTTPVersion httpVersion;

    /**
     * Datei oder Pfad welcher angefragt wird
     */
    private String requestedPath;

    /**
     * Zeit für das Conditional GET
     */
    private long isModifiedSinceDate;

    public Request(BufferedReader reader) throws Exception{
        initRequestString(reader);

        decodeRequest();
    }

    private void initRequestString(BufferedReader reader) throws Exception{
        StringBuilder builder = new StringBuilder();

        String line = "";

        while ((line = reader.readLine()) != null) {
            builder.append(line).append("\r\n");
            if (line.isEmpty()) {
                break;
            }
        }

        request = builder.toString();
    }

    private void decodeRequest(){
        if (!request.equals("")) {
            String[] parts = request.split("\r\n");

            decodeRequestHead(parts[0]);

            if (requestMethod == RequestMethod.GET){
                for (String p:parts){
                    if (p.startsWith("If-Modified-Since")){
                        p = p.replaceFirst("If-Modified-Since: ", "");

                        isModifiedSinceDate = DateFormatter.parseDate(p).getTime();
                    }
                }
            }
        }
    }

    private void decodeRequestHead(String head){
        String[] parts = head.split(" ");

        decodeRequestMethod(parts[0]);
        requestedPath = parts[1];
        decodeHttpVersion(parts[2]);
    }

    private void decodeRequestMethod(String method){
        switch (method){
            case "GET":
                requestMethod = RequestMethod.GET;
                break;
            case "HEAD":
                requestMethod = RequestMethod.HEAD;
                break;
            case "POST":
                requestMethod = RequestMethod.POST;
                break;
            case "PUT":
                requestMethod = RequestMethod.PUT;
                break;
            case "DELETE":
                requestMethod = RequestMethod.DELETE;
                break;
            case "LINK":
                requestMethod = RequestMethod.LINK;
                break;
            case "UPLINK":
                requestMethod = RequestMethod.UPLINK;
                break;

        }
    }

    private void decodeHttpVersion(String version){
        switch (version){
            case "HTTP/1.0":
                httpVersion = HTTPVersion.HTTP_1_0;
                break;
            case "HTTP/1.1":
                httpVersion = HTTPVersion.HTTP_1_1;
                break;
            case "HTTP/2.0":
                httpVersion = HTTPVersion.HTTP_2_0;
                break;
        }
    }

    public RequestMethod getRequestMethod() {
        return requestMethod;
    }

    public HTTPVersion getHttpVersion() {
        return httpVersion;
    }

    public String getRequestedPath() {
        return requestedPath;
    }

    public String getRequest() {
        return request;
    }

    public long getIsModifiedSinceDate() {
        return isModifiedSinceDate;
    }
}
