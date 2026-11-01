package com.anas.strata;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class WriteAheadLog {
    private static final byte PUT = 1;
    private static final byte DELETE = 2;

    private final FileOutputStream fos;
    private final DataOutputStream dos;

    WriteAheadLog(String path) throws IOException {
        fos = new FileOutputStream(path, true);
        dos = new DataOutputStream(fos);
    }

    void appendPut(String key, String value) throws IOException {
        dos.writeByte(PUT);
        dos.writeUTF(key);
        dos.writeUTF(value);

        dos.flush();
        fos.getFD().sync();
    }

    void close() throws IOException {
        dos.close();
    }
}