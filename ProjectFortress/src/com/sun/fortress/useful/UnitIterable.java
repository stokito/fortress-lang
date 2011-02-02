/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/

package com.sun.fortress.useful;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class UnitIterable<T> implements IterableOnce<T> {

    T v;

    public UnitIterable(T v) {
        this.v = v;
    }

    public Iterator<T> iterator() {
        if (v == null) throw new IllegalStateException("UnitIterables are good for one use only");
        return this;
    }

    public boolean hasNext() {
        return v != null;
    }

    public T next() throws NoSuchElementException {
        T x = v;
        v = null;
        return x;
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
}
