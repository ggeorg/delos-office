package io.github.ggeorg.delos.writer.pdf;

import java.io.IOException;

/**
 * Unchecked wrapper used because {@code RenderTarget} methods do not throw
 * checked {@link IOException}s, while PDFBox drawing methods do.
 */
public final class PdfRenderException extends RuntimeException {
    public PdfRenderException(IOException cause) {
        super(cause);
    }

    @Override
    public synchronized IOException getCause() {
        return (IOException) super.getCause();
    }
}
