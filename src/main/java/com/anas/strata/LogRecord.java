package com.anas.strata;

public record LogRecord(byte type, String key, String value) {
    public static final byte PUT = 1;
    public static final byte DELETE = 2;
}
