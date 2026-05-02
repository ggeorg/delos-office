package io.github.ggeorg.delos.writer.io;

import io.github.ggeorg.delos.writer.document.Alignment;
import io.github.ggeorg.delos.writer.document.Block;
import io.github.ggeorg.delos.writer.document.CharacterStyle;
import io.github.ggeorg.delos.writer.document.FormulaBlock;
import io.github.ggeorg.delos.writer.document.HorizontalRuleBlock;
import io.github.ggeorg.delos.writer.document.ImageBlock;
import io.github.ggeorg.delos.writer.document.PageStyle;
import io.github.ggeorg.delos.writer.document.Paragraph;
import io.github.ggeorg.delos.writer.document.ParagraphBlock;
import io.github.ggeorg.delos.writer.document.ParagraphStyle;
import io.github.ggeorg.delos.writer.document.Story;
import io.github.ggeorg.delos.writer.document.TableBlock;
import io.github.ggeorg.delos.writer.document.TableCell;
import io.github.ggeorg.delos.writer.document.TableRow;
import io.github.ggeorg.delos.writer.document.TextRun;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentMarkdownExporterTest {
    private final DocumentMarkdownExporter exporter = new DocumentMarkdownExporter();

    @Test
    void exportsMarkdownHeadingParagraphStylesAndInlineFormatting() {
        Paragraph paragraph = new Paragraph(
                new ParagraphStyle(Alignment.JUSTIFY, 24, 6, 10, 1.15),
                List.of(
                        new TextRun("Delos ", CharacterStyle.PLAIN),
                        new TextRun("bold", CharacterStyle.PLAIN.withBold(true)),
                        new TextRun(" and ", CharacterStyle.PLAIN),
                        new TextRun("italic", CharacterStyle.PLAIN.withItalic(true)),
                        new TextRun(" with ", CharacterStyle.PLAIN),
                        new TextRun("soft\u00ADhint", CharacterStyle.PLAIN.withUnderline(true))
                )
        );

        String markdown = exporter.toMarkdown(
                "Roadmap",
                PageStyle.a4Default(),
                List.of(new ParagraphBlock(paragraph))
        );

        assertTrue(markdown.contains("# Roadmap"));
        assertTrue(markdown.contains("<!-- delos-paragraph: alignment=justify; firstLineIndent=24pt; spacingBefore=6pt; spacingAfter=10pt; lineHeight=1.15 -->"));
        assertTrue(markdown.contains("**bold**"));
        assertTrue(markdown.contains("*italic*"));
        assertTrue(markdown.contains("<span style=\"text-decoration: underline\">soft&shy;hint</span>"));
    }

    @Test
    void exportsHorizontalRulesImagesAndTablesWithReadableMarkdownFallbacks() {
        List<Block> blocks = List.of(
                new HorizontalRuleBlock(),
                new ImageBlock("images/diagram.png", 120, 80),
                sampleTable(2, 3)
        );

        String markdown = exporter.toMarkdown("Blocks", PageStyle.a4Default(), blocks);

        assertTrue(markdown.contains("---"));
        assertTrue(markdown.contains("![diagram.png](images/diagram.png)"));
        assertTrue(markdown.contains("<!-- delos-image: width=120pt height=80pt -->"));
        assertTrue(markdown.contains("| Column 1 | Column 2 | Column 3 |"));
        assertTrue(markdown.contains("| Cell 2,1 | Cell 2,2 | Cell 2,3 |"));
    }

    @Test
    void exportsFormulaBlocksAsMathFenceFallbacks() {
        String markdown = exporter.toMarkdown("Formula", PageStyle.a4Default(), List.of(
                new FormulaBlock("E = mc^2", "Einstein mass energy equation")
        ));

        assertTrue(markdown.contains("<!-- delos-formula: source-format=latex; alt-text=Einstein mass energy equation -->"));
        assertTrue(markdown.contains("```math\nE = mc^2\n```"));
    }

    
    void exportsTableCellStoriesThroughSharedBlockExporter() {
        TableCell cell = new TableCell(Story.ofBlocks(List.of(
                new FormulaBlock("x^2", "x squared"),
                new ImageBlock("media/cell.png", 24, 24, "cell icon")
        )));

        String markdown = exporter.toMarkdown("Cell story", PageStyle.a4Default(), List.of(
                new TableBlock(List.of(new TableRow(List.of(cell))))
        ));

        assertTrue(markdown.contains("delos-formula"));
        assertTrue(markdown.contains("x^2"));
        assertTrue(markdown.contains("![cell icon](media/cell.png)"));
    }

    private static TableBlock sampleTable(int rows, int columns) {
        return new TableBlock(java.util.stream.IntStream.rangeClosed(1, rows)
                .mapToObj(row -> new TableRow(java.util.stream.IntStream.rangeClosed(1, columns)
                        .mapToObj(column -> new TableCell(List.of(Paragraph.of(cellText(row, column)))))
                        .toList()))
                .toList());
    }

    private static String cellText(int row, int column) {
        if (row == 1) {
            return "Column " + column;
        }
        return "Cell " + row + "," + column;
    }
}
