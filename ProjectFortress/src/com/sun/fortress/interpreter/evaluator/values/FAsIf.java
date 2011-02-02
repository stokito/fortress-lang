/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.values;

import static com.sun.fortress.exceptions.InterpreterBug.bug;
import static com.sun.fortress.exceptions.ProgramError.errorMsg;
import com.sun.fortress.interpreter.evaluator.types.FType;

public class FAsIf extends FConstructedValue {
    private final FValue fvalue;

    public FAsIf(FValue val, FType ty) {
        super(ty);
        this.fvalue = val;
    }

    public FValue getValue() {
        return fvalue;
    }

    public String toString() {
        return "(" + fvalue.toString() + " asif " + type().toString() + ")";
    }

    public String getString() {
        return "(" + fvalue.getString() + " asif " + type().getName() + ")";
    }

    public int getInt() {
        return fvalue.getInt();
    }

    public long getLong() {
        return fvalue.getLong();
    }

    public int getNN32() {
        return fvalue.getNN32();
    }

    public long getNN64() {
        return fvalue.getNN64();
    }

    public double getFloat() {
        return fvalue.getFloat();
    }

    public int getChar() {
        return fvalue.getChar();
    }

    public boolean seqv(FValue v) {
        bug(errorMsg("seqv on FAsIf ", this, " and ", v));
        return false;
    }
}
