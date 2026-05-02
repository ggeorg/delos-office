package io.github.ggeorg.delos.base.app;

import io.github.ggeorg.delos.base.core.DatabaseObject;
import io.github.ggeorg.delos.base.core.DatabaseProject;
import io.github.ggeorg.delos.base.ui.control.DelosBaseView;
import io.github.ggeorg.delos.javafx.command.CommandRegistry;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Objects;

public final class BaseMainWindow extends BorderPane {
    private final Stage stage;
    private final DelosBaseView baseView = new DelosBaseView();
    private final CommandRegistry commandRegistry = new CommandRegistry();
    private final BaseMenuBar menuBar;
    private final BaseToolBar toolBar;
    private final BaseStatusBar statusBar = new BaseStatusBar();
    private boolean dirty;
    private boolean loading;

    public BaseMainWindow(Stage stage) {
        this.stage = Objects.requireNonNull(stage, "stage");
        getStyleClass().add("base-main-window");

        new BaseCommandProvider(commandRegistry, this).registerCommands();
        menuBar = new BaseMenuBar(commandRegistry);
        toolBar = new BaseToolBar(commandRegistry);

        VBox topChrome = new VBox(menuBar, toolBar);
        topChrome.getStyleClass().add("base-top-chrome");

        setTop(topChrome);
        setCenter(baseView);
        setBottom(statusBar);

        baseView.projectProperty().addListener((ignored, oldProject, newProject) -> {
            if (!loading) {
                dirty = true;
                refreshChrome();
            }
        });
        baseView.selectedObjectProperty().addListener((ignored, oldObject, newObject) -> refreshChrome());
        sceneProperty().addListener((ignored, oldScene, newScene) -> {
            uninstallAccelerators(oldScene);
            installAccelerators(newScene);
        });
        refreshChrome();
    }

    public boolean requestClose() {
        return true;
    }

    void newProject() {
        loading = true;
        try {
            baseView.setProject(DatabaseProject.blank());
        } finally {
            loading = false;
        }
        dirty = false;
        refreshChrome();
    }

    void addTable() {
        addObject(DatabaseObject.table(nextName("Table")));
    }

    void addQuery() {
        addObject(DatabaseObject.query(nextName("Query")));
    }

    void addForm() {
        addObject(DatabaseObject.form(nextName("Form")));
    }

    void addReport() {
        addObject(DatabaseObject.report(nextName("Report")));
    }

    void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(stage);
        alert.setTitle("About Delos Base");
        alert.setHeaderText("Delos Base");
        alert.setContentText("Delos Base is the database application in the Delos Office suite.");
        alert.showAndWait();
    }

    private void addObject(DatabaseObject object) {
        baseView.setProject(baseView.getProject().addObject(object));
    }

    private String nextName(String prefix) {
        int number = 1;
        while (baseView.getProject().findObject(kindForPrefix(prefix), prefix + number).isPresent()) {
            number++;
        }
        return prefix + number;
    }

    private io.github.ggeorg.delos.base.core.DatabaseObjectKind kindForPrefix(String prefix) {
        return switch (prefix) {
            case "Table" -> io.github.ggeorg.delos.base.core.DatabaseObjectKind.TABLE;
            case "Query" -> io.github.ggeorg.delos.base.core.DatabaseObjectKind.QUERY;
            case "Form" -> io.github.ggeorg.delos.base.core.DatabaseObjectKind.FORM;
            case "Report" -> io.github.ggeorg.delos.base.core.DatabaseObjectKind.REPORT;
            default -> throw new IllegalArgumentException("Unsupported prefix: " + prefix);
        };
    }

    private void installAccelerators(Scene scene) {
        commandRegistry.installAccelerators(scene);
    }

    private void uninstallAccelerators(Scene scene) {
        commandRegistry.uninstallAccelerators(scene);
    }

    private void refreshChrome() {
        menuBar.refreshFromCommands();
        toolBar.refreshFromCommands();
        statusBar.update(baseView.getProject(), baseView.getSelectedObject(), dirty);
        stage.setTitle("Delos Base — " + baseView.getProject().title() + (dirty ? " *" : ""));
    }

    DelosBaseView baseView() {
        return baseView;
    }

    boolean dirty() {
        return dirty;
    }
}
