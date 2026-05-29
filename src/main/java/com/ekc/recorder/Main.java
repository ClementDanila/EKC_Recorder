package com.ekc.recorder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

public final class Main {

    private static final String DEFAULT_CONFIG_FILE = "ftp.properties";

    private Main() {
        // Utility class
    }

    public static void main(String[] args) {
        Path configPath = resolveConfigPath(args);
        FTPClient ftpClient = new FTPClient();

        try {
            FtpConfig config = loadConfig(configPath);
            Files.createDirectories(config.localDirectory());
            if (config.localMode()) {
                System.out.printf("Mode local activé : dossier de travail '%s'.%n",
                        config.localDirectory().toAbsolutePath());
                ensureLocalFilesExist(config);
            } else {
                connect(ftpClient, config);
                System.out.printf("Connexion FTP établie vers %s:%d pour l'utilisateur %s.%n",
                        config.host(), config.port(), config.username());
                ensureFtpFilesExist(ftpClient, config);
                copyFtpFilesToLocalDirectory(ftpClient, config);
            }
            System.out.println("Appuie sur Entrée pour quitter...");

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
                reader.readLine();
            }
        } catch (IOException | RuntimeException e) {
            System.err.printf("Impossible de démarrer l'application : %s%n", e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        } finally {
            disconnectQuietly(ftpClient);
        }
    }

    private static Path resolveConfigPath(String[] args) {
        if (args != null && args.length > 0 && !args[0].isBlank()) {
            return Paths.get(args[0]);
        }

        String systemProperty = System.getProperty("ftp.config");
        if (systemProperty != null && !systemProperty.isBlank()) {
            return Paths.get(systemProperty);
        }

        return Paths.get(DEFAULT_CONFIG_FILE);
    }

    private static FtpConfig loadConfig(Path configPath) throws IOException {
        if (!Files.exists(configPath)) {
            throw new IOException("Fichier de propriétés introuvable : " + configPath.toAbsolutePath());
        }

        Properties properties = new Properties();
        try (BufferedReader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }

        boolean localMode = Boolean.parseBoolean(properties.getProperty("local", "false").trim());
        String localDirectoryValue = required(properties, "localDirectory");
        String filePast = required(properties, "file_past");
        String fileOngoing = required(properties, "file_ongoing");
        String fileFuture = required(properties, "file_future");

        if (localMode) {
            return new FtpConfig("", "", 0, "", true, Paths.get(localDirectoryValue), filePast, fileOngoing,
                    fileFuture);
        }

        String username = required(properties, "username");
        String host = required(properties, "host");
        String password = required(properties, "password");
        String portValue = required(properties, "port");

        int port;
        try {
            port = Integer.parseInt(portValue);
        } catch (NumberFormatException e) {
            throw new IOException("La propriété 'port' doit être un entier valide : " + portValue, e);
        }

        if (port < 1 || port > 65535) {
            throw new IOException("La propriété 'port' doit être comprise entre 1 et 65535 : " + port);
        }

        return new FtpConfig(username, host, port, password, false, null, filePast, fileOngoing, fileFuture);
    }

    private static String required(Properties properties, String key) throws IOException {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IOException("Propriété FTP manquante ou vide : " + key);
        }

        return value.trim();
    }

    private static void connect(FTPClient ftpClient, FtpConfig config) throws IOException {
        ftpClient.setConnectTimeout(10_000);
        ftpClient.connect(config.host(), config.port());

        if (!FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
            safeDisconnect(ftpClient);
            throw new IOException("Le serveur FTP a refusé la connexion : code de réponse " + ftpClient.getReplyCode());
        }

        if (!ftpClient.login(config.username(), config.password())) {
            safeDisconnect(ftpClient);
            throw new IOException("Authentification FTP refusée pour l'utilisateur : " + config.username());
        }

        ftpClient.enterLocalPassiveMode();
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
    }

    private static void ensureLocalFilesExist(FtpConfig config) throws IOException {
        ensureLocalFileExists(config, config.filePast());
        ensureLocalFileExists(config, config.fileOngoing());
        ensureLocalFileExists(config, config.fileFuture());
    }

    private static void ensureLocalFileExists(FtpConfig config, String fileName) throws IOException {
        Path filePath = config.localDirectory().resolve(fileName);
        if (!Files.exists(filePath)) {
            throw new IOException("Fichier local introuvable : " + filePath.toAbsolutePath());
        }

        System.out.printf("Fichier local trouvé : %s%n", filePath.toAbsolutePath());
    }

    private static void ensureFtpFilesExist(FTPClient ftpClient, FtpConfig config) throws IOException {
        ensureFtpFileExists(ftpClient, config.filePast());
        ensureFtpFileExists(ftpClient, config.fileOngoing());
        ensureFtpFileExists(ftpClient, config.fileFuture());
    }

    private static void copyFtpFilesToLocalDirectory(FTPClient ftpClient, FtpConfig config) throws IOException {
        copyFtpFileToLocalDirectory(ftpClient, config, config.filePast());
        copyFtpFileToLocalDirectory(ftpClient, config, config.fileOngoing());
        copyFtpFileToLocalDirectory(ftpClient, config, config.fileFuture());
    }

    private static void copyFtpFileToLocalDirectory(FTPClient ftpClient, FtpConfig config, String fileName)
            throws IOException {
        Path targetFile = config.localDirectory().resolve(fileName);
        Path parent = targetFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (OutputStream outputStream = Files.newOutputStream(targetFile)) {
            if (!ftpClient.retrieveFile(fileName, outputStream)) {
                throw new IOException("Impossible de copier le fichier FTP '" + fileName + "' vers '"
                        + targetFile.toAbsolutePath() + "'");
            }
        }

        System.out.printf("Fichier FTP copié vers : %s%n", targetFile.toAbsolutePath());
    }

    private static void ensureFtpFileExists(FTPClient ftpClient, String fileName) throws IOException {
        if (ftpClient.listFiles(fileName).length == 0) {
            throw new IOException("Fichier FTP introuvable : " + fileName);
        }

        System.out.printf("Fichier FTP trouvé : %s%n", fileName);
    }

    private static void disconnectQuietly(FTPClient ftpClient) {
        if (ftpClient == null || !ftpClient.isConnected()) {
            return;
        }

        try {
            ftpClient.logout();
        } catch (IOException ignored) {
            // Ignored on shutdown
        }

        try {
            ftpClient.disconnect();
        } catch (IOException ignored) {
            // Ignored on shutdown
        }
    }

    private static void safeDisconnect(FTPClient ftpClient) {
        disconnectQuietly(ftpClient);
    }

    private record FtpConfig(String username, String host, int port, String password, boolean localMode,
            Path localDirectory, String filePast, String fileOngoing, String fileFuture) {
    }
}

