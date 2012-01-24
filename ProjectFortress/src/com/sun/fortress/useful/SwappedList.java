/*******************************************************************************
 Copyright 2010, Oracle and/or its affiliates.
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

/**
 * A copy of list l, except the element at index i has been swapped to front
 * @author dr2chase
 */
public class SwappedList<T> extends AbstractList<T> {
    List<T> l;
    int i;

    public SwappedList(List<T> l, int i) {
        this.l = l;
        this.i = i;
    }

    @Override
    public T get(int arg0) {
        if (arg0 == 0)
            return l.get(i);
        else if (arg0 <= i) {
            return l.get(arg0-1);
        } else {
            return l.get(arg0);
        }
    }

    @Override
    public int size() {
        return l.size();
    }

}
