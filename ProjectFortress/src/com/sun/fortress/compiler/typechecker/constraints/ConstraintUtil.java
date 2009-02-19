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

package com.sun.fortress.compiler.typechecker.constraints;

import com.sun.fortress.compiler.typechecker.constraints.JavaConstraintUtil;
import com.sun.fortress.scala_src.typechecker.ScalaConstraintUtil;
import com.sun.fortress.nodes.*;
import com.sun.fortress.compiler.typechecker.*;

/**
 * Temporary Factory that can produce both Java and Scala
 * constraint formulas
 */
public class ConstraintUtil {

	private static boolean java = true;

	public static void useJavaFormulas(){
		java = true;
	}

	public static void useScalaFormulas(){
		java = false;
	}

	public static void setJava(Boolean _java){
		java = _java;
	}

	public static ConstraintFormula trueFormula() {
		if(java)
			return JavaConstraintUtil.trueFormula();
		else
			return ScalaConstraintUtil.trueFormula();
	}
	public static ConstraintFormula falseFormula() {
		if(java)
			return JavaConstraintUtil.falseFormula();
		else
			return ScalaConstraintUtil.falseFormula();
	}

	public static ConstraintFormula upperBound(_InferenceVarType ivar, Type type, SubtypeHistory h) {
		if(java)
			return JavaConstraintUtil.upperBound(ivar,type,h);
		else
			return ScalaConstraintUtil.upperBound(ivar,type,h);
	}

	public static ConstraintFormula lowerBound(_InferenceVarType ivar, Type type, SubtypeHistory h) {
		if(java)
			return JavaConstraintUtil.lowerBound(ivar,type,h);
		else
			return ScalaConstraintUtil.lowerBound(ivar,type,h);
	}

	public static ConstraintFormula bigAnd(Iterable<? extends ConstraintFormula> constraints,
			SubtypeHistory hist) {
		ConstraintFormula result = trueFormula();
		for( ConstraintFormula constraint : constraints ) {
			result = result.and(constraint, hist);
		}
		return result;
	}

	public static ConstraintFormula fromBoolean(Boolean bool){
		if(java)
			return JavaConstraintUtil.fromBoolean(bool);
		else
			return ScalaConstraintUtil.fromBoolean(bool);
	}

}
