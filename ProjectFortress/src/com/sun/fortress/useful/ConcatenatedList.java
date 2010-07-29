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

public class ConcatenatedList<E> extends AbstractList<E> {

    private final List<E> l1;
    private final List<E> l2;
    private final int sl1;
    private final int sl2;
    
    public ConcatenatedList(List<E> l1, List<E> l2) {
        this.l1 = l1;
        this.l2 = l2;
        sl1 = l1.size();
        sl2 = l2.size();
    }
    
    @Override
    public E get(int index) {
        if (index < sl1)
            return l1.get(index);
        else
            return l2.get(index - sl1);
    }

    @Override
    public int size() {
        return sl1 + sl2;
    }

}
