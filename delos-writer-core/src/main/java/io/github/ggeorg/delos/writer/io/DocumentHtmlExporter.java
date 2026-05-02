package io.github.ggeorg.delos.writer.io;

import io.github.ggeorg.delos.writer.document.Alignment;
import io.github.ggeorg.delos.writer.document.Block;
import io.github.ggeorg.delos.writer.document.CharacterStyle;
import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.document.FormulaBlock;
import io.github.ggeorg.delos.writer.document.HorizontalRuleBlock;
import io.github.ggeorg.delos.writer.document.ImageBlock;
import io.github.ggeorg.delos.writer.document.ListMarkerKind;
import io.github.ggeorg.delos.writer.document.PageStyle;
import io.github.ggeorg.delos.writer.document.Paragraph;
import io.github.ggeorg.delos.writer.document.ParagraphBlock;
import io.github.ggeorg.delos.writer.document.ParagraphStyle;
import io.github.ggeorg.delos.writer.document.Story;
import io.github.ggeorg.delos.writer.document.TableBlock;
import io.github.ggeorg.delos.writer.document.TableCell;
import io.github.ggeorg.delos.writer.document.TableRow;
import io.github.ggeorg.delos.writer.document.TextRun;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Export a Delos document to a single self-contained HTML file.
 */
public final class DocumentHtmlExporter {

    public String toHtml(Document document) {
        Objects.requireNonNull(document, "document");
        return toHtml(document.title(), document.pageStyle(), document.body());
    }

    public String toHtml(String title, PageStyle pageStyle, Story story) {
        Objects.requireNonNull(pageStyle, "pageStyle");
        Objects.requireNonNull(story, "story");
        return toHtml(title, pageStyle, story.blocks());
    }

    public String toHtml(String title, PageStyle pageStyle, List<Block> blocks) {
        Objects.requireNonNull(pageStyle, "pageStyle");
        Objects.requireNonNull(blocks, "blocks");

        StringBuilder html = new StringBuilder(4096);

        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"en\">\n");
        html.append("<head>\n");
        html.append("  <meta charset=\"UTF-8\">\n");
        html.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n");
        html.append("  <meta name=\"generator\" content=\"Delos Writer\">\n");
        html.append("  <title>")
                .append(escapeHtml(title == null ? "Untitled" : title))
                .append("</title>\n");
        html.append("  <style>\n");
        html.append("    :root { color-scheme: light; }\n");
        html.append("    body { margin: 0; padding: 24px; background: #f3f4f6; color: #111827; font-family: system-ui, -apple-system, BlinkMacSystemFont, \"Segoe UI\", sans-serif; }\n");
        html.append("    .delos-document { box-sizing: border-box; width: ")
                .append(format(pageStyle.width()))
                .append("pt; min-height: ")
                .append(format(pageStyle.height()))
                .append("pt; margin: 0 auto; padding: ")
                .append(format(pageStyle.marginTop())).append("pt ")
                .append(format(pageStyle.marginRight())).append("pt ")
                .append(format(pageStyle.marginBottom())).append("pt ")
                .append(format(pageStyle.marginLeft())).append("pt; background: white; box-shadow: 0 10px 28px rgba(15, 23, 42, 0.12); }\n");
        html.append("    .delos-paragraph { margin: 0; white-space: pre-wrap; overflow-wrap: break-word; }\n");
        html.append("    .delos-list { margin: 0 0 8pt 0; padding-left: 22pt; }\n");
        html.append("    .delos-list-level-1 { padding-left: 40pt; }\n");
        html.append("    .delos-list-level-2 { padding-left: 58pt; }\n");
        html.append("    .delos-hr { border: 0; border-top: 1px solid #d1d5db; margin: 18pt 0; }\n");
        html.append("    .delos-figure { margin: 12pt auto; }\n");
        html.append("    .delos-image { display: block; max-width: 100%; height: auto; margin: 0 auto; }\n");
        html.append("    .delos-figcaption { margin-top: 6pt; color: #64748b; font-size: 9pt; text-align: center; }\n");
        html.append("    .delos-formula { display: flex; align-items: baseline; gap: 8pt; box-sizing: border-box; margin: 12pt 0; padding: 8pt 10pt; border: 1px solid #cbd5e1; border-radius: 6pt; background: #f8fafc; }\n");
        html.append("    .delos-formula-badge { color: #475569; font-weight: 700; }\n");
        html.append("    .delos-formula-source { white-space: pre-wrap; overflow-wrap: anywhere; font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace; }\n");
        html.append("    .delos-table { width: 100%; border-collapse: collapse; margin: 12pt 0; font-size: 10pt; }\n");
        html.append("    .delos-table th, .delos-table td { border: 1px solid #cbd5e1; padding: 6pt 8pt; text-align: left; }\n");
        html.append("    .delos-table thead th { background: #f8fafc; }\n");
        html.append("    .delos-align-left { text-align: left; }\n");
        html.append("    .delos-align-center { text-align: center; }\n");
        html.append("    .delos-align-right { text-align: right; }\n");
        html.append("    .delos-align-justify { text-align: justify; }\n");
        html.append("    .delos-link { color: #2563eb; text-decoration: underline; }\n");
        html.append("  </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("  <article class=\"delos-document\">\n");

