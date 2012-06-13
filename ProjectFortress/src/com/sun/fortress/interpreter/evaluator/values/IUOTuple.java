/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.values;

import static com.sun.fortress.exceptions.ProgramError.error;
import static com.sun.fortress.exceptions.ProgramError.errorMsg;
import com.sun.fortress.interpreter.glue.Glue;
import com.sun.fortress.interpreter.glue.IndexedArrayWrapper;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.Useful;

import java.util.List;

/**
 * Like a tuple, but not. Useful for intermediate results from array pastings
 * (and perhaps other constructors, but we need to tread carefully here).
 */

public class IUOTuple extends FTupleLike implements IndexedShape {
    /**
     * The rank of this pasting.
     */
    int rank;

    /**
     * The rank of the inputs.
     */
    int elementRank;

    public int resultRank() {
        return Math.max(rank, elementRank);
    }

    /**
     * Allocated to rank (outermost! -- inner pastes share) Elements 0 through
     * rank, inclusive, tell how many elements are encountered along each
     * dimension in the paste. That is, how many "bricks", ignoring the sizes of
     * the bricks.
     */
    int[] pastingLengths;

    /**
     * For each axis, the series of extents along that axis. That is, the size
     * of the bricks at that position along that axis.
     */
    int[][] extents;

    int[] extentSums;

    HasAt at;

    public IUOTuple(List<FValue> elems, HasAt at) {
        super(elems);
        this.at = at;
        int rank_seen = -1;
        int element_rank_seen = -1;

        /*
         * Scan the pasting elements, accumulate rank and element rank upwards.
         */
        for (FValue i : elems) {
            // Used twice -- once for tuple rank, then for rank of
            // elements collected in the paste.
            Integer element_rank;

            // tuple case
            if (i instanceof IUOTuple) {
                element_rank = Integer.valueOf(((IUOTuple) i).rank);
                if (rank_seen < 0) {
                    rank_seen = element_rank.intValue();
                } else if (rank_seen != element_rank.intValue()) {
                    element_rank = error(at, errorMsg("Mixed-rank pastings within array/matrix paste"));
                }
                element_rank = Integer.valueOf(((IUOTuple) i).elementRank);

                // non-tuple #1 -- there should be no other tuple elements
            } else if (rank_seen > 0) {
                element_rank = error(at, errorMsg("Mixed-rank (paste/nonpaste) elements within array/matrix paste"));
                // The rank is negative or zero, make it zero
            } else {
                rank_seen = 0;
                element_rank = Integer.valueOf(Glue.arrayRank(i));
            }

            // Sanity-check the element rank; they had better all match.
            if (element_rank_seen <= 0) {
                element_rank_seen = element_rank.intValue();
            } else if (element_rank_seen != element_rank.intValue() && element_rank.intValue() > 0) {
                error(at, errorMsg("Mixed-rank elements within array/matrix paste"));
            }

        }
        rank = rank_seen + 1;
        if (element_rank_seen >= 0) elementRank = element_rank_seen;
    }

    public void finish() {
        // Push newly allocated arrays through the paste.
        // Members of the paste of lower rank will only
        // access a prefix of the arrays.

        int result_rank = resultRank();

        setLengths(new int[rank], new int[result_rank], new int[rank][]);
        // Finish initializing the extents arrays.
        // Fill in -1 to express lack of a constraint.
        for (int i = 0; i < extents.length; i++) {
            int[] a = new int[pastingLengths[i]];
            extents[i] = a;
            for (int j = 0; j < a.length; j++) {
                a[j] = -1;
            }
        }
        // Next push lengths out of the leaves into extents,
        // and check for consistency.
        int[] coordinate = new int[rank];

        pushLengths(coordinate);

        for (int i = 0; i < extents.length; i++) {
            int sum = 0;
            for (int j = 0; j < extents[i].length; j++) {
                int a = extents[i][j];
                if (a < 0) {// all scalar!
                    a = 1;
                    extents[i][j] = 1;
                }
                sum += extents[i][j];
            }
            extentSums[i] = sum;
        }

    }

    private void setLengths(int[] lens, int[] extent_sums, int[][] extents) {
        pastingLengths = lens;
        extentSums = extent_sums;
        this.extents = extents;

        for (FValue i : getVals()) {
            if (i instanceof IUOTuple) {
                ((IUOTuple) i).setLengths(lens, extent_sums, extents);
            }
        }
        int l = getVals().size();
        if (lens[rank - 1] == 0) {
            lens[rank - 1] = l;
        } else if (lens[rank - 1] != l) {
            error(at, errorMsg("At paste level ", rank, " pasting lengths ", l, " and ", lens[rank], " do not match"));
        }
    }

