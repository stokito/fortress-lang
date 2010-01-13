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

public final class FFloatLiteral extends fortress.CompilerBuiltin.FloatLiteral.DefaultTraitMethods
        implements fortress.CompilerBuiltin.FloatLiteral {

    private String val;

    private FFloatLiteral(String val) {
        this.val = val;
    }

    private FFloatLiteral(double val) {
	this(new Double(val).toString());
    }

    public static FFloatLiteral make(float x) {
        return new FFloatLiteral((double)x);
    }

    public static FFloatLiteral make(double x) {
        return new FFloatLiteral(x);
    }

    public static FFloatLiteral make(String s) {
        return new FFloatLiteral(s);
    }

    public String toString() {
        return val;
    }

    public String asString() {
        return this.toString();
    }

    public Error outOfRange(String t) {
        return new Error("Not in range for "+t+": "+this);
    }

    public FRR64 asRR64() {
        return FRR64.make(Double.valueOf(val));
    }


    public FRR32 asRR32() {
        return FRR32.make(Float.valueOf(val));
    }
}
