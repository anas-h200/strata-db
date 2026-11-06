package com.anas.strata;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class MemTableTest {
    @Test
    void getReturnsValueAfterPut() {
        MemTable table = new MemTable();
        table.put("Raj", "97xxx");

        assertEquals("97xxx", table.get("Raj"));
    }

    @Test
    void getThrowsAfterDelete() {
        MemTable table = new MemTable();
        table.put("Raj", "97xxx");
        table.delete("Raj");

        assertThrows(ResourceNotFoundException.class, () -> table.get("Raj"));
    }

    @Test
    void getThrowsForMissingKey() {
        MemTable table = new MemTable();

        assertThrows(ResourceNotFoundException.class, () -> table.get("Nobody"));
    }

    @Test
    void entriesAreSortedAndIncludeTombstones() {
        MemTable table = new MemTable();
        table.put("sam", "11xxx");
        table.put("ravi", "98xxx");
        table.put("asha", "77xxx");
        table.delete("ravi");

        List<String> keys = new ArrayList<>();
        boolean raviDeleted = false;

        for (Map.Entry<String, MemTable.ValueEntry> entry: table.entries()) {
            keys.add(entry.getKey());

            if (entry.getKey().equals("ravi")) {
                raviDeleted = entry.getValue().deleted();
            }
        }

        assertEquals(List.of("asha", "ravi", "sam"), keys);
        assertTrue(raviDeleted);
    }
}
