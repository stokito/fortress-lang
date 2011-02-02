/*******************************************************************************
 Copyright 2009,2010, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.nativeHelpers;

public class equality {
    public static boolean sEquiv(fortress.AnyType.Any a, fortress.AnyType.Any b) {
        // Eventually need to deal with tuples, value types, etc.
        return a == b;
    }
}
