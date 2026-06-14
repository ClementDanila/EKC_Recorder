package com.ekc.recorder.util;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.net.ftp.FTPClient;

public final class ChangeMonitor {

    private final RecorderConfig config;
    private final FTPClient ftpClient;
    private final ChangeLog changeLog = new ChangeLog();
    private final ChangeJournalWriter writer = new ChangeJournalWriter();
    private final XmlDiffCalculator diffCalculator = new XmlDiffCalculator();
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Object monitorLock = new Object();

    public ChangeMonitor(RecorderConfig config, FTPClient ftpClient) {
        this.config = config;
        this.ftpClient = ftpClient;
    }

    public void run() throws IOException, InterruptedException {
        Map<String, String> previousSnapshots = captureCurrentSnapshots();
        for (TrackedFile trackedFile : trackedFiles()) {
            changeLog.add(trackedFile.reference(), List.of(new XmlChange("/", XmlChangeType.INITIAL, "",
                    previousSnapshots.get(trackedFile.reference()))));
        }
        writer.write(config.changesFile(), changeLog.records());

        while (running.get()) {
            System.out.println("check diff at " + Instant.now());
            waitForNextCheck();
            if (!running.get()) {
                break;
            }

            if (!config.localMode()) {
                refreshFromFtp();
            }
            try {
                Map<String, String> currentSnapshots = captureCurrentSnapshots();
                detectAndStoreChanges(previousSnapshots, currentSnapshots);
                previousSnapshots = currentSnapshots;
                writer.write(config.changesFile(), changeLog.records());
            } catch (Exception e) {
                System.err.printf("Erreur lors de la détection des changements : %s%n", e.getMessage());
                e.printStackTrace(System.err);
            }
        }
    }

    public void stop() {
        running.set(false);
        synchronized (monitorLock) {
            monitorLock.notifyAll();
        }
    }

    private void waitForNextCheck() throws InterruptedException {
        synchronized (monitorLock) {
            monitorLock.wait(config.checkIntervalMs());
        }
    }

    private void refreshFromFtp() throws IOException {
        if (ftpClient == null || !ftpClient.isConnected()) {
            throw new IOException("Connexion FTP indisponible pour le rafraîchissement périodique.");
        }

        for (TrackedFile trackedFile : trackedFiles()) {
            Path targetFile = trackedFile.path();
            Path parent = targetFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            try (OutputStream outputStream = Files.newOutputStream(targetFile)) {
                if (!ftpClient.retrieveFile(trackedFile.reference(), outputStream)) {
                    throw new IOException("Impossible de rafraîchir le fichier FTP : " + trackedFile.reference());
                }
            }
        }
    }

    private Map<String, String> captureCurrentSnapshots() throws IOException {
        Map<String, String> snapshots = new LinkedHashMap<>();

        for (TrackedFile trackedFile : trackedFiles()) {
            Path path = trackedFile.path();
            if (!Files.exists(path)) {
                throw new IOException("Fichier introuvable : " + path.toAbsolutePath());
            }

            snapshots.put(trackedFile.reference(), Files.readString(path));
        }

        return snapshots;
    }

    private void detectAndStoreChanges(Map<String, String> previous, Map<String, String> current) throws IOException {
        for (TrackedFile trackedFile : trackedFiles()) {
            String beforeXml = previous.get(trackedFile.reference());
            String afterXml = current.get(trackedFile.reference());
            if (beforeXml == null || afterXml == null) {
                continue;
            }

            List<XmlChange> changes = diffCalculator.diff(beforeXml, afterXml);
            if (!changes.isEmpty()) {
                System.out.println("changes detected !");
                changeLog.add(trackedFile.reference(), changes);
            }
        }
    }

    private List<TrackedFile> trackedFiles() {
        return List.of(
                new TrackedFile(config.filePast(), config.localDirectory().resolve(config.filePast())),
                new TrackedFile(config.fileOngoing(), config.localDirectory().resolve(config.fileOngoing())),
                new TrackedFile(config.fileFuture(), config.localDirectory().resolve(config.fileFuture())));
    }
}
