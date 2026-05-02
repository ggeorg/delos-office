package io.github.ggeorg.delos.writer.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ViewThemeDefaultsContractTest {
    @Test
    void viewThemeOwnsCompactViewportSpacingAndKeepsPagesFlat() {
        ViewTheme theme = ViewTheme.defaultTheme();

        assertTrue(theme.outerPadding() >= 12.0 && theme.outerPadding() <= 24.0,
                "outer padding should be compact so the first page sits near the ruler");
        assertTrue(theme.interPageGap() >= 24.0, "inter-page gap should leave clear separation between pages");
        assertEquals(0.0, theme.shadowExtentX(), 0.001);
        assertEquals(0.0, theme.shadowExtentY(), 0.001);
    }
}
