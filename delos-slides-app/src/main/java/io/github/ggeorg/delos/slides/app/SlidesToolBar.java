package io.github.ggeorg.delos.slides.app;

import io.github.ggeorg.delos.javafx.chrome.DelosToolBars;
import io.github.ggeorg.delos.javafx.command.CommandRegistry;
import io.github.ggeorg.delos.javafx.icon.DelosIconId;
import javafx.scene.Node;
import javafx.scene.control.Separator;
import javafx.scene.control.ToolBar;

import java.util.Objects;

final class SlidesToolBar extends ToolBar {
    private final CommandRegistry commandRegistry;

    SlidesToolBar(CommandRegistry commandRegistry) {
        this.commandRegistry = Objects.requireNonNull(commandRegistry, "commandRegistry");
        DelosToolBars.configure(this, "slides-toolbar");
        getItems().setAll(
                button("file.new", DelosIconId.NEW, "New"),
                button("file.open", DelosIconId.OPEN, "Open"),
                button("file.save", DelosIconId.SAVE, "Save"),
                separator(),
                button("insert.slide", DelosIconId.SLIDE, "Slide"),
                button("insert.textBox", DelosIconId.TEXT_BOX, "Text"),
                button("insert.image", DelosIconId.IMAGE, "Image"),
                button("insert.shape", DelosIconId.SHAPE, "Shape"),
                separator(),
                button("format.slideLayout", DelosIconId.SLIDE, "Layout"),
                button("format.theme", DelosIconId.THEME, "Theme"),
                separator(),
                button("slideshow.start", DelosIconId.PLAY, "Play")
        );
        refreshFromCommands();
    }

    void refreshFromCommands() {
        DelosToolBars.refresh(this);
    }

    private Node button(String commandId, DelosIconId iconId, String displayText) {
        Node button = DelosToolBars.button(commandRegistry, commandId, iconId, displayText);
        button.getStyleClass().add("slides-toolbar-button");
        return button;
    }

    private static Separator separator() {
        return DelosToolBars.separator();
    }
}
