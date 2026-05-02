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
 * Export a Delos document to GitHub-flavored Markdown with small HTML fallbacks
 * for unsupported inline styling.
 */
public final class DocumentMarkdownExporter {

    public String toMarkdown(Document document) {
        Objects.requireNonNull(document, "document");
        return toMarkdown(document.title(), document.pageStyle(), document.body());
    }

    public String toMarkdown(String title, PageStyle pageStyle, Story story) {
        Objects.requireNonNull(pageStyle, "pageStyle");
        Objects.requireNonNull(story, "story");
        return toMarkdown(title, pageStyle, story.blocks());
    }

    public String toMarkdown(String title, PageStyle pageStyle, List<Block> blocks) {
        Objects.requireNonNull(pageStyle, "pageStyle");
        Objects.requireNonNull(blocks, "blocks");

        StringBuilder markdown = new StringBuilder(2048);
        String safeTitle = title == null || title.isBlank() ? "Untitled" : title.trim();
        markdown.append("# ").append(escapeMarkdownText(safeTitle)).append("\n\n");
        markdown.append("<!-- delos-page: width=")
                .append(format(pageStyle.width())).append("pt; height=")
                .append(format(pageStyle.height())).append("pt; margins=")
                .append(format(pageStyle.marginTop())).append(' ')
                .append(format(pageStyle.marginRight())).append(' ')
                .append(format(pageStyle.marginBottom())).append(' ')
                .append(format(pageStyle.marginLeft())).append("pt -->\n\n");

        appendStory(markdown, blocks);
        markdown.append('\n');
        return markdown.toString();
    }

    public void write(Document document, OutputStream outputStream) throws IOException {
        Objects.requireNonNull(outputStream, "outputStream");
        outputStream.write(toMarkdown(document).getBytes(StandardCharsets.UTF_8));
    }

    private void appendStory(StringBuilder markdown, List<Block> blocks) {
        boolean first = true;
        for (Block block : blocks) {
            if (!first) {
                markdown.append("\n\n");
            }
            appendBlock(markdown, block);
            first = false;
        }
    }

    private void appendStory(StringBuilder markdown, Story story) {
        appendStory(markdown, story.blocks());
    }

    private void appendBlock(StringBuilder markdown, Block block) {
        if (block instanceof ParagraphBlock paragraphBlock) {
            appendParagraph(markdown, paragraphBlock.paragraph());
            return;
        }
        if (block instanceof HorizontalRuleBlock) {
            markdown.append("---");
            return;
        }
        if (block instanceof ImageBlock imageBlock) {
            appendImage(markdown, imageBlock);
            return;
        }
        if (block instanceof FormulaBlock formulaBlock) {
            appendFormula(markdown, formulaBlock);
            return;
        }
        if (block instanceof TableBlock tableBlock) {
            appendTable(markdown, tableBlock);
            return;
        }
        throw new IllegalArgumentException("Unsupported block type: " + block.getClass().getName());
    }

    private void appendParagraph(StringBuilder markdown, Paragraph paragraph) {
        if (paragraph.style().isListItem()) {
            appendListParagraph(markdown, paragraph);
            return;
        }
        ParagraphStyle style = paragraph.style();
        if (!isDefaultBodyStyle(style)) {
            markdown.append("<!-- delos-paragraph: alignment=")
                    .append(style.alignment().name().toLowerCase(Locale.ROOT));
            if (style.firstLineIndent() != 0) {
                markdown.append("; firstLineIndent=").append(format(style.firstLineIndent())).append("pt");
            }
            if (style.spacingBefore() != 0) {
                markdown.append("; spacingBefore=").append(format(style.spacingBefore())).append("pt");
            }
            if (style.spacingAfter() != 0) {
                markdown.append("; spacingAfter=").append(format(style.spacingAfter())).append("pt");
            }
            if (style.lineSpacingMultiplier() != 1.0) {
                markdown.append("; lineHeight=").append(format(style.lineSpacingMultiplier()));
            }
            markdown.append(" -->\n");
        }

        StringBuilder line = new StringBuilder();
        for (TextRun run : paragraph.runs()) {
            line.append(renderRun(run));
        }
        markdown.append(line);
    }

    private void appendListParagraph(StringBuilder markdown, Paragraph paragraph) {
        int level = paragraph.style().listStyle().level();
        markdown.append("  ".repeat(Math.max(0, level)));
        if (paragraph.style().listStyle().kind() == ListMarkerKind.NUMBERED) {
            markdown.append(paragraph.style().listStyle().start()).append(". " );
        } else {
            markdown.append("- " );
        }
        for (TextRun run : paragraph.runs()) {
            markdown.append(renderRun(run));
        }
    }

    private void appendImage(StringBuilder markdown, ImageBlock imageBlock) {
        String source = imageBlock.source() == null ? "" : imageBlock.source();
        String alt = imageBlock.altText().isBlank() ? (source.isBlank() ? "Image" : deriveAltText(source)) : imageBlock.altText();
        markdown.append("![")
                .append(escapeMarkdownImageAltText(alt))
                .append("](")
                .append(escapeMarkdownDestination(source))
                .append(")");
        if (imageBlock.width() > 0 || imageBlock.height() > 0) {
            markdown.append("\n<!-- delos-image:");
            if (imageBlock.width() > 0) {
                markdown.append(" width=").append(format(imageBlock.width())).append("pt");
            }
            if (imageBlock.height() > 0) {
                markdown.append(" height=").append(format(imageBlock.height())).append("pt");
            }
            markdown.append(" -->");
        }
    }

