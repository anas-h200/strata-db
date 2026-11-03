package com.anas.strata;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class WriteAheadLog {
    private final FileOutputStream fos;
    private final DataOutputStream dos;

    WriteAheadLog(String path) throws IOException {
        fos = new FileOutputStream(path, true);
        dos = new DataOutputStream(fos);
    }

    void appendPut(String key, String value) throws IOException {
        dos.writeByte(LogRecord.PUT);
        dos.writeUTF(key);
        dos.writeUTF(value);

        dos.flush();
        fos.getFD().sync();
    }

    void appendDelete(String key) throws IOException {
        dos.writeByte(LogRecord.DELETE);
        dos.writeUTF(key);

        dos.flush();
        fos.getFD().sync();
    }

    void close() throws IOException {
        dos.close();
    }
}