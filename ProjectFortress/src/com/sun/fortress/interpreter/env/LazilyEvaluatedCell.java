/*
 * Created on Jan 8, 2007
 *
 */
package com.sun.fortress.interpreter.env;

import com.sun.fortress.interpreter.evaluator.Evaluator;
import com.sun.fortress.interpreter.evaluator.InterpreterError;
import com.sun.fortress.interpreter.evaluator.ProgramError;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.nodes.Expr;

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
            throw new ProgramError(exp, "Value is self-dependent, cannot be evaluated.");
        }
       
        return theValue;
    }
}
