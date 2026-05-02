package io.github.ggeorg.delos.writer.pdf;

import io.github.ggeorg.delos.render.RenderFont;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * PDF font registry for the Writer PDF backend.
 *
 * <p>The first path remains deliberately boring: use PDFBox Standard 14 fonts
 * for simple Latin text. Common document font families are mapped to the
 * closest Standard 14 family instead of silently forcing every font to
 * Helvetica. When text cannot be encoded by those fonts, the registry falls
 * back to an installed TrueType/OpenType font and embeds/subsets it via
 * {@code PDType0Font}. This keeps Greek and other Unicode text exportable
 * without bundling font files in Delos.</p>
 */
public final class PdfFontRegistry implements PdfFontResolver {
    private final PDDocument document;
    private final List<Path> unicodeFontCandidates;
    private final boolean useDefaultStyledUnicodeCandidates;
    private final Map<FontKey, PDFont> standardFonts = new HashMap<>();
    private final Map<UnicodeFontKey, PDFont> unicodeFonts = new HashMap<>();

    public PdfFontRegistry() {
        this(null, PdfUnicodeFontLocator.defaultCandidates(), true);
    }

    public PdfFontRegistry(List<Path> unicodeFontCandidates) {
        this(null, unicodeFontCandidates, false);
    }

    private PdfFontRegistry(PDDocument document, List<Path> unicodeFontCandidates, boolean useDefaultStyledUnicodeCandidates) {
        this.document = document;
        this.unicodeFontCandidates = List.copyOf(Objects.requireNonNull(unicodeFontCandidates, "unicodeFontCandidates"));
        this.useDefaultStyledUnicodeCandidates = useDefaultStyledUnicodeCandidates;
    }

    PdfFontRegistry forDocument(PDDocument document) {
        return new PdfFontRegistry(
                Objects.requireNonNull(document, "document"),
                unicodeFontCandidates,
                useDefaultStyledUnicodeCandidates
        );
    }

    @Override
    public RenderFont resolve(RenderFont font) {
        return PdfStandardFontMapper.resolve(font);
    }

    @Override
    public PDFont fontFor(RenderFont font) {
        return standardFontFor(FontKey.from(resolve(font)));
    }

    @Override
    public PDFont fontFor(RenderFont font, String text) {
        FontKey key = FontKey.from(resolve(font));
        String safeText = text == null ? "" : text;
        PDFont standard = standardFontFor(key);
        if (canEncode(standard, safeText)) {
            return standard;
        }

        PDFont unicode = unicodeFontFor(key, safeText);
        if (unicode != null) {
            return unicode;
        }

        throw cannotEncode(safeText);
    }

    private PDFont standardFontFor(FontKey key) {
        return standardFonts.computeIfAbsent(key, PdfFontRegistry::createStandardFont);
    }

    private PDFont unicodeFontFor(FontKey key, String text) {
        if (document == null) {
            return null;
        }
        for (Path path : unicodeFontCandidatesFor(key)) {
            if (path == null || !Files.isRegularFile(path)) {
                continue;
            }
            try {
                PDFont font = unicodeFonts.computeIfAbsent(new UnicodeFontKey(key, path), candidate -> loadUnicodeFont(candidate.path()));
                if (canEncode(font, text)) {
                    return font;
                }
            } catch (PdfRenderException ignored) {
                // Keep searching. A system font may exist but be unsupported by PDFBox.
            }
        }
        return null;
    }

    private List<Path> unicodeFontCandidatesFor(FontKey key) {
        if (useDefaultStyledUnicodeCandidates) {
            return PdfUnicodeFontLocator.defaultCandidates(key.bold(), key.italic());
        }
        return unicodeFontCandidates;
    }

    private PDFont loadUnicodeFont(Path path) {
        try {
            return PDType0Font.load(document, path.toFile());
        } catch (IOException | RuntimeException ex) {
            throw new PdfRenderException(new IOException("Could not load PDF Unicode fallback font: " + path, ex));
        }
    }

    private static PDFont createStandardFont(FontKey key) {
        return new PDType1Font(standardFontNameFor(key));
    }

    private static org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName standardFontNameFor(FontKey key) {
        return PdfStandardFontMapper.standardFontNameFor(new RenderFont(key.family(), 12.0, key.bold(), key.italic()));
    }

    private static boolean canEncode(PDFont font, String text) {
        if (text == null || text.isEmpty()) {
            return true;
        }
        try {
            font.encode(text);
            return true;
        } catch (IOException | IllegalArgumentException ex) {
            return false;
        }
    }

    private static PdfRenderException cannotEncode(String text) {
        String sample = text == null ? "" : text;
        if (sample.length() > 32) {
            sample = sample.substring(0, 32) + "…";
        }
        return new PdfRenderException(new IOException(
                "PDF font cannot encode text: '" + sample + "'. "
                        + "Install a Unicode TrueType/OpenType font such as DejaVu Sans, Noto Sans, or Arial. "
                        + "Platform: " + PdfUnicodeFontLocator.describePlatform()
        ));
    }

    private record FontKey(String family, boolean bold, boolean italic) {
        static FontKey from(RenderFont font) {
            RenderFont safeFont = Objects.requireNonNull(font, "font");
            return new FontKey(safeFont.family(), safeFont.bold(), safeFont.italic());
        }
    }

    private record UnicodeFontKey(FontKey fontKey, Path path) {
    }
}
