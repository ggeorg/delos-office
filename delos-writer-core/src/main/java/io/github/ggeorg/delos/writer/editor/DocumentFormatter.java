package io.github.ggeorg.delos.writer.editor;

import io.github.ggeorg.delos.writer.document.*;
import io.github.ggeorg.delos.writer.document.SelectionRange;
import io.github.ggeorg.delos.writer.document.TextPosition;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * Applies inline formatting to selected document ranges.
 */
public final class DocumentFormatter {
    public DocumentEdit toggle(
            Document document,
            SelectionRange selection,
            TextPosition caret,
            TextStyle style,
            String description
    ) {
        if (selection == null || selection.isCollapsed()) {
            return DocumentEdit.ofSelection(document, caret, selection, description);
        }

        TextPosition start = selection.start();
        TextPosition end = selection.end();
        boolean applyValue = !allStyled(document, start, end, style);

        Document updated = updateRunsInRange(
                document,
                start,
                end,
                run -> applyStyle(run, style, applyValue)
        );
        return DocumentEdit.ofSelection(updated, caret, selection, description);
    }

    public DocumentEdit applyLink(
            Document document,
            SelectionRange selection,
            TextPosition caret,
            String href,
            String description
    ) {
        if (selection == null || selection.isCollapsed()) {
            return DocumentEdit.ofSelection(document, caret, selection, description);
        }

        String normalizedHref = normalizeHref(href);
        Document updated = updateRunsInRange(
                document,
                selection.start(),
                selection.end(),
                run -> run.withLinkHref(normalizedHref)
        );
        return DocumentEdit.ofSelection(updated, caret, selection, description);
    }

    public DocumentEdit removeLink(
            Document document,
            SelectionRange selection,
            TextPosition caret,
            String description
    ) {
        if (selection == null || selection.isCollapsed()) {
            return DocumentEdit.ofSelection(document, caret, selection, description);
        }

        Document updated = updateRunsInRange(
                document,
                selection.start(),
                selection.end(),
                run -> run.withLinkHref(null)
        );
        return DocumentEdit.ofSelection(updated, caret, selection, description);
    }


    public DocumentEdit alignParagraphs(
            Document document,
            SelectionRange selection,
            TextPosition caret,
            Alignment alignment,
            String description
    ) {
        if (document.paragraphs().isEmpty()) {
            return DocumentEdit.ofSelection(document, caret, selection, description);
        }

        ParagraphRange range = paragraphRange(document, selection, caret);
        Document updated = updateParagraphs(
                document,
                range,
                paragraph -> paragraph.withStyle(paragraph.style().withAlignment(alignment))
        );
        return DocumentEdit.ofSelection(updated, range.caret(), selection, description);
    }

    public DocumentEdit toggleListKind(
            Document document,
            SelectionRange selection,
            TextPosition caret,
            ListMarkerKind kind,
            String description
    ) {
        Objects.requireNonNull(kind, "kind");
        if (kind == ListMarkerKind.NONE || document.paragraphs().isEmpty()) {
            return DocumentEdit.ofSelection(document, caret, selection, description);
        }

        ParagraphRange range = paragraphRange(document, selection, caret);
        boolean remove = allParagraphsHaveListKind(document, range, kind);
        Document updated = updateParagraphs(
                document,
                range,
                paragraph -> paragraph.withStyle(remove
                        ? paragraph.style().withoutListStyle()
                        : withListKind(paragraph.style(), kind))
        );
        return DocumentEdit.ofSelection(updated, range.caret(), selection, description);
    }

    public DocumentEdit increaseListLevel(
            Document document,
            SelectionRange selection,
            TextPosition caret,
            String description
    ) {
        if (document.paragraphs().isEmpty()) {
            return DocumentEdit.ofSelection(document, caret, selection, description);
        }

        ParagraphRange range = paragraphRange(document, selection, caret);
        Document updated = updateParagraphs(
                document,
                range,
                paragraph -> paragraph.style().isListItem()
                        ? paragraph.withStyle(withListLevel(paragraph.style(), paragraph.style().listStyle().level() + 1))
                        : paragraph
        );
        return DocumentEdit.ofSelection(updated, range.caret(), selection, description);
    }

