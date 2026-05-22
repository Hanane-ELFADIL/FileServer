package src;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class SharedState {

    private int clientsConnectes    = 0;
    private int totalConnexions     = 0;   
    private int totalTelechargements= 0;
    private int totalRequetes       = 0;   
    private long octetsTransferes   = 0;   

    private final List<String> journal = new ArrayList<>();

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    
    public synchronized void clientConnecte() {
        clientsConnectes++;
        totalConnexions++;
    }

    public synchronized void clientDeconnecte() {
        if (clientsConnectes > 0) clientsConnectes--;
    }

    public synchronized int getClientsConnectes()  { return clientsConnectes; }
    public synchronized int getTotalConnexions()   { return totalConnexions;  }

    

    public synchronized void fichierEnvoye(long taille) {
        totalTelechargements++;
        octetsTransferes += taille;
        totalRequetes++;
    }

    public synchronized int  getTotalTelechargements() { return totalTelechargements; }
    public synchronized long getOctetsTransferes()     { return octetsTransferes;     }

    

    public synchronized void requeteTraitee() {
        totalRequetes++;
    }

    public synchronized int getTotalRequetes() { return totalRequetes; }

   

    public synchronized void log(String niveau, String message) {
        String horodatage = LocalDateTime.now().format(FMT);
        String entree = String.format("[%s] %-9s %s", horodatage, niveau, message);
        journal.add(entree);
        System.out.println(entree);
    }

    public synchronized List<String> getJournal() {
        return Collections.unmodifiableList(new ArrayList<>(journal));
    }

    

    public synchronized String resume() {
        return String.format(
            " Statistiques du serveur %n" +
            "  Clients actuellement connectés : %d%n" +
            "  Total connexions depuis démarrage : %d%n" +
            "  Fichiers téléchargés : %d%n" +
            "  Volume transféré : %s%n" +
            "  Requêtes traitées : %d%n" +
            "------------------%n",
            clientsConnectes,
            totalConnexions,
            totalTelechargements,
            formaterTaille(octetsTransferes),
            totalRequetes
        );
    }

    

    private static String formaterTaille(long octets) {
        if (octets < 1024)             return octets + " o";
        if (octets < 1024 * 1024)      return String.format("%.1f Ko", octets / 1024.0);
        if (octets < 1024 * 1024 * 1024) return String.format("%.1f Mo", octets / (1024.0 * 1024));
        return String.format("%.1f Go", octets / (1024.0 * 1024 * 1024));
    }
}