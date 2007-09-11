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

package com.sun.fortress.interpreter.evaluator;

import com.sun.fortress.interpreter.evaluator.values.FString;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.useful.HasAt;

import static com.sun.fortress.interpreter.evaluator.ProgramError.errorMsg;
import static com.sun.fortress.interpreter.evaluator.ProgramError.error;

/**
 * A BaseEnv supplies (enforces!) some overloadings that
 * every environment must support.
 */

abstract public class BaseEnv implements Environment {

    static public String string(FValue f1) {
        return ((FString) f1).getString();
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.Environment#assignValue(com.sun.fortress.interpreter.evaluator.values.FValue, com.sun.fortress.interpreter.evaluator.values.FValue)
     */
    // final public void assignValue(FValue f1, FValue f2) {
    //     assignValue(string(f1), f2);
    // }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.Environment#casValue(com.sun.fortress.interpreter.evaluator.values.FValue, com.sun.fortress.interpreter.evaluator.values.FValue, com.sun.fortress.interpreter.evaluator.values.FValue)
     */
    final public Boolean casValue(FValue f1, FValue old_value, FValue new_value) {
        return casValue(string(f1), old_value, new_value);
    }

    final public void putValue(FValue f1, FValue f2) {
        putValue(string(f1), f2);
    }

    final public  FValue getValue(FValue f1) {
        return getValue(string(f1));
    }

    public void assignValue(HasAt loc, String str, FValue f2) {
        // TODO track down references, catch error, and fix.
        if (hasValue(str)) putValueUnconditionally(str, f2);
        else error(loc,this, errorMsg("Cannot assign to unbound ", str));
    }

    abstract public void putValueUnconditionally(String str, FValue f2);
}
