/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.env;

import com.sun.fortress.interpreter.evaluator.BaseEnv;
import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.Visitor2;

import java.io.IOException;

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

    public void visit(Visitor2<String, FType> vt,
                      Visitor2<String, Number> vn,
                      Visitor2<String, Number> vi,
                      Visitor2<String, FValue> vv,
                      Visitor2<String, Boolean> vb) {
        throw new Error();
    }

    public void visit(Visitor2<String, Object> nameCollector) {
        throw new Error();
    }

}
