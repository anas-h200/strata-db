package com.anas.strata;

import java.io.*;
import java.util.List;
import java.util.ArrayList;

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

    static LogRecord readNext(DataInputStream in) throws IOException {
        byte type;
        try {
            type = in.readByte();
        } catch(EOFException e) {
            return null;
        }

        if (type == LogRecord.PUT) {
            String key = in.readUTF();
            String value = in.readUTF();

            return new LogRecord(type, key, value);
        }
        else if (type == LogRecord.DELETE) {
            String key = in.readUTF();

            return new LogRecord(type, key, null);
        }

        throw new IOException("LogRecord contains an unknown byte type: type " + type + ".");
    }

    static List<LogRecord> readAll(String path) throws IOException {
        List<LogRecord> logRecords = new ArrayList<>();

        try (DataInputStream in = new DataInputStream(new FileInputStream(path))) {
            try {
                while (true) {
                    LogRecord record = readNext(in);

                    if (record == null) {
                        break;
                    }

                    logRecords.add(record);
                }
            } catch (EOFException e) {
                // The last record was cut off by a crash before it was fully
                // written, so the caller was never told it succeeded.
                // Stop here and keep the complete records already collected.
            }
        }

        return logRecords;
    }
}