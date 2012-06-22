/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.useful;

/**
 * Like a void, for Java generics.
 * The only way to get a Voidoid is to return null.
 */
public final class Voidoid {
    private Voidoid() {
        throw new Error("Don't try to get cute with reflection.");
    }
}
