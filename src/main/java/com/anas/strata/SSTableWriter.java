package com.anas.strata;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;

public class SSTableWriter {
    static final int INDEX_INTERVAL = 16;
    record WriteResult(SSTableIndex index, List<String> keys) {}

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

    static WriteResult write(String path, MergingScanner merged, boolean dropTombstones) throws IOException{
        try (FileOutputStream fos = new FileOutputStream(path);
             DataOutputStream dos = new DataOutputStream(fos)) {

            SSTableIndex index = new SSTableIndex();
            List<String> keys = new ArrayList<>();
            int count = 0;

            while (merged.next()) {
                if (dropTombstones && merged.entry().deleted()) {
                    continue;
                }
                if (count % INDEX_INTERVAL == 0) {
                    index.add(merged.key(), dos.size());
                }

                writeRecord(dos, merged.key(), merged.entry());

                keys.add(merged.key());

                count++;
            }

            dos.flush();
            fos.getFD().sync();

            return new WriteResult(index, keys);
        }
    }
}
