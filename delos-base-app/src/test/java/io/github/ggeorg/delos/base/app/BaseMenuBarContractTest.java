package io.github.ggeorg.delos.base.app;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class BaseMenuBarContractTest {
    @Test
    void menuBarUsesTraditionalDatabaseApplicationMenus() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/io/github/ggeorg/delos/base/app/BaseMenuBar.java"
        ));

        assertTrue(source.contains("setUseSystemMenuBar(true)"));
        assertTrue(source.contains("item(\"insert.table\")"));
        assertTrue(source.contains("item(\"insert.query\")"));
        assertTrue(source.contains("item(\"insert.form\")"));
        assertTrue(source.contains("item(\"insert.report\")"));
        assertTrue(source.contains("item(\"tools.sqlConsole\")"));
        assertTrue(source.contains("item(\"tools.relationships\")"));
    }
}
