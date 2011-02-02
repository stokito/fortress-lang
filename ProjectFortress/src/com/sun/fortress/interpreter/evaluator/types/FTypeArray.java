/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.types;

import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.Useful;


public class FTypeArray extends FAggregateType {
    private FTypeArray(FType elt_type, TypeIndices indices) {
        super("Array");
        this.indexedType = elt_type;
        this.indices = indices;
    }

    FType indexedType;

    TypeIndices indices;

    String lazyName;

    public String getName() {
        // TODO need to stick the indices in there
        if (lazyName == null) lazyName = "Array " + Useful.inOxfords(indexedType.getName());
        return lazyName;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.evaluator.types.FType#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object other) {
        if (other instanceof FTypeArray) {
            FTypeArray fta = (FTypeArray) other;
            return fta.indexedType.equals(indexedType) && fta.indices.equals(indices);
        }
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.evaluator.types.FType#hashCode()
     */
    @Override
    public int hashCode() {
        return indexedType.hashCode() ^ indices.hashCode();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.evaluator.types.FType#subtypeOf(com.sun.fortress.interpreter.evaluator.types.FType)
     */
    @Override
    public boolean subtypeOf(FType other) {
        return commonSubtypeOf(other);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.evaluator.types.FType#toString()
     */
    @Override
    public String toString() {

        return String.valueOf(indexedType) + "[" + indices + "]";
    }

    public static FType make(FType elementType, FValue[] val, Environment e, HasAt at) {
        return make(elementType, TypeFixedDimIndices.make(val), e, at);
    }

    @Override
    public FType getElementType() {
        return indexedType;
    }

    public static FType make(FType elt_type, TypeIndices f_indices, Environment e, HasAt at) {

        //        if (f_indices instanceof TypeFixedDimIndices) {
        //            TypeFixedDimIndices tfdi = (TypeFixedDimIndices) f_indices;
        //            List<TypeRange> ranges = tfdi.getRanges();
        //
        //            // Lookup a particular trait name, and return it.
        //            int rank = ranges.size();
        //            String traitName = "Array" + String.valueOf(rank);
        //            FType arrayType = e.getTypeNull(traitName);
        //
        //            if (arrayType instanceof FTypeGeneric) {
        //                FTypeGeneric gat = (FTypeGeneric) arrayType;
        //                ArrayList<FType> args = new ArrayList<FType>(1 + 2*ranges.size());
        //                // Instantiate gat with element type, and sequence of lo-hi pairs.
        //                args.add(elt_type);
        //                for (TypeRange tr : ranges) {
        //                    args.add(tr.base);
        //                    args.add(tr.size);
        //                }
        //                return gat.make(args, at);
        //            }
        //        }

        return new FTypeArray(elt_type, f_indices);
    }
}
