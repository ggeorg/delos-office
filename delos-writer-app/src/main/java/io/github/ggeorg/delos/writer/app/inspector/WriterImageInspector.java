package io.github.ggeorg.delos.writer.app.inspector;

import io.github.ggeorg.delos.javafx.command.CommandRegistry;
import io.github.ggeorg.delos.javafx.command.EditorCommand;
import io.github.ggeorg.delos.javafx.icon.DelosIconId;
import io.github.ggeorg.delos.javafx.icon.DelosIconSize;
import io.github.ggeorg.delos.javafx.icon.DelosIcons;
import io.github.ggeorg.delos.javafx.inspector.InspectorSection;
import io.github.ggeorg.delos.writer.document.ImageBlock;
import io.github.ggeorg.delos.writer.ui.control.DelosEditor;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Objects;

/**
 * Inline-image inspector for Delos Writer.
 *
 * <p>v1 intentionally edits only the existing block-image properties: width,
 * height, alt text, and replacement. No floating layout, wrapping, anchors, or
 * resize handles live here yet.</p>
 */
final class WriterImageInspector extends VBox {
    private static final double MIN_IMAGE_SIZE = 1.0;
    private static final double MAX_IMAGE_SIZE = 4096.0;
    private static final double DEFAULT_IMAGE_WIDTH = 240.0;
    private static final double DEFAULT_IMAGE_HEIGHT = 160.0;

    private final DelosEditor editor;
    private final EditorCommand replaceImageCommand;
    private final Label emptyState = new Label("Select an image to edit its size and description.");
    private final Spinner<Double> width = imageSizeSpinner(DEFAULT_IMAGE_WIDTH);
    private final Spinner<Double> height = imageSizeSpinner(DEFAULT_IMAGE_HEIGHT);
    private final CheckBox keepAspectRatio = new CheckBox("Keep aspect ratio");
    private final TextField altText = new TextField();
    private final Button replaceImage = new Button("Replace Image…");
    private final List<Node> imageControls = List.of(width, height, keepAspectRatio, altText, replaceImage);

    private boolean refreshing;
    private double aspectRatio = DEFAULT_IMAGE_WIDTH / DEFAULT_IMAGE_HEIGHT;

    WriterImageInspector(DelosEditor editor, CommandRegistry commandRegistry) {
        this.editor = Objects.requireNonNull(editor, "editor");
        Objects.requireNonNull(commandRegistry, "commandRegistry");
        this.replaceImageCommand = commandRegistry.byId("image.replace")
                .orElseThrow(() -> new IllegalArgumentException("Unknown command: image.replace"));

        getStyleClass().add("writer-image-inspector");
        setPadding(new Insets(0, 14, 18, 14));
        setSpacing(10);

        configureControls();
        getChildren().setAll(imageSection());
    }

    void refresh() {
        ImageBlock selected = editor.selectedImageBlock();
        boolean hasImage = selected != null;

        refreshing = true;
        try {
            emptyState.setManaged(!hasImage);
            emptyState.setVisible(!hasImage);
            for (Node control : imageControls) {
                control.setDisable(!hasImage);
            }
            replaceImage.setDisable(!hasImage || !replaceImageCommand.isEnabled());

            if (!hasImage) {
                return;
            }

            aspectRatio = aspectRatioFor(selected);
            setSpinnerValue(width, selected.width());
            setSpinnerValue(height, selected.height());
            altText.setText(selected.altText());
        } finally {
            refreshing = false;
        }
    }

    boolean hasSelectedImage() {
        return editor.hasSelectedImageBlock();
    }

    private Node imageSection() {
        InspectorSection section = new InspectorSection("Image");
        section.addAll(
                emptyState,
                InspectorSection.row("Width", width),
                InspectorSection.row("Height", height),
                InspectorSection.row("Proportions", keepAspectRatio),
                InspectorSection.row("Alt text", altText),
                actionRow(replaceImage)
        );
        return section;
    }

