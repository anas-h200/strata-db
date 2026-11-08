package com.anas.strata;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BloomFilterTest {
    @Test
    void emptyFilterSaysDefinitelyNotForAnyKey() {
        BloomFilter filter = new BloomFilter(100, 3);

        assertFalse(filter.mightContain("ravi"));
    }

    @Test
    void addedKeyIsAlwaysFound() {
        BloomFilter filter = new BloomFilter(100, 3);
        filter.add("ravi");

        assertTrue(filter.mightContain("ravi"));
    }

    @Test
    void manyAddedKeysAreAllFound() {
        BloomFilter filter = new BloomFilter(1000, 3);

        for (int i=0; i<100; i++) {
            filter.add("key"+i);
        }

        for (int i=0; i<100; i++) {
            assertTrue(filter.mightContain("key"+i));
        }
    }

    @Test
    void mostMissingKeysAreRejected() {
        BloomFilter filter = new BloomFilter(1000, 3);

        for (int i=0; i<100; i++) {
            filter.add("key"+i);
        }

        int falsePositives = 0;

        for (int i=0; i<1000; i++) {
            if (filter.mightContain("missing"+i)) {
                falsePositives++;
            }
        }

        assertTrue(falsePositives < 50);
    }
}
