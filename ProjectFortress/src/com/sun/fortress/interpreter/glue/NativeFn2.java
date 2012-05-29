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
 * A 2-argument native function.  All the unwrapping is done by
 * applyToArgs.  The client just needs to define applyToArgs(...).
 */
public abstract class NativeFn2 extends NativeApp {
    public final int getArity() {
        return 2;
    }

    protected abstract FValue applyToArgs(FValue x, FValue y);

    public final FValue applyToArgs(List<FValue> args) {
        return applyToArgs(args.get(0), args.get(1));
    }
}
