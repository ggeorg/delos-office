package io.github.ggeorg.delos.slides.ui.control;

import io.github.ggeorg.delos.slides.core.PresentationDeck;
import io.github.ggeorg.delos.slides.core.Slide;
import io.github.ggeorg.delos.slides.core.SlideElement;
import io.github.ggeorg.delos.slides.core.TextBoxElement;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Objects;

/**
 * Minimal JavaFX presentation surface backed by the immutable Slides core model.
 */
public final class DelosSlidesView extends BorderPane {
    private final ObjectProperty<PresentationDeck> deck =
            new SimpleObjectProperty<>(this, "deck", PresentationDeck.blank());
    private final IntegerProperty selectedSlideIndex =
            new SimpleIntegerProperty(this, "selectedSlideIndex", 0);
    private final ListView<String> slideNavigator = new ListView<>();
    private final Pane slideCanvas = new Pane();
    private boolean refreshing;

    public DelosSlidesView() {
        this(PresentationDeck.blank());
    }

    public DelosSlidesView(PresentationDeck deck) {
        this.deck.set(Objects.requireNonNullElseGet(deck, PresentationDeck::blank));
        getStyleClass().add("delos-slides-view");
        configureNavigator();
        configureCanvas();

        ScrollPane canvasScroll = new ScrollPane(slideCanvas);
        canvasScroll.setFitToWidth(false);
        canvasScroll.setFitToHeight(false);
        canvasScroll.getStyleClass().add("delos-slides-canvas-scroll");

        setLeft(slideNavigator);
        setCenter(canvasScroll);

        this.deck.addListener((ignored, oldDeck, newDeck) -> refresh());
        this.selectedSlideIndex.addListener((ignored, oldIndex, newIndex) -> refreshSelectionAndCanvas());
        refresh();
    }

    public PresentationDeck getDeck() {
        return deck.get();
    }

    public void setDeck(PresentationDeck deck) {
        this.deck.set(Objects.requireNonNullElseGet(deck, PresentationDeck::blank));
    }

    public ObjectProperty<PresentationDeck> deckProperty() {
        return deck;
    }

    public int getSelectedSlideIndex() {
        return selectedSlideIndex.get();
    }

    public void selectSlide(int index) {
        if (index < 0 || index >= getDeck().slides().size()) {
            return;
        }
        selectedSlideIndex.set(index);
    }

    public ReadOnlyIntegerProperty selectedSlideIndexProperty() {
        return selectedSlideIndex;
    }

    public Slide selectedSlide() {
        return getDeck().slideAt(getSelectedSlideIndex());
    }

    private void configureNavigator() {
        slideNavigator.getStyleClass().add("delos-slides-navigator");
        slideNavigator.setPrefWidth(180);
        slideNavigator.getSelectionModel().selectedIndexProperty().addListener((ignored, oldIndex, newIndex) -> {
            if (!refreshing && newIndex.intValue() >= 0) {
                selectSlide(newIndex.intValue());
            }
        });
    }

    private void configureCanvas() {
        slideCanvas.getStyleClass().add("delos-slide-canvas");
        slideCanvas.setMinSize(960, 540);
        slideCanvas.setPrefSize(960, 540);
        slideCanvas.setMaxSize(960, 540);
    }

    private void refresh() {
        if (refreshing) {
            return;
        }
        refreshing = true;
        try {
            slideNavigator.getItems().setAll(
                    getDeck().slides().stream()
                            .map(Slide::title)
                            .toList()
            );
            if (getSelectedSlideIndex() >= getDeck().slides().size()) {
                selectedSlideIndex.set(getDeck().slides().size() - 1);
            }
            slideNavigator.getSelectionModel().select(getSelectedSlideIndex());
            refreshCanvas();
        } finally {
            refreshing = false;
        }
    }

    private void refreshSelectionAndCanvas() {
        if (!refreshing) {
            slideNavigator.getSelectionModel().select(getSelectedSlideIndex());
            refreshCanvas();
        }
    }

    private void refreshCanvas() {
        slideCanvas.getChildren().clear();
        VBox content = new VBox(16);
        content.setPadding(new Insets(48));
        content.setAlignment(Pos.TOP_LEFT);
        content.setPrefSize(960, 540);
        content.getStyleClass().add("delos-slide-content");
        VBox.setVgrow(content, Priority.ALWAYS);

        Slide slide = selectedSlide();
        Label title = new Label(slide.title());
        title.getStyleClass().add("delos-slide-title");
        content.getChildren().add(title);

        for (SlideElement element : slide.elements()) {
            if (element instanceof TextBoxElement textBox && !"title".equals(textBox.id())) {
                Label text = new Label(textBox.text());
                text.getStyleClass().add("delos-slide-textbox");
                text.setWrapText(true);
                text.setMaxWidth(Math.max(120, textBox.width()));
                content.getChildren().add(text);
            }
        }
        slideCanvas.getChildren().add(content);
    }
}
