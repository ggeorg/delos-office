package io.github.ggeorg.delos.writer.pdf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Locates ordinary TrueType/OpenType fonts that PDFBox can embed for Unicode text.
 *
 * <p>This is intentionally conservative. PDF export starts with PDFBox Standard
 * 14 fonts for simple Latin text, then falls back to an installed Unicode font
 * when the text cannot be encoded by the Standard 14 font. No font files are
 * bundled by Delos.</p>
 */
final class PdfUnicodeFontLocator {
    private PdfUnicodeFontLocator() {
    }

    static List<Path> defaultCandidates() {
        return defaultCandidates(false, false);
    }

    static List<Path> defaultCandidates(boolean bold, boolean italic) {
        List<Path> candidates = new ArrayList<>();
        addMacCandidates(candidates, bold, italic);
        addLinuxCandidates(candidates, bold, italic);
        addWindowsCandidates(candidates, bold, italic);
        addDiscoveredCandidates(candidates, bold, italic);
        if (bold || italic) {
            addMacCandidates(candidates, false, false);
            addLinuxCandidates(candidates, false, false);
            addWindowsCandidates(candidates, false, false);
            addDiscoveredCandidates(candidates, false, false);
        }
        return List.copyOf(new LinkedHashSet<>(candidates));
    }

