package com.anas.strata;

import java.io.IOException;
import java.util.List;

public class MergingScanner implements AutoCloseable {
    private final List<SSTableScanner> inputs;
    private final boolean[] alive;
    private String currentKey;
    private MemTable.ValueEntry currentEntry;

    MergingScanner(List<SSTableScanner> inputs) throws IOException {

    }

    boolean next() throws IOException {

    }

    String key() {

    }

    MemTable.ValueEntry entry() {

    }

    public void close() throws IOException {

    }
}
