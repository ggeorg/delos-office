package io.github.ggeorg.delos.writer.pdf;

import io.github.ggeorg.delos.render.RenderTheme;
import io.github.ggeorg.delos.writer.layout.LayoutTheme;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class HeadlessOutputLayoutPolicyContractTest {
    @Test
    void defaultPolicyOwnsPdfFacingLayoutAndRenderFonts() {
        HeadlessOutputLayoutPolicy policy = HeadlessOutputLayoutPolicy.defaultPolicy();
        LayoutTheme layout = policy.layoutTheme();
        RenderTheme render = policy.renderTheme();

        assertEquals("Helvetica", layout.titleFont().family());
        assertEquals("Times", layout.bodyFont().family());
        assertEquals(layout.titleFont(), render.titleFont());
        assertEquals(layout.bodyFont(), render.bodyFont());
    }

    @Test
    void pdfServiceUsesExplicitHeadlessPolicy() throws IOException {
        String service = Files.readString(Path.of("src/main/java/io/github/ggeorg/delos/writer/pdf/WriterPdfService.java"));
        String engine = Files.readString(Path.of("src/main/java/io/github/ggeorg/delos/writer/pdf/PdfDocumentLayoutEngine.java"));

        assertTrue(service.contains("HeadlessOutputLayoutPolicy.defaultPolicy()"));
        assertTrue(service.contains("HeadlessOutputLayoutPolicy.fromExportOptions(options)"));
        assertTrue(service.contains("new PdfDocumentLayoutEngine(fonts, outputPolicy)"));
        assertTrue(engine.contains("outputPolicy.layoutThemeFor(theme)"));
    }
}
