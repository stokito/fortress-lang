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
    public final static FBool TRUE = new FBool(true);

    public final static FBool FALSE = new FBool(false);

    private final boolean val;

    private FBool(boolean b) {
        this.val = b;
    }

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
        return val ? "true" : "false";
    }

    // Stuff for nativizing FBool.
    // public void setConstructor(NativeConstructor con) {
    //     // WARNING!  In order to run the tests we must reset con for
    //     // each new test, so it's not OK to ignore setConstructor
    //     // attempts after the first one.
    //     if (con==null) return;
    //     FBool.con = con;
    // }

    // public NativeConstructor getConstructor() {
    //     return FBool.con;
    // }
}
