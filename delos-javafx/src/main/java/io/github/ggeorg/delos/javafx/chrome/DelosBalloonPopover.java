package io.github.ggeorg.delos.javafx.chrome;

import javafx.geometry.Side;
import javafx.scene.Node;
import org.controlsfx.control.PopOver;

import java.util.Objects;

/**
 * Shared Delos wrapper around ControlsFX {@link PopOver}.
 * <p>
 * Application modules should depend on this class instead of ControlsFX directly
 * so Delos can replace or customize the backing implementation later.
 */
public final class DelosBalloonPopover {
    private final PopOver popOver;

    public DelosBalloonPopover(Node content) {
        Objects.requireNonNull(content, "content");
        content.getStyleClass().add("delos-balloon-popover-content");

        popOver = new PopOver(content);
        popOver.setAutoFix(true);
        popOver.setAutoHide(true);
        popOver.setDetachable(false);
        popOver.setHeaderAlwaysVisible(false);
        popOver.setHideOnEscape(true);
        popOver.setArrowLocation(PopOver.ArrowLocation.TOP_CENTER);
        popOver.getStyleClass().add("delos-balloon-popover");
    }

    public void showBelow(Node owner) {
        Objects.requireNonNull(owner, "owner");
        popOver.setArrowLocation(PopOver.ArrowLocation.TOP_CENTER);
        popOver.show(owner);
    }

    public void show(Node owner, Side side) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(side, "side");
        popOver.setArrowLocation(arrowLocationFor(side));
        popOver.show(owner);
    }

    public void hide() {
        popOver.hide();
    }

    public boolean isShowing() {
        return popOver.isShowing();
    }

    public Node content() {
        return popOver.getContentNode();
    }

    PopOver popOver() {
        return popOver;
    }

    private static PopOver.ArrowLocation arrowLocationFor(Side side) {
        return switch (side) {
            case TOP -> PopOver.ArrowLocation.BOTTOM_CENTER;
            case RIGHT -> PopOver.ArrowLocation.LEFT_CENTER;
            case BOTTOM -> PopOver.ArrowLocation.TOP_CENTER;
            case LEFT -> PopOver.ArrowLocation.RIGHT_CENTER;
        };
    }
}
