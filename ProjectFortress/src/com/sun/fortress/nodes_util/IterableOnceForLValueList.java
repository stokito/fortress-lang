/*******************************************************************************
    Copyright 2008, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.nodes_util;

import java.util.Iterator;
import java.util.List;

import com.sun.fortress.useful.IterableOnce;
import com.sun.fortress.nodes.LValue;


final public class IterableOnceForLValueList implements IterableOnce<String> {
    /**
     * i = -1 before iterator() is called. i in 0..lhs.size()-1 while iterating.
     * i = lhs.size() when done.
     */
    int i = -1;

    List<? extends LValue> lhs;

    IterableOnce<String> current;

    public IterableOnceForLValueList(List<? extends LValue> lhs) {
        this.lhs = lhs;
    }

    public Iterator<String> iterator() {
        if (i >= 0) {
            throw new IllegalStateException("One-shot iterable");
        }
        nextCurrent();
        return this;
    }

    void nextCurrent() {
        if (++i < lhs.size()) {
            current = NodeUtil.stringNames(lhs.get(i));
        }
    }

    public boolean hasNext() {
        return i < lhs.size() && current.hasNext();
    }

    public String next() {
        String s = current.next();
        if (!current.hasNext()) {
            nextCurrent();
        }
        return s;
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
}
