/*******************************************************************************
 Copyright 2008,2010, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.index;

import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.Modifiers;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.Span;
import edu.rice.cs.plt.lambda.SimpleBox;
import edu.rice.cs.plt.lambda.Thunk;
import edu.rice.cs.plt.tuple.Option;

import java.util.Collections;
import java.util.List;

public class DeclaredFunction extends Function {
    private final FnDecl _ast;

    public DeclaredFunction(FnDecl ast) {
        _ast = ast;
        if (NodeUtil.getReturnType(_ast).isSome())
            _thunk = Option.<Thunk<Option<Type>>>some(SimpleBox.make(NodeUtil.getReturnType(_ast)));
    }

    /**
     * Copy another DeclaredFunction, performing a substitution with the visitor.
     */
    public DeclaredFunction(DeclaredFunction that, NodeUpdateVisitor visitor) {
        _ast = (FnDecl) that._ast.accept(visitor);

        _thunk = that._thunk;
        _thunkVisitors = that._thunkVisitors;
        pushVisitor(visitor);
    }

    public FnDecl ast() {
        return _ast;
    }

    public Modifiers mods() {
        return NodeUtil.getMods(ast());
    }

    public IdOrOpOrAnonymousName toUndecoratedName() {
        return ast().getHeader().getName();
    }

    @Override
    public IdOrOp name() {
        // Declared functions cannot have anonymous names.
        return (IdOrOp) NodeUtil.getName(_ast);
    }

    public IdOrOp unambiguousName() {
        return (IdOrOp) _ast.getUnambiguousName();
    }

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
    public boolean hasDeclaredReturnType() {
        return NodeUtil.getReturnType(_ast).isSome();
    }

}
