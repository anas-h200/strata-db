package com.anas.strata;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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

    @Test
    void writerIndexesEveryNthRecord(@TempDir Path tempDir) throws IOException {
        MemTable memTable = new MemTable();

        for (int i = 0; i < 33; i++) {
            memTable.put(String.format("k%02d", i), "v");
        }

        Path sstPath = tempDir.resolve("sstable-1.sst");
        SSTableIndex index = SSTableWriter.write(sstPath.toString(), memTable);


        assertEquals(0, index.floorOffset("k00"));
        assertEquals(144, index.floorOffset("k16"));
        assertEquals(144, index.floorOffset("k20"));
        assertEquals(288, index.floorOffset("k32"));
    }

    private List<SSTableScanner> twoInputs(Path tempDir) throws IOException {
        MemTable newer = new MemTable();
        newer.put("a", "9");
        newer.delete("d");

        MemTable older = new MemTable();
        older.put("a", "1");
        older.put("c", "3");
        older.put("e", "5");

        String newerPath = tempDir.resolve("newer.sst").toString();
        String olderPath = tempDir.resolve("older.sst").toString();
        SSTableWriter.write(newerPath, newer);
        SSTableWriter.write(olderPath, older);

        return List.of(new SSTableScanner(newerPath), new SSTableScanner(olderPath));
    }

    private List<String> readAll(String path) throws IOException {
        List<String> actual = new ArrayList<>();
        try (SSTableScanner scanner = new SSTableScanner(path)) {
            while (scanner.next()) {
                if (scanner.entry().deleted()) {
                    actual.add(scanner.key() + "=T");
                } else {
                    actual.add(scanner.key() + "=" + scanner.entry().value());
                }
            }
        }
        return actual;
    }

    @Test
    void compactionWriterDropsTombstonesAndKeepsNewestValues(@TempDir Path tempDir) throws IOException {
        String outPath = tempDir.resolve("out.sst").toString();
        SSTableWriter.WriteResult result;

        try (MergingScanner merged = new MergingScanner(twoInputs(tempDir))) {
            result = SSTableWriter.write(outPath, merged, true);
        }

        assertEquals(List.of("a=9", "c=3", "e=5"), readAll(outPath));
        assertEquals(List.of("a", "c", "e"), result.keys());
        assertEquals(0, result.index().floorOffset("a"));
    }

    @Test
    void dropTombstonesFalseKeepsThem(@TempDir Path tempDir) throws IOException {
        String outPath = tempDir.resolve("out.sst").toString();
        SSTableWriter.WriteResult result;

        try (MergingScanner merged = new MergingScanner(twoInputs(tempDir))) {
            result = SSTableWriter.write(outPath, merged, false);
        }

        assertEquals(List.of("a=9", "c=3", "d=T", "e=5"), readAll(outPath));
        assertEquals(List.of("a", "c", "d", "e"), result.keys());
    }
}
