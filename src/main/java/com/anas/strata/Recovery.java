package com.anas.strata;

import java.util.List;

public class Recovery {
    static void replay(List<LogRecord> records, MemTable memTable) {
        for (LogRecord record: records) {
            if (record.type() == LogRecord.PUT) {
                memTable.put(record.key(), record.value());
            }
            else if (record.type() == LogRecord.DELETE) {
                memTable.delete(record.key());
            }
        }
    }
}
