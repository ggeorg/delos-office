package io.github.ggeorg.delos.writer.layout;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * One laid-out visual line with enough geometry for hit testing.
 *
 * <p>{@code text} is the visual text that renderers draw. It may contain
 * discretionary glyphs such as an automatic hyphen at a line break. The
 * {@code startOffset}, {@code endOffset}, and {@code caretOffsets} remain in
 * document-source coordinates so editing, hit testing, selection, and word
 * navigation never treat inserted hyphen glyphs as real document characters.</p>
 */
public record LaidOutLine(
        String text,
        double x,
        double y,
        double width,
        double height,
        double baseline,
        int startOffset,
        int endOffset,
        List<LaidOutRun> runs,
        List<Double> caretStops,
        List<Integer> caretOffsets
) {
    public LaidOutLine(
            String text,
            double x,
            double y,
            double width,
            double height,
            double baseline,
            int startOffset,
            int endOffset,
            List<LaidOutRun> runs,
            List<Double> caretStops
    ) {
        this(text, x, y, width, height, baseline, startOffset, endOffset, runs, caretStops,
                defaultCaretOffsets(startOffset, endOffset, caretStops));
    }

    public LaidOutLine {
        text = Objects.requireNonNullElse(text, "");
        runs = List.copyOf(Objects.requireNonNull(runs, "runs"));
        caretStops = List.copyOf(Objects.requireNonNull(caretStops, "caretStops"));
        caretOffsets = List.copyOf(Objects.requireNonNull(caretOffsets, "caretOffsets"));

        if (caretStops.isEmpty()) {
            throw new IllegalArgumentException("caretStops must contain at least one entry");
        }
        if (caretStops.size() != caretOffsets.size()) {
            throw new IllegalArgumentException("caretStops and caretOffsets must have the same size");
        }
        if (startOffset < 0) {
            throw new IllegalArgumentException("startOffset must be >= 0");
        }
        if (endOffset < startOffset) {
            throw new IllegalArgumentException("endOffset must be >= startOffset");
        }
        int previous = Integer.MIN_VALUE;
        for (int offset : caretOffsets) {
            if (offset < startOffset || offset > endOffset) {
                throw new IllegalArgumentException("caretOffsets must stay within the line source range");
            }
            if (offset < previous) {
                throw new IllegalArgumentException("caretOffsets must be monotonic");
            }
            previous = offset;
        }
    }

    public int length() {
        return text.length();
    }

    /**
     * Returns the line text mapped back to document-source text.
     *
     * <p>Automatically inserted discretionary hyphens have zero source advance
     * and are therefore omitted. This is the text navigation/copy/search code
     * should use when it needs logical content rather than visible glyphs.</p>
     */
    public String sourceText() {
        if (text.isEmpty()) {
            return "";
        }
        StringBuilder source = new StringBuilder(text.length());
        int limit = Math.min(text.length(), caretOffsets.size() - 1);
        for (int i = 0; i < limit; i++) {
            if (caretOffsets.get(i + 1) > caretOffsets.get(i)) {
                source.append(text.charAt(i));
            }
        }
        return source.toString();
    }

    public boolean containsY(double localY) {
        return localY >= y && localY <= y + height;
    }

    public int nearestColumn(double localX) {
        int bestIndex = 0;
        double bestDistance = Double.MAX_VALUE;

        for (int i = 0; i < caretStops.size(); i++) {
            double distance = Math.abs(localX - caretStops.get(i));
            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = i;
            }
        }

        return bestIndex;
    }

    public double caretXForColumn(int columnIndex) {
        int safeIndex = safeColumn(columnIndex);
        return x + caretStops.get(safeIndex);
    }

    public int offsetForColumn(int columnIndex) {
        return caretOffsets.get(safeColumn(columnIndex));
    }

    public int columnForOffset(int sourceOffset) {
        if (sourceOffset <= caretOffsets.getFirst()) {
            return 0;
        }
        for (int i = 1; i < caretOffsets.size(); i++) {
            if (caretOffsets.get(i) >= sourceOffset) {
                return i;
            }
        }
        return caretOffsets.size() - 1;
    }

    private int safeColumn(int columnIndex) {
        return Math.max(0, Math.min(columnIndex, caretStops.size() - 1));
    }

    private static List<Integer> defaultCaretOffsets(int startOffset, int endOffset, List<Double> caretStops) {
        Objects.requireNonNull(caretStops, "caretStops");
        if (caretStops.isEmpty()) {
            return List.of();
        }
        List<Integer> offsets = new ArrayList<>(caretStops.size());
        int span = Math.max(0, endOffset - startOffset);
        for (int i = 0; i < caretStops.size(); i++) {
            offsets.add(startOffset + Math.min(i, span));
        }
        if (!offsets.isEmpty()) {
            offsets.set(offsets.size() - 1, endOffset);
        }
        return List.copyOf(offsets);
    }
}
