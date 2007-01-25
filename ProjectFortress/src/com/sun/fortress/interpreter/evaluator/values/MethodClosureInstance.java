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
import com.sun.fortress.interpreter.nodes.Applicable;
import com.sun.fortress.interpreter.useful.HasAt;


public class MethodClosureInstance extends MethodClosure  implements MethodInstance {

    GenericMethod generator;
    BetterEnv genericEnv;

    public MethodClosureInstance(BetterEnv within, BetterEnv genericEnv, Applicable fndef, String self_name, List<FType> args, GenericMethod generator) {
        super(within, fndef, self_name, args);
        this.generator = generator;
        this.genericEnv = genericEnv;
   }

    public BetterEnv getEvalEnv() {
        return genericEnv;
    }

    public FValue applyMethod(List<FValue> args, FObject selfValue, HasAt loc) {
        args = conditionallyUnwrapTupledArgs(args);
        // This is a little over-tricky.  In theory, all instances of objectExpr from the same
        // "place" are environment-free, and instead they snag their environments from self.
        // This might be wrong; what about the case where the surrounding environment is the
        // instantiation of some generic?  It seems like signatures etc will depend on this.
        Evaluator eval = new Evaluator(buildEnvFromEnvAndParams(selfValue.getLexicalEnv().genericLeafEnvHack(genericEnv, loc), args, loc));
        eval.e.putValue(selfName(), selfValue);
         return eval.eval(getBody());
     }

     public GenericMethod getGenerator() {
        return generator;
    }

}
