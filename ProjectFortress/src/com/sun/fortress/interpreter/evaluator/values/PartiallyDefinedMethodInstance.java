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
import com.sun.fortress.interpreter.evaluator.Evaluator;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.nodes.Applicable;
import com.sun.fortress.useful.HasAt;


/**
 * A PartiallyDefinedMethodInstance is the result of instantiating a trait generic method.
 * Its environment is unusual.
 * @author chase
 */
public class PartiallyDefinedMethodInstance extends PartiallyDefinedMethod  implements MethodInstance {

    GenericMethod generator;
    //BetterEnv evaluationEnv;

    public MethodClosure completeClosure(BetterEnv env) {
        return this;
        // return new TraitMethodInstance(this, evaluationEnv, selfName(), generator);
    }

    public PartiallyDefinedMethodInstance(BetterEnv within, BetterEnv evaluationEnv, Applicable fndef,
            List<FType> args, GenericMethod generator) {
        super(within, evaluationEnv, fndef, args);
        //this.evaluationEnv = evaluationEnv;
        this.generator = generator;
    }

    public GenericMethod getGenerator() {
        return generator;
    }

    // I think this should not be called; the com.sun.fortress.interpreter.evaluator is supposed to short-circuit
    // around this to generate a specific, corrected instance of a generic method.
    public FValue applyMethod(List<FValue> args, FObject selfValue, HasAt loc) {
        args = conditionallyUnwrapTupledArgs(args);
        // TraitMethods do not get their environment from the object.
        Evaluator eval = new Evaluator(buildEnvFromEnvAndParams(
                evaluationEnv, // getWithin(),
                args, loc));
        eval.e.putValue(selfName(), selfValue);
         return eval.eval(getBody());
     }

}
