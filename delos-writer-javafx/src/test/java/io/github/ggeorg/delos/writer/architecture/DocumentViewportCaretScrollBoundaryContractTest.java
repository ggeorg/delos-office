package io.github.ggeorg.delos.writer.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;

final class DocumentViewportCaretScrollBoundaryContractTest {
    private static final Path MAIN_SOURCES = Path.of("src/main/java/io/github/ggeorg/delos/writer/ui");

    @Test
    void oldDocumentEditGuardStateDoesNotReturnToDocumentViewport() throws IOException {
        String source = Files.readString(MAIN_SOURCES.resolve("DocumentViewport.java"));

        assertFalse(source.contains("documentEditInProgress"), "do not restore the old document edit guard flag");
        assertFalse(source.contains("beginDocumentEdit"), "do not restore the old begin-edit guard hook");
        assertFalse(source.contains("finishDocumentEdit"), "do not restore the old finish-edit guard hook");
        assertFalse(source.contains("caretScrollGeneration"), "do not restore generation-based caret scroll cancellation");
        assertFalse(source.contains("caretScrollPending"), "caret-follow debouncing belongs in CaretFollowCoordinator");
        assertFalse(source.contains("Platform.runLater"), "DocumentViewport should not use runLater for caret-follow timing");
    }

    @Test
    void editControllerDoesNotExposeDocumentEditGuardHooks() throws IOException {
        String source = Files.readString(MAIN_SOURCES.resolve("DocumentViewportEditController.java"));

        assertFalse(source.contains("beforeDocumentEdit"), "edit controller should rebuild against explicit target caret state");
        assertFalse(source.contains("afterDocumentEdit"), "edit controller should not have a finish-edit guard hook");
    }

    @Test
    void caretScrollTargetingDoesNotDependOnSceneGraphTransforms() throws IOException {
        String source = Files.readString(MAIN_SOURCES.resolve("DocumentViewportViewSynchronizer.java"));
        int start = source.indexOf("public Bounds caretBoundsInContent");
        int end = source.indexOf("public double contentWidth", start);
        String caretBoundsMethod = source.substring(start, end);

        assertFalse(caretBoundsMethod.contains(".localToParent("), "caret scroll bounds must stay model-derived");
        assertFalse(caretBoundsMethod.contains(".localToScene("), "caret scroll bounds must not wait for scene layout transforms");
        assertFalse(caretBoundsMethod.contains(".sceneToLocal("), "caret scroll bounds must not use hit-testing transforms");
    }

    @Test
    void caretFollowCoordinationIsCentralized() throws IOException {
        String viewport = Files.readString(MAIN_SOURCES.resolve("DocumentViewport.java"));
        String coordinator = Files.readString(MAIN_SOURCES.resolve("CaretFollowCoordinator.java"));

        assertFalse(viewport.contains("scrollIntoViewHandler.accept"), "viewport should not perform caret reveal directly");
        assertFalse(coordinator.contains("localToScene"), "coordinator must not use scene-graph transforms");
        assertFalse(coordinator.contains("sceneToLocal"), "coordinator must not use hit-test transforms");
    }
}
