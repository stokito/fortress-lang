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

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.values.FInt;
import com.sun.fortress.interpreter.evaluator.values.FObject;
import com.sun.fortress.interpreter.evaluator.values.FString;
import com.sun.fortress.interpreter.evaluator.values.FStringLiteral;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.FVoid;
import com.sun.fortress.interpreter.evaluator.values.Method;
import com.sun.fortress.interpreter.glue.prim.PrimImmutableArray;
import com.sun.fortress.interpreter.glue.prim.PrimImmutableArray.PrimImmutableArrayObject;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Enclosing;
import com.sun.fortress.nodes.EnclosingFixity;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.Fixity;
import com.sun.fortress.nodes.FnRef;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.IntLiteralExpr;
import com.sun.fortress.nodes.LooseJuxt;
import com.sun.fortress.nodes.Modifier;
import com.sun.fortress.nodes.Op;
import com.sun.fortress.nodes.OpName;
import com.sun.fortress.nodes.OpRef;
import com.sun.fortress.nodes.OprExpr;
import com.sun.fortress.nodes.QualifiedIdName;
import com.sun.fortress.nodes.QualifiedOpName;
import com.sun.fortress.nodes.SimpleName;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.VoidLiteralExpr;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.SourceLoc;
import com.sun.fortress.nodes_util.Span;

import edu.rice.cs.plt.tuple.Option;

/*
 * Translate from a Fortress interpreter representation of Fortress AST to
 * Java representation of Fortress AST.
 * TODO: Implement cases for all the AST nodes. Do so by matching on their name.
 */
public class FortressObjectASTVisitor<T> {

	private static final String VALUE_FIELD_NAME = "val";
	private Span span;

	public FortressObjectASTVisitor(Span span) {
		this.span = span;
	}

	public T dispatch(FValue value) {
		if (value instanceof FStringLiteral) {
			return (T) ((FStringLiteral) value).getString();
		}
		if (value instanceof FObject) {
			return dispatch((FObject) value);
		}
		if (null == value) {
			throw new RuntimeException("Unexpected value was null");
		}
		throw new RuntimeException("Unexpected type of value: "+value.getClass());
	}

	public <V> List<V> dispatchList(FObject fObject) {
		if (fObject.type().getName().equals("ArrayList")) {
			List<V> ls = new LinkedList<V>();
			FValue firstUnused = getField(fObject, "firstUnused");
			FValue firstUsed = getField(fObject, "firstUsed");
			int size = firstUnused.getInt() - firstUsed.getInt();
			PrimImmutableArrayObject a = (PrimImmutableArrayObject) getField(fObject, "underlying");
			for (int inx=0;inx<size;inx++) {
				FValue elm = a.get(inx);
				ls.add(new FortressObjectASTVisitor<V>(this.span).dispatch(elm));
			}
			return ls;
		}
		throw new RuntimeException("Unexpected list type: "+fObject.type().getName());
	}

	private <V> Option<V> dispatchMaybe(FObject fObject) {
		if (fObject.type().getName().equals("Just")) {
			FValue v = getField(fObject, "x");			
			V value = new FortressObjectASTVisitor<V>(this.span).dispatch(v);
			return Option.<V>some(value);
		}
		if (fObject.type().getName().equals("Nothing")) {
			return Option.<V>none();
		}
		throw new RuntimeException("Unexpected Maybe type: "+fObject.type().getName());
	}
	
	/**
	 * The intention is that each AST node appears in the same order as in Fortress.ast
	 * @param value
	 * @return
	 */
	public T dispatch(FObject value) {
		if (value.type().toString().equals("FnDef")) {
			return dispatchFnDef(value);
		} else if (value.type().toString().equals("FnRef")) {
			return dispatchFnRef(value);
		} else if (value.type().toString().equals("OpRef")) {
			return dispatchOpRef(value);
		} else if (value.type().toString().equals("LooseJuxt")) {
			return dispatchLooseJuxt(value);
		} else if (value.type().toString().equals("TightJuxt")) {
			return dispatchTightJuxt(value);
		} else if (value.type().toString().equals("OprExpr")) {
			return dispatchOprExpr(value);
		} else if (value.type().toString().equals("IntLiteralExpr")) {
			return dispatchInteger(value);
		} else if (value.type().toString().equals("CharLiteralExpr")) {
			return dispatchChar(value);
		} else if (value.type().toString().equals("StringLiteralExpr")) {
			return dispatchString(value);
		} else if (value.type().toString().equals("VoidLiteralExpr")) {
			return dispatchVoid(value);
		} else if (value.type().toString().equals("APIName")) {
			return dispatchAPIName(value);
		} else if (value.type().toString().equals("QualifiedIdName")) {
			return dispatchQualifiedIdName(value);
		} else if (value.type().toString().equals("QualifiedOpName")) {
			return dispatchQualifiedOpName(value);
		} else if (value.type().toString().equals("Id")) {
			return dispatchId(value);
		} else if (value.type().toString().equals("Op")) {
			return dispatchOp(value);
		} else if (value.type().toString().equals("Enclosing")) {
			return dispatchEnclosing(value);
		} else if (value.type().toString().equals("EnclosingFixity")) {
			return dispatchEnclosingFixity(value);
		} else {
			throw new RuntimeException("NYI: "+value.type());
		}
	}

