/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.glue;

import com.sun.fortress.compiler.WellKnownNames;
import static com.sun.fortress.exceptions.ProgramError.error;
import static com.sun.fortress.exceptions.ProgramError.errorMsg;
import com.sun.fortress.interpreter.evaluator.values.*;

import java.util.ArrayList;

/**
 * This wrapper class is used in the implementation of pasting in
 * LHSEvaluator and IUOTuple.
 */
public class IndexedArrayWrapper implements IndexedTarget, IndexedSource {

    Method putter;
    Method getter;
    FObject array;
    int rank;

    // Note reuse of temporary, "copyTo" cannot (yet) go in parallel.
    ArrayList<FValue> l;


    public IndexedArrayWrapper(FValue fv) {
        if (!(fv instanceof FObject)) {
            // This should only happen if the library is buggy.
            // But it *has* happened, and will happen again...
            error(errorMsg(" expected an array object, got ", fv));
        }
        array = (FObject) fv;
        putter = (Method) array.getSelfEnv().getLeafValue(WellKnownNames.arrayPutter);
        getter = (Method) array.getSelfEnv().getLeafValue(WellKnownNames.arrayGetter);
        rank = Glue.arrayRank(array);
        l = new ArrayList<FValue>(1 + rank);
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
        putter.applyMethod(array, l);
    }

    public void put(FValue what, int index) {
        l.clear();
        l.add(FInt.make(index));
        l.add(what);
        putter.applyMethod(array, l);
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
        return getter.applyMethod(array, l);
    }

    public int dim() {
        return rank;
    }

}
