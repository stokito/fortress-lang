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
        return l.get(l.size()-arg0-1);
    }

    @Override
    public int size() {
        return l.size();
    }

}
