package io.github.ggeorg.delos.writer.editor;

import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.document.PageStyle;
import io.github.ggeorg.delos.writer.document.Paragraph;
import io.github.ggeorg.delos.writer.document.ParagraphBlock;
import io.github.ggeorg.delos.writer.document.TableBlock;
import io.github.ggeorg.delos.writer.document.TableCell;
import io.github.ggeorg.delos.writer.document.TableCellSelection;
import io.github.ggeorg.delos.writer.document.TableRow;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TableCellStoryEditingContractTest {
    @Test
    void replacingTableCellTextUpdatesTheCellStory() {
        TableBlock table = new TableBlock(List.of(
                new TableRow(List.of(TableCell.blank()))
        ));
        Document document = Document.fromBlocks(
                "Table Story Test",
                PageStyle.a4Default(),
                List.of(table, new ParagraphBlock(Paragraph.of("")))
        );

        DocumentEdit edit = new DocumentEditor().replaceTableCellText(
                document,
                new TableCellSelection(0, 0, 0),
                "Alpha\nBeta",
                "Replace Table Cell"
        );

        TableBlock updatedTable = (TableBlock) edit.document().blocks().get(0);
        TableCell updatedCell = updatedTable.rows().get(0).cells().get(0);

        assertEquals(2, updatedCell.content().paragraphs().size());
        assertEquals("Alpha", updatedCell.content().paragraphs().get(0).plainText());
        assertEquals("Beta", updatedCell.content().paragraphs().get(1).plainText());
        assertEquals(
                "Alpha\nBeta",
                new DocumentEditor().tableCellPlainText(edit.document(), new TableCellSelection(0, 0, 0))
        );
    }
}
