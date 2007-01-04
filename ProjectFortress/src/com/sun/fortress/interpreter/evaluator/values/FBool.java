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

import com.sun.fortress.interpreter.evaluator.types.FTypeBool;

public class FBool extends FValue {
    public final static FBool TRUE = new FBool(true, "True");

    public final static FBool FALSE = new FBool(false, "False");

    private boolean val;

    private String name;

    static public FBool make(boolean b) {
        return b ? TRUE : FALSE;
    }

    public Boolean getBool() {
        return val ? Boolean.TRUE : Boolean.FALSE;
    }

    public String getString() {
        return name;
    }

    private FBool(boolean b, String s) {
        val = b;
        name = s;
        setFtype(FTypeBool.T);
    }
}
