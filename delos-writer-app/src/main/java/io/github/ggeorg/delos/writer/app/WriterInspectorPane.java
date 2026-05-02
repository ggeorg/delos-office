package io.github.ggeorg.delos.writer.app;

import io.github.ggeorg.delos.javafx.chrome.DelosInspector;
import io.github.ggeorg.delos.javafx.chrome.FormRow;
import io.github.ggeorg.delos.javafx.chrome.InspectorSection;
import io.github.ggeorg.delos.javafx.chrome.InspectorTab;
import io.github.ggeorg.delos.javafx.chrome.SegmentedControl;
import io.github.ggeorg.delos.javafx.chrome.SegmentedOption;
import io.github.ggeorg.delos.javafx.icon.DelosIconId;
import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.document.PageMargins;
import io.github.ggeorg.delos.writer.document.PageOrientation;
import io.github.ggeorg.delos.writer.document.PageSetup;
import io.github.ggeorg.delos.writer.document.PageSize;
import io.github.ggeorg.delos.writer.document.PageStyle;
import io.github.ggeorg.delos.writer.session.EditorSession;
import io.github.ggeorg.delos.writer.session.UndoableCommand;
import io.github.ggeorg.delos.writer.ui.control.DelosEditor;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Writer-specific inspector content hosted by the shared Delos inspector shell. */
final class WriterInspectorPane extends DelosInspector {
    private final EditorSession session;
    private final DelosEditor editor;
    private final ComboBox<PaperPreset> paperCombo = new ComboBox<>(FXCollections.observableArrayList(PaperPreset.values()));
    private final ComboBox<PageOrientation> orientationCombo = new ComboBox<>(FXCollections.observableArrayList(PageOrientation.values()));
    private final TextField widthField = new TextField();
    private final TextField heightField = new TextField();
    private final TextField marginTopField = new TextField();
    private final TextField marginBottomField = new TextField();
    private final TextField marginLeftField = new TextField();
    private final TextField marginRightField = new TextField();
    private final Label pageSetupStatus = disabledMessage("Changes update the document model; PDF export stays headless.");
    private PageStyle displayedStyle;
    private boolean refreshing;

    WriterInspectorPane(EditorSession session, DelosEditor editor) {
        this.session = Objects.requireNonNull(session, "session");
        this.editor = Objects.requireNonNull(editor, "editor");
        getStyleClass().add("writer-inspector");
        configurePageSetupControls();
        setInspectorTabs(List.of(
                new InspectorTab("document", "Document", DelosIconId.SHEET, documentTab()),
                new InspectorTab("format", "Format", DelosIconId.TEXT_BOX, formatTab()),
                new InspectorTab("arrange", "Arrange", DelosIconId.SHAPE, arrangeTab())
        ));
        refreshFromDocument();
        this.session.addStateListener(this::refreshFromDocument);
    }

    private Node documentTab() {
        VBox content = inspectorContent();
        content.getChildren().setAll(
                new InspectorSection("Page Setup", pageSetupForm()),
                new InspectorSection("Document Info", documentInfoPlaceholder())
        );
        return content;
    }

    private static Node formatTab() {
        VBox content = inspectorContent();
        SegmentedControl subTabs = new SegmentedControl(List.of(
                new SegmentedOption("style", "Style"),
                new SegmentedOption("layout", "Layout"),
                new SegmentedOption("more", "More")
        ));
        VBox panel = new VBox(10.0);
        panel.getStyleClass().add("writer-inspector-subtab-content");
        subTabs.setOnSelectionChanged(id -> setFormatSubTabContent(panel, id));
        setFormatSubTabContent(panel, "style");
        content.getChildren().setAll(subTabs, panel);
        return content;
    }

