/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.glue;

import com.sun.fortress.interpreter.evaluator.values.FValue;

import java.util.List;


/**
 * A 0-argument native function.  No unwrapping is necessary, but this
 * makes things nicely uniform with the other NativeFn_k classes.  The
 * client just needs to define applyToArgs().
 */
public abstract class NativeFn0 extends NativeApp {
    public final int getArity() {
        return 0;
    }

    protected abstract FValue applyToArgs();

    public final FValue applyToArgs(List<FValue> args) {
        return applyToArgs();
    }
}
