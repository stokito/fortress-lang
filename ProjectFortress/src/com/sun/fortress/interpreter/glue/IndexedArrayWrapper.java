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

import com.sun.fortress.interpreter.evaluator.values.FInt;
import com.sun.fortress.interpreter.evaluator.values.FObject;
import com.sun.fortress.interpreter.evaluator.values.FTuple;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.IndexedSource;
import com.sun.fortress.interpreter.evaluator.values.IndexedTarget;
import com.sun.fortress.interpreter.evaluator.values.Method;
import com.sun.fortress.useful.HasAt;

import static com.sun.fortress.interpreter.evaluator.ProgramError.error;
import static com.sun.fortress.interpreter.evaluator.ProgramError.errorMsg;

/** This wrapper class is used in the implementation of pasting in
    LHSEvaluator and IUOTuple.
 */
public class IndexedArrayWrapper implements IndexedTarget, IndexedSource {

    Method putter;
    Method getter;
    FObject array;
    HasAt at;
    int rank;

    // Note reuse of temporary, "copyTo" cannot (yet) go in parallel.
    ArrayList<FValue> l;


    public IndexedArrayWrapper(FValue fv, HasAt at) {
        if (!(fv instanceof FObject)) {
            // This should only happen if the library is buggy.
            // But it *has* happened, and will happen again...
            error(at,errorMsg(" expected an array object, got ", fv));
        }
        array = (FObject) fv;
        putter = (Method) array.getSelfEnv().getValue(WellKnownNames.arrayPutter);
        getter = (Method) array.getSelfEnv().getValue(WellKnownNames.arrayGetter);
        rank = Glue.arrayRank(array);
        this.at = at;
        l = new ArrayList<FValue>(1+rank);
    }

    public void put(FValue what, int[] indices, int indices_depth) {
        l.clear();
        if (indices_depth == 1) {
            l.add(FInt.make(indices[0]));
        } else {
            // This "fixes" the row-column confusion in pasted arrays.
            ArrayList<FValue> tup = new ArrayList<FValue>(rank);
            tup.add(FInt.make(indices[1]));
            tup.add(FInt.make(indices[0]));
            for (int i = 2; i < indices_depth; i++) {
                tup.add(FInt.make(indices[i]));
            }
            l.add(FTuple.make(tup));
        }
        l.add(what);
        putter.applyMethod(l,array,at, null);
    }

    public void put(FValue what, int index) {
        l.clear();
        l.add(FInt.make(index));
        l.add(what);
        putter.applyMethod(l,array,at, null);
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
        return getter.applyMethod(l,array,at, null);
    }

    public int dim() {
        return rank;
    }

}
