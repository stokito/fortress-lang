/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.values;

import com.sun.fortress.compiler.WellKnownNames;
import static com.sun.fortress.exceptions.InterpreterBug.bug;
import static com.sun.fortress.exceptions.ProgramError.errorMsg;
import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.Evaluator;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.nodes.Applicable;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.useful.Hasher;
import com.sun.fortress.useful.Useful;

import java.util.Collections;
import java.util.List;

public class MethodClosure extends FunctionClosure implements Method {

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
        return selfParameterIndex == -1 ? params2 : Useful.removeIndex(selfParameterIndex, params2);
    }

    // The choice of evaluation environment is the only difference between applying
    // a MethodClosure and applying its subclass, a PartiallyDefinedMethod (which
    // appears to actually represent some piece of a functional method in practice).
    protected Environment envForApplication(FObject selfValue) {
        return selfValue.getLexicalEnv();
    }

    public FValue applyMethod(FObject selfValue, List<FValue> args) {
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
            Evaluator eval = new Evaluator(buildEnvFromEnvAndParams(envForApplication(selfValue), args));
            // selfName() was rewritten to our special "$self", and
            // we don't care about shadowing here.
            eval.e.putValueRaw(selfName(), selfValue);
            return eval.eval(body);
        } else if (def instanceof Method) {
            return ((Method) def).applyMethod(selfValue, args);
        } else {
            return bug(errorMsg("MethodClosure ", this, " has neither body nor def instanceof Method"));

        }
    }

    public FValue applyMethod(FObject self) {
        return applyMethod(self, Collections.<FValue>emptyList());
    }

    // Remaining applyMethod work like a functional method invocation
    // if applicable, otherwise they work like a method invocation with self first.

    public FValue applyMethod(FObject self, FValue a) {
        return applyMethod(self, Collections.singletonList(a));
    }

    public FValue applyMethod(FObject self, FValue a, FValue b) {
        return applyMethod(self, Useful.list(a, b));
    }

    public FValue applyMethod(FObject self, FValue a, FValue b, FValue c) {
        return applyMethod(self, Useful.list(a, b, c));
    }

    /* A MethodClosure should be invoked via applyInnerPossiblyGeneric iff:
     *   The corresponding FunctionalMethod closure is an overloading at top level.
     *   We're obtaining the MethodClosure from the overloading table,
     *      where it was cached during a previous call.
     *   In that case we can strip "AsIf" information from self, as
     *      we've already dealt with the type information.
     */
    public FValue applyInnerPossiblyGeneric(List<FValue> args) {
        if (selfParameterIndex == -1) {
            return bug(errorMsg("MethodClosure for dotted method ",
                                this,
                                " was invoked as if it were a functional method."));
        }
        // We're a functional method instance, so fish out self and
        // chain to applyMethod.
        FObject self = (FObject) args.get(selfParameterIndex).getValue();
        args = Useful.removeIndex(selfParameterIndex, args);
        return applyMethod(self, args);
    }

    public FValue applyToArgs() {
        return bug(errorMsg("No recipient object for method ", this));
    }

    public FValue applyToArgs(FValue a) {
        return applyToArgs(toSelf(a), Collections.<FValue>emptyList());
    }

    // Remaining applyToArgs work like a functional method invocation
    // if applicable, otherwise they work like a method invocation with self first.

    public FValue applyToArgs(FValue a, FValue b) {
        if (selfParameterIndex <= 0) {
            return applyToArgs(toSelf(a), Collections.singletonList(b));
        }
        return applyToArgs(toSelf(b), Collections.singletonList(a));
    }

    public FValue applyToArgs(FValue a, FValue b, FValue c) {
        if (selfParameterIndex <= 0) {
            return applyToArgs(toSelf(a), Useful.list(b, c));
        } else if (selfParameterIndex == 1) {
            return applyToArgs(toSelf(b), Useful.list(a, c));
        } else {
            return applyToArgs(toSelf(c), Useful.list(a, b));
        }
    }

    public FValue applyToArgs(FValue a, FValue b, FValue c, FValue d) {
        if (selfParameterIndex <= 0) {
            return applyToArgs(toSelf(a), Useful.list(b, c, d));
        } else if (selfParameterIndex == 1) {
            return applyToArgs(toSelf(b), Useful.list(a, c, d));
        } else if (selfParameterIndex == 2) {
            return applyToArgs(toSelf(c), Useful.list(a, b, d));
        } else {
            return applyToArgs(toSelf(d), Useful.list(a, b, c));
        }
    }

    protected FObject toSelf(FValue a) {
        if (a instanceof FObject) return (FObject) a;
        return bug(errorMsg("Non-object recipient ", a, " for method ", this));
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
            return Simple_fcn.signatureEquivalence.equiv(x, y);
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
