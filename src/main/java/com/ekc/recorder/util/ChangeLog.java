package com.ekc.recorder.util;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ChangeLog {

    private long sequence = 0;
    private final List<ChangeRecord> records = new ArrayList<>();

    public synchronized ChangeRecord add(String fileReference, List<XmlChange> changes) {
        ChangeRecord record = new ChangeRecord(++sequence, fileReference, Instant.now(), List.copyOf(changes));
        records.add(record);
        return record;
    }

    public synchronized List<ChangeRecord> records() {
        return Collections.unmodifiableList(new ArrayList<>(records));
    }
}
