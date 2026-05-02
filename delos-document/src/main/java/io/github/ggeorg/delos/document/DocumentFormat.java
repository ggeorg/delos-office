package io.github.ggeorg.delos.document;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Reader/writer contract for one native Delos document family.
 */
public interface DocumentFormat<T> {
    DocumentType type();

    T createBlank(String title);

    T read(InputStream inputStream) throws IOException;

    void write(T document, OutputStream outputStream) throws IOException;
}
