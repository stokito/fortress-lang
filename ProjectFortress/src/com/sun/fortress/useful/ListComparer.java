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

import java.util.Comparator;

public class ListComparer<T extends Comparable<T>> extends AnyListComparer<T> {

    public ListComparer() {
        super(new Comparator<T>() {
            public int compare(T o1, T o2) {
                /* Deal appropriately with null, which for convenience
                 * we consider to be less than everything else. */
                if (o1==null && o2==null) return 0;
                if (o1==null) return -1;
                if (o2==null) return 1;
                return o1.compareTo(o2);
            }
        });
    }

    public final static ListComparer<String> stringListComparer = new ListComparer<String>();
}
