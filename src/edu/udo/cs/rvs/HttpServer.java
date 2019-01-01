package edu.udo.cs.rvs;

import edu.udo.cs.rvs.request.Request;
import edu.udo.cs.rvs.response.Response;

import java.io.*;
import java.lang.*;
import java.net.*;
import java.util.*;


/**
 * Nutzen Sie diese Klasse um den HTTP Server zu implementieren. Sie duerfen
 * weitere Klassen erstellen, sowie Klassen aus den in der Aufgabe aufgelisteten
 * Paketen benutzen. Achten Sie darauf, Ihren Code zu dokumentieren und moegliche
 * Ausnahmen (Exceptions) sinnvoll zu behandeln.
 *
 * @author Vorname Nachname, Matrikelnummer
 * @author Vorname Nachname, Matrikelnummer
 * @author Vorname Nachname, Matrikelnummer
 */
public class HttpServer
{
    /**
     * Beispiel Dokumentation fuer dieses Attribut:
     * Dieses Attribut gibt den Basis-Ordner fuer den HTTP-Server an.
     */
    private static final File wwwroot = new File("wwwroot");
    
    /**
     * Der Port, auf dem der HTTP-Server lauschen soll.
     */
    private int port;

    /**
     * Socket welcher auf ankommende Verbindungen wartet.
     */
    private ServerSocket serverSocket;

    /**
     * Server ist online.
     */
    private boolean online;

    /**
     * Beispiel Dokumentation fuer diesen Konstruktor:
     * Der Server wird initialisiert und der gewuenschte Port
     * gespeichert.
     * 
     * @param port
     *            der Port auf dem der HTTP-Server lauschen soll
     */
    public HttpServer(int port)
    {
        this.port = port;

        try {
            serverSocket = new ServerSocket();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Beispiel Dokumentation fuer diese Methode:
     * Diese Methode oeffnet einen Port, auf dem der HTTP-Server lauscht.
     * Eingehende Verbindungen werden in einem eigenen Thread behandelt.
     */
    public void startServer()
    {
        System.out.println("Starting Server on Port " + port);
        online = true;

        try {
            serverSocket.bind(new InetSocketAddress(port));

            Thread thread = new Thread(() -> {
                while (online){
                    try {
                        Socket client = serverSocket.accept();

                        PrintWriter out = new PrintWriter(
                                client.getOutputStream(), true
                        );

                        BufferedReader in = new BufferedReader(
                                new InputStreamReader(
                                        client.getInputStream()
                                )
                        );

                        Thread clientThread = new Thread(() -> {
                            try {
                                Request request = new Request(in);

                                Response response = new Response(request, client.getOutputStream());

                                response.sendResponse();

                                client.close();

                            } catch (Exception e) {
                                Response.sendErrorResponse(out, e);
                            }
                        });

                        clientThread.start();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            thread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void dispose()
    {
        online = false;

        try {
            serverSocket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
