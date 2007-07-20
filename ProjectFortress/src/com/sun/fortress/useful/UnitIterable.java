/*******************************************************************************
    Copyright 2007 Sun Microsystems, Inc.,
    4150 Network Circle, Santa Clara, California 95054, U.S.A.
    All rights reserved.

    U.S. Government Rights - Commercial software.
    Government users are subject to the Sun Microsystems, Inc. standard
    license agreement and applicable provisions of the FAR and its supplements.

    Use is subject to license terms.

    This distribution may include materials developed by third parties.

    Sun, Sun Microsystems, the Sun logo and Java are trademarks or registered
    trademarks of Sun Microsystems, Inc. in the U.S. and other countries.
 ******************************************************************************/

package com.sun.fortress.useful;

import java.util.Iterator;

public class UnitIterable<T> implements IterableOnce<T> {

    T v;

    public UnitIterable(T v) {
        this.v = v;
    }

    public Iterator<T> iterator() {
        if (v == null)
            throw new IllegalStateException("UnitIterables are good for one use only");
        return this;
    }

    public boolean hasNext() {
         return v != null;
    }

    public T next() {
        T x = v;
        v = null;
        return x;
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
}
