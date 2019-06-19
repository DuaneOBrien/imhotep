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

package com.indeed.flamdex.simple;

import com.indeed.flamdex.api.IntTermIterator;
import com.indeed.flamdex.api.StringTermIterator;
import com.indeed.imhotep.scheduling.TaskScheduler;

import java.io.IOException;
import java.nio.file.Path;

/**
 * @author vladimir
 */

public class UnsortedStringToIntTermIterator<T extends StringTermIterator> implements IntTermIterator {
    protected final T stringTermIterator;
    private long currentTerm;

    public UnsortedStringToIntTermIterator(final T stringTermIterator) {
        this.stringTermIterator = stringTermIterator;
    }

    public T getStringTermIterator() {
        return stringTermIterator;
    }

    @Override
    public void reset(final long term) {
        stringTermIterator.reset(String.valueOf(term));
    }

    @Override
    public long term() {
        return currentTerm;
    }

    @Override
    public boolean next() {
        // searching for next string term that is convertible to long
        while (stringTermIterator.next()) {
            try {
                currentTerm = Long.parseLong(stringTermIterator.term());
                return true;
            } catch (final NumberFormatException ignored) {
                // This method call could be long if we try to convert field with lot of string terms.
                // So yielding in case of failure.
                TaskScheduler.CPUScheduler.yieldIfNecessary();

                // So if we fail to parse, then move to next possible variant

                // allowed chars in string representation of a number are:
                // '+' - ASCII code 43
                // '-' - ASCII code 45
                // '0' - '9' - ASCII codes 48-57

                if (stringTermIterator.term().isEmpty()) {
                    // it was just empty string. let's see what's next.
                    continue;
                }

                final char firstChar = stringTermIterator.term().charAt(0);
                if (firstChar < '+') {
                    // Skipping all terms until first lexicographically valid string.
                    stringTermIterator.reset("+0");
                    continue;
                }

                if (firstChar > '+' && firstChar < '-') { // one symbol actually ',' - code 44
                    stringTermIterator.reset("-0");
                    continue;
                }

                if (firstChar > '-' && firstChar < '0') {
                    stringTermIterator.reset("0");
                    continue;
                }

                if (firstChar > '9') {
                    // There will be no numbers in this string iterator, finish iteration.
                    return false;
                }

                // If we get here, then term has a prefix that is valid prefix for a number.
                // Theoretically, it's possible to find bad character in a term and do clever reset() to next valid
                // prefix, but it's probably not worth it.
            }
        }
        return false;
    }

    @Override
    public int docFreq() {
        return stringTermIterator.docFreq();
    }

    @Override
    public void close() {
        stringTermIterator.close();
    }

    public static class SimpleUnsortedStringToIntTermIterator
            extends UnsortedStringToIntTermIterator<SimpleStringTermIterator>
            implements SimpleIntTermIterator {

        SimpleUnsortedStringToIntTermIterator(final SimpleStringTermIterator simpleIterator) {
            super(simpleIterator);
        }

        @Deprecated
        public long getDocListAddress() throws IOException {
            return stringTermIterator.getDocListAddress();
        }

        public Path getFilename() {
            return stringTermIterator.getFilename();
        }

        public long getOffset() {
            return stringTermIterator.getOffset();
        }
    }

}
