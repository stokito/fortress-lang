/*******************************************************************************
 Copyright 2010 Sun Microsystems, Inc.,
 4150 Network Circle, Santa Clara, California 95054, U.S.A.
 All rights reserved.

 U.S. Government Rights - Commercial software.
 Government users are subject to the Sun Microsystems, Inc. standard
 license agreement and applicable provisions of the FAR and its supplements.

 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 Sun, Sun Microsystems, the Sun logo and Java are trademarks or registered
 trademarks of Sun Microsystems, Inc. in the U.S. and other countries.
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
 * Thus, new DeletedList(l,0,e) inserts e at the beginning of the list.
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
