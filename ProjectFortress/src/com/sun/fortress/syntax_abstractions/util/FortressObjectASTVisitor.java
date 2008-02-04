/*******************************************************************************
    Copyright 2007 Sun Microsystems, Inc.,
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

import com.sun.fortress.interpreter.evaluator.values.FObject;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.nodes_util.NodeFactory;

/*
 * Translate from a Fortress interpreter representation of Fortress AST to
 * Java representation of Fortress AST.
 */
public class FortressObjectASTVisitor<T> {

	private static final String VALUE_FIELD_NAME = "val";

	public T dispatch(FValue value) {
		if (value instanceof FObject) {
			return dispatch((FObject) value);
		}
		throw new RuntimeException("Unexpected type of value: "+value.getClass());
	}

	private FValue getVal(FObject value) {
		return value.getSelfEnv().getValueNull(VALUE_FIELD_NAME);
	}

	public T dispatch(FObject value) {
		if (value.type().toString().equals("IntLiteralExpr")) {
			return dispatchInteger(value);
		} else if (value.type().toString().equals("CharLiteralExpr")) {
			return dispatchChar(value);
		} else if (value.type().toString().equals("StringLiteralExpr")) {
			return dispatchString(value);
		} else if (value.type().toString().equals("VoidLiteralExpr")) {
			return dispatchVoid(value);
		} else {
			throw new RuntimeException("NYI: "+value.type());
		}
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
}
