package io.github.ggeorg.delos.base.app;

import io.github.ggeorg.delos.base.core.DatabaseObject;
import io.github.ggeorg.delos.base.core.DatabaseProject;
import io.github.ggeorg.delos.javafx.chrome.DelosStatusLine;
import javafx.scene.control.Label;

final class BaseStatusBar extends DelosStatusLine {
    private final Label statusLabel = statusItem();
    private final Label objectLabel = statusItem();
    private final Label dirtyLabel = statusItem();

    BaseStatusBar() {
        getStyleClass().add("base-status-bar");
        statusLabel.setText("Ready");
        getChildren().setAll(statusLabel, spacer(), objectLabel, dirtyLabel);
    }

    void update(DatabaseProject project, DatabaseObject selectedObject, boolean dirty) {
        statusLabel.setText("Ready");
        objectLabel.setText(selectedObject == null
                ? project.objectCount() + " objects"
                : selectedObject.displayName());
        dirtyLabel.setText(dirty ? "Modified" : "Saved");
    }
}
