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
import com.sun.fortress.interpreter.evaluator.EvalType;
import com.sun.fortress.interpreter.evaluator.Evaluator;
import com.sun.fortress.interpreter.evaluator.ProgramError;
import com.sun.fortress.interpreter.evaluator.scopes.Scope;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.FTypeArrow;
import com.sun.fortress.interpreter.evaluator.types.FTypeTuple;
import com.sun.fortress.interpreter.glue.NativeApp;
import com.sun.fortress.interpreter.nodes.Applicable;
import com.sun.fortress.interpreter.nodes.Expr;
import com.sun.fortress.interpreter.nodes.FnName;
import com.sun.fortress.interpreter.nodes.Option;
import com.sun.fortress.interpreter.nodes.Param;
import com.sun.fortress.interpreter.nodes.TypeRef;
import com.sun.fortress.interpreter.useful.HasAt;
import com.sun.fortress.interpreter.useful.NI;
import com.sun.fortress.interpreter.useful.Useful;


/**
 * A Closure value is a function, plus some environment information.
 */
public class Closure extends NonPrimitive implements Scope {
    protected FType returnType;
    protected List<FType> instArgs;
    protected Applicable def;

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.values.Fcn#getFnName()
     */
    @Override
    public FnName getFnName() {
        return def.getFnName();
    }

    protected HasAt getAt() {
        return def;
    }

    public boolean hasSelfDotMethodInvocation() {
        return false;
    }

    public String selfName() {
        return NI.na();
    }

    public String toString() {
        return instArgs == null ? String.valueOf(def) : (def + Useful.listInOxfords(instArgs));
    }

    public Closure(BetterEnv e, Applicable fndef) {
        super(e); // TODO verify that this is the proper environment
        def = NativeApp.checkAndLoadNative(fndef);
    }

    public Closure(BetterEnv e, Applicable fndef, List<FType> args) {
        super(e);
        def = NativeApp.checkAndLoadNative(fndef);
        instArgs = args;
    }

    /*
     * Just like the PartiallyDefinedMethod, but used a specific environemnt
     */
    public Closure(PartiallyDefinedMethod method, BetterEnv environment) {
        super(environment);
        def = NativeApp.checkAndLoadNative(method.def);
        instArgs = method.instArgs;
        setParamsAndReturnType(method.getParams(), method.returnType);
    }

    private void setReturnType(FType rt) {
        // TODO need to get this test right
        if (this.returnType != null && !this.returnType.equals(rt)) {
            throw new IllegalStateException(
                    "Attempted second set of closure return type");
        }
        returnType = rt;
    }


    public Applicable getDef() {
        return def;
    }

    /**
     * @return Returns the closure_body.
     */
    public Expr getBody() {
        return getDef().getBody();
    }

    public FValue applyInner(List<FValue> args, HasAt loc,
                             BetterEnv envForInference) {
        if (def instanceof NativeApp) {
            typecheckParams(args,loc);
            try {
                return ((NativeApp)def).applyToArgs(args);
            } catch (ProgramError ex) {
                /* Wrap all other errors, but not these. */
                throw ex.setWhere(loc);
            } catch (RuntimeException ex) {
                throw new ProgramError(loc, "Wrapped exception", ex);
            } catch (Error ex) {
                throw new ProgramError(loc, "Wrapped error", ex);
            }
        } else {
            Evaluator eval = new Evaluator(buildEnvFromParams(args, loc));
            return getBody().accept(eval);
        }
    }

    /**
     * The environment, sort of, in which the closure's name is bound.
     */
    public BetterEnv getEnv() {
        return getWithin();
    }

    /**
     * The environment used to evaluate the closure.
     * @return
     */
    public BetterEnv getEvalEnv() {
        return getWithin();
    }

    /**
     * Call this for Closures, not setParams.
     * @param fparams
     * @param ft
     */
    public void setParamsAndReturnType(List<Parameter> fparams, FType ft) {
        setReturnType(ft);
        setParams(fparams);
    }

    protected void setValueType() {
        setFtype(FTypeArrow.make(FTypeTuple.make(getDomain()), returnType));
    }

    /**
     * @param cl
     */
    public Closure finishInitializing() {
        // This needs to be done right with a generic.
        Applicable x = getDef();
        List<Param> params = x.getParams();
        Option<TypeRef> rt = x.getReturnType();
        BetterEnv env = getEvalEnv(); // should need this for types,
                                    // below.
        FType ft = EvalType.getFTypeFromOption(rt, env);
        List<Parameter> fparams = EvalType.paramsToParameters(env, params);

        setParamsAndReturnType(fparams, ft);
        return this;
    }

    @Override
    boolean getFinished() {
       return returnType != null;
    }


}
