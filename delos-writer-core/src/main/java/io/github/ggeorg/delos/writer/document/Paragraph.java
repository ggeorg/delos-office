package io.github.ggeorg.delos.writer.document;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Block-level paragraph composed of text runs.
 */
public record Paragraph(ParagraphStyle style, List<TextRun> runs) {

    public Paragraph(List<TextRun> runs) {
        this(ParagraphStyle.defaultBody(), runs);
    }

    public Paragraph {
        style = Objects.requireNonNullElse(style, ParagraphStyle.defaultBody());
        runs = normalizeRuns(Objects.requireNonNull(runs, "runs"));
    }

    public static Paragraph of(String text) {
        return new Paragraph(ParagraphStyle.defaultBody(), List.of(TextRun.plain(text)));
    }

    public static Paragraph of(ParagraphStyle style, String text) {
        return new Paragraph(style, List.of(TextRun.plain(text)));
    }

    public String plainText() {
        return runs.stream()
                .map(TextRun::text)
                .collect(Collectors.joining());
    }

    public int length() {
        return plainText().length();
    }

    public Paragraph withPlainText(String text) {
        return Paragraph.of(style, text);
    }

    public Paragraph withStyle(ParagraphStyle style) {
        return new Paragraph(style, runs);
    }

    private static List<TextRun> normalizeRuns(List<TextRun> source) {
        List<TextRun> normalized = new ArrayList<>();

        for (TextRun rawRun : source) {
            TextRun run = Objects.requireNonNull(rawRun, "run");
            if (run.text().isEmpty()) {
                continue;
            }

            if (!normalized.isEmpty()) {
                TextRun last = normalized.get(normalized.size() - 1);
                if (last.sameStyleAs(run)) {
                    normalized.set(normalized.size() - 1, last.withText(last.text() + run.text()));
                    continue;
                }
            }

            normalized.add(run);
        }

        if (normalized.isEmpty()) {
            normalized.add(TextRun.plain(""));
        }

        return List.copyOf(normalized);
    }
}
