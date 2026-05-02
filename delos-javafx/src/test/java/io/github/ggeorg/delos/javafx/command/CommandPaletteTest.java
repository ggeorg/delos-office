package io.github.ggeorg.delos.javafx.command;

import io.github.ggeorg.delos.javafx.JavaFxTestSupport;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

final class CommandPaletteTest extends JavaFxTestSupport {

    @Test
    void filtersCommandsWithRegistrySearchAndShowsCount() {
        AtomicInteger boldInvocations = new AtomicInteger();
        CommandRegistry registry = new CommandRegistry();
        registry.register(new EditorCommand("format.bold", "Bold", "Format",
                new KeyCodeCombination(KeyCode.B, KeyCombination.SHORTCUT_DOWN), boldInvocations::incrementAndGet, null, null));
        registry.register(new EditorCommand("format.alignCenter", "Align Center", "Format", null, () -> { }, null, null));
        registry.register(new EditorCommand("file.save", "Save", "File", null, () -> { }, null, null));

        CommandPalette palette = onFxThread(() -> new CommandPalette(registry));

        onFxThread(() -> {
            palette.showPalette();
            palette.applyQuery("alce");
        });

        assertTrue(onFxThread(palette::isOpen));
        assertEquals(1, onFxThread(() -> palette.resultList().getItems().size()));
        assertEquals("Align Center", onFxThread(() -> palette.resultList().getItems().getFirst().label()));
        assertEquals("1 of 3 commands", onFxThread(() -> palette.countLabel().getText()));
    }

    @Test
    void enterExecutesSelectedCommandAndInvokesCloseHook() {
        AtomicInteger invocations = new AtomicInteger();
        AtomicInteger closes = new AtomicInteger();
        CommandRegistry registry = new CommandRegistry();
        registry.register(new EditorCommand("format.bold", "Bold", "Format",
                new KeyCodeCombination(KeyCode.B, KeyCombination.SHORTCUT_DOWN), invocations::incrementAndGet, null, null));

        CommandPalette palette = onFxThread(() -> new CommandPalette(registry));
        onFxThread(() -> {
            palette.setOnCommandExecuted(closes::incrementAndGet);
            palette.showPalette();
            palette.searchField().fireEvent(new KeyEvent(
                    KeyEvent.KEY_PRESSED,
                    "",
                    "",
                    KeyCode.ENTER,
                    false,
                    false,
                    false,
                    false
            ));
        });

        assertEquals(1, invocations.get());
        assertEquals(1, closes.get());
    }

    @Test
    void typingWhileResultListHasFocusContinuesFilteringInSearchField() {
        CommandRegistry registry = new CommandRegistry();
        registry.register(new EditorCommand("format.bold", "Bold", "Format", null, () -> { }, null, null));
        registry.register(new EditorCommand("file.save", "Save", "File", null, () -> { }, null, null));

        CommandPalette palette = onFxThread(() -> new CommandPalette(registry));
        onFxThread(() -> {
            palette.showPalette();
            palette.resultList().requestFocus();
            palette.resultList().fireEvent(new KeyEvent(
                    KeyEvent.KEY_PRESSED,
                    "b",
                    "b",
                    KeyCode.B,
                    false,
                    false,
                    false,
                    false
            ));
        });

        assertEquals("b", onFxThread(() -> palette.searchField().getText()));
        assertEquals(1, onFxThread(() -> palette.resultList().getItems().size()));
        assertEquals("Bold", onFxThread(() -> palette.resultList().getItems().getFirst().label()));
    }

}
