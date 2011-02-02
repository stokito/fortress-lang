/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.env;

import static com.sun.fortress.exceptions.InterpreterBug.bug;
import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.Evaluator;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes_util.NodeUtil;

public class LazilyEvaluatedCell extends IndirectionCell {
    Expr exp;
    Environment e;

    public LazilyEvaluatedCell(Expr exp, Environment e) {
        this.exp = exp;
        this.e = e;
    }

    public String toString() {
        if (theValue == null) {
            return "thunk " + NodeUtil.dump(exp);
        }
        if (theValue instanceof IndirectionCell) return "Uninitialized " + theValue.getClass().getSimpleName();
        return theValue.toString();
    }

    public void storeValue(FValue f2) {
        bug("Cannot store value " + f2 + " into lazy cell; possible duplicate definition?");
    }

    public FValue getValue() {
        return getValueNull();
    }

    public FValue getValueNull() {
        if (theValue == null) {
            synchronized (this) {
                if (theValue == null) {
                    Expr exp0 = exp;
                    Environment e0 = e;
                    exp = null;
                    e = null;
                    theValue = (new Evaluator(e0)).eval(exp0);
                }
            }
        }
        return theValue;
    }

    public void postInit(Expr init, Environment containing) {
        if (exp == null && e == null) {
            exp = init;
            e = containing;
        } else {
            bug("Second init of lazy cell");
        }

    }
}
