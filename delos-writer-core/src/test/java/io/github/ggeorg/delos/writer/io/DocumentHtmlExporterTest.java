package io.github.ggeorg.delos.writer.io;

import io.github.ggeorg.delos.writer.document.Alignment;
import io.github.ggeorg.delos.writer.document.Block;
import io.github.ggeorg.delos.writer.document.CharacterStyle;
import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.document.FormulaBlock;
import io.github.ggeorg.delos.writer.document.HorizontalRuleBlock;
import io.github.ggeorg.delos.writer.document.ImageBlock;
import io.github.ggeorg.delos.writer.document.PageStyle;
import io.github.ggeorg.delos.writer.document.Paragraph;
import io.github.ggeorg.delos.writer.document.ParagraphStyle;
import io.github.ggeorg.delos.writer.document.Story;
import io.github.ggeorg.delos.writer.document.TableBlock;
import io.github.ggeorg.delos.writer.document.TableCell;
import io.github.ggeorg.delos.writer.document.TableRow;
import io.github.ggeorg.delos.writer.document.TextRun;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentHtmlExporterTest {
    private final DocumentHtmlExporter exporter = new DocumentHtmlExporter();

    @Test
    void exportsEscapedDocumentTitleAndStyledRuns() {
        Document document = new Document(
                "<Delos & Writer>",
                PageStyle.a4Default(),
                List.of(new Paragraph(List.of(
                        new TextRun("5 < 7 & 9 ", CharacterStyle.PLAIN),
                        new TextRun("bold", CharacterStyle.PLAIN.withBold(true).withUnderline(true)),
                        new TextRun(" and soft\u00ADhint", CharacterStyle.PLAIN.withItalic(true))
                )))
        );

        String html = exporter.toHtml(document);

        assertTrue(html.contains("<!DOCTYPE html>"));
        assertTrue(html.contains("<title>&lt;Delos &amp; Writer&gt;</title>"));
        assertTrue(html.contains("5 &lt; 7 &amp; 9 "));
        assertTrue(html.contains("font-weight: 700"));
        assertTrue(html.contains("text-decoration: underline"));
        assertTrue(html.contains("font-style: italic"));
        assertTrue(html.contains("&shy;"));
    }

    @Test
    void exportsParagraphAndPageMetricsIntoCss() {
        ParagraphStyle paragraphStyle = new ParagraphStyle(Alignment.JUSTIFY, 24, 6, 10, 1.15);
        Document document = new Document(
                "Roadmap",
                new PageStyle(595, 842, 68, 72, 72, 72),
                List.of(Paragraph.of(paragraphStyle, "Delos export baseline"))
        );

        String html = exporter.toHtml(document);

        assertTrue(html.contains("width: 595pt"));
        assertTrue(html.contains("min-height: 842pt"));
        assertTrue(html.contains("padding: 68pt 72pt 72pt 72pt"));
        assertTrue(html.contains("delos-align-justify"));
        assertTrue(html.contains("text-indent: 24pt"));
        assertTrue(html.contains("margin-top: 6pt"));
        assertTrue(html.contains("margin-bottom: 10pt"));
        assertTrue(html.contains("line-height: 1.15"));
    }

    @Test
    void exportsSemanticFallbacksForImagesTablesAndRules() {
        List<Block> blocks = List.of(
                new HorizontalRuleBlock(),
                new ImageBlock("media/chart.png", 144, 96),
                sampleTable(2, 2)
        );

        String html = exporter.toHtml("Blocks", PageStyle.a4Default(), blocks);

        assertTrue(html.contains("<hr class=\"delos-hr\">"));
        assertTrue(html.contains("<figure class=\"delos-figure\">"));
        assertTrue(html.contains("alt=\"chart.png\""));
        assertTrue(html.contains("<table class=\"delos-table\">"));
        assertTrue(html.contains("data-row=\"1\" data-column=\"1\""));
        assertTrue(html.contains("Column 1"));
        assertTrue(html.contains("data-row=\"2\" data-column=\"2\""));
        assertTrue(html.contains("Cell 2,2"));
    }

    @Test
    void exportsFormulaBlocksAsSourceBasedHtmlFallbacks() {
        String html = exporter.toHtml("Formula", PageStyle.a4Default(), List.of(
                new FormulaBlock("E = mc^2", "Einstein mass energy equation")
        ));

        assertTrue(html.contains("class=\"delos-formula\""));
        assertTrue(html.contains("data-source-format=\"latex\""));
        assertTrue(html.contains("aria-label=\"Einstein mass energy equation\""));
        assertTrue(html.contains("ƒx"));
        assertTrue(html.contains("E = mc^2"));
    }

    
    void exportsTableCellStoriesThroughSharedBlockExporter() {
        TableCell cell = new TableCell(Story.ofBlocks(List.of(
                new FormulaBlock("x^2", "x squared"),
                new ImageBlock("media/cell.png", 24, 24, "cell icon")
        )));

        String html = exporter.toHtml("Cell story", PageStyle.a4Default(), List.of(
                new TableBlock(List.of(new TableRow(List.of(cell))))
        ));

        assertTrue(html.contains("class=\"delos-formula\""));
        assertTrue(html.contains("x^2"));
        assertTrue(html.contains("media/cell.png"));
        assertTrue(html.contains("alt=\"cell icon\""));
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
