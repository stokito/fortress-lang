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
import java.io.BufferedWriter;
import com.sun.fortress.interpreter.evaluator.types.FType;

public class FAsIf extends FValue {
    private final FValue fvalue;
    private final FType ftype;

    public FAsIf(FValue val, FType ty) {
        this.fvalue = val;
        this.ftype = ty;
    }

    public FValue getValue() {
        return fvalue;
    }

    public String toString() {
        return "(" + fvalue.toString() + " asif " + ftype.toString() + ")";
    }

    public String getString() {
        return "(" + fvalue.getString() + " asif " + ftype.getName() + ")";
    }

    public FType type() {
        return ftype;
    }

    public BufferedWriter getBufferedWriter() {
        return fvalue.getBufferedWriter();
    }
    public int getInt() {
        return fvalue.getInt();
    }
    public long getLong() {
        return fvalue.getLong();
    }
    public double getFloat() {
        return fvalue.getFloat();
    }
    public char getChar() {
        return fvalue.getChar();
    }

}
