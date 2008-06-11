package com.sun.fortress.interpreter.env;

import java.io.IOException;
import java.util.Iterator;

import com.sun.fortress.interpreter.evaluator.BaseEnv;
import com.sun.fortress.interpreter.evaluator.Declaration;
import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.scopes.SApi;
import com.sun.fortress.interpreter.evaluator.scopes.SComponent;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.values.Closure;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.Visitor2;

public class WorseEnv extends BaseEnv {

	@Override
	public void debugPrint(String debugString) {
		// TODO Auto-generated method stub

	}

	@Override
	public Appendable dump(Appendable a) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SApi getApiNull(String str) {
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

	@Override
	public SComponent getComponentNull(APIName d) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Declaration getDeclNull(String str) {
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

	@Override
	public FValue getValueNull(String str) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FType getVarTypeNull(String str) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterator<String> iterator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void putValueUnconditionally(String str, FValue v) {
		// TODO Auto-generated method stub

	}

	public void bless() {
		// TODO Auto-generated method stub

	}

	public Environment extend(Environment additions) {
		// TODO Auto-generated method stub
		return null;
	}

	public Environment extend() {
		// TODO Auto-generated method stub
		return null;
	}

	public Environment extendAt(HasAt x) {
		// TODO Auto-generated method stub
		return null;
	}

	public HasAt getAt() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean getBlessed() {
		// TODO Auto-generated method stub
		return false;
	}

	public Closure getClosure(String toBeRun) {
		// TODO Auto-generated method stub
		return null;
	}

	public Number getIntNull(String s) {
		// TODO Auto-generated method stub
		return null;
	}

	public FValue getValueRaw(String s) {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean hasType(String str) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean hasValue(String str) {
		// TODO Auto-generated method stub
		return false;
	}

	public Environment installPrimitives() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isTopLevel() {
		// TODO Auto-generated method stub
		return false;
	}

	public void noteName(String s) {
		// TODO Auto-generated method stub

	}

	public void putApi(String s, SApi api) {
		// TODO Auto-generated method stub

	}


	public void putBool(String str, Boolean f2) {
		// TODO Auto-generated method stub

	}


	public void putComponent(String name, SComponent comp) {
		// TODO Auto-generated method stub

	}

	public void putDecl(String str, Declaration f2) {
		// TODO Auto-generated method stub

	}

	public void putFunctionalMethodInstance(String fndodname, FValue cl) {
		// TODO Auto-generated method stub

	}

	public void putInt(String add_as, Number cnnf) {
		// TODO Auto-generated method stub

	}

	public void putNat(String str, Number f2) {
		// TODO Auto-generated method stub

	}

	public void putType(String str, FType f2) {
		// TODO Auto-generated method stub

	}


	public void putValue(String str, FValue f2) {
		// TODO Auto-generated method stub

	}


	public void putValueNoShadowFn(String fndodname, FValue cl) {
		// TODO Auto-generated method stub

	}

	public void putValueShadowFn(String fname, FValue cl) {
		// TODO Auto-generated method stub

	}

	public void putValueUnconditionally(String name, FValue value, FType ft) {
		// TODO Auto-generated method stub

	}

	public void putVariable(String str, FValue f2, FType ft) {
		// TODO Auto-generated method stub

	}

	public void putVariable(String s, FValue value) {
		// TODO Auto-generated method stub

	}

	public void putVariable(String string, FType fvt) {
		// TODO Auto-generated method stub

	}

	public void putVariablePlaceholder(String sname) {
		// TODO Auto-generated method stub

	}

	public void removeType(String s) {
		// TODO Auto-generated method stub

	}

	public void removeVar(String name) {
		// TODO Auto-generated method stub

	}

	public void setTopLevel() {
		// TODO Auto-generated method stub

	}

	public void storeType(HasAt x, String sname, FType ft) {
		// TODO Auto-generated method stub

	}

	public void visit(Visitor2<String, Object> nameCollector) {
		// TODO Auto-generated method stub

	}

	public void visit(Visitor2<String, FType> vt, Visitor2<String, Number> vn,
			Visitor2<String, Number> vi, Visitor2<String, FValue> vv,
			Visitor2<String, Boolean> vb) {
		// TODO Auto-generated method stub

	}

	public Iterable<String> youngestFrame() {
		// TODO Auto-generated method stub
		return null;
	}

}
