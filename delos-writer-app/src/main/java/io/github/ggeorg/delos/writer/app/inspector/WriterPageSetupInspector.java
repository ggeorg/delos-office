package io.github.ggeorg.delos.writer.app.inspector;

import io.github.ggeorg.delos.javafx.inspector.InspectorSection;
import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.document.PageStyle;
import io.github.ggeorg.delos.writer.document.TextPosition;
import io.github.ggeorg.delos.writer.editor.DocumentEdit;
import io.github.ggeorg.delos.writer.editor.EditCommand;
import io.github.ggeorg.delos.writer.session.EditorSession;
import io.github.ggeorg.delos.writer.ui.control.DelosEditor;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Locale;
import java.util.Objects;

/** Editable document-level page setup controls for the Writer Document inspector. */
final class WriterPageSetupInspector extends VBox {
    private static final double POINTS_PER_INCH = 72.0;
    private static final double CM_PER_INCH = 2.54;
    private static final double POINTS_PER_CM = POINTS_PER_INCH / CM_PER_INCH;
    private static final double MIN_MARGIN_CM = 0.0;
    private static final double MAX_MARGIN_CM = 10.0;

    private final EditorSession session;
    private final DelosEditor editor;
    private final ComboBox<PaperPreset> paper = new ComboBox<>();
    private final Label pageSizeSummary = new Label();
    private final ToggleButton portrait = new ToggleButton("Portrait");
    private final ToggleButton landscape = new ToggleButton("Landscape");
    private final Spinner<Double> marginTop = marginSpinner();
    private final Spinner<Double> marginRight = marginSpinner();
    private final Spinner<Double> marginBottom = marginSpinner();
    private final Spinner<Double> marginLeft = marginSpinner();
    private boolean refreshing;

    WriterPageSetupInspector(EditorSession session, DelosEditor editor) {
        this.session = Objects.requireNonNull(session, "session");
        this.editor = Objects.requireNonNull(editor, "editor");

        getStyleClass().add("writer-page-setup-inspector");
        setPadding(new Insets(12, 14, 18, 14));
        setSpacing(10);

        configureControls();
        getChildren().setAll(pageSetupSection(), marginSection());
        refresh();
    }

    void refresh() {
        PageStyle pageStyle = session.document().pageStyle();
        refreshing = true;
        try {
            paper.getSelectionModel().select(PaperPreset.closestTo(pageStyle));
            if (pageStyle.width() > pageStyle.height()) {
                landscape.setSelected(true);
            } else {
                portrait.setSelected(true);
            }
            setSpinnerValueCm(marginTop, pointsToCm(pageStyle.marginTop()));
            setSpinnerValueCm(marginRight, pointsToCm(pageStyle.marginRight()));
            setSpinnerValueCm(marginBottom, pointsToCm(pageStyle.marginBottom()));
            setSpinnerValueCm(marginLeft, pointsToCm(pageStyle.marginLeft()));
            updatePageSizeSummary(pageStyle.width(), pageStyle.height());
        } finally {
            refreshing = false;
        }
    }

    private Node pageSetupSection() {
        InspectorSection section = new InspectorSection("Page Setup");
        pageSizeSummary.getStyleClass().add("delos-inspector-help-text");
        section.addAll(
                InspectorSection.row("Paper", paper),
                pageSizeSummary,
                InspectorSection.row("Orientation", orientationStrip())
        );
        return section;
    }

    private Node marginSection() {
        InspectorSection section = new InspectorSection("Document Margins");
        Label unitHint = new Label("Margins are shown in centimeters. The document model stores page sizes and margins in points for layout/PDF precision.");
        unitHint.getStyleClass().add("delos-inspector-help-text");
        section.addAll(
                unitHint,
                InspectorSection.row("Top", lengthInput(marginTop, "cm")),
                InspectorSection.row("Right", lengthInput(marginRight, "cm")),
                InspectorSection.row("Bottom", lengthInput(marginBottom, "cm")),
                InspectorSection.row("Left", lengthInput(marginLeft, "cm"))
        );
        return section;
    }

