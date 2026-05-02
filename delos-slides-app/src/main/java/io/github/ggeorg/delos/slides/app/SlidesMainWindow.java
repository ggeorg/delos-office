package io.github.ggeorg.delos.slides.app;

import io.github.ggeorg.delos.javafx.command.CommandRegistry;
import io.github.ggeorg.delos.slides.core.PresentationDeck;
import io.github.ggeorg.delos.slides.core.Slide;
import io.github.ggeorg.delos.slides.ui.control.DelosSlidesView;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Objects;

public final class SlidesMainWindow extends BorderPane {
    private final Stage stage;
    private final DelosSlidesView slidesView = new DelosSlidesView();
    private final CommandRegistry commandRegistry = new CommandRegistry();
    private final SlidesMenuBar menuBar;
    private final SlidesToolBar toolBar;
    private final SlidesStatusBar statusBar = new SlidesStatusBar();
    private boolean dirty;
    private boolean loading;

    public SlidesMainWindow(Stage stage) {
        this.stage = Objects.requireNonNull(stage, "stage");
        getStyleClass().add("slides-main-window");

        new SlidesCommandProvider(commandRegistry, this).registerCommands();
        menuBar = new SlidesMenuBar(commandRegistry);
        toolBar = new SlidesToolBar(commandRegistry);

        VBox topChrome = new VBox(menuBar, toolBar);
        topChrome.getStyleClass().add("slides-top-chrome");

        setTop(topChrome);
        setCenter(slidesView);
        setBottom(statusBar);

        slidesView.deckProperty().addListener((ignored, oldDeck, newDeck) -> {
            if (!loading) {
                dirty = true;
                refreshChrome();
            }
        });
        slidesView.selectedSlideIndexProperty().addListener((ignored, oldIndex, newIndex) -> refreshChrome());
        sceneProperty().addListener((ignored, oldScene, newScene) -> {
            uninstallAccelerators(oldScene);
            installAccelerators(newScene);
        });
        refreshChrome();
    }

    public boolean requestClose() {
        return true;
    }

    void newDeck() {
        loading = true;
        try {
            slidesView.setDeck(PresentationDeck.blank());
            slidesView.selectSlide(0);
        } finally {
            loading = false;
        }
        dirty = false;
        refreshChrome();
    }

    void addSlide() {
        PresentationDeck deck = slidesView.getDeck();
        int nextNumber = deck.slides().size() + 1;
        PresentationDeck updated = deck.addSlide(Slide.blank("Slide " + nextNumber));
        slidesView.setDeck(updated);
        slidesView.selectSlide(updated.slides().size() - 1);
    }

    void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(stage);
        alert.setTitle("About Delos Slides");
        alert.setHeaderText("Delos Slides");
        alert.setContentText("Delos Slides is the presentation application in the Delos Office suite.");
        alert.showAndWait();
    }

    private void installAccelerators(Scene scene) {
        commandRegistry.installAccelerators(scene);
    }

    private void uninstallAccelerators(Scene scene) {
        commandRegistry.uninstallAccelerators(scene);
    }

    private void refreshChrome() {
        menuBar.refreshFromCommands();
        toolBar.refreshFromCommands();
        statusBar.update(slidesView.getDeck(), slidesView.getSelectedSlideIndex(), dirty);
        stage.setTitle("Delos Slides — " + slidesView.getDeck().title() + (dirty ? " *" : ""));
    }

    DelosSlidesView slidesView() {
        return slidesView;
    }

    boolean dirty() {
        return dirty;
    }
}
