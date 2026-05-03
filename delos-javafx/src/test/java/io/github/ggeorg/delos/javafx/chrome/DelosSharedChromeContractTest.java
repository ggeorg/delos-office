package io.github.ggeorg.delos.javafx.chrome;

import io.github.ggeorg.delos.javafx.DelosStylesheets;
import io.github.ggeorg.delos.javafx.icon.DelosIconId;
import io.github.ggeorg.delos.javafx.inspector.DelosInspector;
import io.github.ggeorg.delos.javafx.inspector.FormRow;
import io.github.ggeorg.delos.javafx.inspector.InspectorSection;
import io.github.ggeorg.delos.javafx.inspector.InspectorTab;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DelosSharedChromeContractTest {
    @Test
    void sharedJavaFxModuleOwnsIkonliAndChromeHelpers() throws IOException {
        String pom = Files.readString(Path.of("pom.xml"));
        String moduleInfo = Files.readString(Path.of("src/main/java/module-info.java"));

        assertTrue(pom.contains("ikonli-javafx"));
        assertTrue(pom.contains("ikonli-materialdesign-pack"));
        assertTrue(moduleInfo.contains("org.kordamp.ikonli.core"));
        assertTrue(moduleInfo.contains("org.kordamp.ikonli.javafx"));
        assertTrue(moduleInfo.contains("org.kordamp.ikonli.materialdesign"));
        assertTrue(moduleInfo.contains("io.github.ggeorg.delos.javafx.chrome"));
        assertTrue(moduleInfo.contains("io.github.ggeorg.delos.javafx.icon"));
    }

    @Test
    void sharedInspectorControlsLiveInExportedInspectorPackage() {
        assertEquals("io.github.ggeorg.delos.javafx.inspector", DelosInspector.class.getPackageName());
        assertEquals("io.github.ggeorg.delos.javafx.inspector", InspectorSection.class.getPackageName());
        assertEquals("io.github.ggeorg.delos.javafx.inspector", InspectorTab.class.getPackageName());
        assertEquals("io.github.ggeorg.delos.javafx.inspector", FormRow.class.getPackageName());
    }

    @Test
    void sharedChromePackageKeepsMenuToolbarAndSegmentedControlsOnly() {
        assertEquals("io.github.ggeorg.delos.javafx.chrome", DelosMenus.class.getPackageName());
        assertEquals("io.github.ggeorg.delos.javafx.chrome", DelosToolBars.class.getPackageName());
        assertEquals("io.github.ggeorg.delos.javafx.chrome", DelosToolbarGroup.class.getPackageName());
        assertEquals("io.github.ggeorg.delos.javafx.chrome", SegmentedControl.class.getPackageName());
        assertEquals("io.github.ggeorg.delos.javafx.chrome", SegmentedOption.class.getPackageName());
    }

    @Test
    void appCodeCanUseStableDelosIconIdsInsteadOfIkonliTypes() {
        assertTrue(DelosIconId.SAVE.iconLiteral().startsWith("mdi-"));
        assertTrue(DelosIconId.PRINT.iconLiteral().startsWith("mdi-"));
        assertTrue(DelosIconId.IMAGE.iconLiteral().startsWith("mdi-"));
    }

    @Test
    void sharedCssIsInstalledThroughOneSmallHelper() {
        assertTrue(DelosStylesheets.class.getName().endsWith("DelosStylesheets"));
    }
}
