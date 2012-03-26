/*******************************************************************************
 Copyright 2010, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.useful;

import java.util.AbstractList;
import java.util.List;

/**
 * A copy of list l, except that e has been inserted at index i.
 * Thus, new InsertedList(l,0,e) inserts e at the beginning of the list.
 * @author dr2chase
 */
public class InsertedList<T> extends AbstractList<T> {
    List<T> l;
    T e;
    int i;

    public InsertedList(List<T> l, int i, T e) {
        this.l = l;
        this.e = e;
        this.i = i;
    }

    @Override
    public T get(int arg0) {
        if (arg0 < i)
            return l.get(arg0);
        if (arg0 == i)
            return e;
        return l.get(arg0-1);
    }

    @Override
    public int size() {
        return 1 + l.size();
    }

}
