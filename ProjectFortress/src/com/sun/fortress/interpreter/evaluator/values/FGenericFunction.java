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
import com.sun.fortress.interpreter.evaluator.EvaluatorBase;
import com.sun.fortress.interpreter.evaluator.ProgramError;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.nodes.Applicable;
import com.sun.fortress.nodes.FnAbsDeclOrDecl;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.SimpleName;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.WhereClause;
import com.sun.fortress.nodes_util.NodeComparator;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.useful.BATree;
import com.sun.fortress.useful.Factory1P;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.Memo1P;
import com.sun.fortress.useful.Useful;

import edu.rice.cs.plt.tuple.Option;

import static com.sun.fortress.interpreter.evaluator.ProgramError.errorMsg;
import static com.sun.fortress.interpreter.evaluator.ProgramError.error;

public class FGenericFunction extends SingleFcn
                              implements GenericFunctionOrMethod,
                              Factory1P<List<FType>, Simple_fcn, HasAt> {

    volatile Simple_fcn symbolicInstantiation;

   /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.values.SingleFcn#at()
     */
    @Override
    public String at() {
        return fndef.at();
    }

     public String stringName() {
        return fndef.stringName();
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.values.SingleFcn#getDomain()
     */
    @Override
    public List<FType> getDomain() {
        if (symbolicInstantiation == null) {
            synchronized (this) {
                if (symbolicInstantiation == null) {
                    List<FType> symbolic_static_args = symbolicStaticsByPartition.get(this);
                    if (symbolic_static_args == null) {
                        /* TODO This is not quite right, because we risk
                         * identifying two functions whose where clauses are
                         * interpreted differently in two different environments.
                         */
                        symbolic_static_args =
                            symbolicStaticsByPartition.syncPutIfMissing(this,
                                    createSymbolicInstantiation(getEnv(), getStaticParams(), getWhere(), fndef));
                    }
                    symbolicInstantiation = typeApply(getEnv(), fndef, symbolic_static_args);
                }
            }
        }
        return symbolicInstantiation.getDomain();

    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.values.FValue#getString()
     */
    @Override
    public String getString() {
        return s(fndef);
    }

    @Override
    public String toString() {
        return getString() + fndef.at();
    }

    protected Simple_fcn newClosure(BetterEnv clenv, List<FType> args) {
        Closure cl = FType.anyAreSymbolic(args) ? new ClosureInstance(clenv, fndef, args, this) : new Closure(clenv, fndef, args);
         cl.finishInitializing();
        return cl;
    }

//    public BATreeEC<List<FValue>, List<FType>, Simple_fcn> cache =
//        new BATreeEC<List<FValue>, List<FType>, Simple_fcn>(FValue.asTypesList);

    private class Factory implements Factory1P<List<FType>, Simple_fcn, HasAt> {

        public Simple_fcn make(List<FType> args, HasAt location) {
            BetterEnv clenv = new BetterEnv(getEnv(), location);
            List<StaticParam> params = getStaticParams();
            EvalType.bindGenericParameters(params, args, clenv, location, fndef);

            return newClosure(clenv, args);
        }


    }

     Memo1P<List<FType>, Simple_fcn, HasAt> memo = new Memo1P<List<FType>, Simple_fcn, HasAt>(new Factory());

    public Simple_fcn make(List<FType> l, HasAt location) {
        return memo.make(l, location);
    }

    FnAbsDeclOrDecl fndef;

    public FnAbsDeclOrDecl getFnDefOrDecl() {
        return fndef;
    }

    public BetterEnv getEnv() {
        return getWithin();
    }

    public FGenericFunction(BetterEnv e, FnAbsDeclOrDecl fndef) {
        super(e);
        this.fndef = fndef;
    }

    public Simple_fcn typeApply(List<StaticArg> args, BetterEnv e, HasAt location) {
        EvalType et = new EvalType(e);
        // TODO Can combine these two functions if we enhance the memo and factory
        // to pass two parameters instead of one.
        ArrayList<FType> argValues = et.forStaticArgList(args);

        return typeApply(e, location, argValues);
    }

    /**
     * Same as typeApply, but with the types evaliated already.
     *
     * @param args
     * @param e
     * @param location
     * @param argValues
     * @return
     * @throws ProgramError
     */
    Simple_fcn typeApply(BetterEnv e, HasAt location, List<FType> argValues) throws ProgramError {
        List<StaticParam> params = getStaticParams();

        // Evaluate each of the args in e, inject into clenv.
        if (argValues.size() != params.size() ) {
            error(location, e,
                  errorMsg("Generic instantiation (size) mismatch, expected ",
                           Useful.listInParens(params), " got ",
                           Useful.listInParens(argValues)));
        }
        return make(argValues, location);
    }

    public Simple_fcn typeApply(HasAt location, List<FType> argValues) throws ProgramError {
        return make(argValues, location);
    }

    public  List<StaticParam> getStaticParams() {
        return  fndef.getStaticParams();
    }

    public List<Param> getParams() {
        return fndef.getParams();
    }


    protected WhereClause getWhere() {
        return fndef.getWhere();
    }

    @Override
    public FValue applyInner(List<FValue> args, HasAt loc, BetterEnv envForInference) {
        // TODO Auto-generated method stub
        Simple_fcn foo = EvaluatorBase.inferAndInstantiateGenericFunction(args, this, loc, envForInference);
        return foo.apply(args, loc, envForInference);
    }

    @Override
    public SimpleName getFnName() {
        return fndef.getName();
    }

    public SimpleName getName() {
        return fndef.getName();
    }

    public Applicable getDef() {
        return fndef;
    }

    static class GenericComparer implements Comparator<GenericFunctionOrMethod> {

        public int compare(GenericFunctionOrMethod a0, GenericFunctionOrMethod a1) {

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

    // static final GenericComparer genComparer = new GenericComparer();

    static class GenericFullComparer implements Comparator<FGenericFunction> {

        public int compare(FGenericFunction arg0, FGenericFunction arg1) {
            return NodeComparator.compare(arg0.fndef, arg1.fndef);

        }
    }
    static final GenericFullComparer genFullComparer = new GenericFullComparer();

    public Option<Type> getReturnType() {
         return fndef.getReturnType();
    }


}
