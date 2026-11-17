package com.anas.strata;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

public class SSTableWriter {
    static final int INDEX_INTERVAL = 16;

    private static void writeRecord(DataOutputStream dos, String key, MemTable.ValueEntry entry) throws IOException {
        dos.writeByte(entry.deleted() ? 1 : 0);
        dos.writeUTF(key);

        if (!entry.deleted()) {
            dos.writeUTF(entry.value());
        }
    }

    static SSTableIndex write(String path, MemTable memTable) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(path);
             DataOutputStream dos = new DataOutputStream(fos)) {

            SSTableIndex index = new SSTableIndex();
            int count = 0;

            for (Map.Entry<String, MemTable.ValueEntry> entry: memTable.entries()) {
                if (count % INDEX_INTERVAL == 0) {
                    index.add(entry.getKey(), dos.size());
                }

                writeRecord(dos, entry.getKey(), entry.getValue());

                count++;
            }

            dos.flush();
            fos.getFD().sync();

            return index;
        }
    }
}
