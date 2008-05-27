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

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.Evaluator;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.nodes.Applicable;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.Hasher;
import com.sun.fortress.useful.Useful;

import static com.sun.fortress.interpreter.evaluator.ProgramError.errorMsg;
import static com.sun.fortress.interpreter.evaluator.InterpreterBug.bug;

/**
 * Trait methods are only partially defined.
 */
public class TraitMethod extends MethodClosure {

    protected BetterEnv evaluationEnv;

    public BetterEnv getEvalEnv() {
        return evaluationEnv;
    }

    public String toString() {
        return (instArgs == null ?
                s(def) : (s(def) + Useful.listInOxfords(instArgs))) + def.at();
    }

    public TraitMethod(BetterEnv within, BetterEnv evaluationEnv, Applicable fndef, FType definer) {
        super(within, fndef, definer); // TODO verify that this is the proper environment
        if (!evaluationEnv.getBlessed())
            System.err.println("urp!");
        this.evaluationEnv = evaluationEnv;
     }

    protected TraitMethod(BetterEnv within, BetterEnv evaluationEnv, Applicable fndef, FType definer, List<FType> args) {
        super(within, fndef, definer, args);
        this.evaluationEnv = evaluationEnv;
        if (!evaluationEnv.getBlessed())
            System.err.println("urp!");

    }

    @Override
    protected BetterEnv envForApplication(FObject selfValue, HasAt loc) {
        return getEvalEnv();
    }

    public static Hasher<TraitMethod> signatureEquivalence = new Hasher<TraitMethod>() {

        @Override
        public long hash(TraitMethod x) {
            return Simple_fcn.signatureEquivalence.hash(x);
        }

        @Override
        public boolean equiv(TraitMethod x, TraitMethod y) {
            return Simple_fcn.signatureEquivalence.equiv(x,y);
        }

    };

    public static Hasher<TraitMethod> nameEquivalence = new Hasher<TraitMethod>() {

        @Override
        public long hash(TraitMethod x) {
            return Simple_fcn.nameEquivalence.hash(x);
        }

        @Override
        public boolean equiv(TraitMethod x, TraitMethod y) {
            return Simple_fcn.nameEquivalence.equiv(x,y);
        }

    };


}
