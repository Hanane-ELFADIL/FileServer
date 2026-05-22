package src;

import java.io.*;
import java.net.Socket;


public class ClientHandler implements Runnable {

    //dossier partage passé par le serveur au constructeur
    private final String dossierPartage;

    //Socket et etat partage
    private final Socket      socket;
    private final SharedState etat;

    
    private final String idClient;

    public ClientHandler(Socket socket, SharedState etat, String dossierPartage) {
        this.socket         = socket;
        this.etat           = etat;
        this.dossierPartage = dossierPartage;
        this.idClient       = socket.getInetAddress().getHostAddress()+ ":" + socket.getPort();
    }

    

    @Override
    public void run() {
        etat.clientConnecte();
        etat.log("CONNECT", idClient + "  (actifs : " + etat.getClientsConnectes() + ")");

        try (
            BufferedReader lecteur   = new BufferedReader(
                                           new InputStreamReader(socket.getInputStream(),  "UTF-8"));
            PrintWriter    ecrivain  = new PrintWriter(
                                           new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            OutputStream   binaire   = socket.getOutputStream()
        ) {
            envoyerBienvenue(ecrivain);

            String ligne;
            while ((ligne = lecteur.readLine()) != null) {

                ligne = ligne.trim();
                if (ligne.isEmpty()) continue;

                
                String[] parts    = ligne.split("\\s+", 2);
                String   commande = parts[0].toUpperCase();
                String   argument = parts.length > 1 ? parts[1].trim() : "";

                switch (commande) {
                    case "LIST"  -> cmdList(ecrivain);
                    case "GET"   -> cmdGet(argument, ecrivain, binaire);
                    case "SIZE"  -> cmdSize(argument, ecrivain);
                    case "COUNT" -> cmdCount(ecrivain);
                    case "STATS" -> cmdStats(ecrivain);
                    case "HELP"  -> cmdHelp(ecrivain);
                    case "QUIT"  -> { cmdQuit(ecrivain); return; }
                    default      -> {
                        ecrivain.println("ERREUR : commande inconnue « " + parts[0] + " ».");
                        ecrivain.println("Tapez HELP pour la liste des commandes.");
                        ecrivain.println("---");
                        etat.log("INVALIDE", idClient + " -> \"" + ligne + "\"");
                    }
                }
            }

            // le client a fermé la connexion sans quit
            etat.log("RUPTURE", idClient + " déconnecté sans QUIT");

        } catch (IOException e) {
            etat.log("RUPTURE", idClient + " : " + e.getMessage());
        } finally {
            etat.clientDeconnecte();
            etat.log("DEPART", idClient + "  (actifs : " + etat.getClientsConnectes() + ")");
            fermerSocket();
        }
    }

   

    // LIST — liste les fichiers du dossier partagé. 
    private void cmdList(PrintWriter out) {
        File[] fichiers = listerFichiers();

        if (fichiers == null || fichiers.length == 0) {
            out.println("INFO : aucun fichier disponible pour l'instant.");
        } else {
            out.println("LISTE — " + fichiers.length + " fichier(s) disponible(s) :");
            for (File f : fichiers) {
                out.printf("  %-30s  %s%n", f.getName(), formaterTaille(f.length()));
            }
        }
        out.println("---");
        etat.requeteTraitee();
        etat.log("LIST", idClient);
    }

    
     // GET  envoie le contenu binaire d'un fichier.
    private void cmdGet(String nomFichier, PrintWriter out, OutputStream binaire)
            throws IOException {

        if (nomFichier.isEmpty()) {
            out.println("ERREUR : usage -> GET <nomFichier>");
            out.println("---");
            return;
        }

        File fichier = resoudre(nomFichier);
        if (fichier == null) {
            out.println("ERREUR : accès refusé (nom de fichier non autorisé).");
            out.println("---");
            etat.log("REFUSE", idClient + " -> " + nomFichier);
            return;
        }
        if (!fichier.exists() || !fichier.isFile()) {
            out.println("ERREUR : fichier introuvable → " + nomFichier);
            out.println("---");
            etat.log("ABSENT", idClient + " -> " + nomFichier);
            return;
        }

        long taille = fichier.length();

        // Annonce (texte)
        out.println("FILE_START " + fichier.getName() + " " + taille);
        out.flush();

        // Envoi binaire
        byte[] tampon = new byte[8192];
        try (FileInputStream fis = new FileInputStream(fichier)) {
            int lu;
            while ((lu = fis.read(tampon)) != -1) {
                binaire.write(tampon, 0, lu);
            }
            binaire.flush();
        }

        // Marqueur de fin (texte)
        out.println("");
        out.println("FILE_END");
        out.println("---");

        etat.fichierEnvoye(taille);
        etat.log("GET", idClient + " <- " + nomFichier
                + "  (" + formaterTaille(taille) + ")"
                + "  [total DL : " + etat.getTotalTelechargements() + "]");
    }

    // SIZE  renvoie la taille sans télécharger. 
    private void cmdSize(String nomFichier, PrintWriter out) {
        if (nomFichier.isEmpty()) {
            out.println("ERREUR : usage -> SIZE <nomFichier>");
            out.println("---");
            return;
        }
        File fichier = resoudre(nomFichier);
        if (fichier == null || !fichier.exists()) {
            out.println("ERREUR : fichier introuvable → " + nomFichier);
        } else {
            out.println("SIZE " + fichier.getName() + " " + fichier.length()
                    + "  (" + formaterTaille(fichier.length()) + ")");
        }
        out.println("---");
        etat.requeteTraitee();
        etat.log("SIZE", idClient + " -> " + nomFichier);
    }

    // COUNT  nombre total de requêtes traitées depuis le démarrage.
    private void cmdCount(PrintWriter out) {
        out.println("RESULT " + etat.getTotalRequetes() + " requête(s) traitée(s) au total.");
        out.println("---");
        etat.requeteTraitee();
        etat.log("COUNT", idClient);
    }

    // STATS  tableau de bord du serveur. 
    private void cmdStats(PrintWriter out) {
        out.print(etat.resume());
        out.println("---");
        etat.requeteTraitee();
        etat.log("STATS", idClient);
    }

    // HELP  liste des commandes disponibles.
    private void cmdHelp(PrintWriter out) {
        out.println("Commandes disponibles :");
        out.println("  LIST               liste les fichiers partagés");
        out.println("  GET  <fichier>     télécharge un fichier");
        out.println("  SIZE <fichier>     affiche la taille sans télécharger");
        out.println("  COUNT              nombre de requêtes traitées");
        out.println("  STATS              statistiques globales du serveur");
        out.println("  HELP               affiche cette aide");
        out.println("  QUIT               se déconnecter");
        out.println("---");
    }

        // QUIT  se déconnecter proprement.
    private void cmdQuit(PrintWriter out) {
        out.println("BYE — à bientôt !");
        etat.log("QUIT", idClient);
    }

    

    // Envoie le message d'accueil à la connexion. 
    private void envoyerBienvenue(PrintWriter out) {        
        out.println("   Serveur de fichiers — bienvenue !       ");
        out.println("Tapez HELP pour la liste des commandes.");
        out.println("---");
    }

    // Liste les fichiers du dossier partagé (triés par nom). 
    private File[] listerFichiers() {
        File dossier = new File(dossierPartage);
        File[] fichiers = dossier.listFiles(File::isFile);
        if (fichiers != null) java.util.Arrays.sort(fichiers);
        return fichiers;
    }

    
    private File resoudre(String nom) {
        if (nom.contains("..") || nom.contains("/") || nom.contains("\\")) return null;
        return new File(dossierPartage, nom);
    }

    private void fermerSocket() {
        try { if (!socket.isClosed()) socket.close(); }
        catch (IOException ignored) {}
    }

    // Formatte un nombre d'octets en unité lisible. 
    private static String formaterTaille(long octets) {
        if (octets < 1024)        return octets + " o";
        if (octets < 1_048_576)   return String.format("%.1f Ko", octets / 1024.0);
        return                           String.format("%.1f Mo", octets / 1_048_576.0);
    }
}