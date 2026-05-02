package io.github.ggeorg.delos.writer.ui.control;

import javafx.scene.Scene;

import java.net.URL;
import java.util.Objects;

/**
 * Stylesheet installer for reusable Writer JavaFX controls.
 *
 * <p>Application shells should load this stylesheet in addition to the shared
 * Delos stylesheet and their own app-specific stylesheet. Keeping Writer
 * control styles here lets third-party apps embed {@link WriterDocumentView}
 * without copying the Delos Writer desktop app stylesheet.</p>
 */
public final class WriterStylesheets {
    private static final String WRITER_CONTROLS_CSS = "/io/github/ggeorg/delos/writer/ui/control/writer-controls.css";

    private WriterStylesheets() {
    }

    public static void addTo(Scene scene) {
        Objects.requireNonNull(scene, "scene");
        URL resource = WriterStylesheets.class.getResource(WRITER_CONTROLS_CSS);
        if (resource == null) {
            throw new IllegalStateException("Missing Writer controls stylesheet: " + WRITER_CONTROLS_CSS);
        }
        String stylesheet = resource.toExternalForm();
        if (!scene.getStylesheets().contains(stylesheet)) {
            scene.getStylesheets().add(stylesheet);
        }
    }
}
