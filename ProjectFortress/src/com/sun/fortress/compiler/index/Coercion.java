/*******************************************************************************
 Copyright 2008,2010, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.index;

import com.sun.fortress.compiler.NamingCzar;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.Modifiers;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.scala_src.useful.STypesUtil;
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
    protected final boolean _lifted;

    /** Construct a Coercion index using the original, unlifted function. */
    public Coercion(FnDecl ast,
                    TraitObjectDecl traitDecl,
                    Option<APIName> apiName,
                    List<StaticParam> traitParams) {
        _lifted = false;
        _ast = ast;
        _declaringTrait = NodeFactory.makeId(apiName, NodeUtil.getName(traitDecl));
        _traitParams = CollectUtil.makeList(IterUtil.map(traitParams, liftStaticParam));

        // TODO: Is this type right?  It seems to only be half the type.
        // And why is traitParams distinct from the
        _thunk = Option.<Thunk<Option<Type>>>some(new Thunk<Option<Type>>() {
            public Option<Type> value() {
                return Option.<Type>some(NodeFactory.makeTraitType(_declaringTrait, STypesUtil.staticParamsToArgs(_traitParams)));
            }
        });
    }

    /** Construct a Coercion index using the renamed, lifted function. */
    public Coercion(FnDecl ast, Option<APIName> apiName) {

        // Return type always a TraitType inserted by CoercionLifter.
        Type returnTy = NodeUtil.getReturnType(ast).unwrap();
        final TraitType returnType;
        if (returnTy instanceof TraitSelfType)
            returnType = (TraitType)((TraitSelfType)returnTy).getNamed();
        else returnType = (TraitType) returnTy;


        _lifted = true;
        _ast = ast;
        _declaringTrait = NodeFactory.makeId(apiName, returnType.getName());
        _traitParams = Collections.emptyList();

        _thunk = Option.<Thunk<Option<Type>>>some(new Thunk<Option<Type>>() {
            public Option<Type> value() {
                return Option.<Type>some(returnType);
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
        _lifted = that._lifted;

        _thunk = that._thunk;
        _thunkVisitors = that._thunkVisitors;
        pushVisitor(visitor);
    }

    public boolean isLifted() {
        return _lifted;
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

    public IdOrOp unambiguousName() {
        return (IdOrOp) ast().getUnambiguousName();
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
        List<StaticParam> fnSparams = NodeUtil.getStaticParams(_ast);
        if (_lifted)
            return fnSparams;
        else
            return CollectUtil.makeList(IterUtil.compose(_traitParams, fnSparams));
    }

    @Override
    public List<Type> thrownTypes() {
        if (NodeUtil.getThrowsClause(_ast).isNone()) return Collections.emptyList();
        else return Collections.unmodifiableList(NodeUtil.getThrowsClause(_ast).unwrap());
    }

    
    @Override
    public String toString() {
        return String.format("%s.%s", _declaringTrait.getText(), super.toString());
    }
}
