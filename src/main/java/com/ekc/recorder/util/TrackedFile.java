package com.ekc.recorder.util;

import java.nio.file.Path;

public record TrackedFile(String reference, Path path) {
}

