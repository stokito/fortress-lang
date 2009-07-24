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

/**
 * Provides an ordinary, boring, comparator for things that provide a
 * default order.  Also a reversed comparator, also methods to extract
 * them in a make-Java-generics-happy way.
 *
 * @author dr2chase
 */
public class DefaultComparator<T extends Comparable> implements Comparator<T> {

    public int compare(T arg0, T arg1) {
        return arg0.compareTo(arg1);
    }

    final static class Reversed extends DefaultComparator {
        public int compare(String arg0, String arg1) {
            int rc = arg0.compareTo(arg1);
            // Don't forget that -MININT = MININT
            return rc >= 0 ? -rc : 1;
        }
    }

    public final static <U extends Comparable> DefaultComparator<U> normal() {
        return (DefaultComparator<U>) V;
    }

    public final static <U extends Comparable> DefaultComparator<U> reversed() {
        return (DefaultComparator<U>) Vreversed;
    }

    public final static DefaultComparator V = new DefaultComparator();
    public final static DefaultComparator Vreversed = new Reversed();

}
