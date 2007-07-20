/*******************************************************************************
    Copyright 2007 Sun Microsystems, Inc.,
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

import java.util.List;

import java.util.AbstractList;

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
