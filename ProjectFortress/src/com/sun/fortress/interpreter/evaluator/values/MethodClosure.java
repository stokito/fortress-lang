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
import com.sun.fortress.interpreter.nodes_util.NodeUtil;
import com.sun.fortress.interpreter.useful.HasAt;
import com.sun.fortress.interpreter.useful.Hasher;
import com.sun.fortress.interpreter.useful.Useful;


public class MethodClosure extends Closure implements Method {

    public MethodClosure(BetterEnv within, Applicable fndef, String self_name) {
        super(within, fndef);
        self = self_name;

    }

    public MethodClosure(BetterEnv within, Applicable fndef, String self_name, List<FType> args) {
        super(within, fndef, args);
        self = self_name;
        // TODO this is really not figured out yet.
    }

    /**
     * Used by TraitMethod to capture the trait environment.
     * @param method
     * @param environment
     * @param self_name
     */
    protected MethodClosure(PartiallyDefinedMethod method, BetterEnv environment, String self_name) {
        super(method, environment);
        self = self_name;
    }

    String self;

        /**
         * Method values have this filtering applied to them.
         * This is necessary to create proper fm$whatever methods
         * to be called by the functional method wrapper.
         */
    protected List<Parameter> adjustParameterList(List<Parameter> params2) {
        int i = NodeUtil.selfParameterIndex(getDef());
        if (i == -1)
            return params2;
        return  Useful.removeIndex(i, params2);
    }

    public FValue applyMethod(List<FValue> args, FObject selfValue, HasAt loc, BetterEnv envForInference) {
        args = conditionallyUnwrapTupledArgs(args);
        // This is a little over-tricky.  In theory, all instances of objectExpr from the same
        // "place" are environment-free, and instead they snag their environments from self.
        // This might be wrong; what about the case where the surrounding environment is the
        // instantiation of some generic?  It seems like signatures etc will depend on this.
        Evaluator eval = new Evaluator(buildEnvFromEnvAndParams(
                selfValue.getLexicalEnv(),
                args, loc));
        // selfName() was rewritten to our special "$self", and
        // we don't care about shadowing here.
        eval.e.putValueUnconditionally(selfName(), selfValue);
        return eval.eval(getBody());
     }
    public boolean isMethod() {
        return true;
    }

    public String selfName() {
        return self;
    }

    public static Hasher<MethodClosure> signatureEquivalence = new Hasher<MethodClosure>() {

        @Override
        public long hash(MethodClosure x) {
            return Simple_fcn.signatureEquivalence.hash(x);
        }

        @Override
        public boolean equiv(MethodClosure x, MethodClosure y) {
            return Simple_fcn.signatureEquivalence.equiv(x,y);
        }

    };

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.values.Fcn#asMethodName()
     */
    @Override
    public String asMethodName() {
        // TODO Auto-generated method stub
        return NodeUtil.nameAsMethod(getDef());
    }


}
