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

import java.util.Collections;
import java.util.List;

import com.sun.fortress.nodes.ArrowType;
import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.FnDecl;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.IdOrOpOrAnonymousName;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeDepthFirstVisitor;
import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.useful.NI;
import com.sun.fortress.compiler.typechecker.TypesUtil;

import edu.rice.cs.plt.lambda.SimpleBox;
import edu.rice.cs.plt.lambda.Lambda;
import edu.rice.cs.plt.lambda.Thunk;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.collect.CollectUtil;

/**
 * Note that this is a {@link com.sun.fortress.compiler.index.Function}, not a {@link com.sun.fortress.compiler.index.Method}, despite the name
 * (methods have distinct receivers).
 */
public class Coercion extends Function {
    protected final FnDecl _ast;
    protected final Id _declaringTrait;
    protected final List<StaticParam> _traitParams;

    public Coercion(FnDecl ast, Id declaringTrait, List<StaticParam> traitParams) {
        _ast = ast;
        _declaringTrait = declaringTrait;
        _traitParams = CollectUtil.makeList(IterUtil.map(traitParams, liftStaticParam));

        putThunk(new Thunk<Option<Type>>() {
          @Override public Option<Type> value() {
            return Option.<Type>some(
                NodeFactory.makeTraitType(_declaringTrait,
                                          TypesUtil.staticParamsToArgs(_traitParams)));
          }
        });
    }

    public FnDecl ast() { return _ast; }

    @Override
    public Span getSpan() { return NodeUtil.getSpan(_ast); }

    protected String mandatoryToString() {
        return "coercion " + declaringTrait().toString() + "." + ast();
    }

    @Override
    protected IdOrOpOrAnonymousName mandatoryToUndecoratedName() {
        return ast().getHeader().getName();
    }

    public Id declaringTrait() { return _declaringTrait; }

	@Override
	public Option<Expr> body() {
		return _ast.accept(new NodeDepthFirstVisitor<Option<Expr>>(){
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
		if(  NodeUtil.getThrowsClause(_ast).isNone() )
			return Collections.emptyList();
		else
			return Collections.unmodifiableList(NodeUtil.getThrowsClause(_ast).unwrap());
	}

	@Override
	public Functional instantiate(List<StaticParam> params, List<StaticArg> args) {
		// TODO Auto-generated method stub
		return NI.nyi();
	}

	@Override
	public Functional acceptNodeUpdateVisitor(NodeUpdateVisitor visitor) {
		return new Coercion((FnDecl)this.ast().accept(visitor), this._declaringTrait, this._traitParams);
	}
}