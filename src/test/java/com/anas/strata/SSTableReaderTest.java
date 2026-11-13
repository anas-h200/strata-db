package com.anas.strata;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
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

    @Test
    void readAllKeysReturnsEveryKeyIncludingTombstones(@TempDir Path tempDir) throws IOException {
        MemTable memTable = new MemTable();
        memTable.put("sam", "11xxx");
        memTable.put("asha", "77xxx");
        memTable.put("ravi", "98xxx");
        memTable.delete("ravi");

        String path = tempDir.resolve("sstable-1.sst").toString();
        SSTableWriter.write(path, memTable);

        List<String> keys = SSTableReader.readAllKeys(path);
        assertEquals(List.of("asha", "ravi", "sam"), keys);
    }

    @Test
    void getFromOffsetStartsReadingThere(@TempDir Path tempDir) throws IOException {
        MemTable memTable = new MemTable();

        for (int i=0; i<33; i++) {
            memTable.put(String.format("k%02d", i), "v");
        }

        String path = tempDir.resolve("sstable-1.sst").toString();
        SSTableIndex index = SSTableWriter.write(path, memTable);

        assertNotNull(SSTableReader.get(path, "k20", index.floorOffset("k20")));
        assertEquals("v", SSTableReader.get(path, "k20", 144).value());
        assertNull(SSTableReader.get(path, "k05", 144));
    }

    @Test
    void buildIndexMatchesIndexFromWriter(@TempDir Path tempDir) throws IOException {
        MemTable memTable = new MemTable();

        for (int i=0; i<33; i++) {
            memTable.put(String.format("k%02d", i), "v");
        }

        String path = tempDir.resolve("sstable-1.sst").toString();
        SSTableIndex fromWriter =SSTableWriter.write(path, memTable);
        SSTableIndex rebuilt = SSTableReader.buildIndex(path);

        for (int i=0; i<33; i++) {
            String key = String.format("k%02d", i);
            assertEquals(rebuilt.floorOffset(key), fromWriter.floorOffset(key));
        }
    }
}
