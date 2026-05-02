package io.github.ggeorg.delos.writer.editor;

import io.github.ggeorg.delos.writer.document.Block;
import io.github.ggeorg.delos.writer.document.Paragraph;
import io.github.ggeorg.delos.writer.document.ParagraphBlock;
import io.github.ggeorg.delos.writer.document.Story;
import io.github.ggeorg.delos.writer.document.TextPosition;
import io.github.ggeorg.delos.writer.document.TextRun;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Story-local editing service.
 *
 * <p>The model stays passive: {@link Story} only stores blocks. This service
 * owns reusable paragraph/block edits that can be applied to the document body,
 * table cells, and future editable containers such as headers, footnotes, and
 * text boxes.</p>
 */
public final class StoryEditor {
    public StoryEdit replace(Story story, TextPosition start, TextPosition end, String replacement) {
        Objects.requireNonNull(story, "story");
        List<Paragraph> paragraphs = new ArrayList<>(story.paragraphs());
        if (paragraphs.isEmpty()) {
            paragraphs.add(Paragraph.of(""));
        }

        TextPosition safeStart = ParagraphRuns.clampPosition(paragraphs, Objects.requireNonNull(start, "start"));
        TextPosition safeEnd = ParagraphRuns.clampPosition(paragraphs, Objects.requireNonNull(end, "end"));
        if (safeStart.compareTo(safeEnd) > 0) {
            TextPosition tmp = safeStart;
            safeStart = safeEnd;
            safeEnd = tmp;
        }

        Paragraph startParagraph = paragraphs.get(safeStart.paragraphIndex());
        Paragraph endParagraph = paragraphs.get(safeEnd.paragraphIndex());
        List<TextRun> prefixRuns = ParagraphRuns.prefix(startParagraph, safeStart.offset());
        List<TextRun> suffixRuns = ParagraphRuns.suffix(endParagraph, safeEnd.offset());
        String[] replacementParts = ParagraphRuns.normalizeReplacement(replacement).split("\n", -1);

        List<Paragraph> replacementParagraphs = new ArrayList<>();
        TextPosition newCaret;
        if (replacementParts.length == 1) {
            List<TextRun> mergedRuns = new ArrayList<>(prefixRuns);
            if (!replacementParts[0].isEmpty()) {
                mergedRuns.add(TextRun.plain(replacementParts[0]));
            }
            mergedRuns.addAll(suffixRuns);
            replacementParagraphs.add(new Paragraph(startParagraph.style(), mergedRuns));
            newCaret = new TextPosition(safeStart.paragraphIndex(), safeStart.offset() + replacementParts[0].length());
        } else {
            List<TextRun> firstRuns = new ArrayList<>(prefixRuns);
            if (!replacementParts[0].isEmpty()) {
                firstRuns.add(TextRun.plain(replacementParts[0]));
            }
            replacementParagraphs.add(new Paragraph(startParagraph.style(), firstRuns));

            for (int index = 1; index < replacementParts.length - 1; index++) {
                replacementParagraphs.add(Paragraph.of(startParagraph.style(), replacementParts[index]));
            }

            List<TextRun> lastRuns = new ArrayList<>();
            if (!replacementParts[replacementParts.length - 1].isEmpty()) {
                lastRuns.add(TextRun.plain(replacementParts[replacementParts.length - 1]));
            }
            lastRuns.addAll(suffixRuns);
            replacementParagraphs.add(new Paragraph(startParagraph.style(), lastRuns));

            newCaret = new TextPosition(
                    safeStart.paragraphIndex() + replacementParts.length - 1,
                    replacementParts[replacementParts.length - 1].length()
            );
        }

        List<Block> updatedBlocks = replaceParagraphRange(
                story.blocks(),
                safeStart.paragraphIndex(),
                safeEnd.paragraphIndex(),
                replacementParagraphs
        );
        return StoryEdit.ofCaret(Story.ofBlocks(updatedBlocks), newCaret);
    }

    public StoryEdit insert(Story story, TextPosition caret, String text) {
        Objects.requireNonNull(caret, "caret");
        return replace(story, caret, caret, text);
    }

    public StoryEdit delete(Story story, TextPosition start, TextPosition end) {
        return replace(story, start, end, "");
    }

    public Story fromPlainText(String text) {
        String normalized = ParagraphRuns.normalizeReplacement(text);
        String[] parts = normalized.split("\n", -1);
        List<Paragraph> paragraphs = new ArrayList<>();
        for (String part : parts) {
            paragraphs.add(Paragraph.of(part));
        }
        if (paragraphs.isEmpty()) {
            paragraphs.add(Paragraph.of(""));
        }
        return Story.ofParagraphs(paragraphs);
    }

    public String plainText(Story story) {
        Objects.requireNonNull(story, "story");
        StringBuilder out = new StringBuilder();
        List<Paragraph> paragraphs = story.paragraphs();
        for (int index = 0; index < paragraphs.size(); index++) {
            if (index > 0) {
                out.append('\n');
            }
            out.append(paragraphs.get(index).plainText());
        }
        return out.toString();
    }

    private List<Block> replaceParagraphRange(
            List<Block> blocks,
            int startParagraphIndex,
            int endParagraphIndex,
            List<Paragraph> replacementParagraphs
    ) {
        List<Block> updatedBlocks = new ArrayList<>();
        int paragraphIndex = 0;
        boolean insertedReplacement = false;

        for (Block block : blocks) {
            if (!(block instanceof ParagraphBlock)) {
                updatedBlocks.add(block);
                continue;
            }

            if (paragraphIndex < startParagraphIndex || paragraphIndex > endParagraphIndex) {
                updatedBlocks.add(block);
            } else if (!insertedReplacement) {
                for (Paragraph paragraph : replacementParagraphs) {
                    updatedBlocks.add(new ParagraphBlock(paragraph));
                }
                insertedReplacement = true;
            }
            paragraphIndex += 1;
        }

        if (!insertedReplacement) {
            for (Paragraph paragraph : replacementParagraphs) {
                updatedBlocks.add(new ParagraphBlock(paragraph));
            }
        }
        if (updatedBlocks.stream().noneMatch(ParagraphBlock.class::isInstance)) {
            updatedBlocks.add(new ParagraphBlock(Paragraph.of("")));
        }
        return List.copyOf(updatedBlocks);
    }

}
