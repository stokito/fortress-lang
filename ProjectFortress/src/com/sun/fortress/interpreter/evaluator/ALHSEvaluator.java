/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator;

import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.useful.HasAt;


/**
 * Assignment LHS Evaluator
 * <p/>
 * Just like LHS Evaluator, except where different.
 */

public class ALHSEvaluator extends LHSEvaluator {

    public ALHSEvaluator(Evaluator evaluator, FValue value) {
        super(evaluator, value);
    }

    /**
     * @param s
     */
    protected void putOrAssignVariable(HasAt x, String s) {
        evaluator.e.assignValue(x, s, value);
    }


}
