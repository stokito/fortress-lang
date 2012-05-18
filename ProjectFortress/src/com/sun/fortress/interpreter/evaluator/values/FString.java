/*******************************************************************************
 Copyright 2008,2010, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.values;

import com.sun.fortress.nodes_util.Unprinter;


public class FString extends FNativeObject {
    public static final FString EMPTY = new FString("");
    private static volatile NativeConstructor con;
    private final String val;

    private FString(String x) {
        super(null);
        val = x;
    }

    public String getString() {
        return val;
    }

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
        if (con == null) return;
        FString.con = con;
    }

    public NativeConstructor getConstructor() {
        return FString.con;
    }

    public static void resetConstructor() {
        FString.con = null;
    }
}
