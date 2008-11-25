/*******************************************************************************
    Copyright 2008 Sun Microsystems, Inc.,
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

import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.nodes.Applicable;
import com.sun.fortress.useful.HasAt;

/**
 * A MethodClosureInstance is the result of instantiating an object generic method.
 * Its environment is unusual.
 * @author chase
 */
public class MethodClosureInstance extends MethodClosure implements MethodInstance {

    GenericMethod generator;
    Environment genericEnv;

    public MethodClosureInstance(Environment within, Environment genericEnv, Applicable fndef, FType definer, List<FType> args, GenericMethod generator) {
        super(within, fndef, definer, args);
        this.generator = generator;
        this.genericEnv = genericEnv;
        if (!genericEnv.getBlessed()) {
            System.out.println("Creating a MethodClosureInstance for "+fndef+args+
                               "with unblessed genericEnv "+genericEnv);
        }
    }

    public Environment getEvalEnv() {
        return genericEnv;
    }

    // The choice of evaluation environment is the only difference between applying
    // a MethodClosure and applying its subclass, a PartiallyDefinedMethod (which
    // appears to actually represent some piece of a functional method in practice).
    @Override
    protected Environment envForApplication(FObject selfValue, HasAt loc) {
        return selfValue.getLexicalEnv().genericLeafEnvHack(genericEnv, loc);
    }

    public GenericMethod getGenerator() {
        return generator;
    }

}
