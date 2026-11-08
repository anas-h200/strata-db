package com.anas.strata;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BloomFilterTest {
    @Test
    void emptyFilterSaysDefinitelyNotForAnyKey() {
        BloomFilter filter = new BloomFilter(100);

        assertFalse(filter.mightContain("ravi"));
    }

    @Test
    void addedKeyIsAlwaysFound() {
        BloomFilter filter = new BloomFilter(100);
        filter.add("ravi");

        assertTrue(filter.mightContain("ravi"));
    }
}
