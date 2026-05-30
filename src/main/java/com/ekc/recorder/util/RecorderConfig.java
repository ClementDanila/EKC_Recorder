package com.ekc.recorder.util;

import java.nio.file.Path;

public record RecorderConfig(
        boolean localMode,
        Path localDirectory,
        String filePast,
        String fileOngoing,
        String fileFuture,
        RecorderCategory category,
        boolean validateXml,
        String username,
        String host,
        int port,
        String password) {
}
