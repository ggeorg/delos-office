package io.github.ggeorg.delos.calc.app;

import io.github.ggeorg.delos.calc.core.CalcWorkbookFormat;
import io.github.ggeorg.delos.calc.core.Workbook;
import io.github.ggeorg.delos.document.DocumentIo;
import io.github.ggeorg.delos.document.DocumentPackage;
import io.github.ggeorg.delos.document.DocumentRegistry;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** JavaFX file-chooser adapter for standalone Calc workbooks. */
final class CalcFileService {
    private static final FileChooser.ExtensionFilter CALC_FILTER =
            new FileChooser.ExtensionFilter("Delos Spreadsheets (*.dcalc)", "*.dcalc");

    private final CalcWorkbookFormat calcFormat;
    private final DocumentIo documentIo;

    CalcFileService() {
        this(new CalcWorkbookFormat());
    }

    CalcFileService(CalcWorkbookFormat calcFormat) {
        this.calcFormat = Objects.requireNonNull(calcFormat, "calcFormat");
        this.documentIo = new DocumentIo(DocumentRegistry.of(calcFormat));
    }

    LoadedWorkbook open(Window owner, Path initialFile) throws IOException {
        Path selected = chooseOpenPath(owner, initialFile);
        return selected == null ? null : read(selected);
    }

    LoadedWorkbook read(Path path) throws IOException {
        Objects.requireNonNull(path, "path");
        DocumentPackage<?> loaded = documentIo.read(path);
        Object content = loaded.content();
        if (content instanceof Workbook workbook) {
            return new LoadedWorkbook(loaded.path(), workbook);
        }
        throw new IOException("Unsupported Calc document content: " + content.getClass().getName());
    }

    Path save(Window owner, Path currentFile, Workbook workbook, boolean saveAs) throws IOException {
        Objects.requireNonNull(workbook, "workbook");
        Path target = saveAs || currentFile == null
                ? chooseSavePath(owner, currentFile, workbook.title())
                : currentFile;
        return target == null ? null : documentIo.write(target, calcFormat, workbook);
    }

    private Path chooseOpenPath(Window owner, Path initialFile) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open Spreadsheet");
        chooser.getExtensionFilters().add(CALC_FILTER);
        configureInitialLocation(chooser, initialFile);
        var file = chooser.showOpenDialog(owner);
        return file == null ? null : file.toPath();
    }

    private Path chooseSavePath(Window owner, Path currentFile, String title) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Spreadsheet");
        chooser.getExtensionFilters().add(CALC_FILTER);
        configureInitialLocation(chooser, currentFile);
        chooser.setInitialFileName(toDisplayFileName(sanitizeFileName(exportBaseName(currentFile, title)), calcFormat.type().extension()));
        var file = chooser.showSaveDialog(owner);
        return file == null ? null : file.toPath();
    }

    private static void configureInitialLocation(FileChooser chooser, Path path) {
        if (path == null) {
            return;
        }
        Path absolute = path.toAbsolutePath();
        Path parent = Files.isDirectory(absolute) ? absolute : absolute.getParent();
        if (parent != null && Files.isDirectory(parent)) {
            chooser.setInitialDirectory(parent.toFile());
        }
    }

    private static String toDisplayFileName(String candidateName, String extension) {
        return candidateName.endsWith(extension)
                ? candidateName.substring(0, candidateName.length() - extension.length())
                : candidateName;
    }

    private static String exportBaseName(Path currentFile, String title) {
        if (currentFile != null) {
            String filename = currentFile.getFileName().toString();
            int extensionIndex = filename.lastIndexOf('.');
            return extensionIndex > 0 ? filename.substring(0, extensionIndex) : filename;
        }
        return title;
    }

    private static String sanitizeFileName(String value) {
        String candidate = value == null || value.isBlank() ? "Untitled Spreadsheet" : value.trim();
        candidate = candidate.replaceAll("[\\\\/:*?\"<>|]", "-");
        return candidate.isBlank() ? "Untitled Spreadsheet" : candidate;
    }

    record LoadedWorkbook(Path path, Workbook workbook) { }
}
