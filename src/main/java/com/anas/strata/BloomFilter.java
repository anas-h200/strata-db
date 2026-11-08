package com.anas.strata;

import java.util.BitSet;

public class BloomFilter {
    private final BitSet bits;
    private final int numBits;

    public BloomFilter(int numBits) {
        this.numBits = numBits;
        this.bits = new BitSet(numBits);
    }

    public void add(String key) {
        bits.set(position(key));
    }

    public boolean mightContain(String key) {
        return bits.get(position(key));
    }

    private int position(String key) {
        return Math.floorMod(key.hashCode(), numBits);
    }
}
