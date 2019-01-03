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
 * @author Daniel Kilimnik, 201143
 * @author Leonard Fricke, 201173
 * @author Sönke Tiemann, 205951
 */
public class HttpServer
{
    /**
     * Beispiel Dokumentation fuer dieses Attribut:
     * Dieses Attribut gibt den Basis-Ordner fuer den HTTP-Server an.
     */
    public static final File wwwroot = new File("wwwroot");
    
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
        System.out.println("Type \"help\" for a list of available commadns.");
        init(port);
    }

    /**
     * Initialisierung des Servers
     *
     * @param port
     *              der Port auf dem der HTTP-Server lauschen soll
     */
    private void init(int port){
        this.port = port;
        if(port < 0 || port > 65535){
            System.out.println("Given Port was out of Range - Defaulting to 80");
            this.port = 80;
        }
        try {
            serverSocket = new ServerSocket();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Diese Methode oeffnet einen Port, auf dem der HTTP-Server lauscht.
     * Eingehende Verbindungen werden in einem eigenen Thread behandelt und verarbeitet.
     */
    public void startServer()
    {
        System.out.println("Starting Server on Port " + port);
        online = true;

        initCommands();

        try {
            serverSocket.bind(new InetSocketAddress(port));

            Thread thread = new Thread(() -> {
                while (online){
                    try {
                        Socket client = serverSocket.accept();

                        System.out.println("Client connected from " + client.getRemoteSocketAddress().toString() + " connected.");

                        PrintWriter out = new PrintWriter(client.getOutputStream(), true);

                        BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));

                        Thread clientThread = new Thread(() -> {
                            try {
                                Request request = new Request(in);

                                Response response = new Response(request, client.getOutputStream());

                                response.sendResponse();

                                client.close();

                            } catch (Exception e) {
                                //Unerwarteter Fehler aufgetreten

                                Response.sendErrorResponse(out, e);
                            }
                        });

                        clientThread.start();

                    } catch (SocketException e){
                        //Fehler welcher beim beenden des Serves auftritt.
                    } catch (IOException e) {
                        //Fehler beim annehmen eines Nutzers

                        e.printStackTrace();
                    }
                }
            });
            thread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Initialisierung der verfügabren Kommados
     */
    private void initCommands(){
        Thread thread = new Thread(() -> {
            Scanner scanner = new Scanner(System.in);

            while (online){
                //Warten auf Eingabe
                while (!scanner.hasNextLine()){}

                String input = scanner.nextLine();
                String[] splits = input.split(" ");

                int port = -1;
                try {
                    port = Integer.parseInt(splits[1]);
                }catch (Exception e){}

                //Verarbietung von Eingaben
                if (input.equalsIgnoreCase("exit")){
                    dispose();
                }else if (input.equalsIgnoreCase("help")){
                    System.out.println("Known commands:");
                    System.out.println("exit        Stopping the server.");
                    System.out.println("help        List all available commands.");
                    System.out.println("port        Print current Port.");
                    System.out.println("port <Int>  Change Port to <Int>.");
                }else if (splits[0].equalsIgnoreCase("port") && (splits.length == 1 || (splits.length == 2 && port != -1))){
                    if (splits.length == 1){
                        System.out.println("Server connected to Port " + this.port + ".");
                    }else{
                        System.println(port);
                        dispose();

                        init(port);
                        startServer();

                        return;
                    }
                }else {
                    System.out.println("Unkown Command: " + input + ". Type help for known commands.");
                }
            }
        });
        thread.start();
    }

    /**
     * Beenden des Serves
     */
    private void dispose()
    {
        System.out.println("Stopping Server.");

        online = false;

        try {
            serverSocket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
