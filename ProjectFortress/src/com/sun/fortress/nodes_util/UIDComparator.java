/*******************************************************************************
    Copyright 2008, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.nodes_util;

import java.util.Comparator;

public class UIDComparator implements Comparator<UIDObject> {

    public final int compare(UIDObject o1, UIDObject o2) {
        // The subtraction hack works
        long c = o1.getUID() - o2.getUID();
        return c < 0 ? -1 : c == 0 ? 0 : 1;
    }

    public final static UIDComparator V = new UIDComparator();

}
