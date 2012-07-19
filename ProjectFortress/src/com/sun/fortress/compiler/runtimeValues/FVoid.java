/*******************************************************************************
    Copyright 2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.runtimeValues;

public class FVoid  extends FValue {
    private FVoid() { }
    private final static FVoid V = new FVoid();
    public String toString() { return "()";}
    public FVoid getValue() {return V;}
    public static FVoid make() {return V;}
    
    @Override
    public RTTI getRTTI() { return /* RTTIc.ONLY*/ VoidRTTI.ONLY; }
    
    public static class RTTIc extends VoidRTTI {
        public RTTIc() { super(FVoid.class); };
        // public static final RTTI ONLY = new RTTIc();
    }
}
