package io.github.ggeorg.delos.writer.layout;

import io.github.ggeorg.delos.writer.document.CharacterStyle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LaidOutRunLinkContractTest {
    @Test
    void linkedRunsAreVisiblyUnderlinedByRenderPolicy() {
        LaidOutRun run = new LaidOutRun(
                "Delos",
                0,
                5,
                0,
                42,
                CharacterStyle.PLAIN.withLinkHref("https://example.com")
        );

        assertTrue(run.underline());
    }
}
