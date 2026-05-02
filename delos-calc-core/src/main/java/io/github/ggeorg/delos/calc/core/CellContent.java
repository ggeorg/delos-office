package io.github.ggeorg.delos.calc.core;

import java.util.Locale;
import java.util.Objects;
import java.util.OptionalDouble;

/**
 * Raw spreadsheet cell content as entered by the user.
 *
 * <p>Formula evaluation is intentionally not implemented in H14. Formula cells
 * are stored as formulas so a later formula engine can evaluate them without
 * changing the workbook model.</p>
 */
public record CellContent(Type type, String text) {
    public enum Type {
        BLANK,
        TEXT,
        NUMBER,
        BOOLEAN,
        FORMULA
    }

    public CellContent {
        type = Objects.requireNonNull(type, "type");
        text = Objects.requireNonNullElse(text, "");
    }

    public static CellContent blank() {
        return new CellContent(Type.BLANK, "");
    }

    public static CellContent text(String value) {
        return new CellContent(Type.TEXT, Objects.requireNonNullElse(value, ""));
    }

    public static CellContent number(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Cell number must be finite");
        }
        return new CellContent(Type.NUMBER, Double.toString(value));
    }

    public static CellContent bool(boolean value) {
        return new CellContent(Type.BOOLEAN, Boolean.toString(value));
    }

    public static CellContent formula(String expression) {
        String normalized = Objects.requireNonNullElse(expression, "").trim();
        if (normalized.startsWith("=")) {
            normalized = normalized.substring(1).trim();
        }
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Formula expression must not be blank");
        }
        return new CellContent(Type.FORMULA, normalized);
    }

    public static CellContent parseInput(String input) {
        String raw = Objects.requireNonNullElse(input, "");
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return blank();
        }
        if (trimmed.startsWith("=") && trimmed.length() > 1) {
            return formula(trimmed);
        }
        if ("true".equalsIgnoreCase(trimmed) || "false".equalsIgnoreCase(trimmed)) {
            return bool(Boolean.parseBoolean(trimmed.toLowerCase(Locale.ROOT)));
        }
        try {
            return number(Double.parseDouble(trimmed));
        } catch (NumberFormatException ignored) {
            return text(raw);
        }
    }

    public boolean isBlank() {
        return type == Type.BLANK;
    }

    public OptionalDouble numberValue() {
        if (type != Type.NUMBER) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(Double.parseDouble(text));
    }

    public boolean booleanValue() {
        if (type != Type.BOOLEAN) {
            throw new IllegalStateException("Cell content is not boolean: " + type);
        }
        return Boolean.parseBoolean(text);
    }

    public String displayText() {
        return switch (type) {
            case BLANK -> "";
            case FORMULA -> "=" + text;
            default -> text;
        };
    }
}
