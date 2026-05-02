package io.github.ggeorg.delos.writer.layout;

import io.github.ggeorg.delos.writer.document.PageStyle;

import java.util.List;
import java.util.Objects;

/**
 * Immutable layout result that can be rendered, printed, or hit-tested later.
 */
public record LaidOutDocument(PageStyle pageStyle, List<LaidOutPage> pages) {
    public LaidOutDocument {
        pageStyle = Objects.requireNonNull(pageStyle, "pageStyle");
        pages = List.copyOf(Objects.requireNonNull(pages, "pages"));
    }
}
