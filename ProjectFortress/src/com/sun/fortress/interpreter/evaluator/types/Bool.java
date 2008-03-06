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

import com.sun.fortress.interpreter.evaluator.values.FBool;

/**
 * The type for "true" and "false", that can appear as
 * "bool" parameters to generics.  Do not confuse this with
 * the type of boolean-valued variables (FBool), or the two
 * boolean constant values.
 */
public class Bool extends FType {

    static final Bool TRUE  = new Bool("true",  FBool.TRUE);
    static final Bool FALSE = new Bool("false", FBool.FALSE);

    FBool value;

    private Bool(String s, FBool value) {
        super(s);
        this.value = value;
        cannotBeExtended = true;
    }

    public static Bool make(boolean b) {
        return b ? Bool.TRUE : Bool.FALSE;
    }

    public FBool getValue() {
        return value;
    }

    public boolean getBooleanValue() {
        return value.getBool();
    }

}
