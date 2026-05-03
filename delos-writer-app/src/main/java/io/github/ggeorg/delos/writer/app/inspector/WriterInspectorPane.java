package io.github.ggeorg.delos.writer.app.inspector;

import io.github.ggeorg.delos.javafx.command.CommandRegistry;
import io.github.ggeorg.delos.javafx.inspector.DelosInspector;
import io.github.ggeorg.delos.javafx.inspector.InspectorSection;
import io.github.ggeorg.delos.writer.session.EditorSession;
import io.github.ggeorg.delos.writer.ui.control.DelosEditor;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.Objects;

/** Writer-owned content mounted inside the shared Delos inspector shell. */
public final class WriterInspectorPane extends DelosInspector {
    private final WriterPageSetupInspector pageSetupInspector;
    private final WriterTextFormatInspector textFormatInspector;
    private final WriterImageInspector imageInspector;
    private final WriterTableInspector tableInspector;

    public WriterInspectorPane(EditorSession session, DelosEditor editor, CommandRegistry commandRegistry) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(editor, "editor");
        Objects.requireNonNull(commandRegistry, "commandRegistry");

        getStyleClass().add("writer-inspector");
        pageSetupInspector = new WriterPageSetupInspector(session, editor);
        textFormatInspector = new WriterTextFormatInspector(editor, commandRegistry);
        imageInspector = new WriterImageInspector(editor, commandRegistry);
        tableInspector = new WriterTableInspector(editor, commandRegistry);

        addTab("style", "Style", formatPanel());
        addTab("layout", "Layout", layoutPanel());
        addTab("document", "Document", documentPanel());
        selectTab("style");
        refresh();
    }

    public void refresh() {
        pageSetupInspector.refresh();
        textFormatInspector.refresh();
        imageInspector.refresh();
        tableInspector.refresh();

        boolean hasTable = tableInspector.hasSelectedTable();
        boolean hasImage = !hasTable && imageInspector.hasSelectedImage();
        setInspectorVisible(textFormatInspector, !hasImage && !hasTable);
        setInspectorVisible(imageInspector, hasImage);
        setInspectorVisible(tableInspector, hasTable);
    }

    private VBox formatPanel() {
        VBox panel = new VBox(0, textFormatInspector, imageInspector, tableInspector);
        panel.getStyleClass().add("writer-format-inspector");
        return panel;
    }

    private VBox layoutPanel() {
        VBox panel = panel();
        panel.getChildren().add(pageSetupInspector);
        return panel;
    }

    private VBox documentPanel() {
        VBox panel = panel();
        InspectorSection placeholder = new InspectorSection("Document");
        Label empty = new Label("Document statistics and metadata coming soon.");
        empty.getStyleClass().add("delos-inspector-empty-text");
        placeholder.add(empty);
        panel.getChildren().add(placeholder);
        return panel;
    }

    private static void setInspectorVisible(javafx.scene.Node inspector, boolean visible) {
        inspector.setManaged(visible);
        inspector.setVisible(visible);
    }

    private static VBox panel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(0));
        return panel;
    }
}
