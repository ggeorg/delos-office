package io.github.ggeorg.delos.writer.ui.control;

import io.github.ggeorg.delos.render.RenderFont;
import io.github.ggeorg.delos.writer.layout.LayoutTheme;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WriterDesktopPreviewFactoryContractTest {
    @Test
    void factoryOwnsJavaFxPreviewInternalsBehindExportedControlBoundary() throws IOException {
        String factory = Files.readString(Path.of("src/main/java/io/github/ggeorg/delos/writer/ui/control/WriterDesktopPreviewFactory.java"));
        String policy = Files.readString(Path.of("src/main/java/io/github/ggeorg/delos/writer/ui/control/DesktopPreviewLayoutPolicy.java"));
        String moduleInfo = Files.readString(Path.of("src/main/java/module-info.java"));

        assertTrue(moduleInfo.contains("exports io.github.ggeorg.delos.writer.ui.control;"));
        assertTrue(factory.contains("DesktopPreviewLayoutPolicy"));
        assertTrue(policy.contains("new JavaFxRenderTextMeasurer()"));
        assertTrue(policy.contains("new PaginatingDocumentLayoutEngine"));
        assertTrue(policy.contains("new KnuthPlassParagraphLayouter(textMeasurer)"));
        assertTrue(policy.contains("new WriterDocumentView"));
    }

    @Test
    void desktopPreviewPolicyIsSeparateFromPdfOutputPolicy() throws IOException {
        String policy = Files.readString(Path.of("src/main/java/io/github/ggeorg/delos/writer/ui/control/DesktopPreviewLayoutPolicy.java"));

        assertTrue(policy.contains("DesktopPreviewLayoutPolicy"));
        assertTrue(policy.contains("JavaFX-facing"));
        assertTrue(policy.contains("desktopPreviewFont"));
    }

    @Test
    void defaultDesktopPreviewKeepsJavaFxLogicalSerifInsteadOfPdfTimesName() {
        DesktopPreviewLayoutPolicy policy = DesktopPreviewLayoutPolicy.defaultPolicy();

        assertEquals("System", policy.layoutTheme().titleFont().family());
        assertEquals("Serif", policy.layoutTheme().bodyFont().family());
    }

    @Test
    void desktopPreviewConvertsOnlyPdfStandardNamesBackToJavaFxSafeLogicalFamilies() {
        LayoutTheme pdfCanonicalTheme = new LayoutTheme(
                new RenderFont("Helvetica", 24.0, false, false),
                new RenderFont("Times", 13.5, false, false),
                12.0,
                5.0,
                10.0,
                8.0,
                5.5
        );

        DesktopPreviewLayoutPolicy policy = DesktopPreviewLayoutPolicy.fromLayoutTheme(pdfCanonicalTheme);

        assertEquals("Helvetica", policy.layoutTheme().titleFont().family());
        assertEquals("Serif", policy.layoutTheme().bodyFont().family());
    }
}
