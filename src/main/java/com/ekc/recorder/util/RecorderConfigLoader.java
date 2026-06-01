package com.ekc.recorder.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class RecorderConfigLoader {

    private RecorderConfigLoader() {
    }

    public static RecorderConfig load(Path configPath) throws IOException {
        if (!Files.exists(configPath)) {
            throw new IOException("Fichier de propriétés introuvable : " + configPath.toAbsolutePath());
        }

        Properties properties = new Properties();
        try (BufferedReader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }

        boolean localMode = Boolean.parseBoolean(properties.getProperty("local", "false").trim());
        Path localDirectory = Path.of(required(properties, "localDirectory"));
        String filePast = required(properties, "file_past");
        String fileOngoing = required(properties, "file_ongoing");
        String fileFuture = required(properties, "file_future");
        RecorderCategory category = RecorderCategory.fromProperty(required(properties, "category"));
        boolean validateXml = Boolean.parseBoolean(properties.getProperty("validate_xml", "true").trim());
        long checkIntervalMs = parsePositiveLong(properties.getProperty("check_interval_ms", "60000"), "check_interval_ms");
        Path changesFile = resolveChangesFile(properties.getProperty("changes_file"), localDirectory);

        if (localMode) {
            return new RecorderConfig(true, localDirectory, filePast, fileOngoing, fileFuture, category, validateXml,
                    checkIntervalMs, changesFile, "", "", 0, "");
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

        return new RecorderConfig(false, localDirectory, filePast, fileOngoing, fileFuture, category, validateXml,
                checkIntervalMs, changesFile, username, host, port, password);
    }

    private static Path resolveChangesFile(String rawValue, Path localDirectory) {
        if (rawValue == null || rawValue.isBlank()) {
            return localDirectory.resolve("changes.json");
        }

        return Path.of(rawValue.trim());
    }

    private static long parsePositiveLong(String value, String key) throws IOException {
        try {
            long parsed = Long.parseLong(value.trim());
            if (parsed <= 0) {
                throw new IOException("La propriété '" + key + "' doit être strictement positive : " + value);
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IOException("La propriété '" + key + "' doit être un entier valide : " + value, e);
        }
    }

    private static String required(Properties properties, String key) throws IOException {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IOException("Propriété manquante ou vide : " + key);
        }

        return value.trim();
    }
}
