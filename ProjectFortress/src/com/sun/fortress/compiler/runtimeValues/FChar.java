/*******************************************************************************
    Copyright 2009,2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.runtimeValues;

public class FChar  extends fortress.CompilerBuiltin.Char.DefaultTraitMethods implements fortress.CompilerBuiltin.Char {
    final char val;

    FChar(char x) { val = x; }
    public String toString() { return "" + val;}
    public char getValue() {return val;}
    public static FChar make(char x) {return new FChar(x);}

    @Override
    public RTTI getRTTI() { return RTTIc.ONLY; }
    
    public static class RTTIc extends RTTI {
        private RTTIc() { super(FChar.class); };
        public static final RTTI ONLY = new RTTIc();
    }

}
