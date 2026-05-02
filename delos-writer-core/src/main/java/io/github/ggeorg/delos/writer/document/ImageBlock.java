package io.github.ggeorg.delos.writer.document;

import java.util.Objects;

/**
 * Block-level image stored as a reference to a document media asset.
 *
 * <p>For native {@code .dlw} files, {@code source} is a package-relative media
 * path such as {@code media/image-1.png}. Images are block content only: no
 * floating, no wrapping, no resize handles.</p>
 */
public record ImageBlock(String source, double width, double height, String altText) implements Block {
    public ImageBlock(String source, double width, double height) {
        this(source, width, height, "");
    }

    public ImageBlock {
        source = Objects.requireNonNullElse(source, "").trim().replace('\\', '/');
        altText = Objects.requireNonNullElse(altText, "").trim();
        if (width < 0) {
            throw new IllegalArgumentException("width must be >= 0");
        }
        if (height < 0) {
            throw new IllegalArgumentException("height must be >= 0");
        }
    }

    @Override
    public BlockKind kind() {
        return BlockKind.IMAGE;
    }
}
