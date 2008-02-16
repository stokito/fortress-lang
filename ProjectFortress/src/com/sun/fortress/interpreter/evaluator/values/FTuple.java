/*******************************************************************************
    Copyright 2008 Sun Microsystems, Inc.,
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
import java.util.List;

public class FTuple extends FTupleLike {

    public static FValue make(List<FValue> elems) {
        if (elems.size() <= 1) {
            if (elems.size() == 0) return FVoid.V;
            return elems.get(0);
        } else {
            return new FTuple(elems);
        }
    }

    protected FTuple() {
        super();
    }

    protected FTuple(List<FValue> elems) {
        super(elems);
    }

}
