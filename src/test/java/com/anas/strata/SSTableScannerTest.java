package com.anas.strata;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SSTableScannerTest {
    @Test
    void scansRecordsInSortedOrderAndReportsTombstones(@TempDir Path tempDir) throws IOException {
        MemTable memTable = new MemTable();
        memTable.put("sam", "11xxx");
        memTable.put("ravi", "98xxx");
        memTable.put("asha", "77xxx");
        memTable.delete("ravi");

        String path = tempDir.resolve("test.sst").toString();
        SSTableWriter.write(path, memTable);

        List<String> keys = new ArrayList<>();

        try (SSTableScanner scanner = new SSTableScanner(path)) {
            while (scanner.next()) {
                keys.add(scanner.key());

                if (scanner.key().equals("ravi")) {
                    assertTrue(scanner.entry().deleted());
                    assertNull(scanner.entry().value());
                }
                else {
                    assertFalse(scanner.entry().deleted());
                    if (scanner.key().equals("sam")) {
                        assertEquals("11xxx", scanner.entry().value());
                    }
                    else if (scanner.key().equals("asha")) {
                        assertEquals("77xxx", scanner.entry().value());
                    }
                }
            }

            assertFalse(scanner.next());
        }

        assertEquals(List.of("asha", "ravi", "sam"), keys);
    }
}
