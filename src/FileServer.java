package src;

import java.io.*;
import java.net.*;
import java.util.concurrent.atomic.AtomicBoolean;


public class FileServer {

    static final int    PORT            = 12346;
    static final String DOSSIER_PARTAGE = "shared";

    
    private static final AtomicBoolean actif = new AtomicBoolean(true);

    public static void main(String[] args) throws IOException {

        
        File dossier = new File(DOSSIER_PARTAGE);
        if (!dossier.exists() && dossier.mkdirs()) {
            System.out.println("[INFO] Dossier « " + DOSSIER_PARTAGE + " » créé.");
        }

        SharedState etat = new SharedState();

        //démarrage
        System.out.println();
        System.out.println("    Serveur de fichiers — IRIS 2            ");
        System.out.printf( "  Port          : %-27d %n", PORT);
        System.out.printf( "  Dossier       : %-27s %n", DOSSIER_PARTAGE + "/");
        System.out.println("En attente de clients... (Ctrl+C pour arrêter)\n");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            actif.set(false);
            System.out.println("\n[ARRET] Serveur en cours d'arrêt...");
            System.out.println(etat.resume());
        }));

        //boucle principale
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            serverSocket.setReuseAddress(true);

            while (actif.get()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    clientSocket.setKeepAlive(true);

                    Thread t = new Thread(
                        new ClientHandler(clientSocket, etat, DOSSIER_PARTAGE),
                        "client-" + clientSocket.getPort()   // nom lisible dans les logs JVM
                    );
                    t.setDaemon(true);
                    t.start();

                } catch (SocketException e) {
                    if (actif.get()) e.printStackTrace();
                }
            }
        }
    }
}