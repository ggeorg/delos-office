package io.github.ggeorg.delos.writer.document;

import java.util.Locale;

/**
 * Canonical source language for a Delos Writer formula block.
 */
public enum FormulaSourceFormat {
    LATEX("latex");

    private final String xmlValue;

    FormulaSourceFormat(String xmlValue) {
        this.xmlValue = xmlValue;
    }

    public String xmlValue() {
        return xmlValue;
    }

    public static FormulaSourceFormat fromXmlValue(String value) {
        if (value == null || value.isBlank()) {
            return LATEX;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (FormulaSourceFormat format : values()) {
            if (format.xmlValue.equals(normalized) || format.name().toLowerCase(Locale.ROOT).equals(normalized)) {
                return format;
            }
        }
        throw new IllegalArgumentException("Unsupported formula source format: " + value);
    }
}
