package io.github.ggeorg.delos.render;

import java.util.Optional;

/** Resolves document-local image sources to renderer-neutral binary image assets. */
@FunctionalInterface
public interface RenderImageResolver {
    Optional<RenderImage> resolve(String source);

    static RenderImageResolver empty() {
        return source -> Optional.empty();
    }
}
