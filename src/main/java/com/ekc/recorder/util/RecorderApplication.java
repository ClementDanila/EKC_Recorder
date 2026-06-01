package com.ekc.recorder.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

public final class RecorderApplication {

    public void run(RecorderConfig config) throws IOException {
        Files.createDirectories(config.localDirectory());
        ensureChangesFile(config);

        FTPClient ftpClient = null;
        if (!config.localMode()) {
            ftpClient = new FTPClient();
            connect(ftpClient, config);
            copyFilesFromFtp(ftpClient, config);
        } else {
            System.out.printf("Mode local activé : dossier '%s'.%n", config.localDirectory().toAbsolutePath());
            validateLocalFiles(config);
        }

        if (config.validateXml()) {
            validateXmlFiles(config);
        }

        ChangeMonitor monitor = new ChangeMonitor(config, ftpClient);
        Thread monitoringThread = new Thread(() -> {
            try {
                monitor.run();
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, "xml-change-monitor");
        monitoringThread.setDaemon(true);
        monitoringThread.start();

        try {
            waitForEnter();
        } finally {
            monitor.stop();
            monitoringThread.interrupt();
            try {
                monitoringThread.join(5_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            disconnectQuietly(ftpClient);
        }
    }

    private void ensureChangesFile(RecorderConfig config) throws IOException {
        Path changesFile = config.changesFile();
        if (changesFile != null) {
            Path parent = changesFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        }
    }

    private void validateLocalFiles(RecorderConfig config) throws IOException {
        for (String fileName : trackedFiles(config)) {
            Path filePath = config.localDirectory().resolve(fileName);
            if (!Files.exists(filePath)) {
                throw new IOException("Fichier local introuvable : " + filePath.toAbsolutePath());
            }
            System.out.printf("Fichier local trouvé : %s%n", filePath.toAbsolutePath());
        }
    }

    private void copyFilesFromFtp(FTPClient ftpClient, RecorderConfig config) throws IOException {
        for (String fileName : trackedFiles(config)) {
            if (ftpClient.listFiles(fileName).length == 0) {
                throw new IOException("Fichier FTP introuvable : " + fileName);
            }
            Path targetFile = config.localDirectory().resolve(fileName);
            Path parent = targetFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (OutputStream outputStream = Files.newOutputStream(targetFile)) {
                if (!ftpClient.retrieveFile(fileName, outputStream)) {
                    throw new IOException("Impossible de copier le fichier FTP '" + fileName + "' vers '" + targetFile.toAbsolutePath() + "'");
                }
            }
            System.out.printf("Fichier FTP copié vers : %s%n", targetFile.toAbsolutePath());
        }
    }

    private void validateXmlFiles(RecorderConfig config) throws IOException {
        XmlFileValidator validator = new XmlFileValidator(config.category());
        for (String fileName : trackedFiles(config)) {
            validator.validate(config.localDirectory().resolve(fileName));
        }
    }

    private List<String> trackedFiles(RecorderConfig config) {
        return List.of(config.filePast(), config.fileOngoing(), config.fileFuture());
    }

    private void connect(FTPClient ftpClient, RecorderConfig config) throws IOException {
        ftpClient.setConnectTimeout(10_000);
        ftpClient.connect(config.host(), config.port());

        if (!FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
            disconnectQuietly(ftpClient);
            throw new IOException("Le serveur FTP a refusé la connexion : code de réponse " + ftpClient.getReplyCode());
        }

        if (!ftpClient.login(config.username(), config.password())) {
            disconnectQuietly(ftpClient);
            throw new IOException("Authentification FTP refusée pour l'utilisateur : " + config.username());
        }

        ftpClient.enterLocalPassiveMode();
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
    }

    private void waitForEnter() throws IOException {
        System.out.println("Appuie sur Entrée pour quitter...");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            reader.readLine();
        }
    }

    private void disconnectQuietly(FTPClient ftpClient) {
        if (ftpClient == null || !ftpClient.isConnected()) {
            return;
        }

        try {
            ftpClient.logout();
        } catch (IOException ignored) {
        }

        try {
            ftpClient.disconnect();
        } catch (IOException ignored) {
        }
    }
}
