package com.anas.strata;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

public class WriteAheadLogTest {
    @Test
    void appendPutCreatesNonEmptyFile(@TempDir Path tempDir) throws IOException {
        Path logPath = tempDir.resolve("wal.log");

        WriteAheadLog wal = new WriteAheadLog(logPath.toString());
        wal.appendPut("ravi", "98xxx");
        wal.close();

        assertTrue(Files.exists(logPath));
        assertTrue(Files.size(logPath) > 0);
    }
}
