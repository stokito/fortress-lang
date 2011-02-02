/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.values;

import static com.sun.fortress.exceptions.ProgramError.error;
import static com.sun.fortress.exceptions.ProgramError.errorMsg;
import com.sun.fortress.interpreter.evaluator.types.FType;

import java.util.List;

public class Indexed extends FValue implements IndexedShape, IndexedTarget {
    final FType elementType;
    final FValue[] val;
    final List<FRange> extents;
    final int[] strides;

    public Indexed(IndexedShape s, FType t) {
        elementType = t;
        extents = FRange.make(s);
        strides = new int[s.dim()];
        val = new FValue[size()];

    }

    public FType type() {
        return error("Asking for type() of Indexed");
    }

    /* (non-Javadoc)
    * @see com.sun.fortress.interpreter.evaluator.values.IndexedShape#put(com.sun.fortress.interpreter.evaluator.values.FValue, int[], int)
    */
    public void put(FValue what, int[] indices, int indices_depth) {
        val[offset(indices, indices_depth, 0)] = what;
    }

    public FValue get(int[] indices, int indices_depth) {
        return val[offset(indices, indices_depth, 0)];
    }

    private int offset(int[] indices, int indices_depth, int offset) {
        FRange extent = extents.get(indices_depth - 1);
        int normalized_index = indices[indices_depth - 1] - extent.getBase();
        if (normalized_index < 0 || normalized_index >= extent.getSize()) {
            error(errorMsg("Index#",
                           (indices_depth - 1),
                           " value ",
                           indices[indices_depth - 1],
                           " is out of bounds ",
                           extent.getBase(),
                           " through ",
                           extent.getBase(),
                           extent.getSize() - 1));
        }
        if (indices_depth == 1) {
            return offset + normalized_index;
        } else {
            return offset(indices, indices_depth - 1, (offset + normalized_index) * extents.get(indices_depth - 2)
                    .getSize());
        }
    }

    public int size(int i) {
        FRange fr = extents.get(i);
        return fr.getSize();
    }

    public int dim() {
        return extents.size();
    }

    public int size() {
        int product = 1;
        int dim_index = 0;
        for (FRange r : extents) {
            strides[dim_index] = product;
            product *= r.getSize();
            dim_index++;
        }
        return product;
    }

    public boolean seqv(FValue v) {
        return false;
    }

    /* (non-Javadoc)
    * @see com.sun.fortress.interpreter.evaluator.values.IndexedShape#copyTo(com.sun.fortress.interpreter.evaluator.values.Indexed, int[], int)
    */
    public void copyTo(IndexedTarget target, int[] toIndex, int dim) {
        copyTo(target, toIndex, dim(), dim, 0);
    }

    public void copyTo(IndexedTarget target) {
        int[] indices = new int[target.dim()];
        copyTo(target, indices, indices.length);
    }

    public void copyTo(IndexedTarget target, int[] toIndex, int dim, int original_dim, int thisOffset) {
        if (dim == 0) {
            target.put(val[thisOffset], toIndex, original_dim);
        } else {
            int saved_toIndex = toIndex[dim - 1];
            for (int i = 0; i < size(dim - 1); i++) {
                toIndex[dim - 1] = i;
                copyTo(target, toIndex, dim - 1, original_dim, thisOffset + i * strides[dim - 1]);
            }
            toIndex[dim - 1] = saved_toIndex;
        }
    }

    public String getString() {
        StringBuilder sb = new StringBuilder();
        int[] index = new int[extents.size()];
        // index is initially zero.
        return formatInto(sb, index, extents.size(), 0).toString();
    }

    private StringBuilder formatInto(StringBuilder sb, int[] index, int dim, int offset) {
        if (dim == 0) {
            sb.append(val[offset]);
        } else {
            int saved_toIndex = index[dim - 1];
            for (int i = 0; i < size(dim - 1); i++) {
                if (i > 0) switch (dim) {
                    case 1:
                        sb.append(" ");
                        break;
                    case 2:
                        sb.append("\n");
                        break;
                    case 3:
                        sb.append("\n\n");
                        break;
                }
                formatInto(sb, index, dim - 1, offset + i * strides[dim - 1]);

            }
            index[dim - 1] = saved_toIndex;
        }
        return sb;

    }

}
