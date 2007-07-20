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

package com.sun.fortress.interpreter.evaluator.types;

import java.util.ArrayList;
import java.util.List;

import com.sun.fortress.interpreter.evaluator.InterpreterError;
import com.sun.fortress.interpreter.evaluator.ProgramError;
import com.sun.fortress.interpreter.evaluator.values.FInt;
import com.sun.fortress.interpreter.evaluator.values.FIntLiteral;
import com.sun.fortress.interpreter.evaluator.values.FRange;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.useful.MagicNumbers;
import com.sun.fortress.useful.NI;


public class TypeRange {
    FTypeNat base;
    FTypeNat size;

    public FTypeNat getBase() { return base; }
    public FTypeNat getSize() { return size; }

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
        } else if (v instanceof FRange) {
            FRange r = (FRange) v;
            return new TypeRange(r.getBase(), r.getSize());
        } else
            return NI.ni();
    }

    public long getEvaluatedBase() {
        return evaluatedNat(base);
    }

    public long getEvaluatedSize() {
        return evaluatedNat(size);
    }

    private static long evaluatedNat(FTypeNat x) throws ProgramError {
        if (x instanceof IntNat) {
            return ((IntNat) x).getValue();
        }
        throw new InterpreterError("Expected fully evaluated nat parameter, found " + x);
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
        else return  sbase + "#" + String.valueOf(size);
    }

    public static List<TypeRange> makeList(FValue[] val) {
        ArrayList<TypeRange> a = new ArrayList<TypeRange>(val.length);
        for (FValue v : val) {
            a.add(make(v));
        }
        return a;
    }
}
