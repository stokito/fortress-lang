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

package com.sun.fortress.compiler.typechecker;

import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeFactory;
import edu.rice.cs.plt.tuple.Option;

import java.util.Arrays;
import java.util.List;

public abstract class StaticParamEnv {
    public static StaticParamEnv make(StaticParam... params) {
        return EmptyStaticParamEnv.ONLY.extend(params);
    }

    public static StaticParamEnv make(List<StaticParam> params) {
        return EmptyStaticParamEnv.ONLY.extend(params);
    }

    public abstract Option<StaticParam> binding(SimpleName name);

    public Option<StaticParam> binding(String name) {
        return binding(NodeFactory.makeId(name));
    }

    public Option<StaticParam> opBinding(String name) {
        return binding(NodeFactory.makeOp(name));
    }

    public StaticParamEnv extend(StaticParam... params) {
        return this.extend(Arrays.asList(params));
    }

    public StaticParamEnv extend(List<StaticParam> params) {
        if (params.size() == 0) { return this; }
        else { return new NonEmptyStaticParamEnv(params, this); }
    }

    public StaticParamEnv extend(List<StaticParam> params, WhereClause whereClause) {
        // For now, only bindings of hidden type variables are supported (not constraints).
        List<WhereBinding> whereBindings = whereClause.getBindings();
        if (params.size() == 0 && whereBindings.size() == 0) { return this; }
        else { return new WhereClauseEnv(params, whereClause, this); }
    }


//    public abstract Option<OperatorParam> operatorParam(Op name);
//    public abstract Option<BoolParam> boolParam(Id name);
//    public abstract Option<DimensionParam> dimensionParam(Id name);
//    public abstract Option<IntParam> intParam(Id name);
//    public abstract Option<NatParam> natParam(Id name);
//    public abstract Option<SimpleTypeParam> typeParam(Id name);
//    public abstract Option<UnitParam> unitParam(Id name);
}
