package io.github.ggeorg.delos.writer.io;

import io.github.ggeorg.delos.writer.document.Alignment;
import io.github.ggeorg.delos.writer.document.CharacterStyle;
import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.document.FormulaBlock;
import io.github.ggeorg.delos.writer.document.FormulaSourceFormat;
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
import io.github.ggeorg.delos.writer.document.TableStyle;
import io.github.ggeorg.delos.writer.document.TextRun;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentSerializerTest {
    private final DocumentSerializer serializer = new DocumentSerializer();

    @Test
    void roundTripsDocumentThroughXmlString() {
        Document original = new Document(
                "Roadmap",
                new PageStyle(612, 792, 64, 70, 72, 68),
                List.of(
                        new Paragraph(
                                new ParagraphStyle(Alignment.CENTER, 12, 6, 10, 1.2),
                                List.of(
                                        new TextRun("Delos ", CharacterStyle.PLAIN.withBold(true).withColor("#112233")),
                                        new TextRun("v11", CharacterStyle.PLAIN.withItalic(true).withUnderline(true).withFontSize(18.0))
                                )
                        ),
                        new Paragraph(
                                new ParagraphStyle(Alignment.LEFT, 0, 0, 14, 1.0),
                                List.of(new TextRun("Native save/load baseline", CharacterStyle.PLAIN))
                        )
                )
        );

        String xml = serializer.toXml(original);
        Document restored = serializer.fromXml(xml);

        assertTrue(xml.contains("<delos-document"));
        assertEquals(original, restored);
    }

    @Test
    void roundTripsDocumentThroughStreams() throws Exception {
        Document original = new Document(
                "Blank",
                PageStyle.a4Default(),
                List.of(Paragraph.of("Hello Delos"))
        );

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        serializer.write(original, output);
        ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
        Document restored = serializer.read(input);

        assertEquals(original, restored);
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("Hello Delos"));
    }

    @Test
    void roundTripsParagraphListMetadata() {
        Paragraph paragraph = new Paragraph(
                ParagraphStyle.defaultBody().asNumberedListItem(1, 3),
                List.of(TextRun.plain("List item"))
        );
        Document original = new Document("Lists", PageStyle.a4Default(), List.of(paragraph));

        String xml = serializer.toXml(original);
        Document restored = serializer.fromXml(xml);

        assertTrue(xml.contains("list-kind=\"NUMBERED\""));
        assertTrue(xml.contains("list-level=\"1\""));
        assertTrue(xml.contains("list-start=\"3\""));
        assertEquals(original, restored);
    }

    @Test
    void roundTripsImageBlocksThroughContentXml() {
        Document original = Document.fromBlocks(
                "Images",
                PageStyle.a4Default(),
                List.of(
                        ParagraphBlock.of(Paragraph.of("before")),
                        new ImageBlock("media/image-1.png", 320, 180, "Mechanism diagram"),
                        ParagraphBlock.of(Paragraph.of("after"))
                )
        );

        String xml = serializer.toXml(original);
        Document restored = serializer.fromXml(xml);

        assertTrue(xml.contains("<image"));
        assertTrue(xml.contains("source=\"media/image-1.png\""));
        assertEquals(original, restored);
        ImageBlock image = assertInstanceOf(ImageBlock.class, restored.blocks().get(1));
        assertEquals("Mechanism diagram", image.altText());
    }

    @Test
    void roundTripsTableStyleMetadataThroughContentXml() {
        TableBlock table = TableBlock.blank(2, 2)
                .withHeaderRowCount(1)
                .withStyle(new TableStyle(0.75, 12.0, false));
        Document original = Document.fromBlocks(
                "Table Style",
                PageStyle.a4Default(),
                List.of(table)
        );

        String xml = serializer.toXml(original);
        Document restored = serializer.fromXml(xml);

        assertTrue(xml.contains("width-fraction=\"0.75\""));
        assertTrue(xml.contains("cell-padding=\"12.0\""));
        assertTrue(xml.contains("borders-enabled=\"false\""));
        assertEquals(original, restored);
    }

    @Test
    void roundTripsFormulaBlocksThroughContentXml() {
        Document original = Document.fromBlocks(
                "Formulas",
                PageStyle.a4Default(),
                List.of(
                        ParagraphBlock.of(Paragraph.of("before")),
                        new FormulaBlock(
                                FormulaSourceFormat.LATEX,
                                "E = mc^2",
                                "Einstein mass energy equation"
                        ),
                        ParagraphBlock.of(Paragraph.of("after"))
                )
        );

        String xml = serializer.toXml(original);
        Document restored = serializer.fromXml(xml);

        assertTrue(xml.contains("<formula"));
        assertTrue(xml.contains("source-format=\"latex\""));
        assertTrue(xml.contains("E = mc^2"));
        assertEquals(original, restored);
        FormulaBlock formula = assertInstanceOf(FormulaBlock.class, restored.blocks().get(1));
        assertEquals("Einstein mass energy equation", formula.altText());
    }

    @Test
    void roundTripsRichTableCellStoriesThroughContentXml() {
        TableCell cell = new TableCell(Story.ofBlocks(List.of(
                ParagraphBlock.of(Paragraph.of("before formula")),
                new FormulaBlock("E = mc^2", "mass energy"),
                new ImageBlock("media/cell-image.png", 96, 48, "cell image"),
                new HorizontalRuleBlock(),
                ParagraphBlock.of(Paragraph.of("after rule"))
        )));
        Document original = Document.fromBlocks(
                "Cell stories",
                PageStyle.a4Default(),
                List.of(
                        ParagraphBlock.of(Paragraph.of("body")),
                        new TableBlock(List.of(new TableRow(List.of(cell))))
                )
        );

        String xml = serializer.toXml(original);
        Document restored = serializer.fromXml(xml);

        assertTrue(xml.contains("<cell>"));
        assertTrue(xml.contains("<story>"));
        assertTrue(xml.contains("<formula"));
        assertTrue(xml.contains("<image"));
        assertTrue(xml.contains("<horizontal-rule"));
        assertEquals(original, restored);

        TableBlock restoredTable = assertInstanceOf(TableBlock.class, restored.blocks().get(1));
        assertEquals(
                cell.content().blocks(),
                restoredTable.rows().getFirst().cells().getFirst().content().blocks()
        );
    }

    @Test
    void readsLegacyTableCellsWithDirectParagraphChildren() {
        String legacyXml = """
                <delos-document version="1">
                  <title>Legacy cell</title>
                  <page-style width="595.0" height="842.0" margin-top="72.0" margin-right="72.0" margin-bottom="72.0" margin-left="72.0"/>
                  <blocks>
                    <table>
                      <row>
                        <cell>
                          <paragraph alignment="LEFT" first-line-indent="0.0" spacing-before="0.0" spacing-after="8.0" line-spacing-multiplier="1.0">
                            <run bold="false" italic="false" underline="false" strikethrough="false">legacy paragraph</run>
                          </paragraph>
                        </cell>
                      </row>
                    </table>
                  </blocks>
                </delos-document>
                """;

        Document restored = serializer.fromXml(legacyXml);
        String rewrittenXml = serializer.toXml(restored);

        TableBlock table = assertInstanceOf(TableBlock.class, restored.blocks().getFirst());
        assertEquals("legacy paragraph", table.rows().getFirst().cells().getFirst().paragraphs().getFirst().plainText());
        assertTrue(rewrittenXml.contains("<story>"));
    }

    @Test
    void malformedStreamReadThrowsIOException() {
        ByteArrayInputStream input = new ByteArrayInputStream("<not-delos/>".getBytes(StandardCharsets.UTF_8));

        IOException exception = assertThrows(IOException.class, () -> serializer.read(input));

        assertEquals("Invalid Delos document", exception.getMessage());
        assertTrue(exception.getCause() instanceof IllegalArgumentException);
    }


    @Test
    void roundTripsParagraphLanguageMetadataWhenExplicitlySet() {
        Paragraph paragraph = Paragraph.of(
                ParagraphStyle.defaultBody().withLanguageTag("el-GR"),
                "γειά σου"
        );
        Document original = new Document("Language", PageStyle.a4Default(), List.of(paragraph));

        String xml = serializer.toXml(original);
        Document restored = serializer.fromXml(xml);

        assertTrue(xml.contains("language=\"el-GR\""));
        assertEquals("el-GR", restored.paragraphs().getFirst().style().languageTag());
        assertEquals(original, restored);
    }
}
