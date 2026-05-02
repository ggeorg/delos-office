package io.github.ggeorg.delos.writer.pdf;

/**
 * Normalizes layout-hostile Unicode spacing while preserving visible text.
 *
 * <p>This is deliberately not a typography transliterator. Smart quotes,
 * em-dashes, ellipses, Greek letters, and other visible Unicode characters are
 * left intact so the PDF backend can render them through an embedded Unicode
 * font when Standard 14 fonts cannot encode them. The only changes are for
 * compatibility spaces and invisible formatting characters that should not
 * decide whether export succeeds.</p>
 */
final class PdfTextSanitizer {
    private PdfTextSanitizer() {
    }

    static String sanitize(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder(text.length());
        text.codePoints().forEach(codePoint -> appendSanitized(builder, codePoint));
        return builder.toString();
    }

    private static void appendSanitized(StringBuilder builder, int codePoint) {
        switch (codePoint) {
            case 0x00A0, // no-break space
                 0x1680, // ogham space mark
                 0x2000, // en quad
                 0x2001, // em quad
                 0x2002, // en space
                 0x2003, // em space
                 0x2004, // three-per-em space
                 0x2005, // four-per-em space
                 0x2006, // six-per-em space
                 0x2007, // figure space
                 0x2008, // punctuation space
                 0x2009, // thin space
                 0x200A, // hair space
                 0x202F, // narrow no-break space
                 0x205F, // medium mathematical space
                 0x3000 -> builder.append(' ');
            case 0x00AD, // soft hyphen
                 0x200B, // zero-width space
                 0xFEFF -> {
                // Drop invisible formatting characters.
            }
            default -> builder.appendCodePoint(codePoint);
        }
    }
}
