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
 * A copy of list l, except that e has been Deleted at index i.
 * Thus, new DeletedList(l,0) inserts e at the beginning of the list.
 * @author dr2chase
 */
public class DeletedList<T> extends AbstractList<T> {
    List<T> l;
    int i;

    public DeletedList(List<T> l, int i) {
        this.l = l;
        this.i = i;
    }

    @Override
    public T get(int arg0) {
        if (arg0 < i)
            return l.get(arg0);
        return l.get(arg0+1);
    }

    @Override
    public int size() {
        return l.size() - 1;
    }

}
