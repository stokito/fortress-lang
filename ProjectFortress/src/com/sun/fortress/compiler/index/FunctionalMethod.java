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
import com.sun.fortress.useful.NI;

import edu.rice.cs.plt.lambda.SimpleBox;
import edu.rice.cs.plt.lambda.Lambda;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.collect.CollectUtil;

/**
 * Note that this is a {@link Function}, not a {@link Method}, despite the name
 * (methods have distinct receivers).
 */
public class FunctionalMethod extends Function {
    protected final FnDecl _ast;
    protected final Id _declaringTrait;
    protected final List<StaticParam> _traitParams;

    // Copy a static parameter but make it lifted.
    protected Lambda<StaticParam, StaticParam> liftStaticParam = new Lambda<StaticParam, StaticParam>() {
        public StaticParam value(StaticParam that) {
            return new StaticParam(that.getInfo(),
                                   that.getName(),
                                   that.getExtendsClause(),
                                   that.getDimParam(),
                                   that.isAbsorbsParam(),
                                   that.getKind(),
                                   true);
        }
    };

    public FunctionalMethod(FnDecl ast, Id declaringTrait, List<StaticParam> traitParams) {
        _ast = ast;
        _declaringTrait = declaringTrait;
        _traitParams = CollectUtil.makeList(IterUtil.map(traitParams, liftStaticParam));
        if (NodeUtil.getReturnType(_ast).isSome())
            putThunk(SimpleBox.make(NodeUtil.getReturnType(_ast)));
    }

    public FnDecl ast() { return _ast; }

    @Override
    public Span getSpan() { return NodeUtil.getSpan(_ast); }
    
    protected String mandatoryToString() {
        return "functionalMethod " + declaringTrait().toString() + "." + ast();
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
		return CollectUtil.makeList(IterUtil.compose(_traitParams, NodeUtil.getStaticParams(_ast)));
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
		return new FunctionalMethod((FnDecl)this.ast().accept(visitor), this._declaringTrait, this._traitParams);
	}
}
