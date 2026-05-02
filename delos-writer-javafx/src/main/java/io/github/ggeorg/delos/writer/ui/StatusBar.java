package io.github.ggeorg.delos.writer.ui;

import io.github.ggeorg.delos.javafx.ZoomMath;
import io.github.ggeorg.delos.javafx.chrome.DelosStatusLine;
import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.document.Paragraph;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import java.util.Objects;

public final class StatusBar extends DelosStatusLine {
    private final Label pageIndicator = statusItem("status-page");
    private final Label wordCountLabel = statusItem("status-words");
    private final Label languageLabel = statusItem("status-language");
    private final Label zoomLabel = statusItem("status-zoom");

    public StatusBar() {
        getStyleClass().add("status-bar");

        HBox zoomBox = new HBox(8, zoomLabel);
        zoomBox.getStyleClass().add("status-zoom-box");
        getChildren().addAll(pageIndicator, wordCountLabel, languageLabel, spacer(), zoomBox);

        setPageInfo(1, 1);
        setWordCount(0);
        setLanguage("English");
        setZoomFactor(1.0);
    }

    public void setPageInfo(int currentPage, int totalPages) {
        int safeTotal = Math.max(1, totalPages);
        int safeCurrent = Math.max(1, Math.min(currentPage, safeTotal));
        pageIndicator.setText("Page " + safeCurrent + " of " + safeTotal);
    }

    public void setWordCount(int words) {
        int safeWords = Math.max(0, words);
        wordCountLabel.setText(safeWords + (safeWords == 1 ? " word" : " words"));
    }

    public void setLanguage(String language) {
        String resolved = language == null || language.isBlank() ? "Unknown" : language.trim();
        languageLabel.setText(resolved);
    }

    /**
     * Backward-compatible status-bar setter used by the existing StatusBarTest.
     */
    public void setZoomText(String zoomText) {
        String resolved = zoomText == null || zoomText.isBlank() ? "100%" : zoomText.trim();
        if (resolved.regionMatches(true, 0, "Zoom:", 0, 5)) {
            zoomLabel.setText(resolved);
        } else {
            zoomLabel.setText("Zoom: " + resolved);
        }
    }

    public void setZoomFactor(double zoomFactor) {
        double percent = ZoomMath.percent(zoomFactor);
        setZoomText((int) percent + "%");
    }

    Label pageIndicatorLabel() { return pageIndicator; }
    Label wordCountLabel() { return wordCountLabel; }
    Label languageLabel() { return languageLabel; }
    Label zoomLabel() { return zoomLabel; }

    public static int countWords(Document document) {
        Objects.requireNonNull(document, "document");
        int words = 0;
        for (Paragraph paragraph : document.paragraphs()) {
            String text = paragraph.plainText();
            if (text == null) {
                continue;
            }
            String trimmed = text.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            for (String token : trimmed.split("\\s+")) {
                if (!token.isEmpty()) {
                    words++;
                }
            }
        }
        return words;
    }
}
