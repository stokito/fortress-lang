/********************************************************************************
 Copyright 2010, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ********************************************************************************/

package com.sun.fortress.runtimeSystem;

public abstract class MiscCodegenFunctions {
    /** Backstop function that is called when overload matching fails.
     * Returns the exception to be thrown!
     * cf OverloadSet
     */
    public static Error overloadMatchFailure() {
        return new Error("Overloading instanceof match failure: Should not happen!");
    }
}
