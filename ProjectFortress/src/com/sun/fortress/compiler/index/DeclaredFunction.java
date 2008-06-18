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

package com.sun.fortress.compiler.index;

import java.util.Collections;
import java.util.List;

import com.sun.fortress.nodes.ArrowType;
import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.FnAbsDeclOrDecl;
import com.sun.fortress.nodes.FnDef;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeDepthFirstVisitor;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.useful.NI;

import edu.rice.cs.plt.tuple.Option;

public class DeclaredFunction extends Function {
    private final FnAbsDeclOrDecl _ast;

    public DeclaredFunction(FnAbsDeclOrDecl ast) { _ast = ast; }

    public FnAbsDeclOrDecl ast() { return _ast; }

	@Override
	public Option<Expr> body() {
		return _ast.accept(new NodeDepthFirstVisitor<Option<Expr>>(){
			@Override
			public Option<Expr> defaultCase(Node that) {
				return Option.none();
			}
			@Override
			public Option<Expr> forFnDef(FnDef that) {
				return Option.some(that.getBody());
			}
		});
	}

	@Override
	public List<Param> parameters() {
		return _ast.getParams();
	}

	@Override
	public List<StaticParam> staticParameters() {
		return _ast.getStaticParams();
	}

	@Override
	public Iterable<BaseType> thrownTypes() {
		if( _ast.getThrowsClause().isNone() )
			return Collections.emptyList();
		else
			return Collections.unmodifiableList(_ast.getThrowsClause().unwrap());
	}

	@Override
	public Functional instantiate(List<StaticParam> params, List<StaticArg> args) {
		// TODO Auto-generated method stub
		return NI.nyi();
	}

	@Override
	public Type getReturnType() {
		return _ast.getReturnType().unwrap();
	}

}
