/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/

package com.sun.fortress.interpreter.evaluator.values;

import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.types.FTraitOrObjectOrGeneric;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.FTypeGeneric;
import com.sun.fortress.nodes.FnDecl;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.WhereClause;
import com.sun.fortress.nodes_util.ErrorMsgMaker;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.useful.Useful;
import edu.rice.cs.plt.tuple.Option;

import java.util.List;

public class GenericFunctionalMethod extends FGenericFunction implements HasSelfParameter {


    int selfParameterIndex;
    private FTypeGeneric selfParameterType;

    public GenericFunctionalMethod(Environment e,
                                   FnDecl fndef,
                                   int self_parameter_index,
                                   FTypeGeneric self_parameter_type) {
        super(e, fndef);
        this.selfParameterIndex = self_parameter_index;
        this.selfParameterType = self_parameter_type;
    }

    @Override
    protected Simple_fcn newClosure(Environment clenv, List<FType> args) {
        // BUG IS HERE, NEED TO instantiate the selfParameterType! ;

        FTraitOrObjectOrGeneric instantiatedSelfType = ((FTypeGeneric) selfParameterType).make(args, getFnDecl());

        FunctionalMethod cl = FType.anyAreSymbolic(args) ?
                              new FunctionalMethodInstance(clenv,
                                                           fndef,
                                                           args,
                                                           this,
                                                           selfParameterIndex,
                                                           instantiatedSelfType) :
                              new FunctionalMethod(clenv, fndef, args, selfParameterIndex, instantiatedSelfType);
        cl.finishInitializing();
        return cl;
    }

    @Override
    public List<StaticParam> getStaticParams() {
        return NodeUtil.getStaticParams(selfParameterType.getDef());
    }

    protected Option<WhereClause> getWhere() {
        // TODO need to get where clause from generics, in general.
        return Option.<WhereClause>none();
    }

    public int hashCode() {
        return getDef().hashCode() + selfParameterType.hashCode();
    }

    public boolean equals(Object o) {
        if (o == null) return false;
        if (this == o) return true;
        if (o.getClass().equals(this.getClass())) {
            GenericFunctionalMethod oc = (GenericFunctionalMethod) o;
            return getDef() == oc.getDef() && selfParameterType.equals(oc.selfParameterType);
        }
        return false;
    }

    public int getSelfParameterIndex() {
        return selfParameterIndex;
    }

    public FTraitOrObjectOrGeneric getSelfParameterType() {
        return selfParameterType;
    }

    public FTypeGeneric getSelfParameterTypeAsGeneric() {
        return selfParameterType;
    }


    public String toString() {
        FnDecl node = fndef;
        // Code lifted from ErrorMsgMaker.forFnDecl
        return selfParameterType.toString() + Useful.listInOxfords(ErrorMsgMaker.ONLY.mapSelf(getStaticParams())) +
               "." + NodeUtil.nameString(NodeUtil.getName(node))
               //+ Useful.listInOxfords(ErrorMsgMaker.ONLY.mapSelf(getStaticParams()))
               + Useful.listInParens(ErrorMsgMaker.ONLY.mapSelf(NodeUtil.getParams(node))) + (NodeUtil.getReturnType(
                node).isSome() ? (":" + NodeUtil.getReturnType(node).unwrap().accept(ErrorMsgMaker.ONLY)) : "") +
               fndef.at();
    }

}
