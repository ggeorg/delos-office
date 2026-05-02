package io.github.ggeorg.delos.writer.app;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class WriterImagePropertiesContractTest {
    private static final Path INSERT_CONTROLLER = Path.of(
            "src/main/java/io/github/ggeorg/delos/writer/app/WriterInsertController.java"
    );
    private static final Path COMMAND_PROVIDER = Path.of(
            "src/main/java/io/github/ggeorg/delos/writer/app/WriterCommandProvider.java"
    );
    private static final Path MENU_BAR = Path.of(
            "src/main/java/io/github/ggeorg/delos/writer/app/WriterMenuBar.java"
    );

    @Test
    void imagePropertiesDialogPrefillsSelectedImageAndUpdatesOnlyMutableBlockProperties() throws IOException {
        String source = Files.readString(INSERT_CONTROLLER);

        assertTrue(source.contains("void editSelectedImageProperties()"));
        assertTrue(source.contains("ImageBlock selected = editor.selectedImageBlock()"));
        assertTrue(source.contains("chooseImageProperties(selected)"));
        assertTrue(source.contains("new SpinnerValueFactory.DoubleSpinnerValueFactory"));
        assertTrue(source.contains("new TextField(imageBlock.altText())"));
        assertTrue(source.contains("editor.updateSelectedImageProperties(input.width(), input.height(), input.altText())"));
        assertTrue(source.contains("new ImagePropertiesInput(width.getValue(), height.getValue(), altText.getText())"));
    }

    @Test
    void imagePropertiesCommandIsEnabledOnlyForSelectedImageBlocks() throws IOException {
        String source = Files.readString(COMMAND_PROVIDER);

        assertTrue(source.contains("register(\"edit.imageProperties\", \"Image Properties…\", \"Edit\""));
        assertTrue(source.contains("insertController::editSelectedImageProperties"));
        assertTrue(source.contains("editor::hasSelectedImageBlock"));
    }

    @Test
    void editMenuSurfacesImageAndFormulaPropertyCommands() throws IOException {
        String source = Files.readString(MENU_BAR);

        assertTrue(source.contains("item(\"edit.formula\")"));
        assertTrue(source.contains("item(\"edit.imageProperties\")"));
    }
}
