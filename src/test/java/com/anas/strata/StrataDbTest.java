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
}
