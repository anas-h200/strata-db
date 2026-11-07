package com.anas.strata;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

public class SSTableWriter {
    static void write(String path, MemTable memTable) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(path);
             DataOutputStream dos = new DataOutputStream(fos)) {

            for (Map.Entry<String, MemTable.ValueEntry> entry: memTable.entries()) {
                dos.writeByte(entry.getValue().deleted() ? 1 : 0);
                dos.writeUTF(entry.getKey());

                if (!entry.getValue().deleted()) {
                    dos.writeUTF(entry.getValue().value());
                }
            }

            dos.flush();
            fos.getFD().sync();
        }
    }
}
