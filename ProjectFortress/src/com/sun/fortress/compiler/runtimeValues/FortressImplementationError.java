/*******************************************************************************
 Copyright 2011 Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.runtimeValues;

public final class FortressImplementationError extends Error {
    public FortressImplementationError(String msg) {
	super("Fortress internal implementation error: " + msg);
    }
    public FortressImplementationError(Throwable x) {
	super(x.getMessage());
    }
}
