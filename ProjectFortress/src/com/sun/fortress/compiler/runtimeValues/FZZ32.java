/*******************************************************************************
    Copyright 2009 Sun Microsystems, Inc.,
    4150 Network Circle, Santa Clara, California 95054, U.S.A.
    All rights reserved.

    U.S. Government Rights - Commercial software.
    Government users are subject to the Sun Microsystems, Inc. standard
    license agreement and applicable provisions of the FAR and its supplements.

    Use is subject to license terms.

    This distribution may include materials developed by third parties.

    Sun, Sun Microsystems, the Sun logo and Java are trademarks or registered
    trademarks of Sun Microsystems, Inc. in the U.S. and other countries.
 ******************************************************************************/

package com.sun.fortress.compiler.runtimeValues;

public class FZZ32 extends fortress.CompilerBuiltin.ZZ32.DefaultTraitMethods implements fortress.CompilerBuiltin.ZZ32 {
    final int val;

    FZZ32(int x) { val = x; }
    public String toString() { return String.valueOf(val);}
    public FString asString() { return new FString(String.valueOf(val));}
    public int getValue() {return val;}
    public static FZZ32 make(int x) {
        return new FZZ32(x);
        }
    public static FZZ32 plus(FZZ32 a, FZZ32 b) {return make(a.getValue() + b.getValue());}
 
   
}