    public DocumentEdit decreaseListLevel(
            Document document,
            SelectionRange selection,
            TextPosition caret,
            String description
    ) {
        if (document.paragraphs().isEmpty()) {
            return DocumentEdit.ofSelection(document, caret, selection, description);
        }

        ParagraphRange range = paragraphRange(document, selection, caret);
        Document updated = updateParagraphs(
                document,
                range,
                paragraph -> {
                    ParagraphStyle style = paragraph.style();
                    if (!style.isListItem()) {
                        return paragraph;
                    }
                    int level = style.listStyle().level();
                    return paragraph.withStyle(level <= 0
                            ? style.withoutListStyle()
                            : withListLevel(style, level - 1));
                }
        );
        return DocumentEdit.ofSelection(updated, range.caret(), selection, description);
    }

    private Document updateRunsInRange(Document document, TextPosition start, TextPosition end, UnaryOperator<TextRun> updater) {
        List<Paragraph> paragraphs = new ArrayList<>(document.paragraphs());
        for (int paragraphIndex = start.paragraphIndex(); paragraphIndex <= end.paragraphIndex(); paragraphIndex++) {
            Paragraph paragraph = paragraphs.get(paragraphIndex);
            int localStart = paragraphIndex == start.paragraphIndex() ? start.offset() : 0;
            int localEnd = paragraphIndex == end.paragraphIndex() ? end.offset() : paragraph.length();
            paragraphs.set(paragraphIndex, updateRuns(paragraph, localStart, localEnd, updater));
        }
        return documentWithParagraphs(document, paragraphs);
    }

    private ParagraphRange paragraphRange(Document document, SelectionRange selection, TextPosition caret) {
        TextPosition effectiveCaret = caret == null ? new TextPosition(0, 0) : caret;
        int startParagraph = selection != null && !selection.isCollapsed()
                ? selection.start().paragraphIndex()
                : effectiveCaret.paragraphIndex();
        int endParagraph = selection != null && !selection.isCollapsed()
                ? selection.end().paragraphIndex()
                : effectiveCaret.paragraphIndex();

        startParagraph = Math.max(0, Math.min(startParagraph, document.paragraphs().size() - 1));
        endParagraph = Math.max(0, Math.min(endParagraph, document.paragraphs().size() - 1));
        if (startParagraph > endParagraph) {
            int tmp = startParagraph;
            startParagraph = endParagraph;
            endParagraph = tmp;
        }
        return new ParagraphRange(startParagraph, endParagraph, effectiveCaret);
    }

    private Document updateParagraphs(Document document, ParagraphRange range, UnaryOperator<Paragraph> updater) {
        List<Paragraph> paragraphs = new ArrayList<>(document.paragraphs());
        for (int i = range.startParagraph(); i <= range.endParagraph(); i++) {
            paragraphs.set(i, updater.apply(paragraphs.get(i)));
        }
        return documentWithParagraphs(document, paragraphs);
    }

    private boolean allParagraphsHaveListKind(Document document, ParagraphRange range, ListMarkerKind kind) {
        for (int i = range.startParagraph(); i <= range.endParagraph(); i++) {
            if (document.paragraphs().get(i).style().listStyle().kind() != kind) {
                return false;
            }
        }
        return true;
    }

    private ParagraphStyle withListKind(ParagraphStyle style, ListMarkerKind kind) {
        int level = style.isListItem() ? style.listStyle().level() : 0;
        return switch (kind) {
            case BULLET -> style.asBulletListItem(level);
            case NUMBERED -> style.asNumberedListItem(level, 1);
            case NONE -> style.withoutListStyle();
        };
    }

