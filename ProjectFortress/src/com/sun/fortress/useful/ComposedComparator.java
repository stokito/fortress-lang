/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.useful;

import java.util.Comparator;

public final class ComposedComparator<T> implements Comparator<T> {

    final Comparator<T> first;
    final Comparator<T> second;

    public ComposedComparator(Comparator<T> first, Comparator<T> second) {
        this.first = first;
        this.second = second;
    }

    public int compare(T o1, T o2) {
        return Useful.compare(o1, o2, first, second);
    }

}
