package com.anas.strata;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.DataInputStream;
import java.io.FileInputStream;
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

    @Test
    void appendDeleteWrites(@TempDir Path tempDir) throws IOException {
        Path logPath = tempDir.resolve("wal.log");

        WriteAheadLog wal = new WriteAheadLog(logPath.toString());
        wal.appendDelete("ravi");
        wal.close();

        assertEquals(7L, Files.size(logPath));
    }

    @Test
    void appendPutWritesTypeKeyAndValueInOrder(@TempDir Path tempDir) throws IOException {
        Path logPath = tempDir.resolve("wal.log");

        WriteAheadLog wal = new WriteAheadLog(logPath.toString());
        wal.appendPut("ravi", "98xxx");
        wal.close();

        try (DataInputStream in = new DataInputStream(new FileInputStream(logPath.toString()))) {
            byte type = in.readByte();
            String key = in.readUTF();
            String value = in.readUTF();

            assertEquals(1, type);
            assertEquals("ravi", key);
            assertEquals("98xxx", value);
        }
    }
}
