package io.github.ggeorg.delos.writer.app.io;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class WriterFileChoosersContractTest {
    @Test
    void sharedHelpersKeepWriterSaveAndExportNamesConsistent() {
        assertEquals("Bad-Name", WriterFileChoosers.sanitizeFileName("Bad:Name"));
        assertEquals("Untitled", WriterFileChoosers.sanitizeFileName("   "));
        assertEquals("report", WriterFileChoosers.suggestedBaseName(Path.of("/tmp/report.dlw"), "Ignored"));
        assertEquals("Untitled", WriterFileChoosers.sanitizeFileName(WriterFileChoosers.suggestedBaseName(null, null)));
        assertEquals("report", WriterFileChoosers.stripExtensionForDisplay("report.pdf", ".pdf"));
        assertEquals(Path.of("report.pdf"), WriterFileChoosers.normalizeExtension(Path.of("report"), ".pdf"));
        assertEquals(Path.of("report.PDF"), WriterFileChoosers.normalizeExtension(Path.of("report.PDF"), ".pdf"));
    }
}
