/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.types;

public class BottomType extends FType {

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.types.FType#subtypeOf(com.sun.fortress.interpreter.evaluator.types.FType)
     */
    @Override
    public boolean subtypeOf(FType other) {
        return true;
    }

    private BottomType(String s) {
        super(s);
    }

    static public final BottomType ONLY = new BottomType("BottomType");

    public String toString() {
        return "BOTTOM";
    }
}
