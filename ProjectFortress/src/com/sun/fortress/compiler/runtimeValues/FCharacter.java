/*******************************************************************************
    Copyright 2009,2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.runtimeValues;

public class FCharacter  extends fortress.CompilerBuiltin.Character.DefaultTraitMethods implements fortress.CompilerBuiltin.Character {
    // Fortress chars are not equivalent to Java chars.
    // Fortress supports 21-bit Unicode, so we use int to represent char instead of just Java char.
    final int val;

    FCharacter(int x) { val = x; }

    public String toString() {
	if (0 <= val && val <= 0xFFFF) return String.valueOf((char) val);
        int[] temp = { val };
        return new String(temp, 0, 1);
    }

    public int getValue() {return val;}
    public static FCharacter make(int x) {return new FCharacter(x);}

    @Override
    public RTTI getRTTI() { return RTTIc.ONLY; }
    
    public static class RTTIc extends RTTI {
        private RTTIc() { super(FCharacter.class); };
        public static final RTTI ONLY = new RTTIc();
    }

}
