/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.values;

public interface IndexedShape {
    /**
     * The size of this shape along axis i.
     * Axes are numbered from 1 to dim(), inclusive.
     */
    public int size(int i);

    /**
     * The number of axes.
     */
    public int dim();
    //public void put(FValue what, int[] indices, int indices_depth);

    /**
     * Copy this entire IndexedShape into target, beginning at the
     * location specified by toIndex and dim.
     */
    public void copyTo(IndexedTarget target, int[] toIndex, int dim);


    /**
     * Copy this entire IndexedShape into target, beginning at the
     * origin.
     */
    public void copyTo(IndexedTarget target);

}
