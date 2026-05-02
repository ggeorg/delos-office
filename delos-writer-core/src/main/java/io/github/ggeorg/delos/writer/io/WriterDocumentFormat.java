package io.github.ggeorg.delos.writer.io;

import io.github.ggeorg.delos.document.DelosPackage;
import io.github.ggeorg.delos.document.DelosPackageNames;
import io.github.ggeorg.delos.document.DelosPackagePart;
import io.github.ggeorg.delos.document.DelosPackageReader;
import io.github.ggeorg.delos.document.DelosPackageWriter;
import io.github.ggeorg.delos.document.DocumentFormat;
import io.github.ggeorg.delos.document.DocumentType;
import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.document.DocumentMediaItem;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Native Writer document format used by the Delos suite shell.
 *
 * <p>The user-facing file is {@code .dlw}. Internally it is a generic Delos ZIP
 * package whose root MIME type identifies the concrete Writer document family.</p>
 */
public final class WriterDocumentFormat implements DocumentFormat<Document> {
    public static final DocumentType TYPE = new DocumentType(
            "writer",
            "Delos Writer Document",
            WriterDocumentExtensions.DOCUMENT,
            WriterDocumentMimeTypes.DOCUMENT
    );

    private static final byte[] ZIP_MAGIC = {'P', 'K', 3, 4};

    private final DocumentSerializer serializer;
    private final DelosPackageReader packageReader;
    private final DelosPackageWriter packageWriter;

    public WriterDocumentFormat() {
        this(new DocumentSerializer(), new DelosPackageReader(), new DelosPackageWriter());
    }

    public WriterDocumentFormat(DocumentSerializer serializer) {
        this(serializer, new DelosPackageReader(), new DelosPackageWriter());
    }

    WriterDocumentFormat(DocumentSerializer serializer, DelosPackageReader packageReader, DelosPackageWriter packageWriter) {
        this.serializer = Objects.requireNonNull(serializer, "serializer");
        this.packageReader = Objects.requireNonNull(packageReader, "packageReader");
        this.packageWriter = Objects.requireNonNull(packageWriter, "packageWriter");
    }

    @Override
    public DocumentType type() {
        return TYPE;
    }

    @Override
    public Document createBlank(String title) {
        Document blank = Document.blank();
        String normalized = title == null || title.isBlank() ? blank.title() : title.trim();
        return new Document(normalized, blank.pageStyle(), blank.paragraphs());
    }

    @Override
    public Document read(InputStream inputStream) throws IOException {
        Objects.requireNonNull(inputStream, "inputStream");
        try (inputStream) {
            byte[] bytes = inputStream.readAllBytes();
            if (!isZipPackage(bytes)) {
                return serializer.read(new ByteArrayInputStream(bytes));
            }

            DelosPackage delosPackage = packageReader.read(new ByteArrayInputStream(bytes));
            if (!TYPE.mediaType().equals(delosPackage.rootMediaType())) {
                throw new IOException("Unsupported Writer package media type: " + delosPackage.rootMediaType());
            }
            Document document = serializer.fromXml(delosPackage.requirePart(DelosPackageNames.CONTENT_XML).text());
            return document.withMediaItems(readMediaItems(delosPackage));
        }
    }

    @Override
    public void write(Document document, OutputStream outputStream) throws IOException {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(outputStream, "outputStream");

        List<DelosPackagePart> parts = new ArrayList<>();
        parts.add(DelosPackagePart.xml(DelosPackageNames.CONTENT_XML, serializer.toXml(document)));
        parts.add(DelosPackagePart.xml(DelosPackageNames.STYLES_XML, emptyStylesXml()));
        parts.add(DelosPackagePart.xml(DelosPackageNames.SETTINGS_XML, emptySettingsXml()));
        parts.add(DelosPackagePart.xml(DelosPackageNames.META_XML, metaXml(document)));
        parts.add(DelosPackagePart.directory(DelosPackageNames.MEDIA_DIR));
        for (DocumentMediaItem mediaItem : document.mediaItems()) {
            parts.add(DelosPackagePart.file(mediaItem.path(), mediaItem.mediaType(), mediaItem.bytes()));
        }

        DelosPackage delosPackage = new DelosPackage(TYPE.mediaType(), parts);
        packageWriter.write(delosPackage, outputStream);
    }

    private static List<DocumentMediaItem> readMediaItems(DelosPackage delosPackage) {
        List<DocumentMediaItem> mediaItems = new ArrayList<>();
        for (DelosPackagePart part : delosPackage.parts()) {
            if (part.directory()) {
                continue;
            }
            if (part.path().startsWith(DelosPackageNames.MEDIA_DIR)) {
                String mediaType = part.mediaType().isBlank()
                        ? DocumentMediaItem.guessMediaType(part.path())
                        : part.mediaType();
                mediaItems.add(new DocumentMediaItem(part.path(), mediaType, part.bytes()));
            }
        }
        return mediaItems;
    }

    private static boolean isZipPackage(byte[] bytes) {
        if (bytes.length < ZIP_MAGIC.length) {
            return false;
        }
        for (int index = 0; index < ZIP_MAGIC.length; index++) {
            if (bytes[index] != ZIP_MAGIC[index]) {
                return false;
            }
        }
        return true;
    }

    private static String emptyStylesXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<styles xmlns=\"urn:delos:styles:1\"/>\n";
    }

    private static String emptySettingsXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<settings xmlns=\"urn:delos:settings:1\"/>\n";
    }

    private static String metaXml(Document document) {
        String now = Instant.now().toString();
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<meta xmlns=\"urn:delos:meta:1\">\n"
                + "  <title>" + escapeText(document.title()) + "</title>\n"
                + "  <created-at>" + now + "</created-at>\n"
                + "  <modified-at>" + now + "</modified-at>\n"
                + "</meta>\n";
    }

    private static String escapeText(String value) {
        String text = value == null ? "" : value;
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
