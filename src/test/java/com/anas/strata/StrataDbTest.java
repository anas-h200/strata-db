package com.anas.strata;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

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
}
