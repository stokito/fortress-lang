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

package com.sun.fortress.interpreter.env;

import java.io.IOException;
import java.util.Iterator;

import com.sun.fortress.interpreter.evaluator.BaseEnv;
import com.sun.fortress.interpreter.evaluator.Declaration;
import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.scopes.SApi;
import com.sun.fortress.interpreter.evaluator.scopes.SComponent;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.Visitor2;

public class WorseEnv extends BaseEnv {

	public void putBoolRaw(String str, Boolean f2) {
        // TODO Auto-generated method stub
        
    }

    public void putIntRaw(String str, Number f2) {
        // TODO Auto-generated method stub
        
    }

    public void putNatRaw(String str, Number f2) {
        // TODO Auto-generated method stub
        
    }

    public void putValueRaw(String str, FValue f2) {
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

	public Number getIntNull(String s) {
		// TODO Auto-generated method stub
		return null;
	}

	public FValue getValueRaw(String s) {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean hasValue(String str) {
		// TODO Auto-generated method stub
		return false;
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

	public void removeType(String s) {
		// TODO Auto-generated method stub

	}

	public void removeVar(String name) {
		// TODO Auto-generated method stub

	}

	public void setTopLevel() {
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
