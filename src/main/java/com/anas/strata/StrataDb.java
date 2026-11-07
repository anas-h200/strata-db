package com.anas.strata;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class StrataDb {
    private final MemTable memTable;
    private final WriteAheadLog wal;
    private final List<String> sstPaths = new ArrayList<>();

    StrataDb(String path) throws IOException {
        this.memTable = new MemTable();
        loadSSTables(Path.of(path));

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

    String get(String key) throws IOException {
        MemTable.ValueEntry entry = memTable.lookup(key);

        if (entry == null) {
            for (String path: sstPaths) {
                entry = SSTableReader.get(path, key);
                if (entry != null) {
                    break;
                }
            }
        }

        if (entry == null || entry.deleted()) {
            throw new ResourceNotFoundException("This key is not present in the db.");
        }

        return entry.value();
    }

    void close() throws IOException {
        wal.close();
    }

    void flush (String sstPath) throws IOException {
        SSTableWriter.write(sstPath, memTable);
        sstPaths.add(0, sstPath);
        memTable.clear();
    }

    private static int ssTableNumber(Path p) {
        String name = p.getFileName().toString();
        String number = name.substring("sstable-".length(), name.length() - ".sst".length());
        return Integer.parseInt(number);
    }

    private void loadSSTables(Path walPath) throws IOException {
        Path dir = walPath.toAbsolutePath().getParent();

        List<Path> found = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "sstable-*.sst")) {
            for (Path p: stream) {
                found.add(p);
            }
        }

        found.sort((a, b) -> Integer.compare(ssTableNumber(b), ssTableNumber(a)));

        for (Path p: found) {
            sstPaths.add(p.toString());
        }
    }
}
