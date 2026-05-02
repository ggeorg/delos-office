package io.github.ggeorg.delos.writer.layout;

import io.github.ggeorg.delos.writer.document.Block;
import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.document.PageStyle;
import io.github.ggeorg.delos.writer.document.Paragraph;

import java.util.List;

/**
 * Inputs that decide whether the incremental layout cache can be reused.
 *
 * <p>The document title is intentionally excluded: title metadata is not rendered
 * into the page flow, so retitling a document must not invalidate pagination.</p>
 */
record LayoutInputs(
    PageStyle pageStyle,
    List<Block> blocks,
    List<Paragraph> paragraphs,
    FontDescriptor bodyFont,
    double contentWidth,
    double bodyLineGap,
    int paragraphCount
) {
    static LayoutInputs from(Document document, LayoutTheme theme) {
        return new LayoutInputs(
            document.pageStyle(),
            document.blocks(),
            document.paragraphs(),
            FontDescriptor.from(theme.bodyFont()),
            document.pageStyle().contentWidth(),
            theme.bodyLineGap(),
            document.paragraphs().size()
        );
    }

    boolean compatibleWith(LayoutInputs other) {
        return pageStyle.equals(other.pageStyle)
            && bodyFont.equals(other.bodyFont)
            && Double.compare(contentWidth, other.contentWidth) == 0
            && Double.compare(bodyLineGap, other.bodyLineGap) == 0;
    }
}
