package com.anas.strata;

import java.util.BitSet;

public class BloomFilter {
    private final BitSet bits;
    private final int numBits;
    private final int numHashes;

    public BloomFilter(int numBits, int numHashes) {
        this.numBits = numBits;
        this.bits = new BitSet(numBits);
        this.numHashes = numHashes;
    }

    public void add(String key) {
        for (int i=0; i<numHashes; i++) {
            bits.set(position(key, i));
        }
    }

    public boolean mightContain(String key) {
        for (int i=0; i<numHashes; i++) {
            if (!bits.get(position(key, i))) {
                return false;
            }
        }
        return true;
    }

    private int position(String key, int i) {
        int h1 = key.hashCode();
        int h2 = (key + "#").hashCode();

        return Math.floorMod(h1 + i*h2, numBits);
    }
}
