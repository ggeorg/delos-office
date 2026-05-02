package io.github.ggeorg.delos.calc.core;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

final class CalcWorkbookFormatTest {
    @Test
    void roundTripsNativeWorkbook() throws Exception {
        CalcWorkbookFormat format = new CalcWorkbookFormat();
        Workbook workbook = Workbook.blank()
                .withTitle("Budget")
                .withSheet(Workbook.blank().firstSheet()
                        .withInput(CellAddress.parse("A1"), "42")
                        .withInput(CellAddress.parse("B1"), "=A1+1"));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        format.write(workbook, out);

        Workbook loaded = format.read(new ByteArrayInputStream(out.toByteArray()));

        assertEquals("Budget", loaded.title());
        assertEquals("42.0", loaded.firstSheet().cellAt(CellAddress.parse("A1")).content().displayText());
        assertEquals("=A1+1", loaded.firstSheet().cellAt(CellAddress.parse("B1")).content().displayText());
        assertTrue(new String(out.toByteArray(), StandardCharsets.UTF_8).contains("delos-workbook"));
    }
}
