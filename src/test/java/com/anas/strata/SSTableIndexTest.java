package com.anas.strata;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SSTableIndexTest {
    @Test
    void emptyIndexReturnsZero() {
        SSTableIndex index = new SSTableIndex();

        assertEquals(0, index.floorOffset("ravi"));
    }

    @Test
    void exactMatchReturnsItsOwnOffset() {
        SSTableIndex index = new SSTableIndex();

        index.add("a", 0);
        index.add("b", 8);
        index.add("c", 15);

        assertEquals(0, index.floorOffset("a"));
        assertEquals(8, index.floorOffset("b"));
        assertEquals(15, index.floorOffset("c"));
    }
}
