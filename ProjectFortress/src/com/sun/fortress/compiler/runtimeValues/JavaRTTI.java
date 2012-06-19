package com.sun.fortress.compiler.runtimeValues;

public class JavaRTTI extends RTTI {

	public JavaRTTI(Class javaRep) {
		super(javaRep);
		// TODO Auto-generated constructor stub
	}

    //@Override
    public boolean runtimeSupertypeOf(RTTI other) {
        return false;
    }
	
}
