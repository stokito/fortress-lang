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

package com.sun.fortress.syntax_abstractions.util;

import java.util.LinkedList;
import java.util.List;

import com.sun.fortress.interpreter.evaluator.values.FObject;
import com.sun.fortress.interpreter.evaluator.values.FString;
import com.sun.fortress.interpreter.evaluator.values.FStringLiteral;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.QualifiedIdName;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.Span;

/*
 * Translate from a Fortress interpreter representation of Fortress AST to
 * Java representation of Fortress AST.
 * TODO: Implement cases for all the AST nodes. Do so by matching on their name.
 */
public class FortressObjectASTVisitor<T> {

	private static final String VALUE_FIELD_NAME = "val";

	public T dispatch(FValue value) {
		if (value instanceof FStringLiteral) {
			return (T) ((FStringLiteral) value).getString();
		}
		if (value instanceof FObject) {
			return dispatch((FObject) value);
		}
		throw new RuntimeException("Unexpected type of value: "+value.getClass());
	}

	private FValue getVal(FObject value) {
		return value.getSelfEnv().getValueNull(VALUE_FIELD_NAME);
	}

	/**
	 * The intention is that each AST node appears in the same order as in Fortress.ast
	 * @param value
	 * @return
	 */
	public T dispatch(FObject value) {
//		if (value.type().toString().equals("FnRef")) {
//			return dispatchFnRef(value);
//		} else if (value.type().toString().equals("LooseExpr")) {
//			return dispatchLooseJuxt(value);
//		} else 
//			if (value.type().toString().equals("TightExpr")) {
//			return dispatchTightJuxt(value);
//		} 
//			else if (value.type().toString().equals("OprExpr")) {
//			return dispatchOprExpr(value);
//		} else 
			if (value.type().toString().equals("IntLiteralExpr")) {
			return dispatchInteger(value);
		} else if (value.type().toString().equals("CharLiteralExpr")) {
			return dispatchChar(value);
		} else if (value.type().toString().equals("StringLiteralExpr")) {
			return dispatchString(value);
		} else if (value.type().toString().equals("VoidLiteralExpr")) {
			return dispatchVoid(value);
//		} 
//		else if (value.type().toString().equals("APIName")) {
//			return dispatchAPIName(value);
//		} else if (value.type().toString().equals("QualifiedIfName")) {
//			return dispatchQualifiedIdName(value);
//		} else if (value.type().toString().equals("QualifiedOpName")) {
//			return dispatchQualifiedOpName(value);
//		} else if (value.type().toString().equals("Id")) {
//			return dispatchId(value);
//		} else if (value.type().toString().equals("Op")) {
//			return dispatchOp(value);
//		} else if (value.type().toString().equals("Enclosing")) {
//			return dispatchEnclosing(value);
//		} else if (value.type().toString().equals("EnclosingFixity")) {
//			return dispatchEnclosingFixity(value);
		} else {
			throw new RuntimeException("NYI: "+value.type());
		}
	}

	private QualifiedIdName mkQFortressASTName(String name) {
		return NodeFactory.makeQualifiedIdName(SyntaxAbstractionUtil.FORTRESSAST, name);
	}
	
	private T dispatchFnRef(FObject value) {
		FValue v = getVal(value);
		System.err.println("LOOOOK HERE ---->"+v.getString()+" "+value.getClass()+value.getClass().getMethods());
		Span span = new Span();
		QualifiedIdName name = mkQFortressASTName("FnRef");
		List<StaticArg> staticArgs = new LinkedList<StaticArg>();
		return (T) NodeFactory.makeFnRef(span, name, staticArgs);
	}

	private T dispatchLooseJuxt(FObject value) {
		// TODO Auto-generated method stub
		return null;
	}

	private T dispatchTightJuxt(FObject value) {
		FValue v = getVal(value);
		System.err.println("LOOOOK HERE ---->"+v.getString()+" "+value.getClass()+value.getClass().getMethods());
		Span span = new Span();
		List<Expr> exprs = new LinkedList<Expr>();
		return (T) NodeFactory.makeTightJuxt(span, exprs );
	}

	private T dispatchOprExpr(FObject value) {
		// TODO Auto-generated method stub
		return null;
	}

	public T dispatchChar(FObject value) {
		return (T) NodeFactory.makeIntLiteralExpr(getVal(value).getInt());
	}

	public T dispatchInteger(FObject value) {
		return (T) NodeFactory.makeCharLiteralExpr(getVal(value).getChar());
	}

	public T dispatchString(FObject value) {
		return (T) NodeFactory.makeStringLiteralExpr(getVal(value).getString());
	}

	public T dispatchVoid(FObject value) {
		return (T) NodeFactory.makeVoidLiteralExpr();
	}
	
	private T dispatchAPIName(FObject value) {
		// TODO Auto-generated method stub
		return null;
	}

	private T dispatchQualifiedIdName(FObject value) {
		// TODO Auto-generated method stub
		return null;
	}

	private T dispatchQualifiedOpName(FObject value) {
		// TODO Auto-generated method stub
		return null;
	}

	private T dispatchId(FObject value) {
		// TODO Auto-generated method stub
		return null;
	}

	private T dispatchOp(FObject value) {
		// TODO Auto-generated method stub
		return null;
	}
	
	private T dispatchEnclosing(FObject value) {
		// TODO Auto-generated method stub
		return null;
	}

	private T dispatchEnclosingFixity(FObject value) {
		// TODO Auto-generated method stub
		return null;
	}

}
