package io.github.ggeorg.delos.writer.layout;

import io.github.ggeorg.delos.render.RenderFont;
import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.document.PageStyle;
import io.github.ggeorg.delos.writer.document.Paragraph;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LayoutInputsTest {
    private static final LayoutTheme THEME = LayoutTheme.defaultTheme();

    @Test
    void changingDocumentTitleDoesNotInvalidatePaginationCompatibility() {
        Document first = new Document(
            "First Title",
            PageStyle.a4Default(),
            List.of(Paragraph.of("Body"))
        );
        Document second = new Document(
            "Second Title",
            PageStyle.a4Default(),
            first.paragraphs()
        );

        assertTrue(LayoutInputs.from(first, THEME).compatibleWith(LayoutInputs.from(second, THEME)));
    }

    @Test
    void changingBodyFontInvalidatesPaginationCompatibility() {
        LayoutInputs first = LayoutInputs.from(Document.sample(), THEME);
        LayoutTheme changedTheme = withBodyFont(new RenderFont(
            THEME.bodyFont().family(),
            THEME.bodyFont().size() + 1.0,
            THEME.bodyFont().bold(),
            THEME.bodyFont().italic()
        ));

        assertFalse(first.compatibleWith(LayoutInputs.from(Document.sample(), changedTheme)));
    }

    private static LayoutTheme withBodyFont(RenderFont bodyFont) {
        return new LayoutTheme(
            THEME.titleFont(),
            bodyFont,
            THEME.titleGap(),
            THEME.titleLineGap(),
            THEME.separatorGap(),
            THEME.paragraphSpacing(),
            THEME.bodyLineGap()
        );
    }
}
