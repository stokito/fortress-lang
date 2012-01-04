/*******************************************************************************
 Copyright 2008,2010, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.values;


public class FBool extends FNativeObject {
    public final static FBool TRUE = new FBool(true);
    public final static FBool FALSE = new FBool(false);
    private static volatile NativeConstructor con;

    private final boolean val;

    private FBool(boolean b) {
        super(null);
        this.val = b;
    }

    public FBool(NativeConstructor con) {
        super(con);
        setConstructor(con);
        val = false;
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

    public boolean seqv(FValue v) {
        // Pointer equivalence gets checked before call.
        return false;
    }

    // Stuff for nativizing FBool.
    public static void setConstructor(NativeConstructor con) {
        // WARNING!  In order to run the tests we must reset con for
        // each new test, so it's not OK to ignore setConstructor
        // attempts after the first one.
        if (con == null) return;
        FBool.con = con;
    }

    public NativeConstructor getConstructor() {
        return FBool.con;
    }

    public static void resetConstructor() {
        FBool.con = null;
    }
}
