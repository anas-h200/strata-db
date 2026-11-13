package com.anas.strata;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

public class SSTableWriter {
    static final int INDEX_INTERVAL = 16;

    static SSTableIndex write(String path, MemTable memTable) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(path);
             DataOutputStream dos = new DataOutputStream(fos)) {

            SSTableIndex index = new SSTableIndex();
            int count = 0;

            for (Map.Entry<String, MemTable.ValueEntry> entry: memTable.entries()) {
                if (count % INDEX_INTERVAL == 0) {
                    index.add(entry.getKey(), dos.size());
                }

                dos.writeByte(entry.getValue().deleted() ? 1 : 0);
                dos.writeUTF(entry.getKey());

                if (!entry.getValue().deleted()) {
                    dos.writeUTF(entry.getValue().value());
                }

                count++;
            }

            dos.flush();
            fos.getFD().sync();

            return index;
        }
    }
}
