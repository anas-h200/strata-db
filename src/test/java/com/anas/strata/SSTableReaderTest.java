package com.anas.strata;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

public class SSTableReaderTest {
    @Test
    void getFindsValuesTombstonesAndMissingKeys(@TempDir Path tempDir) throws IOException {
        MemTable memTable = new MemTable();
        memTable.put("sam", "11xxx");
        memTable.put("ravi", "98xxx");
        memTable.put("asha", "77xxx");
        memTable.delete("ravi");

        String path = tempDir.resolve("sstable-1.sst").toString();
        SSTableWriter.write(path, memTable);

        assertEquals(new MemTable.ValueEntry("77xxx", false), SSTableReader.get(path, "asha"));
        assertEquals(new MemTable.ValueEntry(null, true), SSTableReader.get(path, "ravi"));
        assertEquals(new MemTable.ValueEntry("11xxx", false), SSTableReader.get(path, "sam"));

        assertNull(SSTableReader.get(path, "bob"));
        assertNull(SSTableReader.get(path, "zed"));
    }
}
