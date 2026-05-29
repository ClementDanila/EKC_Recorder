# EKC_Recorder

Projet Java minimal avec un point d'entrée qui se connecte d'abord à un serveur FTP, ciblant Java 21.

## Structure

```text
src/
└── main/
	└── java/
		└── com/ekc/recorder/Main.java
ftp.properties.example
```

## Exécution

```bash
mvn clean package
cp -f ftp.properties.example ftp.properties
java -jar target/ekc-recorder-1.0.0-SNAPSHOT.jar ftp.properties
```

Le programme lit les propriétés externes `file_past`, `file_ongoing` et `file_future`, ainsi que les paramètres FTP ou locaux selon le mode choisi, puis attend un appui sur la touche Entrée pour se terminer.

Quand `local=false` (ou absent), les propriétés FTP sont utilisées. Quand `local=true`, `localDirectory` remplace le serveur FTP comme dossier de travail.

En mode FTP, les trois fichiers sont d'abord vérifiés sur le serveur puis copiés dans `localDirectory`.

Pour travailler en local avec un dossier sur ta machine à la place du FTP, ajoute `local=true` et renseigne `localDirectory`. Le dossier est créé automatiquement s'il n'existe pas.

Dans les deux cas, le programme cherche trois fichiers dans le dossier FTP ou dans `localDirectory`, selon le mode utilisé. Les noms de ces fichiers sont définis par les propriétés `file_past`, `file_ongoing` et `file_future`.

Exemple en mode local :

```properties
local=true
localDirectory=local-work
file_past=past.csv
file_ongoing=ongoing.csv
file_future=future.csv
```

Si vous préférez, vous pouvez aussi fournir le chemin via la propriété système `ftp.config` :

```bash
java -Dftp.config=/c/chemin/vers/ftp.properties -jar target/ekc-recorder-1.0.0-SNAPSHOT.jar
```
