/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/

package com.sun.fortress.interpreter.evaluator.values;

import com.sun.fortress.exceptions.ProgramError;
import static com.sun.fortress.exceptions.ProgramError.error;
import static com.sun.fortress.exceptions.ProgramError.errorMsg;
import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.EvalType;
import com.sun.fortress.interpreter.evaluator.types.FType;
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

public class FGenericFunction extends GenericFunctionOrConstructor
        implements GenericFunctionOrMethod, Factory1P<List<FType>, Simple_fcn, HasAt> {

    FnDecl fndef;


    /* (non-Javadoc)
    * @see com.sun.fortress.interpreter.evaluator.values.SingleFcn#getDomain()
    */

    protected Simple_fcn getSymbolic() throws Error, ProgramError {
        if (symbolicInstantiation == null) {
            synchronized (this) {
                if (symbolicInstantiation == null) {
                    List<FType> symbolic_static_args = FunctionsAndState.symbolicStaticsByPartition.get(this);
                    if (symbolic_static_args == null) {
                        /* TODO This is not quite right, because we risk
                        * identifying two functions whose where clauses are
                        * interpreted differently in two different environments.
                        */
                        symbolic_static_args = FunctionsAndState.symbolicStaticsByPartition.syncPutIfMissing(this,
                                                                                                             createSymbolicInstantiation(
                                                                                                                     getEnv(),
                                                                                                                     getStaticParams(),
                                                                                                                     getWhere(),
                                                                                                                     fndef));
                    }
                    symbolicInstantiation = typeApply(symbolic_static_args, fndef);
                }
            }
        }
        return symbolicInstantiation;
    }

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

    public boolean seqv(FValue v) {
        return false;
    }

    protected Simple_fcn newClosure(Environment clenv, List<FType> args) {
        FunctionClosure cl = FType.anyAreSymbolic(args) ?
                             new ClosureInstance(clenv, fndef, args, this) :
                             new FunctionClosure(clenv, fndef, args);
        cl.finishInitializing();
        return cl;
    }

    //    public BATreeEC<List<FValue>, List<FType>, Simple_fcn> cache =
    //        new BATreeEC<List<FValue>, List<FType>, Simple_fcn>(FValue.asTypesList);

    private class Factory implements Factory1P<List<FType>, Simple_fcn, HasAt> {

        public Simple_fcn make(List<FType> args, HasAt location) {
            Environment clenv = getEnv().extendAt(location);
            List<StaticParam> params = getStaticParams();
            EvalType.bindGenericParameters(params, args, clenv, location, fndef);

            return newClosure(clenv, args);
        }


    }

    Memo1P<List<FType>, Simple_fcn, HasAt> memo = new Memo1P<List<FType>, Simple_fcn, HasAt>(new Factory());

    public Simple_fcn make(List<FType> l, HasAt location) {
        return memo.make(l, location);
    }

    public FnDecl getFnDecl() {
        return fndef;
    }

    public Environment getEnv() {
        return getWithin();
    }

    public FGenericFunction(Environment e, FnDecl fndef) {
        super(e);
        this.fndef = fndef;
    }

    public Simple_fcn typeApply(List<StaticArg> args, Environment e, HasAt location) {
        EvalType et = new EvalType(e);
        // TODO Can combine these two functions if we enhance the memo and factory
        // to pass two parameters instead of one.
        ArrayList<FType> argValues = et.forStaticArgList(args);

        return typeApply(argValues, location);
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
    public Simple_fcn typeApply(List<FType> argValues, HasAt location) throws ProgramError {
        List<StaticParam> params = getStaticParams();

        if (argValues.size() != params.size()) {
            error(location, errorMsg("Generic instantiation (size) mismatch, expected ", params, " got ", argValues));
        }
        return make(argValues, location);
    }

    public Simple_fcn typeApply(List<FType> argValues) throws ProgramError {
        return typeApply(argValues, fndef);
    }

    public List<StaticParam> getStaticParams() {
        return NodeUtil.getStaticParams(fndef);
    }

    public List<Param> getParams() {
        return NodeUtil.getParams(fndef);
    }


    protected Option<WhereClause> getWhere() {
        return NodeUtil.getWhereClause(fndef);
    }

    @Override
    public IdOrOpOrAnonymousName getFnName() {
        return NodeUtil.getName(fndef);
    }

    public IdOrOpOrAnonymousName getName() {
        return NodeUtil.getName(fndef);
    }

    public Applicable getDef() {
        return fndef;
    }

    static class GenericComparer implements Comparator<GenericFunctionOrMethod>, Serializable {

        public int compare(GenericFunctionOrMethod a0, GenericFunctionOrMethod a1) {

            IdOrOpOrAnonymousName fn0 = a0.getName();
            IdOrOpOrAnonymousName fn1 = a1.getName();
            int x = NodeComparator.compare(fn0, fn1);
            if (x != 0) return x;

            List<StaticParam> oltp0 = a0.getStaticParams();
            List<StaticParam> oltp1 = a1.getStaticParams();

            return NodeComparator.compare(oltp0, oltp1);

        }

    }

    // static final GenericComparer genComparer = new GenericComparer();

    // static class GenericFullComparer implements Comparator<FGenericFunction> {

    //     public int compare(FGenericFunction arg0, FGenericFunction arg1) {
    //         return NodeComparator.compare(arg0.fndef, arg1.fndef);

    //     }
    // }
    // static final GenericFullComparer genFullComparer = new GenericFullComparer();

    public Option<Type> getReturnType() {
        return NodeUtil.getReturnType(fndef);
    }


}
