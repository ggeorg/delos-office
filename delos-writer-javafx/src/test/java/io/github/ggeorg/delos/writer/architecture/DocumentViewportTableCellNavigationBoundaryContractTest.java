package io.github.ggeorg.delos.writer.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocumentViewportTableCellNavigationBoundaryContractTest {
    private static final Path INPUT_HANDLER = Path.of(
            "src/main/java/io/github/ggeorg/delos/writer/ui/DocumentViewportInputHandler.java"
    );

    @Test
    void selectedTableCellsUseDedicatedNavigatorForKeyboardMovement() throws IOException {
        String source = Files.readString(INPUT_HANDLER);

        assertTrue(source.contains("new TableCellNavigator()"));
        assertTrue(source.contains("tableCellNavigator.nextCell"));
        assertTrue(source.contains("tableCellNavigator.previousCell"));
        assertTrue(source.contains("tableCellNavigator.leftCell"));
        assertTrue(source.contains("tableCellNavigator.rightCell"));
        assertTrue(source.contains("tableCellNavigator.aboveCell"));
        assertTrue(source.contains("tableCellNavigator.belowCell"));
        assertTrue(source.contains("BlockNavigationSupport.textPositionAfterBlock"));
        assertTrue(source.contains("BlockNavigationSupport.textPositionBeforeBlock"));
    }
    @Test
    void tableCellCaretUsesStoryCaretEditingPath() throws IOException {
        String inputHandler = Files.readString(INPUT_HANDLER);
        String editController = Files.readString(Path.of(
                "src/main/java/io/github/ggeorg/delos/writer/ui/DocumentViewportEditController.java"
        ));

        assertTrue(inputHandler.contains("interactionModel.setStoryCaret"));
        assertTrue(inputHandler.contains("hit.storyPosition()"));
        assertTrue(inputHandler.contains("handleStoryCaretKey"));
        assertTrue(editController.contains("replaceAtStoryCaret"));
        assertTrue(editController.contains("deleteBackwardAtStoryCaret"));
        assertTrue(editController.contains("deleteForwardAtStoryCaret"));
        assertTrue(editController.contains("documentEditor.replace("));
    }

}
