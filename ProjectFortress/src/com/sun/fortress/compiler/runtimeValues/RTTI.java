/*******************************************************************************
    Copyright 2009,2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.runtimeValues;

public abstract class RTTI {
    
    final Class javaRep;
    
    public boolean argExtendsThis(RTTI other) {
        return this == other;
    }
    
    public RTTI(Class javaRep) {
        this.javaRep = javaRep;
    }
}