    static Path firstExisting(List<Path> candidates) {
        if (candidates == null) {
            return null;
        }
        for (Path candidate : candidates) {
            if (candidate != null && Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static void addMacCandidates(List<Path> candidates, boolean bold, boolean italic) {
        if (bold && italic) {
            candidates.add(Path.of("/System/Library/Fonts/Supplemental/Arial Bold Italic.ttf"));
            candidates.add(Path.of("/Library/Fonts/Arial Bold Italic.ttf"));
            candidates.add(Path.of("/System/Library/Fonts/Supplemental/Times New Roman Bold Italic.ttf"));
            candidates.add(Path.of("/Library/Fonts/Times New Roman Bold Italic.ttf"));
            return;
        }
        if (bold) {
            candidates.add(Path.of("/System/Library/Fonts/Supplemental/Arial Bold.ttf"));
            candidates.add(Path.of("/Library/Fonts/Arial Bold.ttf"));
            candidates.add(Path.of("/System/Library/Fonts/Supplemental/Times New Roman Bold.ttf"));
            candidates.add(Path.of("/Library/Fonts/Times New Roman Bold.ttf"));
            return;
        }
        if (italic) {
            candidates.add(Path.of("/System/Library/Fonts/Supplemental/Arial Italic.ttf"));
            candidates.add(Path.of("/Library/Fonts/Arial Italic.ttf"));
            candidates.add(Path.of("/System/Library/Fonts/Supplemental/Times New Roman Italic.ttf"));
            candidates.add(Path.of("/Library/Fonts/Times New Roman Italic.ttf"));
            return;
        }
        candidates.add(Path.of("/System/Library/Fonts/Supplemental/Arial Unicode.ttf"));
        candidates.add(Path.of("/System/Library/Fonts/Supplemental/Arial Unicode MS.ttf"));
        candidates.add(Path.of("/Library/Fonts/Arial Unicode.ttf"));
        candidates.add(Path.of("/Library/Fonts/Arial Unicode MS.ttf"));
        candidates.add(Path.of("/System/Library/Fonts/Supplemental/Arial.ttf"));
        candidates.add(Path.of("/Library/Fonts/Arial.ttf"));
        candidates.add(Path.of("/System/Library/Fonts/Supplemental/Times New Roman.ttf"));
        candidates.add(Path.of("/Library/Fonts/Times New Roman.ttf"));
    }

    private static void addLinuxCandidates(List<Path> candidates, boolean bold, boolean italic) {
        String dejaVuSuffix = styledSuffix(bold, italic, "", "-Bold", "-Oblique", "-BoldOblique");
        String notoSuffix = styledSuffix(bold, italic, "-Regular", "-Bold", "-Italic", "-BoldItalic");
        String liberationSuffix = styledSuffix(bold, italic, "-Regular", "-Bold", "-Italic", "-BoldItalic");

        candidates.add(Path.of("/usr/share/fonts/truetype/dejavu/DejaVuSans" + dejaVuSuffix + ".ttf"));
        candidates.add(Path.of("/usr/share/fonts/dejavu/DejaVuSans" + dejaVuSuffix + ".ttf"));
        candidates.add(Path.of("/usr/share/fonts/dejavu-sans-fonts/DejaVuSans" + dejaVuSuffix + ".ttf"));
        candidates.add(Path.of("/usr/share/fonts/google-noto/NotoSans" + notoSuffix + ".ttf"));
        candidates.add(Path.of("/usr/share/fonts/noto/NotoSans" + notoSuffix + ".ttf"));
        candidates.add(Path.of("/usr/share/fonts/liberation/LiberationSans" + liberationSuffix + ".ttf"));
        candidates.add(Path.of("/usr/share/fonts/liberation-sans/LiberationSans" + liberationSuffix + ".ttf"));
    }

    private static void addWindowsCandidates(List<Path> candidates, boolean bold, boolean italic) {
        String windowsDirectory = System.getenv("WINDIR");
        if (windowsDirectory == null || windowsDirectory.isBlank()) {
            windowsDirectory = "C:\\Windows";
        }
        Path fonts = Path.of(windowsDirectory, "Fonts");
        if (bold && italic) {
            candidates.add(fonts.resolve("arialbi.ttf"));
            candidates.add(fonts.resolve("segoeuiz.ttf"));
            candidates.add(fonts.resolve("timesbi.ttf"));
            return;
        }
        if (bold) {
            candidates.add(fonts.resolve("arialbd.ttf"));
            candidates.add(fonts.resolve("segoeuib.ttf"));
            candidates.add(fonts.resolve("timesbd.ttf"));
            return;
        }
        if (italic) {
            candidates.add(fonts.resolve("ariali.ttf"));
            candidates.add(fonts.resolve("segoeuii.ttf"));
            candidates.add(fonts.resolve("timesi.ttf"));
            return;
        }
        candidates.add(fonts.resolve("arial.ttf"));
        candidates.add(fonts.resolve("segoeui.ttf"));
        candidates.add(fonts.resolve("times.ttf"));
    }

    private static void addDiscoveredCandidates(List<Path> candidates, boolean bold, boolean italic) {
        Set<Path> roots = fontRoots();
        for (Path root : roots) {
            if (!Files.isDirectory(root)) {
                continue;
            }
            try (Stream<Path> stream = Files.walk(root, 3)) {
                stream
                        .filter(Files::isRegularFile)
                        .filter(PdfUnicodeFontLocator::isEmbeddableFontFile)
                        .filter(path -> isPreferredFontName(path.getFileName().toString(), bold, italic))
                        .sorted()
                        .forEach(candidates::add);
            } catch (IOException | SecurityException ignored) {
                // Font discovery is best effort. Explicit candidates above remain authoritative.
            }
        }
    }

    private static Set<Path> fontRoots() {
        Set<Path> roots = new LinkedHashSet<>();
        roots.add(Path.of("/System/Library/Fonts"));
        roots.add(Path.of("/System/Library/Fonts/Supplemental"));
        roots.add(Path.of("/Library/Fonts"));
        String home = System.getProperty("user.home");
        if (home != null && !home.isBlank()) {
            roots.add(Path.of(home, "Library", "Fonts"));
        }
        roots.add(Path.of("/usr/share/fonts"));
        roots.add(Path.of("/usr/local/share/fonts"));
        String windowsDirectory = System.getenv("WINDIR");
        if (windowsDirectory != null && !windowsDirectory.isBlank()) {
            roots.add(Path.of(windowsDirectory, "Fonts"));
        }
        return roots;
    }

    private static boolean isEmbeddableFontFile(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".ttf") || name.endsWith(".otf");
    }

    private static boolean isPreferredFontName(String fileName, boolean bold, boolean italic) {
        String name = fileName.toLowerCase(Locale.ROOT);
        if (!(name.contains("arial")
                || name.contains("noto")
                || name.contains("dejavu")
                || name.contains("liberation")
                || name.contains("segoe")
                || name.contains("times"))) {
            return false;
        }
        if (bold && !(name.contains("bold") || name.contains("bd"))) {
            return false;
        }
        if (italic && !(name.contains("italic") || name.contains("oblique") || name.contains("i."))) {
            return false;
        }
        return true;
    }

    private static String styledSuffix(boolean bold, boolean italic, String regular, String boldOnly, String italicOnly, String boldItalic) {
        if (bold && italic) {
            return boldItalic;
        }
        if (bold) {
            return boldOnly;
        }
        if (italic) {
            return italicOnly;
        }
        return regular;
    }

    static String describePlatform() {
        return System.getProperty("os.name", "unknown").toLowerCase(Locale.ROOT);
    }
}
