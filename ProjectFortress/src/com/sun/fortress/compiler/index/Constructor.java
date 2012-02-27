/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.index;

import com.sun.fortress.scala_src.useful.STypesUtil;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.Modifiers;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.Span;
import edu.rice.cs.plt.lambda.Thunk;
import edu.rice.cs.plt.tuple.Option;

import java.util.Collections;
import java.util.List;

public class Constructor extends Function {

    private final Id _declaringTrait;
    private final List<StaticParam> _staticParams;
    private final Option<List<Param>> _params;
    private final Option<List<Type>> _throwsClause;
    private final Option<WhereClause> _where;

    public Constructor(Id declaringTrait, List<StaticParam> staticParams, Option<List<Param>> params, Option<List<Type>> throwsClause, Option<WhereClause> where) {
        _declaringTrait = declaringTrait;
        _staticParams = staticParams;
        _params = params;
        _throwsClause = throwsClause;
        _where = where;

        // TODO: Should we just pass in the declaring trait here?
        // Then we can use STypesUtil.declToTraitType.
        _thunk = Option.<Thunk<Option<Type>>>some(new Thunk<Option<Type>>() {
            public Option<Type> value() {
                return Option.<Type>some(NodeFactory.makeTraitType(_declaringTrait, STypesUtil.staticParamsToArgs(_staticParams)));
            }
        });
    }

    /**
     * Copy another Constructor, performing a substitution with the visitor.
     */
    public Constructor(Constructor that, NodeUpdateVisitor visitor) {
        _declaringTrait = that._declaringTrait;
        _staticParams = visitor.recurOnListOfStaticParam(that._staticParams);
        _params = visitor.recurOnOptionOfListOfParam(that._params);
        _throwsClause = visitor.recurOnOptionOfListOfType(that._throwsClause);
        _where = visitor.recurOnOptionOfWhereClause(that._where);

        _thunk = that._thunk;
        _thunkVisitors = that._thunkVisitors;
        pushVisitor(visitor);
    }

    public Modifiers mods() { return Modifiers.None; }

    @Override
    public Span getSpan() {
        return NodeUtil.getSpan(_declaringTrait);
    }

    public Id declaringTrait() {
        return _declaringTrait;
    }

    public List<StaticParam> traitStaticParameters() {
        return _staticParams;
    }

    public IdOrOpOrAnonymousName toUndecoratedName() {
        // This choice is not tested yet, it could well be the wrong one.
        return declaringTrait();
    }

    @Override
    public Id name() {
        return _declaringTrait;
    }

    public IdOrOp unambiguousName() {
        return name();
    }

    //    public List<StaticParam> staticParams() { return _staticParams; }
    //    public Option<List<Param>> params() { return _params; }

    //    public Option<List<BaseType>> throwsClause() { return _throwsClause; }
    public Option<WhereClause> where() {
        return _where;
    }

    @Override
    public Option<Expr> body() {
        return Option.none();
    }

    @Override
    public List<Param> parameters() {
        if (_params.isNone()) return Collections.emptyList();
        else return Collections.unmodifiableList(_params.unwrap());
    }

    @Override
    public List<StaticParam> staticParameters() {
        return Collections.unmodifiableList(_staticParams);
    }

    @Override
    public List<Type> thrownTypes() {
        if (_throwsClause.isNone()) return Collections.emptyList();
        else return Collections.unmodifiableList(_throwsClause.unwrap());
    }

}
