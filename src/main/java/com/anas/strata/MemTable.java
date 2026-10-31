package com.anas.strata;
import java.util.concurrent.ConcurrentSkipListMap;

public class MemTable {
    private record ValueEntry(String value, boolean deleted) {}

    private final ConcurrentSkipListMap<String, ValueEntry> map = new ConcurrentSkipListMap<>();

    public void put(String key, String value) {
        map.put(key, new ValueEntry(value, false));
    }

    public String get(String key) {
        ValueEntry entry = map.get(key);
        if (entry == null || entry.deleted()) {
            throw new ResourceNotFoundException("This key is not present in the db.");
        }

        return entry.value();
    }

    public void delete(String key) {
        map.put(key, new ValueEntry(null, true));
    }

    public int size() {
        return map.size();
    }
}