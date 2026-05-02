package io.github.ggeorg.delos.writer.pdf;

import io.github.ggeorg.delos.render.RenderFont;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfFontRegistryContractTest {
    @Test
    void resolvesApplicationFamiliesToCanonicalPdfFamilies() {
        PdfFontRegistry registry = new PdfFontRegistry(List.of());

        assertEquals(new RenderFont("Helvetica", 12.0, false, false), registry.resolve(new RenderFont("System", 12.0, false, false)));
        assertEquals(new RenderFont("Times", 12.0, true, false), registry.resolve(new RenderFont("serif", 12.0, true, false)));
        assertEquals(new RenderFont("Courier", 12.0, false, true), registry.resolve(new RenderFont("Menlo", 12.0, false, true)));
    }

    @Test
    void standardFontRegistryStillWorksWithoutDocumentContext() {
        PdfFontRegistry registry = new PdfFontRegistry(List.of());

        assertNotNull(registry.fontFor(new RenderFont("Helvetica", 12.0, false, false)));
        assertNotNull(registry.fontFor(new RenderFont("Helvetica", 12.0, true, true)));
    }

    @Test
    void mapsSansSerifFamiliesToHelvetica() throws IOException {
        PdfFontRegistry registry = new PdfFontRegistry(List.of());

        assertEquals("Helvetica", registry.fontFor(new RenderFont("Arial", 12.0, false, false)).getName());
        assertEquals("Helvetica-BoldOblique", registry.fontFor(new RenderFont("sans-serif", 12.0, true, true)).getName());
    }

    @Test
    void mapsSerifFamiliesToTimes() throws IOException {
        PdfFontRegistry registry = new PdfFontRegistry(List.of());

        assertEquals("Times-Roman", registry.fontFor(new RenderFont("Times New Roman", 12.0, false, false)).getName());
        assertEquals("Times-BoldItalic", registry.fontFor(new RenderFont("Georgia", 12.0, true, true)).getName());
    }

    @Test
    void mapsMonospaceFamiliesToCourier() throws IOException {
        PdfFontRegistry registry = new PdfFontRegistry(List.of());

        assertEquals("Courier", registry.fontFor(new RenderFont("Courier New", 12.0, false, false)).getName());
        assertEquals("Courier-BoldOblique", registry.fontFor(new RenderFont("monospace", 12.0, true, true)).getName());
    }

    @Test
    void fallsBackToHelveticaForUnknownStandardFamilies() throws IOException {
        PdfFontRegistry registry = new PdfFontRegistry(List.of());

        assertEquals("Helvetica", registry.fontFor(new RenderFont("Some Missing Font", 12.0, false, false)).getName());
    }

    @Test
    void unicodeFontLocatorIgnoresMissingCandidates() {
        assertNull(PdfUnicodeFontLocator.firstExisting(List.of(Path.of("/definitely/not/a/font.ttf"))));
    }

    
    @Test
    void styledUnicodeFontCandidatesPreferStyledFilesAndKeepRegularFallbacks() {
        List<Path> regular = PdfUnicodeFontLocator.defaultCandidates();
        List<Path> boldItalic = PdfUnicodeFontLocator.defaultCandidates(true, true);

        assertTrue(boldItalic.size() > regular.size());
        assertTrue(boldItalic.stream()
                .map(path -> path.getFileName().toString().toLowerCase())
                .anyMatch(name -> name.contains("bold") || name.contains("bd") || name.contains("bi")));
        assertTrue(boldItalic.containsAll(regular));
    }
}
