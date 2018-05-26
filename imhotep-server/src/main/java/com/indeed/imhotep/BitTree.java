/*
 * Copyright (C) 2018 Indeed Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
 package com.indeed.imhotep;

import org.apache.log4j.Logger;

/**
 * @author jplaisance
 */
public final class BitTree {
    private static final Logger log = Logger.getLogger(BitTree.class);

    private final long[][] bitsets;
    private boolean cleared = true;

    public BitTree(int size) {
        bitsets = new long[log2(size-1)/6+1][];
        for (int i = 0; i < bitsets.length; i++) {
            size = (size+63)/64;
            bitsets[i] = new long[size];
        }
    }

    private static int log2(final int size) {
        return 31-Integer.numberOfLeadingZeros(size);
    }

    public void set(int index) {
        if (!cleared) {
            throw new IllegalStateException();
        }
        for (int i = 0; i < bitsets.length; i++) {
            final int nextIndex = index>>>6;
            bitsets[i][nextIndex] |= 1L<<(index&0x3F);
            index = nextIndex;
        }
    }

    public void set(final int[] indexes, final int n) {
        if (!cleared) {
            throw new IllegalStateException();
        }
        for (int j = 0; j < n; j++) {
            int index = indexes[j];
            for (int i = 0; i < bitsets.length; i++) {
                final int nextIndex = index>>>6;
                bitsets[i][nextIndex] |= 1L<<(index&0x3F);
                index = nextIndex;
            }
        }
    }

    public boolean get(final int index) {
        return (bitsets[0][index>>6]&(1L<<(index&0x3F))) != 0;
    }

    public int dump(final int[] buffer) {
        int count = 0;
        int depth = bitsets.length-1;
        int index = 0;
        cleared = true;
        while (true) {
            while (bitsets[depth][index] == 0) {
                if (depth == bitsets.length-1) {
                    return count;
                }
                depth++;
                index >>>= 6;
            }
            while (depth != 0) {
                final long lsb = bitsets[depth][index] & -bitsets[depth][index];
                bitsets[depth][index] ^= lsb;
                depth--;
                index = (index<<6)+Long.bitCount(lsb-1);
            }
            while (bitsets[0][index] != 0) {
                final long lsb = bitsets[0][index] & -bitsets[0][index];
                bitsets[0][index] ^= lsb;
                buffer[count++] = (index<<6)+Long.bitCount(lsb-1);
            }
            if (bitsets.length == 1) {
                return count;
            }
            depth = 1;
            index >>>= 6;
        }
    }
}
