package com.anas.strata;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;

public class SSTableReader {
    static MemTable.ValueEntry get(String path, String key) throws IOException {
        try (DataInputStream in = new DataInputStream(new FileInputStream(path))) {
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
}
