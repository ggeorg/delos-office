package io.github.ggeorg.delos.writer.editor;

import io.github.ggeorg.delos.writer.document.Paragraph;
import io.github.ggeorg.delos.writer.document.TextPosition;
import io.github.ggeorg.delos.writer.document.TextRun;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Shared paragraph run slicing helpers for editor mutations. */
final class ParagraphRuns {
    private ParagraphRuns() {
    }

    static TextPosition clampPosition(List<Paragraph> paragraphs, TextPosition position) {
        Objects.requireNonNull(paragraphs, "paragraphs");
        Objects.requireNonNull(position, "position");
        if (paragraphs.isEmpty()) {
            return new TextPosition(0, 0);
        }
        int paragraphIndex = Math.max(0, Math.min(position.paragraphIndex(), paragraphs.size() - 1));
        int offset = Math.max(0, Math.min(position.offset(), paragraphs.get(paragraphIndex).length()));
        return new TextPosition(paragraphIndex, offset);
    }

    static List<TextRun> prefix(Paragraph paragraph, int endOffsetExclusive) {
        Objects.requireNonNull(paragraph, "paragraph");
        List<TextRun> result = new ArrayList<>();
        int offset = 0;
        for (TextRun run : paragraph.runs()) {
            int runStart = offset;
            int runEnd = offset + run.text().length();
            if (endOffsetExclusive <= runStart) {
                break;
            }
            if (endOffsetExclusive >= runEnd) {
                result.add(run);
            } else {
                result.add(run.withText(run.text().substring(0, endOffsetExclusive - runStart)));
                break;
            }
            offset = runEnd;
        }
        return List.copyOf(result);
    }

    static List<TextRun> suffix(Paragraph paragraph, int startOffsetInclusive) {
        Objects.requireNonNull(paragraph, "paragraph");
        List<TextRun> result = new ArrayList<>();
        int offset = 0;
        for (TextRun run : paragraph.runs()) {
            int runStart = offset;
            int runEnd = offset + run.text().length();
            if (startOffsetInclusive >= runEnd) {
                offset = runEnd;
                continue;
            }
            if (startOffsetInclusive <= runStart) {
                result.add(run);
            } else {
                result.add(run.withText(run.text().substring(startOffsetInclusive - runStart)));
            }
            offset = runEnd;
        }
        return List.copyOf(result);
    }

    static String normalizeReplacement(String replacement) {
        return replacement == null ? "" : replacement.replace("\r\n", "\n").replace('\r', '\n');
    }
}
