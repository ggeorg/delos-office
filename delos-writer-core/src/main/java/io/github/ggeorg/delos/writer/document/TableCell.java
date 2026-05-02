package io.github.ggeorg.delos.writer.document;

import java.util.List;
import java.util.Objects;

/**
 * Table cell content container.
 *
 * <p>A cell owns a {@link Story}, not a special table-only text model. This lets
 * table cells reuse the same paragraph, list, image, formula, and future link
 * model as the document body while keeping the cell object passive and
 * serializable.</p>
 */
public record TableCell(Story content, TableCellStyle style) {
    public TableCell {
        content = Objects.requireNonNull(content, "content");
        style = Objects.requireNonNullElse(style, TableCellStyle.none());
    }

    public TableCell(Story content) {
        this(content, TableCellStyle.none());
    }

    /**
     * Compatibility constructor for the existing paragraph-based call sites.
     */
    public TableCell(List<Paragraph> paragraphs) {
        this(Story.ofParagraphs(paragraphs), TableCellStyle.none());
    }

    public List<Paragraph> paragraphs() {
        return content.paragraphs();
    }

    public TableCell withStyle(TableCellStyle style) {
        return new TableCell(content, style);
    }

    public TableCell withBackground(String color) {
        return withStyle(TableCellStyle.background(color));
    }

    public static TableCell blank() {
        return new TableCell(Story.blank());
    }
}
