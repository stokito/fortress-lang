/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.types;

import com.sun.fortress.interpreter.evaluator.values.FBool;

/**
 * The type for "true" and "false", that can appear as
 * "bool" parameters to generics.  Do not confuse this with
 * the type of boolean-valued variables (FBool), or the two
 * boolean constant values.
 */
public class Bool extends BoolType {

    static final Bool TRUE = new Bool("true", FBool.TRUE);
    static final Bool FALSE = new Bool("false", FBool.FALSE);

    final FBool value;

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
