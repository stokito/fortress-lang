/*
 * Created on Apr 23, 2007
 *
 */
package com.sun.fortress.interpreter.evaluator.values;

import java.util.List;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.Evaluator;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.nodes.Applicable;
import com.sun.fortress.interpreter.useful.HasAt;
import com.sun.fortress.interpreter.useful.NI;

public class FunctionalMethod extends Closure {

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.values.Closure#applyInner(java.util.List, com.sun.fortress.interpreter.useful.HasAt, com.sun.fortress.interpreter.env.BetterEnv)
     */
    @Override
    public FValue applyInner(List<FValue> args, HasAt loc, BetterEnv envForInference) {
        // Not quite right
        if (true)
            NI.nyi("Functional method apply");
        Evaluator eval = new Evaluator(buildEnvFromParams(args, loc));
        return eval.eval(getBody());

    }

    public FunctionalMethod(BetterEnv e, Applicable fndef) {
        super(e, fndef);
        // TODO Auto-generated constructor stub
    }

}
