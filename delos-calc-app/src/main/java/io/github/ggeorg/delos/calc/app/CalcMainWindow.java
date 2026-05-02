package io.github.ggeorg.delos.calc.app;

import io.github.ggeorg.delos.calc.core.Workbook;
import io.github.ggeorg.delos.calc.ui.control.DelosSpreadsheet;
import io.github.ggeorg.delos.javafx.command.CommandRegistry;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

public final class CalcMainWindow extends BorderPane {
    private final Stage stage;
    private final CalcFileService fileService = new CalcFileService();
    private final DelosSpreadsheet spreadsheet = new DelosSpreadsheet();
    private final CommandRegistry commandRegistry = new CommandRegistry();
    private final CalcMenuBar menuBar;
    private final CalcToolBar toolBar;
    private final CalcStatusBar statusBar;
    private final VBox topChrome;
    private Path currentFile;
    private boolean dirty;
    private boolean loading;

    public CalcMainWindow(Stage stage) {
        this.stage = Objects.requireNonNull(stage, "stage");
        getStyleClass().add("calc-main-window");

        new CalcCommandProvider(
                commandRegistry,
                this::newWorkbook,
                this::openWorkbook,
                this::saveWorkbook,
                this::saveWorkbookAs,
                this::clearSelectedCell,
                this::canClearSelectedCell,
                this::exitApplication,
                this::showAboutDialog
        ).registerCommands();

        menuBar = new CalcMenuBar(commandRegistry);
        toolBar = new CalcToolBar(commandRegistry);
        topChrome = new VBox(menuBar, toolBar);
        topChrome.getStyleClass().add("calc-top-chrome");
        statusBar = new CalcStatusBar();

        setTop(topChrome);
        setCenter(spreadsheet);
        setBottom(statusBar);

        spreadsheet.workbookProperty().addListener((obs, oldWorkbook, newWorkbook) -> {
            if (!loading) {
                dirty = true;
            }
            refreshChrome();
        });
        spreadsheet.selectedCellProperty().addListener((obs, oldCell, newCell) -> refreshChrome());
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            uninstallAccelerators(oldScene);
            installAccelerators(newScene);
        });
        refreshChrome();
    }

    private void installAccelerators(Scene scene) {
        commandRegistry.installAccelerators(scene);
    }

    private void uninstallAccelerators(Scene scene) {
        commandRegistry.uninstallAccelerators(scene);
    }

    private void newWorkbook() {
        if (!confirmAbandonChanges()) {
            return;
        }
        currentFile = null;
        setWorkbook(Workbook.blank(), false);
    }

    private void openWorkbook() {
        if (!confirmAbandonChanges()) {
            return;
        }
        try {
            CalcFileService.LoadedWorkbook loaded = fileService.open(stage, currentFile);
            if (loaded == null) {
                return;
            }
            currentFile = loaded.path();
            setWorkbook(loaded.workbook(), false);
        } catch (IOException exception) {
            showError("Open failed", "Delos Calc could not open the selected spreadsheet.", exception);
        }
    }

    private void saveWorkbook() {
        saveCurrentWorkbook(false);
    }

    private void saveWorkbookAs() {
        saveCurrentWorkbook(true);
    }

    private boolean saveCurrentWorkbook(boolean saveAs) {
        try {
            Path savedFile = fileService.save(stage, currentFile, spreadsheet.getWorkbook(), saveAs);
            if (savedFile == null) {
                return false;
            }
            currentFile = savedFile;
            dirty = false;
            refreshChrome();
            return true;
        } catch (IOException exception) {
            showError("Save failed", "Delos Calc could not save the current spreadsheet.", exception);
            return false;
        }
    }

    private void clearSelectedCell() {
        Workbook current = spreadsheet.getWorkbook();
        Workbook updated = current.withSheet(current.firstSheet().clear(spreadsheet.getSelectedCell()));
        spreadsheet.setWorkbook(updated);
    }

    private boolean canClearSelectedCell() {
        return !spreadsheet.getWorkbook().firstSheet().cellAt(spreadsheet.getSelectedCell()).content().isBlank();
    }

    private void exitApplication() {
        if (requestClose()) {
            stage.close();
        }
    }

    private void setWorkbook(Workbook workbook, boolean dirty) {
        loading = true;
        try {
            spreadsheet.setWorkbook(workbook);
        } finally {
            loading = false;
        }
        this.dirty = dirty;
        refreshChrome();
    }

    public boolean requestClose() {
        return confirmAbandonChanges();
    }

    private boolean confirmAbandonChanges() {
        if (!dirty) {
            return true;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(stage);
        alert.setTitle("Unsaved changes");
        alert.setHeaderText("Save changes before continuing?");
        alert.setContentText("Delos Calc has unsaved changes in “" + displayName() + "”.");
        ButtonType save = new ButtonType("Save");
        ButtonType discard = new ButtonType("Discard");
        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(save, discard, cancel);
        Button discardButton = (Button) alert.getDialogPane().lookupButton(discard);
        if (discardButton != null) {
            discardButton.getStyleClass().add("danger-button");
        }
        ButtonType decision = alert.showAndWait().orElse(cancel);
        if (decision == save) {
            return saveCurrentWorkbook(false);
        }
        return decision == discard;
    }

    private void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(stage);
        alert.setTitle("About Delos Calc");
        alert.setHeaderText("Delos Calc");
        alert.setContentText("Delos Calc is the spreadsheet application in the Delos Office suite.");
        alert.showAndWait();
    }

    private void refreshChrome() {
        stage.setTitle("Delos Calc — " + displayName() + (dirty ? " *" : ""));
        menuBar.refreshFromCommands();
        toolBar.refreshFromCommands();
        statusBar.update(spreadsheet.getWorkbook(), spreadsheet.getSelectedCell(), dirty);
    }

    private String displayName() {
        return currentFile == null ? spreadsheet.getWorkbook().title() : currentFile.getFileName().toString();
    }

    CommandRegistry commandRegistry() { return commandRegistry; }
    DelosSpreadsheet spreadsheet() { return spreadsheet; }
    CalcMenuBar menuBar() { return menuBar; }
    CalcToolBar toolBar() { return toolBar; }
    CalcStatusBar statusBar() { return statusBar; }
    Path currentFile() { return currentFile; }
    boolean dirty() { return dirty; }

    private void showError(String title, String header, Exception exception) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(stage);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(exception.getMessage());
        alert.showAndWait();
    }
}
