package io.github.ggeorg.delos.javafx.icon;

/** Standard icon sizes used by Delos JavaFX chrome. */
public enum DelosIconSize {
    SMALL(14),
    TOOLBAR(16),
    LARGE(20);

    private final int pixels;

    DelosIconSize(int pixels) {
        this.pixels = pixels;
    }

    public int pixels() {
        return pixels;
    }
}
