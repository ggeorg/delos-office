package io.github.ggeorg.delos.writer.contract;

import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.document.PageStyle;
import io.github.ggeorg.delos.writer.document.Paragraph;
import io.github.ggeorg.delos.writer.layout.DocumentLayoutEngine;
import io.github.ggeorg.delos.writer.layout.ParagraphLayouter;
import io.github.ggeorg.delos.writer.layout.PaginatingDocumentLayoutEngine;
import io.github.ggeorg.delos.writer.layout.GreedyParagraphLayouter;
import io.github.ggeorg.delos.writer.layout.LayoutTheme;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class LayoutCacheValidationContractTest {
    private static final LayoutTheme THEME = LayoutTheme.defaultTheme();

    @Test
    void validationModeAcceptsIncrementalRelayoutAfterBodyEdit() {
        DocumentLayoutEngine engine = validatingEngine();
        Document original = Document.sample();
        Document edited = new Document(
                original.title(),
                original.pageStyle(),
                replaceParagraph(original.paragraphs(), 1, Paragraph.of(original.paragraphs().get(1).plainText() + " more text"))
        );

        assertDoesNotThrow(() -> {
            engine.layout(original, THEME);
            engine.layout(edited, THEME);
        });
    }

    @Test
    void validationModeAcceptsIncrementalRelayoutAfterTitleOnlyChange() {
        DocumentLayoutEngine engine = validatingEngine();
        Document original = Document.sample();
        Document retitled = new Document(
                original.title() + " (retitled)",
                original.pageStyle(),
                original.paragraphs()
        );

        assertDoesNotThrow(() -> {
            engine.layout(original, THEME);
            engine.layout(retitled, THEME);
        });
    }

    @Test
    void validationModeAcceptsParagraphFragmentationAcrossPages() {
        DocumentLayoutEngine engine = validatingEngine();
        Document document = new Document(
                "Validation",
                new PageStyle(220.0, 240.0, 20.0, 20.0, 20.0, 20.0),
                List.of(Paragraph.of(longParagraph(350)))
        );

        assertDoesNotThrow(() -> {
            engine.layout(document, THEME);
            engine.layout(document, THEME);
        });
    }

    @Test
    void validationModeAcceptsAppendParagraphWhenNoPrefixPageCanBeReused() {
        DocumentLayoutEngine engine = validatingEngine();
        Document original = new Document(
                "Enter Trace",
                PageStyle.a4Default(),
                List.of(
                        Paragraph.of("Line 1"),
                        Paragraph.of("Line 2")
                )
        );
        Document afterEnter = new Document(
                original.title(),
                original.pageStyle(),
                List.of(
                        Paragraph.of("Line 1"),
                        Paragraph.of("Line 2"),
                        Paragraph.of("")
                )
        );

        assertDoesNotThrow(() -> {
            engine.layout(original, THEME);
            engine.layout(afterEnter, THEME);
        });
    }

    @Test
    void validationModeAcceptsSecondEnterAfterMidDocumentEdit() {
        DocumentLayoutEngine engine = validatingEngine();
        Document original = new Document(
                "Enter Trace",
                PageStyle.a4Default(),
                List.of(
                        Paragraph.of("Intro paragraph remains above the edit."),
                        Paragraph.of("Paragraph 1 keeps the document long enough to exercise multi-page flow."),
                        Paragraph.of("Paragraph 2 remains after the edit.")
                )
        );
        Document afterFirstEnter = new Document(
                original.title(),
                original.pageStyle(),
                List.of(
                        original.paragraphs().get(0),
                        Paragraph.of(original.paragraphs().get(1).plainText() + " typed replacement text"),
                        Paragraph.of("."),
                        original.paragraphs().get(2)
                )
        );
        Document afterSecondEnter = new Document(
                original.title(),
                original.pageStyle(),
                List.of(
                        original.paragraphs().get(0),
                        Paragraph.of(original.paragraphs().get(1).plainText() + " typed replacement text"),
                        Paragraph.of(""),
                        Paragraph.of("."),
                        original.paragraphs().get(2)
                )
        );

        assertDoesNotThrow(() -> {
            engine.layout(original, THEME);
            engine.layout(afterFirstEnter, THEME);
            engine.layout(afterSecondEnter, THEME);
        });
    }

    private static DocumentLayoutEngine validatingEngine() {
        ParagraphLayouter layouter = new GreedyParagraphLayouter();
        return new PaginatingDocumentLayoutEngine(layouter, true);
    }

    private static List<Paragraph> replaceParagraph(List<Paragraph> paragraphs, int index, Paragraph replacement) {
        return java.util.stream.IntStream.range(0, paragraphs.size())
                .mapToObj(i -> i == index ? replacement : paragraphs.get(i))
                .toList();
    }

    private static String longParagraph(int repetitions) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < repetitions; i++) {
            builder.append("Delos validates incremental and cold layouts against each other ");
        }
        return builder.toString();
    }
}
