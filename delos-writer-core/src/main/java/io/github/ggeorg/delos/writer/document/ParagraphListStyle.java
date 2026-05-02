package io.github.ggeorg.delos.writer.document;

import java.util.Objects;

/**
 * Paragraph-owned list metadata.
 *
 * <p>Lists are modeled as paragraph properties instead of fake bullet text so
 * editing, export, and future numbering rules can stay honest.</p>
 */
public record ParagraphListStyle(
        ListMarkerKind kind,
        int level,
        int start
) {
    public ParagraphListStyle {
        kind = Objects.requireNonNullElse(kind, ListMarkerKind.NONE);
        if (level < 0) {
            throw new IllegalArgumentException("level must be >= 0");
        }
        if (start < 1) {
            throw new IllegalArgumentException("start must be >= 1");
        }
        if (kind == ListMarkerKind.NONE) {
            level = 0;
            start = 1;
        }
    }

    public static ParagraphListStyle none() {
        return new ParagraphListStyle(ListMarkerKind.NONE, 0, 1);
    }

    public static ParagraphListStyle bullet(int level) {
        return new ParagraphListStyle(ListMarkerKind.BULLET, level, 1);
    }

    public static ParagraphListStyle numbered(int level, int start) {
        return new ParagraphListStyle(ListMarkerKind.NUMBERED, level, start);
    }

    public boolean enabled() {
        return kind != ListMarkerKind.NONE;
    }
}