	/*
	 * mods:List[\Modifier\],
      name:SimpleName,
      staticParams:List[\StaticParam\],
      params:List[\Param\],
      returnType:Maybe[\Type\],
      throwsClause:Maybe[\List[\TraitType\]\],
      whereClauses:List[\WhereClause\],
      acontract:Contract,
      selfName:String,
      body:Expr
	 */
	private T dispatchFnDef(FObject value) {
		FValue v1 = getField(value, "mods");
		List<Modifier> mods = dispatchList((FObject)v1);
		FValue v2 = getField(value, "name");
		SimpleName name = new FortressObjectASTVisitor<SimpleName>(this.span).dispatch((FObject)v2);
		FValue v3 = getField(value, "staticParams");
		List<StaticParam> staticParams = dispatchList((FObject)v3);
//		return (T) new FnDef(this.span, mods, name, staticParams, params, returnType,
//					 		 throwsClause, whereClause, aconstract, selfName, body);
		return (T) null;
	}

	private QualifiedIdName mkQFortressASTName(String name) {
		return NodeFactory.makeQualifiedIdName(SyntaxAbstractionUtil.FORTRESSAST, name);
	}
	
	private T dispatchFnRef(FObject value) {
		FValue v1 = getField(value, "fns");
		List<QualifiedIdName> fns = dispatchList((FObject)v1);
		FValue v2 = getField(value, "staticArgs");
		List<StaticArg> staticArgs = dispatchList((FObject)v2);
		return (T) new FnRef(this.span, fns, staticArgs);
	}

	private T dispatchOpRef(FObject value) {
		FValue v1 = getField(value, "ops");
		List<QualifiedOpName> ops = dispatchList((FObject)v1);
		FValue v2 = getField(value, "staticArgs");
		List<StaticArg> staticArgs = dispatchList((FObject)v2);
		return (T) new OpRef(this.span, ops, staticArgs);
	}
	
	private T dispatchLooseJuxt(FObject value) {
		FValue v = getField(value, "exprs");
		List<Expr> exprs = dispatchList((FObject)v);
		return (T) new LooseJuxt(this.span, exprs);
	}

	private T dispatchTightJuxt(FObject value) {
		FValue v = getField(value, "exprs");
		List<Expr> exprs = dispatchList((FObject)v);
		return (T) NodeFactory.makeTightJuxt(this.span, exprs);
	}

	private T dispatchOprExpr(FObject value) {
		FValue v1 = getField(value, "op");
		OpRef opRef = new FortressObjectASTVisitor<OpRef>(this.span).dispatch((FObject)v1);
		FValue v2 = getField(value, "args");
		List<Expr> exprs = dispatchList((FObject) v2);
		return (T) new OprExpr(this.span, opRef, exprs);
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
		return (T) new VoidLiteralExpr(this.span);
	}
	
	private T dispatchAPIName(FObject value) {
		FValue v1 = getField(value, "ids");
		List<Id> ids = dispatchList((FObject)v1);
		return (T) new APIName(this.span, ids);
	}

	private T dispatchQualifiedIdName(FObject value) {
		FValue v1 = getField(value, "apiName");
		Option<APIName> apiName = dispatchMaybe((FObject)v1);
		FValue v2 = getField(value, "name");
		Id id = new FortressObjectASTVisitor<Id>(this.span).dispatch((FObject)v2);
		return (T) new QualifiedIdName(this.span, apiName, id);
	}

	private T dispatchQualifiedOpName(FObject value) {
		FValue v1 = getField(value, "apiName");
		Option<APIName> apiName = dispatchMaybe((FObject)v1);
		FValue v2 = getField(value, "name");
		OpName op = new FortressObjectASTVisitor<OpName>(this.span).dispatch((FObject)v2);
		return (T) new QualifiedOpName(this.span, apiName, op);
	}

	private T dispatchId(FObject value) {
		FValue v1 = getField(value, "text");
		return (T) new Id(this.span, v1.getString());
	}

	private T dispatchOp(FObject value) {
		FValue v1 = getField(value, "text");
		FValue v2 = getField(value, "fixity");
		Option<Fixity> fixity = dispatchMaybe((FObject) v2);
		return (T) new Op(this.span, v1.getString(), fixity);
	}
	
	private T dispatchEnclosing(FObject value) {
		FValue v1 = getField(value, "open");
		Op op1 = new FortressObjectASTVisitor<Op>(this.span).dispatch((FObject)v1);
		FValue v2 = getField(value, "close");
		Op op2 = new FortressObjectASTVisitor<Op>(this.span).dispatch((FObject)v2);
		return (T) new Enclosing(this.span, op1, op2);
	}

	private T dispatchEnclosingFixity(FObject value) {
		return (T) new EnclosingFixity(this.span);
	}

	private FValue getVal(FObject value) {
		return value.getSelfEnv().getValueNull(VALUE_FIELD_NAME);
	}
	
	private FValue getField(FObject fObject, String field) {
		return fObject.getSelfEnv().getValueNull(field);
	}

}
