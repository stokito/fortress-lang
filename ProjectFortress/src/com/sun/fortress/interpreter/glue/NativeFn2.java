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

package com.sun.fortress.interpreter.glue;

import java.util.List;

import com.sun.fortress.interpreter.evaluator.values.FValue;


/**
 * A 2-argument native function.  All the unwrapping is done by
 * applyToArgs.  The client just needs to define act().
 */
public abstract class NativeFn2 extends NativeApp {
    public final int getArity() { return 2; }
    protected abstract FValue act(FValue x, FValue y);
    public final FValue applyToArgs(List<FValue> args) {
        return act(args.get(0), args.get(1));
    }
}
