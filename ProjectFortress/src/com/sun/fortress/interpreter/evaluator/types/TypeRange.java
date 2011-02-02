/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.types;

import com.sun.fortress.exceptions.InterpreterBug;
import static com.sun.fortress.exceptions.InterpreterBug.bug;
import static com.sun.fortress.exceptions.ProgramError.errorMsg;
import com.sun.fortress.interpreter.evaluator.values.FInt;
import com.sun.fortress.interpreter.evaluator.values.FIntLiteral;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.useful.MagicNumbers;
import com.sun.fortress.useful.NI;

import java.util.ArrayList;
import java.util.List;

public class TypeRange {
    FTypeNat base;
    FTypeNat size;

    public FTypeNat getBase() {
        return base;
    }

    public FTypeNat getSize() {
        return size;
    }

    public TypeRange(FTypeNat base, FTypeNat size) {
        this.base = base;
        this.size = size;
    }

    public TypeRange(long base, long size) {
        this.base = IntNat.make(base);
        this.size = IntNat.make(size);
    }

    public static TypeRange make(FValue v) {
        if (v instanceof FInt) {
            FInt i = (FInt) v;
            return new TypeRange(0, i.getInt());
        } else if (v instanceof FIntLiteral) {
            FIntLiteral i = (FIntLiteral) v;
            return new TypeRange(0, i.getInt());
        } else return NI.ni();
    }

    public Long getEvaluatedBase() {
        return evaluatedNat(base);
    }

    public Long getEvaluatedSize() {
        return evaluatedNat(size);
    }

    private static Long evaluatedNat(FTypeNat x) throws InterpreterBug {
        if (x instanceof IntNat) {
            return Long.valueOf(((IntNat) x).getValue());
        }
        return bug(errorMsg("Expected fully evaluated nat parameter, found ", x));
    }

    public boolean compatible(TypeRange j) {
        return size.equals(j.size);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object arg0) {
        if (arg0 instanceof TypeRange) {
            TypeRange tr = (TypeRange) arg0;
            return compatible(tr) && base.equals(tr.base);
        }
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return base.hashCode() + size.hashCode() * MagicNumbers.C;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        String sbase = String.valueOf(base);
        if (sbase.equals("0")) return String.valueOf(size);
        else return sbase + "#" + String.valueOf(size);
    }

    public static List<TypeRange> makeList(FValue[] val) {
        ArrayList<TypeRange> a = new ArrayList<TypeRange>(val.length);
        for (FValue v : val) {
            a.add(make(v));
        }
        return a;
    }
}
