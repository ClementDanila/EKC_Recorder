package com.ekc.recorder.util;

import java.time.Instant;
import java.util.List;

public record ChangeRecord(
        long sequence,
        String fileReference,
        Instant detectedAt,
        List<XmlChange> changes) {
}
