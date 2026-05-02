package io.github.ggeorg.delos.calc.core;

import java.util.Objects;
import java.util.function.Function;

/**
 * Pure workbook editing facade.
 *
 * <p>The JavaFX layer should express user actions through this class instead of
 * rewriting workbook internals directly. It is intentionally immutable: every
 * operation returns a new editor wrapping a new workbook snapshot.</p>
 */
public final class WorkbookEditor {
    private static final long MAX_RANGE_EDIT_CELLS = 100_000;

    private final Workbook workbook;

    public WorkbookEditor(Workbook workbook) {
        this.workbook = Objects.requireNonNullElseGet(workbook, Workbook::blank);
    }

    public static WorkbookEditor edit(Workbook workbook) {
        return new WorkbookEditor(workbook);
    }

    public Workbook workbook() {
        return workbook;
    }

    public WorkbookEditor setCellInput(String sheetName, CellAddress address, String input) {
        Objects.requireNonNull(address, "address");
        return updateSheet(sheetName, sheet -> sheet.withInput(address, input));
    }

    public WorkbookEditor setCellContent(String sheetName, CellAddress address, CellContent content) {
        Objects.requireNonNull(address, "address");
        return updateSheet(sheetName, sheet -> sheet.withCell(address, content));
    }

    public WorkbookEditor clearCell(String sheetName, CellAddress address) {
        Objects.requireNonNull(address, "address");
        return updateSheet(sheetName, sheet -> sheet.clear(address));
    }

    public WorkbookEditor clearRange(String sheetName, CellRange range) {
        Objects.requireNonNull(range, "range");
        assertRangeEditIsReasonable(range);
        return updateSheet(sheetName, sheet -> {
            Sheet updated = sheet;
            for (CellAddress address : range.addresses().toList()) {
                updated = updated.clear(address);
            }
            return updated;
        });
    }

    public WorkbookEditor addSheet(String name) {
        return new WorkbookEditor(workbook.addSheet(Sheet.named(name)));
    }

    private WorkbookEditor updateSheet(String sheetName, Function<Sheet, Sheet> update) {
        Objects.requireNonNull(update, "update");
        Sheet sheet = workbook.findSheet(sheetName)
                .orElseThrow(() -> new IllegalArgumentException("No sheet named: " + sheetName));
        Sheet updated = Objects.requireNonNull(update.apply(sheet), "updated sheet");
        return updated.equals(sheet) ? this : new WorkbookEditor(workbook.withSheet(updated));
    }

    private static void assertRangeEditIsReasonable(CellRange range) {
        if (range.cellCount() > MAX_RANGE_EDIT_CELLS) {
            throw new IllegalArgumentException(
                    "Range edit is too large for one command: " + range.toA1() + " (" + range.cellCount() + " cells)"
            );
        }
    }
}
