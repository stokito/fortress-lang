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

package com.sun.fortress.interpreter.evaluator.values;

import java.util.List;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.nodes.Applicable;

/**
 * When a generic function is instantiated, the result is a closure instance,
 * which is very much like a closure.
 * @author chase
 */
public class ClosureInstance extends Closure {

    public ClosureInstance(BetterEnv e, Applicable fndef, List<FType> args, FGenericFunction generator) {
        super(e, fndef, args);
        this.generator = generator;
    }

    FGenericFunction generator;

    public FGenericFunction getGenerator() {
        return generator;
    }

}
