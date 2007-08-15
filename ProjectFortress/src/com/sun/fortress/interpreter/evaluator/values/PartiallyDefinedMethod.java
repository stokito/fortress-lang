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
import com.sun.fortress.useful.Hasher;
import com.sun.fortress.useful.Useful;


/**
 * Trait methods are only partially defined.
 */
public class PartiallyDefinedMethod extends MethodClosure {

    protected BetterEnv evaluationEnv;

    public BetterEnv getEvalEnv() {
        return evaluationEnv;
    }

//    public MethodClosure completeClosure(BetterEnv com.sun.fortress.interpreter.env) {
//        return this;
//        //return new TraitMethod(this, evaluationEnv, selfName());
//    }

    public boolean isMethod() {
        return false;
    }

    public String toString() {
        return (instArgs == null ?
                s(def) : (s(def) + Useful.listInOxfords(instArgs))) + def.at();
    }

    public PartiallyDefinedMethod(BetterEnv within, BetterEnv evaluationEnv, Applicable fndef) {
        super(within, fndef); // TODO verify that this is the proper environment
        if (!evaluationEnv.getBlessed())
            System.err.println("urp!");
        this.evaluationEnv = evaluationEnv;
     }

    protected PartiallyDefinedMethod(BetterEnv within, BetterEnv evaluationEnv, Applicable fndef, List<FType> args) {
        super(within, fndef, args);
        this.evaluationEnv = evaluationEnv;
        if (!evaluationEnv.getBlessed())
            System.err.println("urp!");

    }

    public FValue applyInner(List<FValue> args, HasAt loc, BetterEnv envForInference) {
        return super.applyInner(args, loc, envForInference);
    }

    @Override
    public FValue applyMethod(List<FValue> args0, FObject selfValue, HasAt loc, BetterEnv envForInference) {
        List<FValue> args = conditionallyUnwrapTupledArgs(args0);
        // TraitMethods do not get their environment from the object.
        Evaluator eval = new Evaluator(buildEnvFromEnvAndParams(evaluationEnv, args, loc));
        eval.e.putValue(selfName(), selfValue);
        return eval.eval(getBody());
     }



    public static Hasher<PartiallyDefinedMethod> signatureEquivalence = new Hasher<PartiallyDefinedMethod>() {

        @Override
        public long hash(PartiallyDefinedMethod x) {
            return Simple_fcn.signatureEquivalence.hash(x);
        }

        @Override
        public boolean equiv(PartiallyDefinedMethod x, PartiallyDefinedMethod y) {
            return Simple_fcn.signatureEquivalence.equiv(x,y);
        }

    };

    public static Hasher<PartiallyDefinedMethod> nameEquivalence = new Hasher<PartiallyDefinedMethod>() {

        @Override
        public long hash(PartiallyDefinedMethod x) {
            return Simple_fcn.nameEquivalence.hash(x);
        }

        @Override
        public boolean equiv(PartiallyDefinedMethod x, PartiallyDefinedMethod y) {
            return Simple_fcn.nameEquivalence.equiv(x,y);
        }

    };


}
