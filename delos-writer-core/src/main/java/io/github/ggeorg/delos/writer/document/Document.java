package io.github.ggeorg.delos.writer.document;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Root document aggregate for Delos Writer.
 *
 * <p>The live editor path can still consume {@link #paragraphs()}, but the
 * native model now stores the body as a {@link Story}. Paragraph-only code
 * remains source-compatible through the historical constructor.</p>
 */
public final class Document {
    private final String title;
    private final PageStyle pageStyle;
    private final Story body;
    private final List<DocumentMediaItem> mediaItems;

    public Document(String title, PageStyle pageStyle, List<Paragraph> paragraphs) {
        this(title, pageStyle, Story.ofParagraphs(paragraphs), List.of());
    }

    private Document(String title, PageStyle pageStyle, List<Block> blocks, List<DocumentMediaItem> mediaItems) {
        this(title, pageStyle, Story.ofBlocks(blocks), mediaItems);
    }

    private Document(String title, PageStyle pageStyle, Story body, List<DocumentMediaItem> mediaItems) {
        this.title = Objects.requireNonNullElse(title, "Untitled");
        this.pageStyle = Objects.requireNonNull(pageStyle, "pageStyle");
        this.body = Objects.requireNonNull(body, "body");
        this.mediaItems = List.copyOf(Objects.requireNonNull(mediaItems, "mediaItems").stream()
                .map(item -> Objects.requireNonNull(item, "mediaItem"))
                .toList());
    }

    public static Document fromBlocks(String title, PageStyle pageStyle, List<Block> blocks) {
        return fromBlocks(title, pageStyle, blocks, List.of());
    }

    public static Document fromBlocks(String title, PageStyle pageStyle, List<Block> blocks, List<DocumentMediaItem> mediaItems) {
        return new Document(title, pageStyle, blocks, mediaItems);
    }

    public String title() {
        return title;
    }

    public PageStyle pageStyle() {
        return pageStyle;
    }

    public Story body() {
        return body;
    }

    /**
     * Paragraph projection used by the current editor/layout stack.
     *
     * <p>Non-paragraph blocks are intentionally skipped here. Code that needs
     * full-fidelity native content must use {@link #blocks()} or {@link #body()}.</p>
     */
    public List<Paragraph> paragraphs() {
        return body.paragraphs();
    }

    public List<Block> blocks() {
        return body.blocks();
    }

    public List<DocumentMediaItem> mediaItems() {
        return mediaItems;
    }

    public Document withPageStyle(PageStyle pageStyle) {
        return fromBlocks(title, Objects.requireNonNull(pageStyle, "pageStyle"), blocks(), mediaItems);
    }

    public Document withBlocks(List<Block> blocks) {
        return fromBlocks(title, pageStyle, blocks, mediaItems);
    }

    public Document withMediaItems(List<DocumentMediaItem> mediaItems) {
        return fromBlocks(title, pageStyle, blocks(), mediaItems);
    }

    public static Document blank() {
        return new Document(
                "Untitled",
                PageStyle.a4Default(),
                List.of(Paragraph.of(""))
        );
    }

    public static Document sample() {
        List<Paragraph> paragraphs = new ArrayList<>();
        paragraphs.add(new Paragraph(List.of(
                new TextRun("Delos Writer ", true, false, false),
                new TextRun("now supports ", false, false, false),
                new TextRun("styled runs", false, true, false),
                new TextRun(" inside the same paragraph.", false, false, false)
        )));
        paragraphs.add(new Paragraph(List.of(
                new TextRun("Select text and press ", false, false, false),
                new TextRun("Bold", true, false, false),
                new TextRun(", ", false, false, false),
                new TextRun("Italic", false, true, false),
                new TextRun(", or ", false, false, false),
                new TextRun("Underline", false, false, true),
                new TextRun(" to change the document model instead of just the toolbar.", false, false, false)
        )));
        for (int i = 1; i <= 18; i++) {
            paragraphs.add(Paragraph.of(
                    "This page exists to demonstrate Delos as a structured editor: document model, pagination, caret mapping, editing commands, and now run-level formatting. Paragraph "
                            + i
                            + " keeps the document long enough to exercise multi-page flow."
            ));
        }

        return new Document(
                "Untitled",
                PageStyle.a4Default(),
                paragraphs
        );
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Document document
                && title.equals(document.title)
                && pageStyle.equals(document.pageStyle)
                && body.equals(document.body)
                && mediaItems.equals(document.mediaItems);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, pageStyle, body, mediaItems);
    }

    @Override
    public String toString() {
        return "Document[title=" + title + ", pageStyle=" + pageStyle
                + ", body=" + body + ", mediaItems=" + mediaItems + ']';
    }
}