    private static void setFormatSubTabContent(VBox panel, String id) {
        switch (id) {
            case "style" -> panel.getChildren().setAll(
                    new InspectorSection("Text", textPlaceholder())
            );
            case "layout" -> panel.getChildren().setAll(
                    new InspectorSection("Paragraph", paragraphPlaceholder()),
                    new InspectorSection("Lists", listPlaceholder())
            );
            case "more" -> panel.getChildren().setAll(
                    new InspectorSection("Image", imagePlaceholder()),
                    new InspectorSection("Table", tablePlaceholder()),
                    new InspectorSection("Pagination", paginationPlaceholder())
            );
            default -> throw new IllegalArgumentException("Unknown format inspector sub-tab: " + id);
        }
    }

    private static Node arrangeTab() {
        VBox content = inspectorContent();
        content.getChildren().setAll(
                new InspectorSection("Position", disabledMessage("Select an image, table, or object to arrange it.")),
                new InspectorSection("Wrapping", disabledMessage("Inline objects are supported first. Rich wrapping comes later."))
        );
        return content;
    }

    private VBox pageSetupForm() {
        VBox box = formBox();
        box.getChildren().setAll(
                new FormRow("Paper", paperCombo),
                new FormRow("Orientation", orientationCombo),
                new FormRow("Width", widthField),
                new FormRow("Height", heightField),
                new FormRow("Top", marginTopField),
                new FormRow("Bottom", marginBottomField),
                new FormRow("Left", marginLeftField),
                new FormRow("Right", marginRightField),
                pageSetupStatus
        );
        return box;
    }

    private void configurePageSetupControls() {
        paperCombo.setFocusTraversable(false);
        orientationCombo.setFocusTraversable(false);
        widthField.setPromptText("mm");
        heightField.setPromptText("mm");
        marginTopField.setPromptText("mm");
        marginBottomField.setPromptText("mm");
        marginLeftField.setPromptText("mm");
        marginRightField.setPromptText("mm");
        List.of(widthField, heightField, marginTopField, marginBottomField, marginLeftField, marginRightField)
                .forEach(this::configureNumericField);

        paperCombo.valueProperty().addListener((obs, oldValue, newValue) -> applyPageSetupFromControls());
        orientationCombo.valueProperty().addListener((obs, oldValue, newValue) -> applyPageSetupFromControls());
    }

