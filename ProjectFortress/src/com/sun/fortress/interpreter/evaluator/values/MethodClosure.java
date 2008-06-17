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

import static com.sun.fortress.exceptions.InterpreterBug.bug;
import static com.sun.fortress.exceptions.ProgramError.errorMsg;

import java.util.List;

import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.Evaluator;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.glue.WellKnownNames;
import com.sun.fortress.nodes.Applicable;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.Hasher;
import com.sun.fortress.useful.Useful;

public class MethodClosure extends Closure implements Method {

    final int selfParameterIndex;

    final FType definer;

    public MethodClosure(Environment within, Applicable fndef, FType definer) {
        super(within, fndef);
        this.definer = definer;
        selfParameterIndex = NodeUtil.selfParameterIndex(getDef());

    }

    public MethodClosure(Environment within, Applicable fndef, FType definer, List<FType> args) {
        super(within, fndef, args);
        this.definer = definer;
        selfParameterIndex = NodeUtil.selfParameterIndex(getDef());

        // TODO this is really not figured out yet.
    }

    /**
     * Method values have this filtering applied to them.
     * This is necessary to create proper fm$whatever methods
     * to be called by the functional method wrapper.
     */
    protected List<Parameter> adjustParameterList(List<Parameter> params2) {
        return selfParameterIndex == -1 ? params2 :  Useful.removeIndex(selfParameterIndex, params2);
    }

    // The choice of evaluation environment is the only difference between applying
    // a MethodClosure and applying its subclass, a PartiallyDefinedMethod (which
    // appears to actually represent some piece of a functional method in practice).
    protected Environment envForApplication(FObject selfValue, HasAt loc) {
        return selfValue.getLexicalEnv();
    }

    public FValue applyMethod(List<FValue> args, FObject selfValue,
                              HasAt loc, Environment envForInference) {
        args = conditionallyUnwrapTupledArgs(args);
        Expr body = getBodyNull();

        if (body != null) {
            // This is a little over-tricky. In theory, all instances of
            // objectExpr
            // from the same
            // "place" are environment-free, and instead they snag their
            // environments from self.
            // This might be wrong; what about the case where the surrounding
            // environment is the
            // instantiation of some generic? It seems like signatures etc will
            // depend on this.
            Evaluator eval =
                new Evaluator(buildEnvFromEnvAndParams(envForApplication(selfValue,loc),
                                                       args, loc));
            // selfName() was rewritten to our special "$self", and
            // we don't care about shadowing here.
            eval.e.putValueUnconditionally(selfName(), selfValue);
            return eval.eval(body);
        } else if (def instanceof Method) {
            return ((Method)def).applyMethod(args, selfValue, loc, envForInference);
        } else {
            return bug(loc,errorMsg("MethodClosure ",this,
                                    " has neither body nor def instanceof Method"));

        }
    }

    /* A MethodClosure should be invoked via applyInner iff:
     *   The corresponding FunctionalMethod closure is an overloading at top level.
     *   We're obtaining the MethodClosure from the overloading table,
     *      where it was cached during a previous call.
     *   In that case we can strip "AsIf" information from self, as
     *      we've already dealt with the type information.
     */
    public FValue applyInner(List<FValue> args, HasAt loc,
                             Environment envForInference) {
        if (selfParameterIndex == -1) {
            return bug(loc,errorMsg("MethodClosure for dotted method ",this,
                                    " was invoked as if it were a functional method."));
        }
        // We're a functional method instance, so fish out self and
        // chain to applyMethod.
        FObject self = (FObject)args.get(selfParameterIndex).getValue();
        args = Useful.removeIndex(selfParameterIndex,args);
        return applyMethod(args,self,loc,envForInference);
    }

    public String selfName() {
        return WellKnownNames.secretSelfName;
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

    public FType getDefiner() {
        return definer;
    }

}
