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

import com.sun.fortress.interpreter.evaluator.BaseEnv;
import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.Visitor2;

public class WorseEnv extends BaseEnv {

	@Override
	public Appendable dump(Appendable a) throws IOException {
            throw new Error();

	}

	public Environment extend() {
            throw new Error();

	}

	public Environment extend(Environment additions) {
            throw new Error();

	}

	public Environment extendAt(HasAt x) {
            throw new Error();

	}

	@Override
	public Boolean getBoolNull(String str) {
            throw new Error();

	}

	public Number getIntNull(String s) {
            throw new Error();

	}

	@Override
	public Number getNatNull(String str) {
            throw new Error();

	}

	@Override
	public FType getTypeNull(String str) {
            throw new Error();

	}

	public FValue getValueRaw(String s) {
            throw new Error();

	}

	public void putBoolRaw(String str, Boolean f2) {
            throw new Error();

	}

	public void putIntRaw(String str, Number f2) {
            throw new Error();

	}

	public void putNatRaw(String str, Number f2) {
            throw new Error();

	}

	public void putTypeRaw(String str, FType f2) {
            throw new Error();

	}

	public void putValueRaw(String str, FValue f2) {
            throw new Error();

	}

	public void removeType(String s) {
            throw new Error();

	}

	public void removeVar(String name) {
            throw new Error();

	}

	public void visit(Visitor2<String, FType> vt, Visitor2<String, Number> vn,
			Visitor2<String, Number> vi, Visitor2<String, FValue> vv,
			Visitor2<String, Boolean> vb) {
            throw new Error();

	}

	public void visit(Visitor2<String, Object> nameCollector) {
            throw new Error();

	}

	public Iterable<String> youngestFrame() {
		throw new Error();
	}
	
	@Override
    public Environment getApiNull(String apiName) {
    	throw new Error();
    }
    
	@Override
    public void putApi(String apiName, Environment env) {
        throw new Error();
    }
    

}
