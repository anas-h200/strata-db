package com.anas.strata;

import java.io.*;

public class SSTableScanner implements AutoCloseable {
    private final CountingInputStream counter;
    private final DataInputStream in;
    private long currentOffset;
    private String currentKey;
    private MemTable.ValueEntry currentEntry;

    SSTableScanner(String path) throws IOException {
        this.counter = new CountingInputStream(new BufferedInputStream(new FileInputStream(path)));
        this.in = new DataInputStream(counter);
    }

    boolean next() throws IOException {
        long start = counter.count();
        byte flag;

        try {
            flag = in.readByte();
        } catch (EOFException e) {
            return false;
        }

        currentOffset = start;

        currentKey = in.readUTF();
        String value = null;

        if (flag == 0) {
            value = in.readUTF();
        }

        currentEntry = new MemTable.ValueEntry(value, flag == 1);
        return true;
    }

    long offset() {
        return currentOffset;
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

    private static class CountingInputStream extends FilterInputStream {
        private long count = 0;

        CountingInputStream(InputStream in) {
            super(in);
        }

        @Override
        public int read() throws IOException {
            int b = super.read();
            if (b != -1) count++;
            return b;
        }

        @Override
        public int read(byte[] buf, int off, int len) throws IOException {
            int n = super.read(buf, off, len);
            if (n > 0) count += n;
            return n;
        }

        long count() {
            return count;
        }
    }
}
