package com.anas.strata;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class StrataDb {
    private final MemTable memTable;
    private final WriteAheadLog wal;

    StrataDb(String path) throws IOException {
        this.memTable = new MemTable();

        if (Files.exists(Path.of(path))) {
            List<LogRecord> records = WriteAheadLog.readAll(path);
            Recovery.replay(records, this.memTable);
        }

        this.wal = new WriteAheadLog(path);
    }

    void put(String key, String value) throws IOException {
        wal.appendPut(key, value);
        memTable.put(key, value);
    }

    String get(String key) {
        return memTable.get(key);
    }

    void close() throws IOException {
        wal.close();
    }
}