    private ParagraphStyle withListLevel(ParagraphStyle style, int level) {
        int normalizedLevel = Math.max(0, Math.min(level, 8));
        ParagraphListStyle listStyle = style.listStyle();
        return switch (listStyle.kind()) {
            case BULLET -> style.asBulletListItem(normalizedLevel);
            case NUMBERED -> style.asNumberedListItem(normalizedLevel, listStyle.start());
            case NONE -> style;
        };
    }

    private Document documentWithParagraphs(Document document, List<Paragraph> paragraphs) {
        List<Block> blocks = new ArrayList<>();
        int paragraphIndex = 0;
        for (Block block : document.blocks()) {
            if (block instanceof ParagraphBlock) {
                if (paragraphIndex < paragraphs.size()) {
                    blocks.add(new ParagraphBlock(paragraphs.get(paragraphIndex)));
                }
                paragraphIndex += 1;
            } else {
                blocks.add(block);
            }
        }
        while (paragraphIndex < paragraphs.size()) {
            blocks.add(new ParagraphBlock(paragraphs.get(paragraphIndex)));
            paragraphIndex += 1;
        }
        return Document.fromBlocks(document.title(), document.pageStyle(), blocks, document.mediaItems());
    }

    private boolean allStyled(Document document, TextPosition start, TextPosition end, TextStyle style) {
        for (int paragraphIndex = start.paragraphIndex(); paragraphIndex <= end.paragraphIndex(); paragraphIndex++) {
            Paragraph paragraph = document.paragraphs().get(paragraphIndex);
            int localStart = paragraphIndex == start.paragraphIndex() ? start.offset() : 0;
            int localEnd = paragraphIndex == end.paragraphIndex() ? end.offset() : paragraph.length();
            int runOffset = 0;
            for (TextRun run : paragraph.runs()) {
                int runStart = runOffset;
                int runEnd = runOffset + run.text().length();
                int overlapStart = Math.max(localStart, runStart);
                int overlapEnd = Math.min(localEnd, runEnd);
                if (overlapEnd > overlapStart && !hasStyle(run, style)) {
                    return false;
                }
                runOffset = runEnd;
            }
        }
        return true;
    }

    private Paragraph updateRuns(Paragraph paragraph, int startOffset, int endOffset, UnaryOperator<TextRun> updater) {
        if (endOffset <= startOffset) {
            return paragraph;
        }

        List<TextRun> updated = new ArrayList<>();
        int runOffset = 0;
        for (TextRun run : paragraph.runs()) {
            int runStart = runOffset;
            int runEnd = runOffset + run.text().length();
            int overlapStart = Math.max(startOffset, runStart);
            int overlapEnd = Math.min(endOffset, runEnd);

            if (overlapEnd <= overlapStart) {
                updated.add(run);
            } else {
                int localStart = overlapStart - runStart;
                int localEnd = overlapEnd - runStart;
                if (localStart > 0) {
                    updated.add(run.withText(run.text().substring(0, localStart)));
                }
                if (localEnd > localStart) {
                    updated.add(updater.apply(run.withText(run.text().substring(localStart, localEnd))));
                }
                if (localEnd < run.text().length()) {
                    updated.add(run.withText(run.text().substring(localEnd)));
                }
            }
            runOffset = runEnd;
        }

        return new Paragraph(paragraph.style(), updated);
    }

    private boolean hasStyle(TextRun run, TextStyle style) {
        return switch (style) {
            case BOLD -> run.bold();
            case ITALIC -> run.italic();
            case UNDERLINE -> run.underline();
            case STRIKETHROUGH -> run.strikethrough();
        };
    }

    private TextRun applyStyle(TextRun run, TextStyle style, boolean value) {
        return switch (style) {
            case BOLD -> run.withBold(value);
            case ITALIC -> run.withItalic(value);
            case UNDERLINE -> run.withUnderline(value);
            case STRIKETHROUGH -> run.withStrikethrough(value);
        };
    }

    private String normalizeHref(String href) {
        String normalized = Objects.requireNonNullElse(href, "").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("href is required");
        }
        return normalized;
    }

    private record ParagraphRange(int startParagraph, int endParagraph, TextPosition caret) { }
}
