/*******************************************************************************
    Copyright 2009,2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.runtimeValues;

import com.sun.fortress.compiler.runtimeValues.FortressBufferedReader;

public class FJavaBufferedReader extends fortress.CompilerBuiltin.JavaBufferedReader.DefaultTraitMethods implements fortress.CompilerBuiltin.JavaBufferedReader {
    // This is a temporary hack until we get a full-blown theory of automatically importing all Java libraries.
    // We need to have some basic file I/O in order to implement the BirdCount benchmark.
    final FortressBufferedReader val;

    FJavaBufferedReader(FortressBufferedReader x) { val = x; }

    public String toString() {
	return "[buffered reader " + val + "]";
    }

    public FortressBufferedReader getValue() {return val;}
    public static FJavaBufferedReader make(FortressBufferedReader x) {return new FJavaBufferedReader(x);}

    @Override
    public RTTI getRTTI() { return RTTIjbr.ONLY; }
    
    public static class RTTIjbr extends RTTI {
        private RTTIjbr() { super(FJavaBufferedReader.class); };
        public static final RTTI ONLY = new RTTIjbr();
    }

}
