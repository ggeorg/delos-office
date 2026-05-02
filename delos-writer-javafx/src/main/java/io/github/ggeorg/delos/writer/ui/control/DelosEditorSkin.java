package io.github.ggeorg.delos.writer.ui.control;

import io.github.ggeorg.delos.writer.ui.DocumentViewport;
import javafx.scene.control.SkinBase;

/**
 * JavaFX skin for {@link DelosEditor}.
 * <p>
 * The skin owns the viewport implementation. This keeps the public control API
 * stable while the editor surface is still being decomposed internally.
 */
public final class DelosEditorSkin extends SkinBase<DelosEditor> {
    private final DocumentViewport viewport;

    DelosEditorSkin(DelosEditor control) {
        super(control);
        this.viewport = new DocumentViewport(control.session());
        control.attachViewport(viewport);
        getChildren().add(viewport);
    }

    @Override
    public void dispose() {
        getSkinnable().detachViewport(viewport);
        super.dispose();
    }

    @Override
    protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        return leftInset + viewport.prefWidth(height) + rightInset;
    }

    @Override
    protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        return topInset + viewport.prefHeight(width) + bottomInset;
    }

    @Override
    protected void layoutChildren(double contentX, double contentY, double contentWidth, double contentHeight) {
        viewport.resizeRelocate(contentX, contentY, contentWidth, contentHeight);
    }
}
