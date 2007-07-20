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

import java.util.List;

import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.IUOTuple;
import com.sun.fortress.nodes.ArrayExpr;
import com.sun.fortress.nodes.ArrayElement;
import com.sun.fortress.nodes.ArrayElements;


public class EvaluatorInPaste extends Evaluator {
    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.Evaluator#forArrayElements(com.sun.fortress.interpreter.nodes.ArrayElements)
     */
    @Override
    public FValue forArrayElements(ArrayElements x) {
        List<ArrayExpr> elements = x.getElements();
        /* This dies bafflingly if we do the next line in parallel. */
        List<FValue> values = evalExprList/*Parallel*/(elements);
        return new IUOTuple(values, x);
    }

    @Override
    public FValue forArrayElement(ArrayElement x) {
        // MDEs occur only within ArrayElements, and reset
        // row evaluation to an outercontext (in the scope
        // of the element, that is).
        Evaluator notInPaste = new Evaluator(this);
        return x.getElement().accept(notInPaste);
    }

    EvaluatorInPaste(Evaluator e) {
        super(e);
    }
}
