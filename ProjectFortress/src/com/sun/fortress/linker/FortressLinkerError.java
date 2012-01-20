/*******************************************************************************
    Copyright 2009,2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/

package com.sun.fortress.linker;

public class FortressLinkerError extends Error {
	public final String message;
	
	public FortressLinkerError(String message) {
		this.message = message;
	}
}