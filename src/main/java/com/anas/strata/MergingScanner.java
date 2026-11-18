package com.anas.strata;

import java.io.IOException;
import java.util.List;

public class MergingScanner implements AutoCloseable {
    private final List<SSTableScanner> inputs;
    private final boolean[] alive;
    private String currentKey;
    private MemTable.ValueEntry currentEntry;

    MergingScanner(List<SSTableScanner> inputs) throws IOException {
        this.inputs = inputs;
        this.alive = new boolean[inputs.size()];
        for (int i=0; i<inputs.size(); i++) {
            alive[i] = inputs.get(i).next();
        }
    }

    boolean next() throws IOException {
        int best = -1;

        for (int i=0; i<inputs.size(); i++) {
            if (!alive[i]) {
                continue;
            }
            else {
                if (best == -1 || inputs.get(i).key().compareTo(inputs.get(best).key()) < 0) {
                    best = i;
                }
            }
        }

        if (best == -1) {
            return false;
        }

        currentKey = inputs.get(best).key();
        currentEntry = inputs.get(best).entry();

        for (int i=0; i<inputs.size(); i++) {
            if (alive[i] && inputs.get(i).key().equals(currentKey)) {
                alive[i] = inputs.get(i).next();
            }
        }

        return true;
    }

    String key() {
        return currentKey;
    }

    MemTable.ValueEntry entry() {
        return currentEntry;
    }

    public void close() throws IOException {
        for (SSTableScanner input: inputs) {
            input.close();
        }
    }
}
