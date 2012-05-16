/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

/*
 * Created on Apr 24, 2007
 *
 */
package com.sun.fortress.useful;

import java.util.AbstractList;
import java.util.List;

public class AssignedList<T> extends AbstractList<T> {
    List<T> l;
    T e;
    int i;

    public AssignedList(List<T> l, int i, T e) {
        this.l = l;
        this.e = e;
        this.i = i;
    }

    @Override
    public T get(int arg0) {
        if (arg0 == i) return e;
        return l.get(arg0);
    }

    @Override
    public int size() {
        return l.size();
    }

}
