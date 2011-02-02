/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.types;

import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.useful.MagicNumbers;
import com.sun.fortress.useful.NI;
import com.sun.fortress.useful.Useful;

import java.util.Iterator;
import java.util.List;


public class TypeFixedDimIndices extends TypeIndices {
    public TypeFixedDimIndices(List<TypeRange> indices) {
        this.ranges = indices;
    }

    public List<TypeRange> getRanges() {
        return ranges;
    }

    List<TypeRange> ranges;

    /**
     * Tests for equality of sizes only.
     */
    public boolean compatible(TypeIndices other) {
        if (other instanceof TypeFixedDimIndices) {
            TypeFixedDimIndices tfdi = (TypeFixedDimIndices) other;
            if (ranges.size() != tfdi.ranges.size()) {
                return false; // TODO or throw an error?  different dimensionality should not come here
            }
            Iterator<TypeRange> other_iter = tfdi.ranges.iterator();
            for (TypeRange i : ranges) {
                TypeRange j = other_iter.next();
                if (!i.compatible(j)) return false;
            }
            return true;
        }

        NI.nyi("Indices compatibility, other is not fixed sized"); // TODO
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object other) {
        if (other instanceof TypeFixedDimIndices) {
            TypeFixedDimIndices tfdi = (TypeFixedDimIndices) other;
            if (ranges.size() != tfdi.ranges.size()) {
                return false; // TODO or throw an error?  different dimensionality should not come here
            }
            Iterator<TypeRange> other_iter = tfdi.ranges.iterator();
            for (TypeRange i : ranges) {
                TypeRange j = other_iter.next();
                if (!i.equals(j)) return false;
            }
            return true;
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
        int hc = MagicNumbers.D;
        for (TypeRange i : ranges) {
            hc = (hc + i.hashCode()) * MagicNumbers.E;
        }
        return hc;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        // Rely on dimensioned thingie to supply appropriate brackets.
        return Useful.listInDelimiters("", ranges, "");
    }

    public static TypeIndices make(FValue[] val) {
        return new TypeFixedDimIndices(TypeRange.makeList(val));
    }
}
