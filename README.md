# FileServer — Serveur de fichiers (Java)

Description
-----------

Ce projet implémente un petit serveur de fichiers écrit en Java avec un client console minimal. Il permet de lister les fichiers disponibles, récupérer un fichier en binaire et consulter des statistiques simples.

Fonctionnalités
--------------

- Serveur TCP multi‑clients (thread par client).
- Commandes prises en charge : LIST, GET, SIZE, COUNT, STATS, HELP, QUIT.
- Répertoire partagé côté serveur : `shared/` (créé automatiquement si nécessaire).
- Répertoire de téléchargement côté client : `downloads/` (créé automatiquement).

Prérequis
---------

- Java 8 ou ultérieur installé (`java` et `javac` disponibles dans le PATH).

Structure du dépôt
------------------

- `src/` : sources Java (package `src`).
- `shared/` : fichiers partagés par le serveur (ex. `file1.txt`).
- `downloads/` : dossier local où le client place les fichiers reçus.

Compilation
-----------

Depuis la racine du projet, compiler les sources vers un dossier de classes :

```bash
javac -d out src/*.java
```

Exécution
---------

1) Démarrer le serveur (port par défaut 12346) :

```bash
java -cp out src.FileServer
```

Le serveur crée automatiquement le dossier `shared/` si nécessaire et affiche des informations de démarrage.

2) Lancer le client depuis une autre console (connexion à `localhost` et port par défaut) :

```bash
java -cp out src.FileClient [hote] [port]
# Exemple : java -cp out src.FileClient localhost 12346
```

Utilisation (commandes client)
-------------------------------

- `LIST`  — liste les fichiers disponibles dans le dossier partagé.
- `GET <nomFichier>` — télécharge le fichier demandé vers `downloads/`.
- `SIZE <nomFichier>` — affiche la taille sans transférer.
- `COUNT` — nombre total de requêtes traitées par le serveur.
- `STATS` — affiche un tableau de bord simple du serveur.
- `HELP` — affiche l'aide.
- `QUIT` — se déconnecter proprement.

Notes et comportement
---------------------

- Le serveur refuse les chemins contenant `..`, `/` ou `\\` pour des raisons de sécurité.
- Le client affiche une barre de progression pendant le téléchargement.
- Les classes Java utilisent le package `src` ; les commandes `javac -d out` et `java -cp out src.FileServer` permettent d'exécuter correctement les classes.

