/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.values;

import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.nodes.Applicable;

import java.util.List;

/**
 * When a generic function is instantiated, the result is a closure instance,
 * which is very much like a closure.
 *
 * @author chase
 */
public class ClosureInstance extends FunctionClosure {

    public ClosureInstance(Environment e, Applicable fndef, List<FType> args, FGenericFunction generator) {
        super(e, fndef, args);
        this.generator = generator;
    }

    FGenericFunction generator;

    public FGenericFunction getGenerator() {
        return generator;
    }

}
