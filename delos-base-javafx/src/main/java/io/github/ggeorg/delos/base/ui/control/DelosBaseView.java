package io.github.ggeorg.delos.base.ui.control;

import io.github.ggeorg.delos.base.core.DatabaseObject;
import io.github.ggeorg.delos.base.core.DatabaseObjectKind;
import io.github.ggeorg.delos.base.core.DatabaseProject;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Objects;

/**
 * Minimal JavaFX project browser backed by the immutable Base core model.
 */
public final class DelosBaseView extends BorderPane {
    private final ObjectProperty<DatabaseProject> project =
            new SimpleObjectProperty<>(this, "project", DatabaseProject.blank());
    private final ObjectProperty<DatabaseObject> selectedObject =
            new SimpleObjectProperty<>(this, "selectedObject");
    private final ListView<DatabaseObject> objectList = new ListView<>();
    private final Label detailTitle = new Label("No object selected");
    private final Label detailDescription = new Label("Create or select a table, query, form, or report.");
    private boolean refreshing;

    public DelosBaseView() {
        this(DatabaseProject.blank());
    }

    public DelosBaseView(DatabaseProject project) {
        this.project.set(Objects.requireNonNullElseGet(project, DatabaseProject::blank));
        getStyleClass().add("delos-base-view");
        configureObjectList();

        VBox left = new VBox(8, sectionLabel("Objects"), objectList);
        left.getStyleClass().add("delos-base-sidebar");
        left.setPadding(new Insets(12));
        left.setPrefWidth(240);
        VBox.setVgrow(objectList, Priority.ALWAYS);

        VBox detail = new VBox(12, detailTitle, detailDescription);
        detail.getStyleClass().add("delos-base-detail");
        detail.setPadding(new Insets(24));
        detail.setAlignment(Pos.TOP_LEFT);
        detailTitle.getStyleClass().add("delos-base-detail-title");
        detailDescription.getStyleClass().add("delos-base-detail-description");
        detailDescription.setWrapText(true);

        setLeft(left);
        setCenter(detail);

        this.project.addListener((ignored, oldProject, newProject) -> refresh());
        this.selectedObject.addListener((ignored, oldObject, newObject) -> refreshDetail());
        refresh();
    }

    public DatabaseProject getProject() {
        return project.get();
    }

    public void setProject(DatabaseProject project) {
        this.project.set(Objects.requireNonNullElseGet(project, DatabaseProject::blank));
    }

    public ObjectProperty<DatabaseProject> projectProperty() {
        return project;
    }

    public DatabaseObject getSelectedObject() {
        return selectedObject.get();
    }

    public ReadOnlyObjectProperty<DatabaseObject> selectedObjectProperty() {
        return selectedObject;
    }

    private void configureObjectList() {
        objectList.getStyleClass().add("delos-base-object-list");
        objectList.setCellFactory(list -> new ObjectCell());
        objectList.getSelectionModel().selectedItemProperty().addListener((ignored, oldObject, newObject) -> {
            if (!refreshing) {
                selectedObject.set(newObject);
            }
        });
    }

    private Label sectionLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("delos-base-section-label");
        return label;
    }

    private void refresh() {
        refreshing = true;
        try {
            objectList.getItems().setAll(getProject().objects());
            if (!objectList.getItems().contains(selectedObject.get())) {
                selectedObject.set(objectList.getItems().isEmpty() ? null : objectList.getItems().getFirst());
            }
            objectList.getSelectionModel().select(selectedObject.get());
            refreshDetail();
        } finally {
            refreshing = false;
        }
    }

    private void refreshDetail() {
        DatabaseObject object = selectedObject.get();
        if (object == null) {
            detailTitle.setText("No object selected");
            detailDescription.setText("Create or select a table, query, form, or report.");
            return;
        }
        detailTitle.setText(object.name());
        detailDescription.setText(object.kind().displayName() + " are part of the Delos Base project model. "
                + "The editing surface will be introduced when Base becomes an active workstream.");
    }

    private static final class ObjectCell extends ListCell<DatabaseObject> {
        @Override
        protected void updateItem(DatabaseObject item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            Label kind = new Label(shortKind(item.kind()));
            kind.getStyleClass().add("delos-base-object-kind");
            Label name = new Label(item.name());
            name.getStyleClass().add("delos-base-object-name");
            HBox row = new HBox(8, kind, name);
            row.setAlignment(Pos.CENTER_LEFT);
            setGraphic(row);
            setText(null);
        }

        private static String shortKind(DatabaseObjectKind kind) {
            return switch (kind) {
                case TABLE -> "T";
                case QUERY -> "Q";
                case FORM -> "F";
                case REPORT -> "R";
                case RELATIONSHIP -> "L";
            };
        }
    }
}
