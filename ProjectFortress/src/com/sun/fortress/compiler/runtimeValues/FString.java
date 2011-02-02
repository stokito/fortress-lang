/*******************************************************************************
    Copyright 2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.runtimeValues;

public class FString  extends fortress.CompilerBuiltin.String.DefaultTraitMethods implements fortress.CompilerBuiltin.String {
    final String val;

    FString(String x) { val = x; }
    public String getValue() { return val;}
    public String toString() { return val;}
    public static FString make(String s) { return new FString(s);}
    public static FString concatenate(FString s1, FString s2) { return new FString(s1.toString() + s2.toString());}
}
