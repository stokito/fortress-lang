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
