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

package com.sun.fortress.interpreter.evaluator.values;
import java.util.ArrayList;
import java.util.List;

import com.sun.fortress.interpreter.evaluator.InterpreterError;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.useful.NI;


public class FValue {
    //   public static final FValue ZERO = new FInt(0);
    //  public static final FValue ONE = new FInt(1);

    public String toString() {
        return getClass().getSimpleName() + " " + getString();
    }
    public String getString() { return "No String Representation Implemented for " + getClass().getSimpleName();}
    public FType type() { return ftype; }
    public int getInt() { throw new InterpreterError("getInt not implemented for "  + getClass().getSimpleName());}
    public long getLong() { throw new InterpreterError("getLong not implemented for "  + getClass().getSimpleName());}
    public double getFloat() { throw new InterpreterError("getFloat not implemented for "  + getClass().getSimpleName());}
    /**
     * @param ftype The ftype to set.
     */
    public void setFtype(FType ftype) {
        if (this.ftype != null)
            throw new IllegalStateException("Cannot set twice");
        this.ftype = ftype;
    }

    public void setFtypeUnconditionally(FType ftype) {
        this.ftype = ftype;
    }

    private FType ftype;

    // map "select type"
    static protected List<FType> typeListFromParameters(List<Parameter> params) {
        ArrayList<FType> al = new ArrayList<FType>(params.size());
        for (Parameter p : params) al.add(p.param_type);
        return al;
    }

    // map "select type"
    static protected List<FType> typeListFromValues(List<FValue> params) {
        ArrayList<FType> al = new ArrayList<FType>(params.size());
        for (FValue p : params) al.add(p.ftype);
        return al;
    }


}
