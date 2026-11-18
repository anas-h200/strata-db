package com.anas.strata;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;

public class SSTableScanner implements AutoCloseable {
    private final DataInputStream in;
    private String currentKey;
    private MemTable.ValueEntry currentEntry;

    SSTableScanner (String path) throws IOException {
        this.in = new DataInputStream(new BufferedInputStream(new FileInputStream(path)));
    }

    boolean next() throws IOException {
        byte flag;

        try {
            flag = in.readByte();
        } catch (EOFException e) {
            return false;
        }

        currentKey = in.readUTF();
        String value = null;

        if (flag == 0) {
            value = in.readUTF();
        }

        currentEntry = new MemTable.ValueEntry(value, flag == 1);
        return true;
    }

    String key() {
        return currentKey;
    }

    MemTable.ValueEntry entry() {
        return currentEntry;
    }

    public void close() throws IOException {
        in.close();
    }
}
