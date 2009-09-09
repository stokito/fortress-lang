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

public final class FRR64 extends fortress.CompilerBuiltin.RR64.DefaultTraitMethods
        implements fortress.CompilerBuiltin.RR64 {
    final double val;

    FRR64(double x) { val = x; }
    public String toString() { return String.valueOf(val); }
    public FString asString() { return new FString(String.valueOf(val)); }
    public double getValue() {return val;}
    public static FRR64 make(double x) {return new FRR64(x);}
}
