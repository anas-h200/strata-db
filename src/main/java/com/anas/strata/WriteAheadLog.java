package com.anas.strata;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
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

    void reset() throws IOException {
        dos.flush();
        fos.getChannel().truncate(0);
        fos.getFD().sync();

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

    static ReadResult readAllWithLength(String path) throws IOException {
        List<LogRecord> logRecords = new ArrayList<>();
        long validLength = 0;

        try (FileInputStream fis = new FileInputStream(path);
             DataInputStream in = new DataInputStream(fis)) {

            try {
                while (true) {
                    LogRecord record = readNext(in);

                    if (record == null) {
                        break;
                    }

                    logRecords.add(record);
                    validLength = fis.getChannel().position();
                }
            } catch (EOFException e) {
                // The last record was cut off by a crash before it was fully
                // written, so the caller was never told it succeeded.
                // Stop here and keep the complete records already collected.
            }
        }

        return new ReadResult(logRecords, validLength);
    }

    static void truncate(String path, long length) throws IOException {
        try (FileChannel channel = FileChannel.open(Path.of(path), StandardOpenOption.WRITE)) {
            channel.truncate(length);
        }
    }
}