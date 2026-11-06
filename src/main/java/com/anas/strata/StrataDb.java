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
            ReadResult results = WriteAheadLog.readAllWithLength(path);
            Recovery.replay(results.records(), this.memTable);
            WriteAheadLog.truncate(path, results.validLength());
        }

        this.wal = new WriteAheadLog(path);
    }

    void put(String key, String value) throws IOException {
        wal.appendPut(key, value);
        memTable.put(key, value);
    }

    void delete(String key) throws IOException {
        wal.appendDelete(key);
        memTable.delete(key);
    }

    String get(String key) {
        return memTable.get(key);
    }

    void close() throws IOException {
        wal.close();
    }

    void flush (String sstPath) throws IOException {
        SSTableWriter.write(sstPath, memTable);
    }
}
