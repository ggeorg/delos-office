package io.github.ggeorg.delos.writer.document;

import java.util.Objects;

/**
 * Server-safe page setup model for report/PDF production.
 *
 * <p>This is intentionally independent of JavaFX printer APIs. It gives Delos
 * Reports and future server code a clear input model for paper size,
 * orientation, and margins, then lowers to the existing immutable
 * {@link PageStyle} used by layout and PDF export.</p>
 */
public record PageSetup(
        PageSize size,
        PageOrientation orientation,
        PageMargins margins
) {
    public PageSetup {
        size = Objects.requireNonNull(size, "size");
        orientation = Objects.requireNonNull(orientation, "orientation");
        margins = Objects.requireNonNull(margins, "margins");
    }

    public static PageSetup a4Default() {
        return new PageSetup(PageSize.A4, PageOrientation.PORTRAIT, PageMargins.writerDefault());
    }

    public static PageSetup letterDefault() {
        return new PageSetup(PageSize.LETTER, PageOrientation.PORTRAIT, PageMargins.oneInch());
    }

    public PageStyle toPageStyle() {
        return PageStyle.of(size, orientation, margins);
    }

    public PageSetup withSize(PageSize size) {
        return new PageSetup(size, orientation, margins);
    }

    public PageSetup withOrientation(PageOrientation orientation) {
        return new PageSetup(size, orientation, margins);
    }

    public PageSetup withMargins(PageMargins margins) {
        return new PageSetup(size, orientation, margins);
    }
}
