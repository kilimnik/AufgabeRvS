package edu.udo.cs.rvs.request;

import edu.udo.cs.rvs.DateFormatter;
import edu.udo.cs.rvs.HTTPVersion;

import java.io.BufferedReader;

/**
 * Anfrage Klasse welche die Anfrage verarbeitet
 */
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
    private long isModifiedSinceDate = -1;

    /**
     * Initialisierung der Anfrage sowie verarbietung dieser
     *
     * @param reader Objekt um Anfrage auszulesen
     *
     * @throws Exception Wird geworfen falls es zu einem unerwarteten Fehler kommt
     */
    public Request(BufferedReader reader) throws Exception{
        initRequestString(reader);

        decodeRequest();
    }

    /**
     * Initialisierung bzw. Lesen der Anfrage
     *
     * @param reader Objekt um Anfrage auszulesen
     *
     * @throws Exception Wird bei unerwarteten Fehler beim lesen geworfen
     */
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

    /**
     * Decodiert die Anfrage in seine einzelnen Teile und verarbeitet diese
     */
    private void decodeRequest(){
        if (!request.equals("")) {
            String[] parts = request.split("\r\n");

            decodeRequestHead(parts[0]);

            //Conditional GET verarbeitung
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

    /**
     * Decodieren des Kopfes der Anfrage
     *
     * @param head Kopf der Anfrage
     */
    private void decodeRequestHead(String head){
        String[] parts = head.split(" ");

        decodeRequestMethod(parts[0]);
        requestedPath = parts[1];
        decodeHttpVersion(parts[2]);
    }

    /**
     * Decodieren des Anfrage Methode für eine bessere Struckturierbarkeit
     *
     * @param method Methoden String
     */
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

    /**
     * Decodieren der HTTP Versions Nummer für eine bessere Verarbeitbarkeit
     *
     * @param version HTTP Version als String
     */
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

    /**
     * Gibt Anfrage Methode zurück
     *
     * @return Anfrage Methode
     */
    public RequestMethod getRequestMethod() {
        return requestMethod;
    }

    /**
     * Gibt HTTP Version zurück
     *
     * @return HTTP Version
     */
    public HTTPVersion getHttpVersion() {
        return httpVersion;
    }

    /**
     * Gibt den Pfad der angeforderten Datei zurück
     *
     * @return Relativer Pfad der Datei
     */
    public String getRequestedPath() {
        return requestedPath;
    }

    /**
     * Gibt die Ganze Anfrage als String zurück
     *
     * @return Anfrage als String
     */
    public String getRequest() {
        return request;
    }

    /**
     * Gibt das Datum in Millisekunden einer Conditionel GET Anfrage mit IsModifiedSince zurück. Falls nicht exitsent dann -1
     *
     * @return Datum in Millisekunden
     */
    public long getIsModifiedSinceDate() {
        return isModifiedSinceDate;
    }
}
