package io.github.ggeorg.delos.writer.ui;

/**
 * Computes scroll targets that keep a caret-ish rectangle comfortably visible.
 */
public final class ScrollIntoViewCoordinator {
    private ScrollIntoViewCoordinator() {
    }

    public static double adjustedOffset(double currentOffset, double viewportExtent, double contentExtent, double targetMin, double targetMax, double margin) {
        if (viewportExtent <= 0 || contentExtent <= viewportExtent) {
            return 0.0;
        }

        double maxOffset = Math.max(0.0, contentExtent - viewportExtent);
        double safeMargin = Math.min(Math.max(0.0, margin), viewportExtent / 2.0);
        double current = clamp(currentOffset, 0.0, maxOffset);
        double visibleMin = current + safeMargin;
        double visibleMax = current + viewportExtent - safeMargin;

        if (targetMin >= visibleMin && targetMax <= visibleMax) {
            return current;
        }
        if (targetMin < visibleMin) {
            return clamp(targetMin - safeMargin, 0.0, maxOffset);
        }
        if (targetMax > visibleMax) {
            return clamp(current + (targetMax - visibleMax), 0.0, maxOffset);
        }
        return current;
    }

    public static double currentOffset(double normalizedValue, double viewportExtent, double contentExtent) {
        double maxOffset = Math.max(0.0, contentExtent - viewportExtent);
        if (maxOffset == 0.0) {
            return 0.0;
        }
        return clamp(normalizedValue, 0.0, 1.0) * maxOffset;
    }

    public static double normalizedValue(double offset, double viewportExtent, double contentExtent) {
        double maxOffset = Math.max(0.0, contentExtent - viewportExtent);
        if (maxOffset == 0.0) {
            return 0.0;
        }
        return clamp(offset / maxOffset, 0.0, 1.0);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
