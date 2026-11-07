package com.anas.strata;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

public class SSTableWriterTest {
    @Test
    void writesEntriesInSortedOrderWithTombstones(@TempDir Path tempDir) throws IOException {
        MemTable memTable = new MemTable();
        memTable.put("sam", "11xxx");
        memTable.put("ravi", "98xxx");
        memTable.put("asha", "77xxx");
        memTable.delete("ravi");

        Path sstPath = tempDir.resolve("sstable-1.sst");
        SSTableWriter.write(sstPath.toString(), memTable);

        try (DataInputStream in = new DataInputStream(new FileInputStream(sstPath.toFile()))) {
            assertEquals(0, in.readByte());
            assertEquals("asha", in.readUTF());
            assertEquals("77xxx", in.readUTF());

            assertEquals(1, in.readByte());
            assertEquals("ravi", in.readUTF());

            assertEquals(0, in.readByte());
            assertEquals("sam", in.readUTF());
            assertEquals("11xxx", in.readUTF());

            assertEquals(-1, in.read());
        }
    }
}
