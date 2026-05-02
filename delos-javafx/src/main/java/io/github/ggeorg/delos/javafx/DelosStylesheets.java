package io.github.ggeorg.delos.javafx;

import javafx.scene.Scene;

import java.net.URL;
import java.util.Objects;

/** Shared stylesheet installer for Delos JavaFX application chrome. */
public final class DelosStylesheets {
    private static final String DELOS_UI_CSS = "/io/github/ggeorg/delos/javafx/delos-ui.css";

    private DelosStylesheets() {
    }

    public static void addTo(Scene scene) {
        Objects.requireNonNull(scene, "scene");
        URL resource = DelosStylesheets.class.getResource(DELOS_UI_CSS);
        if (resource == null) {
            throw new IllegalStateException("Missing Delos UI stylesheet: " + DELOS_UI_CSS);
        }
        scene.getStylesheets().add(resource.toExternalForm());
    }
}
