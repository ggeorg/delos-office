package io.github.ggeorg.delos.slides.app;

import io.github.ggeorg.delos.javafx.chrome.DelosStatusLine;
import io.github.ggeorg.delos.slides.core.PresentationDeck;
import javafx.scene.control.Label;

final class SlidesStatusBar extends DelosStatusLine {
    private final Label statusLabel = statusItem();
    private final Label slideLabel = statusItem();
    private final Label dirtyLabel = statusItem();

    SlidesStatusBar() {
        getStyleClass().add("slides-status-bar");
        statusLabel.setText("Ready");
        getChildren().setAll(statusLabel, spacer(), slideLabel, dirtyLabel);
    }

    void update(PresentationDeck deck, int selectedSlideIndex, boolean dirty) {
        statusLabel.setText("Ready");
        slideLabel.setText("Slide " + (selectedSlideIndex + 1) + " of " + deck.slides().size());
        dirtyLabel.setText(dirty ? "Modified" : "Saved");
    }
}
