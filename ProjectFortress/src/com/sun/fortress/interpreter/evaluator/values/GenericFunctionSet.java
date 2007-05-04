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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.EvalType;
import com.sun.fortress.interpreter.evaluator.Evaluator;
import com.sun.fortress.interpreter.evaluator.ProgramError;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.nodes.Applicable;
import com.sun.fortress.interpreter.nodes.FnName;
import com.sun.fortress.interpreter.nodes.StaticArg;
import com.sun.fortress.interpreter.useful.BATreeEC;
import com.sun.fortress.interpreter.useful.HasAt;
import com.sun.fortress.interpreter.useful.Useful;


public class GenericFunctionSet extends
        GenericFunctionOrMethodSet<FGenericFunction> {

    // public GenericFunctionSet(FnName name, BetterEnv within,
    //         Set<FGenericFunction> gs) {
    //     super(name, within, gs);
    // }

    public GenericFunctionSet(FnName name, BetterEnv within) {
        super(name, within, FGenericFunction.genFullComparer);
    }

    OverloadedFunction oaf = null;
    BATreeEC<List<FValue>, List<FType>, SingleFcn> cache =
        new BATreeEC<List<FValue>, List<FType>, SingleFcn>(FValue.asTypesList);

    public OverloadedFunction getOverloadSymbolicFunction() {
        return oaf;
    }

    public boolean isMethod() {
        return false;
    }

    // Derive some symbolic parameters to the generic, use those to
    // instantiate functions and perform a symbolic overloading.
    public void finishInitializing() {
        // If we don't do the following check, things are O(n^2) for n
        // overloadings.
        if (this.oaf == null) {
            Set<FGenericFunction> fns = getMethods();
            Applicable app = getAnApplicable();
            List<FType> iTypes =
                SingleFcn.createSymbolicInstantiation(getWithin(), app, app);
            this.oaf = instantiateOverloaded(fns, app.getFnName(), app, iTypes);
        }
    }

    @Override
    public GenericFunctionOrMethodSet addOverload(FGenericFunction cl) {
        // Invalidate oaf to handle the case in which we're adding new
        // overloadings to an imported function.  This forces oaf to
        // be reconstructed from the new method set.
        this.oaf = null;
        super.addOverload(cl);
        return this;
    }

    public Applicable getAnApplicable() {
        return getMethods().iterator().next().getDef();
    }

    /**
     * @param fns
     * @param app
     * @param iTypes
     */
    private OverloadedFunction instantiateOverloaded(Set<FGenericFunction> fns,
            FnName fn_name, HasAt at, List<FType> iTypes) {
        // Given instantiation types, create an overloaded function
        OverloadedFunction oaf = new OverloadedFunction(fn_name, getWithin());
        for (FGenericFunction gf : fns) {
            Simple_fcn sfcn = gf.make(iTypes, at);
            oaf.addOverload(sfcn);
        }
        oaf.finishInitializingSecondPart();
        return oaf;
    }

    public FValue typeApply(List<StaticArg> args, BetterEnv e, HasAt x) {
        Set<FGenericFunction> fns = getMethods();
        EvalType et = new EvalType(e);
        ArrayList<FType> argValues = et.forStaticArgList(args);

        OverloadedFunction oaf = new OverloadedFunction(getAnApplicable()
                .getFnName(), getWithin());
        for (FGenericFunction gf : fns) {
            Simple_fcn sfcn = gf.typeApply(e, x, argValues);
            oaf.addOverload(sfcn);
        }
        oaf.finishInitializingSecondPart();
        return oaf;
    }

    public FValue applyInner(List<FValue> args, HasAt loc,
            BetterEnv envForInference) {
        
        SingleFcn best = cache.get(args);
        
        if (best == null) {
            best = inferAnInstance(args, loc, envForInference);
            cache.syncPut(args, best);
        }
        return best.applyInner(args, loc, envForInference);

    }

    /**
     * @param args
     * @param loc
     * @param envForInference
     * @return
     * @throws ProgramError
     */
    private SingleFcn inferAnInstance(List<FValue> args, HasAt loc, BetterEnv envForInference) throws ProgramError {
        final boolean TRACE = false;
            // I've inserted this code twice to find bugs - Jan
        OverloadedFunction oaf = getOverloadSymbolicFunction();
        List<Overload> ols = oaf.getOverloads();
        SingleFcn best = null;
        List<FType> best_params = null;
        Evaluator ev = new Evaluator(envForInference);
        if (TRACE) System.err.println("applyInner "+this);
        for (Overload o : ols) {
            SingleFcn f = o.getFn();
            if (f instanceof ClosureInstance) {
                ClosureInstance ci = (ClosureInstance) f;
                FGenericFunction gf = ci.getGenerator();
                try {
                    f = ev.inferAndInstantiateGenericFunction(args, gf, loc);
                } catch (ProgramError ex) {
                    if (TRACE) System.err.println("Rejecting "+f);
                    if (TRACE) ex.printStackTrace();
                    /* When unification fails, we assume the instance
                     * can't possibly match.  There really ought to be
                     * a distinct UnificationError so that we can
                     * distinguish this from a mere ProgramError. */
                    continue;
                }
            }
            if (TRACE) System.err.println("Considering "+f);
            List<FType> candidate_params = f.getDomain();
            if (!f.argCountIsWrong(args)
                    && OverloadedFunction
                            .argsMatchTypes(args, candidate_params)) {
                if (best == null) {
                    if (TRACE) System.err.println("First match");
                    best = f;
                    best_params = candidate_params;
                } else {
                    if (FType.moreSpecificThan(candidate_params, best_params)) {
                        if (TRACE) System.err.println("More specific type");
                        best = f;
                        best_params = candidate_params;
                    } else if (TRACE) System.err.println("Less specific.");
                }
            }

        }
        if (best==null) {
            throw new ProgramError(loc, within,
                                   "Failed to find matching overload, " +
                                   "args = " + Useful.listInParens(args) +
                                   ", overload = " + this);
        }
        return best;
    }
}
