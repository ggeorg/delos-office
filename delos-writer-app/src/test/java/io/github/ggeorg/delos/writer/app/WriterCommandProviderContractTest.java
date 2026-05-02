package io.github.ggeorg.delos.writer.app;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class WriterCommandProviderContractTest {
    private static final Path COMMAND_PROVIDER = Path.of(
            "src/main/java/io/github/ggeorg/delos/writer/app/WriterCommandProvider.java"
    );

    @Test
    void registersExpectedFileEditInsertFormatViewToolsAndApplicationCommands() throws IOException {
        String source = Files.readString(COMMAND_PROVIDER);

        for (String commandId : List.of(
                "file.new",
                "file.open",
                "file.save",
                "file.saveAs",
                "file.exportPdf",
                "file.print",
                "export.html",
                "export.markdown",
                "edit.undo",
                "edit.redo",
                "edit.redo.alt",
                "edit.copy",
                "edit.cut",
                "edit.paste",
                "edit.selectAll",
                "edit.find",
                "edit.formula",
                "edit.imageProperties",
                "insert.pageBreak",
                "insert.image",
                "image.replace",
                "insert.table",
                "table.insertRowAbove",
                "table.insertRowBelow",
                "table.deleteRow",
                "table.insertColumnLeft",
                "table.insertColumnRight",
                "table.deleteColumn",
                "insert.formula",
                "format.bold",
                "format.italic",
                "format.underline",
                "format.strikethrough",
                "format.textColor",
                "format.clearFormatting",
                "format.alignLeft",
                "format.alignCenter",
                "format.alignRight",
                "format.justify",
                "format.bulletedList",
                "format.numberedList",
                "format.decreaseListLevel",
                "format.increaseListLevel",
                "format.lineSpacing",
                "view.commandPalette",
                "view.zoomIn",
                "view.zoomOut",
                "view.zoomReset",
                "view.zoomFitWidth",
                "view.zoom50",
                "view.zoom75",
                "view.zoom90",
                "view.zoom100",
                "view.zoom125",
                "view.zoom150",
                "view.zoom200",
                "view.toggleRuler",
                "view.toggleInspector",
                "tools.wordCount",
                "app.preferences",
                "app.about"
        )) {
            assertRegistered(source, commandId);
        }
    }

    @Test
    void exportCommandsStayRegisteredAndGroupedUnderExport() throws IOException {
        String source = Files.readString(COMMAND_PROVIDER);

        assertTrue(source.contains("register(\"export.html\", \"Export HTML\", \"Export\""));
        assertTrue(source.contains("register(\"export.markdown\", \"Export Markdown\", \"Export\""));
        assertTrue(source.contains("register(\"file.exportPdf\", \"Export PDF…\", \"File\", null, fileController::exportPdf)"));
    }

    @Test
    void printAndListCommandsAreEnabledAndUnsupportedTraditionalCommandsRemainDisabledPlaceholders() throws IOException {
        String source = Files.readString(COMMAND_PROVIDER);

        assertTrue(source.contains("register(\"file.print\", \"Print…\", \"File\""));
        assertTrue(source.contains("fileController::printDocument"));

        assertTrue(source.contains("register(\"format.bulletedList\""));
        assertTrue(source.contains("editor.toggleListKind(ListMarkerKind.BULLET)"));
        assertTrue(source.contains("register(\"format.numberedList\""));
        assertTrue(source.contains("editor.toggleListKind(ListMarkerKind.NUMBERED)"));
        assertTrue(source.contains("register(\"format.decreaseListLevel\""));
        assertTrue(source.contains("editor::decreaseListLevel"));
        assertTrue(source.contains("register(\"format.increaseListLevel\""));
        assertTrue(source.contains("editor::increaseListLevel"));

        assertTrue(source.contains("register(\"image.replace\", \"Replace Image…\", \"Format\""));
        assertTrue(source.contains("insertController::insertImage, editor::hasSelectedImageBlock"));

        assertTrue(source.contains("registerDisabled(\"insert.pageBreak\""));
        assertTrue(source.contains("registerDisabled(\"format.lineSpacing\""));
    }

    private static void assertRegistered(String source, String commandId) {
        boolean registeredDirectly = source.contains("register(\"" + commandId + "\"");
        boolean registeredAsZoomPreset = source.contains("registerZoomPreset(\"" + commandId + "\"");
        boolean registeredDisabled = source.contains("registerDisabled(\"" + commandId + "\"");

        assertTrue(registeredDirectly || registeredAsZoomPreset || registeredDisabled, () -> "Missing command: " + commandId);
    }
}
