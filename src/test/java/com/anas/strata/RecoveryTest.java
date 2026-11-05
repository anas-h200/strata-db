package com.anas.strata;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;

public class RecoveryTest {
    @Test
    void replayRebuildsStateFromRecords() {
        MemTable memTable = new MemTable();

        List<LogRecord> records = List.of(
                new LogRecord(LogRecord.PUT, "ravi", "98xxx"),
                new LogRecord(LogRecord.PUT, "ravi", "11xxx"),
                new LogRecord(LogRecord.PUT, "sami", "55xxx"),
                new LogRecord(LogRecord.DELETE, "sami", null)
        );

        Recovery.replay(records, memTable);

        assertEquals("11xxx", memTable.get("ravi"));
        assertThrows(ResourceNotFoundException.class, ()->memTable.get("sami"));
    }
}
