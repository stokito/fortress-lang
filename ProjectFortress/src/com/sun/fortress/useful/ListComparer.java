/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.useful;

import java.util.Comparator;

public class ListComparer<T extends Comparable<T>> extends AnyListComparer<T> {

    public ListComparer() {
        super(new Comparator<T>() {
            public int compare(T o1, T o2) {
                /* Deal appropriately with null, which for convenience
                 * we consider to be less than everything else. */
                if (o1 == null && o2 == null) return 0;
                if (o1 == null) return -1;
                if (o2 == null) return 1;
                return o1.compareTo(o2);
            }
        });
    }

    public final static ListComparer<String> stringListComparer = new ListComparer<String>();
}
