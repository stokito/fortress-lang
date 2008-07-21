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

package com.sun.fortress.interpreter.evaluator.values;
import static com.sun.fortress.exceptions.InterpreterBug.bug;
import static com.sun.fortress.exceptions.ProgramError.errorMsg;

import java.io.BufferedWriter;

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
        bug(errorMsg("seqv on FAsIf ",this," and ",v));
        return false;
    }
}
