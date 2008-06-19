package com.sun.fortress.interpreter.env;

import java.io.IOException;

import com.sun.fortress.interpreter.evaluator.BaseEnv;
import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.scopes.SApi;
import com.sun.fortress.interpreter.evaluator.scopes.SComponent;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.Visitor2;

public class WorseEnv extends BaseEnv {

	@Override
	public Appendable dump(Appendable a) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	public Environment extend() {
		// TODO Auto-generated method stub
		return null;
	}

	public Environment extend(Environment additions) {
		// TODO Auto-generated method stub
		return null;
	}

	public Environment extendAt(HasAt x) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SApi getApiNull(String str) {
		// TODO Auto-generated method stub
		return null;
	}

	public HasAt getAt() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Boolean getBoolNull(String str) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SComponent getComponentNull(String str) {
		// TODO Auto-generated method stub
		return null;
	}

	public Number getIntNull(String s) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Number getNatNull(String str) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FType getTypeNull(String str) {
		// TODO Auto-generated method stub
		return null;
	}

	public FValue getValueRaw(String s) {
		// TODO Auto-generated method stub
		return null;
	}

	public void putApi(String s, SApi api) {
		// TODO Auto-generated method stub

	}

	public void putBoolRaw(String str, Boolean f2) {
		// TODO Auto-generated method stub

	}

	public void putComponent(String name, SComponent comp) {
		// TODO Auto-generated method stub

	}

	public void putIntRaw(String str, Number f2) {
		// TODO Auto-generated method stub

	}

	public void putNatRaw(String str, Number f2) {
		// TODO Auto-generated method stub

	}

	public void putTypeRaw(String str, FType f2) {
		// TODO Auto-generated method stub

	}

	public void putValueRaw(String str, FValue f2) {
		// TODO Auto-generated method stub

	}

	public void removeType(String s) {
		// TODO Auto-generated method stub

	}

	public void removeVar(String name) {
		// TODO Auto-generated method stub

	}

	public void visit(Visitor2<String, FType> vt, Visitor2<String, Number> vn,
			Visitor2<String, Number> vi, Visitor2<String, FValue> vv,
			Visitor2<String, Boolean> vb) {
		// TODO Auto-generated method stub

	}

	public void visit(Visitor2<String, Object> nameCollector) {
		// TODO Auto-generated method stub

	}

	public Iterable<String> youngestFrame() {
		// TODO Auto-generated method stub
		return null;
	}

}
