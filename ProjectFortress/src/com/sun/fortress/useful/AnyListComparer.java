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
import java.util.List;

public class AnyListComparer<T> implements Comparator<List<T>> {

    Comparator<T> compareElements;

    public int compare(List<T> arg0, List<T> arg1) {
        int l0 = arg0.size();
        int l1 = arg1.size();
        if (l0 < l1) return -1;
        if (l0 > l1) return 1;
        for (int i = 0; i < l0; i++) {
            int c = compareElements.compare(arg0.get(i),arg1.get(i));
            if (c != 0)
                return c;
        }
        return 0;
    }


    public AnyListComparer (Comparator<T> compareElements) {
        this.compareElements = compareElements;
    }

}