    private void configureNumericField(TextField field) {
        field.setFocusTraversable(true);
        field.setOnAction(event -> applyPageSetupFromControls());
        field.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                applyPageSetupFromControls();
            }
        });
    }

    private void refreshFromDocument() {
        refreshing = true;
        try {
            PageStyle style = session.document().pageStyle();
            displayedStyle = style;
            PageOrientation orientation = style.orientation();
            PaperPreset preset = PaperPreset.from(style);
            paperCombo.getSelectionModel().select(preset);
            orientationCombo.getSelectionModel().select(orientation);
            widthField.setText(formatMillimeters(style.width()));
            heightField.setText(formatMillimeters(style.height()));
            marginTopField.setText(formatMillimeters(style.marginTop()));
            marginBottomField.setText(formatMillimeters(style.marginBottom()));
            marginLeftField.setText(formatMillimeters(style.marginLeft()));
            marginRightField.setText(formatMillimeters(style.marginRight()));
            boolean custom = preset == PaperPreset.CUSTOM;
            widthField.setDisable(!custom);
            heightField.setDisable(!custom);
            pageSetupStatus.setText("Changes update the document model; PDF export stays headless.");
        } finally {
            refreshing = false;
        }
    }

    private void applyPageSetupFromControls() {
        if (refreshing || paperCombo.getValue() == null || orientationCombo.getValue() == null) {
            return;
        }
        try {
            PageStyle newStyle = readPageStyleFromControls();
            PageStyle currentStyle = session.document().pageStyle();
            if (newStyle.equals(currentStyle)) {
                refreshFromDocument();
                return;
            }
            session.execute(new PageStyleCommand(session, newStyle));
            editor.reloadDocument();
            refreshFromDocument();
        } catch (IllegalArgumentException ex) {
            pageSetupStatus.setText(ex.getMessage());
        }
    }

    private PageStyle readPageStyleFromControls() {
        PaperPreset preset = paperCombo.getValue();
        PageOrientation orientation = orientationCombo.getValue();
        PageSize size = preset == PaperPreset.CUSTOM
                ? new PageSize("Custom", pageDimensionPoints(widthField, true), pageDimensionPoints(heightField, false))
                : preset.size();
        PageMargins margins = new PageMargins(
                marginPoints(marginTopField, displayedStyle == null ? Double.NaN : displayedStyle.marginTop()),
                marginPoints(marginRightField, displayedStyle == null ? Double.NaN : displayedStyle.marginRight()),
                marginPoints(marginBottomField, displayedStyle == null ? Double.NaN : displayedStyle.marginBottom()),
                marginPoints(marginLeftField, displayedStyle == null ? Double.NaN : displayedStyle.marginLeft())
        );
        return new PageSetup(size, orientation, margins).toPageStyle();
    }

    private static VBox documentInfoPlaceholder() {
        VBox box = formBox();
        box.getChildren().setAll(
                new FormRow("Title", disabledText("Untitled")),
                disabledMessage("Document metadata and report/template metadata belong here later.")
        );
        return box;
    }

    private static VBox textPlaceholder() {
        VBox box = formBox();
        box.getChildren().setAll(
                new FormRow("Style", disabledCombo("Normal text", "Title", "Heading 1", "Heading 2")),
                new FormRow("Font", disabledCombo("Serif", "Sans Serif", "Monospace")),
                new FormRow("Size", disabledCombo("10", "11", "12", "13", "14", "16", "18", "24", "36")),
                disabledMessage("Bold, italic, underline, and color controls will bind to the existing text commands here.")
        );
        return box;
    }

    private static VBox paragraphPlaceholder() {
        VBox box = formBox();
        box.getChildren().setAll(
                new FormRow("Align", disabledCombo("Left", "Center", "Right", "Justify")),
                new FormRow("Spacing", disabledCombo("Single", "1.15", "1.5", "Double")),
                new FormRow("Before", disabledText("0 pt")),
                new FormRow("After", disabledText("8 pt"))
        );
        return box;
    }

    private static VBox listPlaceholder() {
        VBox box = formBox();
        box.getChildren().setAll(
                new FormRow("Type", disabledCombo("None", "Bullets", "Numbering")),
                disabledMessage("List controls move here so the top toolbar can stay clean.")
        );
        return box;
    }

    private static VBox imagePlaceholder() {
        VBox box = formBox();
        box.getChildren().setAll(
                new FormRow("Width", disabledText("—")),
                new FormRow("Height", disabledText("—")),
                new FormRow("Alt text", disabledText("")),
                disabledMessage("Shown when an image is selected.")
        );
        return box;
    }

    private static VBox tablePlaceholder() {
        VBox box = formBox();
        box.getChildren().setAll(
                new FormRow("Rows", disabledText("—")),
                new FormRow("Columns", disabledText("—")),
                new FormRow("Header", disabledCombo("Off", "On")),
                disabledMessage("Tables matter for reports; the table inspector comes after text/list basics.")
        );
        return box;
    }

    private static VBox paginationPlaceholder() {
        VBox box = formBox();
        box.getChildren().setAll(
                new FormRow("Widows", disabledCombo("Prevent", "Allow")),
                new FormRow("Hyphenation", disabledCombo("Document default", "Off", "On")),
                disabledMessage("Advanced paragraph and publishing controls will live here, Pages-style.")
        );
        return box;
    }

    private static VBox inspectorContent() {
        VBox content = new VBox(10.0);
        content.getStyleClass().add("delos-inspector-content");
        return content;
    }

    private static VBox formBox() {
        VBox box = new VBox(4.0);
        box.getStyleClass().add("writer-inspector-form");
        return box;
    }

    private static Label disabledMessage(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("delos-inspector-empty-state");
        label.setWrapText(true);
        return label;
    }

    private static TextField disabledText(String value) {
        TextField field = new TextField(value);
        field.setDisable(true);
        field.setFocusTraversable(false);
        return field;
    }

    private static ComboBox<String> disabledCombo(String first, String... rest) {
        ComboBox<String> combo = new ComboBox<>(FXCollections.observableArrayList(values(first, rest)));
        combo.getSelectionModel().selectFirst();
        combo.setDisable(true);
        combo.setFocusTraversable(false);
        return combo;
    }

    private static List<String> values(String first, String[] rest) {
        List<String> values = new java.util.ArrayList<>();
        values.add(first);
        values.addAll(List.of(rest));
        return values;
    }

    private double pageDimensionPoints(TextField field, boolean width) {
        if (displayedStyle != null) {
            double current = width ? displayedStyle.width() : displayedStyle.height();
            if (sameDisplayedMillimeters(field, current)) {
                return current;
            }
        }
        return parseMillimeters(field);
    }

    private double marginPoints(TextField field, double currentPoints) {
        if (Double.isFinite(currentPoints) && sameDisplayedMillimeters(field, currentPoints)) {
            return currentPoints;
        }
        return parseMillimeters(field);
    }

    private static boolean sameDisplayedMillimeters(TextField field, double points) {
        String text = field.getText() == null ? "" : field.getText().trim().replace(',', '.');
        return text.equals(formatMillimeters(points));
    }

    private static double parseMillimeters(TextField field) {
        String text = field.getText() == null ? "" : field.getText().trim().replace(',', '.');
        if (text.isEmpty()) {
            throw new IllegalArgumentException("Page setup value is required");
        }
        try {
            return millimetersToPoints(Double.parseDouble(text));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Page setup value must be a number");
        }
    }

    private static String formatMillimeters(double points) {
        return String.format(Locale.ROOT, "%.2f", pointsToMillimeters(points));
    }

    private static double millimetersToPoints(double millimeters) {
        return millimeters * 72.0 / 25.4;
    }

    private static double pointsToMillimeters(double points) {
        return points * 25.4 / 72.0;
    }

    private enum PaperPreset {
        A4(PageSize.A4),
        LETTER(PageSize.LETTER),
        LEGAL(PageSize.LEGAL),
        CUSTOM(null);

        private static final double PRESET_TOLERANCE = 0.75;
        private final PageSize size;

        PaperPreset(PageSize size) {
            this.size = size;
        }

        PageSize size() {
            if (size == null) {
                throw new IllegalStateException("Custom paper size must be read from the fields");
            }
            return size;
        }

        static PaperPreset from(PageStyle style) {
            double shortSide = Math.min(style.width(), style.height());
            double longSide = Math.max(style.width(), style.height());
            for (PaperPreset preset : List.of(A4, LETTER, LEGAL)) {
                if (same(shortSide, Math.min(preset.size.width(), preset.size.height()))
                        && same(longSide, Math.max(preset.size.width(), preset.size.height()))) {
                    return preset;
                }
            }
            return CUSTOM;
        }

        private static boolean same(double first, double second) {
            return Math.abs(first - second) <= PRESET_TOLERANCE;
        }

        @Override
        public String toString() {
            return switch (this) {
                case A4 -> "A4";
                case LETTER -> "Letter";
                case LEGAL -> "Legal";
                case CUSTOM -> "Custom";
            };
        }
    }

    private static final class PageStyleCommand implements UndoableCommand {
        private final EditorSession session;
        private final Document oldDocument;
        private final Document newDocument;

        PageStyleCommand(EditorSession session, PageStyle newPageStyle) {
            this.session = Objects.requireNonNull(session, "session");
            this.oldDocument = session.document();
            this.newDocument = oldDocument.withPageStyle(Objects.requireNonNull(newPageStyle, "newPageStyle"));
        }

        @Override
        public String description() {
            return "Page Setup";
        }

        @Override
        public void execute() {
            session.setDocument(newDocument);
        }

        @Override
        public void undo() {
            session.setDocument(oldDocument);
        }
    }
}
