package io.github.ggeorg.delos.writer.document;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TableCellStoryModelContractTest {
    @Test
    void tableCellOwnsStoryButKeepsParagraphProjection() {
        TableCell cell = new TableCell(Story.ofParagraphs(List.of(Paragraph.of("alpha"))));

        assertEquals("alpha", cell.content().paragraphs().get(0).plainText());
        assertEquals("alpha", cell.paragraphs().get(0).plainText());
        assertEquals(1, cell.content().blocks().size());
    }

    @Test
    void paragraphConstructorStillBuildsStoryBackedCell() {
        TableCell cell = new TableCell(List.of(Paragraph.of("legacy")));

        assertEquals("legacy", cell.content().paragraphs().get(0).plainText());
        assertEquals("legacy", cell.paragraphs().get(0).plainText());
    }
}
