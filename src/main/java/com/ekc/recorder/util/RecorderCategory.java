package com.ekc.recorder.util;

public enum RecorderCategory {
    TEAM("team", "competition.xsd"),
    INDIVIDUAL("individual", "individuals.xsd");

    private final String propertyValue;
    private final String schemaResource;

    RecorderCategory(String propertyValue, String schemaResource) {
        this.propertyValue = propertyValue;
        this.schemaResource = schemaResource;
    }

    public String schemaResource() {
        return schemaResource;
    }

    public static RecorderCategory fromProperty(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("La propriété 'category' est manquante ou vide.");
        }

        String normalized = value.trim().toLowerCase(java.util.Locale.ROOT);
        for (RecorderCategory category : values()) {
            if (category.propertyValue.equals(normalized)) {
                return category;
            }
        }

        throw new IllegalArgumentException("La propriété 'category' doit valoir 'team' ou 'individual' : " + value);
    }
}

