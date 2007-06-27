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
import java.util.Comparator;
import java.util.List;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.EvalType;
import com.sun.fortress.interpreter.evaluator.InterpreterError;
import com.sun.fortress.interpreter.evaluator.ProgramError;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.values.FGenericFunction.GenericFullComparer;
import com.sun.fortress.interpreter.nodes.Applicable;
import com.sun.fortress.interpreter.nodes.DimensionParam;
import com.sun.fortress.interpreter.nodes.FnDefOrDecl;
import com.sun.fortress.interpreter.nodes.FnName;
import com.sun.fortress.interpreter.nodes.NatParam;
import com.sun.fortress.interpreter.nodes.OperatorParam;
import com.sun.fortress.interpreter.useful.Option;
import com.sun.fortress.interpreter.nodes.Param;
import com.sun.fortress.interpreter.nodes.SimpleTypeParam;
import com.sun.fortress.interpreter.nodes.StaticArg;
import com.sun.fortress.interpreter.nodes.StaticParam;
import com.sun.fortress.interpreter.nodes.TypeRef;
import com.sun.fortress.interpreter.useful.Factory1P;
import com.sun.fortress.interpreter.useful.HasAt;
import com.sun.fortress.interpreter.useful.Memo1P;


public class GenericMethod extends MethodClosure implements
        GenericFunctionOrMethod, Factory1P<List<FType>, MethodClosure, HasAt> {

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.values.FValue#getString()
     */
    @Override
    public String getString() {
        return getDef().toString();
    }

    boolean isTraitMethod;

    BetterEnv evaluationEnv;

    protected MethodClosure newClosure(BetterEnv clenv, List<FType> args) {
        MethodClosure cl;
        if (anyAreSymbolic(args))
            cl = isTraitMethod ? new PartiallyDefinedMethodInstance(getEnv(),
                    clenv, getDef(), selfName(), args, this)
                    : new MethodClosureInstance(getEnv(), clenv, getDef(),
                            selfName(), args, this);
        else {
            // TODO Intention is that this is a plain old instantiation,
            // however there are issues of capturing the evaluation
            // environment that makes this not work quite right
            // MethodClosureInstance ought to be MethodClosure, but
            // isn't, yet.
            cl = isTraitMethod ? new PartiallyDefinedMethod(getEnv(),
                    clenv, getDef(), selfName(), args)
                    : new MethodClosureInstance(getEnv(), clenv, getDef(),
                            selfName(), args, this);
        }
        return (MethodClosure) cl.finishInitializing();
    }

    private class Factory implements
            Factory1P<List<FType>, MethodClosure, HasAt> {

        public MethodClosure make(List<FType> args, HasAt within) {
            BetterEnv clenv = new BetterEnv(evaluationEnv, within); // TODO is this the right environment?
            // It looks like it might be, or else good enough.  The disambiguating
            // pass effectively hides all the names defined in the interior
            // of the trait.
            List<StaticParam> params = getDef().getStaticParams().getVal();
            EvalType.bindGenericParameters(params, args, clenv, within,
                    getDef());
            clenv.bless();
            return newClosure(clenv, args);
        }
    }

    Memo1P<List<FType>, MethodClosure, HasAt> memo = new Memo1P<List<FType>, MethodClosure, HasAt>(
            new Factory());

    public MethodClosure make(List<FType> l, HasAt within) {
        return memo.make(l, within);
    }

    public GenericMethod(BetterEnv declarationEnv, BetterEnv evaluationEnv,
            FnDefOrDecl fndef, String selfName, boolean isTraitMethod) {
        super(// new SpineEnv(declarationEnv, fndef), // Add an extra scope/layer for the generics.
                declarationEnv, // not yet, it changes overloading semantics.
                fndef, selfName);
        this.isTraitMethod = isTraitMethod;
        this.evaluationEnv = evaluationEnv;
    }

    //    public GenericMethod(Environment declarationEnv, Environment traitEnv, FnDefOrDecl fndef, String selfName) {
    //        super(declarationEnv, fndef, selfName);
    //
    //    }

    public MethodClosure typeApply(List<StaticArg> args, BetterEnv e, HasAt within) {
        List<StaticParam> params = getDef().getStaticParams().getVal();

        // Evaluate each of the args in e, inject into clenv.
        if (args.size() != params.size()) {
            throw new ProgramError(within, e,
                    "Generic instantiation (size) mismatch, expected " + params
                            + " got " + args);
        }
        EvalType et = new EvalType(e);
        // TODO Can combine these two functions if we enhance the memo and factory
        // to pass two parameters instead of one.
        
        ArrayList<FType> argValues = et.forStaticArgList(args);
        return make(argValues, within);
    }

    public Closure finishInitializing() {
        Applicable x = getDef();
        List<Param> params = x.getParams();
        Option<TypeRef> rt = x.getReturnType();
        BetterEnv env = getEnv(); // should need this for types,
        // below.
        // TODO work in progress
        // Inject type parameters into environment as symbolics
        List<StaticParam> tparams = getDef().getStaticParams().getVal();
        for (StaticParam tp : tparams) {
            if (tp instanceof DimensionParam) {
                DimensionParam dp = (DimensionParam) tp;
            } else if (tp instanceof NatParam) {
                NatParam np = (NatParam) tp;
            } else if (tp instanceof OperatorParam) {
                OperatorParam op = (OperatorParam) tp;
            } else if (tp instanceof SimpleTypeParam) {
                SimpleTypeParam stp = (SimpleTypeParam) tp;
            } else {
                throw new InterpreterError("Unexpected StaticParam " + tp);
            }
        }

        FType ft = EvalType.getFTypeFromOption(rt, env);
        List<Parameter> fparams = EvalType.paramsToParameters(env, params);

        setParamsAndReturnType(fparams, ft);
        return this;
    }

    static class GenericComparer implements Comparator<GenericMethod> {

        public int compare(GenericMethod arg0, GenericMethod arg1) {
            Applicable a0 = arg0.getDef();
            Applicable a1 = arg1.getDef();

            FnName fn0 = a0.getFnName();
            FnName fn1 = a1.getFnName();
            int x = fn0.compareTo(fn1);
            if (x != 0)
                return x;

            List<StaticParam> oltp0 = a0.getStaticParams().getVal();
            List<StaticParam> oltp1 = a1.getStaticParams().getVal();

            return StaticParam.listComparer.compare(oltp0, oltp1);

        }

    }

    static final GenericComparer genComparer = new GenericComparer();

    static class GenericFullComparer implements Comparator<GenericMethod> {

        public int compare(GenericMethod arg0, GenericMethod arg1) {
            return arg0.getDef().applicableCompareTo(arg1.getDef());
            
        }
    }
    static final GenericFullComparer genFullComparer = new GenericFullComparer();
    
}
