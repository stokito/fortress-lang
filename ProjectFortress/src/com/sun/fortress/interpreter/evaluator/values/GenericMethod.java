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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import edu.rice.cs.plt.tuple.Option;

import com.sun.fortress.interpreter.evaluator.InterpreterBug;
import com.sun.fortress.interpreter.glue.NativeApp;
import com.sun.fortress.useful.Useful;
import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.EvalType;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.values.FGenericFunction.GenericFullComparer;
import com.sun.fortress.nodes.Applicable;
import com.sun.fortress.nodes.DimensionParam;
import com.sun.fortress.nodes.FnAbsDeclOrDecl;
import com.sun.fortress.nodes.SimpleName;
import com.sun.fortress.nodes.FnExpr;
import com.sun.fortress.nodes.NatParam;
import com.sun.fortress.nodes.OperatorParam;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.SimpleTypeParam;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes_util.NodeComparator;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.useful.Factory1P;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.Memo1P;

import static com.sun.fortress.interpreter.evaluator.ProgramError.errorMsg;
import static com.sun.fortress.interpreter.evaluator.ProgramError.error;
import static com.sun.fortress.interpreter.evaluator.InterpreterBug.bug;

public class GenericMethod extends MethodClosure implements
        GenericFunctionOrMethod, Factory1P<List<FType>, MethodClosure, HasAt> {

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.values.FValue#getString()
     */
    @Override
    public String getString() {
        return s(getDef());
    }

    boolean isTraitMethod;

    BetterEnv evaluationEnv;

    protected MethodClosure newClosure(BetterEnv clenv, List<FType> args) {
        MethodClosure cl;
        if (FType.anyAreSymbolic(args))
            cl = isTraitMethod ? new PartiallyDefinedMethodInstance(getEnv(),
                    clenv, getDef(), args, this)
                    : new MethodClosureInstance(getEnv(), clenv, getDef(),
                           args, this);
        else {
            // TODO Intention is that this is a plain old instantiation,
            // however there are issues of capturing the evaluation
            // environment that makes this not work quite right
            // MethodClosureInstance ought to be MethodClosure, but
            // isn't, yet.
            cl = isTraitMethod ? new PartiallyDefinedMethod(getEnv(),
                    clenv, getDef(), args)
                    : new MethodClosureInstance(getEnv(), clenv, getDef(),
                            args, this);
        }
        cl.finishInitializing();
        return (MethodClosure) cl;
    }

    private class Factory implements
            Factory1P<List<FType>, MethodClosure, HasAt> {

        public MethodClosure make(List<FType> args, HasAt location) {
            BetterEnv clenv = new BetterEnv(evaluationEnv, location); // TODO is this the right environment?
            // It looks like it might be, or else good enough.  The disambiguating
            // pass effectively hides all the names defined in the interior
            // of the trait.
            List<StaticParam> params = getDef().getStaticParams();
            EvalType.bindGenericParameters(params, args, clenv, location,
                    getDef());
            clenv.bless();
            return newClosure(clenv, args);
        }
    }

    Memo1P<List<FType>, MethodClosure, HasAt> memo = new Memo1P<List<FType>, MethodClosure, HasAt>(
            new Factory());

    public MethodClosure make(List<FType> l, HasAt location) {
        return memo.make(l, location);
    }

    public GenericMethod(BetterEnv declarationEnv, BetterEnv evaluationEnv,
            FnAbsDeclOrDecl fndef, boolean isTraitMethod) {
        super(// new SpineEnv(declarationEnv, fndef), // Add an extra scope/layer for the generics.
                declarationEnv, // not yet, it changes overloading semantics.
                fndef);
        this.isTraitMethod = isTraitMethod;
        this.evaluationEnv = evaluationEnv;
    }

    //    public GenericMethod(Environment declarationEnv, Environment traitEnv, FnAbsDeclOrDecl fndef, String selfName) {
    //        super(declarationEnv, fndef, selfName);
    //
    //    }

    public MethodClosure typeApply(List<StaticArg> args, BetterEnv e, HasAt location) {
        List<StaticParam> params = getDef().getStaticParams();

        // Evaluate each of the args in e, inject into clenv.
        if (args.size() != params.size()) {
            error(location, e,
                  "Generic instantiation (size) mismatch, expected "
                  + Useful.listInParens(params)
                  + " got " + Useful.listInParens(args));
        }
        EvalType et = new EvalType(e);
        // TODO Can combine these two functions if we enhance the memo and factory
        // to pass two parameters instead of one.

        ArrayList<FType> argValues = et.forStaticArgList(args);
        return make(argValues, location);
    }
    
    public Simple_fcn typeApply(HasAt location, List<FType> argValues) {
        return make(argValues, location);
    }

    public void finishInitializing() {
        Applicable x = getDef();
        List<Param> params = x.getParams();
        Option<Type> rt = x.getReturnType();
        BetterEnv env = getEnv(); // should need this for types,
        // below.
        // TODO work in progress
        // Inject type parameters into environment as symbolics
        List<StaticParam> tparams = getDef().getStaticParams();
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
                bug(tp, errorMsg("Unexpected StaticParam ", tp));
            }
        }

        FType ft = EvalType.getFTypeFromOption(rt, env);
        List<Parameter> fparams = EvalType.paramsToParameters(env, params);

        setParamsAndReturnType(fparams, ft);
        return;
    }

    static class GenericComparer implements Comparator<GenericMethod> {

        public int compare(GenericMethod arg0, GenericMethod arg1) {
            Applicable a0 = arg0.getDef();
            Applicable a1 = arg1.getDef();

            SimpleName fn0 = a0.getName();
            SimpleName fn1 = a1.getName();
            int x = NodeComparator.compare(fn0, fn1);
            if (x != 0)
                return x;

            List<StaticParam> oltp0 = a0.getStaticParams();
            List<StaticParam> oltp1 = a1.getStaticParams();

            return NodeComparator.compare(oltp0, oltp1);

        }

    }

    static final GenericComparer genComparer = new GenericComparer();

    static class GenericFullComparer implements Comparator<GenericMethod> {

        public int compare(GenericMethod arg0, GenericMethod arg1) {
            return compare(arg0.getDef(), arg1.getDef());
        }

        int compare(Applicable left, Applicable right) {
        if (left instanceof FnExpr) {
            int x = Useful.compareClasses(left, right);
            if (x != 0) return x;
            return NodeUtil.nameString(((FnExpr)left).getName())
                .compareTo(NodeUtil.nameString(((FnExpr)right).getName()));
        } else if (left instanceof FnAbsDeclOrDecl) {
            int x = Useful.compareClasses(left, right);
            if (x != 0) return x;
            return compare(left, (FnAbsDeclOrDecl)right);
        } else if (left instanceof NativeApp) {
            return Useful.compareClasses(left, right);
        } else {
            throw new InterpreterBug(left, "NodeComparator.compare(" +
				     left.getClass() + ", " + right.getClass());
        }
    }

    }
    static final GenericFullComparer genFullComparer = new GenericFullComparer();

    public SimpleName getName() {
        return getDef().getName();
    }

    public List<StaticParam> getStaticParams() {
        return getDef().getStaticParams();
    }

    public List<Param> getParams() {
        return getDef().getParams();
    }

    public Option<Type> getReturnType() {
        return getDef().getReturnType();
    }

    
    
}
