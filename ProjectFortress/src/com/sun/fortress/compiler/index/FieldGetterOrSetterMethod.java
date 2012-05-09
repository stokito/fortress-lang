/*******************************************************************************
    Copyright 2009,2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.index;

import com.sun.fortress.compiler.typechecker.StaticTypeReplacer;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.Span;

import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;
import java.util.Collections;
import java.util.List;

/**
 * Represents either a getter or setter method, created either implicitly or
 * explicitly. Contains all the common code between FieldGetterMethod and
 * FieldSetterMethod.
 */
abstract public class FieldGetterOrSetterMethod extends Method {
    /* comprises {FieldGetterMethod, FieldSetterMethod} */
    
    protected final Binding _ast;
    protected final Id _declaringTrait;
    protected final Option<SelfType> _selfType;
    protected final List<StaticParam> _traitParams;
    protected final Option<FnDecl> _fnDecl;
    protected final Method _originalMethod;

    /** Create an implicit getter/setter from a variable binding. */
    public FieldGetterOrSetterMethod(Binding b, TraitObjectDecl traitDecl) {
        _ast = b;
        _declaringTrait = NodeUtil.getName(traitDecl);
        _selfType = traitDecl.getSelfType();
        _traitParams = CollectUtil.makeList(IterUtil.map(NodeUtil.getStaticParams(traitDecl), liftStaticParam));
        _fnDecl = Option.<FnDecl>none();
        _originalMethod = this;
    }

    /** Create an explicit getter/setter from a function. */
    public FieldGetterOrSetterMethod(FnDecl f, Binding b, TraitObjectDecl traitDecl) {
        _ast = b;
        _declaringTrait = NodeUtil.getName(traitDecl);
        _selfType = traitDecl.getSelfType();
        _traitParams = CollectUtil.makeList(IterUtil.map(NodeUtil.getStaticParams(traitDecl), liftStaticParam));
        _fnDecl = Option.some(f);
        _originalMethod = this;
    }
    
    /**
     * Copy another getter/setter, performing a substitution with the visitor.
     */
    protected FieldGetterOrSetterMethod(FieldGetterOrSetterMethod that, List<StaticParam> params, StaticTypeReplacer visitor) {
        _ast = (Binding) that._ast.accept(visitor);
        _declaringTrait = that._declaringTrait;
        _selfType = visitor.recurOnOptionOfSelfType(that._selfType);
        // Static instantiation clears params
        _traitParams = params;
        if (that._fnDecl.isSome()) {
            _fnDecl = Option.some((FnDecl) that._fnDecl.unwrap().accept(visitor));
        } else {
            _fnDecl = Option.<FnDecl>none();
        }
        _originalMethod = that.originalMethod();

        _thunk = that._thunk;
        _thunkVisitors = that._thunkVisitors;
        pushVisitor(visitor);
    }
    
    @Override
    public Method originalMethod() {return _originalMethod;}
    
    public Option<FnDecl> fnDecl() {
        return _fnDecl;
    }

    public Binding ast() {
        return _ast;
    }

    @Override
    public Span getSpan() {
        return NodeUtil.getSpan(_ast);
    }

    @Override
    public Option<Expr> body() {
        if (_fnDecl.isNone()) return Option.none();
        return _fnDecl.unwrap().getBody();
    }

    @Override
    public List<StaticParam> staticParameters() {
        return Collections.emptyList();
    }

    @Override
    public List<Type> thrownTypes() {
        return Collections.emptyList();
    }

    public Id declaringTrait() {
        return this._declaringTrait;
    }

    public Option<SelfType> selfType() {
        return _selfType;
    }

    @Override
    public Id name() {
        return _ast.getName();
    }
    
    public IdOrOpOrAnonymousName toUndecoratedName() {
        return ast().getName();
    }

    public IdOrOp unambiguousName() {
        return name();
    }
    
    @Override
    public List<StaticParam> traitStaticParameters() {
        return _traitParams;
    }
}
