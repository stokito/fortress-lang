/*******************************************************************************
    Copyright 2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/
package com.sun.fortress.compiler.runtimeValues;

public abstract class ArrowRTTI extends RTTI {

	 final RTTI[] inputsRTTI;
	 final RTTI outputRTTI;
	    
	 public ArrowRTTI(Class javaRep, RTTI[] params) {
		 super(javaRep);
		 if (params.length > 1) {
			 this.inputsRTTI = new RTTI[params.length-1];
			 for (int i = 0; i < params.length-1; i++) this.inputsRTTI[i] = params[i];
			 this.outputRTTI = params[params.length-1];
		 } else { //TODO: when we know how to handle void types, we can put something here, or bomb, or something
			 if (params.length == 1) this.outputRTTI = params[0];
			 else this.outputRTTI = null;
			 
			 this.inputsRTTI = null;
		 }
	 }
	 
	 public ArrowRTTI(Class javaRep, RTTI[] inputs, RTTI output) {
		 super(javaRep);
		 inputsRTTI = inputs;
		 outputRTTI = output;
	 }

	 /**
	  * Arrow extension-of test - contravariant in inputs, covariant in output 
	  */
	 public boolean argExtendsThis(RTTI other) {
		 if (super.argExtendsThis(other))
			 return true;
	     if (! (other instanceof ArrowRTTI))
	    	 return false;
	     RTTI[] otherInputsRTTI = ((ArrowRTTI) other).inputsRTTI;
	     if (otherInputsRTTI.length != inputsRTTI.length)
	    	 return false;
	     for (int i = 0; i < inputsRTTI.length; i++) {
	    	 if (! otherInputsRTTI[i].argExtendsThis(inputsRTTI[i])) //contravariant in inputs
	    		 return false;
	     }
	     if (! outputRTTI.argExtendsThis(((ArrowRTTI) other).outputRTTI)) //covariant in output
	    	 return false;
	     return true;
	 }
	
}
