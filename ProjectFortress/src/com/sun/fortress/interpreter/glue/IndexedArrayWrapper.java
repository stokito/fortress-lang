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

package com.sun.fortress.interpreter.glue;

import java.util.ArrayList;

import com.sun.fortress.interpreter.evaluator.ProgramError;
import com.sun.fortress.interpreter.evaluator.values.FInt;
import com.sun.fortress.interpreter.evaluator.values.FObject;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.IndexedSource;
import com.sun.fortress.interpreter.evaluator.values.IndexedTarget;
import com.sun.fortress.interpreter.evaluator.values.MethodClosure;
import com.sun.fortress.interpreter.useful.HasAt;


public class IndexedArrayWrapper implements IndexedTarget, IndexedSource {

    MethodClosure putter;
    MethodClosure getter;
    FObject array;
    HasAt at;
    int rank;

    // Note reuse of temporary, "copyTo" cannot (yet) go in parallel.
    ArrayList<FValue> l;


    public IndexedArrayWrapper(FValue fv, HasAt at) {
        if (!(fv instanceof FObject)) {
            // This should only happen if the library is buggy.
            // But it *has* happened, and will happen again...
            throw new ProgramError(at," expected an array object, got "+fv);
        }
        array = (FObject) fv;
        putter = (MethodClosure) array.getSelfEnv().getValue(WellKnownNames.arrayPutter);
        getter = (MethodClosure) array.getSelfEnv().getValue(WellKnownNames.arrayGetter);
        rank = Glue.arrayRank(array);
        this.at = at;
        l = new ArrayList<FValue>(1+rank);
    }

    public void put(FValue what, int[] indices, int indices_depth) {
        l.clear();
        l.add(what);
        if (indices_depth == 1) {
          l.add(FInt.make(indices[0]));
        } else {
            // This "fixes" the row-column confusion in pasted arrays.
            l.add(FInt.make(indices[1]));
            l.add(FInt.make(indices[0]));
            for (int i = 2; i < indices_depth; i++) {
                l.add(FInt.make(indices[i]));
            }
        }
        putter.applyMethod(l,array,at);
    }

    public void put(FValue what, int index) {
        l.clear();
        l.add(what);
        l.add(FInt.make(index));
        putter.applyMethod(l,array,at);
    }

    public FValue get(int[] indices, int indices_depth) {
        l.clear();
        if (indices_depth == 1) {
          l.add(FInt.make(indices[0]));
        } else {
            // This "fixes" the row-column confusion in pasted arrays.
            l.add(FInt.make(indices[1]));
            l.add(FInt.make(indices[0]));
            for (int i = 2; i < indices_depth; i++) {
                l.add(FInt.make(indices[i]));
            }
        }
        return getter.applyMethod(l,array,at);
    }

    public int dim() {
        return rank;
    }

}
