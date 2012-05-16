/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

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
