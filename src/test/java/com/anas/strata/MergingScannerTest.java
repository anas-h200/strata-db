package com.anas.strata;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MergingScannerTest {
    @Test
    void newestVersionWinsAndKeysComeOutSorted(@TempDir Path tempDir) throws IOException {
        MemTable memTable1 = new MemTable();
        memTable1.put("a", "9");
        memTable1.delete("d");

        MemTable memTable2 = new MemTable();
        memTable2.put("a", "1");
        memTable2.put("c", "3");
        memTable2.put("e", "5");

        String newerPath = tempDir.resolve("newer.sst").toString();
        String olderPath = tempDir.resolve("older.sst").toString();
        SSTableWriter.write(newerPath, memTable1);
        SSTableWriter.write(olderPath, memTable2);

        List<SSTableScanner> inputs = List.of(
                new SSTableScanner(newerPath),
                new SSTableScanner(olderPath));

        List<String> actual = new ArrayList<>();

        try (MergingScanner merged = new MergingScanner(inputs)) {
            while (merged.next()) {
                if (merged.entry().deleted()) {
                    actual.add(merged.key() + "=T");
                } else {
                    actual.add(merged.key() + "=" + merged.entry().value());
                }
            }
        }

        assertEquals(List.of("a=9", "c=3", "d=T", "e=5"), actual);
    }
}