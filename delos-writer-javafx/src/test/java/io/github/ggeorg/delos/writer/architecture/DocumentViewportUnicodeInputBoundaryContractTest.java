package io.github.ggeorg.delos.writer.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocumentViewportUnicodeInputBoundaryContractTest {
    private static final Path MAIN_SOURCES = Path.of("src/main/java/io/github/ggeorg/delos/writer/ui");

    @Test
    void committedUnicodeTextStaysOnPlainKeyTypedPath() throws IOException {
        String inputHandler = Files.readString(MAIN_SOURCES.resolve("DocumentViewportInputHandler.java"));

        assertTrue(inputHandler.contains("String text = event.getCharacter()"), "KEY_TYPED must use JavaFX committed character text");
        assertTrue(inputHandler.contains("editController.replaceSelection(text, \"Insert Text\")"), "committed Unicode text must be inserted directly");
        assertFalse(inputHandler.contains("keyTypedTextResolver"), "normal Unicode typing must not be routed through IME composition filtering");
    }

    @Test
    void inputMethodCompositionInstallsTheRichTextFxPair() throws IOException {
        String viewport = Files.readString(MAIN_SOURCES.resolve("DocumentViewport.java"));
        String controller = Files.readString(MAIN_SOURCES.resolve("InputMethodController.java"));

        assertTrue(viewport.contains("setOnInputMethodTextChanged(inputMethodController::handleInputMethodTextChanged)"),
                "IME composition needs the input-method event handler");
        assertTrue(viewport.contains("setInputMethodRequests(inputMethodController.requests())"),
                "IME composition also needs InputMethodRequests; RichTextFX explicitly requires both");
        assertTrue(controller.contains("imStart"), "composition must track the current pre-edit range start");
        assertTrue(controller.contains("imLength"), "composition must track the current pre-edit range length");
        assertTrue(controller.contains("editController.replaceSelection(replacement"),
                "composition text must be represented as a real editable text range, not only as an overlay");
    }
}
