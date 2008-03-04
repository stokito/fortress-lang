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

import com.sun.fortress.interpreter.evaluator.types.FBuiltinType;
import com.sun.fortress.interpreter.evaluator.types.FTypeBool;

public class FBool extends FBuiltinValue {
    public final static FBool TRUE = new FBool(true, "true");

    public final static FBool FALSE = new FBool(false, "false");

    private final boolean val;

    private final String name;

    public FBuiltinType type() {
        return FTypeBool.ONLY;
    }

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
    }
}
