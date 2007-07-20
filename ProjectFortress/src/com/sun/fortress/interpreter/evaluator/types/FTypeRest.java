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

package com.sun.fortress.interpreter.evaluator.types;

import com.sun.fortress.useful.Factory1;
import com.sun.fortress.useful.Memo1;

public class FTypeRest extends FType {
    private static class Factory implements Factory1<FType, FType> {

        public FType make(FType t) {
            return new FTypeRest(t);
        }
    }

    static Memo1<FType, FType> memo = null;

    static public void reset() {
        memo = new Memo1<FType, FType>(new Factory());
    }

    static public FType make(FType t) {
        return memo.make(t);
    }

    FType type;
    private FTypeRest(FType _t) {
        super(_t.getName() + "...");
        type = _t;
    }

    public FType getType() {
        return type;
    }

    /**
     * Return the type wrapped by this rest (all other types
     * return themselves).
     */
    public FType deRest() {
        return type;
    }

    public String toString() {
        return type.toString() + "...";
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.types.FType#subtypeOf(com.sun.fortress.interpreter.evaluator.types.FType)
     */
    @Override
    public boolean subtypeOf(FType other) {
        if (commonSubtypeOf(other)) return true;
        if (other instanceof FTypeRest) {
            FTypeRest that = (FTypeRest) other;
            return this.getType().subtypeOf(that.getType());
        }
        return false;
    }

}
