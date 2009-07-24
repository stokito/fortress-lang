/*******************************************************************************
 Copyright 2008 Sun Microsystems, Inc.,
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
