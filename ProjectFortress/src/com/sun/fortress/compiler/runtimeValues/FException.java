/*******************************************************************************
    Copyright 2009,2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.runtimeValues;

import java.util.HashMap;

public class FException extends java.lang.Exception {

    static HashMap exceptions = new HashMap();

    final FValue error;
    
    public FException(FValue v) { 
        super("Fortress Exception: " + v.asString());
        error = v;
        exceptions.put(v, this);
    }

    public String toString() { return error.asString().getValue();}
    public Object getValue() { return error; }

}
