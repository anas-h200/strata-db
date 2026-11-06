package com.anas.strata;

import java.util.List;

public record ReadResult(List<LogRecord> records, long validLength) {

}
