package io.github.ggeorg.delos.writer.layout;

import java.util.List;
import java.util.Objects;

/**
 * One laid-out visual line with enough geometry for hit testing.
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
        List<Double> caretStops
) {
    public LaidOutLine {
        text = Objects.requireNonNullElse(text, "");
        runs = List.copyOf(Objects.requireNonNull(runs, "runs"));
        caretStops = List.copyOf(Objects.requireNonNull(caretStops, "caretStops"));

        if (caretStops.isEmpty()) {
            throw new IllegalArgumentException("caretStops must contain at least one entry");
        }
        if (startOffset < 0) {
            throw new IllegalArgumentException("startOffset must be >= 0");
        }
        if (endOffset < startOffset) {
            throw new IllegalArgumentException("endOffset must be >= startOffset");
        }
    }

    public int length() {
        return text.length();
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
        int safeIndex = Math.max(0, Math.min(columnIndex, caretStops.size() - 1));
        return x + caretStops.get(safeIndex);
    }
}
