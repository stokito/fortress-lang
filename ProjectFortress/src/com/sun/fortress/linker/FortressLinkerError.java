/*******************************************************************************
    Copyright 2012, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/

package com.sun.fortress.linker;

class FortressLinkerError extends Error {
	final String message;
	
	FortressLinkerError(String message) {
		this.message = message;
	}
}