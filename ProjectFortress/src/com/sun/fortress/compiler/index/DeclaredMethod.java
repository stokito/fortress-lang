/*******************************************************************************
    Copyright 2008,2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.index;

import com.sun.fortress.compiler.typechecker.StaticTypeReplacer;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.nodes_util.NodeFactory;

import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.lambda.SimpleBox;
import edu.rice.cs.plt.lambda.Thunk;
import edu.rice.cs.plt.tuple.Option;

import java.util.Collections;
import java.util.List;

public class DeclaredMethod extends Method {

    private final FnDecl _ast;
    private final Id _declaringTrait;
    private final Option<SelfType> _selfType;
    private final List<StaticParam> _traitParams;
    private final Method _originalMethod;
    
    public DeclaredMethod(FnDecl ast, TraitObjectDecl traitDecl) {
        _originalMethod = this;
        _ast = ast;
        _declaringTrait = NodeUtil.getName(traitDecl);
        _selfType = traitDecl.getSelfType();
        _traitParams = CollectUtil.makeList(IterUtil.map(NodeUtil.getStaticParams(traitDecl), liftStaticParam));
        if (NodeUtil.getReturnType(_ast).isSome())
            _thunk = Option.<Thunk<Option<Type>>>some(SimpleBox.make(NodeUtil.getReturnType(_ast)));
    }

    /**
     * Copy another DeclaredMethod, performing a substitution with the visitor.
     */
    private DeclaredMethod(DeclaredMethod that, List<StaticParam> params, StaticTypeReplacer visitor) {
        _originalMethod = that.originalMethod();
        _ast = (FnDecl) that._ast.accept(visitor);
        _declaringTrait = that._declaringTrait;
        _selfType = visitor.recurOnOptionOfSelfType(that._selfType);
        // Static instantiation clears params
        _traitParams = params;
        _thunk = that._thunk;
        _thunkVisitors = that._thunkVisitors;
        pushVisitor(visitor);
    }
    
    public FnDecl ast() {
        return _ast;
    }

    @Override
    public Method originalMethod() {return _originalMethod;}
    
    @Override
    public Span getSpan() {
        return NodeUtil.getSpan(_ast);
    }

    @Override
    public Option<Expr> body() {
        return _ast.accept(new NodeDepthFirstVisitor<Option<Expr>>() {
            @Override
            public Option<Expr> defaultCase(Node that) {
                return Option.none();
            }

            @Override
            public Option<Expr> forFnDecl(FnDecl that) {
                return that.getBody();
            }
        });
    }

    @Override
    public List<Param> parameters() {
        return NodeUtil.getParams(_ast);
    }

    @Override
    public List<StaticParam> staticParameters() {
        return NodeUtil.getStaticParams(_ast);
    }

    @Override
    public List<Type> thrownTypes() {
        if (NodeUtil.getThrowsClause(_ast).isNone()) return Collections.emptyList();
        else return Collections.unmodifiableList(NodeUtil.getThrowsClause(_ast).unwrap());
    }

    @Override
    public DeclaredMethod instantiateTraitStaticParameters(List<StaticParam> params, List<StaticArg> args) {
        return new DeclaredMethod(this, params, new StaticTypeReplacer(_traitParams, args));
    }

    @Override
    public DeclaredMethod instantiateTraitStaticParameters(List<StaticParam> params, StaticTypeReplacer str) {
        return new DeclaredMethod(this, params, str);
    }

    public Id declaringTrait() {
        return this._declaringTrait;
    }

    public Option<SelfType> selfType() {
        return _selfType;
    }

    @Override
    public IdOrOp name() {
        // Declared methods cannot have anonymous names.
        return (IdOrOp) NodeUtil.getName(_ast);
    }
    
    public IdOrOpOrAnonymousName toUndecoratedName() {
        return ast().getHeader().getName();
    }

    public IdOrOp unambiguousName() {
        return (IdOrOp) _ast.getUnambiguousName();
    }

    @Override
    public boolean hasDeclaredReturnType() {
        return NodeUtil.getReturnType(_ast).isSome();
    }
    
    @Override
    public List<StaticParam> traitStaticParameters() {
        return _traitParams;
    }
}
