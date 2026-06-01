package com.ekc.recorder.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class ChangeJournalWriter {

    public void write(Path outputFile, List<ChangeRecord> records) throws IOException {
        Path parent = outputFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Files.writeString(outputFile, toJson(records), StandardCharsets.UTF_8);
    }

    private String toJson(List<ChangeRecord> records) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\n  \"changes\": [\n");

        for (int i = 0; i < records.size(); i++) {
            ChangeRecord record = records.get(i);
            builder.append("    {")
                    .append("\"sequence\": ").append(record.sequence()).append(", ")
                    .append("\"fileReference\": \"").append(escape(record.fileReference())).append("\", ")
                    .append("\"detectedAt\": \"").append(escape(String.valueOf(record.detectedAt()))).append("\", ")
                    .append("\"changes\": [");

            List<XmlChange> changes = record.changes();
            for (int j = 0; j < changes.size(); j++) {
                XmlChange change = changes.get(j);
                builder.append("{")
                        .append("\"xpath\": \"").append(escape(change.xpath())).append("\", ")
                        .append("\"type\": \"").append(change.type().name()).append("\", ")
                        .append("\"beforeFragment\": \"").append(escape(change.beforeFragment())).append("\", ")
                        .append("\"afterFragment\": \"").append(escape(change.afterFragment())).append("\"")
                        .append("}");
                if (j < changes.size() - 1) {
                    builder.append(',');
                }
            }

            builder.append("]}");
            if (i < records.size() - 1) {
                builder.append(',');
            }
            builder.append('\n');
        }

        builder.append("  ]\n}");
        return builder.toString();
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
