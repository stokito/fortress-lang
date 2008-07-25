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

import java.util.Comparator;

/**
 * Provides an ordinary, boring, comparator for strings that provides the
 * default order.
 *
 * @author dr2chase
 */
public final class StringHashComparer implements Comparator<String> {

    public int compare(String arg0, String arg1) {
        int h0 = arg0.hashCode();
        int h1 = arg1.hashCode();
        if (h0 < h1) return -1;
        if (h0 > h1) return 1;
        return arg0.compareTo(arg1);
    }

    public final static StringHashComparer V = new StringHashComparer();

}
