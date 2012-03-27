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
import com.sun.fortress.useful.Hasher;
import com.sun.fortress.useful.Useful;

import java.util.List;

/**
 * Trait methods are only partially defined.
 */
public class TraitMethod extends MethodClosure {

    protected Environment evaluationEnv;

    public Environment getEvalEnv() {
        return evaluationEnv;
    }

    public String toString() {
        return (instArgs == null ? s(def) : (s(def) + Useful.listInOxfords(instArgs))) + def.at();
    }

    public TraitMethod(Environment within, Environment evaluationEnv, Applicable fndef, FType definer) {
        super(within, fndef, definer); // TODO verify that this is the proper environment
        if (!evaluationEnv.getBlessed()) System.err.println("urp!");
        this.evaluationEnv = evaluationEnv;
    }

    protected TraitMethod(Environment within,
                          Environment evaluationEnv,
                          Applicable fndef,
                          FType definer,
                          List<FType> args) {
        super(within, fndef, definer, args);
        this.evaluationEnv = evaluationEnv;
        if (!evaluationEnv.getBlessed()) System.err.println("urp!");

    }

    @Override
    protected Environment envForApplication(FObject selfValue) {
        return getEvalEnv();
    }

    public static Hasher<TraitMethod> signatureEquivalence = new Hasher<TraitMethod>() {

        @Override
        public long hash(TraitMethod x) {
            return Simple_fcn.signatureEquivalence.hash(x);
        }

        @Override
        public boolean equiv(TraitMethod x, TraitMethod y) {
            return Simple_fcn.signatureEquivalence.equiv(x, y);
        }

    };

    public static Hasher<TraitMethod> nameEquivalence = new Hasher<TraitMethod>() {

        @Override
        public long hash(TraitMethod x) {
            return Simple_fcn.nameEquivalence.hash(x);
        }

        @Override
        public boolean equiv(TraitMethod x, TraitMethod y) {
            return Simple_fcn.nameEquivalence.equiv(x, y);
        }

    };


}
