/*******************************************************************************
 Copyright 2010, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.useful;

import java.util.AbstractList;
import java.util.List;

public class ProjectedList<T, U> extends AbstractList<U> {

    private final List<T> l;
    private final F<T,U> f;
    
    public ProjectedList(List<T> l, F<T,U> f) {
        this.l = l;
        this.f = f;
    }
    
    @Override
    public U get(int index) {
        return f.apply(l.get(index));
    }

    @Override
    public int size() {
        return l.size();
    }

}
