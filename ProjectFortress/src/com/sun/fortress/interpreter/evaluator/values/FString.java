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

import com.sun.fortress.interpreter.evaluator.values.NativeConstructor;
import com.sun.fortress.nodes_util.Unprinter;


public class FString extends NativeConstructor.FNativeObject {
    public static final FString EMPTY = new FString("");
    private static volatile NativeConstructor con;
    private final String val;

    protected FString(String x) {
        super(null);
        val = x;
    }

    public String getString() {return val;}

    public String toString() {
        return "\"" + Unprinter.enQuote(val) + "\"";
    }

    public boolean seqv(FValue v) {
        if (!(v instanceof FString)) return false;
        return (getString().equals(v.getString()));
    }

    public static FString make(String s) {
        return new FString(s);
    }

    // Stuff for nativizing.
    public static void setConstructor(NativeConstructor con) {
        // WARNING!  In order to run the tests we must reset con for
        // each new test, so it's not OK to ignore setConstructor
        // attempts after the first one.
        if (con==null) return;
        FString.con = con;
    }

    public NativeConstructor getConstructor() {
        return FString.con;
    }
}
