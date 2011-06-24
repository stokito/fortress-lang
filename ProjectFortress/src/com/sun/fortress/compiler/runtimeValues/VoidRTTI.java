package com.sun.fortress.compiler.runtimeValues;

public class VoidRTTI extends RTTI {
	
	public static final RTTI ONLY = new VoidRTTI(VoidRTTI.class);
	
	public VoidRTTI(Class javaRep) {
		super(javaRep);
	}
	
	public boolean argExtendsThis(RTTI other) {
        if (other instanceof VoidRTTI) return true;
		return false;
    }

}
