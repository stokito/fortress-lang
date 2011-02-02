/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/
package com.sun.fortress.useful;

public class PairOfComparable<T extends Comparable, U extends Comparable> extends Pair<T, U>
        implements Comparable<Pair<T, U>> {

    public PairOfComparable(T a, U b) {
        super(a, b);
        // TODO Auto-generated constructor stub
    }

    public int compareTo(Pair<T, U> o) {
        int c = getA().compareTo(o.getA());
        if (c != 0) return c;
        return getB().compareTo(o.getB());
    }

}
