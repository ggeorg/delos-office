package io.github.ggeorg.delos.javafx.icon;

import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Objects;

/** JavaFX factory for Delos icon ids. */
public final class DelosIcons {
    /**
     * Deliberately conservative Material Design fallback.
     * <p>
     * Ikonli pack descriptions vary slightly between icon packs/versions. A
     * single bad literal must never crash a Delos application during startup.
     */
    private static final String FALLBACK_ICON_LITERAL = "mdi-help-circle";

    private DelosIcons() {
    }

    public static Node toolbarIcon(DelosIconId iconId) {
        Objects.requireNonNull(iconId, "iconId");
        if (iconId == DelosIconId.LEFT_SIDEBAR) {
            return leftSidebarIcon();
        }
        return icon(iconId, DelosIconSize.TOOLBAR);
    }

    public static FontIcon icon(DelosIconId iconId, DelosIconSize size) {
        Objects.requireNonNull(iconId, "iconId");
        Objects.requireNonNull(size, "size");

        FontIcon icon = new FontIcon();
        setIconLiteralSafely(icon, iconId.iconLiteral());
        icon.setIconSize(size.pixels());
        icon.getStyleClass().add("delos-icon");
        return icon;
    }


    private static Node leftSidebarIcon() {
        Pane pane = new Pane();
        pane.getStyleClass().addAll("delos-icon", "delos-left-sidebar-icon");
        pane.setMinSize(20.0, 20.0);
        pane.setPrefSize(20.0, 20.0);
        pane.setMaxSize(20.0, 20.0);

        Rectangle outline = new Rectangle(3.0, 3.0, 14.0, 14.0);
        outline.setArcWidth(2.0);
        outline.setArcHeight(2.0);
        outline.setFill(Color.TRANSPARENT);
        outline.setStroke(Color.web("#263445"));
        outline.setStrokeWidth(1.8);

        Rectangle sidebar = new Rectangle(4.8, 4.8, 4.2, 10.4);
        sidebar.setArcWidth(1.2);
        sidebar.setArcHeight(1.2);
        sidebar.setFill(Color.web("#263445"));

        Rectangle divider = new Rectangle(10.3, 4.8, 1.3, 10.4);
        divider.setFill(Color.web("#263445"));
        divider.setOpacity(0.75);

        pane.getChildren().setAll(outline, sidebar, divider);
        return pane;
    }

    private static void setIconLiteralSafely(FontIcon icon, String iconLiteral) {
        try {
            icon.setIconLiteral(iconLiteral);
        } catch (IllegalArgumentException firstFailure) {
            try {
                icon.setIconLiteral(FALLBACK_ICON_LITERAL);
            } catch (IllegalArgumentException ignored) {
                // Leave the FontIcon empty rather than failing application startup.
            }
        }
    }
}
