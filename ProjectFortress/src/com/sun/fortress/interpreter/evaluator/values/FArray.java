/*******************************************************************************
 Copyright 2009 Sun Microsystems, Inc.,
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

import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.types.FTypeArray;
import com.sun.fortress.useful.HasAt;


public class FArray extends FConstructedValue implements IndexedShape {
    final Indexed val;

    public FArray(Indexed v, Environment env, HasAt at) {
        super(FTypeArray.make(v.elementType, v.val, env, at));
        val = v;
    }

    public FArray(Indexed v, FTypeArray ft) {
        super(ft);
        val = v;
    }

    public int size(int i) {
        return val.size(i);
    }

    public int dim() {
        return val.dim();
    }

    public void copyTo(IndexedTarget target, int[] toIndex, int dim) {
        val.copyTo(target, toIndex, dim);
    }

    public void copyTo(IndexedTarget target) {
        val.copyTo(target);
    }
}
