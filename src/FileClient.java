package src;

import java.io.*;
import java.net.*;
import java.util.Scanner;


public class FileClient {

    private static final String DOWNLOADS = "downloads";
    private static final String SEPARATEUR = "---";

    public static void main(String[] args) {

        String hote = args.length > 0 ? args[0] : "localhost";
        int    port = args.length > 1 ? Integer.parseInt(args[1]) : FileServer.PORT;

        //Creer le dossier de telechargement si besoin
        new File(DOWNLOADS).mkdirs();

        System.out.println("Connexion à " + hote + ":" + port + " ...");

        try (
            Socket         socket   = new Socket(hote, port);
            BufferedReader lecteur  = new BufferedReader(
                                     new InputStreamReader(socket.getInputStream(), "UTF-8"));
            PrintWriter    ecrivain = new PrintWriter(
                                new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            Scanner        clavier  = new Scanner(System.in)
        ) {
            System.out.println("Connecté !\n");

            // Afficher le message  du serveur
            lireJusquaSeparateur(lecteur);

            while (true) {
                System.out.print("\n> ");
                if (!clavier.hasNextLine()) break;

                String saisie = clavier.nextLine().trim();
                if (saisie.isEmpty()) continue;

                // Envoyer la commande au serveur
                ecrivain.println(saisie);

                String[] parties  = saisie.split("\\s+", 2);
                String   commande = parties[0].toUpperCase();

                switch (commande) {
                    case "QUIT" -> {
                        String rep = lecteur.readLine();
                        System.out.println(rep != null ? rep : "");
                        System.out.println("Déconnecté. Au revoir !");
                        return;
                    }
                    case "GET" -> {
                        String nomFichier = parties.length > 1 ? parties[1].trim() : "";
                        recevoirFichier(nomFichier, lecteur, socket.getInputStream());
                    }
                    default -> lireJusquaSeparateur(lecteur);
                }
            }

        } catch (ConnectException e) {
            System.err.println(
                "ERREUR Impossible de se connecter. Le serveur est-il démarré ?");
        } catch (IOException e) {
            System.err.println("ERREUR " + e.getMessage());
        }
    }

    //reception de fichier 
    private static void recevoirFichier(String nomFichier,
                                        BufferedReader lecteur,
                                        InputStream flux) throws IOException {

        String premiereLigne = lecteur.readLine();
        if (premiereLigne == null) return;

        // Erreur serveur
        if (premiereLigne.startsWith("ERREUR")) {
            System.out.println(premiereLigne);
            lireJusquaSeparateur(lecteur);
            return;
        }

        //reponse attendue : FILE_START <nom> <taille>
        if (!premiereLigne.startsWith("FILE_START")) {
            System.out.println(premiereLigne);
            lireJusquaSeparateur(lecteur);
            return;
        }

        String[] meta    = premiereLigne.split(" ");
        String   nom     = meta[1];
        long     taille  = Long.parseLong(meta[2]);

        System.out.printf("Téléchargement de « %s » (%s)...%n",
                nom, formaterTaille(taille));

        //ecriture sur disque
        File destination = new File(DOWNLOADS, nom);
        try (FileOutputStream fos = new FileOutputStream(destination)) {
            byte[] tampon = new byte[8192];
            long   recu   = 0;
            int    lu;

            while (recu < taille &&
                   (lu = flux.read(tampon, 0, (int) Math.min(tampon.length, taille - recu))) != -1) {
                fos.write(tampon, 0, lu);
                recu += lu;
                afficherProgression(recu, taille);
            }
        }

        System.out.println();  

        
        String l;
        while ((l = lecteur.readLine()) != null) {
            if (l.equals(SEPARATEUR)) break;
        }

        System.out.println("Fichier sauvegardé → " + DOWNLOADS + "/" + nom);
    }

   
    private static void lireJusquaSeparateur(BufferedReader lecteur) throws IOException {
        String ligne;
        while ((ligne = lecteur.readLine()) != null) {
            System.out.println(ligne);
            if (ligne.equals(SEPARATEUR)) break;
        }
    }

    /** Affiche une barre de progression ASCII sur la même ligne. */
    private static void afficherProgression(long recu, long total) {
        if (total <= 0) return;
        int pct     = (int) (recu * 100 / total);
        int barres  = pct / 5;                     // barre de 20 caractères
        String barre = "*".repeat(barres) + ".".repeat(20 - barres);
        System.out.printf("\r  [%s] %3d%%  %s / %s  ",
                barre, pct,
                formaterTaille(recu), formaterTaille(total));
        System.out.flush();
    }

    private static String formaterTaille(long octets) {
        if (octets < 1024)       return octets + " o";
        if (octets < 1_048_576)  return String.format("%.1f Ko", octets / 1024.0);
        return                          String.format("%.1f Mo", octets / 1_048_576.0);
    }
}