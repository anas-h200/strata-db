package com.anas.strata;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class StrataDbTest {
    @Test
    void dataSurvivesRestart(@TempDir Path tempDir) throws IOException {
        Path logPath = tempDir.resolve("wal.log");

        StrataDb firstStrataDb = new StrataDb(logPath.toString());
        firstStrataDb.put("ravi", "98xxx");
        firstStrataDb.close();

        StrataDb secondStrataDb = new StrataDb(logPath.toString());

        assertEquals("98xxx", secondStrataDb.get("ravi"));
        secondStrataDb.close();
    }

    @Test
    void deleteSurvivesRestart(@TempDir Path tempDir) throws IOException {
        Path logPath = tempDir.resolve("wal.log");

        StrataDb firstStrataDb = new StrataDb(logPath.toString());
        firstStrataDb.put("ravi", "98xxx");
        firstStrataDb.delete("ravi");
        firstStrataDb.close();

        StrataDb secondStrataDb = new StrataDb(logPath.toString());

        assertThrows(ResourceNotFoundException.class, () -> secondStrataDb.get("ravi"));
        secondStrataDb.close();
    }

    @Test
    void newWritesAfterTornTailSurviveRestart(@TempDir Path tempDir) throws IOException {
        Path logPath = tempDir.resolve("wal.log");

        StrataDb firstStrataDb = new StrataDb(logPath.toString());
        firstStrataDb.put("ravi", "98xxx");
        firstStrataDb.put("sam", "11xxx");
        firstStrataDb.close();

        byte []bytes = Files.readAllBytes(logPath);
        byte []cutBytes = Arrays.copyOf(bytes, bytes.length-1);

        Files.write(logPath, cutBytes);

        StrataDb secondStrataDb = new StrataDb(logPath.toString());
        secondStrataDb.put("asha", "77xxx");
        secondStrataDb.close();

        StrataDb thirdStrataDb = new StrataDb(logPath.toString());

        assertEquals("98xxx", thirdStrataDb.get("ravi"));
        assertEquals("77xxx", thirdStrataDb.get("asha"));
        assertThrows(ResourceNotFoundException.class, () -> thirdStrataDb.get("sam"));

        thirdStrataDb.close();
    }

    @Test
    void newWritesAfterTornTailMidRecordSurviveRestart(@TempDir Path tempDir) throws IOException {
        Path logPath = tempDir.resolve("wal.log");

        StrataDb firstStrataDb = new StrataDb(logPath.toString());
        firstStrataDb.put("ravi", "98xxx");
        firstStrataDb.put("sam", "11xxx");
        firstStrataDb.close();

        byte []bytes = Files.readAllBytes(logPath);
        byte []cutBytes = Arrays.copyOf(bytes, bytes.length-7);

        Files.write(logPath, cutBytes);

        StrataDb secondStrataDb = new StrataDb(logPath.toString());
        secondStrataDb.put("asha", "77xxx");
        secondStrataDb.close();

        StrataDb thirdStrataDb = new StrataDb(logPath.toString());

        assertEquals("98xxx", thirdStrataDb.get("ravi"));
        assertEquals("77xxx", thirdStrataDb.get("asha"));
        assertThrows(ResourceNotFoundException.class, () -> thirdStrataDb.get("sam"));

        thirdStrataDb.close();
    }

    @Test
    void flushWritesMemTableToSSTable(@TempDir Path tempDir) throws IOException {
        StrataDb db = new StrataDb(tempDir.resolve("wal.log").toString());
        db.put("ravi", "98xxx");
        db.put("sam", "11xxx");
        db.delete("sam");

        String sstPath = tempDir.resolve("sstable-1.sst").toString();
        db.flush(sstPath);
        db.close();

        assertEquals(new MemTable.ValueEntry("98xxx", false), SSTableReader.get(sstPath, "ravi"));
        assertEquals(new MemTable.ValueEntry(null, true), SSTableReader.get(sstPath, "sam"));
    }

    @Test
    void getReadsFromSSTableAfterFlushAndRespectsTombstones(@TempDir Path tempDir) throws IOException {
        StrataDb db = new StrataDb(tempDir.resolve("wal.log").toString());
        db.put("ravi", "98xxx");
        db.put("sam", "11xxx");
        db.flush(tempDir.resolve("sstable-1.sst").toString());

        assertEquals("98xxx", db.get("ravi"));

        db.delete("ravi");
        assertThrows(ResourceNotFoundException.class, () -> db.get("ravi"));

        assertEquals("11xxx", db.get("sam"));
        assertThrows(ResourceNotFoundException.class, () -> db.get("bob"));

        db.close();
    }

    @Test
    void getSearchesNewestSSTableFirst(@TempDir Path tempDir) throws IOException {
        StrataDb db = new StrataDb(tempDir.resolve("wal.log").toString());

        db.put("ravi", "98xxx");
        db.put("sam", "11xxx");
        db.put("bob", "55xxx");
        db.flush(tempDir.resolve("sstable-1.sst").toString());

        db.put("ravi", "99yyy");
        db.delete("sam");
        db.put("asha", "77xxx");
        db.flush(tempDir.resolve("sstable-2.sst").toString());

        assertEquals("99yyy", db.get("ravi"));
        assertThrows(ResourceNotFoundException.class, () -> db.get("sam"));
        assertEquals("55xxx", db.get("bob"));
        assertEquals("77xxx", db.get("asha"));

        db.close();
    }

    @Test
    void sstablesAreFoundAgainAfterRestart(@TempDir Path tempDir) throws IOException {
        Path logPath = tempDir.resolve("wal.log");

        StrataDb firstStrataDb = new StrataDb(logPath.toString());
        firstStrataDb.put("ravi", "98xxx");
        firstStrataDb.flush(tempDir.resolve("sstable-1.sst").toString());
        firstStrataDb.put("ravi", "99yyy");
        firstStrataDb.flush(tempDir.resolve("sstable-2.sst").toString());
        firstStrataDb.close();

        Files.delete(logPath);

        StrataDb secondStrataDb = new StrataDb(logPath.toString());
        assertEquals("99yyy", secondStrataDb.get("ravi"));

        secondStrataDb.close();
    }

    @Test
    void flushClearsWalAndDataSurvivesRestart(@TempDir Path tempDir) throws IOException {
        Path walPath = tempDir.resolve("wal.log");

        StrataDb first = new StrataDb(walPath.toString());
        first.put("ravi", "98xxx");
        first.flush(tempDir.resolve("sstable-1.sst").toString());

        assertEquals(0, Files.size(walPath));

        first.put("sam", "11xxx");
        first.close();

        StrataDb second = new StrataDb(walPath.toString());
        assertEquals("98xxx", second.get("ravi"));
        assertEquals("11xxx", second.get("sam"));
        second.close();
    }

    @Test
    void flushPicksNextSSTableNumberEvenAfterRestart(@TempDir Path tempDir) throws IOException {
        Path walPath = tempDir.resolve("wal.log");

        StrataDb first = new StrataDb(walPath.toString());
        first.put("ravi", "98xxx");
        first.flush();
        first.put("sam", "11xxx");
        first.flush();
        first.close();

        assertTrue(Files.exists(tempDir.resolve("sstable-1.sst")));
        assertTrue(Files.exists(tempDir.resolve("sstable-2.sst")));

        StrataDb second = new StrataDb(walPath.toString());
        second.put("asha", "77xxx");
        second.flush();
        second.close();

        assertTrue(Files.exists(tempDir.resolve("sstable-3.sst")));
        assertEquals(3, Files.list(tempDir).filter(p -> p.toString().endsWith(".sst")).count());
    }

    @Test
    void putFlushesAutomaticallyAtThreshold(@TempDir Path tempDir) throws IOException {
        Path walPath = tempDir.resolve("wal.log");
        StrataDb db = new StrataDb(walPath.toString(), 3);

        db.put("ravi", "98xxx");
        db.put("sam", "11xxx");
        assertFalse(Files.exists(tempDir.resolve("sstable-1.sst")));

        db.put("asha", "77xxx");
        assertTrue(Files.exists(tempDir.resolve("sstable-1.sst")));
        assertEquals(0, Files.size(walPath));

        assertEquals("98xxx", db.get("ravi"));
        assertEquals("11xxx", db.get("sam"));
        assertEquals("77xxx", db.get("asha"));

        db.close();
    }

    @Test
    void deleteFlushesAutomaticallyAtThreshold(@TempDir Path tempDir) throws IOException {
        StrataDb db = new StrataDb(tempDir.resolve("wal.log").toString(), 3);

        db.put("ravi", "98xxx");
        db.put("sam", "11xxx");
        db.delete("asha");

        assertTrue(Files.exists(tempDir.resolve("sstable-1.sst")));
        assertThrows(ResourceNotFoundException.class, () -> db.get("asha"));
        assertEquals("98xxx", db.get("ravi"));

        db.close();
    }

    @Test
    void flushBuildsFilterContainingAllKeysIncludingTombstones(@TempDir Path tempDir) throws IOException {
        StrataDb db = new StrataDb(tempDir.resolve("wal.log").toString());
        db.put("sam", "11xxx");
        db.put("ravi", "98xxx");
        db.put("asha", "77xxx");
        db.delete("ravi");

        String sstPath = tempDir.resolve("sstable-1.sst").toString();
        db.flush(sstPath);

        BloomFilter filter = db.filterFor(sstPath);
        assertTrue(filter.mightContain("sam"));
        assertTrue(filter.mightContain("asha"));
        assertTrue(filter.mightContain("ravi"));
        db.close();
    }

    @Test
    void restartRebuildsFiltersForExistingSSTables(@TempDir Path tempDir) throws IOException {
        StrataDb db1 = new StrataDb(tempDir.resolve("wal.log").toString());
        db1.put("sam", "11xxx");
        db1.put("ravi", "98xxx");
        db1.put("asha", "77xxx");
        db1.delete("ravi");

        String sstPath = tempDir.resolve("sstable-1.sst").toString();
        db1.flush(sstPath);
        db1.close();

        StrataDb db2 = new StrataDb(tempDir.resolve("wal.log").toString());
        BloomFilter filter = db2.filterFor(sstPath);

        assertNotNull(filter);
        assertTrue(filter.mightContain("sam"));
        assertTrue(filter.mightContain("ravi"));
        assertTrue(filter.mightContain("asha"));
        db2.close();
    }
}
