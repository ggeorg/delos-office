package io.github.ggeorg.delos.writer.print;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Desktop print may use java.desktop, but it must remain PDF-first and must not
 * print JavaFX nodes or depend on the Writer editor scene graph.
 */
class PdfPrintBoundaryContractTest {
    private static final Path MAIN_SOURCES = Path.of("src/main/java");

    @Test
    void printModuleDoesNotImportJavaFxOrWriterUi() throws IOException {
        List<Path> offenders = javaFiles()
                .stream()
                .filter(PdfPrintBoundaryContractTest::importsForbiddenPackage)
                .toList();

        assertTrue(offenders.isEmpty(), () -> "Print module leaked into JavaFX/UI APIs: " + offenders);
    }

    @Test
    void printModuleKeepsPdfFirstBoundary() throws IOException {
        String printService = sourceWithoutComments(Path.of(
                "src/main/java/io/github/ggeorg/delos/writer/print/PdfWriterPrintService.java"));
        String documentPrinter = sourceWithoutComments(Path.of(
                "src/main/java/io/github/ggeorg/delos/writer/print/PdfBoxDocumentPrinter.java"));

        assertTrue(printService.contains("WriterPdfService"));
        assertTrue(printService.contains("pdfService.export(document, pdf)"));
        assertTrue(documentPrinter.contains("new PDFPageable(document)"));
        assertFalse(printService.contains("DelosEditor"));
        assertFalse(printService.contains("createLayoutSnapshot"));
    }

    private static List<Path> javaFiles() throws IOException {
        try (var paths = Files.walk(MAIN_SOURCES)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList();
        }
    }

    private static boolean importsForbiddenPackage(Path path) {
        String source = sourceWithoutComments(path);
        return source.contains("import javafx.")
                || source.contains("import io.github.ggeorg.delos.writer.ui.")
                || source.contains("import io.github.ggeorg.delos.writer.javafx.");
    }

    private static String sourceWithoutComments(Path path) {
        return stripComments(read(path));
    }

    private static String stripComments(String source) {
        StringBuilder result = new StringBuilder(source.length());
        boolean inString = false;
        boolean inChar = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;
        boolean escaped = false;

        for (int i = 0; i < source.length(); i++) {
            char current = source.charAt(i);
            char next = i + 1 < source.length() ? source.charAt(i + 1) : '\0';

            if (inLineComment) {
                if (current == '\n' || current == '\r') {
                    inLineComment = false;
                    result.append(current);
                }
                continue;
            }

            if (inBlockComment) {
                if (current == '*' && next == '/') {
                    inBlockComment = false;
                    i++;
                } else if (current == '\n' || current == '\r') {
                    result.append(current);
                }
                continue;
            }

            if (!inString && !inChar && current == '/' && next == '/') {
                inLineComment = true;
                i++;
                continue;
            }

            if (!inString && !inChar && current == '/' && next == '*') {
                inBlockComment = true;
                i++;
                continue;
            }

            result.append(current);

            if (escaped) {
                escaped = false;
            } else if (current == '\\') {
                escaped = inString || inChar;
            } else if (!inChar && current == '"') {
                inString = !inString;
            } else if (!inString && current == '\'') {
                inChar = !inChar;
            }
        }

        return result.toString();
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + path, e);
        }
    }
}
