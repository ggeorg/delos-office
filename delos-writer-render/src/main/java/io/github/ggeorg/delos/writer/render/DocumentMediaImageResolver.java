package io.github.ggeorg.delos.writer.render;

import io.github.ggeorg.delos.render.RenderImage;
import io.github.ggeorg.delos.render.RenderImageResolver;
import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.document.DocumentMediaItem;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Resolves Writer document media items for platform-neutral page rendering. */
public final class DocumentMediaImageResolver implements RenderImageResolver {
    private final Map<String, RenderImage> imagesBySource;

    private DocumentMediaImageResolver(Map<String, RenderImage> imagesBySource) {
        this.imagesBySource = Map.copyOf(imagesBySource);
    }

    public static DocumentMediaImageResolver from(Document document) {
        Objects.requireNonNull(document, "document");
        Map<String, RenderImage> images = new HashMap<>();
        for (DocumentMediaItem item : document.mediaItems()) {
            if (item.mediaType().startsWith("image/")) {
                images.put(item.path(), new RenderImage(item.path(), item.mediaType(), item.bytes()));
            }
        }
        return new DocumentMediaImageResolver(images);
    }

    @Override
    public Optional<RenderImage> resolve(String source) {
        String normalized = Objects.requireNonNullElse(source, "").trim().replace('\\', '/');
        if (normalized.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(imagesBySource.get(normalized));
    }
}
