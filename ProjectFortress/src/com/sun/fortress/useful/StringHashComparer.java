/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.useful;

import java.util.Comparator;

/**
 * Provides an ordinary, boring, comparator for strings that provides the
 * default order.
 *
 * @author dr2chase
 */
public final class StringHashComparer implements Comparator<String>, java.io.Serializable {

    public int compare(String arg0, String arg1) {
        int h0 = arg0.hashCode();
        int h1 = arg1.hashCode();
        if (h0 < h1) return -1;
        if (h0 > h1) return 1;
        return arg0.compareTo(arg1);
    }

    public final static StringHashComparer V = new StringHashComparer();

}
