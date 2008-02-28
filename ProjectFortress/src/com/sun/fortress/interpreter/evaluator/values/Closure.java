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
import edu.rice.cs.plt.tuple.Option;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.EvalType;
import com.sun.fortress.interpreter.evaluator.Evaluator;
import com.sun.fortress.interpreter.evaluator.FortressError;
import com.sun.fortress.interpreter.evaluator.scopes.Scope;
import com.sun.fortress.interpreter.evaluator.types.BottomType;
import com.sun.fortress.interpreter.evaluator.types.FTraitOrObjectOrGeneric;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.FTypeArrow;
import com.sun.fortress.interpreter.evaluator.types.FTypeDynamic;
import com.sun.fortress.interpreter.evaluator.types.FTypeTop;
import com.sun.fortress.interpreter.evaluator.types.FTypeTrait;
import com.sun.fortress.interpreter.evaluator.types.FTypeTuple;
import com.sun.fortress.interpreter.glue.NativeApp;
import com.sun.fortress.nodes.Applicable;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.FnAbsDeclOrDecl;
import com.sun.fortress.nodes.FnDef;
import com.sun.fortress.nodes.FnExpr;
import com.sun.fortress.nodes.Modifier;
import com.sun.fortress.nodes.ModifierOverride;
import com.sun.fortress.nodes.SimpleName;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.NI;
import com.sun.fortress.useful.Useful;

import static com.sun.fortress.interpreter.evaluator.ProgramError.errorMsg;
import static com.sun.fortress.interpreter.evaluator.ProgramError.error;

/**
 * A Closure value is a function, plus some environment information.
 */
public class Closure extends NonPrimitive implements Scope, HasFinishInitializing {
    
    protected FType returnType;
    protected List<FType> instArgs;
    protected Applicable def;

    @Override
    public boolean isOverride() {
        if (def instanceof FnAbsDeclOrDecl) {
            List<Modifier> mods = ((FnAbsDeclOrDecl) def).getMods();
            for (Modifier mod : mods)
                if (mod instanceof ModifierOverride)
                    return true;
        }
        return false;
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.values.Fcn#getFnName()
     */
    @Override
    public SimpleName getFnName() {
        return def.getName();
    }

    protected HasAt getAt() {
        return def;
    }
    
    public String stringName() {
        return def.stringName();
    }

    public boolean hasSelfDotMethodInvocation() {
        return false;
    }

    public String selfName() {
        return NI.na();
    }

    public String toString() {
        String name = getFnName().toString();

        return ((instArgs == null ? s(def) :
            (s(def) + Useful.listInOxfords(instArgs))) + " " +
            (type() != null ? type() : "NULL")) + def.at();
    }

    public Closure(BetterEnv e, Applicable fndef) {
        super(e); // TODO verify that this is the proper environment
        def = NativeApp.checkAndLoadNative(fndef);
    }

    protected Closure(BetterEnv e, Applicable fndef, List<FType> args) {
        super(e);
        def = NativeApp.checkAndLoadNative(fndef);
        instArgs = args;
    }
    
    public int hashCode() {
        return def.hashCode() +
        System.identityHashCode(getEnv()) +
        (instArgs == null ? 0 : instArgs.hashCode());
    }
    
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o.getClass().equals(this.getClass())) {
            Closure oc = (Closure) o;
            return def == oc.def &&
            getEnv() == oc.getEnv() &&
            (instArgs == null ? (oc.instArgs == null) :
                oc.instArgs == null ? false : instArgs.equals(oc.instArgs));
        }
        return false;
    }

    /*
     * Just like the PartiallyDefinedMethod, but used a specific environemnt
     */
    public Closure(PartiallyDefinedMethod method, BetterEnv environment) {
        super(environment);
        def = NativeApp.checkAndLoadNative(method.def);
        instArgs = method.instArgs;
        setParamsAndReturnType(method.getParameters(), method.returnType);
    }

//    public Closure(BetterEnv e, FnExpr x, Option<Type> return_type,
//            List<Param> params) {
//        super(e);
//        def = NativeApp.checkAndLoadNative(x);
//        EvalType et = new EvalType(e);
//        setParamsAndReturnType(
//                et.paramsToParameters(e, params),
//                return_type.isPresent() ? et.evalType(return_type.getVal()) : BottomType.ONLY
//                );
//    }

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
        Option<Expr> optBody = NodeUtil.getBody(def);
        return Option.unwrap(optBody);
    }

    public Expr getBodyNull() {
        Option<Expr> optBody = NodeUtil.getBody(def);
        return Option.unwrap(optBody, (Expr) null);
    }


    public FValue applyInner(List<FValue> args, HasAt loc,
                             BetterEnv envForInference) {
        if (def instanceof NativeApp) {
            args = typecheckParams(args,loc);
            try {
                return ((NativeApp)def).applyToArgs(args);
            } catch (FortressError ex) {
                /* Wrap all other errors, but not these. */
                throw ex.setWhere(loc);
            } catch (RuntimeException ex) {
                return error(loc, errorMsg("Wrapped exception ", ex.toString()), ex);
            } catch (Error ex) {
                return error(loc, errorMsg("Wrapped error ", ex.toString()), ex);
            }
        } else {
            Evaluator eval = new Evaluator(buildEnvFromParams(args, loc));
            return eval.eval(getBody());
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
        setFtype(FTypeArrow.make(getDomain(), returnType));
    }

    public void finishInitializing() {
        // This needs to be done right with a generic.
        Applicable x = getDef();
        List<Param> params = x.getParams();
        Option<Type> rt = x.getReturnType();
        BetterEnv env = getEvalEnv(); // should need this for types,
                                    // below.
        FType ft = EvalType.getFTypeFromOption(rt, env);
        if (ft instanceof FTypeDynamic)
            ft = BottomType.ONLY;
        
        List<Parameter> fparams = EvalType.paramsToParameters(env, params);

        setParamsAndReturnType(fparams, ft);
        
        return; //  this;
    }

    @Override
    boolean getFinished() {
       return returnType != null;
    }


}
