/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator;

import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.IUOTuple;
import com.sun.fortress.nodes.ArrayElement;
import com.sun.fortress.nodes.ArrayElements;
import com.sun.fortress.nodes.ArrayExpr;

import java.util.List;


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
