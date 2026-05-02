package io.github.ggeorg.delos.writer.pdf;

import io.github.ggeorg.delos.render.RenderFont;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.util.Locale;
import java.util.Objects;

/**
 * Boring PDF Standard-14 family mapping shared by PDF measurement and drawing.
 */
final class PdfStandardFontMapper {
    private PdfStandardFontMapper() {
    }

    static RenderFont resolve(RenderFont font) {
        RenderFont safeFont = Objects.requireNonNull(font, "font");
        return new RenderFont(
                standardFamilyName(safeFont.family()),
                safeFont.size(),
                safeFont.bold(),
                safeFont.italic()
        );
    }

    static Standard14Fonts.FontName standardFontNameFor(RenderFont font) {
        RenderFont resolved = resolve(font);
        return switch (StandardFamily.fromResolvedName(resolved.family())) {
            case TIMES -> timesFont(resolved.bold(), resolved.italic());
            case COURIER -> courierFont(resolved.bold(), resolved.italic());
            case HELVETICA -> helveticaFont(resolved.bold(), resolved.italic());
        };
    }

    private static String standardFamilyName(String family) {
        return switch (StandardFamily.from(family)) {
            case TIMES -> "Times";
            case COURIER -> "Courier";
            case HELVETICA -> "Helvetica";
        };
    }

    private static Standard14Fonts.FontName helveticaFont(boolean bold, boolean italic) {
        if (bold && italic) {
            return Standard14Fonts.FontName.HELVETICA_BOLD_OBLIQUE;
        }
        if (bold) {
            return Standard14Fonts.FontName.HELVETICA_BOLD;
        }
        if (italic) {
            return Standard14Fonts.FontName.HELVETICA_OBLIQUE;
        }
        return Standard14Fonts.FontName.HELVETICA;
    }

    private static Standard14Fonts.FontName timesFont(boolean bold, boolean italic) {
        if (bold && italic) {
            return Standard14Fonts.FontName.TIMES_BOLD_ITALIC;
        }
        if (bold) {
            return Standard14Fonts.FontName.TIMES_BOLD;
        }
        if (italic) {
            return Standard14Fonts.FontName.TIMES_ITALIC;
        }
        return Standard14Fonts.FontName.TIMES_ROMAN;
    }

    private static Standard14Fonts.FontName courierFont(boolean bold, boolean italic) {
        if (bold && italic) {
            return Standard14Fonts.FontName.COURIER_BOLD_OBLIQUE;
        }
        if (bold) {
            return Standard14Fonts.FontName.COURIER_BOLD;
        }
        if (italic) {
            return Standard14Fonts.FontName.COURIER_OBLIQUE;
        }
        return Standard14Fonts.FontName.COURIER;
    }

    private enum StandardFamily {
        HELVETICA,
        TIMES,
        COURIER;

        static StandardFamily from(String family) {
            String normalized = normalize(family);
            if (normalized.isEmpty()) {
                return HELVETICA;
            }
            if (isTimesFamily(normalized)) {
                return TIMES;
            }
            if (isCourierFamily(normalized)) {
                return COURIER;
            }
            return HELVETICA;
        }

        static StandardFamily fromResolvedName(String family) {
            String normalized = normalize(family);
            if (normalized.equals("times")) {
                return TIMES;
            }
            if (normalized.equals("courier")) {
                return COURIER;
            }
            return HELVETICA;
        }

        private static boolean isTimesFamily(String family) {
            return family.equals("times")
                    || family.equals("timesroman")
                    || family.equals("timesnewroman")
                    || family.equals("georgia")
                    || family.equals("serif");
        }

        private static boolean isCourierFamily(String family) {
            return family.equals("courier")
                    || family.equals("couriernew")
                    || family.equals("monospace")
                    || family.equals("monospaced")
                    || family.equals("menlo")
                    || family.equals("monaco")
                    || family.equals("consolas");
        }

        private static String normalize(String family) {
            return family == null ? "" : family.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        }
    }
}
