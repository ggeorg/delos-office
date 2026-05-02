package io.github.ggeorg.delos.base.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DatabaseProjectTest {
    @Test
    void blankProjectStartsEmpty() {
        DatabaseProject project = DatabaseProject.blank();

        assertEquals("Untitled", project.title());
        assertEquals(0, project.objectCount());
    }

    @Test
    void addObjectReturnsNewProject() {
        DatabaseProject project = DatabaseProject.blank();
        DatabaseProject updated = project.addObject(DatabaseObject.table("Customers"));

        assertEquals(0, project.objectCount());
        assertEquals(1, updated.objectCount());
        assertTrue(updated.findObject(DatabaseObjectKind.TABLE, "customers").isPresent());
    }

    @Test
    void duplicateObjectsAreRejectedByKindAndName() {
        assertThrows(IllegalArgumentException.class, () -> new DatabaseProject("Bad", List.of(
                DatabaseObject.table("Customers"),
                DatabaseObject.table(" customers ")
        )));
    }
}
