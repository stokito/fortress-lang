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

    final Object error;
    
    public FException(Object o) { 
        super("Fortress Exception: " + o.toString());
        error = o;
        exceptions.put(o, this);
    }

    public String toString() { return error.toString();}
    public Object getValue() { return error; }

}