    private void appendFormula(StringBuilder markdown, FormulaBlock formulaBlock) {
        markdown.append("<!-- delos-formula: source-format=")
                .append(formulaBlock.sourceFormat().xmlValue());
        if (!formulaBlock.altText().isBlank()) {
            markdown.append("; alt-text=").append(escapeHtml(formulaBlock.altText()));
        }
        markdown.append(" -->\n");
        markdown.append("```math\n");
        markdown.append(escapeMarkdownFenceContent(formulaBlock.source()));
        if (!formulaBlock.source().endsWith("\n")) {
            markdown.append('\n');
        }
        markdown.append("```");
    }

    private void appendTable(StringBuilder markdown, TableBlock tableBlock) {
        List<TableRow> rows = tableBlock.rows();
        int columns = Math.max(1, maxColumnCount(rows));

        appendMarkdownTableRow(markdown, rows.isEmpty() ? null : rows.get(0), columns);
        markdown.append('\n').append('|');
        for (int column = 0; column < columns; column++) {
            markdown.append(" --- |");
        }
        for (int rowIndex = 1; rowIndex < rows.size(); rowIndex++) {
            markdown.append('\n');
            appendMarkdownTableRow(markdown, rows.get(rowIndex), columns);
        }
        if (rows.size() < 2) {
            markdown.append('\n');
            appendMarkdownTableRow(markdown, null, columns);
        }
    }

    private void appendMarkdownTableRow(StringBuilder markdown, TableRow row, int columns) {
        markdown.append('|');
        for (int columnIndex = 0; columnIndex < columns; columnIndex++) {
            String cellText = "";
            if (row != null && columnIndex < row.cells().size()) {
                cellText = renderCellText(row.cells().get(columnIndex));
            }
            markdown.append(' ').append(cellText).append(' ').append('|');
        }
    }

    private String renderCellText(TableCell cell) {
        StringBuilder text = new StringBuilder();
        appendStory(text, cell.content());
        return text.toString()
                .replace("\r\n", "\n")
                .replace("\n\n", "<br>")
                .replace("\n", "<br>");
    }

    private static int maxColumnCount(List<TableRow> rows) {
        int max = 0;
        for (TableRow row : rows) {
            max = Math.max(max, row.cells().size());
        }
        return max;
    }

    private String renderRun(TextRun run) {
        CharacterStyle style = run.style();
        String text = escapeMarkdownText(run.text()).replace("\u00AD", "&shy;");

        boolean needsHtmlSpan = style.underline()
                || style.fontFamily() != null
                || style.fontSize() != null
                || style.color() != null;

        if (needsHtmlSpan) {
            StringBuilder html = new StringBuilder();
            html.append("<span style=\"");
            appendCss(html, style);
            html.append("\">");
            html.append(text.replace("\n", "<br>"));
            html.append("</span>");
            text = html.toString();
        }

        if (style.strikethrough()) {
            text = "~~" + text + "~~";
        }
        if (style.bold() && style.italic()) {
            text = "***" + text + "***";
        } else if (style.bold()) {
            text = "**" + text + "**";
        } else if (style.italic()) {
            text = "*" + text + "*";
        }
        if (style.linkHref() != null) {
            text = "[" + text + "](" + escapeMarkdownDestination(style.linkHref()) + ")";
        }
        return text;
    }

    private static void appendCss(StringBuilder html, CharacterStyle style) {
        List<String> properties = new ArrayList<>();
        if (style.underline()) {
            properties.add("text-decoration: underline");
        }
        if (style.fontFamily() != null) {
            properties.add("font-family: '" + escapeHtml(style.fontFamily()) + "'");
        }
        if (style.fontSize() != null) {
            properties.add("font-size: " + format(style.fontSize()) + "pt");
        }
        if (style.color() != null) {
            properties.add("color: " + escapeHtml(style.color()));
        }
        html.append(String.join("; ", properties));
    }

    private static boolean isDefaultBodyStyle(ParagraphStyle style) {
        ParagraphStyle defaults = ParagraphStyle.defaultBody();
        return style.alignment() == defaults.alignment()
                && style.firstLineIndent() == defaults.firstLineIndent()
                && style.spacingBefore() == defaults.spacingBefore()
                && style.spacingAfter() == defaults.spacingAfter()
                && style.lineSpacingMultiplier() == defaults.lineSpacingMultiplier();
    }

    private static String deriveAltText(String source) {
        String normalized = source.replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        String tail = slash >= 0 ? normalized.substring(slash + 1) : normalized;
        return tail.isBlank() ? "Image" : tail;
    }


    private static String escapeMarkdownImageAltText(String value) {
        StringBuilder escaped = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '\\', '[', ']', '(', ')' -> escaped.append('\\').append(ch);
                case '&' -> escaped.append("&amp;");
                case '<' -> escaped.append("&lt;");
                case '>' -> escaped.append("&gt;");
                default -> escaped.append(ch);
            }
        }
        return escaped.toString();
    }

    private static String escapeMarkdownText(String value) {
        StringBuilder escaped = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '\\', '`', '*', '_', '{', '}', '[', ']', '(', ')', '#', '+', '-', '.', '!', '|' -> escaped.append('\\').append(ch);
                case '&' -> escaped.append("&amp;");
                case '<' -> escaped.append("&lt;");
                case '>' -> escaped.append("&gt;");
                default -> escaped.append(ch);
            }
        }
        return escaped.toString();
    }

    private static String escapeMarkdownDestination(String value) {
        return value.replace(" ", "%20").replace("(", "%28").replace(")", "%29");
    }

    private static String escapeMarkdownFenceContent(String value) {
        return value.replace("```", "`\u200B``");
    }

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static String format(double value) {
        if (value == (long) value) {
            return Long.toString((long) value);
        }
        return String.format(Locale.ROOT, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
