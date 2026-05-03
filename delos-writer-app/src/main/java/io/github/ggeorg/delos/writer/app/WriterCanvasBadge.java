package io.github.ggeorg.delos.writer.app;

import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.document.Paragraph;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.Objects;

/** Viewport-fixed canvas status badge replacing the old bottom status bar. */
final class WriterCanvasBadge extends VBox {
    private final Label badge = new Label();
    private final GridPane popover = new GridPane();
    private int currentPage = 1;
    private int totalPages = 1;
    private int words;
    private int characters;
    private int paragraphs;

    WriterCanvasBadge() {
        getStyleClass().add("writer-canvas-badge-host");
        setAlignment(Pos.CENTER);
        setSpacing(8.0);
        setPickOnBounds(false);
        setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        popover.getStyleClass().add("writer-statistics-popover");
        popover.setHgap(18);
        popover.setVgap(6);
        popover.setVisible(false);
        popover.setManaged(false);

        badge.getStyleClass().add("writer-canvas-badge");
        badge.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                toggleStatisticsPopover();
                event.consume();
            }
        });

        getChildren().addAll(popover, badge);
        updateBadgeText();
        rebuildPopover();
    }

    void update(int currentPage, int totalPages, Document document) {
        Objects.requireNonNull(document, "document");
        this.totalPages = Math.max(1, totalPages);
        this.currentPage = Math.max(1, Math.min(currentPage, this.totalPages));
        Statistics statistics = Statistics.from(document);
        this.words = statistics.words();
        this.characters = statistics.characters();
        this.paragraphs = statistics.paragraphs();
        updateBadgeText();
        rebuildPopover();
    }

    void showStatisticsPopover() {
        popover.setManaged(true);
        popover.setVisible(true);
    }

    void hideStatisticsPopover() {
        popover.setVisible(false);
        popover.setManaged(false);
    }

    void toggleStatisticsPopover() {
        if (popover.isVisible()) {
            hideStatisticsPopover();
        } else {
            showStatisticsPopover();
        }
    }

    private void updateBadgeText() {
        badge.setText("Page " + currentPage + " of " + totalPages + " · " + words + (words == 1 ? " word" : " words"));
    }

    private void rebuildPopover() {
        popover.getChildren().clear();
        addStat(0, "Words", String.valueOf(words));
        addStat(1, "Characters", String.valueOf(characters));
        addStat(2, "Paragraphs", String.valueOf(paragraphs));
        addStat(3, "Pages", String.valueOf(totalPages));
        addStat(4, "Reading time", readingTimeLabel(words));
    }

    private void addStat(int row, String label, String value) {
        Label labelNode = new Label(label);
        labelNode.getStyleClass().add("writer-statistics-label");
        Label valueNode = new Label(value);
        valueNode.getStyleClass().add("writer-statistics-value");
        popover.add(labelNode, 0, row);
        popover.add(valueNode, 1, row);
    }

    private static String readingTimeLabel(int words) {
        int minutes = Math.max(1, (int) Math.ceil(words / 225.0));
        return "~" + minutes + " min";
    }

    private record Statistics(int words, int characters, int paragraphs) {
        static Statistics from(Document document) {
            Objects.requireNonNull(document, "document");
            int words = 0;
            int characters = 0;
            int paragraphs = 0;
            for (Paragraph paragraph : document.paragraphs()) {
                paragraphs++;
                String text = paragraph.plainText();
                if (text == null) {
                    continue;
                }
                characters += text.length();
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
            return new Statistics(words, characters, paragraphs);
        }
    }
}
