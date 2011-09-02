/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
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
import com.sun.fortress.interpreter.evaluator.InstantiationLock;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.FTypeGeneric;
import com.sun.fortress.interpreter.evaluator.types.FTypeObject;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.useful.Factory1P;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.Memo1PCL;
import edu.rice.cs.plt.tuple.Option;

import java.util.ArrayList;
import java.util.List;

public class GenericConstructor extends GenericFunctionOrConstructor
        implements Factory1P<List<FType>, Simple_fcn, HasAt>, GenericFunctionOrMethod {
    private class Factory implements Factory1P<List<FType>, Constructor, HasAt> {

        public Constructor make(List<FType> args, HasAt within) {
            // Use the generic type to make the specific type
            String name = NodeUtil.stringName(odefOrDecl);

            FTypeGeneric.startPendingTraitFMs();
            FTypeGeneric gt = (FTypeGeneric) env.getRootType(name); // toplevel

            /*
            * Necessary to fake an instantiation expression.
            */
            FTypeObject ft = (FTypeObject) gt.make(args, odefOrDecl);

            // Use the augmented environment from the specific type.
            Environment clenv = ft.getWithin();

            // Build the constructor
            //            Option<List<Param>> params = odefOrDecl.getParams();
            //            List<Parameter> fparams =
            //                EvalType.paramsToParameters(clenv, params.unwrap());

            Constructor cl = makeAConstructor(clenv, ft, NodeUtil.getParams(odefOrDecl));
            FTypeGeneric.flushPendingTraitFMs();
            return cl;
        }


    }

    Memo1PCL<List<FType>, Constructor, HasAt> memo = new Memo1PCL<List<FType>, Constructor, HasAt>(new Factory(),
                                                                                                   FType.listComparer,
                                                                                                   InstantiationLock.L);

    public Constructor make(List<FType> l, HasAt within) {
        return memo.make(l, within);
    }

    public GenericConstructor(Environment env, ObjectConstructor odefOrDecl, IdOrOpOrAnonymousName cfn) {
        super(env);
        this.env = env;
        this.odefOrDecl = odefOrDecl;
        this.cfn = cfn;
    }

    Environment env;
    ObjectConstructor odefOrDecl;
    IdOrOpOrAnonymousName cfn;

    public ObjectConstructor getDefOrDecl() {
        return odefOrDecl;
    }

    public String getString() {
        return s(odefOrDecl);
    }

    public boolean seqv(FValue v) {
        return false;
    }

    protected Constructor constructAConstructor(Environment clenv,
                                                FTypeObject objectType,
                                                Option<List<Param>> objectParams) {
        return new Constructor(clenv, objectType, odefOrDecl, objectParams);
    }

    private Constructor makeAConstructor(Environment clenv, FTypeObject objectType, Option<List<Param>> objectParams) {
        Constructor cl = constructAConstructor(clenv, objectType, objectParams);
        cl.finishInitializing();
        return cl;
    }

    public Simple_fcn typeApply(List<FType> argValues, HasAt location) throws ProgramError {
        return make(argValues, location);
    }

    public Simple_fcn typeApply(List<FType> argValues) throws ProgramError {
        return make(argValues, odefOrDecl);
    }

    public Simple_fcn typeApply(List<StaticArg> args, Environment e, HasAt x) {
        List<StaticParam> params = NodeUtil.getStaticParams(odefOrDecl);

        ArrayList<FType> argValues = argsToTypes(args, e, x, params);
        return make(argValues, x);
    }

    public static ArrayList<FType> argsToTypes(List<StaticArg> args, Environment e, HasAt x, List<StaticParam> params) {
        // Evaluate each of the args in e, inject into clenv.
        if (args.size() != params.size()) {
            error(x, e, errorMsg("Generic instantiation (size) mismatch, expected ", params, " got ", args));
        }
        EvalType et = new EvalType(e);
        ArrayList<FType> argValues = et.forStaticArgList(args);
        return argValues;
    }

    @Override
    public String at() {
        // TODO Auto-generated method stub
        return odefOrDecl.at();
    }

    @Override
    public IdOrOpOrAnonymousName getFnName() {
        // TODO Auto-generated method stub
        return cfn;
    }

    public String stringName() {
        // TODO Auto-generated method stub
        return cfn.stringName();
    }

    /* (non-Javadoc)
    * @see com.sun.fortress.interpreter.evaluator.values.SingleFcn#getDomain()
    *
    * Cut and paste from FGenericFunction
    */
    @Override
    public List<FType> getDomain() {
        return getSymbolic().getDomain();

    }

    public FType getRange() {
        return getSymbolic().getRange();
    }

    protected Simple_fcn getSymbolic() throws Error {
        if (symbolicInstantiation == null) {
            synchronized (this) {
                if (symbolicInstantiation == null) {
                    List<FType> symbolic_static_args =
                            FGenericFunction.FunctionsAndState.symbolicStaticsByPartition.get(this);
                    if (symbolic_static_args == null) {
                        /* TODO This is not quite right, because we risk
                        * identifying two functions whose where clauses are
                        * interpreted differently in two different environments.
                        */
                        symbolic_static_args =
                                FGenericFunction.FunctionsAndState.symbolicStaticsByPartition.syncPutIfMissing(this,
                                                                                                               createSymbolicInstantiation(
                                                                                                                       getWithin(),
                                                                                                                       NodeUtil.getStaticParams(
                                                                                                                               odefOrDecl),
                                                                                                                       Option.<WhereClause>none(),
                                                                                                                       odefOrDecl));
                    }
                    symbolicInstantiation = make(symbolic_static_args, odefOrDecl);
                }
            }
        }
        return symbolicInstantiation;
    }

    public IdOrOpOrAnonymousName getName() {
        // TODO Auto-generated method stub
        return cfn;
    }

    public List<StaticParam> getStaticParams() {
        // TODO Auto-generated method stub
        return NodeUtil.getStaticParams(odefOrDecl);
    }

    public List<Param> getParams() {
        // TODO Auto-generated method stub
        assert (NodeUtil.getParams(odefOrDecl).isSome());
        return NodeUtil.getParams(odefOrDecl).unwrap();
    }

    public Option<Type> getReturnType() {
        // TODO Auto-generated method stub
        // TODO this will probably not be good enough.
        return Option.<Type>none();
    }

}
