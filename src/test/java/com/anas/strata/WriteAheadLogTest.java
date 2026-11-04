package com.anas.strata;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

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

    @Test
    void readingPastLastRecordThrowsEndOfFile(@TempDir Path tempDir) throws IOException {
        Path logPath = tempDir.resolve("wal.log");

        WriteAheadLog wal = new WriteAheadLog(logPath.toString());
        wal.appendPut("ravi", "98xxx");
        wal.close();

        try (DataInputStream in = new DataInputStream(new FileInputStream(logPath.toString()))) {
            in.readByte();
            in.readUTF();
            in.readUTF();

            assertThrows(EOFException.class, () -> in.readByte());
        }
    }

    @Test
    void readNextReturnsNullOnEmptyFile(@TempDir Path tempDir) throws IOException {
        Path logPath = tempDir.resolve("wal.log");

        Files.createFile(logPath);

        try (DataInputStream in = new DataInputStream(new FileInputStream(logPath.toString()))) {
            LogRecord log = WriteAheadLog.readNext(in);

            assertNull(log);
        }
    }

    @Test
    void readNextReturnsPutRecord(@TempDir Path tempDir) throws IOException {
        Path logPath = tempDir.resolve("wal.log");

        WriteAheadLog wal = new WriteAheadLog(logPath.toString());
        wal.appendPut("ravi", "98xxx");
        wal.close();

        try (DataInputStream in = new DataInputStream(new FileInputStream(logPath.toString()))) {
            LogRecord record = WriteAheadLog.readNext(in);

            assertEquals(LogRecord.PUT, record.type());
            assertEquals("ravi", record.key());
            assertEquals("98xxx", record.value());
        }
    }

    @Test
    void readNextReturnsDeleteRecord(@TempDir Path tempDir) throws IOException {
        Path logPath = tempDir.resolve("wal.log");

        WriteAheadLog wal = new WriteAheadLog(logPath.toString());
        wal.appendDelete("ravi");
        wal.close();

        try (DataInputStream in = new DataInputStream(new FileInputStream(logPath.toString()))) {
            LogRecord record = WriteAheadLog.readNext(in);

            assertEquals(LogRecord.DELETE, record.type());
            assertEquals("ravi", record.key());
            assertNull(record.value());
        }
    }

    @Test
    void readNextThrowsOnUnknownType(@TempDir Path tempDir) throws IOException {
        Path logPath = tempDir.resolve("wal.log");

        Files.write(logPath, new byte[] { 7 });

        try (DataInputStream in = new DataInputStream(new FileInputStream(logPath.toString()))) {
            assertThrows(IOException.class, () -> WriteAheadLog.readNext(in));
        }
    }

    @Test
    void readNextOnCutoffRecord(@TempDir Path tempDir) throws IOException {
        Path logPath = tempDir.resolve("wal.log");

        WriteAheadLog wal = new WriteAheadLog(logPath.toString());
        wal.appendPut("ravi", "98xxx");
        wal.close();

        byte []bytes = Files.readAllBytes(logPath);
        byte []cutBytes = Arrays.copyOf(bytes, 8);

        Files.write(logPath, cutBytes);

        try (DataInputStream in = new DataInputStream(new FileInputStream(logPath.toString()))) {
            assertThrows(EOFException.class, () -> WriteAheadLog.readNext(in));
        }
    }

    @Test
    void readAllReturnsRecordsInOrder(@TempDir Path tempDir) throws IOException {
        Path logPath = tempDir.resolve("wal.log");

        WriteAheadLog wal = new WriteAheadLog(logPath.toString());
        wal.appendPut("ravi", "98xxx");
        wal.appendDelete("ravi");
        wal.appendPut("sam", "11xxx");
        wal.close();

        List<LogRecord> results = WriteAheadLog.readAll(logPath.toString());

        assertEquals(3, results.size());

        LogRecord first = results.get(0);
        assertEquals(LogRecord.PUT, first.type());
        assertEquals("ravi", first.key());
        assertEquals("98xxx", first.value());

        LogRecord second = results.get(1);
        assertEquals(LogRecord.DELETE, second.type());
        assertEquals("ravi", second.key());
        assertNull(second.value());

        LogRecord third = results.get(2);
        assertEquals(LogRecord.PUT, third.type());
        assertEquals("sam", third.key());
        assertEquals("11xxx", third.value());
    }
}
