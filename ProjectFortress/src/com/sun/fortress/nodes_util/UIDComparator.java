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
