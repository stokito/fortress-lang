package com.sun.fortress.syntax_abstractions.util;

import java.util.LinkedList;
import java.util.List;

import xtc.util.Pair;

import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.AsExpr;
import com.sun.fortress.nodes.AsIfExpr;
import com.sun.fortress.nodes.Do;
import com.sun.fortress.nodes.DoFront;
import com.sun.fortress.nodes.Enclosing;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.FnRef;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.InstantiatedType;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeDepthFirstVisitor;
import com.sun.fortress.nodes.Op;
import com.sun.fortress.nodes.OpExpr;
import com.sun.fortress.nodes.OpName;
import com.sun.fortress.nodes.OpRef;
import com.sun.fortress.nodes.OprExpr;
import com.sun.fortress.nodes.QualifiedIdName;
import com.sun.fortress.nodes.QualifiedOpName;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.StringLiteralExpr;
import com.sun.fortress.nodes.TightJuxt;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.TypeArg;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.parser_util.FortressUtil;

import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;

public class JavaASTToFortressAST extends NodeDepthFirstVisitor<Expr> {

	private Span _span;

	public JavaASTToFortressAST(Span span) {
		this._span = span;
	}

	public Expr dispatch(Object value, Option<Type> option) {
		// It is eiter the result of a optional
		if (option.isSome()) {
			if (Option.unwrap(option) instanceof InstantiatedType) {
				InstantiatedType it = (InstantiatedType) Option.unwrap(option);
				if (it.getName().equals(NodeFactory.makeQualifiedIdName("FortressLibrary","Maybe"))) {
					return handleOption(value, it);
				}
				// or the result of a repetition
				if ((value instanceof Pair) && 
					 it.getName().equals(NodeFactory.makeQualifiedIdName("ArrayList","List"))) {
					return handleRepetition((Pair) value, it);
				}
			}
		}
		// It is a piece of AST
		if (value instanceof Node) {
			return handleNode((Node) value);
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
		if (value instanceof Character) {
			return NodeFactory.makeCharLiteralExpr((Character)value);
		}

		throw new RuntimeException("Wrong type: "+value.getClass());
	}

	private Expr handleNode(Node value) {
		return value.accept(this);
	}

	private Expr handleOption(Object value, InstantiatedType it) {
		if (value == null) {
			return SyntaxAbstractionUtil.makeObjectInstantiation(this._span, SyntaxAbstractionUtil.FORTRESSLIBRARY, SyntaxAbstractionUtil.NOTHING, Option.<Expr>none(), it.getArgs());
		}
		Option<Expr> args = Option.some(handleNode((Node)value));
		return SyntaxAbstractionUtil.makeObjectInstantiation(this._span, SyntaxAbstractionUtil.FORTRESSLIBRARY, SyntaxAbstractionUtil.JUST, args , it.getArgs());
	}
	
	private Expr handleRepetition(Pair value, InstantiatedType type) {
		if (value.list().isEmpty()) {
			return SyntaxAbstractionUtil.makeObjectInstantiation(this._span, "ArrayList", "emptyList", Option.<Expr>none(), type.getArgs());
		}
		List<QualifiedOpName> ops = new LinkedList<QualifiedOpName>();
		ops.add(NodeFactory.makeQualifiedEncloserOpName(this._span));
		List<Expr> args = new LinkedList<Expr>();
		boolean first = true;
		for (Object o: value.list()) {
			Node n = (Node) o;
			JavaASTToFortressAST jaTofss = new JavaASTToFortressAST(n.getSpan());
			Expr e = n.accept(jaTofss);
			if (first) {
				Type t = IterUtil.first(type.getArgs());
				e = new AsIfExpr(e, t);
				first = false;
			}
			args.add(e);
		}
		return new OprExpr(new OpRef(ops), args);
	}

	@Override
	public Expr forStringLiteralExpr(StringLiteralExpr that) {
		return SyntaxAbstractionUtil.makeObjectInstantiation(this._span, "FortressAst", "StringLiteralExpr", Option.<Expr>some(that));
	}


}
