package io.github.ggeorg.delos.writer.app;

import io.github.ggeorg.delos.javafx.icon.DelosIconId;
import io.github.ggeorg.delos.javafx.chrome.DelosInspector;
import io.github.ggeorg.delos.javafx.chrome.FormRow;
import io.github.ggeorg.delos.javafx.chrome.InspectorSection;
import io.github.ggeorg.delos.javafx.chrome.InspectorTab;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.List;

/** Writer-specific inspector content hosted by the shared Delos inspector shell. */
final class WriterInspectorPane extends DelosInspector {
    WriterInspectorPane() {
        getStyleClass().add("writer-inspector");
        setInspectorTabs(List.of(
                new InspectorTab("document", "Document", DelosIconId.SHEET, documentTab()),
                new InspectorTab("format", "Format", DelosIconId.TEXT_BOX, formatTab()),
                new InspectorTab("arrange", "Arrange", DelosIconId.SHAPE, arrangeTab())
        ));
    }

    private static Node documentTab() {
        VBox content = inspectorContent();
        content.getChildren().setAll(
                new InspectorSection("Page Setup", pageSetupPlaceholder()),
                new InspectorSection("Document Info", documentInfoPlaceholder())
        );
        return content;
    }

    private static Node formatTab() {
        VBox content = inspectorContent();
        content.getChildren().setAll(
                new InspectorSection("Text", textPlaceholder()),
                new InspectorSection("Paragraph", paragraphPlaceholder()),
                new InspectorSection("Lists", listPlaceholder()),
                new InspectorSection("Image", imagePlaceholder()),
                new InspectorSection("Table", tablePlaceholder())
        );
        return content;
    }

    private static Node arrangeTab() {
        VBox content = inspectorContent();
        content.getChildren().setAll(
                new InspectorSection("Position", disabledMessage("Select an image, table, or object to arrange it.")),
                new InspectorSection("Wrapping", disabledMessage("Inline objects are supported first. Rich wrapping comes later."))
        );
        return content;
    }

    private static VBox pageSetupPlaceholder() {
        VBox box = formBox();
        box.getChildren().setAll(
                new FormRow("Paper", disabledCombo("A4", "Letter", "Legal", "Custom")),
                new FormRow("Orientation", disabledCombo("Portrait", "Landscape")),
                new FormRow("Top", disabledText("20 mm")),
                new FormRow("Bottom", disabledText("20 mm")),
                new FormRow("Left", disabledText("20 mm")),
                new FormRow("Right", disabledText("20 mm")),
                disabledMessage("Page setup will update the document model in the next step; PDF export stays headless.")
        );
        return box;
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
}
