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

package com.sun.fortress.interpreter.env;

import com.sun.fortress.interpreter.evaluator.CircularDependenceError;
import com.sun.fortress.interpreter.evaluator.Evaluator;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes_util.NodeUtil;

import static com.sun.fortress.interpreter.evaluator.InterpreterBug.bug;

public class LazilyEvaluatedCell extends IndirectionCell {
    Expr exp;
    BetterEnv e;

    public LazilyEvaluatedCell( Expr exp, BetterEnv e) {
        this.exp = exp;
        this.e = e;
    }

    public String toString() {
        if (theValue==null) { 
            return "thunk " + NodeUtil.dump(exp);
        }
        if (theValue instanceof IndirectionCell)
            return "Uninitialized " + theValue.getClass().getSimpleName();
        return theValue.toString();
    }
    
    public void storeValue(FValue f2) {
        bug("Cannot store value "+ f2 +" into lazy cell; possible duplicate definition?");
    }

    public FValue getValue() {
        if (theValue == null) {
            theValue = this;
            theValue = (new Evaluator(e)).eval(exp);
            exp = null;
            e = null;
        } else if (theValue == this) {
            throw new CircularDependenceError(exp, "Value is self-dependent, cannot be evaluated.");
        }

        return theValue;
    }
}