    private void configureControls() {
        keepAspectRatio.setSelected(true);
        keepAspectRatio.getStyleClass().add("delos-inspector-check");

        altText.getStyleClass().add("delos-inspector-text-field");
        altText.setPromptText("Optional accessible description");
        altText.setMaxWidth(Double.MAX_VALUE);
        altText.setOnAction(event -> commitImageProperties());
        altText.focusedProperty().addListener((obs, oldValue, focused) -> {
            if (!focused) {
                commitImageProperties();
            }
        });

        configureSpinner(width, true);
        configureSpinner(height, false);

        replaceImage.getStyleClass().add("delos-inspector-action-button");
        replaceImage.setMaxWidth(Double.MAX_VALUE);
        replaceImage.setTooltip(new Tooltip(replaceImageCommand.label()));
        replaceImage.setGraphic(DelosIcons.icon(DelosIconId.IMAGE, DelosIconSize.TOOLBAR));
        replaceImage.setContentDisplay(ContentDisplay.LEFT);
        replaceImage.setOnAction(event -> {
            if (!replaceImageCommand.isEnabled()) {
                return;
            }
            replaceImageCommand.execute();
            editor.focusEditor();
            refresh();
        });
    }

    private void configureSpinner(Spinner<Double> spinner, boolean editsWidth) {
        spinner.getStyleClass().add("delos-inspector-number-field");
        spinner.setEditable(true);
        spinner.setMaxWidth(Double.MAX_VALUE);
        spinner.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (refreshing || newValue == null) {
                return;
            }
            if (keepAspectRatio.isSelected()) {
                refreshing = true;
                try {
                    if (editsWidth) {
                        setSpinnerValue(height, newValue / safeAspectRatio());
                    } else {
                        setSpinnerValue(width, newValue * safeAspectRatio());
                    }
                } finally {
                    refreshing = false;
                }
            }
            commitImageProperties();
        });
        spinner.getEditor().setOnAction(event -> commitSpinnerEditor(spinner));
        spinner.focusedProperty().addListener((obs, oldValue, focused) -> {
            if (!focused) {
                commitSpinnerEditor(spinner);
            }
        });
    }

    private Spinner<Double> imageSizeSpinner(double initialValue) {
        Spinner<Double> spinner = new Spinner<>();
        spinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(
                MIN_IMAGE_SIZE,
                MAX_IMAGE_SIZE,
                initialValue,
                1.0
        ));
        return spinner;
    }

    private HBox actionRow(Button button) {
        HBox row = new HBox(button);
        row.getStyleClass().add("delos-inspector-action-row");
        HBox.setHgrow(button, Priority.ALWAYS);
        return row;
    }

    private void commitImageProperties() {
        if (refreshing || !editor.hasSelectedImageBlock()) {
            return;
        }
        editor.updateSelectedImageProperties(
                spinnerValue(width),
                spinnerValue(height),
                altText.getText()
        );
    }

    private void commitSpinnerEditor(Spinner<Double> spinner) {
        String text = spinner.getEditor().getText();
        if (text == null || text.isBlank()) {
            setSpinnerValue(spinner, spinnerValue(spinner));
            return;
        }
        try {
            setSpinnerValue(spinner, Double.parseDouble(text.trim()));
        } catch (NumberFormatException ignored) {
            setSpinnerValue(spinner, spinnerValue(spinner));
        }
    }

    private double spinnerValue(Spinner<Double> spinner) {
        Double value = spinner.getValue();
        return clampImageSize(value == null ? 0.0 : value);
    }

    private void setSpinnerValue(Spinner<Double> spinner, double value) {
        spinner.getValueFactory().setValue(clampImageSize(value));
    }

    private static double aspectRatioFor(ImageBlock imageBlock) {
        if (imageBlock.height() <= 0.0 || !Double.isFinite(imageBlock.height())) {
            return DEFAULT_IMAGE_WIDTH / DEFAULT_IMAGE_HEIGHT;
        }
        double ratio = imageBlock.width() / imageBlock.height();
        return Double.isFinite(ratio) && ratio > 0.0 ? ratio : DEFAULT_IMAGE_WIDTH / DEFAULT_IMAGE_HEIGHT;
    }

    private double safeAspectRatio() {
        return Double.isFinite(aspectRatio) && aspectRatio > 0.0 ? aspectRatio : DEFAULT_IMAGE_WIDTH / DEFAULT_IMAGE_HEIGHT;
    }

    private static double clampImageSize(double value) {
        if (!Double.isFinite(value)) {
            return DEFAULT_IMAGE_WIDTH;
        }
        return Math.max(MIN_IMAGE_SIZE, Math.min(MAX_IMAGE_SIZE, value));
    }
}
