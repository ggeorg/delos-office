package io.github.ggeorg.delos.writer.contract;

import io.github.ggeorg.delos.writer.document.PageStyle;
import io.github.ggeorg.delos.writer.layout.LayoutTheme;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ThemeDefaultsContractTest {

    @Test
    void defaultThemeUsesTighterBodySpacingForReadingComfort() {
        LayoutTheme theme = LayoutTheme.defaultTheme();

        assertTrue(theme.paragraphSpacing() <= 8.0, "paragraph spacing should be tighter than the early prototype defaults");
        assertTrue(theme.bodyLineGap() >= 5.0, "body line gap should allow slightly more breathing room");
    }

    @Test
    void a4DefaultKeepsStandardHorizontalMarginsAndSlightlyTighterTopMargin() {
        PageStyle style = PageStyle.a4Default();

        assertEquals(72.0, style.marginLeft(), 0.001);
        assertEquals(72.0, style.marginRight(), 0.001);
        assertEquals(68.0, style.marginTop(), 0.001);
        assertEquals(72.0, style.marginBottom(), 0.001);
    }
}
