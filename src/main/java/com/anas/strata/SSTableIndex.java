package com.anas.strata;

import java.util.TreeMap;
import java.util.Map;

public class SSTableIndex {
    private final TreeMap<String, Long> entries = new TreeMap<>();

    public void add(String key, long offset) {
        entries.put(key, offset);
    }

    public long floorOffset(String key) {
        Map.Entry<String, Long> result = entries.floorEntry(key);

        if (result == null) {
            return 0;
        }

        return result.getValue();
    }
}