    private void configureControls() {
        paper.getStyleClass().add("delos-inspector-combo");
        paper.setItems(FXCollections.observableArrayList(PaperPreset.values()));
        paper.setMaxWidth(Double.MAX_VALUE);
        paper.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (!refreshing && newValue != null) {
                commitPageStyle();
            }
        });

        ToggleGroup orientation = new ToggleGroup();
        configureOrientationButton(portrait, orientation);
        configureOrientationButton(landscape, orientation);
        orientation.selectedToggleProperty().addListener((obs, oldValue, newValue) -> {
            if (!refreshing && newValue != null) {
                commitPageStyle();
            }
        });

        configureMarginSpinner(marginTop);
        configureMarginSpinner(marginRight);
        configureMarginSpinner(marginBottom);
        configureMarginSpinner(marginLeft);
    }

    private void configureOrientationButton(ToggleButton button, ToggleGroup group) {
        button.getStyleClass().add("delos-inspector-command-button");
        button.setToggleGroup(group);
        button.setFocusTraversable(false);
        button.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(button, Priority.ALWAYS);
    }

    private HBox orientationStrip() {
        HBox strip = new HBox(6, portrait, landscape);
        strip.getStyleClass().add("delos-inspector-command-strip");
        return strip;
    }

    private void configureMarginSpinner(Spinner<Double> spinner) {
        spinner.getStyleClass().add("delos-inspector-number-field");
        spinner.setEditable(true);
        spinner.setMaxWidth(Double.MAX_VALUE);
        spinner.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (!refreshing && newValue != null) {
                commitPageStyle();
            }
        });
        spinner.getEditor().setOnAction(event -> commitSpinnerEditor(spinner));
        spinner.focusedProperty().addListener((obs, oldValue, focused) -> {
            if (!focused) {
                commitSpinnerEditor(spinner);
            }
        });
    }

    private Spinner<Double> marginSpinner() {
        Spinner<Double> spinner = new Spinner<>();
        spinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(MIN_MARGIN_CM, MAX_MARGIN_CM, 2.0, 0.1));
        return spinner;
    }

    private HBox lengthInput(Spinner<Double> spinner, String unit) {
        Label unitLabel = new Label(unit);
        unitLabel.getStyleClass().add("delos-inspector-unit-label");
        HBox row = new HBox(6, spinner, unitLabel);
        row.getStyleClass().add("delos-inspector-length-input");
        HBox.setHgrow(spinner, Priority.ALWAYS);
        return row;
    }

    private void commitPageStyle() {
        if (refreshing) {
            return;
        }
        PageStyle oldStyle = session.document().pageStyle();
        PageStyle newStyle = buildPageStyle(oldStyle);
        updatePageSizeSummary(newStyle.width(), newStyle.height());
        if (oldStyle.equals(newStyle)) {
            return;
        }

        Document oldDocument = session.document();
        Document updated = Document.fromBlocks(
                oldDocument.title(),
                newStyle,
                oldDocument.blocks(),
                oldDocument.mediaItems()
        );
        TextPosition caret = editor.caretPosition() == null ? new TextPosition(0, 0) : editor.caretPosition();
        DocumentEdit edit = new DocumentEdit(updated, caret, editor.selectionRange(), "Change Page Setup");
        session.execute(new EditCommand(session, caret, editor.selectionRange(), edit));
        editor.reloadDocument();
    }

    private PageStyle buildPageStyle(PageStyle base) {
        PaperPreset preset = paper.getValue() == null ? PaperPreset.closestTo(base) : paper.getValue();
        boolean landscapeSelected = landscape.isSelected();
        double shortEdge = Math.min(preset.widthPoints(), preset.heightPoints());
        double longEdge = Math.max(preset.widthPoints(), preset.heightPoints());
        double width = landscapeSelected ? longEdge : shortEdge;
        double height = landscapeSelected ? shortEdge : longEdge;
        return new PageStyle(
                width,
                height,
                cmToPoints(spinnerValueCm(marginTop)),
                cmToPoints(spinnerValueCm(marginRight)),
                cmToPoints(spinnerValueCm(marginBottom)),
                cmToPoints(spinnerValueCm(marginLeft))
        );
    }

    private void commitSpinnerEditor(Spinner<Double> spinner) {
        String text = spinner.getEditor().getText();
        if (text == null || text.isBlank()) {
            setSpinnerValueCm(spinner, spinnerValueCm(spinner));
            return;
        }
        try {
            setSpinnerValueCm(spinner, parseLengthAsCm(text));
        } catch (NumberFormatException ignored) {
            setSpinnerValueCm(spinner, spinnerValueCm(spinner));
        }
    }

    private double parseLengthAsCm(String text) {
        String normalized = text.trim().toLowerCase(Locale.ROOT).replace(',', '.');
        if (normalized.endsWith("mm")) {
            return Double.parseDouble(normalized.substring(0, normalized.length() - 2).trim()) / 10.0;
        }
        if (normalized.endsWith("cm")) {
            return Double.parseDouble(normalized.substring(0, normalized.length() - 2).trim());
        }
        if (normalized.endsWith("pt")) {
            return pointsToCm(Double.parseDouble(normalized.substring(0, normalized.length() - 2).trim()));
        }
        if (normalized.endsWith("in")) {
            return Double.parseDouble(normalized.substring(0, normalized.length() - 2).trim()) * CM_PER_INCH;
        }
        return Double.parseDouble(normalized);
    }

    private double spinnerValueCm(Spinner<Double> spinner) {
        Double value = spinner.getValue();
        if (value == null || !Double.isFinite(value)) {
            return 0.0;
        }
        return clampMarginCm(value);
    }

    private void setSpinnerValueCm(Spinner<Double> spinner, double valueCm) {
        spinner.getValueFactory().setValue(clampMarginCm(round1(valueCm)));
    }

    private double clampMarginCm(double valueCm) {
        return Math.max(MIN_MARGIN_CM, Math.min(MAX_MARGIN_CM, valueCm));
    }

    private void updatePageSizeSummary(double widthPoints, double heightPoints) {
        pageSizeSummary.setText(formatCm(pointsToCm(widthPoints)) + " × " + formatCm(pointsToCm(heightPoints)) + " cm");
    }

    private static double cmToPoints(double valueCm) {
        return valueCm * POINTS_PER_CM;
    }

    private static double pointsToCm(double valuePoints) {
        return valuePoints / POINTS_PER_CM;
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private static String formatCm(double valueCm) {
        return String.format(Locale.US, "%.1f", round1(valueCm));
    }

    private enum PaperPreset {
        PHOTO_100X150("100 × 150 mm", mm(100), mm(150)),
        PHOTO_35X5("3.5 × 5 in", inches(3.5), inches(5.0)),
        PHOTO_4X6("4 × 6 in", inches(4.0), inches(6.0)),
        PHOTO_5X7("5 × 7 in", inches(5.0), inches(7.0)),
        PHOTO_5X8("5 × 8 in", inches(5.0), inches(8.0)),
        PHOTO_8X10("8 × 10 in", inches(8.0), inches(10.0)),
        A4("A4", mm(210), mm(297)),
        A5("A5", mm(148), mm(210)),
        A6("A6", mm(105), mm(148)),
        B5("B5", mm(176), mm(250)),
        ENVELOPE_10("Envelope #10", inches(4.125), inches(9.5)),
        ENVELOPE_C6("Envelope C6", mm(114), mm(162)),
        ENVELOPE_CHOUKEI_3("Envelope Choukei 3", mm(120), mm(235)),
        ENVELOPE_CHOUKEI_4("Envelope Choukei 4", mm(90), mm(205)),
        ENVELOPE_DL("Envelope DL", mm(110), mm(220)),
        EXECUTIVE("Executive", inches(7.25), inches(10.5)),
        JIS_B5("JIS B5", mm(182), mm(257)),
        POSTCARD("Postcard", mm(100), mm(148)),
        US_LEGAL("US Legal", inches(8.5), inches(14.0)),
        US_LETTER("US Letter", inches(8.5), inches(11.0));

        private final String label;
        private final double widthPoints;
        private final double heightPoints;

        PaperPreset(String label, double widthPoints, double heightPoints) {
            this.label = label;
            this.widthPoints = widthPoints;
            this.heightPoints = heightPoints;
        }

        double widthPoints() {
            return widthPoints;
        }

        double heightPoints() {
            return heightPoints;
        }

        @Override
        public String toString() {
            return label;
        }

        static PaperPreset closestTo(PageStyle pageStyle) {
            PaperPreset best = A4;
            double bestDistance = Double.MAX_VALUE;
            double pageShort = Math.min(pageStyle.width(), pageStyle.height());
            double pageLong = Math.max(pageStyle.width(), pageStyle.height());
            for (PaperPreset preset : values()) {
                double presetShort = Math.min(preset.widthPoints, preset.heightPoints);
                double presetLong = Math.max(preset.widthPoints, preset.heightPoints);
                double distance = Math.abs(pageShort - presetShort) + Math.abs(pageLong - presetLong);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = preset;
                }
            }
            return best;
        }

        private static double mm(double valueMm) {
            return valueMm / 10.0 * POINTS_PER_CM;
        }

        private static double inches(double valueInches) {
            return valueInches * POINTS_PER_INCH;
        }
    }
}
