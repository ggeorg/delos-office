package io.github.ggeorg.delos.javafx.command;

import io.github.ggeorg.delos.javafx.JavaFxTestSupport;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CommandRegistryTest extends JavaFxTestSupport {

    @Test
    void registersCommandsAndFindsThemByIdAndAccelerator() {
        CommandRegistry registry = new CommandRegistry();
        EditorCommand save = command("file.save", "Save", new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN));

        registry.register(save);

        assertEquals(save, registry.byId("file.save").orElseThrow());
        assertEquals(save, registry.byAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN)).orElseThrow());
    }

    @Test
    void rejectsDuplicateIdsAndAccelerators() {
        CommandRegistry registry = new CommandRegistry();
        registry.register(command("file.save", "Save", new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN)));

        assertThrows(IllegalArgumentException.class,
                () -> registry.register(command("file.save", "Save As", new KeyCodeCombination(KeyCode.A, KeyCombination.SHORTCUT_DOWN))));
        assertThrows(IllegalArgumentException.class,
                () -> registry.register(command("file.saveAs", "Save As", new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN))));
    }

    @Test
    void searchPrefersPrefixMatchesThenFuzzyMatches() {
        CommandRegistry registry = new CommandRegistry();
        registry.register(command("format.alignCenter", "Align Center", null));
        registry.register(command("format.alignLeft", "Align Left", null));
        registry.register(command("format.bold", "Bold", null));
        registry.register(command("app.about", "About Delos", null));

        List<String> labels = registry.search("alce").stream().map(EditorCommand::label).toList();
        assertIterableEquals(List.of("Align Center"), labels);

        List<String> prefixLabels = registry.search("ali").stream().map(EditorCommand::label).toList();
        assertEquals("Align Center", prefixLabels.getFirst());
        assertTrue(prefixLabels.contains("Align Left"));
    }

    @Test
    void installAcceleratorsBindsSceneActionsThroughRegistry() {
        CommandRegistry registry = new CommandRegistry();
        AtomicInteger executions = new AtomicInteger();
        KeyCodeCombination shortcut = new KeyCodeCombination(KeyCode.B, KeyCombination.SHORTCUT_DOWN);
        registry.register(new EditorCommand("format.bold", "Bold", "Format", shortcut,
                executions::incrementAndGet, null, null));

        Scene scene = new Scene(new StackPane());
        registry.installAccelerators(scene);
        scene.getAccelerators().get(shortcut).run();
        registry.uninstallAccelerators(scene);

        assertEquals(1, executions.get());
        assertTrue(scene.getAccelerators().isEmpty());
    }

    private static EditorCommand command(String id, String label, KeyCodeCombination accelerator) {
        return new EditorCommand(id, label, "Test", accelerator, () -> { }, null, null);
    }
}
