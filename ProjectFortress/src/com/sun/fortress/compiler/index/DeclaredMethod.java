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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.sun.fortress.compiler.typechecker.StaticTypeReplacer;
import com.sun.fortress.nodes.ArrowType;
import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.FnAbsDeclOrDecl;
import com.sun.fortress.nodes.FnDef;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeDepthFirstVisitor;
import com.sun.fortress.nodes.NormalParam;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.TupleType;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.TypeArg;
import com.sun.fortress.nodes.VarargsParam;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.useful.NI;

import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.lambda.Lambda;
import edu.rice.cs.plt.tuple.Option;

public class DeclaredMethod extends Method {

    private final FnAbsDeclOrDecl _ast;
    private final Id _declaringTrait;

    public DeclaredMethod(FnAbsDeclOrDecl ast, Id declaringTrait) {
        _ast = ast;
        _declaringTrait = declaringTrait;
    }

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
	public ArrowType instantiatedType(List<StaticArg> args) {
		return this.instantiate(args).asArrowType();

	}

	@Override
	public ArrowType asArrowType() {
		List<Type> types=IterUtil.asList(IterUtil.map(_ast.getParams(), new Lambda<Param,Type>(){
			public Type value(Param arg0) {
				return (arg0 instanceof NormalParam) ? ((NormalParam)arg0).getType().unwrap() : 
					((VarargsParam)arg0).getType();
			}}));
		TupleType domain = NodeFactory.makeTupleType(types);
		return NodeFactory.makeArrowType(new Span(), domain, _ast.getReturnType().unwrap());
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
		if( _ast.getThrowsClause().isSome() )
			return Collections.emptyList();
		else
			return Collections.unmodifiableList(_ast.getThrowsClause().unwrap());
	}

	@Override
	public Functional instantiate(List<StaticArg> args) {
		FnAbsDeclOrDecl replaced_decl = 
			(FnAbsDeclOrDecl)_ast.accept(new StaticTypeReplacer(this.staticParameters(),args));
		return new DeclaredMethod(replaced_decl,_declaringTrait);
	}

	@Override
	public Type getReturnType() {
		return _ast.getReturnType().unwrap();
	}
	
	
	
}
