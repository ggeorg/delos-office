package io.github.ggeorg.delos.writer.print;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * Options for the PDF-backed Writer print pipeline.
 *
 * <p>Printing is intentionally PDF-first: Writer layout is rendered to a
 * frozen temporary PDF artifact, and that PDF is sent to the printer. The
 * editable {@code DelosEditor}, {@code PageView}, or JavaFX scene graph are not
 * printed directly.</p>
 */
public record PdfPrintOptions(
        String jobName,
        Path temporaryDirectory,
        boolean deleteTemporaryPdf,
        boolean showPrintDialog
) {
    private static final String DEFAULT_JOB_NAME = "Delos Writer Document";

    public PdfPrintOptions {
        jobName = normalizeJobName(jobName);
    }

    public static PdfPrintOptions defaultOptions() {
        return new PdfPrintOptions(DEFAULT_JOB_NAME, null, true, true);
    }

    /**
     * Silent printing is useful for tests or controlled batch workflows. The app
     * default should keep the native print dialog enabled.
     */
    public PdfPrintOptions withoutPrintDialog() {
        return new PdfPrintOptions(jobName, temporaryDirectory, deleteTemporaryPdf, false);
    }

    public PdfPrintOptions withJobName(String jobName) {
        return new PdfPrintOptions(jobName, temporaryDirectory, deleteTemporaryPdf, showPrintDialog);
    }

    public PdfPrintOptions withTemporaryDirectory(Path temporaryDirectory) {
        return new PdfPrintOptions(jobName, temporaryDirectory, deleteTemporaryPdf, showPrintDialog);
    }

    public PdfPrintOptions keepingTemporaryPdf() {
        return new PdfPrintOptions(jobName, temporaryDirectory, false, showPrintDialog);
    }

    public Optional<Path> temporaryDirectoryOption() {
        return Optional.ofNullable(temporaryDirectory);
    }

    private static String normalizeJobName(String value) {
        String trimmed = Objects.toString(value, "").trim();
        return trimmed.isEmpty() ? DEFAULT_JOB_NAME : trimmed;
    }
}
