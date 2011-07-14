/*******************************************************************************
    Copyright 2009,2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.runtimeValues;

import com.sun.fortress.compiler.runtimeValues.FortressBufferedWriter;

public class FJavaBufferedWriter extends fortress.CompilerBuiltin.JavaBufferedWriter.DefaultTraitMethods implements fortress.CompilerBuiltin.JavaBufferedWriter {
    // This is a temporary hack until we get a full-blown theory of automatically importing all Java libraries.
    // We need to have some basic file I/O in order to implement the BirdCount benchmark.
    final FortressBufferedWriter val;

    FJavaBufferedWriter(FortressBufferedWriter x) { val = x; }

    public String toString() {
	return "[buffered reader " + val + "]";
    }

    public FortressBufferedWriter getValue() {return val;}
    public static FJavaBufferedWriter make(FortressBufferedWriter x) {return new FJavaBufferedWriter(x);}

    @Override
    public RTTI getRTTI() { return RTTIjbw.ONLY; }
    
    public static class RTTIjbw extends RTTI {
        private RTTIjbw() { super(FJavaBufferedWriter.class); };
        public static final RTTI ONLY = new RTTIjbw();
    }

}
