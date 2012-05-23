/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.useful;

import java.util.AbstractList;
import java.util.List;

public class ReversedList<T> extends AbstractList<T> {

    List<T> l;

    public ReversedList(List<T> l) {
        this.l = l;
    }

    @Override
    public T get(int arg0) {
        return l.get(l.size() - arg0 - 1);
    }

    @Override
    public int size() {
        return l.size();
    }

}
