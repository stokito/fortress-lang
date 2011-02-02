/*******************************************************************************
    Copyright 2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.runtimeValues;

public final class FZZ32 extends fortress.CompilerBuiltin.ZZ32.DefaultTraitMethods
        implements fortress.CompilerBuiltin.ZZ32 {
    final int val;

    private FZZ32(int x) { val = x; }
    public String toString() { return String.valueOf(val);}
    public FString asString() { return new FString(String.valueOf(val));}
    public int getValue() {return val;}
    public static FZZ32 make(int x) {
        return new FZZ32(x);
    }
    public static FZZ32 plus(FZZ32 a, FZZ32 b) {return make(a.getValue() + b.getValue());}
}
