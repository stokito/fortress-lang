/*******************************************************************************
 Copyright 2009 Sun Microsystems, Inc.,
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

package com.sun.fortress.compiler.index;

import com.sun.fortress.compiler.typechecker.TypesUtil;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.Modifiers;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.Span;
import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.lambda.Thunk;
import edu.rice.cs.plt.tuple.Option;

import java.util.Collections;
import java.util.List;

/**
 * Note that this is a {@link com.sun.fortress.compiler.index.Function}, not a
 * {@link com.sun.fortress.compiler.index.Method}, despite the name
 * (methods have distinct receivers).
 */
public class Coercion extends Function {
    protected final FnDecl _ast;
    protected final Id _declaringTrait;
    protected final List<StaticParam> _traitParams;

    public Coercion(FnDecl ast, TraitObjectDecl traitDecl, List<StaticParam> traitParams) {
        _ast = ast;
        _declaringTrait = NodeUtil.getName(traitDecl);
        _traitParams = CollectUtil.makeList(IterUtil.map(traitParams, liftStaticParam));

        _thunk = Option.<Thunk<Option<Type>>>some(new Thunk<Option<Type>>() {
            public Option<Type> value() {
                return Option.<Type>some(NodeFactory.makeTraitType(_declaringTrait, TypesUtil.staticParamsToArgs(_traitParams)));
            }
        });
    }

    /**
     * Copy another Coercion, performing a substitution with the visitor.
     */
    public Coercion(Coercion that, NodeUpdateVisitor visitor) {
        _ast = (FnDecl) that._ast.accept(visitor);
        _declaringTrait = that._declaringTrait;
        _traitParams = visitor.recurOnListOfStaticParam(that._traitParams);

        _thunk = that._thunk;
        _thunkVisitors = that._thunkVisitors;
        pushVisitor(visitor);
    }

    public FnDecl ast() {
        return _ast;
    }

    public Modifiers mods() { return Modifiers.None; }

    @Override
    public Span getSpan() {
        return NodeUtil.getSpan(_ast);
    }

    @Override
    public IdOrOpOrAnonymousName toUndecoratedName() {
        return ast().getHeader().getName();
    }

    @Override
    public IdOrOp name() {
        // Coercions cannot have anonymous names.
        return (IdOrOp) ast().getHeader().getName();
    }

    public Id declaringTrait() {
        return _declaringTrait;
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
        // No static parameters allowed on individual coercions.
        return _traitParams;
    }

    @Override
    public List<BaseType> thrownTypes() {
        if (NodeUtil.getThrowsClause(_ast).isNone()) return Collections.emptyList();
        else return Collections.unmodifiableList(NodeUtil.getThrowsClause(_ast).unwrap());
    }

    @Override
    public Functional acceptNodeUpdateVisitor(NodeUpdateVisitor visitor) {
        return new Coercion(this, visitor);
    }

    @Override
    public String toString() {
        return String.format("%s.%s", _declaringTrait.getText(), super.toString());
    }
}
