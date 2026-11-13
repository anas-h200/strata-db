package com.anas.strata;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SSTableReader {
    static MemTable.ValueEntry get(String path, String key) throws IOException {
        return get(path, key, 0);
    }

    static MemTable.ValueEntry get(String path, String key, long startOffset) throws IOException {
        try (FileInputStream fis = new FileInputStream(path);
             DataInputStream in = new DataInputStream(fis)) {
            fis.getChannel().position(startOffset);
            while (true) {
                byte flag;

                try {
                    flag = in.readByte();
                } catch (EOFException e) {
                    return null;
                }

                String currentKey = in.readUTF();
                String value = null;

                if (flag == 0) {
                    value = in.readUTF();
                }

                if (currentKey.equals(key)) {
                    return new MemTable.ValueEntry(value, flag == 1);
                }
                else if (currentKey.compareTo(key) > 0) {
                    return null;
                }
            }
        }
    }

    static List<String> readAllKeys(String path) throws IOException {
        List<String> keys = new ArrayList<>();

        try (DataInputStream in = new DataInputStream(new FileInputStream(path))) {
            while (true) {
                byte flag;
                try {
                    flag = in.readByte();
                } catch (EOFException e) {
                    return keys;
                }

                String currentKey = in.readUTF();
                if (flag == 0) {
                    in.readUTF();
                }

                keys.add(currentKey);
            }
        }
    }

    static SSTableIndex buildIndex(String path) throws IOException {
        SSTableIndex index = new SSTableIndex();
        int count = 0;

        try (FileInputStream fis = new FileInputStream(path);
        DataInputStream in = new DataInputStream(fis)) {
            while (true) {
                long currentOffset = fis.getChannel().position();

                byte flag;
                try {
                    flag = in.readByte();
                } catch (EOFException e) {
                    return index;
                }

                String key = in.readUTF();
                if (flag == 0) {
                    in.readUTF();
                }

                if (count % SSTableWriter.INDEX_INTERVAL == 0) {
                    index.add(key, currentOffset);
                }

                count++;
            }
        }
    }
}
