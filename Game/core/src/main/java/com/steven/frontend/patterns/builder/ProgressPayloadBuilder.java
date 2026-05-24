package com.steven.frontend.patterns.builder;

// Menyusun payload progress secara bertahap agar lebih rapi dan mudah dirawat.
public class ProgressPayloadBuilder {
    private final StringBuilder sb = new StringBuilder();
    private boolean first = true;

    public ProgressPayloadBuilder start() {
        sb.setLength(0);
        sb.append('{');
        first = true;
        return this;
    }

    public ProgressPayloadBuilder field(String key, int value) {
        return fieldRaw(key, Integer.toString(value));
    }

    public ProgressPayloadBuilder field(String key, float value) {
        return fieldRaw(key, Float.toString(value));
    }

    public ProgressPayloadBuilder field(String key, boolean value) {
        return fieldRaw(key, Boolean.toString(value));
    }

    public ProgressPayloadBuilder fieldString(String key, String value) {
        String safe = value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
        return fieldRaw(key, '"' + safe + '"');
    }

    private ProgressPayloadBuilder fieldRaw(String key, String rawValue) {
        if (!first) {
            sb.append(',');
        }
        sb.append('"').append(key).append('"').append(':').append(rawValue);
        first = false;
        return this;
    }

    public String build() {
        return sb.append('}').toString();
    }
}
