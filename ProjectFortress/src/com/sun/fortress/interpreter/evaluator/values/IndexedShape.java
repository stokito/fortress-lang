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
