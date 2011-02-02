/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.values;

import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.nodes.IdOrOpOrAnonymousName;
import com.sun.fortress.nodes.ObjectDecl;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.useful.Factory1P;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.Memo1P;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GenericSingleton extends FValue implements Factory1P<List<FType>, FObject, HasAt> {

    ObjectDecl odecl;
    FType t;
    GenericConstructor genericConstructor;

    public GenericSingleton(ObjectDecl odecl, FType t, GenericConstructor gc) {
        super();
        this.odecl = odecl;
        this.t = t;
        this.genericConstructor = gc;
    }

    private class Factory implements Factory1P<List<FType>, FObject, HasAt> {
        public FObject make(List<FType> args, HasAt site) {
            return (FObject) genericConstructor.typeApply(args, site).
                    applyToArgs(Collections.<FValue>emptyList());
        }
    }

    Memo1P<List<FType>, FObject, HasAt> memo = new Memo1P<List<FType>, FObject, HasAt>(new Factory());

    public FObject make(List<FType> l, HasAt location) {
        return memo.make(l, location);
    }

    @Override
    public FType type() {
        return t;
    }

    public IdOrOpOrAnonymousName getName() {
        return NodeUtil.getName(odecl);
    }

    public String getString() {
        return s(odecl);
    }

    public boolean seqv(FValue v) {
        // pointer equality checked already.
        return false;
    }

    public List<StaticParam> getStaticParams() {
        return NodeUtil.getStaticParams(odecl);
    }

    public FObject typeApply(List<FType> argValues, HasAt location) {
        return make(argValues, location);
    }

    public FObject typeApply(List<FType> argValues) {
        return make(argValues, odecl);
    }

    public FObject typeApply(List<StaticArg> args, Environment e, HasAt x) {
        List<StaticParam> params = NodeUtil.getStaticParams(odecl);
        ArrayList<FType> argValues = GenericConstructor.argsToTypes(args, e, x, params);
        return make(argValues, x);
    }

}
