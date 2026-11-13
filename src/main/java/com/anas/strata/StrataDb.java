package com.anas.strata;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StrataDb {
    private final MemTable memTable;
    private final WriteAheadLog wal;
    private final List<String> sstPaths = new ArrayList<>();
    private final Map<String, BloomFilter> filters = new HashMap<>();
    private final Map<String, SSTableIndex> indexes = new HashMap<>();
    private final Path dir;
    private int nextSstNumber = 1;
    private final int flushThreshold;

    StrataDb(String path, int flushThreshold) throws IOException {
        this.flushThreshold = flushThreshold;
        this.memTable = new MemTable();
        this.dir = Path.of(path).toAbsolutePath().getParent();
        loadSSTables(Path.of(path));

        if (Files.exists(Path.of(path))) {
            ReadResult results = WriteAheadLog.readAllWithLength(path);
            Recovery.replay(results.records(), this.memTable);
            WriteAheadLog.truncate(path, results.validLength());
        }

        this.wal = new WriteAheadLog(path);
    }

    StrataDb(String path) throws IOException {
        this(path, 1000);
    }

    void put(String key, String value) throws IOException {
        wal.appendPut(key, value);
        memTable.put(key, value);

        if (memTable.size() >= flushThreshold) {
            flush();
        }
    }

    void delete(String key) throws IOException {
        wal.appendDelete(key);
        memTable.delete(key);

        if (memTable.size() >= flushThreshold) {
            flush();
        }
    }

    String get(String key) throws IOException {
        MemTable.ValueEntry entry = memTable.lookup(key);

        if (entry == null) {
            for (String path: sstPaths) {
                BloomFilter filter = filters.get(path);
                if (!filter.mightContain(key)) {
                    continue;
                }

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

    BloomFilter filterFor(String sstPath) {
        return filters.get(sstPath);
    }

    void flush (String sstPath) throws IOException {
        SSTableIndex index = SSTableWriter.write(sstPath, memTable);
        indexes.put(sstPath, index);

        List<String> keys = new ArrayList<>();
        for (Map.Entry<String, MemTable.ValueEntry> entry: memTable.entries()) {
            keys.add(entry.getKey());
        }

        filters.put(sstPath, buildFilter(keys));
        sstPaths.add(0, sstPath);
        memTable.clear();
        wal.reset();
    }

    void flush() throws IOException {
        String sstPath = dir.resolve("sstable-" + nextSstNumber + ".sst").toString();
        nextSstNumber++;
        flush(sstPath);
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
            String sstPath = p.toString();
            sstPaths.add(sstPath);

            List<String> keys = SSTableReader.readAllKeys(sstPath);
            filters.put(sstPath, buildFilter(keys));
        }

        if (!found.isEmpty()) {
            nextSstNumber = ssTableNumber(found.get(0)) + 1;
        }
    }

    private static BloomFilter buildFilter(List<String> keys) {
        BloomFilter filter = new BloomFilter(Math.max(64, keys.size()*10), 3);

        for (String key: keys) {
            filter.add(key);
        }

        return filter;
    }
}