        appendStory(html, blocks);

        html.append("  </article>\n");
        html.append("</body>\n");
        html.append("</html>\n");
        return html.toString();
    }

    public void write(Document document, OutputStream outputStream) throws IOException {
        Objects.requireNonNull(outputStream, "outputStream");
        outputStream.write(toHtml(document).getBytes(StandardCharsets.UTF_8));
    }

    private void appendStory(StringBuilder html, List<Block> blocks) {
        for (Block block : blocks) {
            appendBlock(html, block);
        }
    }

    private void appendStory(StringBuilder html, Story story) {
        appendStory(html, story.blocks());
    }

    private void appendBlock(StringBuilder html, Block block) {
        if (block instanceof ParagraphBlock paragraphBlock) {
            appendParagraph(html, paragraphBlock.paragraph());
            return;
        }
        if (block instanceof HorizontalRuleBlock) {
            html.append("    <hr class=\"delos-hr\">\n");
            return;
        }
        if (block instanceof ImageBlock imageBlock) {
            appendImage(html, imageBlock);
            return;
        }
        if (block instanceof FormulaBlock formulaBlock) {
            appendFormula(html, formulaBlock);
            return;
        }
        if (block instanceof TableBlock tableBlock) {
            appendTable(html, tableBlock);
            return;
        }
        throw new IllegalArgumentException("Unsupported block type: " + block.getClass().getName());
    }

    private void appendParagraph(StringBuilder html, Paragraph paragraph) {
        if (paragraph.style().isListItem()) {
            appendListParagraph(html, paragraph);
            return;
        }
        String alignmentClass = "delos-align-" + switch (paragraph.style().alignment()) {
            case LEFT -> "left";
            case CENTER -> "center";
            case RIGHT -> "right";
            case JUSTIFY -> "justify";
        };
        html.append("    <p class=\"delos-paragraph ").append(alignmentClass).append("\"");
        String paragraphStyle = toCss(paragraph.style());
        if (!paragraphStyle.isBlank()) {
            html.append(" style=\"").append(paragraphStyle).append("\"");
        }
        html.append(">");

        for (TextRun run : paragraph.runs()) {
            appendRun(html, run);
        }

        html.append("</p>\n");
    }

    private void appendListParagraph(StringBuilder html, Paragraph paragraph) {
        String tag = paragraph.style().listStyle().kind() == ListMarkerKind.NUMBERED ? "ol" : "ul";
        int level = paragraph.style().listStyle().level();
        html.append("    <").append(tag)
                .append(" class=\"delos-list delos-list-level-").append(level).append("\">");
        html.append("<li");
        String paragraphStyle = toCss(paragraph.style().withoutListStyle());
        if (!paragraphStyle.isBlank()) {
            html.append(" style=\"").append(paragraphStyle).append("\"");
        }
        html.append(">");
        for (TextRun run : paragraph.runs()) {
            appendRun(html, run);
        }
        html.append("</li></").append(tag).append(">\n");
    }

    private void appendImage(StringBuilder html, ImageBlock imageBlock) {
        String source = imageBlock.source() == null ? "" : imageBlock.source();
        String alt = imageBlock.altText().isBlank() ? deriveAltText(source) : imageBlock.altText();
        html.append("    <figure class=\"delos-figure\">\n");
        html.append("      <img class=\"delos-image\" src=\"")
                .append(escapeHtml(source))
                .append("\" alt=\"")
                .append(escapeHtml(alt))
                .append("\"");
        List<String> styles = new ArrayList<>();
        if (imageBlock.width() > 0) {
            styles.add("width: " + format(imageBlock.width()) + "pt");
        }
        if (imageBlock.height() > 0) {
            styles.add("height: " + format(imageBlock.height()) + "pt");
        }
        if (!styles.isEmpty()) {
            html.append(" style=\"").append(String.join("; ", styles)).append(";\"");
        }
        html.append(">\n");
        if (!source.isBlank()) {
            html.append("      <figcaption class=\"delos-figcaption\">")
                    .append(escapeHtml(alt))
                    .append("</figcaption>\n");
        }
        html.append("    </figure>\n");
    }

    private void appendFormula(StringBuilder html, FormulaBlock formulaBlock) {
        String alt = formulaBlock.altText().isBlank() ? formulaBlock.source() : formulaBlock.altText();
        html.append("    <figure class=\"delos-formula\"");
        if (!alt.isBlank()) {
            html.append(" aria-label=\"").append(escapeHtml(alt)).append("\"");
        }
        html.append(" data-source-format=\"")
                .append(escapeHtml(formulaBlock.sourceFormat().xmlValue()))
                .append("\">");
        html.append("<span class=\"delos-formula-badge\">ƒx</span>");
        html.append("<code class=\"delos-formula-source\">")
                .append(escapeHtml(formulaBlock.source()))
                .append("</code>");
        html.append("</figure>\n");
    }

    private void appendTable(StringBuilder html, TableBlock tableBlock) {
        List<TableRow> rows = tableBlock.rows();
        int columns = Math.max(1, maxColumnCount(rows));

        html.append("    <table class=\"delos-table\">\n");
        appendTableColgroup(html, tableBlock);
        int headerRows = Math.min(tableBlock.headerRowCount(), rows.size());
        if (headerRows > 0) {
            html.append("      <thead>\n");
            for (int rowIndex = 0; rowIndex < headerRows; rowIndex++) {
                appendHtmlTableRow(html, rows.get(rowIndex), columns, rowIndex, true);
            }
            html.append("      </thead>\n");
        }
        html.append("      <tbody>\n");
        if (rows.isEmpty()) {
            html.append("        <tr><td></td></tr>\n");
        } else {
            for (int rowIndex = headerRows; rowIndex < rows.size(); rowIndex++) {
                appendHtmlTableRow(html, rows.get(rowIndex), columns, rowIndex, false);
            }
        }
        html.append("      </tbody>\n");
        html.append("    </table>\n");
    }

    private void appendTableColgroup(StringBuilder html, TableBlock tableBlock) {
        double totalWeight = tableBlock.columns().stream().mapToDouble(column -> column.widthWeight()).sum();
        if (totalWeight <= 0.0) {
            return;
        }
        html.append("      <colgroup>\n");
        for (var column : tableBlock.columns()) {
            double percent = column.widthWeight() / totalWeight * 100.0;
            html.append("        <col style=\"width: ")
                    .append(String.format(Locale.ROOT, "%.4f", percent))
                    .append("%;\">\n");
        }
        html.append("      </colgroup>\n");
    }

    private void appendHtmlTableRow(StringBuilder html, TableRow row, int columns, int rowIndex, boolean headerRow) {
        html.append("        <tr>");
        String tag = headerRow ? "th" : "td";
        for (int columnIndex = 0; columnIndex < columns; columnIndex++) {
            html.append("<").append(tag).append(" data-row=\"").append(rowIndex + 1)
                    .append("\" data-column=\"").append(columnIndex + 1).append("\"");
            if (columnIndex < row.cells().size()) {
                appendTableCellStyle(html, row.cells().get(columnIndex));
            }
            html.append(">");
            if (columnIndex < row.cells().size()) {
                appendTableCell(html, row.cells().get(columnIndex));
            }
            html.append("</").append(tag).append(">");
        }
        html.append("</tr>\n");
    }

    private void appendTableCellStyle(StringBuilder html, TableCell cell) {
        if (cell.style().backgroundColor() != null) {
            html.append(" style=\"background-color: ")
                    .append(escapeHtml(cell.style().backgroundColor()))
                    .append(";\"");
        }
    }

    private void appendTableCell(StringBuilder html, TableCell cell) {
        appendStory(html, cell.content());
    }

    private static int maxColumnCount(List<TableRow> rows) {
        int max = 0;
        for (TableRow row : rows) {
            max = Math.max(max, row.cells().size());
        }
        return max;
    }

    private void appendRun(StringBuilder html, TextRun run) {
        String runStyle = toCss(run.style());
        String text = escapeHtml(run.text());
        if (run.linked()) {
            html.append("<a href=\"")
                    .append(escapeHtml(run.linkHref()))
                    .append("\" class=\"delos-link\"");
            if (!runStyle.isBlank()) {
                html.append(" style=\"").append(runStyle).append("\"");
            }
            html.append(">").append(text).append("</a>");
            return;
        }
        if (runStyle.isBlank()) {
            html.append(text);
            return;
        }
        html.append("<span style=\"")
                .append(runStyle)
                .append("\">")
                .append(text)
                .append("</span>");
    }

    private static String toCss(ParagraphStyle style) {
        List<String> properties = new ArrayList<>();
        if (style.firstLineIndent() != 0) {
            properties.add("text-indent: " + format(style.firstLineIndent()) + "pt");
        }
        if (style.spacingBefore() != 0) {
            properties.add("margin-top: " + format(style.spacingBefore()) + "pt");
        }
        if (style.spacingAfter() != 0) {
            properties.add("margin-bottom: " + format(style.spacingAfter()) + "pt");
        }
        if (style.lineSpacingMultiplier() != 1.0) {
            properties.add("line-height: " + format(style.lineSpacingMultiplier()));
        }
        return String.join("; ", properties) + (properties.isEmpty() ? "" : ";");
    }

    private static String toCss(CharacterStyle style) {
        List<String> properties = new ArrayList<>();
        if (style.bold()) {
            properties.add("font-weight: 700");
        }
        if (style.italic()) {
            properties.add("font-style: italic");
        }
        if (style.underline() || style.strikethrough()) {
            StringBuilder textDecoration = new StringBuilder();
            if (style.underline()) {
                textDecoration.append("underline");
            }
            if (style.strikethrough()) {
                if (!textDecoration.isEmpty()) {
                    textDecoration.append(' ');
                }
                textDecoration.append("line-through");
            }
            properties.add("text-decoration: " + textDecoration);
        }
        if (style.fontFamily() != null) {
            properties.add("font-family: '" + escapeCss(style.fontFamily()) + "'");
        }
        if (style.fontSize() != null) {
            properties.add("font-size: " + format(style.fontSize()) + "pt");
        }
        if (style.color() != null) {
            properties.add("color: " + escapeCss(style.color()));
        }
        return String.join("; ", properties) + (properties.isEmpty() ? "" : ";");
    }

    private static String format(double value) {
        if (value == (long) value) {
            return Long.toString((long) value);
        }
        return String.format(Locale.ROOT, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private static String escapeHtml(String value) {
        StringBuilder escaped = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '&' -> escaped.append("&amp;");
                case '<' -> escaped.append("&lt;");
                case '>' -> escaped.append("&gt;");
                case '"' -> escaped.append("&quot;");
                case '\'' -> escaped.append("&#39;");
                case '\u00AD' -> escaped.append("&shy;");
                default -> escaped.append(ch);
            }
        }
        return escaped.toString();
    }

    private static String escapeCss(String value) {
        return value.replace("\\", "\\\\").replace("'", "\\'");
    }

    private static String deriveAltText(String source) {
        if (source == null || source.isBlank()) {
            return "Image";
        }
        String normalized = source.replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        String tail = slash >= 0 ? normalized.substring(slash + 1) : normalized;
        return tail.isBlank() ? "Image" : tail;
    }
}
