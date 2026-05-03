package io.github.ggeorg.delos.javafx.chrome;

import javafx.scene.control.Label;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DelosBalloonPopoverContractTest {
    @Test
    void sharedJavaFxModuleWrapsControlsFxPopoverBehindDelosApi() throws IOException {
        String pom = Files.readString(Path.of("pom.xml"));
        String moduleInfo = Files.readString(Path.of("src/main/java/module-info.java"));
        String source = Files.readString(Path.of("src/main/java/io/github/ggeorg/delos/javafx/chrome/DelosBalloonPopover.java"));

        assertTrue(pom.contains("org.controlsfx"));
        assertTrue(pom.contains("controlsfx"));
        assertTrue(moduleInfo.contains("requires org.controlsfx.controls"));
        assertTrue(source.contains("org.controlsfx.control.PopOver"));
        assertTrue(source.contains("setAutoHide(true)"));
        assertTrue(source.contains("setHideOnEscape(true)"));
        assertTrue(source.contains("setDetachable(false)"));
        assertTrue(source.contains("showBelow(Node owner"));
    }

    @Test
    void contentReceivesDelosStyleClass() {
        Label content = new Label("Name");
        DelosBalloonPopover popover = new DelosBalloonPopover(content);

        assertEquals(content, popover.content());
        assertTrue(content.getStyleClass().contains("delos-balloon-popover-content"));
    }
}
