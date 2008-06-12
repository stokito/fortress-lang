/*
 * Created on Jun 12, 2008
 *
 */
package com.sun.fortress.useful;

import java.util.Iterator;

public class StringArrayIterator implements Iterator<String> {
    String[] a;
    int n;
    int i;

    public StringArrayIterator(String[] a, int n) {
        this.a = a;
        this.n = n;
    }

    public boolean hasNext() {
        return i < n;
    }

    public String next() {
        if (hasNext()) {
            return a[i++];
        }
        return null;
    }

    public void remove() {
        throw new Error("remove not implemented");
    }

}