/*******************************************************************************
    Copyright 2007 Sun Microsystems, Inc.,
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

package com.sun.fortress.interpreter.evaluator;

import com.sun.fortress.interpreter.evaluator.values.FBool;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.nodes.NodeVisitor;
import com.sun.fortress.interpreter.nodes.Id;
import com.sun.fortress.interpreter.nodes.VarRefExpr;


public class LHSAtomicEvaluator extends NodeVisitor<FValue> {

    LHSAtomicEvaluator(Evaluator evaluator,
		       FValue old_value,
		       FValue new_value) {
        this.evaluator = evaluator;
	this.old_value = old_value;
	this.new_value = new_value;
    }

    Evaluator evaluator;
    FValue old_value;
    FValue new_value;
    boolean debug = false;
    public void debugPrint(String debugString) {
	if (debug) System.out.println(debugString);
    }

    public FValue forVarRefExpr(VarRefExpr x) {
	debugPrint("LHSforVarRefExpr " + x);
	Id var = x.getVar();
	String s = var.getName();
	debugPrint("LHSforVarRefExpr " + s);
	return FBool.make(evaluator.e.casValue(s, old_value, new_value));
    }
}
