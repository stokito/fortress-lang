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

/*
 * Created on Jan 8, 2007
 *
 */
package com.sun.fortress.interpreter.env;

import com.sun.fortress.interpreter.evaluator.CircularDependenceError;
import com.sun.fortress.interpreter.evaluator.Evaluator;
import com.sun.fortress.interpreter.evaluator.InterpreterError;
import com.sun.fortress.interpreter.evaluator.ProgramError;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.nodes.Expr;

public class LazilyEvaluatedCell extends IndirectionCell {
    Expr exp;
    BetterEnv e;

    public LazilyEvaluatedCell( Expr exp, BetterEnv e) {
        this.exp = exp;
        this.e = e;
    }

    public void storeValue(FValue f2) {
        throw new InterpreterError("Cannot store into lazy cell");
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
