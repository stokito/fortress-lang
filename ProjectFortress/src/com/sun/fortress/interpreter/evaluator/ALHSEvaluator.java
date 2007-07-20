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

import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.useful.HasAt;


/**
 * Assignment LHS Evaluator
 *
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
