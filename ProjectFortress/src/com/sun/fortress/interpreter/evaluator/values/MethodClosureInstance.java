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
 * A MethodClosureInstance is the result of instantiating an object generic method.
 * Its environment is unusual.
 *
 * @author chase
 */
public class MethodClosureInstance extends MethodClosure implements MethodInstance {

    GenericMethod generator;
    Environment genericEnv;

    public MethodClosureInstance(Environment within,
                                 Environment genericEnv,
                                 Applicable fndef,
                                 FType definer,
                                 List<FType> args,
                                 GenericMethod generator) {
        super(within, fndef, definer, args);
        this.generator = generator;
        this.genericEnv = genericEnv;
        if (!genericEnv.getBlessed()) {
            System.out.println(
                    "Creating a MethodClosureInstance for " + fndef + args + "with unblessed genericEnv " + genericEnv);
        }
    }

    public Environment getEvalEnv() {
        return genericEnv;
    }

    // The choice of evaluation environment is the only difference between applying
    // a MethodClosure and applying its subclass, a PartiallyDefinedMethod (which
    // appears to actually represent some piece of a functional method in practice).
    @Override
    protected Environment envForApplication(FObject selfValue) {
        return selfValue.getLexicalEnv().genericLeafEnvHack(genericEnv);
    }

    public GenericMethod getGenerator() {
        return generator;
    }

}
