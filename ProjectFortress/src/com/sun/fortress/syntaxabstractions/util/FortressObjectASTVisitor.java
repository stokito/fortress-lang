package com.sun.fortress.syntaxabstractions.util;

import com.sun.fortress.interpreter.evaluator.values.FObject;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.Literal;
import com.sun.fortress.nodes_util.NodeFactory;

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
		if (value.type().toString().equals("IntLiteral")) {
			return dispatchInteger(value);
		} else if (value.type().toString().equals("CharLiteral")) {
			return dispatchChar(value);
		} else if (value.type().toString().equals("StringLiteral")) {
			return dispatchString(value);
		} else if (value.type().toString().equals("VoidLiteral")) {
			return dispatchVoid(value);
		} else { 
			throw new RuntimeException("NYI: "+value.type());
		}
	}

	public T dispatchChar(FObject value) {
		return (T) NodeFactory.makeIntLiteral(getVal(value).getInt());
	}
	
	public T dispatchInteger(FObject value) {
		return (T) NodeFactory.makeCharLiteral(getVal(value).getChar());
	}
	
	public T dispatchString(FObject value) {
		return (T) NodeFactory.makeStringLiteral(getVal(value).getString());
	}
	
	public T dispatchVoid(FObject value) {
		return (T) NodeFactory.makeVoidLiteral();
	}	
}
