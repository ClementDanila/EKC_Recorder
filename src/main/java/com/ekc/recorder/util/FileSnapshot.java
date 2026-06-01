package com.ekc.recorder.util;

import java.nio.file.Path;
import java.time.Instant;

public record FileSnapshot(Path file, String content, Instant capturedAt) {
}

