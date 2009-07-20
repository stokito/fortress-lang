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

import com.sun.fortress.compiler.Types;
import com.sun.fortress.compiler.typechecker.StaticTypeReplacer;
import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.FnDecl;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeDepthFirstVisitor;
import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.Span;

import edu.rice.cs.plt.lambda.SimpleBox;
import edu.rice.cs.plt.lambda.Thunk;
import edu.rice.cs.plt.lambda.Lambda;
import edu.rice.cs.plt.tuple.Option;

public class DeclaredMethod extends Method {

    private final FnDecl _ast;
    private final Id _declaringTrait;

    public DeclaredMethod(FnDecl ast, Id declaringTrait) {
        _ast = ast;
        _declaringTrait = declaringTrait;
        if (NodeUtil.getReturnType(_ast).isSome())
            putThunk(SimpleBox.make(NodeUtil.getReturnType(_ast)));
    }

    /**
     * Copy another DeclaredMethod, performing a substitution with the visitor.
     */
    public DeclaredMethod(DeclaredMethod that, NodeUpdateVisitor visitor) {
        _ast = (FnDecl)that._ast.accept(visitor);
        _declaringTrait = that._declaringTrait;
        
        _thunk = that._thunk;
        _thunkVisitors = that._thunkVisitors;
        pushVisitor(visitor);
    }

    public FnDecl ast() { return _ast; }

    @Override
    public Span getSpan() { return NodeUtil.getSpan(_ast); }

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
		return NodeUtil.getStaticParams(_ast);
	}

	@Override
	public List<BaseType> thrownTypes() {
		if( NodeUtil.getThrowsClause(_ast).isNone() )
			return Collections.emptyList();
		else
			return Collections.unmodifiableList(NodeUtil.getThrowsClause(_ast).unwrap());
	}

	@Override
	public Method instantiate(List<StaticParam> params, List<StaticArg> args) {
        StaticTypeReplacer replacer = new StaticTypeReplacer(params,args);
		return new DeclaredMethod(this, replacer);
	}

	@Override
	public Id getDeclaringTrait() {
		return this._declaringTrait;
	}

	@Override
	public Functional acceptNodeUpdateVisitor(NodeUpdateVisitor visitor) {
		return new DeclaredMethod(this, visitor);
	}
}
