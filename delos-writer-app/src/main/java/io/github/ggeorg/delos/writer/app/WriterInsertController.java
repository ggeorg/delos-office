package io.github.ggeorg.delos.writer.app;

import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.document.DocumentMediaItem;
import io.github.ggeorg.delos.writer.document.FormulaBlock;
import io.github.ggeorg.delos.writer.document.FormulaSourceFormat;
import io.github.ggeorg.delos.writer.document.ImageBlock;
import io.github.ggeorg.delos.writer.ui.control.DelosEditor;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.Objects;
import java.util.Set;

/**
 * Insert commands that need application chrome such as file choosers.
 */
final class WriterInsertController {
    private static final double MAX_IMAGE_WIDTH = 420.0;
    private static final double MAX_IMAGE_HEIGHT = 360.0;
    private static final double DEFAULT_IMAGE_WIDTH = 240.0;
    private static final double DEFAULT_IMAGE_HEIGHT = 160.0;
    private static final double MIN_EDITED_IMAGE_SIZE = 1.0;
    private static final double MAX_EDITED_IMAGE_SIZE = 4096.0;

    private static final FileChooser.ExtensionFilter IMAGE_FILTER =
            new FileChooser.ExtensionFilter("Images (*.png, *.jpg, *.jpeg, *.gif)", "*.png", "*.jpg", "*.jpeg", "*.gif");

    private final Stage stage;
    private final DelosEditor editor;

    WriterInsertController(Stage stage, DelosEditor editor) {
        this.stage = Objects.requireNonNull(stage, "stage");
        this.editor = Objects.requireNonNull(editor, "editor");
    }

    void insertImage() {
        Path source = chooseImagePath();
        if (source == null) {
            return;
        }

        try {
            byte[] bytes = Files.readAllBytes(source);
            ImageSize size = readImageSize(bytes);
            String mediaPath = nextMediaPath(editor.document(), source);
            DocumentMediaItem mediaItem = DocumentMediaItem.image(
                    mediaPath,
                    DocumentMediaItem.guessMediaType(mediaPath),
                    bytes
            );

            editor.insertImage(mediaItem, size.width(), size.height(), source.getFileName().toString());
            editor.focusEditor();
        } catch (IOException | RuntimeException exception) {
            showError("Insert image failed", "Delos Writer could not insert the selected image.", exception);
        }
    }

    void insertTable() {
        Optional<TableSize> tableSize = chooseTableSize();
        if (tableSize.isEmpty()) {
            return;
        }

        TableSize size = tableSize.get();
        editor.insertTable(size.rows(), size.columns());
        editor.focusEditor();
    }

    void insertFormula() {
        Optional<FormulaInput> formula = chooseFormulaForInsert();
        if (formula.isEmpty()) {
            return;
        }

        FormulaInput input = formula.get();
        editor.insertFormula(input.sourceFormat(), input.source(), input.altText());
        editor.focusEditor();
    }

    void editSelectedFormula() {
        FormulaBlock selected = editor.selectedFormulaBlock();
        if (selected == null) {
            return;
        }

        Optional<FormulaInput> formula = chooseFormulaForEdit(selected);
        if (formula.isEmpty()) {
            return;
        }

        FormulaInput input = formula.get();
        editor.updateSelectedFormula(input.sourceFormat(), input.source(), input.altText());
        editor.focusEditor();
    }

    void editSelectedImageProperties() {
        ImageBlock selected = editor.selectedImageBlock();
        if (selected == null) {
            return;
        }

        Optional<ImagePropertiesInput> properties = chooseImageProperties(selected);
        if (properties.isEmpty()) {
            return;
        }

        ImagePropertiesInput input = properties.get();
        editor.updateSelectedImageProperties(input.width(), input.height(), input.altText());
        editor.focusEditor();
    }

