/*******************************************************************************
    Copyright 2009,2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.runtimeValues;

import java.util.HashMap;

public class FException extends java.lang.Error {

    static HashMap exceptions = new HashMap();

    final FValue error;
    
    public FException(FValue v) { 
        super("Fortress Exception: " + v.asString());
        // System.out.println("Creating Fortress exception with error value " + v);
        error = v;
        exceptions.put(v, this);
    }

    public String toString() {
        String the_class = error.getClass().toString();
        FJavaString the_java_string = (FJavaString) (error.asString());
        String the_string = the_java_string.getValue();
         return "FortressException: " + the_class + " with string " + the_string;
         }

    // Codegen.java knows about the return type of this getValue method.
    public FValue getValue() { return error; }
    public FException make(FValue x) { return new FException(x); }

}
