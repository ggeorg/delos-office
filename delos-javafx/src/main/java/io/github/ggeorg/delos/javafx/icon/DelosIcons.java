package io.github.ggeorg.delos.javafx.icon;

import javafx.scene.Node;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Objects;

/** JavaFX factory for Delos icon ids. */
public final class DelosIcons {
    private DelosIcons() {
    }

    public static Node toolbarIcon(DelosIconId iconId) {
        return icon(iconId, DelosIconSize.TOOLBAR);
    }

    public static FontIcon icon(DelosIconId iconId, DelosIconSize size) {
        Objects.requireNonNull(iconId, "iconId");
        Objects.requireNonNull(size, "size");

        FontIcon icon = new FontIcon();
        icon.setIconLiteral(iconId.iconLiteral());
        icon.setIconSize(size.pixels());
        icon.getStyleClass().add("delos-icon");
        return icon;
    }
}
