/*******************************************************************************
 Copyright 2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.useful;

import java.util.Iterator;
import java.util.NoSuchElementException;

import edu.rice.cs.plt.tuple.*;

/**
 * A class that packages up a number of useful operations on Iterators and Iterables,
 * most notably iter (makes an Iterator<T> into an Iterable<T>) and zip.
 */
public class Iter {
    private Iter() {
        super();
    }

    static final class IteratorForIterable<T> implements Iterable<T> {
        private final Iterator<T> iter;

        protected IteratorForIterable(Iterator<T> iter) {
            super();
            this.iter = iter;
        }

        public Iterator<T> iterator() { return iter; }
    }

    static final class Zip2<T1,T2>
            implements Iterable<Pair<T1,T2>>, Iterator<Pair<T1,T2>> {
        private final Iterator<T1> iter1;
        private final Iterator<T2> iter2;

        protected Zip2(Iterator<T1> iter1, Iterator<T2> iter2) {
            super();
            this.iter1 = iter1;
            this.iter2 = iter2;
        }

        public Iterator<Pair<T1,T2>> iterator() { return this; }

        public boolean hasNext() { return iter1.hasNext() && iter2.hasNext(); }

        public Pair<T1,T2> next() throws NoSuchElementException {
            // Avoid advancing iterators if either is out of elements.
            if (!hasNext()) throw new NoSuchElementException("Zip2("+iter1+","+iter2+")");
            return new Pair<T1,T2>(iter1.next(), iter2.next());
        }

        public void remove() {
            iter1.remove();
            iter2.remove();
        }
    }

    /**
     * iter converts an Iterator<T> into an Iterable<T>.
     * It's really just a wrapper to make for loops look nice.
     * usage: for (T t : Iter.iter(myiterator)) { ... }
     */
    public static <T> Iterable<T> iter(Iterator<T> i) {
        return new IteratorForIterable<T>(i);
    }

    /**
     * zip consumes two iterables in lockstep, yielding a pair of corresponding
     * elements.
     * Usage:
     * for (Pair<T1,T2> p : Iter.zip(a, b)) {
     *     T1 t1 = p.first();
     *     T2 t2 = p.second();
     *     ...
     * }
     */
    public static <T1,T2> Zip2<T1,T2> zip(Iterable<T1> i1, Iterable<T2> i2) {
        return new Zip2<T1,T2>(i1.iterator(),i2.iterator());
    }
}
