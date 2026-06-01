package com.ekc.recorder;

import java.io.IOException;
import java.time.Instant;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.ekc.recorder.util.RecorderApplication;
import com.ekc.recorder.util.RecorderConfig;
import com.ekc.recorder.util.RecorderConfigLoader;

public final class Main {

    private static final String DEFAULT_CONFIG_FILE = "ftp.properties";

    private Main() {
        // Utility class
    }

    public static void main(String[] args) {
        Path configPath = resolveConfigPath(args);
        Instant launchTimestamp = Instant.now();

        try {
            RecorderConfig config = RecorderConfigLoader.load(configPath, launchTimestamp);
            new RecorderApplication().run(config);
        } catch (IOException | RuntimeException e) {
            System.err.printf("Impossible de démarrer l'application : %s%n", e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
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
}

