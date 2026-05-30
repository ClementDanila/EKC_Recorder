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

        if (localMode) {
            return new RecorderConfig(true, localDirectory, filePast, fileOngoing, fileFuture, category, validateXml, "", "", 0, "");
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

        return new RecorderConfig(false, localDirectory, filePast, fileOngoing, fileFuture, category, validateXml, username, host, port, password);
    }

    private static String required(Properties properties, String key) throws IOException {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IOException("Propriété manquante ou vide : " + key);
        }

        return value.trim();
    }
}