    /**
     * Iterated recursion to leaves, accumulating a coordinate which is then
     * used to set/check the extents of the elements being pasted.
     */
    private void pushLengths(int[] coordinate) {
        int j = 0;

        for (FValue i : getVals()) {
            coordinate[rank - 1] = j;
            if (i instanceof IUOTuple) {
                ((IUOTuple) i).pushLengths(coordinate);
            } else {
                // Note that elementRank suffices -- if it is less than the
                // pasting rank, then all the elements are scalars anyhow.
                for (int k = 0; k < elementRank; k++) {
                    // Get the length along axis [k]
                    int length = Glue.lengthAlongArrayAxis(i, k);
                    if (length >= 0) {
                        // If the dimension being measured is within
                        // the paste, then it is must set/match an extent,
                        // otherwise
                        // it must affects the output dimension of the paste.
                        if (k < extents.length) {
                            int[] along_k = extents[k];
                            int current = along_k[coordinate[k]];
                            if (current < 0) {
                                along_k[coordinate[k]] = length;
                            } else if (length != current) {
                                error(at, errorMsg("Element at ",
                                                   Useful.coordInDelimiters("[", coordinate, k, "]"),
                                                   " has extent ",
                                                   length,
                                                   " along axis ",
                                                   k,
                                                   " but an earlier element has length ",
                                                   current));
                            }
                        } else {
                            int current = extentSums[k];
                            if (current < 0) {
                                extentSums[k] = length;
                            } else if (length != current) {
                                error(at, errorMsg("Element at ",
                                                   Useful.coordInDelimiters("[", coordinate, "]"),
                                                   " has extent ",
                                                   length,
                                                   " along axis ",
                                                   k,
                                                   " but an earlier element has length ",
                                                   current));
                            }
                        }
                    }
                }
            }
            j++;
        }
    }

    public int size(int i) {
        return extentSums[i];
    }

    /**
     * Returns the PASTING rank. This can be smaller than the element rank or
     * the result rank.
     */
    public int dim() {
        return rank;
    }

    /**
     * Copy this pasting into target starting at the origin.
     */
    public void copyTo(IndexedTarget target) {
        int[] indices = new int[target.dim()];
        copyTo(target, indices, indices.length);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.evaluator.values.IndexedShape#copyTo(com.sun.fortress.interpreter.evaluator.values.Indexed,
     *      int[], int)
     */
    public void copyTo(IndexedTarget target, int[] toIndex, int dim) {
        int[] fromIndex = new int[rank];
        copyTo(target, toIndex, fromIndex, dim(), dim);
    }

    public void copyTo(IndexedTarget target, int[] toIndex, int[] pasteIndex, int from_dim, int target_dim) {
        int saved = toIndex[from_dim - 1];
        int saved_from = pasteIndex[from_dim - 1];
        List<FValue> vals = getVals();

        if (from_dim == 1) {
            for (int j = 0; j < vals.size(); j++) {
                FValue v = vals.get(j);
                if (Glue.arrayRank(v) <= 0) {
                    scalarCopyTo(target, toIndex, pasteIndex, toIndex.length, target_dim, v);
                    // target.put(v, toIndex, target_dim);
                } else {
                    int[] fromIndex = new int[target_dim];
                    arrayCopyto(target,
                                toIndex,
                                pasteIndex,
                                toIndex.length,
                                fromIndex,
                                target_dim,
                                new IndexedArrayWrapper(v));
                }
                // if v is scalar, spread it across the
                // extent, as appropriate.

                // if v is aggregate, copy it across.

                toIndex[from_dim - 1] += extents[from_dim - 1][j];
                pasteIndex[from_dim - 1]++;
            }
        } else {
            for (int j = 0; j < vals.size(); j++) {
                // The elements, if dimension > 1, should be other IUOTuples.
                IUOTuple shape = (IUOTuple) vals.get(j);
                shape.copyTo(target, toIndex, pasteIndex, from_dim - 1, target_dim);
                // The increment depends on the dimensionality of the
                // pasted item.
                toIndex[from_dim - 1] += extents[from_dim - 1][j];
                pasteIndex[from_dim - 1]++;

                // if (shape.dim() >= rank) {
                // toIndex[dim - 1] += shape.size(dim - 1);
                // } else {
                // toIndex[dim - 1] += 1;
                // }
            }
        }
        toIndex[from_dim - 1] = saved;
        pasteIndex[from_dim - 1] = saved_from;
    }

    private void arrayCopyto(IndexedTarget target,
                             int[] toIndex,
                             int[] pasteIndex,
                             int dim,
                             int[] fromIndex,
                             int targetDim,
                             IndexedSource v) {
        if (dim == 0) target.put(v.get(fromIndex, targetDim), toIndex, targetDim);
        else {
            int lo = 0;
            int hi = (dim <= extents.length) ? extents[dim - 1][pasteIndex[dim - 1]] : extentSums[dim - 1];
            int saved_to = toIndex[dim - 1];
            int saved_from = fromIndex[dim - 1];

            for (int i = lo; i < hi; i++) {

                arrayCopyto(target, toIndex, pasteIndex, dim - 1, fromIndex, targetDim, v);
                toIndex[dim - 1]++;
                fromIndex[dim - 1]++;

            }

            fromIndex[dim - 1] = saved_from;
            toIndex[dim - 1] = saved_to;
        }
    }

    /**
     * Using the extents at this coordinate, copy the scalar value v into
     * target, filling in starting at toIndex.
     *
     * @param target
     * @param toIndex
     * @param pasteIndex
     * @param dim
     * @param original_dim
     * @param v
     */
    private void scalarCopyTo(IndexedTarget target,
                              int[] toIndex,
                              int[] pasteIndex,
                              int dim,
                              int original_dim,
                              FValue v) {
        if (dim == 0) {
            target.put(v, toIndex, original_dim);
        } else {
            int lo = 0;
            int hi = (dim <= extents.length) ? extents[dim - 1][pasteIndex[dim - 1]] : extentSums[dim - 1];
            int saved_to = toIndex[dim - 1];
            for (int i = lo; i < hi; i++) {

                scalarCopyTo(target, toIndex, pasteIndex, dim - 1, original_dim, v);
                toIndex[dim - 1]++;
            }

            toIndex[dim - 1] = saved_to;

        }
    }

    public int[] resultExtents() {
        return extentSums;
    }

}
