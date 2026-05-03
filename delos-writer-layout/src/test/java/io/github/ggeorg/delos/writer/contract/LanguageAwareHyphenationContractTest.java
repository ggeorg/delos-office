package io.github.ggeorg.delos.writer.contract;

import io.github.ggeorg.delos.render.RenderFont;
import io.github.ggeorg.delos.writer.document.Paragraph;
import io.github.ggeorg.delos.writer.document.ParagraphStyle;
import io.github.ggeorg.delos.writer.layout.KnuthPlassParagraphLayouter;
import io.github.ggeorg.delos.writer.layout.LaidOutLine;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LanguageAwareHyphenationContractTest {
    @Test
    void defaultEnglishParagraphsStillHyphenate() {
        Paragraph paragraph = Paragraph.of(ParagraphStyle.defaultBody(), "demonstration");

        List<LaidOutLine> lines = new KnuthPlassParagraphLayouter().layoutLines(
                paragraph,
                new RenderFont("System", 14, false, false),
                48,
                4
        );

        assertTrue(lines.stream().limit(lines.size() - 1L).anyMatch(line -> line.text().endsWith("-")),
                "default en-US paragraphs should preserve current English hyphenation behavior");
    }

    @Test
    void missingOrUnknownParagraphLanguageDisablesAutomaticHyphenationSafely() {
        RenderFont font = new RenderFont("System", 14, false, false);
        KnuthPlassParagraphLayouter layouter = new KnuthPlassParagraphLayouter();

        Paragraph missingLanguage = Paragraph.of(ParagraphStyle.defaultBody().withoutLanguageTag(), "demonstration");
        Paragraph unknownLanguage = Paragraph.of(ParagraphStyle.defaultBody().withLanguageTag("zz-ZZ"), "demonstration");

        assertFalse(hasVisibleAutomaticHyphen(layouter.layoutLines(missingLanguage, font, 48, 4)));
        assertFalse(hasVisibleAutomaticHyphen(layouter.layoutLines(unknownLanguage, font, 48, 4)));
    }

    private static boolean hasVisibleAutomaticHyphen(List<LaidOutLine> lines) {
        return lines.stream().limit(Math.max(0L, lines.size() - 1L)).anyMatch(line -> line.text().endsWith("-"));
    }
}
