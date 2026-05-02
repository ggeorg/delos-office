package io.github.ggeorg.delos.writer.app;

import io.github.ggeorg.delos.writer.app.io.DocumentFileService;
import io.github.ggeorg.delos.writer.app.io.WriterFileChoosers;
import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.pdf.WriterPdfService;
import io.github.ggeorg.delos.writer.print.PdfPrintOptions;
import io.github.ggeorg.delos.writer.print.PdfWriterPrintService;
import io.github.ggeorg.delos.writer.session.EditorSession;
import io.github.ggeorg.delos.javafx.UnsavedChangesCoordinator;
import io.github.ggeorg.delos.writer.ui.control.DelosEditor;
import io.github.ggeorg.delos.writer.ui.control.WriterLayoutSnapshot;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.print.PrinterException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

final class WriterFileController {
    private static final FileChooser.ExtensionFilter PDF_FILTER =
            new FileChooser.ExtensionFilter("PDF Documents (*.pdf)", "*.pdf");
    private static final ExecutorService PRINT_EXECUTOR = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "delos-writer-print");
        thread.setDaemon(true);
        return thread;
    });

    private final Stage stage;
    private final EditorSession session;
    private final DelosEditor editor;
    private final WriterFileService fileService;
    private final DocumentFileService exportService;
    private final WriterPdfService pdfService;
    private final PdfWriterPrintService printService;
    private final Runnable refreshChrome;
    private final AtomicBoolean printing = new AtomicBoolean(false);

    private Path currentFile;

    WriterFileController(Stage stage, EditorSession session, DelosEditor editor, Runnable refreshChrome) {
        this(stage, session, editor, new WriterFileService(), new DocumentFileService(),
                new WriterPdfService(), new PdfWriterPrintService(), refreshChrome);
    }

    WriterFileController(
            Stage stage,
            EditorSession session,
            DelosEditor editor,
            WriterFileService fileService,
            DocumentFileService exportService,
            Runnable refreshChrome
    ) {
        this(stage, session, editor, fileService, exportService,
                new WriterPdfService(), new PdfWriterPrintService(), refreshChrome);
    }

    WriterFileController(
            Stage stage,
            EditorSession session,
            DelosEditor editor,
            WriterFileService fileService,
            DocumentFileService exportService,
            WriterPdfService pdfService,
            PdfWriterPrintService printService,
            Runnable refreshChrome
    ) {
        this.stage = Objects.requireNonNull(stage, "stage");
        this.session = Objects.requireNonNull(session, "session");
        this.editor = Objects.requireNonNull(editor, "editor");
        this.fileService = Objects.requireNonNull(fileService, "fileService");
        this.exportService = Objects.requireNonNull(exportService, "exportService");
        this.pdfService = Objects.requireNonNull(pdfService, "pdfService");
        this.printService = Objects.requireNonNull(printService, "printService");
        this.refreshChrome = Objects.requireNonNull(refreshChrome, "refreshChrome");
    }

    void newDocument() {
        if (!confirmAbandonChanges()) {
            return;
        }
        currentFile = null;
        session.loadDocument(Document.blank());
        editor.reloadDocument();
        refreshChrome.run();
    }

    void openDocument() {
        if (!confirmAbandonChanges()) {
            return;
        }
        try {
            WriterFileService.LoadedWriterDocument loaded = fileService.open(stage, currentFile);
            if (loaded == null) {
                return;
            }
            currentFile = loaded.path();
            session.loadDocument(loaded.document());
            editor.reloadDocument();
            refreshChrome.run();
        } catch (IOException exception) {
            showError("Open failed", "Delos Writer could not open the selected document.", exception);
        }
    }

    void saveDocument() {
        saveCurrentDocument(false);
    }

    void saveDocumentAs() {
        saveCurrentDocument(true);
    }

    boolean saveCurrentDocument(boolean saveAs) {
        try {
            Path savedFile = fileService.save(stage, currentFile, session.document(), saveAs);
            if (savedFile == null) {
                return false;
            }
            currentFile = savedFile;
            session.markClean();
            refreshChrome.run();
            return true;
        } catch (IOException exception) {
            showError("Save failed", "Delos Writer could not save the current document.", exception);
            return false;
        }
    }

    void exportHtml() {
        try {
            exportService.exportHtml(stage, currentFile, session.document());
        } catch (IOException exception) {
            showError("Export failed", "Delos Writer could not export the current document as HTML.", exception);
        }
    }

    void exportMarkdown() {
        try {
            exportService.exportMarkdown(stage, currentFile, session.document());
        } catch (IOException exception) {
            showError("Export failed", "Delos Writer could not export the current document as Markdown.", exception);
        }
    }

    void exportPdf() {
        try {
            Path target = choosePdfExportPath();
            if (target == null) {
                return;
            }
            Path normalizedTarget = WriterFileChoosers.normalizeExtension(target, ".pdf");
            WriterLayoutSnapshot snapshot = editor.createLayoutSnapshot();
            pdfService.exportLayout(snapshot.document(), snapshot.layout(), normalizedTarget);
        } catch (IOException | RuntimeException exception) {
            showError("Export failed", "Delos Writer could not export the current document as PDF.", exception);
        }
    }

    void printDocument() {
        if (!printing.compareAndSet(false, true)) {
            return;
        }
        refreshChrome.run();

        WriterLayoutSnapshot printSnapshot;
        try {
            printSnapshot = editor.createLayoutSnapshot();
        } catch (RuntimeException exception) {
            finishPrintAttempt();
            showError("Print failed", "Delos Writer could not prepare the current document for printing.", exception);
            return;
        }

        PdfPrintOptions options = PdfPrintOptions.defaultOptions()
                .withJobName(displayName());

        PRINT_EXECUTOR.execute(() -> {
            try {
                printService.print(printSnapshot.document(), printSnapshot.layout(), options);
            } catch (IOException | RuntimeException | PrinterException exception) {
                Platform.runLater(() -> showError(
                        "Print failed",
                        "Delos Writer could not print the current document.",
                        exception
                ));
            } finally {
                Platform.runLater(this::finishPrintAttempt);
            }
        });
    }

    boolean canPrintDocument() {
        return !printing.get();
    }

    private void finishPrintAttempt() {
        printing.set(false);
        refreshChrome.run();
    }

    boolean requestClose() {
        return confirmAbandonChanges();
    }

    void renameDocumentTitle(String newTitle) {
        String normalized = normalizeTitle(newTitle, session.document().title());
        if (normalized.equals(session.document().title())) {
            refreshChrome.run();
            return;
        }
        Document current = session.document();
        session.setDocument(Document.fromBlocks(normalized, current.pageStyle(), current.blocks(), current.mediaItems()));
        editor.reloadDocument();
        refreshChrome.run();
    }

    Path currentFile() {
        return currentFile;
    }

    String displayName() {
        return currentFile == null ? session.document().title() : currentFile.getFileName().toString();
    }

    private Path choosePdfExportPath() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export PDF");
        chooser.getExtensionFilters().add(PDF_FILTER);
        WriterFileChoosers.configureInitialLocation(chooser, currentFile);
        String candidateName = WriterFileChoosers.sanitizeFileName(
                WriterFileChoosers.suggestedBaseName(currentFile, session.document().title())
        );
        chooser.setInitialFileName(WriterFileChoosers.stripExtensionForDisplay(candidateName, ".pdf"));
        var file = chooser.showSaveDialog(stage);
        return file == null ? null : file.toPath();
    }

    private boolean confirmAbandonChanges() {
        return UnsavedChangesCoordinator.canProceed(session.isDirty(), this::showPendingChangesDialog, () -> saveCurrentDocument(false));
    }

    private UnsavedChangesCoordinator.Decision showPendingChangesDialog() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(stage);
        alert.setTitle("Unsaved changes");
        alert.setHeaderText("Save changes before continuing?");
        alert.setContentText("Delos Writer has unsaved changes in “" + displayName() + "”.");
        ButtonType save = new ButtonType("Save");
        ButtonType discard = new ButtonType("Discard");
        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(save, discard, cancel);
        Button discardButton = (Button) alert.getDialogPane().lookupButton(discard);
        if (discardButton != null) {
            discardButton.getStyleClass().add("danger-button");
        }
        return alert.showAndWait().map(buttonType -> {
            if (buttonType == save) {
                return UnsavedChangesCoordinator.Decision.SAVE;
            }
            if (buttonType == discard) {
                return UnsavedChangesCoordinator.Decision.DISCARD;
            }
            return UnsavedChangesCoordinator.Decision.CANCEL;
        }).orElse(UnsavedChangesCoordinator.Decision.CANCEL);
    }

    private void showError(String title, String header, Exception exception) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(stage);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(exception.getMessage());
        alert.showAndWait();
    }

    private static String normalizeTitle(String newTitle, String fallback) {
        String normalized = newTitle == null ? "" : newTitle.trim();
        return normalized.isEmpty() ? fallback : normalized;
    }
}
