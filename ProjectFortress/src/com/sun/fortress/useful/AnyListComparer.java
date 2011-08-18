/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.useful;

import java.util.Comparator;
import java.util.List;

public class AnyListComparer<T> implements Comparator<List<? extends T>> {

    Comparator<T> compareElements;

    public int compare(List<? extends T> arg0, List<? extends T> arg1) {
        int l0 = arg0.size();
        int l1 = arg1.size();
        if (l0 < l1) return -1;
        if (l0 > l1) return 1;
        for (int i = 0; i < l0; i++) {
            int c = compareElements.compare(arg0.get(i), arg1.get(i));
            if (c != 0) return c;
        }
        return 0;
    }


    public AnyListComparer(Comparator<T> compareElements) {
        this.compareElements = compareElements;
    }

}
