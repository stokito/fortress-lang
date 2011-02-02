/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.values;

import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.Evaluator;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.nodes.Applicable;

import java.util.List;


/**
 * A PartiallyDefinedMethodInstance is the result of instantiating a trait generic method.
 * Its environment is unusual.
 *
 * @author chase
 */
public class TraitMethodInstance extends TraitMethod implements MethodInstance {

    GenericMethod generator;
    //BetterEnv evaluationEnv;

    public MethodClosure completeClosure(Environment env) {
        return this;
        // return new TraitMethodInstance(this, evaluationEnv, selfName(), generator);
    }

    public TraitMethodInstance(Environment within,
                               Environment evaluationEnv,
                               Applicable fndef,
                               FType definer,
                               List<FType> args,
                               GenericMethod generator) {
        super(within, evaluationEnv, fndef, definer, args);
        //this.evaluationEnv = evaluationEnv;
        this.generator = generator;
    }

    public GenericMethod getGenerator() {
        return generator;
    }

    // I think this should not be called; the com.sun.fortress.interpreter.evaluator is supposed to short-circuit
    // around this to generate a specific, corrected instance of a generic method.
    public FValue applyMethod(FObject selfValue, List<FValue> args) {
        args = conditionallyUnwrapTupledArgs(args);
        // TraitMethods do not get their environment from the object.
        Evaluator eval = new Evaluator(buildEnvFromEnvAndParams(evaluationEnv, args));
        eval.e.putValue(selfName(), selfValue);
        return eval.eval(getBody());
    }

}
