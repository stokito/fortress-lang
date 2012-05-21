/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.values;

import static com.sun.fortress.exceptions.ProgramError.error;
import static com.sun.fortress.exceptions.ProgramError.errorMsg;
import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.EvalType;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.FTypeTop;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeComparator;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.useful.Factory1P;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.Memo1P;
import edu.rice.cs.plt.tuple.Option;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class GenericMethod extends MethodClosure
        implements GenericFunctionOrMethod, Factory1P<List<FType>, MethodClosure, HasAt> {

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.values.FValue#getString()
     */
    @Override
    public String getString() {
        return s(getDef());
    }

    boolean isTraitMethod;

    Environment evaluationEnv;

    protected MethodClosure newClosure(Environment clenv, List<FType> args) {
        MethodClosure cl;
        if (!isTraitMethod) {
            cl = new MethodClosureInstance(getEnv(), clenv, getDef(), getDefiner(), args, this);
        } else if (FType.anyAreSymbolic(args)) {
            cl = new TraitMethodInstance(getEnv(), clenv, getDef(), getDefiner(), args, this);
        } else {
            // TODO Intention is that this is a plain old instantiation,
            // however there are issues of capturing the evaluation
            // environment that makes this not work quite right
            // MethodClosureInstance ought to be MethodClosure, but
            // isn't, yet.
            cl = new TraitMethod(getEnv(), clenv, getDef(), getDefiner(), args);
        }
        cl.finishInitializing();
        return (MethodClosure) cl;
    }

    private class Factory implements Factory1P<List<FType>, MethodClosure, HasAt> {

        public MethodClosure make(List<FType> args, HasAt location) {
            Environment clenv = evaluationEnv.extendAt(location); // TODO is this the right environment?
            // It looks like it might be, or else good enough.  The disambiguating
            // pass effectively hides all the names defined in the interior
            // of the trait.
            List<StaticParam> params = NodeUtil.getStaticParams(getDef());
            EvalType.bindGenericParameters(params, args, clenv, location, getDef());
            clenv.bless();
            return newClosure(clenv, args);
        }
    }

    Memo1P<List<FType>, MethodClosure, HasAt> memo = new Memo1P<List<FType>, MethodClosure, HasAt>(new Factory());

    public MethodClosure make(List<FType> l, HasAt location) {
        return memo.make(l, location);
    }

    public GenericMethod(Environment declarationEnv,
                         Environment evaluationEnv,
                         FnDecl fndef,
                         FType definer,
                         boolean isTraitMethod) {
        super(// new SpineEnv(declarationEnv, fndef), // Add an extra scope/layer for the generics.
              declarationEnv, // not yet, it changes overloading semantics.
              fndef, definer);
        this.isTraitMethod = isTraitMethod;
        this.evaluationEnv = evaluationEnv;
    }

    //    public GenericMethod(Environment declarationEnv, Environment traitEnv, FnDecl fndef, String selfName) {
    //        super(declarationEnv, fndef, selfName);
    //
    //    }

    public MethodClosure typeApply(List<StaticArg> args, Environment e, HasAt location) {
        List<StaticParam> params = NodeUtil.getStaticParams(getDef());

        // Evaluate each of the args in e, inject into clenv.
        if (args.size() != params.size()) {
            error(location, e, errorMsg("Generic instantiation (size) mismatch, expected ", params, " got ", args));
        }
        EvalType et = new EvalType(e);
        // TODO Can combine these two functions if we enhance the memo and factory
        // to pass two parameters instead of one.

        ArrayList<FType> argValues = et.forStaticArgList(args);
        return make(argValues, location);
    }

    public Simple_fcn typeApply(List<FType> argValues, HasAt location) {
        return make(argValues, location);
    }

    public Simple_fcn typeApply(List<FType> argValues) {
        return make(argValues, getDef());
    }

    public void finishInitializing() {
        Applicable x = getDef();
        List<Param> params = NodeUtil.getParams(x);
        Option<Type> rt = NodeUtil.getReturnType(x);
        Environment env = getEnv(); // should need this for types,
        // below.
        // TODO work in progress
        // Inject type parameters into environment as symbolics
        //List<StaticParam> tparams = NodeUtil.getStaticParams(getDef());

        FType ft = EvalType.getFTypeFromOption(rt, env, FTypeTop.ONLY);
        List<Parameter> fparams = EvalType.paramsToParameters(env, params);

        setParamsAndReturnType(fparams, ft);
        return;
    }

    static class GenericComparer implements Comparator<GenericMethod>, Serializable {

        public int compare(GenericMethod arg0, GenericMethod arg1) {
            Applicable a0 = arg0.getDef();
            Applicable a1 = arg1.getDef();

            IdOrOpOrAnonymousName fn0 = NodeUtil.getName(a0);
            IdOrOpOrAnonymousName fn1 = NodeUtil.getName(a1);
            int x = NodeComparator.compare(fn0, fn1);
            if (x != 0) return x;

            List<StaticParam> oltp0 = NodeUtil.getStaticParams(a0);
            List<StaticParam> oltp1 = NodeUtil.getStaticParams(a1);

            return NodeComparator.compare(oltp0, oltp1);

        }

    }

    static final GenericComparer genComparer = new GenericComparer();

    // static class GenericFullComparer implements Comparator<GenericMethod> {

    //     public int compare(GenericMethod arg0, GenericMethod arg1) {
    //         return compare(arg0.getDef(), arg1.getDef());
    //     }

    //     int compare(Applicable left, Applicable right) {
    //         if (left instanceof FnExpr) {
    //             int x = Useful.compareClasses(left, right);
    //             if (x != 0) return x;
    //             return NodeUtil.nameString(((FnExpr)left).getName())
    //                 .compareTo(NodeUtil.nameString(((FnExpr)right).getName()));
    //         } else if (left instanceof FnDecl) {
    //             int x = Useful.compareClasses(left, right);
    //             if (x != 0) return x;
    //             return compare(left, (FnDecl)right);
    //         } else if (left instanceof NativeApp) {
    //             return Useful.compareClasses(left, right);
    //         } else {
    //             throw new InterpreterBug(left, "NodeComparator.compare(" +
    //                                      left.getClass() + ", " + right.getClass());
    //         }
    //     }

    // }
    // static final GenericFullComparer genFullComparer = new GenericFullComparer();

    public IdOrOpOrAnonymousName getName() {
        return NodeUtil.getName(getDef());
    }

    public List<StaticParam> getStaticParams() {
        return NodeUtil.getStaticParams(getDef());
    }

    public List<Param> getParams() {
        return NodeUtil.getParams(getDef());
    }

    public Option<Type> getReturnType() {
        return NodeUtil.getReturnType(getDef());
    }


}
