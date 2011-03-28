/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/
package com.sun.fortress.useful;

import java.util.Comparator;

/**
 * Given a predicate on T, returns a comparator that assigns "smaller" to
 * things that are in the predicate (for which the predicate returns true).
 * Two objects that are both in, or both out, compare equal.
 *
 * @author dr2chase
 */
public final class PredicateComparator<T> implements Comparator<T> {

    final F<T, Boolean> f;

    public PredicateComparator(F<T, Boolean> f) {
        this.f = f;
    }

    public int compare(T o1, T o2) {
        boolean o1in = f.apply(o1);
        boolean o2in = f.apply(o2);
        return o1in == o2in ? 0 : o1in ? -1 : 1;
    }

}
