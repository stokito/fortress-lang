package com.sun.fortress.syntax_abstractions.util;

import java.util.LinkedList;
import java.util.List;

import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.FnRef;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeDepthFirstVisitor;
import com.sun.fortress.nodes.QualifiedIdName;
import com.sun.fortress.nodes.StringLiteralExpr;
import com.sun.fortress.nodes.TightJuxt;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.Span;

import edu.rice.cs.plt.tuple.Option;

public class JavaASTToFortressAST extends NodeDepthFirstVisitor<Expr> {

	private Span _span;

	public JavaASTToFortressAST(Span span) {
		this._span = span;
	}

	public Expr dispatch(Object value) {
		if (value == null) {
//			System.err.println("Dispatch: "+value);
			return makeObjectInstantiation("FortressLibrary", "Nothing", Option.<Expr>none());
		}
//		System.err.println("Dispatch: "+value.getClass()+" "+value);
		if (value instanceof Node) {
			return ((Node) value).accept(this);
		}
		// it is one of the primitive types
		if (value instanceof String) {
			return NodeFactory.makeStringLiteralExpr((String)value);
		}
		if (value instanceof Integer) {
			return NodeFactory.makeIntLiteralExpr((Integer)value);
		}
		if (value instanceof Short) {
			return NodeFactory.makeIntLiteralExpr((Short)value);
		}
		if (value instanceof Byte) {
			return NodeFactory.makeIntLiteralExpr((Byte)value);
		}
		if (value instanceof Long) {
			return NodeFactory.makeIntLiteralExpr((Long)value);
		}
		return NodeFactory.makeCharLiteralExpr((Character)value);
	}

	@Override
	public Expr forStringLiteralExpr(StringLiteralExpr that) {
		return makeObjectInstantiation("FortressAst", "StringLiteralExpr", Option.<Expr>some(that));
	}

	private Expr makeObjectInstantiation(String apiName, String objectName, Option<Expr> arg) {
		List<Expr> exprs = new LinkedList<Expr>();
		List<Id> ids = new LinkedList<Id>();
		ids.add(NodeFactory.makeId(this._span, apiName));
		APIName api = NodeFactory.makeAPIName(this._span, ids);
		Id typeName = NodeFactory.makeId(this._span, objectName);
		QualifiedIdName name = NodeFactory.makeQualifiedIdName(this._span, api, typeName);
		exprs.add(NodeFactory.makeFnRef(this._span, name));
		if (arg.isSome()) {
			exprs.add(Option.unwrap(arg));
		}
		return NodeFactory.makeTightJuxt(this._span, exprs);
	}

}
