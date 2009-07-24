/*******************************************************************************
 Copyright 2009 Sun Microsystems, Inc.,
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

public final class ComposedComparator<T> implements Comparator<T> {

    final Comparator<T> first;
    final Comparator<T> second;

    public ComposedComparator(Comparator<T> first, Comparator<T> second) {
        this.first = first;
        this.second = second;
    }

    public int compare(T o1, T o2) {
        return Useful.compare(o1, o2, first, second);
    }

}
