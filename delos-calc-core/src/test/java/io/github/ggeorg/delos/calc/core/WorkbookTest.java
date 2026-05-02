package io.github.ggeorg.delos.calc.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class WorkbookTest {
    @Test
    void blankWorkbookHasOneSheet() {
        Workbook workbook = Workbook.blank();

        assertEquals("Untitled", workbook.title());
        assertEquals(1, workbook.sheets().size());
        assertEquals("Sheet1", workbook.firstSheet().name());
    }

    @Test
    void replacesExistingSheetByName() {
        Workbook workbook = Workbook.blank();
        Sheet updatedSheet = workbook.firstSheet().withInput(CellAddress.parse("A1"), "100");

        Workbook updated = workbook.withSheet(updatedSheet);

        assertEquals(0, workbook.firstSheet().usedCellCount());
        assertEquals(1, updated.firstSheet().usedCellCount());
    }

    @Test
    void preventsDuplicateSheetNamesCaseInsensitively() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Workbook("Bad", List.of(Sheet.named("Sheet1"), Sheet.named("sheet1")))
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> Workbook.blank().addSheet(Sheet.named("sheet1"))
        );
    }

    @Test
    void canAddSiblingSheets() {
        Workbook workbook = Workbook.blank().addSheet(Sheet.named("Data"));

        assertEquals(2, workbook.sheets().size());
        assertTrue(workbook.findSheet("data").isPresent());
    }
}
