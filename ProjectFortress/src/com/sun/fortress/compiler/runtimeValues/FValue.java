/*******************************************************************************
    Copyright 2009,2011 Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.runtimeValues;

public abstract class FValue implements fortress.AnyType.Any {
    
    @Override
    public RTTI getRTTI() {
        return null;
    }
    
    public fortress.CompilerBuiltin.String asString() {
        return FJavaString.make("Bogus value from FValue.asString()"); /* replaced in generated code; necessary for primitive hierarchy */
    }
    
    public String toString() {
        return this.asString().toString();
    }

}