    private Path chooseImagePath() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Insert Image");
        chooser.getExtensionFilters().add(IMAGE_FILTER);
        var selected = chooser.showOpenDialog(stage);
        return selected == null ? null : selected.toPath();
    }

    private Optional<ImagePropertiesInput> chooseImageProperties(ImageBlock imageBlock) {
        Dialog<ImagePropertiesInput> dialog = new Dialog<>();
        dialog.initOwner(stage);
        dialog.setTitle("Image Properties");
        dialog.setHeaderText("Edit image placement properties");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Spinner<Double> width = imageSizeSpinner(imageBlock.width());
        Spinner<Double> height = imageSizeSpinner(imageBlock.height());

        TextField altText = new TextField(imageBlock.altText());
        altText.setPromptText("Optional accessible description");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.add(new Label("Width"), 0, 0);
        grid.add(width, 1, 0);
        grid.add(new Label("Height"), 0, 1);
        grid.add(height, 1, 1);
        grid.add(new Label("Alt text"), 0, 2);
        grid.add(altText, 1, 2);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(button -> button == ButtonType.OK
                ? new ImagePropertiesInput(width.getValue(), height.getValue(), altText.getText())
                : null);
        return dialog.showAndWait();
    }

    private Spinner<Double> imageSizeSpinner(double initialValue) {
        double safeInitial = clampImageSize(initialValue);
        Spinner<Double> spinner = new Spinner<>();
        spinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(
                MIN_EDITED_IMAGE_SIZE,
                MAX_EDITED_IMAGE_SIZE,
                safeInitial,
                1.0
        ));
        spinner.setEditable(true);
        return spinner;
    }

    private double clampImageSize(double value) {
        if (!Double.isFinite(value)) {
            return DEFAULT_IMAGE_WIDTH;
        }
        return Math.max(MIN_EDITED_IMAGE_SIZE, Math.min(MAX_EDITED_IMAGE_SIZE, value));
    }

    private Optional<TableSize> chooseTableSize() {
        Dialog<TableSize> dialog = new Dialog<>();
        dialog.initOwner(stage);
        dialog.setTitle("Insert Table");
        dialog.setHeaderText("Choose table size");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Spinner<Integer> rows = new Spinner<>(1, 20, 2);
        Spinner<Integer> columns = new Spinner<>(1, 12, 2);
        rows.setEditable(true);
        columns.setEditable(true);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.add(new Label("Rows"), 0, 0);
        grid.add(rows, 1, 0);
        grid.add(new Label("Columns"), 0, 1);
        grid.add(columns, 1, 1);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(button -> button == ButtonType.OK
                ? new TableSize(rows.getValue(), columns.getValue())
                : null);
        return dialog.showAndWait();
    }

    private Optional<FormulaInput> chooseFormulaForInsert() {
        return chooseFormula("Insert Formula", "Enter a LaTeX formula", new FormulaInput(FormulaSourceFormat.LATEX, "E = mc^2", ""));
    }

    private Optional<FormulaInput> chooseFormulaForEdit(FormulaBlock formulaBlock) {
        return chooseFormula(
                "Edit Formula",
                "Edit the canonical LaTeX formula source",
                new FormulaInput(formulaBlock.sourceFormat(), formulaBlock.source(), formulaBlock.altText())
        );
    }

    private Optional<FormulaInput> chooseFormula(String title, String headerText, FormulaInput initial) {
        Dialog<FormulaInput> dialog = new Dialog<>();
        dialog.initOwner(stage);
        dialog.setTitle(title);
        dialog.setHeaderText(headerText);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextArea source = new TextArea(initial == null ? "" : initial.source());
        source.setPrefColumnCount(32);
        source.setPrefRowCount(4);
        source.setWrapText(true);

        TextField altText = new TextField(initial == null ? "" : initial.altText());
        altText.setPromptText("Optional accessible description");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.add(new Label("Format"), 0, 0);
        grid.add(new Label("LaTeX"), 1, 0);
        grid.add(new Label("Source"), 0, 1);
        grid.add(source, 1, 1);
        grid.add(new Label("Alt text"), 0, 2);
        grid.add(altText, 1, 2);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(button -> {
            if (button != ButtonType.OK) {
                return null;
            }
            String formulaSource = source.getText() == null ? "" : source.getText().strip();
            if (formulaSource.isBlank()) {
                return null;
            }
            return new FormulaInput(FormulaSourceFormat.LATEX, formulaSource, altText.getText());
        });
        return dialog.showAndWait();
    }

    private ImageSize readImageSize(byte[] bytes) throws IOException {
        Image image = new Image(new ByteArrayInputStream(bytes));
        if (image.isError()) {
            throw new IOException(image.getException() == null ? "Unsupported or corrupt image." : image.getException().getMessage());
        }

        double width = image.getWidth();
        double height = image.getHeight();
        if (width <= 0.0 || height <= 0.0) {
            return new ImageSize(DEFAULT_IMAGE_WIDTH, DEFAULT_IMAGE_HEIGHT);
        }

        double scale = Math.min(1.0, Math.min(MAX_IMAGE_WIDTH / width, MAX_IMAGE_HEIGHT / height));
        return new ImageSize(Math.max(1.0, width * scale), Math.max(1.0, height * scale));
    }

    private String nextMediaPath(Document document, Path source) {
        String extension = normalizedExtension(source);
        Set<String> existing = document.mediaItems().stream()
                .map(DocumentMediaItem::path)
                .collect(java.util.stream.Collectors.toSet());
        document.blocks().stream()
                .filter(ImageBlock.class::isInstance)
                .map(ImageBlock.class::cast)
                .map(ImageBlock::source)
                .forEach(existing::add);

        int index = existing.size() + 1;
        String candidate;
        do {
            candidate = "media/image-" + index + extension;
            index += 1;
        } while (existing.contains(candidate));
        return candidate;
    }

    private String normalizedExtension(Path source) {
        String filename = source == null || source.getFileName() == null ? "" : source.getFileName().toString();
        int dotIndex = filename.lastIndexOf('.');
        String extension = dotIndex >= 0 ? filename.substring(dotIndex).toLowerCase(Locale.ROOT) : ".png";
        return switch (extension) {
            case ".png", ".jpg", ".jpeg", ".gif" -> extension;
            default -> ".png";
        };
    }

    private void showError(String title, String header, Exception exception) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(stage);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(exception.getMessage());
        alert.showAndWait();
    }

    private record ImageSize(double width, double height) {
    }

    private record ImagePropertiesInput(double width, double height, String altText) {
    }

    private record TableSize(int rows, int columns) {
    }

    private record FormulaInput(FormulaSourceFormat sourceFormat, String source, String altText) {
    }
}
