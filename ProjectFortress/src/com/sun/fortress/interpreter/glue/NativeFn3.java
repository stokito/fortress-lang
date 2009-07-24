/*******************************************************************************
 Copyright 2009 Sun Microsystems, Inc.,
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

import com.sun.fortress.interpreter.evaluator.values.FValue;

import java.util.List;


/**
 * A 3-argument native function.  All the unwrapping is done by
 * applyToArgs.  The client just needs to define applyToArgs(...).
 */
public abstract class NativeFn3 extends NativeApp {
    public final int getArity() {
        return 3;
    }

    protected abstract FValue applyToArgs(FValue x, FValue y, FValue z);

    public final FValue applyToArgs(List<FValue> args) {
        return applyToArgs(args.get(0), args.get(1), args.get(2));
    }
}
